package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.scene.control.Button;
import tn.neuron.ardhi.models.UserAndDiag.Diagnostic;
import tn.neuron.ardhi.services.UserAndDiag.DiagnosticService;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;
import tn.neuron.ardhi.services.UserAndDiag.EpidemicAlertService;
import tn.neuron.ardhi.services.UserAndDiag.LocationService;
import tn.neuron.ardhi.services.UserAndDiag.NDVIService;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import netscape.javascript.JSObject;
import tn.neuron.ardhi.services.UserAndDiag.SporeCastService;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Random;

/**
 * Controller for the Disease Map view.
 * Displays diagnostics with location data on a Leaflet map.
 */
public class DiseaseMapController implements Initializable {

    @FXML
    private WebView webView;

    @FXML
    private Label lblStatus;

    @FXML
    private VBox regionalSummaryContainer;

    @FXML
    private Button btnNDVI;

    private WebEngine webEngine;
    private final DiagnosticService diagnosticService = new DiagnosticService();
    private final EpidemicAlertService epidemicService = new EpidemicAlertService();
    private final LocationService locationService = new LocationService();
    private final NDVIService ndviService = new NDVIService();
    private final JavaBridge javaBridge = new JavaBridge();
    private boolean ndviOverlayActive = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        webEngine = webView.getEngine();

