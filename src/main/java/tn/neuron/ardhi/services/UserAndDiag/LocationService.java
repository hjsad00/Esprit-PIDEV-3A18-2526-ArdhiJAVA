package tn.neuron.ardhi.services.UserAndDiag;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;

/**
 * Service for detecting user location via IP geolocation.
 * Uses free ip-api.com service for automatic location detection during
 * diagnostic scans.
 */
public class LocationService {

    // Free tier: 45 requests/minute, no API key needed
    private static final String API_URL = "http://ip-api.com/json/?fields=status,lat,lon,city,regionName,country";

    /**
     * Location data holder.
     */
    public static class LocationData {
        public final Double latitude;
        public final Double longitude;
        public final String label;

        public LocationData(Double latitude, Double longitude, String label) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.label = label;
        }
    }

    /**
     * Detects current location based on IP address.
     * Uses ip-api.com free service.
     * 
     * @return LocationData with coordinates and label, or null if detection fails
     */
    public LocationData detectLocation() {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String json = response.toString();

                // Check status
                if (!json.contains("\"status\":\"success\"")) {
                    return null;
                }

                // Parse values using regex (avoids external JSON library dependency)
                Double lat = parseDouble(json, "lat");
                Double lon = parseDouble(json, "lon");
                String city = parseString(json, "city");
                String region = parseString(json, "regionName");
                String country = parseString(json, "country");

                if (lat == null || lon == null) {
                    return null;
                }

                // Build label like "Tunis, Tunis Governorate, Tunisia"
                StringBuilder labelBuilder = new StringBuilder();
                if (city != null && !city.isEmpty())
                    labelBuilder.append(city);
                if (region != null && !region.isEmpty()) {
                    if (labelBuilder.length() > 0)
                        labelBuilder.append(", ");
                    labelBuilder.append(region);
                }
                if (country != null && !country.isEmpty()) {
                    if (labelBuilder.length() > 0)
                        labelBuilder.append(", ");
                    labelBuilder.append(country);
                }

                return new LocationData(lat, lon, labelBuilder.toString());
            }
        } catch (Exception e) {
            LogUtils.error(LocationService.class, "Failed to detect location", e);
        }
        return null;
    }

    private Double parseDouble(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*([\\d.-]+)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private String parseString(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
