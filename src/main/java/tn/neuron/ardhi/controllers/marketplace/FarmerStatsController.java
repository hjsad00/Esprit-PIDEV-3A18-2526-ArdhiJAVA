package tn.neuron.ardhi.controllers.marketplace;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import tn.neuron.ardhi.models.marketplace.Produit;
import tn.neuron.ardhi.services.marketplace.StatsMarketService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.util.List;
import java.util.Map;

public class FarmerStatsController {

    // ── Cartes résumé ────────────────────────────────────────────────────────
    @FXML private Label lblGains;
    @FXML private Label lblProduits;
    @FXML private Label lblNote;
    @FXML private Label lblCommandes;
    @FXML private Label lblSousTitre;

    // ── BarChart ─────────────────────────────────────────────────────────────
    @FXML private BarChart<String, Number>  barChartTop;
    @FXML private CategoryAxis              barXAxis;
    @FXML private NumberAxis                barYAxis;

    // ── PieChart ─────────────────────────────────────────────────────────────
    @FXML private PieChart pieChartStock;

    // ── LineChart ────────────────────────────────────────────────────────────
    @FXML private LineChart<String, Number> lineChartTendance;
    @FXML private CategoryAxis              lineXAxis;
    @FXML private NumberAxis                lineYAxis;
    @FXML private Button btn7j;
    @FXML private Button btn30j;
    @FXML private Button btn90j;

    // ── Alertes ──────────────────────────────────────────────────────────────
    @FXML private VBox  listeAlertes;
    @FXML private VBox  alertesVides;
    @FXML private Label lblNbAlertes;

    // ── Interne ──────────────────────────────────────────────────────────────
    private StatsMarketService statsService;
    private int                idVendeur;

    private static final int    SEUIL_STOCK = 5;
    private static final String STYLE_BTN_ACTIF   =
            "-fx-background-color: #046436; -fx-text-fill: white;" +
                    "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 4 12;" +
                    "-fx-font-size: 11px; -fx-font-weight: bold;";
    private static final String STYLE_BTN_INACTIF =
            "-fx-background-color: #f0f3f0; -fx-text-fill: #6a8a78;" +
                    "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 4 12;" +
                    "-fx-font-size: 11px; -fx-font-weight: bold;";

    // Palette verts dégradés pour les graphiques
    private static final String[] PALETTE = {
            "#046436", "#2d7a4f", "#43a047", "#7fffc0", "#a5d6a7", "#1b5e20"
    };

    @FXML
    public void initialize() {
        statsService = new StatsMarketService();
        idVendeur    = UserSession.getInstance().getUser().getId();
        String prenom = UserSession.getInstance().getUser().getPrenom();
        lblSousTitre.setText("Bienvenue " + prenom + " — voici vos performances du moment");

        chargerCartesResume();
        chargerBarChart();
        chargerPieChart();
        chargerLineChart(30);
        chargerAlertes();
        appliquerStylesGraphiques();
    }

    // ════════════════════════════════════════════════════════════════════════
    // CARTES RÉSUMÉ
    // ════════════════════════════════════════════════════════════════════════

    private void chargerCartesResume() {
        // Gains
        double gains = statsService.getTotalEarnings(idVendeur);
        animerLabel(lblGains, String.format("%.2f DT", gains));

        // Produits
        int nbProduits = statsService.getTotalProduits(idVendeur);
        animerLabel(lblProduits, String.valueOf(nbProduits));

        // Note
        double note = statsService.getAverageRating(idVendeur);
        animerLabel(lblNote, note > 0 ? String.format("%.1f ⭐", note) : "N/A");

        // Commandes
        int nbCommandes = statsService.getTotalCommandes(idVendeur);
        animerLabel(lblCommandes, String.valueOf(nbCommandes));

        // Hover animation sur cartes
        ajouterHoverCarte("carteGains");
    }

