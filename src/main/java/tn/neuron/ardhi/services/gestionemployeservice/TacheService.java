package tn.neuron.ardhi.services.gestionemployeservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tn.neuron.ardhi.models.gestionemployemodel.Tache;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;
import tn.neuron.ardhi.utils.gestionemployeutils.AgriculteurContext;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TacheService {

    private static final Logger log = LoggerFactory.getLogger(TacheService.class);

    private Connection getCnx() {
        return MyDatabase.getInstance().getCnx();
    }

    public boolean createTache(Tache tache) {
        tache.setDateModification(LocalDateTime.now());
        Integer idAgriculteur = AgriculteurContext.getActiveAgriculteurId();
        tache.setIdAgriculteur(idAgriculteur);

        String query = "INSERT INTO tache (titre, description, statut, date_debut, date_fin, " +
                "id_employe, id_agriculteur, priorite, categorie, date_modification, google_event_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = getCnx().prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, tache.getTitre());
            stmt.setString(2, tache.getDescription());
            stmt.setString(3, tache.getStatut());
            stmt.setDate(4, tache.getDateDebut() != null ? Date.valueOf(tache.getDateDebut()) : null);
            stmt.setDate(5, tache.getDateFin() != null ? Date.valueOf(tache.getDateFin()) : null);
            setIntOrNull(stmt, 6, tache.getIdEmploye());
            setIntOrNull(stmt, 7, idAgriculteur);
            setIntOrNull(stmt, 8, tache.getPriorite());
            stmt.setString(9, tache.getCategorie());
            stmt.setTimestamp(10, Timestamp.valueOf(tache.getDateModification()));
            if (tache.getGoogleEventId() != null) stmt.setString(11, tache.getGoogleEventId());
            else stmt.setNull(11, java.sql.Types.VARCHAR);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) tache.setId(keys.getInt(1));
                }
                System.out.println("✅ Tâche créée ! ID: " + tache.getId());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur création tâche: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public List<Tache> getAllTaches() {
        List<Tache> taches = new ArrayList<>();
        Integer idAgriculteur = AgriculteurContext.getActiveAgriculteurId();

        String query;
        if (idAgriculteur != null) {
            query = "SELECT * FROM tache WHERE id_agriculteur = " + idAgriculteur + " ORDER BY id_tache DESC";
        } else {
            query = "SELECT * FROM tache ORDER BY id_tache DESC";
        }

        try (Statement stmt = getCnx().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) taches.add(mapResultSetToTache(rs));
            System.out.println("✅ Tâches récupérées: " + taches.size() + " (agriculteur=" + idAgriculteur + ")");
        } catch (SQLException e) {
            System.err.println("❌ Erreur getAllTaches: " + e.getMessage());
            e.printStackTrace();
        }
        return taches;
    }

    public Tache getTacheById(int id) {
        String query = "SELECT * FROM tache WHERE id_tache = ?";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapResultSetToTache(rs);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getTacheById: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateTache(Tache tache) {
        tache.setDateModification(LocalDateTime.now());

        String query = "UPDATE tache SET titre=?, description=?, statut=?, date_debut=?, date_fin=?, " +
                "id_employe=?, priorite=?, categorie=?, date_modification=?, google_event_id=? WHERE id_tache=?";

        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setString(1, tache.getTitre());
            stmt.setString(2, tache.getDescription());
            stmt.setString(3, tache.getStatut());
            stmt.setDate(4, tache.getDateDebut() != null ? Date.valueOf(tache.getDateDebut()) : null);
            stmt.setDate(5, tache.getDateFin() != null ? Date.valueOf(tache.getDateFin()) : null);
            setIntOrNull(stmt, 6, tache.getIdEmploye());
            setIntOrNull(stmt, 7, tache.getPriorite());
            stmt.setString(8, tache.getCategorie());
            stmt.setTimestamp(9, Timestamp.valueOf(tache.getDateModification()));
            if (tache.getGoogleEventId() != null) stmt.setString(10, tache.getGoogleEventId());
            else stmt.setNull(10, java.sql.Types.VARCHAR);
            stmt.setInt(11, tache.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur updateTache: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteTache(int id) {
        String query = "DELETE FROM tache WHERE id_tache=?";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();
            System.out.println("✅ Tâche supprimée. Lignes: " + rows);
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur deleteTache: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public List<Tache> searchTaches(String recherche) {
        List<Tache> taches = new ArrayList<>();
        Integer idAgriculteur = AgriculteurContext.getActiveAgriculteurId();
        String pattern = "%" + recherche + "%";

        String query;
        if (idAgriculteur != null) {
            query = "SELECT * FROM tache WHERE id_agriculteur = ? AND (titre LIKE ? OR description LIKE ?) ORDER BY id_tache DESC";
            try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
                stmt.setInt(1, idAgriculteur);
                stmt.setString(2, pattern);
                stmt.setString(3, pattern);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) taches.add(mapResultSetToTache(rs));
                }
            } catch (SQLException e) {
                System.err.println("❌ Erreur searchTaches: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            query = "SELECT * FROM tache WHERE titre LIKE ? OR description LIKE ? ORDER BY id_tache DESC";
            try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
                stmt.setString(1, pattern);
                stmt.setString(2, pattern);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) taches.add(mapResultSetToTache(rs));
                }
            } catch (SQLException e) {
                System.err.println("❌ Erreur searchTaches: " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println("🔍 Résultats recherche tâches: " + taches.size());
        return taches;
    }

    public List<Tache> getTachesByStatut(String statut) {
        List<Tache> taches = new ArrayList<>();
        Integer idAgriculteur = AgriculteurContext.getActiveAgriculteurId();

        String query;
        if (idAgriculteur != null) {
            query = "SELECT * FROM tache WHERE id_agriculteur = ? AND statut = ? ORDER BY id_tache DESC";
            try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
                stmt.setInt(1, idAgriculteur);
                stmt.setString(2, statut);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) taches.add(mapResultSetToTache(rs));
                }
            } catch (SQLException e) {
                System.err.println("❌ Erreur getTachesByStatut: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            query = "SELECT * FROM tache WHERE statut = ? ORDER BY id_tache DESC";
            try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
                stmt.setString(1, statut);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) taches.add(mapResultSetToTache(rs));
                }
            } catch (SQLException e) {
                System.err.println("❌ Erreur getTachesByStatut: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return taches;
    }

    public List<Tache> getTachesByEmployeId(int idEmploye) {
        List<Tache> taches = new ArrayList<>();
        String query = "SELECT * FROM tache WHERE id_employe = ? ORDER BY date_debut DESC";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idEmploye);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) taches.add(mapResultSetToTache(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ getTachesByEmployeId: " + e.getMessage());
            e.printStackTrace();
        }
        return taches;
    }

    public List<Tache> getTachesByAgriculteur(Integer idAgriculteur) {
        List<Tache> taches = new ArrayList<>();
        String query = "SELECT * FROM tache WHERE id_agriculteur = ? ORDER BY id_tache DESC";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idAgriculteur);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) taches.add(mapResultSetToTache(rs));
            }
            System.out.println("✅ " + taches.size() + " tâche(s) pour agriculteur " + idAgriculteur);
        } catch (SQLException e) {
            System.err.println("❌ getTachesByAgriculteur: " + e.getMessage());
            e.printStackTrace();
        }
        return taches;
    }

    public int countByAgriculteur(Integer idAgriculteur) {
        String query = "SELECT COUNT(*) FROM tache WHERE id_agriculteur = ?";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idAgriculteur);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ countByAgriculteur (tache): " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Retourne l'ensemble de tous les IDs de tâches existantes en base (toutes confondues).
     * Utilisé par le nettoyage Google Calendar pour identifier les événements orphelins.
     */
    public java.util.Set<Integer> getAllTaskIds() {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        String query = "SELECT id_tache FROM tache";
        try (Statement stmt = getCnx().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) ids.add(rs.getInt(1));
        } catch (SQLException e) {
            System.err.println("❌ getAllTaskIds: " + e.getMessage());
        }
        return ids;
    }

    private Tache mapResultSetToTache(ResultSet rs) throws SQLException {
        Tache tache = new Tache();
        tache.setId(rs.getInt("id_tache"));
        tache.setTitre(rs.getString("titre"));
        tache.setDescription(rs.getString("description"));
        tache.setStatut(rs.getString("statut"));

        Date dateDebut = rs.getDate("date_debut");
        if (dateDebut != null) tache.setDateDebut(dateDebut.toLocalDate());

        Date dateFin = rs.getDate("date_fin");
        if (dateFin != null) tache.setDateFin(dateFin.toLocalDate());

        int idEmploye = rs.getInt("id_employe");
        if (!rs.wasNull()) tache.setIdEmploye(idEmploye);

        tache.setIdAgriculteur(rs.getObject("id_agriculteur", Integer.class));

        int priorite = rs.getInt("priorite");
        if (!rs.wasNull()) tache.setPriorite(priorite);

        tache.setCategorie(rs.getString("categorie"));
        try { tache.setGoogleEventId(rs.getString("google_event_id")); }
        catch (SQLException ignored) { /* colonne pas encore en base */ }

        Timestamp ts = rs.getTimestamp("date_modification");
        if (ts != null) tache.setDateModification(ts.toLocalDateTime());

        return tache;
    }

    /**
     * Persiste le google_event_id après sync Google Calendar.
     */
    public boolean saveGoogleEventId(int idTache, String googleEventId) {
        String q = "UPDATE tache SET google_event_id = ? WHERE id_tache = ?";
        try (PreparedStatement stmt = getCnx().prepareStatement(q)) {
            if (googleEventId != null) stmt.setString(1, googleEventId);
            else stmt.setNull(1, java.sql.Types.VARCHAR);
            stmt.setInt(2, idTache);
            System.out.println("[GCal] google_event_id sauvegardé → tâche #" + idTache + " : " + googleEventId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[GCal] Erreur saveGoogleEventId : " + e.getMessage());
            return false;
        }
    }

    private void setIntOrNull(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value != null) {
            stmt.setInt(index, value);
        } else {
            stmt.setNull(index, Types.INTEGER);
        }
    }
}