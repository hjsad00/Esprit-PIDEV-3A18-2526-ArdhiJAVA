package tn.neuron.ardhi.controllers.Evenement;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.neuron.ardhi.models.Evenement.Evenement;
import tn.neuron.ardhi.models.Evenement.Participation;
import tn.neuron.ardhi.services.Evenement.EvenementService;
import tn.neuron.ardhi.services.Evenement.ParticipationService;
import tn.neuron.ardhi.services.Evenement.QRCodeService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Contrôleur du scanner QR Check-in.
 * Permet à l'organisateur de valider la présence des participants via QR.
 */
public class QRCheckInController implements Initializable {

    // ── Header ────────────────────────────────────────────────────────────────
    @FXML private Label evenementTitreLabel;

    // ── Stats ────────────────────────────────────────────────────────────────
    @FXML private Label presentsLabel;
    @FXML private Label inscritsLabel;
    @FXML private Label enAttenteLabel;
    @FXML private Label tauxLabel;

    // ── Controls ─────────────────────────────────────────────────────────────
    @FXML private ComboBox<String> evenementCombo;
    @FXML private TextArea qrInputField;
    @FXML private TextField searchParticipant;

    // ── Result box ───────────────────────────────────────────────────────────
    @FXML private VBox resultBox;
    @FXML private Label resultIcon;
    @FXML private Label resultLabel;
    @FXML private Label resultDetailLabel;

    // ── Table ────────────────────────────────────────────────────────────────
    @FXML private TableView<Participation> participantsTable;
    @FXML private TableColumn<Participation, String> colNom;
    @FXML private TableColumn<Participation, String> colEmail;
    @FXML private TableColumn<Participation, String> colStatut;
    @FXML private TableColumn<Participation, Void>   colAction;

    // ── State ─────────────────────────────────────────────────────────────────
    private EvenementService evenementService;
    private ParticipationService participationService;
    private QRCodeService qrCodeService;

    private List<Evenement> allEvenements;
    private Evenement selectedEvenement;
    private ObservableList<Participation> participantsList = FXCollections.observableArrayList();

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        evenementService = new EvenementService();
        participationService = new ParticipationService();
        qrCodeService = new QRCodeService();

        setupTable();
        loadEvenements();
        setupSearch();

        // Masquer la box de résultat au démarrage
        resultBox.setVisible(false);
        resultBox.setManaged(false);

        // Auto-focus sur le champ de scan
        Platform.runLater(() -> qrInputField.requestFocus());
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupTable() {
        colNom.setCellValueFactory(data -> {
            Participation p = data.getValue();
            return new javafx.beans.property.SimpleStringProperty(p.getNomComplet());
        });
        colEmail.setCellValueFactory(new PropertyValueFactory<>("emailUtilisateur"));

        // Colonne statut — badge coloré
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }

                Participation p = getTableView().getItems().get(getIndex());
                String statut = p.getStatut();

