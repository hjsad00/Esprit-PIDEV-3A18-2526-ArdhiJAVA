package tn.neuron.ardhi.controllers.marketplace;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import tn.neuron.ardhi.models.marketplace.Avis;
import tn.neuron.ardhi.models.marketplace.Produit;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.services.marketplace.AvisService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.interfaces.marketplace.IAvisService;
import javafx.stage.Stage;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;

public class AvisProduitController implements Initializable {

    @FXML
    private Label lblTitreProduit;
    /** Big numeric score e.g. "4.2" */
    @FXML
    private Label lblScoreNum;
    /** Row of 5 coloured stars representing average */
    @FXML
    private Label lblEtoilesMoyenne;
    /** "(12 avis)" */
    @FXML
    private Label lblNombreAvis;
    /** "3 / 5" next to the interactive star picker */
    @FXML
    private Label lblNoteSelectionnee;

    @FXML
    private VBox vboxAvisList;
    @FXML
    private HBox hboxEtoiles;
    @FXML
    private TextArea taCommentaire;
    @FXML
    private Button btnPublier;

    private Produit produit;
    private IAvisService avisService;
    private int noteSelectionnee = 0;
    private Button[] etoilesButtons;
    private CatalogueProduitController marketplaceController;

    // ── Star styles ───────────────────────────────────────────────────────────
    private static final String STAR_FILLED_STYLE = "-fx-background-color: transparent; -fx-font-size: 26px; " +
            "-fx-text-fill: #f1c40f; -fx-cursor: hand; -fx-padding: 0;" +
            "-fx-effect: dropshadow(gaussian, rgba(241,196,15,0.5), 6, 0, 0, 0);";

    private static final String STAR_EMPTY_STYLE = "-fx-background-color: transparent; -fx-font-size: 26px; " +
            "-fx-text-fill: rgba(255,255,255,0.25); -fx-cursor: hand; -fx-padding: 0;";

    private static final String STAR_HOVER_STYLE = "-fx-background-color: transparent; -fx-font-size: 26px; " +
            "-fx-text-fill: #f39c12; -fx-cursor: hand; -fx-padding: 0;";

    // ─────────────────────────────────────────────────────────────────────────

    public void setMarketplaceController(CatalogueProduitController marketplaceController) {
        this.marketplaceController = marketplaceController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        avisService = new AvisService();
        setupEtoilesSelection();
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
        lblTitreProduit.setText(produit.getNom());
        chargerAvis();
    }

    // ── Load reviews ──────────────────────────────────────────────────────────
    private void chargerAvis() {
        vboxAvisList.getChildren().clear();
        List<Avis> avisList = avisService.getAvisByProduit(produit.getIdProduit());

        double moyenne = avisService.getNoteMoyenne(produit.getIdProduit());
        int nombre = avisService.getNombreAvis(produit.getIdProduit());

        // Numeric badge "4.2"
        lblScoreNum.setText(String.format("%.1f", moyenne));

        // Star row (mixed half-stars via threshold per star)
        lblEtoilesMoyenne.setText(buildStarRow(moyenne));

        // Count
        lblNombreAvis.setText(nombre + (nombre <= 1 ? " avis" : " avis"));

        if (avisList.isEmpty()) {
            Label placeholder = new Label("Aucun avis pour le moment. Soyez le premier ! 🌱");
            placeholder.setStyle("-fx-text-fill: rgba(255,255,255,0.4); -fx-font-style: italic; -fx-font-size: 13px;");
            vboxAvisList.getChildren().add(placeholder);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy · HH:mm");
            for (Avis avis : avisList) {
                vboxAvisList.getChildren().add(creerAvisCard(avis, sdf));
            }
        }
    }

