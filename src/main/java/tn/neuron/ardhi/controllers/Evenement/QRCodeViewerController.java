package tn.neuron.ardhi.controllers.Evenement;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.neuron.ardhi.models.Evenement.Evenement;
import tn.neuron.ardhi.models.Evenement.Participation;
import tn.neuron.ardhi.services.Evenement.QRCodeService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import java.util.ResourceBundle;

/**
 * Affiche le QR code d'un participant après son inscription.
 */
public class QRCodeViewerController implements Initializable {

    @FXML private Label eventTitleLabel;
    @FXML private ImageView qrImageView;
    @FXML private Label participantNameLabel;
    @FXML private Label tokenLabel;

    private Participation participation;
    private Evenement evenement;
    private String base64QR;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialisation vide, données chargées via setData()
    }

    /**
     * Charge les données et génère le QR code.
     */
    public void setData(Participation participation, Evenement evenement) {
        this.participation = participation;
        this.evenement = evenement;

        eventTitleLabel.setText("📍 " + evenement.getTitre());
        participantNameLabel.setText(participation.getNomComplet());

        // Générer et afficher le QR code
        QRCodeService qrService = new QRCodeService();
        base64QR = qrService.genererQRCodeBase64(participation.getId(), evenement.getId());

        if (base64QR != null) {
            displayQRCode(base64QR);

            // Afficher le token (tronqué pour la lisibilité)
            String token = qrService.getTokenForParticipation(participation.getId());
            if (token != null) {
                tokenLabel.setText("Token: " + token.substring(0, 8) + "••••••••" + token.substring(token.length() - 4));
            }
        } else {
            showErrorPlaceholder();
        }
    }

    private void displayQRCode(String base64) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64);
            ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
            Image fxImage = new Image(bais);
            qrImageView.setImage(fxImage);
        } catch (Exception e) {
            System.err.println("Erreur affichage QR: " + e.getMessage());
            showErrorPlaceholder();
        }
    }

    private void showErrorPlaceholder() {
        eventTitleLabel.setText("⚠️ Erreur de génération du QR code");
    }

    @FXML
    private void handleSauvegarder() {
        if (base64QR == null) {
            showAlert("Aucun QR code à sauvegarder");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sauvegarder le QR Code");
        fileChooser.setInitialFileName("QRCode_" + participation.getNomComplet().replace(" ", "_") + ".png");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image PNG", "*.png"));

        File file = fileChooser.showSaveDialog(qrImageView.getScene().getWindow());
        if (file != null) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(base64QR);
                ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
                BufferedImage bufferedImage = ImageIO.read(bais);
                ImageIO.write(bufferedImage, "PNG", file);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Succès");
                alert.setHeaderText(null);
                alert.setContentText("QR Code sauvegardé dans:\n" + file.getAbsolutePath());
                alert.showAndWait();
            } catch (IOException e) {
                showAlert("Erreur lors de la sauvegarde: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleFermer() {
        Stage stage = (Stage) qrImageView.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Attention");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}