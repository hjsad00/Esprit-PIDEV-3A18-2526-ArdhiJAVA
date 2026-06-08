package tn.neuron.ardhi.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import tn.neuron.ardhi.controllers.UserAndDiag.LoginController;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;
import tn.neuron.ardhi.models.UserAndDiag.Role;
public class LandingPageController {

    @FXML
    private Button btnLogin;

    @FXML
    private Button btnCommencer;

    @FXML
    private HBox profileContainer;

    @FXML
    private HBox navMenu;

    @FXML
    private Button btnNavDiagnostics;

    @FXML
    private Button btnNavMarketplace;

    @FXML
    private Button btnNavMaintenance;

    @FXML
    private Button btnNavCultures;

    @FXML
    private Button btnNavEvents;

    @FXML
    private Button btnNavEmployees;

    @FXML
    private javafx.scene.layout.AnchorPane heroBackground;

    @FXML
    private javafx.scene.layout.AnchorPane heroBackgroundOverlay;

    @FXML
    private HBox paginationContainer;

    private final java.util.List<javafx.scene.layout.Background> slideshowBackgrounds = new java.util.ArrayList<>();
    private int currentImageIndex = 0;
    private javafx.animation.Timeline slideshowTimeline;

    @FXML
    private javafx.scene.control.ScrollPane servicesScrollPane;

    @FXML
    public void initialize() {
        updateHeaderBasedOnSession();
        startSlideshow();
        setupPagination();

        // Prevent nested scrolling from bubbling up to the main page
        if (servicesScrollPane != null) {
            servicesScrollPane.setOnScroll(event -> {
                event.consume();
            });
        }
    }

