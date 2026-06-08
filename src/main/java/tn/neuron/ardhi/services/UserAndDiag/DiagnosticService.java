package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.interfaces.IService;

import tn.neuron.ardhi.models.UserAndDiag.Diagnostic;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DiagnosticService implements IService<Diagnostic> {

    private Connection cnx = MyDatabase.getInstance().getCnx();

    // Auto-create table if not exists

    // AJOUT (Utilisé par l'IA quand l'agriculteur scanne)
    @Override
    public void ajouter(Diagnostic d) throws SQLException {
        String req = "INSERT INTO diagnostic (image_scannee, resultat_ia, confiance, user_id, date_scan, latitude, longitude, location_label, severity) VALUES (?, ?, ?, ?, NOW(), ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, d.getImageScannee());
        ps.setString(2, d.getResultatIA());
        ps.setFloat(3, d.getConfiance());
        ps.setInt(4, d.getUserId());
        if (d.getLatitude() != null) {
            ps.setDouble(5, d.getLatitude());
        } else {
            ps.setNull(5, java.sql.Types.DOUBLE);
        }
        if (d.getLongitude() != null) {
            ps.setDouble(6, d.getLongitude());
        } else {
            ps.setNull(6, java.sql.Types.DOUBLE);
        }
        ps.setString(7, d.getLocationLabel());
        ps.setString(8, d.getSeverityAsString());
        ps.executeUpdate();
    }

    // LECTURE ADMIN (SUPERVISION GLOBALE)
    @Override
    public List<Diagnostic> recuperer() throws SQLException {
        List<Diagnostic> list = new ArrayList<>();
        String req = "SELECT d.*, u.nom as user_nom " +
                "FROM diagnostic d " +
                "JOIN user u ON d.user_id = u.id " +
                "ORDER BY d.date_scan DESC";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            list.add(mapResultSetToDiagnostic(rs, true));
        }
        return list;
    }

    @Override
    public List<Diagnostic> rechercher(String keyword) throws SQLException {
        List<Diagnostic> list = new ArrayList<>();
        String req = "SELECT d.*, u.nom as user_nom " +
                "FROM diagnostic d " +
                "JOIN user u ON d.user_id = u.id " +
                "WHERE LOWER(d.resultat_ia) LIKE ? OR LOWER(u.nom) LIKE ? " +
                "ORDER BY d.date_scan DESC";

        PreparedStatement ps = cnx.prepareStatement(req);
        String pattern = "%" + keyword.toLowerCase() + "%";
        ps.setString(1, pattern);
        ps.setString(2, pattern);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapResultSetToDiagnostic(rs, true));
        }
        return list;
    }

    // SUPPRESSION ADMIN
    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM diagnostic WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    // MODIFICATION ADMIN
    @Override
    public void modifier(Diagnostic d) throws SQLException {
        String req = "UPDATE diagnostic SET resultat_ia=?, confiance=?, user_id=?, image_scannee=?, date_scan=?, latitude=?, longitude=?, location_label=?, severity=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, d.getResultatIA());
        ps.setFloat(2, d.getConfiance());
        ps.setInt(3, d.getUserId());
        ps.setString(4, d.getImageScannee());
        ps.setTimestamp(5, d.getDateScan());
        if (d.getLatitude() != null) {
            ps.setDouble(6, d.getLatitude());
        } else {
            ps.setNull(6, java.sql.Types.DOUBLE);
        }
        if (d.getLongitude() != null) {
            ps.setDouble(7, d.getLongitude());
        } else {
            ps.setNull(7, java.sql.Types.DOUBLE);
        }
        ps.setString(8, d.getLocationLabel());
        ps.setString(9, d.getSeverityAsString());
        ps.setInt(10, d.getId());
        ps.executeUpdate();
    }

    // --- Méthode utilitaire pour mapper ResultSet -> Diagnostic ---
    private Diagnostic mapResultSetToDiagnostic(ResultSet rs, boolean includeUserName) throws SQLException {
        Diagnostic d = new Diagnostic();
        d.setId(rs.getInt("id"));
        d.setDateScan(rs.getTimestamp("date_scan"));
        d.setImageScannee(rs.getString("image_scannee"));
        d.setResultatIA(rs.getString("resultat_ia"));
        d.setConfiance(rs.getFloat("confiance"));
        d.setUserId(rs.getInt("user_id"));

        // Read location fields if present
        try {
            d.setLatitude(rs.getObject("latitude") != null ? rs.getDouble("latitude") : null);
            d.setLongitude(rs.getObject("longitude") != null ? rs.getDouble("longitude") : null);
            d.setLocationLabel(rs.getString("location_label"));
            d.setSeverityFromString(rs.getString("severity"));
        } catch (SQLException e) {
            // Location/severity columns not present, ignore
        }

        return d;
    }

    /**
     * Retrieves all diagnostics that have location data for map display.
     */
    public List<Diagnostic> recupererAvecLocation() throws SQLException {
        List<Diagnostic> list = new ArrayList<>();
        String req = "SELECT * FROM diagnostic " +
                "WHERE latitude IS NOT NULL AND longitude IS NOT NULL " +
                "ORDER BY date_scan DESC";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            list.add(mapResultSetToDiagnostic(rs, false));
        }
        return list;
    }

    // LECTURE USER (Historique personnel)
    public List<Diagnostic> recupererHistoriqueParUser(int userId) {
        List<Diagnostic> list = new ArrayList<>();
        String req = "SELECT * FROM diagnostic WHERE user_id = ? ORDER BY date_scan DESC";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToDiagnostic(rs, false));
            }
        } catch (SQLException e) {
            LogUtils.error(DiagnosticService.class,
                    "Erreur lors de la récupération de l'historique", e);
        }
        return list;
    }

    /**
     * Recherche dans l'historique d'un utilisateur avec un mot-clé.
     * Recherche dans le résultat IA.
     */
    public List<Diagnostic> rechercherHistoriqueParUser(int userId, String keyword) {
        List<Diagnostic> list = new ArrayList<>();
        String req = "SELECT * FROM diagnostic " +
                "WHERE user_id = ? AND LOWER(COALESCE(resultat_ia, '')) LIKE ? " +
                "ORDER BY date_scan DESC";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            String pattern = "%" + keyword.toLowerCase() + "%";
            ps.setInt(1, userId);
            ps.setString(2, pattern);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToDiagnostic(rs, false));
            }
        } catch (SQLException e) {
            LogUtils.error(DiagnosticService.class,
                    "Erreur lors de la recherche dans l'historique", e);
        }
        return list;

    }

    public boolean existe(int id) {
        String req = "SELECT 1 FROM diagnostic WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public int ajouterEtRecupererId(Diagnostic d) throws SQLException {
        String req = "INSERT INTO diagnostic (image_scannee, resultat_ia, confiance, user_id, date_scan, latitude, longitude, location_label, severity) VALUES (?, ?, ?, ?, NOW(), ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, d.getImageScannee());
        ps.setString(2, d.getResultatIA());
        ps.setFloat(3, d.getConfiance());
        ps.setInt(4, d.getUserId());
        if (d.getLatitude() != null) {
            ps.setDouble(5, d.getLatitude());
        } else {
            ps.setNull(5, java.sql.Types.DOUBLE);
        }
        if (d.getLongitude() != null) {
            ps.setDouble(6, d.getLongitude());
        } else {
            ps.setNull(6, java.sql.Types.DOUBLE);
        }
        ps.setString(7, d.getLocationLabel());
        ps.setString(8, d.getSeverityAsString());
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            int diagId = rs.getInt(1);
            d.setId(diagId);
            return diagId;
        }
        return -1;
    }

    public int compterDiagnosticsHeure(int userId) {
        String sql = "SELECT COUNT(*) FROM diagnostic WHERE user_id = ? AND date_scan >= DATE_SUB(NOW(), INTERVAL 1 HOUR)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LogUtils.error(DiagnosticService.class, "Erreur comptage diagnostics", e);
        }
        return 0;
    }

    public String getProchainResetTime(int userId) {
        String sql = "SELECT MIN(date_scan) FROM diagnostic WHERE user_id = ? AND date_scan >= DATE_SUB(NOW(), INTERVAL 1 HOUR)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp oldest = rs.getTimestamp(1);
                    if (oldest != null) {
                        long expiresAt = oldest.getTime() + (60 * 60 * 1000); // +1 hour in ms
                        long now = System.currentTimeMillis();
                        long diffMs = expiresAt - now;

                        if (diffMs <= 0) {
                            return "maintenant";
                        } else {
                            long minutes = diffMs / (60 * 1000);
                            if (minutes < 60) {
                                return "dans " + minutes + " min";
                            } else {
                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
                                return sdf.format(new java.util.Date(expiresAt));
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LogUtils.error(DiagnosticService.class, "Erreur calcul reset time", e);
        }
        return "bientôt";
    }

    // --- STATISTIQUES POUR DASHBOARD ---

    public java.util.Map<String, Integer> getMaladieDistribution() {
        java.util.Map<String, Integer> stats = new java.util.HashMap<>();
        String req = "SELECT resultat_ia, COUNT(*) as total FROM diagnostic GROUP BY resultat_ia";
        try (Statement st = cnx.createStatement();
                ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                String label = rs.getString("resultat_ia");
                // Nettoyage optionnel : garder seulement la maladie si format "Plante -
                // Maladie"
                if (label != null && label.contains("-")) {
                    String[] parts = label.split("-");
                    if (parts.length > 1)
                        label = parts[1].trim();
                }
                stats.put(label, rs.getInt("total"));
            }
        } catch (SQLException e) {
            LogUtils.error(DiagnosticService.class, "Erreur stats maladies", e);
        }
        return stats;
    }

    public java.util.Map<String, Integer> getDiagnosticsPerDate() {
        java.util.Map<java.lang.String, Integer> stats = new java.util.LinkedHashMap<>();
        // Derniers 7 jours
        String req = "SELECT DATE(date_scan) as jour, COUNT(*) as total " +
                "FROM diagnostic " +
                "WHERE date_scan >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
                "GROUP BY DATE(date_scan) " +
                "ORDER BY jour ASC";
        try (Statement st = cnx.createStatement();
                ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                stats.put(rs.getString("jour"), rs.getInt("total"));
            }
        } catch (SQLException e) {
            LogUtils.error(DiagnosticService.class, "Erreur stats dates", e);
        }
        return stats;
    }
}