package tn.neuron.ardhi.controllers.gestionemployecontroller;

import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import tn.neuron.ardhi.models.gestionemployemodel.Notification;
import tn.neuron.ardhi.services.gestionemployeservice.NotificationService;
import tn.neuron.ardhi.utils.gestionemployeutils.AgriculteurContext;
import tn.neuron.ardhi.utils.gestionemployeutils.NotificationToast;
import tn.neuron.ardhi.utils.gestionemployeutils.LanguageManager;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class NotificationController implements Initializable {

    @FXML private VBox notificationList;
    @FXML private Label lblTotalNotifications;
    @FXML private Label lblNonLues;
    @FXML private Button btnMarkAllRead;
    @FXML private TabPane tabPane;
    @FXML private Tab tabNonLues;
    @FXML private Tab tabTout;
    @FXML private ScrollPane scrollPane;

    private NotificationService notificationService;
    private ObservableList<Notification> allNotifications;
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        notificationService = new NotificationService();
        allNotifications = FXCollections.observableArrayList();

        analyserEtCharger();
        updateStatistics();

        LanguageManager.getInstance().applyOrientation(
                notificationList != null && notificationList.getScene() != null ?
                        (javafx.scene.Parent) notificationList.getScene().getRoot() : null
        );
    }

    private Integer getActiveUserId() {
        Integer fromContext = AgriculteurContext.getActiveAgriculteurId();
        if (fromContext != null) return fromContext;
        UserSession session = UserSession.getInstance();
        if (session != null && session.getUser() != null) {
            return session.getUser().getId();
        }
        return null;
    }

    private void loadNotifications() {
        Integer idAgri = getActiveUserId();
        if (idAgri == null) {
            System.err.println("❌ NotificationController: aucun utilisateur actif");
            return;
        }

        List<Notification> notifications = notificationService.getNotificationsByAgriculteur(idAgri);
        allNotifications.setAll(notifications);
        displayNotifications(notifications);
    }

    /**
     * Appelée UNE SEULE FOIS à l'ouverture :
     * analyse les nouvelles notifications, puis charge la liste.
     */
    private void analyserEtCharger() {
        Integer idAgri = getActiveUserId();
        if (idAgri == null) return;
        notificationService.analyserNotifications(idAgri);
        loadNotifications();
    }

    private void displayNotifications(List<Notification> notifications) {
        notificationList.getChildren().clear();

        if (notifications.isEmpty()) {
            VBox emptyState = createEmptyState();
            notificationList.getChildren().add(emptyState);
            return;
        }

        for (Notification notif : notifications) {
            VBox card = createNotificationCard(notif);
            notificationList.getChildren().add(card);
            animateCardEntry(card);
        }
    }

    private VBox createNotificationCard(Notification notif) {
        VBox card = new VBox(10);
        card.getStyleClass().add("notification-card");

        if (!notif.isLue()) {
            card.getStyleClass().add("notification-unread");
        } else {
            card.getStyleClass().add("notification-read");
        }

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(notif.getIcone());
        iconLabel.setStyle("-fx-font-size: 32px;");

        VBox titleBox = new VBox(5);
        Label titleLabel = new Label(notif.getTitre());
        titleLabel.getStyleClass().add("notification-title");
        Label timeLabel = new Label(notif.getDateCreation().format(timeFormatter));
        timeLabel.getStyleClass().add("notification-time");
        titleBox.getChildren().addAll(titleLabel, timeLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label priorityBadge = new Label(getPriorityText(notif.getPriorite()));
        priorityBadge.setStyle(
                "-fx-background-color: " + notif.getCouleurPriorite() + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 15px;" +
                        "-fx-padding: 5px 12px;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;"
        );

        header.getChildren().addAll(iconLabel, titleBox, spacer, priorityBadge);

        Label messageLabel = new Label(notif.getMessage());
        messageLabel.getStyleClass().add("notification-message");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(Double.MAX_VALUE);

        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        if (!notif.isLue()) {
            Button btnMarkRead = new Button("✓ " + LanguageManager.getInstance().get("notification.mark.all.read").replace("✓ ", ""));
            btnMarkRead.getStyleClass().add("button-edit");
            btnMarkRead.setOnAction(e -> markAsRead(notif));
            actionBox.getChildren().add(btnMarkRead);
        }

        card.getChildren().addAll(header, messageLabel, actionBox);

        card.setOnMouseClicked(e -> {
            if (!notif.isLue()) markAsRead(notif);
        });

        return card;
    }

    private VBox createEmptyState() {
        VBox empty = new VBox(20);
        empty.setAlignment(Pos.CENTER);
        empty.setStyle("-fx-padding: 100px;");

        Label iconLabel = new Label("🔔");
        iconLabel.setStyle("-fx-font-size: 80px;");

        Label textLabel = new Label(LanguageManager.getInstance().get("notification.title").replace("🔔 ", ""));
        textLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: #7F8C8D; -fx-font-weight: bold;");

        Label descLabel = new Label(LanguageManager.getInstance().get("notification.subtitle"));
        descLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #95A5A6;");

        empty.getChildren().addAll(iconLabel, textLabel, descLabel);
        return empty;
    }

    private void markAsRead(Notification notif) {
        if (notificationService.markAsRead(notif.getId())) {
            notif.setLue(true);
            loadNotifications();
            updateStatistics();

            NotificationToast.showNotification(
                    "✓ Notification marquée comme lue",
                    NotificationToast.SUCCESS,
                    2000
            );
        }
    }

    @FXML
    private void handleMarkAllRead() {
        Integer idAgri = getActiveUserId();
        if (idAgri == null) return;

        if (notificationService.markAllAsRead(idAgri)) {
            loadNotifications();
            updateStatistics();

            NotificationToast.showNotification(
                    "✓ Toutes les notifications ont été marquées comme lues",
                    NotificationToast.SUCCESS,
                    2500
            );
        }
    }

    @FXML
    private void handleRefresh() {
        analyserEtCharger();
        updateStatistics();

        NotificationToast.showNotification(
                "🔄 " + LanguageManager.getInstance().get("common.refresh"),
                NotificationToast.INFO,
                1500
        );
    }

    private void updateStatistics() {
        Integer idAgri = getActiveUserId();
        if (idAgri == null) return;

        int total = allNotifications.size();
        int unread = (int) allNotifications.stream().filter(n -> !n.isLue()).count();

        if (lblTotalNotifications != null) {
            lblTotalNotifications.setText(String.valueOf(total));
        }
        if (lblNonLues != null) {
            lblNonLues.setText(String.valueOf(unread));
        }
        if (btnMarkAllRead != null) {
            btnMarkAllRead.setDisable(unread == 0);
        }
    }

    private String getPriorityText(String priorite) {
        switch (priorite) {
            case Notification.PRIORITE_CRITICAL: return "CRITIQUE";
            case Notification.PRIORITE_WARNING: return "ATTENTION";
            case Notification.PRIORITE_INFO: return "INFO";
            default: return "INFO";
        }
    }

    private void animateCardEntry(VBox card) {
        card.setOpacity(0);
        card.setTranslateY(20);

        FadeTransition fade = new FadeTransition(Duration.millis(400), card);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition translate = new TranslateTransition(Duration.millis(400), card);
        translate.setFromY(20);
        translate.setToY(0);

        ParallelTransition parallel = new ParallelTransition(fade, translate);
        parallel.setDelay(Duration.millis(50));
        parallel.play();
    }
}