    // ── Build star row string from a double average ───────────────────────────
    /**
     * Returns a string of 5 characters: ★ (full), ⯨ (half), ☆ (empty).
     * JavaFX Label renders Unicode correctly.
     */
    private String buildStarRow(double moyenne) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            if (moyenne >= i) {
                sb.append("★");
            } else if (moyenne >= i - 0.5) {
                sb.append("⭐"); // half-star visual fallback
            } else {
                sb.append("☆");
            }
        }
        return sb.toString();
    }

    /** Simple integer star string used inside cards. */
    private String getEtoilesString(int note) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(i < note ? "★" : "☆");
        }
        return sb.toString();
    }

    // ── Review card ──────────────────────────────────────────────────────────
    private VBox creerAvisCard(Avis avis, SimpleDateFormat sdf) {
        VBox card = new VBox(6);
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(107,127,63,0.25);" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 14;");

        // ── Header row ──
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        // Avatar circle (first letter)
        String nom = avis.getNomUser() != null ? avis.getNomUser() : "Anonyme";
        String letter = nom.substring(0, 1).toUpperCase();
        Label avatar = new Label(letter);
        avatar.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #6B7F3F, #4A5A2B);" +
                        "-fx-background-radius: 20;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13px;" +
                        "-fx-min-width: 32; -fx-min-height: 32;" +
                        "-fx-max-width: 32; -fx-max-height: 32;" +
                        "-fx-alignment: center;");

        VBox info = new VBox(2);

        Label nomUser = new Label(nom);
        nomUser.setFont(Font.font("System", FontWeight.BOLD, 12));
        nomUser.setStyle("-fx-text-fill: white;");

        Label dateLabel = new Label(sdf.format(avis.getDateAvis()));
        dateLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.4); -fx-font-size: 10px;");

        info.getChildren().addAll(nomUser, dateLabel);

        // Rating badge on right
        HBox spacer = new HBox();
        spacer.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Stars + score
        VBox rateBadge = new VBox(2);
        rateBadge.setAlignment(Pos.CENTER_RIGHT);

        Label stars = new Label(getEtoilesString(avis.getNote()));
        stars.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 14px;");

        Label scoreLabel = new Label(avis.getNote() + " / 5");
        scoreLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.55); -fx-font-size: 10px; -fx-font-weight: bold;");

        rateBadge.getChildren().addAll(stars, scoreLabel);

        header.getChildren().addAll(avatar, info, spacer, rateBadge);

        // ── Comment ──
        if (avis.getCommentaire() != null && !avis.getCommentaire().isEmpty()) {
            Label commentaire = new Label(avis.getCommentaire());
            commentaire.setWrapText(true);
            commentaire.setStyle("-fx-text-fill: rgba(255,255,255,0.75); -fx-font-size: 12px; -fx-line-spacing: 2;");
            card.getChildren().addAll(header, commentaire);
        } else {
            card.getChildren().add(header);
        }

        return card;
    }

    // ── Interactive star picker ───────────────────────────────────────────────
    private void setupEtoilesSelection() {
        etoilesButtons = new Button[5];
        for (int i = 0; i < 5; i++) {
            final int note = i + 1;
            Button btn = new Button("☆");
            btn.setStyle(STAR_EMPTY_STYLE);

            // Hover enter: light up up to this star
            btn.setOnMouseEntered(e -> highlightUpTo(note));
            // Hover exit: restore selection
            btn.setOnMouseExited(e -> renderStars(noteSelectionnee));
            // Click: lock selection
            btn.setOnAction(e -> setNote(note));

            etoilesButtons[i] = btn;
            hboxEtoiles.getChildren().add(btn);
        }
        refreshNoteLabel();
    }

    /** Preview up to `hoverNote` stars in hover colour. */
    private void highlightUpTo(int hoverNote) {
        for (int i = 0; i < 5; i++) {
            if (i < hoverNote) {
                etoilesButtons[i].setText("★");
                etoilesButtons[i].setStyle(STAR_HOVER_STYLE);
            } else {
                etoilesButtons[i].setText("☆");
                etoilesButtons[i].setStyle(STAR_EMPTY_STYLE);
            }
        }
    }

    /** Render the locked selection. */
    private void renderStars(int selected) {
        for (int i = 0; i < 5; i++) {
            if (i < selected) {
                etoilesButtons[i].setText("★");
                etoilesButtons[i].setStyle(STAR_FILLED_STYLE);
            } else {
                etoilesButtons[i].setText("☆");
                etoilesButtons[i].setStyle(STAR_EMPTY_STYLE);
            }
        }
    }

    private void setNote(int note) {
        this.noteSelectionnee = note;
        renderStars(note);
        refreshNoteLabel();
    }

    private void refreshNoteLabel() {
        if (lblNoteSelectionnee == null)
            return;
        if (noteSelectionnee == 0) {
            lblNoteSelectionnee.setText("— / 5");
            lblNoteSelectionnee
                    .setStyle("-fx-text-fill: rgba(255,255,255,0.35); -fx-font-size: 13px; -fx-font-weight: bold;");
        } else {
            lblNoteSelectionnee.setText(noteSelectionnee + " / 5");
            lblNoteSelectionnee.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 13px; -fx-font-weight: bold;");
        }
    }

    // ── Publish ───────────────────────────────────────────────────────────────
    @FXML
    void publierAvis(ActionEvent event) {
        User user = UserSession.getInstance().getUser();
        if (user == null) {
            showAlert("Erreur", "Vous devez être connecté pour publier un avis.");
            return;
        }
        if (noteSelectionnee == 0) {
            showAlert("Attention", "Veuillez sélectionner une note (1 à 5 étoiles).");
            return;
        }

        String commentaire = taCommentaire.getText().trim();
        Avis nouvelAvis = new Avis(user.getId(), produit.getIdProduit(), noteSelectionnee, commentaire);

        if (avisService.ajouterAvis(nouvelAvis)) {
            taCommentaire.clear();
            setNote(0);
            chargerAvis();
            showAlert("Succès", "Votre avis a été publié ! Merci 🌱");

            Stage stage = (Stage) btnPublier.getScene().getWindow();
            if (stage != null)
                stage.close();

            if (marketplaceController != null) {
                marketplaceController.rafraichirProduits();
            }
        } else {
            showAlert("Erreur", "Impossible de publier votre avis. Réessayez.");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}
