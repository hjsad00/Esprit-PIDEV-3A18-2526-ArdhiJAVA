package tn.neuron.ardhi.services.gestionemployeservice;

import tn.neuron.ardhi.models.gestionemployemodel.EvaluationPerformance;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;
import tn.neuron.ardhi.utils.gestionemployeutils.DatabaseInitializer;


import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de gestion des évaluations de performance
 */
public class EvaluationService {

    private Connection getCnx() {
        return MyDatabase.getInstance().getCnx();
    }

    // ══════════════════════════════════════════════════════════════════════
    // CRUD EVALUATIONS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Ajouter une évaluation
     */
    public boolean ajouterEvaluation(EvaluationPerformance evaluation) {
        String query = "INSERT INTO evaluation_performance " +
                "(id_employe, id_tache, note_qualite, note_rapidite, note_autonomie, " +
                "note_communication, commentaire, evaluateur) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = getCnx().prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, evaluation.getIdEmploye());
            if (evaluation.getIdTache() != null) {
                stmt.setInt(2, evaluation.getIdTache());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.setInt(3, evaluation.getNoteQualite());
            stmt.setInt(4, evaluation.getNoteRapidite());
            stmt.setInt(5, evaluation.getNoteAutonomie());
            stmt.setInt(6, evaluation.getNoteCommunication());
            stmt.setString(7, evaluation.getCommentaire());
            stmt.setString(8, evaluation.getEvaluateur());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        evaluation.setIdEvaluation(keys.getInt(1));
                    }
                }
                System.out.println("✅ Évaluation ajoutée pour l'employé " + evaluation.getIdEmploye());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur ajouterEvaluation: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Récupérer toutes les évaluations d'un employé
     */
    public List<EvaluationPerformance> getEvaluationsByEmploye(int idEmploye) {
        List<EvaluationPerformance> evaluations = new ArrayList<>();
        String query = "SELECT * FROM evaluation_performance WHERE id_employe = ? ORDER BY date_evaluation DESC";

        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idEmploye);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    evaluations.add(mapResultSetToEvaluation(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getEvaluationsByEmploye: " + e.getMessage());
            e.printStackTrace();
        }
        return evaluations;
    }

