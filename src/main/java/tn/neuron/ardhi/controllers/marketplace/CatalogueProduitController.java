package tn.neuron.ardhi.controllers.marketplace;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import tn.neuron.ardhi.services.marketplace.CommandeService;

import tn.neuron.ardhi.services.marketplace.ChatbotService;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.neuron.ardhi.interfaces.marketplace.IPanierService;
import tn.neuron.ardhi.interfaces.marketplace.IProduitService;
import tn.neuron.ardhi.models.marketplace.Panier;
import tn.neuron.ardhi.models.marketplace.Produit;
import tn.neuron.ardhi.models.marketplace.TypeReduction;
import tn.neuron.ardhi.models.UserAndDiag.Role;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.services.marketplace.PanierService;
import tn.neuron.ardhi.services.marketplace.ProduitService;
import tn.neuron.ardhi.services.UserAndDiag.UserService;
import tn.neuron.ardhi.services.marketplace.AvisService;
import tn.neuron.ardhi.interfaces.marketplace.IAvisService;
import tn.neuron.ardhi.services.marketplace.WishlistService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;
import tn.neuron.ardhi.models.marketplace.ChatIntent;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import tn.neuron.ardhi.services.marketplace.SpeechToTextService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import tn.neuron.ardhi.utils.marketplace.CurrencyService;

public class CatalogueProduitController implements Initializable {

    // ── FXML fields ──────────────────────────────────────────────────────────
    // ── Chatbot intégré ───────────────────────────────────────────────────────
    @FXML
    private VBox chatbotPanel;
    @FXML
    private VBox chatMessagesBox;
    @FXML
    private ScrollPane chatScrollPane;
    @FXML
    private TextArea chatInputField;
    @FXML
    private Button chatSendButton;
    @FXML
    private HBox chatLoadingBox;
    @FXML
    private Button btnToggleChatbot;

    private ChatbotService chatbotService;
    private boolean chatbotInitialized = false;
    private List<String> historiqueMessages = new ArrayList<>();
    private int indexHistorique = -1;
    @FXML
    private TextField searchField;
    @FXML
    private Slider sliderPrixMin;
    @FXML
    private Slider sliderPrixMax;
    @FXML
    private Label lblPrixMin;
    @FXML
    private Label lblPrixMax;

    // ── Comparaison ───────────────────────────────────────────────────────────
    @FXML private HBox   barreComparaison;
    @FXML private HBox   comparaisonProduitsBox;
    @FXML private Label  lblComparaisonCount;
    @FXML private Button btnLancerComparaison;        // bouton sidebar
    @FXML private Button btnLancerComparaisonBarre;   // bouton barre flottante
    private final List<Produit> produitsAComparer = new ArrayList<>();
    private static final int MAX_COMPARAISON = 2;

    private double prixMinFiltre = 0;
    private double prixMaxFiltre = 500;
    @FXML
    private ComboBox<String> categorieComboBox;
    @FXML
    private ComboBox<String> deviseComboBox;
    @FXML
    private ComboBox<String> sortComboBox;
    @FXML
    private CheckBox checkSolde;
    @FXML
    private Button btnRechercher;
    @FXML
    private Button btnVoirPanier;
    @FXML
    private GridPane produitsGrid;
    @FXML
    private Label panierCountLabel;
    @FXML
    private Label panierTotalLabel;
    @FXML
    private Label lblNombreProduits;

    private String deviseSelectionnee = "DT";

    @FXML
    private Label lblPointsFideliteNavbar;

    // vcl
    @FXML
    private Button btnMicrophone;
    @FXML
    private HBox recordingIndicatorBox;
    @FXML
    private Label recordingDot;

    private SpeechToTextService speechToTextService;
    private Timeline blinkTimeline; // pour l'animation clignotante
    // ── Services ─────────────────────────────────────────────────────────────
    private IProduitService produitService;
    private IPanierService panierService;
    private UserService userService;
    private IAvisService avisService;
    private WishlistService wishlistService;

    // ── Palette (design system) ───────────────────────────────────────────────
    private static final String GREEN_DARK = "#2b5329";
    private static final String GREEN_MAIN = "#4a7c47";
    private static final String ORANGE = "#e05c1a";
    private static final String RED_BADGE = "#ef4444";
    private static final String STAR_GOLD = "#f59e0b";
    private static final String GRAY_BG = "#f9fafb";
    private static final String GRAY_TEXT = "#6b7280";
    private static final String BORDER = "#e5e7eb";

    // ── Initialize ───────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        wishlistService = new WishlistService();
        updatePointsFideliteDisplay();

        if (UserSession.getInstance() == null || UserSession.getInstance().getUser() == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur de session", "Aucun utilisateur connecté.");
            return;
        }

        produitService = new ProduitService();
        panierService = new PanierService();
        userService = new UserService();
        avisService = new AvisService();

        initializeCategorieComboBox();
        initializeSortComboBox();
        initializeDeviseComboBox();
        chargerProduits(null);
        updatePanierDisplay();
        setupEventHandlers();
        initBarreComparaison();
        // Init chatbot service
        chatbotService = new ChatbotService();

