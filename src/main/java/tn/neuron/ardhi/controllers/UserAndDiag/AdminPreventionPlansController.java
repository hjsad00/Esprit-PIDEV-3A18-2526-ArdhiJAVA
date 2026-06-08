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
import tn.neuron.ardhi.models.UserAndDiag.PreventionPlan;
import tn.neuron.ardhi.services.UserAndDiag.PreventionPlanService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminPreventionPlansController implements Initializable {

    @FXML
    private TableView<PreventionPlan> tablePlans;
    @FXML
    private TableColumn<PreventionPlan, Integer> colId;
    @FXML
    private TableColumn<PreventionPlan, Integer> colReportId;
    @FXML
    private TableColumn<PreventionPlan, String> colTitle;
    @FXML
    private TableColumn<PreventionPlan, String> colImpact;
    @FXML
    private TableColumn<PreventionPlan, String> colStatus;
    @FXML
    private TableColumn<PreventionPlan, String> colStartDate;

    @FXML
    private TextField tfRecherche;
    @FXML
    private Label lblInfo;
    @FXML
    private TextField tfReportId;
    @FXML
    private TextField tfTitle;
    @FXML
    private ComboBox<PlanStatus> cbStatus;
    @FXML
    private ComboBox<String> cbImpact;
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

    private final PreventionPlanService planService = new PreventionPlanService();
    private List<PreventionPlan> allPlans = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colReportId.setCellValueFactory(new PropertyValueFactory<>("reportId"));
            colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
            colImpact.setCellValueFactory(new PropertyValueFactory<>("impactLevel"));
            colStatus.setCellValueFactory(cellData -> {
                PlanStatus s = cellData.getValue().getStatus();
                return new SimpleStringProperty(s != null ? s.name() : "-");
            });
            colStartDate.setCellValueFactory(cellData -> {
                if (cellData.getValue().getStartDate() != null) {
                    SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy");
                    return new SimpleStringProperty(fmt.format(cellData.getValue().getStartDate()));
                }
                return new SimpleStringProperty("");
            });

            cbStatus.setItems(FXCollections.observableArrayList(PlanStatus.values()));
            cbImpact.setItems(FXCollections.observableArrayList("HIGH", "MEDIUM", "LOW"));
            chargerPlans();
            updateButtonState(false);

            tfRecherche.textProperty().addListener((obs, o, n) -> rechercherPlans(n));

            tablePlans.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
                if (sel != null) {
                    lblTitreFormulaire.setText("Plan #" + sel.getId());
                    tfReportId.setText(String.valueOf(sel.getReportId()));
                    tfTitle.setText(sel.getTitle());
                    cbStatus.setValue(sel.getStatus());
                    cbImpact.setValue(sel.getImpactLevel());
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
        int reportId;
        try {
            reportId = Integer.parseInt(tfReportId.getText().trim());
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "ID Rapport invalide.");
            return;
        }
        String title = tfTitle.getText();
        if (title == null || title.trim().isEmpty()) {
            WindowUtils.showAlert("Erreur", "Le titre ne peut pas être vide.");
            return;
        }
        PlanStatus status = cbStatus.getValue();
        if (status == null) {
            WindowUtils.showAlert("Erreur", "Veuillez sélectionner un statut.");
            return;
        }

        PreventionPlan plan = new PreventionPlan();
        plan.setReportId(reportId);
        plan.setTitle(title.trim());
        plan.setStatus(status);
        plan.setImpactLevel(cbImpact.getValue() != null ? cbImpact.getValue() : "MEDIUM");
        plan.setSteps("");
        int id = planService.save(plan);
        if (id > 0) {
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
        PreventionPlan selected = tablePlans.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        int reportId;
        try {
            reportId = Integer.parseInt(tfReportId.getText().trim());
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "ID Rapport invalide.");
            return;
        }
        String title = tfTitle.getText();
        if (title == null || title.trim().isEmpty()) {
            WindowUtils.showAlert("Erreur", "Le titre ne peut pas être vide.");
            return;
        }
        PlanStatus status = cbStatus.getValue();
        if (status == null) {
            WindowUtils.showAlert("Erreur", "Veuillez sélectionner un statut.");
            return;
        }

        selected.setReportId(reportId);
        selected.setTitle(title.trim());
        selected.setStatus(status);
        selected.setImpactLevel(cbImpact.getValue() != null ? cbImpact.getValue() : "MEDIUM");
        planService.updatePlan(selected);
        WindowUtils.showAlert("Succès", "Plan modifié !");
        chargerPlans();
        tablePlans.getSelectionModel().clearSelection();
    }

    @FXML
    void supprimerPlan(ActionEvent event) {
        PreventionPlan selected = tablePlans.getSelectionModel().getSelectedItem();
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
        tfReportId.clear();
        tfTitle.clear();
        cbStatus.setValue(null);
        cbImpact.setValue(null);
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Admin Dashboard");
    }
}
