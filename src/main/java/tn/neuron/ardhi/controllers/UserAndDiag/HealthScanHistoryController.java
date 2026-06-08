package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import tn.neuron.ardhi.models.UserAndDiag.FarmHealthReport;
import tn.neuron.ardhi.models.UserAndDiag.FarmHealthScan;
import tn.neuron.ardhi.services.UserAndDiag.FarmHealthScanService;
import tn.neuron.ardhi.services.UserAndDiag.HealthScanAnalysisService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class HealthScanHistoryController implements Initializable {

    @FXML
    private TextField tfRecherche;

    @FXML
    private TableView<FarmHealthScan> tableHistorique;

    @FXML
    private TableColumn<FarmHealthScan, String> colDate;

    @FXML
    private TableColumn<FarmHealthScan, String> colAiAnalysis;

    @FXML
    private TableColumn<FarmHealthScan, String> colHealthScore;

    @FXML
    private Label lblInfo;

    private final FarmHealthScanService scanService = new FarmHealthScanService();
    private final HealthScanAnalysisService analysisService = new HealthScanAnalysisService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private int currentUserId;
    private List<FarmHealthScan> allScans;
    private Map<Integer, FarmHealthReport> reportCache = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUserId = UserSession.getInstance().getUser().getId();

        // Configuration des colonnes
        colDate.setCellValueFactory(cellData -> {
            if (cellData.getValue().getScanDate() != null) {
                return new SimpleStringProperty(dateFormat.format(cellData.getValue().getScanDate()));
            }
            return new SimpleStringProperty("N/A");
        });

        colAiAnalysis.setCellValueFactory(cellData -> {
            FarmHealthReport r = reportCache.get(cellData.getValue().getId());
            String aiAnalysis = "En attente...";
            if (r != null && r.getLlavaAnalysis() != null) {
                aiAnalysis = r.getLlavaAnalysis();
                if (aiAnalysis.length() > 80) {
                    aiAnalysis = aiAnalysis.substring(0, 77) + "...";
                }
            }
            return new SimpleStringProperty(aiAnalysis);
        });

        colHealthScore.setCellValueFactory(cellData -> {
            FarmHealthReport r = reportCache.get(cellData.getValue().getId());
            if (r != null) {
                return new SimpleStringProperty(String.valueOf(r.getHealthScore()));
            }
            return new SimpleStringProperty("Non évalué");
        });

        chargerHistorique();

        tfRecherche.textProperty().addListener((observable, oldValue, newValue) -> {
            rechercherScans(newValue);
        });

        // Double click to view associated vulnerabilities
        tableHistorique.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                viewReportVulnerabilities();
            }
        });
    }

    private void chargerHistorique() {
        allScans = scanService.getByUser(currentUserId);

        // Pre-load all reports into cache (1 query per scan instead of per-cell)
        reportCache.clear();
        for (FarmHealthScan scan : allScans) {
            FarmHealthReport report = analysisService.getReportByScan(scan.getId());
            if (report != null) {
                reportCache.put(scan.getId(), report);
            }
        }

        tableHistorique.setItems(FXCollections.observableArrayList(allScans));
        mettreAJourInfo(allScans.size());
    }

    private void rechercherScans(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            tableHistorique.setItems(FXCollections.observableArrayList(allScans));
            mettreAJourInfo(allScans.size());
        } else {
            String lowerKw = keyword.toLowerCase();
            List<FarmHealthScan> filtered = allScans.stream()
                    .filter(s -> {
                        boolean matchCrop = s.getCropType() != null && s.getCropType().toLowerCase().contains(lowerKw);
                        FarmHealthReport r = reportCache.get(s.getId());
                        boolean matchAi = r != null && r.getLlavaAnalysis() != null
                                && r.getLlavaAnalysis().toLowerCase().contains(lowerKw);
                        return matchCrop || matchAi;
                    })
                    .collect(Collectors.toList());
            tableHistorique.setItems(FXCollections.observableArrayList(filtered));
            mettreAJourInfo(filtered.size());
        }
    }

    private void mettreAJourInfo(int count) {
        if (count == 0) {
            lblInfo.setText("Aucun scan trouvé");
        } else if (count == 1) {
            lblInfo.setText("1 scan trouvé");
        } else {
            lblInfo.setText(count + " scans trouvés");
        }
    }

    @FXML
    void retourDashboard(ActionEvent event) {
        WindowUtils.goBack(event);
    }

    @FXML
    void reinitialiser(ActionEvent event) {
        tfRecherche.clear();
        chargerHistorique();
    }

    /**
     * Navigates to the Report Vulnerabilities view for the selected scan.
     */
    private void viewReportVulnerabilities() {
        FarmHealthScan selected = tableHistorique.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/UserAndDiag/ReportVulnerabilities.fxml"));
            javafx.scene.Parent root = loader.load();

            ReportVulnerabilitiesController controller = loader.getController();
            controller.initData(selected.getId());

            tableHistorique.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            WindowUtils.showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le rapport: " + e.getMessage());
        }
    }
}
