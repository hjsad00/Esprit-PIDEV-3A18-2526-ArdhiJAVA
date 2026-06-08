package tn.neuron.ardhi.controllers.marketplace;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.neuron.ardhi.models.marketplace.Produit;
import tn.neuron.ardhi.models.marketplace.TypeReduction;
import tn.neuron.ardhi.models.marketplace.UniteMesure;
import tn.neuron.ardhi.models.UserAndDiag.Role;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.services.marketplace.ProduitService;
import tn.neuron.ardhi.services.marketplace.VisionService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;
import java.util.UUID;

public class GestionProduitsController implements Initializable {

    // ── Grille ───────────────────────────────────────────────────────────────
    @FXML private FlowPane produitsContainer;
    @FXML private VBox mainContainer;

    // ── Filtres ──────────────────────────────────────────────────────────────
    @FXML private TextField tfFiltreId;
    @FXML private TextField tfFiltreNom;
    @FXML private TextField tfFiltreCategorie;

    // ── Drawer ───────────────────────────────────────────────────────────────
    @FXML private VBox drawerOverlay;
    @FXML private VBox drawerPanel;
    @FXML private Label lblTitreFormulaire;

    // ── Formulaire ───────────────────────────────────────────────────────────
    @FXML private TextField tfNom;
    @FXML private TextArea taDescription;
    @FXML private TextField tfPrix;
    @FXML private TextField tfStock;
    @FXML private ComboBox<String> cbCategorie;
    @FXML private ComboBox<UniteMesure> cbUniteMesure;
    @FXML private ImageView ivProduit;
    @FXML private ComboBox<String> cbTypeRemise;
    @FXML private TextField tfRemise;
    @FXML private Label lblRemiseHint;

    // ── [NOUVEAU] Label indicateur IA ─────────────────────────────────────────
    // Ajoute dans ton FXML : <Label fx:id="lblIaStatus" text="" style="-fx-text-fill: #046436; -fx-font-size: 11px;"/>
    @FXML private Label lblIaStatus;

    // ── Boutons CRUD ─────────────────────────────────────────────────────────
    @FXML private Button btnAjouter;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;

    private String imagePath;
    private ProduitService produitService;
    // ── [NOUVEAU] Service Vision ──────────────────────────────────────────────
    private VisionService visionService;

    private ObservableList<Produit> listeProduits  = FXCollections.observableArrayList();
    private ObservableList<Produit> listeFiltree   = FXCollections.observableArrayList();
    private User currentUser;
    private Role currentUserRole;
    private Produit selectedProduit;

    // ── Drawer height ────────────────────────────────────────────────────────
    private static final double DRAWER_HEIGHT = 500;

    // ── Initialize ───────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        produitService = new ProduitService();
        visionService  = new VisionService(); // [NOUVEAU]
        currentUser    = UserSession.getInstance().getUser();
        if (currentUser == null) { showAlert("Erreur", "Session expirée."); return; }
        currentUserRole = currentUser.getRole();

