package tn.neuron.ardhi.controllers.Parcelle_Cultures;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.neuron.ardhi.models.Parcelle_Cultures.CreditDossier;
import tn.neuron.ardhi.models.Parcelle_Cultures.Parcelle;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.services.Parcelle_Cultures.CreditAnalysisService;
import tn.neuron.ardhi.services.Parcelle_Cultures.CultureService;
import tn.neuron.ardhi.services.Parcelle_Cultures.PdfCreditExportService;
import tn.neuron.ardhi.services.Parcelle_Cultures.ParcelleService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Contrôleur du module Premium — Dossier de Crédit Agricole.
 *
 * Flux : Sélection langue/durée → Analyse (CreditAnalysisService) → Aperçu → Export PDF (PdfCreditExportService)
 */
public class CreditDossierController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(CreditDossierController.class.getName());

    // ==================== FXML – Left Panel ====================
    @FXML private Label lblRefParcelle;
    @FXML private Label lblLocalisation;
    @FXML private Label lblSurface;
    @FXML private Label lblTypeSol;
    @FXML private Label lblIrrigation;
    @FXML private Label lblNbCultures;

    @FXML private ComboBox<Parcelle> cbParcelle;
    @FXML private ComboBox<String> cbLangue;
    @FXML private Spinner<Integer> spinDuree;
    @FXML private Button btnGenerer;
    @FXML private Label lblStatus;

    // ==================== FXML – Right Panel ====================
    @FXML private VBox vboxPreview;
    @FXML private Label lblCouts;
    @FXML private Label lblCA;
    @FXML private Label lblMarge;
    @FXML private Label lblROI;
    @FXML private Label lblMontantPret;
    @FXML private Label lblCapacite;
    @FXML private Label lblScoreRisque;
    @FXML private Label lblNiveauRisque;
    @FXML private Label lblScoreRenta;
    @FXML private Label lblScoreClimat;
    @FXML private Label lblScoreDiversif;
    @FXML private Label lblScoreHisto;

    // ==================== State ====================
    private Parcelle parcelle;
    private User currentUser;
    private CreditDossier dossierCourant;

    private final CreditAnalysisService analysisService = new CreditAnalysisService();
    private final PdfCreditExportService pdfService = new PdfCreditExportService();
    private final CultureService cultureService = new CultureService();
    private final ParcelleService parcelleService = new ParcelleService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = UserSession.getInstance().getUser();
        if (currentUser == null) return;

        // Charger les parcelles
        try {
            cbParcelle.setItems(FXCollections.observableArrayList(parcelleService.recupererParAgriculteur(currentUser.getId())));
        } catch (Exception e) {
            LOGGER.warning("Erreur chargement parcelles: " + e.getMessage());
        }

        // Langue
        cbLangue.setItems(FXCollections.observableArrayList(
                "Français", "العربية", "English"
        ));
        cbLangue.getSelectionModel().selectFirst();

        // Durée (1-15 ans, défaut 5)
        spinDuree.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 15, 5));

        // Listeners
        cbParcelle.setOnAction(e -> {
            this.parcelle = cbParcelle.getValue();
            afficherInfoParcelle();
            recalculer();
        });
        cbLangue.setOnAction(e -> recalculer());
        spinDuree.valueProperty().addListener((obs, o, n) -> recalculer());
    }

    /**
     * Méthode appelée par le contrôleur parent pour injecter la parcelle sélectionnée.
     */
    public void setParcelle(Parcelle parcelle) {
        this.parcelle = parcelle;
        if (parcelle != null) {
            cbParcelle.getSelectionModel().select(parcelle);
        }
        afficherInfoParcelle();
        recalculer();
    }

    // ==================== AFFICHAGE INFO PARCELLE ====================

    private void afficherInfoParcelle() {
        if (parcelle == null) return;

        lblRefParcelle.setText("P-" + parcelle.getId());
        lblLocalisation.setText(parcelle.getLocalisation() != null ? parcelle.getLocalisation() : "—");
        lblSurface.setText(String.format("%.2f ha", parcelle.getSurface()));
        lblTypeSol.setText(parcelle.getTypeSol() != null ? parcelle.getTypeSol() : "—");
        lblIrrigation.setText(parcelle.getSystemeIrrigation() != null ? parcelle.getSystemeIrrigation() : "—");

        try {
            int nbCultures = cultureService.recupererParParcelle(parcelle.getId()).size();
            lblNbCultures.setText(nbCultures + " culture(s)");
        } catch (Exception e) {
            lblNbCultures.setText("—");
        }
    }

    // ==================== ANALYSE & APERÇU ====================

    private void recalculer() {
        if (parcelle == null || currentUser == null) return;

        String langCode = getLangueCode();
        int duree = spinDuree.getValue();

        lblStatus.setText("⏳ Analyse en cours...");
        lblStatus.setStyle("-fx-text-fill: #888888; -fx-font-size: 11;");

        // Exécuter l'analyse en tâche de fond
        new Thread(() -> {
            try {
                CreditDossier dossier = analysisService.genererDossier(parcelle, currentUser, duree, langCode);
                dossierCourant = dossier;

                Platform.runLater(() -> {
                    afficherApercu(dossier);
                    lblStatus.setText("✅ Analyse terminée — Prêt à générer le PDF");
                    lblStatus.setStyle("-fx-text-fill: #6B7F3F; -fx-font-size: 11;");
                    btnGenerer.setDisable(false);
                });
            } catch (Exception e) {
                LOGGER.warning("Erreur analyse crédit: " + e.getMessage());
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Erreur : " + e.getMessage());
                    lblStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11;");
                });
            }
        }).start();
    }

    private void afficherApercu(CreditDossier d) {
        // Financier
        lblCouts.setText(String.format("%,.2f DT", d.getCoutsTotaux()));
        lblCA.setText(String.format("%,.2f DT", d.getChiffreAffaires()));
        lblMarge.setText(String.format("%,.2f DT", d.getMargeBrute()));
        lblMarge.setStyle(d.getMargeBrute() >= 0
                ? "-fx-font-weight: bold; -fx-text-fill: #6B7F3F;"
                : "-fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        lblROI.setText(String.format("%.1f %%", d.getRoi()));

        // Prêt
        lblMontantPret.setText(String.format("%,.2f DT", d.getMontantPretMax()));
        lblCapacite.setText(String.format("Capacité remboursement : %,.2f DT/an", d.getCapaciteRemboursement()));

        // Risque
        lblScoreRisque.setText(String.format("%.1f/10", d.getScoreRisque()));
        lblScoreRisque.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: " + getScoreHexColor(d.getScoreRisque()) + ";");

        lblNiveauRisque.setText(d.getNiveauRisque().toUpperCase());
        lblNiveauRisque.setStyle("-fx-text-fill: white; -fx-background-color: " + getScoreHexColor(d.getScoreRisque())
                + "; -fx-background-radius: 5; -fx-padding: 4 14; -fx-font-weight: bold; -fx-font-size: 12;");

        lblScoreRenta.setText(String.format("%.1f / 10", d.getScoreRentabilite()));
        lblScoreClimat.setText(String.format("%.1f / 10", d.getScoreStabiliteClimat()));
        lblScoreDiversif.setText(String.format("%.1f / 10", d.getScoreDiversification()));
        lblScoreHisto.setText(String.format("%.1f / 10", d.getScoreHistorique()));
    }

    private String getScoreHexColor(double score) {
        if (score >= 7) return "#2ecc71";
        if (score >= 4) return "#f39c12";
        return "#e74c3c";
    }

    // ==================== GÉNÉRATION PDF ====================

    @FXML
    void genererDossier(javafx.event.ActionEvent event) {
        if (dossierCourant == null) {
            WindowUtils.showAlert("Attention", "Veuillez attendre la fin de l'analyse avant de générer le PDF.");
            return;
        }

        // FileChooser pour le chemin de sauvegarde
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le Dossier Crédit");
        fileChooser.setInitialFileName("Dossier_Credit_Ardhi_P" + parcelle.getId() + ".pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Document PDF", "*.pdf"));

        // Dossier initial : Bureau de l'utilisateur
        File desktopDir = new File(System.getProperty("user.home"), "Desktop");
        if (desktopDir.exists()) {
            fileChooser.setInitialDirectory(desktopDir);
        }

        Stage stage = (Stage) btnGenerer.getScene().getWindow();
        File fichierSortie = fileChooser.showSaveDialog(stage);

        if (fichierSortie == null) return; // Annulé

        btnGenerer.setDisable(true);
        lblStatus.setText("⏳ Génération du PDF en cours...");
        lblStatus.setStyle("-fx-text-fill: #888888; -fx-font-size: 11;");

        new Thread(() -> {
            try {
                // Mettre à jour la langue et la durée
                dossierCourant.setLangue(getLangueCode());
                dossierCourant.setDureeEmpruntAnnees(spinDuree.getValue());

                pdfService.genererPdf(dossierCourant, fichierSortie.getAbsolutePath());

                Platform.runLater(() -> {
                    lblStatus.setText("✅ PDF généré avec succès !");
                    lblStatus.setStyle("-fx-text-fill: #6B7F3F; -fx-font-size: 11; -fx-font-weight: bold;");
                    btnGenerer.setDisable(false);

                    // Confirmation
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Dossier Crédit Généré");
                    alert.setHeaderText("PDF créé avec succès !");
                    alert.setContentText("Le dossier a été sauvegardé :\n" + fichierSortie.getAbsolutePath()
                            + "\n\nVous pouvez le présenter à votre banque.");
                    alert.showAndWait();

                    // Ouvrir le fichier
                    try {
                        java.awt.Desktop.getDesktop().open(fichierSortie);
                    } catch (Exception ignored) {}
                });
            } catch (Exception e) {
                LOGGER.warning("Erreur génération PDF: " + e.getMessage());
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Erreur : " + e.getMessage());
                    lblStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11;");
                    btnGenerer.setDisable(false);
                    WindowUtils.showAlert("Erreur", "Impossible de générer le PDF :\n" + e.getMessage());
                });
            }
        }).start();
    }

    // ==================== NAVIGATION ====================

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/AgriculteurDashboard.fxml", "Ardhi - Tableau de bord");
    }

    // ==================== UTILITAIRES ====================

    private String getLangueCode() {
        String selected = cbLangue.getSelectionModel().getSelectedItem();
        if (selected == null) return "fr";
        return switch (selected) {
            case "العربية" -> "ar";
            case "English" -> "en";
            default -> "fr";
        };
    }
}