        // Ctrl+Entrée pour envoyer dans le chatbot intégré
        chatInputField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && event.isControlDown()) {
                // Ctrl+Entrée → envoyer
                envoyerMessageChat();
                event.consume();

            } else if (event.getCode() == KeyCode.UP) {
                // Flèche HAUT → message précédent
                if (!historiqueMessages.isEmpty() && indexHistorique > 0) {
                    indexHistorique--;
                    chatInputField.setText(historiqueMessages.get(indexHistorique));
                    chatInputField.positionCaret(chatInputField.getText().length());
                }
                event.consume();

            } else if (event.getCode() == KeyCode.DOWN) {
                // Flèche BAS → message suivant ou vider
                if (!historiqueMessages.isEmpty()) {
                    if (indexHistorique < historiqueMessages.size() - 1) {
                        indexHistorique++;
                        chatInputField.setText(historiqueMessages.get(indexHistorique));
                        chatInputField.positionCaret(chatInputField.getText().length());
                    } else {
                        // Au bout de l'historique → vider le champ
                        indexHistorique = historiqueMessages.size();
                        chatInputField.clear();
                    }
                }
                event.consume();
            }
        });

        // Auto-scroll chatbot
        chatMessagesBox.heightProperty().addListener((obs, o, n) -> chatScrollPane.setVvalue(1.0));
    }



    // ── ComboBox initialisations ──────────────────────────────────────────────
    private void initializeCategorieComboBox() {
        categorieComboBox.getItems().add("Toutes");
        categorieComboBox.getItems().addAll(produitService.getAllCategories());
        categorieComboBox.setValue("Toutes");
    }

    private void initializeSortComboBox() {
        sortComboBox.setItems(FXCollections.observableArrayList(
                "Défaut",
                "Prix croissant",
                "Prix décroissant",
                "Nom A → Z",
                "Nom Z → A",
                "Meilleures notes",
                "En solde en premier"));
        sortComboBox.setValue("Défaut");
        sortComboBox.setOnAction(e -> appliquerFiltresEtTri());
    }

    private void initializeDeviseComboBox() {
        deviseComboBox.getItems().addAll("DT", "EUR", "USD");
        deviseComboBox.setValue("DT");
        deviseComboBox.setOnAction(e -> {
            deviseSelectionnee = deviseComboBox.getValue();
            appliquerFiltresEtTri();
        });
    }

    // ── Event handlers ────────────────────────────────────────────────────────
    private void setupEventHandlers() {
        categorieComboBox.setOnAction(event -> appliquerFiltresEtTri());
        if (btnRechercher != null)
            btnRechercher.setOnAction(event -> handleSearch());
        searchField.setOnAction(event -> handleSearch());
        if (btnVoirPanier != null)
            btnVoirPanier.setOnAction(event -> ouvrirPanier());
        if (checkSolde != null)
            checkSolde.setOnAction(event -> appliquerFiltresEtTri());
        initializePrixSliders();
    }

    private void initializePrixSliders() {
        if (sliderPrixMin == null || sliderPrixMax == null)
            return;

        // Calculer max réel depuis les produits
        double maxPrix = produitService.getAllProduits().stream()
                .mapToDouble(p -> (double) p.getPrixApresRemise())
                .max().orElse(500);
        double roundedMax = Math.ceil(maxPrix / 50) * 50;

        sliderPrixMin.setMax(roundedMax);
        sliderPrixMax.setMax(roundedMax);
        sliderPrixMax.setValue(roundedMax);
        prixMaxFiltre = roundedMax;

        updatePrixLabels();

        sliderPrixMin.valueProperty().addListener((obs, oldVal, newVal) -> {
            double min = newVal.doubleValue();
            double max = sliderPrixMax.getValue();
            // Empêcher min > max
            if (min > max) {
                sliderPrixMin.setValue(max);
                return;
            }
            prixMinFiltre = min;
            updatePrixLabels();
            appliquerFiltresEtTri();
        });

        sliderPrixMax.valueProperty().addListener((obs, oldVal, newVal) -> {
            double max = newVal.doubleValue();
            double min = sliderPrixMin.getValue();
            // Empêcher max < min
            if (max < min) {
                sliderPrixMax.setValue(min);
                return;
            }
            prixMaxFiltre = max;
            updatePrixLabels();
            appliquerFiltresEtTri();
        });
    }

    private void updatePrixLabels() {
        if (lblPrixMin != null)
            lblPrixMin.setText(String.format("%.0f DT", prixMinFiltre));
        if (lblPrixMax != null)
            lblPrixMax.setText(String.format("%.0f DT", prixMaxFiltre));
    }


    // ══════════════════════════════════════════════════════════════════════════
