package tn.neuron.ardhi.controllers.Parcelle_Cultures;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import tn.neuron.ardhi.models.Parcelle_Cultures.FarmerStats;
import tn.neuron.ardhi.models.Parcelle_Cultures.Culture;
import tn.neuron.ardhi.models.Parcelle_Cultures.Parcelle;
import tn.neuron.ardhi.models.Parcelle_Cultures.IrrigationResult;
import tn.neuron.ardhi.models.Parcelle_Cultures.RoiResult;
import tn.neuron.ardhi.services.Parcelle_Cultures.AgricultureStatisticsService;
import tn.neuron.ardhi.services.Parcelle_Cultures.IrrigationService;
import tn.neuron.ardhi.services.Parcelle_Cultures.FinancialService;
import tn.neuron.ardhi.services.Parcelle_Cultures.CultureService;
import tn.neuron.ardhi.services.Parcelle_Cultures.ParcelleService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 * Tableau de bord analytique côté Agriculteur.
 * À utiliser avec un FXML du type /fxml/Parcelles/AgriculteurStats.fxml
 * contenant les labels et graphiques déclarés ci-dessous.
 */
public class AgriculteurStatsController implements Initializable {

    // Cartes de chiffres clés
    @FXML private Label lblSurfaceTotale;
    @FXML private Label lblProductionTotale;
    @FXML private Label lblTauxActives;
    @FXML private Label lblDiversification;
    @FXML private Label lblRendementMoyen;
    @FXML private Label lblScoreGlobal;
    @FXML private Label lblNiveau;

    // Graphiques
    @FXML private PieChart pieRepartitionTypes;
    @FXML private BarChart<String, Number> barProductionParType;

    private final AgricultureStatisticsService statsService = new AgricultureStatisticsService();
    private final IrrigationService irrigationService = new IrrigationService();
    private final FinancialService financialService = new FinancialService();
    private final ParcelleService parcelleService = new ParcelleService();
    private final CultureService cultureService = new CultureService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        var user = UserSession.getInstance().getUser();
        if (user == null) {
            return;
        }

        try {
            // Calcul de scores réels à partir des données actuelles
            double optimEau = calculerScoreOptimisationEau(user.getId());
            double rentabilite = calculerScoreRentabilite(user.getId());

            FarmerStats stats = statsService.getFarmerStats(user.getId(), optimEau, rentabilite);
            afficherStats(stats);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void afficherStats(FarmerStats s) {
        if (lblSurfaceTotale != null)
            lblSurfaceTotale.setText(String.format("%.2f ha", s.getSurfaceTotale()));
        if (lblProductionTotale != null)
            lblProductionTotale.setText(String.format("%.2f t", s.getProductionTotale()));
        if (lblTauxActives != null)
            lblTauxActives.setText(String.format("%.1f%%", s.getTauxParcellesActives()));
        if (lblDiversification != null)
            lblDiversification.setText(String.format("%.2f", s.getDiversification()));
        if (lblRendementMoyen != null)
            lblRendementMoyen.setText(String.format("%.2f t/ha", s.getRendementMoyen()));
        if (lblScoreGlobal != null)
            lblScoreGlobal.setText(String.format("%.1f / 10", s.getScoreGlobal()));
        if (lblNiveau != null)
            lblNiveau.setText(s.getNiveauPerformance());

        if (pieRepartitionTypes != null && s.getRepartitionTypesPourcent() != null) {
            var pieData = s.getRepartitionTypesPourcent().entrySet().stream()
                    .map(e -> new PieChart.Data(
                            e.getKey() + " (" + String.format("%.1f%%", e.getValue()) + ")",
                            e.getValue()))
                    .toList();
            pieRepartitionTypes.setData(FXCollections.observableArrayList(pieData));
            pieRepartitionTypes.setTitle("Répartition des types de cultures");
        }

        if (barProductionParType != null && s.getRepartitionTypesPourcent() != null) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Poids relatif (%)");
            s.getRepartitionTypesPourcent().forEach((type, pct) ->
                    series.getData().add(new XYChart.Data<>(type, pct))
            );
            barProductionParType.getData().clear();
            barProductionParType.getData().add(series);
        }
    }

    @FXML
    private void retour(javafx.event.ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/AgriculteurDashboard.fxml", "Ardhi - Tableau de bord");
    }

    /**
     * Calcule un score 0..10 d'optimisation de l'eau
     * à partir des plans d'irrigation simulés pour les cultures de l'agriculteur.
     */
    private double calculerScoreOptimisationEau(int agriculteurId) throws SQLException {
        // On prend une culture récente (si existe) et sa parcelle pour estimer l'efficacité hydrique.
        var cultures = cultureService.recupererParAgriculteur(agriculteurId);
        if (cultures.isEmpty()) {
            return 5.0; // neutre si pas de données
        }
        Culture c = cultures.get(0);
        Parcelle p = parcelleService.recupererParId(c.getParcelleId());
        if (p == null) return 5.0;

        IrrigationResult ir = irrigationService.calculerPlanIrrigation(c, p);
        double efficacite = ir.getEfficaciteHydrique(); // t / ML
        if (efficacite <= 0) return 5.0;

        // Normalisation simple : 0..20 t/ML → 0..10
        double score = Math.min(10.0, (efficacite / 20.0) * 10.0);
        return Math.max(0.0, score);
    }

    /**
     * Calcule un score 0..10 de rentabilité à partir du ROI.
     */
    private double calculerScoreRentabilite(int agriculteurId) throws SQLException {
        var cultures = cultureService.recupererParAgriculteur(agriculteurId);
        if (cultures.isEmpty()) return 5.0;
        Culture c = cultures.get(0);
        Parcelle p = parcelleService.recupererParId(c.getParcelleId());
        if (p == null) return 5.0;

        // Hypothèses simples de coûts/prix (à raffiner ou à relier à l'UI ROI)
        double prixVente = 800.0;      // DT / tonne
        double coutSemences = 300.0;
        double coutEngrais = 400.0;
        double coutMainOeuvre = 500.0;
        double coutIrrigation = 250.0;
        double coutAutres = 150.0;

        RoiResult roi = financialService.calculerRoi(
                c, p, prixVente,
                coutSemences, coutEngrais,
                coutMainOeuvre, coutIrrigation, coutAutres);

        double scoreRoiPourcent = roi.getScoreRoi(); // ROI % (marge / coût *100)
        // Normalisation : -50%..+150% → 0..10
        double norm = (scoreRoiPourcent + 50.0) / 200.0 * 10.0;
        double score = Math.max(0.0, Math.min(10.0, norm));
        return score;
    }
}

