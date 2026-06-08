package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import tn.neuron.ardhi.models.UserAndDiag.Diagnostic;
import tn.neuron.ardhi.models.UserAndDiag.Offre;
import tn.neuron.ardhi.models.UserAndDiag.Traitement;
import tn.neuron.ardhi.services.UserAndDiag.AbonnementService;
import tn.neuron.ardhi.services.UserAndDiag.DiagnosticService;
import tn.neuron.ardhi.services.UserAndDiag.TraitementService;
import tn.neuron.ardhi.services.UserAndDiag.SpeechToTextService;
import tn.neuron.ardhi.utils.UserAndDiag.SubscriptionConfig;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;

public class DiagnosticHistoryController implements Initializable {

    @FXML
    private TextField tfRecherche;

    @FXML
    private TableView<Diagnostic> tableHistorique;

    @FXML
    private TableColumn<Diagnostic, String> colDate;

    @FXML
    private TableColumn<Diagnostic, String> colResultatIA;

    @FXML
    private TableColumn<Diagnostic, String> colConfiance;

    @FXML
    private Label lblInfo;
    @FXML
    private Button btnMicSearch;

    private final DiagnosticService diagnosticService = new DiagnosticService();
    private final TraitementService traitementService = new TraitementService();
    private final AbonnementService abonnementService = new AbonnementService();
    private final SpeechToTextService sttService = new SpeechToTextService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private int currentUserId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUserId = UserSession.getInstance().getUser().getId();

        // Configuration des colonnes
        colDate.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDateScan() != null) {
                return new SimpleStringProperty(dateFormat.format(cellData.getValue().getDateScan()));
            }
            return new SimpleStringProperty("N/A");
        });

        colResultatIA.setCellValueFactory(cellData -> {
            String resultat = cellData.getValue().getResultatIA();
            if (resultat != null && resultat.length() > 80) {
                resultat = resultat.substring(0, 77) + "...";
            }
            return new SimpleStringProperty(resultat != null ? resultat : "N/A");
        });

        colConfiance.setCellValueFactory(cellData -> {
            float confiance = cellData.getValue().getConfiance();
            return new SimpleStringProperty(String.format("%.0f%%", confiance));
        });

        chargerHistorique();

        tfRecherche.textProperty().addListener((observable, oldValue, newValue) -> {
            rechercherDiagnostics(newValue);
        });

        // Double-click to view treatment
        tableHistorique.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                showTreatmentForSelectedDiagnostic();
            }
        });
    }

    private void chargerHistorique() {
        List<Diagnostic> diagnostics = diagnosticService.recupererHistoriqueParUser(currentUserId);
        tableHistorique.setItems(FXCollections.observableArrayList(diagnostics));
        mettreAJourInfo(diagnostics.size());
    }

    private void rechercherDiagnostics(String keyword) {
        List<Diagnostic> diagnostics;
        if (keyword == null || keyword.trim().isEmpty()) {
            diagnostics = diagnosticService.recupererHistoriqueParUser(currentUserId);
        } else {
            diagnostics = diagnosticService.rechercherHistoriqueParUser(currentUserId, keyword.trim());
        }
        tableHistorique.setItems(FXCollections.observableArrayList(diagnostics));
        mettreAJourInfo(diagnostics.size());
    }

    private void mettreAJourInfo(int count) {
        if (count == 0) {
            lblInfo.setText("Aucun diagnostic trouvé");
        } else if (count == 1) {
            lblInfo.setText("1 diagnostic trouvé");
        } else {
            lblInfo.setText(count + " diagnostics trouvés");
        }
    }

    @FXML
    void retourDashboard(ActionEvent event) {
        WindowUtils.goBack(event);
    }

    @FXML
    void reinitialiser(ActionEvent event) {
        tfRecherche.clear();
        chargerHistorique();
    }

    @FXML
    void voiceSearch(ActionEvent event) {
        if (sttService.isRecording()) {
            btnMicSearch.setText("⏳");
            btnMicSearch.setDisable(true);
            sttService.stopAndTranscribe(
                    text -> {
                        tfRecherche.setText(text);
                        btnMicSearch.setText("🎙️");
                        btnMicSearch.setDisable(false);
                    },
                    error -> {
                        btnMicSearch.setText("🎙️");
                        btnMicSearch.setDisable(false);
                    });
        } else {
            sttService.startRecording();
            btnMicSearch.setText("⏹️");
            btnMicSearch.setStyle(
                    "-fx-background-color: #e74c3c; -fx-background-radius: 20; -fx-cursor: hand; -fx-min-width: 38; -fx-min-height: 38;");
        }
    }

    /**
     * Affiche le traitement associé au diagnostic sélectionné.
     * Vérifie les privilèges d'abonnement avant d'afficher.
     */
    private void showTreatmentForSelectedDiagnostic() {
        Diagnostic selected = tableHistorique.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        // Check subscription privileges
        try {
            Offre offre = abonnementService.getOffreActiveParUser(currentUserId);
            boolean canSeeTreatment = false;

            if (offre != null && offre.isAccesTraitement()) {
                canSeeTreatment = true;
            } else if (offre == null && SubscriptionConfig.isFreeAccesTraitement()) {
                canSeeTreatment = true; // Free tier has treatment access
            }

            if (!canSeeTreatment) {
                WindowUtils.showAlert("Accès restreint",
                        "🔒 L'accès aux traitements nécessite un abonnement avec cette fonctionnalité activée.\n\n" +
                                "Souscrivez à une offre incluant l'accès aux traitements pour débloquer cette fonctionnalité.");
                return;
            }

            // Fetch and display treatment
            Traitement traitement = traitementService.getByDiagnosticId(selected.getId());
            if (traitement != null) {
                String message = "🌿 " + traitement.getSolutionNom() + "\n\n" +
                        "Type: " + traitement.getTypeTraitement().name() + "\n\n" +
                        "Description:\n" + traitement.getDescriptionDetaillee();
                WindowUtils.showAlert("Traitement Recommandé", message);
            } else {
                WindowUtils.showAlert("Information", "Aucun traitement associé à ce diagnostic.");
            }
        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", "Impossible de récupérer le traitement: " + e.getMessage());
        }
    }
}