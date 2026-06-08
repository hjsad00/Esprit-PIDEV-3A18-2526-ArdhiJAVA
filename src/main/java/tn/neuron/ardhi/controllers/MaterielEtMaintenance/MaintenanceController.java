package tn.neuron.ardhi.controllers.MaterielEtMaintenance;

import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.neuron.ardhi.models.MaterielEtMaintenance.Materiel;
import tn.neuron.ardhi.services.MaterielEtMaintenance.MaterielService;
import tn.neuron.ardhi.utils.MaterielEtMaintenance.MaintenanceAlertUtils;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ResourceBundle;

public class MaintenanceController implements Initializable {

    // ── Header ────────────────────────────────────────────────
    @FXML private Label lblBienvenue;

    // ── Stats ─────────────────────────────────────────────────
    @FXML private Label lblMaintenancesPlanifiees;
    @FXML private Label lblMaintenancesEnRetard;
    @FXML private Label lblMaintenancesTerminees;

    // ── Timeline ──────────────────────────────────────────────
    @FXML private VBox timelineContainer;

    // ── Tableau urgents ───────────────────────────────────────
    @FXML private TableView<Materiel>           tableMaterielsMaintenanceUrgente;
    @FXML private TableColumn<Materiel, String> colNomMateriel;
    @FXML private TableColumn<Materiel, String> colTypeMateriel;
    @FXML private TableColumn<Materiel, String> colStatutMaintenance;
    @FXML private Label                         lblNombreUrgents;

    // ── Chatbot ───────────────────────────────────────────────
    @FXML private ScrollPane scrollChatbot;
    @FXML private VBox       vboxConversation;
    @FXML private TextField  txtChatInput;
    @FXML private Button     btnChatEnvoyer;

    // ── Services ──────────────────────────────────────────────
    private final MaterielService materielService = new MaterielService();
    private ChatbotController chatbot;

    // ── Couleurs palette ARDHI ────────────────────────────────
    private static final String C_VERT_FONCE  = "#203e24";
    private static final String C_OLIVE       = "#6B7F3F";
    private static final String C_OLIVE_MED   = "#7A9148";
    private static final String C_VERT_CLAIR  = "#C8D9A0";

    // ── Étapes Aramex ─────────────────────────────────────────
    // { label, emoji, couleur }
    private static final String[][] ETAPES = {
            { "Planifié",    "📅", "#5b9cf6" },
            { "En attente",  "⏳", "#f6a623" },
            { "En cours",    "🔧", "#c27fe8" },
            { "Contrôle",    "🔍", "#5cc8a8" },
            { "Terminé",     "✅", "#6ab87a" }
    };

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ════════════════════════════════════════════════════════
    //  INITIALIZE
    // ════════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        int userId = UserSession.getInstance().getUser().getId();

        if (lblBienvenue != null) {
            String nom = UserSession.getInstance().getUser().getNom();
            lblBienvenue.setText("Bienvenue, " + nom + " — Suivi de vos maintenances");
        }

        if (tableMaterielsMaintenanceUrgente != null) {
            initialiserTableau();
            chargerDonnees(userId);
        }

