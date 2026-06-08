package tn.neuron.ardhi.controllers.marketplace;

import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import tn.neuron.ardhi.interfaces.marketplace.ICommandeService;
import tn.neuron.ardhi.interfaces.marketplace.IPanierService;
import tn.neuron.ardhi.models.marketplace.Commande;
import tn.neuron.ardhi.models.marketplace.DetailsCommande;
import tn.neuron.ardhi.models.marketplace.Panier;
import tn.neuron.ardhi.models.marketplace.PanierProduit;
import tn.neuron.ardhi.models.marketplace.Coupon;
import tn.neuron.ardhi.models.marketplace.TypeReduction;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.services.marketplace.*;
import tn.neuron.ardhi.interfaces.marketplace.ICouponService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.marketplace.LocalPaymentServer;
import tn.neuron.ardhi.utils.marketplace.PdfCommande;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import tn.neuron.ardhi.models.marketplace.ModeLivraison;

public class PanierController implements Initializable {

    @FXML
    private TableView<PanierProduit> tablePanier;

    @FXML
    private TableColumn<PanierProduit, String> colNom;

    @FXML
    private TableColumn<PanierProduit, Float> colPrix;

    @FXML
    private TableColumn<PanierProduit, Integer> colQuantite;

    @FXML
    private TableColumn<PanierProduit, Float> colSousTotal;

    @FXML
    private TableColumn<PanierProduit, Void> colActions;

    @FXML
    private Label lblTotalProduits;

    @FXML
    private Label lblTotalProduitsHeader;

    @FXML
    private Label lblTotalMontant;

    @FXML
    private Button btnViderPanier;

    @FXML
    private Button btnValiderCommande;

    // Coupon UI
    @FXML
    private TextField tfCodeCoupon;
    @FXML
    private Button btnAppliquerCoupon;
    @FXML
    private Label lblMessageCoupon;
    @FXML
    private Label lblReduction;
    @FXML
    private javafx.scene.layout.HBox boxReduction;

    @FXML
    private Button btnContinuerAchats;

    // Points de fidélité UI
    @FXML
    private RadioButton rbCarte;
    @FXML
    private RadioButton rbPoints;
    @FXML
    private Label lblSoldePoints;
    @FXML
    private javafx.scene.layout.VBox boxPaiementPoints; // section entière points (peut être cachée si solde = 0)

    private ToggleGroup togglePaiement;

    // Livraison UI
    @FXML
    private RadioButton rbRecuperation;
    @FXML
    private RadioButton rbLivraison;
    @FXML
    private Label lblInfoLivraison;
    @FXML
    private javafx.scene.layout.HBox boxFraisLivraison;
    @FXML
    private Label lblFraisLivraison;
    private ToggleGroup toggleGroupLivraison;

