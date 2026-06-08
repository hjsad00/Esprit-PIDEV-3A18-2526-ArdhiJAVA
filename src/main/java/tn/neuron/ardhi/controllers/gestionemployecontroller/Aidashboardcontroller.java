package tn.neuron.ardhi.controllers.gestionemployecontroller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.net.URL;
import java.sql.*;
import java.util.*;

/**
 * 📊 Dashboard Analytics des Recommandations AI
 */
public class Aidashboardcontroller implements Initializable {

    @FXML private Label lblTotalRecommandations;
    @FXML private Label lblTauxAcceptation;
    @FXML private Label lblScoreMoyen;
    @FXML private Label lblMeilleurEmploye;

    @FXML private PieChart pieChartAcceptation;
    @FXML private BarChart<String, Number> barChartScores;
    @FXML private LineChart<String, Number> lineChartTendance;

    @FXML private TableView<RecommandationStat> tableTopEmployes;
    @FXML private TableColumn<RecommandationStat, String> colEmploye;
    @FXML private TableColumn<RecommandationStat, Integer> colNbRecommandations;
    @FXML private TableColumn<RecommandationStat, Integer> colAcceptees;
    @FXML private TableColumn<RecommandationStat, Double> colScoreMoyen;
    @FXML private TableColumn<RecommandationStat, String> colTaux;

    private Connection getCnx() {
        return MyDatabase.getInstance().getCnx();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.out.println("📊 Initialisation AI Dashboard...");

        setupTableColumns();
        loadStatistics();
        loadCharts();
        loadTopEmployes();
    }

