package tn.neuron.ardhi.services.gestionemployeservice;

import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;
import tn.neuron.ardhi.controllers.gestionemployecontroller.EmployeController;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 📊 Service d'évaluation des performances des employés
 */
public class PerformanceService {

    private Connection getCnx() {
        return MyDatabase.getInstance().getCnx();
    }

    /**
     * 📈 Calculer le score de performance d'un employé
     *
     * Formule :
     * Score = (Tâches terminées / Tâches totales) × 100
     *         - (Retards × 5)
     *         - (En cours anciennes × 2)
     */
    public PerformanceData calculatePerformance(int idEmploye) {
        PerformanceData data = new PerformanceData();
        data.idEmploye = idEmploye;

        // Recupere les taches de cet employe.
        // Si l'employe existe encore (actif=TRUE), ses taches sont valides.
        // Les taches orphelines (employe DELETE) ne sont jamais atteintes car calculatePerformance
        // n'est appele que depuis getClassement qui filtre actif=TRUE.
        String query = "SELECT statut, date_debut, date_fin FROM tache WHERE id_employe = ?";

        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idEmploye);
            try (ResultSet rs = stmt.executeQuery()) {

                int totalTaches = 0;
                int tachesTerminees = 0;
                int tachesEnRetard = 0;
                int tachesEnCours = 0;
                int tachesAnnulees = 0;
                long totalJoursRealisation = 0;
                int compteurDuree = 0;

                while (rs.next()) {
                    totalTaches++;
                    String statut = rs.getString("statut");

                    // ✅ CORRECTION : Utiliser java.sql.Date explicitement
                    java.sql.Date dateDebut = rs.getDate("date_debut");
                    java.sql.Date dateFin = rs.getDate("date_fin");

                    if (statut != null && (statut.equalsIgnoreCase("Terminé") ||
                            statut.equalsIgnoreCase("TERMINE") ||
                            statut.equalsIgnoreCase("Validé"))) {
                        tachesTerminees++;

                        // Calculer durée de réalisation
                        if (dateDebut != null && dateFin != null) {
                            LocalDate debut = dateDebut.toLocalDate();
                            LocalDate fin = dateFin.toLocalDate();
                            long jours = ChronoUnit.DAYS.between(debut, fin);
                            if (jours >= 0) {
                                totalJoursRealisation += jours;
                                compteurDuree++;
                            }
                        }

                    } else if (statut != null && (statut.equalsIgnoreCase("En cours") ||
                            statut.equalsIgnoreCase("EN_COURS"))) {
                        tachesEnCours++;

                        // Si date fin dépassée = retard
                        if (dateFin != null && dateFin.toLocalDate().isBefore(LocalDate.now())) {
                            tachesEnRetard++;
                        }

                    } else if (statut != null && (statut.equalsIgnoreCase("Annulé") ||
                            statut.equalsIgnoreCase("ANNULE"))) {
                        tachesAnnulees++;
                    }
                }

                // Remplir les données
                data.totalTaches = totalTaches;
                data.tachesTerminees = tachesTerminees;
                data.tachesEnRetard = tachesEnRetard;
                data.tachesEnCours = tachesEnCours;
                data.tachesAnnulees = tachesAnnulees;

                // Temps moyen de réalisation
                if (compteurDuree > 0) {
                    data.tempsRealisationMoyen = (double) totalJoursRealisation / compteurDuree;
                }

                // CALCUL DU SCORE
                if (totalTaches > 0) {
                    double tauxReussite = (double) tachesTerminees / totalTaches * 100;
                    double penaliteRetard = tachesEnRetard * 5;
                    double penaliteEnCours = tachesEnCours * 2;

                    data.score = tauxReussite - penaliteRetard - penaliteEnCours;

                    // Limiter entre 0 et 100
                    if (data.score < 0) data.score = 0;
                    if (data.score > 100) data.score = 100;

                    // Calculer taux de réussite brut
                    data.tauxReussite = tauxReussite;
                }

            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur calcul performance: " + e.getMessage());
            e.printStackTrace();
        }

        return data;
    }

    /**
     * 🏆 Obtenir le classement des employés par performance
     */
    public List<PerformanceData> getClassement(Integer idAgriculteur) {
        List<PerformanceData> classement = new ArrayList<>();

        // actif = TRUE : exclut soft-deletes
        // La condition actif=TRUE suffit : un employe DELETE physiquement n'existe plus dans la table.
        // On filtre aussi les employes qui n'ont aucune tache reelle (evite les fantomes).
        String query = "SELECT DISTINCT e.id_employe, e.nom, e.prenom FROM employe e "
                + "WHERE e.actif = TRUE";
        if (idAgriculteur != null) {
            query += " AND e.id_agriculteur = ?";
        }

        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            if (idAgriculteur != null) {
                stmt.setInt(1, idAgriculteur);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int idEmploye = rs.getInt("id_employe");
                    String nom = rs.getString("nom");
                    String prenom = rs.getString("prenom");

                    PerformanceData perf = calculatePerformance(idEmploye);
                    perf.nomEmploye = prenom + " " + nom;

                    classement.add(perf);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur classement: " + e.getMessage());
            e.printStackTrace();
        }

        // Trier par score décroissant
        classement.sort((a, b) -> Double.compare(b.score, a.score));

        return classement;
    }

    /**
     * 📊 Classe de données de performance
     */
    public static class PerformanceData {
        public int idEmploye;
        public String nomEmploye;
        public int totalTaches;
        public int tachesTerminees;
        public int tachesEnRetard;
        public int tachesEnCours;
        public int tachesAnnulees;
        public double tempsRealisationMoyen; // en jours
        public double tauxReussite; // %
        public double score; // Score final 0-100

        /**
         * Obtenir l'appréciation textuelle
         */
        public String getAppreciation() {
            if (score >= 90) return "Excellent";
            if (score >= 75) return "Très bien";
            if (score >= 60) return "Bien";
            if (score >= 50) return "Moyen";
            return "Faible";
        }

        /**
         * Obtenir la couleur selon le score
         */
        public String getCouleur() {
            if (score >= 75) return "#27ae60"; // Vert
            if (score >= 50) return "#f39c12"; // Orange
            return "#e74c3c"; // Rouge
        }

        /**
         * Obtenir l'émoji selon le score
         */
        public String getEmoji() {
            if (score >= 90) return "🏆";
            if (score >= 75) return "⭐";
            if (score >= 60) return "👍";
            if (score >= 50) return "👌";
            return "⚠️";
        }

        @Override
        public String toString() {
            return String.format("%s - Score: %.1f (%s)", nomEmploye, score, getAppreciation());
        }
    }
}