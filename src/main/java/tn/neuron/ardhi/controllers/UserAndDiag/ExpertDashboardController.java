package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import tn.neuron.ardhi.models.UserAndDiag.*;
import tn.neuron.ardhi.services.UserAndDiag.*;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.text.SimpleDateFormat;
import java.util.List;

public class ExpertDashboardController {

    @FXML
    private ListView<Review> reviewListView;
    @FXML
    private VBox detailPanel;
    @FXML
    private VBox vboxNoSelection;
    @FXML
    private Label lblNoSelection;
    @FXML
    private Label lblNoSelectionSub;

    // --- Diagnosis review panel ---
    @FXML
    private VBox diagnosisPanel;
    @FXML
    private ImageView diagImage;
    @FXML
    private Label lblFarmerName;
    @FXML
    private Label lblAiDiagnosis;
    @FXML
    private Label lblDiagDate;
    @FXML
    private TextField txtExpertDiseaseName;
    @FXML
    private TextArea txtExpertNotes;

    // --- Progress/Prevention review panel ---
    @FXML
    private VBox progressPanel;
    @FXML
    private Label lblProgressTitle;
    @FXML
    private ImageView imgOriginal;
    @FXML
    private ImageView imgFollowUp;
    @FXML
    private HBox photoComparisonBox;
    @FXML
    private VBox aiAnalysisBox;
    @FXML
    private Label lblAiAnalysisTitle;
    @FXML
    private Label lblAiAnalysis;
    @FXML
    private Label lblProgressFarmer;
    @FXML
    private Label lblDiseaseName;
    @FXML
    private VBox taskListContainer;
    @FXML
    private TextArea txtProgressNotes;
    @FXML
    private ComboBox<ExpertVerdict> verdictCombo;

    // --- Add task controls ---
    @FXML
    private TextField txtNewTaskDay;
    @FXML
    private TextField txtNewTaskDescription;

    private final ReviewService reviewService = new ReviewService();
    private final TreatmentPlanService planService = new TreatmentPlanService();
    private final TreatmentTaskService taskService = new TreatmentTaskService();
    private final PreventionPlanService preventionPlanService = new PreventionPlanService();
    private final PreventionTaskService preventionTaskService = new PreventionTaskService();

    private Review selectedReview;

    @FXML
    public void initialize() {
        // Setup verdict combo
        verdictCombo.setItems(FXCollections.observableArrayList(ExpertVerdict.values()));

        // Setup list rendering
        reviewListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Review item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    VBox card = new VBox(5);
                    card.getStyleClass().add("card-review");

                    String icon = switch (item.getReviewType()) {
                        case DIAGNOSIS -> "🔬";
                        case PROGRESS -> "📊";
                        case PREVENTION -> "🛡";
                    };
                    String typeString = switch (item.getReviewType()) {
                        case DIAGNOSIS -> "Diagnostic";
                        case PROGRESS -> "Suivi";
                        case PREVENTION -> "Prévention";
                    };

                    Label headerLabel = new Label(icon + " " + typeString);
                    headerLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13;");

                    String farmer = item.getFarmerName() != null ? item.getFarmerName() : "Agriculteur inconnu";
                    Label farmerLabel = new Label("De: " + farmer);
                    farmerLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.8); -fx-font-size: 11;");

                    Label statusLabel = new Label(item.getStatus().name());
                    statusLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 10; -fx-font-weight: bold;");
                    if (item.getStatus().name().equals("APPROVED")) {
                        statusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 10; -fx-font-weight: bold;");
                    } else if (item.getStatus().name().equals("REJECTED")) {
                        statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10; -fx-font-weight: bold;");
                    }

                    card.getChildren().addAll(headerLabel, farmerLabel, statusLabel);

