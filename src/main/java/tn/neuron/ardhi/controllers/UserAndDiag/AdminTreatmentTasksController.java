package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import tn.neuron.ardhi.models.UserAndDiag.TaskStatus;
import tn.neuron.ardhi.models.UserAndDiag.TreatmentTask;
import tn.neuron.ardhi.services.UserAndDiag.TreatmentTaskService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminTreatmentTasksController implements Initializable {

    @FXML
    private TableView<TreatmentTask> tableTasks;
    @FXML
    private TableColumn<TreatmentTask, Integer> colId;
    @FXML
    private TableColumn<TreatmentTask, Integer> colPlanId;
    @FXML
    private TableColumn<TreatmentTask, Integer> colDay;
    @FXML
    private TableColumn<TreatmentTask, String> colDescription;
    @FXML
    private TableColumn<TreatmentTask, String> colStatus;

    @FXML
    private TextField tfRecherche;
    @FXML
    private Label lblInfo;
    @FXML
    private TextField tfPlanId;
    @FXML
    private TextField tfDayOffset;
    @FXML
    private TextArea taDescription;
    @FXML
    private ComboBox<TaskStatus> cbStatus;
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

    private final TreatmentTaskService taskService = new TreatmentTaskService();
    private List<TreatmentTask> allTasks = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colPlanId.setCellValueFactory(new PropertyValueFactory<>("treatmentPlanId"));
            colDay.setCellValueFactory(new PropertyValueFactory<>("dayOffset"));
            colDescription.setCellValueFactory(new PropertyValueFactory<>("taskDescription"));
            colStatus.setCellValueFactory(cellData -> {
                TaskStatus s = cellData.getValue().getStatus();
                return new SimpleStringProperty(s != null ? s.name() : "-");
            });

            cbStatus.setItems(FXCollections.observableArrayList(TaskStatus.values()));
            chargerTasks();
            updateButtonState(false);

            tfRecherche.textProperty().addListener((obs, o, n) -> rechercherTasks(n));

            tableTasks.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
                if (sel != null) {
                    lblTitreFormulaire.setText("Tâche #" + sel.getId());
                    tfPlanId.setText(String.valueOf(sel.getTreatmentPlanId()));
                    tfDayOffset.setText(String.valueOf(sel.getDayOffset()));
                    taDescription.setText(sel.getTaskDescription());
                    cbStatus.setValue(sel.getStatus());
                    updateButtonState(true);
                } else {
                    lblTitreFormulaire.setText("Ajouter une tâche");
                    viderChamps();
                    updateButtonState(false);
                }
            });

            tableTasks.getSelectionModel().clearSelection();
            WindowUtils.setupTableDeselection(tableTasks, mainContainer);
        } catch (Exception e) {
            WindowUtils.showAlert("Erreur", "Impossible d'initialiser la vue : " + e.getMessage());
        }
    }

    private void chargerTasks() {
        allTasks = taskService.getAllTasks();
        tableTasks.setItems(FXCollections.observableArrayList(allTasks));
        WindowUtils.updateInfoLabel(lblInfo, allTasks.size(), "tâche");
    }

    private void rechercherTasks(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            allTasks = taskService.getAllTasks();
        } else {
            allTasks = taskService.searchTasks(keyword.trim());
        }
        tableTasks.setItems(FXCollections.observableArrayList(allTasks));
        WindowUtils.updateInfoLabel(lblInfo, allTasks.size(), "tâche");
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
    void ajouterTask(ActionEvent event) {
        int planId, dayOffset;
        try {
            planId = Integer.parseInt(tfPlanId.getText().trim());
            dayOffset = Integer.parseInt(tfDayOffset.getText().trim());
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "Plan ID et Jour doivent être des nombres.");
            return;
        }
        String desc = taDescription.getText();
        if (desc == null || desc.trim().isEmpty()) {
            WindowUtils.showAlert("Erreur", "La description ne peut pas être vide.");
            return;
        }
        TaskStatus status = cbStatus.getValue();
        if (status == null) {
            WindowUtils.showAlert("Erreur", "Veuillez sélectionner un statut.");
            return;
        }

        TreatmentTask task = new TreatmentTask(planId, dayOffset, desc.trim(), status);
        taskService.saveTask(task);
        WindowUtils.showAlert("Succès", "Tâche ajoutée !");
        chargerTasks();
        viderChamps();
        tableTasks.getSelectionModel().clearSelection();
    }

    @FXML
    void modifierTask(ActionEvent event) {
        TreatmentTask selected = tableTasks.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        int planId, dayOffset;
        try {
            planId = Integer.parseInt(tfPlanId.getText().trim());
            dayOffset = Integer.parseInt(tfDayOffset.getText().trim());
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "Plan ID et Jour doivent être des nombres.");
            return;
        }
        String desc = taDescription.getText();
        if (desc == null || desc.trim().isEmpty()) {
            WindowUtils.showAlert("Erreur", "La description ne peut pas être vide.");
            return;
        }
        TaskStatus status = cbStatus.getValue();
        if (status == null) {
            WindowUtils.showAlert("Erreur", "Veuillez sélectionner un statut.");
            return;
        }

        selected.setTreatmentPlanId(planId);
        selected.setDayOffset(dayOffset);
        selected.setTaskDescription(desc.trim());
        selected.setStatus(status);
        taskService.updateTask(selected);
        WindowUtils.showAlert("Succès", "Tâche modifiée !");
        chargerTasks();
        tableTasks.getSelectionModel().clearSelection();
    }

    @FXML
    void supprimerTask(ActionEvent event) {
        TreatmentTask selected = tableTasks.getSelectionModel().getSelectedItem();
        if (selected == null) {
            WindowUtils.showAlert("Attention", "Sélectionnez une tâche à supprimer.");
            return;
        }
        if (WindowUtils.showConfirmation("Supprimer cette tâche ?",
                "Cette action est irréversible.")) {
            taskService.deleteTask(selected.getId());
            chargerTasks();
            tableTasks.getSelectionModel().clearSelection();
            WindowUtils.showAlert("Succès", "Tâche supprimée !");
        }
    }

    @FXML
    void rafraichir(ActionEvent event) {
        tfRecherche.clear();
        chargerTasks();
        tableTasks.getSelectionModel().clearSelection();
    }

    @FXML
    void viderChamps(ActionEvent event) {
        viderChamps();
        tableTasks.getSelectionModel().clearSelection();
    }

    private void viderChamps() {
        tfPlanId.clear();
        tfDayOffset.clear();
        taDescription.clear();
        cbStatus.setValue(null);
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Admin Dashboard");
    }
}
