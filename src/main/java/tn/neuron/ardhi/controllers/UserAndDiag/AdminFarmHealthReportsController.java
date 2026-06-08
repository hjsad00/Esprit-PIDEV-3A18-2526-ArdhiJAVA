package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import tn.neuron.ardhi.models.UserAndDiag.FarmHealthReport;
import tn.neuron.ardhi.services.UserAndDiag.FarmHealthReportService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminFarmHealthReportsController implements Initializable {

    @FXML
    private TableView<FarmHealthReport> tableReports;
    @FXML
    private TableColumn<FarmHealthReport, Integer> colId;
    @FXML
    private TableColumn<FarmHealthReport, Integer> colScanId;
    @FXML
    private TableColumn<FarmHealthReport, String> colCropType;
    @FXML
    private TableColumn<FarmHealthReport, Integer> colHealthScore;
    @FXML
    private TableColumn<FarmHealthReport, Integer> colBioScore;
    @FXML
    private TableColumn<FarmHealthReport, String> colGeneratedAt;

    @FXML
    private TextField tfRecherche;
    @FXML
    private Label lblInfo;
    @FXML
    private Label lblTitreFormulaire;
    @FXML
    private VBox mainContainer;

    @FXML
    private TextField tfScanId;
    @FXML
    private TextField tfHealthScore;
    @FXML
    private TextField tfBioScore;
    @FXML
    private TextArea taLlavaAnalysis;

    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;

    private final FarmHealthReportService reportService = new FarmHealthReportService();
    private List<FarmHealthReport> allReports = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colScanId.setCellValueFactory(new PropertyValueFactory<>("scanId"));
            colCropType.setCellValueFactory(cellData -> new SimpleStringProperty(
                    cellData.getValue().getCropType() != null ? cellData.getValue().getCropType() : "-"));
            colHealthScore.setCellValueFactory(new PropertyValueFactory<>("healthScore"));
            colBioScore.setCellValueFactory(new PropertyValueFactory<>("biodiversityScore"));
            colGeneratedAt.setCellValueFactory(cellData -> new SimpleStringProperty(
                    cellData.getValue().getGeneratedAt() != null ? cellData.getValue().getGeneratedAt().toString()
                            : "-"));

            chargerReports();
            updateButtonState(false);

            tfRecherche.textProperty().addListener((obs, o, n) -> rechercherReports(n));

            tableReports.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
                if (sel != null) {
                    lblTitreFormulaire.setText("Rapport #" + sel.getId());
                    tfScanId.setText(String.valueOf(sel.getScanId()));
                    tfHealthScore.setText(String.valueOf(sel.getHealthScore()));
                    tfBioScore.setText(String.valueOf(sel.getBiodiversityScore()));
                    taLlavaAnalysis.setText(sel.getLlavaAnalysis());
                    updateButtonState(true);
                } else {
                    lblTitreFormulaire.setText("Ajouter un rapport");
                    viderChampsInternal();
                    updateButtonState(false);
                }
            });

            tableReports.getSelectionModel().clearSelection();
            WindowUtils.setupTableDeselection(tableReports, mainContainer);
        } catch (Exception e) {
            WindowUtils.showAlert("Erreur", "Impossible d'initialiser la vue : " + e.getMessage());
        }
    }

    private void chargerReports() {
        allReports = reportService.getAll();
        tableReports.setItems(FXCollections.observableArrayList(allReports));
        WindowUtils.updateInfoLabel(lblInfo, allReports.size(), "rapport");
    }

    private void rechercherReports(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            allReports = reportService.getAll();
        } else {
            allReports = reportService.search(keyword.trim());
        }
        tableReports.setItems(FXCollections.observableArrayList(allReports));
        WindowUtils.updateInfoLabel(lblInfo, allReports.size(), "rapport");
    }

    private void updateButtonState(boolean isEditMode) {
        if (btnAjouter != null) {
            btnAjouter.setVisible(!isEditMode);
            btnAjouter.setManaged(!isEditMode);
        }
        if (btnModifier != null) {
            btnModifier.setVisible(isEditMode);
            btnModifier.setManaged(isEditMode);
        }
        if (btnSupprimer != null) {
            btnSupprimer.setVisible(isEditMode);
            btnSupprimer.setManaged(isEditMode);
        }
    }

    @FXML
    void ajouterReport(ActionEvent event) {
        if (!validerChamps())
            return;
        FarmHealthReport report = new FarmHealthReport();
        remplirReport(report);
        int id = reportService.save(report);
        if (id > 0) {
            WindowUtils.showAlert("Succès", "Rapport ajouté avec l'ID " + id);
            chargerReports();
            viderChampsInternal();
            tableReports.getSelectionModel().clearSelection();
        } else {
            WindowUtils.showAlert("Erreur", "Échec de l'ajout.");
        }
    }

    @FXML
    void modifierReport(ActionEvent event) {
        FarmHealthReport selected = tableReports.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;
        if (!validerChamps())
            return;
        remplirReport(selected);
        reportService.update(selected);
        WindowUtils.showAlert("Succès", "Rapport modifié !");
        chargerReports();
        tableReports.getSelectionModel().clearSelection();
    }

    @FXML
    void supprimerReport(ActionEvent event) {
        FarmHealthReport selected = tableReports.getSelectionModel().getSelectedItem();
        if (selected == null) {
            WindowUtils.showAlert("Attention", "Sélectionnez un rapport à supprimer.");
            return;
        }
        if (WindowUtils.showConfirmation("Supprimer ce rapport ?", "Cette action est irréversible.")) {
            reportService.delete(selected.getId());
            chargerReports();
            tableReports.getSelectionModel().clearSelection();
            WindowUtils.showAlert("Succès", "Rapport supprimé !");
        }
    }

    private boolean validerChamps() {
        try {
            Integer.parseInt(tfScanId.getText().trim());
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "Scan ID invalide.");
            return false;
        }
        try {
            int h = Integer.parseInt(tfHealthScore.getText().trim());
            if (h < 0 || h > 100) {
                WindowUtils.showAlert("Erreur", "Score santé doit être entre 0 et 100.");
                return false;
            }
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "Score santé invalide.");
            return false;
        }
        try {
            int b = Integer.parseInt(tfBioScore.getText().trim());
            if (b < 0 || b > 100) {
                WindowUtils.showAlert("Erreur", "Score biodiversité doit être entre 0 et 100.");
                return false;
            }
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "Score biodiversité invalide.");
            return false;
        }
        return true;
    }

    private void remplirReport(FarmHealthReport report) {
        report.setScanId(Integer.parseInt(tfScanId.getText().trim()));
        report.setHealthScore(Integer.parseInt(tfHealthScore.getText().trim()));
        report.setBiodiversityScore(Integer.parseInt(tfBioScore.getText().trim()));
        report.setLlavaAnalysis(taLlavaAnalysis.getText().trim());
    }

    @FXML
    void rafraichir(ActionEvent event) {
        tfRecherche.clear();
        chargerReports();
        tableReports.getSelectionModel().clearSelection();
    }

    @FXML
    void viderChamps(ActionEvent event) {
        viderChampsInternal();
        tableReports.getSelectionModel().clearSelection();
    }

    private void viderChampsInternal() {
        tfScanId.clear();
        tfHealthScore.clear();
        tfBioScore.clear();
        taLlavaAnalysis.clear();
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Admin Dashboard");
    }
}
