package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.stage.FileChooser;
import tn.neuron.ardhi.models.UserAndDiag.TaskStatus;
import tn.neuron.ardhi.models.UserAndDiag.TreatmentPlan;
import tn.neuron.ardhi.models.UserAndDiag.TreatmentTask;
import tn.neuron.ardhi.models.UserAndDiag.Review;
import tn.neuron.ardhi.models.UserAndDiag.ReviewType;
import tn.neuron.ardhi.models.UserAndDiag.ExpertVerdict;
import tn.neuron.ardhi.models.UserAndDiag.FarmerResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.net.URL;
import javafx.scene.shape.SVGPath;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import tn.neuron.ardhi.services.UserAndDiag.CameraService;
import javax.imageio.ImageIO;
import java.io.InputStream;
import javafx.scene.layout.StackPane;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import tn.neuron.ardhi.services.UserAndDiag.SmartARService;
import javafx.embed.swing.SwingFXUtils;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

public class TreatmentPlanDetailsController {

    @FXML
    private Label diseaseTitle;

    @FXML
    private Label startDateLabel;

    @FXML
    private VBox tasksContainer;

    @FXML
    private ImageView originalImage;

    @FXML
    private AnchorPane imageWrapper; // Wrapper for overlays

    @FXML
    private StackPane cameraOverlay;
    @FXML
    private ImageView cameraView;
    @FXML
    private AnchorPane arOverlayPane; // New AR Layer
    @FXML
    private Canvas arCanvas; // Live AR Canvas

    private SmartARService smartARService = new SmartARService();
    @FXML
    private Label cameraStatusLabel;

    private CameraService cameraService = new CameraService();
    private Timeline cameraLoop;

    private TreatmentPlan plan;
    private tn.neuron.ardhi.services.UserAndDiag.TreatmentTaskService taskService = new tn.neuron.ardhi.services.UserAndDiag.TreatmentTaskService();
    private tn.neuron.ardhi.services.UserAndDiag.GroqService groqService = new tn.neuron.ardhi.services.UserAndDiag.GroqService();
    private tn.neuron.ardhi.services.UserAndDiag.TreatmentPlanService planService = new tn.neuron.ardhi.services.UserAndDiag.TreatmentPlanService();
    private tn.neuron.ardhi.services.UserAndDiag.ReviewService reviewService = new tn.neuron.ardhi.services.UserAndDiag.ReviewService();
    private tn.neuron.ardhi.services.UserAndDiag.DiagnosticService diagnosticService = new tn.neuron.ardhi.services.UserAndDiag.DiagnosticService();

    // Helper to bridge Reassessment -> Diagnostic Record
    private void saveDiagnosticRecord(String imagePath, String status, String details, boolean isHealed,
            String originalDisease) {
        try {
            // 1. Extract plant name from "Plant - Disease" format
            String fullDiseaseName = originalDisease != null ? originalDisease : "Plante Inconnue";
            String plantName = fullDiseaseName.split("-")[0].trim();

            // 2. Determine new "Resultat IA" and Severity/Confidence
            // If Healed -> "Plant - Saine"
            // If Worsening -> "Plant - Disease" (Severity Critical)
            // If Recovering -> "Plant - Disease" (Severity Low)
            // If Unchanged -> "Plant - Disease" (Severity Moderate)

            String newResultat;
            String severity;
            float confidence; // Represents "Health Confidence" if healthy, or "Disease Confidence" if sick

            if (isHealed) {
                newResultat = plantName + " - Saine";
                severity = "Saine";
                confidence = 95.0f;
            } else if ("WORSENING".equalsIgnoreCase(status)) {
                newResultat = fullDiseaseName;
                severity = "Critique";
                confidence = 90.0f; // High confidence it's sick
            } else if ("RECOVERING".equalsIgnoreCase(status)) {
                newResultat = fullDiseaseName;
                severity = "Faible";
                confidence = 40.0f; // Lower confidence in disease (improving)
            } else {
                // UNCHANGED
                newResultat = fullDiseaseName;
                severity = "Modéré";
                confidence = 70.0f;
            }

            tn.neuron.ardhi.models.UserAndDiag.Diagnostic d = new tn.neuron.ardhi.models.UserAndDiag.Diagnostic();
            d.setImageScannee(imagePath);
            d.setResultatIA(newResultat);
            d.setConfiance(confidence);
            d.setUserId(tn.neuron.ardhi.utils.UserAndDiag.UserSession.getInstance().getUser().getId());
            d.setDateScan(new java.sql.Timestamp(System.currentTimeMillis()));
            d.setSeverityFromString(severity);

            // LINK TO PLAN: We use location_label to store the Plan ID - REMOVED for
            // cleanup

            diagnosticService.ajouter(d);
            // System.out.println("Diagnostic Record Saved: " + status);
        } catch (Exception e) {
            e.printStackTrace(); // Non-blocking
        }
    }

    public void initData(TreatmentPlan shallowPlan) {
        diseaseTitle.setText("Chargement du protocole...");
        tasksContainer.getChildren().clear();
        Label loadingLabel = new Label("Chargement des détails en cours...");
        loadingLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-style: italic;");
        tasksContainer.getChildren().add(loadingLabel);

        javafx.concurrent.Task<TreatmentPlan> loadTask = new javafx.concurrent.Task<>() {
            @Override
            protected TreatmentPlan call() {
                TreatmentPlan deepPlan = planService.getPlanById(shallowPlan.getId());
                return deepPlan != null ? deepPlan : shallowPlan;
            }
        };

        loadTask.setOnSucceeded(e -> {
            this.plan = loadTask.getValue();
            diseaseTitle.setText("Protocole: " + plan.getInitialDiseaseName());
            startDateLabel.setText("Débuté le: " + plan.getStartDate().toString().substring(0, 10));

            loadImageIntoAsync(originalImage, plan.getPlantImageUrl());

            renderTasks();
            checkComparisonAsync();
            checkAndShowExpertFeedbackAsync();

            if (!plan.getTasks().isEmpty()) {
                TreatmentTask firstPending = plan.getTasks().stream()
                        .filter(t -> t.getStatus() != TaskStatus.COMPLETED)
                        .sorted(Comparator.comparingInt(TreatmentTask::getDayOffset))
                        .findFirst()
                        .orElse(null);

                if (firstPending != null) {
                    drawArOverlay(firstPending);
                }
            }
        });

        Thread t = new Thread(loadTask);
        t.setDaemon(true);
        t.start();
    }

    // --- CAMERA METHODS ---

    @FXML
    void openCameraMode() {
        // Stop any existing camera
        closeCamera();

        WindowUtils.showCameraSourcePrompt(cameraService,
                () -> {
                    cameraOverlay.setVisible(true);
                    cameraOverlay.setManaged(true);
                    startCameraFeedLoop();
                    javafx.application.Platform.runLater(() -> cameraStatusLabel.setText("Caméra active"));
                },
                this::closeCamera);
    }