    private IPanierService panierService;
    private ICommandeService commandeService;
    private ICouponService couponService;
    private StripeService stripeService;
    private LocalPaymentServer localPaymentServer;
    private Coupon couponApplique;
    private int userId;
    private Panier panierActif;
    private ObservableList<PanierProduit> produitsPanier;
    private CatalogueProduitController marketplaceController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (UserSession.getInstance() == null || UserSession.getInstance().getUser() == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur de session",
                    "Aucun utilisateur connecté.");
            return;
        }

        panierService = new PanierService();
        commandeService = new CommandeService();
        couponService = new CouponService();
        try {
            stripeService = new StripeService();
        } catch (Exception e) {
            System.err.println("[PanierController] StripeService non initialisé : " + e.getMessage());
            stripeService = null;
        }
        produitsPanier = FXCollections.observableArrayList();

        // Initialiser le ToggleGroup pour les modes de paiement
        togglePaiement = new ToggleGroup();
        if (rbCarte != null)
            rbCarte.setToggleGroup(togglePaiement);
        if (rbPoints != null)
            rbPoints.setToggleGroup(togglePaiement);
        if (rbCarte != null)
            rbCarte.setSelected(true); // Carte sélectionnée par défaut

        setupTableColumns();

        // Initialiser le ToggleGroup pour la livraison
        toggleGroupLivraison = new ToggleGroup();
        if (rbRecuperation != null)
            rbRecuperation.setToggleGroup(toggleGroupLivraison);
        if (rbLivraison != null)
            rbLivraison.setToggleGroup(toggleGroupLivraison);
        if (rbRecuperation != null)
            rbRecuperation.setSelected(true);

        User currentUser = UserSession.getInstance().getUser();
        this.userId = currentUser.getId();
        chargerPanier();

        // Afficher le solde de points de fidélité
        mettreAJourAffichagePoints();
    }

    // Met à jour le label du solde de points depuis la base
    private void mettreAJourAffichagePoints() {
        double solde = ((CommandeService) commandeService).getPointsFidelite(userId);
        if (lblSoldePoints != null) {
            lblSoldePoints.setText(String.format("Votre solde : %.2f pts (= %.2f DT)", solde, solde));
        }
        // Désactiver le radio "Points" si solde insuffisant (0 points)
        if (rbPoints != null) {
            rbPoints.setDisable(solde <= 0);
        }
    }

    private void setupTableColumns() {
        colNom.setCellValueFactory(cellData -> {
            if (cellData.getValue().getProduit() != null) {
                return new SimpleStringProperty(cellData.getValue().getProduit().getNom());
            }
            return new SimpleStringProperty("");
        });

        colNom.setCellFactory(column -> new TableCell<PanierProduit, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    PanierProduit pp = getTableView().getItems().get(getIndex());
                    tn.neuron.ardhi.models.marketplace.Produit produit = pp != null ? pp.getProduit() : null;

                    javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(4);
                    Label lblNom = new Label(item);
                    lblNom.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827; -fx-font-size: 13px;");
                    
                    String categName = (produit != null && produit.getCategorie() != null) ? produit.getCategorie() : "Catégorie";
                    Label lblBadge = new Label("🌿 " + categName);
                    lblBadge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-font-size: 10px; -fx-padding: 2 6; -fx-background-radius: 10;");
                    
                    vbox.getChildren().addAll(lblNom, lblBadge);
                    vbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    
                    javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(10);
                    hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    
                    javafx.scene.Node imageNode = null;
                    boolean imageLoaded = false;
                    
                    if (produit != null && produit.getImage() != null && !produit.getImage().isEmpty()) {
                        java.io.File file = new java.io.File(produit.getImage());
                        if (file.exists()) {
                            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(new javafx.scene.image.Image(file.toURI().toString()));
                            iv.setFitWidth(45);
                            iv.setFitHeight(45);
                            iv.setPreserveRatio(true);
                            
                            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(45, 45);
                            clip.setArcWidth(10);
                            clip.setArcHeight(10);
                            iv.setClip(clip);
                            
                            // Wrapping in a StackPane to ensure constant 45x45 dimension
                            javafx.scene.layout.StackPane sp = new javafx.scene.layout.StackPane(iv);
                            sp.setPrefSize(45, 45);
                            sp.setMinSize(45, 45);
                            sp.setMaxSize(45, 45);
                            sp.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 5;");
                            
                            imageNode = sp;
                            imageLoaded = true;
                        }
                    }
                    
                    if (!imageLoaded) {
                        javafx.scene.shape.Rectangle imgPlaceholder = new javafx.scene.shape.Rectangle(45, 45);
                        imgPlaceholder.setArcWidth(10);
                        imgPlaceholder.setArcHeight(10);
                        imgPlaceholder.setFill(javafx.scene.paint.Color.web("#e5e7eb"));
                        imageNode = imgPlaceholder;
                    }
                    
                    hbox.getChildren().addAll(imageNode, vbox);
                    setGraphic(hbox);
                    setText(null);
                }
            }
        });

        colPrix.setCellValueFactory(cellData -> {
            if (cellData.getValue().getProduit() != null) {
                return new SimpleFloatProperty(cellData.getValue().getProduit().getPrix()).asObject();
            }
            return new SimpleFloatProperty(0).asObject();
        });

        colPrix.setCellFactory(column -> new TableCell<PanierProduit, Float>() {
            @Override
            protected void updateItem(Float item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f DT", item));
                }
            }
        });

        colQuantite.setCellValueFactory(
                cellData -> new SimpleIntegerProperty(cellData.getValue().getQuantite()).asObject());

        colQuantite.setCellFactory(column -> new TableCell<PanierProduit, Integer>() {
            private final Button btnRefresh = new Button("🔄");
            private final Label lblQuantite = new Label();
            private final javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(8);

            {
                btnRefresh.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-cursor: hand; -fx-background-radius: 5; -fx-padding: 4 8; -fx-font-size: 11px;");
                lblQuantite.setStyle("-fx-border-color: #e5e7eb; -fx-border-radius: 5; -fx-padding: 4 12; -fx-background-color: white; -fx-font-weight: bold; -fx-text-fill: #111827;");
                hbox.setAlignment(javafx.geometry.Pos.CENTER);
                hbox.getChildren().addAll(lblQuantite, btnRefresh);

                btnRefresh.setOnAction(event -> {
                    PanierProduit pp = getTableView().getItems().get(getIndex());
                    modifierQuantite(pp);
                });
            }

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    lblQuantite.setText(String.valueOf(item));
                    setGraphic(hbox);
                }
            }
        });

        colSousTotal.setCellValueFactory(
                cellData -> new SimpleFloatProperty(cellData.getValue().getSousTotal()).asObject());

        colSousTotal.setCellFactory(column -> new TableCell<PanierProduit, Float>() {
            @Override
            protected void updateItem(Float item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f DT", item));
                }
            }
        });

        colActions.setCellFactory(param -> new TableCell<PanierProduit, Void>() {
            private final Button btnSupprimer = new Button("🗑️");

            {
                btnSupprimer.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 6 10; -fx-font-size: 12px;");

                btnSupprimer.setOnAction(event -> {
                    PanierProduit pp = getTableView().getItems().get(getIndex());
                    supprimerProduit(pp);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    javafx.scene.layout.HBox box = new javafx.scene.layout.HBox();
                    box.setAlignment(javafx.geometry.Pos.CENTER);
                    box.getChildren().add(btnSupprimer);
                    setGraphic(box);
                }
            }
        });
    }

    public void setUserId(int userId) {
        this.userId = userId;
        chargerPanier();
    }

    public void setMarketplaceController(CatalogueProduitController controller) {
        this.marketplaceController = controller;
    }

    private void chargerPanier() {
        panierActif = panierService.getPanierActif(userId);

        if (panierActif != null) {
            List<PanierProduit> produits = panierService.getProduitsParPanier(panierActif.getIdPanier());
            produitsPanier.clear();
            produitsPanier.addAll(produits);
            tablePanier.setItems(produitsPanier);

            updateTotaux();

            if (couponApplique != null) {
                appliquerCoupon();
            }
        }
    }

    @FXML
    private void appliquerCoupon() {
        String code = tfCodeCoupon.getText();
        if (code == null || code.trim().isEmpty()) {
            lblMessageCoupon.setText("Veuillez entrer un code.");
            lblMessageCoupon.setStyle("-fx-text-fill: red;");
            couponApplique = null;
            updateTotaux();
            return;
        }

        Coupon coupon = couponService.findByCode(code);
        if (coupon == null) {
            lblMessageCoupon.setText("Code invalide.");
            lblMessageCoupon.setStyle("-fx-text-fill: red;");
            couponApplique = null;
            updateTotaux();
            return;
        }

        float totalActuel = panierService.calculerTotal(panierActif.getIdPanier());
        String error = couponService.validerCoupon(coupon, totalActuel, userId);

        if (error != null) {
            lblMessageCoupon.setText(error);
            lblMessageCoupon.setStyle("-fx-text-fill: red;");
            couponApplique = null;
            updateTotaux();
        } else {
            lblMessageCoupon.setText("Coupon appliqué !");
            lblMessageCoupon.setStyle("-fx-text-fill: green;");
            couponApplique = coupon;
            updateTotaux();
        }
    }

    /** Appelé quand l'utilisateur change le mode de livraison */
    @FXML
    private void onModeLivraisonChange() {
        updateTotaux();
    }

    private ModeLivraison getModeLivraison() {
        return (rbLivraison != null && rbLivraison.isSelected())
                ? ModeLivraison.LIVRAISON
                : ModeLivraison.RECUPERATION;
    }

    /** Compte le nombre de vendeurs distincts dans le panier */
    private int getNombreVendeurs() {
        return (int) produitsPanier.stream()
                .filter(pp -> pp.getProduit() != null)
                .mapToInt(pp -> pp.getProduit().getIdUser())
                .distinct()
                .count();
    }

    private void updateTotaux() {
        if (panierActif != null) {
            int totalProduits = panierService.compterProduits(panierActif.getIdPanier());
            float totalMontant = panierService.calculerTotal(panierActif.getIdPanier());
            float fraisLivraison = 0f;

            if (getModeLivraison() == ModeLivraison.LIVRAISON) {
                int nbrVendeurs = getNombreVendeurs();
                fraisLivraison = 7.0f * Math.max(1, nbrVendeurs);
                boxFraisLivraison.setVisible(true);
                boxFraisLivraison.setManaged(true);
                lblFraisLivraison.setText(String.format("+%.2f DT", fraisLivraison));
                if (lblInfoLivraison != null)
                    lblInfoLivraison.setText(
                            String.format("Livraison : %d vendeur(s) × 7 DT = %.0f DT", nbrVendeurs, fraisLivraison));
            } else {
                boxFraisLivraison.setVisible(false);
                boxFraisLivraison.setManaged(false);
                if (lblInfoLivraison != null)
                    lblInfoLivraison.setText("Récupération gratuite sélectionnée.");
            }

            lblTotalProduits.setText(String.valueOf(totalProduits));
            if (lblTotalProduitsHeader != null) {
                lblTotalProduitsHeader.setText(totalProduits + " article(s)");
            }

            if (couponApplique != null) {
                double reduction = 0;
                if (couponApplique.getTypeReduction() == TypeReduction.POURCENTAGE) {
                    reduction = totalMontant * (couponApplique.getValeur() / 100.0);
                } else {
                    reduction = Math.min(couponApplique.getValeur(), totalMontant);
                }
                double montantFinal = Math.max(0, totalMontant - reduction) + fraisLivraison;
                boxReduction.setVisible(true);
                boxReduction.setManaged(true);
                lblReduction.setText(String.format("-%.2f DT", reduction));
                lblTotalMontant.setText(String.format("%.2f DT", montantFinal));
            } else {
                boxReduction.setVisible(false);
                boxReduction.setManaged(false);
                lblTotalMontant.setText(String.format("%.2f DT", totalMontant + fraisLivraison));
            }
        }
    }

    private void modifierQuantite(PanierProduit pp) {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(pp.getQuantite()));
        dialog.setTitle("Modifier la quantité");
        dialog.setHeaderText("Produit: " + pp.getProduit().getNom());
        dialog.setContentText("Nouvelle quantité:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(quantiteStr -> {
            try {
                int nouvelleQuantite = Integer.parseInt(quantiteStr);

                if (nouvelleQuantite <= 0) {
                    showAlert(Alert.AlertType.WARNING, "Quantité invalide",
                            "La quantité doit être supérieure à 0.");
                    return;
                }

                boolean success = panierService.updateQuantite(
                        panierActif.getIdPanier(),
                        pp.getIdProduit(),
                        nouvelleQuantite);

                if (success) {
                    chargerPanier();
                    rafraichirMarketplace();
                    showAlert(Alert.AlertType.INFORMATION, "Succès",
                            "Quantité mise à jour avec succès !");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur",
                            "Stock insuffisant ou erreur lors de la mise à jour.");
                }

            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Veuillez entrer un nombre valide.");
            }
        });
    }

    private void supprimerProduit(PanierProduit pp) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer le produit");
        confirmation.setContentText("Voulez-vous vraiment supprimer " + pp.getProduit().getNom() + " du panier ?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = panierService.supprimerProduit(
                    panierActif.getIdPanier(),
                    pp.getIdProduit());

            if (success) {
                chargerPanier();
                rafraichirMarketplace();
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "Produit supprimé du panier.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Impossible de supprimer le produit.");
            }
        }
    }

    @FXML
    private void viderPanier() {
        if (produitsPanier.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Panier vide",
                    "Le panier est déjà vide.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Vider le panier");
        confirmation.setContentText("Voulez-vous vraiment vider tout le panier ?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = panierService.viderPanier(panierActif.getIdPanier());

            if (success) {
                chargerPanier();
                rafraichirMarketplace();
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "Le panier a été vidé.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Impossible de vider le panier.");
            }
        }
    }

    @FXML
    private void validerCommande() {
        if (produitsPanier.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Panier vide",
                    "Votre panier est vide. Ajoutez des produits avant de valider.");
            return;
        }

        // Détecter le mode de paiement choisi
        boolean paiementParPoints = (rbPoints != null && rbPoints.isSelected());

        if (paiementParPoints) {
            validerCommandeParPoints();
        } else {
            validerCommandeParCarte();
        }
    }

    // Paiement par points de fidélité
    private void validerCommandeParPoints() {
        float sousTotal = panierService.calculerTotal(panierActif.getIdPanier());
        double totalFinal = sousTotal;
        String messageCoupon = "";

        // Appliquer coupon si présent
        if (couponApplique != null) {
            double reduction;
            if (couponApplique.getTypeReduction() == TypeReduction.POURCENTAGE) {
                reduction = sousTotal * (couponApplique.getValeur() / 100.0);
            } else {
                reduction = Math.min(couponApplique.getValeur(), sousTotal);
            }
            messageCoupon = String.format("Coupon (%s) : -%.2f DT\n", couponApplique.getCode(), reduction);
            totalFinal = Math.max(0, sousTotal - reduction);
        }

        // ✅ Ajouter les frais de livraison
        float fraisLivraison = 0f;
        String messageLivraison = "Récupération sur place (gratuit)\n";
        if (getModeLivraison() == ModeLivraison.LIVRAISON) {
            int nbrVendeurs = getNombreVendeurs();
            fraisLivraison = 7.0f * Math.max(1, nbrVendeurs);
            totalFinal += fraisLivraison;
            messageLivraison = String.format(
                    "Frais de livraison : +%.2f DT (%d vendeur(s) × 7 DT)\n", fraisLivraison, nbrVendeurs);
        }

        // Vérifier le solde de points
        double soldeActuel = ((CommandeService) commandeService).getPointsFidelite(userId);

        if (soldeActuel < totalFinal) {
            showAlert(Alert.AlertType.WARNING, "Solde insuffisant",
                    String.format("Votre solde de points (%.2f pts) est insuffisant pour couvrir le total (%.2f DT).\n"
                            + "Veuillez choisir le paiement par carte.", soldeActuel, totalFinal));
            return;
        }

        // Confirmation avec détail complet
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Paiement par points");
        confirmation.setHeaderText("Confirmer la commande");
        confirmation.setContentText(String.format(
                "Sous-total : %.2f DT\n%s%sTotal : %.2f DT\n"
                        + "Solde actuel : %.2f pts\nSolde après achat : %.2f pts\n\nConfirmer ?",
                sousTotal, messageCoupon, messageLivraison,
                totalFinal, soldeActuel, soldeActuel - totalFinal));

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK)
            return;

        // Créer les commandes
        String codeCoupon = (couponApplique != null) ? couponApplique.getCode() : null;
        List<Commande> commandes = ((CommandeService) commandeService).creerCommandeFromPanier(
                panierActif.getIdPanier(), userId, codeCoupon, getModeLivraison(), true);

        if (commandes == null || commandes.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "La commande n'a pas pu être créée.");
            return;
        }

        // Déduire les points
        double nouveauSolde = soldeActuel - totalFinal;
        ((CommandeService) commandeService).updateUserFidelityPoints(userId, nouveauSolde);
        UserSession.getInstance().getUser().setPointsFidelite(nouveauSolde);

        for (Commande c : commandes) {
            genererFacturePdfAsync(c);
        }
        panierService.viderPanier(panierActif.getIdPanier());
        chargerPanier();
        rafraichirMarketplace();
        mettreAJourAffichagePoints();

        showAlert(Alert.AlertType.INFORMATION, "Commande confirmée !",
                String.format("✅ Commande payée avec vos points !\n\n%s%sTotal débité : %.2f pts\nNouveau solde : %.2f pts",
                        messageCoupon, messageLivraison, totalFinal, nouveauSolde));
        fermerFenetrePanier();
    }

    // Ancien flux Stripe (inchangé, juste extrait dans une méthode dédiée)
    private void validerCommandeParCarte() {
        float sousTotal = panierService.calculerTotal(panierActif.getIdPanier());
        double totalFinal = sousTotal;
        String messageCoupon = "";

        if (couponApplique != null) {
            double reduction;
            if (couponApplique.getTypeReduction() == TypeReduction.POURCENTAGE) {
                reduction = sousTotal * (couponApplique.getValeur() / 100.0);
            } else {
                reduction = Math.min(couponApplique.getValeur(), sousTotal);
            }
            messageCoupon = String.format("Coupon (%s) : -%.2f DT\n",
                    couponApplique.getCode(), sousTotal - (sousTotal - reduction));
            totalFinal = Math.max(0, sousTotal - reduction);
        }

        // ✅ Ajouter les frais de livraison
        float fraisLivraison = 0f;
        String messageLivraison = "Récupération sur place (gratuit)\n";
        if (getModeLivraison() == ModeLivraison.LIVRAISON) {
            int nbrVendeurs = getNombreVendeurs();
            fraisLivraison = 7.0f * Math.max(1, nbrVendeurs);
            totalFinal += fraisLivraison;
            messageLivraison = String.format(
                    "Frais de livraison : +%.2f DT (%d vendeur(s) × 7 DT)\n", fraisLivraison, nbrVendeurs);
        }

        String resumé = String.format(
                "Sous-total : %.2f DT\n%s%sTotal à payer : %.2f DT",
                sousTotal, messageCoupon, messageLivraison, totalFinal);

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Validation de commande");
        confirmation.setHeaderText("Confirmer la commande");
        confirmation.setContentText(resumé + "\n\nVoulez-vous procéder au paiement par carte ?");
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK)
            return;

        if (stripeService == null) {
            showAlert(Alert.AlertType.ERROR, "Stripe non configuré",
                    "Éditez src/main/resources/stripe.properties et renseignez votre clé secrète Stripe.");
            return;
        }

        final String codeCoupon = (couponApplique != null) ? couponApplique.getCode() : null;
        final double montantFinalCapture = totalFinal;

        String checkoutUrl;
        try {
            checkoutUrl = stripeService.createCheckoutSession(montantFinalCapture, "Commande Ardhi");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur Stripe",
                    "Impossible de créer la session de paiement :\n" + e.getMessage());
            return;
        }

        localPaymentServer = new LocalPaymentServer();
        localPaymentServer.start(success -> {
            if (success) {
                finaliserApresPaiementCarte(codeCoupon, montantFinalCapture);
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Paiement annulé",
                        "Le paiement a été annulé. Votre panier est intact.");
            }
        });

        ouvrirWebViewPaiement(checkoutUrl);
    }

    private void ouvrirWebViewPaiement(String checkoutUrl) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/marketplace/PaymentWebView.fxml"));
            javafx.scene.Parent root = loader.load();

            PaymentWebViewController controller = loader.getController();
            controller.loadCheckoutUrl(checkoutUrl, success -> {
                if (localPaymentServer != null)
                    localPaymentServer.stop();
                showAlert(Alert.AlertType.INFORMATION, "Paiement annulé",
                        "Le paiement a été annulé. Votre panier est intact.");
            });

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Paiement sécurisé – Ardhi");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            if (localPaymentServer != null)
                localPaymentServer.stop();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir la fenêtre de paiement :\n" + e.getMessage());
        }
    }

    /**
     * Exécuté après paiement Stripe réussi.
     * ✅ MODIFIÉ : crédite 10% du totalFinal en points de fidélité.
     */
    private void finaliserApresPaiementCarte(String codeCoupon, double totalFinal) {
        List<Commande> commandes = ((CommandeService) commandeService).creerCommandeFromPanier(
                panierActif.getIdPanier(), userId, codeCoupon, getModeLivraison(), false);
        if (commandes == null || commandes.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur de validation",
                    "Le paiement a été effectué mais la commande n'a pas pu être créée.\n" +
                            "Veuillez contacter le support en mentionnant votre paiement Stripe.");
            return;
        }

        // Créditer 10% du totalFinal en points de fidélité
        double soldeActuel = ((CommandeService) commandeService).getPointsFidelite(userId);
        double pointsGagnes = 0.1 * totalFinal;
        double nouveauSolde = soldeActuel + pointsGagnes;
        ((CommandeService) commandeService).updateUserFidelityPoints(userId, nouveauSolde);

        // Mettre à jour l'objet User en session
        UserSession.getInstance().getUser().setPointsFidelite(nouveauSolde);

        // Générer PDF
        for (Commande c : commandes) {
            genererFacturePdfAsync(c);
        }

        boolean panierVide = panierService.viderPanier(panierActif.getIdPanier());
        chargerPanier();
        rafraichirMarketplace();
        mettreAJourAffichagePoints(); // ✅ Rafraîchir l'affichage du solde

        StringBuilder detailsCommandes = new StringBuilder();
        double fraisTotalLivraison = 0;
        for (Commande c : commandes) {
            fraisTotalLivraison += c.getFraisLivraison();
            detailsCommandes.append(String.format("- Commande #%d : %.2f DT", c.getIdCommande(), c.getTotal()));
            if (c.getFraisLivraison() > 0) {
                detailsCommandes.append(String.format(" (dont %.2f DT livraison)", c.getFraisLivraison()));
            }
            detailsCommandes.append("\n");
        }

        String modeLivraisonLabel = (getModeLivraison() == ModeLivraison.LIVRAISON)
                ? "🚚 Livraison à domicile (" + String.format("%.2f DT", fraisTotalLivraison) + " frais inclus)"
                : "🏪 Récupération sur place (gratuit)";

        String messageFinal;
        if (commandes.size() == 1) {
            messageFinal = String.format(
                    "✅ Paiement confirmé !\n\n" +
                            "Votre commande a bien été enregistrée :\n\n%s\n" +
                            "%s\n\n" +
                            "🎁 Vous avez gagné %.2f points de fidélité !\n" +
                            "Nouveau solde : %.2f pts\n\n" +
                            "Vous recevrez une confirmation par email.",
                    detailsCommandes, modeLivraisonLabel, pointsGagnes, nouveauSolde);
        } else {
            messageFinal = String.format(
                    "✅ Paiement confirmé !\n\n" +
                            "Votre panier a été réparti en %d commande(s) distincte(s) :\n\n%s\n" +
                            "%s\n\n" +
                            "🎁 Vous avez gagné %.2f points de fidélité !\n" +
                            "Nouveau solde : %.2f pts\n\n" +
                            "Vous recevrez une confirmation pour chaque commande.",
                    commandes.size(), detailsCommandes, modeLivraisonLabel, pointsGagnes, nouveauSolde);
        }

        if (!panierVide) {
            messageFinal += "\n\n⚠️ Le panier n'a pas pu être vidé automatiquement.";
        }


        showAlert(Alert.AlertType.INFORMATION, "Commande confirmée !", messageFinal);
        fermerFenetrePanier();
    }

    // Méthode conservée pour compatibilité (ancien nom appelé depuis
    // LocalPaymentServer)
    private void finaliserApresPaiement(String codeCoupon) {
        float sousTotal = panierService.calculerTotal(panierActif.getIdPanier());
        finaliserApresPaiementCarte(codeCoupon, sousTotal);
    }

    private void genererFacturePdfAsync(Commande commande) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                List<DetailsCommande> details = commandeService.getDetailsByCommande(commande.getIdCommande());
                PdfCommande.genererPdf(commande, details);
                return null;
            }
        };

        task.setOnFailed(evt -> {
            Throwable ex = task.getException();
            ex.printStackTrace();
            showAlert(Alert.AlertType.WARNING, "PDF non généré",
                    "La commande a été validée, mais la génération du PDF a échoué.");
        });

        task.setOnSucceeded(evt -> {
            String dest = "factures/facture_" + commande.getIdCommande() + ".pdf";
            showAlert(Alert.AlertType.INFORMATION, "Facture générée",
                    "Votre facture a été générée : " + dest);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void fermerFenetrePanier() {
        if (btnValiderCommande == null || btnValiderCommande.getScene() == null)
            return;
        Stage stage = (Stage) btnValiderCommande.getScene().getWindow();
        if (stage != null)
            stage.close();
    }

    @FXML
    private void continuerAchats() {
        Stage stage = (Stage) btnContinuerAchats.getScene().getWindow();
        stage.close();
    }

    private void rafraichirMarketplace() {
        if (marketplaceController != null) {
            marketplaceController.rafraichirAffichagePanier();
            marketplaceController.rafraichirProduits();
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