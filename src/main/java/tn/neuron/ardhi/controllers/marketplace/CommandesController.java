package tn.neuron.ardhi.controllers.marketplace;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.BlurType;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import tn.neuron.ardhi.interfaces.marketplace.ICommandeService;
import tn.neuron.ardhi.models.marketplace.Commande;
import tn.neuron.ardhi.models.marketplace.EtatCommande;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.services.marketplace.CommandeService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CommandesController implements Initializable {

    // ── Stats ────────────────────────────────────────────────────────────────
    @FXML private Label lblTitre;
    @FXML private Label lblTotalCommandes;
    @FXML private Label lblTotalDepenses;

    // ── Sidebar filtres ──────────────────────────────────────────────────────
    @FXML private TextField  txtRechercheId;
    @FXML private ComboBox<String> comboFiltreStatut;
    @FXML private Slider     sliderMinPrix;
    @FXML private Slider     sliderMaxPrix;
    @FXML private Label      lblMinPrix;
    @FXML private Label      lblMaxPrix;

    // ── Cards container ──────────────────────────────────────────────────────
    @FXML private FlowPane commandesContainer;
    @FXML private VBox     emptyState;

    // ── State ────────────────────────────────────────────────────────────────
    private ICommandeService commandeService;
    private int userId;
    private List<Commande> toutesLesCommandes;

    // ── Palette ──────────────────────────────────────────────────────────────
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
        commandeService = new CommandeService();
        User currentUser = UserSession.getInstance().getUser();
        this.userId = currentUser.getId();
        lblTitre.setText("Commandes de " + currentUser.getPrenom() + " " + currentUser.getNom());

        initSidebar();
        chargerCommandes();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // INIT SIDEBAR
    // ══════════════════════════════════════════════════════════════════════════

    private void initSidebar() {
        // ComboBox statut
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

        // Listeners en temps réel
        txtRechercheId.textProperty().addListener((obs, o, n) -> appliquerFiltre());
        comboFiltreStatut.valueProperty().addListener((obs, o, n) -> appliquerFiltre());

        sliderMinPrix.valueProperty().addListener((obs, o, n) -> {
            // Empêcher min > max
            if (n.doubleValue() > sliderMaxPrix.getValue()) {
                sliderMinPrix.setValue(sliderMaxPrix.getValue());
            }
            lblMinPrix.setText(String.format("%.0f DT", sliderMinPrix.getValue()));
            appliquerFiltre();
        });

        sliderMaxPrix.valueProperty().addListener((obs, o, n) -> {
            // Empêcher max < min
            if (n.doubleValue() < sliderMinPrix.getValue()) {
                sliderMaxPrix.setValue(sliderMinPrix.getValue());
            }
            lblMaxPrix.setText(String.format("%.0f DT", sliderMaxPrix.getValue()));
            appliquerFiltre();
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CHARGEMENT
    // ══════════════════════════════════════════════════════════════════════════

    private void chargerCommandes() {
        try {
            toutesLesCommandes = commandeService.getCommandesByUser(userId);

            // Adapter les max sliders au max réel des commandes
            if (!toutesLesCommandes.isEmpty()) {
                double maxTotal = toutesLesCommandes.stream()
                        .mapToDouble(Commande::getTotal).max().orElse(9999);
                double plafond = Math.max(maxTotal, 100);
                sliderMinPrix.setMax(plafond);
                sliderMaxPrix.setMax(plafond);
                sliderMaxPrix.setValue(plafond);
                lblMaxPrix.setText(String.format("%.0f DT", plafond));
            }

            updateStats(toutesLesCommandes);
            appliquerFiltre();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les commandes : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FILTRAGE (Predicate combiné)
    // ══════════════════════════════════════════════════════════════════════════

    private void appliquerFiltre() {
        if (toutesLesCommandes == null) return;

        String  statut   = comboFiltreStatut.getValue();
        String  search   = txtRechercheId.getText() == null ? "" : txtRechercheId.getText().trim();
        double  minPrice = sliderMinPrix.getValue();
        double  maxPrice = sliderMaxPrix.getValue();

        Predicate<Commande> matchStatut = c ->
                statut == null || "Tous".equals(statut) ||
                        c.getEtat().toString().equals(statut);

        Predicate<Commande> matchTexte = c ->
                search.isEmpty() ||
                        String.valueOf(c.getIdCommande()).contains(search);

        Predicate<Commande> matchPrix = c ->
                c.getTotal() >= minPrice && c.getTotal() <= maxPrice;

        List<Commande> filtrees = toutesLesCommandes.stream()
                .filter(matchStatut.and(matchTexte).and(matchPrix))
                .collect(Collectors.toList());

        afficherCards(filtrees);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AFFICHAGE CARDS
    // ══════════════════════════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════════════════════════
    // CARD BUILDER
    // ══════════════════════════════════════════════════════════════════════════

    private VBox creerCard(Commande commande) {
        VBox card = new VBox(0);
        card.setPrefWidth(320);
        card.setMaxWidth(320);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 18;" +
                        "-fx-border-width: 1;" +
                        "-fx-cursor: hand;"
        );
        DropShadow shadow = new DropShadow(BlurType.GAUSSIAN, Color.rgb(13, 31, 21, 0.08), 18, 0, 0, 5);
        card.setEffect(shadow);

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: " + GREEN_MAIN + ";" +
                        "-fx-border-radius: 18;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-cursor: hand;" +
                        "-fx-translate-y: -4;" +
                        "-fx-effect: dropshadow(gaussian, rgba(4,100,54,0.22), 28, 0, 0, 10);"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 18;" +
                        "-fx-border-width: 1;" +
                        "-fx-cursor: hand;"
        ));

        // Header coloré selon statut
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 18, 14, 18));
        String[] statut = getStatutInfo(commande.getEtat());
        header.setStyle("-fx-background-color: " + statut[2] + "; -fx-background-radius: 18 18 0 0;");

        Label iconStatut = new Label(statut[0]);
        iconStatut.setStyle("-fx-font-size: 18px;");

        VBox statutBox = new VBox(2);
        Label lblStatut = new Label(statut[1]);
        lblStatut.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + statut[3] + ";");
        Label lblNumero = new Label("Commande  #" + commande.getIdCommande());
        lblNumero.setStyle("-fx-font-size: 11px; -fx-text-fill: " + statut[3] + "; -fx-opacity: 0.7;");
        statutBox.getChildren().addAll(lblStatut, lblNumero);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label totalBadge = new Label(String.format("%.2f DT", commande.getTotal()));
        totalBadge.setStyle(
                "-fx-background-color: rgba(255,255,255,0.30);" +
                        "-fx-text-fill: " + statut[3] + ";" +
                        "-fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 10; -fx-padding: 4 10 4 10;"
        );
        header.getChildren().addAll(iconStatut, statutBox, spacer, totalBadge);

        // Corps
        VBox body = new VBox(14);
        body.setPadding(new Insets(18, 18, 18, 18));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.FRENCH);
        HBox dateRow    = makeInfoRow("📅", "Date", commande.getDateCommande().format(fmt));
        HBox progressRow = creerProgressBar(commande.getEtat());

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #eef2ef;");

        Button btnDetails = new Button("📋   Voir les détails");
        btnDetails.setPrefWidth(284);
        btnDetails.setPrefHeight(40);
        String baseStyle  = "-fx-background-color: " + GRAY_BG + "; -fx-text-fill: " + GREEN_DARK + "; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-border-color: " + BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;";
        String hoverStyle = "-fx-background-color: " + GREEN_MAIN + "; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;";
        btnDetails.setStyle(baseStyle);
        btnDetails.setOnMouseEntered(e -> btnDetails.setStyle(hoverStyle));
        btnDetails.setOnMouseExited(e  -> btnDetails.setStyle(baseStyle));
        btnDetails.setOnAction(e -> afficherDetailsCommande(commande));

        body.getChildren().addAll(dateRow, progressRow, sep, btnDetails);
        card.getChildren().addAll(header, body);
        return card;
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
                            "-fx-opacity: " + (done ? "1" : "0.4") + ";"
            );
            Label name = new Label(etapes[i][1]);
            name.setStyle(
                    "-fx-font-size: 9px; -fx-font-weight: " + (current ? "bold" : "normal") + ";" +
                            "-fx-text-fill: " + (done ? GREEN_MAIN : "#aab8b0") + ";"
            );
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
    // STATS
    // ══════════════════════════════════════════════════════════════════════════

    private void updateStats(List<Commande> commandes) {
        if (lblTotalCommandes != null)
            lblTotalCommandes.setText(String.valueOf(commandes.size()));
        if (lblTotalDepenses != null) {
            float total = (float) commandes.stream()
                    .filter(c -> c.getEtat() != EtatCommande.annulee)
                    .mapToDouble(Commande::getTotal).sum();
            lblTotalDepenses.setText(String.format("%.2f DT", total));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DETAILS DIALOG
    // ══════════════════════════════════════════════════════════════════════════

    private void afficherDetailsCommande(Commande commande) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/marketplace/DetailsCommande.fxml"));
            Parent content = loader.load();
            DetailsCommandeController detailsController = loader.getController();
            detailsController.initCommande(commande);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Détails de la commande #" + commande.getIdCommande());
            dialog.setHeaderText(null);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().setStyle("-fx-background-color: white;");

            detailsController.setDialog(dialog);
            dialog.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir les détails de la commande.");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ACTIONS FXML
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    void clicTable(MouseEvent event) { }

    @FXML
    void rafraichir(ActionEvent event) {
        chargerCommandes();
    }

    @FXML
    void resetFiltre(ActionEvent event) {
        txtRechercheId.clear();
        comboFiltreStatut.setValue("Tous");
        sliderMinPrix.setValue(sliderMinPrix.getMin());
        sliderMaxPrix.setValue(sliderMaxPrix.getMax());
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/marketplace/Marketplace.fxml", "Ardhi - Marketplace");
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

    private String[] getStatutInfo(EtatCommande etat) {
        if (etat == null) return new String[]{"❓", "Inconnu", "#f0f3f0", "#333"};
        return switch (etat) {
            case en_attente -> new String[]{"⏳", "En attente", "#fff8e1", "#b7860b"};
            case en_cours   -> new String[]{"🔄", "En cours",   "#e3f0ff", "#1a5faa"};
            case livree     -> new String[]{"✅", "Livrée",     "#e6f5ee", "#046436"};
            case annulee    -> new String[]{"❌", "Annulée",    "#fdecea", "#c0392b"};
        };
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}