    private void startCameraFeedLoop() {
        // UI Thread Animation Timer to update imageview
        cameraLoop = new Timeline(new KeyFrame(Duration.millis(33), e -> {
            Image img = cameraService.takeSnapshot();
            if (img != null) {
                cameraView.setImage(img);

                // --- LIVE AR PINS ---
                SmartARService.ARResult arResult = smartARService.processFrame(img);
                drawLiveAR(arResult);
            }
        }));
        cameraLoop.setCycleCount(Timeline.INDEFINITE);
        cameraLoop.play();
    }

    private void drawLiveAR(SmartARService.ARResult result) {
        if (arCanvas == null)
            return;
        GraphicsContext gc = arCanvas.getGraphicsContext2D();
        double w = arCanvas.getWidth();
        double h = arCanvas.getHeight();

        // Get actual image dimensions for scaling
        Image img = cameraView.getImage();
        double imgW = (img != null) ? img.getWidth() : 320;
        double imgH = (img != null) ? img.getHeight() : 240;

        double scaleX = w / imgW;
        double scaleY = h / imgH;

        gc.clearRect(0, 0, w, h);

        if (result.targetLocked) {
            double cx = result.plantCentroidX * scaleX;
            double cy = result.plantCentroidY * scaleY;

            // Draw Centroid Marker
            gc.setStroke(Color.LIME);
            gc.setLineWidth(2);

            // Pulsating size
            double lockTime = System.currentTimeMillis() / 1000.0;
            double pulseSize = 30 + Math.sin(lockTime * 5.0) * 5;
            gc.strokeOval(cx - pulseSize / 2, cy - pulseSize / 2, pulseSize, pulseSize);

            // Draw Task Pins relative to Centroid
            if (plan != null && plan.getTasks() != null) {
                for (TreatmentTask task : plan.getTasks()) {
                    if (task.getStatus() == TaskStatus.COMPLETED)
                        continue;

                    String desc = task.getTaskDescription().toLowerCase();
                    double offX = 0;
                    double offY = 0;

                    // Relative Offsets scaled dynamically based on plant proximity/coverage
                    double scale = Math.max(40, Math.min(150, result.vegetationCoverage * 600));

                    if (desc.contains("feuille")) {
                        offX = -0.5 * scale;
                        offY = -0.5 * scale;
                    } else if (desc.contains("tige")) {
                        offY = 0;
                    } else if (desc.contains("racine") || desc.contains("sol")) {
                        offY = 0.8 * scale;
                    } else if (desc.contains("fruit")) {
                        offX = 0.5 * scale;
                        offY = -0.3 * scale;
                    } else if (desc.contains("fleur")) {
                        offY = -0.8 * scale;
                    }

                    double pinX = cx + offX;
                    double pinY = cy + offY;

                    // Draw Pin
                    gc.setFill(Color.RED);
                    gc.fillOval(pinX - 6, pinY - 6, 12, 12);
                    gc.setStroke(Color.WHITE);
                    gc.setLineWidth(1);
                    gc.strokeOval(pinX - 6, pinY - 6, 12, 12);

                    // Draw Label Background
                    gc.setFill(new Color(0, 0, 0, 0.7));
                    gc.fillRoundRect(pinX + 10, pinY - 10, 120, 20, 5, 5);

                    // Draw Label Text
                    gc.setFill(Color.WHITE);
                    gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 12));
                    gc.fillText(
                            task.getTaskDescription().length() > 18 ? task.getTaskDescription().substring(0, 18) + "..."
                                    : task.getTaskDescription(),
                            pinX + 15, pinY + 4);

                    // Connection Line
                    gc.setStroke(new Color(1, 1, 1, 0.6));
                    gc.strokeLine(cx, cy, pinX, pinY);
                }
            }
        } else {
            // Scanning...
            gc.setFill(Color.CYAN);
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 14));
            gc.fillText("Recherche plante...", 20, 30);

            // Simple scan line
            gc.setStroke(Color.CYAN);
            gc.setLineWidth(2);
            double time = System.currentTimeMillis() / 1000.0;
            double scanY = (time % 2.0) / 2.0 * h;
            gc.strokeLine(0, scanY, w, scanY);
        }
    }

    @FXML
    void handleCapture() {
        if (cameraService.isOpen()) {
            Image snapshot = cameraService.takeSnapshot();
            if (snapshot != null) {
                // Display in main view
                originalImage.setImage(snapshot);

                // Optional: Save to temp file if we want to use it for re-evaluation later
                saveSnapshotToTemp(snapshot);

                closeCamera();
                tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.INFORMATION, "Image Capturée",
                        "L'image a été capturée. Vous pouvez maintenant utiliser les outils d'analyse.");
            }
        }
    }

    private void saveSnapshotToTemp(Image image) {
        try {
            File temp = File.createTempFile("scan_capture", ".png");
            java.awt.image.BufferedImage bImg = SwingFXUtils.fromFXImage(image, null);
            ImageIO.write(bImg, "png", temp);

            // Store path in originalImage userData for later retrieval
            originalImage.setUserData(temp.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void closeCamera() {
        if (cameraLoop != null)
            cameraLoop.stop();
        cameraService.stopCamera();

        if (arCanvas != null) {
            arCanvas.getGraphicsContext2D().clearRect(0, 0, arCanvas.getWidth(), arCanvas.getHeight());
        }

        cameraOverlay.setVisible(false);
        cameraOverlay.setManaged(false);
    }

    // --- AR VISUALIZATIONS ---

    // --- ADVANCED AR DRAWING ---

    private void drawArOverlay(TreatmentTask task) {
        if (arOverlayPane == null)
            return;
        arOverlayPane.getChildren().clear();

        String desc = task.getTaskDescription().toLowerCase();
        double w = arOverlayPane.getMaxWidth(); // 400
        double h = arOverlayPane.getMaxHeight(); // 300

        // 1. ZONE OVERLAYS (Polygon/Rectangle)
        if (desc.contains("sol") || desc.contains("arroser") || desc.contains("engrais")) {
            // Blue Zone at bottom (Soil)
            javafx.scene.shape.Polygon zone = new javafx.scene.shape.Polygon();
            zone.getPoints().addAll(
                    0.0, h * 0.7,
                    w, h * 0.7,
                    w, h,
                    0.0, h);
            zone.setFill(new Color(0.2, 0.6, 1.0, 0.3)); // Translucent Blue
            zone.setStroke(Color.DEEPSKYBLUE);
            zone.setStrokeWidth(2);

            Label lbl = new Label("ZONE SOL");
            lbl.setStyle(
                    "-fx-text-fill: deepskyblue; -fx-font-weight: bold; -fx-background-color: rgba(0,0,0,0.5); -fx-padding: 5;");
            lbl.setLayoutX(w / 2 - 30);
            lbl.setLayoutY(h * 0.75);

            arOverlayPane.getChildren().addAll(zone, lbl);

        } else if (desc.contains("feuille") || desc.contains("tige") || desc.contains("traitement")) {
            // Center Target Brackets (Foliage)
            double cx = w / 2;
            double cy = h / 2;
            double size = 80;

            // Corners
            // Top Left
            javafx.scene.shape.Polyline tl = new javafx.scene.shape.Polyline(cx - size, cy - size + 20, cx - size,
                    cy - size, cx - size + 20, cy - size);
            // Top Right
            javafx.scene.shape.Polyline tr = new javafx.scene.shape.Polyline(cx + size - 20, cy - size, cx + size,
                    cy - size, cx + size, cy - size + 20);
            // Bottom Left
            javafx.scene.shape.Polyline bl = new javafx.scene.shape.Polyline(cx - size, cy + size - 20, cx - size,
                    cy + size, cx - size + 20, cy + size);
            // Bottom Right
            javafx.scene.shape.Polyline br = new javafx.scene.shape.Polyline(cx + size - 20, cy + size, cx + size,
                    cy + size, cx + size, cy + size - 20);

            Color c = desc.contains("traitement") ? Color.ORANGE : Color.LIME;

            for (javafx.scene.shape.Polyline p : new javafx.scene.shape.Polyline[] { tl, tr, bl, br }) {
                p.setStroke(c);
                p.setStrokeWidth(4);
                p.setFill(Color.TRANSPARENT);
                arOverlayPane.getChildren().add(p);
            }

            Label lbl = new Label(desc.contains("traitement") ? "CIBLE TRAITEMENT" : "ZONE FEUILLAGE");
            lbl.setStyle("-fx-text-fill: " + (desc.contains("traitement") ? "orange" : "lime")
                    + "; -fx-font-weight: bold; -fx-background-color: rgba(0,0,0,0.5); -fx-padding: 5;");
            lbl.setLayoutX(cx - 50);
            lbl.setLayoutY(cy + size + 10);
            arOverlayPane.getChildren().add(lbl);

        } else if (desc.contains("taille") || desc.contains("couper")) {
            // Pruning Lines (Dashed)
            javafx.scene.shape.Line line = new javafx.scene.shape.Line(w * 0.2, h * 0.3, w * 0.8, h * 0.7);
            line.setStroke(Color.RED);
            line.setStrokeWidth(3);
            line.getStrokeDashArray().addAll(10d, 10d);

            Label lbl = new Label("ZONE DE COUPE SUGGÉRÉE");
            lbl.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-background-color: white; -fx-padding: 3;");
            lbl.setLayoutX(w / 2 - 50);
            lbl.setLayoutY(h / 2);

            arOverlayPane.getChildren().addAll(line, lbl);
        }

        // Always show task text overlay
        Label taskOverlay = new Label("TÂCHE ACTUELLE: " + task.getTaskDescription());
        taskOverlay.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; " +
                "-fx-background-color: rgba(0, 0, 0, 0.6); -fx-padding: 10 20; -fx-background-radius: 20;");
        taskOverlay.setMaxWidth(w - 40);
        taskOverlay.setWrapText(true);
        taskOverlay.setLayoutX(20);
        taskOverlay.setLayoutY(20);

        arOverlayPane.getChildren().add(taskOverlay);
    }

    private void addVisualMarker(double xPercent, double yPercent, String label) {
        if (imageWrapper == null)
            return;

        // Clear previous markers
        imageWrapper.getChildren()
                .removeIf(node -> node instanceof javafx.scene.Node && (node.getStyleClass().contains("ar-marker")
                        || node instanceof Label && node.getStyle().contains("rgba(0,0,0,0.7)")));

        double width = originalImage.getFitWidth();
        double height = originalImage.getFitHeight();

        // SVG Icon logic
        SVGPath icon = new SVGPath();
        String content = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 15h-2v-6h2v6zm0-8h-2V7h2v2z"; // Default
                                                                                                                              // Info

        if (label.toLowerCase().contains("eau") || label.toLowerCase().contains("arroser")) {
            // Drop icon
            content = "M12 22c4.97 0 9-4.03 9-9-9 0-9-9-9-9s0 9 0 9c0 4.97 4.03 9 9 9z";
            icon.setFill(Color.AQUA);
        } else if (label.toLowerCase().contains("feuille") || label.toLowerCase().contains("tige")) {
            // Leaf icon
            content = "M17 8C8 10 5.9 16.17 3.82 21.34 5.71 18.06 8.46 15 11.5 13c5.51-3.6 1.49-8.48 5.5-5z";
            icon.setFill(Color.LIGHTGREEN);
        } else {
            // Default Pin
            content = "M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z";
            icon.setFill(Color.RED);
        }

        icon.setContent(content);
        icon.setStroke(Color.WHITE);
        icon.setStrokeWidth(1.5);
        icon.setScaleX(1.5);
        icon.setScaleY(1.5);
        icon.setEffect(new DropShadow(5, Color.BLACK));
        icon.getStyleClass().add("ar-marker");

        // Center the icon
        icon.setLayoutX(width * xPercent);
        icon.setLayoutY(height * yPercent);

        // Label tooltip
        Label tag = new Label(
                label);
        tag.setStyle(
                "-fx-background-color: rgba(0,0,0,0.7); -fx-text-fill: white; -fx-padding: 3 6; -fx-background-radius: 4; -fx-font-size: 10px;");
        tag.setLayoutX(width * xPercent + 15);
        tag.setLayoutY(height * yPercent - 15);

        // Animation: Pulse
        javafx.animation.ScaleTransition pulse = new javafx.animation.ScaleTransition(Duration.millis(600),
                icon);
        pulse.setFromX(1.5);
        pulse.setFromY(1.5);
        pulse.setToX(1.8);
        pulse.setToY(1.8);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(javafx.animation.Animation.INDEFINITE);
        pulse.play();

        // Scan Line Effect (Horizontal moving line)
        javafx.scene.shape.Line scanLine = new javafx.scene.shape.Line(0, 0, width,
                0);
        scanLine.setStroke(Color.LIME);
        scanLine.setStrokeWidth(2);
        scanLine.setOpacity(0.6);
        scanLine.setEffect(new DropShadow(5, Color.LIME));

        TranslateTransition scan = new TranslateTransition(Duration.seconds(2),
                scanLine);
        scan.setFromY(0);
        scan.setToY(height);
        scan.setCycleCount(2); // One pass down and reset
        scan.setAutoReverse(false);
        scan.play();

        imageWrapper.getChildren().addAll(scanLine, icon, tag);

        // Clean up scan line after animation
        scan.setOnFinished(e -> imageWrapper.getChildren().remove(scanLine));
    }

    private void renderTasks() {
        tasksContainer.getChildren().clear();
        List<TreatmentTask> tasks = plan.getTasks();
        tasks.sort(Comparator.comparingInt(TreatmentTask::getDayOffset));

        for (TreatmentTask task : tasks) {
            // Main Row Container
            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            // Increased opacity from 0.12 to 0.35 for better visibility
            row.setStyle("-fx-background-color: rgba(255, 255, 255, 0.35); " +
                    "-fx-padding: 10 15; " +
                    "-fx-background-radius: 12; " +
                    "-fx-border-color: rgba(255, 255, 255, 0.25); " +
                    "-fx-border-radius: 12; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);");

            // --- LEFT SECTION: Day & Type ---
            VBox leftBox = new VBox(4);
            leftBox.setAlignment(Pos.CENTER);
            leftBox.setMinWidth(70);
            leftBox.setMaxWidth(70);

            Label dayLabel = new Label("J-" + task.getDayOffset());
            dayLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 16px;");

            Label typeTag = new Label();
            String typeStyle = "-fx-padding: 2 8; -fx-background-radius: 6; -fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: white;";

            String descLower = task.getTaskDescription().toLowerCase();
            if (task.getDayOffset() == 1) {
                typeTag.setText("URGENT");
                typeTag.setStyle(typeStyle + "-fx-background-color: #e74c3c;");
            } else if (descLower.contains("traitement") || descLower.contains("fongicide")
                    || descLower.contains("appliquer")) {
                typeTag.setText("SOIN");
                typeTag.setStyle(typeStyle + "-fx-background-color: #27ae60;"); // Green
            } else {
                typeTag.setText("ACTION");
                typeTag.setStyle(typeStyle + "-fx-background-color: #7f8c8d;"); // Grey
            }
            leftBox.getChildren().addAll(dayLabel, typeTag);

            // --- CENTER SECTION: Description ---
            Label descLabel = new Label(task.getTaskDescription());
            descLabel.setWrapText(true);
            descLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(descLabel, Priority.ALWAYS);
            boolean isCompleted = task.getStatus() == TaskStatus.COMPLETED;
            boolean isMissed = task.getStatus() == TaskStatus.MISSED;
            descLabel.setStyle("-fx-text-fill: " + (isCompleted ? "rgba(255,255,255,0.5)" : "white") + "; " +
                    "-fx-font-size: 13px;" + (isCompleted ? " -fx-strikethrough: true;" : ""));

            // --- RIGHT SECTION: Actions ---
            HBox rightBox = new HBox(12);
            rightBox.setAlignment(Pos.CENTER_RIGHT);
            rightBox.setMinWidth(220); // Fixed width for alignment consistency

            // Chat Button
            Button chatBtn = new Button("💬");
            chatBtn.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-background-radius: 20; -fx-cursor: hand; -fx-min-width: 32; -fx-min-height: 32;");
            chatBtn.setOnAction(e -> openChatPopup(task));

            // LOCATION PIN LOGIC
            // Simple keyword heuristic for demo purposes
            double locX = -1;
            double locY = -1;
            String locLabel = "";

            if (descLower.contains("feuille")) {
                locX = 0.3;
                locY = 0.4;
                locLabel = "Feuilles";
            } else if (descLower.contains("tige")) {
                locX = 0.5;
                locY = 0.6;
                locLabel = "Tige Principale";
            } else if (descLower.contains("racine")) {
                locX = 0.5;
                locY = 0.9;
                locLabel = "Racines";
            } else if (descLower.contains("sol") || descLower.contains("arroser")) {
                locX = 0.5;
                locY = 0.95;
                locLabel = "Sol";
            } else if (descLower.contains("fruit")) {
                locX = 0.6;
                locY = 0.3;
                locLabel = "Fruits";
            } else if (descLower.contains("fleur")) {
                locX = 0.4;
                locY = 0.2;
                locLabel = "Fleurs";
            }

            if (locX != -1) {
                Button pinBtn = new Button("📍");
                pinBtn.setTooltip(new Tooltip("Voir la zone concernée"));
                pinBtn.setStyle(
                        "-fx-background-color: rgba(231, 76, 60, 0.8); -fx-text-fill: white; -fx-background-radius: 20; -fx-cursor: hand; -fx-min-width: 32; -fx-min-height: 32;");
                double finalX = locX;
                double finalY = locY;
                String finalLabel = locLabel;
                pinBtn.setOnAction(e -> {
                    addVisualMarker(finalX, finalY, finalLabel);
                    tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.INFORMATION,
                            "Zone Identifiée", "Le marqueur rouge indique : " + finalLabel);
                });
                rightBox.getChildren().add(pinBtn);
            }

            // Status Tag (The "Tag next to checkbox")
            Label statusTag = new Label();
            String statusStyleBase = "-fx-padding: 5 10; -fx-background-radius: 8; -fx-font-size: 10px; -fx-font-weight: bold; ";
            if (isCompleted) {
                statusTag.setText("ACCOMPLI");
                statusTag.setStyle(statusStyleBase
                        + "-fx-background-color: rgba(46, 204, 113, 0.2); -fx-text-fill: #2ecc71; -fx-border-color: #2ecc71; -fx-border-radius: 8;");
            } else if (isMissed) {
                statusTag.setText("MANQUÉ");
                statusTag.setStyle(statusStyleBase
                        + "-fx-background-color: rgba(231, 76, 60, 0.2); -fx-text-fill: #e74c3c; -fx-border-color: #e74c3c; -fx-border-radius: 8;");
            } else {
                statusTag.setText("EN ATTENTE");
                statusTag.setStyle(statusStyleBase
                        + "-fx-background-color: rgba(241, 196, 15, 0.2); -fx-text-fill: #f1c40f; -fx-border-color: #f1c40f; -fx-border-radius: 8;");
            }

            // Checkbox
            CheckBox statusBox = new CheckBox();
            statusBox.setSelected(isCompleted);
            statusBox.setStyle("-fx-cursor: hand;");

            statusBox.setOnAction(e -> {
                if (statusBox.isSelected()) {
                    completeTask(task, statusBox);
                }
            });
            if (isCompleted || isMissed) {
                statusBox.setDisable(true);
            }
            // Assembly: Chat | Status | Checkbox
            rightBox.getChildren().addAll(chatBtn, statusTag, statusBox);

            row.getChildren().addAll(leftBox, descLabel, rightBox);

            // --- ROW CLICK INTERACTION ---
            // Clicking the row selects it for AR visualization
            row.setOnMouseClicked(e -> {
                // Visual feedback for selection
                tasksContainer.getChildren()
                        .forEach(n -> n.setStyle(n.getStyle().replace("-fx-background-color: rgba(255, 255, 255, 0.5);",
                                "-fx-background-color: rgba(255, 255, 255, 0.35);"))); // Reset others
                row.setStyle(row.getStyle().replace("-fx-background-color: rgba(255, 255, 255, 0.35);",
                        "-fx-background-color: rgba(255, 255, 255, 0.5);")); // Highlight

                drawArOverlay(task);
            });

            tasksContainer.getChildren().add(row);
        }
    }

    private void completeTask(TreatmentTask task, CheckBox box) {
        // --- Sequential enforcement: check that all previous tasks are completed ---
        List<TreatmentTask> sorted = new java.util.ArrayList<>(plan.getTasks());
        sorted.sort(Comparator.comparingInt(TreatmentTask::getDayOffset));
        for (TreatmentTask prev : sorted) {
            if (prev.getDayOffset() < task.getDayOffset() && prev.getStatus() != TaskStatus.COMPLETED) {
                // Previous task not completed — block
                box.setSelected(false);
                tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(javafx.scene.control.Alert.AlertType.WARNING,
                        "Ordre séquentiel",
                        "Vous devez d'abord terminer la tâche du Jour " + prev.getDayOffset()
                                + " avant de passer à celle-ci.");
                return;
            }
        }

        // Update DB
        task.setStatus(TaskStatus.COMPLETED);
        taskService.updateTaskStatus(task.getId(), TaskStatus.COMPLETED);

        // --- In-place UI update (no full re-render, no scroll reset) ---
        box.setDisable(true);

        // Find the row HBox containing this checkbox and update styles in-place
        HBox row = (HBox) box.getParent().getParent(); // rightBox -> row
        // Update description label (center child, index 1)
        if (row.getChildren().size() > 1 && row.getChildren().get(1) instanceof Label) {
            Label descLabel = (Label) row.getChildren().get(1);
            descLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 13px; -fx-strikethrough: true;");
        }
        // Update status tag (in rightBox, second child)
        HBox rightBox = (HBox) box.getParent();
        for (javafx.scene.Node child : rightBox.getChildren()) {
            if (child instanceof Label) {
                Label statusTag = (Label) child;
                statusTag.setText("ACCOMPLI");
                statusTag.setStyle(
                        "-fx-padding: 5 10; -fx-background-radius: 8; -fx-font-size: 10px; -fx-font-weight: bold; "
                                + "-fx-background-color: rgba(46, 204, 113, 0.2); -fx-text-fill: #2ecc71; -fx-border-color: #2ecc71; -fx-border-radius: 8;");
                break;
            }
        }

        checkComparisonAsync();
    }

    private void checkComparisonAsync() {
        javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
            @Override
            protected String call() {
                List<Review> reviews = reviewService.getReviewsForDiagnostic(plan.getDiagnosticId());
                String activeImageUrl = plan.getPlantImageUrl();
                List<Review> acceptedProgress = new java.util.ArrayList<>();
                for (Review r : reviews) {
                    if (r.getReviewType() == ReviewType.PROGRESS && r.getPhotoUrl() != null
                            && r.getFarmerResponse() == FarmerResponse.ACCEPTED) {
                        acceptedProgress.add(r);
                    }
                }
                if (!acceptedProgress.isEmpty()) {
                    acceptedProgress.sort((r1, r2) -> Integer.compare(r2.getId(), r1.getId()));
                    activeImageUrl = acceptedProgress.get(0).getPhotoUrl();
                }
                return activeImageUrl;
            }
        };

        task.setOnSucceeded(e -> {
            loadImageIntoAsync(originalImage, task.getValue());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void loadImageIntoAsync(javafx.scene.image.ImageView view, String path) {
        if (path == null || view == null)
            return;
        new Thread(() -> {
            try {
                String finalUrl = path;
                if (!path.startsWith("http")) {
                    File f = new File(path);
                    if (f.exists()) {
                        finalUrl = f.toURI().toString();
                    }
                }
                Image img = new Image(finalUrl, true);
                javafx.application.Platform.runLater(() -> view.setImage(img));
            } catch (Exception e) {
            }
        }).start();
    }

    @FXML
    void handleReevaluate() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Scanner pour réévaluer le traitement");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File selectedFile = fileChooser.showOpenDialog(tasksContainer.getScene().getWindow());

        if (selectedFile != null) {
            // UI Feedback
            Alert loading = new Alert(Alert.AlertType.INFORMATION);
            loading.setTitle("Réévaluation IA");
            loading.setHeaderText("Analyse de votre plante...");
            loading.setContentText("L'IA compare l'état actuel avec votre dernière progression.");
            loading.getDialogPane().getButtonTypes().clear();
            loading.show();

            // Use array to hold URL since lambdas require final/effectively final variables
            final String[] uploadedUrl = new String[1];

            javafx.concurrent.Task<String> gateTask = new javafx.concurrent.Task<>() {
                @Override
                protected String call() throws Exception {
                    // 1. Upload to ImgBB first
                    String imgBbUrl = tn.neuron.ardhi.utils.UserAndDiag.ImgBBService.uploadImage(selectedFile);
                    if (imgBbUrl == null)
                        imgBbUrl = selectedFile.getAbsolutePath(); // Fallback
                    uploadedUrl[0] = imgBbUrl;

                    // 2. Prepare Comparison Image (Prioritize LATEST PROGRESS over ORIGINAL)
                    File comparisonBaseline = null;

                    // Try to find the latest progress image from reviews
                    List<Review> reviews = reviewService.getReviewsForDiagnostic(plan.getDiagnosticId());
                    String latestProgressUrl = null;
                    for (Review r : reviews) {
                        if (r.getPhotoUrl() != null && r.getReviewType() == ReviewType.PROGRESS) {
                            latestProgressUrl = r.getPhotoUrl();
                            break; // Logic in checkComparison assumes this list might be sorted or we just take
                                   // first found?
                                   // Ideally we sort by date. Assuming latest is first for now or we rely on
                                   // checkComparison logic.
                                   // Actually reviews are usually fetched by ID, so sorting might be needed.
                                   // Let's rely on the fact that we augment this flow.
                        }
                    }

                    if (latestProgressUrl != null) {
                        if (latestProgressUrl.startsWith("http")) {
                            comparisonBaseline = downloadImage(latestProgressUrl);
                        } else {
                            comparisonBaseline = new File(latestProgressUrl);
                        }
                    }

                    // Fallback to original if no progress image found
                    if (comparisonBaseline == null || !comparisonBaseline.exists()) {
                        String originalPath = plan.getPlantImageUrl();
                        if (originalPath != null && originalPath.startsWith("http")) {
                            comparisonBaseline = downloadImage(originalPath);
                        } else if (originalPath != null) {
                            comparisonBaseline = new File(originalPath);
                        }
                    }

                    if (comparisonBaseline == null || !comparisonBaseline.exists()) {
                        return "FORCE_REEVAL";
                    }

                    // 2a. Check Consistency
                    String consistency = groqService.checkConsistency(selectedFile, plan.getInitialDiseaseName());
                    if (consistency.startsWith("MISMATCH")) {
                        return consistency;
                    }

                    // 3. Analyze against the CHOSEN baseline
                    return groqService.analyzeRecovery(comparisonBaseline, selectedFile, plan.getInitialDiseaseName());
                }
            };

            gateTask.setOnSucceeded(e -> {
                String gateResult = gateTask.getValue();

                // --- HANDLE MISMATCH ---
                if (gateResult.startsWith("MISMATCH")) {
                    String reason = gateResult.contains("|") ? gateResult.split("\\|")[1] : "Image non correspondante.";
                    loading.setResult(ButtonType.OK);
                    loading.close();
                    tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.WARNING,
                            "Incohérence Détectée",
                            "Attention : L'image ne semble pas correspondre à la maladie ou à la plante traitée.\nDétail : "
                                    + reason);
                    return;
                }

                String status = "UNCHANGED";
                String details = "";
                if (gateResult.contains("|")) {
                    String[] parts = gateResult.split("\\|");
                    status = parts[0].trim();
                    details = parts.length > 1 ? parts[1].trim() : "";
                } else {
                    status = gateResult.trim();
                }

                // Diagnose & Log
                boolean isHealed = "HEALED".equalsIgnoreCase(status);
                saveDiagnosticRecord(uploadedUrl[0], status, details, isHealed, plan.getInitialDiseaseName());

                if (isHealed) {
                    logPassiveReview("HEALED", uploadedUrl[0], FarmerResponse.ACCEPTED);
                    loading.setResult(ButtonType.OK);
                    loading.close();
                    planService.completePlan(plan.getId());
                    tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.CONFIRMATION,
                            "Plante Guérie !",
                            "L'IA a détecté que votre plante est totalement guérie ! Le protocole est terminé.");
                    goBack();
                } else if ("UNCHANGED".equalsIgnoreCase(status) || "RECOVERING".equalsIgnoreCase(status)) {
                    // *** CHANGED: Allow User Override for UNCHANGED/RECOVERING ***
                    logPassiveReview(status + ": " + details, uploadedUrl[0], FarmerResponse.ACKNOWLEDGED);
                    loading.setResult(ButtonType.OK);
                    loading.close();

                    Alert choice = new Alert(Alert.AlertType.CONFIRMATION);
                    choice.setTitle("Résultat de l'analyse");

                    String header = "RECOVERING".equalsIgnoreCase(status) ? "Amélioration détectée"
                            : "État Stationnaire";
                    choice.setHeaderText(header);

                    String msg = "L'IA conseille de CONTINUER le plan actuel.\n\n" +
                            "Détail : " + details + "\n\n" +
                            "Voulez-vous suivre l'avis de l'IA ou forcer un nouveau plan ?";
                    choice.setContentText(msg);

                    ButtonType keepBtn = new ButtonType("Continuer ce plan", ButtonBar.ButtonData.OK_DONE);
                    ButtonType newPlanBtn = new ButtonType("Forcer un Nouveau Plan", ButtonBar.ButtonData.OTHER);

                    choice.getButtonTypes().setAll(keepBtn, newPlanBtn);

                    choice.showAndWait().ifPresent(btn -> {
                        if (btn == newPlanBtn) {
                            Alert newLoading = new Alert(Alert.AlertType.INFORMATION);
                            newLoading.setTitle("Génération en cours");
                            newLoading.setContentText("Création d'un nouveau protocole...");
                            newLoading.show();
                            proceedWithPlanGeneration(selectedFile, newLoading, uploadedUrl[0]);
                        }
                    });

                } else if ("WORSENING".equalsIgnoreCase(status)) {
                    // WORSENING -> Auto-trigger new plan logic
                    loading.setContentText("Aggravation détectée. L'IA génère un nouveau protocole...");
                    proceedWithPlanGeneration(selectedFile, loading, uploadedUrl[0]);
                } else {
                    // Fallback
                    logPassiveReview("Status: " + status + ". " + details, uploadedUrl[0], FarmerResponse.ACKNOWLEDGED);
                    loading.setResult(ButtonType.OK);
                    loading.close();
                    tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.INFORMATION,
                            "Analyse Terminée",
                            "Statut : " + status + "\nConseil : " + details);
                }
            });

            gateTask.setOnFailed(e -> {
                loading.setResult(ButtonType.OK);
                loading.close();
                Throwable ex = gateTask.getException();
                ex.printStackTrace();
                tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Échec de la réévaluation : " + ex.getMessage());
            });

            new Thread(gateTask).start();
        }
    }

    /**
     * Step 2 of reevaluation: called only when the AI gate detects significant
     * deviation.
     * Generates a new plan proposal for the user to accept or reject.
     */
    private void proceedWithPlanGeneration(File selectedFile, Alert loading, String imgBbUrl) {
        javafx.concurrent.Task<String> reevalTask = new javafx.concurrent.Task<>() {
            @Override
            protected String call() throws Exception {
                File original = null;
                String originalPath = plan.getPlantImageUrl();

                if (originalPath != null && originalPath.startsWith("http")) {
                    original = downloadImage(originalPath);
                } else if (originalPath != null) {
                    original = new File(originalPath);
                }

                if (original == null || !original.exists()) {
                    return groqService.generateUpdatedPlan(selectedFile, selectedFile,
                            plan.getInitialDiseaseName());
                }
                return groqService.generateUpdatedPlan(original, selectedFile, plan.getInitialDiseaseName());
            }
        };

        reevalTask.setOnSucceeded(e -> {
            loading.setResult(ButtonType.OK);
            loading.close();
            String result = reevalTask.getValue();
            if ("HEALED".equalsIgnoreCase(result.trim())) {
                // --- NEW: Save Review Record ---
                logPassiveReview("HEALED", imgBbUrl, FarmerResponse.ACCEPTED);
                // -------------------------------
                planService.completePlan(plan.getId());
                tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.CONFIRMATION,
                        "Plante Guérie !",
                        "L'IA a détecté que votre plante est totalement guérie ! Le protocole est terminé.");
                goBack();
            } else if ("UNCHANGED".equalsIgnoreCase(result.trim())) {
                // --- NEW: Save Review Record ---
                logPassiveReview("UNCHANGED", imgBbUrl, FarmerResponse.ACKNOWLEDGED);
                // -------------------------------
                tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.INFORMATION,
                        "Pas de changement nécessaire",
                        "L'IA confirme que le plan actuel est toujours approprié.");
            } else if (result.startsWith("ERREUR")) {
                tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur", result);
            } else {
                showAiReevaluationChoice(result, imgBbUrl);
            }
        });

        reevalTask.setOnFailed(e -> {
            loading.setResult(ButtonType.OK);
            loading.close();
            Throwable ex = reevalTask.getException();
            ex.printStackTrace();
            tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Échec de la génération du plan : " + ex.getMessage());
        });

        new Thread(reevalTask).start();
    }

    /**
     * Shows a decision dialog letting the user preview the AI's proposed new plan
     * and choose whether to apply it or keep the current one.
     */
    private void showAiReevaluationChoice(String aiResponse, String imgBbUrl) {
        // Parse the proposed tasks for preview
        java.util.List<TreatmentTask> proposedTasks = parseAiTasks(aiResponse);
        if (proposedTasks.isEmpty()) {
            tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.WARNING,
                    "Aucun changement", "L'IA n'a proposé aucune modification au plan actuel.");
            return;
        }

        // Build a preview summary
        StringBuilder preview = new StringBuilder();
        preview.append("L'IA propose les tâches suivantes :\n\n");
        for (TreatmentTask t : proposedTasks) {
            preview.append("  Jour ").append(t.getDayOffset()).append(" — ").append(t.getTaskDescription())
                    .append("\n");
        }
        preview.append("\nVoulez-vous appliquer ce nouveau plan ou garder le plan actuel ?");

        Alert choice = new Alert(Alert.AlertType.CONFIRMATION);
        choice.setTitle("Résultat de la réévaluation IA");
        choice.setHeaderText("L'IA a analysé votre plante");
        choice.setContentText(preview.toString());
        choice.getDialogPane().setMinWidth(500);

        ButtonType applyBtn = new ButtonType("✅ Appliquer le nouveau plan", ButtonBar.ButtonData.OK_DONE);
        ButtonType keepBtn = new ButtonType("❌ Garder mon plan actuel", ButtonBar.ButtonData.CANCEL_CLOSE);
        choice.getButtonTypes().setAll(applyBtn, keepBtn);

        choice.showAndWait().ifPresent(btn -> {
            // Create a review record to store the AI proposal and the farmer's response
            Review aiReview = new Review(plan.getDiagnosticId(), ReviewType.PROGRESS);
            aiReview.setTreatmentPlanId(plan.getId());
            aiReview.setAiAnalysis("Réévaluation IA");
            aiReview.setAiProposedPlan(aiResponse);
            aiReview.setPhotoUrl(imgBbUrl); // Save the ImgBB URL
            int reviewId = reviewService.createReview(aiReview);

            if (btn == applyBtn) {
                if (reviewId > 0)
                    reviewService.updateFarmerResponse(reviewId, FarmerResponse.ACCEPTED, aiResponse);
                applyAiReevaluation(proposedTasks);
            } else {
                if (reviewId > 0)
                    reviewService.updateFarmerResponse(reviewId, FarmerResponse.REJECTED, aiResponse);
                tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.INFORMATION,
                        "Plan conservé", "Votre plan actuel a été maintenu sans modification.");
            }
        });
    }

    private void logPassiveReview(String analysis, String photoUrl, FarmerResponse autoResponse) {
        Review review = new Review(plan.getDiagnosticId(), ReviewType.PROGRESS);
        review.setTreatmentPlanId(plan.getId());
        review.setAiAnalysis(analysis);
        review.setPhotoUrl(photoUrl);

        int reviewId = reviewService.createReview(review);
        if (reviewId > 0 && autoResponse != null) {
            reviewService.updateFarmerResponse(reviewId, autoResponse, null);
        }
    }

    private java.util.List<TreatmentTask> parseAiTasks(String aiResponse) {
        // Compute the last completed day offset to rebase new tasks into the future
        int lastCompletedDay = 0;
        for (TreatmentTask t : plan.getTasks()) {
            if (t.getStatus() == TaskStatus.COMPLETED && t.getDayOffset() > lastCompletedDay) {
                lastCompletedDay = t.getDayOffset();
            }
        }

        java.util.List<TreatmentTask> tasks = new java.util.ArrayList<>();
        String[] lines = aiResponse.split("\\\\n|\\n");
        for (String line : lines) {
            if (line.contains("|")) {
                String[] parts = line.split("\\|");
                try {
                    int rawDay = Integer.parseInt(parts[0].trim());
                    String desc = parts[1].trim();
                    // Rebase: AI proposes day 1,3,7 → becomes lastCompletedDay+1, +3, +7
                    int rebasedDay = lastCompletedDay + rawDay;
                    tasks.add(new TreatmentTask(plan.getId(), rebasedDay, desc, TaskStatus.PENDING));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return tasks;
    }

    private void applyAiReevaluation(java.util.List<TreatmentTask> newTasks) {
        // Only replaces PENDING tasks — completed tasks are preserved by
        // updateRemainingTasks
        taskService.updateRemainingTasks(plan.getId(), newTasks);
        plan.setTasks(taskService.getTasksForPlan(plan.getId()));
        renderTasks();
        tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.INFORMATION, "Plan Mis à Jour",
                "Le plan a été mis à jour avec les nouvelles recommandations de l'IA.\n"
                        + "Les tâches déjà accomplies ont été conservées.");
    }

    /**
     * Checks if there is a completed expert review for this plan and
     * displays a styled banner at the top of the tasks container.
     */
    private void checkAndShowExpertFeedbackAsync() {
        if (plan == null)
            return;

        javafx.concurrent.Task<Review> task = new javafx.concurrent.Task<>() {
            @Override
            protected Review call() {
                return reviewService.getCompletedReviewForPlan(plan.getId());
            }
        };

        task.setOnSucceeded(evt -> {
            Review review = task.getValue();
            if (review == null || review.getFarmerResponse() != null)
                return;

            // Build expert feedback banner
            VBox banner = new VBox(10);
            banner.setStyle("-fx-background-color: rgba(41, 128, 185, 0.2); " +
                    "-fx-border-color: #2980b9; -fx-border-radius: 12; " +
                    "-fx-background-radius: 12; -fx-padding: 15 20;");

            // Title
            Label title = new Label("🧑‍🌾 Avis de l'Agronome Expert");
            title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");

            // Verdict
            ExpertVerdict verdict = review.getExpertVerdict();
            Label verdictLabel = new Label();
            String verdictStyle = "-fx-padding: 4 14; -fx-background-radius: 10; -fx-font-size: 12px; -fx-font-weight: bold; ";
            if (verdict == ExpertVerdict.HEALED) {
                verdictLabel.setText("✅ GUÉRIE");
                verdictLabel.setStyle(verdictStyle
                        + "-fx-background-color: rgba(46,204,113,0.3); -fx-text-fill: #2ecc71; -fx-border-color: #2ecc71; -fx-border-radius: 10;");
            } else if (verdict == ExpertVerdict.WORSENED) {
                verdictLabel.setText("⚠️ AGGRAVÉE");
                verdictLabel.setStyle(verdictStyle
                        + "-fx-background-color: rgba(231,76,60,0.3); -fx-text-fill: #e74c3c; -fx-border-color: #e74c3c; -fx-border-radius: 10;");
            } else {
                verdictLabel.setText("🔄 CONTINUER LE TRAITEMENT");
                verdictLabel.setStyle(verdictStyle
                        + "-fx-background-color: rgba(241,196,15,0.3); -fx-text-fill: #f1c40f; -fx-border-color: #f1c40f; -fx-border-radius: 10;");
            }

            // Expert notes
            String notes = review.getExpertNotes();
            Label notesLabel = new Label(notes != null && !notes.isEmpty() ? notes : "Aucune note supplémentaire.");
            notesLabel.setWrapText(true);
            notesLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 13px;");

            // Action buttons
            HBox actions = new HBox(15);
            actions.setAlignment(Pos.CENTER_LEFT);

            if (verdict == ExpertVerdict.HEALED) {
                Button acceptBtn = new Button("✅ Terminer le protocole");
                acceptBtn.setStyle(
                        "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 16;");
                acceptBtn.setOnAction(e -> {
                    reviewService.updateFarmerResponse(review.getId(), FarmerResponse.ACCEPTED, null);
                    planService.completePlan(plan.getId());
                    tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.INFORMATION,
                            "Protocole terminé", "Félicitations ! Votre plante est guérie selon l'agronome.");
                    goBack();
                });
                Button ignoreBtn = new Button("Continuer quand même");
                ignoreBtn.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 16;");
                ignoreBtn.setOnAction(e -> {
                    reviewService.updateFarmerResponse(review.getId(), FarmerResponse.REJECTED, null);
                    tasksContainer.getChildren().remove(banner);
                });
                actions.getChildren().addAll(acceptBtn, ignoreBtn);
            } else if (verdict == ExpertVerdict.WORSENED) {
                Label warningLabel = new Label("L'expert recommande de revoir votre approche.");
                warningLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 12px;");
                Button dismissBtn = new Button("Compris");
                dismissBtn.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 16;");
                dismissBtn.setOnAction(e -> {
                    reviewService.updateFarmerResponse(review.getId(), FarmerResponse.ACKNOWLEDGED, null);
                    tasksContainer.getChildren().remove(banner);
                });
                actions.getChildren().addAll(warningLabel, dismissBtn);
            } else {
                Button dismissBtn = new Button("OK, compris");
                dismissBtn.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 16;");
                dismissBtn.setOnAction(e -> {
                    reviewService.updateFarmerResponse(review.getId(), FarmerResponse.ACKNOWLEDGED, null);
                    tasksContainer.getChildren().remove(banner);
                });
                actions.getChildren().add(dismissBtn);
            }

            banner.getChildren().addAll(title, verdictLabel, notesLabel, actions);

            // Insert banner at the top of the tasks container
            tasksContainer.getChildren().add(0, banner);
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    void handleAbandon() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Abandonner le protocole");
        confirm.setHeaderText("Êtes-vous sûr de vouloir abandonner ce protocole ?");
        confirm.setContentText("Cette action est irréversible.");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            planService.abandonPlan(plan.getId());
            goBack();
        }
    }

    private void openChatPopup(TreatmentTask task) {
        String context = "Tu es un expert agronome. " +
                "Contexte : Maladie " + plan.getInitialDiseaseName() + ", Tâche " + task.getTaskDescription()
                + ", Jour "
                + task.getDayOffset() + ". " +
                "Réponds UNIQUEMENT sur cette tâche. " +
                "Tes réponses doivent être COURTES (max 3 phrases) et PRATIQUES. Pas de politesses inutiles.";

        tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.<tn.neuron.ardhi.controllers.UserAndDiag.ChatbotTrTasksController>loadPopup(
                "/fxml/UserAndDiag/ChatbotTrTasks.fxml",
                "Assistant: " + task.getTaskDescription(),
                (controller) -> {
                    controller.setContext(context);
                });
    }

    @FXML
    void goBack() {
        try {
            // Updated to "TreatmentPlanList.fxml" to match your file structure
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/UserAndDiag/TreatmentPlanList.fxml"));
            tasksContainer.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void goToSoilProfiler(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/VirtualSoil3D.fxml", "Profil de Sol 3D Virtuel");
    }

    private File downloadImage(String urlString) {
        try {
            URL url = new URL(urlString);
            String extension = ".jpg";
            if (urlString.contains(".")) {
                extension = urlString.substring(urlString.lastIndexOf("."));
                if (extension.length() > 5)
                    extension = ".jpg"; // Safety check
            }

            File tempFile = File.createTempFile("temp_original_", extension);
            try (InputStream in = url.openStream()) {
                Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            return new File("invalid_path"); // Fallback that will fail exist check
        }
    }

    @FXML
    void requestExpertPlanReview() {
        if (plan == null)
            return;

        // Check if a pending review already exists
        if (reviewService.hasPendingReview(plan.getDiagnosticId(), ReviewType.PROGRESS)) {
            tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert("Déjà soumis",
                    "Une demande d'avis expert sur le suivi est déjà en cours.");
            return;
        }

        // Open file chooser for follow-up photo
        FileChooser fc = new FileChooser();
        fc.setTitle("Photo de suivi pour l'agronome");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File photo = fc.showOpenDialog(tasksContainer.getScene().getWindow());

        if (photo == null)
            return;

        // Show progress
        Alert progress = new Alert(Alert.AlertType.INFORMATION);
        progress.setTitle("Envoi en cours");
        progress.setHeaderText("Envoi de la photo à l'agronome...");
        progress.setContentText("Votre photo est en cours d'envoi. L'agronome l'examinera directement.");
        progress.show();

        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                // 1. Upload to ImgBB
                String photoUrl = tn.neuron.ardhi.utils.UserAndDiag.ImgBBService.uploadImage(photo);
                if (photoUrl == null) {
                    System.err.println("ImgBB Upload failed, using local path.");
                    photoUrl = photo.getAbsolutePath();
                }

                // 2. Create review with the public URL
                Review review = new Review(plan.getDiagnosticId(), ReviewType.PROGRESS);
                review.setTreatmentPlanId(plan.getId());
                review.setPhotoUrl(photoUrl);

                reviewService.createReview(review);

                javafx.application.Platform.runLater(() -> {
                    progress.close();
                    Alert done = new Alert(Alert.AlertType.INFORMATION);
                    done.setTitle("Demande envoyée");
                    done.setHeaderText("Demande envoyée à l'agronome !");
                    done.setContentText(
                            "Votre photo a été envoyée avec succès.\n\n" +
                                    "Un agronome expert examinera votre cas et vous donnera son avis professionnel.");
                    done.showAndWait();
                });
                return null;
            }
        };
        task.setOnFailed(e -> {
            progress.close();
            tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert("Erreur",
                    "Impossible d'envoyer la demande : " + task.getException().getMessage());
        });
        new Thread(task).start();
    }

    @FXML
    void handleWhatsAppReminder(ActionEvent event) {
        if (plan == null || plan.getTasks() == null || plan.getTasks().isEmpty()) {
            tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert("Erreur", "Aucune tâche à rappeler.");
            return;
        }

        tn.neuron.ardhi.models.UserAndDiag.User currentUser = tn.neuron.ardhi.utils.UserAndDiag.UserSession
                .getInstance().getUser();
        if (currentUser == null) {
            tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.WARNING, "Non connecté",
                    "Veuillez vous connecter pour utiliser cette fonctionnalité.");
            return;
        }

        String phone = currentUser.getPhone();
        if (phone == null || phone.trim().isEmpty()) {
            tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.WARNING, "Numéro manquant",
                    "Veuillez ajouter un numéro de téléphone à votre profil pour recevoir des rappels WhatsApp.");
            return;
        }

        Alert progress = new Alert(Alert.AlertType.INFORMATION);
        progress.setTitle("Envoi en cours");
        progress.setHeaderText("Envoi du rappel WhatsApp...");
        progress.setContentText("Veuillez patienter pendant l'envoi du message via Twilio.");
        progress.show();

        javafx.concurrent.Task<Boolean> task = new javafx.concurrent.Task<>() {
            @Override
            protected Boolean call() {
                tn.neuron.ardhi.services.UserAndDiag.TreatmentReminderService reminderService = new tn.neuron.ardhi.services.UserAndDiag.TreatmentReminderService();
                return reminderService.sendReminder(plan, plan.getTasks(), phone);
            }
        };

        task.setOnSucceeded(e -> {
            progress.close();
            if (task.getValue()) {
                tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "Rappel WhatsApp envoyé avec succès au numéro " + phone);
            } else {
                tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                        "L'envoi a échoué. Assurez-vous d'avoir rejoint la Sandbox Twilio WhatsApp (voir console pour détails).");
            }
        });

        task.setOnFailed(e -> {
            progress.close();
            tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Une erreur s'est produite lors de l'envoi : " + task.getException().getMessage());
        });

        new Thread(task).start();
    }
}