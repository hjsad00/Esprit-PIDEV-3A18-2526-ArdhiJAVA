package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import tn.neuron.ardhi.models.UserAndDiag.*;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;

public class HealthScanReportController implements Initializable {

    @FXML
    private Label lblHealthScore;
    @FXML
    private Label lblScoreLabel;
    @FXML
    private ProgressBar barHealthScore;
    @FXML
    private Label lblBioScore;
    @FXML
    private ProgressBar barBioScore;
    @FXML
    private Label lblCropInfo;
    @FXML
    private Label lblVulnCount;
    @FXML
    private Label lblPlanCount;
    @FXML
    private Label lblScanDate;
    @FXML
    private VBox vulnContainer;
    @FXML
    private VBox planContainer;
    @FXML
    private Label lblNoVulns;

    private FarmHealthReport report;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Data will be set via initData()
    }

    /**
     * Called after FXMLLoader.load() to populate the report view.
     */
    public void initData(FarmHealthReport report) {
        this.report = report;

        // Health Score
        int hs = report.getHealthScore();
        lblHealthScore.setText(hs + "/100");
        lblHealthScore.setTextFill(Color.web(report.getScoreColor()));
        barHealthScore.setProgress(hs / 100.0);
        lblScoreLabel.setText(report.getScoreLabel());

        // Bio Score
        int bs = report.getBiodiversityScore();
        lblBioScore.setText(bs + "/100");
        barBioScore.setProgress(bs / 100.0);

        // Scan Info
        lblCropInfo.setText("🌱 " + (report.getCropType() != null ? report.getCropType() : "—") +
                " — " + (report.getGrowthStage() != null ? report.getGrowthStage() : ""));

        List<Vulnerability> vulns = report.getVulnerabilities();
        lblVulnCount.setText("⚠️ " + (vulns != null ? vulns.size() : 0) + " vulnérabilités détectées");

        List<PreventionPlan> plans = report.getPreventionPlans();
        lblPlanCount.setText("🛡 " + (plans != null ? plans.size() : 0) + " plans de prévention générés");

        if (report.getGeneratedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            lblScanDate.setText("📅 Scan effectué le " + sdf.format(report.getGeneratedAt()));
        }

        // Populate vulnerabilities
        if (vulns == null || vulns.isEmpty()) {
            lblNoVulns.setVisible(true);
            lblNoVulns.setManaged(true);
        } else {
            for (Vulnerability v : vulns) {
                vulnContainer.getChildren().add(createVulnCard(v));
            }
        }

        // Populate prevention plans
        if (plans != null) {
            for (PreventionPlan plan : plans) {
                planContainer.getChildren().add(createPlanCard(plan));
            }
        }
    }

    private VBox createVulnCard(Vulnerability vuln) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 12; " +
                "-fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 12;");

        // Header row: icon + threat + severity badge
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label(vuln.getTypeIcon());
        icon.setStyle("-fx-font-size: 20;");

        Label threat = new Label(vuln.getThreat());
        threat.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
        threat.setWrapText(true);

        String sevColor = vuln.getSeverity() == Severity.CRITICAL ? "#e74c3c"
                : vuln.getSeverity() == Severity.MEDIUM ? "#f39c12" : "#27ae60";
        Label sevBadge = new Label(vuln.getSeverity().name());
        sevBadge.setStyle("-fx-background-color: " + sevColor + "; -fx-text-fill: white; " +
                "-fx-padding: 2 8; -fx-background-radius: 5; -fx-font-size: 10; -fx-font-weight: bold;");

        header.getChildren().addAll(icon, threat, sevBadge);

        // Description
        Label desc = new Label(vuln.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 12;");

        // Details row
        HBox details = new HBox(15);
        details.setAlignment(Pos.CENTER_LEFT);

        if (vuln.getRiskScore() > 0) {
            details.getChildren().add(makeDetail("Risque", String.format("%.0f%%", vuln.getRiskScore() * 100)));
        }
        if (vuln.getTimeframeDays() > 0) {
            details.getChildren().add(makeDetail("Délai", vuln.getTimeframeDays() + " jours"));
        }
        if (vuln.getEstimatedYieldLossPercent() > 0) {
            details.getChildren().add(makeDetail("Perte", vuln.getEstimatedYieldLossPercent() + "%"));
        }
        if (vuln.getEstimatedCostIfOccurs() > 0) {
            details.getChildren().add(makeDetail("Coût", String.format("%.0f TND", vuln.getEstimatedCostIfOccurs())));
        }

        card.getChildren().addAll(header, desc);
        if (!details.getChildren().isEmpty())
            card.getChildren().add(details);

        return card;
    }

    private Label makeDetail(String label, String value) {
        Label lbl = new Label(label + ": " + value);
        lbl.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 11;");
        return lbl;
    }

    private VBox createPlanCard(PreventionPlan plan) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 12; " +
                "-fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 12; -fx-cursor: hand;");

        // Title
        Label title = new Label("🛡 " + plan.getTitle());
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
        title.setWrapText(true);

        // Info row
        HBox info = new HBox(12);
        info.setAlignment(Pos.CENTER_LEFT);
        info.getChildren().add(makeDetail("Durée", plan.getTimelineDays() + " jours"));
        if (plan.getImpactLevel() != null) {
            String impactColor = "HIGH".equals(plan.getImpactLevel()) ? "#e74c3c" : "#f39c12";
            Label impact = new Label("Impact: " + plan.getImpactLevel());
            impact.setStyle("-fx-text-fill: " + impactColor + "; -fx-font-size: 11; -fx-font-weight: bold;");
            info.getChildren().add(impact);
        }
        if (plan.getTotalTasks() > 0) {
            info.getChildren().add(makeDetail("Tâches", plan.getCompletedTasks() + "/" + plan.getTotalTasks()));
        }

        // Progress bar
        double progress = plan.getTotalTasks() > 0 ? (double) plan.getCompletedTasks() / plan.getTotalTasks() : 0;
        ProgressBar bar = new ProgressBar(progress);
        bar.setPrefWidth(Double.MAX_VALUE);
        bar.setStyle("-fx-accent: #27ae60;");

        // View details button
        Button btnView = new Button("📋 Voir le Plan");
        btnView.setStyle("-fx-background-color: rgba(52, 152, 219, 0.7); -fx-background-radius: 6; " +
                "-fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 12;");
        btnView.setOnAction(e -> openPlanDetails(plan));

        card.getChildren().addAll(title, info, bar, btnView);

        // Click to open
        card.setOnMouseClicked(e -> openPlanDetails(plan));

        return card;
    }

    private void openPlanDetails(PreventionPlan plan) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserAndDiag/PreventionPlanDetails.fxml"));
            Parent root = loader.load();
            PreventionPlanDetailsController controller = loader.getController();
            controller.initData(plan, report.getScanId());
            lblHealthScore.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void rescan(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/HealthScanForm.fxml", "Scan Santé");
    }

    @FXML
    void goBack(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/ClientDashboard.fxml", "Tableau de Bord");
    }
}
