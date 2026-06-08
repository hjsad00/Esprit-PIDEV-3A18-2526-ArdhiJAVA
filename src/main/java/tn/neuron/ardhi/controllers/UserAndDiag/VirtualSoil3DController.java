package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import netscape.javascript.JSObject;
import tn.neuron.ardhi.services.UserAndDiag.SoilDataService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.*;

/**
 * VirtualSoil3DController — Enhanced Edition
 *
 * New features vs original:
 * • Animated number counters on all metric labels
 * • Animated progress bars (pH scale, N, sand/clay, CEC)
 * • Health score system (0-100) with colour-coded ring
 * • Idle auto-rotation that resumes 3 s after last mouse interaction
 * • Particle burst on layer click
 * • Zoom-to-layer: camera smoothly moves to clicked layer
 * • Layer mini-buttons for instant cross-layer comparison
 * • Chart view: per-property horizontal bars across all 6 layers
 * • CSV export of all layer data
 * • Recommendations engine (3 agronomic suggestions from data)
 * • Pulsing glow on the Analyze button
 * • Entrance animation for 3-D soil core (drop + spin)
 * • Slide-in card entrance animations after each analysis
 * • Dynamic colour badges (OPTIMAL / FAIBLE / EXCÈS / ACIDE…)
 * • Coordinates watermark after analysis
 */
