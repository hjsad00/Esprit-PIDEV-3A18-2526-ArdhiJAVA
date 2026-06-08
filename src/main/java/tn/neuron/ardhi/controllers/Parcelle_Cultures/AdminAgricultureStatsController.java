package tn.neuron.ardhi.controllers.Parcelle_Cultures;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import tn.neuron.ardhi.models.Parcelle_Cultures.AdminAgricultureStats;
import tn.neuron.ardhi.models.Parcelle_Cultures.AdminAgricultureStats.TopCulture;
import tn.neuron.ardhi.services.Parcelle_Cultures.AgricultureStatisticsService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 * Tableau de bord global Parcelles/Cultures pour l'administrateur.
 */
public class AdminAgricultureStatsController implements Initializable {

    @FXML private Label lblSurfaceTotalePlateforme;
    @FXML private Label lblNbAgriculteurs;
    @FXML private Label lblProductionGlobale;
    @FXML private Label lblRendementMoyenGlobal;

    @FXML private BarChart<String, Number> barTopCultures;

    private final AgricultureStatisticsService statsService = new AgricultureStatisticsService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            AdminAgricultureStats stats = statsService.getAdminStats();
            afficherStats(stats);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void afficherStats(AdminAgricultureStats s) {
        if (lblSurfaceTotalePlateforme != null)
            lblSurfaceTotalePlateforme.setText(String.format("%.2f ha", s.getSurfaceTotalePlateforme()));
        if (lblNbAgriculteurs != null)
            lblNbAgriculteurs.setText(String.valueOf(s.getNbAgriculteursActifs()));
        if (lblProductionGlobale != null)
            lblProductionGlobale.setText(String.format("%.2f t", s.getProductionGlobale()));
        if (lblRendementMoyenGlobal != null)
            lblRendementMoyenGlobal.setText(String.format("%.2f t/ha", s.getRendementMoyenGlobal()));

        if (barTopCultures != null && s.getTopCultures() != null) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Top 5 Cultures");
            for (TopCulture tc : s.getTopCultures()) {
                String label = tc.getNom() + " (" + (tc.getType() != null ? tc.getType() : "") + ")";
                series.getData().add(new XYChart.Data<>(label, tc.getFrequence()));
            }
            barTopCultures.setData(FXCollections.observableArrayList(series));
        }
    }

    @FXML
    private void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Admin Dashboard");
    }
}

