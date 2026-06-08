package tn.neuron.ardhi.controllers.marketplace;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.models.marketplace.Avis;
import tn.neuron.ardhi.models.marketplace.Panier;
import tn.neuron.ardhi.models.marketplace.Produit;
import tn.neuron.ardhi.models.marketplace.TypeReduction;
import tn.neuron.ardhi.services.marketplace.AvisService;
import tn.neuron.ardhi.services.marketplace.PanierService;
import tn.neuron.ardhi.services.marketplace.WishlistService;
import tn.neuron.ardhi.services.UserAndDiag.UserService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la page de détail d'un produit.
 * Affiche toutes les infos du produit + section avis avec formulaire.
 */
public class DetailProduitController implements Initializable {

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML
    private StackPane imgZone;
    @FXML
    private ImageView imgProduit;
    @FXML
    private Label soldeBadge;
    @FXML
    private Label lblBreadcrumb;
    @FXML
    private Label lblCategorieBadge;
    @FXML
    private Label lblStockBadge;
    @FXML
    private Label lblNomProduit;
    @FXML
    private Label lblVendeur;
    @FXML
    private Label lblEtoiles;
    @FXML
    private Label lblScoreRating;
    @FXML
    private Label lblNombreAvis;
    @FXML
    private Label lblPrixBarre;
    @FXML
    private Label lblPrixFinal;
    @FXML
    private Label lblDevise;
    @FXML
    private Label lblEconomie;
    @FXML
    private Label lblQuantiteChip;
    @FXML
    private Label lblUniteChip;
    @FXML
    private Label lblDescription;
    @FXML
    private Spinner<Integer> quantiteSpinner;
    @FXML
    private Button btnAjouterPanier;
    @FXML
    private Button btnFavori;

    // Avis section
    @FXML
    private Label lblScoreGros;
    @FXML
    private Label lblEtoilesMoy;
    @FXML
    private Label lblTotalAvis;
    @FXML
    private VBox vboxAvisList;
    @FXML
    private HBox hboxEtoiles;
    @FXML
    private TextArea taCommentaire;
    @FXML
    private Button btnPublierAvis;

    // ── Services ─────────────────────────────────────────────────────────────
    private AvisService avisService;
    private WishlistService wishlistService;
    private PanierService panierService;
    private UserService userService;

    // ── State ─────────────────────────────────────────────────────────────────
    private Produit produit;
    private User currentUser;
    private int noteSelectionnee = 0;
    private Button[] etoilesButtons;

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final String GREEN_MAIN = "#4a7c47";
    private static final String RED_FAV = "#e74c3c";
    private static final String STAR_GOLD = "#f59e0b";

