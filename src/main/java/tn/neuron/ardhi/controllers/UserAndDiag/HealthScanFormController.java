package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import tn.neuron.ardhi.models.UserAndDiag.*;
import tn.neuron.ardhi.services.UserAndDiag.FarmHealthScanService;
import tn.neuron.ardhi.services.UserAndDiag.HealthScanAnalysisService;
import tn.neuron.ardhi.services.UserAndDiag.SpeechToTextService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.io.File;
import java.net.URL;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class HealthScanFormController implements Initializable {

    @FXML
    private ComboBox<String> comboCropType;
    @FXML
    private DatePicker dpPlantingDate;
    @FXML
    private ComboBox<String> comboGrowthStage;
    @FXML
    private TextArea txtConcerns;
    @FXML
    private Button btnLaunchScan;
    @FXML
    private HBox progressBox;
    @FXML
    private Label lblProgress;
    @FXML
    private Label lblPhotoCount;

    // Photo ImageViews
    @FXML
    private ImageView imgCrops;
    @FXML
    private ImageView imgSoil;
    @FXML
    private ImageView imgEdges;
    @FXML
    private ImageView imgInsects;
    @FXML
    private ImageView imgSpacing;
    @FXML
    private ImageView imgOverview;

    // Photo VBoxes (for border changes)
    @FXML
    private VBox boxPhotoCrops;
    @FXML
    private VBox boxPhotoSoil;
    @FXML
    private VBox boxPhotoEdges;
    @FXML
    private VBox boxPhotoInsects;
    @FXML
    private VBox boxPhotoSpacing;
    @FXML
    private VBox boxPhotoOverview;

    // Photo Labels
    @FXML
    private Label lblCrops;
    @FXML
    private Label lblSoil;
    @FXML
    private Label lblEdges;
    @FXML
    private Label lblInsects;
    @FXML
    private Label lblSpacing;
    @FXML
    private Label lblOverview;

    private Map<String, File> selectedPhotos = new HashMap<>();
    private FarmHealthScanService scanService;
    private HealthScanAnalysisService analysisService;
    private final SpeechToTextService sttService = new SpeechToTextService();

    @FXML
    private Button btnMicConcerns;

    public HealthScanFormController() {
        try {
            scanService = new FarmHealthScanService();
        } catch (Exception e) {
            System.err.println("CRITICAL: Failed to init FarmHealthScanService");
            e.printStackTrace();
        }

        try {
            analysisService = new HealthScanAnalysisService();
        } catch (Exception e) {
            System.err.println("CRITICAL: Failed to init HealthScanAnalysisService");
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Populate crop types
        comboCropType.getItems().addAll(
                "Tomates", "Blé", "Olives", "Agrumes", "Pommes de terre",
                "Piments", "Oignons", "Fraises", "Vigne", "Pastèque",
                "Melon", "Courgette", "Haricots", "Pois chiches", "Orge",
                "Laitue", "Autre");

        // Populate growth stages
        comboGrowthStage.getItems().addAll(
                "Semis", "Germination", "Croissance végétative", "Floraison",
                "Fructification", "Maturation", "Récolte");

        // Enable scan button when minimum data is present
        comboCropType.setOnAction(e -> checkReadiness());
        dpPlantingDate.setOnAction(e -> checkReadiness());
        comboGrowthStage.setOnAction(e -> checkReadiness());
    }

    private void checkReadiness() {
        boolean ready = comboCropType.getValue() != null
                && dpPlantingDate.getValue() != null
                && comboGrowthStage.getValue() != null
                && !selectedPhotos.isEmpty();
        btnLaunchScan.setDisable(!ready);
    }

    // ── Photo Selection Handlers ──

    @FXML
    void selectPhotoCrops(MouseEvent event) {
        selectPhoto("crops", imgCrops, lblCrops, boxPhotoCrops);
    }

    @FXML
    void selectPhotoSoil(MouseEvent event) {
        selectPhoto("soil", imgSoil, lblSoil, boxPhotoSoil);
    }

    @FXML
    void selectPhotoEdges(MouseEvent event) {
        selectPhoto("edges", imgEdges, lblEdges, boxPhotoEdges);
    }

    @FXML
    void selectPhotoInsects(MouseEvent event) {
        selectPhoto("insects", imgInsects, lblInsects, boxPhotoInsects);
    }

    @FXML
    void selectPhotoSpacing(MouseEvent event) {
        selectPhoto("spacing", imgSpacing, lblSpacing, boxPhotoSpacing);
    }

    @FXML
    void selectPhotoOverview(MouseEvent event) {
        selectPhoto("overview", imgOverview, lblOverview, boxPhotoOverview);
    }

    private void selectPhoto(String type, ImageView imgView, Label label, VBox box) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Sélectionner une photo — " + type);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.bmp"));
        File file = chooser.showOpenDialog(null);
        if (file != null) {
            selectedPhotos.put(type, file);
            imgView.setImage(new Image(file.toURI().toString()));
            box.setStyle(
                    "-fx-border-color: rgba(39,174,96,0.8); -fx-border-style: solid; -fx-border-radius: 12; -fx-border-width: 2; -fx-background-color: rgba(39,174,96,0.15); -fx-background-radius: 12; -fx-cursor: hand;");
            label.setText("✅ " + getPhotoLabel(type));
            updatePhotoCount();
        }
    }

    private String getPhotoLabel(String type) {
        switch (type) {
            case "crops":
                return "Cultures";
            case "soil":
                return "Sol";
            case "edges":
                return "Bordures";
            case "insects":
                return "Insectes";
            case "spacing":
                return "Espacement";
            case "overview":
                return "Vue d'ensemble";
            default:
                return type;
        }
    }

    private void updatePhotoCount() {
        lblPhotoCount.setText(selectedPhotos.size() + "/6 photos importées");
        checkReadiness();
    }

    // ── Launch Scan ──

    @FXML
    void launchScan(ActionEvent event) {
        if (UserSession.getInstance() == null || UserSession.getInstance().getUser() == null) {
            showAlert("Erreur", "Vous devez être connecté pour effectuer un scan.");
            return;
        }

        if (selectedPhotos.isEmpty()) {
            showAlert("Attention", "Veuillez importer au moins une photo de votre champ.");
            return;
        }

        // Disable UI
        btnLaunchScan.setDisable(true);
        comboCropType.setDisable(true);
        dpPlantingDate.setDisable(true);
        comboGrowthStage.setDisable(true);
        txtConcerns.setDisable(true);
        progressBox.setVisible(true);
        progressBox.setManaged(true);

        // Create scan object
        FarmHealthScan scan = new FarmHealthScan();
        scan.setUserId(UserSession.getInstance().getUser().getId());
        scan.setCropType(comboCropType.getValue());
        scan.setPlantingDate(Date.valueOf(dpPlantingDate.getValue()));
        scan.setGrowthStage(comboGrowthStage.getValue());
        scan.setConcerns(txtConcerns.getText());
        scan.setStatus(ScanStatus.PENDING);

        // Save scan first
        int scanId = scanService.save(scan);
        if (scanId <= 0) {
            showAlert("Erreur", "Impossible de sauvegarder le scan.");
            resetUI();
            return;
        }

        // Run analysis in background
        Task<FarmHealthReport> analyzeTask = new Task<>() {
            @Override
            protected FarmHealthReport call() {
                updateMessage("Téléchargement des photos...");
                return analysisService.processScan(scanId, selectedPhotos);
            }
        };

        analyzeTask.messageProperty().addListener((obs, old, msg) -> lblProgress.setText(msg));

        analyzeTask.setOnSucceeded(e -> {
            FarmHealthReport report = analyzeTask.getValue();
            if (report != null) {
                navigateToReport(report);
            } else {
                showAlert("Erreur", "Le scan a échoué. Veuillez réessayer.");
                resetUI();
            }
        });

        analyzeTask.setOnFailed(e -> {
            showAlert("Erreur", "Erreur lors de l'analyse : " +
                    (analyzeTask.getException() != null ? analyzeTask.getException().getMessage() : "Inconnue"));
            resetUI();
        });

        new Thread(analyzeTask).start();
    }

    private void navigateToReport(FarmHealthReport report) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserAndDiag/HealthScanReport.fxml"));
            Parent root = loader.load();
            HealthScanReportController controller = loader.getController();
            controller.initData(report);
            btnLaunchScan.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetUI() {
        btnLaunchScan.setDisable(false);
        comboCropType.setDisable(false);
        dpPlantingDate.setDisable(false);
        comboGrowthStage.setDisable(false);
        txtConcerns.setDisable(false);
        progressBox.setVisible(false);
        progressBox.setManaged(false);
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    @FXML
    void goBack(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/ClientDashboard.fxml", "Tableau de Bord");
    }

    // ── Voice Input ──

    @FXML
    void dictateConcerns(ActionEvent event) {
        if (sttService.isRecording()) {
            btnMicConcerns.setText("⏳");
            btnMicConcerns.setDisable(true);
            sttService.stopAndTranscribe(
                    text -> {
                        String existing = txtConcerns.getText();
                        txtConcerns.setText(existing.isEmpty() ? text : existing + " " + text);
                        btnMicConcerns.setText("🎙️");
                        btnMicConcerns.setDisable(false);
                    },
                    error -> {
                        btnMicConcerns.setText("🎙️");
                        btnMicConcerns.setDisable(false);
                    });
        } else {
            sttService.startRecording();
            btnMicConcerns.setText("⏹️");
            btnMicConcerns.setStyle(
                    "-fx-background-color: #e74c3c; -fx-background-radius: 20; -fx-cursor: hand; -fx-min-width: 35; -fx-min-height: 35;");
        }
    }
}
