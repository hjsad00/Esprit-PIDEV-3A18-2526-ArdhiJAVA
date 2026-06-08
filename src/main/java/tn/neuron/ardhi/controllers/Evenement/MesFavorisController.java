package tn.neuron.ardhi.controllers.Evenement;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.neuron.ardhi.models.Evenement.Evenement;
import tn.neuron.ardhi.services.Evenement.EvenementService;
import tn.neuron.ardhi.services.Evenement.FavorisService;
import tn.neuron.ardhi.services.Evenement.PartageService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import javafx.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Contrôleur pour afficher les événements favoris de l'utilisateur
 */
public class MesFavorisController implements Initializable {

    @FXML private FlowPane favorisFlowPane;
    @FXML private Label totalFavorisLabel;
    @FXML private Label evenementsAVenirLabel;
    @FXML private Label emptyStateLabel;
    @FXML private VBox emptyStateBox;

    // NOUVEAUX CHAMPS FXML
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterTypeCombo;
    @FXML private ComboBox<String> filterStatutCombo;

    private FavorisService favorisService;
    private EvenementService evenementService;
    private PartageService partageService;
    private int userId;

    // Liste complète des favoris (pour filtrage)
    private List<Evenement> allFavoris;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        favorisService = new FavorisService();
        evenementService = new EvenementService();
        partageService = new PartageService();

        // Récupérer l'utilisateur connecté
        if (UserSession.getInstance() != null && UserSession.getInstance().getUser() != null) {
            userId = UserSession.getInstance().getUser().getId();
        } else {
            userId = 0;
        }

        // Initialiser les filtres
        initializeFilters();

        // Configurer les listeners
        setupListeners();

