package tn.neuron.ardhi.controllers.Evenement;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import tn.neuron.ardhi.models.Evenement.Participation;
import tn.neuron.ardhi.models.Evenement.Evenement;
import tn.neuron.ardhi.models.UserAndDiag.Role;
import tn.neuron.ardhi.services.Evenement.*;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

public class ParticipationManagementController implements Initializable {

    // ── FXML ────────────────────────────────────────────────────────────────
    @FXML private Label pageTitle;

    @FXML private TableView<Participation> participationsTable;
    @FXML private TableColumn<Participation, Integer> idColumn;
    @FXML private TableColumn<Participation, String> nomColumn;
    @FXML private TableColumn<Participation, String> prenomColumn;
    @FXML private TableColumn<Participation, String> emailColumn;
    @FXML private TableColumn<Participation, String> evenementColumn;
    @FXML private TableColumn<Participation, LocalDateTime> dateInscriptionColumn;
    @FXML private TableColumn<Participation, String> statutColumn;
    @FXML private TableColumn<Participation, String> commentaireColumn;
    @FXML private TableColumn<Participation, Void> actionsColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatutCombo;

    @FXML private Label totalLabel;
    @FXML private Label confirmesLabel;
    @FXML private Label enAttenteLabel;
    @FXML private Label annulesLabel;

    // ── état interne ────────────────────────────────────────────────────────
    private ParticipationService participationService;
    private ObservableList<Participation> participationsList;

    private Role userRole;
    private int userId;
    private EmailSchedulerService emailScheduler;
    private boolean isAdmin;

    // ── initialisation ──────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        participationService = new ParticipationService();
        participationsList = FXCollections.observableArrayList();

        userRole = UserSession.getInstance().getUser().getRole();
        userId = UserSession.getInstance().getUser().getId();
        isAdmin = (userRole == Role.ADMIN);

        if (pageTitle != null) {
            pageTitle.setText(isAdmin
                    ? "👥 Gestion des Participations — Tous les Événements"
                    : "👥 Gestion des Participations — Mes Événements");
        }

