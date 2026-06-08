package tn.neuron.ardhi.controllers.marketplace;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import tn.neuron.ardhi.interfaces.marketplace.IReclamationService;
import tn.neuron.ardhi.models.marketplace.Reclamation;
import tn.neuron.ardhi.models.marketplace.StatutReclamation;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.services.marketplace.ReclamationService;
import tn.neuron.ardhi.services.UserAndDiag.UserService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminReclamationsController implements Initializable {

    // ── Navbar / Stats ────────────────────────────────────────────────────────
    @FXML private Label lblTotalReclamations;
    @FXML private Label lblEnAttente;
    @FXML private Label lblEnCours;
    @FXML private Label lblResolues;
    @FXML private Label lblRejetees;
    @FXML private Label lblDerniereMAJ;

    // ── Filtres ───────────────────────────────────────────────────────────────
    @FXML private TextField        tfFiltreId;
    @FXML private ComboBox<String> comboFiltreStatut;
    @FXML private ComboBox<String> comboFiltreType;

    // ── Liste ─────────────────────────────────────────────────────────────────
    @FXML private FlowPane reclamationsContainer;
    @FXML private VBox     emptyState;

    // ── State ─────────────────────────────────────────────────────────────────
    private IReclamationService           reclamationService;
    private UserService                   userService;
    private ObservableList<Reclamation>   listeReclamations = FXCollections.observableArrayList();

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final String GREEN_DARK = "#2b5329";
    private static final String GREEN_MAIN = "#4a7c47";
    private static final String BORDER     = "#e0e8e2";
    private static final String GRAY_BG    = "#f8faf8";
    private static final String GRAY_TEXT  = "#6a8a78";

    // ══════════════════════════════════════════════════════════════════════════
    // INITIALIZE
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        reclamationService = new ReclamationService();
        userService        = new UserService();

        initFiltres();
        chargerReclamations();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // INIT FILTRES
    // ══════════════════════════════════════════════════════════════════════════

    private void initFiltres() {
        comboFiltreStatut.setItems(FXCollections.observableArrayList(
                "Tous", "EN_ATTENTE", "EN_COURS", "RESOLUE", "REJETEE"));
        comboFiltreStatut.setValue("Tous");

        comboFiltreType.setItems(FXCollections.observableArrayList(
                "Tous", "QUALITE_RECOLTE", "PRODUIT_AVARIE", "QUANTITE_INCORRECTE",
                "PRIX_NON_CONFORME", "PRODUIT_NON_CONFORME", "RETARD_LIVRAISON", "AUTRE"));
        comboFiltreType.setValue("Tous");

        comboFiltreStatut.setOnAction(e -> appliquerFiltre());
        comboFiltreType.setOnAction(e -> appliquerFiltre());
        tfFiltreId.textProperty().addListener((o, a, b) -> appliquerFiltre());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DONNÉES
    // ══════════════════════════════════════════════════════════════════════════

    private void chargerReclamations() {
        try {
            listeReclamations.setAll(reclamationService.getAllReclamations());
            updateStatistiques();
            updateDerniereMAJ();
            appliquerFiltre();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les réclamations : " + e.getMessage());
        }
    }

    private void appliquerFiltre() {
        String filtreId     = tfFiltreId.getText().trim();
        String filtreStatut = comboFiltreStatut.getValue();
        String filtreType   = comboFiltreType.getValue();

        List<Reclamation> filtrées = listeReclamations.stream()
                .filter(r -> filtreId.isEmpty() || String.valueOf(r.getIdReclamation()).contains(filtreId))
                .filter(r -> "Tous".equals(filtreStatut) || r.getStatut().name().equals(filtreStatut))
                .filter(r -> "Tous".equals(filtreType)   || r.getType().name().equals(filtreType))
                .collect(Collectors.toList());

        afficherCards(filtrées);
    }

    private void afficherCards(List<Reclamation> liste) {
        reclamationsContainer.getChildren().clear();
        if (liste.isEmpty()) {
            emptyState.setVisible(true);  emptyState.setManaged(true);
            reclamationsContainer.setVisible(false); reclamationsContainer.setManaged(false);
        } else {
            emptyState.setVisible(false); emptyState.setManaged(false);
            reclamationsContainer.setVisible(true);  reclamationsContainer.setManaged(true);
            for (Reclamation r : liste) reclamationsContainer.getChildren().add(creerCard(r));
        }
    }

    private void updateStatistiques() {
        lblTotalReclamations.setText(String.valueOf(listeReclamations.size()));
        lblEnAttente.setText(String.valueOf(listeReclamations.stream().filter(r -> r.getStatut() == StatutReclamation.EN_ATTENTE).count()));
        lblEnCours.setText  (String.valueOf(listeReclamations.stream().filter(r -> r.getStatut() == StatutReclamation.EN_COURS).count()));
        lblResolues.setText (String.valueOf(listeReclamations.stream().filter(r -> r.getStatut() == StatutReclamation.RESOLUE).count()));
        lblRejetees.setText (String.valueOf(listeReclamations.stream().filter(r -> r.getStatut() == StatutReclamation.REJETEE).count()));
    }

    private void updateDerniereMAJ() {
        lblDerniereMAJ.setText("MAJ : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
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

        Label typeBadge = new Label(r.getTypeLibelle());
        typeBadge.setStyle(
                "-fx-background-color: rgba(255,255,255,0.30); -fx-text-fill: " + info[3] + ";" +
                        "-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 4 10;");
        typeBadge.setWrapText(false);
        header.getChildren().addAll(iconStatut, statutBox, spacer, typeBadge);

        // ── Corps ─────────────────────────────────────────────────────────
        VBox body = new VBox(12);
        body.setPadding(new Insets(16, 18, 18, 18));

        // Produit
        HBox produitRow = makeInfoRow("📦", "Produit", r.getNomProduit());

        // Utilisateur
        HBox userRow = makeInfoRow("👤", "Utilisateur", getUserName(r.getIdUser()));

        // Date
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");
        HBox dateRow = makeInfoRow("📅", "Date", r.getDateReclamation().format(fmt));

        // Description tronquée
        String desc = r.getDescription() != null && r.getDescription().length() > 75
                ? r.getDescription().substring(0, 75) + "…" : r.getDescription();
        HBox descRow = makeInfoRow("📝", "Description", desc != null ? desc : "—");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #eef2ef;");

        // ── Boutons ───────────────────────────────────────────────────────
        Button btnVoir = makeBtn("👁️   Voir les détails", GRAY_BG, GREEN_DARK, BORDER, GREEN_MAIN, "white");
        btnVoir.setOnAction(e -> afficherDetails(r));

        Button btnTraiter = makeBtn("⚙️   Changer le statut", "#e3f0ff", "#1a5faa", "#b8d4f0", "#0e3d7a", "white");
        btnTraiter.setOnAction(e -> afficherMenuTraitement(r));

        body.getChildren().addAll(produitRow, userRow, dateRow, descRow, sep, btnVoir, btnTraiter);
        card.getChildren().addAll(header, body);
        return card;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ACTIONS
    // ══════════════════════════════════════════════════════════════════════════

    private void afficherDetails(Reclamation r) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
        String[] statutInfo = getStatutInfo(r.getStatut());

        // ── Contenu du dialog ─────────────────────────────────────────────────
        VBox content = new VBox(0);
        content.setStyle("-fx-background-color: white;");

        // Header coloré
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setStyle("-fx-background-color: #0d1f15;");

        Label icon = new Label("📋");
        icon.setStyle("-fx-font-size: 24px;");

        VBox titleBox = new VBox(3);
        Label title = new Label("Réclamation  #" + r.getIdReclamation());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #7fffc0;");
        Label subtitle = new Label(r.getNomProduit());
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #6a8a78;");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Badge statut dans le header
        Label badgeStatut = new Label(statutInfo[0] + "  " + statutInfo[1]);
        badgeStatut.setStyle(
                "-fx-background-color: " + statutInfo[2] + ";" +
                        "-fx-text-fill: " + statutInfo[3] + ";" +
                        "-fx-font-size: 12px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 10; -fx-padding: 6 14;");

        header.getChildren().addAll(icon, titleBox, spacer, badgeStatut);

        // ── Corps : grille d'infos ────────────────────────────────────────────
        VBox body = new VBox(16);
        body.setPadding(new Insets(24, 28, 24, 28));

        // Ligne 1 : Utilisateur + Produit
        HBox row1 = new HBox(24);
        row1.getChildren().addAll(
                makeDetailBlock("👤", "UTILISATEUR",
                        getUserName(r.getIdUser()) + "  (ID: " + r.getIdUser() + ")"),
                makeDetailBlock("📦", "PRODUIT",
                        r.getNomProduit() + "  (ID: " + r.getIdProduit() + ")")
        );

        // Ligne 2 : Type + Date
        HBox row2 = new HBox(24);
        row2.getChildren().addAll(
                makeDetailBlock("🏷️", "TYPE", r.getTypeLibelle()),
                makeDetailBlock("📅", "DATE", r.getDateReclamation().format(fmt))
        );

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #eef2ef;");

        // Description
        VBox descBlock = new VBox(6);
        Label descLabel = new Label("DESCRIPTION");
        descLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #8aaa98; -fx-letter-spacing: 1px;");
        Label descValue = new Label(r.getDescription() != null ? r.getDescription() : "—");
        descValue.setStyle("-fx-font-size: 13px; -fx-text-fill: #0d1f15; -fx-wrap-text: true;");
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
        dialog.getDialogPane().setPrefWidth(580);

        // Styliser le bouton Fermer
        Button btnClose = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        btnClose.setStyle(
                "-fx-background-color: #0d1f15; -fx-text-fill: #7fffc0;" +
                        "-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 24;");

        dialog.showAndWait();
    }

    /** Bloc info réutilisable pour le dialog détails */
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

    private void afficherMenuTraitement(Reclamation r) {
        // ── Contenu du dialog ─────────────────────────────────────────────────
        VBox content = new VBox(0);
        content.setStyle("-fx-background-color: white;");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setStyle("-fx-background-color: #0d1f15;");

        Label icon = new Label("⚙️");
        icon.setStyle("-fx-font-size: 22px;");

        VBox titleBox = new VBox(3);
        Label title = new Label("Changer le statut");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #7fffc0;");
        Label subtitle = new Label("Réclamation #" + r.getIdReclamation() + "  —  " + r.getNomProduit());
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #6a8a78;");
        titleBox.getChildren().addAll(title, subtitle);

        header.getChildren().addAll(icon, titleBox);

        // ── Corps ─────────────────────────────────────────────────────────────
        VBox body = new VBox(16);
        body.setPadding(new Insets(24, 28, 28, 28));

        // Statut actuel
        String[] infoCourant = getStatutInfo(r.getStatut());
        HBox statutActuel = new HBox(10);
        statutActuel.setAlignment(Pos.CENTER_LEFT);
        statutActuel.setStyle(
                "-fx-background-color: " + infoCourant[2] + ";" +
                        "-fx-background-radius: 10; -fx-padding: 10 16;");
        Label lblActuelTitre = new Label("STATUT ACTUEL");
        lblActuelTitre.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: " + infoCourant[3] + "; -fx-letter-spacing: 1px;");
        Label lblActuelVal = new Label(infoCourant[0] + "  " + infoCourant[1]);
        lblActuelVal.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + infoCourant[3] + ";");
        VBox actuelBox = new VBox(2, lblActuelTitre, lblActuelVal);
        statutActuel.getChildren().add(actuelBox);

        // Label "Choisir le nouveau statut"
        Label lblChoix = new Label("NOUVEAU STATUT");
        lblChoix.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #8aaa98; -fx-letter-spacing: 1px;");

        // Boutons de sélection de statut
        // On utilise un ToggleGroup pour sélection exclusive
        ToggleGroup toggleGroup = new ToggleGroup();

        VBox statutButtons = new VBox(10);
        for (StatutReclamation statut : StatutReclamation.values()) {
            String[] info = getStatutInfo(statut);
            ToggleButton btn = new ToggleButton(info[0] + "   " + info[1]);
            btn.setToggleGroup(toggleGroup);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setPrefHeight(44);
            btn.setUserData(statut);

            boolean isCurrent = statut == r.getStatut();
            String baseStyle =
                    "-fx-background-color: " + (isCurrent ? info[2] : "#f8faf8") + ";" +
                            "-fx-text-fill: " + (isCurrent ? info[3] : "#0d1f15") + ";" +
                            "-fx-font-size: 13px; -fx-font-weight: bold;" +
                            "-fx-background-radius: 10; -fx-cursor: hand;" +
                            "-fx-border-color: " + (isCurrent ? info[3] : "#e0e8e2") + ";" +
                            "-fx-border-radius: 10; -fx-border-width: " + (isCurrent ? "2" : "1") + ";";

            String selectedStyle =
                    "-fx-background-color: " + info[2] + ";" +
                            "-fx-text-fill: " + info[3] + ";" +
                            "-fx-font-size: 13px; -fx-font-weight: bold;" +
                            "-fx-background-radius: 10; -fx-cursor: hand;" +
                            "-fx-border-color: " + info[3] + "; -fx-border-radius: 10; -fx-border-width: 2;";

            btn.setStyle(baseStyle);
            if (isCurrent) btn.setSelected(true);

            btn.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) btn.setStyle(selectedStyle);
                else            btn.setStyle(baseStyle);
            });

            statutButtons.getChildren().add(btn);
        }

        body.getChildren().addAll(statutActuel, lblChoix, statutButtons);
        content.getChildren().addAll(header, body);

        // ── Dialog ────────────────────────────────────────────────────────────
        Dialog<StatutReclamation> dialog = new Dialog<>();
        dialog.setTitle("Changer le statut");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-padding: 0;");
        dialog.getDialogPane().setPrefWidth(460);

        ButtonType btnConfirmerType = new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnulerType   = new ButtonType("Annuler",   ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnConfirmerType, btnAnnulerType);

        // Style des boutons
        Button btnConfirmer = (Button) dialog.getDialogPane().lookupButton(btnConfirmerType);
        btnConfirmer.setStyle(
                "-fx-background-color: #046436; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 9 28;");

        Button btnAnnuler = (Button) dialog.getDialogPane().lookupButton(btnAnnulerType);
        btnAnnuler.setStyle(
                "-fx-background-color: #f0f3f0; -fx-text-fill: #6a8a78;" +
                        "-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;" +
                        "-fx-border-color: #d8e4da; -fx-border-radius: 10; -fx-border-width: 1; -fx-padding: 9 28;");

        dialog.setResultConverter(buttonType -> {
            if (buttonType == btnConfirmerType && toggleGroup.getSelectedToggle() != null) {
                return (StatutReclamation) toggleGroup.getSelectedToggle().getUserData();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(nouveauStatut -> {
            if (nouveauStatut != r.getStatut()) {
                if (reclamationService.updateStatut(r.getIdReclamation(), nouveauStatut)) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Statut mis à jour avec succès.");
                    chargerReclamations();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre à jour le statut.");
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    void rafraichir(ActionEvent event) {
        chargerReclamations();
    }

    @FXML
    void resetFiltre(ActionEvent event) {
        tfFiltreId.clear();
        comboFiltreStatut.setValue("Tous");
        comboFiltreType.setValue("Tous");
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Tableau de Bord Admin");
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

    private String getUserName(int userId) {
        try {
            User user = userService.chercherParId(userId);
            if (user != null) {
                String nom = (user.getPrenom() != null ? user.getPrenom() + " " : "") +
                        (user.getNom() != null ? user.getNom() : "");
                return nom.trim().isEmpty() ? user.getEmail() : nom.trim();
            }
        } catch (Exception ignored) {}
        return "Utilisateur #" + userId;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
