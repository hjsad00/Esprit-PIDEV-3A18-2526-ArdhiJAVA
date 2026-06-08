package tn.neuron.ardhi.services.Parcelle_Cultures;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Service de géolocalisation utilisant l'API Nominatim d'OpenStreetMap.
 * Gratuit, sans clé API.
 * Documentation: https://nominatim.openstreetmap.org/
 */
public class LocationService {

    private static final Logger LOGGER = Logger.getLogger(LocationService.class.getName());

    private static final String NOMINATIM_BASE =
            "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=";

    private static final String NOMINATIM_REVERSE =
            "https://nominatim.openstreetmap.org/reverse?format=json&lat=%s&lon=%s";

    /**
     * Résultat du géocodage
     */
    public static class GeocodageResult {
        private double latitude;
        private double longitude;
        private String adresse;
        private boolean found;

        public GeocodageResult() {}

        public GeocodageResult(double lat, double lon, String adresse) {
            this.latitude = lat;
            this.longitude = lon;
            this.adresse = adresse;
            this.found = true;
        }

        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public String getAdresse() { return adresse; }
        public boolean isFound() { return found; }
    }

    /**
     * Geocode un nom de lieu en coordonnées GPS via Nominatim.
     *
     * @param nomLieu Nom du lieu (ex: "Tunis, Tunisie")
     * @return GeocodageResult avec lat/lng ou found=false
     */
    public GeocodageResult geocoder(String nomLieu) {
        try {
            String query = URLEncoder.encode(nomLieu + ", Tunisie", StandardCharsets.UTF_8);
            String urlStr = NOMINATIM_BASE + query;

            LOGGER.info("Géocodage: " + nomLieu);

            String json = httpGet(urlStr);

            if (json != null && json.startsWith("[") && json.length() > 10) {
                // Parse premier résultat du tableau JSON
                double lat = extractDoubleFromArray(json, "\"lat\":\"");
                double lon = extractDoubleFromArray(json, "\"lon\":\"");
                String displayName = extractStringFromArray(json, "\"display_name\":\"");

                if (lat != 0 || lon != 0) {
                    LOGGER.info("Géocodage réussi: " + nomLieu + " → " + lat + "," + lon);
                    return new GeocodageResult(lat, lon, displayName);
                }
            }

        } catch (Exception e) {
            LOGGER.warning("Erreur géocodage '" + nomLieu + "': " + e.getMessage());
        }

        // Fallback: coordonnées de Tunis
        LOGGER.warning("Géocodage échoué pour '" + nomLieu + "' → fallback Tunis");
        return new GeocodageResult(36.8065, 10.1815, "Tunis (par défaut)");
    }

    /**
     * Géocodage inverse: coordonnées → nom du lieu
     *
     * @param latitude  Latitude
     * @param longitude Longitude
     * @return Nom du lieu ou null
     */
    public String reverseGeocode(double latitude, double longitude) {
        try {
            String urlStr = String.format(NOMINATIM_REVERSE,
                    String.valueOf(latitude).replace(',', '.'),
                    String.valueOf(longitude).replace(',', '.'));

            String json = httpGet(urlStr);
            if (json != null && json.contains("display_name")) {
                return extractStringFromArray(json, "\"display_name\":\"");
            }
        } catch (Exception e) {
            LOGGER.warning("Erreur reverse geocodage: " + e.getMessage());
        }
        return null;
    }

    /**
     * Calcule la distance (km) entre deux points GPS via formule de Haversine.
     */
    public double calculerDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // Rayon Terre en km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    private String httpGet(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            // Nominatim requiert un User-Agent valide
            conn.setRequestProperty("User-Agent", "Ardhi-Agricultural-App/1.0 (educational project)");
            conn.setRequestProperty("Accept-Language", "fr");

            int code = conn.getResponseCode();
            if (code == 200) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                conn.disconnect();
                return sb.toString();
            }
            conn.disconnect();
        } catch (Exception e) {
            LOGGER.warning("HTTP GET failed: " + e.getMessage());
        }
        return null;
    }

    /** Extrait une valeur numérique après une clé (valeur entre guillemets) */
    private double extractDoubleFromArray(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return 0.0;
        int start = idx + key.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return 0.0;
        try {
            return Double.parseDouble(json.substring(start, end));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /** Extrait une valeur chaîne après une clé JSON */
    private String extractStringFromArray(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return "";
        int start = idx + key.length();
        // Chercher la fin (guillemet non échappé)
        StringBuilder result = new StringBuilder();
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                i += 2; // sauter l'échappement
                continue;
            }
            if (c == '"') break;
            result.append(c);
            i++;
        }
        return result.toString();
    }
}
