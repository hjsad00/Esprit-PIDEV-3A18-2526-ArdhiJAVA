package tn.neuron.ardhi.controllers.Parcelle_Cultures;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import tn.neuron.ardhi.models.Parcelle_Cultures.Culture;
import tn.neuron.ardhi.models.Parcelle_Cultures.Parcelle;
import tn.neuron.ardhi.models.Parcelle_Cultures.RoiResult;
import tn.neuron.ardhi.services.Parcelle_Cultures.CultureService;
import tn.neuron.ardhi.services.Parcelle_Cultures.FinancialService;
import tn.neuron.ardhi.services.Parcelle_Cultures.ParcelleService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Contrôleur du Calculateur Intelligent de Rendement et Revenus (ROI Agricole).
 *
 * Flow : Sélection Culture+Parcelle → Saisie coûts → Calcul complet →
 *        Affichage résultats + Simulation dynamique prix/coûts
 */
public class RoiCalculatorController implements Initializable {

    // ==================== SÉLECTION ====================
    @FXML private ComboBox<Culture>  cbCulture;
    @FXML private ComboBox<Parcelle> cbParcelle;

    // ==================== SAISIE COÛTS & PRIX ====================
    @FXML private TextField tfPrixVente;
    @FXML private TextField tfCoutSemences;
    @FXML private TextField tfCoutEngrais;
    @FXML private TextField tfCoutMainOeuvre;
    @FXML private TextField tfCoutIrrigation;
    @FXML private TextField tfCoutAutres;

    // ==================== CHARGEMENT ====================
    @FXML private Button          btnCalculer;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label           lblEtat;
    @FXML private VBox            vboxResultats;

    // ==================== RÉSULTATS CLIMATIQUES ====================
    @FXML private Label lblFacteurClimat;
    @FXML private Label lblJoursCanicule;
    @FXML private Label lblJoursPluie;
    @FXML private Label lblJoursGel;
    @FXML private Label lblSourceClimat;

    // ==================== RÉSULTATS PRODUCTION ====================
    @FXML private Label lblSurface;
    @FXML private Label lblRendementTheo;
    @FXML private Label lblProductionTheo;
    @FXML private Label lblProductionReelle;
    @FXML private Label lblPerteClimat;

    // ==================== RÉSULTATS FINANCIERS ====================
    @FXML private Label lblCoutTotal;
    @FXML private Label lblRevenuBrut;
    @FXML private Label lblMargeBrute;
    @FXML private Label lblPrixSeuil;
    @FXML private Label lblScoreRoi;
    @FXML private Label lblStatutRentabilite;
    @FXML private VBox  vboxStatut;

    // ==================== ALERTES & CONSEILS ====================
    @FXML private VBox  vboxAlertes;
    @FXML private Label lblConseil;

    // ==================== SIMULATION ====================
    @FXML private Slider  sliderPrixVente;
    @FXML private Label   lblSimuMarge;
    @FXML private Label   lblSimuRoi;

    // ==================== ÉTAT ====================
    private final FinancialService financialService = new FinancialService();
    private final CultureService   cultureService   = new CultureService();
    private final ParcelleService  parcelleService  = new ParcelleService();
    private RoiResult dernierResultat;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chargerDonnees();
        vboxResultats.setVisible(false);
        vboxResultats.setManaged(false);

        // Auto-sélection de la parcelle liée à la culture
        cbCulture.setOnAction(e -> {
            Culture c = cbCulture.getValue();
            if (c == null) return;
            cbParcelle.getItems().stream()
                    .filter(p -> p.getId() == c.getParcelleId())
                    .findFirst().ifPresent(cbParcelle::setValue);
            // Pré-remplir rendement
        });

