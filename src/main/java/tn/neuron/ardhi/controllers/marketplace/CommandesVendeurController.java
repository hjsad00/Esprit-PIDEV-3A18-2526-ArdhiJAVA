package tn.neuron.ardhi.controllers.marketplace;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import tn.neuron.ardhi.interfaces.marketplace.ICommandeService;
import tn.neuron.ardhi.models.marketplace.Commande;
import tn.neuron.ardhi.models.marketplace.EtatCommande;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.services.marketplace.CommandeService;
import tn.neuron.ardhi.services.UserAndDiag.UserService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class CommandesVendeurController implements Initializable {

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private Label lblTitre;
    @FXML private Label lblTotalCommandes;
    @FXML private Label lblEnAttente;
    @FXML private Label lblTotalEncaisse;
    @FXML private FlowPane commandesContainer;
    @FXML private VBox emptyState;

    private ICommandeService commandeService;
    private UserService userService;
    private int vendeurId;
    private List<Commande> commandes;

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final String GREEN_DARK = "#2b5329";
    private static final String GREEN_MAIN = "#4a7c47";
    private static final String BORDER     = "#e0e8e2";
    private static final String GRAY_BG    = "#f8faf8";
    private static final String GRAY_TEXT  = "#6a8a78";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (UserSession.getInstance() == null || UserSession.getInstance().getUser() == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur de session", "Aucun utilisateur connecté.");
            return;
        }
        User currentUser = UserSession.getInstance().getUser();
        this.vendeurId = currentUser.getId();
        commandeService = new CommandeService();
        userService = new UserService();

        lblTitre.setText("Commandes de " + currentUser.getPrenom() + " " + currentUser.getNom());
        chargerCommandes();
    }

    // ── Chargement ───────────────────────────────────────────────────────────
    private void chargerCommandes() {
        try {
            commandes = commandeService.getCommandesByVendeur(vendeurId);
            commandesContainer.getChildren().clear();

            if (commandes.isEmpty()) {
                emptyState.setVisible(true);
                emptyState.setManaged(true);
                commandesContainer.setVisible(false);
                commandesContainer.setManaged(false);
            } else {
                emptyState.setVisible(false);
                emptyState.setManaged(false);
                commandesContainer.setVisible(true);
                commandesContainer.setManaged(true);
                for (Commande c : commandes) {
                    commandesContainer.getChildren().add(creerCard(c));
                }
            }
            updateStats();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les commandes : " + e.getMessage());
        }
    }

    // ── Card builder ─────────────────────────────────────────────────────────
    private VBox creerCard(Commande commande) {
        VBox card = new VBox(0);
        card.setPrefWidth(340);
        card.setMaxWidth(340);
        card.setStyle(cardStyle(false));
        DropShadow shadow = new DropShadow(BlurType.GAUSSIAN, Color.rgb(13,31,21,0.08), 18, 0, 0, 5);
        card.setEffect(shadow);
        card.setOnMouseEntered(e -> card.setStyle(cardStyle(true)));
        card.setOnMouseExited(e -> card.setStyle(cardStyle(false)));

        // ── Header coloré statut ──────────────────────────────────────────
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
                "-fx-background-color: rgba(255,255,255,0.30);" +
                        "-fx-text-fill: " + statut[3] + ";" +
                        "-fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 10; -fx-padding: 4 10 4 10;"
        );
        header.getChildren().addAll(iconStatut, statutBox, spacer, totalBadge);

        // ── Corps ────────────────────────────────────────────────────────
        VBox body = new VBox(12);
        body.setPadding(new Insets(16, 18, 18, 18));

        // Date
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);
        HBox dateRow = makeInfoRow("📅", "Date", commande.getDateCommande().format(fmt));

        // Client
        String nomClient = getNomClient(commande.getIdUser());
        HBox clientRow = makeInfoRow("👤", "Client", nomClient);

        // Progress bar
        HBox progressRow = creerProgressBar(commande.getEtat());

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #eef2ef;");

        // ── Boutons d'action ─────────────────────────────────────────────
        VBox actionsBox = new VBox(8);

        // Bouton détails (toujours visible)
        Button btnDetails = makeBtn("📋   Voir les détails", GRAY_BG, GREEN_DARK, BORDER, GREEN_MAIN, "white");
        btnDetails.setOnAction(e -> afficherDetailsCommande(commande));

        actionsBox.getChildren().add(btnDetails);

        // Accepter / Annuler uniquement si en_attente
        if (commande.getEtat() == EtatCommande.en_attente) {
            HBox actionRow = new HBox(8);
            actionRow.setAlignment(Pos.CENTER);

            Button btnAccepter = makeBtn("✅   Accepter", "#e6f5ee", GREEN_MAIN, "#b8dfc8", "#034e2b", "white");
            btnAccepter.setPrefWidth(130);
            btnAccepter.setOnAction(e -> changerStatut(commande, EtatCommande.en_cours, card));

            Button btnAnnuler = makeBtn("❌   Refuser", "#fdecea", "#c0392b", "#f5b7b1", "#922b21", "white");
            btnAnnuler.setPrefWidth(130);
            btnAnnuler.setOnAction(e -> changerStatut(commande, EtatCommande.annulee, card));

            HBox.setHgrow(btnAccepter, Priority.ALWAYS);
            HBox.setHgrow(btnAnnuler, Priority.ALWAYS);
            actionRow.getChildren().addAll(btnAccepter, btnAnnuler);
            actionsBox.getChildren().add(actionRow);
        }

        body.getChildren().addAll(dateRow, clientRow, progressRow, sep, actionsBox);
        card.getChildren().addAll(header, body);
        return card;
    }

    // ── Changer statut + refresh card ────────────────────────────────────────
    private void changerStatut(Commande commande, EtatCommande nouveauStatut, VBox card) {
        String action = (nouveauStatut == EtatCommande.en_cours) ? "accepter" : "refuser";
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Voulez-vous vraiment " + action + " la commande #" + commande.getIdCommande() + " ?");

        confirmation.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                boolean success = commandeService.updateStatutCommande(commande.getIdCommande(), nouveauStatut);
                if (success) {
                    commande.setEtat(nouveauStatut);
                    // Remplacer la card par une nouvelle version mise à jour
                    int idx = commandesContainer.getChildren().indexOf(card);
                    if (idx >= 0) {
                        commandesContainer.getChildren().set(idx, creerCard(commande));
                    }
                    updateStats();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre à jour le statut.");
                }
            }
        });
    }

    // ── Progress bar ─────────────────────────────────────────────────────────
    private HBox creerProgressBar(EtatCommande etat) {
        String[][] etapes = {{"⏳","Attente"}, {"🔄","En cours"}, {"✅","Livrée"}};
        int activeIdx = switch (etat) {
            case en_attente -> 0;
            case en_cours   -> 1;
            case livree     -> 2;
            default         -> -1;
        };

        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER);

        if (etat == EtatCommande.annulee) {
            Label lbl = new Label("❌  Commande refusée");
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

    // ── Helpers ──────────────────────────────────────────────────────────────
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
        btn.setPrefWidth(304);
        btn.setPrefHeight(38);
        String base = "-fx-background-color: " + bg + "; -fx-text-fill: " + fg + ";" +
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;" +
                "-fx-border-color: " + border + "; -fx-border-radius: 10; -fx-border-width: 1;";
        String hover = "-fx-background-color: " + hoverBg + "; -fx-text-fill: " + hoverFg + ";" +
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private String cardStyle(boolean hovered) {
        return hovered
                ? "-fx-background-color: white; -fx-background-radius: 18; -fx-border-color: " + GREEN_MAIN + "; -fx-border-radius: 18; -fx-border-width: 1.5; -fx-translate-y: -4; -fx-effect: dropshadow(gaussian, rgba(4,100,54,0.22), 28, 0, 0, 10); -fx-cursor: default;"
                : "-fx-background-color: white; -fx-background-radius: 18; -fx-border-color: " + BORDER + "; -fx-border-radius: 18; -fx-border-width: 1; -fx-cursor: default;";
    }

    private String[] getStatutInfo(EtatCommande etat) {
        if (etat == null) return new String[]{"❓", "Inconnu", "#f0f3f0", "#333"};
        return switch (etat) {
            case en_attente -> new String[]{"⏳", "En attente",  "#fff8e1", "#b7860b"};
            case en_cours   -> new String[]{"🔄", "En cours",    "#e3f0ff", "#1a5faa"};
            case livree     -> new String[]{"✅", "Livrée",      "#e6f5ee", "#046436"};
            case annulee    -> new String[]{"❌", "Refusée",     "#fdecea", "#c0392b"};
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

    private void updateStats() {
        if (commandes == null) return;
        long enAttente = commandes.stream().filter(c -> c.getEtat() == EtatCommande.en_attente).count();
        double total = commandes.stream()
                .filter(c -> c.getEtat() != EtatCommande.annulee)
                .mapToDouble(Commande::getTotal).sum();

        if (lblTotalCommandes != null) lblTotalCommandes.setText(String.valueOf(commandes.size()));
        if (lblEnAttente != null)      lblEnAttente.setText(String.valueOf(enAttente));
        if (lblTotalEncaisse != null)  lblTotalEncaisse.setText(String.format("%.2f DT", total));
    }

    private void afficherDetailsCommande(Commande commande) {
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
            dialog.showAndWait();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir les détails.");
        }
    }

    @FXML
    private void rafraichir(ActionEvent event) {
        chargerCommandes();
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/marketplace/Marketplace.fxml", "Ardhi - Marketplace");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}