// COMPARAISON
// ══════════════════════════════════════════════════════════════════════════

    private void initBarreComparaison() {
        if (barreComparaison != null) {
            barreComparaison.setVisible(false);
            barreComparaison.setManaged(false);
        }
    }

    private void toggleComparaison(Produit produit, Button btnComparer) {
        boolean estDeja = produitsAComparer.stream()
                .anyMatch(p -> p.getIdProduit() == produit.getIdProduit());

        if (estDeja) {
            produitsAComparer.removeIf(p -> p.getIdProduit() == produit.getIdProduit());
            appliquerStyleBtnComparaison(btnComparer, false);
        } else {
            if (produitsAComparer.size() >= MAX_COMPARAISON) {
                showAlert(Alert.AlertType.WARNING, "Comparaison",
                        "Maximum " + MAX_COMPARAISON + " produits.");
                return;
            }
            produitsAComparer.add(produit);
            appliquerStyleBtnComparaison(btnComparer, true);
        }
        mettreAJourBarreComparaison();
    }

    private void appliquerStyleBtnComparaison(Button btn, boolean actif) {
        if (actif) {
            btn.setText("⚖️  Comparé");
            btn.setStyle("-fx-background-color: #046436; -fx-text-fill: white; -fx-font-size: 11px;" +
                    "-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;" +
                    "-fx-border-color: #034e2b; -fx-border-radius: 10; -fx-border-width: 1;");
        } else {
            btn.setText("⚖️  Comparer");
            btn.setStyle("-fx-background-color: #f0f3f0; -fx-text-fill: #4a6a58; -fx-font-size: 11px;" +
                    "-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;" +
                    "-fx-border-color: #d8e4da; -fx-border-radius: 10; -fx-border-width: 1;");
        }
    }

    private void mettreAJourBarreComparaison() {
        int nb = produitsAComparer.size();
        boolean peutComparer = nb >= 2;

        // Barre flottante
        if (barreComparaison != null) {
            barreComparaison.setVisible(nb > 0);
            barreComparaison.setManaged(nb > 0);
        }

        // Label count
        if (lblComparaisonCount != null)
            lblComparaisonCount.setText(nb + " produit" + (nb > 1 ? "s" : "") + " sélectionné" + (nb > 1 ? "s" : ""));

        // Chips dans la barre
        if (comparaisonProduitsBox != null) {
            comparaisonProduitsBox.getChildren().clear();
            for (Produit p : produitsAComparer)
                comparaisonProduitsBox.getChildren().add(creerChipComparaison(p));
        }

        // Bouton sidebar
        if (btnLancerComparaison != null) {
            btnLancerComparaison.setDisable(!peutComparer);
            btnLancerComparaison.setText("⚖️  Comparer (" + nb + ")");
            btnLancerComparaison.setStyle(peutComparer
                    ? "-fx-background-color: #1a5faa; -fx-text-fill: white; -fx-font-size: 12px;" +
                    "-fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;" +
                    "-fx-border-color: #0e3d7a; -fx-border-radius: 12; -fx-border-width: 1.5;"
                    : "-fx-background-color: #e8f0fe; -fx-text-fill: #1a5faa; -fx-font-size: 12px;" +
                    "-fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;" +
                    "-fx-border-color: #b8d0f0; -fx-border-radius: 12; -fx-border-width: 1.5;");
        }

        // Bouton barre flottante
        if (btnLancerComparaisonBarre != null) {
            btnLancerComparaisonBarre.setDisable(!peutComparer);
            btnLancerComparaisonBarre.setStyle(peutComparer
                    ? "-fx-background-color: #7fffc0; -fx-text-fill: #0d1f15; -fx-font-size: 13px;" +
                    "-fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;" +
                    "-fx-effect: dropshadow(gaussian, rgba(127,255,192,0.4), 10, 0, 0, 3); -fx-padding: 10 24;"
                    : "-fx-background-color: #3a5a48; -fx-text-fill: #6a8a78; -fx-font-size: 13px;" +
                    "-fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: default; -fx-padding: 10 24;");
        }
    }

    private HBox creerChipComparaison(Produit p) {
        HBox chip = new HBox(6);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setStyle("-fx-background-color: #1a3525; -fx-background-radius: 20;" +
                "-fx-border-color: #2e4838; -fx-border-radius: 20; -fx-border-width: 1; -fx-padding: 6 10 6 12;");

        Label nomLbl = new Label(p.getNom().length() > 18 ? p.getNom().substring(0, 18) + "…" : p.getNom());
        nomLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #7fffc0;");

        Label prixLbl = new Label(String.format("%.2f DT", p.getPrixApresRemise()));
        prixLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #4a8a68;");

        Button removeBtn = new Button("✕");
        String rmBase = "-fx-background-color: transparent; -fx-text-fill: #6a8a78; -fx-font-size: 10px;" +
                "-fx-cursor: hand; -fx-border-width: 0; -fx-padding: 0 0 0 4;";
        removeBtn.setStyle(rmBase);
        removeBtn.setOnMouseEntered(e -> removeBtn.setStyle(rmBase.replace("#6a8a78", "#e74c3c")));
        removeBtn.setOnMouseExited(e  -> removeBtn.setStyle(rmBase));
        removeBtn.setOnAction(e -> {
            produitsAComparer.removeIf(x -> x.getIdProduit() == p.getIdProduit());
            mettreAJourBarreComparaison();
            appliquerFiltresEtTri();
        });

        chip.getChildren().addAll(nomLbl, prixLbl, removeBtn);
        return chip;
    }

    @FXML
    private void lancerComparaison() {
        if (produitsAComparer.size() < 2) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/marketplace/ComparaisonProduits.fxml"));
            Parent root = loader.load();
            ComparaisonProduitsController ctrl = loader.getController();
            ctrl.setCatalogueController(this);
            ctrl.chargerComparaison(new ArrayList<>(produitsAComparer));
            Stage stage = (Stage) produitsGrid.getScene().getWindow();
            ctrl.setOwnerStage(stage);
            stage.setTitle("Ardhi - Comparaison");
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la comparaison : " + e.getMessage());
        }
    }

    @FXML
    private void viderComparaison() {
        produitsAComparer.clear();
        mettreAJourBarreComparaison();
        appliquerFiltresEtTri();
    }



    @FXML
    private void handleSearch() {
        appliquerFiltresEtTri();
    }

    @FXML
    private void handleCategorieChange() {
        appliquerFiltresEtTri();
    }

    private void updatePointsFideliteDisplay() {
        if (lblPointsFideliteNavbar == null)
            return;
        User currentUser = UserSession.getInstance() != null ? UserSession.getInstance().getUser() : null;
        if (currentUser == null)
            return;

        double solde = new CommandeService().getPointsFidelite(currentUser.getId());

        // Mettre à jour aussi l'objet User en session pour cohérence
        currentUser.setPointsFidelite(solde);

        lblPointsFideliteNavbar.setText(String.format("%.2f pts", solde));
    }

    // ── Filtrage + tri centralisé ─────────────────────────────────────────────
    private void appliquerFiltresEtTri() {
        String categorie = categorieComboBox.getValue();
        String search = searchField != null ? searchField.getText().trim() : "";
        boolean soldeOnly = checkSolde != null && checkSolde.isSelected();

        List<Produit> produits;
        if (categorie == null || "Toutes".equals(categorie)) {
            produits = produitService.getAllProduits();
        } else {
            produits = produitService.getProduitsByCategorie(categorie);
        }

        // Filtre rôle
        produits = filtrerProduitsPourRole(produits);

        // Filtre recherche
        if (!search.isEmpty()) {
            String q = search.toLowerCase();
            produits = produits.stream()
                    .filter(p -> p.getNom().toLowerCase().contains(q)
                            || (p.getCategorie() != null && p.getCategorie().toLowerCase().contains(q)))
                    .collect(java.util.stream.Collectors.toList());
        }

        // Filtre "en solde"
        if (soldeOnly) {
            produits = produits.stream().filter(Produit::aUneRemise)
                    .collect(java.util.stream.Collectors.toList());
        }

        // Filtre prix
        final double prixMin = prixMinFiltre;
        final double prixMax = prixMaxFiltre;
        produits = produits.stream()
                .filter(p -> p.getPrixApresRemise() >= prixMin && p.getPrixApresRemise() <= prixMax)
                .collect(java.util.stream.Collectors.toList());

        // Tri
        String tri = sortComboBox != null ? sortComboBox.getValue() : "Défaut";
        if (tri != null) {
            switch (tri) {
                case "Prix croissant":
                    produits.sort(Comparator.comparingDouble(p -> p.getPrixApresRemise()));
                    break;
                case "Prix décroissant":
                    produits.sort((a, b) -> Float.compare(b.getPrixApresRemise(), a.getPrixApresRemise()));
                    break;
                case "Nom A → Z":
                    produits.sort(Comparator.comparing(Produit::getNom));
                    break;
                case "Nom Z → A":
                    produits.sort(Comparator.comparing(Produit::getNom).reversed());
                    break;
                case "Meilleures notes":
                    produits.sort((a, b) -> Double.compare(
                            avisService.getNoteMoyenne(b.getIdProduit()),
                            avisService.getNoteMoyenne(a.getIdProduit())));
                    break;
                case "En solde en premier":
                    produits.sort((a, b) -> Boolean.compare(!a.aUneRemise(), !b.aUneRemise()));
                    break;
            }
        }

        afficherProduits(produits);
    }

    private void chargerProduits(String categorie) {
        List<Produit> produits = (categorie == null)
                ? produitService.getAllProduits()
                : produitService.getProduitsByCategorie(categorie);
        produits = filtrerProduitsPourRole(produits);
        afficherProduits(produits);
    }

    // ── Affichage grille ─────────────────────────────────────────────────────
    private void afficherProduits(List<Produit> produits) {
        produitsGrid.getChildren().clear();

        if (lblNombreProduits != null) {
            lblNombreProduits.setText(produits.size() + " produit(s) trouvé(s)");
        }

        if (produits.isEmpty()) {
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(60));
            Label icon = new Label("🌾");
            icon.setStyle("-fx-font-size: 52px;");
            Label msg = new Label("Aucun produit disponible");
            msg.setStyle("-fx-font-size: 18px; -fx-text-fill: #8aaa98; -fx-font-weight: bold;");
            Label sub = new Label("Essayez de modifier vos filtres");
            sub.setStyle("-fx-font-size: 13px; -fx-text-fill: #aab8b0;");
            emptyBox.getChildren().addAll(icon, msg, sub);
            produitsGrid.add(emptyBox, 0, 0);
            return;
        }

        final int COLS = 4;
        int col = 0, row = 0;

        for (int i = 0; i < produits.size(); i++) {
            Produit produit = produits.get(i);
            VBox card = creerProduitCard(produit);

            // Fade-in animate each card
            card.setOpacity(0);
            produitsGrid.add(card, col, row);

            FadeTransition ft = new FadeTransition(Duration.millis(250 + i * 30L), card);
            ft.setFromValue(0);
            ft.setToValue(1);

            TranslateTransition tt = new TranslateTransition(Duration.millis(250 + i * 30L), card);
            tt.setFromY(12);
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

    // ── Premium Card Builder ──────────────────────────────────────────────────
    private VBox creerProduitCard(Produit produit) {
        boolean enStock = produit.getQuantiteStock() > 0;

        // ── Carte principale ─────────────────────────────────────────────────
        VBox card = new VBox(0);
        card.setPrefWidth(295);
        card.setMaxWidth(295);
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle(cardStyle(false));

        card.setOnMouseEntered(e -> card.setStyle(cardStyle(true)));
        card.setOnMouseExited(e -> card.setStyle(cardStyle(false)));

        // ── IMAGE ZONE ───────────────────────────────────────────────────────
        StackPane imgZone = new StackPane();
        imgZone.setPrefHeight(200);
        imgZone.setMinHeight(200);
        imgZone.setMaxHeight(200);
        imgZone.setStyle(
                "-fx-background-color: #f3f4f6;" +
                        "-fx-background-radius: 11 11 0 0;");

        // Image produit
        if (produit.getImage() != null && !produit.getImage().isEmpty()) {
            File file = new File(produit.getImage());
            if (file.exists()) {
                ImageView iv = new ImageView(new Image(file.toURI().toString()));
                iv.setFitWidth(295);
                iv.setFitHeight(200);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
                imgZone.getChildren().add(iv);
            } else {
                imgZone.getChildren().add(makePlaceholder());
            }
        } else {
            imgZone.getChildren().add(makePlaceholder());
        }

        // Overlay gradient (bottom)
        Label overlay = new Label();
        overlay.setMaxWidth(Double.MAX_VALUE);
        overlay.setPrefHeight(60);
        overlay.setStyle(
                "-fx-background-color: linear-gradient(to top, rgba(0,0,0,0.35), transparent);" +
                        "-fx-background-radius: 0 0 0 0;");
        StackPane.setAlignment(overlay, Pos.BOTTOM_RIGHT);

        // Badge favori
        Button favBtn = makeFavBtn(produit);
        StackPane.setAlignment(favBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(favBtn, new Insets(10, 10, 0, 0));

        // Badge catégorie
        Label categBadge = new Label("🌿  " + produit.getCategorie());
        categBadge.setStyle(
                "-fx-background-color: rgba(255,255,255,0.93);" +
                        "-fx-text-fill: " + GREEN_MAIN + ";" +
                        "-fx-font-size: 10px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 20; -fx-padding: 4 10 4 10;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 4, 0, 0, 1);");
        StackPane.setAlignment(categBadge, Pos.BOTTOM_LEFT);
        StackPane.setMargin(categBadge, new Insets(0, 0, 10, 10));

        // Badge stock
        Label stockBadge = new Label(enStock ? "✓ En stock" : "✗ Rupture");
        stockBadge.setStyle(
                "-fx-background-color: " + (enStock ? "#dcfce7" : "#fee2e2") + ";" +
                        "-fx-text-fill: " + (enStock ? "#166534" : "#991b1b") + ";" +
                        "-fx-font-size: 10px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 20; -fx-padding: 4 10 4 10;");
        StackPane.setAlignment(stockBadge, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(stockBadge, new Insets(0, 10, 10, 0));

        imgZone.getChildren().addAll(overlay, favBtn, categBadge, stockBadge);

        // Badge SOLDE (transparent pill, haut gauche)
        if (produit.aUneRemise()) {
            String badgeText;
            if (produit.getTypeRemise() == TypeReduction.POURCENTAGE) {
                badgeText = "SOLDE  -" + (int) produit.getRemise() + "%";
            } else {
                badgeText = "SOLDE  -" + String.format("%.2f", produit.getRemise()) + " DT";
            }
            Label soldeBadge = new Label(badgeText);
            soldeBadge.setStyle(
                    "-fx-background-color: " + RED_BADGE + ";" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 10px; -fx-font-weight: bold;" +
                            "-fx-background-radius: 0 0 11 0;" +
                            "-fx-padding: 5 12 5 12;");
            StackPane.setAlignment(soldeBadge, Pos.TOP_LEFT);
            imgZone.getChildren().add(soldeBadge);
        }

        // ── CONTENU ──────────────────────────────────────────────────────────
        VBox content = new VBox(10);
        content.setPadding(new Insets(16, 18, 18, 18));

        // Nom
        Label nomLabel = new Label(produit.getNom());
        nomLabel.setStyle(
                "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + GREEN_DARK + ";");
        nomLabel.setWrapText(true);
        nomLabel.setMaxWidth(259);

        // Vendeur
        String vendeurText = getNomVendeur(produit);
        HBox vendeurRow = new HBox(5);
        vendeurRow.setAlignment(Pos.CENTER_LEFT);
        Label pinIcon = new Label("📍");
        pinIcon.setStyle("-fx-font-size: 11px;");
        Label vendeurLabel = new Label(vendeurText);
        vendeurLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + GRAY_TEXT + ";");
        vendeurRow.getChildren().addAll(pinIcon, vendeurLabel);

        // Chips: stock + unité
        HBox chipsRow = new HBox(8);
        chipsRow.setAlignment(Pos.CENTER_LEFT);
        Label chipQte = makeChip("📦  " + produit.getQuantiteStock()
                + (produit.getUniteMesure() != null ? " " + produit.getUniteMesure() : " unités"));
        Label chipDesc = makeChip("🏷  " + (produit.getDescription() != null && produit.getDescription().length() > 14
                ? produit.getDescription().substring(0, 14) + "…"
                : (produit.getDescription() != null ? produit.getDescription() : "—")));
        chipsRow.getChildren().addAll(chipQte, chipDesc);

        // Séparateur
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #eef2ef;");

        // ── Bloc prix ────────────────────────────────────────────────────────
        VBox prixBlock = new VBox(2);
        prixBlock.setAlignment(Pos.CENTER_LEFT);
        HBox prixRow = new HBox(0);
        prixRow.setAlignment(Pos.CENTER_LEFT);

        // Calcul prix - les produits sont toujours en DT
        float prixBase, prixFinal;
        String unite = produit.getUniteMesure() != null ? "/" + produit.getUniteMesure() : "";
        String deviseSuffix;
        if ("DT".equals(deviseSelectionnee) || deviseSelectionnee == null) {
            prixBase = produit.getPrix();
            prixFinal = produit.getPrixApresRemise();
            deviseSuffix = " DT" + unite;
        } else {
            // Conversion depuis DT vers EUR ou USD
            prixBase = (float) CurrencyService.getInstance().convert(produit.getPrix(), "TND", deviseSelectionnee);
            prixFinal = (float) CurrencyService.getInstance().convert(produit.getPrixApresRemise(), "TND",
                    deviseSelectionnee);
            deviseSuffix = " " + getSymboleDevise(deviseSelectionnee) + unite;
        }

        if (produit.aUneRemise()) {
            Label prixBarreLabel = new Label(String.format("%.2f", prixBase) + deviseSuffix);
            prixBarreLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #bbb; -fx-strikethrough: true;");
            prixBlock.getChildren().add(prixBarreLabel);
        }

        Label prixLabel = new Label(String.format("%.2f", prixFinal));
        String prixColor = produit.aUneRemise() ? RED_BADGE : GREEN_DARK;
        prixLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + prixColor + ";");

        Label deviseLabel = new Label(deviseSuffix);
        deviseLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + GRAY_TEXT + "; -fx-padding: 0 0 5 2;");
        deviseLabel.setAlignment(Pos.BOTTOM_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Rating
        double moyenne = avisService.getNoteMoyenne(produit.getIdProduit());
        int nbAvis = avisService.getNombreAvis(produit.getIdProduit());

        VBox ratingBox = new VBox(2);
        ratingBox.setAlignment(Pos.CENTER_RIGHT);

        HBox starsRow = new HBox(2);
        starsRow.setAlignment(Pos.CENTER_RIGHT);
        for (int i = 0; i < 5; i++) {
            Label star = new Label(i < (int) Math.round(moyenne) ? "★" : "☆");
            star.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (i < (int) Math.round(moyenne) ? STAR_GOLD : "#ccc")
                    + ";");
            starsRow.getChildren().add(star);
        }

        Button btnAvis = new Button(nbAvis + " avis");
        btnAvis.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #3498db;" +
                        "-fx-font-size: 10px; -fx-underline: true; -fx-cursor: hand; -fx-padding: 0; -fx-border-width: 0;");
        btnAvis.setOnAction(e -> ouvrirFenetreAvis(produit));
        ratingBox.getChildren().addAll(starsRow, btnAvis);

        prixRow.getChildren().addAll(prixLabel, deviseLabel, spacer, ratingBox);
        prixBlock.getChildren().add(prixRow);

        // ── Spinner quantité ─────────────────────────────────────────────────
        HBox spinnerRow = new HBox(10);
        spinnerRow.setAlignment(Pos.CENTER_LEFT);

        Label qteLabel = new Label("Quantité");
        qteLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + GRAY_TEXT + "; -fx-font-weight: bold;");

        int maxQte = Math.max(1, produit.getQuantiteStock());
        Spinner<Integer> quantiteSpinner = new Spinner<>(1, maxQte, 1);
        quantiteSpinner.setPrefWidth(100);
        quantiteSpinner.setPrefHeight(34);
        quantiteSpinner.setEditable(true);
        quantiteSpinner.setStyle(
                "-fx-background-color: " + GRAY_BG + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 8;" +
                        "-fx-border-width: 1; -fx-font-size: 13px;");
        spinnerRow.getChildren().addAll(qteLabel, quantiteSpinner);

        // ── Bouton Ajouter ───────────────────────────────────────────────────
        Button ajouterBtn = new Button(enStock ? "🛒   Ajouter au panier" : "Indisponible");
        ajouterBtn.setPrefWidth(259);
        ajouterBtn.setPrefHeight(44);
        ajouterBtn.setDisable(!enStock);
        ajouterBtn.setStyle(enStock
                ? btnAddStyle(false)
                : "-fx-background-color: #d8e0d9; -fx-text-fill: #aaa; -fx-font-size: 13px;" +
                        "-fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: default;");
        if (enStock) {
            ajouterBtn.setOnMouseEntered(e -> ajouterBtn.setStyle(btnAddStyle(true)));
            ajouterBtn.setOnMouseExited(e -> ajouterBtn.setStyle(btnAddStyle(false)));
            ajouterBtn.setOnAction(e -> ajouterAuPanier(produit, quantiteSpinner.getValue()));
        }

        // ── Bouton Voir Détail ────────────────────────────────────────────────
        Button detailBtn = new Button("🔍   Voir le détail  →");
        detailBtn.setPrefWidth(259);
        detailBtn.setPrefHeight(38);
        detailBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: " + GREEN_MAIN + ";" +
                        "-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;" +
                        "-fx-border-color: " + BORDER + "; -fx-border-radius: 12; -fx-border-width: 1;");
        detailBtn.setOnMouseEntered(e -> detailBtn.setStyle(
                "-fx-background-color: #e8f5ec; -fx-text-fill: " + GREEN_MAIN + ";" +
                        "-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;" +
                        "-fx-border-color: #b8dfc8; -fx-border-radius: 12; -fx-border-width: 1;"));
        detailBtn.setOnMouseExited(e -> detailBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: " + GREEN_MAIN + ";" +
                        "-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;" +
                        "-fx-border-color: " + BORDER + "; -fx-border-radius: 12; -fx-border-width: 1;"));
        detailBtn.setOnAction(e -> ouvrirDetailProduit(produit));
