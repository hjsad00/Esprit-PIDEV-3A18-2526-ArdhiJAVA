package tn.neuron.ardhi.controllers.gestionemployecontroller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.neuron.ardhi.services.gestionemployeservice.PerformanceService;
import tn.neuron.ardhi.services.gestionemployeservice.PerformanceService.PerformanceData;
import tn.neuron.ardhi.utils.gestionemployeutils.AgriculteurContext;
import tn.neuron.ardhi.utils.gestionemployeutils.NotificationToast;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 📊 Contrôleur pour afficher les performances des employés
 */
public class PerformanceController implements Initializable {

    @FXML private TableView<PerformanceData> tablePerformance;
    @FXML private TableColumn<PerformanceData, String> colNom;
    @FXML private TableColumn<PerformanceData, Integer> colTotalTaches;
    @FXML private TableColumn<PerformanceData, Integer> colTerminees;
    @FXML private TableColumn<PerformanceData, Integer> colEnRetard;
    @FXML private TableColumn<PerformanceData, Void> colScore;
    @FXML private TableColumn<PerformanceData, Void> colAppreciation;
    @FXML private TableColumn<PerformanceData, Void> colStatut;

    @FXML private Label lblMoyenneScore;
    @FXML private Label lblMeilleurEmploye;
    @FXML private Label lblTotalTaches;

    private PerformanceService performanceService;
    private ObservableList<PerformanceData> performanceList;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.out.println("✅ PerformanceController - Initialisation");

        performanceService = new PerformanceService();
        performanceList = FXCollections.observableArrayList();

