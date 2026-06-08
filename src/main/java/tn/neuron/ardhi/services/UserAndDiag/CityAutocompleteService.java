package tn.neuron.ardhi.services.UserAndDiag;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;

/**
 * City autocomplete service using the GeoNames free API.
 * Returns real city/municipality names for search queries.
 */
public class CityAutocompleteService {

    private static final String GEONAMES_USERNAME = "hjsadel";
    private static final String API_URL = "http://api.geonames.org/searchJSON";

    /**
     * Searches for cities matching the given query.
     * 
     * @param query   Partial city name (e.g., "Tun")
     * @param maxRows Maximum number of results (default 10)
     * @return List of city descriptions (e.g., "Tunis, Tunis Governorate, Tunisia")
     */
    public List<String> searchCities(String query, int maxRows) {
        List<String> results = new ArrayList<>();
        if (query == null || query.trim().length() < 2) {
            return results;
        }

        try {
            String urlStr = API_URL
                    + "?q=" + URLEncoder.encode(query.trim(), "UTF-8")
                    + "&maxRows=" + maxRows
                    + "&featureClass=P" // Populated places only
                    + "&orderby=relevance"
                    + "&username=" + GEONAMES_USERNAME;

            System.out.println("[CityAutocomplete] Requesting: " + urlStr);

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            System.out.println("[CityAutocomplete] Response code: " + responseCode);

            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String responseBody = response.toString();
                System.out.println("[CityAutocomplete] Response: "
                        + responseBody.substring(0, Math.min(500, responseBody.length())));

                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

                // Check for GeoNames error (e.g. "user does not exist" or "web services not
                // enabled")
                if (json.has("status")) {
                    JsonObject status = json.getAsJsonObject("status");
                    String message = status.has("message") ? status.get("message").getAsString() : "Unknown error";
                    System.err.println("[CityAutocomplete] GeoNames API error: " + message);
                    System.err.println(
                            "[CityAutocomplete] → Go to https://www.geonames.org/manageaccount and enable free web services!");
                    return results;
                }

                JsonArray geonames = json.getAsJsonArray("geonames");

                if (geonames != null) {
                    for (JsonElement elem : geonames) {
                        JsonObject place = elem.getAsJsonObject();
                        String name = place.has("name") ? place.get("name").getAsString() : "";
                        String adminName = place.has("adminName1") ? place.get("adminName1").getAsString() : "";
                        String country = place.has("countryName") ? place.get("countryName").getAsString() : "";

                        StringBuilder label = new StringBuilder(name);
                        if (!adminName.isEmpty())
                            label.append(", ").append(adminName);
                        if (!country.isEmpty())
                            label.append(", ").append(country);

                        results.add(label.toString());
                    }
                    System.out.println("[CityAutocomplete] Found " + results.size() + " results");
                }
            } else {
                BufferedReader errReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                StringBuilder errResponse = new StringBuilder();
                String errLine;
                while ((errLine = errReader.readLine()) != null)
                    errResponse.append(errLine);
                errReader.close();
                System.err.println("[CityAutocomplete] HTTP error " + responseCode + ": " + errResponse);
            }
        } catch (Exception e) {
            System.err.println("[CityAutocomplete] Exception: " + e.getMessage());
            LogUtils.error(CityAutocompleteService.class, "City search failed", e);
        }

        return results;
    }

    /**
     * Searches for cities with default max of 10 results.
     */
    public List<String> searchCities(String query) {
        return searchCities(query, 10);
    }
}
