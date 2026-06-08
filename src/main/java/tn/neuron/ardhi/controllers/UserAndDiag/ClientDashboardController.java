package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.FlowPane;
import tn.neuron.ardhi.models.UserAndDiag.User;

import tn.neuron.ardhi.models.UserAndDiag.UserBadge;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;
import tn.neuron.ardhi.utils.UserAndDiag.SubscriptionConfig;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import tn.neuron.ardhi.services.UserAndDiag.WeatherService;
import tn.neuron.ardhi.services.UserAndDiag.EpidemicAlertService;
import tn.neuron.ardhi.services.UserAndDiag.WeatherAlertService;
import tn.neuron.ardhi.services.UserAndDiag.LocationService;
import tn.neuron.ardhi.services.UserAndDiag.GamificationService;
import tn.neuron.ardhi.services.UserAndDiag.UserBadgeService;
import tn.neuron.ardhi.models.UserAndDiag.Offre;
import tn.neuron.ardhi.services.UserAndDiag.AbonnementService;

import java.sql.SQLException;

public class ClientDashboardController implements Initializable {

    @FXML
    private Label lblBienvenue;

    // New Navigation Menu
    @FXML
    private HBox navMenu;
    @FXML
    private javafx.scene.control.Button btnNavDiagnostics;
    @FXML
    private javafx.scene.control.Button btnNavMarketplace;
    @FXML
    private javafx.scene.control.Button btnNavMaintenance;
    @FXML
    private javafx.scene.control.Button btnNavCultures;
    @FXML
    private javafx.scene.control.Button btnNavEvents;
    @FXML
    private javafx.scene.control.Button btnNavEmployees;

    @FXML
    private Label lblWeatherIcon;
    @FXML
    private Label lblTemperature;
    @FXML
    private Label lblCondition;
    @FXML
    private Label lblHumidity;
    @FXML
    private Label lblWind;
    @FXML
    private Label lblFeelsLike;
    @FXML
    private Label lblAdvice;

    // Gamification UI
    @FXML
    private Label lblLevel;
    @FXML
    private ProgressBar barLevel;
    @FXML
    private Label lblPoints;
    @FXML
    private VBox leaderboardContainer;
    @FXML
    private FlowPane badgesContainer;

    // Epidemic Intelligence & Weather Contextualization
    @FXML
    private VBox regionalAlertsContainer;
    @FXML
    private VBox regionalAlertsContent; // The content wrapper to collapse
    @FXML
    private Label btnToggleRegional; // The arrow button
    @FXML
    private Slider sliderRadius; // Radius slider

    @FXML
    private VBox predictiveWeatherContainer;
    @FXML
    private VBox predictiveWeatherContent; // The content wrapper to collapse
    @FXML
    private Label btnTogglePredictive; // The arrow button
    @FXML
    private Slider sliderTimeframe; // Timeframe slider

    private final WeatherService weatherService = new WeatherService();
    private final EpidemicAlertService epidemicService = new EpidemicAlertService();
    private final WeatherAlertService weatherAlertService = new WeatherAlertService();
    private final LocationService locationService = new LocationService();
    private final GamificationService gamificationService = new GamificationService();
    private final UserBadgeService userBadgeService = new UserBadgeService();
    private final AbonnementService abonnementService = new AbonnementService();

    // Cached user location (detected once, reused)
    private double userLat = 36.8065;
    private double userLon = 10.1815;
    private boolean locationDetected = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        UserSession session = UserSession.getInstance();
        User currentUser = (session != null) ? session.getUser() : null;

