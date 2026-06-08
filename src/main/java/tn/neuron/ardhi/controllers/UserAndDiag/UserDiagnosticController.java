package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import tn.neuron.ardhi.models.UserAndDiag.Traitement;
import tn.neuron.ardhi.models.UserAndDiag.TypeTraitement;
import tn.neuron.ardhi.models.UserAndDiag.Review;
import tn.neuron.ardhi.models.UserAndDiag.ReviewType;
import tn.neuron.ardhi.services.UserAndDiag.TreatmentPlanService;
import tn.neuron.ardhi.services.UserAndDiag.ReviewService;
import tn.neuron.ardhi.models.UserAndDiag.Offre;
import tn.neuron.ardhi.models.UserAndDiag.Diagnostic;
import tn.neuron.ardhi.services.UserAndDiag.AbonnementService;
import tn.neuron.ardhi.services.UserAndDiag.DiagnosticService;
import tn.neuron.ardhi.services.UserAndDiag.TraitementService;
import tn.neuron.ardhi.services.UserAndDiag.GroqService;
import tn.neuron.ardhi.services.UserAndDiag.GamificationService;
import tn.neuron.ardhi.services.UserAndDiag.LocationService;
import tn.neuron.ardhi.services.UserAndDiag.WeatherService;
import tn.neuron.ardhi.services.UserAndDiag.WeatherAlertService;
import tn.neuron.ardhi.services.UserAndDiag.EpidemicAlertService;
import tn.neuron.ardhi.utils.UserAndDiag.ImgBBService;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;
import tn.neuron.ardhi.utils.UserAndDiag.SubscriptionConfig;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;
import tn.neuron.ardhi.services.UserAndDiag.SpeechToTextService;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import tn.neuron.ardhi.services.UserAndDiag.SmartARService;

import java.io.File;

public class UserDiagnosticController {

    @FXML
    private ImageView imgPreview;
    @FXML
    private Label lblPlaceholder, lblMaladieTitre, lblConfiance, lblTraitement;
    @FXML
    private Label lblStatusAbonnement;
    @FXML
    private Button btnScanner, btnStartTreatment;
    @FXML
    private VBox boxResultat;

    // Camera UI Elements
    @FXML
    private VBox paneImagePreview;
    @FXML
    private javafx.scene.layout.StackPane paneCamera;
    @FXML
    private ImageView cameraView;
    @FXML
    private Button btnCapture;
    @FXML
    private Button btnCamera;
    @FXML
    private Canvas arOverlay;

    private tn.neuron.ardhi.services.UserAndDiag.CameraService cameraService = new tn.neuron.ardhi.services.UserAndDiag.CameraService();
    private SmartARService smartARService = new SmartARService();
    private javafx.animation.Timeline cameraLoop;

    @FXML
    private ProgressBar barConfiance;
    @FXML
    private Label lblWeatherContext;
    @FXML
    private Label lblRegionalContext;

    private File selectedImageFile;
    private final GroqService groqService = new GroqService();
    private final TraitementService traitementService = new TraitementService();
    private final TreatmentPlanService recoveryService = new TreatmentPlanService();
    private final AbonnementService abonnementService = new AbonnementService();
    private final DiagnosticService diagnosticService = new DiagnosticService();
    private final GamificationService gamificationService = new GamificationService();
    private final LocationService locationService = new LocationService();
    private final ReviewService reviewService = new ReviewService();
    private final WeatherService weatherService = new WeatherService();
    private final WeatherAlertService weatherAlertService = new WeatherAlertService();
    private final EpidemicAlertService epidemicService = new EpidemicAlertService();

    private String pPlante, pMaladie, pTraitement, pType, pDescription, pSeverity;
    private float pConfiance;
    private int lastDiagnosticId = -1;

    // Voice Commands
    @FXML
    private Button btnVoiceCmd;
    private final SpeechToTextService sttService = new SpeechToTextService();
    private volatile boolean voiceCommandsActive = false;
    private Thread voiceCommandThread;