    /**
     * Récupérer l'évaluation d'une tâche spécifique
     */
    public EvaluationPerformance getEvaluationByTache(int idTache) {
        String query = "SELECT * FROM evaluation_performance WHERE id_tache = ?";

        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idTache);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEvaluation(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getEvaluationByTache: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Calculer la note moyenne d'un employé
     */
    public double getNoteMoyenne(int idEmploye) {
        String query = "SELECT AVG(note_globale) as moyenne FROM evaluation_performance WHERE id_employe = ?";

        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idEmploye);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("moyenne");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getNoteMoyenne: " + e.getMessage());
        }
        return 0.0;
    }

    /**
     * Obtenir le nombre total d'évaluations d'un employé
     */
    public int getNombreEvaluations(int idEmploye) {
        String query = "SELECT COUNT(*) as total FROM evaluation_performance WHERE id_employe = ?";

        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idEmploye);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getNombreEvaluations: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Obtenir les statistiques d'évaluation détaillées
     */
    public EvaluationStats getStats(int idEmploye) {
        String query = "SELECT " +
                "AVG(note_qualite) as moy_qualite, " +
                "AVG(note_rapidite) as moy_rapidite, " +
                "AVG(note_autonomie) as moy_autonomie, " +
                "AVG(note_communication) as moy_communication, " +
                "AVG(note_globale) as moy_globale, " +
                "COUNT(*) as total " +
                "FROM evaluation_performance WHERE id_employe = ?";

        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idEmploye);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new EvaluationStats(
                            rs.getDouble("moy_qualite"),
                            rs.getDouble("moy_rapidite"),
                            rs.getDouble("moy_autonomie"),
                            rs.getDouble("moy_communication"),
                            rs.getDouble("moy_globale"),
                            rs.getInt("total")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getStats: " + e.getMessage());
        }
        return new EvaluationStats(0, 0, 0, 0, 0, 0);
    }

    /**
     * Supprimer une évaluation
     */
    public boolean supprimerEvaluation(int idEvaluation) {
        String query = "DELETE FROM evaluation_performance WHERE id_evaluation = ?";

        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idEvaluation);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur supprimerEvaluation: " + e.getMessage());
        }
        return false;
    }

    // ══════════════════════════════════════════════════════════════════════
    // AUTO-EVALUATION BASEE SUR LES TACHES
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Générer une évaluation automatique basée sur la tâche terminée
     * Appelé quand une tâche passe à "Terminé"
     */
    public boolean genererEvaluationAuto(int idEmploye, int idTache) {
        // Vérifier si une évaluation existe déjà
        if (getEvaluationByTache(idTache) != null) {
            return false; // Déjà évaluée
        }

        // Générer des notes basées sur l'historique
        EvaluationPerformance eval = new EvaluationPerformance();
        eval.setIdEmploye(idEmploye);
        eval.setIdTache(idTache);

        // Notes par défaut basées sur la moyenne historique
        double moyenneHistorique = getNoteMoyenne(idEmploye);
        int noteBase = moyenneHistorique > 0 ? (int) Math.round(moyenneHistorique) : 3;

        eval.setNoteQualite(noteBase);
        eval.setNoteRapidite(noteBase);
        eval.setNoteAutonomie(noteBase);
        eval.setNoteCommunication(noteBase);
        eval.setCommentaire("Évaluation automatique basée sur l'historique");
        eval.setEvaluateur("Système AI");

        return ajouterEvaluation(eval);
    }

    // ══════════════════════════════════════════════════════════════════════
    // UTILITAIRES
    // ══════════════════════════════════════════════════════════════════════

    private EvaluationPerformance mapResultSetToEvaluation(ResultSet rs) throws SQLException {
        EvaluationPerformance eval = new EvaluationPerformance();
        eval.setIdEvaluation(rs.getInt("id_evaluation"));
        eval.setIdEmploye(rs.getInt("id_employe"));

        int idTache = rs.getInt("id_tache");
        if (!rs.wasNull()) {
            eval.setIdTache(idTache);
        }

        eval.setNoteQualite(rs.getInt("note_qualite"));
        eval.setNoteRapidite(rs.getInt("note_rapidite"));
        eval.setNoteAutonomie(rs.getInt("note_autonomie"));
        eval.setNoteCommunication(rs.getInt("note_communication"));
        eval.setNoteGlobale(rs.getDouble("note_globale"));
        eval.setCommentaire(rs.getString("commentaire"));
        eval.setEvaluateur(rs.getString("evaluateur"));

        Timestamp ts = rs.getTimestamp("date_evaluation");
        if (ts != null) {
            eval.setDateEvaluation(ts.toLocalDateTime());
        }

        return eval;
    }

    // ══════════════════════════════════════════════════════════════════════
    // CLASSE INTERNE : STATISTIQUES
    // ══════════════════════════════════════════════════════════════════════

    public static class EvaluationStats {
        public double moyenneQualite;
        public double moyenneRapidite;
        public double moyenneAutonomie;
        public double moyenneCommunication;
        public double moyenneGlobale;
        public int nombreEvaluations;

        public EvaluationStats(double qualite, double rapidite, double autonomie,
                               double communication, double globale, int nombre) {
            this.moyenneQualite = qualite;
            this.moyenneRapidite = rapidite;
            this.moyenneAutonomie = autonomie;
            this.moyenneCommunication = communication;
            this.moyenneGlobale = globale;
            this.nombreEvaluations = nombre;
        }

        public String getAppreciation() {
            if (moyenneGlobale >= 4.5) return "Excellent";
            if (moyenneGlobale >= 4.0) return "Très bien";
            if (moyenneGlobale >= 3.5) return "Bien";
            if (moyenneGlobale >= 3.0) return "Satisfaisant";
            if (moyenneGlobale >= 2.5) return "Moyen";
            return "Insuffisant";
        }

        public String getPointFort() {
            double max = Math.max(Math.max(moyenneQualite, moyenneRapidite),
                    Math.max(moyenneAutonomie, moyenneCommunication));

            if (max == moyenneQualite) return "Qualité du travail";
            if (max == moyenneRapidite) return "Rapidité d'exécution";
            if (max == moyenneAutonomie) return "Autonomie";
            return "Communication";
        }

        public String getPointAmeliorer() {
            double min = Math.min(Math.min(moyenneQualite, moyenneRapidite),
                    Math.min(moyenneAutonomie, moyenneCommunication));

            if (min == moyenneQualite) return "Qualité du travail";
            if (min == moyenneRapidite) return "Rapidité d'exécution";
            if (min == moyenneAutonomie) return "Autonomie";
            return "Communication";
        }
    }
}