                    setText(null);
                    setGraphic(card);
                    setStyle("-fx-background-color: transparent; -fx-padding: 5 0;");
                }
            }
        });

        // Handle selection
        reviewListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedReview = newVal;
                showReviewDetails(newVal);
            }
        });

        loadPendingReviews();
    }

    private void loadPendingReviews() {
        List<Review> reviews = reviewService.getPendingReviews();
        reviewListView.setItems(FXCollections.observableArrayList(reviews));

        if (reviews.isEmpty()) {
            lblNoSelection.setText("Aucune demande d'avis en attente.");
            lblNoSelectionSub.setVisible(false);
            lblNoSelectionSub.setManaged(false);
        } else {
            lblNoSelection.setText("Sélectionnez un cas à examiner.");
            lblNoSelectionSub.setVisible(true);
            lblNoSelectionSub.setManaged(true);
        }
    }

    private void showReviewDetails(Review review) {
        vboxNoSelection.setVisible(false);
        vboxNoSelection.setManaged(false);

        if (review.getReviewType() == ReviewType.DIAGNOSIS) {
            showDiagnosisReview(review);
        } else {
            // PROGRESS and PREVENTION both use the same task list panel
            showProgressReview(review);
        }
    }

    private void showDiagnosisReview(Review review) {
        diagnosisPanel.setVisible(true);
        diagnosisPanel.setManaged(true);
        progressPanel.setVisible(false);
        progressPanel.setManaged(false);

        lblFarmerName.setText("Agriculteur: " + (review.getFarmerName() != null ? review.getFarmerName() : "N/A"));
        lblAiDiagnosis.setText(
                "Diagnostic IA: " + (review.getDiagnosisResult() != null ? review.getDiagnosisResult() : "N/A"));

        if (review.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            lblDiagDate.setText("Reçu le: " + sdf.format(review.getCreatedAt()));
        }

        // Load image
        if (review.getOriginalImageUrl() != null) {
            try {
                diagImage.setImage(new Image(review.getOriginalImageUrl(), true));
            } catch (Exception e) {
                LogUtils.warn(ExpertDashboardController.class, "Could not load image: " + e.getMessage());
            }
        }

        // Pre-fill with AI diagnosis
        txtExpertDiseaseName.setText(review.getDiagnosisResult() != null ? review.getDiagnosisResult() : "");
        txtExpertNotes.setText("");
    }

    private void showProgressReview(Review review) {
        diagnosisPanel.setVisible(false);
        diagnosisPanel.setManaged(false);
        progressPanel.setVisible(true);
        progressPanel.setManaged(true);

        boolean isPrevention = review.getReviewType() == ReviewType.PREVENTION;

        // --- Adapt title, labels, and visibility per type ---
        if (isPrevention) {
            lblProgressTitle.setText("🛡 Suivi de Prévention");
            lblDiseaseName.setVisible(false);
            lblDiseaseName.setManaged(false);
            photoComparisonBox.setVisible(false);
            photoComparisonBox.setManaged(false);
            lblAiAnalysisTitle.setText("📋 Résumé des tâches en attente:");
        } else {
            lblProgressTitle.setText("📊 Suivi de Traitement");
            lblDiseaseName.setVisible(true);
            lblDiseaseName.setManaged(true);
            photoComparisonBox.setVisible(true);
            photoComparisonBox.setManaged(true);
            lblAiAnalysisTitle.setText("🤖 Analyse IA:");
        }

        lblProgressFarmer.setText("Agriculteur: " + (review.getFarmerName() != null ? review.getFarmerName() : "N/A"));
        lblDiseaseName
                .setText("Maladie: " + (review.getDiagnosisResult() != null ? review.getDiagnosisResult() : "N/A"));
        lblAiAnalysis.setText(review.getAiAnalysis() != null ? review.getAiAnalysis() : "Analyse IA non disponible");

        // Load images (only for PROGRESS)
        if (!isPrevention) {
            if (review.getOriginalImageUrl() != null) {
                try {
                    imgOriginal.setImage(new Image(review.getOriginalImageUrl(), true));
                } catch (Exception e) {
                    LogUtils.warn(ExpertDashboardController.class, "Could not load original image");
                }
            }
            if (review.getPhotoUrl() != null) {
                try {
                    imgFollowUp.setImage(new Image(review.getPhotoUrl(), true));
                } catch (Exception e) {
                    LogUtils.warn(ExpertDashboardController.class, "Could not load follow-up image");
                }
            }
        }

        // Load tasks — treatment plan tasks or prevention plan tasks
        taskListContainer.getChildren().clear();
        if (review.getTreatmentPlanId() != null) {
            TreatmentPlan plan = planService.getPlanById(review.getTreatmentPlanId());
            if (plan != null && plan.getTasks() != null) {
                for (TreatmentTask task : plan.getTasks()) {
                    taskListContainer.getChildren().add(createTaskRow(task));
                }
            }
        } else if (review.getPreventionPlanId() != null) {
            PreventionPlan pPlan = preventionPlanService.getById(review.getPreventionPlanId());
            if (pPlan != null && pPlan.getTasks() != null) {
                for (PreventionTask task : pPlan.getTasks()) {
                    taskListContainer.getChildren().add(createPreventionTaskRow(task));
                }
            }
        }

        txtProgressNotes.setText("");
        verdictCombo.getSelectionModel().clearSelection();
    }

    private HBox createTaskRow(TreatmentTask task) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 8; -fx-padding: 10;");

        // Status icon
        String statusIcon = switch (task.getStatus()) {
            case COMPLETED -> "✅";
            case MISSED -> "❌";
            default -> "⏳";
        };

        Label lblDay = new Label("J" + task.getDayOffset());
        lblDay.setStyle("-fx-text-fill: #f1c40f; -fx-font-weight: bold; -fx-min-width: 35;");

        Label lblStatus = new Label(statusIcon);
        lblStatus.setStyle("-fx-font-size: 14;");

        TextField txtDesc = new TextField(task.getTaskDescription());
        txtDesc.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 5; -fx-background-radius: 5;");
        HBox.setHgrow(txtDesc, Priority.ALWAYS);
        txtDesc.setMinWidth(50);
        txtDesc.setPrefWidth(200);

        Button btnSave = new Button("💾");
        btnSave.setStyle(
                "-fx-background-color: rgba(39, 174, 96, 0.7); -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
        btnSave.setOnAction(e -> {
            taskService.updateTaskDescription(task.getId(), txtDesc.getText());
            showToast("Tâche mise à jour !");
        });

        Button btnDelete = new Button("🗑");
        btnDelete.setStyle(
                "-fx-background-color: rgba(231, 76, 60, 0.7); -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
        btnDelete.setOnAction(e -> {
            taskService.deleteTask(task.getId());
            taskListContainer.getChildren().remove(row);
            showToast("Tâche supprimée.");
        });

        // Only allow editing/deleting pending tasks
        if (task.getStatus() != TaskStatus.PENDING) {
            btnSave.setDisable(true);
            btnDelete.setDisable(true);
            txtDesc.setEditable(false);
        }

        row.getChildren().addAll(lblDay, lblStatus, txtDesc, btnSave, btnDelete);
        return row;
    }

    private HBox createPreventionTaskRow(PreventionTask task) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 8; -fx-padding: 10;");

        String statusIcon = switch (task.getStatus()) {
            case COMPLETED -> "✅";
            case MISSED -> "❌";
            default -> "⏳";
        };

        Label lblDay = new Label("J" + task.getDayOffset());
        lblDay.setStyle("-fx-text-fill: #f1c40f; -fx-font-weight: bold; -fx-min-width: 35;");

        Label lblStatus = new Label(statusIcon);
        lblStatus.setStyle("-fx-font-size: 14;");

        TextField txtDesc = new TextField(task.getTaskDescription());
        txtDesc.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 5; -fx-background-radius: 5;");
        HBox.setHgrow(txtDesc, Priority.ALWAYS);
        txtDesc.setMinWidth(50);
        txtDesc.setPrefWidth(200);

        Button btnSave = new Button("💾");
        btnSave.setStyle(
                "-fx-background-color: rgba(39, 174, 96, 0.7); -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
        btnSave.setOnAction(e -> {
            preventionTaskService.updateTaskDescription(task.getId(), txtDesc.getText());
            showToast("Tâche mise à jour !");
        });

        Button btnDelete = new Button("🗑");
        btnDelete.setStyle(
                "-fx-background-color: rgba(231, 76, 60, 0.7); -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
        btnDelete.setOnAction(e -> {
            preventionTaskService.deleteTask(task.getId());
            taskListContainer.getChildren().remove(row);
            showToast("Tâche supprimée.");
        });

        if (task.getStatus() != TaskStatus.PENDING) {
            btnSave.setDisable(true);
            btnDelete.setDisable(true);
            txtDesc.setEditable(false);
        }

        row.getChildren().addAll(lblDay, lblStatus, txtDesc, btnSave, btnDelete);
        return row;
    }

    @FXML
    void handleAddTask() {
        if (selectedReview == null) {
            WindowUtils.showAlert("Erreur", "Aucune revue sélectionnée.");
            return;
        }

        String dayStr = txtNewTaskDay.getText().trim();
        String desc = txtNewTaskDescription.getText().trim();

        if (dayStr.isEmpty() || desc.isEmpty()) {
            WindowUtils.showAlert("Champs requis", "Veuillez remplir le jour et la description.");
            return;
        }

        try {
            int day = Integer.parseInt(dayStr);

            if (selectedReview.getTreatmentPlanId() != null) {
                TreatmentTask newTask = new TreatmentTask(selectedReview.getTreatmentPlanId(), day, desc,
                        TaskStatus.PENDING);
                taskService.saveTask(newTask);
                taskListContainer.getChildren().add(createTaskRow(newTask));
            } else if (selectedReview.getPreventionPlanId() != null) {
                PreventionTask newTask = new PreventionTask(selectedReview.getPreventionPlanId(), day, desc);
                preventionTaskService.save(newTask);
                taskListContainer.getChildren().add(createPreventionTaskRow(newTask));
            } else {
                WindowUtils.showAlert("Erreur", "Aucun plan associé à cette revue.");
                return;
            }

            txtNewTaskDay.clear();
            txtNewTaskDescription.clear();
            showToast("Nouvelle tâche ajoutée !");
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "Le jour doit être un nombre entier.");
        }
    }

    @FXML
    void handleSubmitDiagnosis() {
        if (selectedReview == null)
            return;

        String diseaseName = txtExpertDiseaseName.getText().trim();
        String notes = txtExpertNotes.getText().trim();

        if (diseaseName.isEmpty()) {
            WindowUtils.showAlert("Champ requis", "Veuillez indiquer le nom de la maladie.");
            return;
        }

        int expertId = UserSession.getInstance().getUser().getId();
        reviewService.completeDiagnosisReview(selectedReview.getId(), expertId, notes, diseaseName);

        showToast("Avis diagnostic soumis avec succès !");
        loadPendingReviews();
        resetDetailPanel();
    }

    @FXML
    void handleSubmitProgress() {
        if (selectedReview == null)
            return;

        ExpertVerdict verdict = verdictCombo.getValue();
        String notes = txtProgressNotes.getText().trim();

        if (verdict == null) {
            WindowUtils.showAlert("Verdict requis", "Veuillez sélectionner un verdict (Continuer, Guéri, Aggravé).");
            return;
        }

        int expertId = UserSession.getInstance().getUser().getId();
        reviewService.completeProgressReview(selectedReview.getId(), expertId, notes, verdict);

        // If HEALED, complete the plan (treatment or prevention)
        if (verdict == ExpertVerdict.HEALED) {
            if (selectedReview.getTreatmentPlanId() != null) {
                planService.completePlan(selectedReview.getTreatmentPlanId());
            } else if (selectedReview.getPreventionPlanId() != null) {
                preventionPlanService.completePlan(selectedReview.getPreventionPlanId());
            }
            showToast("Plan marqué comme résolu et terminé !");
        } else {
            showToast("Avis de suivi soumis !");
        }

        loadPendingReviews();
        resetDetailPanel();
    }

    private void resetDetailPanel() {
        selectedReview = null;
        diagnosisPanel.setVisible(false);
        diagnosisPanel.setManaged(false);
        progressPanel.setVisible(false);
        progressPanel.setManaged(false);
        vboxNoSelection.setVisible(true);
        vboxNoSelection.setManaged(true);
        lblNoSelection.setText("Sélectionnez un cas à examiner.");
        lblNoSelectionSub.setVisible(true);
        lblNoSelectionSub.setManaged(true);
    }

    @FXML
    void handleRefresh() {
        loadPendingReviews();
        resetDetailPanel();
        showToast("Liste actualisée.");
    }

    @FXML
    void goBack(javafx.event.ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/LandingPage.fxml", "Ardhi - Accueil");
    }

    private void showToast(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}