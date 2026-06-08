package tn.neuron.ardhi.controllers.Evenement;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import tn.neuron.ardhi.models.Evenement.Evenement;
import tn.neuron.ardhi.services.Evenement.EmailSchedulerService;
import tn.neuron.ardhi.services.Evenement.EvenementService;
import tn.neuron.ardhi.services.Evenement.ParticipationService;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.stage.Stage;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Contrôleur pour le dashboard admin des événements
 * Gestion complète de tous les événements avec actions administrateur
 */
public class AdminEvenementDashboardController implements Initializable {

    @FXML
    private TableView<Evenement> eventsTable;

    @FXML
    private TableColumn<Evenement, Integer> idColumn;

    @FXML
    private TableColumn<Evenement, String> titreColumn;

    @FXML
    private TableColumn<Evenement, String> typeColumn;

    @FXML
    private TableColumn<Evenement, LocalDate> dateColumn;

    @FXML
    private TableColumn<Evenement, String> lieuColumn;

    @FXML
    private TableColumn<Evenement, Integer> participantsColumn;

    @FXML
    private TableColumn<Evenement, String> statutColumn;

    @FXML
    private TableColumn<Evenement, Void> actionsColumn;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> filterTypeCombo;

    @FXML
    private ComboBox<String> filterStatutCombo;

    @FXML
    private Label totalLabel;

    @FXML
    private Label actifLabel;

    @FXML
    private Label termineLabel;

    @FXML
    private Label annuleLabel;

    private EvenementService evenementService;
    private ParticipationService participationService;
    private ObservableList<Evenement> evenementsList;
    private EmailSchedulerService emailScheduler;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        evenementService = new EvenementService();
        participationService = new ParticipationService();
        emailScheduler = new EmailSchedulerService();
        evenementsList = FXCollections.observableArrayList();

