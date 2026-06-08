package tn.neuron.ardhi.controllers.marketplace;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.models.marketplace.Produit;
import tn.neuron.ardhi.models.marketplace.TypeReduction;
import tn.neuron.ardhi.services.marketplace.AvisService;
import tn.neuron.ardhi.services.marketplace.WishlistService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la page "Mes Favoris".
 * Affiche les produits sauvegardés par l'utilisateur courant.
 */
public class MesFavorisController implements Initializable {

    @FXML
    private GridPane favorisGrid;
    @FXML
    private VBox emptyStateBox;
    @FXML
    private Label lblNbFavoris;

    // ── Design system (same palette as CatalogueProduitController) ────────────
    private static final String GREEN_DARK = "#2b5329";
    private static final String GREEN_MAIN = "#4a7c47";
    private static final String RED_FAV = "#e74c3c";
    private static final String ORANGE = "#e05c1a";
    private static final String STAR_GOLD = "#f59e0b";
    private static final String GRAY_BG = "#f9fafb";
    private static final String GRAY_TEXT = "#6b7280";
    private static final String BORDER = "#e5e7eb";

    private WishlistService wishlistService;
    private AvisService avisService;
    private User currentUser;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        wishlistService = new WishlistService();
        avisService = new AvisService();

        currentUser = UserSession.getInstance() != null ? UserSession.getInstance().getUser() : null;
        chargerFavoris();
    }

    // ── Chargement ─────────────────────────────────────────────────────────────
    private void chargerFavoris() {
        favorisGrid.getChildren().clear();

        if (currentUser == null) {
            afficherEtatVide();
            return;
        }

        List<Produit> favoris = wishlistService.getFavoris(currentUser.getId());

        if (favoris.isEmpty()) {
            afficherEtatVide();
            return;
        }

        // Mettre à jour le badge
        if (lblNbFavoris != null)
            lblNbFavoris.setText(favoris.size() + " favori(s)");

        // Afficher la grille
        emptyStateBox.setVisible(false);
        emptyStateBox.setManaged(false);
        favorisGrid.setVisible(true);
        favorisGrid.setManaged(true);

        final int COLS = 4;
        int col = 0, row = 0;

        for (int i = 0; i < favoris.size(); i++) {
            Produit produit = favoris.get(i);
            VBox card = creerCarteFavori(produit);

            // Fade-in animate
            card.setOpacity(0);
            favorisGrid.add(card, col, row);

            FadeTransition ft = new FadeTransition(Duration.millis(250 + i * 40L), card);
            ft.setFromValue(0);
            ft.setToValue(1);

            TranslateTransition tt = new TranslateTransition(Duration.millis(250 + i * 40L), card);
            tt.setFromY(15);
            tt.setToY(0);

            ft.play();
            tt.play();

            col++;
            if (col == COLS) {
                col = 0;
                row++;
            }
        }
    }

    private void afficherEtatVide() {
        favorisGrid.setVisible(false);
        favorisGrid.setManaged(false);
        emptyStateBox.setVisible(true);
        emptyStateBox.setManaged(true);
        if (lblNbFavoris != null)
            lblNbFavoris.setText("0 favori(s)");
    }

    // ── Carte favori ───────────────────────────────────────────────────────────
    private VBox creerCarteFavori(Produit produit) {
        boolean enStock = produit.getQuantiteStock() > 0;

        // Carte principale
        VBox card = new VBox(0);
        card.setPrefWidth(280);
        card.setMaxWidth(280);
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle(styleCard(false));
        card.setOnMouseEntered(e -> card.setStyle(styleCard(true)));
        card.setOnMouseExited(e -> card.setStyle(styleCard(false)));

        // ── Zone image ──────────────────────────────────────────────────────
        StackPane imgZone = new StackPane();
        imgZone.setPrefHeight(190);
        imgZone.setMinHeight(190);
        imgZone.setMaxHeight(190);
        imgZone.setStyle("-fx-background-color: #eef2ef; -fx-background-radius: 17 17 0 0;");

        if (produit.getImage() != null && !produit.getImage().isEmpty()) {
            File file = new File(produit.getImage());
            if (file.exists()) {
                ImageView iv = new ImageView(new Image(file.toURI().toString()));
                iv.setFitWidth(280);
                iv.setFitHeight(190);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
                imgZone.getChildren().add(iv);
            } else {
                imgZone.getChildren().add(makePlaceholder());
            }
        } else {
            imgZone.getChildren().add(makePlaceholder());
        }

        // Overlay gradient
        Label overlay = new Label();
        overlay.setMaxWidth(Double.MAX_VALUE);
        overlay.setPrefHeight(60);
        overlay.setStyle("-fx-background-color: linear-gradient(to top, rgba(13,31,21,0.38), transparent);");
        StackPane.setAlignment(overlay, Pos.BOTTOM_RIGHT);

        // Bouton cœur rouge (déjà favori) — clic = retirer
        Button favBtn = new Button("\u2665");
        String base = "-fx-background-color: white; -fx-font-size: 16px; -fx-background-radius: 50;" +
                "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0, 0, 2);" +
                "-fx-pref-width: 34; -fx-pref-height: 34; -fx-min-width: 34; -fx-min-height: 34;";
        favBtn.setStyle(base + "-fx-text-fill: " + RED_FAV + ";");
        StackPane.setAlignment(favBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(favBtn, new Insets(10, 10, 0, 0));

        favBtn.setOnAction(e -> {
            if (currentUser == null)
                return;
            wishlistService.supprimerFavori(currentUser.getId(), produit.getIdProduit());
            // Retirer la carte et mettre à jour l'affichage
            chargerFavoris();
        });

        // Badge catégorie
        Label categBadge = new Label("🌿  " + produit.getCategorie());
        categBadge.setStyle(
                "-fx-background-color: rgba(255,255,255,0.93);" +
                        "-fx-text-fill: " + GREEN_MAIN + ";" +
                        "-fx-font-size: 10px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 20; -fx-padding: 4 10 4 10;");
        StackPane.setAlignment(categBadge, Pos.BOTTOM_LEFT);
        StackPane.setMargin(categBadge, new Insets(0, 0, 10, 10));

        // Badge stock
        Label stockBadge = new Label(enStock ? "✓ En stock" : "✗ Rupture");
        stockBadge.setStyle(
                "-fx-background-color: " + (enStock ? GREEN_MAIN : RED_FAV) + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 10px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 20; -fx-padding: 4 10 4 10;");
        StackPane.setAlignment(stockBadge, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(stockBadge, new Insets(0, 10, 10, 0));

        imgZone.getChildren().addAll(overlay, favBtn, categBadge, stockBadge);

        // Badge SOLDE
        if (produit.aUneRemise()) {
            String badgeText = produit.getTypeRemise() == TypeReduction.POURCENTAGE
                    ? "SOLDE  -" + (int) produit.getRemise() + "%"
                    : "SOLDE  -" + String.format("%.2f", produit.getRemise()) + " DT";
            Label soldeBadge = new Label(badgeText);
            soldeBadge.setStyle(
                    "-fx-background-color: " + RED_FAV + ";" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 10px; -fx-font-weight: bold;" +
                            "-fx-background-radius: 0 0 12 0; -fx-padding: 5 12 5 12;");
            StackPane.setAlignment(soldeBadge, Pos.TOP_LEFT);
            imgZone.getChildren().add(soldeBadge);
        }

        // ── Contenu ─────────────────────────────────────────────────────────
        VBox content = new VBox(10);
        content.setPadding(new Insets(16, 18, 18, 18));

        Label nomLabel = new Label(produit.getNom());
        nomLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + GREEN_DARK + ";");
        nomLabel.setWrapText(true);
        nomLabel.setMaxWidth(244);

        // Prix
        VBox prixBlock = new VBox(2);
        if (produit.aUneRemise()) {
            Label prixBarre = new Label(String.format("%.2f DT", produit.getPrix()));
            prixBarre.setStyle("-fx-font-size: 11px; -fx-text-fill: #bbb; -fx-strikethrough: true;");
            prixBlock.getChildren().add(prixBarre);
        }
        HBox prixRow = new HBox(4);
        prixRow.setAlignment(Pos.CENTER_LEFT);
        Label prixLabel = new Label(String.format("%.2f", produit.getPrixApresRemise()));
        String prixColor = produit.aUneRemise() ? RED_FAV : GREEN_DARK;
        prixLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + prixColor + ";");
        Label devLabel = new Label(" DT" + (produit.getUniteMesure() != null ? "/" + produit.getUniteMesure() : ""));
        devLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + GRAY_TEXT + "; -fx-padding: 0 0 4 0;");
        devLabel.setAlignment(Pos.BOTTOM_LEFT);

        // Rating
        double moyenne = avisService.getNoteMoyenne(produit.getIdProduit());
        int nbAvis = avisService.getNombreAvis(produit.getIdProduit());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox starsRow = new HBox(2);
        starsRow.setAlignment(Pos.CENTER_RIGHT);
        for (int i = 0; i < 5; i++) {
            Label star = new Label(i < (int) Math.round(moyenne) ? "★" : "☆");
            star.setStyle("-fx-font-size: 11px; -fx-text-fill: " +
                    (i < (int) Math.round(moyenne) ? STAR_GOLD : "#ccc") + ";");
            starsRow.getChildren().add(star);
        }
        Label avisLabel = new Label(nbAvis + " avis");
        avisLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + GRAY_TEXT + ";");
        VBox ratingBox = new VBox(1);
        ratingBox.setAlignment(Pos.CENTER_RIGHT);
        ratingBox.getChildren().addAll(starsRow, avisLabel);

        prixRow.getChildren().addAll(prixLabel, devLabel, spacer, ratingBox);
        prixBlock.getChildren().add(prixRow);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #eef2ef;");

        // Bouton Retirer des favoris
        Button retirerBtn = new Button("✕  Retirer des favoris");
        retirerBtn.setPrefWidth(244);
        retirerBtn.setPrefHeight(40);
        retirerBtn.setStyle(
                "-fx-background-color: #fff0f0; -fx-text-fill: " + RED_FAV + ";" +
                        "-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;" +
                        "-fx-border-color: #f5c6cb; -fx-border-radius: 12; -fx-border-width: 1;");
        retirerBtn.setOnMouseEntered(e -> retirerBtn.setStyle(
                "-fx-background-color: #e74c3c; -fx-text-fill: white;" +
                        "-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;"));
        retirerBtn.setOnMouseExited(e -> retirerBtn.setStyle(
                "-fx-background-color: #fff0f0; -fx-text-fill: " + RED_FAV + ";" +
                        "-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;" +
                        "-fx-border-color: #f5c6cb; -fx-border-radius: 12; -fx-border-width: 1;"));
        retirerBtn.setOnAction(e -> {
            if (currentUser == null)
                return;
            wishlistService.supprimerFavori(currentUser.getId(), produit.getIdProduit());
            chargerFavoris();
        });

        content.getChildren().addAll(nomLabel, prixBlock, sep, retirerBtn);
        card.getChildren().addAll(imgZone, content);
        return card;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private String styleCard(boolean hovered) {
        if (hovered) {
            return "-fx-background-color: white;" +
                    "-fx-background-radius: 20; -fx-border-color: " + RED_FAV + ";" +
                    "-fx-border-radius: 20; -fx-border-width: 1.5;" +
                    "-fx-effect: dropshadow(gaussian, rgba(231,76,60,0.22), 28, 0, 0, 10);" +
                    "-fx-translate-y: -5;";
        }
        return "-fx-background-color: white;" +
                "-fx-background-radius: 20; -fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 20; -fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 18, 0, 0, 5);" +
                "-fx-translate-y: 0;";
    }

    private Label makePlaceholder() {
        Label ph = new Label("🌾");
        ph.setStyle("-fx-font-size: 52px;");
        return ph;
    }

    // ── Navigation ─────────────────────────────────────────────────────────────
    @FXML
    private void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/marketplace/Marketplace.fxml", "Ardhi - Marketplace");
    }

    @FXML
    private void allerAuCatalogue(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/marketplace/CatalogueProduit.fxml", "Ardhi - Catalogue");
    }
}