                Label badge = new Label(getStatutEmoji(statut) + " " + getStatutLabel(statut));
                badge.setStyle(getStatutBadgeStyle(statut));
                setGraphic(badge);
                setText(null);
                setStyle("-fx-alignment: center;");
            }
        });

        // Colonne action — bouton check-in manuel
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("✅");

            {
                btn.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white;" +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 14px;" +
                        "-fx-padding: 4 10;");
                btn.setTooltip(new Tooltip("Marquer présent manuellement"));
                btn.setOnAction(e -> {
                    Participation p = getTableView().getItems().get(getIndex());
                    handleManualCheckin(p);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Participation p = getTableView().getItems().get(getIndex());
                // Désactiver si déjà présent ou annulé
                btn.setDisable("PRESENT".equals(p.getStatut()) || "ANNULE".equals(p.getStatut()));
                btn.setStyle(btn.isDisabled()
                        ? "-fx-background-color: #BDC3C7; -fx-text-fill: white;" +
                        "-fx-background-radius: 8; -fx-font-size: 14px; -fx-padding: 4 10;"
                        : "-fx-background-color: #27AE60; -fx-text-fill: white;" +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 14px;" +
                        "-fx-padding: 4 10;");

                setGraphic(btn);
                setText(null);
                setStyle("-fx-alignment: center;");
            }
        });

        participantsTable.setItems(participantsList);

        // Ligne vide stylisée
        participantsTable.setPlaceholder(new Label("Sélectionnez un événement pour afficher les participants"));
    }

    private void loadEvenements() {
        allEvenements = evenementService.getAllEvenements();
        evenementCombo.getItems().clear();

        // Filtrer : l'organisateur voit ses événements (ou admin voit tout)
        String role = UserSession.getInstance() != null
                ? UserSession.getInstance().getUser().getRole().name() : "CLIENT";
        int userId = UserSession.getInstance() != null
                ? UserSession.getInstance().getUser().getId() : 0;

        for (Evenement e : allEvenements) {
            boolean canManage = "ADMIN".equals(role) || e.getIdCreateur() == userId;
            if (canManage) {
                evenementCombo.getItems().add(e.getId() + " — " + e.getTitre());
            }
        }

        if (!evenementCombo.getItems().isEmpty()) {
            evenementCombo.getSelectionModel().selectFirst();
            handleEvenementChange();
        }
    }

    private void setupSearch() {
        searchParticipant.textProperty().addListener((obs, old, val) -> {
            if (selectedEvenement == null) return;
            String lower = val.toLowerCase();
            List<Participation> all = participationService.getParticipationsByEvenement(selectedEvenement.getId());
            participantsList.setAll(
                    all.stream().filter(p ->
                            (p.getNomComplet() != null && p.getNomComplet().toLowerCase().contains(lower)) ||
                                    (p.getEmailUtilisateur() != null && p.getEmailUtilisateur().toLowerCase().contains(lower))
                    ).toList()
            );
        });
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    @FXML
    private void handleEvenementChange() {
        int selectedIndex = evenementCombo.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0 || allEvenements == null) return;

        // Retrouver l'événement (uniquement ceux filtrés)
        String selected = evenementCombo.getValue();
        if (selected == null) return;

        int eventId = Integer.parseInt(selected.split(" — ")[0].trim());
        selectedEvenement = evenementService.getEvenementById(eventId);

        if (selectedEvenement != null) {
            evenementTitreLabel.setText("📍 " + selectedEvenement.getTitre() + " — " + selectedEvenement.getLieu());
            loadParticipants();
            updateStats();
        }
    }

    @FXML
    private void handleManualScan() {
        String input = qrInputField.getText().trim();
        if (input.isEmpty()) {
            showResult(false, "⚠️ Champ vide", "Collez un QR code ou saisissez un token", null);
            return;
        }

        qrCodeService = new QRCodeService();
        QRCodeService.CheckInResult result;

        // Détecter si c'est un token direct ou un contenu QR complet
        if (input.startsWith("ARDHI_CHECKIN|")) {
            result = qrCodeService.processQRScan(input);
        } else {
            result = qrCodeService.validateToken(input);
        }

        displayCheckInResult(result);
        qrInputField.clear();
        qrInputField.requestFocus();

        // Recharger la liste après scan
        if (result.isSuccess()) {
            loadParticipants();
            updateStats();
        }
    }

    private void handleManualCheckin(Participation p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Check-in manuel");
        alert.setHeaderText("Marquer comme présent");
        alert.setContentText("Marquer " + p.getNomComplet() + " comme présent(e) ?");

        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                boolean ok = participationService.updateStatut(p.getId(), "PRESENT");
                if (ok) {
                    showResult(true, "✅ " + p.getNomComplet() + " — Check-in réussi !",
                            "Marqué(e) présent(e) manuellement", p);
                    loadParticipants();
                    updateStats();
                } else {
                    showResult(false, "❌ Erreur", "Impossible de mettre à jour le statut", null);
                }
            }
        });
    }

    @FXML
    private void handleRefreshStats() {
        if (selectedEvenement != null) {
            loadParticipants();
            updateStats();
        }
    }

    // ── Affichage résultat ────────────────────────────────────────────────────

    private void displayCheckInResult(QRCodeService.CheckInResult result) {
        String detail = "";
        if (result.getParticipation() != null) {
            Participation p = result.getParticipation();
            detail = p.getNomComplet();
            if (result.getEvenement() != null) {
                detail += " • " + result.getEvenement().getTitre();
            }
        }
        showResult(result.isSuccess(), result.getMessage(), detail, result.getParticipation());
    }

    private void showResult(boolean success, String message, String detail, Participation p) {
        resultIcon.setText(success ? "✅" : "❌");
        resultLabel.setText(message != null ? message : "");
        resultDetailLabel.setText(detail != null ? detail : "");

        String bgColor = success
                ? "-fx-background-color: rgba(39,174,96,0.1); -fx-border-color: rgba(39,174,96,0.4);"
                : "-fx-background-color: rgba(231,76,60,0.1); -fx-border-color: rgba(231,76,60,0.4);";
        resultBox.setStyle(bgColor + " -fx-background-radius: 15; -fx-padding: 20; -fx-border-width: 2; -fx-border-radius: 15;");

        resultLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-alignment: center;" +
                "-fx-text-fill: " + (success ? "#27AE60" : "#E74C3C") + ";");

        resultBox.setVisible(true);
        resultBox.setManaged(true);

        // Fade in
        FadeTransition ft = new FadeTransition(Duration.millis(300), resultBox);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        // Auto-masquer après 5 s
        PauseTransition pause = new PauseTransition(Duration.seconds(5));
        pause.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), resultBox);
            fadeOut.setFromValue(1); fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> {
                resultBox.setVisible(false);
                resultBox.setManaged(false);
            });
            fadeOut.play();
        });
        pause.play();
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadParticipants() {
        if (selectedEvenement == null) return;
        List<Participation> list = participationService.getParticipationsByEvenement(selectedEvenement.getId());
        participantsList.setAll(list);
    }

    private void updateStats() {
        if (selectedEvenement == null) return;
        List<Participation> list = participationService.getParticipationsByEvenement(selectedEvenement.getId());

        long presents = list.stream().filter(p -> "PRESENT".equals(p.getStatut())).count();
        long inscrits = list.stream().filter(p -> !"ANNULE".equals(p.getStatut())).count();
        long enAttente = list.stream().filter(p -> "EN_ATTENTE".equals(p.getStatut())).count();
        double taux = inscrits > 0 ? (double) presents / inscrits * 100 : 0;

        presentsLabel.setText(String.valueOf(presents));
        inscritsLabel.setText(String.valueOf(inscrits));
        enAttenteLabel.setText(String.valueOf(enAttente));
        tauxLabel.setText(String.format("%.0f%%", taux));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getStatutLabel(String statut) {
        return switch (statut) {
            case "PRESENT"   -> "Présent";
            case "CONFIRME"  -> "Confirmé";
            case "EN_ATTENTE"-> "En attente";
            case "ANNULE"    -> "Annulé";
            default          -> statut;
        };
    }

    private String getStatutEmoji(String statut) {
        return switch (statut) {
            case "PRESENT"   -> "✅";
            case "CONFIRME"  -> "🟢";
            case "EN_ATTENTE"-> "⏳";
            case "ANNULE"    -> "❌";
            default          -> "•";
        };
    }

    private String getStatutBadgeStyle(String statut) {
        String base = "-fx-background-radius: 10; -fx-padding: 4 10; -fx-font-size: 11px; -fx-font-weight: bold;";
        return base + switch (statut) {
            case "PRESENT"   -> "-fx-background-color: #50C878; -fx-text-fill: white;";
            case "CONFIRME"  -> "-fx-background-color: #3498DB; -fx-text-fill: white;";
            case "EN_ATTENTE"-> "-fx-background-color: #F39C12; -fx-text-fill: white;";
            case "ANNULE"    -> "-fx-background-color: #E74C3C; -fx-text-fill: white;";
            default          -> "-fx-background-color: #95A5A6; -fx-text-fill: white;";
        };
    }

    @FXML
    private void handleRetour(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/Evenement/NavigationEvenements.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, WindowUtils.APP_WIDTH, WindowUtils.APP_HEIGHT));
            stage.setTitle("Ardhi - Module Événements");
            WindowUtils.applyStandardSize(stage);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}