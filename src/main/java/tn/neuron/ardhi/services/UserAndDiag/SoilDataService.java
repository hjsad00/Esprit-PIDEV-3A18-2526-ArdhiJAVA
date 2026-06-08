package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to fetch soil data from the ISRIC SoilGrids REST API.
 */
public class SoilDataService {

    private static final String SOILGRIDS_API_URL = "https://rest.isric.org/soilgrids/v2.0/properties/query";

    public static class SoilLayer {
        public String depthLabel; // e.g., "0-5cm", "5-15cm"
        public Double phh2o; // pH
        public Double nitrogen; // cg/kg
        public Double sand; // g/kg
        public Double clay; // g/kg
        public Double cec; // Cation Exchange Capacity mmol(c)/kg

        public SoilLayer(String depthLabel) {
            this.depthLabel = depthLabel;
        }
    }

    /**
     * Fetches soil properties for a given latitude and longitude.
     * Returns a list of SoilLayer objects, representing different depths.
     */
    public List<SoilLayer> fetchSoilData(double lat, double lon) {
        String url = String.format(java.util.Locale.US,
                "%s?lat=%f&lon=%f&property=phh2o&property=nitrogen&property=sand&property=clay&property=cec&depth=0-5cm&depth=5-15cm&depth=15-30cm&depth=30-60cm&depth=60-100cm&depth=100-200cm&value=mean",
                SOILGRIDS_API_URL, lat, lon);

        List<SoilLayer> layers = new ArrayList<>();
        Map<String, SoilLayer> layerMap = new HashMap<>();

        // Initialize layers
        String[] depths = { "0-5cm", "5-15cm", "15-30cm", "30-60cm", "60-100cm", "100-200cm" };
        for (String depth : depths) {
            SoilLayer layer = new SoilLayer(depth);
            layers.add(layer);
            layerMap.put(depth, layer);
        }

        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("Accept", "application/json")
                    .timeout(java.time.Duration.ofSeconds(15))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String jsonResponse = response.body();

                String[] properties = { "phh2o", "nitrogen", "sand", "clay", "cec" };

                for (String propName : properties) {
                    for (String depthLabel : depths) {
                        Double meanValue = extractMeanValue(jsonResponse, propName, depthLabel);

                        if (meanValue != null) {
                            SoilLayer layer = layerMap.get(depthLabel);
                            if (layer != null) {
                                switch (propName) {
                                    case "phh2o":
                                        layer.phh2o = meanValue / 10.0;
                                        break;
                                    case "nitrogen":
                                        layer.nitrogen = meanValue;
                                        break;
                                    case "sand":
                                        layer.sand = meanValue / 10.0;
                                        break;
                                    case "clay":
                                        layer.clay = meanValue / 10.0;
                                        break;
                                    case "cec":
                                        layer.cec = meanValue / 10.0;
                                        break;
                                }
                            }
                        }
                    }
                }
            } else {
                LogUtils.error(SoilDataService.class,
                        "Failed to fetch soil data. Status: " + response.statusCode(), null);
            }
        } catch (Exception e) {
            LogUtils.error(SoilDataService.class, "Error fetching soil data from ISRIC", e);
        }

        // Handle missing data by propagating from layer above if needed to prevent
        // nulls in UI
        for (int i = 1; i < layers.size(); i++) {
            SoilLayer current = layers.get(i);
            SoilLayer prev = layers.get(i - 1);
            if (current.phh2o == null && prev.phh2o != null)
                current.phh2o = prev.phh2o;
            if (current.nitrogen == null && prev.nitrogen != null)
                current.nitrogen = prev.nitrogen;
            if (current.sand == null && prev.sand != null)
                current.sand = prev.sand;
            if (current.clay == null && prev.clay != null)
                current.clay = prev.clay;
            if (current.cec == null && prev.cec != null)
                current.cec = prev.cec;
        }

        return layers;
    }

    private Double extractMeanValue(String json, String property, String depthLabel) {
        String propBlockRegex = "\"name\":\"" + property + "\".*?(?=\"name\":|$)";
        Pattern propPattern = Pattern.compile(propBlockRegex);
        Matcher propMatcher = propPattern.matcher(json);
        if (propMatcher.find()) {
            String propBlock = propMatcher.group();
            String depthRegex = "\"label\":\"" + depthLabel + "\",\"values\":\\{\"mean\":(null|[\\d\\.-]+)\\}";
            Pattern depthPattern = Pattern.compile(depthRegex);
            Matcher depthMatcher = depthPattern.matcher(propBlock);
            if (depthMatcher.find()) {
                String val = depthMatcher.group(1);
                if (!"null".equals(val)) {
                    try {
                        return Double.parseDouble(val);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }
}
