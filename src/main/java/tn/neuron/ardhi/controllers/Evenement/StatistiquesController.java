package tn.neuron.ardhi.controllers.Evenement;

import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.neuron.ardhi.models.Evenement.Evenement;
import tn.neuron.ardhi.models.Evenement.Participation;
import tn.neuron.ardhi.services.Evenement.EvenementService;
import tn.neuron.ardhi.services.Evenement.ParticipationService;
import tn.neuron.ardhi.services.Evenement.StatistiquesService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrôleur amélioré pour les statistiques avec design moderne et animations
 */
public class StatistiquesController implements Initializable {

    // Cartes de statistiques
    @FXML private Label totalEvenementsLabel;
    @FXML private Label totalParticipationsLabel;
    @FXML private Label tauxPresenceLabel;
    @FXML private Label noteMoyenneLabel;
    @FXML private Label tauxAnnulationLabel;

    // Graphiques principaux
    @FXML private PieChart typesChart;
    @FXML private BarChart<String, Number> participantsChart;
    @FXML private AreaChart<String, Number> tendanceChart;
    @FXML private BarChart<String, Number> notesChart;

    // Top événements
    @FXML private VBox topEventsContainer;

    // Distribution des statuts
    @FXML private VBox statutsContainer;

    private EvenementService evenementService;
    private ParticipationService participationService;
    private StatistiquesService statistiquesService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        evenementService = new EvenementService();
        participationService = new ParticipationService();
        statistiquesService = new StatistiquesService();

