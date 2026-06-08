package tn.neuron.ardhi.services.MaterielEtMaintenance;

import tn.neuron.ardhi.models.MaterielEtMaintenance.Maintenance;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service JDBC pour la gestion des maintenances.
 *
 * CORRECTION PRINCIPALE :
 *  - getCnx() appelé à chaque méthode (plus de connexion stockée dans un champ)
 *  - Évite les erreurs "Communications link failure" après timeout MySQL
 */
public class MaintenanceService {

    // ══ Pas de champ connection stocké — connexion fraîche à chaque appel ══
    private Connection getCnx() {
        return MyDatabase.getInstance().getCnx();
    }

    // ════════════════════════════════════════════════════════
    //  CRÉER UNE MAINTENANCE
    // ════════════════════════════════════════════════════════

    public boolean planifierMaintenance(Maintenance maintenance) {
        String sql = "INSERT INTO maintenance (materiel_id, description, date_planifiee, " +
                "google_calendar_event_id, statut_maintenance, type_maintenance, cout) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = getCnx().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, maintenance.getMateriel_id());
            stmt.setString(2, maintenance.getDescription());
            stmt.setDate(3, Date.valueOf(maintenance.getDate_planifiee()));
            stmt.setString(4, maintenance.getGoogle_calendar_event_id());
            stmt.setString(5, "planifiee");
            stmt.setString(6, maintenance.getType_maintenance());
            stmt.setDouble(7, maintenance.getCout());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    maintenance.setId_maintenance(generatedKeys.getInt(1));
                }
                System.out.println("✅ Maintenance planifiée : ID=" + maintenance.getId_maintenance());
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("❌ Erreur planifierMaintenance : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ════════════════════════════════════════════════════════
    //  RÉCUPÉRER
    // ════════════════════════════════════════════════════════

    /**
     * Toutes les maintenances d'un utilisateur (tous ses matériels).
     */
    public List<Maintenance> getMaintenancesByUserId(int userId) {
        List<Maintenance> maintenances = new ArrayList<>();
        String sql = "SELECT m.*, mat.nom as materiel_nom " +
                "FROM maintenance m " +
                "INNER JOIN materiel mat ON m.materiel_id = mat.id_materiel " +
                "WHERE mat.user_id = ? " +
                "ORDER BY m.date_planifiee DESC, m.date_maintenance DESC";

        try (PreparedStatement stmt = getCnx().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) maintenances.add(extractMaintenanceFromResultSet(rs));
            System.out.println("✅ " + maintenances.size() + " maintenances pour user_id=" + userId);
        } catch (SQLException e) {
            System.err.println("❌ Erreur getMaintenancesByUserId : " + e.getMessage());
            e.printStackTrace();
        }
        return maintenances;
    }

    /**
     * Toutes les maintenances d'un matériel précis.
     */
    public List<Maintenance> getMaintenancesByMaterielId(int materielId) {
        List<Maintenance> maintenances = new ArrayList<>();
        String sql = "SELECT m.*, mat.nom as materiel_nom " +
                "FROM maintenance m " +
                "INNER JOIN materiel mat ON m.materiel_id = mat.id_materiel " +
                "WHERE m.materiel_id = ? " +
                "ORDER BY m.date_maintenance DESC";

        try (PreparedStatement stmt = getCnx().prepareStatement(sql)) {
            stmt.setInt(1, materielId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) maintenances.add(extractMaintenanceFromResultSet(rs));
        } catch (SQLException e) {
            System.err.println("❌ Erreur getMaintenancesByMaterielId : " + e.getMessage());
            e.printStackTrace();
        }
        return maintenances;
    }