        if (currentUser != null) {
            lblBienvenue.setText("Bonjour, " + currentUser.getPrenom() + " " + currentUser.getNom());

            // Initialize Radius Slider
            if (sliderRadius != null) {
                sliderRadius.setMin(10);
                sliderRadius.setMax(100);
                sliderRadius.setValue(25);
                // Reload on release to avoid API spam
                sliderRadius.setOnMouseReleased(e -> loadRegionalAlerts());
            }

            // Initialize Timeframe Slider
            if (sliderTimeframe != null) {
                sliderTimeframe.setMin(24);
                sliderTimeframe.setMax(72);
                sliderTimeframe.setValue(72);
                sliderTimeframe.setOnMouseReleased(e -> loadPredictiveWeather());
            }

            // Load Data
            loadWeatherData();
            loadGamificationData(currentUser);
        }
    }

    @FXML
    void goToProfil(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/Profil.fxml", "Mon Profil");
    }

    @FXML
    void goToAbonnement(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/Sabonner.fxml", "Nos Offres Premium");
    }

    @FXML
    void goToDiagnostics(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/UserDiagnostic.fxml", "Diagnostic IA de vos Plantes");
    }

    @FXML
    void goToTreatments(javafx.event.Event event) {
        try {
            int userId = UserSession.getInstance().getUser().getId();
            Offre offre = abonnementService.getOffreActiveParUser(userId);

            boolean hasAccess = false;
            if (offre != null && offre.isAccesPlanTraitement()) {
                hasAccess = true;
            } else if (offre == null && SubscriptionConfig.isFreeAccesPlanTraitement()) {
                hasAccess = true;
            }

            if (!hasAccess) {
                WindowUtils.showAlert("Accès Limité",
                        "Les protocoles de soin IA sont réservés aux membres Premium.\n" +
                                "Passez à l'étape supérieure pour soigner vos plantes avec Ardhi !");
                return;
            }
        } catch (Exception e) {
            // Allow if check fails
        }
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/TreatmentPlanList.fxml", "Mes Protocoles de Soin");
    }

    @FXML
    void goToHistoriqueDiagnostics(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/DiagnosticHistory.fxml", "Historique des Diagnostics");
    }

    @FXML
    void goToCommunity(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/CommunityFeed.fxml", "Communauté Ardhi");
    }

    @FXML
    void logout(ActionEvent event) {
        UserSession.getInstance().cleanUserSession();
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/Login.fxml", "Connexion");
    }

    @FXML
    void goToMap(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/DiseaseMap.fxml", "Carte des Maladies");
    }

    @FXML
    void goToExpertDashboard(javafx.event.Event event) {
        User user = UserSession.getInstance().getUser();
        if (user == null || user.getRole() != tn.neuron.ardhi.models.UserAndDiag.Role.AGRONOME) {
            WindowUtils.showAlert("Accès réservé", "Cette section est réservée aux agronomes experts.");
            return;
        }
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/ExpertDashboard.fxml", "Tableau Expert");
    }

    @FXML
    void goToHealthScan(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/HealthScanForm.fxml", "Scan Santé du Champ");
    }

    @FXML
    void goToVirtualSoil(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/VirtualSoil3D.fxml", "Profil de Sol 3D Virtuel");
    }

    @FXML
    void goToHealthScanHistory(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/HealthScanHistory.fxml", "Historique de vos Scans Santé");
    }

    @FXML
    void goToLandingPage(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/LandingPage.fxml", "Ardhi - Accueil");
    }

    @FXML
    void goToMarketplace(javafx.event.Event event) { // Changed to Event to support both Mouse and Action
        WindowUtils.loadScene(event, "/fxml/marketplace/Marketplace.fxml", "Ardhi - Marketplace");
    }

    @FXML
    void goToMaintenance(javafx.event.Event event) {
        WindowUtils.loadScene(event, "/fxml/MaterielEtMaintenance/MaterielsEtMaintenance.fxml",
                "Ardhi - Matériels & Maintenance");
    }

    @FXML
    void goToCultures(javafx.event.Event event) {
        WindowUtils.loadScene(event, "/fxml/Parcelles/AgriculteurCultures.fxml", "Ardhi - Mes Cultures");
    }

    @FXML
    void goToEvents(javafx.event.Event event) {
        WindowUtils.loadScene(event, "/fxml/Evenement/Navigationevenements.fxml", "Ardhi - Évènements");
    }

    @FXML
    void goToEmployees(javafx.event.Event event) {
        WindowUtils.loadScene(event, "/fxml/gestionemploye/Dashboard_employe_agriculture.fxml", "Ardhi - Gestion des Employés");
    }

    private void loadWeatherData() {
        new Thread(() -> {
            // Detect user location for weather and epidemic features
            LocationService.LocationData loc = locationService.detectLocation();
            if (loc != null && loc.latitude != null && loc.longitude != null) {
                userLat = loc.latitude;
                userLon = loc.longitude;
                locationDetected = true;
            }

            WeatherService.WeatherData data = weatherService.getCurrentWeather(userLat, userLon);
            if (data != null) {
                Platform.runLater(() -> {
                    lblWeatherIcon.setText(data.getWeatherIcon());
                    lblTemperature.setText(data.temperature + " °C");
                    lblHumidity.setText(
                            "💧 Humidité: " + data.humidity + "% | 🌧️ Pluie: " + data.precipitation + "mm");
                    lblWind.setText(String.format("💨 Vent: %.1f km/h", data.windSpeed));
                    lblFeelsLike.setText(String.format("🌡️ Ressenti: %.1f °C", data.apparentTemperature));

                    String shortLocation = "";
                    if (locationDetected && loc != null && loc.label != null) {
                        shortLocation = loc.label.split(",")[0].trim();
                    }

                    if (!shortLocation.isEmpty()) {
                        lblCondition.setText(data.getWeatherDescription() + " | " + shortLocation);
                    } else {
                        lblCondition.setText(data.getWeatherDescription());
                    }
                    lblAdvice.setText(data.advice);
                });
            } else {
                Platform.runLater(() -> {
                    lblCondition.setText("Indisponible");
                    lblAdvice.setText("Impossible de récupérer les données météo.");
                });
            }

            // After weather loads, load epidemic and predictive features
            loadRegionalAlerts();
            loadPredictiveWeather();
        }).start();
    }

    /**
     * Loads regional epidemic alerts and active diseases into the dashboard widget.
     */
    private void loadRegionalAlerts() {
        try {
            double radiusKm = 25.0;
            if (sliderRadius != null) {
                radiusKm = sliderRadius.getValue();
            }

            final double finalRadius = radiusKm;
            List<EpidemicAlertService.RegionalDisease> diseases = epidemicService.getActiveDiseases(userLat, userLon,
                    finalRadius);
            List<EpidemicAlertService.RegionalAlert> alerts = epidemicService.getRegionalAlerts(userLat, userLon,
                    finalRadius, 14);
            int[] stats = epidemicService.getRegionalStats(userLat, userLon, finalRadius, 14);

            Platform.runLater(() -> {
                if (regionalAlertsContainer == null)
                    return;
                regionalAlertsContainer.getChildren().clear();

                if (diseases.isEmpty() && alerts.isEmpty()) {
                    Label noAlerts = new Label(
                            String.format("✅ Aucune alerte dans votre région (rayon %.0fkm)", finalRadius));
                    noAlerts.setStyle("-fx-text-fill: white; -fx-font-size: 13;");
                    regionalAlertsContainer.getChildren().add(noAlerts);
                    return;
                }

                // Stats summary
                if (stats[1] > 0) {
                    Label statsLabel = new Label(String.format(
                            "📊 %d maladie(s) détectée(s) | %d signalement(s) dans un rayon de %.0fkm ces 14 derniers jours",
                            stats[0], stats[1], finalRadius));
                    statsLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 11;");
                    statsLabel.setWrapText(true);
                    regionalAlertsContainer.getChildren().add(statsLabel);
                }

                // Active diseases dashboard
                for (EpidemicAlertService.RegionalDisease d : diseases) {
                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setStyle(
                            "-fx-background-color: rgba(255,255,255,0.15); -fx-background-radius: 8; -fx-padding: 8 12;");

                    Label icon = new Label(d.getIcon());
                    icon.setStyle("-fx-font-size: 16;");

                    Label name = new Label(d.diseaseName);
                    name.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12;");
                    name.setPrefWidth(180);

                    Label level = new Label(d.severityLevel);
                    level.setStyle("-fx-text-fill: white; -fx-font-size: 11; -fx-background-color: " +
                            ("Élevé".equals(d.severityLevel) ? "rgba(231,76,60,0.7)"
                                    : "Modéré".equals(d.severityLevel) ? "rgba(243,156,18,0.7)" : "rgba(39,174,96,0.7)")
                            +
                            "; -fx-background-radius: 5; -fx-padding: 2 8;");

                    Label dist = new Label(String.format("%.1fkm", d.nearestDistanceKm));
                    dist.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 11;");

                    row.getChildren().addAll(icon, name, level, dist);
                    regionalAlertsContainer.getChildren().add(row);
                }

                // Alert messages
                for (EpidemicAlertService.RegionalAlert alert : alerts) {
                    Label alertLabel = new Label(alert.message);
                    alertLabel.setWrapText(true);
                    alertLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 11; -fx-padding: 4 0 0 0;");
                    regionalAlertsContainer.getChildren().add(alertLabel);
                }
            });
        } catch (Exception e) {
            LogUtils.error(ClientDashboardController.class, "Error loading regional alerts", e);
        }
    }

    /**
     * Loads predictive weather-based disease risk alerts and treatment timing.
     */
    private void loadPredictiveWeather() {
        try {
            int timeframeHours = 72;
            if (sliderTimeframe != null) {
                timeframeHours = (int) sliderTimeframe.getValue();
            }

            final int finalHours = timeframeHours;
            List<WeatherAlertService.DiseaseRiskAlert> risks = weatherAlertService.getDiseaseRiskAlerts(userLat,
                    userLon, finalHours);
            WeatherAlertService.TreatmentTimingSuggestion timing = weatherAlertService.getTreatmentTiming(userLat,
                    userLon);

            Platform.runLater(() -> {
                if (predictiveWeatherContainer == null)
                    return;
                predictiveWeatherContainer.getChildren().clear();

                if (risks.isEmpty()) {
                    Label noRisks = new Label(
                            String.format("✅ Aucun risque phytosanitaire élevé détecté pour les %d prochaines heures",
                                    finalHours));
                    noRisks.setStyle("-fx-text-fill: white; -fx-font-size: 13;");
                    noRisks.setWrapText(true);
                    predictiveWeatherContainer.getChildren().add(noRisks);
                } else {
                    for (WeatherAlertService.DiseaseRiskAlert risk : risks) {
                        VBox riskCard = new VBox(4);
                        riskCard.setStyle(
                                "-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 8; -fx-padding: 8 12;");

                        HBox header = new HBox(8);
                        header.setAlignment(Pos.CENTER_LEFT);
                        Label riskIcon = new Label(risk.getIcon());
                        riskIcon.setStyle("-fx-font-size: 14;");
                        Label riskType = new Label(risk.diseaseType + " — " + risk.riskLevel);
                        riskType.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12;");
                        header.getChildren().addAll(riskIcon, riskType);

                        Label reason = new Label(risk.reason);
                        reason.setWrapText(true);
                        reason.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 11;");

                        Label advice = new Label("💡 " + risk.advice);
                        advice.setWrapText(true);
                        advice.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 11;");

                        riskCard.getChildren().addAll(header, reason, advice);
                        predictiveWeatherContainer.getChildren().add(riskCard);
                    }
                }

                // Treatment timing
                if (timing != null && timing.overallAdvice != null) {
                    Label timingLabel = new Label("⏱️ " + timing.overallAdvice);
                    timingLabel.setWrapText(true);
                    timingLabel
                            .setStyle("-fx-text-fill: rgba(255,255,255,0.9); -fx-font-size: 12; -fx-padding: 6 0 0 0;");
                    predictiveWeatherContainer.getChildren().add(timingLabel);
                }
            });
        } catch (Exception e) {
            LogUtils.error(ClientDashboardController.class, "Error loading predictive weather", e);
        }
    }

    private void loadGamificationData(User user) {
        new Thread(() -> {
            // 1. Fetch fresh stats for current user
            User freshUser = gamificationService.getUserStats(user.getId());

            // 2. Fetch Leaderboard
            List<User> topUsers = gamificationService.getLeaderboard(5);

            // 3. Fetch User Badges
            List<UserBadge> userBadges = null;
            try {
                userBadges = userBadgeService.getBadgesForUser(user.getId());
            } catch (SQLException e) {
                e.printStackTrace();
            }
            final List<UserBadge> finalUserBadges = userBadges;

            Platform.runLater(() -> {
                // Update User Stats UI
                lblPoints.setText(freshUser.getPoints() + " pts");
                lblLevel.setText("Niveau " + freshUser.getLevel());

                // Progress bar logic (e.g., assumes 500 pts per level)
                // Level 1: 0-500, Level 2: 500-1000
                int pointsForCurrentLevel = freshUser.getPoints() % 500;
                barLevel.setProgress(pointsForCurrentLevel / 500.0);

                // Update Badges UI
                if (badgesContainer != null) {
                    badgesContainer.getChildren().clear();
                    if (finalUserBadges == null || finalUserBadges.isEmpty()) {
                        Label placeholder = new Label("Aucun badge");
                        placeholder.setStyle("-fx-text-fill: #aaa; -fx-font-style: italic; -fx-font-size: 11;");
                        badgesContainer.getChildren().add(placeholder);
                    } else {
                        for (UserBadge ub : finalUserBadges) {
                            badgesContainer.getChildren().add(createBadgeNode(ub));
                        }
                    }
                }

                // Update Leaderboard UI
                leaderboardContainer.getChildren().clear();
                int rank = 1;
                for (User u : topUsers) {
                    HBox row = createLeaderboardRow(rank++, u);
                    leaderboardContainer.getChildren().add(row);
                }
            });
        }).start();
    }

    private VBox createBadgeNode(UserBadge ub) {
        VBox badgeBox = new VBox();
        badgeBox.setAlignment(javafx.geometry.Pos.CENTER);
        badgeBox.setSpacing(2);
        badgeBox.setStyle(
                "-fx-padding: 8; -fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 1);");
        badgeBox.setPrefWidth(70);

        Label iconLabel = new Label(ub.getBadgeIcon());
        iconLabel.setStyle("-fx-font-size: 24; -fx-text-fill: white;");

        Label nameLabel = new Label(ub.getBadgeName());
        nameLabel.setStyle(
                "-fx-font-size: 10; -fx-font-weight: bold; -fx-text-fill: white;");
        nameLabel.setWrapText(true);
        nameLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        badgeBox.getChildren().addAll(iconLabel, nameLabel);

        Tooltip tooltip = new Tooltip(
                ub.getBadgeName() + "\n" + (ub.getBadgeDescription() != null ? ub.getBadgeDescription() : ""));
        Tooltip.install(badgeBox, tooltip);

        return badgeBox;
    }

    private HBox createLeaderboardRow(int rank, User u) {
        HBox row = new HBox();
        row.setSpacing(15);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle(
                "-fx-padding: 12; -fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 1);");

        Label lblRank = new Label((rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : "#" + rank));
        lblRank.setMinWidth(30);
        lblRank.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: white;");

        Label lblName = new Label(u.getPrenom() + " " + u.getNom());
        lblName.setStyle(
                "-fx-font-weight: bold; -fx-text-fill: white;");
        lblName.setPrefWidth(200);

        Label lblScore = new Label(u.getPoints() + " pts (Niv. " + u.getLevel() + ")");
        lblScore.setStyle(
                "-fx-text-fill: #a9dfbf; -fx-font-weight: bold;");

        row.getChildren().addAll(lblRank, lblName, lblScore);
        return row;
    }

    @FXML
    void toggleRegional(MouseEvent event) {
        if (regionalAlertsContent.isVisible()) {
            regionalAlertsContent.setVisible(false);
            regionalAlertsContent.setManaged(false);
            btnToggleRegional.setText("▼"); // Point down when collapsed
        } else {
            regionalAlertsContent.setVisible(true);
            regionalAlertsContent.setManaged(true);
            btnToggleRegional.setText("▲"); // Point up when expanded
        }
    }

    @FXML
    void togglePredictive(MouseEvent event) {
        if (predictiveWeatherContent.isVisible()) {
            predictiveWeatherContent.setVisible(false);
            predictiveWeatherContent.setManaged(false);
            btnTogglePredictive.setText("▼");
        } else {
            predictiveWeatherContent.setVisible(true);
            predictiveWeatherContent.setManaged(true);
            btnTogglePredictive.setText("▲");
        }
    }

}