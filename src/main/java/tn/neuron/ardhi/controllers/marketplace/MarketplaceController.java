package tn.neuron.ardhi.controllers.marketplace;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import tn.neuron.ardhi.models.UserAndDiag.Role;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.models.marketplace.Produit;
import tn.neuron.ardhi.services.marketplace.CommandeService;
import tn.neuron.ardhi.services.marketplace.ProduitService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;
import javafx.scene.input.MouseEvent;
import tn.neuron.ardhi.services.marketplace.NotificationMarketService;


import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Contrôleur pour la page Marketplace.
 * Gère le slideshow, la navigation, l'affichage conditionnel (AGRICULTEUR)
 * et la section recommandations (derniers achats + nouveautés).
 */
public class MarketplaceController {

    // --- FXML Fields matching Marketplace.fxml ---
    @FXML
    private Label lblBienvenue;
    @FXML
    private Label heroDescription;
    @FXML
    private Label lblRole;

    @FXML
    private ImageView heroBackground;
    @FXML
    private ImageView heroBackgroundOverlay;
    @FXML
    private HBox paginationContainer;
    @FXML
    private ScrollPane servicesScrollPane;
    @FXML private HBox statsCard;


    // Cards (HBox in FXML)
    @FXML
    private HBox productManagementCard;
    @FXML
    private HBox ordersReceivedCard;

    // Nav Buttons (visible only for AGRICULTEUR)
    @FXML
    private Button btnNavProduits;
    @FXML
    private Button btnNavCommandesVendeur;
    @FXML
    private Button btnNavMarketplace;

    // ── Recommandations ────────────────────────────────────────────────
    @FXML
    private VBox blocDerniersAchats;
    @FXML
    private Region separateurRec;
    @FXML
    private HBox cartesDerniersAchats;
    @FXML
    private HBox cartesNouveautes;
//notifications
@FXML private javafx.scene.control.Label badgeNotif;
    @FXML private javafx.scene.layout.StackPane btnNotifWrapper;
    // --- Slideshow State ---
    private final List<Image> slideshowImages = new ArrayList<>();
    private int currentImageIndex = 0;
    private Timeline slideshowTimeline;

    private final String[] slideDescriptions = {
            "Achetez et vendez vos produits agricoles en toute confiance. Gérez vos commandes et suivez vos ventes depuis un seul endroit.",
            "Accédez à un vaste catalogue de produits frais, semences, engrais et équipements agricoles.",
            "Gérez vos stocks, suivez vos commandes et optimisez vos ventes grâce à nos outils dédiés.",
            "Rejoignez une communauté d'agriculteurs connectés et développez votre activité."
    };

    @FXML
    public void initialize() {
        UserSession session = UserSession.getInstance();
        if (session != null && session.getUser() != null) {
            User user = session.getUser();
            lblBienvenue.setText("Bienvenue, " + user.getPrenom() + " " + user.getNom());

            if (lblRole != null) {
                lblRole.setText("Marketplace · " + formatRole(user.getRole()));
            }

            // Show AGRICULTEUR-only elements
            if (user.getRole() == Role.AGRICULTEUR) {
                if (statsCard != null) {
                    statsCard.setVisible(true);
                    statsCard.setManaged(true);
                }
                if (productManagementCard != null) {
                    productManagementCard.setVisible(true);
                    productManagementCard.setManaged(true);
                }
                if (ordersReceivedCard != null) {
                    ordersReceivedCard.setVisible(true);
                    ordersReceivedCard.setManaged(true);
                }
                if (btnNavProduits != null) {
                    btnNavProduits.setVisible(true);
                    btnNavProduits.setManaged(true);
                }
                if (btnNavCommandesVendeur != null) {
                    btnNavCommandesVendeur.setVisible(true);
                    btnNavCommandesVendeur.setManaged(true);
                }
            }

            // ── Charger les recommandations ──────────────────────────
            chargerRecommandations(user.getId());
            chargerBadgeNotifications(user.getId()); // ✅ AJOUT

        }

        // --- Slideshow ---
        startSlideshow();
        setupPagination();

        // Prevent nested scrolling
        if (servicesScrollPane != null) {
            servicesScrollPane.setOnScroll(event -> event.consume());
        }
    }

