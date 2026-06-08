package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Cross-Farm Epidemic Intelligence Network service.
 * Aggregates anonymized diagnostic data across all users within a geographic
 * region
 * to generate early-warning alerts and regional disease dashboards.
 */
public class EpidemicAlertService {

    private Connection cnx = MyDatabase.getInstance().getCnx();

    // ─── Data Classes ───

    /**
     * A disease currently active in the user's region.
     */
    public static class RegionalDisease {
        public String diseaseName;
        public int reportCount;
        public double nearestDistanceKm;
        public String severityLevel; // "Élevé", "Modéré", "Faible"

        public RegionalDisease(String diseaseName, int reportCount, double nearestDistanceKm) {
            this.diseaseName = diseaseName;
            this.reportCount = reportCount;
            this.nearestDistanceKm = nearestDistanceKm;
            // Determine alert level based on report count
            if (reportCount >= 10) {
                this.severityLevel = "Élevé";
            } else if (reportCount >= 3) {
                this.severityLevel = "Modéré";
            } else {
                this.severityLevel = "Faible";
            }
        }

        public String getIcon() {
            if ("Élevé".equals(severityLevel))
                return "🔴";
            if ("Modéré".equals(severityLevel))
                return "🟡";
            return "🟢";
        }
    }

    /**
     * A regional epidemic alert for a specific disease.
     */
    public static class RegionalAlert {
        public String diseaseName;
        public int reportCount;
        public double nearestDistanceKm;
        public int daysActive;
        public String message;

        public RegionalAlert(String diseaseName, int reportCount, double nearestDistanceKm, int daysActive) {
            this.diseaseName = diseaseName;
            this.reportCount = reportCount;
            this.nearestDistanceKm = nearestDistanceKm;
            this.daysActive = daysActive;
            this.message = buildAlertMessage();
        }

        private String buildAlertMessage() {
            String distStr = nearestDistanceKm < 1
                    ? String.format("%.0fm", nearestDistanceKm * 1000)
                    : String.format("%.1fkm", nearestDistanceKm);
            return String.format(
                    "⚠️ %s détecté à %s de votre position. %d agriculteur(s) l'ont signalé ces %d derniers jours.",
                    diseaseName, distStr, reportCount, daysActive);
        }
    }

    /**
     * Seasonal pattern data for a disease.
     */
    public static class SeasonalPattern {
        public String diseaseName;
        public int peakMonth; // 1-12
        public int peakCount;
        public boolean isInPeakWindow;
        public String warning;
    }

    // ─── Core Methods ───

    /**
     * Get alerts for diseases detected near the user's location in the last N days.
     * Only includes diseases with at least 2 reports (to avoid noise).
     */
    public List<RegionalAlert> getRegionalAlerts(double lat, double lon, double radiusKm, int lastDays) {
        List<RegionalAlert> alerts = new ArrayList<>();
        // Haversine formula in SQL to compute distance in km
        String sql = "SELECT " +
                "SUBSTRING_INDEX(resultat_ia, ' - ', -1) AS disease_name, " +
                "COUNT(*) AS report_count, " +
                "MIN(6371 * ACOS(LEAST(1.0, COS(RADIANS(?)) * COS(RADIANS(latitude)) * COS(RADIANS(longitude) - RADIANS(?)) "
                +
                "+ SIN(RADIANS(?)) * SIN(RADIANS(latitude))))) AS nearest_km, " +
                "DATEDIFF(NOW(), MIN(date_scan)) AS days_active " +
                "FROM diagnostic " +
                "WHERE latitude IS NOT NULL AND longitude IS NOT NULL " +
                "AND date_scan >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                "AND 6371 * ACOS(LEAST(1.0, COS(RADIANS(?)) * COS(RADIANS(latitude)) * COS(RADIANS(longitude) - RADIANS(?)) "
                +
                "+ SIN(RADIANS(?)) * SIN(RADIANS(latitude)))) <= ? " +
                "AND resultat_ia IS NOT NULL AND resultat_ia != '' " +
                "GROUP BY disease_name " +
                "HAVING report_count >= 2 " +
                "ORDER BY report_count DESC " +
                "LIMIT 10";

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setDouble(1, lat);
            pst.setDouble(2, lon);
            pst.setDouble(3, lat);
            pst.setInt(4, lastDays);
            pst.setDouble(5, lat);
            pst.setDouble(6, lon);
            pst.setDouble(7, lat);
            pst.setDouble(8, radiusKm);

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String diseaseName = cleanDiseaseName(rs.getString("disease_name"));
                int count = rs.getInt("report_count");
                double nearestKm = rs.getDouble("nearest_km");
                int daysActive = rs.getInt("days_active");
                alerts.add(new RegionalAlert(diseaseName, count, nearestKm, Math.max(1, lastDays - daysActive)));
            }
        } catch (SQLException e) {
            LogUtils.error(EpidemicAlertService.class, "Error fetching regional alerts", e);
        }
        return alerts;
    }

