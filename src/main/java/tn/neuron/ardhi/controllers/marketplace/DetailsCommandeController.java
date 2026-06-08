package tn.neuron.ardhi.controllers.marketplace;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import tn.neuron.ardhi.interfaces.marketplace.ICommandeService;
import tn.neuron.ardhi.interfaces.marketplace.IProduitService;
import tn.neuron.ardhi.models.marketplace.Commande;
import tn.neuron.ardhi.models.marketplace.DetailsCommande;
import tn.neuron.ardhi.models.marketplace.EtatCommande;
import tn.neuron.ardhi.models.marketplace.Produit;
import tn.neuron.ardhi.services.marketplace.CommandeService;
import tn.neuron.ardhi.services.marketplace.ProduitService;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class DetailsCommandeController implements Initializable {

    // ── FXML fields ──────────────────────────────────────────────────────────
    @FXML
    private VBox root;
    @FXML
    private Label lblSousTitreHeader;
    @FXML
    private Label lblDate;
    @FXML
    private Label lblStatut;
    @FXML
    private HBox rowSousTotal;
    @FXML
    private Label lblSousTotal;
    @FXML
    private HBox rowReduction;
    @FXML
    private Label lblReduction;
    @FXML
    private Separator sepReduction;
    @FXML
    private HBox rowFraisLivraison;
    @FXML
    private Label lblFraisLivraison;
    @FXML
    private Label lblTotal;
    @FXML
    private Label lblModeLivraison;
    @FXML
    private Label lblProduitsTitre;
    @FXML
    private VBox produitsContainer;
    @FXML
    private Button btnFermer;

    private ICommandeService commandeService;
    private IProduitService produitService;
    private Dialog<?> dialog;

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final String GREEN_DARK = "#2b5329";
    private static final String GREEN_MAIN = "#4a7c47";
    private static final String BORDER = "#e5e7eb";
    private static final String GRAY_TEXT = "#6b7280";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        commandeService = new CommandeService();
        produitService = new ProduitService();
    }

    // ── Init avec la commande ────────────────────────────────────────────────
    public void initCommande(Commande commande) {
        if (commande == null)
            return;

        // Header sous-titre
        lblSousTitreHeader.setText("Commande  #" + commande.getIdCommande());

        // Date
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);
        lblDate.setText(commande.getDateCommande().format(fmt));

        // Statut avec style
        String[] statutInfo = getStatutInfo(commande.getEtat());
        lblStatut.setText(statutInfo[0] + "  " + statutInfo[1]);
        lblStatut.setStyle(
                "-fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-background-color: " + statutInfo[2] + ";" +
                        "-fx-text-fill: " + statutInfo[3] + ";" +
                        "-fx-background-radius: 8; -fx-padding: 4 10 4 10;");

        // Prix
        List<DetailsCommande> details = commandeService.getDetailsByCommande(commande.getIdCommande());
        double sousTotal = details.stream().mapToDouble(d -> d.getPrixUnitaire() * d.getQuantite()).sum();
        double totalCommande = commande.getTotal();
        float fraisLivraison = commande.getFraisLivraison();
        // La réduction = sous-total moins (total - frais), pour isoler la réduction
        // prix des frais livraison
        double reduction = sousTotal - (totalCommande - fraisLivraison);

        if (reduction > 0.01) {
            lblSousTotal.setText(String.format("%.2f DT", sousTotal));
            lblReduction.setText(String.format("- %.2f DT", reduction));
            rowSousTotal.setVisible(true);
            rowSousTotal.setManaged(true);
            rowReduction.setVisible(true);
            rowReduction.setManaged(true);
            sepReduction.setVisible(true);
            sepReduction.setManaged(true);
        }

        // Frais de livraison
        if (rowFraisLivraison != null) {
            if (fraisLivraison > 0.0f) {
                if (lblFraisLivraison != null)
                    lblFraisLivraison.setText(String.format("+%.2f DT", fraisLivraison));
                rowFraisLivraison.setVisible(true);
                rowFraisLivraison.setManaged(true);
            } else {
                rowFraisLivraison.setVisible(false);
                rowFraisLivraison.setManaged(false);
            }
        }

        // Mode de livraison
        if (lblModeLivraison != null) {
            boolean isLivraison = commande.getModeLivraison() != null
                    && commande.getModeLivraison() == tn.neuron.ardhi.models.marketplace.ModeLivraison.LIVRAISON;
            lblModeLivraison.setText(isLivraison ? "🚚 Livraison à domicile" : "🏪 Récupération sur place");
        }

        lblTotal.setText(String.format("%.2f DT", totalCommande));
        lblProduitsTitre.setText("📦  Produits commandés (" + details.size() + ")");

        // Produits rows
        produitsContainer.getChildren().clear();
        for (int i = 0; i < details.size(); i++) {
            DetailsCommande d = details.get(i);
            Produit p = produitService.getProduitById(d.getIdProduit());
            String nom = (p != null) ? p.getNom() : "Produit #" + d.getIdProduit();
            float sousT = d.getPrixUnitaire() * d.getQuantite();

            produitsContainer.getChildren()
                    .add(creerLigneProduit(nom, d.getQuantite(), d.getPrixUnitaire(), sousT, i % 2 == 0));

            if (i < details.size() - 1) {
                Separator sep = new Separator();
                sep.setStyle("-fx-background-color: #f0f3f0; -fx-padding: 0 16;");
                produitsContainer.getChildren().add(sep);
            }
        }
    }

    // ── Ligne produit ────────────────────────────────────────────────────────
    private HBox creerLigneProduit(String nom, int quantite, float prixU, float sousTotal, boolean pair) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setStyle("-fx-background-color: " + (pair ? "white" : "#fafcfa") + ";");

        // Icône + nom
        HBox left = new HBox(10);
        left.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label iconLabel = new Label("🌿");
        iconLabel.setStyle("-fx-font-size: 14px;");
        iconLabel.setMinWidth(24);

        VBox nameBox = new VBox(2);
        Label nomLabel = new Label(nom);
        nomLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + GREEN_DARK + ";");
        nomLabel.setWrapText(true);
        nomLabel.setMaxWidth(180);

        Label prixLabel = new Label(String.format("%.2f DT × %d", prixU, quantite));
        prixLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + GRAY_TEXT + ";");
        nameBox.getChildren().addAll(nomLabel, prixLabel);

        left.getChildren().addAll(iconLabel, nameBox);

        // Badge sous-total
        Label totalBadge = new Label(String.format("%.2f DT", sousTotal));
        totalBadge.setStyle(
                "-fx-background-color: #e6f5ee;" +
                        "-fx-text-fill: " + GREEN_MAIN + ";" +
                        "-fx-font-size: 12px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 8; -fx-padding: 4 10 4 10;");

        row.getChildren().addAll(left, totalBadge);
        return row;
    }

    // ── Statut helper ────────────────────────────────────────────────────────
    private String[] getStatutInfo(EtatCommande etat) {
        if (etat == null)
            return new String[] { "❓", "Inconnu", "#f0f3f0", "#333" };
        return switch (etat) {
            case en_attente -> new String[] { "⏳", "En attente", "#fff8e1", "#b7860b" };
            case en_cours -> new String[] { "🔄", "En cours", "#e3f0ff", "#1a5faa" };
            case livree -> new String[] { "✅", "Livrée", "#e6f5ee", "#046436" };
            case annulee -> new String[] { "❌", "Annulée", "#fdecea", "#c0392b" };
        };
    }

    public void setDialog(Dialog<?> dialog) {
        this.dialog = dialog;
    }

    @FXML
    private void fermer() {
        if (dialog != null) {
            dialog.close();
        } else if (btnFermer != null && btnFermer.getScene() != null) {
            btnFermer.getScene().getWindow().hide();
        }
    }

    // ── LigneDetailCommande (gardé pour compatibilité) ───────────────────────
    public static class LigneDetailCommande {
        private final String nomProduit;
        private final int quantite;
        private final float prixUnitaire;
        private final float sousTotal;

        public LigneDetailCommande(String nomProduit, int quantite, float prixUnitaire, float sousTotal) {
            this.nomProduit = nomProduit;
            this.quantite = quantite;
            this.prixUnitaire = prixUnitaire;
            this.sousTotal = sousTotal;
        }

        public String getNomProduit() {
            return nomProduit;
        }

        public int getQuantite() {
            return quantite;
        }

        public float getPrixUnitaire() {
            return prixUnitaire;
        }

        public float getSousTotal() {
            return sousTotal;
        }
    }
}