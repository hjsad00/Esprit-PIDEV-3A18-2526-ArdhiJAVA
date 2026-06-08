package tn.neuron.ardhi.controllers.marketplace;

import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import tn.neuron.ardhi.models.marketplace.NotificationMarket;
import tn.neuron.ardhi.services.marketplace.NotificationMarketService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationMarketController {

    @FXML private VBox  listeNotifications;
    @FXML private VBox  etatVide;
    @FXML private Label lblCompteurNonLues;
    @FXML private Button btnFiltreAll;
    @FXML private Button btnFiltreAchat;
    @FXML private Button btnFiltreAvis;

    private NotificationMarketService service;
    private List<NotificationMarket>  toutesLesNotifs;
    private int                        idVendeur;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    // ── Styles boutons filtre ─────────────────────────────────────────────────
    private static final String STYLE_ACTIF =
            "-fx-background-color: #8BC34A; -fx-text-fill: white;" +
                    "-fx-background-radius: 20; -fx-cursor: hand;" +
                    "-fx-padding: 6 18; -fx-font-size: 12px; -fx-font-weight: bold;";
    private static final String STYLE_INACTIF =
            "-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: rgba(255,255,255,0.7);" +
                    "-fx-background-radius: 20; -fx-border-color: rgba(255,255,255,0.15);" +
                    "-fx-border-radius: 20; -fx-cursor: hand; -fx-padding: 6 18; -fx-font-size: 12px;";

    @FXML
    public void initialize() {
        service    = new NotificationMarketService();
        idVendeur  = UserSession.getInstance().getUser().getId();
        charger();
    }

    // ── Chargement ────────────────────────────────────────────────────────────

    private void charger() {
        toutesLesNotifs = service.getNotificationsParVendeur(idVendeur);
        afficher(toutesLesNotifs);
        mettreAJourCompteur();
    }

    private void afficher(List<NotificationMarket> liste) {
        listeNotifications.getChildren().clear();

        if (liste.isEmpty()) {
            etatVide.setVisible(true);
            etatVide.setManaged(true);
        } else {
            etatVide.setVisible(false);
            etatVide.setManaged(false);
            for (NotificationMarket n : liste) {
                listeNotifications.getChildren().add(creerCarteNotif(n));
            }
        }
    }

    private void mettreAJourCompteur() {
        long nb = toutesLesNotifs.stream().filter(n -> !n.isLue()).count();
        lblCompteurNonLues.setText(nb + " non lue" + (nb > 1 ? "s" : ""));
        lblCompteurNonLues.setVisible(nb > 0);
        lblCompteurNonLues.setManaged(nb > 0);
    }

    // ── Carte notification ────────────────────────────────────────────────────

    private HBox creerCarteNotif(NotificationMarket notif) {
        boolean nonLue = !notif.isLue();

        // Conteneur principal
        HBox carte = new HBox(16);
        carte.setAlignment(Pos.CENTER_LEFT);
        carte.setPadding(new Insets(18, 22, 18, 22));
        carte.setStyle(
                "-fx-background-color: " + (nonLue
                        ? "rgba(139,195,74,0.08)"
                        : "rgba(255,255,255,0.04)") + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: " + (nonLue
                        ? "rgba(139,195,74,0.3)"
                        : "rgba(255,255,255,0.07)") + ";" +
                        "-fx-border-radius: 14;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 10, 0, 0, 3);");

        // Icône
        Label lblIcone = new Label(notif.getIcone());
        lblIcone.setStyle(
                "-fx-font-size: 28px;" +
                        "-fx-background-color: " + notif.getCouleur() + "22;" +
                        "-fx-background-radius: 50;" +
                        "-fx-padding: 10;" +
                        "-fx-min-width: 52; -fx-min-height: 52;" +
                        "-fx-alignment: center;");

        // Barre non-lue (trait vert à gauche)
        Region barreNonLue = new Region();
        barreNonLue.setPrefWidth(3);
        barreNonLue.setPrefHeight(60);
        barreNonLue.setStyle("-fx-background-color: " + notif.getCouleur() + "; -fx-background-radius: 3;");
        barreNonLue.setVisible(nonLue);
        barreNonLue.setManaged(nonLue);

        // Textes
        VBox textes = new VBox(5);
        HBox.setHgrow(textes, javafx.scene.layout.Priority.ALWAYS);

        Label lblTitre = new Label(notif.getTitre());
        lblTitre.setStyle(
                "-fx-text-fill: " + (nonLue ? "white" : "rgba(255,255,255,0.75)") + ";" +
                        "-fx-font-size: 14px; -fx-font-weight: bold;");

        Label lblMessage = new Label(notif.getMessage());
        lblMessage.setStyle("-fx-text-fill: rgba(255,255,255,0.55); -fx-font-size: 12px;");
        lblMessage.setWrapText(true);

        Label lblDate = new Label(notif.getDateCreation() != null
                ? notif.getDateCreation().format(FMT) : "");
        lblDate.setStyle("-fx-text-fill: rgba(255,255,255,0.3); -fx-font-size: 11px;");

        textes.getChildren().addAll(lblTitre, lblMessage, lblDate);

        // Badge NON LU
        Label badge = new Label("●");
        badge.setStyle("-fx-text-fill: #8BC34A; -fx-font-size: 10px;");
        badge.setVisible(nonLue);
        badge.setManaged(nonLue);

        // Bouton supprimer
        Button btnSuppr = new Button("✕");
        btnSuppr.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.25);" +
                        "-fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 4 8;");
        btnSuppr.setOnMouseEntered(e ->
                btnSuppr.setStyle("-fx-background-color: rgba(231,76,60,0.2); " +
                        "-fx-text-fill: #e74c3c; -fx-cursor: hand; -fx-font-size: 13px; " +
                        "-fx-padding: 4 8; -fx-background-radius: 6;"));
        btnSuppr.setOnMouseExited(e ->
                btnSuppr.setStyle("-fx-background-color: transparent; " +
                        "-fx-text-fill: rgba(255,255,255,0.25); " +
                        "-fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 4 8;"));
        btnSuppr.setOnAction(e -> {
            service.supprimerNotification(notif.getIdNotif());
            toutesLesNotifs.removeIf(n -> n.getIdNotif() == notif.getIdNotif());
            afficher(toutesLesNotifs);
            mettreAJourCompteur();
        });

        carte.getChildren().addAll(barreNonLue, lblIcone, textes, badge, btnSuppr);

        // Clic → marquer comme lue
        carte.setOnMouseClicked(e -> {
            if (!notif.isLue()) {
                service.marquerCommeLue(notif.getIdNotif());
                notif.setLue(true);
                afficher(toutesLesNotifs);
                mettreAJourCompteur();
            }
        });

        // Hover animation
        ScaleTransition si = new ScaleTransition(Duration.millis(120), carte);
        si.setToX(1.01); si.setToY(1.01);
        ScaleTransition so = new ScaleTransition(Duration.millis(120), carte);
        so.setToX(1.0); so.setToY(1.0);
        carte.setOnMouseEntered(e -> { si.playFromStart(); carte.setStyle(carte.getStyle()
                .replace("rgba(0,0,0,0.25)", "rgba(0,0,0,0.15)")); });
        carte.setOnMouseExited(e -> so.playFromStart());

        return carte;
    }

    // ── Filtres ───────────────────────────────────────────────────────────────

    @FXML
    void filtrerToutes(ActionEvent event) {
        setFiltreActif(btnFiltreAll);
        afficher(toutesLesNotifs);
    }

    @FXML
    void filtrerAchats(ActionEvent event) {
        setFiltreActif(btnFiltreAchat);
        afficher(toutesLesNotifs.stream()
                .filter(n -> NotificationMarket.TYPE_ACHAT.equals(n.getType()))
                .collect(Collectors.toList()));
    }

    @FXML
    void filtrerAvis(ActionEvent event) {
        setFiltreActif(btnFiltreAvis);
        afficher(toutesLesNotifs.stream()
                .filter(n -> NotificationMarket.TYPE_AVIS.equals(n.getType()))
                .collect(Collectors.toList()));
    }

    private void setFiltreActif(Button actif) {
        for (Button b : new Button[]{btnFiltreAll, btnFiltreAchat, btnFiltreAvis}) {
            b.setStyle(b == actif ? STYLE_ACTIF : STYLE_INACTIF);
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    @FXML
    void marquerToutesLues(ActionEvent event) {
        service.marquerToutesCommeLues(idVendeur);
        toutesLesNotifs.forEach(n -> n.setLue(true));
        afficher(toutesLesNotifs);
        mettreAJourCompteur();
    }

    @FXML
    void retourMarketplace(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/marketplace/Marketplace.fxml", "Ardhi - Marketplace");
    }
}