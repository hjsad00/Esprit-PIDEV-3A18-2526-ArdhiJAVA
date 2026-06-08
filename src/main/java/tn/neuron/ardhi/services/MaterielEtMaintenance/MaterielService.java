package tn.neuron.ardhi.services.MaterielEtMaintenance;

import tn.neuron.ardhi.models.MaterielEtMaintenance.Materiel;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * CORRECTION : plus de champ "connection" stocké.
 * getCnx() est appelé à chaque méthode → connexion toujours fraîche.
 */
public class MaterielService {

    // ══ Pas de champ connection — on récupère à chaque appel ══
    private Connection getCnx() {
        return MyDatabase.getInstance().getCnx();
    }

    // ════════════════════════════════════════════════════════
    //  MÉTHODES ADMIN
    // ════════════════════════════════════════════════════════

    public List<Materiel> recuperer() {
        List<Materiel> materiels = new ArrayList<>();
        String query = "SELECT * FROM materiel";

        try (Statement stmt = getCnx().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) materiels.add(creerMaterielComplet(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return materiels;
    }

    public void ajouter(Materiel materiel) {
        String query = "INSERT INTO materiel (nom, type, etat, user_id, date_achat) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = getCnx().prepareStatement(query)) {
            pstmt.setString(1, materiel.getNom());
            pstmt.setString(2, materiel.getType());
            pstmt.setString(3, materiel.getEtat());
            pstmt.setInt(4, materiel.getUser_id());
            pstmt.setDate(5, materiel.getDate_achat() != null ? Date.valueOf(materiel.getDate_achat()) : null);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void modifier(Materiel materiel) {
        String query = "UPDATE materiel SET nom = ?, type = ?, etat = ?, user_id = ?, date_achat = ? WHERE id_materiel = ?";

        try (PreparedStatement pstmt = getCnx().prepareStatement(query)) {
            pstmt.setString(1, materiel.getNom());
            pstmt.setString(2, materiel.getType());
            pstmt.setString(3, materiel.getEtat());
            pstmt.setInt(4, materiel.getUser_id());
            pstmt.setDate(5, materiel.getDate_achat() != null ? Date.valueOf(materiel.getDate_achat()) : null);
            pstmt.setInt(6, materiel.getId_materiel());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void supprimer(int idMateriel) {
        String query = "DELETE FROM materiel WHERE id_materiel = ?";

        try (PreparedStatement pstmt = getCnx().prepareStatement(query)) {
            pstmt.setInt(1, idMateriel);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════
    //  RÉCUPÉRER PAR ID
    // ════════════════════════════════════════════════════════

    /**
     * Récupère un Materiel par son ID.
     * Utilisé par NotificationScheduler pour obtenir le nom du matériel.
     */
    public Materiel getById(int idMateriel) {
        String query = "SELECT * FROM materiel WHERE id_materiel = ?";

        try (PreparedStatement pstmt = getCnx().prepareStatement(query)) {
            pstmt.setInt(1, idMateriel);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return creerMaterielComplet(rs);
        } catch (SQLException e) {
            System.err.println("❌ Erreur getById(" + idMateriel + ") : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // ════════════════════════════════════════════════════════
    //  MÉTHODES AGRICULTEUR
    // ════════════════════════════════════════════════════════

    public boolean ajouterMateriel(Materiel materiel) {
        String query = "INSERT INTO materiel (nom, type, etat, user_id, date_achat) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = getCnx().prepareStatement(query)) {
            pstmt.setString(1, materiel.getNom());
            pstmt.setString(2, materiel.getType());
            pstmt.setString(3, materiel.getEtat());
            pstmt.setInt(4, materiel.getUser_id());
            pstmt.setDate(5, materiel.getDate_achat() != null ? Date.valueOf(materiel.getDate_achat()) : null);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean modifierMateriel(Materiel materiel) {
        String query = "UPDATE materiel SET nom = ?, type = ?, date_achat = ? WHERE id_materiel = ?";

        try (PreparedStatement pstmt = getCnx().prepareStatement(query)) {
            pstmt.setString(1, materiel.getNom());
            pstmt.setString(2, materiel.getType());
            pstmt.setDate(3, materiel.getDate_achat() != null ? Date.valueOf(materiel.getDate_achat()) : null);
            pstmt.setInt(4, materiel.getId_materiel());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean supprimerMateriel(int idMateriel) {
        String query = "DELETE FROM materiel WHERE id_materiel = ?";

        try (PreparedStatement pstmt = getCnx().prepareStatement(query)) {
            pstmt.setInt(1, idMateriel);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ════════════════════════════════════════════════════════
    //  MÉTHODES COMMUNES
    // ════════════════════════════════════════════════════════

    public List<Materiel> getMaterielsByUserId(int userId) {
        List<Materiel> materiels = new ArrayList<>();
        String query = "SELECT * FROM materiel WHERE user_id = ?";

        try (PreparedStatement pstmt = getCnx().prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) materiels.add(creerMaterielComplet(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return materiels;
    }

    // ════════════════════════════════════════════════════════
    //  MÉTHODES NOTIFICATIONS (anciennes — basées sur le modèle Materiel)
    // ════════════════════════════════════════════════════════

    public List<Materiel> getMaterielsNeedingMaintenance(int userId) {
        List<Materiel> materiels = new ArrayList<>();
        for (Materiel m : getMaterielsByUserId(userId)) {
            if (m.needsMaintenance()) materiels.add(m);
        }
        return materiels;
    }

    public List<Materiel> getMaterielsMaintenanceSoon(int userId) {
        List<Materiel> materiels = new ArrayList<>();
        for (Materiel m : getMaterielsByUserId(userId)) {
            long daysUntil = m.getDaysUntilMaintenance();
            if (daysUntil >= 0 && daysUntil <= 30) materiels.add(m);
        }
        return materiels;
    }

    // ════════════════════════════════════════════════════════
    //  MÉTHODES GOOGLE CALENDAR
    // ════════════════════════════════════════════════════════

    public boolean planifierMaintenance(int materielId, LocalDate dateMaintenance, String googleEventId) {
        String query = "UPDATE materiel SET date_prochaine_maintenance = ?, google_calendar_event_id = ? WHERE id_materiel = ?";

        try (PreparedStatement pstmt = getCnx().prepareStatement(query)) {
            pstmt.setDate(1, Date.valueOf(dateMaintenance));
            pstmt.setString(2, googleEventId);
            pstmt.setInt(3, materielId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean marquerMaintenanceEffectuee(int materielId) {
        String query = "UPDATE materiel SET derniere_maintenance = CURDATE(), " +
                "date_prochaine_maintenance = DATE_ADD(CURDATE(), INTERVAL frequence_maintenance_mois MONTH) " +
                "WHERE id_materiel = ?";

        try (PreparedStatement pstmt = getCnx().prepareStatement(query)) {
            pstmt.setInt(1, materielId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean annulerMaintenancePlanifiee(int materielId) {
        String query = "UPDATE materiel SET date_prochaine_maintenance = NULL, google_calendar_event_id = NULL WHERE id_materiel = ?";

        try (PreparedStatement pstmt = getCnx().prepareStatement(query)) {
            pstmt.setInt(1, materielId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateFrequenceMaintenance(int materielId, int frequenceMois) {
        String query = "UPDATE materiel SET frequence_maintenance_mois = ? WHERE id_materiel = ?";

        try (PreparedStatement pstmt = getCnx().prepareStatement(query)) {
            pstmt.setInt(1, frequenceMois);
            pstmt.setInt(2, materielId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ════════════════════════════════════════════════════════
    //  REQUÊTES SQL AVANCÉES
    // ════════════════════════════════════════════════════════

    public List<Materiel> getMaterielsMaintenanceUrgente(int userId) {
        List<Materiel> materiels = new ArrayList<>();
        String query = "SELECT * FROM materiel WHERE user_id = ? AND " +
                "((date_prochaine_maintenance IS NOT NULL AND date_prochaine_maintenance < CURDATE()) OR " +
                "(date_prochaine_maintenance IS NULL AND date_achat IS NOT NULL AND " +
                "DATE_ADD(date_achat, INTERVAL 12 MONTH) < CURDATE()))";

        try (PreparedStatement pstmt = getCnx().prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) materiels.add(creerMaterielComplet(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return materiels;
    }

    public List<Materiel> getMaterielsMaintenanceProcheSQL(int userId) {
        List<Materiel> materiels = new ArrayList<>();
        String query = "SELECT * FROM materiel WHERE user_id = ? AND " +
                "((date_prochaine_maintenance IS NOT NULL AND " +
                "date_prochaine_maintenance BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)) OR " +
                "(date_prochaine_maintenance IS NULL AND date_achat IS NOT NULL AND " +
                "DATE_ADD(date_achat, INTERVAL 12 MONTH) BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)))";

        try (PreparedStatement pstmt = getCnx().prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) materiels.add(creerMaterielComplet(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return materiels;
    }

    // ════════════════════════════════════════════════════════
    //  UTILITAIRE PRIVÉ
    // ════════════════════════════════════════════════════════

    private Materiel creerMaterielComplet(ResultSet rs) throws SQLException {
        Materiel materiel = new Materiel();
        materiel.setId_materiel(rs.getInt("id_materiel"));
        materiel.setNom(rs.getString("nom"));
        materiel.setType(rs.getString("type"));
        materiel.setEtat(rs.getString("etat"));
        materiel.setUser_id(rs.getInt("user_id"));

        Date dateAchat = rs.getDate("date_achat");
        if (dateAchat != null) materiel.setDate_achat(dateAchat.toLocalDate());

        Date dateProchaine = rs.getDate("date_prochaine_maintenance");
        if (dateProchaine != null) materiel.setDate_prochaine_maintenance(dateProchaine.toLocalDate());

        String googleEventId = rs.getString("google_calendar_event_id");
        if (googleEventId != null) materiel.setGoogle_calendar_event_id(googleEventId);

        Date derniere = rs.getDate("derniere_maintenance");
        if (derniere != null) materiel.setDerniere_maintenance(derniere.toLocalDate());

        materiel.setFrequence_maintenance_mois(rs.getInt("frequence_maintenance_mois"));

        return materiel;
    }
}