    private void startSlideshow() {
        // Load images individually to prevent one failure from stopping the whole show
        loadImageQuietly("/img/hero_1.jpg");
        loadImageQuietly("/img/hero_2.jpg");
        loadImageQuietly("/img/hero_3.jpg");
        loadImageQuietly("/img/hero_4.jpg");
        loadImageQuietly("/img/hero_5.jpg");

        if (slideshowBackgrounds.isEmpty()) {
            System.err.println("No slideshow images loaded. Slideshow will not start.");
            return;
        }

        // Set initial image
        heroBackground.setBackground(slideshowBackgrounds.get(0));

        // Setup Timeline for automatic transition (5 seconds)
        slideshowTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(5), event -> {
                    nextSlide(null); // Auto-advance
                }));
        slideshowTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        slideshowTimeline.play();
    }

    private void resetSlideshowTimer() {
        if (slideshowTimeline != null) {
            slideshowTimeline.stop();
            slideshowTimeline.play();
        }
    }

    private void loadImageQuietly(String path) {
        try {
            java.net.URL resource = getClass().getResource(path);
            if (resource != null) {
                // Optimized image loading: 1600px width (plenty for 1060px window),
                // preserve ratio, smooth scaling, and background loading.
                javafx.scene.image.Image image = new javafx.scene.image.Image(
                        resource.toExternalForm(), 1600, 0, true, true, true);

                javafx.scene.layout.BackgroundImage bgImg = new javafx.scene.layout.BackgroundImage(
                        image,
                        javafx.scene.layout.BackgroundRepeat.NO_REPEAT,
                        javafx.scene.layout.BackgroundRepeat.NO_REPEAT,
                        javafx.scene.layout.BackgroundPosition.CENTER,
                        new javafx.scene.layout.BackgroundSize(
                                javafx.scene.layout.BackgroundSize.AUTO,
                                javafx.scene.layout.BackgroundSize.AUTO,
                                false, false, true, true // cover = true, true
                        ));
                slideshowBackgrounds.add(new javafx.scene.layout.Background(bgImg));
            } else {
                System.err.println("Warning: Slideshow image not found: " + path);
            }
        } catch (Exception e) {
            System.err.println("Error loading slideshow image " + path + ": " + e.getMessage());
        }
    }

    @FXML
    void nextSlide(ActionEvent event) {
        if (slideshowBackgrounds.isEmpty())
            return;

        // If event is not null, it's a manual click -> reset timer
        if (event != null) {
            resetSlideshowTimer();
        }

        currentImageIndex = (currentImageIndex + 1) % slideshowBackgrounds.size();
        transitionToImage(currentImageIndex);
    }

    @FXML
    void prevSlide(ActionEvent event) {
        if (slideshowBackgrounds.isEmpty())
            return;

        // Manual click -> reset timer
        resetSlideshowTimer();

        currentImageIndex = (currentImageIndex - 1 + slideshowBackgrounds.size()) % slideshowBackgrounds.size();
        transitionToImage(currentImageIndex);
    }

    @FXML
    private javafx.scene.control.Label heroDescription;

    private final String[] slideDescriptions = {
            "Analysez vos cultures avec notre IA avancée pour détecter précocement les maladies et optimiser vos traitements.",
            "Accédez à un marché vaste pour acheter et vendre vos produits agricoles et équipements en toute sécurité.",
            "Gérez efficacement la maintenance de votre matériel agricole pour éviter les pannes coûteuses.",
            "Suivez l'état de vos parcelles en temps réel et optimisez vos cycles de production.",
            "Restez informé des événements agricoles majeurs et gérez vos équipes avec simplicité."
    };

    private void transitionToImage(int index) {
        javafx.scene.layout.Background nextBackground = slideshowBackgrounds.get(index);

        // Update Text
        if (heroDescription != null && index >= 0 && index < slideDescriptions.length) {
            heroDescription.setText(slideDescriptions[index]);
        }

        // Prepare overlay with next image, initially invisible
        heroBackgroundOverlay.setBackground(nextBackground);
        heroBackgroundOverlay.setOpacity(0.0);

        // Cross-fade: Fade in the overlay
        updatePagination(); // Update dots
        javafx.animation.FadeTransition fadeInOverlay = new javafx.animation.FadeTransition(
                javafx.util.Duration.millis(500), heroBackgroundOverlay); // Faster transition for clicks
        fadeInOverlay.setFromValue(0.0);
        fadeInOverlay.setToValue(1.0);
        fadeInOverlay.setOnFinished(e -> {
            // Once faded in, swap the background and reset overlay
            heroBackground.setBackground(nextBackground);
            heroBackgroundOverlay.setOpacity(0.0);
        });
        fadeInOverlay.play();
    }

    private void updateHeaderBasedOnSession() {
        UserSession session = UserSession.getInstance();
        boolean isLoggedIn = (session != null && session.getUser() != null);

        if (isLoggedIn) {
            // User is logged in
            if (btnLogin != null) {
                btnLogin.setVisible(false);
                btnLogin.setManaged(false);
            }

            if (profileContainer != null) {
                profileContainer.setVisible(true);
                profileContainer.setManaged(true);
            }

            // Hide "Commencer" button if logged in
            if (btnCommencer != null) {
                btnCommencer.setVisible(false);
                btnCommencer.setManaged(false);
            }

        } else {
            // User is guest
            if (btnLogin != null) {
                btnLogin.setVisible(true);
                btnLogin.setManaged(true);
            }

            if (profileContainer != null) {
                profileContainer.setVisible(false);
                profileContainer.setManaged(false);
            }

            // Show "Commencer" button if guest
            if (btnCommencer != null) {
                btnCommencer.setVisible(true);
                btnCommencer.setManaged(true);
            }
        }
    }

    @FXML
    void goToLogin(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/Login.fxml", "Ardhi - Connexion");
    }

    @FXML
    void goToProfile(javafx.scene.input.MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/Profil.fxml", "Ardhi - Mon Profil");
    }

    @FXML
    void goToUserDiag(javafx.scene.input.MouseEvent event) {
        if (ensureLoggedIn(event, "/fxml/UserAndDiag/ClientDashboard.fxml", "Ardhi - Espace Agriculteur")) {
            WindowUtils.loadScene(event, "/fxml/UserAndDiag/ClientDashboard.fxml", "Ardhi - Espace Agriculteur");
        }
    }

    @FXML
    void goToAgriculteur(ActionEvent event) {
        if (ensureLoggedIn(event, "/fxml/UserAndDiag/ClientDashboard.fxml", "Ardhi - Espace Agriculteur")) {
            WindowUtils.loadScene(event, "/fxml/UserAndDiag/ClientDashboard.fxml", "Ardhi - Espace Agriculteur");
        }
    }

    /**
     * Handlers for navigation menu items (Marketplace, Maintenance, etc.)
     * These are currently placeholders or redirect to specific pages if
     * implemented.
     * For now, we'll route them to the main dashboard or specific pages if they
     * exist.
     */
    @FXML
    void goToMarketplace(javafx.event.Event event) {
        // Navigate to the marketplace page, but require login first
        if (ensureLoggedIn(event, "/fxml/marketplace/Marketplace.fxml", "Ardhi - Marketplace")) {
            WindowUtils.loadScene(event, "/fxml/marketplace/Marketplace.fxml", "Ardhi - Marketplace");
        }
    }

    @FXML
    void goToMaintenance(javafx.event.Event event) {
        if (ensureLoggedIn(event, "/fxml/MaterielEtMaintenance/MaterielsEtMaintenance.fxml",
                "Ardhi - Matériels & Maintenance")) {
            WindowUtils.loadScene(event, "/fxml/MaterielEtMaintenance/MaterielsEtMaintenance.fxml",
                    "Ardhi - Matériels & Maintenance");
        }
    }

    @FXML
    void goToCultures(javafx.event.Event event) {
        if (ensureLoggedIn(event, "/fxml/Parcelle_Cultures/AgriculteurDashboard.fxml", "Ardhi - Espace Cultures")) {
            WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/AgriculteurDashboard.fxml", "Ardhi - Espace Cultures");
        }
    }

    @FXML
    void goToEvents(javafx.event.Event event) {
        if (ensureLoggedIn(event, "/fxml/Evenement/Navigationevenements.fxml", "Ardhi - Évènements")) {
            WindowUtils.loadScene(event, "/fxml/Evenement/Navigationevenements.fxml", "Ardhi - Évènements");
        }
    }

    @FXML
    void goToEmployees(javafx.event.Event event) {
        // Vérifier si l'utilisateur est connecté
        UserSession session = UserSession.getInstance();
        if (session == null || session.getUser() == null) {
            // Pas connecté → Rediriger vers login
            LoginController.setRedirect("/fxml/gestionemploye/Dashboard_employe_agriculture.fxml", "Ardhi - Gestion des Employés");
            WindowUtils.loadScene(event, "/fxml/UserAndDiag/Login.fxml", "Ardhi - Connexion");
            return;
        }

        // Récupérer le rôle de l'utilisateur
        Role userRole = session.getUser().getRole();

        // Vérifier si le rôle est ADMIN ou AGRICULTEUR
        if (userRole != Role.ADMIN && userRole != Role.AGRICULTEUR) {
            // ❌ Accès refusé pour CLIENT, AGRONOME, etc.
            showAccessDeniedAlert(
                    "Accès refusé",
                    "Ce service est réservé uniquement aux administrateurs et aux agriculteurs.\n\n" +
                            "Votre rôle actuel : " + userRole
            );
            return;
        }

        // ✅ Accès autorisé
        WindowUtils.loadScene(event, "/fxml/gestionemploye/Dashboard_employe_agriculture.fxml", "Ardhi - Gestion des Employés");
    }

    @FXML
    void handleCallToAction(ActionEvent event) {
        UserSession session = UserSession.getInstance();
        if (session != null && session.getUser() != null) {
            // Already logged in -> Dashboard
            WindowUtils.loadScene(event, "/fxml/UserAndDiag/ClientDashboard.fxml", "Ardhi - Espace Agriculteur");
        } else {
            // Not logged in -> Register (or Login if preferred)
            // Usually "Start" goes to registration.
            // If they register, they usually go to login then dashboard.
            // Complex flow: Register -> Login -> Redirect?
            // For now, let's just send to Inscription.
            WindowUtils.loadScene(event, "/fxml/UserAndDiag/Inscription.fxml", "Ardhi - Inscription");
        }
    }

    private boolean ensureLoggedIn(javafx.event.Event event, String targetFxml, String targetTitle) {
        UserSession session = UserSession.getInstance();
        if (session == null || session.getUser() == null) {
            // Redirect to Login, setting the static target
            LoginController.setRedirect(targetFxml, targetTitle);
            WindowUtils.loadScene(event, "/fxml/UserAndDiag/Login.fxml", "Ardhi - Connexion");
            return false;
        }
        return true;
    }

    // Overload for backward compatibility or simple checks
    // private boolean ensureLoggedIn(javafx.event.Event event) {
    // return ensureLoggedIn(event, null, null);
    // }
    // Pagination Logic
    private void setupPagination() {
        if (paginationContainer == null)
            return;
        paginationContainer.getChildren().clear();

        for (int i = 0; i < slideshowBackgrounds.size(); i++) {
            javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(4); // Radius 4
            dot.setFill(javafx.scene.paint.Color.WHITE);
            dot.setStroke(javafx.scene.paint.Color.WHITE);
            dot.setStrokeWidth(1);
            dot.setStyle("-fx-cursor: hand;");

            final int index = i;
            dot.setOnMouseClicked(e -> {
                resetSlideshowTimer(); // Manual click -> reset timer
                currentImageIndex = index;
                transitionToImage(currentImageIndex);
            });

            paginationContainer.getChildren().add(dot);
        }
        updatePagination();
    }
    private void showAccessDeniedAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle(title);
        alert.setHeaderText("🚫 Accès non autorisé");
        alert.setContentText(message);

        // Style de l'alerte
        javafx.scene.control.DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(
                "-fx-background-color: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 20px; " +
                        "-fx-border-color: #e74c3c; " +
                        "-fx-border-width: 2px;"
        );

        alert.showAndWait();
    }
    private void updatePagination() {
        if (paginationContainer == null)
            return;
        for (int i = 0; i < paginationContainer.getChildren().size(); i++) {
            javafx.scene.Node node = paginationContainer.getChildren().get(i);
            if (node instanceof javafx.scene.shape.Circle) {
                javafx.scene.shape.Circle dot = (javafx.scene.shape.Circle) node;
                if (i == currentImageIndex) {
                    dot.setOpacity(1.0);
                    dot.setRadius(6); // Slightly larger when active
                } else {
                    dot.setOpacity(0.4); // Semi-transparent when inactive
                    dot.setRadius(4);
                }
            }
        }
    }
}