    @FXML
    void importerImage(ActionEvent event) {
        stopCamera(); // Ensure camera is off
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        selectedImageFile = fc.showOpenDialog(null);
        if (selectedImageFile != null) {
            imgPreview.setImage(new Image(selectedImageFile.toURI().toString()));
            lblPlaceholder.setVisible(false);
            btnScanner.setDisable(false);

            // Switch view
            paneImagePreview.setVisible(true);
            paneImagePreview.setManaged(true);
            paneCamera.setVisible(false);
            paneCamera.setManaged(false);
            btnCapture.setVisible(false);
            btnCapture.setManaged(false);
        }
    }

    @FXML
    void toggleCameraMode(ActionEvent event) {
        // Toggle Logic
        boolean isCameraActive = paneCamera.isVisible();

        if (isCameraActive) {
            // Turn OFF
            stopCamera();
            paneCamera.setVisible(false);
            paneCamera.setManaged(false);
            paneImagePreview.setVisible(true);
            paneImagePreview.setManaged(true);
            btnCapture.setVisible(false);
            btnCapture.setManaged(false);
            btnCamera.setText("📷 Caméra");
        } else {
            // Turn ON
            paneImagePreview.setVisible(false);
            paneImagePreview.setManaged(false);
            paneCamera.setVisible(true);
            paneCamera.setManaged(true);
            btnCapture.setVisible(true);
            btnCapture.setManaged(true);
            btnCamera.setText("❌ Fermer Cam");

            WindowUtils.showCameraSourcePrompt(cameraService,
                    this::startCameraLoop,
                    () -> toggleCameraMode(null));
        }
    }

