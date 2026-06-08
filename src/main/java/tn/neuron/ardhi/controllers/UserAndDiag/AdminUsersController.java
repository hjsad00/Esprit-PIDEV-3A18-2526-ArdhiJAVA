package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.models.UserAndDiag.Role;
import tn.neuron.ardhi.services.UserAndDiag.UserService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.ValidationUtils;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminUsersController implements Initializable {

    @FXML
    private TableView<User> tvUsers;
    @FXML
    private TableColumn<User, Integer> colId;
    @FXML
    private TableColumn<User, String> colNom;
    @FXML
    private TableColumn<User, String> colPrenom;
    @FXML
    private TableColumn<User, String> colEmail;
    @FXML
    private TableColumn<User, Role> colRole;
    @FXML
    private TableColumn<User, String> colPhone;
    @FXML
    private TableColumn<User, String> colLocation;

    @FXML
    private TextField tfNomEdit;
    @FXML
    private TextField tfPrenomEdit;
    @FXML
    private TextField tfEmailEdit;
    @FXML
    private ComboBox<Role> cbRoleEdit;
    @FXML
    private PasswordField tfMdpEdit;
    @FXML
    private TextField tfPhoneEdit;
    @FXML
    private TextField tfLocationEdit;

    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;
    @FXML
    private VBox mainContainer;

    @FXML
    private Label lblTitreFormulaire;
    @FXML
    private Label lblInfo;
    @FXML
    private TextField tfRecherche;
    @FXML
    private TextField tfFiltreId;
    @FXML
    private ComboBox<String> cbFiltreRole;

    private UserService us = new UserService();
    private ObservableList<User> masterData = FXCollections.observableArrayList();
    private FilteredList<User> filteredData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Colonnes
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));

        // Setup role ComboBox
        cbRoleEdit.setItems(FXCollections.observableArrayList(Role.values()));

        afficherUtilisateurs();

        // Setup des rôles pour le filtre
        cbFiltreRole.getItems().add("Tous");
        for (Role r : Role.values()) {
            cbFiltreRole.getItems().add(r.name());
        }
        cbFiltreRole.setValue("Tous");

        // 2. Recherche combinée (ID + Rôle + Texte)
        filteredData = new FilteredList<>(masterData, p -> true);

        // Listener pour le texte
        tfRecherche.textProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());

        // Listener pour l'ID
        tfFiltreId.textProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());

        // Listener pour le rôle
        cbFiltreRole.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());

        SortedList<User> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tvUsers.comparatorProperty());
        tvUsers.setItems(sortedData);

        // 3. Row Factory (Couleurs)
        tvUsers.setRowFactory(tv -> new TableRow<User>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    if (item.getId() == UserSession.getInstance().getUser().getId()) {
                        setStyle("-fx-background-color: #c8e6c9; -fx-text-fill: black;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        // 4. Écouteur de sélection (C'est ICI que j'ai remis la logique manquante)
        tvUsers.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // --- MODE MODIFICATION ---
                lblTitreFormulaire
                        .setText("Modification de : " + newSelection.getNom() + " " + newSelection.getPrenom());
                tfNomEdit.setText(newSelection.getNom());
                tfPrenomEdit.setText(newSelection.getPrenom());
                tfEmailEdit.setText(newSelection.getEmail());
                if (newSelection.getRole() != null) {
                    cbRoleEdit.setValue(newSelection.getRole());
                }
                tfMdpEdit.setText("");
                tfPhoneEdit.setText(newSelection.getPhone() != null ? newSelection.getPhone() : "");
                tfLocationEdit.setText(newSelection.getLocation() != null ? newSelection.getLocation() : "");

                // GESTION DU CHAMP MOT DE PASSE (CORRIGÉ)
                int currentUserId = UserSession.getInstance().getUser().getId();
                if (newSelection.getId() == currentUserId) {
                    // C'est MOI : Je peux voir le champ pour changer MON mot de passe
                    tfMdpEdit.setVisible(true);
                    tfMdpEdit.setManaged(true);
                    tfMdpEdit.setPromptText("Nouveau mdp (laisser vide si inchangé)");
                } else {
                    // C'est un AUTRE : Je cache le champ mot de passe
                    tfMdpEdit.setVisible(false);
                    tfMdpEdit.setManaged(false);
                }

                btnAjouter.setVisible(false);
                btnModifier.setVisible(true);
                btnSupprimer.setVisible(true);
            } else {
                // --- MODE AJOUT ---
                lblTitreFormulaire.setText("Ajouter un nouvel utilisateur");
                viderChamps(null); // On remet tout à zéro et visible

                btnAjouter.setVisible(true);
                btnModifier.setVisible(false);
                btnSupprimer.setVisible(false);
            }
        });

        tvUsers.getSelectionModel().clearSelection();

        // Initial state: Add mode - hide modification buttons
        btnAjouter.setVisible(true);
        btnModifier.setVisible(false);
        btnSupprimer.setVisible(false);

        // Setup table deselection logic (New centralized utility)
        WindowUtils.setupTableDeselection(tvUsers, mainContainer);
        mettreAJourInfo();
    }

    private void afficherUtilisateurs() {
        try {
            masterData.setAll(us.recuperer());
            mettreAJourInfo();
        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur BDD", "Impossible de charger les utilisateurs : " + e.getMessage());
        }
    }

    private void appliquerFiltres() {
        String texteRecherche = tfRecherche.getText();
        String idRecherche = tfFiltreId.getText();

        // 1. Optimize: Parse ID once
        Integer idCherche = null;
        if (idRecherche != null && !idRecherche.isEmpty()) {
            try {
                idCherche = Integer.parseInt(idRecherche);
            } catch (NumberFormatException e) {
                // Invalid format, ignore ID filter
            }
        }
        final Integer finalId = idCherche;

        // 2. Fetch candidates (DB search if text present, else use masterData)
        try {
            List<User> candidats;
            if (texteRecherche != null && !texteRecherche.trim().isEmpty()) {
                candidats = us.rechercher(texteRecherche.trim());
            } else {
                candidats = new ArrayList<>(masterData);
            }

            // 3. Apply remaining filters (ID, Role) locally
            ObservableList<User> filteredResult = FXCollections.observableArrayList();

            for (User user : candidats) {
                // Filtre par ID
                if (finalId != null) {
                    if (user.getId() != finalId) {
                        continue;
                    }
                }

                // Filtre par rôle
                String selectedRoleStr = cbFiltreRole.getValue();
                if (selectedRoleStr != null && !selectedRoleStr.equals("Tous")) {
                    try {
                        Role roleDB = Role.valueOf(selectedRoleStr);
                        if (user.getRole() != roleDB) {
                            continue;
                        }
                    } catch (IllegalArgumentException e) {
                        // Ignore
                    }
                }

                filteredResult.add(user);
            }

            // Update Table
            tvUsers.setItems(new SortedList<>(filteredResult, tvUsers.getComparator()));
            mettreAJourInfo();

        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", "Erreur recherche: " + e.getMessage());
        }
    }

    @FXML
    void resetFiltre(ActionEvent event) {
        tfFiltreId.clear();
        tfRecherche.clear();
        cbFiltreRole.setValue("Tous");
        mettreAJourInfo();
    }

    @FXML
    void viderChamps(ActionEvent event) {
        tfNomEdit.clear();
        tfPrenomEdit.clear();
        tfEmailEdit.clear();
        cbRoleEdit.setValue(null);
        tfMdpEdit.clear();
        tfPhoneEdit.clear();
        tfLocationEdit.clear();

        // QUAND ON VIDE, ON REPASSE EN MODE AJOUT -> ON MONTRE LE MDP (CORRIGÉ)
        tfMdpEdit.setVisible(true);
        tfMdpEdit.setManaged(true);
        tfMdpEdit.setPromptText("Mot de passe (Obligatoire)");

        tvUsers.getSelectionModel().clearSelection();
    }

    @FXML
    void ajouterUser(ActionEvent event) {
        String nom = tfNomEdit.getText();
        String prenom = tfPrenomEdit.getText();
        String email = tfEmailEdit.getText();
        Role role = cbRoleEdit.getValue();
        String mdp = tfMdpEdit.getText();

        if (ValidationUtils.champsVides(nom, prenom, email) || role == null || mdp.isEmpty()) {
            WindowUtils.showAlert("Erreur", "Tous les champs sont obligatoires.");
            return;
        }
        if (!ValidationUtils.validerEmail(email)) {
            WindowUtils.showAlert("Erreur", "Email invalide.");
            return;
        }
        if (!ValidationUtils.validerNom(nom) || !ValidationUtils.validerNom(prenom)) {
            WindowUtils.showAlert("Erreur", "Noms invalides.");
            return;
        }
        if (!ValidationUtils.validerMotDePasse(mdp)) {
            WindowUtils.showAlert("Erreur", "Mot de passe trop court (min 6).");
            return;
        }

        User u = new User(email, role, mdp, nom, prenom);
        String phone = tfPhoneEdit.getText() != null ? tfPhoneEdit.getText().trim() : "";
        u.setPhone(phone.isEmpty() ? null : phone);
        String loc = tfLocationEdit.getText() != null ? tfLocationEdit.getText().trim() : "";
        u.setLocation(loc.isEmpty() ? null : loc);
        try {
            us.ajouter(u);
            afficherUtilisateurs();
            viderChamps(null);
            WindowUtils.showAlert("Succès", "Utilisateur ajouté.");
        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", e.getMessage());
        }
    }

    @FXML
    void modifierUser(ActionEvent event) {
        User selectedUser = tvUsers.getSelectionModel().getSelectedItem();
        if (selectedUser == null)
            return;

        String nom = tfNomEdit.getText();
        String prenom = tfPrenomEdit.getText();
        String email = tfEmailEdit.getText();
        Role role = cbRoleEdit.getValue();

        // Note : On ne récupère le mdp que s'il est visible (donc si c'est mon compte)
        String nouveauMdp = tfMdpEdit.isVisible() ? tfMdpEdit.getText() : "";

        if (ValidationUtils.champsVides(nom, prenom, email) || role == null) {
            WindowUtils.showAlert("Erreur", "Champs vides.");
            return;
        }
        if (!ValidationUtils.validerEmail(email)) {
            WindowUtils.showAlert("Erreur", "Email invalide.");
            return;
        }
        if (!ValidationUtils.validerNom(nom) || !ValidationUtils.validerNom(prenom)) {
            WindowUtils.showAlert("Erreur", "Noms invalides.");
            return;
        }

        // Si c'est mon compte et que j'ai tapé un mdp, on vérifie sa longueur
        if (!nouveauMdp.isEmpty() && !ValidationUtils.validerMotDePasse(nouveauMdp)) {
            WindowUtils.showAlert("Erreur", "Nouveau mot de passe trop court.");
            return;
        }

        selectedUser.setNom(nom);
        selectedUser.setPrenom(prenom);
        selectedUser.setEmail(email);
        selectedUser.setRole(role);

        // Update phone and location
        String phone = tfPhoneEdit.getText() != null ? tfPhoneEdit.getText().trim() : "";
        selectedUser.setPhone(phone.isEmpty() ? null : phone);
        String loc = tfLocationEdit.getText() != null ? tfLocationEdit.getText().trim() : "";
        selectedUser.setLocation(loc.isEmpty() ? null : loc);

        try {
            // 1. On modifie les infos de base
            us.modifier(selectedUser);

            // 2. Si un nouveau mot de passe a été saisi, on le met à jour aussi (CORRIGÉ)
            if (!nouveauMdp.isEmpty()) {
                us.modifierMotDePasse(selectedUser.getId(), nouveauMdp);
            }

            afficherUtilisateurs();
            tvUsers.refresh(); // Important pour voir le changement immédiat
            WindowUtils.showAlert("Succès", "Modifié.");
        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", e.getMessage());
        }
    }

    @FXML
    void supprimerUser(ActionEvent event) {
        User selectedUser = tvUsers.getSelectionModel().getSelectedItem();
        if (selectedUser == null)
            return;

        if (selectedUser.getId() == UserSession.getInstance().getUser().getId()) {
            WindowUtils.showAlert("Action impossible", "Vous ne pouvez pas vous supprimer !");
            return;
        }

        // Confirmation dialog
        if (WindowUtils.showConfirmation("Confirmation",
                "Êtes-vous sûr de vouloir bannir " + selectedUser.getNom() + " " + selectedUser.getPrenom() + " ?")) {
            try {
                us.supprimer(selectedUser.getId());
                afficherUtilisateurs();
                viderChamps(null);
                WindowUtils.showAlert("Succès", "Utilisateur banni avec succès !");
            } catch (SQLException e) {
                WindowUtils.showAlert("Erreur", e.getMessage());
            }
        }
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Admin Dashboard");
    }

    private void mettreAJourInfo() {
        WindowUtils.updateInfoLabel(lblInfo, tvUsers.getItems().size(), "utilisateur");
    }

}