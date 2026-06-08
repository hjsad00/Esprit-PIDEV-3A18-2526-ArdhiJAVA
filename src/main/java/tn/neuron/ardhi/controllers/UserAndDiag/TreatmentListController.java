package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.TilePane;
import tn.neuron.ardhi.models.UserAndDiag.TreatmentPlan;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;
import java.io.File;

public class TreatmentListController implements Initializable {

    @FXML
    private TilePane treatmentContainer;

    @FXML
    private ScrollPane scrollPane;

    private tn.neuron.ardhi.services.UserAndDiag.TreatmentPlanService treatmentPlanService = new tn.neuron.ardhi.services.UserAndDiag.TreatmentPlanService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadTreatments();
    }

    private void loadTreatments() {
        treatmentContainer.getChildren().clear();
        Label loadingLabel = new Label("Chargement des traitements en cours...");
        loadingLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-style: italic;");
        treatmentContainer.getChildren().add(loadingLabel);

        javafx.concurrent.Task<List<TreatmentPlan>> loadTask = new javafx.concurrent.Task<>() {
            @Override
            protected List<TreatmentPlan> call() {
                int userId = UserSession.getInstance().getUser().getId();
                return treatmentPlanService.getActivePlansByUser(userId);
            }
        };

        loadTask.setOnSucceeded(e -> {
            treatmentContainer.getChildren().clear();
            List<TreatmentPlan> plans = loadTask.getValue();

            if (plans == null || plans.isEmpty()) {
                Label emptyLabel = new Label("Aucun traitement actif trouvé.");
                emptyLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                treatmentContainer.getChildren().add(emptyLabel);
                return;
            }

            for (TreatmentPlan plan : plans) {
                AnchorPane item = createTreatmentCard(plan);
                treatmentContainer.getChildren().add(item);
            }
        });

        loadTask.setOnFailed(e -> {
            treatmentContainer.getChildren().clear();
            Label errorLabel = new Label("Erreur de chargement: " + loadTask.getException().getMessage());
            errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
            treatmentContainer.getChildren().add(errorLabel);
            loadTask.getException().printStackTrace();
        });

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    private AnchorPane createTreatmentCard(TreatmentPlan plan) {
        AnchorPane card = new AnchorPane();
        card.setPrefSize(230, 265);
        card.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.15); -fx-background-radius: 15; -fx-border-color: rgba(255, 255, 255, 0.2); -fx-border-radius: 15; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 5);");

        VBox vbox = new VBox(10);
        vbox.setAlignment(Pos.TOP_CENTER);
        AnchorPane.setTopAnchor(vbox, 10.0);
        AnchorPane.setBottomAnchor(vbox, 10.0);
        AnchorPane.setLeftAnchor(vbox, 10.0);
        AnchorPane.setRightAnchor(vbox, 10.0);

        // Image
        ImageView imgView = new ImageView();
        imgView.setFitHeight(120);
        imgView.setFitWidth(200);
        imgView.setPreserveRatio(true);
        imgView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 1);");

        if (plan.getPlantImageUrl() != null && !plan.getPlantImageUrl().isEmpty()) {
            final String path = plan.getPlantImageUrl();
            new Thread(() -> {
                try {
                    String finalUrl = path;
                    if (!path.startsWith("http")) {
                        File file = new File(path);
                        if (file.exists()) {
                            finalUrl = file.toURI().toString();
                        }
                    }
                    Image img = new Image(finalUrl, true);
                    javafx.application.Platform.runLater(() -> imgView.setImage(img));
                } catch (Exception e) {
                }
            }).start();
        }

        // Disease Name
        Label nameLbl = new Label(plan.getInitialDiseaseName());
        nameLbl.setMinHeight(40);
        nameLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        nameLbl.setTextFill(javafx.scene.paint.Color.WHITE);
        nameLbl.setWrapText(true);
        nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        // Date
        String dateStr = plan.getStartDate() != null ? plan.getStartDate().toString().substring(0, 10) : "?";
        Label dateLbl = new Label("Débuté le: " + dateStr);
        dateLbl.setTextFill(javafx.scene.paint.Color.rgb(255, 255, 255, 0.7));
        dateLbl.setStyle("-fx-font-size: 11px;");

        // Progress
        // Calculate progress (completed tasks / total tasks) from pre-calculated fields
        int total = plan.getTotalTasks();
        int completed = plan.getCompletedTasks();
        int percent = total > 0 ? (int) ((completed * 100) / total) : 0;

        Label progressLbl = new Label(percent + "% complété");
        progressLbl.setTextFill(javafx.scene.paint.Color.web("#2ecc71")); // Green
        progressLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        vbox.getChildren().addAll(imgView, nameLbl, dateLbl, progressLbl);

        // Status Tag (if Completed)
        if ("COMPLETED".equalsIgnoreCase(plan.getStatus().name())) {
            Label statusTag = new Label("TERMINÉ");
            statusTag.setStyle(
                    "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-padding: 3 10; -fx-background-radius: 5; -fx-font-weight: bold; -fx-font-size: 10px;");
            VBox.setMargin(statusTag, new javafx.geometry.Insets(5, 0, 0, 0));
            vbox.getChildren().add(statusTag);
        }

        card.getChildren().add(vbox);

        // Click event
        card.setOnMouseClicked(e -> openPlanDetails(plan));

        return card;
    }

    // Callback to open details
    private void openPlanDetails(TreatmentPlan plan) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserAndDiag/TreatmentPlanDetails.fxml"));
            Parent root = loader.load();

            TreatmentPlanDetailsController controller = loader.getController();
            controller.initData(plan);

            // Navigate (Replace current view or open new window - assuming replacing
            // content in main layout)
            // If this is part of a larger layout, we might need access to the main content
            // pane.
            // For now, let's assume we replace the root of the scene or we have a main
            // container.
            // A common pattern is getting the scene's root or a specific container.

            // Hacky navigation for now:
            treatmentContainer.getScene().setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Unused prevention plan logic removed

    @FXML
    void goBack(javafx.event.ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/ClientDashboard.fxml", "Tableau de Bord");
    }

    @FXML
    void viewCalendar(javafx.event.ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/CalendarView.fxml", "Calendrier des Traitements");
    }
}
