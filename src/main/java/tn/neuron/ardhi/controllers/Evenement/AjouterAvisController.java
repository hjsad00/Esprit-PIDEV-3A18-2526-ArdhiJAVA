package tn.neuron.ardhi.controllers.Evenement;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import tn.neuron.ardhi.models.Evenement.Participation;
import tn.neuron.ardhi.services.Evenement.ParticipationService;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour le formulaire d'ajout d'avis sur un événement
 */
public class AjouterAvisController implements Initializable {

    @FXML
    private Label eventTitleLabel;

    @FXML
    private HBox starsBox;

    @FXML
    private Label selectedNoteLabel;

    @FXML
    private TextArea avisTextArea;

    @FXML
    private Button submitButton;

    @FXML
    private Button cancelButton;

    private Participation participation;
    private ParticipationService participationService;
    private int selectedNote = 0;
    private Label[] starLabels;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        participationService = new ParticipationService();
        setupStars();
    }

    /**
     * Configure le système d'étoiles interactif
     */
    private void setupStars() {
        starLabels = new Label[5];
        starsBox.getChildren().clear();

        for (int i = 0; i < 5; i++) {
            final int note = i + 1;
            Label star = new Label("☆");
            star.setStyle("-fx-font-size: 48px; -fx-cursor: hand; -fx-text-fill: #d4d4d4;");

            // Au survol
            star.setOnMouseEntered(e -> highlightStars(note));

            // Au clic
            star.setOnMouseClicked(e -> selectNote(note));

            starLabels[i] = star;
            starsBox.getChildren().add(star);
        }

        // Quand la souris quitte la zone des étoiles
        starsBox.setOnMouseExited(e -> {
            if (selectedNote > 0) {
                highlightStars(selectedNote);
            } else {
                resetStars();
            }
        });
    }

    /**
     * Surligne les étoiles jusqu'à la note spécifiée
     */
    private void highlightStars(int upToNote) {
        for (int i = 0; i < 5; i++) {
            if (i < upToNote) {
                starLabels[i].setText("⭐");
                starLabels[i].setStyle("-fx-font-size: 48px; -fx-cursor: hand; -fx-text-fill: #f5a623;");
            } else {
                starLabels[i].setText("☆");
                starLabels[i].setStyle("-fx-font-size: 48px; -fx-cursor: hand; -fx-text-fill: #d4d4d4;");
            }
        }
    }

    /**
     * Sélectionne une note
     */
    private void selectNote(int note) {
        selectedNote = note;
        highlightStars(note);
        selectedNoteLabel.setText(note + " / 5");
        submitButton.setDisable(false);
    }

    /**
     * Réinitialise toutes les étoiles
     */
    private void resetStars() {
        for (Label star : starLabels) {
            star.setText("☆");
            star.setStyle("-fx-font-size: 48px; -fx-cursor: hand; -fx-text-fill: #d4d4d4;");
        }
    }

    /**
     * Définit la participation à évaluer
     */
    public void setParticipation(Participation participation) {
        this.participation = participation;
        // eventTitleLabel sera mis à jour depuis le contrôleur parent
    }

    /**
     * Définit le titre de l'événement à afficher
     */
    public void setEventTitle(String title) {
        eventTitleLabel.setText("Évaluer: " + title);
    }

    /**
     * Soumet l'avis
     */
    @FXML
    private void handleSubmit() {
        if (selectedNote == 0) {
            showError("Veuillez sélectionner une note (1 à 5 étoiles)");
            return;
        }

        String avisTexte = avisTextArea.getText().trim();

        // Mise à jour de la participation avec la note et l'avis
        if (participationService.ajouterAvis(participation.getId(), selectedNote, avisTexte)) {
            showSuccess("Merci pour votre avis! 🌾");
            closeWindow();
        } else {
            showError("Erreur lors de l'enregistrement de votre avis");
        }
    }

    /**
     * Annule et ferme la fenêtre
     */
    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
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
}