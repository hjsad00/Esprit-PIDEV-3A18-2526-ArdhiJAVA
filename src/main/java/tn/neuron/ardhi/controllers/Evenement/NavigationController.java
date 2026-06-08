package tn.neuron.ardhi.controllers.Evenement;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.neuron.ardhi.models.UserAndDiag.Role;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class NavigationController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;

    // Cartes AGRICULTEUR/ADMIN
    @FXML private HBox evenementsCard;
    @FXML private Label btnEvenements;
    @FXML private HBox mesEvenementsCard;
    @FXML private Label btnMesEvenements;
    @FXML private HBox statistiquesCard;
    @FXML private Label btnStatistiques;
    @FXML private HBox adminCard;
    @FXML private Label btnAdminEvenements;
    @FXML private HBox gestionParticipationsCard;
    @FXML private Label labelGestionTitre;
    @FXML private Label labelGestionDesc;

    // Calendrier & QR
    @FXML private HBox calendrierCard;
    @FXML private HBox qrCheckinCard;

    // Cartes CLIENT
    @FXML private HBox evenementsCardClient;
    @FXML private HBox mesEvenementsCardClient;
    @FXML private HBox mesFavorisCard;

    // Carousel (image de fond pour CLIENT)
    @FXML private VBox carouselContainer;
    @FXML private ImageView carouselImage;

    private Role userRole;
    private int currentSlideIndex = 0;
    private final String[] slideImages = {
            "/img/slide1.png",
            "/img/slide2.png",
            "/img/slide3.png",
            "/img/slide4.png"
    };

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (UserSession.getInstance() != null && UserSession.getInstance().getUser() != null) {
            userRole = UserSession.getInstance().getUser().getRole();
            String nom = UserSession.getInstance().getUser().getPrenom() + " " +
                    UserSession.getInstance().getUser().getNom();
            welcomeLabel.setText("Bienvenue, " + nom);

            updateRoleLabel();
            setupNavigationCards();

            if (userRole == Role.CLIENT && carouselImage != null) {
                startCarousel();
            }
        }
    }

    // ── Carousel ─────────────────────────────────────────────────────────
    private void startCarousel() {
        loadSlide(currentSlideIndex);

        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.seconds(3), e -> {
                    currentSlideIndex = (currentSlideIndex + 1) % slideImages.length;
                    loadSlideWithTransition(currentSlideIndex);
                })
        );
        timeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        timeline.play();
    }

    private void loadSlide(int index) {
        try {
            Image image = new Image(getClass().getResourceAsStream(slideImages[index]));
            carouselImage.setImage(image);
        } catch (Exception e) {
            System.err.println("Erreur chargement slide: " + slideImages[index]);
        }
    }

    private void loadSlideWithTransition(int index) {
        javafx.animation.FadeTransition fadeOut =
                new javafx.animation.FadeTransition(Duration.millis(500), carouselImage);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            loadSlide(index);
            javafx.animation.FadeTransition fadeIn =
                    new javafx.animation.FadeTransition(Duration.millis(500), carouselImage);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    @FXML private void handlePreviousSlide() {
        currentSlideIndex = (currentSlideIndex - 1 + slideImages.length) % slideImages.length;
        loadSlideWithTransition(currentSlideIndex);
    }

    @FXML private void handleNextSlide() {
        currentSlideIndex = (currentSlideIndex + 1) % slideImages.length;
        loadSlideWithTransition(currentSlideIndex);
    }

    // ── Badge rôle ────────────────────────────────────────────────────────
    private void updateRoleLabel() {
        if (roleLabel == null || userRole == null) return;

        String text, color;
        switch (userRole) {
            case ADMIN      -> { text = "🔧 Espace Admin";        color = "rgba(231, 76, 60, 0.9)"; }
            case AGRICULTEUR-> { text = "🌾 Espace Agriculteur";  color = "rgba(107, 127, 63, 0.9)"; }
            default         -> { text = "👤 Espace Client";       color = "rgba(52, 152, 219, 0.9)"; }
        }

        roleLabel.setText(text);
        roleLabel.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-text-fill: white; -fx-padding: 8 20;" +
                        "-fx-background-radius: 20; -fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-border-color: rgba(255,255,255,0.3); -fx-border-width: 1.5; -fx-border-radius: 20;" +
                        "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);");

        if (userRole == Role.ADMIN) {
            roleLabel.setOnMouseClicked(this::goToAdminDashboard);
        }
    }

    // ── Affichage des cartes selon le rôle ────────────────────────────────
    private void setupNavigationCards() {
        hideCard(evenementsCard);
        hideCard(mesEvenementsCard);
        hideCard(statistiquesCard);
        hideCard(adminCard);
        hideCard(gestionParticipationsCard);
        hideCard(evenementsCardClient);
        hideCard(mesEvenementsCardClient);
        hideCard(mesFavorisCard);
        hideCard(carouselContainer);
        hideCard(calendrierCard);
        hideCard(qrCheckinCard);

        switch (userRole) {
            case ADMIN -> {
                showCard(statistiquesCard);
                showCard(adminCard);
                btnAdminEvenements.setText("Dashboard Admin");
                showCard(gestionParticipationsCard);
                setCardText(labelGestionTitre, "Toutes Participations");
                setCardText(labelGestionDesc, "Gérez toutes les participations");
                showCard(calendrierCard);
                showCard(qrCheckinCard);
            }
            case AGRICULTEUR -> {
                showCard(evenementsCard);
                btnEvenements.setText("Tous les Événements");
                showCard(mesEvenementsCard);
                btnMesEvenements.setText("Mes Inscriptions");
                showCard(gestionParticipationsCard);
                setCardText(labelGestionTitre, "Mes Participations");
                setCardText(labelGestionDesc, "Gérez vos événements créés");
                showCard(calendrierCard);
                showCard(qrCheckinCard);
                showCard(mesFavorisCard);
            }
            case CLIENT -> {
                showCard(evenementsCardClient);
                showCard(mesEvenementsCardClient);
                showCard(mesFavorisCard);
                showCard(calendrierCard);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private void hideCard(javafx.scene.layout.Region card) {
        if (card != null) { card.setVisible(false); card.setManaged(false); }
    }
    private void showCard(javafx.scene.layout.Region card) {
        if (card != null) { card.setVisible(true);  card.setManaged(true);  }
    }
    private void setCardText(Label label, String text) {
        if (label != null) label.setText(text);
    }

    // ── Vérification accès CLIENT ─────────────────────────────────────────
    /**
     * Vérifie si l'utilisateur CLIENT tente d'accéder à une page restreinte.
     * @return true si l'accès est BLOQUÉ (client), false si accès autorisé
     */
    private boolean isClientRestricted() {
        if (userRole == Role.CLIENT) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Accès restreint");
            alert.setHeaderText("⛔ Accès non autorisé");
            alert.setContentText("Ce module est réservé aux Agriculteurs et Administrateurs.\n\nVeuillez vous connecter avec un compte approprié.");
            alert.showAndWait();
            return true;
        }
        return false;
    }

    // ── Handlers navigation (TOUS LES MODULES) ────────────────────────────
    @FXML private void goToLandingPage(MouseEvent event)    { navigateTo(event, "/fxml/LandingPage.fxml", "Ardhi - Accueil"); }
    @FXML private void goToProfil(MouseEvent event)         { navigateTo(event, "/fxml/UserAndDiag/Profil.fxml", "Mon Profil"); }
    @FXML private void goToDiagnostics(ActionEvent event)   { navigateTo(event, "/fxml/UserAndDiag/ClientDashboard.fxml", "Dashboard"); }
    @FXML private void goToAdminDashboard(MouseEvent event) { navigateTo(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Administration"); }

    // ══ Navigations vers autres modules — RESTREINTS POUR CLIENT ══
    @FXML
    private void goToEmployes(ActionEvent event) {
        if (isClientRestricted()) return;
        navigateTo(event, "/fxml/gestionemploye/Dashboard_employe_agriculture.fxml", "Gestion Employés");
    }

    @FXML
    private void goToMarketplace(ActionEvent event) {
        // Marketplace accessible à tous
        navigateTo(event, "/fxml/marketplace/Marketplace.fxml", "Marketplace");
    }

    @FXML
    private void goToMaintenance(ActionEvent event) {
        if (isClientRestricted()) return;
        navigateTo(event, "/fxml/MaterielEtMaintenance/MaterielsEtMaintenance.fxml", "Maintenance");
    }

    @FXML
    private void goToCultures(ActionEvent event) {
        navigateTo(event, "/fxml/Parcelle_Cultures/AgriculteurDashboard.fxml", "Cultures");
    }

    @FXML private void goToClientDashboard(ActionEvent event) { navigateTo(event, "/fxml/UserAndDiag/ClientDashboard.fxml", "Dashboard Client"); }

    // ══ Navigation événements ══
    @FXML private void handleEvenementsClick(MouseEvent event)          { navigateTo(event, "/fxml/Evenement/EvenementList.fxml", "Événements"); }
    @FXML private void handleMesEvenementsClick(MouseEvent event)       { navigateTo(event, "/fxml/Evenement/MesEvenements.fxml", "Mes Événements"); }
    @FXML private void handleGestionParticipationsClick(MouseEvent event){ navigateTo(event, "/fxml/Evenement/ParticipationManagement.fxml", "Gestion des Participations"); }
    @FXML private void handleStatistiquesClick(MouseEvent event)        { navigateTo(event, "/fxml/Evenement/Statistiques.fxml", "Statistiques"); }
    @FXML private void handleAdminEvenementsClick(MouseEvent event)     { navigateTo(event, "/fxml/Evenement/AdminEvenementDashboard.fxml", "Dashboard Admin"); }
    @FXML private void handleCalendrierClick(MouseEvent event)          { navigateTo(event, "/fxml/Evenement/CalendrierView.fxml", "Calendrier des Événements"); }
    @FXML private void handleMesFavoris(MouseEvent event)               { navigateTo(event, "/fxml/Evenement/MesFavoris.fxml", "Mes Favoris"); }
    @FXML private void handleQRCheckinClick(MouseEvent event)           { navigateTo(event, "/fxml/Evenement/QRCheckIn.fxml", "QR Code Check-in"); }

    // ActionEvent variants
    @FXML private void handleEvenements(ActionEvent event)          { navigateTo(event, "/fxml/Evenement/EvenementList.fxml", "Événements"); }
    @FXML private void handleMesEvenements(ActionEvent event)       { navigateTo(event, "/fxml/Evenement/MesEvenements.fxml", "Mes Événements"); }
    @FXML private void handleGestionParticipations(ActionEvent event){ navigateTo(event, "/fxml/Evenement/ParticipationManagement.fxml", "Gestion des Participations"); }
    @FXML private void handleStatistiques(ActionEvent event)        { navigateTo(event, "/fxml/Evenement/Statistiques.fxml", "Statistiques"); }
    @FXML private void handleAdminEvenements(ActionEvent event)     { navigateTo(event, "/fxml/Evenement/AdminEvenementDashboard.fxml", "Dashboard Admin"); }
    @FXML private void handleRetour(ActionEvent event)              { navigateTo(event, "/fxml/LandingPage.fxml", "Ardhi - Dashboard"); }
    @FXML private void handleDeconnexion(ActionEvent event) {
        UserSession.getInstance().cleanUserSession();
        navigateTo(event, "/fxml/UserAndDiag/Login.fxml", "Connexion");
    }

    // ── Utilitaire de navigation ──────────────────────────────────────────
    private void navigateTo(javafx.event.Event event, String fxmlFile, String title) {
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