    /** Anime le chiffre qui "compte" jusqu'à la valeur finale. */
    private void animerLabel(Label lbl, String valeurFinale) {
        FadeTransition ft = new FadeTransition(Duration.millis(600), lbl);
        ft.setFromValue(0);
        ft.setToValue(1);
        lbl.setText(valeurFinale);
        ft.play();
    }

    // ════════════════════════════════════════════════════════════════════════
    // BARCHART — TOP 5 PRODUITS
    // ════════════════════════════════════════════════════════════════════════

    private void chargerBarChart() {
        barChartTop.getData().clear();
        barChartTop.setLegendVisible(false);
        barChartTop.setBarGap(4);
        barChartTop.setCategoryGap(20);

        Map<String, Integer> top = statsService.getTopSellingProducts(idVendeur, 5);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Quantité vendue");

        int i = 0;
        for (Map.Entry<String, Integer> entry : top.entrySet()) {
            XYChart.Data<String, Number> data = new XYChart.Data<>(
                    tronquer(entry.getKey(), 14), entry.getValue());
            series.getData().add(data);

            // Couleur par barre via lookup après ajout
            final int idx = i % PALETTE.length;
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-bar-fill: " + PALETTE[idx] + ";");
                }
            });
            i++;
        }

        barChartTop.getData().add(series);

        if (top.isEmpty()) {
            barChartTop.setTitle("Aucune vente enregistrée");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // PIECHART — RÉPARTITION STOCK
    // ════════════════════════════════════════════════════════════════════════

    private void chargerPieChart() {
        pieChartStock.getData().clear();
        pieChartStock.setLegendVisible(true);

        Map<String, Integer> stock = statsService.getStockDistribution(idVendeur);

        int i = 0;
        for (Map.Entry<String, Integer> entry : stock.entrySet()) {
            PieChart.Data slice = new PieChart.Data(
                    entry.getKey() + " (" + entry.getValue() + ")", entry.getValue());
            pieChartStock.getData().add(slice);

            final int idx = i % PALETTE.length;
            slice.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-pie-color: " + PALETTE[idx] + ";");
                }
            });
            i++;
        }

        if (stock.isEmpty()) {
            pieChartStock.getData().add(new PieChart.Data("Aucun produit", 1));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // LINECHART — TENDANCE DES VENTES
    // ════════════════════════════════════════════════════════════════════════

    private void chargerLineChart(int nbJours) {
        lineChartTendance.getData().clear();
        lineChartTendance.setLegendVisible(false);
        lineChartTendance.setCreateSymbols(nbJours <= 30);

        Map<String, Double> trend = statsService.getSalesTrend(idVendeur, nbJours);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("CA (DT)");

        for (Map.Entry<String, Double> entry : trend.entrySet()) {
            // Afficher seulement jour/mois pour lisibilité
            String label = nbJours <= 30
                    ? entry.getKey().substring(5)   // MM-dd
                    : entry.getKey().substring(0, 7); // yyyy-MM
            series.getData().add(new XYChart.Data<>(label, entry.getValue()));
        }

        lineChartTendance.getData().add(series);

        // Style de la ligne après rendu
        lineChartTendance.lookupAll(".chart-series-line").forEach(node ->
                node.setStyle("-fx-stroke: #046436; -fx-stroke-width: 2.5px;"));
        lineChartTendance.lookupAll(".chart-line-symbol").forEach(node ->
                node.setStyle("-fx-background-color: #046436, white; -fx-background-radius: 5;"));
    }

    @FXML void charger7j(ActionEvent e)  { setPeriodeActive(btn7j);  chargerLineChart(7);  }
    @FXML void charger30j(ActionEvent e) { setPeriodeActive(btn30j); chargerLineChart(30); }
    @FXML void charger90j(ActionEvent e) { setPeriodeActive(btn90j); chargerLineChart(90); }

    private void setPeriodeActive(Button actif) {
        for (Button b : new Button[]{btn7j, btn30j, btn90j}) {
            b.setStyle(b == actif ? STYLE_BTN_ACTIF : STYLE_BTN_INACTIF);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // ALERTES STOCK BAS
    // ════════════════════════════════════════════════════════════════════════

    private void chargerAlertes() {
        listeAlertes.getChildren().clear();
        List<Produit> alertes = statsService.getLowStockAlerts(idVendeur, SEUIL_STOCK);

        lblNbAlertes.setText(String.valueOf(alertes.size()));
        lblNbAlertes.setStyle(alertes.isEmpty()
                ? "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 11px;" +
                "-fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 3 12;"
                : "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 11px;" +
                "-fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 3 12;");

        if (alertes.isEmpty()) {
            alertesVides.setVisible(true);
            alertesVides.setManaged(true);
            return;
        }

        alertesVides.setVisible(false);
        alertesVides.setManaged(false);

        for (Produit p : alertes) {
            listeAlertes.getChildren().add(creerLigneAlerte(p));
        }
    }

    private HBox creerLigneAlerte(Produit p) {
        HBox ligne = new HBox(14);
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.setPadding(new Insets(12, 16, 12, 16));

        boolean rupture = p.getQuantiteStock() == 0;

        ligne.setStyle(
                "-fx-background-color: " + (rupture ? "#fff5f5" : "#fffbf0") + ";" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: " + (rupture ? "#ffcdd2" : "#ffe0b2") + ";" +
                        "-fx-border-radius: 10; -fx-border-width: 1;");

        // Icône
        Label icone = new Label(rupture ? "🔴" : "🟡");
        icone.setStyle("-fx-font-size: 16px;");

        // Infos
        VBox infos = new VBox(3);
        HBox.setHgrow(infos, javafx.scene.layout.Priority.ALWAYS);

        Label nomLabel = new Label(p.getNom());
        nomLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0d1f15;");

        Label catLabel = new Label(p.getCategorie() != null ? p.getCategorie() : "—");
        catLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8aaa98;");

        infos.getChildren().addAll(nomLabel, catLabel);

        // Stock badge
        Label stockBadge = new Label(rupture ? "RUPTURE" : "Stock : " + p.getQuantiteStock());
        stockBadge.setStyle(
                "-fx-background-color: " + (rupture ? "#e74c3c" : "#e67e22") + ";" +
                        "-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 6; -fx-padding: 3 10;");

        ligne.getChildren().addAll(icone, infos, stockBadge);
        return ligne;
    }

    // ════════════════════════════════════════════════════════════════════════
    // STYLES GRAPHIQUES
    // ════════════════════════════════════════════════════════════════════════

    private void appliquerStylesGraphiques() {
        // Fond transparent pour tous les graphiques
        String styleFond = "-fx-background-color: transparent;" +
                "-fx-plot-background-color: transparent;" +
                "-fx-horizontal-grid-lines-visible: true;" +
                "-fx-vertical-grid-lines-visible: false;";

        barChartTop.setStyle(styleFond);
        pieChartStock.setStyle("-fx-background-color: transparent;");
        lineChartTendance.setStyle(styleFond);

        // Grille discrète
        barChartTop.setHorizontalGridLinesVisible(true);
        barChartTop.setVerticalGridLinesVisible(false);
        lineChartTendance.setHorizontalGridLinesVisible(true);
        lineChartTendance.setVerticalGridLinesVisible(false);
    }

    // ════════════════════════════════════════════════════════════════════════
    // HOVER CARTES (ajouté programmatiquement)
    // ════════════════════════════════════════════════════════════════════════

    private void ajouterHoverCarte(String id) {
        // Les cartes ont déjà leur style inline — le hover est géré via CSS inline
        // On pourrait ajouter des ScaleTransitions ici si besoin
    }

    // ════════════════════════════════════════════════════════════════════════
    // NAVIGATION
    // ════════════════════════════════════════════════════════════════════════

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/marketplace/Marketplace.fxml", "Ardhi - Marketplace");
    }

    // ════════════════════════════════════════════════════════════════════════
    // HELPER
    // ════════════════════════════════════════════════════════════════════════

    private String tronquer(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : s;
    }
}