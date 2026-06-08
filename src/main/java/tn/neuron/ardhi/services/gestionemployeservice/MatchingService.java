package tn.neuron.ardhi.services.gestionemployeservice;

import tn.neuron.ardhi.models.gestionemployemodel.Employe;
import tn.neuron.ardhi.models.gestionemployemodel.Tache;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 🤖 MatchingService IA AVANCÉ v2
 *
 * Améliorations :
 *  ① Indice de confiance IA (écart avec le 2e candidat)
 *  ② Score compétences RÉEL via CompetenceService (niveau/niveauRequis)
 *  ③ Pondération dynamique : tâche urgente (priorité ≥ 4) → +poids disponibilité
 *  ④ Bonus affectation + bonus similarité mots-clés conservés
 *  ⑤ Raison enrichie avec indice de confiance
 */
public class MatchingService {

    private Connection getCnx() {
        return MyDatabase.getInstance().getCnx();
    }

    private final CompetenceService competenceService = new CompetenceService();

    // ══════════════════════════════════════════════════════════════════════
    // RECOMMANDATION PRINCIPALE
    // ══════════════════════════════════════════════════════════════════════

    public List<RecommandationResult> recommanderEmployes(int idTache, int limit) {
        System.out.println("🤖 Recommandation IA pour tâche " + idTache);
        List<RecommandationResult> results = new ArrayList<>();

        try {
            Tache tache = getTacheById(idTache);
            if (tache == null) {
                System.err.println("❌ Tâche introuvable: " + idTache);
                return results;
            }

            List<Employe> employes = getAllActiveEmployes();
            System.out.println("📊 Analyse de " + employes.size() + " employés pour \"" + tache.getTitre() + "\"");

            // Mots-clés de la tâche pour bonus similarité
            String[] motsClesTache = extraireMotsCles(
                    tache.getTitre(), tache.getDescription(), tache.getCategorie()
            );

            // ③ Pondération dynamique selon urgence
            double poidsCompetence   = 0.35;
            double poidsExperience   = 0.20;
            double poidsPerformance  = 0.25;
            double poidsDisponibilite = 0.20;

            if (tache.getPriorite() != null && tache.getPriorite() >= 4) {
                // Tâche critique/haute priorité → disponibilité plus importante
                poidsDisponibilite = 0.30;
                poidsCompetence    = 0.30;
                poidsExperience    = 0.20;
                poidsPerformance   = 0.20;
                System.out.println("⚡ Tâche prioritaire détectée → pondération ajustée (dispo ↑)");
            }

            for (Employe emp : employes) {
                RecommandationResult result = new RecommandationResult();
                result.employe = emp;
                result.idTache = idTache;

                // ─── 1. Compétences RÉELLES via CompetenceService ────────────
                result.scoreCompetences = calculerScoreCompetencesReelles(emp.getId(), idTache);

                // ─── 2. Expérience : nb tâches terminées dans la même catégorie
                int nbTerminees = countTachesTerminees(emp.getId());
                result.scoreExperience = calculerScoreExperience(emp.getId(), tache.getCategorie(), nbTerminees);

                // ─── 3. Performance RÉELLE : taux de complétion
                int nbTotal   = countTachesTotal(emp.getId());
                int nbEnCours = countTachesEnCours(emp.getId());
                result.scorePerformance = (nbTotal > 0)
                        ? Math.min((nbTerminees * 100.0 / nbTotal), 100.0)
                        : 50.0;

                // ─── 4. Disponibilité
                result.scoreDisponibilite = Math.max(100.0 - (nbEnCours * 20.0), 0.0);

                // ─── 5. BONUS : déjà assigné à cette tâche (+15)
                Integer assignedId = tache.getIdEmploye();
                if (assignedId != null && assignedId == emp.getId()) {
                    result.scoreCompetences = Math.min(result.scoreCompetences + 15.0, 100.0);
                    System.out.println("⭐ Bonus affectation → " + emp.getPrenom() + " " + emp.getNom());
                }

                // ─── 6. BONUS : similarité mots-clés (jusqu'à +20)
                double bonusSim = calculerBonusSimilarite(emp.getId(), motsClesTache);
                result.scoreCompetences = Math.min(result.scoreCompetences + bonusSim, 100.0);

                // ─── Score total pondéré dynamiquement ──────────────────────
                result.scoreTotal =
                        (result.scoreCompetences   * poidsCompetence)   +
                                (result.scoreExperience    * poidsExperience)   +
                                (result.scorePerformance   * poidsPerformance)  +
                                (result.scoreDisponibilite * poidsDisponibilite);

                result.raisonRecommandation = genererRaison(result, nbTerminees, nbEnCours, tache);
                results.add(result);
            }

            // Tri décroissant
            results.sort((a, b) -> Double.compare(b.scoreTotal, a.scoreTotal));
            if (results.size() > limit) results = results.subList(0, limit);

            // ① Calcul de l'indice de confiance IA
            if (!results.isEmpty()) {
                if (results.size() > 1) {
                    double diff = results.get(0).scoreTotal - results.get(1).scoreTotal;
                    results.get(0).indiceConfiance = Math.min(Math.max(diff * 2.5, 10), 100);
                } else {
                    results.get(0).indiceConfiance = 90.0;
                }
                // Les autres ont une confiance décroissante
                for (int i = 1; i < results.size(); i++) {
                    results.get(i).indiceConfiance = Math.max(results.get(0).indiceConfiance - (i * 15), 10);
                }
            }

            System.out.println("✅ " + results.size() + " recommandation(s) IA générée(s)");

        } catch (Exception e) {
            System.err.println("❌ Erreur recommandation IA: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    // ══════════════════════════════════════════════════════════════════════
    // ② SCORE COMPÉTENCES RÉELLES (CompetenceService)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Score compétences pondéré par importance et ratio niveau/niveauRequis.
     * Retourne 50 si aucune compétence requise définie pour la tâche.
     */
    private double calculerScoreCompetencesReelles(int idEmploye, int idTache) {
        try {
            var competencesEmploye = competenceService.getEmployeCompetences(idEmploye);
            var competencesTache   = competenceService.getTacheCompetencesRequises(idTache);

            if (competencesTache.isEmpty()) {
                // Pas de compétences définies → fallback sur catégorie
                return 50.0;
            }

            double score          = 0;
            double totalImportance = 0;

            for (var entry : competencesTache.entrySet()) {
                var comp = entry.getKey();
                var req  = entry.getValue();

                totalImportance += req.importance;

                if (competencesEmploye.containsKey(comp)) {
                    var empDetails = competencesEmploye.get(comp);
                    double ratio = req.niveauRequis > 0
                            ? (double) empDetails.niveau / req.niveauRequis
                            : 1.0;
                    // ratio plafonné à 1.2 (surqualification légèrement récompensée)
                    score += Math.min(ratio, 1.2) * req.importance * 20.0;
                }
            }

            return totalImportance > 0 ? Math.min(score / totalImportance, 100.0) : 50.0;

        } catch (Exception e) {
            // CompetenceService non disponible → fallback
            System.err.println("⚠️ CompetenceService non disponible, fallback catégorie: " + e.getMessage());
            return hasExperienceSimilaireSQL(idEmploye, getCategorieTache(idTache)) ? 75.0 : 45.0;
        }
    }

    private String getCategorieTache(int idTache) {
        String query = "SELECT categorie FROM tache WHERE id_tache = ?";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idTache);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("categorie");
            }
        } catch (SQLException e) { /* ignore */ }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════
    // SCORE EXPÉRIENCE
    // ══════════════════════════════════════════════════════════════════════

    private double calculerScoreExperience(int idEmploye, String categorie, int nbTerminees) {
        // Base : nb tâches terminées (cap à 100)
        double base = Math.min(nbTerminees * 10.0, 100.0);
        // Bonus : expérience dans la même catégorie
        if (hasExperienceSimilaireSQL(idEmploye, categorie)) {
            base = Math.min(base + 15.0, 100.0);
        }
        return base;
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS SQL
    // ══════════════════════════════════════════════════════════════════════

    private int countTachesTerminees(int idEmploye) {
        String query = "SELECT COUNT(*) FROM tache WHERE id_employe = ? " +
                "AND statut IN ('Terminé','Termine','TERMINE','Validé','VALIDE')";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idEmploye);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ countTachesTerminees: " + e.getMessage());
        }
        return 0;
    }