    /**
     * ⭐ Maintenances PLANIFIÉES (à venir) d'un utilisateur.
     * Appelée par NotificationScheduler pour créer les alertes.
     *
     * IMPORTANT : le statut en base est 'planifiee' (minuscules, sans accent).
     */
    public List<Maintenance> getMaintenancesPlanifiees(int userId) {
        List<Maintenance> maintenances = new ArrayList<>();
        String sql = "SELECT m.*, mat.nom as materiel_nom " +
                "FROM maintenance m " +
                "INNER JOIN materiel mat ON m.materiel_id = mat.id_materiel " +
                "WHERE mat.user_id = ? " +
                "AND m.statut_maintenance = 'planifiee' " +
                "ORDER BY m.date_planifiee ASC";

        System.out.println("🔍 [MaintenanceService] getMaintenancesPlanifiees(userId=" + userId + ")");

        try (PreparedStatement stmt = getCnx().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) maintenances.add(extractMaintenanceFromResultSet(rs));
            System.out.println("📋 " + maintenances.size() + " maintenance(s) planifiée(s)");
        } catch (SQLException e) {
            System.err.println("❌ Erreur getMaintenancesPlanifiees : " + e.getMessage());
            e.printStackTrace();
        }
        return maintenances;
    }

    /**
     * Maintenances terminées d'un utilisateur.
     */
    public List<Maintenance> getMaintenancesTerminees(int userId) {
        List<Maintenance> maintenances = new ArrayList<>();
        String sql = "SELECT m.*, mat.nom as materiel_nom " +
                "FROM maintenance m " +
                "INNER JOIN materiel mat ON m.materiel_id = mat.id_materiel " +
                "WHERE mat.user_id = ? AND m.statut_maintenance = 'terminee' " +
                "ORDER BY m.date_realisee DESC";

        try (PreparedStatement stmt = getCnx().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) maintenances.add(extractMaintenanceFromResultSet(rs));
        } catch (SQLException e) {
            System.err.println("❌ Erreur getMaintenancesTerminees : " + e.getMessage());
            e.printStackTrace();
        }
        return maintenances;
    }

    // ════════════════════════════════════════════════════════
    //  METTRE À JOUR
    // ════════════════════════════════════════════════════════

    public boolean terminerMaintenance(int maintenanceId, double cout) {
        String sql = "UPDATE maintenance SET statut_maintenance = 'terminee', " +
                "date_realisee = ?, date_maintenance = ?, cout = ? " +
                "WHERE id_maintenance = ?";

        try (PreparedStatement stmt = getCnx().prepareStatement(sql)) {
            LocalDate aujourd = LocalDate.now();
            stmt.setDate(1, Date.valueOf(aujourd));
            stmt.setDate(2, Date.valueOf(aujourd));
            stmt.setDouble(3, cout);
            stmt.setInt(4, maintenanceId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur terminerMaintenance : " + e.getMessage());
            return false;
        }
    }

    public boolean annulerMaintenance(int maintenanceId) {
        String sql = "UPDATE maintenance SET statut_maintenance = 'annulee' WHERE id_maintenance = ?";
        try (PreparedStatement stmt = getCnx().prepareStatement(sql)) {
            stmt.setInt(1, maintenanceId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur annulerMaintenance : " + e.getMessage());
            return false;
        }
    }

    // ════════════════════════════════════════════════════════
    //  SUPPRIMER
    // ════════════════════════════════════════════════════════

    public boolean supprimerMaintenance(int maintenanceId) {
        String sql = "DELETE FROM maintenance WHERE id_maintenance = ?";
        try (PreparedStatement stmt = getCnx().prepareStatement(sql)) {
            stmt.setInt(1, maintenanceId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur supprimerMaintenance : " + e.getMessage());
            return false;
        }
    }

    // ════════════════════════════════════════════════════════
    //  STATISTIQUES
    // ════════════════════════════════════════════════════════

    public int compterMaintenancesEnRetard(int userId) {
        String sql = "SELECT COUNT(*) FROM maintenance m " +
                "INNER JOIN materiel mat ON m.materiel_id = mat.id_materiel " +
                "WHERE mat.user_id = ? " +
                "AND m.statut_maintenance = 'planifiee' " +
                "AND m.date_planifiee < CURDATE()";

        try (PreparedStatement stmt = getCnx().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("❌ Erreur compterMaintenancesEnRetard : " + e.getMessage());
        }
        return 0;
    }

    // ════════════════════════════════════════════════════════
    //  UTILITAIRE PRIVÉ
    // ════════════════════════════════════════════════════════

    private Maintenance extractMaintenanceFromResultSet(ResultSet rs) throws SQLException {
        Maintenance m = new Maintenance();
        m.setId_maintenance(rs.getInt("id_maintenance"));
        m.setMateriel_id(rs.getInt("materiel_id"));
        m.setDescription(rs.getString("description"));

        Date dateMaintenance = rs.getDate("date_maintenance");
        if (dateMaintenance != null) m.setDate_maintenance(dateMaintenance.toLocalDate());

        Date datePlanifiee = rs.getDate("date_planifiee");
        if (datePlanifiee != null) m.setDate_planifiee(datePlanifiee.toLocalDate());

        Date dateRealisee = rs.getDate("date_realisee");
        if (dateRealisee != null) m.setDate_realisee(dateRealisee.toLocalDate());

        m.setCout(rs.getDouble("cout"));
        m.setGoogle_calendar_event_id(rs.getString("google_calendar_event_id"));
        m.setStatut_maintenance(rs.getString("statut_maintenance"));
        m.setType_maintenance(rs.getString("type_maintenance"));

        // materiel_nom peut ne pas être présent dans toutes les requêtes
        try { m.setMateriel_nom(rs.getString("materiel_nom")); }
        catch (SQLException ignored) {}

        return m;
    }
}
