package tn.neuron.ardhi.controllers.marketplace;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import tn.neuron.ardhi.models.marketplace.Commande;
import tn.neuron.ardhi.models.marketplace.EtatCommande;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.services.marketplace.CommandeService;
import tn.neuron.ardhi.services.UserAndDiag.UserService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AdminCommandesController implements Initializable {

    // ── Stats ─────────────────────────────────────────────────────────────────
    @FXML private Label lblTotalCommandes;
    @FXML private Label lblEnAttente;
    @FXML private Label lblEnCours;
    @FXML private Label lblTotalEncaisse;

    // ── Sidebar filtres ───────────────────────────────────────────────────────
    @FXML private TextField        txtRecherche;
    @FXML private ComboBox<String> comboFiltreStatut;
    @FXML private Slider           sliderMinPrix;
    @FXML private Slider           sliderMaxPrix;
    @FXML private Label            lblMinPrix;
    @FXML private Label            lblMaxPrix;

    // ── Liste ─────────────────────────────────────────────────────────────────
    @FXML private FlowPane commandesContainer;
    @FXML private VBox     emptyState;

    // ── State ─────────────────────────────────────────────────────────────────
    private CommandeService             commandeService;
    private UserService                 userService;
    private ObservableList<Commande>    commandeList = FXCollections.observableArrayList();

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
        commandeService = new CommandeService();
        userService     = new UserService();

        initSidebar();
        loadCommandes();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // INIT SIDEBAR
    // ══════════════════════════════════════════════════════════════════════════

    private void initSidebar() {
        // ComboBox
        comboFiltreStatut.setItems(FXCollections.observableArrayList(
                "Tous", "en_attente", "en_cours", "livree", "annulee"));
        comboFiltreStatut.setValue("Tous");

        // Sliders
        sliderMinPrix.setMin(0);
        sliderMinPrix.setMax(9999);
        sliderMinPrix.setValue(0);

        sliderMaxPrix.setMin(0);
        sliderMaxPrix.setMax(9999);
        sliderMaxPrix.setValue(9999);

        // Listeners temps réel
        txtRecherche.textProperty().addListener((obs, o, n) -> appliquerFiltre());
        comboFiltreStatut.valueProperty().addListener((obs, o, n) -> appliquerFiltre());

        sliderMinPrix.valueProperty().addListener((obs, o, n) -> {
            if (n.doubleValue() > sliderMaxPrix.getValue())
                sliderMinPrix.setValue(sliderMaxPrix.getValue());
            lblMinPrix.setText(String.format("%.0f DT", sliderMinPrix.getValue()));
            appliquerFiltre();
        });

        sliderMaxPrix.valueProperty().addListener((obs, o, n) -> {
            if (n.doubleValue() < sliderMinPrix.getValue())
                sliderMaxPrix.setValue(sliderMinPrix.getValue());
            lblMaxPrix.setText(String.format("%.0f DT", sliderMaxPrix.getValue()));
            appliquerFiltre();
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DONNÉES
    // ══════════════════════════════════════════════════════════════════════════

    private void loadCommandes() {
        commandeList.setAll(commandeService.getAllCommandes());

        // Adapter plafond sliders au max réel
        if (!commandeList.isEmpty()) {
            double maxTotal = commandeList.stream()
                    .mapToDouble(Commande::getTotal).max().orElse(9999);
            double plafond = Math.max(maxTotal, 100);
            sliderMinPrix.setMax(plafond);
            sliderMaxPrix.setMax(plafond);
            sliderMaxPrix.setValue(plafond);
            lblMaxPrix.setText(String.format("%.0f DT", plafond));
        }

        updateStats();
        appliquerFiltre();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FILTRAGE (Predicate combiné)
    // ══════════════════════════════════════════════════════════════════════════

    private void appliquerFiltre() {
        String statut   = comboFiltreStatut.getValue();
        String search   = txtRecherche.getText() == null ? "" : txtRecherche.getText().trim().toLowerCase();
        double minPrice = sliderMinPrix.getValue();
        double maxPrice = sliderMaxPrix.getValue();

        Predicate<Commande> matchStatut = c ->
                statut == null || "Tous".equals(statut) ||
                        c.getEtat().toString().equals(statut);

        Predicate<Commande> matchTexte = c ->
                search.isEmpty() ||
                        String.valueOf(c.getIdCommande()).contains(search) ||
                        getNomClient(c.getIdUser()).toLowerCase().contains(search);

        Predicate<Commande> matchPrix = c ->
                c.getTotal() >= minPrice && c.getTotal() <= maxPrice;

        List<Commande> filtrees = commandeList.stream()
                .filter(matchStatut.and(matchTexte).and(matchPrix))
                .collect(Collectors.toList());

        afficherCards(filtrees);
    }

    private void afficherCards(List<Commande> liste) {
        commandesContainer.getChildren().clear();
        if (liste.isEmpty()) {
            emptyState.setVisible(true);  emptyState.setManaged(true);
            commandesContainer.setVisible(false); commandesContainer.setManaged(false);
        } else {
            emptyState.setVisible(false); emptyState.setManaged(false);
            commandesContainer.setVisible(true);  commandesContainer.setManaged(true);
            for (Commande c : liste) commandesContainer.getChildren().add(creerCard(c));
        }
    }

    private void updateStats() {
        long enAttente = commandeList.stream().filter(c -> c.getEtat() == EtatCommande.en_attente).count();
        long enCours   = commandeList.stream().filter(c -> c.getEtat() == EtatCommande.en_cours).count();
        double total   = commandeList.stream()
                .filter(c -> c.getEtat() != EtatCommande.annulee)
                .mapToDouble(Commande::getTotal).sum();

        lblTotalCommandes.setText(String.valueOf(commandeList.size()));
        lblEnAttente.setText(String.valueOf(enAttente));
        lblEnCours.setText(String.valueOf(enCours));
        lblTotalEncaisse.setText(String.format("%.2f DT", total));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CARD BUILDER
    // ══════════════════════════════════════════════════════════════════════════

    private VBox creerCard(Commande commande) {
        VBox card = new VBox(0);
        card.setPrefWidth(340);
        card.setMaxWidth(340);
        card.setStyle(cardStyle(false));
        card.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(13, 31, 21, 0.08), 18, 0, 0, 5));
        card.setOnMouseEntered(e -> card.setStyle(cardStyle(true)));
        card.setOnMouseExited(e  -> card.setStyle(cardStyle(false)));

        // Header coloré statut
        String[] statut = getStatutInfo(commande.getEtat());
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 18, 14, 18));
        header.setStyle("-fx-background-color: " + statut[2] + "; -fx-background-radius: 18 18 0 0;");

        Label iconStatut = new Label(statut[0]);
        iconStatut.setStyle("-fx-font-size: 18px;");

        VBox statutBox = new VBox(2);
        Label lblStatut = new Label(statut[1]);
        lblStatut.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + statut[3] + ";");
        Label lblNum = new Label("Commande  #" + commande.getIdCommande());
        lblNum.setStyle("-fx-font-size: 11px; -fx-text-fill: " + statut[3] + "; -fx-opacity: 0.7;");
        statutBox.getChildren().addAll(lblStatut, lblNum);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label totalBadge = new Label(String.format("%.2f DT", commande.getTotal()));
        totalBadge.setStyle(
                "-fx-background-color: rgba(255,255,255,0.30); -fx-text-fill: " + statut[3] + ";" +
                        "-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 4 10;");
        header.getChildren().addAll(iconStatut, statutBox, spacer, totalBadge);

        // Corps
        VBox body = new VBox(12);
        body.setPadding(new Insets(16, 18, 18, 18));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);
        HBox dateRow    = makeInfoRow("📅", "Date",   commande.getDateCommande().format(fmt));
        HBox clientRow  = makeInfoRow("👤", "Client", getNomClient(commande.getIdUser()));
        HBox progressRow = creerProgressBar(commande.getEtat());

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #eef2ef;");

        VBox actionsBox = new VBox(8);
        Button btnDetails = makeBtn("📋   Voir les détails", GRAY_BG, GREEN_DARK, BORDER, GREEN_MAIN, "white");
        btnDetails.setOnAction(e -> handleDetails(commande));

        Button btnEdit = makeBtn("⚙️   Changer le statut", "#e3f0ff", "#1a5faa", "#b8d4f0", "#0e3d7a", "white");
        btnEdit.setOnAction(e -> handleEdit(commande, card));

        Button btnDelete = makeBtn("🗑️   Supprimer", "#fdecea", "#c0392b", "#f5b7b1", "#922b21", "white");
        btnDelete.setOnAction(e -> handleDelete(commande));

        actionsBox.getChildren().addAll(btnDetails, btnEdit, btnDelete);
        body.getChildren().addAll(dateRow, clientRow, progressRow, sep, actionsBox);
        card.getChildren().addAll(header, body);
        return card;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ACTIONS
    // ══════════════════════════════════════════════════════════════════════════

    private void handleDetails(Commande commande) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/marketplace/DetailsCommande.fxml"));
            Parent content = loader.load();
            DetailsCommandeController dc = loader.getController();
            dc.initCommande(commande);
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Détails de la commande #" + commande.getIdCommande());
            dialog.setHeaderText(null);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().setContent(content);
            dc.setDialog(dialog);

            Button btnClose = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
            btnClose.setStyle(
                    "-fx-background-color: #0d1f15; -fx-text-fill: #7fffc0;" +
                            "-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 24;");
            dialog.showAndWait();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir les détails.");
        }
    }

    private void handleEdit(Commande commande, VBox card) {
        VBox content = new VBox(0);
        content.setStyle("-fx-background-color: white;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setStyle("-fx-background-color: #0d1f15;");
        Label icon = new Label("⚙️");
        icon.setStyle("-fx-font-size: 22px;");
        VBox titleBox = new VBox(3);
        Label title = new Label("Changer le statut");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #7fffc0;");
        Label subtitle = new Label("Commande #" + commande.getIdCommande() + "  —  " + getNomClient(commande.getIdUser()));
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #6a8a78;");
        titleBox.getChildren().addAll(title, subtitle);
        header.getChildren().addAll(icon, titleBox);

        VBox body = new VBox(16);
        body.setPadding(new Insets(24, 28, 28, 28));

        String[] infoCourant = getStatutInfo(commande.getEtat());
        HBox statutActuel = new HBox(10);
        statutActuel.setAlignment(Pos.CENTER_LEFT);
        statutActuel.setStyle("-fx-background-color: " + infoCourant[2] + "; -fx-background-radius: 10; -fx-padding: 10 16;");
        VBox actuelBox = new VBox(2,
                labelSmall("STATUT ACTUEL", infoCourant[3]),
                labelBold(infoCourant[0] + "  " + infoCourant[1], infoCourant[3]));
        statutActuel.getChildren().add(actuelBox);

        Label lblChoix = new Label("NOUVEAU STATUT");
        lblChoix.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #8aaa98; -fx-letter-spacing: 1px;");

        ToggleGroup toggleGroup = new ToggleGroup();
        VBox statutButtons = new VBox(10);

        for (EtatCommande etat : EtatCommande.values()) {
            String[] info = getStatutInfo(etat);
            ToggleButton btn = new ToggleButton(info[0] + "   " + info[1]);
            btn.setToggleGroup(toggleGroup);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setPrefHeight(44);
            btn.setUserData(etat);

            boolean isCurrent = etat == commande.getEtat();
            String baseStyle =
                    "-fx-background-color: " + (isCurrent ? info[2] : "#f8faf8") + ";" +
                            "-fx-text-fill: " + (isCurrent ? info[3] : "#0d1f15") + ";" +
                            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;" +
                            "-fx-border-color: " + (isCurrent ? info[3] : "#e0e8e2") + ";" +
                            "-fx-border-radius: 10; -fx-border-width: " + (isCurrent ? "2" : "1") + ";";
            String selectedStyle =
                    "-fx-background-color: " + info[2] + "; -fx-text-fill: " + info[3] + ";" +
                            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;" +
                            "-fx-border-color: " + info[3] + "; -fx-border-radius: 10; -fx-border-width: 2;";

            btn.setStyle(baseStyle);
            if (isCurrent) btn.setSelected(true);
            btn.selectedProperty().addListener((obs, was, is) -> btn.setStyle(is ? selectedStyle : baseStyle));
            statutButtons.getChildren().add(btn);
        }

        body.getChildren().addAll(statutActuel, lblChoix, statutButtons);
        content.getChildren().addAll(header, body);

        Dialog<EtatCommande> dialog = new Dialog<>();
        dialog.setTitle("Changer le statut");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-padding: 0;");
        dialog.getDialogPane().setPrefWidth(440);

        ButtonType btnConfirmerType = new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnulerType   = new ButtonType("Annuler",   ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnConfirmerType, btnAnnulerType);

        ((Button) dialog.getDialogPane().lookupButton(btnConfirmerType)).setStyle(
                "-fx-background-color: #046436; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 9 28;");
        ((Button) dialog.getDialogPane().lookupButton(btnAnnulerType)).setStyle(
                "-fx-background-color: #f0f3f0; -fx-text-fill: #6a8a78; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-border-color: #d8e4da; -fx-border-radius: 10; -fx-border-width: 1; -fx-padding: 9 28;");

        dialog.setResultConverter(bt -> {
            if (bt == btnConfirmerType && toggleGroup.getSelectedToggle() != null)
                return (EtatCommande) toggleGroup.getSelectedToggle().getUserData();
            return null;
        });

        dialog.showAndWait().ifPresent(nouvelEtat -> {
            if (nouvelEtat != commande.getEtat()) {
                if (commandeService.updateStatutCommande(commande.getIdCommande(), nouvelEtat)) {
                    commande.setEtat(nouvelEtat);
                    int idx = commandesContainer.getChildren().indexOf(card);
                    if (idx >= 0) commandesContainer.getChildren().set(idx, creerCard(commande));
                    updateStats();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre à jour le statut.");
                }
            }
        });
    }

    private void handleDelete(Commande commande) {
        VBox content = new VBox(0);
        content.setStyle("-fx-background-color: white;");
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setStyle("-fx-background-color: #0d1f15;");
        Label icon = new Label("🗑️");
        icon.setStyle("-fx-font-size: 22px;");
        VBox titleBox = new VBox(3);
        Label title = new Label("Supprimer la commande");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #f1948a;");
        Label subtitle = new Label("Commande #" + commande.getIdCommande() + "  —  " + getNomClient(commande.getIdUser()));
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #6a8a78;");
        titleBox.getChildren().addAll(title, subtitle);
        header.getChildren().addAll(icon, titleBox);

        VBox body = new VBox(12);
        body.setPadding(new Insets(24, 28, 28, 28));
        Label warning = new Label("⚠️  Cette action est irréversible.\nLa commande sera définitivement supprimée.");
        warning.setStyle("-fx-font-size: 13px; -fx-text-fill: #c0392b; -fx-font-weight: bold;" +
                "-fx-background-color: #fdecea; -fx-background-radius: 10; -fx-padding: 14 18;");
        warning.setWrapText(true);
        body.getChildren().add(warning);
        content.getChildren().addAll(header, body);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Confirmation");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-padding: 0;");
        dialog.getDialogPane().setPrefWidth(420);

        ButtonType btnSupprimerType = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnulerType   = new ButtonType("Annuler",   ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSupprimerType, btnAnnulerType);

        ((Button) dialog.getDialogPane().lookupButton(btnSupprimerType)).setStyle(
                "-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 9 28;");
        ((Button) dialog.getDialogPane().lookupButton(btnAnnulerType)).setStyle(
                "-fx-background-color: #f0f3f0; -fx-text-fill: #6a8a78; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-border-color: #d8e4da; -fx-border-radius: 10; -fx-border-width: 1; -fx-padding: 9 28;");

        dialog.setResultConverter(bt -> bt);
        dialog.showAndWait().ifPresent(result -> {
            if (result == btnSupprimerType) {
                if (commandeService.deleteCommande(commande.getIdCommande())) {
                    commandeList.remove(commande);
                    appliquerFiltre();
                    updateStats();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer la commande.");
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PROGRESS BAR
    // ══════════════════════════════════════════════════════════════════════════

    private HBox creerProgressBar(EtatCommande etat) {
        String[][] etapes = {{"⏳", "Attente"}, {"🔄", "En cours"}, {"✅", "Livrée"}};
        int activeIdx = switch (etat) {
            case en_attente -> 0;
            case en_cours   -> 1;
            case livree     -> 2;
            default         -> -1;
        };

        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER);

        if (etat == EtatCommande.annulee) {
            Label lbl = new Label("❌  Commande annulée");
            lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;" +
                    "-fx-background-color: #fdecea; -fx-background-radius: 8; -fx-padding: 6 14;");
            row.getChildren().add(lbl);
            return row;
        }

        for (int i = 0; i < etapes.length; i++) {
            boolean done    = i <= activeIdx;
            boolean current = i == activeIdx;

            VBox step = new VBox(4);
            step.setAlignment(Pos.CENTER);

            Label dot = new Label(etapes[i][0]);
            dot.setStyle(
                    "-fx-font-size: " + (current ? "18" : "14") + "px;" +
                            "-fx-background-color: " + (done ? GREEN_MAIN : "#e8eee9") + ";" +
                            "-fx-background-radius: 50;" +
                            "-fx-padding: " + (current ? "7 8" : "5 6") + ";" +
                            "-fx-opacity: " + (done ? "1" : "0.4") + ";");
            Label name = new Label(etapes[i][1]);
            name.setStyle(
                    "-fx-font-size: 9px; -fx-font-weight: " + (current ? "bold" : "normal") + ";" +
                            "-fx-text-fill: " + (done ? GREEN_MAIN : "#aab8b0") + ";");
            step.getChildren().addAll(dot, name);
            row.getChildren().add(step);

            if (i < etapes.length - 1) {
                Region line = new Region();
                line.setPrefWidth(40);
                line.setPrefHeight(2);
                line.setStyle("-fx-background-color: " + (i < activeIdx ? GREEN_MAIN : "#e8eee9") + ";");
                VBox wrap = new VBox(line);
                wrap.setAlignment(Pos.CENTER);
                wrap.setPadding(new Insets(0, 0, 16, 0));
                HBox.setHgrow(wrap, Priority.ALWAYS);
                row.getChildren().add(wrap);
            }
        }
        return row;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════════════════════════════════

    @FXML void rafraichir(ActionEvent event) { loadCommandes(); }

    @FXML
    void resetFiltre(ActionEvent event) {
        txtRecherche.clear();
        comboFiltreStatut.setValue("Tous");
        sliderMinPrix.setValue(sliderMinPrix.getMin());
        sliderMaxPrix.setValue(sliderMaxPrix.getMax());
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
        val.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + GREEN_DARK + ";");
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
                ? "-fx-background-color: white; -fx-background-radius: 18; -fx-border-color: " + GREEN_MAIN + "; -fx-border-radius: 18; -fx-border-width: 1.5; -fx-translate-y: -4; -fx-cursor: default;"
                : "-fx-background-color: white; -fx-background-radius: 18; -fx-border-color: " + BORDER + "; -fx-border-radius: 18; -fx-border-width: 1; -fx-cursor: default;";
    }

    private String[] getStatutInfo(EtatCommande etat) {
        if (etat == null) return new String[]{"❓", "Inconnu", "#f0f3f0", "#333"};
        return switch (etat) {
            case en_attente -> new String[]{"⏳", "En attente", "#fff8e1", "#b7860b"};
            case en_cours   -> new String[]{"🔄", "En cours",   "#e3f0ff", "#1a5faa"};
            case livree     -> new String[]{"✅", "Livrée",     "#e6f5ee", "#046436"};
            case annulee    -> new String[]{"❌", "Annulée",    "#fdecea", "#c0392b"};
        };
    }

    private String getNomClient(int idUser) {
        try {
            User client = userService.chercherParId(idUser);
            if (client == null) return "Client #" + idUser;
            String nom = (client.getPrenom() != null ? client.getPrenom() + " " : "") +
                    (client.getNom() != null ? client.getNom() : "");
            return nom.trim().isEmpty() ? client.getEmail() : nom.trim();
        } catch (Exception e) {
            return "Client #" + idUser;
        }
    }

    private Label labelSmall(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: " + color + "; -fx-letter-spacing: 1px;");
        return l;
    }

    private Label labelBold(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        return l;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}