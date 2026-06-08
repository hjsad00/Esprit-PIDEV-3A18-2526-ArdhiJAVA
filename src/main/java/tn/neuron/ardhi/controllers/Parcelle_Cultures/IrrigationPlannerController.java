package tn.neuron.ardhi.controllers.Parcelle_Cultures;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import tn.neuron.ardhi.models.Parcelle_Cultures.Culture;
import tn.neuron.ardhi.models.Parcelle_Cultures.IrrigationResult;
import tn.neuron.ardhi.models.Parcelle_Cultures.Parcelle;
import tn.neuron.ardhi.services.Parcelle_Cultures.CultureService;
import tn.neuron.ardhi.services.Parcelle_Cultures.IrrigationService;
import tn.neuron.ardhi.services.Parcelle_Cultures.ParcelleService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Contrôleur du Planificateur Intelligent des Besoins en Eau.
 *
 * Flow : Sélection Culture + Parcelle → Appel IrrigationService →
 *        Affichage résultats (volume, niveau, stress, efficacité, conseils)
 */
public class IrrigationPlannerController implements Initializable {

    // ==================== SÉLECTION ====================
    @FXML private ComboBox<Culture>  cbCulture;
    @FXML private ComboBox<Parcelle> cbParcelle;
    @FXML private Button             btnCalculer;
    @FXML private Button             btnRetour;

    // ==================== MÉTÉO ====================
    @FXML private Label lblTmoy;
    @FXML private Label lblTmax;
    @FXML private Label lblTmin;
    @FXML private Label lblPrecip;
    @FXML private Label lblHumidite;
    @FXML private Label lblDescMeteo;
    @FXML private Label lblSourceMeteo;

    // ==================== RÉSULTATS CALCUL ====================
    @FXML private Label lblKc;
    @FXML private Label lblET0;
    @FXML private Label lblBesoinBrut;
    @FXML private Label lblBesoinNet;
    @FXML private Label lblVolume;
    @FXML private Label lblVolumeM3;
    @FXML private Label lblNiveauIrrigation;
    @FXML private Label lblEfficacite;

    // ==================== STRESS & CONSEILS ====================
    @FXML private Label  lblStress;
    @FXML private Label  lblCauseStress;
    @FXML private Label  lblConseil;
    @FXML private VBox   vboxStress;

    // ==================== CHARGEMENT ====================
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label             lblEtat;
    @FXML private VBox              vboxResultats;

    // ==================== ÉTAT ====================
    private final IrrigationService irrigationService = new IrrigationService();
    private final CultureService    cultureService    = new CultureService();
    private final ParcelleService   parcelleService   = new ParcelleService();
    private IrrigationResult        dernierResultat;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chargerDonnees();
        vboxResultats.setVisible(false);
        vboxResultats.setManaged(false);

        // Lorsqu'une culture est choisie, auto-sélectionner sa parcelle
        cbCulture.setOnAction(e -> {
            Culture c = cbCulture.getValue();
            if (c != null) {
                cbParcelle.getItems().stream()
                        .filter(p -> p.getId() == c.getParcelleId())
                        .findFirst()
                        .ifPresent(cbParcelle::setValue);
            }
        });
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

    // ==================== CALCUL ====================

