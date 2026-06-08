package tn.neuron.ardhi.controllers.Evenement;

import javafx.stage.Stage;
import javafx.scene.Scene;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.neuron.ardhi.models.Evenement.Evenement;
import tn.neuron.ardhi.models.Evenement.Participation;
import tn.neuron.ardhi.services.Evenement.EvenementService;
import tn.neuron.ardhi.services.Evenement.ParticipationService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.services.Evenement.CalendarExportService;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class EvenementDetailsController implements Initializable {

    @FXML private Label titreLabel;
    @FXML private Label typeLabel;
    @FXML private Label statutLabel;
    @FXML private Label dateLabel;
    @FXML private Label lieuLabel;
    @FXML private Label organisateurLabel;
    @FXML private TextArea descriptionArea;
    @FXML private Label placesLabel;
    @FXML private ProgressBar placesProgressBar;
    @FXML private Label noteMoyenneLabel;
    @FXML private Button inscrireButton;
    @FXML private Button annulerInscriptionButton;
    @FXML private Button ajouterAvisButton;
    @FXML private VBox participantsContainer;
    @FXML private VBox avisContainer;
    @FXML private TabPane tabPane;

    @FXML private Button attestationButton;
    @FXML private Button avisButton;

    private Evenement evenement;
    private EvenementService evenementService;
    private ParticipationService participationService;
    private EvenementListController evenementListController;
    private String userRole;
    private int userId;
    private boolean isUserInscrit = false;
    private CalendarExportService calendarService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        evenementService = new EvenementService();
        participationService = new ParticipationService();

        if (UserSession.getInstance() != null && UserSession.getInstance().getUser() != null) {
            userRole = UserSession.getInstance().getUser().getRole().name();
            userId = UserSession.getInstance().getUser().getId();

            // ✅ Utilise l'email de l'utilisateur connecté pour l'API Google Calendar
            String userEmail = UserSession.getInstance().getUser().getEmail();
            calendarService = new CalendarExportService(userEmail);
        } else {
            userRole = "CLIENT";
            userId = 0;
            // Fallback sans email → mode .ics uniquement
            calendarService = new CalendarExportService();
        }

        descriptionArea.setEditable(false);
        descriptionArea.setWrapText(true);

        if (attestationButton != null) {
            attestationButton.setVisible(false);
            attestationButton.setManaged(false);
        }
        if (avisButton != null) {
            avisButton.setVisible(false);
            avisButton.setManaged(false);
        }
    }

    public void setEvenement(Evenement evenement) {
        this.evenement = evenement;
        displayEventDetails();
        checkUserInscription();
        loadParticipants();
        loadAvis();

        if ("TERMINE".equals(evenement.getStatut()) && isUserInscrit) {
            if (attestationButton != null) {
                attestationButton.setVisible(true);
                attestationButton.setManaged(true);
            }
            if (avisButton != null) {
                avisButton.setVisible(true);
                avisButton.setManaged(true);
            }
        }
    }

    public void setEvenementListController(EvenementListController controller) {
        this.evenementListController = controller;
    }

    private void displayEventDetails() {
        titreLabel.setText(evenement.getTitre());
        typeLabel.setText(evenement.getType());
        typeLabel.getStyleClass().add("badge-" + evenement.getType().toLowerCase());

        statutLabel.setText(getStatutLabel(evenement.getStatut()));
        statutLabel.getStyleClass().add("badge-" + evenement.getStatut().toLowerCase().replace("_", ""));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String dateStr = evenement.getDateDebut().format(formatter);
        if (!evenement.getDateDebut().equals(evenement.getDateFin())) {
            dateStr += " - " + evenement.getDateFin().format(formatter);
        }
        dateLabel.setText(dateStr);

        lieuLabel.setText(evenement.getLieu());
        organisateurLabel.setText(evenement.getOrganisateur());
        descriptionArea.setText(evenement.getDescription());

        int places = evenement.getNombreParticipants();
        int placesMax = evenement.getNombrePlacesMax();
        placesLabel.setText(places + " / " + placesMax + " participants");

        double progress = placesMax > 0 ? (double) places / placesMax : 0;
        placesProgressBar.setProgress(progress);

        double noteMoyenne = participationService.getNoteMoyenneEvenement(evenement.getId());
        if (noteMoyenne > 0) {
            noteMoyenneLabel.setText(String.format("%.1f / 5 ⭐", noteMoyenne));
        } else {
            noteMoyenneLabel.setText("Pas encore d'avis");
        }
    }

    private void checkUserInscription() {
        isUserInscrit = participationService.isUserDejaInscrit(evenement.getId(), userId);

        if (isUserInscrit) {
            inscrireButton.setVisible(false);
            inscrireButton.setManaged(false);
            annulerInscriptionButton.setVisible(true);
            annulerInscriptionButton.setManaged(true);
        } else {
            inscrireButton.setVisible(true);
            inscrireButton.setManaged(true);
            annulerInscriptionButton.setVisible(false);
            annulerInscriptionButton.setManaged(false);
        }

        if (evenement.isComplet() || "TERMINE".equals(evenement.getStatut()) ||
                "ANNULE".equals(evenement.getStatut())) {
            inscrireButton.setDisable(true);
        }

        ajouterAvisButton.setVisible(isUserInscrit && "TERMINE".equals(evenement.getStatut()));
        ajouterAvisButton.setManaged(isUserInscrit && "TERMINE".equals(evenement.getStatut()));
    }

    @FXML
    private void handleInscrire() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Inscription");
        dialog.setHeaderText("S'inscrire à l'événement");
        dialog.setContentText("Commentaire (optionnel):");

        Optional<String> result = dialog.showAndWait();
        String commentaire = result.orElse("");

        Participation participation = new Participation(evenement.getId(), userId, commentaire);

        if (participationService.inscrireParticipant(participation)) {
            showSuccess("Inscription réussie !");
            ouvrirQRCodeViewer(participation);
            refreshEventDetails();
        } else {
            showError("Erreur lors de l'inscription. Vérifiez que l'événement n'est pas complet.");
        }
    }

    private void ouvrirQRCodeViewer(Participation participation) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/Evenement/QRCodeViewer.fxml"));
            Parent root = loader.load();

            QRCodeViewerController controller = loader.getController();
            controller.setData(participation, evenement);

            Stage stage = new Stage();
            stage.setTitle("Votre QR Code de Check-in");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAnnulerInscription() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Annuler l'inscription");
        alert.setContentText("Êtes-vous sûr de vouloir annuler votre inscription ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (participationService.annulerParticipation(evenement.getId(), userId)) {
                showSuccess("Inscription annulée");
                refreshEventDetails();
            } else {
                showError("Erreur lors de l'annulation");
            }
        }
    }

    @FXML
    private void handleAjouterAvis() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un avis");
        dialog.setHeaderText("Évaluez cet événement");

        ButtonType submitButtonType = new ButtonType("Soumettre", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Label noteLabel = new Label("Note (1-5):");
        Spinner<Integer> noteSpinner = new Spinner<>(1, 5, 3);
        noteSpinner.setEditable(true);

        Label avisLabel = new Label("Votre avis:");
        TextArea avisArea = new TextArea();
        avisArea.setPromptText("Partagez votre expérience...");
        avisArea.setPrefRowCount(5);
        avisArea.setWrapText(true);

        content.getChildren().addAll(noteLabel, noteSpinner, avisLabel, avisArea);
        dialog.getDialogPane().setContent(content);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == submitButtonType) {
            int note = noteSpinner.getValue();
            String avis = avisArea.getText();

            List<Participation> userParticipations = participationService.getParticipationsByUtilisateur(userId);
            for (Participation p : userParticipations) {
                if (p.getIdEvenement() == evenement.getId()) {
                    if (participationService.ajouterAvis(p.getId(), note, avis)) {
                        showSuccess("Avis ajouté avec succès !");
                        loadAvis();
                        displayEventDetails();
                    } else {
                        showError("Erreur lors de l'ajout de l'avis");
                    }
                    break;
                }
            }
        }
    }

    private void loadParticipants() {
        participantsContainer.getChildren().clear();

        List<Participation> participants = participationService.getParticipationsByEvenement(evenement.getId());

        if (participants.isEmpty()) {
            Label noParticipantsLabel = new Label("Aucun participant pour le moment");
            noParticipantsLabel.setStyle("-fx-text-fill: #999;");
            participantsContainer.getChildren().add(noParticipantsLabel);
            return;
        }

        for (Participation p : participants) {
            if (!"CONFIRME".equals(p.getStatut())) continue;

            HBox participantBox = new HBox(15);
            participantBox.setAlignment(Pos.CENTER_LEFT);
            participantBox.setPadding(new Insets(10));
            participantBox.setStyle("-fx-background-color: white; -fx-background-radius: 5; " +
                    "-fx-border-color: #e0e0d0; -fx-border-radius: 5;");

            Label iconLabel = new Label("👤");
            iconLabel.setStyle("-fx-font-size: 24px;");

            VBox infoBox = new VBox(5);
            Label nameLabel = new Label(p.getNomComplet());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            Label emailLabel = new Label(p.getEmailUtilisateur());
            emailLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

            infoBox.getChildren().addAll(nameLabel, emailLabel);

            if (p.getCommentaire() != null && !p.getCommentaire().isEmpty()) {
                Label commentLabel = new Label("💬 " + p.getCommentaire());
                commentLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px; -fx-font-style: italic;");
                commentLabel.setWrapText(true);
                infoBox.getChildren().add(commentLabel);
            }

            participantBox.getChildren().addAll(iconLabel, infoBox);

            if ("ADMIN".equals(userRole) || evenement.getIdCreateur() == userId) {
                Button removeButton = new Button("❌");
                removeButton.getStyleClass().addAll("button-small", "button-danger");
                removeButton.setOnAction(e -> removeParticipant(p));
                participantBox.getChildren().add(removeButton);
            }

            participantsContainer.getChildren().add(participantBox);
        }
    }

    private void loadAvis() {
        avisContainer.getChildren().clear();

        List<Participation> avis = participationService.getAvisByEvenement(evenement.getId());

        if (avis.isEmpty()) {
            Label noAvisLabel = new Label("Aucun avis pour le moment");
            noAvisLabel.setStyle("-fx-text-fill: #999;");
            avisContainer.getChildren().add(noAvisLabel);
            return;
        }

        for (Participation p : avis) {
            VBox avisBox = new VBox(10);
            avisBox.setPadding(new Insets(15));
            avisBox.setStyle("-fx-background-color: white; -fx-background-radius: 5; " +
                    "-fx-border-color: #e0e0d0; -fx-border-radius: 5;");

            HBox headerBox = new HBox(10);
            headerBox.setAlignment(Pos.CENTER_LEFT);

            Label nameLabel = new Label(p.getNomComplet());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            HBox starsBox = new HBox(2);
            for (int i = 1; i <= 5; i++) {
                Label star = new Label(i <= p.getNote() ? "⭐" : "☆");
                star.setStyle("-fx-font-size: 14px;");
                starsBox.getChildren().add(star);
            }

            headerBox.getChildren().addAll(nameLabel, starsBox);

            Label avisLabel = new Label(p.getAvis());
            avisLabel.setWrapText(true);
            avisLabel.setStyle("-fx-text-fill: #555;");

            avisBox.getChildren().addAll(headerBox, avisLabel);
            avisContainer.getChildren().add(avisBox);
        }
    }

    private void removeParticipant(Participation p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Retirer le participant");
        alert.setContentText("Êtes-vous sûr de vouloir retirer " + p.getNomComplet() + " ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (participationService.supprimerParticipation(p.getId())) {
                showSuccess("Participant retiré");
                refreshEventDetails();
            } else {
                showError("Erreur lors du retrait du participant");
            }
        }
    }

    private void refreshEventDetails() {
        Evenement updated = evenementService.getEvenementById(evenement.getId());
        if (updated != null) {
            this.evenement = updated;
            displayEventDetails();
            checkUserInscription();
            loadParticipants();
        }
        if (evenementListController != null) {
            evenementListController.refreshList();
        }
    }

    @FXML
    private void handleAjouterCalendrier() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Ajouter au calendrier");
        alert.setHeaderText("📅 " + evenement.getTitre());
        alert.setContentText("Comment souhaitez-vous ajouter cet événement ?");

        ButtonType btnGoogle = new ButtonType("🌐 Google Calendar");
        ButtonType btnICS = new ButtonType("📥 Fichier .ics");
        ButtonType btnCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnGoogle, btnICS, btnCancel);

        alert.showAndWait().ifPresent(response -> {
            if (response == btnGoogle) {
                if (calendarService.isConnected()) {
                    if (calendarService.ajouterAuCalendrier(evenement)) {
                        showSuccess("✅ Événement ajouté directement dans votre Google Calendar !");
                    } else {
                        showError("Erreur lors de l'ajout au calendrier.");
                    }
                } else {
                    showError("Google Calendar non disponible.\nVérifiez votre connexion ou utilisez le fichier .ics.");
                }
            } else if (response == btnICS) {
                String icsPath = calendarService.genererFichierICS(evenement);
                if (icsPath != null && calendarService.ouvrirDansCalendrier(icsPath)) {
                    showSuccess("Fichier .ics généré !\nVotre application calendrier devrait s'ouvrir.");
                } else {
                    showError("Impossible de générer le fichier .ics.");
                }
            }
        });
    }

    private String getStatutLabel(String statut) {
        return switch (statut) {
            case "A_VENIR" -> "À venir";
            case "EN_COURS" -> "En cours";
            case "TERMINE" -> "Terminé";
            case "ANNULE" -> "Annulé";
            default -> statut;
        };
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleVoirLocalisation() {
        TextInputDialog input = new TextInputDialog("Tunis");
        input.setTitle("Votre position");
        input.setHeaderText("D'où partez-vous ?");
        input.setContentText("Ville ou adresse :");

        Optional<String> result = input.showAndWait();
        result.ifPresent(location -> {
            LocationDialog dialog = new LocationDialog(evenement);
            dialog.setUserLocation(location);
            dialog.show();
        });
    }
}