        loadStatistics();
        loadCharts();
        loadTopEvents();
        loadStatutsDistribution();
    }

    private void loadStatistics() {
        List<Evenement> allEvents = evenementService.getAllEvenements();
        List<Participation> allParticipations = participationService.getAllParticipations();

        // Total événements avec animation
        animateNumber(totalEvenementsLabel, 0, allEvents.size());

        // Total participations
        animateNumber(totalParticipationsLabel, 0, allParticipations.size());

        // Taux de présence
        long presents = allParticipations.stream()
                .filter(p -> "PRESENT".equals(p.getStatut()))
                .count();
        long confirmes = allParticipations.stream()
                .filter(p -> "CONFIRME".equals(p.getStatut()) || "PRESENT".equals(p.getStatut()))
                .count();

        double tauxPresence = confirmes > 0 ? (presents * 100.0 / confirmes) : 0;
        tauxPresenceLabel.setText(String.format("%.1f%%", tauxPresence));

        // Note moyenne globale
        double noteMoyenne = allParticipations.stream()
                .filter(p -> p.getNote() > 0)
                .mapToDouble(Participation::getNote)
                .average()
                .orElse(0.0);
        noteMoyenneLabel.setText(String.format("%.1f ⭐", noteMoyenne));

        // Taux d'annulation
        long annules = allParticipations.stream()
                .filter(p -> "ANNULE".equals(p.getStatut()))
                .count();
        double tauxAnnulation = allParticipations.size() > 0 ?
                (annules * 100.0 / allParticipations.size()) : 0;
        tauxAnnulationLabel.setText(String.format("%.1f%%", tauxAnnulation));
    }

    private void loadCharts() {
        loadTypesChart();
        loadParticipantsChart();
        loadTendanceChart();
        loadNotesChart();
    }

    /**
     * Graphique camembert moderne avec couleurs personnalisées
     */
    private void loadTypesChart() {
        List<Evenement> allEvents = evenementService.getAllEvenements();

        Map<String, Long> typeCount = allEvents.stream()
                .collect(Collectors.groupingBy(Evenement::getType, Collectors.counting()));

        List<PieChart.Data> pieData = new ArrayList<>();
        typeCount.forEach((type, count) -> {
            PieChart.Data data = new PieChart.Data(
                    getTypeLabel(type) + " (" + count + ")",
                    count
            );
            pieData.add(data);
        });

        typesChart.setData(FXCollections.observableArrayList(pieData));
        typesChart.setTitle("Répartition par Type d'Événement");
        typesChart.setLegendVisible(true);
        typesChart.setLabelsVisible(true);
        typesChart.setStartAngle(90);

        // Ajouter tooltips
        pieData.forEach(data -> {
            Tooltip tooltip = new Tooltip(
                    data.getName() + "\n" +
                            String.format("%.1f%%", (data.getPieValue() / allEvents.size() * 100))
            );
            Tooltip.install(data.getNode(), tooltip);
        });
    }

    /**
     * Graphique barres colorées pour les participants
     */
    private void loadParticipantsChart() {
        List<Evenement> allEvents = evenementService.getAllEvenements();

        // Top 10 événements par participants
        List<Evenement> topEvents = allEvents.stream()
                .sorted(Comparator.comparingInt(Evenement::getNombreParticipants).reversed())
                .limit(10)
                .collect(Collectors.toList());

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Participants");

        for (Evenement event : topEvents) {
            String label = event.getTitre().length() > 25 ?
                    event.getTitre().substring(0, 22) + "..." : event.getTitre();
            XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(label, event.getNombreParticipants());
            series.getData().add(dataPoint);
        }

        participantsChart.getData().clear();
        participantsChart.getData().add(series);
        participantsChart.setTitle("Top 10 - Événements les Plus Populaires");
        participantsChart.setLegendVisible(false);
    }

    /**
     * Graphique en aires pour la tendance (plus moderne qu'une ligne)
     */
    private void loadTendanceChart() {
        List<Evenement> allEvents = evenementService.getAllEvenements();

        // Grouper par mois des 12 derniers mois
        Map<String, Long> eventsByMonth = new TreeMap<>();

        for (int i = 11; i >= 0; i--) {
            java.time.LocalDate date = java.time.LocalDate.now().minusMonths(i);
            String monthKey = date.format(DateTimeFormatter.ofPattern("MMM yyyy"));

            long count = allEvents.stream()
                    .filter(e -> e.getDateDebut().getYear() == date.getYear() &&
                            e.getDateDebut().getMonthValue() == date.getMonthValue())
                    .count();

            eventsByMonth.put(monthKey, count);
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Événements créés");

        eventsByMonth.forEach((month, count) ->
                series.getData().add(new XYChart.Data<>(month, count))
        );

        tendanceChart.getData().clear();
        tendanceChart.getData().add(series);
        tendanceChart.setTitle("Tendance de Création (12 derniers mois)");
        tendanceChart.setCreateSymbols(true);
    }

    /**
     * Nouveau: Graphique de distribution des notes
     */
    private void loadNotesChart() {
        List<Participation> allParticipations = participationService.getAllParticipations();

        Map<Integer, Long> notesDistribution = new TreeMap<>();
        for (int i = 1; i <= 5; i++) {
            final int note = i;
            long count = allParticipations.stream()
                    .filter(p -> p.getNote() == note)
                    .count();
            notesDistribution.put(i, count);
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Nombre d'avis");

        notesDistribution.forEach((note, count) ->
                series.getData().add(new XYChart.Data<>(note + " ⭐", count))
        );

        notesChart.getData().clear();
        notesChart.getData().add(series);
        notesChart.setTitle("Distribution des Évaluations");
        notesChart.setLegendVisible(false);
    }

    /**
     * Nouveau: Top 5 événements avec détails
     */
    private void loadTopEvents() {
        topEventsContainer.getChildren().clear();

        List<Map<String, Object>> topEvents = statistiquesService.getTopEvenements(5);

        int rank = 1;
        for (Map<String, Object> eventData : topEvents) {
            VBox eventCard = createTopEventCard(rank, eventData);
            topEventsContainer.getChildren().add(eventCard);
            rank++;
        }
    }

    private VBox createTopEventCard(int rank, Map<String, Object> data) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; " +
                "-fx-background-radius: 10; -fx-border-color: #e0e0d0; " +
                "-fx-border-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        // Rang et titre
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label rankLabel = new Label(String.valueOf(rank));
        rankLabel.setStyle("-fx-background-color: #6b7a4f; -fx-text-fill: white; " +
                "-fx-font-size: 18px; -fx-font-weight: bold; " +
                "-fx-padding: 8 12; -fx-background-radius: 50%;");

        VBox infoBox = new VBox(3);
        Label titleLabel = new Label((String) data.get("titre"));
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Label typeLabel = new Label((String) data.get("type"));
        typeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        infoBox.getChildren().addAll(titleLabel, typeLabel);

        header.getChildren().addAll(rankLabel, infoBox);

        // Métriques
        HBox metrics = new HBox(20);
        metrics.setAlignment(Pos.CENTER_LEFT);

        VBox participantsBox = new VBox(2);
        participantsBox.setAlignment(Pos.CENTER);
        Label partLabel = new Label(data.get("participants").toString());
        partLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #4a90e2;");
        Label partText = new Label("participants");
        partText.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");
        participantsBox.getChildren().addAll(partLabel, partText);

        VBox remplissageBox = new VBox(2);
        remplissageBox.setAlignment(Pos.CENTER);
        Label tauxLabel = new Label(String.format("%.0f%%", data.get("tauxRemplissage")));
        tauxLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #50c878;");
        Label tauxText = new Label("remplissage");
        tauxText.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");
        remplissageBox.getChildren().addAll(tauxLabel, tauxText);

        VBox noteBox = new VBox(2);
        noteBox.setAlignment(Pos.CENTER);
        Label noteLabel = new Label(String.format("%.1f ⭐", data.get("noteMoyenne")));
        noteLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #f5a623;");
        Label noteText = new Label("note moyenne");
        noteText.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");
        noteBox.getChildren().addAll(noteLabel, noteText);

        metrics.getChildren().addAll(participantsBox, remplissageBox, noteBox);

        card.getChildren().addAll(header, metrics);

        // Animation au survol
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.02);
            st.setToY(1.02);
            st.play();
        });

        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        return card;
    }

    /**
     * Nouveau: Distribution des statuts avec barres de progression
     */
    private void loadStatutsDistribution() {
        statutsContainer.getChildren().clear();

        List<Participation> allParticipations = participationService.getAllParticipations();
        int total = allParticipations.size();

        Map<String, StatutInfo> statuts = new LinkedHashMap<>();
        statuts.put("CONFIRME", new StatutInfo("Confirmés", "#50c878", "CONFIRME"));
        statuts.put("EN_ATTENTE", new StatutInfo("En attente", "#f5a623", "EN_ATTENTE"));
        statuts.put("PRESENT", new StatutInfo("Présents", "#4a90e2", "PRESENT"));
        statuts.put("ANNULE", new StatutInfo("Annulés", "#d4145a", "ANNULE"));

        statuts.forEach((key, info) -> {
            long count = allParticipations.stream()
                    .filter(p -> info.statut.equals(p.getStatut()))
                    .count();

            VBox statutBox = createStatutBar(info.label, count, total, info.color);
            statutsContainer.getChildren().add(statutBox);
        });
    }

    private VBox createStatutBar(String label, long count, int total, String color) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(10));

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(header, javafx.scene.layout.Priority.ALWAYS);

        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        double percentage = total > 0 ? (count * 100.0 / total) : 0;
        Label countLabel = new Label(String.format("%d (%.1f%%)", count, percentage));
        countLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        header.getChildren().addAll(nameLabel, spacer, countLabel);

        ProgressBar progressBar = new ProgressBar(percentage / 100.0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: " + color + ";");

        box.getChildren().addAll(header, progressBar);

        return box;
    }

    /**
     * Animation des nombres
     */
    private void animateNumber(Label label, int start, int end) {
        javafx.animation.Timeline timeline = new javafx.animation.Timeline();

        int steps = 30;
        for (int i = 0; i <= steps; i++) {
            final int value = start + (end - start) * i / steps;
            javafx.animation.KeyFrame frame = new javafx.animation.KeyFrame(
                    Duration.millis(i * 20),
                    e -> label.setText(String.valueOf(value))
            );
            timeline.getKeyFrames().add(frame);
        }

        timeline.play();
    }

    private String getTypeLabel(String type) {
        return switch (type) {
            case "FOIRE" -> "Foires";
            case "FORMATION" -> "Formations";
            case "CONFERENCE" -> "Conférences";
            case "ATELIER" -> "Ateliers";
            default -> type;
        };
    }

    @FXML
    private void handleRefresh() {
        loadStatistics();
        loadCharts();
        loadTopEvents();
        loadStatutsDistribution();
    }

    @FXML
    private void handleRetour(javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/Evenement/NavigationEvenements.fxml")
            );
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            stage.setScene(new Scene(
                    root,
                    WindowUtils.APP_WIDTH,
                    WindowUtils.APP_HEIGHT
            ));
            stage.setTitle("Ardhi - Module Événements");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Classe helper pour les statuts
    private static class StatutInfo {
        String label;
        String color;
        String statut;

        StatutInfo(String label, String color, String statut) {
            this.label = label;
            this.color = color;
            this.statut = statut;
        }
    }
}