    @FXML
    void calculerIrrigation(ActionEvent event) {
        Culture  culture  = cbCulture.getValue();
        Parcelle parcelle = cbParcelle.getValue();

        if (culture == null) {
            WindowUtils.showAlert("Sélection manquante", "Veuillez sélectionner une culture.");
            return;
        }
        if (parcelle == null) {
            WindowUtils.showAlert("Sélection manquante", "Veuillez sélectionner une parcelle.");
            return;
        }

        // Démarrage indicateur chargement
        setEtatChargement(true);

        Task<IrrigationResult> task = new Task<>() {
            @Override
            protected IrrigationResult call() {
                return irrigationService.calculerPlanIrrigation(culture, parcelle);
            }
        };

        task.setOnSucceeded(e -> {
            dernierResultat = task.getValue();
            Platform.runLater(() -> {
                setEtatChargement(false);
                afficherResultats(dernierResultat);
            });
        });

        task.setOnFailed(e -> Platform.runLater(() -> {
            setEtatChargement(false);
            String msg = task.getException() != null
                    ? task.getException().getMessage() : "Erreur inconnue";
            WindowUtils.showAlert("Erreur Calcul", "Impossible de calculer : " + msg);
        }));

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // ==================== AFFICHAGE RÉSULTATS ====================

    private void afficherResultats(IrrigationResult r) {
        if (r == null) return;

        // ---- Météo ----
        lblTmoy.setText(String.format("%.1f °C", r.getTemperatureMoyenne()));
        lblTmax.setText(String.format("%.1f °C", r.getTemperatureMax()));
        lblTmin.setText(String.format("%.1f °C", r.getTemperatureMin()));
        lblPrecip.setText(String.format("%.1f mm", r.getPrecipitationsSemaine()));
        lblHumidite.setText(String.format("%.0f %%", r.getHumidite()));
        lblDescMeteo.setText(r.getDescriptionMeteo());
        lblSourceMeteo.setText(r.isDonneesFallback()
                ? "📊 Moyennes saisonnières (mode dégradé)"
                : "🌐 Météo en temps réel — Open-Meteo");

        // ---- Calculs agronomiques ----
        lblKc.setText(String.format("%.1f mm/semaine", r.getKcCulture()));
        lblET0.setText(String.format("%.2f mm", r.getEt0()));
        lblBesoinBrut.setText(String.format("%.1f mm", r.getBesoinBrut()));
        lblBesoinNet.setText(String.format("%.1f mm  (%s)", r.getBesoinNet(), r.getBesoinNetFormate()));
        lblVolume.setText(r.getVolumeFormate());
        lblVolumeM3.setText(String.format("(%.2f m³ | %.0f litres)", r.getVolumeEauM3(), r.getVolumeEauLitres()));

        // ---- Niveau irrigation ----
        String niveau = r.getNiveauIrrigation();
        lblNiveauIrrigation.setText("💧 " + niveau);
        String couleurNiveau = switch (niveau) {
            case "AUCUN"   -> "#4CAF50";
            case "FAIBLE"  -> "#8BC34A";
            case "MODÉRÉ"  -> "#FF9800";
            case "ÉLEVÉ"   -> "#F44336";
            default        -> "#607D8B";
        };
        lblNiveauIrrigation.setStyle("-fx-text-fill: " + couleurNiveau + "; -fx-font-size: 18; -fx-font-weight: bold;");

        // ---- Efficacité hydrique ----
        if (r.getEfficaciteHydrique() > 0) {
            lblEfficacite.setText(String.format("%.2f t/ML", r.getEfficaciteHydrique()));
        } else {
            lblEfficacite.setText("N/A (rendement non renseigné)");
        }

        // ---- Stress hydrique ----
        if (r.isStressHydriqueDetecte()) {
            lblStress.setText("🔴 STRESS HYDRIQUE DÉTECTÉ");
            lblStress.setStyle("-fx-text-fill: #D32F2F; -fx-font-weight: bold; -fx-font-size: 14;");
            vboxStress.setStyle("-fx-background-color: #FFEBEE; -fx-background-radius: 10; -fx-padding: 12;");
        } else {
            lblStress.setText("✅ Aucun stress hydrique");
            lblStress.setStyle("-fx-text-fill: #388E3C; -fx-font-weight: bold; -fx-font-size: 14;");
            vboxStress.setStyle("-fx-background-color: #E8F5E9; -fx-background-radius: 10; -fx-padding: 12;");
        }
        lblCauseStress.setText(r.getCauseStress());

        // ---- Conseil ----
        lblConseil.setText(r.getConseilPrincipal());

        // Afficher le panneau résultats
        vboxResultats.setVisible(true);
        vboxResultats.setManaged(true);

        lblEtat.setText("✅ Calcul terminé avec succès");
        lblEtat.setStyle("-fx-text-fill: #388E3C;");
    }

    // ==================== UTILITAIRES ====================

    private void setEtatChargement(boolean loading) {
        progressIndicator.setVisible(loading);
        progressIndicator.setManaged(loading);
        btnCalculer.setDisable(loading);
        lblEtat.setText(loading ? "⏳ Récupération météo et calcul en cours..." : "");
        lblEtat.setStyle(loading ? "-fx-text-fill: #1565C0;" : "-fx-text-fill: #388E3C;");
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
