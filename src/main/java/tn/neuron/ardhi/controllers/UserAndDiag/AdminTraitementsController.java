package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import tn.neuron.ardhi.models.UserAndDiag.Traitement;
import tn.neuron.ardhi.models.UserAndDiag.TypeTraitement;
import tn.neuron.ardhi.services.UserAndDiag.TraitementService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;
import tn.neuron.ardhi.services.UserAndDiag.DiagnosticService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminTraitementsController implements Initializable {

    @FXML
    private TableView<Traitement> tableTraitements;
    @FXML
    private TableColumn<Traitement, Integer> colId;
    @FXML
    private TableColumn<Traitement, Integer> colDiagId;
    @FXML
    private TableColumn<Traitement, String> colSolution;
    @FXML
    private TableColumn<Traitement, String> colType;
    @FXML
    private TableColumn<Traitement, String> colDescription;

    @FXML
    private TextField tfRecherche;
    @FXML
    private ComboBox<String> cbFiltreType;
    @FXML
    private Label lblInfo;

    @FXML
    private TextField tfDiagId;
    @FXML
    private TextField tfSolution;
    @FXML
    private ComboBox<TypeTraitement> cbType;
    @FXML
    private TextArea taDescription;
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

    private TraitementService ts = new TraitementService();
    private DiagnosticService ds = new DiagnosticService();
    private List<Traitement> allTraitements = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colDiagId.setCellValueFactory(new PropertyValueFactory<>("diagnosticId"));
            colSolution.setCellValueFactory(new PropertyValueFactory<>("solutionNom"));
            colType.setCellValueFactory(new PropertyValueFactory<>("typeTraitement"));
            colDescription.setCellValueFactory(new PropertyValueFactory<>("descriptionDetaillee"));

            chargerTraitements();
            updateButtonState(false);

            // Populate ComboBox with TypeTraitement enum values
            cbType.setItems(FXCollections.observableArrayList(TypeTraitement.values()));

            // Setup filter ComboBox (Type filter)
            cbFiltreType.getItems().add("Tous");
            for (TypeTraitement t : TypeTraitement.values()) {
                cbFiltreType.getItems().add(t.name());
            }
            cbFiltreType.setValue("Tous");

            // Real-time search listener
            tfRecherche.textProperty().addListener((observable, oldValue, newValue) -> {
                appliquerFiltres();
            });

            // Real-time type filter listener
            cbFiltreType.valueProperty().addListener((observable, oldValue, newValue) -> {
                appliquerFiltres();
            });

            tableTraitements.getSelectionModel().selectedItemProperty()
                    .addListener((obs, oldSelection, newSelection) -> {

                        if (newSelection != null) {
                            lblTitreFormulaire.setText("Modification du traitement #" + newSelection.getId());
                            tfDiagId.setText(String.valueOf(newSelection.getDiagnosticId()));
                            tfSolution.setText(newSelection.getSolutionNom());
                            cbType.setValue(newSelection.getTypeTraitement());
                            taDescription.setText(newSelection.getDescriptionDetaillee());

                            updateButtonState(true);
                        } else {
                            lblTitreFormulaire.setText("Ajouter un nouveau traitement");
                            viderChamps();
                            updateButtonState(false);
                        }
                    });

            tableTraitements.getSelectionModel().clearSelection();

            // Setup table deselection
            WindowUtils.setupTableDeselection(tableTraitements, mainContainer);

        } catch (Exception e) {
            WindowUtils.showAlert("Erreur Fatal", "Impossible d'initialiser la vue traitements : " + e.getMessage());
        }
    }

    private void chargerTraitements() {
        try {
            allTraitements = ts.recuperer();
            tableTraitements.setItems(FXCollections.observableArrayList(allTraitements));
            mettreAJourInfo(allTraitements.size());
        } catch (SQLException e) {
            WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger les traitements : " + e.getMessage());
        }
    }

    private void appliquerFiltres() {
        String keyword = tfRecherche.getText();
        String selectedType = cbFiltreType.getValue();

        try {
            // 1. Get candidates from DB search or all
            List<Traitement> candidats;
            if (keyword != null && !keyword.trim().isEmpty()) {
                candidats = ts.rechercher(keyword.trim());
            } else {
                candidats = ts.recuperer();
            }

            // 2. Apply local type filter
            List<Traitement> filtered = new ArrayList<>();
            for (Traitement t : candidats) {
                // Filter by type
                if (selectedType != null && !selectedType.equals("Tous")) {
                    try {
                        TypeTraitement typeFiltre = TypeTraitement.valueOf(selectedType);
                        if (t.getTypeTraitement() != typeFiltre) {
                            continue;
                        }
                    } catch (IllegalArgumentException e) {
                        // Ignore invalid type
                    }
                }
                filtered.add(t);
            }

            allTraitements = filtered;
            tableTraitements.setItems(FXCollections.observableArrayList(filtered));
            mettreAJourInfo(filtered.size());

        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", "Erreur lors du filtrage : " + e.getMessage());
        }
    }

    private void mettreAJourInfo(int count) {
        WindowUtils.updateInfoLabel(lblInfo, count, "traitement");
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
    void ajouterTraitement(ActionEvent event) {
        try {
            int diagId = Integer.parseInt(tfDiagId.getText().trim());

            if (diagId <= 0) {
                WindowUtils.showAlert("Erreur", "L'ID diagnostic doit être valide.");
                return;
            }
            if (!ds.existe(diagId)) {
                WindowUtils.showAlert("Erreur", "Le diagnostic avec l'ID " + diagId + " n'existe pas.");
                return;
            }

            // Get type from ComboBox
            TypeTraitement typeEnum = cbType.getValue();
            if (typeEnum == null) {
                WindowUtils.showAlert("Erreur", "Veuillez sélectionner un type de traitement.");
                return;
            }

            Traitement t = new Traitement(diagId, tfSolution.getText(), taDescription.getText(), typeEnum);
            ts.ajouter(t);

            WindowUtils.showAlert("Succès", "Traitement ajouté !");
            chargerTraitements();
            viderChamps();
            tableTraitements.getSelectionModel().clearSelection();

        } catch (NumberFormatException | SQLException e) {
            WindowUtils.showAlert("Erreur", "Données invalides : " + e.getMessage());
        }
    }

    @FXML
    void modifierTraitement(ActionEvent event) {
        Traitement selected = tableTraitements.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        try {
            int diagId = Integer.parseInt(tfDiagId.getText().trim());

            // Verify existence before update
            if (!ds.existe(diagId)) {
                WindowUtils.showAlert("Erreur",
                        "Le diagnostic avec l'ID " + diagId + " n'existe pas. Modification annulée.");
                return;
            }

            // Get type from ComboBox
            TypeTraitement typeEnum = cbType.getValue();
            if (typeEnum == null) {
                WindowUtils.showAlert("Erreur", "Veuillez sélectionner un type de traitement.");
                return;
            }

            selected.setDiagnosticId(diagId);
            selected.setSolutionNom(tfSolution.getText());
            selected.setDescriptionDetaillee(taDescription.getText());
            selected.setTypeTraitement(typeEnum);

            ts.modifier(selected);

            WindowUtils.showAlert("Succès", "Traitement modifié !");
            chargerTraitements();
            tableTraitements.getSelectionModel().clearSelection();

        } catch (NumberFormatException | SQLException e) {
            WindowUtils.showAlert("Erreur", "Erreur de saisie : " + e.getMessage());
        }
    }

    @FXML
    void supprimerTraitement(ActionEvent event) {
        Traitement selected = tableTraitements.getSelectionModel().getSelectedItem();
        if (selected == null) {
            WindowUtils.showAlert("Attention", "Sélectionnez un traitement à supprimer.");
            return;
        }

        if (WindowUtils.showConfirmation("Supprimer ce traitement ?", "Cette action est irréversible.")) {
            try {
                ts.supprimer(selected.getId());
                chargerTraitements();
                tableTraitements.getSelectionModel().clearSelection();
                WindowUtils.showAlert("Succès", "Traitement supprimé !");
            } catch (SQLException e) {
                WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    @FXML
    void rafraichir(ActionEvent event) {
        tfRecherche.clear();
        cbFiltreType.setValue("Tous");
        chargerTraitements();
        tableTraitements.getSelectionModel().clearSelection();
    }

    @FXML
    void viderChamps(ActionEvent event) {
        viderChamps();
        tableTraitements.getSelectionModel().clearSelection();
    }

    private void viderChamps() {
        tfDiagId.clear();
        tfSolution.clear();
        cbType.setValue(null);
        taDescription.clear();
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Admin Dashboard");
    }
}