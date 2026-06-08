package tn.neuron.ardhi.services.Parcelle_Cultures;

import tn.neuron.ardhi.interfaces.IService;
import tn.neuron.ardhi.models.Parcelle_Cultures.Parcelle;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service CRUD pour l'entité Parcelle.
 * Version Premium: inclut latitude, longitude et méthodes Smart Agriculture.
 */
public class ParcelleService implements IService<Parcelle> {

    private Connection cnx;

    public ParcelleService() {
        this.cnx = MyDatabase.getInstance().getCnx();
    }

    public ParcelleService(Connection cnx) {
        this.cnx = cnx;
    }

    // ==================== CRUD OPERATIONS ====================

    @Override
    public void ajouter(Parcelle p) throws SQLException {
        String req = "INSERT INTO parcelle (surface, localisation, type_sol, systeme_irrigation, statut, agriculteur_id, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setDouble(1, p.getSurface());
        ps.setString(2, p.getLocalisation());
        ps.setString(3, p.getTypeSol());
        ps.setString(4, p.getSystemeIrrigation());
        ps.setString(5, p.getStatut());
        ps.setInt(6, p.getAgriculteurId());
        // GPS (nullable)
        if (p.getLatitude() != null) ps.setDouble(7, p.getLatitude());
        else ps.setNull(7, Types.DOUBLE);
        if (p.getLongitude() != null) ps.setDouble(8, p.getLongitude());
        else ps.setNull(8, Types.DOUBLE);
        ps.executeUpdate();
    }