// Bouton Comparer
        boolean dejaSelectionne = produitsAComparer.stream()
                .anyMatch(p -> p.getIdProduit() == produit.getIdProduit());
        Button btnComparer = new Button();
        appliquerStyleBtnComparaison(btnComparer, dejaSelectionne);
        btnComparer.setPrefWidth(259);
        btnComparer.setPrefHeight(34);
        btnComparer.setOnAction(e -> toggleComparaison(produit, btnComparer));
        content.getChildren().addAll(nomLabel, vendeurRow, chipsRow, sep,
                prixBlock, spinnerRow, ajouterBtn, btnComparer, detailBtn);
        card.getChildren().addAll(imgZone, content);

        return card;
    }

    // ── Style helpers ─────────────────────────────────────────────────────────
    private String cardStyle(boolean hovered) {
        if (hovered) {
            return "-fx-background-color: white;" +
                    "-fx-background-radius: 12;" +
                    "-fx-border-color: " + GREEN_MAIN + ";" +
                    "-fx-border-radius: 12;" +
                    "-fx-border-width: 1;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5);" +
                    "-fx-translate-y: -2;";
        }
        return "-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 10, 0, 0, 2);" +
                "-fx-translate-y: 0;";
    }

    private String btnAddStyle(boolean hovered) {
        if (hovered) {
            return "-fx-background-color: " + GREEN_DARK + "; -fx-text-fill: white; -fx-font-size: 13px;" +
                    "-fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 2);";
        }
        return "-fx-background-color: " + GREEN_MAIN + "; -fx-text-fill: white; -fx-font-size: 13px;" +
                "-fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;";
    }

    private Label makePlaceholder() {
        Label ph = new Label("🌾");
        ph.setStyle("-fx-font-size: 58px;");
        return ph;
    }

    private Button makeFavBtn(Produit produit) {
        User currentUser = UserSession.getInstance() != null ? UserSession.getInstance().getUser() : null;
        boolean isFav = currentUser != null && wishlistService.estFavori(currentUser.getId(), produit.getIdProduit());

        String base = "-fx-background-color: white; -fx-font-size: 16px; -fx-background-radius: 50;" +
                "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0, 0, 2);" +
                "-fx-pref-width: 34; -fx-pref-height: 34; -fx-min-width: 34; -fx-min-height: 34;";

        Button favBtn = new Button(isFav ? "\u2665" : "\u2661");
        favBtn.setStyle(base + (isFav ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #aaa;"));

        favBtn.setOnAction(e -> {
            User user = UserSession.getInstance() != null ? UserSession.getInstance().getUser() : null;
            if (user == null)
                return;
            boolean nowFav = wishlistService.estFavori(user.getId(), produit.getIdProduit());
            if (nowFav) {
                wishlistService.supprimerFavori(user.getId(), produit.getIdProduit());
                favBtn.setText("\u2661");
                favBtn.setStyle(base + "-fx-text-fill: #aaa;");
            } else {
                wishlistService.ajouterFavori(user.getId(), produit.getIdProduit());
                favBtn.setText("\u2665");
                favBtn.setStyle(base + "-fx-text-fill: #e74c3c;");
            }
        });

        favBtn.setOnMouseEntered(e -> {
            boolean fav = currentUser != null && wishlistService.estFavori(currentUser.getId(), produit.getIdProduit());
            if (!fav)
                favBtn.setStyle(base + "-fx-text-fill: " + ORANGE + ";");
        });
        favBtn.setOnMouseExited(e -> {
            boolean fav = currentUser != null && wishlistService.estFavori(currentUser.getId(), produit.getIdProduit());
            favBtn.setStyle(base + (fav ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #aaa;"));
        });
        return favBtn;
    }

    private Label makeChip(String text) {
        Label chip = new Label(text);
        chip.setStyle(
                "-fx-background-color: #f0f4f1;" +
                        "-fx-text-fill: #4a6a58;" +
                        "-fx-font-size: 10px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 20; -fx-padding: 4 10 4 10;" +
                        "-fx-border-color: #dceade; -fx-border-radius: 20; -fx-border-width: 1;");
        return chip;
    }

    // ── Role filter ───────────────────────────────────────────────────────────
    private List<Produit> filtrerProduitsPourRole(List<Produit> produits) {
        User currentUser = UserSession.getInstance() != null ? UserSession.getInstance().getUser() : null;
        if (currentUser == null || currentUser.getRole() != Role.AGRICULTEUR)
            return produits;
        List<Produit> filtres = new ArrayList<>();
        for (Produit p : produits) {
            if (p.getIdUser() != currentUser.getId())
                filtres.add(p);
        }
        return filtres;
    }

    // ── Panier ───────────────────────────────────────────────────────────────
    private void ajouterAuPanier(Produit produit, int quantite) {
        User currentUser = UserSession.getInstance().getUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Session expirée", "Veuillez vous reconnecter.");
            return;
        }
        if (!produitService.verifierStock(produit.getIdProduit(), quantite)) {
            showAlert(Alert.AlertType.WARNING, "Stock insuffisant", "La quantité demandée n'est pas disponible.");
            return;
        }
        Panier panier = panierService.getPanierActif(currentUser.getId());
        if (panier != null && panierService.ajouterProduit(panier.getIdPanier(), produit.getIdProduit(), quantite)) {
            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    quantite + " × " + produit.getNom() + " ajouté(s) au panier !");
            updatePanierDisplay();
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ajouter le produit au panier.");
        }
    }

    private void updatePanierDisplay() {
        User currentUser = UserSession.getInstance() != null ? UserSession.getInstance().getUser() : null;
        if (currentUser == null)
            return;
        Panier panier = panierService.getPanierActif(currentUser.getId());
        if (panier != null) {
            int count = panierService.compterProduits(panier.getIdPanier());
            float total = panierService.calculerTotal(panier.getIdPanier());
            if (panierCountLabel != null)
                panierCountLabel.setText(count + " article(s)");
            if (panierTotalLabel != null)
                panierTotalLabel.setText(String.format("%.2f DT", total));
        } else {
            if (panierCountLabel != null)
                panierCountLabel.setText("0 article(s)");
            if (panierTotalLabel != null)
                panierTotalLabel.setText("0.00 DT");
        }
    }

    @FXML
    private void ouvrirPanierClick(MouseEvent event) {
        ouvrirPanier();
    }

    @FXML
    private void ouvrirPanier() {
        try {
            User currentUser = UserSession.getInstance().getUser();
            if (currentUser == null) {
                showAlert(Alert.AlertType.ERROR, "Session", "Reconnectez-vous.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/marketplace/panier.fxml"));
            Parent root = loader.load();
            PanierController pc = loader.getController();
            pc.setUserId(currentUser.getId());
            pc.setMarketplaceController(this);
            Stage stage = new Stage();
            stage.setTitle("Mon Panier");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le panier : " + e.getMessage());
        }
    }

    @FXML
    private void ouvrirChatbot() {
        toggleChatbot();
    }

    @FXML
    private void toggleChatbot() {
        boolean visible = chatbotPanel.isVisible();
        chatbotPanel.setVisible(!visible);
        chatbotPanel.setManaged(!visible);

        if (!visible && !chatbotInitialized) {
            chatbotInitialized = true;
            afficherMessageBotChat(
                    "👋 Bonjour ! Je suis votre assistant du Marketplace Agricole.\n\n" +
                            "Je peux vous aider à :\n" +
                            "• 🛒 Acheter des produits (ex: \"Je veux 2 kg de tomates\")\n" +
                            "• 📦 Vérifier la disponibilité (ex: \"Avez-vous des olives ?\")\n\n" +
                            "Que puis-je faire pour vous ?");
        }

        btnToggleChatbot.setText(visible ? "🤖  Assistant IA" : "✕  Fermer l'assistant");
        btnToggleChatbot.setStyle(visible
                ? "-fx-background-color: #e6f5ee; -fx-text-fill: #046436; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand; -fx-border-color: #b8dfc8; -fx-border-radius: 12; -fx-border-width: 1.5;"
                : "-fx-background-color: #046436; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand; -fx-border-color: #034e2b; -fx-border-radius: 12; -fx-border-width: 1.5;");
    }

    @FXML
    private void envoyerMessageChat() {
        String message = chatInputField.getText().trim();
        if (message.isEmpty())
            return;

        // ── Enregistrer dans l'historique ─────────────────────────────────
        // Éviter les doublons consécutifs
        if (historiqueMessages.isEmpty() ||
                !historiqueMessages.get(historiqueMessages.size() - 1).equals(message)) {
            historiqueMessages.add(message);
        }
        // Réinitialiser l'index à la fin de l'historique
        indexHistorique = historiqueMessages.size();
        // ──────────────────────────────────────────────────────────────────

        afficherMessageUtilisateurChat(message);
        chatInputField.clear();

        chatInputField.setDisable(true);
        chatSendButton.setDisable(true);
        chatLoadingBox.setVisible(true);
        chatLoadingBox.setManaged(true);

        User currentUser = UserSession.getInstance().getUser();
        int uid = currentUser != null ? currentUser.getId() : 0;

        Thread thread = new Thread(() -> {
            ChatIntent intent = chatbotService.traiterMessage(message, uid);

            javafx.application.Platform.runLater(() -> {
                chatLoadingBox.setVisible(false);
                chatLoadingBox.setManaged(false);
                chatInputField.setDisable(false);
                chatSendButton.setDisable(false);

                afficherMessageBotChat(intent.getTexteReponse());

                if ("filtrer".equals(intent.getIntention())) {
                    appliquerFiltresDuChatbot(intent);
                }

                chatInputField.requestFocus();
                updatePanierDisplay();
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void appliquerFiltresDuChatbot(ChatIntent intent) {
        // 1. Texte de recherche
        if (intent.getRecherche() != null && !intent.getRecherche().isBlank()) {
            searchField.setText(intent.getRecherche());
        } else {
            searchField.clear();
        }

        // 2. Catégorie
        if (intent.getCategorie() != null && categorieComboBox.getItems().contains(intent.getCategorie())) {
            categorieComboBox.setValue(intent.getCategorie());
        } else {
            categorieComboBox.setValue("Toutes");
        }

        // 3. Prix minimum
        if (intent.getPrixMin() != null && sliderPrixMin != null) {
            double min = Math.max(sliderPrixMin.getMin(), intent.getPrixMin());
            sliderPrixMin.setValue(min);
            prixMinFiltre = min;
        } else if (sliderPrixMin != null) {
            sliderPrixMin.setValue(0);
            prixMinFiltre = 0;
        }

        // 4. Prix maximum
        if (intent.getPrixMax() != null && sliderPrixMax != null) {
            double max = Math.min(sliderPrixMax.getMax(), intent.getPrixMax());
            sliderPrixMax.setValue(max);
            prixMaxFiltre = max;
        } else if (intent.getPrixMin() == null && sliderPrixMax != null) {
            sliderPrixMax.setValue(sliderPrixMax.getMax());
            prixMaxFiltre = sliderPrixMax.getMax();
        }

        // 5. Tri
        if (intent.getCritere() != null && sortComboBox != null) {
            switch (intent.getCritere()) {
                case "prix_asc":
                    sortComboBox.setValue("Prix croissant");
                    break;
                case "prix_desc":
                    sortComboBox.setValue("Prix décroissant");
                    break;
                case "avis":
                    sortComboBox.setValue("Meilleures notes");
                    break;
            }
        }

        // 6. Mettre à jour labels + rafraîchir la grille
        updatePrixLabels();
        appliquerFiltresEtTri();
    }

    private void afficherMessageUtilisateurChat(String texte) {
        HBox row = new HBox();
        row.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Label bubble = new Label(texte);
        bubble.setWrapText(true);
        bubble.setMaxWidth(240);
        bubble.setPadding(new Insets(8, 12, 8, 12));
        bubble.setStyle(
                "-fx-background-color: #27ae60; -fx-text-fill: white;" +
                        "-fx-background-radius: 16 16 4 16; -fx-font-size: 12px;");
        row.getChildren().add(bubble);
        chatMessagesBox.getChildren().add(row);
    }

    private void afficherMessageBotChat(String texte) {
        HBox row = new HBox(8);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label avatar = new Label("🤖");
        avatar.setStyle("-fx-font-size: 16px;");
        avatar.setMinWidth(26);
        Label bubble = new Label(texte);
        bubble.setWrapText(true);
        bubble.setMaxWidth(260);
        bubble.setPadding(new Insets(8, 12, 8, 12));
        bubble.setStyle(
                "-fx-background-color: white; -fx-text-fill: #2c3e50;" +
                        "-fx-background-radius: 16 16 16 4; -fx-font-size: 12px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 4, 0, 0, 1);");
        row.getChildren().addAll(avatar, bubble);
        chatMessagesBox.getChildren().add(row);
    }

    @FXML
    private void ouvrirReclamation() {
        try {
            User currentUser = UserSession.getInstance().getUser();
            if (currentUser == null) {
                showAlert(Alert.AlertType.ERROR, "Session", "Reconnectez-vous.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/marketplace/reclamation.fxml"));
            Parent root = loader.load();
            ReclamationController rc = loader.getController();
            rc.setUserId(currentUser.getId());
            Stage stage = new Stage();
            stage.setTitle("Faire une réclamation");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la réclamation.");
        }
    }

    @FXML
    public void rafraichirProduits() {
        if (searchField != null)
            searchField.clear();
        categorieComboBox.setValue("Toutes");
        if (sortComboBox != null)
            sortComboBox.setValue("Défaut");
        if (checkSolde != null)
            checkSolde.setSelected(false);
        // Reset sliders
        if (sliderPrixMin != null) {
            sliderPrixMin.setValue(0);
            prixMinFiltre = 0;
        }
        if (sliderPrixMax != null) {
            sliderPrixMax.setValue(sliderPrixMax.getMax());
            prixMaxFiltre = sliderPrixMax.getMax();
        }
        updatePrixLabels();
        chargerProduits(null);
    }

    @FXML
    void retour(ActionEvent event) throws IOException {
        Role role = UserSession.getInstance().getUser().getRole();
        if (role == Role.ADMIN)
            changerScene(event, "/fxml/AdminDashboard.fxml");
        else
            changerScene(event, "/fxml/marketplace/Marketplace.fxml");
    }

    private void changerScene(ActionEvent event, String fxml) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxml));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, WindowUtils.APP_WIDTH, WindowUtils.APP_HEIGHT));
        stage.setResizable(true);
        WindowUtils.applyStandardSize(stage);
        stage.show();
    }

    public void rafraichirAffichagePanier() {
        updatePanierDisplay();
        updatePointsFideliteDisplay();
    }

    private void ouvrirFenetreAvis(Produit produit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/marketplace/AvisProduit.fxml"));
            Parent root = loader.load();
            AvisProduitController controller = loader.getController();
            controller.setProduit(produit);
            controller.setMarketplaceController(this);
            Stage stage = new Stage();
            stage.setTitle("Avis - " + produit.getNom());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir les avis.");
        }
    }

    // ── Détail produit ────────────────────────────────────────────────────────
    public void ouvrirDetailProduit(Produit produit) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/marketplace/DetailProduit.fxml"));
            Parent root = loader.load();
            DetailProduitController ctrl = loader.getController();
            ctrl.setProduit(produit);
            Stage stage = (Stage) produitsGrid.getScene().getWindow();
            stage.setTitle("Ardhi - " + produit.getNom());
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le détail : " + e.getMessage());
        }
    }

    private String getNomVendeur(Produit produit) {
        try {
            if (produit.getIdUser() <= 0)
                return "Inconnu";
            User vendeur = userService.chercherParId(produit.getIdUser());
            if (vendeur == null)
                return "Inconnu";
            String nomComplet = (vendeur.getPrenom() != null ? vendeur.getPrenom() + " " : "") +
                    (vendeur.getNom() != null ? vendeur.getNom() : "");
            return nomComplet.trim().isEmpty() ? vendeur.getEmail() : nomComplet.trim();
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

    private String getSymboleDevise(String devise) {
        if (devise == null)
            return "";
        switch (devise) {
            case "EUR":
                return "€";
            case "USD":
                return "$";
            case "DT":
                return "DT";
            default:
                return devise;
        }
    }

    @FXML
    private void toggleMicrophone() {
        if (speechToTextService == null) {
            speechToTextService = new SpeechToTextService();
            Thread loadThread = new Thread(() -> {
                try {
                    speechToTextService.loadModel();
                    javafx.application.Platform.runLater(() -> afficherMessageBotChat(
                            "🎤 Reconnaissance vocale prête ! Cliquez sur le micro pour parler."));
                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> afficherMessageBotChat(
                            "❌ Impossible de charger le modèle vocal : " + e.getMessage()));
                    speechToTextService = null;
                }
            });
            loadThread.setDaemon(true);
            loadThread.start();
            return;
        }

        if (speechToTextService.isListening()) {
            stopMicrophone();
        } else {
            startMicrophone();
        }
    }

    private void startMicrophone() {
        btnMicrophone.setStyle(
                "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 16px;" +
                        "-fx-background-radius: 50; -fx-cursor: hand; -fx-border-radius: 50;" +
                        "-fx-effect: dropshadow(gaussian, rgba(231,76,60,0.5), 10, 0, 0, 3);");
        btnMicrophone.setText("⏹");
        recordingIndicatorBox.setVisible(true);
        recordingIndicatorBox.setManaged(true);

        blinkTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0.5), e -> recordingDot.setVisible(!recordingDot.isVisible())));
        blinkTimeline.setCycleCount(Timeline.INDEFINITE);
        blinkTimeline.play();

        speechToTextService.startListening(texteReconnu -> {
            javafx.application.Platform.runLater(() -> {
                String actuel = chatInputField.getText();
                chatInputField.setText(actuel.isEmpty() ? texteReconnu : actuel + " " + texteReconnu);
                chatInputField.positionCaret(chatInputField.getText().length());
            });
        });
    }

    private void stopMicrophone() {
        if (speechToTextService != null) {
            speechToTextService.stopListening();
        }
        if (blinkTimeline != null) {
            blinkTimeline.stop();
        }
        btnMicrophone.setStyle(
                "-fx-background-color: #f0f4f1; -fx-text-fill: #046436; -fx-font-size: 16px;" +
                        "-fx-background-radius: 50; -fx-cursor: hand; -fx-border-color: #b8dfc8;" +
                        "-fx-border-radius: 50; -fx-border-width: 1.5;");
        btnMicrophone.setText("🎤");
        recordingIndicatorBox.setVisible(false);
        recordingIndicatorBox.setManaged(false);
        recordingDot.setVisible(true);
    }

}