package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import tn.neuron.ardhi.models.UserAndDiag.FarmHealthScan;
import tn.neuron.ardhi.models.UserAndDiag.ScanStatus;
import tn.neuron.ardhi.services.UserAndDiag.FarmHealthScanService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminFarmHealthScansController implements Initializable {

    @FXML
    private TableView<FarmHealthScan> tableScans;
    @FXML
    private TableColumn<FarmHealthScan, Integer> colId;
    @FXML
    private TableColumn<FarmHealthScan, Integer> colUserId;
    @FXML
    private TableColumn<FarmHealthScan, String> colCropType;
    @FXML
    private TableColumn<FarmHealthScan, String> colGrowthStage;
    @FXML
    private TableColumn<FarmHealthScan, String> colPlantingDate;
    @FXML
    private TableColumn<FarmHealthScan, String> colScanDate;
    @FXML
    private TableColumn<FarmHealthScan, String> colStatus;

    @FXML
    private TextField tfRecherche;
    @FXML
    private Label lblInfo;
    @FXML
    private Label lblTitreFormulaire;
    @FXML
    private VBox mainContainer;

    @FXML
    private TextField tfUserId;
    @FXML
    private TextField tfCropType;
    @FXML
    private TextField tfGrowthStage;
    @FXML
    private DatePicker dpPlantingDate;
    @FXML
    private TextArea taConcerns;
    @FXML
    private TextField tfLatitude;
    @FXML
    private TextField tfLongitude;
    @FXML
    private ComboBox<ScanStatus> cbStatus;

    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;

    private final FarmHealthScanService scanService = new FarmHealthScanService();
    private List<FarmHealthScan> allScans = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
            colCropType.setCellValueFactory(new PropertyValueFactory<>("cropType"));
            colGrowthStage.setCellValueFactory(new PropertyValueFactory<>("growthStage"));
            colPlantingDate.setCellValueFactory(cellData -> new SimpleStringProperty(
                    cellData.getValue().getPlantingDate() != null ? cellData.getValue().getPlantingDate().toString()
                            : "-"));
            colScanDate.setCellValueFactory(cellData -> new SimpleStringProperty(
                    cellData.getValue().getScanDate() != null ? cellData.getValue().getScanDate().toString() : "-"));
            colStatus.setCellValueFactory(cellData -> new SimpleStringProperty(
                    cellData.getValue().getStatus() != null ? cellData.getValue().getStatus().name() : "-"));

            cbStatus.setItems(FXCollections.observableArrayList(ScanStatus.values()));

            chargerScans();
            updateButtonState(false);

            tfRecherche.textProperty().addListener((obs, o, n) -> rechercherScans(n));

            tableScans.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
                if (sel != null) {
                    lblTitreFormulaire.setText("Scan #" + sel.getId());
                    tfUserId.setText(String.valueOf(sel.getUserId()));
                    tfCropType.setText(sel.getCropType());
                    tfGrowthStage.setText(sel.getGrowthStage());
                    if (sel.getPlantingDate() != null) {
                        dpPlantingDate.setValue(sel.getPlantingDate().toLocalDate());
                    } else {
                        dpPlantingDate.setValue(null);
                    }
                    taConcerns.setText(sel.getConcerns());
                    tfLatitude.setText(sel.getLatitude() != null ? String.valueOf(sel.getLatitude()) : "");
                    tfLongitude.setText(sel.getLongitude() != null ? String.valueOf(sel.getLongitude()) : "");
                    cbStatus.setValue(sel.getStatus());
                    updateButtonState(true);
                } else {
                    lblTitreFormulaire.setText("Ajouter un scan");
                    viderChampsInternal();
                    updateButtonState(false);
                }
            });

            tableScans.getSelectionModel().clearSelection();
            WindowUtils.setupTableDeselection(tableScans, mainContainer);
        } catch (Exception e) {
            WindowUtils.showAlert("Erreur", "Impossible d'initialiser la vue : " + e.getMessage());
        }
    }

    private void chargerScans() {
        allScans = scanService.getAll();
        tableScans.setItems(FXCollections.observableArrayList(allScans));
        WindowUtils.updateInfoLabel(lblInfo, allScans.size(), "scan");
    }

    private void rechercherScans(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            allScans = scanService.getAll();
        } else {
            allScans = scanService.search(keyword.trim());
        }
        tableScans.setItems(FXCollections.observableArrayList(allScans));
        WindowUtils.updateInfoLabel(lblInfo, allScans.size(), "scan");
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
    void ajouterScan(ActionEvent event) {
        if (!validerChamps())
            return;
        FarmHealthScan scan = new FarmHealthScan();
        remplirScan(scan);
        int id = scanService.save(scan);
        if (id > 0) {
            WindowUtils.showAlert("Succès", "Scan ajouté avec l'ID " + id);
            chargerScans();
            viderChampsInternal();
            tableScans.getSelectionModel().clearSelection();
        } else {
            WindowUtils.showAlert("Erreur", "Échec de l'ajout.");
        }
    }

    @FXML
    void modifierScan(ActionEvent event) {
        FarmHealthScan selected = tableScans.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;
        if (!validerChamps())
            return;
        remplirScan(selected);
        scanService.update(selected);
        WindowUtils.showAlert("Succès", "Scan modifié !");
        chargerScans();
        tableScans.getSelectionModel().clearSelection();
    }

    @FXML
    void supprimerScan(ActionEvent event) {
        FarmHealthScan selected = tableScans.getSelectionModel().getSelectedItem();
        if (selected == null) {
            WindowUtils.showAlert("Attention", "Sélectionnez un scan à supprimer.");
            return;
        }
        if (WindowUtils.showConfirmation("Supprimer ce scan ?",
                "Cela supprimera aussi les rapports et vulnérabilités associés.")) {
            scanService.delete(selected.getId());
            chargerScans();
            tableScans.getSelectionModel().clearSelection();
            WindowUtils.showAlert("Succès", "Scan supprimé !");
        }
    }

    private boolean validerChamps() {
        try {
            Integer.parseInt(tfUserId.getText().trim());
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "ID Utilisateur invalide.");
            return false;
        }
        if (tfCropType.getText().trim().isEmpty()) {
            WindowUtils.showAlert("Erreur", "Le type de culture est obligatoire.");
            return false;
        }
        return true;
    }

    private void remplirScan(FarmHealthScan scan) {
        scan.setUserId(Integer.parseInt(tfUserId.getText().trim()));
        scan.setCropType(tfCropType.getText().trim());
        scan.setGrowthStage(tfGrowthStage.getText().trim());
        if (dpPlantingDate.getValue() != null) {
            scan.setPlantingDate(Date.valueOf(dpPlantingDate.getValue()));
        }
        scan.setConcerns(taConcerns.getText().trim());
        try {
            if (!tfLatitude.getText().trim().isEmpty())
                scan.setLatitude(Double.parseDouble(tfLatitude.getText().trim()));
            else
                scan.setLatitude(null);
        } catch (NumberFormatException e) {
            scan.setLatitude(null);
        }
        try {
            if (!tfLongitude.getText().trim().isEmpty())
                scan.setLongitude(Double.parseDouble(tfLongitude.getText().trim()));
            else
                scan.setLongitude(null);
        } catch (NumberFormatException e) {
            scan.setLongitude(null);
        }
        scan.setStatus(cbStatus.getValue() != null ? cbStatus.getValue() : ScanStatus.PENDING);
    }

    @FXML
    void rafraichir(ActionEvent event) {
        tfRecherche.clear();
        chargerScans();
        tableScans.getSelectionModel().clearSelection();
    }

    @FXML
    void viderChamps(ActionEvent event) {
        viderChampsInternal();
        tableScans.getSelectionModel().clearSelection();
    }

    private void viderChampsInternal() {
        tfUserId.clear();
        tfCropType.clear();
        tfGrowthStage.clear();
        dpPlantingDate.setValue(null);
        taConcerns.clear();
        tfLatitude.clear();
        tfLongitude.clear();
        cbStatus.setValue(null);
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Admin Dashboard");
    }
}
