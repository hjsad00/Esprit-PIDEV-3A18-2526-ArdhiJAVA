package tn.neuron.ardhi.controllers.marketplace;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tn.neuron.ardhi.models.marketplace.Panier;
import tn.neuron.ardhi.models.marketplace.Produit;
import tn.neuron.ardhi.models.marketplace.TypeReduction;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.services.marketplace.AvisService;
import tn.neuron.ardhi.services.marketplace.PanierService;
import tn.neuron.ardhi.services.UserAndDiag.UserService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ComparaisonProduitsController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private VBox  colonneLibelles;
    @FXML private VBox  colonneProduit1;
    @FXML private VBox  colonneProduit2;
    @FXML private VBox  colProduit1Header;
    @FXML private VBox  colProduit2Header;
    @FXML private VBox  colProduit1Actions;
    @FXML private VBox  colProduit2Actions;
    @FXML private Label lblNbProduits;

    // ── State ─────────────────────────────────────────────────────────────────
    private List<Produit> produits = new ArrayList<>();
    private AvisService   avisService;
    private PanierService panierService;
    private UserService   userService;

    private CatalogueProduitController catalogueController;
    private Stage ownerStage;

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final String GREEN_DARK  = "#0d1f15";
    private static final String GREEN_MAIN  = "#046436";
    private static final String GREEN_LIGHT = "#e6f5ee";
    private static final String RED_BADGE   = "#e74c3c";
    private static final String STAR_GOLD   = "#f0a500";
    private static final String GRAY_TEXT   = "#6a8a78";
    private static final String BORDER      = "#e0e8e2";
    private static final String ROW_ODD     = "#f8faf8";
    private static final String ROW_EVEN    = "white";

    // ══════════════════════════════════════════════════════════════════════════
    // INITIALIZE
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        avisService   = new AvisService();
        panierService = new PanierService();
        userService   = new UserService();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POINT D'ENTRÉE
    // ══════════════════════════════════════════════════════════════════════════

    public void chargerComparaison(List<Produit> produitsAComparer) {
        this.produits = new ArrayList<>(produitsAComparer);
        lblNbProduits.setText(produits.size() + " produits en comparaison");
        construireHeaders();
        construireTableau();
        construireActionsPanel();
    }

    public void setCatalogueController(CatalogueProduitController ctrl) {
        this.catalogueController = ctrl;
    }

    public void setOwnerStage(Stage stage) {
        this.ownerStage = stage;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HEADERS IMAGES
    // ══════════════════════════════════════════════════════════════════════════

    private void construireHeaders() {
        remplirHeader(colProduit1Header, produits.get(0));
        remplirHeader(colProduit2Header, produits.get(1));
    }

    private void remplirHeader(VBox col, Produit p) {
        col.getChildren().clear();

        // Image
        StackPane imgZone = new StackPane();
        imgZone.setPrefSize(180, 160);
        imgZone.setMinSize(180, 160);
        imgZone.setMaxSize(180, 160);
        imgZone.setStyle("-fx-background-color: #eef2ef; -fx-background-radius: 14;");

        if (p.getImage() != null && !p.getImage().isEmpty()) {
            File f = new File(p.getImage());
            if (f.exists()) {
                ImageView iv = new ImageView(new Image(f.toURI().toString()));
                iv.setFitWidth(180);
                iv.setFitHeight(160);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
                imgZone.getChildren().add(iv);
            } else {
                imgZone.getChildren().add(makePlaceholder());
            }
        } else {
            imgZone.getChildren().add(makePlaceholder());
        }

        // Badge solde
        if (p.aUneRemise()) {
            String txt = p.getTypeRemise() == TypeReduction.POURCENTAGE
                    ? "-" + (int) p.getRemise() + "%"
                    : "-" + String.format("%.0f", p.getRemise()) + " DT";
            Label badge = new Label("🔖 " + txt);
            badge.setStyle(
                    "-fx-background-color: " + RED_BADGE + "; -fx-text-fill: white;" +
                            "-fx-font-size: 10px; -fx-font-weight: bold;" +
                            "-fx-background-radius: 0 8 0 8; -fx-padding: 4 8;");
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            imgZone.getChildren().add(badge);
        }

        // Nom
        Label nomLbl = new Label(p.getNom());
        nomLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + GREEN_DARK + ";");
        nomLbl.setWrapText(true);
        nomLbl.setAlignment(Pos.CENTER);
        nomLbl.setMaxWidth(300);

        // Vendeur
        Label vendeurLbl = new Label("📍 " + getNomVendeur(p));
        vendeurLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + GRAY_TEXT + ";");

        // Catégorie badge
        Label catBadge = new Label("🌿 " + p.getCategorie());
        catBadge.setStyle(
                "-fx-background-color: " + GREEN_LIGHT + "; -fx-text-fill: " + GREEN_MAIN + ";" +
                        "-fx-font-size: 10px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 20; -fx-padding: 4 10;");

        col.getChildren().addAll(imgZone, nomLbl, vendeurLbl, catBadge);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TABLEAU COMPARATIF
    // ══════════════════════════════════════════════════════════════════════════

    private void construireTableau() {
        double[] prix  = produits.stream().mapToDouble(p -> p.getPrixApresRemise()).toArray();
        double[] notes = produits.stream().mapToDouble(p -> avisService.getNoteMoyenne(p.getIdProduit())).toArray();
        double[] avis  = produits.stream().mapToDouble(p -> avisService.getNombreAvis(p.getIdProduit())).toArray();
        double[] stock = produits.stream().mapToDouble(p -> p.getQuantiteStock()).toArray();

        Object[][] criteres = {
                { "💰  Prix",             prix,  "best_low"  },
                { "⭐  Note moyenne",     notes, "best_high" },
                { "💬  Nombre d'avis",   avis,  "best_high" },
                { "📦  Stock disponible", stock, "best_high" },
                { "🏷  Catégorie",        null,  "neutral"   },
                { "👤  Vendeur",          null,  "neutral"   },
                { "📏  Unité",            null,  "neutral"   },
                { "📝  Description",      null,  "neutral"   },
        };

        for (int c = 0; c < criteres.length; c++) {
            String   libelle = (String)   criteres[c][0];
            double[] vals    = (double[]) criteres[c][1];
            String   type    = (String)   criteres[c][2];
            boolean  isOdd   = (c % 2 == 0);

            // Cellule libellé
            VBox cellLabel = new VBox();
            cellLabel.setPrefHeight(70);
            cellLabel.setMinHeight(70);
            cellLabel.setPadding(new Insets(0, 16, 0, 20));
            cellLabel.setAlignment(Pos.CENTER_LEFT);
            cellLabel.setStyle(
                    "-fx-background-color: " + (isOdd ? ROW_ODD : ROW_EVEN) + ";" +
                            "-fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0;");
            Label lblCritere = new Label(libelle);
            lblCritere.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #4a6a58;");
            cellLabel.getChildren().add(lblCritere);
            colonneLibelles.getChildren().add(cellLabel);

            // Best / Worst
            int bestIdx  = (vals != null) ? findBest(vals,  type.equals("best_low")) : -1;
            int worstIdx = (vals != null) ? findWorst(vals, type.equals("best_low")) : -1;

            // Cellules produits
            colonneProduit1.getChildren().add(creerCellule(c, 0, vals, bestIdx, worstIdx, isOdd));
            colonneProduit2.getChildren().add(creerCellule(c, 1, vals, bestIdx, worstIdx, isOdd));
        }
    }

    private VBox creerCellule(int critereIdx, int prodIdx,
                              double[] vals, int bestIdx, int worstIdx, boolean isOdd) {
        boolean isBest  = (bestIdx  == prodIdx);
        boolean isWorst = (worstIdx == prodIdx);

        VBox cell = new VBox(4);
        cell.setPrefHeight(70);
        cell.setMinHeight(70);
        cell.setPadding(new Insets(10, 20, 10, 20));
        cell.setAlignment(Pos.CENTER_LEFT);

        String bg = isBest ? "#e6f9ef" : isWorst ? "#fff0ee" : (isOdd ? ROW_ODD : ROW_EVEN);
        cell.setStyle("-fx-background-color: " + bg + ";" +
                "-fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0;");

        Produit p = produits.get(prodIdx);

        switch (critereIdx) {
            case 0 -> { // Prix
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                String prixColor = isBest ? GREEN_MAIN : isWorst ? RED_BADGE : GREEN_DARK;
                Label prixLbl = new Label(String.format("%.2f DT", p.getPrixApresRemise()));
                prixLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + prixColor + ";");
                row.getChildren().add(prixLbl);
                if (p.aUneRemise()) {
                    Label barre = new Label(String.format("%.2f DT", p.getPrix()));
                    barre.setStyle("-fx-font-size: 11px; -fx-text-fill: #bbb; -fx-strikethrough: true;");
                    row.getChildren().add(barre);
                }
                cell.getChildren().add(row);
                if (isBest)  cell.getChildren().add(makeBestBadge("🏆 Moins cher"));
                if (isWorst) cell.getChildren().add(makeWorstBadge("💸 Plus cher"));
            }
            case 1 -> { // Note moyenne
                double note = avisService.getNoteMoyenne(p.getIdProduit());
                HBox starsRow = new HBox(2);
                starsRow.setAlignment(Pos.CENTER_LEFT);
                for (int s = 0; s < 5; s++) {
                    Label star = new Label(s < (int) Math.round(note) ? "★" : "☆");
                    star.setStyle("-fx-font-size: 14px; -fx-text-fill: " +
                            (s < (int) Math.round(note) ? STAR_GOLD : "#ccc") + ";");
                    starsRow.getChildren().add(star);
                }
                Label noteLbl = new Label(String.format("  %.1f / 5", note));
                noteLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + GREEN_DARK + ";");
                starsRow.getChildren().add(noteLbl);
                cell.getChildren().add(starsRow);
                if (isBest)  cell.getChildren().add(makeBestBadge("🏆 Mieux noté"));
                if (isWorst) cell.getChildren().add(makeWorstBadge("👎 Moins bien noté"));
            }
            case 2 -> { // Nombre d'avis
                Label lbl = new Label(avisService.getNombreAvis(p.getIdProduit()) + " avis");
                lbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + GREEN_DARK + ";");
                cell.getChildren().add(lbl);
                if (isBest)  cell.getChildren().add(makeBestBadge("🏆 Plus d'avis"));
                if (isWorst) cell.getChildren().add(makeWorstBadge("💬 Moins d'avis"));
            }
            case 3 -> { // Stock
                boolean en = p.getQuantiteStock() > 0;
                Label lbl = new Label(en
                        ? "✓  " + p.getQuantiteStock() + " disponible(s)"
                        : "✗  Rupture de stock");
                lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " +
                        (en ? GREEN_MAIN : RED_BADGE) + ";");
                cell.getChildren().add(lbl);
                if (isBest)  cell.getChildren().add(makeBestBadge("🏆 Plus en stock"));
                if (isWorst) cell.getChildren().add(makeWorstBadge("⚠ Stock faible"));
            }
            case 4 -> cell.getChildren().add(makeNeutralLabel("🌿 " + p.getCategorie()));
            case 5 -> cell.getChildren().add(makeNeutralLabel("👤 " + getNomVendeur(p)));
            case 6 -> cell.getChildren().add(makeNeutralLabel(
                    p.getUniteMesure() != null ? p.getUniteMesure().toString() : "—"));
            case 7 -> {
                String desc = p.getDescription() != null
                        ? (p.getDescription().length() > 55
                        ? p.getDescription().substring(0, 55) + "…"
                        : p.getDescription())
                        : "—";
                Label lbl = new Label(desc);
                lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + GRAY_TEXT + ";");
                lbl.setWrapText(true);
                lbl.setMaxWidth(300);
                cell.getChildren().add(lbl);
            }
        }
        return cell;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ACTIONS PANIER
    // ══════════════════════════════════════════════════════════════════════════

    private void construireActionsPanel() {
        remplirActions(colProduit1Actions, produits.get(0));
        remplirActions(colProduit2Actions, produits.get(1));
    }

    private void remplirActions(VBox col, Produit p) {
        col.getChildren().clear();
        boolean enStock = p.getQuantiteStock() > 0;

        // Spinner quantité
        HBox spinnerRow = new HBox(10);
        spinnerRow.setAlignment(Pos.CENTER);
        Label qteLabel = new Label("Quantité :");
        qteLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + GRAY_TEXT + "; -fx-font-weight: bold;");
        Spinner<Integer> spinner = new Spinner<>(1, Math.max(1, p.getQuantiteStock()), 1);
        spinner.setPrefWidth(90);
        spinner.setPrefHeight(34);
        spinner.setEditable(true);
        spinner.setStyle(
                "-fx-background-color: #f8faf8; -fx-background-radius: 8;" +
                        "-fx-border-color: " + BORDER + "; -fx-border-radius: 8; -fx-border-width: 1;");
        spinnerRow.getChildren().addAll(qteLabel, spinner);

        // Bouton panier
        Button btnPanier = new Button(enStock ? "🛒   Ajouter au panier" : "✗  Indisponible");
        btnPanier.setMaxWidth(Double.MAX_VALUE);
        btnPanier.setPrefHeight(46);
        btnPanier.setDisable(!enStock);
        String baseStyle = enStock
                ? "-fx-background-color: " + GREEN_MAIN + "; -fx-text-fill: white; -fx-font-size: 13px;" +
                "-fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 4);"
                : "-fx-background-color: #d8e0d9; -fx-text-fill: #aaa; -fx-font-size: 13px;" +
                "-fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: default;";
        btnPanier.setStyle(baseStyle);
        if (enStock) {
            btnPanier.setOnMouseEntered(e -> btnPanier.setStyle(
                    "-fx-background-color: #034e2b; -fx-text-fill: white; -fx-font-size: 13px;" +
                            "-fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;" +
                            "-fx-effect: dropshadow(gaussian, rgba(4,100,54,0.5), 18, 0, 0, 6);"));
            btnPanier.setOnMouseExited(e -> btnPanier.setStyle(baseStyle));
            btnPanier.setOnAction(e -> ajouterAuPanier(p, spinner.getValue()));
        }

        // Bouton détail
        Button btnDetail = new Button("🔍   Voir le détail  →");
        btnDetail.setMaxWidth(Double.MAX_VALUE);
        btnDetail.setPrefHeight(38);
        String detailBase =
                "-fx-background-color: transparent; -fx-text-fill: " + GREEN_MAIN + ";" +
                        "-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;" +
                        "-fx-border-color: " + BORDER + "; -fx-border-radius: 12; -fx-border-width: 1;";
        btnDetail.setStyle(detailBase);
        btnDetail.setOnMouseEntered(e -> btnDetail.setStyle(
                "-fx-background-color: #e8f5ec; -fx-text-fill: " + GREEN_MAIN + ";" +
                        "-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;" +
                        "-fx-border-color: #b8dfc8; -fx-border-radius: 12; -fx-border-width: 1;"));
        btnDetail.setOnMouseExited(e -> btnDetail.setStyle(detailBase));
        btnDetail.setOnAction(e -> ouvrirDetailProduit(p));

        col.getChildren().addAll(spinnerRow, btnPanier, btnDetail);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    void retourCatalogue(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/marketplace/CatalogueProduit.fxml"));
            Parent root = loader.load();
            if (ownerStage != null) {
                ownerStage.setTitle("Ardhi - Catalogue");
                ownerStage.getScene().setRoot(root);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de retourner au catalogue.");
        }
    }

    private void ouvrirDetailProduit(Produit produit) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/marketplace/DetailProduit.fxml"));
            Parent root = loader.load();
            DetailProduitController ctrl = loader.getController();
            ctrl.setProduit(produit);
            if (ownerStage != null) {
                ownerStage.setTitle("Ardhi - " + produit.getNom());
                ownerStage.getScene().setRoot(root);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le détail.");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PANIER
    // ══════════════════════════════════════════════════════════════════════════

    private void ajouterAuPanier(Produit produit, int quantite) {
        User currentUser = UserSession.getInstance() != null ? UserSession.getInstance().getUser() : null;
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Session", "Reconnectez-vous.");
            return;
        }
        Panier panier = panierService.getPanierActif(currentUser.getId());
        if (panier != null && panierService.ajouterProduit(panier.getIdPanier(), produit.getIdProduit(), quantite)) {
            showAlert(Alert.AlertType.INFORMATION, "Panier",
                    quantite + " × " + produit.getNom() + " ajouté(s) au panier !");
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ajouter le produit.");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS BEST VALUE
    // ══════════════════════════════════════════════════════════════════════════

    private int findBest(double[] vals, boolean lowerIsBetter) {
        int best = 0;
        for (int i = 1; i < vals.length; i++) {
            if (lowerIsBetter ? vals[i] < vals[best] : vals[i] > vals[best]) best = i;
        }
        return best;
    }

    private int findWorst(double[] vals, boolean lowerIsBetter) {
        boolean allEqual = true;
        for (int i = 1; i < vals.length; i++) {
            if (vals[i] != vals[0]) { allEqual = false; break; }
        }
        if (allEqual) return -1;

        int worst = 0;
        for (int i = 1; i < vals.length; i++) {
            if (lowerIsBetter ? vals[i] > vals[worst] : vals[i] < vals[worst]) worst = i;
        }
        return worst;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UI HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private Label makeBestBadge(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(
                "-fx-background-color: " + GREEN_MAIN + "; -fx-text-fill: white;" +
                        "-fx-font-size: 9px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 20; -fx-padding: 3 8;");
        return lbl;
    }

    private Label makeWorstBadge(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(
                "-fx-background-color: " + RED_BADGE + "; -fx-text-fill: white;" +
                        "-fx-font-size: 9px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 20; -fx-padding: 3 8;");
        return lbl;
    }

    private Label makeNeutralLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + GREEN_DARK + ";");
        return lbl;
    }

    private Label makePlaceholder() {
        Label ph = new Label("🌾");
        ph.setStyle("-fx-font-size: 48px;");
        return ph;
    }

    private String getNomVendeur(Produit produit) {
        try {
            if (produit.getIdUser() <= 0) return "Inconnu";
            User vendeur = userService.chercherParId(produit.getIdUser());
            if (vendeur == null) return "Inconnu";
            String nom = (vendeur.getPrenom() != null ? vendeur.getPrenom() + " " : "") +
                    (vendeur.getNom() != null ? vendeur.getNom() : "");
            return nom.trim().isEmpty() ? vendeur.getEmail() : nom.trim();
        } catch (Exception e) {
            return "Inconnu";
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}