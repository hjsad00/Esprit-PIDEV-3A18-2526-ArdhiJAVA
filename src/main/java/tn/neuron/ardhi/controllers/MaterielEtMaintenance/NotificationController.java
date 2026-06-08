package tn.neuron.ardhi.controllers.MaterielEtMaintenance;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.geometry.Insets;
import tn.neuron.ardhi.models.MaterielEtMaintenance.Notification;
import tn.neuron.ardhi.services.MaterielEtMaintenance.NotificationService;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Contrôleur JavaFX du centre de notifications (cloche + panel).
 * Emplacement : src/main/java/tn/neuron/ardhi/controllers/MaterielEtMaintenance/NotificationController.java
 *
 * Lier dans ton FXML de navbar :
 *   <Button fx:id="btnCloche" onAction="#togglePanel" />
 *   <Label  fx:id="lblBadge" />
 *   <VBox   fx:id="panelNotifications" visible="false" />
 */
public class NotificationController implements Initializable {

    // ── FXML – à lier dans ta navbar ou ClientDashboard.fxml ─────────
    @FXML private Label lblBadge;           // Badge rouge avec le nombre
    @FXML private VBox  panelNotifications; // Panel liste des notifications
    @FXML private VBox  listeNotifications; // VBox qui contient les items
    @FXML private Label lblAucune;          // Message "Aucune notification"

    // ── Services ──────────────────────────────────────────────────────
    private final NotificationService notificationService = new NotificationService();
    private ScheduledExecutorService pollingScheduler;

    // ── État ──────────────────────────────────────────────────────────
    private int userId;
    private boolean panelVisible = false;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // userId sera injecté via setUserId() après le chargement du FXML
    }

    /**
     * Injecter l'userId après chargement du FXML.
     * Appeler depuis ton ClientDashboard controller :
     *   notificationController.setUserId(sessionUser.getId());
     */
    public void setUserId(int userId) {
        this.userId = userId;
        rafraichirBadge();
        demarrerPolling();
    }

    // ── Actions FXML ──────────────────────────────────────────────────

    @FXML
    public void togglePanel() {
        panelVisible = !panelVisible;
        panelNotifications.setVisible(panelVisible);
        if (panelVisible) chargerNotifications();
    }

    @FXML
    public void fermerPanel() {
        panelVisible = false;
        panelNotifications.setVisible(false);
    }

    @FXML
    public void toutMarquerLu() {
        notificationService.marquerToutesCommeLues(userId);
        chargerNotifications();
        rafraichirBadge();
    }

    // ── Chargement des données ────────────────────────────────────────

    private void chargerNotifications() {
        listeNotifications.getChildren().clear();

        List<Notification> notifications = notificationService.getNotificationsByUserId(userId);

        if (notifications.isEmpty()) {
            lblAucune.setVisible(true);
            return;
        }

        lblAucune.setVisible(false);

        for (Notification notif : notifications) {
            listeNotifications.getChildren().add(creerItemNotification(notif));
        }

        rafraichirBadge();
    }

    private void rafraichirBadge() {
        int count = notificationService.getNombreNonLues(userId);
        Platform.runLater(() -> {
            if (count > 0) {
                lblBadge.setVisible(true);
                lblBadge.setText(count > 99 ? "99+" : String.valueOf(count));
            } else {
                lblBadge.setVisible(false);
            }
        });
    }

    // ── Création d'un item notification ──────────────────────────────

    private HBox creerItemNotification(Notification notif) {
        HBox item = new HBox(10);
        item.setPadding(new Insets(12, 16, 12, 16));
        item.setStyle(getStyleItem(notif));

        // Point bleu si non lue
        if (!notif.isLu()) {
            Circle point = new Circle(5, Color.web("#3182ce"));
            item.getChildren().add(point);
        }

        // Emoji urgence
        Label emoji = new Label(notif.getEmoji());
        emoji.setStyle("-fx-font-size: 18px;");

        // Contenu textuel
        VBox contenu = new VBox(3);
        Label titre = new Label(notif.getTitre());
        titre.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2d3748;");
        titre.setWrapText(true);

        Label message = new Label(notif.getMessage());
        message.setStyle("-fx-font-size: 11px; -fx-text-fill: #4a5568;");
        message.setWrapText(true);

        Label date = new Label(notif.getCreatedAt() != null
                ? notif.getCreatedAt().format(DATE_FORMAT) : "");
        date.setStyle("-fx-font-size: 10px; -fx-text-fill: #a0aec0;");

        contenu.getChildren().addAll(titre, message, date);
        item.getChildren().addAll(emoji, contenu);

        // Clic → marquer comme lue
        item.setOnMouseClicked(e -> {
            if (!notif.isLu()) {
                notificationService.marquerCommeLue(notif.getId());
                notif.setLu(true);
                item.setStyle(getStyleItem(notif));
                rafraichirBadge();
            }
        });

        item.setOnMouseEntered(e -> item.setStyle(getStyleItem(notif) +
                "-fx-background-color: #edf2f7;"));
        item.setOnMouseExited(e -> item.setStyle(getStyleItem(notif)));

        return item;
    }

    // ── Style selon urgence ───────────────────────────────────────────

    private String getStyleItem(Notification notif) {
        String bordure = "#38a169"; // vert par défaut
        if (notif.getNiveauUrgence() != null) {
            switch (notif.getNiveauUrgence()) {
                case URGENT:        bordure = "#e53e3e"; break;
                case CETTE_SEMAINE: bordure = "#dd6b20"; break;
                case CE_MOIS:       bordure = "#3182ce"; break;
                case BIENTOT:       bordure = "#805ad5"; break;
                default:            bordure = "#38a169"; break;
            }
        }
        String fond = notif.isLu() ? "#ffffff" : "#ebf8ff";
        return "-fx-background-color: " + fond + "; " +
                "-fx-border-color: transparent transparent #f0f4f8 transparent; " +
                "-fx-border-width: 0 0 1 4; " +
                "-fx-border-left-color: " + bordure + "; " +
                "-fx-cursor: hand;";
    }

    // ── Polling toutes les 30 secondes ───────────────────────────────

    private void demarrerPolling() {
        pollingScheduler = Executors.newSingleThreadScheduledExecutor();
        pollingScheduler.scheduleAtFixedRate(this::rafraichirBadge, 30, 30, TimeUnit.SECONDS);
    }

    public void arreterPolling() {
        if (pollingScheduler != null) pollingScheduler.shutdown();
    }
}
