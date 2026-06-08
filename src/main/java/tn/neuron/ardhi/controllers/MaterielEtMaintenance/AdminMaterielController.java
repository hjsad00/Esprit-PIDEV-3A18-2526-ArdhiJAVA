package tn.neuron.ardhi.controllers.MaterielEtMaintenance;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.neuron.ardhi.models.MaterielEtMaintenance.Materiel;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.services.MaterielEtMaintenance.MaterielService;
import tn.neuron.ardhi.services.UserAndDiag.UserService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class AdminMaterielController implements Initializable {

    @FXML
    private TableView<Materiel> tvMateriels;
    @FXML
    private TableColumn<Materiel, Integer> colId;
    @FXML
    private TableColumn<Materiel, String> colNom;
    @FXML
    private TableColumn<Materiel, String> colType;
    @FXML
    private TableColumn<Materiel, String> colEtat;
    @FXML
    private TableColumn<Materiel, String> colProprietaire;
    @FXML
    private TableColumn<Materiel, LocalDate> colDateAchat;
    @FXML
    private TableColumn<Materiel, LocalDate> colProchaineMaintenance; // ⭐ NOUVELLE COLONNE

    @FXML
    private TextField tfNom;
    @FXML
    private ComboBox<String> cbType;
    @FXML
    private ComboBox<String> cbEtat;
    @FXML
    private ComboBox<User> cbProprietaire;
    @FXML
    private DatePicker dpDateAchat;

    @FXML
    private TextField tfRecherche;
    @FXML
    private ComboBox<String> cbFiltreType;
    @FXML
    private ComboBox<String> cbFiltreEtat;

    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;
    @FXML
    private Button btnVoirDetails;
    @FXML
    private Button btnPlanifierMaintenance; // ⭐ NOUVEAU BOUTON
    @FXML
    private Button btnVider;

    @FXML
    private Label lblMessage;
    @FXML
    private VBox mainContainer;

    private MaterielService ms = new MaterielService();
    private UserService us = new UserService();
    private ObservableList<Materiel> masterData = FXCollections.observableArrayList();
    private FilteredList<Materiel> filteredData;
    private ObservableList<User> agriculteursList = FXCollections.observableArrayList();
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurerColonnes();
        configurerComboBox();
        chargerAgriculteurs();
        afficherMateriels();
        configurerFiltres();
        configurerSelection();
    }

    private void configurerColonnes() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id_materiel"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colEtat.setCellValueFactory(new PropertyValueFactory<>("etat"));

        // Colonne date d'achat
        if (colDateAchat != null) {
            colDateAchat.setCellValueFactory(new PropertyValueFactory<>("date_achat"));
            colDateAchat.setCellFactory(column -> new TableCell<Materiel, LocalDate>() {
                @Override
                protected void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    if (empty || date == null) {
                        setText(null);
                    } else {
                        setText(dateFormatter.format(date));
                    }
                }
            });
        }

        // ⭐ NOUVELLE COLONNE - Prochaine Maintenance
        if (colProchaineMaintenance != null) {
            colProchaineMaintenance.setCellValueFactory(new PropertyValueFactory<>("date_prochaine_maintenance"));
            colProchaineMaintenance.setCellFactory(column -> new TableCell<Materiel, LocalDate>() {
                @Override
                protected void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    if (empty || date == null) {
                        setText("-");
                        setStyle("");
                    } else {
                        setText(dateFormatter.format(date));

                        // Colorier selon la proximité
                        long jours = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date);
                        if (jours < 0) {
                            setStyle("-fx-background-color: #ffcccc; -fx-text-fill: #cc0000;"); // Rouge - en retard
                        } else if (jours <= 7) {
                            setStyle("-fx-background-color: #ffebcc; -fx-text-fill: #ff6600;"); // Orange - urgent
                        } else if (jours <= 30) {
                            setStyle("-fx-background-color: #fff4cc; -fx-text-fill: #cc8800;"); // Jaune - bientôt
                        } else {
                            setStyle("-fx-background-color: #ccffcc; -fx-text-fill: #006600;"); // Vert - OK
                        }
                    }
                }
            });
        }

        // Colonne propriétaire
        colProprietaire.setCellValueFactory(cellData -> {
            int userId = cellData.getValue().getUser_id();
            for (User user : agriculteursList) {
                if (user.getId() == userId) {
                    return new javafx.beans.property.SimpleStringProperty(
                            user.getNom() + " " + user.getPrenom());
                }
            }
            return new javafx.beans.property.SimpleStringProperty("Inconnu");
        });
    }

    private void configurerComboBox() {
        cbType.setItems(FXCollections.observableArrayList(
                "Tracteur", "Moissonneuse", "Pulvérisateur", "Charrue",
                "Semoir", "Remorque", "Autre"));

        cbEtat.setItems(FXCollections.observableArrayList(
                "Neuf", "Bon", "Moyen", "En Maintenance", "En panne"));

        cbFiltreType.setItems(FXCollections.observableArrayList(
                "Tous", "Tracteur", "Moissonneuse", "Pulvérisateur",
                "Charrue", "Semoir", "Remorque", "Autre"));
        cbFiltreType.setValue("Tous");

        cbFiltreEtat.setItems(FXCollections.observableArrayList(
                "Tous", "Neuf", "Bon", "Moyen", "En Maintenance", "En panne"));
        cbFiltreEtat.setValue("Tous");
    }

    private void chargerAgriculteurs() {
        try {
            List<User> allUsers = us.recuperer();
            for (User user : allUsers) {
                if (user.getRole().name().equals("AGRICULTEUR")) {
                    agriculteursList.add(user);
                }
            }

            // Configuration de l'affichage dans le ComboBox
            cbProprietaire.setCellFactory(param -> new ListCell<User>() {
                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    if (empty || user == null) {
                        setText(null);
                    } else {
                        // Pour être cohérent avec le tableau, afficher nom + prénom
                        setText(user.getNom() + " " + user.getPrenom());
                    }
                }
            });

            // Configuration de l'affichage du bouton sélectionné
            cbProprietaire.setButtonCell(new ListCell<User>() {
                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    if (empty || user == null) {
                        setText(null);
                    } else {
                        setText(user.getNom() + " " + user.getPrenom());
                    }
                }
            });

            cbProprietaire.setItems(agriculteursList);

            if (!agriculteursList.isEmpty()) {
                cbProprietaire.setValue(agriculteursList.get(0));
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les agriculteurs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void afficherMateriels() {
        try {
            masterData.setAll(ms.recuperer());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les matériels: " + e.getMessage());
        }
    }

    private void configurerFiltres() {
        filteredData = new FilteredList<>(masterData, p -> true);

        tfRecherche.textProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());
        cbFiltreType.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());
        cbFiltreEtat.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());

        SortedList<Materiel> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tvMateriels.comparatorProperty());
        tvMateriels.setItems(sortedData);
    }

    private void configurerSelection() {
        tvMateriels.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                chargerMaterielDansFormulaire(newSelection);
                btnAjouter.setVisible(false);
                btnModifier.setVisible(true);
                btnSupprimer.setVisible(true);
                btnVoirDetails.setVisible(true);
                btnPlanifierMaintenance.setVisible(true); // ⭐ Afficher bouton maintenance
                lblMessage.setText("Modification du matériel sélectionné");
            } else {
                viderFormulaire(null);
                btnAjouter.setVisible(true);
                btnModifier.setVisible(false);
                btnSupprimer.setVisible(false);
                btnVoirDetails.setVisible(false);
                btnPlanifierMaintenance.setVisible(false); // ⭐ Cacher bouton maintenance
                lblMessage.setText("Ajout d'un nouveau matériel");
            }
        });

        btnAjouter.setVisible(true);
        btnModifier.setVisible(false);
        btnSupprimer.setVisible(false);
        btnVoirDetails.setVisible(false);
        btnPlanifierMaintenance.setVisible(false); // ⭐ Caché par défaut
        lblMessage.setText("Ajout d'un nouveau matériel");
    }

    private void appliquerFiltres() {
        String texteRecherche = tfRecherche.getText();
        String typeFiltre = cbFiltreType.getValue();
        String etatFiltre = cbFiltreEtat.getValue();

        filteredData.setPredicate(materiel -> {
            if (texteRecherche != null && !texteRecherche.isEmpty()) {
                if (!materiel.getNom().toLowerCase().contains(texteRecherche.toLowerCase())) {
                    return false;
                }
            }

            if (typeFiltre != null && !typeFiltre.equals("Tous")) {
                if (!materiel.getType().equals(typeFiltre)) {
                    return false;
                }
            }

            if (etatFiltre != null && !etatFiltre.equals("Tous")) {
                if (!materiel.getEtat().equals(etatFiltre)) {
                    return false;
                }
            }

            return true;
        });
    }

    private void chargerMaterielDansFormulaire(Materiel m) {
        tfNom.setText(m.getNom());
        cbType.setValue(m.getType());
        cbEtat.setValue(m.getEtat());

        if (dpDateAchat != null) {
            dpDateAchat.setValue(m.getDate_achat());
        }

        for (User user : agriculteursList) {
            if (user.getId() == m.getUser_id()) {
                cbProprietaire.setValue(user);
                break;
            }
        }
    }

    @FXML
    private void viderFormulaire(ActionEvent event) {
        tfNom.clear();
        cbType.setValue(null);
        cbEtat.setValue(null);

        if (dpDateAchat != null) {
            dpDateAchat.setValue(null);
        }

        if (!agriculteursList.isEmpty()) {
            cbProprietaire.setValue(agriculteursList.get(0));
        }
        tvMateriels.getSelectionModel().clearSelection();
        lblMessage.setText("Ajout d'un nouveau matériel");
    }

    @FXML
    private void ajouterMateriel(ActionEvent event) {
        if (!validerFormulaire())
            return;

        try {
            Materiel m = new Materiel(
                    tfNom.getText().trim(),
                    cbType.getValue(),
                    cbEtat.getValue(),
                    cbProprietaire.getValue().getId());

            if (dpDateAchat != null && dpDateAchat.getValue() != null) {
                m.setDate_achat(dpDateAchat.getValue());
            }

            ms.ajouter(m);
            afficherMateriels();
            viderFormulaire(event);
            showAlert("Succès", "Matériel ajouté avec succès !");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de l'ajout: " + e.getMessage());
        }
    }

    @FXML
    private void modifierMateriel(ActionEvent event) {
        Materiel selected = tvMateriels.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Erreur", "Veuillez sélectionner un matériel");
            return;
        }

        if (!validerFormulaire())
            return;

        selected.setNom(tfNom.getText().trim());
        selected.setType(cbType.getValue());
        selected.setEtat(cbEtat.getValue());
        selected.setUser_id(cbProprietaire.getValue().getId());

        if (dpDateAchat != null) {
            selected.setDate_achat(dpDateAchat.getValue());
        }

        try {
            ms.modifier(selected);
            afficherMateriels();
            viderFormulaire(event);
            showAlert("Succès", "Matériel modifié avec succès !");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de la modification: " + e.getMessage());
        }
    }

    @FXML
    private void supprimerMateriel(ActionEvent event) {
        Materiel selected = tvMateriels.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Erreur", "Veuillez sélectionner un matériel");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le matériel ?");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer \"" + selected.getNom() + "\" ?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    ms.supprimer(selected.getId_materiel());
                    afficherMateriels();
                    viderFormulaire(event);
                    showAlert("Succès", "Matériel supprimé avec succès !");
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Erreur", "Erreur lors de la suppression: " + e.getMessage());
                }
            }
        });
    }

    // ⭐ NOUVELLE MÉTHODE - Planifier Maintenance
    @FXML
    private void planifierMaintenance(ActionEvent event) {
        Materiel selected = tvMateriels.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Erreur", "Veuillez sélectionner un matériel");
            return;
        }

        try {
            // Charger la fenêtre de planification
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CalendrierMaintenance.fxml"));
            Parent root = loader.load();

            // Récupérer le controller et passer le matériel
            CalendrierMaintenanceController controller = loader.getController();
            controller.setMateriel(selected);

            // Créer une nouvelle fenêtre modale
            Stage stage = new Stage();
            stage.setTitle("Planifier Maintenance - " + selected.getNom());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(((Node) event.getSource()).getScene().getWindow());

            // Attendre la fermeture de la fenêtre
            stage.showAndWait();

            // Rafraîchir la liste après fermeture
            afficherMateriels();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la fenêtre de planification: " + e.getMessage());
        }
    }

    @FXML
    private void voirDetails(ActionEvent event) {
        Materiel selected = tvMateriels.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Erreur", "Veuillez sélectionner un matériel");
            return;
        }

        String dateAchatStr = selected.getDate_achat() != null ? dateFormatter.format(selected.getDate_achat())
                : "Non renseignée";

        // ⭐ AJOUT - Informations de maintenance
        String maintenanceInfo = "";
        if (selected.getDate_prochaine_maintenance() != null) {
            long daysUntil = selected.getDaysUntilMaintenance();
            String dateMaintenanceStr = dateFormatter.format(selected.getDate_prochaine_maintenance());

            if (daysUntil < 0) {
                maintenanceInfo = "\n\n🔧 MAINTENANCE:\n" +
                        "Date prévue: " + dateMaintenanceStr + "\n" +
                        "⚠️ EN RETARD de " + Math.abs(daysUntil) + " jours!";
            } else if (daysUntil == 0) {
                maintenanceInfo = "\n\n🔧 MAINTENANCE:\n" +
                        "Date prévue: " + dateMaintenanceStr + "\n" +
                        "⚠️ AUJOURD'HUI!";
            } else if (daysUntil <= 7) {
                maintenanceInfo = "\n\n🔧 MAINTENANCE:\n" +
                        "Date prévue: " + dateMaintenanceStr + "\n" +
                        "⚠️ URGENT - Dans " + daysUntil + " jour(s)";
            } else if (daysUntil <= 30) {
                maintenanceInfo = "\n\n🔧 MAINTENANCE:\n" +
                        "Date prévue: " + dateMaintenanceStr + "\n" +
                        "📅 Dans " + daysUntil + " jours";
            } else {
                maintenanceInfo = "\n\n🔧 MAINTENANCE:\n" +
                        "Date prévue: " + dateMaintenanceStr + "\n" +
                        "✓ Dans " + daysUntil + " jours";
            }

            // Ajouter l'ID de l'événement si disponible
            if (selected.getGoogle_calendar_event_id() != null &&
                    !selected.getGoogle_calendar_event_id().isEmpty()) {
                maintenanceInfo += "\nID Google Calendar: " + selected.getGoogle_calendar_event_id();
            }
        } else {
            maintenanceInfo = "\n\n🔧 MAINTENANCE:\nAucune maintenance planifiée";
        }

        // Trouver le nom du propriétaire
        String proprietaireNom = "Inconnu";
        for (User user : agriculteursList) {
            if (user.getId() == selected.getUser_id()) {
                proprietaireNom = user.getNom() + " " + user.getPrenom();
                break;
            }
        }

        Alert details = new Alert(Alert.AlertType.INFORMATION);
        details.setTitle("Détails du matériel");
        details.setHeaderText(selected.getNom());
        details.setContentText(
                "ID: #" + selected.getId_materiel() + "\n" +
                        "Type: " + selected.getType() + "\n" +
                        "État: " + selected.getEtat() + "\n" +
                        "Propriétaire: " + proprietaireNom + "\n" +
                        "Date d'achat: " + dateAchatStr +
                        maintenanceInfo);
        details.showAndWait();
    }

    @FXML
    private void resetFiltres(ActionEvent event) {
        tfRecherche.clear();
        cbFiltreType.setValue("Tous");
        cbFiltreEtat.setValue("Tous");
    }

    @FXML
    private void clicTable(MouseEvent event) {
        Node clickedNode = (Node) event.getTarget();
        while (clickedNode != null && clickedNode != tvMateriels) {
            if (clickedNode instanceof TableRow && !((TableRow<?>) clickedNode).isEmpty()) {
                return;
            }
            clickedNode = clickedNode.getParent();
        }
        tvMateriels.getSelectionModel().clearSelection();
    }

    @FXML
    private void retour(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/UserAndDiag/AdminDashboard.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, WindowUtils.APP_WIDTH, WindowUtils.APP_HEIGHT));
        stage.show();
    }

    private boolean validerFormulaire() {
        if (tfNom.getText().trim().isEmpty()) {
            showAlert("Erreur", "Le nom est obligatoire");
            return false;
        }

        if (cbType.getValue() == null) {
            showAlert("Erreur", "Le type est obligatoire");
            return false;
        }

        if (cbEtat.getValue() == null) {
            showAlert("Erreur", "L'état est obligatoire");
            return false;
        }

        if (cbProprietaire.getValue() == null) {
            showAlert("Erreur", "Le propriétaire est obligatoire");
            return false;
        }

        if (dpDateAchat != null && dpDateAchat.getValue() != null &&
                dpDateAchat.getValue().isAfter(LocalDate.now())) {
            showAlert("Erreur", "La date d'achat ne peut pas être dans le futur");
            return false;
        }

        return true;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