    private void startCameraLoop() {
        if (cameraLoop != null)
            cameraLoop.stop();

        // Ensure AR overlay is clear
        if (arOverlay != null) {
            arOverlay.getGraphicsContext2D().clearRect(0, 0, arOverlay.getWidth(), arOverlay.getHeight());
        }

        cameraLoop = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.millis(33), e -> {
            Image img = cameraService.takeSnapshot();
            if (img != null) {
                cameraView.setImage(img);

                // --- AR PROCESSING ---
                SmartARService.ARResult arResult = smartARService.processFrame(img);
                drawAR(arResult);
            }
        }));
        cameraLoop.setCycleCount(javafx.animation.Animation.INDEFINITE);
        cameraLoop.play();
    }

    private void drawAR(SmartARService.ARResult result) {
        if (arOverlay == null)
            return;
        GraphicsContext gc = arOverlay.getGraphicsContext2D();
        double w = arOverlay.getWidth();
        double h = arOverlay.getHeight();

        // Get actual image dimensions for scaling
        Image img = cameraView.getImage();
        double imgW = (img != null) ? img.getWidth() : 320;
        double imgH = (img != null) ? img.getHeight() : 240;

        double scaleX = w / imgW;
        double scaleY = h / imgH;

        gc.clearRect(0, 0, w, h);

        if (result.targetLocked) {
            // Draw Target Box/Reticle around centroid
            double cx = result.plantCentroidX * scaleX;
            double cy = result.plantCentroidY * scaleY;

            // Pulsating size: base 60, oscillates +/- 5
            double lockTime = System.currentTimeMillis() / 1000.0;
            double size = 60 + Math.sin(lockTime * 5.0) * 5;

            gc.setStroke(Color.LIME);
            gc.setLineWidth(3);

            // Corners
            double len = 15;
            // Top Left
            gc.strokeLine(cx - size, cy - size, cx - size + len, cy - size);
            gc.strokeLine(cx - size, cy - size, cx - size, cy - size + len);
            // Top Right
            gc.strokeLine(cx + size, cy - size, cx + size - len, cy - size);
            gc.strokeLine(cx + size, cy - size, cx + size, cy - size + len);
            // Bottom Left
            gc.strokeLine(cx - size, cy + size, cx - size + len, cy + size);
            gc.strokeLine(cx - size, cy + size, cx - size, cy + size - len);
            // Bottom Right
            gc.strokeLine(cx + size, cy + size, cx + size - len, cy + size);
            gc.strokeLine(cx + size, cy + size, cx + size, cy + size - len);

            // Label
            gc.setFill(Color.LIME);
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 14));
            gc.fillText("VÉGÉTATION DÉTECTÉE", cx - 60, cy - size - 10);

            // Stats
            gc.setFill(Color.WHITE);
            gc.fillText(String.format("COUVERTURE: %.1f%%", result.vegetationCoverage * 100), 10, h - 30);

            // Stress Points
            gc.setFill(new Color(1.0, 0.64, 0.0, 0.5)); // Semi-transparent Orange heat zone
            for (SmartARService.Point p : result.stressPoints) {
                double px = p.x * scaleX;
                double py = p.y * scaleY;
                gc.fillOval(px - 15, py - 15, 30, 30);
            }
            if (!result.stressPoints.isEmpty()) {
                gc.setFill(Color.ORANGE);
                gc.fillText("ZONES DE STRESS POTENTIEL", 10, h - 15);
            }

        } else {
            // Scanning Mode
            gc.setStroke(Color.RED); // Changed to RED for visibility
            gc.setLineWidth(3); // Thicker line
            gc.setLineDashes(10);
            double time = System.currentTimeMillis() / 1000.0;
            double scanY = (time % 2.0) / 2.0 * h;
            gc.strokeLine(0, scanY, w, scanY);

            gc.setFill(Color.RED);
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 12));
            gc.fillText("RECHERCHE DE VÉGÉTATION...", 10, 20);
        }
    }

    private void stopCamera() {
        if (cameraLoop != null)
            cameraLoop.stop();

        if (arOverlay != null) {
            arOverlay.getGraphicsContext2D().clearRect(0, 0, arOverlay.getWidth(), arOverlay.getHeight());
        }

        cameraService.stopCamera();
    }

    @FXML
    void handleCapture() {
        if (cameraService.isOpen()) {
            Image snapshot = cameraService.takeSnapshot();
            if (snapshot != null) {
                saveSnapshotToTemp(snapshot);

                // Show captured image in preview
                imgPreview.setImage(snapshot);
                lblPlaceholder.setVisible(false);
                btnScanner.setDisable(false);

                // Switch back to preview mode
                stopCamera();
                paneCamera.setVisible(false);
                paneCamera.setManaged(false);
                paneImagePreview.setVisible(true);
                paneImagePreview.setManaged(true);
                btnCapture.setVisible(false);
                btnCapture.setManaged(false);
                btnCamera.setText("📷 Caméra");

                WindowUtils.showAlert("Succès", "Photo capturée ! Vous pouvez lancer le diagnostic.");
            }
        }
    }

    private void saveSnapshotToTemp(Image image) {
        try {
            File temp = File.createTempFile("diag_capture", ".png");
            java.awt.image.BufferedImage bImg = javafx.embed.swing.SwingFXUtils.fromFXImage(image, null);
            javax.imageio.ImageIO.write(bImg, "png", temp);
            selectedImageFile = temp;
        } catch (Exception e) {
            e.printStackTrace();
            WindowUtils.showAlert("Erreur", "Impossible de sauvegarder la capture.");
        }
    }

    @FXML
    void lancerScan(ActionEvent event) {
        if (selectedImageFile == null)
            return;

        // Check diagnosis limit based on subscription
        try {
            int userId = UserSession.getInstance().getUser().getId();
            Offre offre = abonnementService.getOffreActiveParUser(userId);

            int maxDiagnostics = SubscriptionConfig.getFreeDiagnosticsParHeure(); // Default for non-subscribers
            if (offre != null) {
                maxDiagnostics = offre.getDiagnosticsParHeure(); // -1 = unlimited
            }

            // Check limit only if not unlimited
            if (maxDiagnostics != -1) {
                int diagCount = diagnosticService.compterDiagnosticsHeure(userId);
                if (diagCount >= maxDiagnostics) {
                    WindowUtils.showAlert("Limite atteinte",
                            "Vous avez atteint la limite de " + maxDiagnostics
                                    + " diagnostics par heure.\n\nPassez à une offre supérieure pour plus de diagnostics !");
                    return;
                }
            }
        } catch (Exception e) {
            // Allow scan if check fails
        }

        btnScanner.setDisable(true);
        boxResultat.setVisible(false);

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return groqService.analyserImage(selectedImageFile);
            }
        };

        task.setOnSucceeded(e -> {
            String res = task.getValue();
            if (res != null && !res.startsWith("ERREUR")) {
                parseAI(res);
                updateUI();
                sauvegarderTout();
                loadDiagnosticContext(); // Load weather + regional context
                updateSubscriptionStatus(); // Refresh counter after save
            } else {
                WindowUtils.showAlert("Erreur API", "Détails: " + res);
            }
            btnScanner.setDisable(false);
        });

        task.setOnFailed(e -> {
            btnScanner.setDisable(false);
            WindowUtils.showAlert("Erreur", "L'analyse a échoué.");
        });

        new Thread(task).start();
    }

    private void parseAI(String res) {
        // Clean up markdown code blocks if present
        res = res.replace("```", "").trim();

        String[] parts = res.split("\\|");
        if (parts.length >= 6) {
            pPlante = parts[0].trim();
            pMaladie = parts[1].trim();
            try {
                // Extract digits only for confidence
                String confStr = parts[2].replace("%", "").replaceAll("[^0-9.]", "").trim();
                pConfiance = Float.parseFloat(confStr);
            } catch (Exception e) {
                pConfiance = 50;
            }
            pTraitement = parts[3].trim(); // Nom commercial du traitement
            pType = parts[4].trim().toUpperCase();
            pDescription = parts[5].trim(); // Description brève du traitement
            // Limit type length to fit database column
            if (pType.length() > 50) {
                pType = pType.substring(0, 50);
            }
            // Parse severity if provided (7th field)
            if (parts.length >= 7) {
                pSeverity = parts[6].trim().toUpperCase();
                // Normalize severity values
                if (!pSeverity.equals("CRITICAL") && !pSeverity.equals("MEDIUM") && !pSeverity.equals("LOW")) {
                    // Default based on confidence if not recognized
                    pSeverity = pConfiance >= 80 ? "CRITICAL" : (pConfiance >= 50 ? "MEDIUM" : "LOW");
                }
            } else {
                // Default severity based on confidence
                pSeverity = pConfiance >= 80 ? "CRITICAL" : (pConfiance >= 50 ? "MEDIUM" : "LOW");
            }
        } else {
            // Fallback parsing or error handling
            pPlante = "Non identifiée";
            pMaladie = "Inconnue";
            pTraitement = res;
            pType = "INFO";
            pDescription = "";
            pConfiance = 0;
            pSeverity = "LOW";
        }
    }

    @FXML
    public void initialize() {
        if (lblStatusAbonnement == null) {
            LogUtils.error(getClass(), "CRITICAL: lblStatusAbonnement failed to inject!");
        }
        updateSubscriptionStatus();
    }

    private void updateSubscriptionStatus() {
        if (lblStatusAbonnement == null)
            return;

        try {
            UserSession session = UserSession.getInstance();
            if (session == null || session.getUser() == null) {
                lblStatusAbonnement.setText("");
                return;
            }

            int userId = session.getUser().getId();
            Offre offre = abonnementService.getOffreActiveParUser(userId);

            int maxDiagnostics;
            boolean isSubscribed = (offre != null);

            if (offre != null) {
                maxDiagnostics = offre.getDiagnosticsParHeure();
            } else {
                maxDiagnostics = SubscriptionConfig.getFreeDiagnosticsParHeure();
            }

            String statusText;
            if (maxDiagnostics == -1) {
                statusText = isSubscribed ? "✅ Diagnostics illimités" : "⏱ Diagnostics illimités (gratuit)";
            } else {
                int used = diagnosticService.compterDiagnosticsHeure(userId);
                int remaining = Math.max(0, maxDiagnostics - used);

                if (remaining == 0) {
                    // Show when next diagnostic becomes available
                    String resetTime = diagnosticService.getProchainResetTime(userId);
                    statusText = (isSubscribed ? "✅ " : "⏱ ") + "0/" + maxDiagnostics + " - Prochain dispo: "
                            + resetTime;
                } else {
                    statusText = (isSubscribed ? "✅ " : "⏱ ") + remaining + "/" + maxDiagnostics + " diagnostics";
                    if (!isSubscribed)
                        statusText += " (gratuit)";
                }
            }

            if (offre != null && offre.isAccesTraitement()) {
                statusText += ", accès traitement";
            }

            lblStatusAbonnement.setText(statusText);
            lblStatusAbonnement.setStyle(isSubscribed
                    ? "-fx-text-fill: #6B7F3F; -fx-background-color: #E8F5E9; -fx-background-radius: 8; -fx-padding: 8 15;"
                    : "-fx-text-fill: #C4A574; -fx-background-color: #FFF8E1; -fx-background-radius: 8; -fx-padding: 8 15;");
        } catch (Exception e) {
            LogUtils.error(getClass(), "Error updating subscription status", e);
            if (lblStatusAbonnement != null) {
                lblStatusAbonnement.setText("");
            }
        }
    }

    @FXML
    void resetUI(ActionEvent event) {
        selectedImageFile = null;
        imgPreview.setImage(null);
        lblPlaceholder.setVisible(true);
        btnScanner.setDisable(true);
        boxResultat.setVisible(false);
        // boxUpload remains visible
    }

    private void updateUI() {
        // boxUpload remains visible
        boxResultat.setVisible(true);
        lblMaladieTitre.setText(pPlante + " : " + pMaladie);
        barConfiance.setProgress(pConfiance / 100.0);
        lblConfiance.setText(pConfiance + "%");

        // Check subscription or free tier for treatment visibility
        try {
            int userId = UserSession.getInstance().getUser().getId();
            Offre offre = abonnementService.getOffreActiveParUser(userId);

            boolean canSeeTreatment = false;
            if (offre != null && offre.isAccesTraitement()) {
                canSeeTreatment = true;
            } else if (offre == null && SubscriptionConfig.isFreeAccesTraitement()) {
                canSeeTreatment = true; // Free tier has treatment access
            }

            if (canSeeTreatment) {
                lblTraitement.setText(pTraitement + " (" + pType + ")");
            } else {
                lblTraitement.setText("🔒 Abonnez-vous pour voir le traitement recommandé.");
            }
        } catch (Exception e) {
            lblTraitement.setText(pTraitement + " (" + pType + ")");
        }
    }

    /**
     * Loads weather context and regional epidemic context after a diagnosis.
     * Runs asynchronously to avoid blocking the UI.
     */
    private void loadDiagnosticContext() {
        new Thread(() -> {
            try {
                // Detect location
                LocationService.LocationData loc = locationService.detectLocation();
                double lat = (loc != null && loc.latitude != null) ? loc.latitude : 36.8065;
                double lon = (loc != null && loc.longitude != null) ? loc.longitude : 10.1815;

                // Weather context
                WeatherService.WeatherData weather = weatherService.getCurrentWeather(lat, lon);
                String weatherContext = weatherAlertService.getWeatherDiagnosticContext(weather, pMaladie);

                // Regional context
                java.util.List<EpidemicAlertService.RegionalDisease> diseases = epidemicService.getActiveDiseases(lat,
                        lon, 10);
                String regionalContext = null;
                if (diseases != null) {
                    for (EpidemicAlertService.RegionalDisease d : diseases) {
                        if (pMaladie != null
                                && d.diseaseName.toLowerCase().contains(pMaladie.toLowerCase().split(" ")[0])) {
                            regionalContext = String.format(
                                    "🌍 %d autre(s) agriculteur(s) dans un rayon de 10km ont signalé cette maladie récemment.",
                                    d.reportCount);
                            break;
                        }
                    }
                    if (regionalContext == null && !diseases.isEmpty()) {
                        regionalContext = String.format("🌍 %d maladie(s) active(s) dans votre région (rayon 10km).",
                                diseases.size());
                    }
                }

                final String wCtx = weatherContext;
                final String rCtx = regionalContext;

                Platform.runLater(() -> {
                    if (lblWeatherContext != null) {
                        if (wCtx != null) {
                            lblWeatherContext.setText(wCtx);
                            lblWeatherContext.setVisible(true);
                            lblWeatherContext.setManaged(true);
                        } else {
                            lblWeatherContext.setVisible(false);
                            lblWeatherContext.setManaged(false);
                        }
                    }
                    if (lblRegionalContext != null) {
                        if (rCtx != null) {
                            lblRegionalContext.setText(rCtx);
                            lblRegionalContext.setVisible(true);
                            lblRegionalContext.setManaged(true);
                        } else {
                            lblRegionalContext.setVisible(false);
                            lblRegionalContext.setManaged(false);
                        }
                    }
                });
            } catch (Exception e) {
                LogUtils.error(getClass(), "Error loading diagnostic context", e);
            }
        }).start();
    }

    private void sauvegarderTout() {
        // Run in background to avoid freezing UI during upload
        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String imagePathStockee;

                // 1. Try ImgBB Upload
                String imgUrl = ImgBBService.uploadImage(selectedImageFile);
                if (imgUrl != null) {
                    imagePathStockee = imgUrl;
                } else {
                    // 2. Fallback to local storage (Original File Path)
                    LogUtils.warn(getClass(), "ImgBB upload failed, falling back to local file path.");
                    imagePathStockee = selectedImageFile.getAbsolutePath();
                }

                Diagnostic d = new Diagnostic(
                        imagePathStockee,
                        pPlante + " - " + pMaladie,
                        pConfiance,
                        UserSession.getInstance().getUser().getId());

                // Capture location
                try {
                    LocationService.LocationData location = locationService.detectLocation();
                    if (location != null) {
                        d.setLatitude(location.latitude);
                        d.setLongitude(location.longitude);
                        d.setLocationLabel(location.label);
                    }
                } catch (Exception e) {
                    LogUtils.error(getClass(), "Location detection failed", e);
                }

                d.setSeverityFromString(pSeverity);

                int diagId = diagnosticService.ajouterEtRecupererId(d);

                if (diagId != -1) {
                    lastDiagnosticId = diagId; // Capture ID for treatment plan
                    TypeTraitement typeEnum;
                    try {
                        typeEnum = TypeTraitement.valueOf(pType);
                    } catch (IllegalArgumentException e) {
                        typeEnum = TypeTraitement.AUTRE;
                    }
                    traitementService.ajouter(new Traitement(diagId, pTraitement, pDescription, typeEnum));

                    int userId = UserSession.getInstance().getUser().getId();
                    gamificationService.addPoints(userId, 50);
                    gamificationService.checkDiagnosticBadges(userId);
                    gamificationService.checkHealthyBadges(userId);
                }
                return null;
            }
        };

        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            ex.printStackTrace();
            WindowUtils.showAlert("Erreur Sauvegarde", ex.getMessage());
        });

        // Start the task
        new Thread(saveTask).start();
    }

    @FXML
    void openChat(ActionEvent event) {
        // Construct RAG Context
        String context = "Tu es un expert agronome. Contexte : " +
                "Plante: " + pPlante + ", Maladie: " + pMaladie + ", Traitement: " + pTraitement + ". " +
                "Réponds aux questions sur ce diagnostic. " +
                "Tes réponses doivent être COURTES (max 50 mots) et DIRECTES. Pas de bla-bla.";

        WindowUtils.loadScene(event, "/fxml/UserAndDiag/ChatbotDiag.fxml", "Assistant IA - " + pPlante,
                (ChatbotDiagController chatCtrl) -> chatCtrl.setContext(context));
    }

    @FXML
    void askCommunity(ActionEvent event) {
        String defaultTitle = "Aide pour " + pPlante;
        String defaultDesc = "L'IA a détecté : " + pMaladie + " (Confiance: " + pConfiance + "%).\n" +
                "Je ne suis pas sûr de ce résultat. Qu'en pensez-vous ?";

        WindowUtils.loadScene(event, "/fxml/UserAndDiag/CreatePost.fxml", "Créer un post",
                (CreatePostController ctrl) -> ctrl.initData(defaultTitle, defaultDesc, selectedImageFile));
    }

    @FXML
    void retourDashboard(ActionEvent event) {
        stopCamera();
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/ClientDashboard.fxml", "Tableau de Bord");
    }

    @FXML
    void startTreatment(ActionEvent event) {
        try {
            int userId = UserSession.getInstance().getUser().getId();
            Offre offre = abonnementService.getOffreActiveParUser(userId);

            boolean hasAccess = false;
            if (offre != null && offre.isAccesPlanTraitement()) {
                hasAccess = true;
            } else if (offre == null && SubscriptionConfig.isFreeAccesPlanTraitement()) {
                hasAccess = true;
            }

            if (!hasAccess) {
                WindowUtils.showAlert("Accès Limité",
                        "Le protocole de soin IA est une fonctionnalité Premium.\n" +
                                "Abonnez-vous à l'une de nos offres pour en bénéficier !");
                return;
            }
        } catch (Exception e) {
            // Allow if check fails for any reason
        }

        if (lastDiagnosticId != -1) {
            // Create plan
            recoveryService.createRecoveryPlan(new Diagnostic(lastDiagnosticId, null, null, null, 0, 0), pMaladie);

            // Navigate to Treatment List
            WindowUtils.loadScene(event, "/fxml/UserAndDiag/TreatmentPlanList.fxml", "Mes Protocoles de Soin");
        } else {
            WindowUtils.showAlert("Erreur", "Veuillez d'abord effectuer un diagnostic.");
        }
    }

    @FXML
    void requestExpertReview(ActionEvent event) {
        if (lastDiagnosticId == -1) {
            WindowUtils.showAlert("Erreur", "Veuillez d'abord effectuer un diagnostic.");
            return;
        }

        // Check if a review already exists
        if (reviewService.hasPendingReview(lastDiagnosticId, ReviewType.DIAGNOSIS)) {
            WindowUtils.showAlert("Déjà soumis",
                    "Une demande d'avis expert est déjà en cours pour ce diagnostic.");
            return;
        }

        Review review = new Review(lastDiagnosticId, ReviewType.DIAGNOSIS);
        int reviewId = reviewService.createReview(review);

        if (reviewId > 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Demande envoyée");
            alert.setHeaderText("Votre demande a été soumise !");
            alert.setContentText(
                    "Un agronome expert examinera votre diagnostic prochainement.\n" +
                            "Vous serez notifié lorsque l'avis sera disponible.");
            alert.showAndWait();
        } else {
            WindowUtils.showAlert("Erreur", "Impossible d'envoyer la demande. Veuillez réessayer.");
        }
    }

    // ── Voice Commands ──

    @FXML
    void toggleVoiceCommands(ActionEvent event) {
        if (voiceCommandsActive) {
            // Stop voice commands
            voiceCommandsActive = false;
            if (sttService.isRecording()) {
                sttService.stopAndTranscribe(text -> {
                }, error -> {
                });
            }
            btnVoiceCmd.setText("🎤 Mains-libres");
            btnVoiceCmd.setStyle(
                    "-fx-background-color: rgba(142, 68, 173, 0.7); -fx-background-radius: 10; -fx-text-fill: white; -fx-cursor: hand;");
        } else {
            // Start voice command loop
            voiceCommandsActive = true;
            btnVoiceCmd.setText("⏹ Arrêter");
            btnVoiceCmd.setStyle(
                    "-fx-background-color: #e74c3c; -fx-background-radius: 10; -fx-text-fill: white; -fx-cursor: hand;");
            listenForNextCommand();
        }
    }

    private void listenForNextCommand() {
        if (!voiceCommandsActive)
            return;

        sttService.startRecording();

        // Auto-stop after 3 seconds for command listening
        voiceCommandThread = new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
                return;
            }
            if (voiceCommandsActive && sttService.isRecording()) {
                sttService.stopAndTranscribe(
                        text -> {
                            String cmd = text.toLowerCase().trim();
                            LogUtils.info(getClass(), "Voice command: " + cmd);
                            if (cmd.contains("capturer") || cmd.contains("photo") || cmd.contains("capture")) {
                                handleCapture();
                            } else if (cmd.contains("scanner") || cmd.contains("diagnostic")
                                    || cmd.contains("analyser")) {
                                lancerScan(null);
                            } else if (cmd.contains("retour") || cmd.contains("quitter")) {
                                retourDashboard(null);
                                voiceCommandsActive = false;
                                return;
                            }
                            // Listen again
                            if (voiceCommandsActive) {
                                listenForNextCommand();
                            }
                        },
                        error -> {
                            // Retry on error
                            if (voiceCommandsActive) {
                                listenForNextCommand();
                            }
                        });
            }
        });
        voiceCommandThread.setDaemon(true);
        voiceCommandThread.start();
    }
}