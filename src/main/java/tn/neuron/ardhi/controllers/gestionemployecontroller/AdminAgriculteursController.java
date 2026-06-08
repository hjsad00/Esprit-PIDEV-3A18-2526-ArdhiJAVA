package tn.neuron.ardhi.controllers.gestionemployecontroller;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.services.UserAndDiag.UserService;
import tn.neuron.ardhi.services.gestionemployeservice.EmployeService;
import tn.neuron.ardhi.services.gestionemployeservice.TacheService;
import tn.neuron.ardhi.utils.gestionemployeutils.AgriculteurContext;
import tn.neuron.ardhi.utils.gestionemployeutils.LanguageManager;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller pour la gestion de la liste des agriculteurs (Admin)
 * Permet à l'admin de superviser chaque agriculteur
 */
public class AdminAgriculteursController implements Initializable {

    @FXML private TableView<User> tableAgriculteurs;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colPrenom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, Integer> colNbEmployes;
    @FXML private TableColumn<User, Integer> colNbTaches;
    @FXML private TableColumn<User, Void> colActions;

    @FXML private TextField txtRecherche;
    @FXML private Label lblTotalAgriculteurs;
    @FXML private Button btnRetour;

    private UserService userService;
    private EmployeService employeService;
    private TacheService tacheService;
    private ObservableList<User> masterList;
    private FilteredList<User> filteredList;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userService = new UserService();
        employeService = new EmployeService();
        tacheService = new TacheService();
        masterList = FXCollections.observableArrayList();

        setupTableColumns();
        setupFilteredList();
        loadAgriculteurs();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        // Colonne Nb Employés (calculée d)
        colNbEmployes.setCellFactory(column -> new TableCell<User, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    int count = employeService.countByAgriculteur(user.getId());
                    setText(String.valueOf(count));
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Colonne Nb Tâches (calculée)
        colNbTaches.setCellFactory(column -> new TableCell<User, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());//Récupère utilisateur de la ligne actuelle.
                    int count = tacheService.countByAgriculteur(user.getId());
                    setText(String.valueOf(count));
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Colonne Actions (boutons)
        colActions.setCellFactory(column -> new TableCell<User, Void>() {
            private final Button btnEmployes = new Button("👷 Employés");
            private final Button btnTaches = new Button("📝 Tâches");
            private final HBox hbox = new HBox(10, btnEmployes, btnTaches);

            {
                hbox.setAlignment(Pos.CENTER);
                btnEmployes.setStyle("-fx-background-color: #75cc2e; -fx-text-fill: white; -fx-cursor: hand;");
                btnTaches.setStyle("-fx-background-color: #4e87ae; -fx-text-fill: white; -fx-cursor: hand;");

                btnEmployes.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    ouvrirGestionEmployes(user);
                });

                btnTaches.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    ouvrirGestionTaches(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(hbox);
                }
            }
        });
    }

    private void setupFilteredList() {
        filteredList = new FilteredList<>(masterList, p -> true);
        tableAgriculteurs.setItems(filteredList);
    }

    private void loadAgriculteurs() {
        masterList.clear();
        List<User> agriculteurs = userService.getAgriculteurs();
        masterList.addAll(agriculteurs);
        updateStatistics();
    }

    private void updateStatistics() {
        if (lblTotalAgriculteurs != null) {
            lblTotalAgriculteurs.setText(String.valueOf(masterList.size()));
        }
    }

    @FXML
    private void handleRechercher() {
        String searchText = txtRecherche.getText().toLowerCase().trim();

        filteredList.setPredicate(user -> {
            if (searchText.isEmpty()) {
                return true;
            }

            return user.getNom().toLowerCase().contains(searchText) ||
                    user.getPrenom().toLowerCase().contains(searchText) ||
                    user.getEmail().toLowerCase().contains(searchText);
        });
    }

    @FXML
    private void handleActualiser() {
        txtRecherche.clear();
        loadAgriculteurs();
        filteredList.setPredicate(p -> true);
    }
    //Récupère l'agriculteur sélectionné dans le tableau
    @FXML
    private void handleTableClick(MouseEvent event) {
        if (event.getClickCount() == 2) { // Double-clic
            User selected = tableAgriculteurs.getSelectionModel().getSelectedItem();
            if (selected != null) {
                ouvrirGestionEmployes(selected);
            }
        }
    }

    /**
     * Ouvrir la gestion des employés d'un agriculteur
     */
    private void ouvrirGestionEmployes(User agriculteur) {
        try {
            // Définir le contexte de supervision
            AgriculteurContext.getInstance().setAgriculteur(
                    agriculteur.getId(),
                    agriculteur.getPrenom() + " " + agriculteur.getNom()
            );

            // Ouvrir la même interface que l'agriculteur
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/gestionemploye/employe_list.fxml"),
                    LanguageManager.getInstance().getBundle()
            );
            Stage stage = new Stage();
            stage.setTitle("Gestion Employes - " + agriculteur.getPrenom() + " " + agriculteur.getNom());
            stage.setScene(new Scene(loader.load(), WindowUtils.APP_WIDTH, WindowUtils.APP_HEIGHT));
            stage.setResizable(true);
            WindowUtils.applyStandardSize(stage);

            // Nettoyer le contexte quand la fenêtre se ferme
            stage.setOnHidden(e -> AgriculteurContext.getInstance().clear());

            stage.show();

            System.out.println("✅ Admin supervise employés de: " + agriculteur.getNom());

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la gestion des employés: " + e.getMessage());
        }
    }

    /**
     * Ouvrir la gestion des tâches d'un agriculteur
     */
    private void ouvrirGestionTaches(User agriculteur) {
        try {
            // Définir le contexte de supervision
            AgriculteurContext.getInstance().setAgriculteur(
                    agriculteur.getId(),
                    agriculteur.getPrenom() + " " + agriculteur.getNom()
            );

            // Ouvrir la même interface que l'agriculteur
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/gestionemploye/tache_list.fxml"),
                    LanguageManager.getInstance().getBundle()
            );
            Stage stage = new Stage();
            stage.setTitle("Gestion Taches - " + agriculteur.getPrenom() + " " + agriculteur.getNom());
            stage.setScene(new Scene(loader.load(), WindowUtils.APP_WIDTH, WindowUtils.APP_HEIGHT));
            stage.setResizable(true);
            WindowUtils.applyStandardSize(stage);

            // Nettoyer le contexte quand la fenêtre se ferme
            stage.setOnHidden(e -> AgriculteurContext.getInstance().clear());

            stage.show();

            System.out.println("✅ Admin supervise tâches de: " + agriculteur.getNom());

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la gestion des tâches: " + e.getMessage());
        }
    }

    @FXML
    private void handleRetour() {
        try {
            Stage currentStage = (Stage) btnRetour.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserAndDiag/AdminDashboard.fxml"));
            Stage dashboardStage = new Stage();
            dashboardStage.setTitle("Dashboard Admin - Ardhi");
            dashboardStage.setScene(new Scene(loader.load()));
            dashboardStage.show();
            currentStage.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}