package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import tn.neuron.ardhi.models.UserAndDiag.Severity;
import tn.neuron.ardhi.models.UserAndDiag.Vulnerability;
import tn.neuron.ardhi.models.UserAndDiag.VulnerabilityType;
import tn.neuron.ardhi.services.UserAndDiag.VulnerabilityService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminVulnerabilitiesController implements Initializable {

    @FXML
    private TableView<Vulnerability> tableVulns;
    @FXML
    private TableColumn<Vulnerability, Integer> colId;
    @FXML
    private TableColumn<Vulnerability, Integer> colReportId;
    @FXML
    private TableColumn<Vulnerability, String> colType;
    @FXML
    private TableColumn<Vulnerability, String> colThreat;
    @FXML
    private TableColumn<Vulnerability, String> colSeverity;
    @FXML
    private TableColumn<Vulnerability, Float> colRisk;

    @FXML
    private TextField tfRecherche;
    @FXML
    private Label lblInfo;
    @FXML
    private TextField tfReportId;
    @FXML
    private TextField tfThreat;
    @FXML
    private TextArea taDescription;
    @FXML
    private ComboBox<VulnerabilityType> cbType;
    @FXML
    private ComboBox<Severity> cbSeverity;
    @FXML
    private TextField tfRiskScore;
    @FXML
    private Label lblTitreFormulaire;
    @FXML
    private VBox mainContainer;

    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;

    private final VulnerabilityService vulnService = new VulnerabilityService();
    private List<Vulnerability> allVulns = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colReportId.setCellValueFactory(new PropertyValueFactory<>("reportId"));
            colType.setCellValueFactory(cellData -> new SimpleStringProperty(
                    cellData.getValue().getType() != null ? cellData.getValue().getType().name() : "-"));
            colThreat.setCellValueFactory(new PropertyValueFactory<>("threat"));
            colSeverity.setCellValueFactory(cellData -> new SimpleStringProperty(
                    cellData.getValue().getSeverity() != null ? cellData.getValue().getSeverity().name() : "-"));
            colRisk.setCellValueFactory(new PropertyValueFactory<>("riskScore"));

            cbType.setItems(FXCollections.observableArrayList(VulnerabilityType.values()));
            cbSeverity.setItems(FXCollections.observableArrayList(Severity.values()));

            chargerVulns();
            updateButtonState(false);

            tfRecherche.textProperty().addListener((obs, o, n) -> rechercherVulns(n));

            tableVulns.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
                if (sel != null) {
                    lblTitreFormulaire.setText("Vulnérabilité #" + sel.getId());
                    tfReportId.setText(String.valueOf(sel.getReportId()));
                    tfThreat.setText(sel.getThreat());
                    taDescription.setText(sel.getDescription());
                    cbType.setValue(sel.getType());
                    cbSeverity.setValue(sel.getSeverity());
                    tfRiskScore.setText(String.valueOf(sel.getRiskScore()));
                    updateButtonState(true);
                } else {
                    lblTitreFormulaire.setText("Ajouter une vulnérabilité");
                    viderChamps();
                    updateButtonState(false);
                }
            });

            tableVulns.getSelectionModel().clearSelection();
            WindowUtils.setupTableDeselection(tableVulns, mainContainer);
        } catch (Exception e) {
            WindowUtils.showAlert("Erreur", "Impossible d'initialiser la vue : " + e.getMessage());
        }
    }

    private void chargerVulns() {
        allVulns = vulnService.getAll();
        tableVulns.setItems(FXCollections.observableArrayList(allVulns));
        WindowUtils.updateInfoLabel(lblInfo, allVulns.size(), "vulnérabilité");
    }

    private void rechercherVulns(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            allVulns = vulnService.getAll();
        } else {
            allVulns = vulnService.search(keyword.trim());
        }
        tableVulns.setItems(FXCollections.observableArrayList(allVulns));
        WindowUtils.updateInfoLabel(lblInfo, allVulns.size(), "vulnérabilité");
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
    void ajouterVuln(ActionEvent event) {
        if (!validerChamps())
            return;

        Vulnerability v = new Vulnerability();
        remplirVulnerability(v);

        int id = vulnService.save(v);
        if (id > 0) {
            WindowUtils.showAlert("Succès", "Vulnérabilité ajoutée avec l'ID " + id);
            chargerVulns();
            viderChamps();
            tableVulns.getSelectionModel().clearSelection();
        } else {
            WindowUtils.showAlert("Erreur", "Échec de l'ajout.");
        }
    }

    @FXML
    void modifierVuln(ActionEvent event) {
        Vulnerability selected = tableVulns.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;
        if (!validerChamps())
            return;

        remplirVulnerability(selected);
        vulnService.update(selected);

        WindowUtils.showAlert("Succès", "Vulnérabilité modifiée !");
        chargerVulns();
        tableVulns.getSelectionModel().clearSelection();
    }

    @FXML
    void supprimerVuln(ActionEvent event) {
        Vulnerability selected = tableVulns.getSelectionModel().getSelectedItem();
        if (selected == null) {
            WindowUtils.showAlert("Attention", "Sélectionnez une vulnérabilité à supprimer.");
            return;
        }
        if (WindowUtils.showConfirmation("Supprimer cette vulnérabilité ?",
                "Cette action est irréversible.")) {
            vulnService.delete(selected.getId());
            chargerVulns();
            tableVulns.getSelectionModel().clearSelection();
            WindowUtils.showAlert("Succès", "Vulnérabilité supprimée !");
        }
    }

    private boolean validerChamps() {
        try {
            Integer.parseInt(tfReportId.getText().trim());
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "ID Rapport invalide.");
            return false;
        }
        if (tfThreat.getText().trim().isEmpty()) {
            WindowUtils.showAlert("Erreur", "La menace est obligatoire.");
            return false;
        }
        try {
            float risk = Float.parseFloat(tfRiskScore.getText().trim());
            if (risk < 0 || risk > 1) {
                WindowUtils.showAlert("Erreur", "Le score de risque doit être entre 0.0 et 1.0");
                return false;
            }
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "Score de risque invalide.");
            return false;
        }
        return true;
    }

    private void remplirVulnerability(Vulnerability v) {
        v.setReportId(Integer.parseInt(tfReportId.getText().trim()));
        v.setThreat(tfThreat.getText().trim());
        v.setDescription(taDescription.getText().trim());
        v.setType(cbType.getValue() != null ? cbType.getValue() : VulnerabilityType.DISEASE_RISK);
        v.setSeverity(cbSeverity.getValue() != null ? cbSeverity.getValue() : Severity.LOW);
        v.setRiskScore(Float.parseFloat(tfRiskScore.getText().trim()));
    }

    @FXML
    void rafraichir(ActionEvent event) {
        tfRecherche.clear();
        chargerVulns();
        tableVulns.getSelectionModel().clearSelection();
    }

    @FXML
    void viderChamps(ActionEvent event) {
        viderChamps();
        tableVulns.getSelectionModel().clearSelection();
    }

    private void viderChamps() {
        tfReportId.clear();
        tfThreat.clear();
        taDescription.clear();
        cbType.setValue(null);
        cbSeverity.setValue(null);
        tfRiskScore.clear();
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Admin Dashboard");
    }
}