        // Load the Leaflet map HTML
        URL mapUrl = getClass().getResource("/html/map.html");
        if (mapUrl != null) {
            webEngine.load(mapUrl.toExternalForm());

            // Wait for the page to load, then populate markers
            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    JSObject window = (JSObject) webEngine.executeScript("window");
                    window.setMember("app", javaBridge);
                    Platform.runLater(this::loadMarkers);
                } else if (newState == Worker.State.FAILED) {
                    lblStatus.setText("Erreur de chargement de la carte");
                    lblStatus.setStyle("-fx-text-fill: #c0392b;");
                }
            });
        } else {
            lblStatus.setText("Fichier map.html introuvable");
            lblStatus.setStyle("-fx-text-fill: #c0392b;");
            LogUtils.error(DiseaseMapController.class, "map.html not found in resources");
        }

        loadRegionalSummary();
    }

    /**
     * Loads diagnostic markers onto the map.
     * Only diagnostics with valid location data are displayed.
     */
    private void loadMarkers() {
        try {
            List<Diagnostic> diagnostics = diagnosticService.recupererAvecLocation();

            int markerCount = 0;
            List<String> usedLocations = new ArrayList<>();
            Random random = new Random();

            for (Diagnostic d : diagnostics) {
                if (d.getLatitude() != null && d.getLongitude() != null) {
                    double lat = d.getLatitude();
                    double lon = d.getLongitude();

                    // Simple collision detection string key
                    // We use a rough precision check or just exact match
                    // Since floating points can be tricky, we'll check allowing for very small
                    // differences?
                    // No, usually the issue is EXACT overlap.

                    // However, let's just use the Jitter approach for EXACT overlaps which is the
                    // main issue
                    // We will check if this exact location key is already used

                    boolean collision = false;
                    for (String loc : usedLocations) {
                        // Check distance is extremely small (virtually identical)
                        String[] parts = loc.split(",");
                        double uLat = Double.parseDouble(parts[0]);
                        double uLon = Double.parseDouble(parts[1]);

                        if (Math.abs(lat - uLat) < 0.00001 && Math.abs(lon - uLon) < 0.00001) {
                            collision = true;
                            break;
                        }
                    }

                    if (collision) {
                        // Apply Jitter: +/- 0.0003 degrees (approx 30m)
                        lat += (random.nextDouble() - 0.5) * 0.0006;
                        lon += (random.nextDouble() - 0.5) * 0.0006;
                    }

                    usedLocations.add(lat + "," + lon);

                    addMarkerToMap(d, lat, lon);
                    markerCount++;
                }
            }

            // Auto-zoom to fit all markers
            if (markerCount > 0) {
                try {
                    webEngine.executeScript("fitAllMarkers();");
                } catch (Exception e) {
                    LogUtils.error(DiseaseMapController.class, "Error calling fitAllMarkers", e);
                }
                lblStatus.setText(markerCount + " diagnostic(s) affiché(s) sur la carte");
                lblStatus.setStyle("-fx-text-fill: #27ae60;");
            } else {
                lblStatus.setText("Aucun diagnostic avec localisation disponible");
                lblStatus.setStyle("-fx-text-fill: #f39c12;");
            }

        } catch (Exception e) {
            LogUtils.error(DiseaseMapController.class, "Error loading markers", e);
            lblStatus.setText("Erreur lors du chargement des données");
            lblStatus.setStyle("-fx-text-fill: #c0392b;");
        }
    }

    /**
     * Adds a single diagnostic as a marker on the Leaflet map.
     */
    /**
     * Adds a single diagnostic as a marker on the Leaflet map.
     */
    private void addMarkerToMap(Diagnostic d, double lat, double lon) {
        try {
            // Use stored severity from AI analysis, with fallback
            String severity = d.getSeverityAsString();
            if (severity == null || severity.isEmpty()) {
                // Fallback based on confidence
                if (d.getConfiance() >= 80) {
                    severity = "CRITICAL";
                } else if (d.getConfiance() >= 50) {
                    severity = "MEDIUM";
                } else {
                    severity = "LOW";
                }
            }

            // Extract disease name from resultatIA (format: "Plant - Disease")
            String title = d.getResultatIA();
            if (title != null && title.contains("-")) {
                String[] parts = title.split("-");
                if (parts.length > 1) {
                    title = parts[1].trim();
                }
            }
            if (title == null || title.isEmpty()) {
                title = "Diagnostic #" + d.getId();
            }

            // Build description
            String description = String.format(
                    "Confiance: %.1f%%<br>Date: %s<br>%s",
                    d.getConfiance(),
                    d.getDateScan() != null ? d.getDateScan().toString().substring(0, 10) : "N/A",
                    d.getLocationLabel() != null ? d.getLocationLabel() : "");

            // Escape single quotes for JavaScript
            title = title.replace("'", "\\'");
            description = description.replace("'", "\\'");

            // Call JavaScript function
            String script = String.format(java.util.Locale.US,
                    "addMarker(%f, %f, '%s', '%s', '%s');",
                    lat,
                    lon,
                    title,
                    description,
                    severity);

            webEngine.executeScript(script);

        } catch (Exception e) {
            LogUtils.error(DiseaseMapController.class, "Error adding marker for diagnostic " + d.getId(), e);
        }
    }

    @FXML
    void refreshMap(ActionEvent event) {
        lblStatus.setText("Actualisation...");
        lblStatus.setStyle("-fx-text-fill: #888888;");

        // Reload the map
        URL mapUrl = getClass().getResource("/html/map.html");
        if (mapUrl != null) {
            webEngine.load(mapUrl.toExternalForm());
            // The listener added in initialize() will auto-re-register the 'app' bridge and
            // reload markers
        }
    }

    @FXML
    void goBack(ActionEvent event) {
        WindowUtils.goBack(event);
    }

    /**
     * Toggles the NDVI satellite vegetation overlay on the map.
     */
    @FXML
    void toggleNDVI(ActionEvent event) {
        if (ndviOverlayActive) {
            // Remove overlay
            try {
                webEngine.executeScript("removeNDVIOverlay();");
            } catch (Exception e) {
                LogUtils.error(DiseaseMapController.class, "Error removing NDVI overlay", e);
            }
            ndviOverlayActive = false;
            if (btnNDVI != null) {
                btnNDVI.setText("🛰️ NDVI Satellite");
                btnNDVI.setStyle(
                        "-fx-background-color: linear-gradient(to right, #2980b9, #1a5276); -fx-background-radius: 20; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 1); -fx-padding: 8 20;");
            }
            lblStatus.setText("Couche NDVI désactivée");
            lblStatus.setStyle("-fx-text-fill: #7f8c8d;");
            return;
        }

        // Show overlay — fetch data in background
        lblStatus.setText("🛰️ Chargement des données satellite...");
        lblStatus.setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;");
        if (btnNDVI != null) {
            btnNDVI.setDisable(true);
        }

        new Thread(() -> {
            try {
                // Detect user location
                double lat = 36.8065; // Default Tunis
                double lon = 10.1815;
                LocationService.LocationData loc = locationService.detectLocation();
                if (loc != null && loc.latitude != null) {
                    lat = loc.latitude;
                    lon = loc.longitude;
                }

                // Try real API first, fall back to simulated data
                List<NDVIService.NDVICell> cells;
                try {
                    cells = ndviService.fetchNDVIGrid(lat, lon, 15.0, 6);
                } catch (Exception e) {
                    LogUtils.info(DiseaseMapController.class, "Falling back to simulated NDVI data");
                    cells = ndviService.fetchSimulatedNDVIGrid(lat, lon, 15.0, 6);
                }

                String json = ndviService.cellsToJson(cells);

                Platform.runLater(() -> {
                    try {
                        webEngine.executeScript("showNDVIOverlay('" + json.replace("'", "\\'") + "');");
                        ndviOverlayActive = true;
                        if (btnNDVI != null) {
                            btnNDVI.setText("❌ Masquer NDVI");
                            btnNDVI.setStyle(
                                    "-fx-background-color: linear-gradient(to right, #c0392b, #922b21); -fx-background-radius: 20; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 1); -fx-padding: 8 20;");
                            btnNDVI.setDisable(false);
                        }
                        lblStatus.setText("🛰️ Couche NDVI active — cliquez sur une zone pour plus de détails");
                        lblStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } catch (Exception e) {
                        LogUtils.error(DiseaseMapController.class, "Error showing NDVI overlay", e);
                        lblStatus.setText("Erreur d'affichage NDVI");
                        lblStatus.setStyle("-fx-text-fill: #c0392b;");
                        if (btnNDVI != null)
                            btnNDVI.setDisable(false);
                    }
                });
            } catch (Exception e) {
                LogUtils.error(DiseaseMapController.class, "Error fetching NDVI data", e);
                Platform.runLater(() -> {
                    lblStatus.setText("Impossible de charger les données satellite");
                    lblStatus.setStyle("-fx-text-fill: #c0392b;");
                    if (btnNDVI != null)
                        btnNDVI.setDisable(false);
                });
            }
        }).start();
    }

    /**
     * Loads regional disease summary into the sidebar.
     */
    private void loadRegionalSummary() {
        new Thread(() -> {
            try {
                // Approximate location (Tunis default if detection fails to avoid blocking)
                double lat = 36.8065;
                double lon = 10.1815;
                LocationService.LocationData loc = locationService.detectLocation();
                if (loc != null && loc.latitude != null) {
                    lat = loc.latitude;
                    lon = loc.longitude;
                }

                List<EpidemicAlertService.RegionalDisease> diseases = epidemicService.getActiveDiseases(lat, lon, 50.0);

                Platform.runLater(() -> {
                    if (regionalSummaryContainer == null)
                        return;
                    regionalSummaryContainer.getChildren().clear();

                    if (diseases.isEmpty()) {
                        Label placeholder = new Label("Aucune maladie signalée récemment dans votre région.");
                        placeholder.setWrapText(true);
                        placeholder.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic; -fx-font-size: 11;");
                        regionalSummaryContainer.getChildren().add(placeholder);
                        return;
                    }

                    for (EpidemicAlertService.RegionalDisease d : diseases) {
                        HBox row = new HBox(8);
                        row.setAlignment(Pos.CENTER_LEFT);
                        row.setStyle(
                                "-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 3, 0, 0, 1);");

                        Label icon = new Label(d.getIcon());
                        Label name = new Label(d.diseaseName);
                        name.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 12;");
                        name.setPrefWidth(120);

                        Label count = new Label(d.reportCount + " cas");
                        count.setStyle(
                                "-fx-text-fill: white; -fx-background-color: #95a5a6; -fx-background-radius: 10; -fx-padding: 2 6; -fx-font-size: 10;");

                        row.getChildren().addAll(icon, name, count);
                        regionalSummaryContainer.getChildren().add(row);
                    }
                });

            } catch (Exception e) {
                LogUtils.error(DiseaseMapController.class, "Error loading regional summary", e);
            }
        }).start();
    }

    /**
     * Bridge class to allow JavaScript to call Java methods.
     */
    public class JavaBridge {
        public void evaluateMarkerSporeCast(double lat, double lon, String diseaseName) {
            new Thread(() -> {
                try {
                    SporeCastService.SporeCastResult result = new SporeCastService().analyzeSourceLocation(lat, lon,
                            diseaseName);

                    if (result.isContagious()) {
                        Platform.runLater(() -> {
                            String script = String.format(java.util.Locale.US,
                                    "drawSporeCastCone(%f, %f, %f, %f, '%s');",
                                    lat, lon, result.getTravelAngle(), result.getBlastRadiusKm(),
                                    result.getWindDirectionLabel());
                            try {
                                webEngine.executeScript(script);
                                lblStatus.setText(
                                        " Radar SporeCast: Vents critiques vers " + result.getWindDirectionLabel());
                                lblStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                            } catch (Exception e) {
                                LogUtils.error(DiseaseMapController.class, "Error executing SporeCast drawing script",
                                        e);
                            }
                        });
                    } else {
                        Platform.runLater(() -> {
                            lblStatus.setText("Maladie non contagieuse par voie aérienne.");
                            lblStatus.setStyle("-fx-text-fill: #7f8c8d;");
                        });
                    }
                } catch (Exception e) {
                    LogUtils.error(DiseaseMapController.class, "SporeCast evaluation failed", e);
                }
            }).start();
        }
    }
}