        // Charger les favoris
        loadFavoris();
    }

    /**
     * Initialise les ComboBox de filtrage
     */
    private void initializeFilters() {
        // Types
        filterTypeCombo.getItems().addAll("Tous", "FOIRE", "FORMATION", "CONFERENCE", "ATELIER");
        filterTypeCombo.setValue("Tous");

        // Statuts
        filterStatutCombo.getItems().addAll("Tous", "A_VENIR", "EN_COURS", "TERMINE", "ANNULE");
        filterStatutCombo.setValue("Tous");
    }

    /**
     * Configure les listeners pour recherche et filtres
     */
    private void setupListeners() {
        // Recherche en temps réel
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                applyFilters();
            });
        }

        // Filtres
        if (filterTypeCombo != null) {
            filterTypeCombo.setOnAction(e -> applyFilters());
        }

        if (filterStatutCombo != null) {
            filterStatutCombo.setOnAction(e -> applyFilters());
        }
    }

    /**
     * Charge les favoris de l'utilisateur
     */
    private void loadFavoris() {
        allFavoris = favorisService.getFavorisUtilisateur(userId);
        applyFilters();
        updateStatistics();
    }

    /**
     * Applique les filtres de recherche et ComboBox
     */
    private void applyFilters() {
        if (allFavoris == null) return;

        List<Evenement> filteredList = allFavoris;

        // Filtre de recherche
        String searchText = searchField != null ? searchField.getText().toLowerCase().trim() : "";
        if (!searchText.isEmpty()) {
            filteredList = filteredList.stream()
                    .filter(e ->
                            e.getTitre().toLowerCase().contains(searchText) ||
                                    e.getLieu().toLowerCase().contains(searchText) ||
                                    e.getOrganisateur().toLowerCase().contains(searchText)
                    )
                    .collect(Collectors.toList());
        }

        // Filtre par type
        String typeFilter = filterTypeCombo != null ? filterTypeCombo.getValue() : "Tous";
        if (typeFilter != null && !"Tous".equals(typeFilter)) {
            filteredList = filteredList.stream()
                    .filter(e -> typeFilter.equals(e.getType()))
                    .collect(Collectors.toList());
        }

        // Filtre par statut
        String statutFilter = filterStatutCombo != null ? filterStatutCombo.getValue() : "Tous";
        if (statutFilter != null && !"Tous".equals(statutFilter)) {
            filteredList = filteredList.stream()
                    .filter(e -> statutFilter.equals(e.getStatut()))
                    .collect(Collectors.toList());
        }

        displayFavoris(filteredList);
    }

    /**
     * Affiche la liste filtrée des favoris
     */
    private void displayFavoris(List<Evenement> favoris) {
        favorisFlowPane.getChildren().clear();

        if (favoris.isEmpty()) {
            showEmptyState();
            return;
        }

        // Masquer l'état vide
        if (emptyStateBox != null) {
            emptyStateBox.setVisible(false);
            emptyStateBox.setManaged(false);
        }

        // Afficher les cartes
        for (Evenement event : favoris) {
            VBox eventCard = createFavoriCard(event);
            favorisFlowPane.getChildren().add(eventCard);
        }
    }

    /**
     * Met à jour les statistiques
     */
    private void updateStatistics() {
        int total = allFavoris != null ? allFavoris.size() : 0;
        totalFavorisLabel.setText(String.valueOf(total));

        // Événements à venir
        int aVenir = allFavoris != null ? (int) allFavoris.stream()
                .filter(e -> "A_VENIR".equals(e.getStatut()) &&
                        e.getDateDebut().isAfter(LocalDate.now()))
                .count() : 0;

        if (evenementsAVenirLabel != null) {
            evenementsAVenirLabel.setText(String.valueOf(aVenir));
        }
    }

    /**
     * Crée une carte pour un événement favori
     */
    private VBox createFavoriCard(Evenement event) {
        VBox card = new VBox(10);
        card.getStyleClass().add("event-card");
        card.setPrefWidth(280);
        card.setPrefHeight(380);
        card.setPadding(new Insets(15));
        card.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-color: #e0e0d0; " +
                        "-fx-border-radius: 15; " +
                        "-fx-border-width: 2; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"
        );

        // Image
        HBox imagePlaceholder = new HBox();
        imagePlaceholder.setStyle("-fx-background-color: #e0e0d0; -fx-background-radius: 8;");
        imagePlaceholder.setPrefHeight(150);
        imagePlaceholder.setAlignment(Pos.CENTER);

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
                    imageView.setFitWidth(250);
                    imageView.setFitHeight(150);
                    imageView.setPreserveRatio(true);
                    imageView.setSmooth(true);
                    imagePlaceholder.getChildren().add(imageView);
                } else {
                    Label imageLabel = new Label("📷");
                    imageLabel.setStyle("-fx-font-size: 40px;");
                    imagePlaceholder.getChildren().add(imageLabel);
                }
            } catch (Exception e) {
                Label imageLabel = new Label("📷");
                imageLabel.setStyle("-fx-font-size: 40px;");
                imagePlaceholder.getChildren().add(imageLabel);
            }
        } else {
            Label imageLabel = new Label("📷");
            imageLabel.setStyle("-fx-font-size: 40px;");
            imagePlaceholder.getChildren().add(imageLabel);
        }

        // Badges
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label typeBadge = new Label(event.getType());
        typeBadge.setStyle(
                "-fx-background-color: #3498DB; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 5 15; " +
                        "-fx-background-radius: 15; " +
                        "-fx-font-size: 12px;"
        );

        Label statutBadge = new Label(getStatutLabel(event.getStatut()));
        String statutColor = getStatutColor(event.getStatut());
        statutBadge.setStyle(
                "-fx-background-color: " + statutColor + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 5 15; " +
                        "-fx-background-radius: 15; " +
                        "-fx-font-size: 12px;"
        );

        topRow.getChildren().addAll(typeBadge, statutBadge);

        // Titre
        Label titleLabel = new Label(event.getTitre());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(250);

        // Date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String dateStr = event.getDateDebut().format(formatter);
        if (!event.getDateDebut().equals(event.getDateFin())) {
            dateStr += " - " + event.getDateFin().format(formatter);
        }
        Label dateLabel = new Label("📅 " + dateStr);
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        // Lieu
        Label lieuLabel = new Label("📍 " + event.getLieu());
        lieuLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        // ═══════════════════════════════════════════════════════════════
        // BOUTONS JOLIS (Version simple et fonctionnelle)
        // ═══════════════════════════════════════════════════════════════
        HBox actionButtons = new HBox(8);
        actionButtons.setAlignment(Pos.CENTER);
        actionButtons.setPadding(new Insets(5, 0, 5, 0));

        // Bouton Détails
        Button detailsButton = new Button("📋");
        detailsButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #3498DB, #2980B9); " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 10; " +
                        "-fx-cursor: hand; " +
                        "-fx-padding: 10 15; " +
                        "-fx-font-size: 16px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(52,152,219,0.4), 5, 0, 0, 2);"
        );
        detailsButton.setTooltip(new Tooltip("Voir les détails"));
        detailsButton.setOnAction(e -> showEventDetails(event));

        // Bouton Retirer (Coeur brisé)
        Button removeButton = new Button("💔");
        removeButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #E74C3C, #C0392B); " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 10; " +
                        "-fx-cursor: hand; " +
                        "-fx-padding: 10 15; " +
                        "-fx-font-size: 16px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(231,76,60,0.4), 5, 0, 0, 2);"
        );
        removeButton.setTooltip(new Tooltip("Retirer des favoris"));
        removeButton.setOnAction(e -> {
            if (favorisService.retirerFavori(event.getId(), userId)) {
                showSuccess("Retiré des favoris");
                loadFavoris(); // Refresh
            }
        });

        // Bouton Partager (Info uniquement)
        Button shareButton = new Button("📧");
        shareButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #95A5A6, #7F8C8D); " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 10; " +
                        "-fx-cursor: hand; " +
                        "-fx-padding: 10 15; " +
                        "-fx-font-size: 16px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(149,165,166,0.4), 5, 0, 0, 2);"
        );
        shareButton.setTooltip(new Tooltip("Partager par email"));
        shareButton.setOnAction(e -> {
            // Ouvrir email avec infos de l'événement
            if (partageService.partagerParEmail(event, null)) {
                showSuccess("Client email ouvert !");
            } else {
                showError("Impossible d'ouvrir le client email");
            }
        });

        actionButtons.getChildren().addAll(detailsButton, removeButton, shareButton);

        // Ajouter tous les éléments
        card.getChildren().addAll(
                imagePlaceholder,
                topRow,
                titleLabel,
                dateLabel,
                lieuLabel,
                actionButtons
        );

        return card;
    }

    @FXML
    private void handleDecouvrirEvenements(ActionEvent event) {
        goToEvenementsList();
    }

    /**
     * Affiche l'état vide (aucun favori)
     */
    private void showEmptyState() {
        if (emptyStateBox != null) {
            emptyStateBox.setVisible(true);
            emptyStateBox.setManaged(true);
        }
    }

    /**
     * Affiche les détails d'un événement
     */
    private void showEventDetails(Evenement event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Evenement/EvenementDetails.fxml"));
            Parent root = loader.load();

            EvenementDetailsController controller = loader.getController();
            controller.setEvenement(event);

            Stage stage = new Stage();
            stage.setTitle("Détails de l'Événement");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // Refresh après fermeture
            loadFavoris();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors de l'ouverture des détails");
        }
    }

    /**
     * Navigation vers la liste des événements
     */
    private void goToEvenementsList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Evenement/EvenementList.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) favorisFlowPane.getScene().getWindow();
            stage.setScene(new Scene(root, WindowUtils.APP_WIDTH, WindowUtils.APP_HEIGHT));
            stage.setTitle("Ardhi - Événements");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh() {
        loadFavoris();
    }

    @FXML
    private void handleRetour(javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Evenement/NavigationEvenements.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, WindowUtils.APP_WIDTH, WindowUtils.APP_HEIGHT));
            stage.setTitle("Ardhi - Module Événements");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Méthodes utilitaires
    private String getStatutLabel(String statut) {
        return switch (statut) {
            case "A_VENIR" -> "À venir";
            case "EN_COURS" -> "En cours";
            case "TERMINE" -> "Terminé";
            case "ANNULE" -> "Annulé";
            default -> statut;
        };
    }

    private String getStatutColor(String statut) {
        return switch (statut) {
            case "A_VENIR" -> "#50C878";
            case "EN_COURS" -> "#F39C12";
            case "TERMINE" -> "#95A5A6";
            case "ANNULE" -> "#E74C3C";
            default -> "#3498DB";
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
}