        setupTableColumns();
        loadPerformances();
        updateStatistics();
    }

    /**
     * Configuration des colonnes du tableau
     */
    private void setupTableColumns() {
        // Vérifier que les colonnes ne sont pas null
        if (colNom == null || colTotalTaches == null || colTerminees == null ||
                colEnRetard == null || colScore == null || colAppreciation == null || colStatut == null) {
            System.err.println("❌ ERREUR: Une ou plusieurs colonnes sont NULL !");
            System.err.println("colNom: " + colNom);
            System.err.println("colTotalTaches: " + colTotalTaches);
            System.err.println("colTerminees: " + colTerminees);
            System.err.println("colEnRetard: " + colEnRetard);
            System.err.println("colScore: " + colScore);
            System.err.println("colAppreciation: " + colAppreciation);
            System.err.println("colStatut: " + colStatut);
            return;
        }

        // Colonne Nom
        colNom.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().nomEmploye));

        // Colonne Total Tâches
        colTotalTaches.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().totalTaches));
        colTotalTaches.setStyle("-fx-alignment: CENTER;");

        // Colonne Terminées
        colTerminees.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().tachesTerminees));
        colTerminees.setStyle("-fx-alignment: CENTER;");

        // Colonne En Retard
        colEnRetard.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().tachesEnRetard));
        colEnRetard.setStyle("-fx-alignment: CENTER;");

        // Colonne Score avec barre de progression
        colScore.setCellFactory(column -> new TableCell<PerformanceData, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    PerformanceData perf = getTableRow().getItem();

                    // Barre de progression
                    ProgressBar progressBar = new ProgressBar(perf.score / 100.0);
                    progressBar.setPrefWidth(100);
                    progressBar.setStyle(
                            "-fx-accent: " + perf.getCouleur() + ";" +
                                    "-fx-control-inner-background: #E0E0E0;"
                    );

                    Label scoreLabel = new Label(String.format("%.0f", perf.score));
                    scoreLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + perf.getCouleur() + ";");

                    HBox box = new HBox(10, progressBar, scoreLabel);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });

        // Colonne Appréciation avec badge
        colAppreciation.setCellFactory(column -> new TableCell<PerformanceData, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    PerformanceData perf = getTableRow().getItem();

                    Label badge = new Label(perf.getEmoji() + " " + perf.getAppreciation());
                    badge.setStyle(
                            "-fx-background-color: " + perf.getCouleur() + ";" +
                                    "-fx-text-fill: white;" +
                                    "-fx-background-radius: 15px;" +
                                    "-fx-padding: 5px 12px;" +
                                    "-fx-font-size: 12px;" +
                                    "-fx-font-weight: bold;"
                    );

                    HBox box = new HBox(badge);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });

        // Colonne Statut visuel
        colStatut.setCellFactory(column -> new TableCell<PerformanceData, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    PerformanceData perf = getTableRow().getItem();

                    Label statusLabel = new Label(
                            String.format("T:%d | R:%d",
                                    perf.tachesTerminees,
                                    perf.tachesEnRetard)
                    );
                    statusLabel.setStyle(
                            "-fx-font-size: 11px; -fx-text-fill: " + perf.getCouleur() + ";" +
                                    "-fx-font-weight: bold;"
                    );

                    HBox box = new HBox(statusLabel);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });

        System.out.println("✅ Colonnes configurées avec succès");
    }

    /**
     * Charger les performances
     */
    private void loadPerformances() {
        Integer idAgri = AgriculteurContext.getActiveAgriculteurId();
        System.out.println("🔍 Chargement performances pour agriculteur: " + idAgri);

        List<PerformanceData> performances = performanceService.getClassement(idAgri);
        performanceList.clear();
        performanceList.addAll(performances);

        if (tablePerformance != null) {
            tablePerformance.setItems(performanceList);
        }

        System.out.println("✅ " + performances.size() + " performance(s) chargée(s)");
    }

    /**
     * Mettre à jour les statistiques globales
     */
    private void updateStatistics() {
        if (performanceList.isEmpty()) {
            if (lblMoyenneScore != null) lblMoyenneScore.setText("0.0");
            if (lblMeilleurEmploye != null) lblMeilleurEmploye.setText("Aucun");
            if (lblTotalTaches != null) lblTotalTaches.setText("0");
            return;
        }

        // Calculer moyenne des scores
        double moyenneScore = performanceList.stream()
                .mapToDouble(p -> p.score)
                .average()
                .orElse(0.0);

        // Trouver le meilleur employé
        PerformanceData meilleur = performanceList.stream()
                .max((a, b) -> Double.compare(a.score, b.score))
                .orElse(null);

        // Total des tâches
        int totalTaches = performanceList.stream()
                .mapToInt(p -> p.totalTaches)
                .sum();

        // Afficher
        if (lblMoyenneScore != null) {
            lblMoyenneScore.setText(String.format("%.0f", moyenneScore));
        }

        if (lblMeilleurEmploye != null && meilleur != null) {
            lblMeilleurEmploye.setText(meilleur.nomEmploye);
        }

        if (lblTotalTaches != null) {
            lblTotalTaches.setText(String.valueOf(totalTaches));
        }

        System.out.println("📊 Statistiques: Moyenne=" + moyenneScore + ", Total=" + totalTaches);
    }

    /**
     * Actualiser les données
     */
    @FXML
    private void handleActualiser() {
        System.out.println("🔄 Actualisation des performances...");
        loadPerformances();
        updateStatistics();

        NotificationToast.showNotification(
                "🔄 Performances actualisées",
                NotificationToast.SUCCESS,
                2000
        );
    }

    /**
     * Afficher les détails d'un employé
     */
    @FXML
    private void handleVoirDetails() {
        PerformanceData selected = tablePerformance.getSelectionModel().getSelectedItem();

        if (selected == null) {
            NotificationToast.showNotification(
                    "⚠️ Veuillez sélectionner un employé",
                    NotificationToast.WARNING,
                    2000
            );
            return;
        }

        // Créer une alerte avec les détails
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de Performance");
        alert.setHeaderText(selected.getEmoji() + " " + selected.nomEmploye);

        String details = String.format(
                "📊 Score de Performance: %.1f/100\n" +
                        "🎯 Appréciation: %s\n\n" +
                        "📋 Tâches:\n" +
                        "   • Total: %d\n" +
                        "   • Terminées: %d\n" +
                        "   • En retard: %d\n" +
                        "   • En cours: %d\n" +
                        "   • Annulées: %d\n\n" +
                        "📈 Statistiques:\n" +
                        "   • Taux de réussite: %.1f%%\n" +
                        "   • Temps moyen: %.1f jours",
                selected.score,
                selected.getAppreciation(),
                selected.totalTaches,
                selected.tachesTerminees,
                selected.tachesEnRetard,
                selected.tachesEnCours,
                selected.tachesAnnulees,
                selected.tauxReussite,
                selected.tempsRealisationMoyen
        );

        alert.setContentText(details);
        alert.showAndWait();
    }
}