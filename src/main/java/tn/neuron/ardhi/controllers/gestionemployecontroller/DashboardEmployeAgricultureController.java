package tn.neuron.ardhi.controllers.gestionemployecontroller;

import javafx.animation.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.neuron.ardhi.models.UserAndDiag.Role;
import tn.neuron.ardhi.models.gestionemployemodel.Tache;
import tn.neuron.ardhi.models.gestionemployemodel.WeatherData;
import tn.neuron.ardhi.services.gestionemployeservice.NotificationService;
import tn.neuron.ardhi.services.gestionemployeservice.TacheService;
import tn.neuron.ardhi.services.gestionemployeservice.WeatherService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;
import tn.neuron.ardhi.utils.gestionemployeutils.AgriculteurContext;
import tn.neuron.ardhi.utils.gestionemployeutils.LanguageManager;
import tn.neuron.ardhi.utils.gestionemployeutils.NotificationBadge;
import tn.neuron.ardhi.utils.gestionemployeutils.NotificationToast;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class DashboardEmployeAgricultureController implements Initializable {

    // ── FXML básicos ────────────────────────────────────────────────────────
    @FXML private Label     lblBienvenue;
    @FXML private StackPane notificationBadgeContainer;
    @FXML private Label     heroDescription;
    @FXML private Label     lblRole;
    @FXML private HBox      paginationContainer;
    @FXML private HBox      navMenu;
    @FXML private HBox      languageSelector;
    @FXML private Button    btnLangFr, btnLangEn, btnLangAr;

    // 🔔 Badge notification colorisé
    @FXML private Label  notificationBell;
    @FXML private Circle notificationBellBg;

    // 🌦️ Panneau météo
    @FXML private VBox   weatherPanel;
    @FXML private Label  lblMeteoIcone;
    @FXML private Label  lblTemperature;
    @FXML private Label  lblMeteoDesc;
    @FXML private Label  lblMeteoVille;
    @FXML private Label  lblMeteoHumid;
    @FXML private Label  lblMeteoVent;
    @FXML private Label  lblMeteoFeels;
    @FXML private VBox   vboxAlertes;

    // ── Services ────────────────────────────────────────────────────────────
    private NotificationBadge notificationBadge;
    private NotificationService notificationService;
    private WeatherService weatherService;
    private ResourceBundle bundle;

    // Animation cloche
    private Timeline bellPulse;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.out.println("✅ Dashboard - Initialisation");

        this.bundle = rb != null ? rb : LanguageManager.getInstance().getBundle();
        weatherService = new WeatherService();

        updateUserInfo();

        // Notification badge (legacy)
        notificationService = new NotificationService();
        if (notificationBadgeContainer != null) {
            notificationBadge = new NotificationBadge();
            notificationBadgeContainer.getChildren().add(notificationBadge);
            updateNotificationBadge();
        }

        // Charger météo en arrière-plan
        loadWeatherAsync();

        highlightActiveLanguageButton();

        LanguageManager.getInstance().applyOrientation(
                lblBienvenue != null && lblBienvenue.getScene() != null
                        ? (javafx.scene.Parent) lblBienvenue.getScene().getRoot() : null
        );
    }

    // ══ User Info ════════════════════════════════════════════════════════════

    private void updateUserInfo() {
        UserSession session = UserSession.getInstance();
        if (session != null && session.getUser() != null) {
            String prenom = session.getUser().getPrenom();
            String nom    = session.getUser().getNom();
            Role   role   = session.getUser().getRole();

            // ✅ Greeting using i18n bundle (supports FR/EN/AR)
            if (lblBienvenue != null) {
                try {
                    String pattern = bundle.getString("dashboard.bienvenue");
                    lblBienvenue.setText(MessageFormat.format(pattern, prenom + " " + nom));
                } catch (Exception e) {
                    lblBienvenue.setText("Bienvenue, " + prenom + " " + nom);
                }
            }
            if (lblRole != null) lblRole.setText("Rôle : " + role);
        }
    }

    // ══ Notification Badge (rouge si non lues) ════════════════════════════════

    private Integer getActiveUserId() {
        UserSession session = UserSession.getInstance();
        if (session != null && session.getUser() != null) return session.getUser().getId();
        return AgriculteurContext.getActiveAgriculteurId();
    }

    private void updateNotificationBadge() {
        Integer idUser = getActiveUserId();
        if (idUser == null) return;

        javafx.concurrent.Task<Integer> task = new javafx.concurrent.Task<>() {
            @Override protected Integer call() {
                return notificationService.countUnread(idUser);
            }
        };

        task.setOnSucceeded(e -> {
            int unread = task.getValue();

            // ── Badge legacy ──────────────────────────────────────────────
            if (notificationBadge != null) {
                notificationBadge.setCount(unread);
                if (unread > 0) notificationBadge.pulse();
            }

            // ── 🔴 Cloche rouge animée ────────────────────────────────────
            if (notificationBell != null) {
                if (unread > 0) {
                    activateRedBell(unread);
                } else {
                    deactivateRedBell();
                }
            }
        });

        new Thread(task).start();
    }

    /** Active la cloche rouge avec animation pulse. */
    private void activateRedBell(int unread) {
        // Cloche rouge + fond rouge semi-transparent
        notificationBell.setStyle(
                "-fx-font-size: 22px; " +
                "-fx-text-fill: #ff4444; " +
                "-fx-effect: dropshadow(gaussian, rgba(255,50,50,0.9), 12, 0.6, 0, 0);"
        );
        if (notificationBellBg != null) {
            notificationBellBg.setFill(Color.web("#e74c3c", 0.3));
        }

        // Arrêter l'ancienne animation si active
        if (bellPulse != null) bellPulse.stop();

        // Animation : scale pulse + glow clignotant
        ScaleTransition scale = new ScaleTransition(Duration.millis(600), notificationBell);
        scale.setFromX(1.0); scale.setToX(1.25);
        scale.setFromY(1.0); scale.setToY(1.25);
        scale.setAutoReverse(true);
        scale.setCycleCount(Animation.INDEFINITE);
        scale.setInterpolator(Interpolator.EASE_BOTH);

        // Rotation légère (shake)
        RotateTransition shake = new RotateTransition(Duration.millis(120), notificationBell);
        shake.setFromAngle(-12); shake.setToAngle(12);
        shake.setAutoReverse(true);
        shake.setCycleCount(6);
        shake.play();

        scale.play();
        bellPulse = new Timeline(
            new KeyFrame(Duration.ZERO),
            new KeyFrame(Duration.seconds(4), kf -> {
                RotateTransition shake2 = new RotateTransition(Duration.millis(100), notificationBell);
                shake2.setFromAngle(-10); shake2.setToAngle(10);
                shake2.setAutoReverse(true);
                shake2.setCycleCount(6);
                shake2.play();
            })
        );
        bellPulse.setCycleCount(Animation.INDEFINITE);
        bellPulse.play();
    }

    /** Remet la cloche en état normal (vert/transparent). */
    private void deactivateRedBell() {
        if (bellPulse != null) { bellPulse.stop(); bellPulse = null; }
        notificationBell.setStyle("-fx-font-size: 22px; -fx-text-fill: rgba(255,255,255,0.9);");
        notificationBell.setScaleX(1.0); notificationBell.setScaleY(1.0);
        notificationBell.setRotate(0);
        if (notificationBellBg != null) notificationBellBg.setFill(Color.TRANSPARENT);
    }

    @FXML
    private void handleOpenNotifications() {
        try {
            Integer idUser = getActiveUserId();
            if (idUser != null) {
                UserSession session = UserSession.getInstance();
                String nomComplet = (session != null && session.getUser() != null)
                        ? session.getUser().getPrenom() + " " + session.getUser().getNom()
                        : "Utilisateur";
                AgriculteurContext.getInstance().setAgriculteur(idUser, nomComplet);
            }

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/gestionemploye/notification.fxml")
            );
            loader.setResources(LanguageManager.getInstance().getBundle());

            Stage stage = new Stage();
            stage.setTitle("🔔 Notifications - Ardhi");
            stage.setScene(new javafx.scene.Scene(loader.load(), 900, 700));
            stage.setResizable(true);
            LanguageManager.getInstance().applyOrientation(stage.getScene().getRoot());
            stage.setOnHidden(e -> updateNotificationBadge());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ══ 🌦️ MÉTÉO ══════════════════════════════════════════════════════════════

    /** Charge la météo en arrière-plan et met à jour le panneau. */
    private void loadWeatherAsync() {
        javafx.concurrent.Task<WeatherData> task = new javafx.concurrent.Task<>() {
            @Override protected WeatherData call() {
                return weatherService.getCurrentWeather();
            }
        };

        task.setOnSucceeded(e -> {
            WeatherData w = task.getValue();
            updateWeatherPanel(w);

            // Lancer l'analyse météo + notifications en arrière-plan
            Integer idUser = getActiveUserId();
            if (idUser != null) {
                new Thread(() -> notificationService.analyserMeteo(idUser)).start();
            }
        });

        task.setOnFailed(e -> {
            System.err.println("⚠️ Météo: " + task.getException().getMessage());
            if (lblMeteoDesc != null) lblMeteoDesc.setText("Météo indisponible");
        });

        new Thread(task).start();
    }

    /** Met à jour les labels du panneau météo avec les données reçues. */
    private void updateWeatherPanel(WeatherData w) {
        if (w == null || !w.isAvailable()) {
            String reason = (w != null && w.getDescription() != null)
                    ? w.getDescription()
                    : "Service météo indisponible";
            if (lblMeteoDesc  != null) lblMeteoDesc.setText("⚠ " + reason);
            if (lblMeteoIcone != null) lblMeteoIcone.setText("❓");
            if (lblTemperature!= null) lblTemperature.setText("--°C");
            return;
        }

        // Animation d'apparition du panneau
        if (weatherPanel != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(800), weatherPanel);
            ft.setFromValue(0); ft.setToValue(1); ft.play();
        }

        if (lblMeteoIcone   != null) lblMeteoIcone.setText(w.getWeatherEmoji());
        if (lblTemperature  != null) lblTemperature.setText((int) w.getTemperature() + "°C");
        if (lblMeteoDesc    != null) lblMeteoDesc.setText(capitalize(w.getDescription()));
        if (lblMeteoVille   != null) lblMeteoVille.setText("📍 " + w.getCityName());
        if (lblMeteoHumid   != null) lblMeteoHumid.setText("💧 Humidité : " + w.getHumidity() + "%");
        if (lblMeteoVent    != null) lblMeteoVent.setText("💨 Vent : " + (int) w.getWindSpeed() + " km/h");
        if (lblMeteoFeels   != null) lblMeteoFeels.setText("🌡️ Ressenti : " + (int) w.getFeelsLike() + "°C");

        // Couleur de fond dynamique selon la météo
        if (weatherPanel != null) {
            String bgColor;
            if (w.isRainExpected())          bgColor = "rgba(30,80,160,0.65)";
            else if (w.getTemperature() > 38) bgColor = "rgba(180,40,0,0.60)";
            else if (w.getTemperature() > 28) bgColor = "rgba(180,100,0,0.55)";
            else                              bgColor = "rgba(0,0,0,0.55)";

            weatherPanel.setStyle(
                "-fx-background-color: " + bgColor + "; " +
                "-fx-background-radius: 18; " +
                "-fx-border-color: rgba(255,255,255,0.2); " +
                "-fx-border-radius: 18; " +
                "-fx-padding: 16; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 15, 0, 0, 4);"
            );
        }

        // Section alertes agricoles
        buildWeatherAlerts(w);
    }

    /** Construit les cartes de recommandations positives/négatives dans le panneau météo. */
    private void buildWeatherAlerts(WeatherData w) {
        if (vboxAlertes == null) return;
        vboxAlertes.getChildren().clear();

        // Récupérer les tâches actives et générer les recommandations
        TacheService tacheService = new TacheService();
        java.util.List<Tache> taches = tacheService.getAllTaches();

        java.util.List<WeatherService.Recommandation> positives = new java.util.ArrayList<>();
        java.util.List<WeatherService.Recommandation> negatives = new java.util.ArrayList<>();

        for (Tache t : taches) {
            if (t.getStatut() != null &&
                (t.getStatut().equalsIgnoreCase("Terminé") || t.getStatut().equalsIgnoreCase("Validé"))) continue;
            for (WeatherService.Recommandation r : weatherService.analyserConditionsPourTache(t, w)) {
                if (r.isPositive()) positives.add(r);
                else negatives.add(r);
            }
        }

        // Séparateur : conditions générales
        if (w.isRainExpected())
            addAlertCard(vboxAlertes, "🌧️ Pluie prévue",
                "Traitements déconseillés • Irrigation non nécessaire", "#2980b9", WeatherService.NiveauReco.DANGER);
        if (w.getTemperature() > 38)
            addAlertCard(vboxAlertes, "🌡️ Chaleur : " + (int) w.getTemperature() + "°C",
                "Travaux tôt le matin uniquement", "#e67e22", WeatherService.NiveauReco.WARNING);
        if (w.getWindSpeed() > 40)
            addAlertCard(vboxAlertes, "💨 Vent fort : " + (int) w.getWindSpeed() + " km/h",
                "Pulvérisations déconseillées", "#8e44ad", WeatherService.NiveauReco.WARNING);

        // Section IA négative
        if (!negatives.isEmpty()) {
            Label hdr = new Label("🚨 Tâches à éviter aujourd'hui");
            hdr.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 6 0 2 0;");
            vboxAlertes.getChildren().add(hdr);
            for (WeatherService.Recommandation r : negatives) {
                String col = r.niveau == WeatherService.NiveauReco.DANGER ? "#e74c3c" : "#f39c12";
                addAlertCard(vboxAlertes,
                    (r.niveau == WeatherService.NiveauReco.DANGER ? "� " : "⚠️ ") + r.message.split(":"  )[0],
                    r.message, col, r.niveau);
            }
        }

        // Section IA positive
        if (!positives.isEmpty()) {
            Label hdr = new Label("🟢 Tâches recommandées aujourd'hui");
            hdr.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 6 0 2 0;");
            vboxAlertes.getChildren().add(hdr);
            for (WeatherService.Recommandation r : positives) {
                addAlertCard(vboxAlertes, "✅ " + extractFirstSentence(r.message), r.message, "#27ae60", r.niveau);
            }
        }

        // Tout va bien
        if (vboxAlertes.getChildren().isEmpty()) {
            Label ok = new Label("✅ Toutes conditions favorables pour les travaux agricoles");
            ok.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 11px; -fx-wrap-text: true;");
            ok.setWrapText(true);
            vboxAlertes.getChildren().add(ok);
        }
    }

    private String extractFirstSentence(String msg) {
        int dot = msg.indexOf(" : ");
        return dot > 0 ? msg.substring(0, dot + 3) : (msg.length() > 50 ? msg.substring(0, 50) + "…" : msg);
    }

    /** Surcharge acceptant NiveauReco — délègue à la version couleur. */
    private void addAlertCard(VBox container, String titre, String detail,
                              String color, WeatherService.NiveauReco niveau) {
        addAlertCard(container, titre, detail, color);
    }

    /** Crée une petite carte d'alerte colorée. */
    private void addAlertCard(VBox container, String titre, String detail, String color) {
        HBox card = new HBox(8);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(8, 10, 8, 10));
        card.setStyle(
            "-fx-background-color: " + color + "44; " +
            "-fx-border-color: " + color + "88; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8;"
        );

        // Barre colorée à gauche
        Rectangle bar = new Rectangle(4, 32);
        bar.setFill(Color.web(color));
        bar.setArcWidth(4); bar.setArcHeight(4);

        VBox textBox = new VBox(2);
        Label lblTitre = new Label(titre);
        lblTitre.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
        Label lblDetail = new Label(detail);
        lblDetail.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 10px;");
        lblDetail.setWrapText(true);
        textBox.getChildren().addAll(lblTitre, lblDetail);

        card.getChildren().addAll(bar, textBox);

        // Slide-in animation
        FadeTransition ft = new FadeTransition(Duration.millis(500), card);
        ft.setFromValue(0); ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(Duration.millis(400), card);
        tt.setFromX(-20); tt.setToX(0);

        new ParallelTransition(ft, tt).play();

        container.getChildren().add(card);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ══ Navigation ════════════════════════════════════════════════════════════

    @FXML void goToEmployeListFromCard(MouseEvent event)    { navigateToEmployes(event); }
    @FXML void goToEmployeListFromNavbar(ActionEvent event) { navigateToEmployes(event); }

    private void navigateToEmployes(javafx.event.Event event) {
        if (!checkRole()) return;
        WindowUtils.loadScene(event, "/fxml/gestionemploye/employe_list.fxml", "Ardhi - Gestion Employés");
    }

    @FXML void goToTacheListFromCard(MouseEvent event)    { navigateToTaches(event); }
    @FXML void goToTacheListFromNavbar(ActionEvent event) { navigateToTaches(event); }

    private void navigateToTaches(javafx.event.Event event) {
        if (!checkRole()) return;
        WindowUtils.loadScene(event, "/fxml/gestionemploye/tache_list.fxml", "Ardhi - Gestion Tâches");
    }

    @FXML void goToDiagnostic(ActionEvent event)  { WindowUtils.loadScene(event, "/fxml/UserAndDiag/ClientDashboard.fxml",          "Ardhi - Diagnostic IA"); }
    @FXML void goToMaintenance(ActionEvent event) { WindowUtils.loadScene(event, "/fxml/MaterielEtMaintenance/MaterielsEtMaintenance.fxml",    "Ardhi - Maintenance"); }
    @FXML void goToCultures(ActionEvent event)    { WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/AgriculteurDashboard.fxml",                 "Ardhi - Cultures"); }
    @FXML void goToEvents(ActionEvent event)      { WindowUtils.loadScene(event, "/fxml/Evenement/Navigationevenements.fxml",                 "Ardhi - Évènements"); }
    @FXML void goToMarketplace(ActionEvent event) { WindowUtils.loadScene(event, "/fxml/marketplace/Marketplace.fxml",                 "Ardhi - Marketplace"); }
    @FXML void goToMarketplaceFromCard(MouseEvent event) { WindowUtils.loadScene(event, "/fxml/marketplace/Marketplace.fxml",          "Ardhi - Marketplace"); }

    @FXML void goToDashboard(ActionEvent event) {
        UserSession session = UserSession.getInstance();
        if (session != null && session.getUser() != null) {
            Role userRole = session.getUser().getRole();
            if (userRole == Role.ADMIN) {
                WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Ardhi - Admin Dashboard");
            } else {
                WindowUtils.loadScene(event, "/fxml/LandingPage.fxml", "Ardhi - Accueil");
            }
        } else {
            WindowUtils.loadScene(event, "/fxml/LandingPage.fxml", "Ardhi - Accueil");
        }
    }

    @FXML void logout(ActionEvent event) {
        if (bellPulse != null) bellPulse.stop();
        UserSession.getInstance().logout();
        WindowUtils.loadScene(event, "/fxml/LandingPage.fxml", "Ardhi - Accueil");
    }

    // ══ Contrôle d'accès ══════════════════════════════════════════════════════

    private boolean checkRole() {
        UserSession session = UserSession.getInstance();
        if (session == null || session.getUser() == null) {
            showAccessDeniedAlert("Session expirée", "Veuillez vous reconnecter.");
            return false;
        }
        Role role = session.getUser().getRole();
        if (role != Role.ADMIN && role != Role.AGRICULTEUR) {
            showAccessDeniedAlert("Accès refusé",
                    "Ce service est réservé aux administrateurs et agriculteurs.\nVotre rôle : " + role);
            return false;
        }
        return true;
    }

    private void showAccessDeniedAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("🚫 Accès non autorisé");
        alert.setContentText(message);
        DialogPane dp = alert.getDialogPane();
        dp.setStyle("-fx-background-color: white; -fx-font-size: 14px; -fx-padding: 20px; " +
                    "-fx-border-color: #e74c3c; -fx-border-width: 2px; -fx-border-radius: 5px;");
        alert.showAndWait();
    }

    // ══ Langue ════════════════════════════════════════════════════════════════

    @FXML void switchToFrench(ActionEvent event)  { switchLanguage(LanguageManager.LOCALE_FR, event); }
    @FXML void switchToEnglish(ActionEvent event) { switchLanguage(LanguageManager.LOCALE_EN, event); }
    @FXML void switchToArabic(ActionEvent event)  { switchLanguage(LanguageManager.LOCALE_AR, event); }

    private void switchLanguage(Locale locale, ActionEvent event) {
        LanguageManager.getInstance().setLocale(locale);
        WindowUtils.loadScene(event, "/fxml/gestionemploye/Dashboard_employe_agriculture.fxml",
                "Ardhi - " + LanguageManager.getInstance().get("dashboard.role"));
    }

    private void highlightActiveLanguageButton() {
        String active   = LanguageManager.getInstance().getLocale().getLanguage();
        String btnOn    = "-fx-background-color: rgba(107,127,63,0.9); -fx-text-fill: white; -fx-background-radius: 15; -fx-cursor: hand; -fx-padding: 4 8; -fx-font-size: 14px; -fx-border-color: white; -fx-border-radius: 15;";
        String btnOff   = "-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; -fx-background-radius: 15; -fx-cursor: hand; -fx-padding: 4 8; -fx-font-size: 14px;";
        if (btnLangFr != null) btnLangFr.setStyle("fr".equals(active) ? btnOn : btnOff);
        if (btnLangEn != null) btnLangEn.setStyle("en".equals(active) ? btnOn : btnOff);
        if (btnLangAr != null) btnLangAr.setStyle("ar".equals(active) ? btnOn : btnOff);
    }
    @FXML
    private void ouvrirChatbot() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/gestionemploye/chatbot.fxml")
            );

            Stage stage = new Stage();
            stage.setTitle("🤖 Assistant RH Ardhi - Intelligence Artificielle");
            stage.setScene(new Scene(loader.load(), 900, 700));
            stage.setResizable(true);

            // Icône de la fenêtre (optionnel)
            // stage.getIcons().add(new Image(getClass().getResourceAsStream("/img/chatbot-icon.png")));

            // Effet d'apparition
            stage.setOpacity(0);
            stage.show();

            Timeline fadeIn = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(stage.opacityProperty(), 0)),
                    new KeyFrame(Duration.millis(300), new KeyValue(stage.opacityProperty(), 1))
            );
            fadeIn.play();

            System.out.println("✅ Chatbot ouvert");

        } catch (IOException e) {
            e.printStackTrace();
            NotificationToast.showNotification(
                    "❌ Erreur ouverture chatbot",
                    NotificationToast.ERROR,
                    2000
            );
        }
    }
    private Parent loadFXMLWithResources(String fxmlPath) throws IOException {
        try {
            ResourceBundle b = ResourceBundle.getBundle("messages", Locale.FRENCH);
            return new FXMLLoader(getClass().getResource(fxmlPath), b).load();
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement ressources: " + fxmlPath);
            return new FXMLLoader(getClass().getResource(fxmlPath)).load();
        }
    }
    // Ajoutez cette méthode
    @FXML
    void ouvrirChatbot(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/gestionemploye/chatbot.fxml")
            );

            Stage stage = new Stage();
            stage.setTitle("🤖 Assistant RH Ardhi");
            stage.setScene(new javafx.scene.Scene(loader.load(), 900, 700));
            stage.setResizable(true);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            NotificationToast.showNotification(
                    "❌ Erreur ouverture chatbot",
                    NotificationToast.ERROR,
                    2000
            );
        }
    }

}