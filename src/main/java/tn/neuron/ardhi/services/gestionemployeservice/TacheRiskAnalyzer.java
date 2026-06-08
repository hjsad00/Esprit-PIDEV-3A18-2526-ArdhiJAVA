package tn.neuron.ardhi.services.gestionemployeservice;

import tn.neuron.ardhi.models.gestionemployemodel.Tache;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║         MODULE IA – PRÉDICTION DE RISQUE DE RETARD              ║
 * ║                  Ardhi · Gestion Agricole                       ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * Algorithme de scoring multi-facteurs :
 *
 *   riskScore =
 *       (tauxRetardsHistoriques  * 0.30)   // Historique employé
 *     + (tauxChargeActuelle      * 0.20)   // Charge de travail
 *     + (complexiteTache         * 0.25)   // Priorité + catégorie
 *     + (facteurSaisonnier       * 0.10)   // Mois / saison agricole
 *     + (pressionDelai           * 0.15)   // Durée estimée vs deadline
 *
 *  Score 0–100 → niveau de risque et recommandations.
 */
public class TacheRiskAnalyzer {

    // ── Résultat de l'analyse ──────────────────────────────────────────────
    public static class RiskResult {
        public final double riskScore;          // 0–100
        public final String niveau;             // FAIBLE / MODÉRÉ / ÉLEVÉ / CRITIQUE
        public final String couleur;            // CSS hex
        public final String emoji;
        public final double probabiliteReussite;
        public final List<String> facteurs;     // Détail des facteurs
        public final List<String> recommandations;

        // Détails des sous-scores (pour le jury 😎)
        public final double scoreHistorique;
        public final double scoreCharge;
        public final double scoreComplexite;
        public final double scoreSaison;
        public final double scoreDelai;

        public RiskResult(double riskScore,
                          double scoreHistorique, double scoreCharge,
                          double scoreComplexite, double scoreSaison, double scoreDelai,
                          List<String> facteurs, List<String> recommandations) {
            this.riskScore           = Math.min(100, Math.max(0, riskScore));
            this.probabiliteReussite = Math.max(0, 100 - this.riskScore);
            this.scoreHistorique  = scoreHistorique;
            this.scoreCharge      = scoreCharge;
            this.scoreComplexite  = scoreComplexite;
            this.scoreSaison      = scoreSaison;
            this.scoreDelai       = scoreDelai;
            this.facteurs         = facteurs;
            this.recommandations  = recommandations;

            if (this.riskScore < 30) {
                this.niveau  = "FAIBLE";
                this.couleur = "#27ae60";
                this.emoji   = "🟢";
            } else if (this.riskScore < 55) {
                this.niveau  = "MODÉRÉ";
                this.couleur = "#f39c12";
                this.emoji   = "🟡";
            } else if (this.riskScore < 75) {
                this.niveau  = "ÉLEVÉ";
                this.couleur = "#e67e22";
                this.emoji   = "🟠";
            } else {
                this.niveau  = "CRITIQUE";
                this.couleur = "#e74c3c";
                this.emoji   = "🔴";
            }
        }
    }

    // ── Point d'entrée principal ───────────────────────────────────────────

