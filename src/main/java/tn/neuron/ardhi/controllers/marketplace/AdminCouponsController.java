package tn.neuron.ardhi.controllers.marketplace;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import tn.neuron.ardhi.models.marketplace.Coupon;
import tn.neuron.ardhi.models.marketplace.TypeReduction;
import tn.neuron.ardhi.services.marketplace.CouponService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.sql.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminCouponsController implements Initializable {

    // ── Drawer ───────────────────────────────────────────────────────────────
    @FXML private VBox drawerOverlay;
    @FXML private VBox drawerPanel;
    @FXML private Label lblTitreFormulaire;

    // ── Sub-bar ──────────────────────────────────────────────────────────────
    @FXML private Label lblSubtitle;
    @FXML private Label lblTotalCoupons;

    // ── Filtres ──────────────────────────────────────────────────────────────
    @FXML private TextField tfFiltreCode;
    @FXML private ComboBox<String> cbFiltreType;
    @FXML private ComboBox<String> cbFiltreStatut;

    // ── Formulaire ───────────────────────────────────────────────────────────
    @FXML private TextField txtCode;
    @FXML private ComboBox<TypeReduction> cbType;
    @FXML private TextField txtValeur;
    @FXML private DatePicker dpDebut;
    @FXML private DatePicker dpFin;
    @FXML private TextField txtUtilMax;
    @FXML private TextField txtLimitUser;
    @FXML private TextField txtMontantMin;
    @FXML private CheckBox chkActif;

    // ── Boutons CRUD ─────────────────────────────────────────────────────────
    @FXML private Button btnAjouter;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;

    // ── Grille ───────────────────────────────────────────────────────────────
    @FXML private FlowPane couponsContainer;

    // ── State ────────────────────────────────────────────────────────────────
    private CouponService couponService;
    private Coupon couponEnEdition = null;
    private ObservableList<Coupon> listeCoupons  = FXCollections.observableArrayList();
    private ObservableList<Coupon> listeFiltree  = FXCollections.observableArrayList();

    private static final double DRAWER_HEIGHT = 540;

    // ══════════════════════════════════════════════════════════════════════════
    // INITIALIZE
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        couponService = new CouponService();

        // Type ComboBox dans le formulaire
        cbType.getItems().addAll(TypeReduction.values());

        // Filtres
        cbFiltreType.getItems().addAll("Tous les types");
        for (TypeReduction t : TypeReduction.values()) cbFiltreType.getItems().add(t.name());
        cbFiltreType.setValue("Tous les types");

        cbFiltreStatut.getItems().addAll("Tous", "Valide", "Expiré", "Épuisé", "À venir", "Inactif");
        cbFiltreStatut.setValue("Tous");

        // Listeners filtres
        tfFiltreCode.textProperty().addListener((o, a, b) -> appliquerFiltre());
        cbFiltreType.valueProperty().addListener((o, a, b) -> appliquerFiltre());
        cbFiltreStatut.valueProperty().addListener((o, a, b) -> appliquerFiltre());

        // Drawer état initial (hors écran)
        drawerPanel.setTranslateY(DRAWER_HEIGHT);
        drawerOverlay.setOpacity(0);

        // managed/visible sync
        btnAjouter.managedProperty().bind(btnAjouter.visibleProperty());
        btnModifier.managedProperty().bind(btnModifier.visibleProperty());
        btnSupprimer.managedProperty().bind(btnSupprimer.visibleProperty());

        chargerDonnees();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DRAWER
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    void ouvrirDrawerAjout() {
        couponEnEdition = null;
        lblTitreFormulaire.setText("Nouveau Coupon");
        clearForm();
        btnAjouter.setVisible(true);
        btnModifier.setVisible(false);
        btnSupprimer.setVisible(false);
        ouvrirDrawer();
    }

    private void ouvrirDrawerModification(Coupon c) {
        couponEnEdition = c;
        lblTitreFormulaire.setText("Modifier le coupon : " + c.getCode());
        remplirFormulaire(c);
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
                        new KeyValue(drawerOverlay.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(320),
                        new KeyValue(drawerPanel.translateYProperty(), 0,
                                javafx.animation.Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0)),
                        new KeyValue(drawerOverlay.opacityProperty(), 1))
        );
        tl.play();
    }

    @FXML
    void fermerDrawer() {
        Timeline tl = new Timeline(
                new KeyFrame(Duration.millis(0),
                        new KeyValue(drawerPanel.translateYProperty(), 0),
                        new KeyValue(drawerOverlay.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(260),
                        new KeyValue(drawerPanel.translateYProperty(), DRAWER_HEIGHT,
                                javafx.animation.Interpolator.SPLINE(0.4, 0, 1, 1)),
                        new KeyValue(drawerOverlay.opacityProperty(), 0))
        );
        tl.setOnFinished(e -> {
            drawerOverlay.setVisible(false);
            drawerOverlay.setManaged(false);
        });
        tl.play();
    }

    @FXML
    void fermerDrawerSiOverlay(MouseEvent event) {
        if (event.getTarget() == drawerOverlay) fermerDrawer();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CRUD
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    void ajouterCoupon(ActionEvent event) {
        if (!validateForm()) return;

        if (couponEnEdition == null) {
            // Mode AJOUT
            Coupon c = new Coupon();
            fillCouponFromForm(c);
            c.setUtilisationActuelle(0);
            if (couponService.addCoupon(c)) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Coupon ajouté avec succès.");
                fermerDrawer();
                chargerDonnees();
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ajouter le coupon.");
            }
        } else {
            // Mode MODIFICATION (déclenché par btnModifier qui pointe aussi ici)
            fillCouponFromForm(couponEnEdition);
            if (couponService.updateCoupon(couponEnEdition)) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Coupon modifié avec succès.");
                fermerDrawer();
                chargerDonnees();
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de modifier le coupon.");
            }
        }
    }

    @FXML
    void supprimerCoupon(ActionEvent event) {
        if (couponEnEdition == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le coupon");
        confirm.setHeaderText("Supprimer " + couponEnEdition.getCode() + " ?");
        confirm.setContentText("Cette action est irréversible.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (couponService.deleteCoupon(couponEnEdition.getIdCoupon())) {
                fermerDrawer();
                chargerDonnees();
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer le coupon.");
            }
        }
    }

    @FXML
    void viderFormulaire(ActionEvent event) {
        couponEnEdition = null;
        clearForm();
        lblTitreFormulaire.setText("Nouveau Coupon");
        btnAjouter.setVisible(true);
        btnModifier.setVisible(false);
        btnSupprimer.setVisible(false);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DONNÉES
    // ══════════════════════════════════════════════════════════════════════════

    private void chargerDonnees() {
        listeCoupons.clear();
        listeCoupons.addAll(couponService.getAllCoupons());
        appliquerFiltre();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FILTRES
    // ══════════════════════════════════════════════════════════════════════════

    private void appliquerFiltre() {
        listeFiltree.clear();
        String filtreCode   = tfFiltreCode != null ? tfFiltreCode.getText().trim().toLowerCase() : "";
        String filtreType   = cbFiltreType != null ? cbFiltreType.getValue() : "Tous les types";
        String filtreStatut = cbFiltreStatut != null ? cbFiltreStatut.getValue() : "Tous";

        java.time.LocalDate today = java.time.LocalDate.now();

        for (Coupon c : listeCoupons) {
            boolean okCode = filtreCode.isEmpty() || c.getCode().toLowerCase().contains(filtreCode);
            boolean okType = "Tous les types".equals(filtreType) || filtreType == null
                    || c.getTypeReduction().name().equals(filtreType);

            // Calcul du statut réel du coupon
            boolean okStatut;
            if ("Tous".equals(filtreStatut) || filtreStatut == null) {
                okStatut = true;
            } else {
                java.time.LocalDate debut = c.getDateDebut().toLocalDate();
                java.time.LocalDate fin   = c.getDateFin().toLocalDate();
                String statut;
                if (!c.isActif())                                      statut = "Inactif";
                else if (c.getUtilisationActuelle() >= c.getUtilisationMax()) statut = "Épuisé";
                else if (today.isAfter(fin))                           statut = "Expiré";
                else if (today.isBefore(debut))                        statut = "À venir";
                else                                                   statut = "Valide";
                okStatut = filtreStatut.equals(statut);
            }

            if (okCode && okType && okStatut) listeFiltree.add(c);
        }
        refreshCards();
    }

    @FXML
    void resetFiltre(ActionEvent event) {
        if (tfFiltreCode != null)   tfFiltreCode.clear();
        if (cbFiltreType != null)   cbFiltreType.setValue("Tous les types");
        if (cbFiltreStatut != null) cbFiltreStatut.setValue("Tous");
        refreshCards();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CARDS
    // ══════════════════════════════════════════════════════════════════════════

    private void refreshCards() {
        couponsContainer.getChildren().clear();
        ObservableList<Coupon> liste = listeFiltree.isEmpty() && tfFiltreCode.getText().isEmpty()
                && "Tous les types".equals(cbFiltreType.getValue())
                && "Tous".equals(cbFiltreStatut.getValue())
                ? listeCoupons : listeFiltree;
        lblTotalCoupons.setText(liste.size() + " coupon(s)");
        for (Coupon c : liste) couponsContainer.getChildren().add(creerCouponCard(c));
    }

    private VBox creerCouponCard(Coupon c) {
        boolean selected = couponEnEdition != null && couponEnEdition.getIdCoupon() == c.getIdCoupon();

        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 14; -fx-padding: 16; -fx-cursor: hand;" +
                        "-fx-border-color: " + (selected ? "#046436" : "#eee") + ";" +
                        "-fx-border-width: " + (selected ? "2" : "1") + "; -fx-border-radius: 14;"
        );
        card.setPrefWidth(220);
        card.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(0, 0, 0, 0.06), 12, 0, 0, 4));

        // Hover
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 14; -fx-padding: 16; -fx-cursor: hand;" +
                        "-fx-border-color: #046436; -fx-border-width: 1.5; -fx-border-radius: 14;" +
                        "-fx-translate-y: -3; -fx-effect: dropshadow(gaussian, rgba(4,100,54,0.18), 18, 0, 0, 6);"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 14; -fx-padding: 16; -fx-cursor: hand;" +
                        "-fx-border-color: " + (selected ? "#046436" : "#eee") + ";" +
                        "-fx-border-width: " + (selected ? "2" : "1") + "; -fx-border-radius: 14;"
        ));

        // Badge statut — 5 états (priorité : Inactif > Épuisé > Expiré > Pas encore valide > Valide)
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate dateDebut = c.getDateDebut().toLocalDate();
        java.time.LocalDate dateFin   = c.getDateFin().toLocalDate();

        String badgeText;
        String badgeStyle;

        if (!c.isActif()) {
            // 1. Inactif (désactivé manuellement)
            badgeText  = "○ Inactif";
            badgeStyle = "-fx-background-color: #f0f0f0; -fx-text-fill: #888888;";
        } else if (c.getUtilisationActuelle() >= c.getUtilisationMax()) {
            // 2. Épuisé (quota atteint)
            badgeText  = "⊘ Épuisé";
            badgeStyle = "-fx-background-color: #fff3e0; -fx-text-fill: #e65100;";
        } else if (today.isAfter(dateFin)) {
            // 3. Expiré (date dépassée)
            badgeText  = "✘ Expiré";
            badgeStyle = "-fx-background-color: #fdecea; -fx-text-fill: #c0392b;";
        } else if (today.isBefore(dateDebut)) {
            // 4. Pas encore valide (période future)
            badgeText  = "◷ À venir";
            badgeStyle = "-fx-background-color: #e8eaf6; -fx-text-fill: #3949ab;";
        } else {
            // 5. Valide (toutes conditions OK)
            badgeText  = "✔ Valide";
            badgeStyle = "-fx-background-color: #e8f8f0; -fx-text-fill: #046436;";
        }

        Label badge = new Label(badgeText);
        badge.setStyle(badgeStyle +
                " -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 3 8;");

        // Code
        Label lblCode = new Label(c.getCode());
        lblCode.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #0d1f15;");

        // Valeur
        String valeurStr = c.getTypeReduction() == TypeReduction.POURCENTAGE
                ? c.getValeur() + "%" : c.getValeur() + " DT";
        Label lblValeur = new Label(c.getTypeReduction().name() + "  —  " + valeurStr);
        lblValeur.setStyle("-fx-text-fill: #046436; -fx-font-weight: bold; -fx-font-size: 12px;");

        // Dates
        Label lblDates = new Label("Du " + c.getDateDebut() + "\nau " + c.getDateFin());
        lblDates.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

        // Montant minimum
        Label lblMontantMin = new Label("Min. commande : " + String.format("%.2f", c.getMontantMin()) + " DT");
        lblMontantMin.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

        // Utilisation + barre
        double ratio = c.getUtilisationMax() > 0
                ? (double) c.getUtilisationActuelle() / c.getUtilisationMax() : 0;
        Label lblUsage = new Label("Utilisé : " + c.getUtilisationActuelle() + " / " + c.getUtilisationMax());
        lblUsage.setStyle("-fx-text-fill: #8aaa98; -fx-font-size: 10px;");

        ProgressBar pb = new ProgressBar(ratio);
        pb.prefWidthProperty().bind(card.widthProperty());
        pb.setStyle("-fx-accent: " + (ratio >= 1.0 ? "#e74c3c" : "#046436") + ";");

        card.getChildren().addAll(badge, lblCode, lblValeur, lblDates, lblMontantMin, lblUsage, pb);
        card.setOnMouseClicked(event -> ouvrirDrawerModification(c));
        return card;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FORMULAIRE
    // ══════════════════════════════════════════════════════════════════════════

    private void remplirFormulaire(Coupon c) {
        txtCode.setText(c.getCode());
        cbType.setValue(c.getTypeReduction());
        txtValeur.setText(String.valueOf(c.getValeur()));
        dpDebut.setValue(c.getDateDebut().toLocalDate());
        dpFin.setValue(c.getDateFin().toLocalDate());
        txtUtilMax.setText(String.valueOf(c.getUtilisationMax()));
        txtLimitUser.setText(String.valueOf(c.getLimiteParUser()));
        txtMontantMin.setText(String.valueOf(c.getMontantMin()));
        chkActif.setSelected(c.isActif());
    }

    private void fillCouponFromForm(Coupon c) {
        c.setCode(txtCode.getText().trim().toUpperCase());
        c.setTypeReduction(cbType.getValue());
        c.setValeur(Double.parseDouble(txtValeur.getText()));
        c.setDateDebut(Date.valueOf(dpDebut.getValue()));
        c.setDateFin(Date.valueOf(dpFin.getValue()));
        c.setUtilisationMax(Integer.parseInt(txtUtilMax.getText()));
        c.setActif(chkActif.isSelected());
        c.setMontantMin(Double.parseDouble(txtMontantMin.getText()));
        c.setLimiteParUser(Integer.parseInt(txtLimitUser.getText()));
    }

    private void clearForm() {
        txtCode.clear();
        cbType.setValue(null);
        txtValeur.clear();
        dpDebut.setValue(null);
        dpFin.setValue(null);
        txtUtilMax.setText("100");
        txtLimitUser.setText("1");
        txtMontantMin.setText("0");
        chkActif.setSelected(true);
    }

    private boolean validateForm() {
        if (txtCode.getText().isEmpty() || cbType.getValue() == null || txtValeur.getText().isEmpty()
                || dpDebut.getValue() == null || dpFin.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez remplir tous les champs obligatoires.");
            return false;
        }
        try {
            Double.parseDouble(txtValeur.getText());
            Integer.parseInt(txtUtilMax.getText());
            Integer.parseInt(txtLimitUser.getText());
            Double.parseDouble(txtMontantMin.getText());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez vérifier les formats numériques.");
            return false;
        }
        if (dpFin.getValue().isBefore(dpDebut.getValue())) {
            showAlert(Alert.AlertType.WARNING, "Attention", "La date de fin doit être après la date de début.");
            return false;
        }
        return true;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Tableau de Bord Admin");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UTILITAIRES
    // ══════════════════════════════════════════════════════════════════════════

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