        // Simulation dynamique via Slider
        if (sliderPrixVente != null) {
            sliderPrixVente.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (dernierResultat != null) {
                    financialService.simulerAvecNouveauPrix(dernierResultat, newVal.doubleValue());
                    mettreAJourSimulation(dernierResultat);
                }
            });
        }
    }

    // ==================== CHARGEMENT DONNÉES ====================

    private void chargerDonnees() {
        try {
            int userId = UserSession.getInstance().getUser().getId();
            List<Culture>  cultures  = cultureService.recupererParAgriculteur(userId);
            List<Parcelle> parcelles = parcelleService.recupererParAgriculteur(userId);
            cbCulture.setItems(FXCollections.observableArrayList(cultures));
            cbParcelle.setItems(FXCollections.observableArrayList(parcelles));
        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", "Impossible de charger les données : " + e.getMessage());
        }
    }

    // ==================== CALCUL PRINCIPAL ====================

    @FXML
    void calculerRoi(ActionEvent event) {
        Culture  culture  = cbCulture.getValue();
        Parcelle parcelle = cbParcelle.getValue();

        if (culture == null || parcelle == null) {
            WindowUtils.showAlert("Sélection manquante", "Veuillez sélectionner une culture et une parcelle.");
            return;
        }

        double prixVente = parseDoubleField(tfPrixVente, "Prix de vente");
        if (prixVente < 0) return;

        // Validation du prix de vente (fourchette réaliste pour l'agriculture)
        if (prixVente > 10_000) {
            WindowUtils.showAlert("Prix irréaliste",
                    String.format("Le prix de vente (%.0f DT/t) semble trop élevé.\n" +
                            "Les prix agricoles sont généralement entre 50 et 5 000 DT/tonne.\n" +
                            "Vérifiez votre saisie (le prix est par tonne, pas total).", prixVente));
            return;
        }

        double semences   = parseDoubleFieldOrZero(tfCoutSemences);
        double engrais    = parseDoubleFieldOrZero(tfCoutEngrais);
        double mainOeuvre = parseDoubleFieldOrZero(tfCoutMainOeuvre);
        double irrigation = parseDoubleFieldOrZero(tfCoutIrrigation);
        double autres     = parseDoubleFieldOrZero(tfCoutAutres);

        double coutTotal = semences + engrais + mainOeuvre + irrigation + autres;
        if (coutTotal <= 0) {
            WindowUtils.showAlert("Coûts manquants", "Veuillez saisir au moins un coût d'exploitation.");
            return;
        }

        setEtatChargement(true);

        Task<RoiResult> task = new Task<>() {
            @Override
            protected RoiResult call() {
                return financialService.calculerRoi(
                        culture, parcelle, prixVente,
                        semences, engrais, mainOeuvre, irrigation, autres);
            }
        };

        task.setOnSucceeded(e -> {
            dernierResultat = task.getValue();
            Platform.runLater(() -> {
                setEtatChargement(false);
                afficherResultats(dernierResultat);
                configurerSlider(prixVente);
            });
        });

        task.setOnFailed(e -> Platform.runLater(() -> {
            setEtatChargement(false);
            WindowUtils.showAlert("Erreur", "Calcul échoué : "
                    + (task.getException() != null ? task.getException().getMessage() : "inconnue"));
        }));

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // ==================== AFFICHAGE RÉSULTATS ====================

    private void afficherResultats(RoiResult r) {
        // ---- Aléas climatiques ----
        lblJoursCanicule.setText(r.getJoursCanicule() + " jour(s)");
        lblJoursPluie.setText(r.getJoursExcesPluie()  + " jour(s)");
        lblJoursGel.setText(r.getJoursGel()            + " jour(s)");
        lblFacteurClimat.setText(r.getFacteurClimatiqueFormate());
        lblSourceClimat.setText(r.isDonneeClimatFallback()
                ? "📊 Moyennes saisonnières (mode dégradé)"
                : "🌐 Données météo 7j — Open-Meteo");

        String couleurFacteur = r.getFacteurClimatique() >= 0.9 ? "#388E3C"
                : r.getFacteurClimatique() >= 0.7              ? "#F57C00"
                :                                                 "#D32F2F";
        lblFacteurClimat.setStyle("-fx-text-fill: " + couleurFacteur + "; -fx-font-weight: bold; -fx-font-size: 13;");

        // ---- Production ----
        lblSurface.setText(String.format("%.2f ha", r.getSurfaceHectares()));
        lblRendementTheo.setText(String.format("%.1f t/ha", r.getRendementTheorique()));
        lblProductionTheo.setText(String.format("%.2f t", r.getProductionTheorique()));
        lblProductionReelle.setText(String.format("%.2f t", r.getProductionReelle()));
        lblPerteClimat.setText(String.format("%.2f t (−%.0f%%)",
                r.getPerteProdClimat(),
                r.getProductionTheorique() > 0
                        ? (r.getPerteProdClimat() / r.getProductionTheorique() * 100) : 0));
        lblPerteClimat.setStyle(r.getPerteProdClimat() > 0
                ? "-fx-text-fill: #D32F2F; -fx-font-weight: bold;"
                : "-fx-text-fill: #388E3C; -fx-font-weight: bold;");

        // ---- Financier ----
        lblCoutTotal.setText(PRICE_FORMAT.format(r.getCoutTotal()) + " DT");
        lblRevenuBrut.setText(PRICE_FORMAT.format(r.getRevenuBrut()) + " DT");
        lblMargeBrute.setText(formatMontant(r.getMargeBrute()) + " DT");
        lblMargeBrute.setStyle("-fx-text-fill: " + r.getCouleurStatut() + "; -fx-font-size: 16; -fx-font-weight: bold;");
        lblPrixSeuil.setText(String.format("%.2f DT/t", r.getPrixSeuil()));
        lblScoreRoi.setText(formatPourcentage(r.getScoreRoi()));
        lblScoreRoi.setStyle("-fx-text-fill: " + r.getCouleurStatut() + "; -fx-font-size: 18; -fx-font-weight: bold;");

        // ---- Statut ----
        String icone = switch (r.getStatut()) {
            case "PERTE"     -> "🔴 PERTE";
            case "ÉQUILIBRE" -> "🟠 ÉQUILIBRE";
            default          -> "🟢 PROFIT";
        };
        lblStatutRentabilite.setText(icone);
        lblStatutRentabilite.setStyle("-fx-text-fill: " + r.getCouleurStatut()
                + "; -fx-font-size: 22; -fx-font-weight: bold;");
        vboxStatut.setStyle("-fx-background-color: " + r.getCouleurStatut() + "22; "
                + "-fx-background-radius: 12; -fx-padding: 18; -fx-border-color: "
                + r.getCouleurStatut() + "66; -fx-border-radius: 12; -fx-border-width: 1.5;");

        // ---- Alertes ----
        vboxAlertes.getChildren().clear();
        for (String alerte : r.getAlertes()) {
            Label lbl = new Label(alerte);
            lbl.setWrapText(true);
            lbl.setStyle("-fx-background-color: #FFF3E0; -fx-background-radius: 6; "
                    + "-fx-padding: 8 12; -fx-text-fill: #E65100; -fx-font-size: 12;");
            lbl.setMaxWidth(Double.MAX_VALUE);
            vboxAlertes.getChildren().add(lbl);
        }

        // ---- Conseil ----
        lblConseil.setText(r.getConseil());

        vboxResultats.setVisible(true);
        vboxResultats.setManaged(true);
        lblEtat.setText("✅ Calcul terminé");
        lblEtat.setStyle("-fx-text-fill: #388E3C;");
    }

    // ==================== SIMULATION DYNAMIQUE ====================

    private static final DecimalFormat PRICE_FORMAT;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        PRICE_FORMAT = new DecimalFormat("#,##0.00", symbols);
    }

    private void configurerSlider(double prixBase) {
        if (sliderPrixVente == null) return;

        // Cap the price to a reasonable range for agriculture (10 - 10,000 DT/tonne)
        double prixEffectif = Math.min(Math.max(prixBase, 10), 10_000);

        double minVal = Math.max(10, prixEffectif * 0.3);
        double maxVal = prixEffectif * 2.5;

        sliderPrixVente.setMin(minVal);
        sliderPrixVente.setMax(maxVal);
        sliderPrixVente.setValue(prixEffectif);

        // Dynamic tick unit: divide range into ~5 ticks
        double range = maxVal - minVal;
        double tickUnit = Math.max(1, Math.round(range / 5.0));
        sliderPrixVente.setMajorTickUnit(tickUnit);
        sliderPrixVente.setMinorTickCount(4);
        sliderPrixVente.setBlockIncrement(tickUnit / 5.0);
        sliderPrixVente.setSnapToTicks(false);

        // Custom tick label formatter for readable numbers
        sliderPrixVente.setLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Double value) {
                if (value == null) return "";
                if (value >= 1000) {
                    return String.format("%.0f", value);
                }
                return String.format("%.0f", value);
            }

            @Override
            public Double fromString(String string) {
                try { return Double.parseDouble(string); }
                catch (NumberFormatException e) { return 0.0; }
            }
        });
    }

    private void mettreAJourSimulation(RoiResult r) {
        if (lblSimuMarge != null) {
            lblSimuMarge.setText(formatMontant(r.getMargeBrute()) + " DT");
            lblSimuMarge.setStyle("-fx-text-fill: " + r.getCouleurStatut()
                    + "; -fx-font-weight: bold; -fx-font-size: 15;");
        }
        if (lblSimuRoi != null) {
            lblSimuRoi.setText("ROI : " + formatPourcentage(r.getScoreRoi()));
            lblSimuRoi.setStyle("-fx-text-fill: " + r.getCouleurStatut() + "; -fx-font-size: 13;");
        }
        // Mise à jour des labels principaux aussi
        if (lblMargeBrute != null) lblMargeBrute.setText(formatMontant(r.getMargeBrute()) + " DT");
        if (lblScoreRoi    != null) lblScoreRoi.setText(formatPourcentage(r.getScoreRoi()));
        if (lblStatutRentabilite != null) {
            String ic = switch (r.getStatut()) {
                case "PERTE"     -> "🔴 PERTE";
                case "ÉQUILIBRE" -> "🟠 ÉQUILIBRE";
                default          -> "🟢 PROFIT";
            };
            lblStatutRentabilite.setText(ic);
            lblStatutRentabilite.setStyle("-fx-text-fill: " + r.getCouleurStatut()
                    + "; -fx-font-size: 22; -fx-font-weight: bold;");
        }
        if (lblConseil != null) lblConseil.setText(r.getConseil());
    }

    /**
     * Formate un montant en DT avec séparateur de milliers et signe.
     */
    private String formatMontant(double montant) {
        String signe = montant >= 0 ? "+" : "";
        return signe + PRICE_FORMAT.format(montant);
    }

    /**
     * Formate un pourcentage ROI de manière lisible.
     */
    private String formatPourcentage(double pct) {
        String signe = pct >= 0 ? "+" : "";
        if (Math.abs(pct) >= 1000) {
            return signe + String.format("%,.0f %%", pct);
        }
        return signe + String.format("%.1f %%", pct);
    }

    // ==================== UTILITAIRES ====================

    private double parseDoubleField(TextField tf, String fieldName) {
        if (tf == null || tf.getText().trim().isEmpty()) {
            WindowUtils.showAlert("Champ manquant", fieldName + " est obligatoire.");
            return -1;
        }
        try {
            return Double.parseDouble(tf.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Format invalide", fieldName + " doit être un nombre (ex: 450.00).");
            return -1;
        }
    }

    private double parseDoubleFieldOrZero(TextField tf) {
        if (tf == null || tf.getText().trim().isEmpty()) return 0;
        try {
            return Double.parseDouble(tf.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void setEtatChargement(boolean loading) {
        progressIndicator.setVisible(loading);
        progressIndicator.setManaged(loading);
        btnCalculer.setDisable(loading);
        lblEtat.setText(loading ? "⏳ Récupération données climatiques..." : "");
        lblEtat.setStyle(loading ? "-fx-text-fill: #1565C0;" : "");
        if (loading) {
            vboxResultats.setVisible(false);
            vboxResultats.setManaged(false);
        }
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/AgriculteurDashboard.fxml", "Ardhi - Tableau de bord");
    }
}