        setupTable();
        setupFilters();
        loadAllParticipations();
        updateStatistics();
    }

    // ── source de données centralisée ───────────────────────────────────────
    private List<Participation> fetchAll() {
        return isAdmin
                ? participationService.getAllParticipations()
                : participationService.getParticipationsByCreateurEvenement(userId);
    }

    // ── table ───────────────────────────────────────────────────────────────
    private void setupTable() {
        participationsTable.setStyle(
                "-fx-background-color: white;" +
                        "-fx-table-cell-border-color: #E0E0E0;" +
                        "-fx-font-size: 13px;");

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nomUtilisateur"));
        prenomColumn.setCellValueFactory(new PropertyValueFactory<>("prenomUtilisateur"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("emailUtilisateur"));
        evenementColumn.setCellValueFactory(new PropertyValueFactory<>("titreEvenement"));
        commentaireColumn.setCellValueFactory(new PropertyValueFactory<>("commentaire"));
        statutColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));

        String headerStyle = "-fx-background-color: #f8f9fa; -fx-text-fill: #2C3E50; -fx-font-weight: bold; -fx-font-size: 13px; -fx-alignment: CENTER; -fx-border-color: #dee2e6;";
        idColumn.setStyle(headerStyle);
        nomColumn.setStyle(headerStyle);
        prenomColumn.setStyle(headerStyle);
        emailColumn.setStyle(headerStyle);
        evenementColumn.setStyle(headerStyle);
        dateInscriptionColumn.setStyle(headerStyle);
        statutColumn.setStyle(headerStyle);
        commentaireColumn.setStyle(headerStyle);
        actionsColumn.setStyle(headerStyle);

        dateInscriptionColumn.setCellValueFactory(new PropertyValueFactory<>("dateInscription"));
        dateInscriptionColumn.setCellFactory(column -> new TableCell<Participation, LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? null : formatter.format(date));
            }
        });

        statutColumn.setCellFactory(column -> new TableCell<Participation, String>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                String displayText = switch (statut) {
                    case "CONFIRME"   -> "Confirmé";
                    case "EN_ATTENTE" -> "En Attente";
                    case "ANNULE"     -> "Annulé";
                    case "PRESENT"    -> "Présent";
                    default           -> statut;
                };
                setText(displayText);

                String color = switch (statut) {
                    case "CONFIRME"   -> "-fx-background-color: #50c878; -fx-text-fill: white;";
                    case "EN_ATTENTE" -> "-fx-background-color: #f5a623; -fx-text-fill: white;";
                    case "ANNULE"     -> "-fx-background-color: #d4145a; -fx-text-fill: white;";
                    case "PRESENT"    -> "-fx-background-color: #4a90e2; -fx-text-fill: white;";
                    default           -> "-fx-background-color: #cccccc; -fx-text-fill: white;";
                };
                setStyle(color + " -fx-alignment: center; -fx-padding: 8 15; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 12px;");
            }
        });

        // ── colonne actions ──────────────────────────────────────────────────
        actionsColumn.setCellFactory(new Callback<TableColumn<Participation, Void>, TableCell<Participation, Void>>() {
            @Override
            public TableCell<Participation, Void> call(TableColumn<Participation, Void> param) {
                return new TableCell<Participation, Void>() {
                    private final Button validerButton  = new Button("Confirmer");
                    private final Button attenteButton  = new Button("En attente");
                    private final Button refuserButton  = new Button("Annuler");
                    private final Button presenceButton = new Button("Présent");
                    private final Button deleteButton   = new Button("Supprimer");
                    // ✅ NOUVEAU : bouton QR Code
                    private final Button qrButton       = new Button("📱 QR");
                    private final HBox buttonsBox = new HBox(5);

                    {
                        buttonsBox.setAlignment(javafx.geometry.Pos.CENTER);
                        buttonsBox.setStyle("-fx-padding: 5;");

                        validerButton.setStyle(
                                "-fx-background-color: #50c878; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 6 12; -fx-cursor: hand; -fx-background-radius: 5;");
                        validerButton.setTooltip(new Tooltip("Valider la participation"));
                        validerButton.setMinWidth(80); validerButton.setMaxWidth(80);

                        attenteButton.setStyle(
                                "-fx-background-color: #f5a623; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 6 12; -fx-cursor: hand; -fx-background-radius: 5;");
                        attenteButton.setTooltip(new Tooltip("Mettre en attente"));
                        attenteButton.setMinWidth(80); attenteButton.setMaxWidth(80);

                        refuserButton.setStyle(
                                "-fx-background-color: #d4145a; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 6 12; -fx-cursor: hand; -fx-background-radius: 5;");
                        refuserButton.setTooltip(new Tooltip("Refuser / Annuler"));
                        refuserButton.setMinWidth(80); refuserButton.setMaxWidth(80);

                        presenceButton.setStyle(
                                "-fx-background-color: #4a90e2; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 6 12; -fx-cursor: hand; -fx-background-radius: 5;");
                        presenceButton.setTooltip(new Tooltip("Marquer présent"));
                        presenceButton.setMinWidth(80); presenceButton.setMaxWidth(80);

                        deleteButton.setStyle(
                                "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 6 12; -fx-cursor: hand; -fx-background-radius: 5;");
                        deleteButton.setTooltip(new Tooltip("Supprimer"));
                        deleteButton.setMinWidth(80); deleteButton.setMaxWidth(80);

                        // ✅ Style bouton QR
                        qrButton.setStyle(
                                "-fx-background-color: #9B59B6; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 6 12; -fx-cursor: hand; -fx-background-radius: 5;");
                        qrButton.setTooltip(new Tooltip("Afficher / Régénérer le QR code de check-in"));
                        qrButton.setMinWidth(65); qrButton.setMaxWidth(65);

                        validerButton.setOnAction(event -> {
                            Participation p = getTableView().getItems().get(getIndex());
                            validerParticipation(p);
                        });
                        attenteButton.setOnAction(event -> {
                            Participation p = getTableView().getItems().get(getIndex());
                            mettreEnAttente(p);
                        });
                        refuserButton.setOnAction(event -> {
                            Participation p = getTableView().getItems().get(getIndex());
                            refuserParticipation(p);
                        });
                        presenceButton.setOnAction(event -> {
                            Participation p = getTableView().getItems().get(getIndex());
                            marquerPresence(p);
                        });
                        deleteButton.setOnAction(event -> {
                            Participation p = getTableView().getItems().get(getIndex());
                            supprimerParticipation(p);
                        });
                        // ✅ Handler bouton QR
                        qrButton.setOnAction(event -> {
                            Participation p = getTableView().getItems().get(getIndex());
                            afficherQRParticipant(p);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) { setGraphic(null); return; }

                        Participation p = getTableView().getItems().get(getIndex());
                        buttonsBox.getChildren().clear();

                        // Slot 1 : Confirmer ou En attente
                        if ("EN_ATTENTE".equals(p.getStatut())) {
                            buttonsBox.getChildren().add(validerButton);
                        } else if ("CONFIRME".equals(p.getStatut())) {
                            buttonsBox.getChildren().add(attenteButton);
                        } else {
                            javafx.scene.layout.Region ph = new javafx.scene.layout.Region();
                            ph.setMinWidth(80); ph.setMaxWidth(80);
                            buttonsBox.getChildren().add(ph);
                        }

                        // Slot 2 : Annuler
                        if (!"ANNULE".equals(p.getStatut()) && !"PRESENT".equals(p.getStatut())) {
                            buttonsBox.getChildren().add(refuserButton);
                        } else {
                            javafx.scene.layout.Region ph = new javafx.scene.layout.Region();
                            ph.setMinWidth(80); ph.setMaxWidth(80);
                            buttonsBox.getChildren().add(ph);
                        }

                        // Slot 3 : Présent
                        if ("CONFIRME".equals(p.getStatut())) {
                            buttonsBox.getChildren().add(presenceButton);
                        } else {
                            javafx.scene.layout.Region ph = new javafx.scene.layout.Region();
                            ph.setMinWidth(80); ph.setMaxWidth(80);
                            buttonsBox.getChildren().add(ph);
                        }

                        // Slot 4 : Supprimer (admin seulement)
                        if (isAdmin) {
                            buttonsBox.getChildren().add(deleteButton);
                        }

                        // ✅ Slot 5 : QR Code (si CONFIRME ou PRESENT)
                        if ("CONFIRME".equals(p.getStatut()) || "PRESENT".equals(p.getStatut())) {
                            buttonsBox.getChildren().add(qrButton);
                        }

                        setGraphic(buttonsBox);
                    }
                };
            }
        });

        participationsTable.setItems(participationsList);
    }

    // ── filtres ─────────────────────────────────────────────────────────────
    private void setupFilters() {
        filterStatutCombo.getItems().addAll("Tous", "CONFIRME", "EN_ATTENTE", "ANNULE", "PRESENT");
        filterStatutCombo.setValue("Tous");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterStatutCombo.setOnAction(e -> applyFilters());
    }

    private void loadAllParticipations() {
        participationsList.clear();
        participationsList.addAll(fetchAll());
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        String statut = filterStatutCombo.getValue();

        List<Participation> filtered = fetchAll().stream()
                .filter(p -> {
                    boolean matchSearch = searchText.isEmpty()
                            || (p.getNomUtilisateur() != null && p.getNomUtilisateur().toLowerCase().contains(searchText))
                            || (p.getPrenomUtilisateur() != null && p.getPrenomUtilisateur().toLowerCase().contains(searchText))
                            || (p.getEmailUtilisateur() != null && p.getEmailUtilisateur().toLowerCase().contains(searchText))
                            || (p.getTitreEvenement() != null && p.getTitreEvenement().toLowerCase().contains(searchText));
                    boolean matchStatut = "Tous".equals(statut) || statut.equals(p.getStatut());
                    return matchSearch && matchStatut;
                })
                .toList();

        participationsList.clear();
        participationsList.addAll(filtered);
    }

    // ── actions sur une participation ───────────────────────────────────────
    private void validerParticipation(Participation p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Valider la participation");
        alert.setContentText("Valider la participation de " + p.getNomComplet() + " ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (participationService.updateStatut(p.getId(), "CONFIRME")) {
                showSuccess("Participation validée");
                loadAllParticipations();
                updateStatistics();
            } else {
                showError("Erreur lors de la validation");
            }
        }
    }

    private void mettreEnAttente(Participation p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Mettre en attente");
        alert.setContentText("Mettre la participation de " + p.getNomComplet() + " en attente ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (participationService.updateStatut(p.getId(), "EN_ATTENTE")) {
                showSuccess("Participation mise en attente");
                loadAllParticipations();
                updateStatistics();
            } else {
                showError("Erreur lors de la mise en attente");
            }
        }
    }

    private void refuserParticipation(Participation p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Refuser la participation");
        alert.setContentText("Refuser / Annuler la participation de " + p.getNomComplet() + " ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (participationService.updateStatut(p.getId(), "ANNULE")) {
                showSuccess("Participation annulée");
                loadAllParticipations();
                updateStatistics();
            } else {
                showError("Erreur lors de l'annulation");
            }
        }
    }

    private void marquerPresence(Participation p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Marquer la présence");
        alert.setContentText("Marquer " + p.getNomComplet() + " comme présent ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (participationService.updateStatut(p.getId(), "PRESENT")) {
                showSuccess("Présence enregistrée");
                loadAllParticipations();
                updateStatistics();
            } else {
                showError("Erreur lors de l'enregistrement");
            }
        }
    }

    private void supprimerParticipation(Participation p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer la participation");
        alert.setContentText("Supprimer définitivement la participation de " + p.getNomComplet() + " ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (participationService.supprimerParticipation(p.getId())) {
                showSuccess("Participation supprimée");
                loadAllParticipations();
                updateStatistics();
            } else {
                showError("Erreur lors de la suppression");
            }
        }
    }

    // ✅ NOUVELLE MÉTHODE : Affiche le QR code du participant dans une popup
    private void afficherQRParticipant(Participation p) {
        try {
            EvenementService evtService = new EvenementService();
            Evenement event = evtService.getEvenementById(p.getIdEvenement());
            if (event == null) {
                showError("Événement introuvable pour cette participation");
                return;
            }

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/Evenement/QRCodeViewer.fxml"));
            Parent root = loader.load();

            QRCodeViewerController controller = loader.getController();
            controller.setData(p, event);

            Stage stage = new Stage();
            stage.setTitle("📱 QR Code — " + p.getNomComplet());
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Impossible d'ouvrir le QR viewer: " + e.getMessage());
        }
    }

    // ── statistiques ────────────────────────────────────────────────────────
    private void updateStatistics() {
        List<Participation> all = fetchAll();
        totalLabel.setText(String.valueOf(all.size()));
        confirmesLabel.setText(String.valueOf(all.stream().filter(p -> "CONFIRME".equals(p.getStatut())).count()));
        enAttenteLabel.setText(String.valueOf(all.stream().filter(p -> "EN_ATTENTE".equals(p.getStatut())).count()));
        annulesLabel.setText(String.valueOf(all.stream().filter(p -> "ANNULE".equals(p.getStatut())).count()));
    }

    // ── boutons header ───────────────────────────────────────────────────────
    @FXML
    private void handleRefresh() {
        loadAllParticipations();
        updateStatistics();
    }

    @FXML
    private void handleGenererAttestations() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Génération des attestations");
        confirmAlert.setHeaderText("Générer les attestations de présence");
        confirmAlert.setContentText(
                "Voulez-vous générer les attestations PDF pour tous les participants marqués PRESENT ?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            javafx.stage.DirectoryChooser dirChooser = new javafx.stage.DirectoryChooser();
            dirChooser.setTitle("Sélectionner le dossier pour les attestations");
            java.io.File selectedDir = dirChooser.showDialog(participationsTable.getScene().getWindow());

            if (selectedDir != null) {
                AttestationPDFService pdfService = new AttestationPDFService();
                EvenementService evtService = new EvenementService();
                int totalGenerated = 0;

                List<Participation> presents = fetchAll().stream()
                        .filter(p -> "PRESENT".equals(p.getStatut()))
                        .toList();

                for (Participation p : presents) {
                    Evenement event = evtService.getEvenementById(p.getIdEvenement());
                    if (event != null) {
                        String filename = selectedDir.getAbsolutePath() +
                                "/Attestation_" + event.getTitre().replaceAll("[^a-zA-Z0-9]", "_") +
                                "_" + p.getNomUtilisateur() + "_" + p.getPrenomUtilisateur() + ".pdf";
                        if (pdfService.genererAttestation(p, event, filename)) totalGenerated++;
                    }
                }

                showSuccess(totalGenerated + " attestation(s) générée(s) avec succès dans:\n" +
                        selectedDir.getAbsolutePath());
            }
        }
    }

    @FXML
    private void handleEnvoyerRappels() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Envoi de rappels");
        confirmAlert.setHeaderText("Envoyer des rappels par email");
        confirmAlert.setContentText("Voulez-vous envoyer des rappels aux participants confirmés des événements à venir ?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            EmailsService emailService = new EmailsService();
            Map<String, Integer> stats = emailService.envoyerRappelsAutomatiques();
            int total = stats.getOrDefault("rappels3jours", 0) + stats.getOrDefault("rappels1jour", 0);

            if (total > 0) {
                showSuccess(total + " email(s) de rappel envoyé(s) avec succès!\n" +
                        "Rappels 3 jours: " + stats.get("rappels3jours") + "\n" +
                        "Rappels 1 jour: " + stats.get("rappels1jour"));
            } else {
                showInfo("Aucun rappel à envoyer pour le moment.\n" +
                        "Vérifiez la configuration SMTP dans EmailsService.java");
            }
        }
    }

    @FXML
    private void handleRetour(ActionEvent event) {
        navigateTo(event, "/fxml/Evenement/NavigationEvenements.fxml", "Module Événements");
    }

    private void navigateTo(ActionEvent event, String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, WindowUtils.APP_WIDTH, WindowUtils.APP_HEIGHT));
            stage.setTitle("Ardhi - " + title);
            stage.setResizable(true);
            WindowUtils.applyStandardSize(stage);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur lors du chargement de : " + fxmlFile);
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur"); alert.setHeaderText(null);
        alert.setContentText(message); alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès"); alert.setHeaderText(null);
        alert.setContentText(message); alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information"); alert.setHeaderText(null);
        alert.setContentText(message); alert.showAndWait();
    }

    @FXML
    private void handleDetectInactifs() {
        InactiveParticipantDetectionService detectionService = new InactiveParticipantDetectionService();
        List<InactiveParticipantDetectionService.ParticipantRiskProfile> profiles =
                detectionService.detecterParticipantsInactifs();
        InactiveParticipantDetectionService.InactivityStats stats = detectionService.genererStatistiques(profiles);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détection Participants Inactifs");
        alert.setHeaderText("Analyse Complète");
        alert.setContentText(String.format("""
                📊 STATISTIQUES GLOBALES
                
                Total analysé: %d participants
                🔴 Risque urgent: %d
                🟠 Risque important: %d
                🟡 Risque modéré: %d
                
                Score moyen: %.1f/100
                
                Voulez-vous envoyer les relances automatiques?
                """,
                stats.getTotalAnalyses(), stats.getRisqueUrgent(),
                stats.getRisqueImportant(), stats.getRisqueModere(), stats.getScoreMoyen()));

        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            Map<String, Integer> relanceStats = detectionService.envoyerRelancesAutomatiques(profiles);
            showSuccess(String.format(
                    "Relances envoyées:\n🔴 Urgentes: %d\n🟠 Importantes: %d\n🟡 Standard: %d\nTotal: %d emails",
                    relanceStats.get("urgentes"), relanceStats.get("importantes"),
                    relanceStats.get("standard"), relanceStats.get("total")));
        }
    }
}