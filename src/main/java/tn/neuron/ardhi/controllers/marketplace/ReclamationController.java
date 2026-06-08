package tn.neuron.ardhi.controllers.marketplace;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import tn.neuron.ardhi.interfaces.marketplace.IProduitService;
import tn.neuron.ardhi.interfaces.marketplace.IReclamationService;
import tn.neuron.ardhi.models.marketplace.Produit;
import tn.neuron.ardhi.models.marketplace.Reclamation;
import tn.neuron.ardhi.models.marketplace.StatutReclamation;
import tn.neuron.ardhi.models.marketplace.TypeReclamation;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.services.marketplace.ProduitService;
import tn.neuron.ardhi.services.marketplace.ReclamationService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ReclamationController implements Initializable {

    // ── Navbar / Stats ────────────────────────────────────────────────────────
    @FXML private Label lblTotalReclamations;
    @FXML private Label lblEnAttente;
    @FXML private Label lblEnCours;
    @FXML private Label lblResolues;
    @FXML private Label lblRejetees;

    // ── Formulaire ────────────────────────────────────────────────────────────
    @FXML private ComboBox<Produit> comboProduit;
    @FXML private ComboBox<String>  comboType;
    @FXML private TextField         txtSujet;
    @FXML private TextArea          txtDescription;
    @FXML private Button            btnSoumettre;
    @FXML private Button            btnAnnuler;

    // ── Liste ─────────────────────────────────────────────────────────────────
    @FXML private FlowPane   reclamationsContainer;
    @FXML private VBox       emptyState;
    @FXML private ComboBox<String> comboFiltreStatut;

    // ── State ─────────────────────────────────────────────────────────────────
    private IReclamationService reclamationService;
    private IProduitService     produitService;
    private int                 userId;
    private List<Reclamation>   toutesLesReclamations = new ArrayList<>();

    // ── Palette (identique CommandesVendeur) ──────────────────────────────────
    private static final String GREEN_DARK = "#2b5329";
    private static final String GREEN_MAIN = "#4a7c47";
    private static final String BORDER     = "#e0e8e2";
    private static final String GRAY_BG    = "#f8faf8";
    private static final String GRAY_TEXT  = "#6a8a78";

    // ══════════════════════════════════════════════════════════════════════════
    // INITIALIZE
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (UserSession.getInstance() == null || UserSession.getInstance().getUser() == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur de session", "Aucun utilisateur connecté.");
            return;
        }
        reclamationService = new ReclamationService();
        produitService     = new ProduitService();

        User currentUser = UserSession.getInstance().getUser();
        this.userId = currentUser.getId();

        initComboBoxes();
        initFiltreStatut();
        chargerReclamations();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // INIT
    // ══════════════════════════════════════════════════════════════════════════

    private void initComboBoxes() {
        // Produits
        List<Produit> produits = produitService.getAllProduits();
        comboProduit.setItems(FXCollections.observableArrayList(produits));
        comboProduit.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Produit item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNom());
            }
        });
        comboProduit.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Produit item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNom());
            }
        });

        // Types
        comboType.setItems(FXCollections.observableArrayList(
                "Qualité de récolte", "Produit avarié", "Quantité incorrecte",
                "Prix non conforme", "Produit non conforme", "Retard de livraison", "Autre"));
    }

    private void initFiltreStatut() {
        comboFiltreStatut.setItems(FXCollections.observableArrayList(
                "Tous", "En attente", "En cours", "Résolues", "Rejetées"));
        comboFiltreStatut.setValue("Tous");
        comboFiltreStatut.setOnAction(e -> appliquerFiltre());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DONNÉES
    // ══════════════════════════════════════════════════════════════════════════

    private void chargerReclamations() {
        toutesLesReclamations = reclamationService.getReclamationsByUser(userId);
        updateStatistiques();
        appliquerFiltre();
    }

    private void appliquerFiltre() {
        String filtre = comboFiltreStatut.getValue();
        List<Reclamation> liste;
        if (filtre == null || "Tous".equals(filtre)) {
            liste = toutesLesReclamations;
        } else {
            StatutReclamation statut = convertirFiltre(filtre);
            liste = toutesLesReclamations.stream()
                    .filter(r -> r.getStatut() == statut)
                    .collect(Collectors.toList());
        }
        afficherCards(liste);
    }

    private void afficherCards(List<Reclamation> liste) {
        reclamationsContainer.getChildren().clear();
        if (liste.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            reclamationsContainer.setVisible(false);
            reclamationsContainer.setManaged(false);
        } else {
            emptyState.setVisible(false);
            emptyState.setManaged(false);
            reclamationsContainer.setVisible(true);
            reclamationsContainer.setManaged(true);
            for (Reclamation r : liste) reclamationsContainer.getChildren().add(creerCard(r));
        }
    }

    private void updateStatistiques() {
        long total     = toutesLesReclamations.size();
        long enAttente = toutesLesReclamations.stream().filter(r -> r.getStatut() == StatutReclamation.EN_ATTENTE).count();
        long enCours   = toutesLesReclamations.stream().filter(r -> r.getStatut() == StatutReclamation.EN_COURS).count();
        long resolues  = toutesLesReclamations.stream().filter(r -> r.getStatut() == StatutReclamation.RESOLUE).count();
        long rejetees  = toutesLesReclamations.stream().filter(r -> r.getStatut() == StatutReclamation.REJETEE).count();

        if (lblTotalReclamations != null) lblTotalReclamations.setText(String.valueOf(total));
        if (lblEnAttente  != null) lblEnAttente.setText(String.valueOf(enAttente));
        if (lblEnCours    != null) lblEnCours.setText(String.valueOf(enCours));
        if (lblResolues   != null) lblResolues.setText(String.valueOf(resolues));
        if (lblRejetees   != null) lblRejetees.setText(String.valueOf(rejetees));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CARD BUILDER
    // ══════════════════════════════════════════════════════════════════════════

    private VBox creerCard(Reclamation r) {
        VBox card = new VBox(0);
        card.setPrefWidth(340);
        card.setMaxWidth(340);
        card.setStyle(cardStyle(false));
        card.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(13, 31, 21, 0.08), 18, 0, 0, 5));
        card.setOnMouseEntered(e -> card.setStyle(cardStyle(true)));
        card.setOnMouseExited(e  -> card.setStyle(cardStyle(false)));

        // ── Header coloré (statut) ────────────────────────────────────────
        String[] info = getStatutInfo(r.getStatut());
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 18, 14, 18));
        header.setStyle("-fx-background-color: " + info[2] + "; -fx-background-radius: 18 18 0 0;");

        Label iconStatut = new Label(info[0]);
        iconStatut.setStyle("-fx-font-size: 18px;");

        VBox statutBox = new VBox(2);
        Label lblStatut = new Label(info[1]);
        lblStatut.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + info[3] + ";");
        Label lblNum = new Label("Réclamation  #" + r.getIdReclamation());
        lblNum.setStyle("-fx-font-size: 11px; -fx-text-fill: " + info[3] + "; -fx-opacity: 0.7;");
        statutBox.getChildren().addAll(lblStatut, lblNum);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Badge type
        Label typeBadge = new Label(r.getTypeLibelle());
        typeBadge.setStyle(
                "-fx-background-color: rgba(255,255,255,0.30);" +
                        "-fx-text-fill: " + info[3] + ";" +
                        "-fx-font-size: 11px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 10; -fx-padding: 4 10;");
        header.getChildren().addAll(iconStatut, statutBox, spacer, typeBadge);

        // ── Corps ─────────────────────────────────────────────────────────
        VBox body = new VBox(12);
        body.setPadding(new Insets(16, 18, 18, 18));

        // Produit
        HBox produitRow = makeInfoRow("📦", "Produit", r.getNomProduit());

        // Date
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMMM yyyy 'à' HH:mm", java.util.Locale.FRENCH);
        HBox dateRow = makeInfoRow("📅", "Date", r.getDateReclamation().format(fmt));

        // Description (tronquée)
        String desc = r.getDescription() != null && r.getDescription().length() > 80
                ? r.getDescription().substring(0, 80) + "…"
                : r.getDescription();
        HBox descRow = makeInfoRow("📝", "Description", desc != null ? desc : "—");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #eef2ef;");

        // ── Bouton détails ────────────────────────────────────────────────
        Button btnDetails = makeBtn("👁️   Voir les détails", GRAY_BG, GREEN_DARK, BORDER, GREEN_MAIN, "white");
        btnDetails.setOnAction(e -> afficherDetails(r));

        body.getChildren().addAll(produitRow, dateRow, descRow, sep, btnDetails);
        card.getChildren().addAll(header, body);
        return card;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DÉTAILS
    // ══════════════════════════════════════════════════════════════════════════

    private void afficherDetails(Reclamation r) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
        String[] info = getStatutInfo(r.getStatut());

        // ── Structure ─────────────────────────────────────────────────────────
        VBox content = new VBox(0);
        content.setStyle("-fx-background-color: white;");

        // Header sombre
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setStyle("-fx-background-color: #0d1f15;");

        Label iconTitle = new Label("📋");
        iconTitle.setStyle("-fx-font-size: 24px;");

        VBox titleBox = new VBox(3);
        Label title = new Label("Réclamation  #" + r.getIdReclamation());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #7fffc0;");
        Label subtitle = new Label(r.getNomProduit());
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #6a8a78;");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Badge statut
        Label badgeStatut = new Label(info[0] + "  " + info[1]);
        badgeStatut.setStyle(
                "-fx-background-color: " + info[2] + ";" +
                        "-fx-text-fill: " + info[3] + ";" +
                        "-fx-font-size: 12px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 10; -fx-padding: 6 14;");

        header.getChildren().addAll(iconTitle, titleBox, spacer, badgeStatut);

        // ── Corps ─────────────────────────────────────────────────────────────
        VBox body = new VBox(16);
        body.setPadding(new Insets(24, 28, 24, 28));

        // Ligne 1 : Produit + Type
        HBox row1 = new HBox(16);
        row1.getChildren().addAll(
                makeDetailBlock("📦", "PRODUIT", r.getNomProduit()),
                makeDetailBlock("🏷️", "TYPE", r.getTypeLibelle())
        );

        // Ligne 2 : Date + Statut
        HBox row2 = new HBox(16);
        row2.getChildren().addAll(
                makeDetailBlock("📅", "DATE", r.getDateReclamation().format(fmt)),
                makeDetailBlock("📊", "STATUT", formatStatut(r.getStatut()))
        );

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #eef2ef;");

        // Description complète
        VBox descBlock = new VBox(6);
        Label descLabel = new Label("DESCRIPTION");
        descLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #8aaa98; -fx-letter-spacing: 1px;");
        Label descValue = new Label(r.getDescription() != null ? r.getDescription() : "—");
        descValue.setStyle("-fx-font-size: 13px; -fx-text-fill: #0d1f15;");
        descValue.setWrapText(true);
        descBlock.getChildren().addAll(descLabel, descValue);

        body.getChildren().addAll(row1, row2, sep, descBlock);
        content.getChildren().addAll(header, body);

        // ── Dialog ────────────────────────────────────────────────────────────
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails de la réclamation");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-padding: 0;");
        dialog.getDialogPane().setPrefWidth(540);

        Button btnClose = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        btnClose.setStyle(
                "-fx-background-color: #0d1f15; -fx-text-fill: #7fffc0;" +
                        "-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 24;");

        dialog.showAndWait();
    }

    private VBox makeDetailBlock(String emoji, String label, String value) {
        VBox block = new VBox(5);
        block.setStyle(
                "-fx-background-color: #f8faf8; -fx-background-radius: 10;" +
                        "-fx-border-color: #e0e8e2; -fx-border-radius: 10; -fx-border-width: 1;" +
                        "-fx-padding: 12 16;");
        HBox.setHgrow(block, Priority.ALWAYS);

        HBox labelRow = new HBox(6);
        labelRow.setAlignment(Pos.CENTER_LEFT);
        Label ico = new Label(emoji);
        ico.setStyle("-fx-font-size: 13px;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #8aaa98; -fx-letter-spacing: 1px;");
        labelRow.getChildren().addAll(ico, lbl);

        Label val = new Label(value);
        val.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #0d1f15;");
        val.setWrapText(true);

        block.getChildren().addAll(labelRow, val);
        return block;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SOUMETTRE
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void soumettreReclamation() {
        if (comboProduit.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Champ requis", "Veuillez sélectionner un produit."); return;
        }
        if (comboType.getValue() == null || comboType.getValue().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ requis", "Veuillez sélectionner un type."); return;
        }
        if (txtDescription.getText() == null || txtDescription.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ requis", "Veuillez rédiger une description."); return;
        }

        Produit p = comboProduit.getValue();
        Reclamation rec = new Reclamation();
        rec.setIdUser(userId);
        rec.setIdProduit(p.getIdProduit());
        rec.setNomProduit(p.getNom());
        rec.setType(convertirTypeReclamation(comboType.getValue()));
        rec.setDescription(txtDescription.getText().trim());
        rec.setStatut(StatutReclamation.EN_ATTENTE);
        rec.setDateReclamation(LocalDateTime.now());

        if (reclamationService.creerReclamation(rec)) {
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Votre réclamation a été soumise avec succès !");
            resetFormulaire();
            chargerReclamations();
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de soumettre la réclamation.");
        }
    }

    private void resetFormulaire() {
        comboProduit.setValue(null);
        comboType.setValue(null);
        txtSujet.clear();
        txtDescription.clear();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void rafraichir() {
        chargerReclamations();
    }

    @FXML
    private void annuler() {
        resetFormulaire();
    }

    @FXML
    void retour(ActionEvent event) {
        ((Stage) btnAnnuler.getScene().getWindow()).close();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private HBox makeInfoRow(String emoji, String label, String value) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label(emoji);
        icon.setStyle("-fx-font-size: 14px;");
        icon.setMinWidth(20);
        VBox info = new VBox(1);
        Label lbl = new Label(label.toUpperCase());
        lbl.setStyle("-fx-font-size: 8px; -fx-text-fill: " + GRAY_TEXT + "; -fx-letter-spacing: 1px;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + GREEN_DARK + ";");
        val.setWrapText(true);
        val.setMaxWidth(260);
        info.getChildren().addAll(lbl, val);
        row.getChildren().addAll(icon, info);
        return row;
    }

    private Button makeBtn(String text, String bg, String fg, String border, String hoverBg, String hoverFg) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(38);
        String base  = "-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-border-color: " + border + "; -fx-border-radius: 10; -fx-border-width: 1;";
        String hover = "-fx-background-color: " + hoverBg + "; -fx-text-fill: " + hoverFg + "; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private String cardStyle(boolean hovered) {
        return hovered
                ? "-fx-background-color: white; -fx-background-radius: 18; -fx-border-color: " + GREEN_MAIN + "; -fx-border-radius: 18; -fx-border-width: 1.5; -fx-translate-y: -4; -fx-effect: dropshadow(gaussian, rgba(4,100,54,0.22), 28, 0, 0, 10); -fx-cursor: default;"
                : "-fx-background-color: white; -fx-background-radius: 18; -fx-border-color: " + BORDER + "; -fx-border-radius: 18; -fx-border-width: 1; -fx-cursor: default;";
    }

    private String[] getStatutInfo(StatutReclamation statut) {
        if (statut == null) return new String[]{"❓", "Inconnu", "#f0f3f0", "#333"};
        return switch (statut) {
            case EN_ATTENTE -> new String[]{"⏳", "En attente", "#fff8e1", "#b7860b"};
            case EN_COURS   -> new String[]{"🔄", "En cours",   "#e3f0ff", "#1a5faa"};
            case RESOLUE    -> new String[]{"✅", "Résolue",    "#e6f5ee", "#046436"};
            case REJETEE    -> new String[]{"❌", "Rejetée",    "#fdecea", "#c0392b"};
        };
    }

    private String formatStatut(StatutReclamation statut) {
        return switch (statut) {
            case EN_ATTENTE -> "⏳ En attente";
            case EN_COURS   -> "🔄 En cours";
            case RESOLUE    -> "✅ Résolue";
            case REJETEE    -> "❌ Rejetée";
        };
    }

    private StatutReclamation convertirFiltre(String filtre) {
        return switch (filtre) {
            case "En attente" -> StatutReclamation.EN_ATTENTE;
            case "En cours"   -> StatutReclamation.EN_COURS;
            case "Résolues"   -> StatutReclamation.RESOLUE;
            case "Rejetées"   -> StatutReclamation.REJETEE;
            default           -> StatutReclamation.EN_ATTENTE;
        };
    }

    private TypeReclamation convertirTypeReclamation(String typeStr) {
        return switch (typeStr) {
            case "Qualité de récolte"  -> TypeReclamation.QUALITE_RECOLTE;
            case "Produit avarié"      -> TypeReclamation.PRODUIT_AVARIE;
            case "Quantité incorrecte" -> TypeReclamation.QUANTITE_INCORRECTE;
            case "Prix non conforme"   -> TypeReclamation.PRIX_NON_CONFORME;
            case "Produit non conforme"-> TypeReclamation.PRODUIT_NON_CONFORME;
            case "Retard de livraison" -> TypeReclamation.RETARD_LIVRAISON;
            default                    -> TypeReclamation.AUTRE;
        };
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void setUserId(int userId) {
        this.userId = userId;
        chargerReclamations();
    }
}
