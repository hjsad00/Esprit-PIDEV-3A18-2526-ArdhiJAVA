package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.models.UserAndDiag.FarmHealthReport;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FarmHealthReportService {

    private Connection cnx = MyDatabase.getInstance().getCnx();

    public List<FarmHealthReport> getAll() {
        List<FarmHealthReport> reports = new ArrayList<>();
        String sql = "SELECT r.*, s.crop_type, s.growth_stage FROM farm_health_report r " +
                "LEFT JOIN farm_health_scan s ON r.scan_id = s.id ORDER BY r.generated_at DESC";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                reports.add(mapReport(rs));
            }
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error getting all reports", e);
        }
        return reports;
    }

    public FarmHealthReport getById(int id) {
        String sql = "SELECT r.*, s.crop_type, s.growth_stage FROM farm_health_report r " +
                "LEFT JOIN farm_health_scan s ON r.scan_id = s.id WHERE r.id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return mapReport(rs);
            }
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error getting report by ID", e);
        }
        return null;
    }

    public int save(FarmHealthReport report) {
        String sql = "INSERT INTO farm_health_report (scan_id, health_score, biodiversity_score, llava_analysis) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, report.getScanId());
            pst.setInt(2, report.getHealthScore());
            pst.setInt(3, report.getBiodiversityScore());
            pst.setString(4, report.getLlavaAnalysis());
            pst.executeUpdate();
            ResultSet keys = pst.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                report.setId(id);
                return id;
            }
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error saving report", e);
        }
        return -1;
    }

    public void update(FarmHealthReport report) {
        String sql = "UPDATE farm_health_report SET scan_id=?, health_score=?, biodiversity_score=?, llava_analysis=? WHERE id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, report.getScanId());
            pst.setInt(2, report.getHealthScore());
            pst.setInt(3, report.getBiodiversityScore());
            pst.setString(4, report.getLlavaAnalysis());
            pst.setInt(5, report.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error updating report", e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM farm_health_report WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error deleting report", e);
        }
    }

    public List<FarmHealthReport> search(String keyword) {
        List<FarmHealthReport> reports = new ArrayList<>();
        String sql = "SELECT r.*, s.crop_type, s.growth_stage FROM farm_health_report r " +
                "LEFT JOIN farm_health_scan s ON r.scan_id = s.id WHERE " +
                "CAST(r.scan_id AS CHAR) LIKE ? OR LOWER(r.llava_analysis) LIKE ? OR LOWER(s.crop_type) LIKE ? " +
                "ORDER BY r.generated_at DESC";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            String pattern = "%" + keyword.toLowerCase() + "%";
            pst.setString(1, pattern);
            pst.setString(2, pattern);
            pst.setString(3, pattern);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                reports.add(mapReport(rs));
            }
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error searching reports", e);
        }
        return reports;
    }

    private FarmHealthReport mapReport(ResultSet rs) throws SQLException {
        FarmHealthReport r = new FarmHealthReport();
        r.setId(rs.getInt("id"));
        r.setScanId(rs.getInt("scan_id"));
        r.setHealthScore(rs.getInt("health_score"));
        r.setBiodiversityScore(rs.getInt("biodiversity_score"));
        r.setLlavaAnalysis(rs.getString("llava_analysis"));
        r.setGeneratedAt(rs.getTimestamp("generated_at"));
        try {
            r.setCropType(rs.getString("crop_type"));
            r.setGrowthStage(rs.getString("growth_stage"));
        } catch (SQLException e) {
            // crop_type/growth_stage might not be in the result set for some queries
        }
        return r;
    }
}