    private int countTachesEnCours(int idEmploye) {
        String query = "SELECT COUNT(*) FROM tache WHERE id_employe = ? " +
                "AND statut IN ('En cours','EnCours','Nouveau','EN_COURS')";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idEmploye);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ countTachesEnCours: " + e.getMessage());
        }
        return 0;
    }

    private int countTachesTotal(int idEmploye) {
        String query = "SELECT COUNT(*) FROM tache WHERE id_employe = ?";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idEmploye);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ countTachesTotal: " + e.getMessage());
        }
        return 0;
    }

    private boolean hasExperienceSimilaireSQL(int idEmploye, String categorie) {
        if (categorie == null || categorie.isEmpty()) return false;
        String query = "SELECT COUNT(*) FROM tache WHERE id_employe = ? " +
                "AND categorie = ? AND statut IN ('Terminé','Termine','Validé')";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idEmploye);
            stmt.setString(2, categorie);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ hasExperienceSimilaire: " + e.getMessage());
        }
        return false;
    }

    // ══════════════════════════════════════════════════════════════════════
    // BONUS SIMILARITÉ MOTS-CLÉS
    // ══════════════════════════════════════════════════════════════════════

    private String[] extraireMotsCles(String titre, String description, String categorie) {
        StringBuilder sb = new StringBuilder();
        if (titre       != null) sb.append(" ").append(titre.toLowerCase());
        if (description != null) sb.append(" ").append(description.toLowerCase());
        if (categorie   != null) sb.append(" ").append(categorie.toLowerCase());
        return sb.toString().trim().split("\\s+");
    }

    private double calculerBonusSimilarite(int idEmploye, String[] motsClesTache) {
        if (motsClesTache == null || motsClesTache.length == 0) return 0;
        List<String> titresPasses = getTitresTachesPassees(idEmploye);
        if (titresPasses.isEmpty()) return 0;

        String historique = String.join(" ", titresPasses).toLowerCase();
        int motsCorrespondants = 0, motsSignificatifs = 0;
        for (String mot : motsClesTache) {
            if (mot.length() < 3) continue;
            motsSignificatifs++;
            if (historique.contains(mot)) motsCorrespondants++;
        }
        if (motsSignificatifs == 0) return 0;
        double ratio = (double) motsCorrespondants / motsSignificatifs;
        double bonus = ratio * 20.0;
        if (bonus > 0)
            System.out.printf("  🔍 Similarité emp#%d: %.0f%% → +%.1f pts%n", idEmploye, ratio * 100, bonus);
        return bonus;
    }

    private List<String> getTitresTachesPassees(int idEmploye) {
        List<String> titres = new ArrayList<>();
        String query = "SELECT titre FROM tache WHERE id_employe = ?";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idEmploye);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) { String t = rs.getString("titre"); if (t != null) titres.add(t); }
            }
        } catch (SQLException e) { System.err.println("❌ getTitresTachesPassees: " + e.getMessage()); }
        return titres;
    }

    // ══════════════════════════════════════════════════════════════════════
    // GÉNÉRATION DE LA RAISON
    // ══════════════════════════════════════════════════════════════════════

    private String genererRaison(RecommandationResult result, int tachesTerminees,
                                 int tachesEnCours, Tache tache) {
        StringBuilder raison = new StringBuilder();

        if (tache.getIdEmploye() != null && tache.getIdEmploye() == result.employe.getId()) {
            raison.append("⭐ Déjà assigné à cette tâche. ");
        }
        if (result.scoreCompetences >= 90)
            raison.append("🎯 Compétences excellentes pour ce poste. ");
        else if (result.scoreCompetences >= 70)
            raison.append("🎯 Compétences bien adaptées. ");

        if (tachesTerminees > 10)
            raison.append("✅ Très expérimenté (").append(tachesTerminees).append(" tâches). ");
        else if (tachesTerminees > 5)
            raison.append("✓ Expérience solide (").append(tachesTerminees).append(" tâches). ");
        else if (tachesTerminees > 0)
            raison.append("Quelques tâches terminées (").append(tachesTerminees).append("). ");

        if (tachesEnCours == 0)
            raison.append("📅 Totalement disponible. ");
        else if (tachesEnCours <= 2)
            raison.append("Disponibilité correcte (").append(tachesEnCours).append(" tâches en cours). ");
        else
            raison.append("⚠️ Charge élevée (").append(tachesEnCours).append(" en cours). ");

        double perf = result.scorePerformance;
        if (perf >= 80)
            raison.append("📈 Excellente performance (").append((int) perf).append("%). ");
        else if (perf >= 60)
            raison.append("📊 Bonne performance (").append((int) perf).append("%). ");
        else if (perf < 40 && perf > 0)
            raison.append("⚠️ Performance faible (").append((int) perf).append("%). ");

        String r = raison.toString().trim();
        return r.isEmpty() ? "Employé actif disponible." : r;
    }

    // ══════════════════════════════════════════════════════════════════════
    // RÉCUPÉRATION DES DONNÉES
    // ══════════════════════════════════════════════════════════════════════

    private Tache getTacheById(int id) {
        String query = "SELECT * FROM tache WHERE id_tache = ?";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Tache tache = new Tache();
                    tache.setId(rs.getInt("id_tache"));
                    tache.setTitre(rs.getString("titre"));
                    tache.setDescription(rs.getString("description"));
                    tache.setCategorie(rs.getString("categorie"));
                    try { tache.setPriorite(rs.getInt("priorite")); } catch (Exception ignored) {}
                    try { tache.setIdEmploye(rs.getInt("id_employe")); } catch (Exception ignored) {}
                    return tache;
                }
            }
        } catch (SQLException e) { System.err.println("❌ getTacheById: " + e.getMessage()); }
        return null;
    }

    private List<Employe> getAllActiveEmployes() {
        List<Employe> employes = new ArrayList<>();
        String query = "SELECT * FROM employe WHERE actif = TRUE ORDER BY nom, prenom";
        try (Statement stmt = getCnx().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Employe emp = new Employe();
                emp.setId(rs.getInt("id_employe"));
                emp.setNom(rs.getString("nom"));
                emp.setPrenom(rs.getString("prenom"));
                emp.setEmail(rs.getString("email"));
                emp.setPoste(rs.getString("poste"));
                emp.setActif(true);
                try { emp.setPhotoPath(rs.getString("photo_path")); } catch (Exception ignored) {}
                employes.add(emp);
            }
        } catch (SQLException e) { System.err.println("❌ getAllActiveEmployes: " + e.getMessage()); }
        return employes;
    }

    public boolean marquerRecommandationAcceptee(int idTache, int idEmploye) {
        System.out.println("✅ Recommandation acceptée : Tâche " + idTache + " → Employé " + idEmploye);
        return true;
    }

    // ══════════════════════════════════════════════════════════════════════
    // ① CLASSE RÉSULTAT — avec indiceConfiance
    // ══════════════════════════════════════════════════════════════════════

    public static class RecommandationResult {
        public Employe employe;
        public int     idTache;
        public double  scoreTotal;
        public double  scoreCompetences;
        public double  scoreExperience;
        public double  scorePerformance;
        public double  scoreDisponibilite;
        public double  indiceConfiance;       // ① NOUVEAU
        public String  raisonRecommandation;

        public String getAppreciation() {
            if (scoreTotal >= 85) return "Excellent match";
            if (scoreTotal >= 70) return "Très bon profil";
            if (scoreTotal >= 55) return "Bon candidat";
            if (scoreTotal >= 40) return "Profil acceptable";
            return "Peu adapté";
        }

        public String getEmoji() {
            if (scoreTotal >= 85) return "🥇";
            if (scoreTotal >= 70) return "🥈";
            if (scoreTotal >= 55) return "🥉";
            if (scoreTotal >= 40) return "✓";
            return "⚠";
        }

        public String getCouleur() {
            if (scoreTotal >= 85) return "#27ae60";
            if (scoreTotal >= 70) return "#3498db";
            if (scoreTotal >= 55) return "#f39c12";
            if (scoreTotal >= 40) return "#95a5a6";
            return "#e74c3c";
        }

        public String getConfianceLabel() {
            if (indiceConfiance >= 80) return "Très haute";
            if (indiceConfiance >= 60) return "Haute";
            if (indiceConfiance >= 40) return "Moyenne";
            return "Basse";
        }

        @Override
        public String toString() {
            return String.format("%s %s %s — Score: %.1f/100 (%s) | Confiance: %.0f%%",
                    getEmoji(), employe.getPrenom(), employe.getNom(),
                    scoreTotal, getAppreciation(), indiceConfiance);
        }
    }
}