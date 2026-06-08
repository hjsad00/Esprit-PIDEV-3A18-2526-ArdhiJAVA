package tn.neuron.ardhi.utils.gestionemployeutils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Utilitaire de generation de QR Codes pour les employes
 *
 * Dependances Maven :
 *   com.google.zxing:core:3.5.2
 *   com.google.zxing:javase:3.5.2
 */
public class QRCodeGenerator {

    private static final int TAILLE_PAR_DEFAUT = 200;
    private static final int QR_NOIR  = 0xFF000000;
    private static final int QR_BLANC = 0xFFFFFFFF;

    /**
     * Genere un QR Code JavaFX Image a partir d'un contenu texte
     *
     * @param contenu    Texte a encoder (ex: "EMP_1024_ABCX98")
     * @param taille     Taille en pixels (carre)
     * @return Image JavaFX ou null si erreur
     */
    public static Image genererImageQR(String contenu, int taille) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 2);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(contenu, BarcodeFormat.QR_CODE, taille, taille, hints);

            // Convertir BitMatrix → BufferedImage
            BufferedImage buffered = new BufferedImage(taille, taille, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < taille; y++) {
                for (int x = 0; x < taille; x++) {
                    buffered.setRGB(x, y, bitMatrix.get(x, y) ? QR_NOIR : QR_BLANC);
                }
            }

            // Convertir BufferedImage → JavaFX Image
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(buffered, "png", out);
            return new Image(new ByteArrayInputStream(out.toByteArray()));

        } catch (WriterException | java.io.IOException e) {
            System.err.println("Erreur generation QR Code: " + e.getMessage());
            return null;
        }
    }

    /**
     * Genere avec taille par defaut (200px)
     */
    public static Image genererImageQR(String contenu) {
        return genererImageQR(contenu, TAILLE_PAR_DEFAUT);
    }

    /**
     * Affiche un QR Code dans une fenetre JavaFX Stage modale
     * (pour imprimer ou afficher le badge d'un employe)
     *
     * @param qrCode    Code QR unique de l'employe
     * @param nomEmploye Nom complet pour l'affichage
     */
    public static void afficherFenetreQR(String qrCode, String nomEmploye) {
        Image image = genererImageQR(qrCode, 280);
        if (image == null) return;

        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.setTitle("Badge QR - " + nomEmploye);
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(16);
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setStyle("-fx-background-color: white; -fx-padding: 28;");

        // Titre badge
        javafx.scene.control.Label lblTitre = new javafx.scene.control.Label("Badge de Pointage");
        lblTitre.setStyle("-fx-font-size: 11px; -fx-text-fill: #888; -fx-font-family: 'Courier New';" +
                "letter-spacing: 1px;");

        javafx.scene.control.Label lblNom = new javafx.scene.control.Label(nomEmploye);
        lblNom.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1a5276;");

        // QR Code
        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(image);
        iv.setFitWidth(280);
        iv.setFitHeight(280);
        iv.setStyle("-fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 8;");

        // Code texte sous le QR
        javafx.scene.control.Label lblCode = new javafx.scene.control.Label(qrCode);
        lblCode.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 13px; -fx-text-fill: #555;" +
                "-fx-background-color: #f8f9fa; -fx-padding: 6 12; -fx-background-radius: 4;");

        javafx.scene.control.Label lblInstr = new javafx.scene.control.Label(
                "Presentez ce badge devant la camera pour pointer");
        lblInstr.setStyle("-fx-font-size: 11px; -fx-text-fill: #aaa; -fx-font-style: italic;");
        lblInstr.setWrapText(true);
        lblInstr.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        lblInstr.setMaxWidth(280);

        javafx.scene.control.Button btnFermer = new javafx.scene.control.Button("Fermer");
        btnFermer.setStyle("-fx-background-color: #1a5276; -fx-text-fill: white; -fx-font-weight: bold;" +
                "-fx-padding: 9 30; -fx-background-radius: 7; -fx-cursor: hand;");
        btnFermer.setOnAction(e -> stage.close());

        root.getChildren().addAll(lblTitre, lblNom, iv, lblCode, lblInstr, btnFermer);

        stage.setScene(new javafx.scene.Scene(root, 360, 520));
        stage.setResizable(false);
        stage.show();
    }
}