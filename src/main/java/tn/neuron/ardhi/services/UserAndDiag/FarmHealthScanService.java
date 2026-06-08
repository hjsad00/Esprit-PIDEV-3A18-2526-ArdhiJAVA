package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.models.UserAndDiag.FarmHealthScan;
import tn.neuron.ardhi.models.UserAndDiag.ScanStatus;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FarmHealthScanService {

    private Connection cnx = MyDatabase.getInstance().getCnx();

    /**
     * Saves a new scan and returns the generated ID.
     */
    public int save(FarmHealthScan scan) {
        String sql = "INSERT INTO farm_health_scan (user_id, crop_type, planting_date, growth_stage, " +
                "latitude, longitude, concerns, photo_crops, photo_soil, photo_edges, " +
                "photo_insects, photo_spacing, photo_overview, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, scan.getUserId());
            pst.setString(2, scan.getCropType());
            pst.setDate(3, scan.getPlantingDate());
            pst.setString(4, scan.getGrowthStage());
            if (scan.getLatitude() != null)
                pst.setDouble(5, scan.getLatitude());
            else
                pst.setNull(5, Types.DOUBLE);
            if (scan.getLongitude() != null)
                pst.setDouble(6, scan.getLongitude());
            else
                pst.setNull(6, Types.DOUBLE);
            pst.setString(7, scan.getConcerns());
            pst.setString(8, scan.getPhotoCrops());
            pst.setString(9, scan.getPhotoSoil());
            pst.setString(10, scan.getPhotoEdges());
            pst.setString(11, scan.getPhotoInsects());
            pst.setString(12, scan.getPhotoSpacing());
            pst.setString(13, scan.getPhotoOverview());
            pst.setString(14, scan.getStatus().name());
            pst.executeUpdate();

            ResultSet keys = pst.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                scan.setId(id);
                return id;
            }
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error saving farm health scan", e);
        }
        return -1;
    }

    /**
     * Updates the scan status.
     */
    public void updateStatus(int scanId, ScanStatus status) {
        String sql = "UPDATE farm_health_scan SET status = ? WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, status.name());
            pst.setInt(2, scanId);
            pst.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error updating scan status", e);
        }
    }

    /**
     * Updates photo URLs on a scan.
     */
    public void updatePhotos(FarmHealthScan scan) {
        String sql = "UPDATE farm_health_scan SET photo_crops=?, photo_soil=?, photo_edges=?, " +
                "photo_insects=?, photo_spacing=?, photo_overview=? WHERE id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, scan.getPhotoCrops());
            pst.setString(2, scan.getPhotoSoil());
            pst.setString(3, scan.getPhotoEdges());
            pst.setString(4, scan.getPhotoInsects());
            pst.setString(5, scan.getPhotoSpacing());
            pst.setString(6, scan.getPhotoOverview());
            pst.setInt(7, scan.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error updating scan photos", e);
        }
    }

    /**
     * Retrieves a scan by ID.
     */
    public FarmHealthScan getById(int id) {
        String sql = "SELECT * FROM farm_health_scan WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return mapScan(rs);
            }
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error getting scan by ID", e);
        }
        return null;
    }

    /**
     * Retrieves all scans for a user, ordered by date descending.
     */
    public List<FarmHealthScan> getByUser(int userId) {
        List<FarmHealthScan> scans = new ArrayList<>();
        String sql = "SELECT * FROM farm_health_scan WHERE user_id = ? ORDER BY scan_date DESC";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                scans.add(mapScan(rs));
            }
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error getting scans for user", e);
        }
        return scans;
    }

    /**
     * Retrieves all scans, ordered by date descending.
     */
    public List<FarmHealthScan> getAll() {
        List<FarmHealthScan> scans = new ArrayList<>();
        String sql = "SELECT * FROM farm_health_scan ORDER BY scan_date DESC";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                scans.add(mapScan(rs));
            }
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error getting all scans", e);
        }
        return scans;
    }

    /**
     * Updates all editable fields of a scan.
     */
    public void update(FarmHealthScan scan) {
        String sql = "UPDATE farm_health_scan SET user_id=?, crop_type=?, planting_date=?, growth_stage=?, " +
                "latitude=?, longitude=?, concerns=?, status=? WHERE id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, scan.getUserId());
            pst.setString(2, scan.getCropType());
            pst.setDate(3, scan.getPlantingDate());
            pst.setString(4, scan.getGrowthStage());
            if (scan.getLatitude() != null)
                pst.setDouble(5, scan.getLatitude());
            else
                pst.setNull(5, Types.DOUBLE);
            if (scan.getLongitude() != null)
                pst.setDouble(6, scan.getLongitude());
            else
                pst.setNull(6, Types.DOUBLE);
            pst.setString(7, scan.getConcerns());
            pst.setString(8, scan.getStatus().name());
            pst.setInt(9, scan.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error updating scan", e);
        }
    }

    /**
     * Searches scans by keyword (crop type, growth stage, concerns, status).
     */
    public List<FarmHealthScan> search(String keyword) {
        List<FarmHealthScan> scans = new ArrayList<>();
        String sql = "SELECT * FROM farm_health_scan WHERE " +
                "LOWER(crop_type) LIKE ? OR LOWER(growth_stage) LIKE ? OR LOWER(concerns) LIKE ? OR LOWER(status) LIKE ? OR CAST(user_id AS CHAR) LIKE ? "
                +
                "ORDER BY scan_date DESC";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            String pattern = "%" + keyword.toLowerCase() + "%";
            for (int i = 1; i <= 5; i++)
                pst.setString(i, pattern);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                scans.add(mapScan(rs));
            }
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error searching scans", e);
        }
        return scans;
    }

    /**
     * Deletes a scan by ID (cascades to report, vulnerabilities, plans, tasks).
     */
    public void delete(int id) {
        String sql = "DELETE FROM farm_health_scan WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error deleting scan", e);
        }
    }

    private FarmHealthScan mapScan(ResultSet rs) throws SQLException {
        FarmHealthScan scan = new FarmHealthScan();
        scan.setId(rs.getInt("id"));
        scan.setUserId(rs.getInt("user_id"));
        scan.setCropType(rs.getString("crop_type"));
        scan.setPlantingDate(rs.getDate("planting_date"));
        scan.setGrowthStage(rs.getString("growth_stage"));

        double lat = rs.getDouble("latitude");
        if (!rs.wasNull())
            scan.setLatitude(lat);
        double lon = rs.getDouble("longitude");
        if (!rs.wasNull())
            scan.setLongitude(lon);

        scan.setConcerns(rs.getString("concerns"));
        scan.setPhotoCrops(rs.getString("photo_crops"));
        scan.setPhotoSoil(rs.getString("photo_soil"));
        scan.setPhotoEdges(rs.getString("photo_edges"));
        scan.setPhotoInsects(rs.getString("photo_insects"));
        scan.setPhotoSpacing(rs.getString("photo_spacing"));
        scan.setPhotoOverview(rs.getString("photo_overview"));
        scan.setScanDate(rs.getTimestamp("scan_date"));

        String statusStr = rs.getString("status");
        try {
            scan.setStatus(ScanStatus.valueOf(statusStr));
        } catch (Exception e) {
            scan.setStatus(ScanStatus.PENDING);
        }

        return scan;
    }
}
