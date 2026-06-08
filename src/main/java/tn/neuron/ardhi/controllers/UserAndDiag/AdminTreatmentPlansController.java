package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import tn.neuron.ardhi.models.UserAndDiag.PlanStatus;
import tn.neuron.ardhi.models.UserAndDiag.TreatmentPlan;
import tn.neuron.ardhi.services.UserAndDiag.TreatmentPlanService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminTreatmentPlansController implements Initializable {

    @FXML
    private TableView<TreatmentPlan> tablePlans;
    @FXML
    private TableColumn<TreatmentPlan, Integer> colId;
    @FXML
    private TableColumn<TreatmentPlan, Integer> colDiagId;
    @FXML
    private TableColumn<TreatmentPlan, String> colDisease;
    @FXML
    private TableColumn<TreatmentPlan, String> colStatus;
    @FXML
    private TableColumn<TreatmentPlan, String> colStartDate;

    @FXML
    private TextField tfRecherche;
    @FXML
    private Label lblInfo;
    @FXML
    private TextField tfDiagId;
    @FXML
    private ComboBox<PlanStatus> cbStatus;
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

    private final TreatmentPlanService planService = new TreatmentPlanService();
    private List<TreatmentPlan> allPlans = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colDiagId.setCellValueFactory(new PropertyValueFactory<>("diagnosticId"));
            colDisease.setCellValueFactory(new PropertyValueFactory<>("initialDiseaseName"));
            colStatus.setCellValueFactory(cellData -> {
                PlanStatus s = cellData.getValue().getStatus();
                return new SimpleStringProperty(s != null ? s.name() : "-");
            });
            colStartDate.setCellValueFactory(cellData -> {
                if (cellData.getValue().getStartDate() != null) {
                    SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                    return new SimpleStringProperty(fmt.format(cellData.getValue().getStartDate()));
                }
                return new SimpleStringProperty("");
            });

            cbStatus.setItems(FXCollections.observableArrayList(PlanStatus.values()));
            chargerPlans();
            updateButtonState(false);

            tfRecherche.textProperty().addListener((obs, o, n) -> rechercherPlans(n));

            tablePlans.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
                if (sel != null) {
                    lblTitreFormulaire.setText("Plan #" + sel.getId());
                    tfDiagId.setText(String.valueOf(sel.getDiagnosticId()));
                    cbStatus.setValue(sel.getStatus());
                    updateButtonState(true);
                } else {
                    lblTitreFormulaire.setText("Ajouter un plan");
                    viderChamps();
                    updateButtonState(false);
                }
            });

            tablePlans.getSelectionModel().clearSelection();
            WindowUtils.setupTableDeselection(tablePlans, mainContainer);
        } catch (Exception e) {
            WindowUtils.showAlert("Erreur", "Impossible d'initialiser la vue : " + e.getMessage());
        }
    }

    private void chargerPlans() {
        allPlans = planService.getAllPlans();
        tablePlans.setItems(FXCollections.observableArrayList(allPlans));
        WindowUtils.updateInfoLabel(lblInfo, allPlans.size(), "plan");
    }

    private void rechercherPlans(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            allPlans = planService.getAllPlans();
        } else {
            allPlans = planService.searchPlans(keyword.trim());
        }
        tablePlans.setItems(FXCollections.observableArrayList(allPlans));
        WindowUtils.updateInfoLabel(lblInfo, allPlans.size(), "plan");
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
    void ajouterPlan(ActionEvent event) {
        int diagId;
        try {
            diagId = Integer.parseInt(tfDiagId.getText().trim());
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "ID Diagnostic invalide.");
            return;
        }
        PlanStatus status = cbStatus.getValue();
        if (status == null) {
            WindowUtils.showAlert("Erreur", "Veuillez sélectionner un statut.");
            return;
        }

        TreatmentPlan plan = new TreatmentPlan();
        plan.setDiagnosticId(diagId);
        plan.setStatus(status);
        int id = planService.savePlan(plan);
        if (id != -1) {
            WindowUtils.showAlert("Succès", "Plan ajouté avec l'ID " + id);
            chargerPlans();
            viderChamps();
            tablePlans.getSelectionModel().clearSelection();
        } else {
            WindowUtils.showAlert("Erreur", "Échec de l'ajout du plan.");
        }
    }

    @FXML
    void modifierPlan(ActionEvent event) {
        TreatmentPlan selected = tablePlans.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        int diagId;
        try {
            diagId = Integer.parseInt(tfDiagId.getText().trim());
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "ID Diagnostic invalide.");
            return;
        }
        PlanStatus status = cbStatus.getValue();
        if (status == null) {
            WindowUtils.showAlert("Erreur", "Veuillez sélectionner un statut.");
            return;
        }

        selected.setDiagnosticId(diagId);
        selected.setStatus(status);
        planService.updatePlan(selected);
        WindowUtils.showAlert("Succès", "Plan modifié !");
        chargerPlans();
        tablePlans.getSelectionModel().clearSelection();
    }

    @FXML
    void supprimerPlan(ActionEvent event) {
        TreatmentPlan selected = tablePlans.getSelectionModel().getSelectedItem();
        if (selected == null) {
            WindowUtils.showAlert("Attention", "Sélectionnez un plan à supprimer.");
            return;
        }
        if (WindowUtils.showConfirmation("Supprimer ce plan ?",
                "Toutes les tâches et revues associées seront supprimées.")) {
            planService.deletePlan(selected.getId());
            chargerPlans();
            tablePlans.getSelectionModel().clearSelection();
            WindowUtils.showAlert("Succès", "Plan supprimé !");
        }
    }

    @FXML
    void rafraichir(ActionEvent event) {
        tfRecherche.clear();
        chargerPlans();
        tablePlans.getSelectionModel().clearSelection();
    }

    @FXML
    void viderChamps(ActionEvent event) {
        viderChamps();
        tablePlans.getSelectionModel().clearSelection();
    }

    private void viderChamps() {
        tfDiagId.clear();
        cbStatus.setValue(null);
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Admin Dashboard");
    }
}
