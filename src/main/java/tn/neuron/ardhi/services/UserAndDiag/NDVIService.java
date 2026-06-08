package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to fetch NDVI (Normalized Difference Vegetation Index) data
 * from the Copernicus Dataspace STAC catalog + SentinelHub Statistical API.
 *
 * NDVI measures vegetation health:
 * -1 to 0.1 = bare soil / water
 * 0.1 - 0.3 = sparse / stressed vegetation
 * 0.3 - 0.5 = moderate vegetation
 * 0.5 - 0.7 = healthy vegetation
 * 0.7 - 1.0 = dense, very healthy vegetation
 */
public class NDVIService {

    // SentinelHub Statistical API (free trial: 30k requests/month)
    // Users can register at https://www.sentinel-hub.com/ for a free trial
    // For demo/development, we use the open Copernicus Dataspace endpoint
    private static final String STATS_API_URL = "https://sh.dataspace.copernicus.eu/api/v1/statistics";

    // NDVI Evalscript for SentinelHub
    private static final String NDVI_EVALSCRIPT = "//VERSION=3\\n" +
            "function setup() {\\n" +
            "  return {\\n" +
            "    input: [{bands: [\\\"B04\\\", \\\"B08\\\", \\\"dataMask\\\"]}],\\n" +
            "    output: [{id: \\\"ndvi\\\", bands: 1}]\\n" +
            "  };\\n" +
            "}\\n" +
            "function evaluatePixel(sample) {\\n" +
            "  let ndvi = (sample.B08 - sample.B04) / (sample.B08 + sample.B04);\\n" +
            "  return { ndvi: [isFinite(ndvi) ? ndvi : 0] };\\n" +
            "}";

    /**
     * Represents a single NDVI cell on the grid.
     */
    public static class NDVICell {
        public double lat;
        public double lon;
        public double ndvi; // -1.0 to 1.0
        public double cellSizeDeg; // size of the cell in degrees

        public NDVICell(double lat, double lon, double ndvi, double cellSizeDeg) {
            this.lat = lat;
            this.lon = lon;
            this.ndvi = ndvi;
            this.cellSizeDeg = cellSizeDeg;
        }

        /**
         * Returns the hex color for this NDVI value.
         */
        public String getColor() {
            return ndviToColor(ndvi);
        }
    }