        // UniteMesure ComboBox
        cbUniteMesure.setItems(FXCollections.observableArrayList(UniteMesure.values()));
        cbUniteMesure.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(UniteMesure item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.name());
            }
        });
        cbUniteMesure.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(UniteMesure item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.name());
            }
        });
        cbUniteMesure.setValue(UniteMesure.Kg);

        chargerCategories();
        chargerDonnees();

        btnAjouter.managedProperty().bind(btnAjouter.visibleProperty());
        btnModifier.managedProperty().bind(btnModifier.visibleProperty());
        btnSupprimer.managedProperty().bind(btnSupprimer.visibleProperty());

        // Remise
        cbTypeRemise.setItems(FXCollections.observableArrayList("Aucune remise","Pourcentage (%)","Montant fixe (DT)"));
        cbTypeRemise.setValue("Aucune remise");
        cbTypeRemise.setOnAction(e -> {
            boolean aucune = "Aucune remise".equals(cbTypeRemise.getValue());
            tfRemise.setVisible(!aucune); tfRemise.setManaged(!aucune);
            if (aucune) tfRemise.clear();
            updateRemiseHint();
        });
        tfRemise.textProperty().addListener((o, a, b) -> updateRemiseHint());
        tfPrix.textProperty().addListener((o, a, b) -> updateRemiseHint());
        tfRemise.setVisible(false); tfRemise.setManaged(false);

        // Drawer initial state (hors écran)
        drawerPanel.setTranslateY(DRAWER_HEIGHT);
        drawerOverlay.setOpacity(0);

        setupFiltreListeners();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DRAWER
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    void ouvrirDrawerAjout() {
        selectedProduit = null;
        lblTitreFormulaire.setText("Ajouter un nouveau produit");
        viderFormulaire();
        btnAjouter.setVisible(true);
        btnModifier.setVisible(false);
        btnSupprimer.setVisible(false);
        ouvrirDrawer();
    }

    private void ouvrirDrawerModification(Produit p) {
        selectedProduit = p;
        lblTitreFormulaire.setText("Modifier le produit #" + p.getIdProduit());
        remplirFormulaire(p);
        btnAjouter.setVisible(false);
        btnModifier.setVisible(true);
        btnSupprimer.setVisible(true);
        ouvrirDrawer();
    }

    private void ouvrirDrawer() {
        drawerOverlay.setVisible(true);
        drawerOverlay.setManaged(true);

        Timeline tl = new Timeline(
                new KeyFrame(Duration.millis(0),
                        new KeyValue(drawerPanel.translateYProperty(), DRAWER_HEIGHT),
                        new KeyValue(drawerOverlay.opacityProperty(), 0)
                ),
                new KeyFrame(Duration.millis(320),
                        new KeyValue(drawerPanel.translateYProperty(), 0,
                                javafx.animation.Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0)),
                        new KeyValue(drawerOverlay.opacityProperty(), 1)
                )
        );
        tl.play();
    }

    @FXML
    void fermerDrawer() {
        Timeline tl = new Timeline(
                new KeyFrame(Duration.millis(0),
                        new KeyValue(drawerPanel.translateYProperty(), 0),
                        new KeyValue(drawerOverlay.opacityProperty(), 1)
                ),
                new KeyFrame(Duration.millis(260),
                        new KeyValue(drawerPanel.translateYProperty(), DRAWER_HEIGHT,
                                javafx.animation.Interpolator.SPLINE(0.4, 0, 1, 1)),
                        new KeyValue(drawerOverlay.opacityProperty(), 0)
                )
        );
        tl.setOnFinished(e -> {
            drawerOverlay.setVisible(false);
            drawerOverlay.setManaged(false);
        });
        tl.play();
    }

    @FXML
    void fermerDrawerSiOverlay(MouseEvent event) {
        if (event.getTarget() == drawerOverlay) {
            fermerDrawer();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CARDS
    // ══════════════════════════════════════════════════════════════════════════

    private Label makePlaceholder() {
        Label ph = new Label("🌾");
        ph.setStyle("-fx-font-size: 52px;");
        return ph;
    }

    private void refreshCards() {
        produitsContainer.getChildren().clear();
        ObservableList<Produit> currentList = listeProduits;

        boolean filtreActif = (tfFiltreId != null && !tfFiltreId.getText().isEmpty())
                || (tfFiltreNom != null && !tfFiltreNom.getText().isEmpty())
                || (tfFiltreCategorie != null && !tfFiltreCategorie.getText().isEmpty());

        if (filtreActif) currentList = listeFiltree;

        for (Produit p : currentList) {
            produitsContainer.getChildren().add(creerCardProduit(p));
        }
    }

    private VBox creerCardProduit(Produit p) {
        boolean selected = selectedProduit != null && selectedProduit.getIdProduit() == p.getIdProduit();

        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 14; -fx-padding: 14; -fx-cursor: hand;" +
                        "-fx-border-color: " + (selected ? "#046436" : "#eee") + ";" +
                        "-fx-border-width: " + (selected ? "2" : "1") + "; -fx-border-radius: 14;"
        );
        card.setPrefWidth(210);
        card.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(0,0,0,0.06), 12, 0, 0, 4));

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 14; -fx-padding: 14; -fx-cursor: hand;" +
                        "-fx-border-color: #046436; -fx-border-width: 1.5; -fx-border-radius: 14;" +
                        "-fx-translate-y: -3; -fx-effect: dropshadow(gaussian, rgba(4,100,54,0.18), 18, 0, 0, 6);"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 14; -fx-padding: 14; -fx-cursor: hand;" +
                        "-fx-border-color: " + (selected ? "#046436" : "#eee") + ";" +
                        "-fx-border-width: " + (selected ? "2" : "1") + "; -fx-border-radius: 14;"
        ));

        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(182, 115);
        imageContainer.setStyle("-fx-background-color: #f5f8f5; -fx-background-radius: 8;");

        if (p.getImage() != null && !p.getImage().isEmpty()) {
            File file = new File(p.getImage());
            if (file.exists()) {
                ImageView imgView = new ImageView(new Image(file.toURI().toString()));
                imgView.setFitHeight(115); imgView.setFitWidth(182);
                imgView.setPreserveRatio(true);
                imageContainer.getChildren().add(imgView);
            } else { imageContainer.getChildren().add(makePlaceholder()); }
        } else { imageContainer.getChildren().add(makePlaceholder()); }

        Label nameLabel = new Label(p.getNom());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0d1f15;");
        nameLabel.setWrapText(true);

        Label priceLabel = new Label(String.format("%.2f DT", p.getPrix()));
        priceLabel.setStyle("-fx-text-fill: #046436; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label stockLabel = new Label("Stock: " + p.getQuantiteStock() + " " + p.getUniteMesure());
        stockLabel.setStyle("-fx-text-fill: #8aaa98; -fx-font-size: 10px;");

        card.getChildren().addAll(imageContainer, nameLabel, priceLabel, stockLabel);
        card.setOnMouseClicked(event -> ouvrirDrawerModification(p));
        return card;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FILTRES
    // ══════════════════════════════════════════════════════════════════════════

    private void setupFiltreListeners() {
        if (tfFiltreId != null)        tfFiltreId.textProperty().addListener((o,a,b) -> appliquerFiltre());
        if (tfFiltreNom != null)       tfFiltreNom.textProperty().addListener((o,a,b) -> appliquerFiltre());
        if (tfFiltreCategorie != null) tfFiltreCategorie.textProperty().addListener((o,a,b) -> appliquerFiltre());
    }

    private void appliquerFiltre() {
        listeFiltree.clear();
        String filtreId  = tfFiltreId != null ? tfFiltreId.getText().trim() : "";
        String filtreNom = tfFiltreNom != null ? tfFiltreNom.getText().trim().toLowerCase() : "";
        String filtreCat = tfFiltreCategorie != null ? tfFiltreCategorie.getText().trim().toLowerCase() : "";

        for (Produit p : listeProduits) {
            boolean okId  = filtreId.isEmpty()  || String.valueOf(p.getIdProduit()).contains(filtreId);
            boolean okNom = filtreNom.isEmpty() || p.getNom().toLowerCase().contains(filtreNom);
            boolean okCat = filtreCat.isEmpty() || (p.getCategorie() != null && p.getCategorie().toLowerCase().contains(filtreCat));
            if (okId && okNom && okCat) listeFiltree.add(p);
        }
        refreshCards();
    }

    @FXML void resetFiltre(ActionEvent event) {
        if (tfFiltreId != null)        tfFiltreId.clear();
        if (tfFiltreNom != null)       tfFiltreNom.clear();
        if (tfFiltreCategorie != null) tfFiltreCategorie.clear();
        refreshCards();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DONNÉES
    // ══════════════════════════════════════════════════════════════════════════

    private void chargerDonnees() {
        listeProduits.clear();
        if (currentUserRole == Role.AGRICULTEUR)
            listeProduits.addAll(produitService.getProduitsByUser(currentUser.getId()));
        else
            listeProduits.addAll(produitService.getAllProduits());
        refreshCards();
    }

    private void chargerCategories() {
        cbCategorie.getItems().clear();
        cbCategorie.getItems().addAll(produitService.getAllCategories());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FORMULAIRE
    // ══════════════════════════════════════════════════════════════════════════

    private void remplirFormulaire(Produit p) {
        tfNom.setText(p.getNom());
        taDescription.setText(p.getDescription());
        tfPrix.setText(String.valueOf(p.getPrix()));
        tfStock.setText(String.valueOf(p.getQuantiteStock()));
        cbCategorie.setValue(p.getCategorie());
        cbUniteMesure.setValue(p.getUniteMesure());

        if (p.aUneRemise()) {
            cbTypeRemise.setValue(p.getTypeRemise() == TypeReduction.POURCENTAGE ? "Pourcentage (%)" : "Montant fixe (DT)");
            tfRemise.setText(String.valueOf(p.getRemise()));
            tfRemise.setVisible(true); tfRemise.setManaged(true);
        } else {
            cbTypeRemise.setValue("Aucune remise");
            tfRemise.clear(); tfRemise.setVisible(false); tfRemise.setManaged(false);
        }
        updateRemiseHint();

        if (p.getImage() != null && !p.getImage().isEmpty()) {
            File f = new File(p.getImage());
            if (f.exists()) { ivProduit.setImage(new Image(f.toURI().toString())); imagePath = p.getImage(); }
            else             { ivProduit.setImage(null); imagePath = null; }
        } else { ivProduit.setImage(null); imagePath = null; }
    }

    private void viderFormulaire() {
        tfNom.clear(); taDescription.clear(); tfPrix.clear(); tfStock.clear();
        cbCategorie.setValue(null); cbUniteMesure.setValue(UniteMesure.Kg);
        ivProduit.setImage(null); imagePath = null;
        cbTypeRemise.setValue("Aucune remise");
        tfRemise.clear(); tfRemise.setVisible(false); tfRemise.setManaged(false);
        if (lblRemiseHint != null) lblRemiseHint.setText("");
        // Réinitialiser l'indicateur IA
        setIaStatus("", false);
    }

    private void updateRemiseHint() {
        if (lblRemiseHint == null) return;
        String type = cbTypeRemise.getValue();
        String val  = tfRemise.getText().trim();
        if ("Aucune remise".equals(type) || type == null) { lblRemiseHint.setText(""); return; }
        if (val.isEmpty()) {
            lblRemiseHint.setText("Pourcentage (%)".equals(type) ? "Entre 0 et 100" : "Montant ≤ prix");
            lblRemiseHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;"); return;
        }
        try {
            float v = Float.parseFloat(val);
            if ("Pourcentage (%)".equals(type)) {
                if (v < 0 || v > 100) { lblRemiseHint.setText("⚠ Entre 0 et 100"); lblRemiseHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #e74c3c;"); }
                else                  { lblRemiseHint.setText("✔ -" + v + "% sur le prix"); lblRemiseHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #27ae60;"); }
            } else {
                try {
                    float prixMax = Float.parseFloat(tfPrix.getText().trim());
                    if (v < 0 || v >= prixMax) { lblRemiseHint.setText("⚠ Doit être < " + String.format("%.2f", prixMax) + " DT"); lblRemiseHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #e74c3c;"); }
                    else                       { lblRemiseHint.setText("✔ -" + String.format("%.2f", v) + " DT"); lblRemiseHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #27ae60;"); }
                } catch (NumberFormatException ex) { lblRemiseHint.setText("-" + String.format("%.2f",v) + " DT"); lblRemiseHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #e67e22;"); }
            }
        } catch (NumberFormatException e) { lblRemiseHint.setText("⚠ Valeur invalide"); lblRemiseHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #e74c3c;"); }
    }

    private void appliquerRemiseSurProduit(Produit p) {
        String type = cbTypeRemise.getValue();
        if ("Pourcentage (%)".equals(type)) {
            try { p.setTypeRemise(TypeReduction.POURCENTAGE); p.setRemise(Float.parseFloat(tfRemise.getText().trim())); }
            catch (NumberFormatException e) { p.setTypeRemise(null); p.setRemise(0); }
        } else if ("Montant fixe (DT)".equals(type)) {
            try { p.setTypeRemise(TypeReduction.MONTANT_FIXE); p.setRemise(Float.parseFloat(tfRemise.getText().trim())); }
            catch (NumberFormatException e) { p.setTypeRemise(null); p.setRemise(0); }
        } else { p.setTypeRemise(null); p.setRemise(0); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // [NOUVEAU] IA — Indicateur de statut
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Met à jour le label de statut IA sous le bouton d'image.
     *
     * @param message  Texte à afficher
     * @param success  true = vert (succès), false = gris (neutre/loading)
     */
    private void setIaStatus(String message, boolean success) {
        if (lblIaStatus == null) return; // null-safe si absent du FXML
        Platform.runLater(() -> {
            lblIaStatus.setText(message);
            lblIaStatus.setStyle(success
                    ? "-fx-text-fill: #046436; -fx-font-size: 11px; -fx-font-weight: bold;"
                    : "-fx-text-fill: #888;    -fx-font-size: 11px;");
        });
    }

    /**
     * Applique un style "rempli par IA" (bordure verte + ✨) sur un TextField ou TextArea.
     */
    private void marquerChampIA(javafx.scene.control.Control champ) {
        if (champ == null) return;
        champ.setStyle("-fx-border-color: #046436; -fx-border-width: 1.5; -fx-border-radius: 6;");
        // On retire le style après 4 secondes pour ne pas perturber l'édition manuelle
        new Thread(() -> {
            try { Thread.sleep(4000); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> champ.setStyle(""));
        }).start();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // IMPORTER IMAGE + ANALYSE IA — [MODIFIÉ]
    // ══════════════════════════════════════════════════════════════════════════

    @FXML void importerImage(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));

        File selected = fc.showOpenDialog(ivProduit.getScene().getWindow());
        if (selected == null) return;

        try {
            // ── 1. Copier l'image dans le dossier ArdhiImages ──────────────────
            File dir = new File(System.getProperty("user.home") + File.separator + "ArdhiImages");
            if (!dir.exists()) dir.mkdirs();
            File dest = new File(dir, UUID.randomUUID() + "_" + selected.getName());
            Files.copy(selected.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

            imagePath = dest.getAbsolutePath();
            ivProduit.setImage(new Image(dest.toURI().toString()));

            // ── 2. Afficher l'indicateur "Analyse IA en cours..." ─────────────
            setIaStatus("✨ Analyse IA en cours...", false);

            // ── 3. Lancer l'analyse en arrière-plan (Thread) ──────────────────
            // On utilise un Thread pour ne pas bloquer l'UI JavaFX pendant l'appel API
            final File imageToAnalyze = dest;
            new Thread(() -> {
                VisionService.ProductInfo info = visionService.analyzeImage(imageToAnalyze);

                // ── 4. Mettre à jour les champs sur le thread JavaFX ──────────
                Platform.runLater(() -> {
                    // Nom
                    if (info.getNom() != null && !info.getNom().isBlank()) {
                        tfNom.setText(info.getNom());
                        marquerChampIA(tfNom);
                    }

                    // Catégorie (chercher dans les items existants du ComboBox)
                    if (info.getCategorie() != null && !info.getCategorie().isBlank()) {
                        boolean trouve = cbCategorie.getItems().contains(info.getCategorie());
                        if (trouve) {
                            cbCategorie.setValue(info.getCategorie());
                        } else {
                            // Ajouter la catégorie si elle n'existe pas encore
                            cbCategorie.getItems().add(info.getCategorie());
                            cbCategorie.setValue(info.getCategorie());
                        }
                        marquerChampIA(cbCategorie);
                    }

                    // Description
                    if (info.getDescription() != null && !info.getDescription().isBlank()) {
                        taDescription.setText(info.getDescription());
                        marquerChampIA(taDescription);
                    }

                    // ── 5. Message de confirmation ────────────────────────────
                    setIaStatus("✅ Champs remplis par l'IA — modifiables librement", true);
                });
            }).start();

        } catch (Exception e) {
            setIaStatus("❌ Erreur lors de l'analyse", false);
            showAlert("Erreur", "Impossible d'importer : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // VALIDATION
    // ══════════════════════════════════════════════════════════════════════════

    private boolean validerFormulaire() {
        if (tfNom.getText().trim().isEmpty()) { showAlert("Erreur","Nom obligatoire."); tfNom.requestFocus(); return false; }
        float prix;
        try { prix = Float.parseFloat(tfPrix.getText()); if (prix <= 0) { showAlert("Erreur","Prix > 0."); tfPrix.requestFocus(); return false; } }
        catch (NumberFormatException e) { showAlert("Erreur","Prix invalide."); tfPrix.requestFocus(); return false; }
        try { int s = Integer.parseInt(tfStock.getText()); if (s < 0) { showAlert("Erreur","Stock ≥ 0."); tfStock.requestFocus(); return false; } }
        catch (NumberFormatException e) { showAlert("Erreur","Stock invalide."); tfStock.requestFocus(); return false; }
        if (cbCategorie.getValue() == null || cbCategorie.getValue().trim().isEmpty()) { showAlert("Erreur","Catégorie obligatoire."); return false; }
        if (cbUniteMesure.getValue() == null) { showAlert("Erreur","Unité obligatoire."); return false; }
        String typeRemise = cbTypeRemise.getValue();
        if (typeRemise != null && !"Aucune remise".equals(typeRemise)) {
            String rs = tfRemise.getText().trim();
            if (rs.isEmpty()) { showAlert("Erreur","Entrez une valeur de remise."); tfRemise.requestFocus(); return false; }
            try {
                float remise = Float.parseFloat(rs);
                if (remise < 0) { showAlert("Erreur","Remise ≥ 0."); return false; }
                if ("Pourcentage (%)".equals(typeRemise) && remise > 100) { showAlert("Erreur","Remise ≤ 100%."); return false; }
                if ("Montant fixe (DT)".equals(typeRemise) && remise >= prix) { showAlert("Erreur","Remise < prix."); return false; }
            } catch (NumberFormatException e) { showAlert("Erreur","Remise invalide."); return false; }
        }
        return true;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CRUD
    // ══════════════════════════════════════════════════════════════════════════

    @FXML void ajouter(ActionEvent event) {
        if (!validerFormulaire()) return;
        try {
            Produit p = new Produit(0, tfNom.getText().trim(), taDescription.getText().trim(),
                    Float.parseFloat(tfPrix.getText()), Integer.parseInt(tfStock.getText()),
                    cbCategorie.getValue().trim(), currentUser.getId(), cbUniteMesure.getValue(), imagePath);
            appliquerRemiseSurProduit(p);
            if (produitService.ajouterProduit(p)) {
                chargerDonnees(); chargerCategories();
                fermerDrawer();
                showAlert("Succès","Produit ajouté !");
            } else showAlert("Erreur","Impossible d'ajouter.");
        } catch (Exception e) { showAlert("Erreur","Erreur : " + e.getMessage()); }
    }

    @FXML void modifier(ActionEvent event) {
        if (selectedProduit == null) { showAlert("Attention","Sélectionnez un produit."); return; }
        if (!validerFormulaire()) return;
        try {
            selectedProduit.setNom(tfNom.getText().trim());
            selectedProduit.setDescription(taDescription.getText().trim());
            selectedProduit.setPrix(Float.parseFloat(tfPrix.getText()));
            selectedProduit.setQuantiteStock(Integer.parseInt(tfStock.getText()));
            selectedProduit.setCategorie(cbCategorie.getValue().trim());
            selectedProduit.setUniteMesure(cbUniteMesure.getValue());
            if (imagePath != null) selectedProduit.setImage(imagePath);
            appliquerRemiseSurProduit(selectedProduit);
            if (produitService.modifierProduit(selectedProduit)) {
                chargerDonnees(); chargerCategories();
                fermerDrawer();
                showAlert("Succès","Produit modifié !");
            } else showAlert("Erreur","Impossible de modifier.");
        } catch (Exception e) { showAlert("Erreur","Erreur : " + e.getMessage()); }
    }

    @FXML void supprimer(ActionEvent event) {
        if (selectedProduit == null) { showAlert("Attention","Sélectionnez un produit."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation"); confirm.setHeaderText("Supprimer ce produit ?");
        confirm.setContentText("Cette action est irréversible.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                if (produitService.supprimerProduit(selectedProduit.getIdProduit())) {
                    chargerDonnees(); chargerCategories();
                    fermerDrawer();
                    showAlert("Succès","Produit supprimé !");
                } else showAlert("Erreur","Impossible de supprimer.");
            }
        });
    }

    @FXML void vider(ActionEvent event) {
        viderFormulaire();
        selectedProduit = null;
        lblTitreFormulaire.setText("Ajouter un nouveau produit");
        btnAjouter.setVisible(true); btnModifier.setVisible(false); btnSupprimer.setVisible(false);
        refreshCards();
    }

    @FXML void clicDehors(MouseEvent event) { /* overlay gère la fermeture */ }

    @FXML void retour(ActionEvent event) {
        try {
            String path = currentUserRole == Role.AGRICULTEUR
                    ? "/fxml/marketplace/Marketplace.fxml"
                    : "/fxml/UserAndDiag/AdminDashboard.fxml";
            Parent root = FXMLLoader.load(getClass().getResource(path));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, WindowUtils.APP_WIDTH, WindowUtils.APP_HEIGHT));
            WindowUtils.applyStandardSize(stage); stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setContentText(content); alert.show();
    }
}