    public int ajouterEtRecupererId(Parcelle p) throws SQLException {
        String req = "INSERT INTO parcelle (surface, localisation, type_sol, systeme_irrigation, statut, agriculteur_id, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        ps.setDouble(1, p.getSurface());
        ps.setString(2, p.getLocalisation());
        ps.setString(3, p.getTypeSol());
        ps.setString(4, p.getSystemeIrrigation());
        ps.setString(5, p.getStatut());
        ps.setInt(6, p.getAgriculteurId());
        if (p.getLatitude() != null) ps.setDouble(7, p.getLatitude());
        else ps.setNull(7, Types.DOUBLE);
        if (p.getLongitude() != null) ps.setDouble(8, p.getLongitude());
        else ps.setNull(8, Types.DOUBLE);
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) return rs.getInt(1);
        return -1;
    }

    @Override
    public List<Parcelle> recuperer() throws SQLException {
        List<Parcelle> list = new ArrayList<>();
        String req = "SELECT p.*, u.nom as agriculteur_nom " +
                "FROM parcelle p " +
                "JOIN user u ON p.agriculteur_id = u.id " +
                "ORDER BY p.id DESC";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            list.add(mapResultSetToParcelle(rs, true));
        }
        return list;
    }

    public List<Parcelle> recupererParAgriculteur(int agriculteurId) throws SQLException {
        List<Parcelle> list = new ArrayList<>();
        String req = "SELECT * FROM parcelle WHERE agriculteur_id = ? ORDER BY id DESC";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, agriculteurId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapResultSetToParcelle(rs, false));
        }
        return list;
    }

    public Parcelle recupererParId(int id) throws SQLException {
        String req = "SELECT p.*, u.nom as agriculteur_nom " +
                "FROM parcelle p " +
                "JOIN user u ON p.agriculteur_id = u.id " +
                "WHERE p.id = ?";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapResultSetToParcelle(rs, true);
        }
        return null;
    }

    @Override
    public List<Parcelle> rechercher(String keyword) throws SQLException {
        List<Parcelle> list = new ArrayList<>();
        String req = "SELECT p.*, u.nom as agriculteur_nom " +
                "FROM parcelle p " +
                "JOIN user u ON p.agriculteur_id = u.id " +
                "WHERE LOWER(p.localisation) LIKE ? OR LOWER(p.type_sol) LIKE ? OR LOWER(u.nom) LIKE ? " +
                "ORDER BY p.id DESC";

        PreparedStatement ps = cnx.prepareStatement(req);
        String pattern = "%" + keyword.toLowerCase() + "%";
        ps.setString(1, pattern);
        ps.setString(2, pattern);
        ps.setString(3, pattern);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapResultSetToParcelle(rs, true));
        }
        return list;
    }

    @Override
    public void modifier(Parcelle p) throws SQLException {
        String req = "UPDATE parcelle SET surface=?, localisation=?, type_sol=?, systeme_irrigation=?, statut=?, latitude=?, longitude=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setDouble(1, p.getSurface());
        ps.setString(2, p.getLocalisation());
        ps.setString(3, p.getTypeSol());
        ps.setString(4, p.getSystemeIrrigation());
        ps.setString(5, p.getStatut());
        if (p.getLatitude() != null) ps.setDouble(6, p.getLatitude());
        else ps.setNull(6, Types.DOUBLE);
        if (p.getLongitude() != null) ps.setDouble(7, p.getLongitude());
        else ps.setNull(7, Types.DOUBLE);
        ps.setInt(8, p.getId());
        ps.executeUpdate();
    }

    /**
     * Met à jour uniquement les coordonnées GPS d'une parcelle.
     * Appelé par le service de géolocalisation automatique.
     */
    public void mettreAJourCoordonnees(int parcelleId, double latitude, double longitude)
            throws SQLException {
        String req = "UPDATE parcelle SET latitude=?, longitude=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setDouble(1, latitude);
        ps.setDouble(2, longitude);
        ps.setInt(3, parcelleId);
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        // Suppression en cascade des cultures associées
        String deleteCultures = "DELETE FROM culture WHERE parcelle_id = ?";
        PreparedStatement psCultures = cnx.prepareStatement(deleteCultures);
        psCultures.setInt(1, id);
        psCultures.executeUpdate();

        // Suppression de la parcelle
        String req = "DELETE FROM parcelle WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    // ==================== STATISTIQUES ====================

    public boolean existe(int id) throws SQLException {
        String req = "SELECT 1 FROM parcelle WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    public int compterParcelles() {
        String req = "SELECT COUNT(*) FROM parcelle";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LogUtils.error(ParcelleService.class, "Erreur comptage parcelles", e);
        }
        return 0;
    }

    public int compterParcellesParAgriculteur(int agriculteurId) {
        String req = "SELECT COUNT(*) FROM parcelle WHERE agriculteur_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, agriculteurId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LogUtils.error(ParcelleService.class, "Erreur comptage parcelles agriculteur", e);
        }
        return 0;
    }

    public double getSurfaceTotaleParAgriculteur(int agriculteurId) {
        String req = "SELECT SUM(surface) FROM parcelle WHERE agriculteur_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, agriculteurId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            LogUtils.error(ParcelleService.class, "Erreur calcul surface totale", e);
        }
        return 0;
    }

    // ==================== MAPPING ====================

    private Parcelle mapResultSetToParcelle(ResultSet rs, boolean includeAgriculteurName)
            throws SQLException {
        Parcelle p = new Parcelle();
        p.setId(rs.getInt("id"));
        p.setSurface(rs.getDouble("surface"));
        p.setLocalisation(rs.getString("localisation"));
        p.setTypeSol(rs.getString("type_sol"));
        p.setSystemeIrrigation(rs.getString("systeme_irrigation"));
        p.setStatut(rs.getString("statut"));
        p.setAgriculteurId(rs.getInt("agriculteur_id"));

        // GPS (peut être NULL en base)
        try {
            double lat = rs.getDouble("latitude");
            if (!rs.wasNull()) p.setLatitude(lat);
            double lon = rs.getDouble("longitude");
            if (!rs.wasNull()) p.setLongitude(lon);
        } catch (SQLException ignored) {
            // Colonne latitude/longitude absente (avant migration)
        }

        if (includeAgriculteurName) {
            try {
                p.setNomAgriculteurTemp(rs.getString("agriculteur_nom"));
            } catch (SQLException ignored) {}
        }
        return p;
    }
}