        // Chatbot si présent
        if (vboxConversation != null && txtChatInput != null && btnChatEnvoyer != null) {
            chatbot = new ChatbotController();
            chatbot.initialiser(scrollChatbot, vboxConversation, txtChatInput, btnChatEnvoyer, userId);
            chatbot.setOnPlanifierMaintenance(() -> {
                try {
                    Parent root = FXMLLoader.load(
                            getClass().getResource("/fxml/MaterielEtMaintenance/Materiels.fxml"));
                    Stage stage = (Stage) txtChatInput.getScene().getWindow();
                    stage.setScene(new Scene(root));
                } catch (Exception e) { e.printStackTrace(); }
            });
        }
    }

    // ════════════════════════════════════════════════════════
    //  CHARGEMENT DONNÉES
    // ════════════════════════════════════════════════════════

    private void chargerDonnees(int userId) {
        List<Materiel> tous    = materielService.getMaterielsByUserId(userId);
        List<Materiel> urgents = MaintenanceAlertUtils.filterMaterielsNeedingMaintenance(tous);

        tableMaterielsMaintenanceUrgente.setItems(FXCollections.observableArrayList(urgents));

        long planifiees = tous.stream()
                .filter(m -> m.getDate_prochaine_maintenance() != null
                        && m.getDate_prochaine_maintenance().isAfter(LocalDate.now()))
                .count();
        long retard = urgents.stream()
                .filter(m -> "URGENT".equals(MaintenanceAlertUtils.getMaintenanceUrgencyLevel(m)))
                .count();

        if (lblMaintenancesPlanifiees != null) lblMaintenancesPlanifiees.setText(String.valueOf(planifiees));
        if (lblMaintenancesEnRetard   != null) lblMaintenancesEnRetard.setText(String.valueOf(retard));
        if (lblMaintenancesTerminees  != null) lblMaintenancesTerminees.setText(String.valueOf(tous.size() - urgents.size()));
        if (lblNombreUrgents          != null) lblNombreUrgents.setText(urgents.isEmpty() ? "" : urgents.size() + " matériel(s)");

        // ── TIMELINE ARAMEX ──
        if (timelineContainer != null) {
            timelineContainer.getChildren().clear();
            for (Materiel m : tous) {
                if (m.getDate_prochaine_maintenance() == null) continue;
                timelineContainer.getChildren().add(creerCarteAramex(m));
            }
            if (timelineContainer.getChildren().isEmpty()) {
                Label vide = new Label("Aucun matériel avec date de maintenance planifiée.");
                vide.setStyle("-fx-text-fill: #999; -fx-font-size: 12px; -fx-padding: 14;");
                timelineContainer.getChildren().add(vide);
            }
        }
    }

    // ════════════════════════════════════════════════════════
    //  CARTE ARAMEX — 1 carte par matériel
    // ════════════════════════════════════════════════════════

    private VBox creerCarteAramex(Materiel m) {
        int etape = calculerEtape(m);

        VBox carte = new VBox(12);
        carte.setPadding(new Insets(16, 18, 16, 18));
        carte.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: " + C_VERT_CLAIR + ";" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);"
        );

        // ── Ligne 1 : nom + type + badge statut + date ────
        HBox entete = new HBox(12);
        entete.setAlignment(Pos.CENTER_LEFT);

        // Bande couleur à gauche
        String couleurEtape = getCouleurEtape(etape);
        Region bande = new Region();
        bande.setPrefWidth(4);
        bande.setMinHeight(36);
        bande.setStyle("-fx-background-color: " + couleurEtape + "; -fx-background-radius: 4;");

        // Nom matériel
        Label nom = new Label(m.getNom());
        nom.setFont(Font.font("System", FontWeight.BOLD, 14));
        nom.setTextFill(Color.web("#1a2a1a"));
        nom.setMinWidth(140);

        // Type
        Label type = new Label(m.getType());
        type.setStyle("-fx-font-size: 12px; -fx-text-fill: #7a8c6a;");
        type.setMinWidth(100);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Badge statut
        Label badge = creerBadge(etape, m);

        // Date
        Label date = new Label(m.getDate_prochaine_maintenance().format(FMT));
        date.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + couleurEtape + ";" +
                        "-fx-background-color: " + couleurEtape + "22;" +
                        "-fx-background-radius: 8; -fx-padding: 4 12;"
        );

        entete.getChildren().addAll(bande, nom, type, spacer, badge, date);

        // ── Ligne 2 : les 5 étapes Aramex ────────────────
        HBox ligneEtapes = creerLigneEtapes(etape);

        // ── Ligne 3 : message statut ──────────────────────
        HBox messageBox = creerMessageStatut(etape, m);

        carte.getChildren().addAll(entete, ligneEtapes, messageBox);
        animerCarteEntree(carte);
        return carte;
    }

    // ── Badge coloré ──────────────────────────────────────────
    private Label creerBadge(int etape, Materiel m) {
        String texte = switch (etape) {
            case -1 -> {
                long r = (m.getDate_prochaine_maintenance() != null)
                        ? ChronoUnit.DAYS.between(m.getDate_prochaine_maintenance(), LocalDate.now()) : 0;
                yield "⚠ EN RETARD " + r + "j";
            }
            case 0 -> "📅 PLANIFIÉ";
            case 1 -> "⏳ EN ATTENTE";
            case 2 -> "🔧 EN COURS";
            case 3 -> "🔍 CONTRÔLE";
            case 4 -> "✅ TERMINÉ";
            default -> "—";
        };
        String couleur = getCouleurEtape(etape);
        Label badge = new Label(texte);
        badge.setStyle(
                "-fx-background-color: " + couleur + ";" +
                        "-fx-background-radius: 20; -fx-text-fill: white;" +
                        "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 12;"
        );
        if (etape == -1) animerPulsation(badge);
        return badge;
    }

    // ── Ligne des 5 nœuds Aramex ─────────────────────────────
    private HBox creerLigneEtapes(int etapeActive) {
        HBox ligne = new HBox(0);
        ligne.setAlignment(Pos.CENTER);
        ligne.setPadding(new Insets(4, 0, 4, 0));

        for (int i = 0; i < ETAPES.length; i++) {
            ligne.getChildren().add(creerNoeud(i, etapeActive));
            if (i < ETAPES.length - 1) {
                ligne.getChildren().add(creerConnecteur(i, etapeActive));
            }
        }
        return ligne;
    }

    private VBox creerNoeud(int i, int etapeActive) {
        boolean enRetard  = (etapeActive == -1);
        boolean estPassee = (!enRetard && i < etapeActive);
        boolean estActive = (!enRetard && i == etapeActive) || (enRetard && i == 0);
        boolean estFuture = (!estPassee && !estActive);

        String couleur;
        if (enRetard)        couleur = (i == 0) ? "#e53e3e" : "#bbb";
        else if (estPassee)  couleur = ETAPES[i][2];
        else if (estActive)  couleur = ETAPES[i][2];
        else                 couleur = "#ddd";

        // Stack : anneau + cercle + icône
        StackPane stack = new StackPane();
        stack.setMinWidth(72);
        stack.setAlignment(Pos.CENTER);

        // Anneau animé pour étape active
        if (estActive) {
            Circle anneau = new Circle(26);
            anneau.setFill(Color.TRANSPARENT);
            anneau.setStroke(Color.web(couleur));
            anneau.setStrokeWidth(1.5);
            anneau.setOpacity(0.3);
            stack.getChildren().add(anneau);
            animerAnneau(anneau);
        }

        // Cercle principal
        double r = estActive ? 22 : 18;
        Circle cercle = new Circle(r);
        if (estFuture) {
            cercle.setFill(Color.web("#f5f3ee"));
            cercle.setStroke(Color.web("#ccc"));
            cercle.setStrokeWidth(1.5);
        } else {
            cercle.setFill(Color.web(couleur));
        }

        // Icône
        String emoji = (enRetard && i == 0) ? "⚠️" : (estPassee ? "✓" : ETAPES[i][1]);
        Label icone = new Label(emoji);
        icone.setStyle(
                "-fx-font-size: " + (estActive ? "15" : "12") + "px;" +
                        (estPassee ? "-fx-text-fill: white; -fx-font-weight: bold;" : "")
        );

        stack.getChildren().addAll(cercle, icone);

        // Texte sous le cercle
        Label texte = new Label(ETAPES[i][0]);
        texte.setMaxWidth(70);
        texte.setWrapText(true);
        texte.setTextAlignment(TextAlignment.CENTER);
        texte.setAlignment(Pos.CENTER);
        texte.setStyle(
                "-fx-font-size: 9px;" +
                        "-fx-font-weight: " + (estActive ? "bold" : "normal") + ";" +
                        "-fx-text-fill: " + (estFuture ? "#bbb" : couleur) + ";" +
                        "-fx-text-alignment: center;"
        );

        VBox node = new VBox(5);
        node.setAlignment(Pos.CENTER);
        node.getChildren().addAll(stack, texte);

        // Apparition en fondu décalé
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(280), node);
        ft.setDelay(Duration.millis(180 + i * 90L));
        ft.setFromValue(0); ft.setToValue(1);
        ft.play();

        return node;
    }

    private StackPane creerConnecteur(int i, int etapeActive) {
        boolean complet = (etapeActive > 0 && i < etapeActive);
        Rectangle rect = new Rectangle(30, 3);
        rect.setArcWidth(3); rect.setArcHeight(3);
        rect.setFill(Color.web(complet ? "#6ab87a" : "#ddd"));
        rect.setOpacity(complet ? 1.0 : 0.5);
        StackPane sp = new StackPane(rect);
        sp.setAlignment(Pos.CENTER);
        HBox.setHgrow(sp, Priority.ALWAYS);
        return sp;
    }

    // ── Message statut en bas ─────────────────────────────────
    private HBox creerMessageStatut(int etape, Materiel m) {
        LocalDate dateMaint = m.getDate_prochaine_maintenance();
        long j = (dateMaint != null) ? ChronoUnit.DAYS.between(LocalDate.now(), dateMaint) : 0;

        String message, bg;
        switch (etape) {
            case -1 -> {
                long retard = Math.abs(j);
                message = "⚠️  En retard de " + retard + " jour(s). Veuillez reprogrammer cette maintenance.";
                bg = "#fff0f0";
            }
            case 0  -> { message = "📅  Planifiée dans " + j + " jours (" + (dateMaint != null ? dateMaint.format(FMT) : "") + ")."; bg = "#f0f5e8"; }
            case 1  -> { message = "⏳  Échéance dans " + j + " jours. Préparez votre matériel !"; bg = "#fffbf0"; }
            case 2  -> { message = "🔧  Intervention prévue aujourd'hui."; bg = "#f8f0ff"; }
            case 3  -> { message = "🔍  Contrôle qualité post-intervention en cours."; bg = "#f0faf5"; }
            case 4  -> { message = "✅  Maintenance terminée avec succès !"; bg = "#eef6ee"; }
            default -> { message = ""; bg = "#f5f3ee"; }
        }

        Label msg = new Label(message);
        msg.setWrapText(true);
        msg.setStyle("-fx-font-size: 11px; -fx-text-fill: #3a5a2a;");
        HBox.setHgrow(msg, Priority.ALWAYS);

        HBox box = new HBox(msg);
        box.setPadding(new Insets(9, 14, 9, 14));
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 8;");
        return box;
    }

    // ── Calcul étape selon date ───────────────────────────────
    private int calculerEtape(Materiel m) {
        LocalDate date = m.getDate_prochaine_maintenance();
        if (date == null) return 0;
        long j = ChronoUnit.DAYS.between(LocalDate.now(), date);
        if (j > 30)  return 0;   // Planifié
        if (j > 0)   return 1;   // En attente
        if (j == 0)  return 2;   // En cours
        if (j >= -3) return 3;   // Contrôle
        if (j >= -7) return 4;   // Terminé
        return -1;                // EN RETARD
    }

    private String getCouleurEtape(int etape) {
        return switch (etape) {
            case -1 -> "#e53e3e";
            case  0 -> "#5b9cf6";
            case  1 -> "#f6a623";
            case  2 -> "#c27fe8";
            case  3 -> "#5cc8a8";
            case  4 -> "#6ab87a";
            default  -> "#aaa";
        };
    }

    // ════════════════════════════════════════════════════════
    //  TABLEAU MATÉRIELS URGENTS
    // ════════════════════════════════════════════════════════

    private void initialiserTableau() {
        colNomMateriel.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colTypeMateriel.setCellValueFactory(new PropertyValueFactory<>("type"));

        colStatutMaintenance.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableView().getItems().size() <= getIndex()) {
                    setText(null); setStyle(""); return;
                }
                Materiel mx = getTableView().getItems().get(getIndex());
                setText(MaintenanceAlertUtils.getMaintenanceMessage(mx));
                switch (MaintenanceAlertUtils.getMaintenanceUrgencyLevel(mx)) {
                    case "URGENT"        -> setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold; -fx-background-color: #ffcdd2;");
                    case "CETTE_SEMAINE" -> setStyle("-fx-text-fill: #e65100; -fx-font-weight: bold; -fx-background-color: #fff3e0;");
                    case "CE_MOIS"       -> setStyle("-fx-text-fill: #6B7F3F; -fx-background-color: #f0f5e8;");
                    case "BIENTOT"       -> setStyle("-fx-text-fill: #7A9148; -fx-background-color: #eef4e0;");
                    default              -> setStyle("-fx-text-fill: #203e24; -fx-background-color: #e8f5e9;");
                }
            }
        });
    }

    // ════════════════════════════════════════════════════════
    //  BOUTON UNIQUE — ouvre fenêtre onglets
    // ════════════════════════════════════════════════════════

    @FXML
    private void handleOuvrirHistoriqueRapports() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/fxml/MaterielEtMaintenance/historique-maintenance.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 1300, 900));
            stage.setTitle("Historique & Rapports — ARDHI");
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la fenêtre.");
        }
    }

    // ════════════════════════════════════════════════════════
    //  IA & NAVIGATION
    // ════════════════════════════════════════════════════════

    @FXML private void handleOuvrirIA() { ChatbotIAController.ouvrir(); }

    @FXML private void handleChatEnvoyer()        { if (chatbot != null) chatbot.envoyerMessage(); }
    @FXML private void handleQuestionBruit()      { if (chatbot != null) chatbot.poserQuestionRapide("Mon tracteur fait un bruit bizarre au démarrage"); }
    @FXML private void handleQuestionDemarrage()  { if (chatbot != null) chatbot.poserQuestionRapide("Mon matériel ne démarre plus"); }
    @FXML private void handleQuestionSurchauffe() { if (chatbot != null) chatbot.poserQuestionRapide("Mon moteur surchauffe, que faire ?"); }
    @FXML private void handleQuestionFuite()      { if (chatbot != null) chatbot.poserQuestionRapide("Il y a une fuite d'huile sur mon matériel"); }
    @FXML private void handleQuestionCout()       { if (chatbot != null) chatbot.poserQuestionRapide("Quel est le coût moyen de maintenance d'un tracteur ?"); }
    @FXML private void handleQuestionEntretien()  { if (chatbot != null) chatbot.poserQuestionRapide("Quelles sont les bonnes pratiques d'entretien préventif ?"); }

    @FXML
    private void handleRetour() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/fxml/MaterielEtMaintenance/MaterielsEtMaintenance.fxml"));
            Stage stage = (Stage) tableMaterielsMaintenanceUrgente.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    // ════════════════════════════════════════════════════════
    //  ANIMATIONS
    // ════════════════════════════════════════════════════════

    private void animerCarteEntree(VBox carte) {
        carte.setOpacity(0);
        carte.setTranslateY(10);
        FadeTransition fade  = new FadeTransition(Duration.millis(320), carte);
        fade.setFromValue(0); fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(320), carte);
        slide.setFromY(10); slide.setToY(0);
        new ParallelTransition(fade, slide).play();
    }

    private void animerAnneau(Circle anneau) {
        ScaleTransition sc = new ScaleTransition(Duration.millis(900), anneau);
        sc.setFromX(0.7); sc.setFromY(0.7); sc.setToX(1.4); sc.setToY(1.4);
        sc.setCycleCount(Animation.INDEFINITE); sc.setAutoReverse(true);
        FadeTransition ft = new FadeTransition(Duration.millis(900), anneau);
        ft.setFromValue(0.45); ft.setToValue(0.05);
        ft.setCycleCount(Animation.INDEFINITE); ft.setAutoReverse(true);
        new ParallelTransition(sc, ft).play();
    }

    private void animerPulsation(Label badge) {
        FadeTransition ft = new FadeTransition(Duration.millis(600), badge);
        ft.setFromValue(1.0); ft.setToValue(0.4);
        ft.setCycleCount(Animation.INDEFINITE); ft.setAutoReverse(true);
        ft.play();
    }
}