    @FXML
    void goToStatsFromCard(MouseEvent event) {
        WindowUtils.loadScene(event,
                "/fxml/marketplace/FarmerStats.fxml",
                "Ardhi - Mes Statistiques");
    }
    private void chargerBadgeNotifications(int idUser) {
        try {
            NotificationMarketService notifService = new NotificationMarketService();
            int nbNonLues = notifService.compterNonLues(idUser);

            if (badgeNotif != null) {
                if (nbNonLues > 0) {
                    badgeNotif.setText(nbNonLues > 99 ? "99+" : String.valueOf(nbNonLues));
                    badgeNotif.setVisible(true);
                    badgeNotif.setManaged(true);

                    // Animation pulse sur le badge
                    javafx.animation.ScaleTransition pulse =
                            new javafx.animation.ScaleTransition(
                                    javafx.util.Duration.millis(600), badgeNotif);
                    pulse.setFromX(1.0); pulse.setFromY(1.0);
                    pulse.setToX(1.2);   pulse.setToY(1.2);
                    pulse.setAutoReverse(true);
                    pulse.setCycleCount(4);
                    pulse.play();
                } else {
                    badgeNotif.setVisible(false);
                    badgeNotif.setManaged(false);
                }
            }
        } catch (Exception e) {
            System.err.println("[MarketplaceController] Erreur badge notif : " + e.getMessage());
        }
    }