    /**
     * Get a dashboard of currently active diseases in the user's region.
     */
    public List<RegionalDisease> getActiveDiseases(double lat, double lon, double radiusKm) {
        List<RegionalDisease> diseases = new ArrayList<>();
        String sql = "SELECT " +
                "SUBSTRING_INDEX(resultat_ia, ' - ', -1) AS disease_name, " +
                "COUNT(*) AS report_count, " +
                "MIN(6371 * ACOS(LEAST(1.0, COS(RADIANS(?)) * COS(RADIANS(latitude)) * COS(RADIANS(longitude) - RADIANS(?)) "
                +
                "+ SIN(RADIANS(?)) * SIN(RADIANS(latitude))))) AS nearest_km " +
                "FROM diagnostic " +
                "WHERE latitude IS NOT NULL AND longitude IS NOT NULL " +
                "AND date_scan >= DATE_SUB(NOW(), INTERVAL 14 DAY) " +
                "AND 6371 * ACOS(LEAST(1.0, COS(RADIANS(?)) * COS(RADIANS(latitude)) * COS(RADIANS(longitude) - RADIANS(?)) "
                +
                "+ SIN(RADIANS(?)) * SIN(RADIANS(latitude)))) <= ? " +
                "AND resultat_ia IS NOT NULL AND resultat_ia != '' " +
                "AND resultat_ia NOT LIKE '%Healthy%' AND resultat_ia NOT LIKE '%Sain%' " +
                "GROUP BY disease_name " +
                "ORDER BY report_count DESC " +
                "LIMIT 8";

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setDouble(1, lat);
            pst.setDouble(2, lon);
            pst.setDouble(3, lat);
            pst.setDouble(4, lat);
            pst.setDouble(5, lon);
            pst.setDouble(6, lat);
            pst.setDouble(7, radiusKm);

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String name = cleanDiseaseName(rs.getString("disease_name"));
                int count = rs.getInt("report_count");
                double nearKm = rs.getDouble("nearest_km");
                diseases.add(new RegionalDisease(name, count, nearKm));
            }
        } catch (SQLException e) {
            LogUtils.error(EpidemicAlertService.class, "Error fetching active diseases", e);
        }
        return diseases;
    }

    /**
     * Detects seasonal patterns for a disease from historical data.
     * Returns a warning if the current month is historically a peak period.
     */
    public SeasonalPattern getSeasonalPattern(String diseaseName, double lat, double lon, double radiusKm) {
        SeasonalPattern pattern = new SeasonalPattern();
        pattern.diseaseName = diseaseName;

        String sql = "SELECT MONTH(date_scan) AS m, COUNT(*) AS cnt " +
                "FROM diagnostic " +
                "WHERE latitude IS NOT NULL AND longitude IS NOT NULL " +
                "AND 6371 * ACOS(LEAST(1.0, COS(RADIANS(?)) * COS(RADIANS(latitude)) * COS(RADIANS(longitude) - RADIANS(?)) "
                +
                "+ SIN(RADIANS(?)) * SIN(RADIANS(latitude)))) <= ? " +
                "AND resultat_ia LIKE ? " +
                "GROUP BY m ORDER BY cnt DESC LIMIT 1";

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setDouble(1, lat);
            pst.setDouble(2, lon);
            pst.setDouble(3, lat);
            pst.setDouble(4, radiusKm);
            pst.setString(5, "%" + diseaseName + "%");

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                pattern.peakMonth = rs.getInt("m");
                pattern.peakCount = rs.getInt("cnt");

                // Check if current month is near the peak
                int currentMonth = java.time.LocalDate.now().getMonthValue();
                pattern.isInPeakWindow = Math.abs(currentMonth - pattern.peakMonth) <= 1
                        || Math.abs(currentMonth - pattern.peakMonth) >= 11; // Handle Dec-Jan wrap

                if (pattern.isInPeakWindow && pattern.peakCount >= 3) {
                    String[] monthNames = { "", "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre" };
                    pattern.warning = String.format(
                            "📅 %s connaît historiquement un pic en %s dans votre région — vous entrez dans cette période.",
                            diseaseName, monthNames[pattern.peakMonth]);
                }
            }
        } catch (SQLException e) {
            LogUtils.error(EpidemicAlertService.class, "Error fetching seasonal patterns", e);
        }
        return pattern;
    }

    /**
     * Gets the total count of distinct diseases and total reports in the region
     * recently.
     */
    public int[] getRegionalStats(double lat, double lon, double radiusKm, int lastDays) {
        String sql = "SELECT COUNT(DISTINCT SUBSTRING_INDEX(resultat_ia, ' - ', -1)) AS disease_count, " +
                "COUNT(*) AS total_reports " +
                "FROM diagnostic " +
                "WHERE latitude IS NOT NULL AND longitude IS NOT NULL " +
                "AND date_scan >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                "AND 6371 * ACOS(LEAST(1.0, COS(RADIANS(?)) * COS(RADIANS(latitude)) * COS(RADIANS(longitude) - RADIANS(?)) "
                +
                "+ SIN(RADIANS(?)) * SIN(RADIANS(latitude)))) <= ? " +
                "AND resultat_ia IS NOT NULL AND resultat_ia != ''";

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, lastDays);
            pst.setDouble(2, lat);
            pst.setDouble(3, lon);
            pst.setDouble(4, lat);
            pst.setDouble(5, radiusKm);

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return new int[] { rs.getInt("disease_count"), rs.getInt("total_reports") };
            }
        } catch (SQLException e) {
            LogUtils.error(EpidemicAlertService.class, "Error fetching regional stats", e);
        }
        return new int[] { 0, 0 };
    }

    /**
     * Cleans the disease name extracted from resultat_ia (e.g. "Tomato - Late
     * Blight" → "Late Blight").
     */
    private String cleanDiseaseName(String raw) {
        if (raw == null)
            return "Maladie inconnue";
        raw = raw.trim();
        // Remove leading/trailing underscores and replace with spaces
        raw = raw.replace("_", " ");
        if (raw.isEmpty())
            return "Maladie inconnue";
        return raw;
    }
}
