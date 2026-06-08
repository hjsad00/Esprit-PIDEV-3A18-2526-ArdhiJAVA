package tn.neuron.ardhi.services.MaterielEtMaintenance;

import tn.neuron.ardhi.models.MaterielEtMaintenance.Maintenance;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service SIMPLIFIÉ pour l'historique des maintenances
 * Utilise la structure ORIGINALE de la table maintenance
 */
public class HistoriqueMaintenanceService {

    private Connection connection;

    public HistoriqueMaintenanceService() {
        this.connection = MyDatabase.getInstance().getCnx();
    }

    /**
     * Récupérer TOUT l'historique des maintenances d'un utilisateur
     */
    public List<Maintenance> getHistoriqueByUserId(int userId) {
        List<Maintenance> maintenances = new ArrayList<>();

        String sql = "SELECT m.*, mat.nom as materiel_nom " +
                "FROM maintenance m " +
                "INNER JOIN materiel mat ON m.materiel_id = mat.id_materiel " +
                "WHERE mat.user_id = ? " +
                "ORDER BY m.date_maintenance DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Maintenance m = new Maintenance();
                m.setId_maintenance(rs.getInt("id_maintenance"));
                m.setMateriel_id(rs.getInt("materiel_id"));
                m.setDescription(rs.getString("description"));
                m.setCout(rs.getDouble("cout"));

                Date dateMaintenance = rs.getDate("date_maintenance");
                if (dateMaintenance != null) {
                    m.setDate_maintenance(dateMaintenance.toLocalDate());
                }

                // Nom du matériel
                try {
                    m.setMateriel_nom(rs.getString("materiel_nom"));
                } catch (SQLException e) {
                    // Colonne absente
                }

                maintenances.add(m);
            }

            System.out.println("✅ " + maintenances.size() + " maintenances trouvées");

        } catch (SQLException e) {
            System.err.println("❌ Erreur historique : " + e.getMessage());
            e.printStackTrace();
        }

        return maintenances;
    }

    /**
     * Compter le nombre total de maintenances
     */
    public int compterMaintenances(int userId) {
        String sql = "SELECT COUNT(*) FROM maintenance m " +
                "INNER JOIN materiel mat ON m.materiel_id = mat.id_materiel " +
                "WHERE mat.user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur comptage : " + e.getMessage());
        }

        return 0;
    }

    /**
     * Calculer le coût total des maintenances
     */
    public double calculerCoutTotal(int userId) {
        String sql = "SELECT SUM(cout) FROM maintenance m " +
                "INNER JOIN materiel mat ON m.materiel_id = mat.id_materiel " +
                "WHERE mat.user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble(1);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur calcul coût : " + e.getMessage());
        }

        return 0.0;
    }

    /**
     * Supprimer une maintenance de l'historique
     */
    public boolean supprimerMaintenance(int maintenanceId) {
        String sql = "DELETE FROM maintenance WHERE id_maintenance = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, maintenanceId);
            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression : " + e.getMessage());
            return false;
        }
    }

    /**
     * Mettre à jour le coût d'une maintenance (après l'avoir effectuée)
     */
    public boolean mettreAJourCout(int maintenanceId, double cout) {
        String sql = "UPDATE maintenance SET cout = ? WHERE id_maintenance = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDouble(1, cout);
            stmt.setInt(2, maintenanceId);
            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur MAJ coût : " + e.getMessage());
            return false;
        }
    }
}