    /**
     * Analyse une tâche et retourne le résultat de prédiction IA.
     *
     * @param tache     La tâche à analyser
     * @param nomEmploye Nom affiché de l'employé (pour les messages)
     */
    public static RiskResult analyser(Tache tache, String nomEmploye) {
        List<String> facteurs       = new ArrayList<>();
        List<String> recommandations = new ArrayList<>();

        // ── 1. Historique retards employé (poids 30%) ──────────────────────
        double scoreHistorique = 50.0; // défaut si pas d'employé
        if (tache.getIdEmploye() != null) {
            scoreHistorique = calculerScoreHistorique(tache.getIdEmploye(), facteurs);
        } else {
            facteurs.add("⚠️ Aucun employé assigné — historique indisponible");
        }

        // ── 2. Charge actuelle (poids 20%) ────────────────────────────────
        double scoreCharge = 50.0;
        if (tache.getIdEmploye() != null) {
            scoreCharge = calculerScoreCharge(tache.getIdEmploye(), tache.getId(), facteurs);
        }

        // ── 3. Complexité tâche (poids 25%) ───────────────────────────────
        double scoreComplexite = calculerScoreComplexite(tache, facteurs);

        // ── 4. Facteur saisonnier (poids 10%) ─────────────────────────────
        double scoreSaison = calculerScoreSaisonnier(tache, facteurs);

        // ── 5. Pression délai (poids 15%) ─────────────────────────────────
        double scoreDelai = calculerScoreDelai(tache, facteurs);

        // ── Calcul final pondéré ──────────────────────────────────────────
        double riskScore =
                (scoreHistorique * 0.30)
                        + (scoreCharge     * 0.20)
                        + (scoreComplexite * 0.25)
                        + (scoreSaison     * 0.10)
                        + (scoreDelai      * 0.15);

        // ── Recommandations intelligentes ─────────────────────────────────
        genererRecommandations(riskScore, scoreHistorique, scoreCharge,
                scoreDelai, scoreComplexite, nomEmploye,
                tache, recommandations);

        return new RiskResult(riskScore,
                scoreHistorique, scoreCharge, scoreComplexite, scoreSaison, scoreDelai,
                facteurs, recommandations);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Facteur 1 : Historique retards employé
    // ─────────────────────────────────────────────────────────────────────────

    private static double calculerScoreHistorique(int idEmploye, List<String> facteurs) {
        try (Connection cnx = MyDatabase.getInstance().getCnx()) {

            // Total tâches terminées par l'employé
            String qTotal = "SELECT COUNT(*) FROM tache WHERE id_employe = ? AND statut IN ('Terminé','Validé','Annulé')";
            int total = 0;
            try (PreparedStatement ps = cnx.prepareStatement(qTotal)) {
                ps.setInt(1, idEmploye);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) total = rs.getInt(1);
                }
            }

            if (total == 0) {
                facteurs.add("📊 Historique : aucune tâche passée — données insuffisantes");
                return 40.0; // neutre légèrement optimiste
            }

            // Tâches terminées EN RETARD (date_fin dépassée au moment de la clôture)
            String qRetards = "SELECT COUNT(*) FROM tache " +
                    "WHERE id_employe = ? AND statut IN ('Terminé','Validé') " +
                    "AND date_modification > date_fin AND date_fin IS NOT NULL";
            int retards = 0;
            try (PreparedStatement ps = cnx.prepareStatement(qRetards)) {
                ps.setInt(1, idEmploye);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) retards = rs.getInt(1);
                }
            }

            double tauxRetard = (double) retards / total * 100;

            if (tauxRetard == 0) {
                facteurs.add("✅ Historique : 0% de retards sur " + total + " tâche(s) — excellent");
            } else if (tauxRetard < 20) {
                facteurs.add("📊 Historique : " + String.format("%.0f", tauxRetard) + "% de retards (" + retards + "/" + total + " tâches)");
            } else if (tauxRetard < 50) {
                facteurs.add("⚠️ Historique : " + String.format("%.0f", tauxRetard) + "% de retards (" + retards + "/" + total + " tâches)");
            } else {
                facteurs.add("🔴 Historique : " + String.format("%.0f", tauxRetard) + "% de retards (" + retards + "/" + total + " tâches) — préoccupant");
            }

            return tauxRetard; // 0–100 directement

        } catch (SQLException e) {
            facteurs.add("⚠️ Historique : données indisponibles (" + e.getMessage() + ")");
            return 50.0;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Facteur 2 : Charge de travail actuelle
    // ─────────────────────────────────────────────────────────────────────────

    private static double calculerScoreCharge(int idEmploye, int idTacheActuelle, List<String> facteurs) {
        try (Connection cnx = MyDatabase.getInstance().getCnx()) {

            // Tâches actives (hors tâche courante et hors clôturées)
            String q = "SELECT COUNT(*) FROM tache " +
                    "WHERE id_employe = ? AND id_tache != ? " +
                    "AND statut NOT IN ('Terminé','Validé','Annulé')";
            int tachesActives = 0;
            try (PreparedStatement ps = cnx.prepareStatement(q)) {
                ps.setInt(1, idEmploye);
                ps.setInt(2, idTacheActuelle);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) tachesActives = rs.getInt(1);
                }
            }

            double score;
            String label;
            if (tachesActives == 0) {
                score = 10.0; label = "Charge : aucune autre tâche active — disponible ✅";
            } else if (tachesActives <= 2) {
                score = 25.0; label = "Charge : " + tachesActives + " tâche(s) active(s) — charge légère";
            } else if (tachesActives <= 4) {
                score = 55.0; label = "⚠️ Charge : " + tachesActives + " tâches actives — charge modérée";
            } else if (tachesActives <= 6) {
                score = 75.0; label = "🔶 Charge : " + tachesActives + " tâches actives — charge élevée";
            } else {
                score = 92.0; label = "🔴 Charge : " + tachesActives + " tâches actives — surcharge critique";
            }

            facteurs.add(label);
            return score;

        } catch (SQLException e) {
            facteurs.add("⚠️ Charge : données indisponibles");
            return 50.0;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Facteur 3 : Complexité de la tâche
    // ─────────────────────────────────────────────────────────────────────────

    private static double calculerScoreComplexite(Tache tache, List<String> facteurs) {
        double score = 30.0; // base

        // Sous-score priorité
        if (tache.getPriorite() != null) {
            switch (tache.getPriorite()) {
                case 1 -> { score += 0;  facteurs.add("📌 Priorité : Basse — impact faible"); }
                case 2 -> { score += 15; facteurs.add("📌 Priorité : Moyenne"); }
                case 3 -> { score += 35; facteurs.add("⚠️ Priorité : Haute — complexité accrue"); }
                case 4 -> { score += 55; facteurs.add("🔴 Priorité : Critique — risque maximal"); }
            }
        } else {
            score += 20;
            facteurs.add("📌 Priorité : non définie");
        }

        // Sous-score catégorie (certaines catégories agricoles sont plus risquées)
        if (tache.getCategorie() != null) {
            score += switch (tache.getCategorie()) {
                case "Récolte"        -> { facteurs.add("🌾 Catégorie : Récolte — dépendance météo forte"); yield 20; }
                case "Plantation"     -> { facteurs.add("🌱 Catégorie : Plantation — timing critique"); yield 15; }
                case "Irrigation"     -> { facteurs.add("💧 Catégorie : Irrigation — technique modérée"); yield 5; }
                case "Fertilisation"  -> { facteurs.add("🧪 Catégorie : Fertilisation — précision requise"); yield 10; }
                case "Maintenance"    -> { facteurs.add("🔧 Catégorie : Maintenance — risque standard"); yield 8; }
                default               -> { facteurs.add("📂 Catégorie : " + tache.getCategorie()); yield 5; }
            };
        }

        return Math.min(100, score);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Facteur 4 : Saison agricole
    // ─────────────────────────────────────────────────────────────────────────

    private static double calculerScoreSaisonnier(Tache tache, List<String> facteurs) {
        LocalDate ref = tache.getDateDebut() != null ? tache.getDateDebut() : LocalDate.now();
        Month mois = ref.getMonth();

        // En Tunisie : périodes de forte activité = risque accru
        return switch (mois) {
            case JUNE, JULY, AUGUST -> {
                facteurs.add("☀️ Saison : été — pic de chaleur, risque agricole élevé");
                yield 80.0;
            }
            case MARCH, APRIL, MAY -> {
                facteurs.add("🌸 Saison : printemps — période de plantation intense");
                yield 65.0;
            }
            case SEPTEMBER, OCTOBER -> {
                facteurs.add("🍂 Saison : automne — période de récolte active");
                yield 60.0;
            }
            case NOVEMBER, DECEMBER -> {
                facteurs.add("🌧️ Saison : début hiver — activité réduite");
                yield 30.0;
            }
            default -> {
                facteurs.add("❄️ Saison : hiver — activité minimale");
                yield 20.0;
            }
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Facteur 5 : Pression du délai
    // ─────────────────────────────────────────────────────────────────────────

    private static double calculerScoreDelai(Tache tache, List<String> facteurs) {
        if (tache.getDateFin() == null) {
            facteurs.add("📅 Délai : aucune date de fin définie — risque neutre");
            return 50.0;
        }

        LocalDate debut = tache.getDateDebut() != null ? tache.getDateDebut() : LocalDate.now();
        LocalDate fin   = tache.getDateFin();
        LocalDate now   = LocalDate.now();

        long dureeTotal = ChronoUnit.DAYS.between(debut, fin);
        long joursRestants = ChronoUnit.DAYS.between(now, fin);

        if (joursRestants < 0) {
            facteurs.add("🔴 Délai : deadline dépassée de " + Math.abs(joursRestants) + " jour(s) !");
            return 100.0;
        }

        if (dureeTotal <= 0) dureeTotal = 1;

        double avancement = 1.0 - ((double) joursRestants / dureeTotal);
        double score;

        if (joursRestants == 0) {
            score = 95.0;
            facteurs.add("🔴 Délai : deadline AUJOURD'HUI !");
        } else if (joursRestants <= 2) {
            score = 85.0;
            facteurs.add("🔶 Délai : seulement " + joursRestants + " jour(s) restant(s) — urgent");
        } else if (joursRestants <= 7) {
            score = 65.0;
            facteurs.add("⚠️ Délai : " + joursRestants + " jours restants (semaine courante)");
        } else if (dureeTotal <= 3) {
            score = 60.0;
            facteurs.add("⚠️ Délai : tâche courte (" + dureeTotal + " jours total)");
        } else if (avancement > 0.75) {
            score = 55.0;
            facteurs.add("⚠️ Délai : " + joursRestants + "j restants / " + dureeTotal + "j total — phase finale");
        } else {
            score = 20.0;
            facteurs.add("✅ Délai : " + joursRestants + " jours restants — confortable");
        }

        return score;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Génération des recommandations
    // ─────────────────────────────────────────────────────────────────────────

    private static void genererRecommandations(
            double riskScore, double scoreHistorique, double scoreCharge,
            double scoreDelai, double scoreComplexite, String nomEmploye,
            Tache tache, List<String> recommandations) {

        if (riskScore < 30) {
            recommandations.add("✅ Tâche bien planifiée — aucune action urgente requise.");
            recommandations.add("📋 Maintenir le suivi hebdomadaire habituel.");
            return;
        }

        if (scoreHistorique > 60) {
            recommandations.add("📌 Planifier un point de suivi quotidien avec " + nomEmploye + ".");
            recommandations.add("🔄 Envisager de réaffecter partiellement la tâche à un autre employé.");
        }

        if (scoreCharge > 60) {
            recommandations.add("⚖️ Alléger la charge de " + nomEmploye +
                    " — suspendre ou déléguer une tâche moins prioritaire.");
        }

        if (scoreDelai > 70) {
            recommandations.add("📅 Prolonger la date limite ou mobiliser des ressources supplémentaires.");
            if (tache.getDateFin() != null) {
                LocalDate nouvelleDate = tache.getDateFin().plusDays(3);
                recommandations.add("💡 Suggestion : repousser la deadline au " + nouvelleDate + " (+3 jours).");
            }
        }

        if (scoreComplexite > 70) {
            recommandations.add("🧩 Décomposer cette tâche complexe en sous-tâches plus petites.");
        }

        if (riskScore >= 75) {
            recommandations.add("🚨 ALERTE : Risque critique — intervention immédiate recommandée.");
            recommandations.add("📞 Contacter directement l'employé pour évaluer la situation.");
        }

        if (recommandations.isEmpty()) {
            recommandations.add("👁️ Surveiller l'avancement de cette tâche de près.");
            recommandations.add("📊 Vérifier l'état dans 2–3 jours.");
        }
    }
}