    /**
     * Configuration des colonnes du tableau
     */
    private void setupTableColumns() {
        colEmploye.setCellValueFactory(new PropertyValueFactory<>("nomEmploye"));
        colNbRecommandations.setCellValueFactory(new PropertyValueFactory<>("nbRecommandations"));
        colAcceptees.setCellValueFactory(new PropertyValueFactory<>("nbAcceptees"));
        colScoreMoyen.setCellValueFactory(new PropertyValueFactory<>("scoreMoyen"));
        colTaux.setCellValueFactory(new PropertyValueFactory<>("tauxAcceptation"));

        // Style pour la colonne score
        colScoreMoyen.setCellFactory(column -> new TableCell<RecommandationStat, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%.1f", item));
                    String color = item >= 80 ? "#27ae60" : item >= 60 ? "#f39c12" : "#e74c3c";
                    setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
                }
            }
        });
    }

    /**
     * Charger les statistiques globales
     */
    private void loadStatistics() {
        String query =
                "SELECT " +
                        "COUNT(*) as total, " +
                        "SUM(CASE WHEN acceptee = TRUE THEN 1 ELSE 0 END) as acceptees, " +
                        "AVG(score_matching) as score_moyen " +
                        "FROM ai_recommandation";

        try (Statement stmt = getCnx().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                int total = rs.getInt("total");
                int acceptees = rs.getInt("acceptees");
                double scoreMoyen = rs.getDouble("score_moyen");
                double tauxAcceptation = total > 0 ? (acceptees * 100.0 / total) : 0;

                if (lblTotalRecommandations != null) {
                    lblTotalRecommandations.setText(String.valueOf(total));
                }
                if (lblTauxAcceptation != null) {
                    lblTauxAcceptation.setText(String.format("%.1f%%", tauxAcceptation));
                }
                if (lblScoreMoyen != null) {
                    lblScoreMoyen.setText(String.format("%.1f", scoreMoyen));
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur loadStatistics: " + e.getMessage());
        }

        // Meilleur employé
        loadMeilleurEmploye();
    }

    /**
     * Charger le meilleur employé
     */
    private void loadMeilleurEmploye() {
        String query =
                "SELECT e.nom, e.prenom, COUNT(*) as nb, AVG(r.score_matching) as score " +
                        "FROM ai_recommandation r " +
                        "INNER JOIN employe e ON r.id_employe_recommande = e.id_employe " +
                        "WHERE r.acceptee = TRUE " +
                        "GROUP BY e.id_employe " +
                        "ORDER BY nb DESC, score DESC " +
                        "LIMIT 1";

        try (Statement stmt = getCnx().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next() && lblMeilleurEmploye != null) {
                String nom = rs.getString("nom") + " " + rs.getString("prenom");
                lblMeilleurEmploye.setText(nom);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur loadMeilleurEmploye: " + e.getMessage());
        }
    }

    /**
     * Charger les graphiques
     */
    private void loadCharts() {
        loadPieChart();
        loadBarChart();
        loadLineChart();
    }

    /**
     * PieChart : Taux d'acceptation
     */
    private void loadPieChart() {
        if (pieChartAcceptation == null) return;

        String query =
                "SELECT " +
                        "SUM(CASE WHEN acceptee = TRUE THEN 1 ELSE 0 END) as acceptees, " +
                        "SUM(CASE WHEN acceptee = FALSE THEN 1 ELSE 0 END) as refusees " +
                        "FROM ai_recommandation";

        try (Statement stmt = getCnx().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                int acceptees = rs.getInt("acceptees");
                int refusees = rs.getInt("refusees");

                ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                        new PieChart.Data("Acceptées (" + acceptees + ")", acceptees),
                        new PieChart.Data("Non acceptées (" + refusees + ")", refusees)
                );

                pieChartAcceptation.setData(pieData);
                pieChartAcceptation.setTitle("Taux d'Acceptation des Recommandations");
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur loadPieChart: " + e.getMessage());
        }
    }

    /**
     * BarChart : Distribution des scores
     */
    private void loadBarChart() {
        if (barChartScores == null) return;

        String query =
                "SELECT " +
                        "CASE " +
                        "    WHEN score_matching >= 85 THEN 'Excellent (85-100)' " +
                        "    WHEN score_matching >= 70 THEN 'Très bon (70-84)' " +
                        "    WHEN score_matching >= 55 THEN 'Bon (55-69)' " +
                        "    WHEN score_matching >= 40 THEN 'Acceptable (40-54)' " +
                        "    ELSE 'Faible (<40)' " +
                        "END as categorie, " +
                        "COUNT(*) as nombre " +
                        "FROM ai_recommandation " +
                        "GROUP BY categorie " +
                        "ORDER BY MIN(score_matching) DESC";

        try (Statement stmt = getCnx().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Nombre de recommandations");

            while (rs.next()) {
                String categorie = rs.getString("categorie");
                int nombre = rs.getInt("nombre");
                series.getData().add(new XYChart.Data<>(categorie, nombre));
            }

            barChartScores.getData().clear();
            barChartScores.getData().add(series);
            barChartScores.setTitle("Distribution des Scores de Matching");

        } catch (SQLException e) {
            System.err.println("❌ Erreur loadBarChart: " + e.getMessage());
        }
    }

    /**
     * LineChart : Tendance des recommandations
     */
    private void loadLineChart() {
        if (lineChartTendance == null) return;

        String query =
                "SELECT DATE(date_recommandation) as date, COUNT(*) as nombre " +
                        "FROM ai_recommandation " +
                        "WHERE date_recommandation >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
                        "GROUP BY DATE(date_recommandation) " +
                        "ORDER BY date";

        try (Statement stmt = getCnx().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Recommandations par jour");

            while (rs.next()) {
                String date = rs.getString("date");
                int nombre = rs.getInt("nombre");
                series.getData().add(new XYChart.Data<>(date, nombre));
            }

            lineChartTendance.getData().clear();
            lineChartTendance.getData().add(series);
            lineChartTendance.setTitle("Tendance des 7 derniers jours");

        } catch (SQLException e) {
            System.err.println("❌ Erreur loadLineChart: " + e.getMessage());
        }
    }

    /**
     * Charger le top des employés
     */
    private void loadTopEmployes() {
        ObservableList<RecommandationStat> stats = FXCollections.observableArrayList();

        String query =
                "SELECT " +
                        "e.nom, e.prenom, " +
                        "COUNT(*) as nb_total, " +
                        "SUM(CASE WHEN r.acceptee = TRUE THEN 1 ELSE 0 END) as nb_acceptees, " +
                        "AVG(r.score_matching) as score_moyen " +
                        "FROM ai_recommandation r " +
                        "INNER JOIN employe e ON r.id_employe_recommande = e.id_employe " +
                        "GROUP BY e.id_employe " +
                        "ORDER BY nb_acceptees DESC, score_moyen DESC " +
                        "LIMIT 10";

        try (Statement stmt = getCnx().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String nom = rs.getString("prenom") + " " + rs.getString("nom");
                int nbTotal = rs.getInt("nb_total");
                int nbAcceptees = rs.getInt("nb_acceptees");
                double scoreMoyen = rs.getDouble("score_moyen");
                double taux = nbTotal > 0 ? (nbAcceptees * 100.0 / nbTotal) : 0;

                stats.add(new RecommandationStat(
                        nom, nbTotal, nbAcceptees, scoreMoyen,
                        String.format("%.0f%%", taux)
                ));
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur loadTopEmployes: " + e.getMessage());
        }

        if (tableTopEmployes != null) {
            tableTopEmployes.setItems(stats);
        }
    }

    /**
     * Actualiser les données
     */
    @FXML
    private void handleRefresh() {
        loadStatistics();
        loadCharts();
        loadTopEmployes();
        System.out.println("🔄 Dashboard actualisé");
    }

    /**
     * Classe pour les statistiques d'employé
     */
    public static class RecommandationStat {
        private String nomEmploye;
        private int nbRecommandations;
        private int nbAcceptees;
        private double scoreMoyen;
        private String tauxAcceptation;

        public RecommandationStat(String nomEmploye, int nbRecommandations, int nbAcceptees,
                                  double scoreMoyen, String tauxAcceptation) {
            this.nomEmploye = nomEmploye;
            this.nbRecommandations = nbRecommandations;
            this.nbAcceptees = nbAcceptees;
            this.scoreMoyen = scoreMoyen;
            this.tauxAcceptation = tauxAcceptation;
        }

        // Getters
        public String getNomEmploye() { return nomEmploye; }
        public int getNbRecommandations() { return nbRecommandations; }
        public int getNbAcceptees() { return nbAcceptees; }
        public double getScoreMoyen() { return scoreMoyen; }
        public String getTauxAcceptation() { return tauxAcceptation; }
    }
}