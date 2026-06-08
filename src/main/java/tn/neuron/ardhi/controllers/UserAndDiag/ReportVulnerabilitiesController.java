package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import tn.neuron.ardhi.models.UserAndDiag.FarmHealthReport;
import tn.neuron.ardhi.models.UserAndDiag.PreventionPlan;
import tn.neuron.ardhi.models.UserAndDiag.Severity;
import tn.neuron.ardhi.models.UserAndDiag.Vulnerability;
import tn.neuron.ardhi.services.UserAndDiag.HealthScanAnalysisService;
import tn.neuron.ardhi.services.UserAndDiag.PreventionPlanService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.text.SimpleDateFormat;
import java.util.List;

public class ReportVulnerabilitiesController {

    @FXML
    private Label lblScanDate;
    @FXML
    private Label lblCropInfo;
    @FXML
    private Label lblHealthScore;
    @FXML
    private FlowPane vulnContainer;
    @FXML
    private Label lblNoVulns;

    private HealthScanAnalysisService analysisService;
    private PreventionPlanService planService;
    private int scanId;
    private int reportId;

    public ReportVulnerabilitiesController() {
        analysisService = new HealthScanAnalysisService();
        planService = new PreventionPlanService();
    }

    public void initData(int scanId) {
        this.scanId = scanId;
        FarmHealthReport report = analysisService.getReportByScan(scanId);
        if (report != null) {
            this.reportId = report.getId();
        }

        if (report == null) {
            WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur", "Rapport introuvable pour ce scan.");
            return;
        }

        // Setup Header Data
        if (report.getGeneratedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            lblScanDate.setText("Scan du: " + sdf.format(report.getGeneratedAt()));
        }

        lblCropInfo.setText("Culture: " + (report.getCropType() != null ? report.getCropType() : "Inconnue"));
        lblHealthScore.setText("Score: " + report.getHealthScore() + "/100");
        lblHealthScore.setStyle(
                "-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 5 15; -fx-background-radius: 20; -fx-background-color: rgba(255, 255, 255, 0.8); -fx-text-fill: "
                        + report.getScoreColor() + "; -fx-border-color: " + report.getScoreColor()
                        + "; -fx-border-radius: 20;");

        // Populate Vulnerabilities
        List<Vulnerability> vulnerabilities = report.getVulnerabilities();

        // Retain only the 'No vulns label'
        vulnContainer.getChildren().clear();
        vulnContainer.getChildren().add(lblNoVulns);

        if (vulnerabilities == null || vulnerabilities.isEmpty()) {
            lblNoVulns.setVisible(true);
            lblNoVulns.setManaged(true);
        } else {
            lblNoVulns.setVisible(false);
            lblNoVulns.setManaged(false);

            for (Vulnerability vuln : vulnerabilities) {
                vulnContainer.getChildren().add(createVulnCard(vuln));
            }
        }
    }

    private AnchorPane createVulnCard(Vulnerability vuln) {
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

        // Icon instead of Image
        Label icon = new Label(vuln.getTypeIcon());
        icon.setMinHeight(100);
        icon.setStyle("-fx-font-size: 72px;");

        // Threat Name
        Label nameLbl = new Label(vuln.getThreat());
        nameLbl.setMinHeight(50);
        nameLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        nameLbl.setTextFill(javafx.scene.paint.Color.WHITE);
        nameLbl.setWrapText(true);
        nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // Severity / Probability
        String sevColor = vuln.getSeverity() == Severity.CRITICAL ? "#e74c3c"
                : vuln.getSeverity() == Severity.MEDIUM ? "#f39c12" : "#2ecc71";

        Label dateLbl = new Label("Gravité: " + vuln.getSeverity().name());
        dateLbl.setTextFill(javafx.scene.paint.Color.web(sevColor));
        dateLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        String prob = vuln.getRiskScore() > 0 ? String.format("%.0f%% Probabilité", vuln.getRiskScore() * 100) : "";
        Label progressLbl = new Label(prob);
        progressLbl.setTextFill(javafx.scene.paint.Color.rgb(255, 255, 255, 0.7));
        progressLbl.setStyle("-fx-font-size: 11px;");

        vbox.getChildren().addAll(icon, nameLbl, dateLbl, progressLbl);

        // Status Tag
        if (vuln.getEstimatedYieldLossPercent() > 0) {
            Label statusTag = new Label("Perte: " + vuln.getEstimatedYieldLossPercent() + "%");
            statusTag.setStyle(
                    "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 3 10; -fx-background-radius: 5; -fx-font-weight: bold; -fx-font-size: 10px;");
            VBox.setMargin(statusTag, new javafx.geometry.Insets(5, 0, 0, 0));
            vbox.getChildren().add(statusTag);
        }

        card.getChildren().add(vbox);

        card.setOnMouseClicked(e -> openPreventionPlanFor(vuln));

        // Hover Effect
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.25); -fx-background-radius: 15; -fx-border-color: rgba(255, 255, 255, 0.4); -fx-border-radius: 15; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 15, 0, 0, 8); -fx-translate-y: -2;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.15); -fx-background-radius: 15; -fx-border-color: rgba(255, 255, 255, 0.2); -fx-border-radius: 15; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 5); -fx-translate-y: 0;"));

        return card;
    }

    private void openPreventionPlanFor(Vulnerability vuln) {
        // Fetch the corresponding prevention plan for this vulnerability
        List<PreventionPlan> allPlans = planService.getByReport(vuln.getReportId());

        PreventionPlan targetPlan = allPlans.stream()
                .filter(p -> p.getVulnerabilityId() == vuln.getId())
                .findFirst()
                .orElse(null);

        if (targetPlan == null) {
            WindowUtils.showAlert(Alert.AlertType.INFORMATION, "Information",
                    "Aucun plan de prévention n'a été généré spécifiquement pour cette vulnérabilité.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserAndDiag/PreventionPlanDetails.fxml"));
            Parent root = loader.load();
            PreventionPlanDetailsController controller = loader.getController();

            controller.initData(targetPlan, scanId); // Passing scanId so "Go Back" returns to the history/report
                                                     // vulnerabilities

            lblScanDate.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le plan de prévention: " + e.getMessage());
        }
    }

    @FXML
    void viewCalendar() {
        try {
            PreventionCalendarViewController.setReportContext(reportId, scanId);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserAndDiag/PreventionCalendarView.fxml"));
            Parent root = loader.load();
            lblScanDate.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserAndDiag/HealthScanHistory.fxml"));
            Parent root = loader.load();
            lblScanDate.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