    @FXML
    void ouvrirNotifications(MouseEvent event) {
        // Utiliser le StackPane comme source au lieu de l'event
        javafx.stage.Stage stage = (javafx.stage.Stage) btnNotifWrapper.getScene().getWindow();
        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/marketplace/NotificationMarket.fxml"));
        try {
            javafx.scene.Parent root = loader.load();
            stage.setScene(new javafx.scene.Scene(root));
            stage.setTitle("Ardhi - Notifications");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===================== RECOMMANDATIONS =====================

    private void chargerRecommandations(int idUser) {
        // 1) Derniers achats
        try {
            CommandeService commandeService = new CommandeService();
            List<Produit> derniersAchats = commandeService.getDerniersAchatsProduits(idUser, 5);
            if (!derniersAchats.isEmpty() && cartesDerniersAchats != null) {
                for (Produit p : derniersAchats) {
                    cartesDerniersAchats.getChildren().add(creerCarteRecommandation(p, false));
                }
                // Afficher le bloc
                if (blocDerniersAchats != null) {
                    blocDerniersAchats.setVisible(true);
                    blocDerniersAchats.setManaged(true);
                }
                if (separateurRec != null) {
                    separateurRec.setVisible(true);
                    separateurRec.setManaged(true);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement derniers achats : " + e.getMessage());
        }

        // 2) Nouveautés
        try {
            ProduitService produitService = new ProduitService();
            List<Produit> nouveautes = produitService.getNouveauxProduits(idUser, 5);
            if (cartesNouveautes != null) {
                for (Produit p : nouveautes) {
                    cartesNouveautes.getChildren().add(creerCarteRecommandation(p, true));
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement nouveautés : " + e.getMessage());
        }
    }

    /**
     * Crée une mini-carte produit glassmorphism pour la section recommandations.
     *
     * @param produit le produit à afficher
     * @param isNew   si true, affiche un badge "NOUVEAU"
     */
    private VBox creerCarteRecommandation(Produit produit, boolean isNew) {
        VBox carte = new VBox(10);
        carte.setAlignment(Pos.TOP_LEFT);
        carte.setPrefWidth(190);
        carte.setMaxWidth(190);
        carte.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1);" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: rgba(255,255,255,0.18);" +
                        "-fx-border-radius: 16;" +
                        "-fx-padding: 0 0 14 0;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 14, 0, 0, 4);");

        // --- Image du produit ---
        Rectangle imageClip = new Rectangle(190, 115);
        imageClip.setArcWidth(16);
        imageClip.setArcHeight(16);

        ImageView iv = new ImageView();
        iv.setFitWidth(190);
        iv.setFitHeight(115);
        iv.setPreserveRatio(false);
        iv.setClip(imageClip);

        // Charger l'image — stockée comme chemin absolu sur le disque
        try {
            String imgPath = produit.getImage();
            if (imgPath != null && !imgPath.isEmpty()) {
                java.io.File imgFile = new java.io.File(imgPath);
                if (imgFile.exists()) {
                    iv.setImage(new Image(imgFile.toURI().toString(), true));
                }
            }
        } catch (Exception ignored) {
        }

        if (iv.getImage() == null || iv.getImage().isError()) {
            // Placeholder couleur vert si pas d'image
            iv.setStyle("-fx-background-color: linear-gradient(to bottom right, #4a7c59, #2d4a36);");
        }

        // Conteneur image + badge
        javafx.scene.layout.StackPane imgStack = new javafx.scene.layout.StackPane(iv);
        imgStack.setPrefHeight(115);

        if (isNew) {
            Label badge = new Label("NOUVEAU");
            badge.setStyle(
                    "-fx-background-color: #8BC34A;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 9px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-padding: 3 8;" +
                            "-fx-background-radius: 20;");
            javafx.scene.layout.StackPane.setAlignment(badge, Pos.TOP_LEFT);
            javafx.scene.layout.StackPane.setMargin(badge, new javafx.geometry.Insets(10, 0, 0, 10));
            imgStack.getChildren().add(badge);
        }

        // --- Infos textuelles ---
        VBox infos = new VBox(5);
        infos.setStyle("-fx-padding: 10 14 0 14;");

        // Catégorie
        Label lblCategorie = new Label(produit.getCategorie() != null ? produit.getCategorie().toUpperCase() : "");
        lblCategorie.setStyle("-fx-text-fill: #8BC34A; -fx-font-size: 9px; -fx-font-weight: bold;");

        // Nom
        Label lblNom = new Label(produit.getNom());
        lblNom.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
        lblNom.setWrapText(true);
        lblNom.setMaxWidth(162);

        // Prix
        String prixStr = String.format("%.2f DT", produit.getPrixApresRemise());
        Label lblPrix = new Label(prixStr);
        lblPrix.setStyle("-fx-text-fill: #8BC34A; -fx-font-size: 14px; -fx-font-weight: bold;");

        infos.getChildren().addAll(lblCategorie, lblNom, lblPrix);

        // Si remise, afficher l'ancien prix barré
        if (produit.aUneRemise()) {
            Label lblAncienPrix = new Label(String.format("%.2f DT", produit.getPrix()));
            lblAncienPrix.setStyle(
                    "-fx-text-fill: rgba(255,255,255,0.4);" +
                            "-fx-font-size: 11px;" +
                            "-fx-strikethrough: true;");
            infos.getChildren().add(1, lblAncienPrix); // juste après nom, avant prix
        }

        carte.getChildren().addAll(imgStack, infos);

        // ── Hover animation ──────────────────────────────────────────
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(150), carte);
        scaleIn.setToX(1.04);
        scaleIn.setToY(1.04);

        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(150), carte);
        scaleOut.setToX(1.0);
        scaleOut.setToY(1.0);

        carte.setOnMouseEntered(e -> {
            carte.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.15);" +
                            "-fx-background-radius: 16;" +
                            "-fx-border-color: rgba(139,195,74,0.4);" +
                            "-fx-border-radius: 16;" +
                            "-fx-padding: 0 0 14 0;" +
                            "-fx-cursor: hand;" +
                            "-fx-effect: dropshadow(gaussian, rgba(139,195,74,0.25), 20, 0, 0, 6);");
            scaleIn.playFromStart();
        });
        carte.setOnMouseExited(e -> {
            carte.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.1);" +
                            "-fx-background-radius: 16;" +
                            "-fx-border-color: rgba(255,255,255,0.18);" +
                            "-fx-border-radius: 16;" +
                            "-fx-padding: 0 0 14 0;" +
                            "-fx-cursor: hand;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 14, 0, 0, 4);");
            scaleOut.playFromStart();
        });
        carte.setOnMouseClicked(
                e -> WindowUtils.loadScene(e, "/fxml/marketplace/CatalogueProduit.fxml", "Ardhi - Catalogue"));

        return carte;
    }

    // ===================== UTILITAIRES =====================

    private String formatRole(Role role) {
        if (role == null)
            return "Utilisateur";
        String name = role.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }

    // ===================== SLIDESHOW LOGIC =====================

    private void startSlideshow() {
        loadImageQuietly("@../img/hero_1.png");
        loadImageQuietly("/img/hero_2.png");
        loadImageQuietly("/img/hero_3.png");
        loadImageQuietly("/img/hero_4.png");

        if (slideshowImages.isEmpty()) {
            System.err.println("No slideshow images loaded.");
            return;
        }

        heroBackground.setImage(slideshowImages.get(0));

        slideshowTimeline = new Timeline(
                new KeyFrame(Duration.seconds(5), event -> nextSlide(null)));
        slideshowTimeline.setCycleCount(Animation.INDEFINITE);
        slideshowTimeline.play();
    }

    private void loadImageQuietly(String path) {
        try {
            URL resource = getClass().getResource(path);
            if (resource != null) {
                slideshowImages.add(new Image(resource.toExternalForm()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetSlideshowTimer() {
        if (slideshowTimeline != null) {
            slideshowTimeline.stop();
            slideshowTimeline.play();
        }
    }

    @FXML
    void nextSlide(ActionEvent event) {
        if (slideshowImages.isEmpty())
            return;
        if (event != null)
            resetSlideshowTimer();
        currentImageIndex = (currentImageIndex + 1) % slideshowImages.size();
        transitionToImage(currentImageIndex);
    }

    @FXML
    void prevSlide(ActionEvent event) {
        if (slideshowImages.isEmpty())
            return;
        resetSlideshowTimer();
        currentImageIndex = (currentImageIndex - 1 + slideshowImages.size()) % slideshowImages.size();
        transitionToImage(currentImageIndex);
    }

    private void transitionToImage(int index) {
        Image nextImage = slideshowImages.get(index);

        if (heroDescription != null && index >= 0 && index < slideDescriptions.length) {
            heroDescription.setText(slideDescriptions[index]);
        }

        heroBackgroundOverlay.setImage(nextImage);
        heroBackgroundOverlay.setOpacity(0.0);
        updatePagination();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), heroBackgroundOverlay);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setOnFinished(e -> {
            heroBackground.setImage(nextImage);
            heroBackgroundOverlay.setOpacity(0.0);
        });
        fadeIn.play();
    }

    private void setupPagination() {
        if (paginationContainer == null)
            return;
        paginationContainer.getChildren().clear();

        for (int i = 0; i < slideshowImages.size(); i++) {
            Circle dot = new Circle(4);
            dot.setFill(Color.WHITE);
            dot.setStroke(Color.WHITE);
            dot.setStrokeWidth(1);
            dot.setStyle("-fx-cursor: hand;");

            final int index = i;
            dot.setOnMouseClicked(e -> {
                resetSlideshowTimer();
                currentImageIndex = index;
                transitionToImage(currentImageIndex);
            });

            paginationContainer.getChildren().add(dot);
        }
        updatePagination();
    }

    private void updatePagination() {
        if (paginationContainer == null)
            return;
        for (int i = 0; i < paginationContainer.getChildren().size(); i++) {
            if (paginationContainer.getChildren().get(i) instanceof Circle) {
                Circle dot = (Circle) paginationContainer.getChildren().get(i);
                if (i == currentImageIndex) {
                    dot.setOpacity(1.0);
                    dot.setRadius(6);
                } else {
                    dot.setOpacity(0.4);
                    dot.setRadius(4);
                }
            }
        }
    }

    // ===================== NAVIGATION HANDLERS =====================

    @FXML
    void goToDashboard(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/LandingPage.fxml", "Ardhi - Accueil");
    }

    @FXML
    void logout(ActionEvent event) {
        UserSession session = UserSession.getInstance();
        if (session != null) {
            session.cleanUserSession();
        }
        WindowUtils.loadScene(event, "/fxml/LandingPage.fxml", "Ardhi - Accueil");
    }

    // --- Card Clicks (MouseEvent) ---

    @FXML
    void goToCatalogueFromCard(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/marketplace/CatalogueProduit.fxml", "Ardhi - Catalogue");
    }

    @FXML
    void goToCommandesFromCard(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/marketplace/Commandes.fxml", "Ardhi - Mes Commandes");
    }

    @FXML
    void goToMesFavorisFromCard(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/marketplace/MesFavoris.fxml", "Ardhi - Mes Favoris");
    }

    @FXML
    void goToProductManagementFromCard(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/marketplace/GestionProduits.fxml", "Ardhi - Gestion Produits");
    }

    @FXML
    void goToCommandesVendeurFromCard(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/marketplace/CommandesVendeur.fxml", "Ardhi - Commandes Reçues");
    }

    // --- Button Clicks (ActionEvent) for catalogue/voir tout ---

    @FXML
    void goToCatalogueAction(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/marketplace/CatalogueProduit.fxml", "Ardhi - Catalogue");
    }

    @FXML
    void goToDiagnostics(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/ClientDashboard.fxml", "Diagnostic IA de vos Plantes");
    }

    @FXML
    void goToMaintenance(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/MaterielEtMaintenance/MaterielsEtMaintenance.fxml",
                "Ardhi - Matériels & Maintenance");
    }

    @FXML
    void goToCultures(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/AgriculteurDashboard.fxml", "Ardhi - Mes Cultures");
    }

    @FXML
    void goToEvents(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/Evenement/Navigationevenements.fxml", "Ardhi - Évènements");
    }

    @FXML
    void goToEmployees(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/gestionemploye/Dashboard_employe_agriculture.fxml",
                "Ardhi - Gestion des Employés");
    }
}