    // ── Star styles ───────────────────────────────────────────────────────────
    private static final String STAR_FILLED = "-fx-background-color: transparent; -fx-font-size: 28px; " +
            "-fx-text-fill: #f1c40f; -fx-cursor: hand; -fx-padding: 0; " +
            "-fx-effect: dropshadow(gaussian, rgba(241,196,15,0.5), 6, 0, 0, 0);";
    private static final String STAR_EMPTY = "-fx-background-color: transparent; -fx-font-size: 28px; " +
            "-fx-text-fill: rgba(255,255,255,0.25); -fx-cursor: hand; -fx-padding: 0;";
    private static final String STAR_HOVER = "-fx-background-color: transparent; -fx-font-size: 28px; " +
            "-fx-text-fill: #f39c12; -fx-cursor: hand; -fx-padding: 0;";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        avisService = new AvisService();
        wishlistService = new WishlistService();
        panierService = new PanierService();
        userService = new UserService();
        currentUser = UserSession.getInstance() != null ? UserSession.getInstance().getUser() : null;
        setupEtoilesSelection();
    }

    // ── Setter appelé par CatalogueProduitController ──────────────────────────
    public void setProduit(Produit p) {
        this.produit = p;
        remplirUI();
        chargerAvis();
    }

    // ── Remplissage de l'UI ───────────────────────────────────────────────────
    private void remplirUI() {
        // Breadcrumb
        if (lblBreadcrumb != null)
            lblBreadcrumb.setText(produit.getNom());

        // Image
        if (produit.getImage() != null && !produit.getImage().isEmpty()) {
            File f = new File(produit.getImage());
            if (f.exists())
                imgProduit.setImage(new Image(f.toURI().toString()));
        }

        // Badge SOLDE
        if (produit.aUneRemise()) {
            String txt = produit.getTypeRemise() == TypeReduction.POURCENTAGE
                    ? "SOLDE  -" + (int) produit.getRemise() + "%"
                    : "SOLDE  -" + String.format("%.2f", produit.getRemise()) + " DT";
            soldeBadge.setText(txt);
            soldeBadge.setVisible(true);
            soldeBadge.setManaged(true);
        }

        // Catégorie + stock
        lblCategorieBadge.setText("🌿  " + (produit.getCategorie() != null ? produit.getCategorie() : "—"));
        boolean enStock = produit.getQuantiteStock() > 0;
        lblStockBadge.setText(enStock ? "✓ En stock" : "✗ Rupture");
        lblStockBadge.setStyle(lblStockBadge.getStyle().replace(
                enStock ? "#e74c3c" : "#046436",
                enStock ? "#046436" : "#e74c3c"));

        // Nom
        lblNomProduit.setText(produit.getNom());

        // Vendeur
        String vendeur = getNomVendeur();
        lblVendeur.setText("Vendu par  " + vendeur);

        // Prix
        float prixFinal = produit.getPrixApresRemise();
        lblPrixFinal.setText(String.format("%.2f", prixFinal));
        String unite = produit.getUniteMesure() != null ? " DT/" + produit.getUniteMesure() : " DT";
        lblDevise.setText(unite);

        if (produit.aUneRemise()) {
            lblPrixBarre.setText(String.format("%.2f", produit.getPrix()) + unite);
            lblPrixBarre.setVisible(true);
            lblPrixBarre.setManaged(true);
            float economie = produit.getPrix() - prixFinal;
            lblEconomie.setText(String.format("Vous économisez : %.2f DT !", economie));
            lblEconomie.setVisible(true);
            lblEconomie.setManaged(true);
            lblPrixFinal.setStyle("-fx-font-size: 42px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        }

        // Stock + Unité
        lblQuantiteChip.setText("📦  Stock : " + produit.getQuantiteStock()
                + (produit.getUniteMesure() != null ? " " + produit.getUniteMesure() : " unités"));
        lblUniteChip.setText(
                "Unité : " + (produit.getUniteMesure() != null ? produit.getUniteMesure().toString() : "—"));

        // Description
        lblDescription.setText(produit.getDescription() != null && !produit.getDescription().isEmpty()
                ? produit.getDescription()
                : "Aucune description disponible.");

        // Spinner quantité
        int max = Math.max(1, produit.getQuantiteStock());
        SpinnerValueFactory.IntegerSpinnerValueFactory vf = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, max,
                1);
        quantiteSpinner.setValueFactory(vf);
        quantiteSpinner.setEditable(true);
        if (!enStock) {
            btnAjouterPanier.setDisable(true);
            btnAjouterPanier.setText("Indisponible");
            btnAjouterPanier.setStyle(
                    "-fx-background-color: #d8e0d9; -fx-text-fill: #aaa; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 12;");
        }

        // Favori
        if (currentUser != null) {
            boolean isFav = wishlistService.estFavori(currentUser.getId(), produit.getIdProduit());
            btnFavori.setText(isFav ? "\u2665" : "\u2661");
            btnFavori.setStyle(btnFavori.getStyle() + (isFav ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #aaa;"));
        }

        // Rating résumé (ligne)
        double moy = avisService.getNoteMoyenne(produit.getIdProduit());
        int nb = avisService.getNombreAvis(produit.getIdProduit());
        lblEtoiles.setText(buildStarRow(moy));
        lblScoreRating.setText(String.format("%.1f", moy));
        lblNombreAvis.setText("(" + nb + " avis)");
    }

    // ── Chargement des avis ───────────────────────────────────────────────────
    private void chargerAvis() {
        vboxAvisList.getChildren().clear();
        List<Avis> avisList = avisService.getAvisByProduit(produit.getIdProduit());
        double moy = avisService.getNoteMoyenne(produit.getIdProduit());
        int nb = avisService.getNombreAvis(produit.getIdProduit());

        lblScoreGros.setText(nb > 0 ? String.format("%.1f", moy) : "—");
        lblEtoilesMoy.setText(buildStarRow(moy));
        lblTotalAvis.setText(nb + " avis");

        if (avisList.isEmpty()) {
            Label placeholder = new Label("🌱  Aucun avis pour le moment. Soyez le premier !");
            placeholder.setStyle("-fx-text-fill: rgba(255,255,255,0.40); -fx-font-style: italic; -fx-font-size: 13px;");
            vboxAvisList.getChildren().add(placeholder);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy · HH:mm");
            for (Avis avis : avisList) {
                vboxAvisList.getChildren().add(creerAvisCard(avis, sdf));
            }
        }
    }

    // ── Carte avis ────────────────────────────────────────────────────────────
    private VBox creerAvisCard(Avis avis, SimpleDateFormat sdf) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-background-radius: 14;" +
                "-fx-border-color: rgba(139,195,74,0.20); -fx-border-radius: 14; -fx-border-width: 1; -fx-padding: 16;");

        // Header : avatar + nom + date + stars
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        String nom = avis.getNomUser() != null ? avis.getNomUser() : "Anonyme";
        String letter = nom.substring(0, 1).toUpperCase();
        Label avatar = new Label(letter);
        avatar.setStyle("-fx-background-color: linear-gradient(to bottom right, #6B7F3F, #4A5A2B);" +
                "-fx-background-radius: 20; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;" +
                "-fx-min-width: 36; -fx-min-height: 36; -fx-max-width: 36; -fx-max-height: 36; -fx-alignment: center;");

        VBox info = new VBox(2);
        Label nomUser = new Label(nom);
        nomUser.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label dateLabel = new Label(sdf.format(avis.getDateAvis()));
        dateLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.40); -fx-font-size: 10px;");
        info.getChildren().addAll(nomUser, dateLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Stars badge
        VBox rateBadge = new VBox(2);
        rateBadge.setAlignment(Pos.CENTER_RIGHT);
        Label stars = new Label(getEtoilesStr(avis.getNote()));
        stars.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 15px;");
        Label score = new Label(avis.getNote() + " / 5");
        score.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 10px; -fx-font-weight: bold;");
        rateBadge.getChildren().addAll(stars, score);

        header.getChildren().addAll(avatar, info, spacer, rateBadge);

        if (avis.getCommentaire() != null && !avis.getCommentaire().isEmpty()) {
            Label comment = new Label(avis.getCommentaire());
            comment.setWrapText(true);
            comment.setStyle("-fx-text-fill: rgba(255,255,255,0.78); -fx-font-size: 13px; -fx-line-spacing: 3;");
            card.getChildren().addAll(header, comment);
        } else {
            card.getChildren().add(header);
        }
        return card;
    }

    // ── Étoiles interactives ──────────────────────────────────────────────────
    private void setupEtoilesSelection() {
        etoilesButtons = new Button[5];
        for (int i = 0; i < 5; i++) {
            final int note = i + 1;
            Button btn = new Button("☆");
            btn.setStyle(STAR_EMPTY);
            btn.setOnMouseEntered(e -> highlightUpTo(note));
            btn.setOnMouseExited(e -> renderStars(noteSelectionnee));
            btn.setOnAction(e -> setNote(note));
            etoilesButtons[i] = btn;
            hboxEtoiles.getChildren().add(btn);
        }
    }

    private void highlightUpTo(int hover) {
        for (int i = 0; i < 5; i++) {
            etoilesButtons[i].setText(i < hover ? "★" : "☆");
            etoilesButtons[i].setStyle(i < hover ? STAR_HOVER : STAR_EMPTY);
        }
    }

    private void renderStars(int sel) {
        for (int i = 0; i < 5; i++) {
            etoilesButtons[i].setText(i < sel ? "★" : "☆");
            etoilesButtons[i].setStyle(i < sel ? STAR_FILLED : STAR_EMPTY);
        }
    }

    private void setNote(int note) {
        noteSelectionnee = note;
        renderStars(note);
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    @FXML
    private void ajouterAuPanier(ActionEvent event) {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Session expirée", "Veuillez vous reconnecter.");
            return;
        }
        int qte = quantiteSpinner.getValue();
        Panier panier = panierService.getPanierActif(currentUser.getId());
        if (panier != null && panierService.ajouterProduit(panier.getIdPanier(), produit.getIdProduit(), qte)) {
            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    qte + " × " + produit.getNom() + " ajouté(s) au panier !");
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ajouter le produit au panier.");
        }
    }

    @FXML
    private void toggleFavori(ActionEvent event) {
        if (currentUser == null)
            return;
        boolean isFav = wishlistService.estFavori(currentUser.getId(), produit.getIdProduit());
        if (isFav) {
            wishlistService.supprimerFavori(currentUser.getId(), produit.getIdProduit());
            btnFavori.setText("\u2661");
            btnFavori.setStyle(
                    "-fx-background-color: white; -fx-font-size: 20px; -fx-background-radius: 12; -fx-cursor: hand; -fx-border-color: #dce8dc; -fx-border-radius: 12; -fx-border-width: 1.5; -fx-text-fill: #aaa;");
        } else {
            wishlistService.ajouterFavori(currentUser.getId(), produit.getIdProduit());
            btnFavori.setText("\u2665");
            btnFavori.setStyle(
                    "-fx-background-color: white; -fx-font-size: 20px; -fx-background-radius: 12; -fx-cursor: hand; -fx-border-color: #f5c6cb; -fx-border-radius: 12; -fx-border-width: 1.5; -fx-text-fill: #e74c3c;");
        }
    }

    @FXML
    private void publierAvis(ActionEvent event) {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Vous devez être connecté pour publier un avis.");
            return;
        }
        if (noteSelectionnee == 0) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner une note (1 à 5 étoiles).");
            return;
        }
        String commentaire = taCommentaire.getText().trim();
        Avis nouvelAvis = new Avis(currentUser.getId(), produit.getIdProduit(), noteSelectionnee, commentaire);
        if (avisService.ajouterAvis(nouvelAvis)) {
            taCommentaire.clear();
            setNote(0);
            renderStars(0);
            chargerAvis();
            // Mettre à jour aussi le résumé rating en haut
            double moy = avisService.getNoteMoyenne(produit.getIdProduit());
            int nb = avisService.getNombreAvis(produit.getIdProduit());
            lblEtoiles.setText(buildStarRow(moy));
            lblScoreRating.setText(String.format("%.1f", moy));
            lblNombreAvis.setText("(" + nb + " avis)");
            showAlert(Alert.AlertType.INFORMATION, "Merci !", "Votre avis a été publié avec succès 🌱");
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de publier votre avis. Réessayez.");
        }
    }

    @FXML
    private void retourCatalogue(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/marketplace/CatalogueProduit.fxml", "Ardhi - Catalogue");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String buildStarRow(double moy) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            sb.append(moy >= i ? "★" : (moy >= i - 0.5 ? "⭐" : "☆"));
        }
        return sb.toString();
    }

    private String getEtoilesStr(int note) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++)
            sb.append(i < note ? "★" : "☆");
        return sb.toString();
    }

    private String getNomVendeur() {
        try {
            if (produit.getIdUser() <= 0)
                return "Inconnu";
            User vendeur = userService.chercherParId(produit.getIdUser());
            if (vendeur == null)
                return "Inconnu";
            String full = (vendeur.getPrenom() != null ? vendeur.getPrenom() + " " : "")
                    + (vendeur.getNom() != null ? vendeur.getNom() : "");
            return full.trim().isEmpty() ? vendeur.getEmail() : full.trim();
        } catch (Exception e) {
            return "Inconnu";
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}
