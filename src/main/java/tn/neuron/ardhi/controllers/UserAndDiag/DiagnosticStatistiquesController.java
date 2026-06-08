package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import tn.neuron.ardhi.services.UserAndDiag.DiagnosticService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

public class DiagnosticStatistiquesController implements Initializable {

    @FXML
    private PieChart pieChartMaladies;

    @FXML
    private BarChart<String, Number> barChartDiagnostics;

    @FXML
    private Label lblTotalDiagnostics;

    private final DiagnosticService diagnosticService = new DiagnosticService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadCharts();
    }

    private void loadCharts() {
        // 1. PieChart: Maladies
        Map<String, Integer> maladies = diagnosticService.getMaladieDistribution();
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        int total = 0;

        for (Map.Entry<String, Integer> entry : maladies.entrySet()) {
            pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
            total += entry.getValue();
        }
        pieChartMaladies.setData(pieData);
        pieChartMaladies.setLegendVisible(false);

        // Update total label
        lblTotalDiagnostics.setText(String.valueOf(total));

        // 2. BarChart: Diagnostics par jour
        Map<String, Integer> diags = diagnosticService.getDiagnosticsPerDate();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Diagnostics");

        for (Map.Entry<String, Integer> entry : diags.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        barChartDiagnostics.getData().clear();
        barChartDiagnostics.getData().add(series);
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Admin Dashboard");
    }
}