        setupTable();
        setupFilters();
        updateEventStatuses();
        loadAllEvents();
        updateStatistics();
        emailScheduler.start();
    }

    private void setupTable() {
        // Style the table
        eventsTable.setStyle(
                "-fx-background-color: white;" +
                        "-fx-table-cell-border-color: #E0E0E0;" +
                        "-fx-font-size: 13px;"
        );

        // Configuration des colonnes
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titreColumn.setCellValueFactory(new PropertyValueFactory<>("titre"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        lieuColumn.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        participantsColumn.setCellValueFactory(new PropertyValueFactory<>("nombreParticipants"));
        statutColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Style headers with white background and dark text
        String headerStyle = "-fx-background-color: #f8f9fa; -fx-text-fill: #2C3E50; -fx-font-weight: bold; -fx-font-size: 13px; -fx-alignment: CENTER; -fx-border-color: #dee2e6;";
        idColumn.setStyle(headerStyle);
        titreColumn.setStyle(headerStyle);
        typeColumn.setStyle(headerStyle);
        dateColumn.setStyle(headerStyle);
        lieuColumn.setStyle(headerStyle);
        participantsColumn.setStyle(headerStyle);
        statutColumn.setStyle(headerStyle);
        actionsColumn.setStyle(headerStyle);

        // Formater la colonne date
        dateColumn.setCellFactory(column -> new TableCell<Evenement, LocalDate>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(formatter.format(date));
                }
            }
        });

        // Colorer la colonne statut avec meilleur contraste
        statutColumn.setCellFactory(column -> new TableCell<Evenement, String>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(getStatutLabel(statut));
                    String color = switch (statut) {
                        case "A_VENIR" -> "-fx-background-color: #50c878; -fx-text-fill: white;";
                        case "EN_COURS" -> "-fx-background-color: #f5a623; -fx-text-fill: white;";
                        case "TERMINE" -> "-fx-background-color: #95A5A6; -fx-text-fill: white;";
                        case "ANNULE" -> "-fx-background-color: #d4145a; -fx-text-fill: white;";
                        default -> "-fx-background-color: #cccccc; -fx-text-fill: white;";
                    };
                    setStyle(color + " -fx-alignment: center; -fx-padding: 8 15; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 12px;");
                }
            }
        });

        // Colonne d'actions - avec texte français
        actionsColumn.setCellFactory(new Callback<TableColumn<Evenement, Void>, TableCell<Evenement, Void>>() {
            @Override
            public TableCell<Evenement, Void> call(TableColumn<Evenement, Void> param) {
                return new TableCell<Evenement, Void>() {
                    private final Button detailsButton = new Button("Détails");
                    private final Button editButton = new Button("Modifier");
                    private final Button deleteButton = new Button("Supprimer");
                    private final HBox buttonsBox = new HBox(5, detailsButton, editButton, deleteButton);

                    {
                        buttonsBox.setAlignment(javafx.geometry.Pos.CENTER);
                        // Styles pour les boutons avec meilleure visibilité
                        detailsButton.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; " +
                                "-fx-background-radius: 5; -fx-padding: 6 12; " +
                                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-radius: 5;");
                        detailsButton.setMinWidth(75);

                        editButton.setStyle("-fx-background-color: #F39C12; -fx-text-fill: white; " +
                                "-fx-background-radius: 5; -fx-padding: 6 12; " +
                                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-radius: 5;");
                        editButton.setMinWidth(75);

                        deleteButton.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; " +
                                "-fx-background-radius: 5; -fx-padding: 6 12; " +
                                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-radius: 5;");
                        deleteButton.setMinWidth(85);

                        // Tooltips
                        detailsButton.setTooltip(new Tooltip("Voir les détails"));
                        editButton.setTooltip(new Tooltip("Modifier l'événement"));
                        deleteButton.setTooltip(new Tooltip("Supprimer l'événement"));

                        detailsButton.setOnAction(event -> {
                            Evenement evenement = getTableView().getItems().get(getIndex());
                            showEventDetails(evenement);
                        });

                        editButton.setOnAction(event -> {
                            Evenement evenement = getTableView().getItems().get(getIndex());
                            editEvent(evenement);
                        });

                        deleteButton.setOnAction(event -> {
                            Evenement evenement = getTableView().getItems().get(getIndex());
                            deleteEvent(evenement);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(buttonsBox);
                        }
                    }
                };
            }
        });

        eventsTable.setItems(evenementsList);
    }

    private void setupFilters() {
        // Types
        filterTypeCombo.getItems().addAll("Tous", "FOIRE", "FORMATION", "CONFERENCE", "ATELIER");
        filterTypeCombo.setValue("Tous");

        // Statuts
        filterStatutCombo.getItems().addAll("Tous", "A_VENIR", "EN_COURS", "TERMINE", "ANNULE");
        filterStatutCombo.setValue("Tous");

        // Listeners
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                loadAllEvents();
            } else {
                searchEvents(newVal);
            }
        });

        filterTypeCombo.setOnAction(e -> applyFilters());
        filterStatutCombo.setOnAction(e -> applyFilters());
    }

    private void loadAllEvents() {
        List<Evenement> events = evenementService.getAllEvenements();
        evenementsList.clear();
        evenementsList.addAll(events);
    }

    private void searchEvents(String keyword) {
        List<Evenement> events = evenementService.rechercherEvenements(keyword);
        evenementsList.clear();
        evenementsList.addAll(events);
    }

    private void applyFilters() {
        String type = filterTypeCombo.getValue();
        String statut = filterStatutCombo.getValue();

        List<Evenement> events;

        if (!"Tous".equals(type) && !"Tous".equals(statut)) {
            events = evenementService.getAllEvenements().stream()
                    .filter(e -> type.equals(e.getType()) && statut.equals(e.getStatut()))
                    .toList();
        } else if (!"Tous".equals(type)) {
            events = evenementService.getEvenementsByType(type);
        } else if (!"Tous".equals(statut)) {
            events = evenementService.getEvenementsByStatut(statut);
        } else {
            events = evenementService.getAllEvenements();
        }

        evenementsList.clear();
        evenementsList.addAll(events);
    }

    @FXML
    private void handleRefresh() {
        updateEventStatuses();
        loadAllEvents();
        updateStatistics();
    }

    @FXML
    private void handleAddEvent() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Evenement/EvenementForm.fxml"));
            Parent root = loader.load();

            EvenementFormController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Créer un Événement");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadAllEvents();
            updateStatistics();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors de l'ouverture du formulaire");
        }
    }

    private void updateEventStatuses() {
        List<Evenement> allEvents = evenementService.getAllEvenements();
        LocalDate today = LocalDate.now();

        for (Evenement event : allEvents) {
            String newStatut = null;

            if ("ANNULE".equals(event.getStatut())) {
                continue;
            }

            if (event.getDateFin().isBefore(today)) {
                if (!"TERMINE".equals(event.getStatut())) {
                    newStatut = "TERMINE";
                }
            } else if (event.getDateDebut().isAfter(today)) {
                if (!"A_VENIR".equals(event.getStatut())) {
                    newStatut = "A_VENIR";
                }
            } else {
                if (!"EN_COURS".equals(event.getStatut())) {
                    newStatut = "EN_COURS";
                }
            }

            if (newStatut != null) {
                event.setStatut(newStatut);
                evenementService.modifierEvenement(event);
            }
        }
    }

    private void editEvent(Evenement evenement) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Evenement/EvenementForm.fxml"));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof EvenementFormController) {
                ((EvenementFormController) controller).setEvenement(evenement);
            }

            Stage stage = new Stage();
            stage.setTitle("Modifier l'Événement");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadAllEvents();
            updateStatistics();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors de l'ouverture du formulaire d'édition");
        }
    }

    private void deleteEvent(Evenement evenement) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer l'événement");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer \"" + evenement.getTitre() + "\" ?\n" +
                "Cette action est irréversible et supprimera également toutes les participations.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (evenementService.supprimerEvenement(evenement.getId())) {
                showSuccess("Événement supprimé avec succès");
                loadAllEvents();
                updateStatistics();
            } else {
                showError("Erreur lors de la suppression de l'événement");
            }
        }
    }

    private void showEventDetails(Evenement evenement) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Evenement/EvenementDetails.fxml"));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof EvenementDetailsController) {
                ((EvenementDetailsController) controller).setEvenement(evenement);
            }

            Stage stage = new Stage();
            stage.setTitle("Détails de l'Événement");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadAllEvents();
            updateStatistics();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors de l'ouverture des détails");
        }
    }

    private void updateStatistics() {
        List<Evenement> allEvents = evenementService.getAllEvenements();

        totalLabel.setText(String.valueOf(allEvents.size()));

        long actifs = allEvents.stream()
                .filter(e -> "A_VENIR".equals(e.getStatut()) || "EN_COURS".equals(e.getStatut()))
                .count();
        actifLabel.setText(String.valueOf(actifs));

        long termines = allEvents.stream()
                .filter(e -> "TERMINE".equals(e.getStatut()))
                .count();
        termineLabel.setText(String.valueOf(termines));

        long annules = allEvents.stream()
                .filter(e -> "ANNULE".equals(e.getStatut()))
                .count();
        annuleLabel.setText(String.valueOf(annules));
    }

    private String getStatutLabel(String statut) {
        return switch (statut) {
            case "A_VENIR" -> "À venir";
            case "EN_COURS" -> "En cours";
            case "TERMINE" -> "Terminé";
            case "ANNULE" -> "Annulé";
            default -> statut;
        };
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleRetour(ActionEvent event) {
        navigateTo(event, "/fxml/Evenement/NavigationEvenements.fxml", "Module Événements");
    }

    private void navigateTo(ActionEvent event, String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, WindowUtils.APP_WIDTH, WindowUtils.APP_HEIGHT));
            stage.setTitle("Ardhi - " + title);
            stage.setResizable(true);
            WindowUtils.applyStandardSize(stage);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur lors du chargement de : " + fxmlFile);
        }
    }

}