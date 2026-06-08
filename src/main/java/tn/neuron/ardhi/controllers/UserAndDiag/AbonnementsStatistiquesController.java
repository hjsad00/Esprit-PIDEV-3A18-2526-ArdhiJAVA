package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import tn.neuron.ardhi.models.UserAndDiag.Abonnement;
import tn.neuron.ardhi.services.UserAndDiag.AbonnementService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;

/**
 * Contrôleur pour les statistiques des abonnements.
 * Compte dynamiquement tous les types d'offres depuis la BDD.
 */
public class AbonnementsStatistiquesController implements Initializable {

    @FXML
    private PieChart pieChartAbonnements;

    @FXML
    private Label lblTotalRevenu;

    private AbonnementService as = new AbonnementService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chargerStatistiques();
    }

    private void chargerStatistiques() {
        try {
            // 1. Récupérer tous les abonnements ACTIFS
            List<Abonnement> liste = as.recuperer();

            // 2. Compter par type d'offre dynamiquement
            Map<String, Integer> countParType = new LinkedHashMap<>();
            double revenuTotal = 0;

            for (Abonnement a : liste) {
                if ("ACTIF".equals(a.getStatut())) {
                    String type = a.getType();
                    if (type == null || type.isEmpty()) {
                        type = "Autre";
                    }

                    // Compter
                    countParType.put(type, countParType.getOrDefault(type, 0) + 1);

                    // Revenu
                    revenuTotal += a.getPrix();
                }
            }

            // 3. Créer les données du PieChart
            int totalAbonnements = countParType.values().stream().mapToInt(Integer::intValue).sum();
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

            for (Map.Entry<String, Integer> entry : countParType.entrySet()) {
                String type = entry.getKey();
                int count = entry.getValue();
                double pct = (totalAbonnements == 0) ? 0 : ((double) count / totalAbonnements) * 100;

                // Label pour le graphique
                String label = String.format("%s (%d) - %.1f%%", type, count, pct);
                pieChartData.add(new PieChart.Data(label, count));
            }

            // 4. Si aucun abonnement actif, afficher un message
            if (pieChartData.isEmpty()) {
                pieChartData.add(new PieChart.Data("Aucun abonnement actif", 1));
            }

            // 5. Configurer le graphique
            pieChartAbonnements.setData(pieChartData);
            pieChartAbonnements.setTitle("📊 Répartition des abonnements actifs");
            pieChartAbonnements.setStartAngle(90);
            pieChartAbonnements.setLabelsVisible(true);
            pieChartAbonnements.setLegendVisible(false);
            pieChartAbonnements.setClockwise(true);
            pieChartAbonnements.setLabelLineLength(20);

            // 6. Afficher le revenu total
            lblTotalRevenu.setText(String.format("%.2f DT", revenuTotal));

        } catch (SQLException e) {
            WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les statistiques.");
        }
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Admin Dashboard");
    }
}