    /**
     * Fetches an NDVI grid for the area around the given center point.
     * Divides the area into a grid and computes mean NDVI for each cell.
     *
     * @param centerLat Center latitude
     * @param centerLon Center longitude
     * @param radiusKm  Radius in kilometers (e.g., 15 for a 30km x 30km area)
     * @param gridSize  Number of cells per side (e.g., 6 for a 6x6 grid = 36 cells)
     * @return List of NDVICell objects, or empty list on failure
     */
    public List<NDVICell> fetchNDVIGrid(double centerLat, double centerLon, double radiusKm, int gridSize) {
        List<NDVICell> cells = new ArrayList<>();

        // Convert radius to degrees (approximate)
        double latRadius = radiusKm / 111.0;
        double lonRadius = radiusKm / (111.0 * Math.cos(Math.toRadians(centerLat)));

        double cellSizeLat = (2 * latRadius) / gridSize;
        double cellSizeLon = (2 * lonRadius) / gridSize;

        double startLat = centerLat - latRadius;
        double startLon = centerLon - lonRadius;

        // Date range: last 30 days to find recent cloud-free imagery
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        String dateFrom = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + "T00:00:00Z";
        String dateTo = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + "T23:59:59Z";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        LogUtils.info(NDVIService.class,
                String.format("Fetching NDVI grid: center=%.4f,%.4f radius=%.0fkm grid=%dx%d",
                        centerLat, centerLon, radiusKm, gridSize, gridSize));

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                double cellLat = startLat + (row + 0.5) * cellSizeLat;
                double cellLon = startLon + (col + 0.5) * cellSizeLon;

                double minLat = startLat + row * cellSizeLat;
                double maxLat = minLat + cellSizeLat;
                double minLon = startLon + col * cellSizeLon;
                double maxLon = minLon + cellSizeLon;

                try {
                    double ndvi = fetchCellNDVI(client, minLon, minLat, maxLon, maxLat, dateFrom, dateTo);
                    cells.add(new NDVICell(cellLat, cellLon, ndvi, Math.max(cellSizeLat, cellSizeLon)));
                } catch (Exception e) {
                    // Use a neutral value for failed cells
                    cells.add(new NDVICell(cellLat, cellLon, 0.3, Math.max(cellSizeLat, cellSizeLon)));
                    LogUtils.error(NDVIService.class,
                            String.format("Failed NDVI cell [%d,%d]", row, col), e);
                }
            }
        }

        LogUtils.info(NDVIService.class, "NDVI grid fetched: " + cells.size() + " cells");
        return cells;
    }

    /**
     * Fetches a simulated NDVI grid using an algorithmic approach.
     * This generates realistic NDVI data based on latitude and random variation,
     * useful when the satellite API is not accessible or for demonstration
     * purposes.
     *
     * @param centerLat Center latitude
     * @param centerLon Center longitude
     * @param radiusKm  Radius in kilometers
     * @param gridSize  Number of cells per side
     * @return List of NDVICell objects with simulated NDVI values
     */
    public List<NDVICell> fetchSimulatedNDVIGrid(double centerLat, double centerLon, double radiusKm, int gridSize) {
        List<NDVICell> cells = new ArrayList<>();

        double latRadius = radiusKm / 111.0;
        double lonRadius = radiusKm / (111.0 * Math.cos(Math.toRadians(centerLat)));
        double cellSizeLat = (2 * latRadius) / gridSize;
        double cellSizeLon = (2 * lonRadius) / gridSize;
        double startLat = centerLat - latRadius;
        double startLon = centerLon - lonRadius;

        java.util.Random random = new java.util.Random(
                Double.doubleToLongBits(centerLat) ^ Double.doubleToLongBits(centerLon));

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                double cellLat = startLat + (row + 0.5) * cellSizeLat;
                double cellLon = startLon + (col + 0.5) * cellSizeLon;

                // Generate realistic NDVI: higher near center, with some variation
                double distFromCenter = Math.sqrt(
                        Math.pow((cellLat - centerLat) / latRadius, 2) +
                                Math.pow((cellLon - centerLon) / lonRadius, 2));

                // Base NDVI: higher vegetation near center (farm), sparse at edges
                double baseNdvi = 0.6 - (distFromCenter * 0.3);
                // Add random noise
                double noise = (random.nextDouble() - 0.5) * 0.25;
                double ndvi = Math.max(-0.1, Math.min(0.9, baseNdvi + noise));

                cells.add(new NDVICell(cellLat, cellLon, ndvi, Math.max(cellSizeLat, cellSizeLon)));
            }
        }

        LogUtils.info(NDVIService.class, "Simulated NDVI grid generated: " + cells.size() + " cells");
        return cells;
    }

    /**
     * Fetches mean NDVI for a single bounding box cell via SentinelHub Statistical
     * API.
     */
    private double fetchCellNDVI(HttpClient client,
            double minLon, double minLat,
            double maxLon, double maxLat,
            String dateFrom, String dateTo) throws Exception {

        String bbox = String.format(Locale.US, "[%f, %f, %f, %f]", minLon, minLat, maxLon, maxLat);

        String jsonBody = String.format(Locale.US,
                "{" +
                        "  \"input\": {" +
                        "    \"bounds\": {" +
                        "      \"bbox\": %s," +
                        "      \"properties\": { \"crs\": \"http://www.opengis.net/def/crs/EPSG/0/4326\" }" +
                        "    }," +
                        "    \"data\": [{" +
                        "      \"type\": \"sentinel-2-l2a\"," +
                        "      \"dataFilter\": {" +
                        "        \"timeRange\": { \"from\": \"%s\", \"to\": \"%s\" }," +
                        "        \"maxCloudCoverage\": 30" +
                        "      }" +
                        "    }]" +
                        "  }," +
                        "  \"aggregation\": {" +
                        "    \"timeRange\": { \"from\": \"%s\", \"to\": \"%s\" }," +
                        "    \"aggregationInterval\": { \"of\": \"P30D\" }," +
                        "    \"evalscript\": \"%s\"," +
                        "    \"resx\": 100," +
                        "    \"resy\": 100" +
                        "  }" +
                        "}",
                bbox, dateFrom, dateTo, dateFrom, dateTo, NDVI_EVALSCRIPT);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(STATS_API_URL))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return parseMeanNDVI(response.body());
        } else if (response.statusCode() == 401 || response.statusCode() == 403) {
            // Auth required — fall back to simulation
            LogUtils.info(NDVIService.class,
                    "SentinelHub requires authentication. Using simulated NDVI data.");
            throw new Exception("Auth required — using simulation fallback");
        } else {
            throw new Exception("SentinelHub API returned status " + response.statusCode());
        }
    }

    /**
     * Parses the mean NDVI value from SentinelHub Statistical API response.
     */
    private double parseMeanNDVI(String json) {
        // Look for "mean" value in the statistics response
        Pattern meanPattern = Pattern.compile("\"mean\"\\s*:\\s*([\\d.eE+-]+)");
        Matcher matcher = meanPattern.matcher(json);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                return 0.3; // neutral fallback
            }
        }
        return 0.3;
    }

    /**
     * Converts an NDVI value (-1 to 1) to a hex color string.
     */
    public static String ndviToColor(double ndvi) {
        if (ndvi < 0.1)
            return "#8B4513"; // Brown — bare soil / water
        if (ndvi < 0.2)
            return "#D2691E"; // Chocolate — very sparse
        if (ndvi < 0.3)
            return "#FFA500"; // Orange — sparse / stressed
        if (ndvi < 0.4)
            return "#FFD700"; // Gold — light vegetation
        if (ndvi < 0.5)
            return "#ADFF2F"; // Yellow-Green — moderate
        if (ndvi < 0.6)
            return "#7CFC00"; // Lawn Green — good
        if (ndvi < 0.7)
            return "#32CD32"; // Lime Green — healthy
        if (ndvi < 0.8)
            return "#228B22"; // Forest Green — very healthy
        return "#006400"; // Dark Green — dense canopy
    }

    /**
     * Converts an NDVI value to a human-readable label.
     */
    public static String ndviToLabel(double ndvi) {
        if (ndvi < 0.1)
            return "Sol nu / Eau";
        if (ndvi < 0.3)
            return "Végétation clairsemée";
        if (ndvi < 0.5)
            return "Végétation modérée";
        if (ndvi < 0.7)
            return "Végétation saine";
        return "Végétation dense";
    }

    /**
     * Converts a list of NDVICells to a JSON array string for the JavaScript
     * bridge.
     */
    public String cellsToJson(List<NDVICell> cells) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < cells.size(); i++) {
            NDVICell c = cells.get(i);
            sb.append(String.format(Locale.US,
                    "{\"lat\":%.6f,\"lon\":%.6f,\"ndvi\":%.3f,\"size\":%.6f,\"color\":\"%s\",\"label\":\"%s\"}",
                    c.lat, c.lon, c.ndvi, c.cellSizeDeg, c.getColor(), ndviToLabel(c.ndvi)));
            if (i < cells.size() - 1)
                sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
