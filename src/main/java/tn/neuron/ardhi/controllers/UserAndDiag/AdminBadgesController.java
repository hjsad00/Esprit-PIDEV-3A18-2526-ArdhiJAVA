package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import tn.neuron.ardhi.models.UserAndDiag.Badge;
import tn.neuron.ardhi.models.UserAndDiag.BadgeType;
import tn.neuron.ardhi.services.UserAndDiag.BadgeService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminBadgesController implements Initializable {

    @FXML
    private TableView<Badge> tableBadges;
    @FXML
    private TableColumn<Badge, String> colIcon;
    @FXML
    private TableColumn<Badge, String> colName;
    @FXML
    private TableColumn<Badge, String> colDescription;
    @FXML
    private TableColumn<Badge, BadgeType> colType;
    @FXML
    private TableColumn<Badge, Integer> colThreshold;

    @FXML
    private TextField tfName;
    @FXML
    private TextArea taDescription;
    @FXML
    private TextField tfIcon;
    @FXML
    private ComboBox<BadgeType> cbType;
    @FXML
    private TextField tfThreshold;

    @FXML
    private VBox mainContainer;
    @FXML
    private Label lblTitreFormulaire;
    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;

    @FXML
    private TextField tfFiltre;
    @FXML
    private ComboBox<String> cbFiltreType;
    @FXML
    private Label lblInfo;

    private final BadgeService badgeService = new BadgeService();
    private final ObservableList<Badge> listeBadges = FXCollections.observableArrayList();
    private final ObservableList<Badge> listeFiltree = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup columns
        colIcon.setCellValueFactory(new PropertyValueFactory<>("icon"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colType.setCellValueFactory(new PropertyValueFactory<>("conditionType"));
        colThreshold.setCellValueFactory(new PropertyValueFactory<>("threshold"));

        // Setup ComboBoxes
        cbType.setItems(FXCollections.observableArrayList(BadgeType.values()));

        // Filter ComboBox with "TOUT" option
        ObservableList<String> typesFilter = FXCollections.observableArrayList();
        typesFilter.add("TOUT");
        for (BadgeType type : BadgeType.values()) {
            typesFilter.add(type.name());
        }
        cbFiltreType.setItems(typesFilter);
        cbFiltreType.setValue("TOUT"); // Default selection

        // Load data
        chargerDonnees();

        // Selection Listener
        tableBadges.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                lblTitreFormulaire.setText("Modifier le badge");
                remplirFormulaire(newVal);
                btnAjouter.setVisible(false);
                btnModifier.setVisible(true);
                btnSupprimer.setVisible(true);
            } else {
                lblTitreFormulaire.setText("Ajouter un nouveau badge");
                viderFormulaire();
                btnAjouter.setVisible(true);
                btnModifier.setVisible(false);
                btnSupprimer.setVisible(false);
            }
        });

        btnAjouter.managedProperty().bind(btnAjouter.visibleProperty());
        btnModifier.managedProperty().bind(btnModifier.visibleProperty());
        btnSupprimer.managedProperty().bind(btnSupprimer.visibleProperty());

        // Default state
        tableBadges.getSelectionModel().clearSelection();
        btnAjouter.setVisible(true);
        btnModifier.setVisible(false);
        btnSupprimer.setVisible(false);

        // Filter Listeners
        tfFiltre.textProperty().addListener((obs, oldVal, newVal) -> appliquerFiltre());
        cbFiltreType.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltre());

        // Deselection support
        WindowUtils.setupTableDeselection(tableBadges, mainContainer);
    }

    private void chargerDonnees() {
        try {
            listeBadges.clear();
            listeBadges.addAll(badgeService.recuperer());
            // Initial filter application
            appliquerFiltre();
        } catch (Exception e) {
            WindowUtils.showAlert("Erreur", "Impossible de charger les badges: " + e.getMessage());
        }
    }

    private void appliquerFiltre() {
        String filterName = tfFiltre.getText().toLowerCase().trim();
        String filterTypeStr = cbFiltreType.getValue();

        listeFiltree.clear();
        for (Badge b : listeBadges) {
            boolean matchesName = filterName.isEmpty() ||
                    b.getName().toLowerCase().contains(filterName);

            boolean matchesType = true;
            if (filterTypeStr != null && !filterTypeStr.equals("TOUT")) {
                matchesType = b.getConditionType().name().equals(filterTypeStr);
            }

            if (matchesName && matchesType) {
                listeFiltree.add(b);
            }
        }
        tableBadges.setItems(listeFiltree);
        mettreAJourInfo();
    }

    @FXML
    void resetFiltre(ActionEvent event) {
        tfFiltre.clear();
        cbFiltreType.setValue("TOUT");
        // appliquerFiltre() triggered by listeners
    }

    private void remplirFormulaire(Badge b) {
        tfName.setText(b.getName());
        taDescription.setText(b.getDescription());
        tfIcon.setText(b.getIcon());
        cbType.setValue(b.getConditionType());
        tfThreshold.setText(String.valueOf(b.getThreshold()));
    }

    private void viderFormulaire() {
        tfName.clear();
        taDescription.clear();
        tfIcon.clear();
        cbType.setValue(null);
        tfThreshold.clear();
    }

    private boolean validerFormulaire() {
        if (tfName.getText().trim().isEmpty()) {
            WindowUtils.showAlert("Attention", "Le nom est obligatoire.");
            return false;
        }
        if (cbType.getValue() == null) {
            WindowUtils.showAlert("Attention", "Le type est obligatoire.");
            return false;
        }
        try {
            int t = Integer.parseInt(tfThreshold.getText().trim());
            if (t <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Attention", "Le seuil doit être un entier positif.");
            return false;
        }
        return true;
    }

    @FXML
    void ajouter(ActionEvent event) {
        if (!validerFormulaire())
            return;
        try {
            Badge b = new Badge(
                    tfName.getText().trim(),
                    taDescription.getText().trim(),
                    tfIcon.getText().trim(),
                    cbType.getValue(),
                    Integer.parseInt(tfThreshold.getText().trim()));
            badgeService.ajouter(b);
            chargerDonnees();
            vider(event);
            WindowUtils.showAlert("Succès", "Badge ajouté !");
        } catch (Exception e) {
            WindowUtils.showAlert("Erreur", "Erreur lors de l'ajout : " + e.getMessage());
        }
    }

    @FXML
    void modifier(ActionEvent event) {
        Badge selected = tableBadges.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;
        if (!validerFormulaire())
            return;

        try {
            selected.setName(tfName.getText().trim());
            selected.setDescription(taDescription.getText().trim());
            selected.setIcon(tfIcon.getText().trim());
            selected.setConditionType(cbType.getValue());
            selected.setThreshold(Integer.parseInt(tfThreshold.getText().trim()));

            badgeService.modifier(selected);
            chargerDonnees();
            vider(event);
            WindowUtils.showAlert("Succès", "Badge modifié !");
        } catch (Exception e) {
            WindowUtils.showAlert("Erreur", "Erreur lors de la modification : " + e.getMessage());
        }
    }

    @FXML
    void supprimer(ActionEvent event) {
        Badge selected = tableBadges.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        if (WindowUtils.showConfirmation("Confirmation", "Voulez-vous vraiment supprimer ce badge ?")) {
            try {
                badgeService.supprimer(selected.getId());
                chargerDonnees();
                vider(event);
                WindowUtils.showAlert("Succès", "Badge supprimé !");
            } catch (Exception e) {
                WindowUtils.showAlert("Erreur", "Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    @FXML
    void vider(ActionEvent event) {
        viderFormulaire();
        tableBadges.getSelectionModel().clearSelection();
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Admin Dashboard");
    }

    private void mettreAJourInfo() {
        WindowUtils.updateInfoLabel(lblInfo, tableBadges.getItems().size(), "badge");
    }
}
