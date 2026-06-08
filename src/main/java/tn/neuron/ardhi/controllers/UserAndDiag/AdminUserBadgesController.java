package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import tn.neuron.ardhi.models.UserAndDiag.Badge;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.models.UserAndDiag.UserBadge;
import tn.neuron.ardhi.services.UserAndDiag.BadgeService;
import tn.neuron.ardhi.services.UserAndDiag.UserBadgeService;
import tn.neuron.ardhi.services.UserAndDiag.UserService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.sql.SQLException;

import java.util.ResourceBundle;

public class AdminUserBadgesController implements Initializable {

    @FXML
    private TableView<UserBadge> tableUserBadges;
    @FXML
    private TableColumn<UserBadge, String> colUser;
    @FXML
    private TableColumn<UserBadge, String> colBadge;
    @FXML
    private TableColumn<UserBadge, String> colDate;

    @FXML
    private ComboBox<User> cbUser;
    @FXML
    private ComboBox<Badge> cbBadge;

    @FXML
    private VBox mainContainer;
    @FXML
    private Label lblTitreFormulaire;
    @FXML
    private Button btnAttribuer;
    @FXML
    private Button btnRevoquer;
    @FXML
    private TextField tfFiltre;
    @FXML
    private Label lblInfo;

    private final UserBadgeService userBadgeService = new UserBadgeService();
    private final BadgeService badgeService = new BadgeService();
    private final UserService userService = new UserService();

    private final ObservableList<UserBadge> listeAffectations = FXCollections.observableArrayList();
    private final ObservableList<User> listeUsers = FXCollections.observableArrayList();
    private final ObservableList<Badge> listeBadges = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Table Columns
        colUser.setCellValueFactory(new PropertyValueFactory<>("userName")); // display name
        colBadge.setCellValueFactory(new PropertyValueFactory<>("badgeName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("acquiredAt"));

        // Load ComboBox data
        chargerUsers();
        chargerBadges();

        // Load Table data
        chargerDonnees();

        // setup ComboBox Converters for readable display
        setupComboBoxConverters();

        // Selection Listener
        tableUserBadges.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                lblTitreFormulaire.setText("Gérer l'affectation");
                selectionnerDansCombo(newVal);

                btnAttribuer.setVisible(false);
                btnRevoquer.setVisible(true);
            } else {
                lblTitreFormulaire.setText("Attribuer un badge");
                viderFormulaire();
                btnAttribuer.setVisible(true);
                btnRevoquer.setVisible(false);
            }
        });

        btnAttribuer.managedProperty().bind(btnAttribuer.visibleProperty());
        btnRevoquer.managedProperty().bind(btnRevoquer.visibleProperty());

        // Deselection support
        WindowUtils.setupTableDeselection(tableUserBadges, mainContainer);

        // Filter listener
        tfFiltre.textProperty().addListener((obs, oldVal, newVal) -> appliquerFiltre());
    }

    private void setupComboBoxConverters() {
        cbUser.setConverter(new StringConverter<User>() {
            @Override
            public String toString(User u) {
                return (u == null) ? "" : u.getNom() + " " + u.getPrenom() + " (" + u.getEmail() + ")";
            }

            @Override
            public User fromString(String string) {
                return null; // Not needed for selection
            }
        });

        cbBadge.setConverter(new StringConverter<Badge>() {
            @Override
            public String toString(Badge b) {
                return (b == null) ? "" : b.getIcon() + " " + b.getName();
            }

            @Override
            public Badge fromString(String string) {
                return null;
            }
        });
    }

    private void chargerUsers() {
        try {
            listeUsers.clear();
            listeUsers.addAll(userService.recuperer()); // Assuming recuperer() returns list of users
            cbUser.setItems(listeUsers);
        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", "Erreur chargement utilisateurs: " + e.getMessage());
        }
    }

    private void chargerBadges() {
        try {
            listeBadges.clear();
            listeBadges.addAll(badgeService.recuperer());
            cbBadge.setItems(listeBadges);
        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", "Erreur chargement badges: " + e.getMessage());
        }
    }

    private void chargerDonnees() {
        try {
            listeAffectations.clear();
            listeAffectations.addAll(userBadgeService.recupererTout());
            tableUserBadges.setItems(listeAffectations);
            mettreAJourInfo();
        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", "Impossible de charger les affectations: " + e.getMessage());
        }
    }

    private void selectionnerDansCombo(UserBadge ub) {
        // Select User
        for (User u : listeUsers) {
            if (u.getId() == ub.getUserId()) {
                cbUser.setValue(u);
                break;
            }
        }
        // Select Badge
        for (Badge b : listeBadges) {
            if (b.getId() == ub.getBadgeId()) {
                cbBadge.setValue(b);
                break;
            }
        }
        // Disable combos in "Revoke" mode to avoid confusion or keep enabled?
        // Let's keep enabled but maybe readonly?
        // Simpler: Just allow re-selection creates "Add mode" automatically via
        // listener if we change selection?
        // No, table selection drives the state.
    }

    @FXML
    void attribuer(ActionEvent event) {
        User u = cbUser.getValue();
        Badge b = cbBadge.getValue();

        if (u == null || b == null) {
            WindowUtils.showAlert("Attention", "Veuillez sélectionner un utilisateur et un badge.");
            return;
        }

        try {
            userBadgeService.attribuerBadge(u.getId(), b.getId());
            chargerDonnees();
            vider(event);
            WindowUtils.showAlert("Succès", "Badge attribué !");
        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", e.getMessage());
        }
    }

    @FXML
    void revoquer(ActionEvent event) {
        UserBadge selected = tableUserBadges.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        if (WindowUtils.showConfirmation("Confirmation", "Révoquer ce badge pour cet utilisateur ?")) {
            try {
                userBadgeService.retirerBadge(selected.getUserId(), selected.getBadgeId());
                chargerDonnees();
                vider(event);
                WindowUtils.showAlert("Succès", "Badge révoqué !");
            } catch (SQLException e) {
                WindowUtils.showAlert("Erreur", "Erreur lors de la révocation: " + e.getMessage());
            }
        }
    }

    @FXML
    void vider(ActionEvent event) {
        viderFormulaire();
        tableUserBadges.getSelectionModel().clearSelection();
    }

    private void viderFormulaire() {
        cbUser.setValue(null);
        cbBadge.setValue(null);
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Admin Dashboard");
    }

    @FXML
    void resetFiltre(ActionEvent event) {
        tfFiltre.clear();
        tableUserBadges.setItems(listeAffectations);
        mettreAJourInfo();
    }

    private void appliquerFiltre() {
        String filter = tfFiltre.getText().toLowerCase().trim();
        if (filter.isEmpty()) {
            tableUserBadges.setItems(listeAffectations);
        } else {
            ObservableList<UserBadge> filtered = FXCollections.observableArrayList();
            for (UserBadge ub : listeAffectations) {
                if (ub.getUserName().toLowerCase().contains(filter) ||
                        ub.getBadgeName().toLowerCase().contains(filter)) {
                    filtered.add(ub);
                }
            }
            tableUserBadges.setItems(filtered);
        }
        mettreAJourInfo();
    }

    private void mettreAJourInfo() {
        WindowUtils.updateInfoLabel(lblInfo, tableUserBadges.getItems().size(), "affectation");
    }
}
