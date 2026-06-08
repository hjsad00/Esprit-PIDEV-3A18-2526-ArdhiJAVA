package tn.neuron.ardhi.services.gestionemployeservice;

import tn.neuron.ardhi.models.gestionemployemodel.Competence;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;
import tn.neuron.ardhi.utils.gestionemployeutils.DatabaseInitializer;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service de gestion des compétences
 */
public class CompetenceService {

    private Connection getCnx() {
        return MyDatabase.getInstance().getCnx();
    }

    // ══════════════════════════════════════════════════════════════════════
    // CRUD COMPETENCES
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Récupérer toutes les compétences
     */
    public List<Competence> getAllCompetences() {
        List<Competence> competences = new ArrayList<>();
        String query = "SELECT * FROM competence ORDER BY categorie, nom";

        try (Statement stmt = getCnx().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                competences.add(mapResultSetToCompetence(rs));
            }
            System.out.println("✅ Compétences récupérées: " + competences.size());

        } catch (SQLException e) {
            System.err.println("❌ Erreur getAllCompetences: " + e.getMessage());
            e.printStackTrace();
        }
        return competences;
    }

    /**
     * Récupérer les compétences par catégorie
     */
    public List<Competence> getCompetencesByCategorie(String categorie) {
        List<Competence> competences = new ArrayList<>();
        String query = "SELECT * FROM competence WHERE categorie = ? ORDER BY nom";

        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setString(1, categorie);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    competences.add(mapResultSetToCompetence(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getCompetencesByCategorie: " + e.getMessage());
            e.printStackTrace();
        }
        return competences;
    }

    /**
     * Ajouter une compétence
     */
    public boolean addCompetence(Competence competence) {
        String query = "INSERT INTO competence (nom, categorie, description) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = getCnx().prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, competence.getNom());
            stmt.setString(2, competence.getCategorie());
            stmt.setString(3, competence.getDescription());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        competence.setIdCompetence(keys.getInt(1));
                    }
                }
                System.out.println("✅ Compétence ajoutée: " + competence.getNom());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur addCompetence: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ══════════════════════════════════════════════════════════════════════
    // COMPETENCES EMPLOYE
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Assigner une compétence à un employé
     */
    public boolean assignCompetenceToEmploye(int idEmploye, int idCompetence, int niveau, double anneesExperience) {
        String query = "INSERT INTO employe_competence (id_employe, id_competence, niveau, annees_experience) " +
                "VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE niveau = ?, annees_experience = ?";

        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idEmploye);
            stmt.setInt(2, idCompetence);
            stmt.setInt(3, niveau);
            stmt.setDouble(4, anneesExperience);
            stmt.setInt(5, niveau);
            stmt.setDouble(6, anneesExperience);

            int rows = stmt.executeUpdate();
            System.out.println("✅ Compétence assignée à l'employé " + idEmploye);
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur assignCompetenceToEmploye: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Récupérer les compétences d'un employé
     */
    public Map<Competence, CompetenceDetails> getEmployeCompetences(int idEmploye) {
        Map<Competence, CompetenceDetails> competences = new HashMap<>();
        String query = "SELECT c.*, ec.niveau, ec.annees_experience, ec.certification, ec.derniere_utilisation " +
                "FROM competence c " +
                "INNER JOIN employe_competence ec ON c.id_competence = ec.id_competence " +
                "WHERE ec.id_employe = ? " +
                "ORDER BY ec.niveau DESC, c.categorie, c.nom";

        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idEmploye);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Competence comp = mapResultSetToCompetence(rs);
                    CompetenceDetails details = new CompetenceDetails(
                            rs.getInt("niveau"),
                            rs.getDouble("annees_experience"),
                            rs.getString("certification"),
                            rs.getDate("derniere_utilisation") != null ?
                                    rs.getDate("derniere_utilisation").toLocalDate() : null
                    );
                    competences.put(comp, details);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getEmployeCompetences: " + e.getMessage());
            e.printStackTrace();
        }
        return competences;
    }

    /**
     * Supprimer une compétence d'un employé
     */
    public boolean removeCompetenceFromEmploye(int idEmploye, int idCompetence) {
        String query = "DELETE FROM employe_competence WHERE id_employe = ? AND id_competence = ?";

        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idEmploye);
            stmt.setInt(2, idCompetence);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur removeCompetenceFromEmploye: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ══════════════════════════════════════════════════════════════════════
    // COMPETENCES TACHE
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Ajouter une compétence requise pour une tâche
     */
    public boolean addCompetenceRequiseToTache(int idTache, int idCompetence, int niveauRequis, int importance) {
        String query = "INSERT INTO tache_competence_requise (id_tache, id_competence, niveau_requis, importance) " +
                "VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE niveau_requis = ?, importance = ?";

        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idTache);
            stmt.setInt(2, idCompetence);
            stmt.setInt(3, niveauRequis);
            stmt.setInt(4, importance);
            stmt.setInt(5, niveauRequis);
            stmt.setInt(6, importance);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur addCompetenceRequiseToTache: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Récupérer les compétences requises pour une tâche
     */
    public Map<Competence, CompetenceRequise> getTacheCompetencesRequises(int idTache) {
        Map<Competence, CompetenceRequise> competences = new HashMap<>();
        String query = "SELECT c.*, tcr.niveau_requis, tcr.importance " +
                "FROM competence c " +
                "INNER JOIN tache_competence_requise tcr ON c.id_competence = tcr.id_competence " +
                "WHERE tcr.id_tache = ? " +
                "ORDER BY tcr.importance DESC, c.nom";

        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idTache);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Competence comp = mapResultSetToCompetence(rs);
                    CompetenceRequise details = new CompetenceRequise(
                            rs.getInt("niveau_requis"),
                            rs.getInt("importance")
                    );
                    competences.put(comp, details);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getTacheCompetencesRequises: " + e.getMessage());
            e.printStackTrace();
        }
        return competences;
    }

    // ══════════════════════════════════════════════════════════════════════
    // UTILITAIRES
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Obtenir le label du niveau de compétence
     */
    public static String getNiveauLabel(int niveau) {
        switch (niveau) {
            case 1: return "Débutant";
            case 2: return "Intermédiaire";
            case 3: return "Avancé";
            case 4: return "Expert";
            case 5: return "Master";
            default: return "Non défini";
        }
    }

    /**
     * Obtenir la couleur associée au niveau
     */
    public static String getNiveauColor(int niveau) {
        switch (niveau) {
            case 1: return "#95a5a6"; // Gris
            case 2: return "#3498db"; // Bleu
            case 3: return "#9b59b6"; // Violet
            case 4: return "#e67e22"; // Orange
            case 5: return "#e74c3c"; // Rouge
            default: return "#bdc3c7";
        }
    }

    private Competence mapResultSetToCompetence(ResultSet rs) throws SQLException {
        Competence comp = new Competence();
        comp.setIdCompetence(rs.getInt("id_competence"));
        comp.setNom(rs.getString("nom"));
        comp.setCategorie(rs.getString("categorie"));
        comp.setDescription(rs.getString("description"));

        Timestamp ts = rs.getTimestamp("date_creation");
        if (ts != null) {
            comp.setDateCreation(ts.toLocalDateTime());
        }

        return comp;
    }

    // ══════════════════════════════════════════════════════════════════════
    // CLASSES INTERNES
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Détails d'une compétence d'employé
     */
    public static class CompetenceDetails {
        public int niveau;
        public double anneesExperience;
        public String certification;
        public java.time.LocalDate derniereUtilisation;

        public CompetenceDetails(int niveau, double anneesExperience, String certification,
                                 java.time.LocalDate derniereUtilisation) {
            this.niveau = niveau;
            this.anneesExperience = anneesExperience;
            this.certification = certification;
            this.derniereUtilisation = derniereUtilisation;
        }

        public String getNiveauLabel() {
            return CompetenceService.getNiveauLabel(niveau);
        }

        public String getNiveauColor() {
            return CompetenceService.getNiveauColor(niveau);
        }
    }

    /**
     * Détails d'une compétence requise pour une tâche
     */
    public static class CompetenceRequise {
        public int niveauRequis;
        public int importance; // 1-10

        public CompetenceRequise(int niveauRequis, int importance) {
            this.niveauRequis = niveauRequis;
            this.importance = importance;
        }

        public String getNiveauLabel() {
            return CompetenceService.getNiveauLabel(niveauRequis);
        }

        public String getImportanceLabel() {
            if (importance >= 8) return "Critique";
            if (importance >= 6) return "Important";
            if (importance >= 4) return "Utile";
            return "Optionnel";
        }
    }
}