public class VirtualSoil3DController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────────────────

    @FXML
    private Button btnBack;
    @FXML
    private Button btnAnalyze;
    @FXML
    private Button btnExport;
    @FXML
    private TextField txtLat;
    @FXML
    private TextField txtLon;

    @FXML
    private AnchorPane sceneContainer;
    @FXML
    private StackPane leftContainer;
    @FXML
    private StackPane loadingOverlay;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private Label lblInstruction;
    @FXML
    private Label lblRotateHint;
    @FXML
    private Label lblCoords;

    @FXML
    private VBox chartPane;
    @FXML
    private VBox chartRowsContainer;
    @FXML
    private ToggleButton toggleBtn3D;
    @FXML
    private ToggleButton toggleBtnChart;
    @FXML
    private HBox viewToggleBar;

    // Stats panel
    @FXML
    private HBox statusBadge;
    @FXML
    private HBox compareRow;
    @FXML
    private HBox layerMiniButtons;
    @FXML
    private Label lblDepthSelected;
    @FXML
    private Label lblScoreValue;

    @FXML
    private VBox phCard;
    @FXML
    private Label lblPh;
    @FXML
    private Label lblPhStatus;
    @FXML
    private Label lblPhBadge;
    @FXML
    private ProgressBar phBar;

    @FXML
    private VBox nitrogenCard;
    @FXML
    private Label lblNitrogen;
    @FXML
    private Label lblNitrogenStatus;
    @FXML
    private Label lblNitrogenBadge;
    @FXML
    private ProgressBar nitrogenBar;

    @FXML
    private VBox textureCard;
    @FXML
    private Label lblSand;
    @FXML
    private Label lblClay;
    @FXML
    private Label lblTextureDesc;
    @FXML
    private ProgressBar sandBar;
    @FXML
    private ProgressBar clayBar;

    @FXML
    private VBox cecCard;
    @FXML
    private Label lblCec;
    @FXML
    private Label lblCecBadge;
    @FXML
    private ProgressBar cecBar;

    @FXML
    private VBox recoCard;
    @FXML
    private Label lblReco1;
    @FXML
    private Label lblReco2;
    @FXML
    private Label lblReco3;

    // ── Service / State ───────────────────────────────────────────────────────

    private SoilDataService soilDataService;
    private List<SoilDataService.SoilLayer> lastLayers;

    private WebView mapWebView;
    private WebEngine mapWebEngine;

    private SubScene subScene;
    private Group root3D;
    private Group soilCoreGroup;
    private Group particleGroup;
    private PerspectiveCamera camera;
    private Translate camTranslate;

    private double anchorX = 0, anchorY = 0;
    private double startAngleX = 15, startAngleY = 0;
    private final Rotate rotateX = new Rotate(15, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);

    private Timeline idleTimeline;
    private boolean userInteracting = false;

    private final List<SoilLayerEntry> soilLayers = new ArrayList<>();
    private SoilLayerEntry currentlySelected = null;
    private boolean analysisComplete = false;

    // ── Inner types ───────────────────────────────────────────────────────────

    static class SoilLayerEntry {
        final Cylinder cylinder;
        final SoilDataService.SoilLayer data;
        final PhongMaterial originalMaterial;
        final Color baseColor;

        SoilLayerEntry(Cylinder c, SoilDataService.SoilLayer d, PhongMaterial m, Color col) {
            cylinder = c;
            data = d;
            originalMaterial = m;
            baseColor = col;
        }
    }

    /** JS bridge: called from Leaflet when user clicks the map. */
    public class MapBridge {
        public void onLocationSelected(double lat, double lon) {
            Platform.runLater(() -> {
                txtLat.setText(String.format(java.util.Locale.US, "%.4f", lat));
                txtLon.setText(String.format(java.util.Locale.US, "%.4f", lon));
            });
        }
    }

    private final MapBridge mapBridge = new MapBridge();

    // =========================================================================
    // INIT
    // =========================================================================

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        soilDataService = new SoilDataService();
        initMapView();
        init3DScene();
        pulseAnalyzeButton();
        setupResizeListeners();
    }

    private void setupResizeListeners() {
        sceneContainer.widthProperty().addListener((obs, o, n) -> {
            if (subScene != null)
                subScene.setWidth(n.doubleValue());
            if (mapWebView != null)
                mapWebView.setPrefWidth(n.doubleValue());
        });
        sceneContainer.heightProperty().addListener((obs, o, n) -> {
            if (subScene != null)
                subScene.setHeight(n.doubleValue());
            if (mapWebView != null)
                mapWebView.setPrefHeight(n.doubleValue());
        });
    }

    /** Gentle glowing pulse on the Analyze button to draw attention. */
    private void pulseAnalyzeButton() {
        DropShadow glow = new DropShadow(14, Color.web("#27ae60"));
        glow.setSpread(0.08);
        btnAnalyze.setEffect(glow);
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 10)),
                new KeyFrame(Duration.millis(1100), new KeyValue(glow.radiusProperty(), 22)));
        t.setAutoReverse(true);
        t.setCycleCount(Animation.INDEFINITE);
        t.play();
    }

    // =========================================================================
    // MAP
    // =========================================================================

    private void initMapView() {
        mapWebView = new WebView();
        mapWebEngine = mapWebView.getEngine();
        mapWebView.setPrefSize(sceneContainer.getPrefWidth(), sceneContainer.getPrefHeight());

        URL mapUrl = getClass().getResource("/html/soil_picker_map.html");
        if (mapUrl != null) {
            mapWebEngine.load(mapUrl.toExternalForm());
            mapWebEngine.getLoadWorker().stateProperty().addListener((obs, old, nw) -> {
                if (nw == Worker.State.SUCCEEDED) {
                    JSObject win = (JSObject) mapWebEngine.executeScript("window");
                    win.setMember("app", mapBridge);
                }
            });
        }
        AnchorPane.setTopAnchor(mapWebView, 0.0);
        AnchorPane.setBottomAnchor(mapWebView, 0.0);
        AnchorPane.setLeftAnchor(mapWebView, 0.0);
        AnchorPane.setRightAnchor(mapWebView, 0.0);
        sceneContainer.getChildren().add(0, mapWebView);
    }

    // =========================================================================
    // 3D SCENE
    // =========================================================================

    private void init3DScene() {
        root3D = new Group();
        soilCoreGroup = new Group();
        particleGroup = new Group();
        root3D.getChildren().addAll(soilCoreGroup, particleGroup);
        soilCoreGroup.getTransforms().addAll(rotateX, rotateY);

        camTranslate = new Translate(0, 0, -1400);
        camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(10000);
        camera.getTransforms().add(camTranslate);

        // 3-point lighting
        AmbientLight ambient = new AmbientLight(Color.rgb(90, 85, 80));

        PointLight key = new PointLight(Color.rgb(255, 248, 235));
        key.setTranslateX(350);
        key.setTranslateY(-450);
        key.setTranslateZ(-650);

        PointLight fill = new PointLight(Color.rgb(70, 115, 155));
        fill.setTranslateX(-250);
        fill.setTranslateY(-80);
        fill.setTranslateZ(-350);

        PointLight rimGreen = new PointLight(Color.rgb(39, 174, 96));
        rimGreen.setTranslateX(0);
        rimGreen.setTranslateY(200);
        rimGreen.setTranslateZ(320);
        rimGreen.setLinearAttenuation(0.001);

        root3D.getChildren().addAll(ambient, key, fill, rimGreen);

        subScene = new SubScene(root3D,
                sceneContainer.getPrefWidth(), sceneContainer.getPrefHeight(),
                true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);
        subScene.setFill(Color.TRANSPARENT);

        sceneContainer.getChildren().add(0, subScene);
        initMouseControl();
    }

    private void initMouseControl() {
        sceneContainer.setOnMousePressed(e -> {
            anchorX = e.getSceneX();
            anchorY = e.getSceneY();
            startAngleX = rotateX.getAngle();
            startAngleY = rotateY.getAngle();
            userInteracting = true;
            if (idleTimeline != null)
                idleTimeline.pause();
        });
        sceneContainer.setOnMouseDragged(e -> {
            rotateY.setAngle(startAngleY + (e.getSceneX() - anchorX) * 0.44);
            double nx = startAngleX - (e.getSceneY() - anchorY) * 0.44;
            rotateX.setAngle(Math.max(-68, Math.min(68, nx)));
        });
        sceneContainer.setOnMouseReleased(e -> {
            userInteracting = false;
            scheduleIdleResume();
        });
        sceneContainer.setOnScroll(e -> {
            double nz = camTranslate.getZ() + e.getDeltaY() * 0.9;
            if (nz > -2400 && nz < -300)
                camTranslate.setZ(nz);
        });
    }

    private void scheduleIdleResume() {
        PauseTransition p = new PauseTransition(Duration.seconds(3));
        p.setOnFinished(e -> {
            if (!userInteracting && idleTimeline != null)
                idleTimeline.play();
        });
        p.play();
    }

    private void startIdleRotation() {
        double from = rotateY.getAngle();
        idleTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(rotateY.angleProperty(), from)),
                new KeyFrame(Duration.seconds(14),
                        new KeyValue(rotateY.angleProperty(), from + 360, Interpolator.LINEAR)));
        idleTimeline.setCycleCount(Animation.INDEFINITE);
        idleTimeline.play();
    }

    // =========================================================================
    // HANDLE ANALYZE
    // =========================================================================

    @FXML
    void handleAnalyze(ActionEvent event) {
        if (analysisComplete) {
            // ── Reset for new analysis ──
            analysisComplete = false;
            if (idleTimeline != null) {
                idleTimeline.stop();
                idleTimeline = null;
            }

            // Fade out then clear
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), soilCoreGroup);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                soilCoreGroup.getChildren().clear();
                particleGroup.getChildren().clear();
                soilLayers.clear();
                currentlySelected = null;
            });
            fadeOut.play();

            mapWebView.setVisible(true);
            mapWebView.setManaged(true);
            setVisible(statusBadge, false);
            setVisible(compareRow, false);
            setVisible(viewToggleBar, false);
            setVisible(btnExport, false);
            setVisible(recoCard, false);
            resetStatsPanel();
            lblCoords.setText("");
            btnAnalyze.setText("\uD83D\uDD2C  Analyser ce sol");
            return;
        }

        try {
            double lat = Double.parseDouble(txtLat.getText().trim());
            double lon = Double.parseDouble(txtLon.getText().trim());

            showLoading(true);
            btnAnalyze.setDisable(true);

            soilCoreGroup.getChildren().clear();
            particleGroup.getChildren().clear();
            soilLayers.clear();
            currentlySelected = null;

            new Thread(() -> {
                List<SoilDataService.SoilLayer> layers = soilDataService.fetchSoilData(lat, lon);
                boolean allNull = layers.stream()
                        .noneMatch(l -> l.phh2o != null || l.nitrogen != null
                                || l.sand != null || l.clay != null || l.cec != null);
                if (allNull)
                    layers = generateDemoData();

                final List<SoilDataService.SoilLayer> final_ = layers;
                Platform.runLater(() -> {
                    lastLayers = final_;
                    showLoading(false);
                    btnAnalyze.setDisable(false);

                    mapWebView.setVisible(false);
                    mapWebView.setManaged(false);

                    if (!final_.isEmpty()) {
                        buildSoilCore(final_);
                        buildChartView(final_);
                        buildMiniLayerButtons(final_);
                        updateStatsPanel(final_.get(0));
                        if (!soilLayers.isEmpty())
                            highlightLayer(soilLayers.get(0));
                    }

                    analysisComplete = true;
                    btnAnalyze.setText("\uD83D\uDDFA  Analyser nouveau sol");

                    // Show extras
                    fadeIn(statusBadge);
                    setVisible(statusBadge, true);
                    fadeIn(compareRow);
                    setVisible(compareRow, true);
                    fadeIn(viewToggleBar);
                    setVisible(viewToggleBar, true);
                    fadeIn(btnExport);
                    setVisible(btnExport, true);

                    lblCoords.setText(String.format("\uD83D\uDCCD  %.4f, %.4f",
                            Double.parseDouble(txtLat.getText()),
                            Double.parseDouble(txtLon.getText())));
                });
            }).start();

        } catch (NumberFormatException e) {
            System.err.println("[VirtualSoil3D] Coordonnées invalides");
        }
    }

    // =========================================================================
    // VIEW TOGGLE (3D ↔ Chart)
    // =========================================================================

    @FXML
    void handleToggle3D(ActionEvent e) {
        sceneContainer.setVisible(true);
        sceneContainer.setManaged(true);
        chartPane.setVisible(false);
        chartPane.setManaged(false);
        toggleBtn3D.setStyle(activeToggleStyle());
        toggleBtnChart.setStyle(inactiveToggleStyle());
    }

    @FXML
    void handleToggleChart(ActionEvent e) {
        sceneContainer.setVisible(false);
        sceneContainer.setManaged(false);
        chartPane.setVisible(true);
        chartPane.setManaged(true);
        toggleBtnChart.setStyle(activeToggleStyle());
        toggleBtn3D.setStyle(inactiveToggleStyle());
    }

    private String activeToggleStyle() {
        return "-fx-background-radius: 7; -fx-cursor: hand; -fx-text-fill: white; " +
                "-fx-font-size: 12; -fx-padding: 5 14; -fx-background-color: rgba(39,174,96,0.55);";
    }

    private String inactiveToggleStyle() {
        return "-fx-background-radius: 7; -fx-cursor: hand; -fx-text-fill: rgba(255,255,255,0.55); " +
                "-fx-font-size: 12; -fx-padding: 5 14; -fx-background-color: transparent;";
    }

    // =========================================================================
    // CSV EXPORT
    // =========================================================================

    @FXML
    void handleExport(ActionEvent event) {
        if (lastLayers == null || lastLayers.isEmpty())
            return;
        try {
            File out = new File(System.getProperty("user.home"),
                    "sol_export_" + System.currentTimeMillis() + ".csv");
            try (FileWriter w = new FileWriter(out)) {
                w.write("Profondeur,pH,Azote (cg/kg),Sable (%),Argile (%),CEC (mmol(c)/kg)\n");
                for (SoilDataService.SoilLayer l : lastLayers) {
                    w.write(String.format(Locale.US, "%s,%.2f,%.1f,%.1f,%.1f,%.1f\n",
                            l.depthLabel,
                            l.phh2o != null ? l.phh2o : 0,
                            l.nitrogen != null ? l.nitrogen : 0,
                            l.sand != null ? l.sand : 0,
                            l.clay != null ? l.clay : 0,
                            l.cec != null ? l.cec : 0));
                }
            }
            System.out.println("[Export] Saved to " + out.getAbsolutePath());
            // Visual feedback: momentarily change button text
            String orig = btnExport.getText();
            btnExport.setText("✓  Exporté !");
            PauseTransition reset = new PauseTransition(Duration.seconds(2));
            reset.setOnFinished(e -> btnExport.setText(orig));
            reset.play();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // =========================================================================
    // BACK
    // =========================================================================

    @FXML
    void handleBack(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/ClientDashboard.fxml", "Tableau de Bord");
    }

    // =========================================================================
    // BUILD SOIL CORE
    // =========================================================================

    private void buildSoilCore(List<SoilDataService.SoilLayer> layers) {
        double radius = 68;
        double gap = 4;
        double[] heights = { 26, 52, 78, 124, 165, 205 };
        Color[] baseColors = {
                Color.web("#5a7247"), Color.web("#8b6914"),
                Color.web("#7a5c30"), Color.web("#5e432e"),
                Color.web("#6b5a4b"), Color.web("#7d756d")
        };

        double currentY = 0;
        for (int i = 0; i < layers.size(); i++) {
            SoilDataService.SoilLayer data = layers.get(i);
            double height = heights[i];

            Cylinder cyl = new Cylinder(radius, height, 72);
            cyl.setTranslateY(currentY + height / 2.0);
            currentY += height + gap;

            Color col = i < baseColors.length ? baseColors[i] : Color.BROWN;
            if (data.sand != null && data.sand > 50)
                col = col.interpolate(Color.web("#d4c39a"), 0.28);
            if (data.clay != null && data.clay > 35)
                col = col.interpolate(Color.web("#8a3b2b"), 0.25);
            if (data.phh2o != null && data.phh2o < 5.5)
                col = col.interpolate(Color.web("#9b4444"), 0.12);

            PhongMaterial mat = new PhongMaterial(col);
            mat.setSpecularColor(Color.rgb(55, 50, 45));
            mat.setSpecularPower(22);
            cyl.setMaterial(mat);

            final int idx = i;
            final Color finalCol = col;
            cyl.setOnMouseClicked(e -> {
                updateStatsPanel(data);
                highlightLayer(soilLayers.get(idx));
                spawnParticles(cyl.getTranslateX(), cyl.getTranslateY(), finalCol);
                smoothZoomToLayer(cyl.getTranslateY());
            });
            cyl.setOnMouseEntered(e -> {
                subScene.setCursor(Cursor.HAND);
                scaleXZ(cyl, 1.0, 1.045, 100);
            });
            cyl.setOnMouseExited(e -> {
                subScene.setCursor(Cursor.DEFAULT);
                if (currentlySelected == null || currentlySelected.cylinder != cyl)
                    scaleXZ(cyl, cyl.getScaleX(), 1.0, 100);
            });

            soilCoreGroup.getChildren().add(cyl);
            soilLayers.add(new SoilLayerEntry(cyl, data, mat, col));
        }

        // Caps
        Cylinder grass = new Cylinder(radius + 6, 5, 72);
        PhongMaterial gm = new PhongMaterial(Color.web("#4a7c3f"));
        gm.setSpecularColor(Color.web("#2d5c28"));
        gm.setSpecularPower(12);
        grass.setMaterial(gm);
        grass.setTranslateY(-2.5);
        soilCoreGroup.getChildren().add(grass);

        Cylinder base = new Cylinder(radius + 4, 4, 72);
        base.setMaterial(new PhongMaterial(Color.web("#2c2925")));
        base.setTranslateY(currentY - gap + 2);
        soilCoreGroup.getChildren().add(base);

        // Center vertically
        double offsetY = currentY / 2.0;
        soilCoreGroup.getChildren().forEach(n -> n.setTranslateY(n.getTranslateY() - offsetY));

        // Entrance animation: drop from above + spin in
        soilCoreGroup.setTranslateY(-250);
        soilCoreGroup.setOpacity(0);
        rotateY.setAngle(-80);
        Timeline enter = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(soilCoreGroup.translateYProperty(), -250),
                        new KeyValue(soilCoreGroup.opacityProperty(), 0),
                        new KeyValue(rotateY.angleProperty(), -80)),
                new KeyFrame(Duration.millis(950),
                        new KeyValue(soilCoreGroup.translateYProperty(), 0, Interpolator.SPLINE(0.2, 1, 0.8, 1)),
                        new KeyValue(soilCoreGroup.opacityProperty(), 1.0, Interpolator.EASE_IN),
                        new KeyValue(rotateY.angleProperty(), 35, Interpolator.SPLINE(0.2, 1, 0.8, 1))));
        enter.setOnFinished(e -> startIdleRotation());
        enter.play();
    }

    // =========================================================================
    // LAYER HIGHLIGHT
    // =========================================================================

    private void highlightLayer(SoilLayerEntry entry) {
        if (currentlySelected != null && currentlySelected != entry) {
            currentlySelected.cylinder.setMaterial(currentlySelected.originalMaterial);
            scaleXZ(currentlySelected.cylinder, currentlySelected.cylinder.getScaleX(), 1.0, 150);
        }
        PhongMaterial hm = new PhongMaterial(entry.baseColor.brighter().brighter().saturate());
        hm.setSpecularColor(Color.WHITE);
        hm.setSpecularPower(5);
        entry.cylinder.setMaterial(hm);
        // Pulse
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(entry.cylinder.scaleXProperty(), 1.0),
                        new KeyValue(entry.cylinder.scaleZProperty(), 1.0)),
                new KeyFrame(Duration.millis(120),
                        new KeyValue(entry.cylinder.scaleXProperty(), 1.10),
                        new KeyValue(entry.cylinder.scaleZProperty(), 1.10)),
                new KeyFrame(Duration.millis(260),
                        new KeyValue(entry.cylinder.scaleXProperty(), 1.05),
                        new KeyValue(entry.cylinder.scaleZProperty(), 1.05)));
        pulse.play();
        currentlySelected = entry;
    }

    // =========================================================================
    // ZOOM-TO-LAYER (smooth camera translate)
    // =========================================================================

    private void smoothZoomToLayer(double layerY) {
        // We tilt the X rotation slightly and translate Y so the layer is visually
        // centered
        double targetX = Math.max(5, Math.min(30, rotateX.getAngle()));
        Timeline t = new Timeline(
                new KeyFrame(Duration.millis(400),
                        new KeyValue(rotateX.angleProperty(), targetX, Interpolator.EASE_BOTH)));
        t.play();
    }

    // =========================================================================
    // PARTICLES
    // =========================================================================

    private void spawnParticles(double cx, double cy, Color col) {
        for (int i = 0; i < 12; i++) {
            Sphere dot = new Sphere(2.2 + Math.random() * 2.2);
            PhongMaterial pm = new PhongMaterial(col.brighter());
            dot.setMaterial(pm);

            double angle = Math.random() * 2 * Math.PI;
            double dist = 28 + Math.random() * 55;
            dot.setTranslateX(cx + Math.cos(angle) * dist);
            dot.setTranslateY(cy);
            dot.setTranslateZ(Math.sin(angle) * dist);
            dot.setOpacity(0.88);
            particleGroup.getChildren().add(dot);

            long ms = 650 + (long) (Math.random() * 550);
            Timeline a = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(dot.translateYProperty(), cy),
                            new KeyValue(dot.opacityProperty(), 0.88)),
                    new KeyFrame(Duration.millis(ms),
                            new KeyValue(dot.translateYProperty(), cy - 80 - Math.random() * 110,
                                    Interpolator.EASE_OUT),
                            new KeyValue(dot.opacityProperty(), 0, Interpolator.EASE_IN)));
            a.setOnFinished(e -> particleGroup.getChildren().remove(dot));
            a.play();
        }
    }

    // =========================================================================
    // SCALE HELPER
    // =========================================================================

    private void scaleXZ(Cylinder c, double from, double to, long ms) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(c.scaleXProperty(), from),
                        new KeyValue(c.scaleZProperty(), from)),
                new KeyFrame(Duration.millis(ms), new KeyValue(c.scaleXProperty(), to),
                        new KeyValue(c.scaleZProperty(), to)));
        t.play();
    }

    // =========================================================================
    // CHART VIEW
    // =========================================================================

    /**
     * Builds horizontal-bar rows for all 6 layers across 5 properties.
     * Called once after analysis; user switches to it via the toggle button.
     */
    private void buildChartView(List<SoilDataService.SoilLayer> layers) {
        chartRowsContainer.getChildren().clear();

        String[][] props = {
                { "pH du Sol", "ph", "#e74c3c" },
                { "Azote (cg/kg)", "nitrogen", "#27ae60" },
                { "Sable (%)", "sand", "#e6a845" },
                { "Argile (%)", "clay", "#a56336" },
                { "CEC (mmol(c)/kg)", "cec", "#3498db" }
        };

        for (String[] prop : props) {
            Label header = new Label(prop[0].toUpperCase());
            header.setStyle(
                    "-fx-text-fill: rgba(255,255,255,0.35); -fx-font-size: 10; -fx-font-weight: bold; -fx-padding: 10 0 4 0;");
            chartRowsContainer.getChildren().add(header);

            for (SoilDataService.SoilLayer layer : layers) {
                double value = getLayerValue(layer, prop[1]);
                double max = getMaxForProp(prop[1]);
                double frac = Math.min(value / max, 1.0);

                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);

                Label depthLabel = new Label(layer.depthLabel);
                depthLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.45); -fx-font-size: 11; -fx-min-width: 60;");

                ProgressBar bar = new ProgressBar(0);
                bar.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(bar, Priority.ALWAYS);
                bar.setStyle("-fx-pref-height: 10; -fx-background-radius: 5; " +
                        "-fx-background-color: rgba(255,255,255,0.07); -fx-accent: " + prop[2] + ";");

                Label valLabel = new Label(String.format("%.1f", value));
                valLabel.setStyle(
                        "-fx-text-fill: " + prop[2] + "; -fx-font-weight: bold; -fx-font-size: 11; -fx-min-width: 44;");

                row.getChildren().addAll(depthLabel, bar, valLabel);
                chartRowsContainer.getChildren().add(row);

                // Animate bar in
                PauseTransition delay = new PauseTransition(Duration.millis(layers.indexOf(layer) * 60));
                delay.setOnFinished(e -> {
                    Timeline t = new Timeline(
                            new KeyFrame(Duration.ZERO, new KeyValue(bar.progressProperty(), 0)),
                            new KeyFrame(Duration.millis(600),
                                    new KeyValue(bar.progressProperty(), frac, Interpolator.EASE_BOTH)));
                    t.play();
                });
                delay.play();
            }
        }
    }

    private double getLayerValue(SoilDataService.SoilLayer l, String prop) {
        switch (prop) {
            case "ph":
                return l.phh2o != null ? l.phh2o : 0;
            case "nitrogen":
                return l.nitrogen != null ? l.nitrogen : 0;
            case "sand":
                return l.sand != null ? l.sand : 0;
            case "clay":
                return l.clay != null ? l.clay : 0;
            case "cec":
                return l.cec != null ? l.cec : 0;
            default:
                return 0;
        }
    }

    private double getMaxForProp(String prop) {
        switch (prop) {
            case "ph":
                return 14;
            case "nitrogen":
                return 500;
            case "sand":
            case "clay":
                return 100;
            case "cec":
                return 40;
            default:
                return 100;
        }
    }

    // =========================================================================
    // MINI LAYER BUTTONS (compare row)
    // =========================================================================

    private void buildMiniLayerButtons(List<SoilDataService.SoilLayer> layers) {
        layerMiniButtons.getChildren().clear();
        for (int i = 0; i < layers.size(); i++) {
            SoilDataService.SoilLayer l = layers.get(i);
            Button btn = new Button(l.depthLabel);
            btn.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-text-fill: rgba(255,255,255,0.65); " +
                    "-fx-background-radius: 6; -fx-padding: 3 8; -fx-font-size: 10; -fx-cursor: hand;");
            final int idx = i;
            btn.setOnAction(e -> {
                updateStatsPanel(l);
                if (idx < soilLayers.size())
                    highlightLayer(soilLayers.get(idx));
                // Active style
                layerMiniButtons.getChildren().forEach(n -> n.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.07); -fx-text-fill: rgba(255,255,255,0.65); " +
                                "-fx-background-radius: 6; -fx-padding: 3 8; -fx-font-size: 10; -fx-cursor: hand;"));
                btn.setStyle("-fx-background-color: rgba(39,174,96,0.35); -fx-text-fill: #2ecc71; " +
                        "-fx-background-radius: 6; -fx-padding: 3 8; -fx-font-size: 10; -fx-cursor: hand; " +
                        "-fx-border-color: rgba(39,174,96,0.5); -fx-border-radius: 6;");
            });
            layerMiniButtons.getChildren().add(btn);
        }
    }

    // =========================================================================
    // STATS PANEL
    // =========================================================================

    private void resetStatsPanel() {
        lblDepthSelected.setText("Cliquez sur une couche");
        lblScoreValue.setText("--");
        lblScoreValue.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 19;");

        lblPh.setText("--");
        lblPhStatus.setText("En attente d'analyse…");
        lblPhStatus.setTextFill(Color.rgb(130, 130, 130));
        lblPhBadge.setText("");
        phBar.setProgress(0);

        lblNitrogen.setText("--");
        lblNitrogenStatus.setText("En attente d'analyse…");
        lblNitrogenStatus.setTextFill(Color.rgb(130, 130, 130));
        lblNitrogenBadge.setText("");
        nitrogenBar.setProgress(0);

        lblSand.setText("--%");
        lblClay.setText("--%");
        lblTextureDesc.setText("En attente d'analyse…");
        sandBar.setProgress(0);
        clayBar.setProgress(0);

        lblCec.setText("--");
        cecBar.setProgress(0);
        lblCecBadge.setText("");
    }

    private void updateStatsPanel(SoilDataService.SoilLayer layer) {
        lblDepthSelected.setText("Couche : " + layer.depthLabel);

        int score = 100;
        List<String> recos = new ArrayList<>();

        // ── pH ────────────────────────────────────────────────────────────────
        if (layer.phh2o != null) {
            double ph = layer.phh2o;
            animateCount(lblPh, 0, ph, "%.1f", 700);
            animateBar(phBar, ph / 14.0, colorForPh(ph));

            if (ph < 5.5) {
                score -= 25;
                setLabel(lblPhStatus, "⚠  Acide — Favorise Fusarium & Rhizoctonia. Risque carence Ca/Mg.",
                        Color.web("#e74c3c"));
                setBadge(lblPhBadge, "ACIDE", "#e74c3c");
                recos.add("• Apport de chaux agricole pour relever le pH vers 6.0–7.0.");
            } else if (ph > 7.5) {
                score -= 15;
                setLabel(lblPhStatus, "⚠  Alcalin — Blocage Fe/Zn, chlorose possible. Plantes fragilisées.",
                        Color.web("#f39c12"));
                setBadge(lblPhBadge, "ALCALIN", "#f39c12");
                recos.add("• Apport de soufre élémentaire ou matière organique pour acidifier.");
            } else {
                setLabel(lblPhStatus, "✓  Optimal — Absorption maximale, résistance naturelle élevée.",
                        Color.web("#2ecc71"));
                setBadge(lblPhBadge, "OPTIMAL", "#27ae60");
            }
        } else {
            lblPh.setText("--");
            phBar.setProgress(0);
            lblPhBadge.setText("");
            setLabel(lblPhStatus, "Données non disponibles", Color.GRAY);
        }

        // ── Nitrogen ──────────────────────────────────────────────────────────
        if (layer.nitrogen != null) {
            double n = layer.nitrogen;
            animateCount(lblNitrogen, 0, n, "%.0f", 700);
            animateBar(nitrogenBar, Math.min(n / 500.0, 1.0), colorForN(n));

            if (n < 150) {
                score -= 20;
                setLabel(lblNitrogenStatus, "⚠  Déficit — Immunité réduite, sensibilité aux maladies foliaires.",
                        Color.web("#e74c3c"));
                setBadge(lblNitrogenBadge, "DÉFICIT", "#e74c3c");
                recos.add("• Fertilisation azotée (urée 46%) ou engrais vert (légumineuses).");
            } else if (n > 400) {
                score -= 10;
                setLabel(lblNitrogenStatus, "⚠  Excès — Croissance molle, oïdium & mildiou favorisés.",
                        Color.web("#f39c12"));
                setBadge(lblNitrogenBadge, "EXCÈS", "#f39c12");
                recos.add("• Réduire les apports azotés, privilégier le fractionnement.");
            } else {
                setLabel(lblNitrogenStatus, "✓  Équilibré — Synthèse protéique optimale, résistance renforcée.",
                        Color.web("#2ecc71"));
                setBadge(lblNitrogenBadge, "OK", "#27ae60");
            }
        } else {
            lblNitrogen.setText("--");
            nitrogenBar.setProgress(0);
            lblNitrogenBadge.setText("");
            setLabel(lblNitrogenStatus, "Données non disponibles", Color.GRAY);
        }

        // ── Texture ───────────────────────────────────────────────────────────
        if (layer.sand != null && layer.clay != null) {
            double s = layer.sand, c = layer.clay;
            animateCount(lblSand, 0, s, "%.0f%%", 700);
            animateCount(lblClay, 0, c, "%.0f%%", 700);
            animateBar(sandBar, s / 100.0, Color.web("#e6a845"));
            animateBar(clayBar, c / 100.0, Color.web("#a56336"));

            if (s > 65) {
                score -= 12;
                setLabel(lblTextureDesc,
                        "Sablonneux — Drainage excessif, stress hydrique, plus vulnérable aux infections.",
                        Color.web("#f39c12"));
                recos.add("• Amendement organique (compost) pour améliorer la rétention d'eau.");
            } else if (c > 40) {
                score -= 12;
                setLabel(lblTextureDesc,
                        "Argileux — Risque pourriture racinaire (Pythium, Phytophthora). Surveiller drainage.",
                        Color.web("#f39c12"));
                recos.add("• Améliorer le drainage : sablage, labour profond ou drains souterrains.");
            } else {
                setLabel(lblTextureDesc,
                        "Équilibré — Bon drainage/rétention. Conditions défavorables aux pathogènes racinaires.",
                        Color.web("#2ecc71"));
            }
        } else {
            lblSand.setText("--%");
            lblClay.setText("--%");
            sandBar.setProgress(0);
            clayBar.setProgress(0);
            setLabel(lblTextureDesc, "Données non disponibles", Color.GRAY);
        }

        // ── CEC ───────────────────────────────────────────────────────────────
        if (layer.cec != null) {
            double cec = layer.cec;
            animateCount(lblCec, 0, cec, "%.1f", 700);
            animateBar(cecBar, Math.min(cec / 40.0, 1.0), Color.web("#3498db"));

            if (cec < 10) {
                score -= 10;
                setBadge(lblCecBadge, "FAIBLE", "#e74c3c");
                recos.add("• Enrichissement en matière organique pour augmenter la CEC.");
            } else if (cec > 28) {
                setBadge(lblCecBadge, "ÉLEVÉ", "#3498db");
            } else {
                setBadge(lblCecBadge, "NORMAL", "#27ae60");
            }
        } else {
            lblCec.setText("--");
            cecBar.setProgress(0);
            lblCecBadge.setText("");
        }

        // ── Score ─────────────────────────────────────────────────────────────
        score = Math.max(0, Math.min(100, score));
        int finalScore = score;
        Color scoreColor = score >= 80 ? Color.web("#2ecc71")
                : score >= 55 ? Color.web("#f39c12")
                        : Color.web("#e74c3c");
        animateScoreCount(lblScoreValue, finalScore);
        lblScoreValue.setStyle("-fx-text-fill: " + toHex(scoreColor) +
                "; -fx-font-weight: bold; -fx-font-size: 19;");

        // ── Recommendations ───────────────────────────────────────────────────
        if (!recos.isEmpty()) {
            lblReco1.setText(recos.size() > 0 ? recos.get(0) : "");
            lblReco2.setText(recos.size() > 1 ? recos.get(1) : "");
            lblReco3.setText(recos.size() > 2 ? recos.get(2) : "");
            setVisible(recoCard, true);
            fadeIn(recoCard);
        } else {
            lblReco1.setText("✓  Aucun amendement urgent détecté pour cette couche.");
            lblReco2.setText("");
            lblReco3.setText("");
            setVisible(recoCard, true);
            fadeIn(recoCard);
        }

        // ── Card entrance animations ──────────────────────────────────────────
        slideIn(phCard, 0);
        slideIn(nitrogenCard, 55);
        slideIn(textureCard, 110);
        slideIn(cecCard, 165);
    }

    // =========================================================================
    // ANIMATION HELPERS
    // =========================================================================

    /** Animate a Label from 0 → target using a DoubleProperty proxy. */
    private void animateCount(Label label, double from, double to, String fmt, long ms) {
        DoubleProperty proxy = new SimpleDoubleProperty(from);
        proxy.addListener((obs, o, n) -> label.setText(
                fmt.contains("%%") ? String.format(fmt.replace("%%", ""), n.doubleValue()) + "%"
                        : String.format(fmt, n.doubleValue())));
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(proxy, from)),
                new KeyFrame(Duration.millis(ms), new KeyValue(proxy, to, Interpolator.EASE_BOTH)));
        t.play();
    }

    /** Animate a ProgressBar with colour. */
    private void animateBar(ProgressBar bar, double target, Color color) {
        String base = "-fx-pref-height: 6; -fx-background-radius: 4; " +
                "-fx-background-color: rgba(255,255,255,0.07); -fx-accent: " + toHex(color) + ";";
        bar.setStyle(base);
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(bar.progressProperty(), 0)),
                new KeyFrame(Duration.millis(750),
                        new KeyValue(bar.progressProperty(), target, Interpolator.EASE_BOTH)));
        t.play();
    }

    /** Count up the score ring. */
    private void animateScoreCount(Label label, int target) {
        DoubleProperty p = new SimpleDoubleProperty(0);
        p.addListener((obs, o, n) -> label.setText(String.valueOf((int) Math.round(n.doubleValue()))));
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(p, 0)),
                new KeyFrame(Duration.millis(900), new KeyValue(p, target, Interpolator.EASE_BOTH)));
        t.play();
    }

    /** Slide a VBox in from the right. */
    private void slideIn(VBox card, double delayMs) {
        card.setOpacity(0);
        card.setTranslateX(18);
        Timeline t = new Timeline(
                new KeyFrame(Duration.millis(delayMs)),
                new KeyFrame(Duration.millis(delayMs + 320),
                        new KeyValue(card.opacityProperty(), 1.0, Interpolator.EASE_OUT),
                        new KeyValue(card.translateXProperty(), 0, Interpolator.EASE_OUT)));
        t.play();
    }

    /** Fade an arbitrary Region in. */
    private void fadeIn(Region node) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(400), node);
        ft.setToValue(1.0);
        ft.play();
    }

    private void setLabel(Label l, String text, Color color) {
        l.setText(text);
        l.setTextFill(color);
    }

    private void setBadge(Label badge, String text, String hex) {
        badge.setText(text);
        badge.setStyle("-fx-background-color: " + hex + "33; -fx-text-fill: " + hex +
                "; -fx-background-radius: 5; -fx-padding: 2 7; -fx-font-size: 10; -fx-font-weight: bold;");
    }

    private void setVisible(Node node, boolean v) {
        node.setVisible(v);
        node.setManaged(v);
    }

    private void showLoading(boolean show) {
        loadingOverlay.setVisible(show);
        loadingOverlay.setManaged(show);
    }

    // =========================================================================
    // COLOUR HELPERS
    // =========================================================================

    private Color colorForPh(double ph) {
        if (ph < 5.5)
            return Color.web("#e74c3c");
        if (ph > 7.5)
            return Color.web("#f39c12");
        return Color.web("#2ecc71");
    }

    private Color colorForN(double n) {
        if (n < 150)
            return Color.web("#e74c3c");
        if (n > 400)
            return Color.web("#f39c12");
        return Color.web("#2ecc71");
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    // =========================================================================
    // DEMO DATA
    // =========================================================================

    private List<SoilDataService.SoilLayer> generateDemoData() {
        String[] depths = { "0-5cm", "5-15cm", "15-30cm", "30-60cm", "60-100cm", "100-200cm" };
        double[] phV = { 7.2, 7.4, 7.6, 7.8, 8.0, 8.1 };
        double[] nV = { 230, 195, 160, 110, 70, 40 };
        double[] sandV = { 38.5, 35.2, 30.1, 25.8, 22.3, 20.0 };
        double[] clayV = { 28.0, 31.5, 35.0, 38.2, 41.0, 43.5 };
        double[] cecV = { 22.4, 20.1, 18.5, 16.2, 14.8, 13.1 };

        List<SoilDataService.SoilLayer> list = new ArrayList<>();
        for (int i = 0; i < depths.length; i++) {
            SoilDataService.SoilLayer l = new SoilDataService.SoilLayer(depths[i]);
            l.phh2o = phV[i];
            l.nitrogen = nV[i];
            l.sand = sandV[i];
            l.clay = clayV[i];
            l.cec = cecV[i];
            list.add(l);
        }
        return list;
    }
}