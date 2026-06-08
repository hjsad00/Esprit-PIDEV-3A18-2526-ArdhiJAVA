package tn.neuron.ardhi.controllers.Evenement;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.neuron.ardhi.models.Evenement.Evenement;
import tn.neuron.ardhi.services.Evenement.EvenementService;
import tn.neuron.ardhi.services.Evenement.FavorisService;
import tn.neuron.ardhi.services.Evenement.PartageService;
import tn.neuron.ardhi.services.Evenement.ParticipationService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.scene.Node;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;


public class EvenementListController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterTypeCombo;
    @FXML private ComboBox<String> filterStatutCombo;
    @FXML private FlowPane eventsFlowPane;
    @FXML private Button addEventButton;
    @FXML private Label totalEventsLabel;
    @FXML private Label activeEventsLabel;

    private EvenementService evenementService;
    private ParticipationService participationService;
    private String userRole;
    private int userId;
    private FavorisService favorisService;
    private PartageService partageService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        evenementService = new EvenementService();
        participationService = new ParticipationService();
        favorisService = new FavorisService();
        partageService = new PartageService();

        if (UserSession.getInstance() != null && UserSession.getInstance().getUser() != null) {
            userRole = UserSession.getInstance().getUser().getRole().name();
            userId = UserSession.getInstance().getUser().getId();
        } else {
            userRole = "CLIENT";
            userId = 0;
        }

        setupRoleBasedUI();
        initializeFilters();
        loadAllEvents();
        updateStatistics();
        setupListeners();
    }

    private void setupRoleBasedUI() {
        if (!"ADMIN".equals(userRole) && !"AGRICULTEUR".equals(userRole)) {
            addEventButton.setVisible(false);
            addEventButton.setManaged(false);
        }
    }

    private void initializeFilters() {
        filterTypeCombo.getItems().addAll("Tous", "FOIRE", "FORMATION", "CONFERENCE", "ATELIER");
        filterTypeCombo.setValue("Tous");
        filterStatutCombo.getItems().addAll("Tous", "A_VENIR", "EN_COURS", "TERMINE", "ANNULE");
        filterStatutCombo.setValue("Tous");
    }

    private void setupListeners() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) loadAllEvents();
            else searchEvents(newVal);
        });
        filterTypeCombo.setOnAction(e -> applyFilters());
        filterStatutCombo.setOnAction(e -> applyFilters());
    }

    @FXML
    private void handleAddEvent() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Evenement/EvenementForm.fxml"));
            Parent root = loader.load();
            EvenementFormController controller = loader.getController();
            controller.setEvenementListController(this);
            if ("AGRICULTEUR".equals(userRole)) controller.setTypeRestriction("AGRICULTEUR");

            Stage stage = new Stage();
            stage.setTitle("Créer un Événement");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadAllEvents();
            updateStatistics();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors de l'ouverture du formulaire");
        }
    }

    private void loadAllEvents() {
        displayEvents(evenementService.getAllEvenements());
    }

    private void searchEvents(String keyword) {
        displayEvents(evenementService.rechercherEvenements(keyword));
    }

    private void applyFilters() {
        String type = filterTypeCombo.getValue();
        String statut = filterStatutCombo.getValue();
        List<Evenement> events;
        if (!"Tous".equals(type) && !"Tous".equals(statut)) {
            events = evenementService.getAllEvenements().stream()
                    .filter(e -> type.equals(e.getType()) && statut.equals(e.getStatut())).toList();
        } else if (!"Tous".equals(type)) {
            events = evenementService.getEvenementsByType(type);
        } else if (!"Tous".equals(statut)) {
            events = evenementService.getEvenementsByStatut(statut);
        } else {
            events = evenementService.getAllEvenements();
        }
        displayEvents(events);
    }

    private void displayEvents(List<Evenement> events) {
        eventsFlowPane.getChildren().clear();
        if (events.isEmpty()) {
            Label noEventsLabel = new Label("Aucun événement trouvé");
            noEventsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #999;");
            eventsFlowPane.getChildren().add(noEventsLabel);
            return;
        }
        for (Evenement event : events) {
            eventsFlowPane.getChildren().add(createEventCard(event));
        }
    }

    private VBox createEventCard(Evenement event) {
        VBox card = new VBox(10);
        card.getStyleClass().add("event-card");
        card.setPrefWidth(280);
        card.setPrefHeight(420);
        card.setPadding(new Insets(15));

        // Image
        StackPane imagePlaceholder = new StackPane();
        imagePlaceholder.setStyle("-fx-background-color: #e0e0d0; -fx-background-radius: 8;");
        imagePlaceholder.setPrefHeight(150);

        if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            try {
                // The DB stores a web-relative path like "/uploads/evenements/file.jpg".
                // Resolve it against the Symfony public uploads folder so both the
                // desktop app and the web app read from the same physical directory.
                String fileName = event.getImageUrl()
                        .replace("/uploads/evenements/", "")
                        .replace("\\uploads\\evenements\\", "");
                File imageFile = new File(
                        tn.neuron.ardhi.services.Evenement.UnsplashImageService.SYMFONY_PUBLIC_PATH
                        + "\\" + fileName);
                if (imageFile.exists()) {
                    ImageView imageView = new ImageView(new Image(imageFile.toURI().toString()));
                    imageView.setFitWidth(250); imageView.setFitHeight(150);
                    imageView.setPreserveRatio(true); imageView.setSmooth(true);
                    imagePlaceholder.getChildren().add(imageView);
                } else {
                    imagePlaceholder.getChildren().add(makePhotoLabel());
                }
            } catch (Exception e) {
                imagePlaceholder.getChildren().add(makePhotoLabel());
            }
        } else {
            imagePlaceholder.getChildren().add(makePhotoLabel());
        }

        // Badges
        Label typeBadge = new Label(event.getType());
        typeBadge.getStyleClass().addAll("badge", "badge-" + event.getType().toLowerCase());
        Label statutBadge = new Label(getStatutLabel(event.getStatut()));
        statutBadge.getStyleClass().addAll("badge", "badge-" + event.getStatut().toLowerCase().replace("_", ""));
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.getChildren().addAll(typeBadge, statutBadge);

        Label titleLabel = new Label(event.getTitre());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");
        titleLabel.setWrapText(true); titleLabel.setMaxWidth(250);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String dateStr = event.getDateDebut().format(formatter);
        if (!event.getDateDebut().equals(event.getDateFin()))
            dateStr += " - " + event.getDateFin().format(formatter);
        Label dateLabel = new Label("📅 " + dateStr);
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        Label lieuLabel = new Label("📍 " + event.getLieu());
        lieuLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        Label placesLabel = new Label("👥 " + event.getNombreParticipants() + "/" + event.getNombrePlacesMax() + " participants");
        placesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        // ── Social buttons ──────────────────────────────────────────────
        HBox socialButtons = new HBox(8);
        socialButtons.setAlignment(Pos.CENTER);
        socialButtons.setPadding(new Insets(8, 0, 8, 0));

        boolean isFavoris = favorisService.estFavori(event.getId(), userId);

        // FAVORIS — toujours fond rouge, cœur plein ♥ ou creux ♡ en blanc
        Button favorisButton = new Button(isFavoris ? "♥" : "♡");
        favorisButton.setStyle(buildFavorisStyle(isFavoris));
        favorisButton.setTooltip(new Tooltip(isFavoris ? "Retirer des favoris" : "Ajouter aux favoris"));
        favorisButton.setOnAction(e -> {
            favorisService.toggleFavori(event.getId(), userId);
            loadAllEvents();
        });

        // PARTAGE
        Button shareButton = new Button("✉");
        shareButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #95A5A6, #7F8C8D);" +
                        "-fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;" +
                        "-fx-padding: 10 15; -fx-font-size: 16px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(149,165,166,0.4), 5, 0, 0, 2);");
        shareButton.setTooltip(new Tooltip("Partager par email"));
        shareButton.setOnAction(e -> partageService.partagerParEmail(event, null));

        // COMPTEUR
        int nbFavoris = favorisService.getNombreFavoris(event.getId());
        Label favorisCountLabel = new Label(nbFavoris + " ♥");
        favorisCountLabel.setStyle(
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #FF6B6B;" +
                        "-fx-padding: 8 15; -fx-background-color: rgba(255,107,107,0.1);" +
                        "-fx-background-radius: 20; -fx-border-color: rgba(255,107,107,0.2);" +
                        "-fx-border-width: 1.5; -fx-border-radius: 20;");

        socialButtons.getChildren().addAll(favorisButton, shareButton, favorisCountLabel);

        // ── Action buttons ──────────────────────────────────────────────
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER);
        Button detailsButton = new Button("Détails");
        detailsButton.getStyleClass().add("button-small");
        detailsButton.setOnAction(e -> showEventDetails(event));
        actionButtons.getChildren().add(detailsButton);

        if ("ADMIN".equals(userRole) || event.getIdCreateur() == userId) {
            Button editButton = new Button("Modifier");
            editButton.getStyleClass().addAll("button-small", "button-secondary");
            editButton.setOnAction(e -> editEvent(event));
            Button deleteButton = new Button("Supprimer");
            deleteButton.getStyleClass().addAll("button-small", "button-danger");
            deleteButton.setOnAction(e -> deleteEvent(event));
            actionButtons.getChildren().addAll(editButton, deleteButton);
        }

        card.getChildren().addAll(imagePlaceholder, topRow, titleLabel, dateLabel,
                lieuLabel, placesLabel, socialButtons, actionButtons);

        card.setOnMouseClicked(e -> { if (e.getClickCount() == 2) showEventDetails(event); });
        return card;
    }

    /** Style uniforme du bouton favoris : fond rouge, texte blanc, ♥ ou ♡ */
    private String buildFavorisStyle(boolean active) {
        return "-fx-background-color: " + (active
                ? "linear-gradient(to bottom, #E74C3C, #C0392B)"
                : "linear-gradient(to bottom, #F1948A, #E74C3C)") + ";" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 10;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 10 15;" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-effect: dropshadow(gaussian, rgba(231,76,60,0.45), 5, 0, 0, 2);";
    }

    private Label makePhotoLabel() {
        Label l = new Label("📷");
        l.setStyle("-fx-font-size: 40px;");
        return l;
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

    private void showEventDetails(Evenement event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Evenement/EvenementDetails.fxml"));
            Parent root = loader.load();
            EvenementDetailsController controller = loader.getController();
            controller.setEvenement(event);
            controller.setEvenementListController(this);
            Stage stage = new Stage();
            stage.setTitle("Détails de l'Événement");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadAllEvents(); updateStatistics();
        } catch (IOException e) { e.printStackTrace(); showError("Erreur lors de l'ouverture des détails"); }
    }

    private void editEvent(Evenement event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Evenement/EvenementForm.fxml"));
            Parent root = loader.load();
            EvenementFormController controller = loader.getController();
            controller.setEvenement(event);
            controller.setEvenementListController(this);
            Stage stage = new Stage();
            stage.setTitle("Modifier l'Événement");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadAllEvents(); updateStatistics();
        } catch (IOException e) { e.printStackTrace(); showError("Erreur lors de l'ouverture du formulaire"); }
    }

    private void deleteEvent(Evenement event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer l'événement");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cet événement ? Cette action est irréversible.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (evenementService.supprimerEvenement(event.getId())) {
                showSuccess("Événement supprimé avec succès");
                loadAllEvents(); updateStatistics();
            } else {
                showError("Erreur lors de la suppression de l'événement");
            }
        }
    }

    private void updateStatistics() {
        totalEventsLabel.setText(String.valueOf(evenementService.getNombreTotalEvenements()));
        activeEventsLabel.setText(String.valueOf(evenementService.getNombreEvenementsActifs()));
    }

    public void refreshList() { loadAllEvents(); updateStatistics(); }

    private void showError(String m)   { Alert a = new Alert(Alert.AlertType.ERROR);       a.setTitle("Erreur");       a.setHeaderText(null); a.setContentText(m); a.showAndWait(); }
    private void showSuccess(String m) { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle("Succès");       a.setHeaderText(null); a.setContentText(m); a.showAndWait(); }
    private void showInfo(String m)    { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle("Information");  a.setHeaderText(null); a.setContentText(m); a.showAndWait(); }

    @FXML
    private void handleRetour(javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Evenement/NavigationEvenements.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, WindowUtils.APP_WIDTH, WindowUtils.APP_HEIGHT));
            stage.setTitle("Ardhi - Module Événements");
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }
}