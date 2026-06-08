package tn.neuron.ardhi.controllers.MaterielEtMaintenance;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import tn.neuron.ardhi.services.MaterielEtMaintenance.GrokService;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ChatbotController v4 — Propulsé par l'API Groq directement depuis Java.
 * Plus besoin de serveur Python. IaService remplacé par GrokService.
 *
 * Conserve toute la logique contextuelle (mémoire de conversation,
 * thèmes, réponses de suivi) de la v3.
 */
public class ChatbotController {

    // ── UI ──────────────────────────────────────────────────────
    private ScrollPane  scrollPane;
    private VBox        vboxConversation;
    private TextField   txtInput;
    private Button      btnEnvoyer;
    private HBox        hboxSuggestions;
    private Label       lblMsgCount;

    // ── Métier ──────────────────────────────────────────────────
    private int         userId;
    private GrokService grokService;          // ← Remplace IaService
    private Runnable    onPlanifierMaintenance;

    // ── Mémoire conversationnelle ────────────────────────────────
    private final List<ConvEntry> historique   = new ArrayList<>();
    private final Set<String>     themesActifs = new LinkedHashSet<>();
    private int    nbEchanges       = 0;
    private String dernierThemeBot  = null;
    private String derniereQuestionBot = null;

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private record ConvEntry(String role, String texte) {}

    // ══════════════════════════════════════════════════════════════
    //  INITIALISATION
    // ══════════════════════════════════════════════════════════════

    public void initialiser(ScrollPane scrollPane, VBox vboxConversation,
                            TextField txtInput, Button btnEnvoyer, int userId) {
        this.scrollPane       = scrollPane;
        this.vboxConversation = vboxConversation;
        this.txtInput         = txtInput;
        this.btnEnvoyer       = btnEnvoyer;
        this.userId           = userId;
        this.grokService      = new GrokService();   // ← Groq API directe

        if (txtInput != null) txtInput.setOnAction(e -> envoyerMessage());
        Platform.runLater(this::afficherMessageBienvenue);
    }

    public void setHboxSuggestions(HBox hboxSuggestions) { this.hboxSuggestions = hboxSuggestions; }
    public void setLblMsgCount(Label lblMsgCount)         { this.lblMsgCount = lblMsgCount; }
    public void setOnPlanifierMaintenance(Runnable cb)    { this.onPlanifierMaintenance = cb; }

    // ══════════════════════════════════════════════════════════════
    //  BIENVENUE
    // ══════════════════════════════════════════════════════════════

    private void afficherMessageBienvenue() {
        vboxConversation.getChildren().clear();
        historique.clear();
        themesActifs.clear();
        nbEchanges          = 0;
        dernierThemeBot     = null;
        derniereQuestionBot = null;
        mettreAJourCompteur();

        ajouterBulleIA(
                "👋 Bonjour ! Je suis ARDHI Assistant IA, votre expert en maintenance du matériel agricole.\n\n" +
                        "Je m'adapte à votre situation au fil de la conversation. Vous pouvez me décrire :\n" +
                        "• Un symptôme précis (bruit, panne, fuite…)\n" +
                        "• Une situation d'urgence → je vous guide immédiatement\n" +
                        "• Une question de planning ou de budget\n\n" +
                        "Pour un meilleur diagnostic, précisez : le type de matériel, quand le problème est apparu, et si d'autres symptômes accompagnent.",
                "NORMAL", false,
                List.of("Bruit bizarre", "Ne démarre pas", "Fuite détectée", "Planning saisonnier"));
    }

    public void reinitialiser() {
        Platform.runLater(this::afficherMessageBienvenue);
    }

    // ══════════════════════════════════════════════════════════════
    //  ENVOI MESSAGE
    // ══════════════════════════════════════════════════════════════

    public void envoyerMessage() {
        if (txtInput == null) return;
        String message = txtInput.getText().trim();
        if (message.isEmpty()) return;

        ajouterBulleUtilisateur(message);
        historique.add(new ConvEntry("user", message));
        txtInput.clear();
        cacherSuggestions();

        Label typing = creerIndicateurTyping();
        vboxConversation.getChildren().add(typing);
        scrollToBottom();
        if (btnEnvoyer != null) btnEnvoyer.setDisable(true);

        String historiqueTexte = buildHistoriqueTexte();

        Task<Map<String, Object>> task = new Task<>() {
            @Override
            protected Map<String, Object> call() {
                // Appel direct à l'API Groq — pas de serveur Python
                return grokService.envoyerMessage(message, historiqueTexte);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            vboxConversation.getChildren().remove(typing);
            Map<String, Object> res = task.getValue();
            traiterEtAfficherReponse(res);
            if (btnEnvoyer != null) btnEnvoyer.setDisable(false);
            scrollToBottom();
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            vboxConversation.getChildren().remove(typing);
            Map<String, Object> err = new HashMap<>();
            err.put("texte",       "⚠️ Erreur de connexion. Vérifiez votre connexion internet et réessayez.");
            err.put("gravite",     "FAIBLE");
            err.put("planifier",   false);
            err.put("suggestions", List.of("Bruit bizarre", "Ne démarre pas", "Fuite détectée"));
            traiterEtAfficherReponse(err);
            if (btnEnvoyer != null) btnEnvoyer.setDisable(false);
            scrollToBottom();
        }));

        new Thread(task, "groq-api-thread").start();
    }

    // ══════════════════════════════════════════════════════════════
    //  TRAITEMENT ET AFFICHAGE DE LA RÉPONSE
    // ══════════════════════════════════════════════════════════════

    private void traiterEtAfficherReponse(Map<String, Object> reponse) {
        // Normaliser les clés (GrokService retourne "texte"/"gravite"/"planifier")
        String texte = getString(reponse, "texte", "reponse", "response");
        String graviteRaw = getString(reponse, "gravite", "severity", "");
        boolean planif = getBool(reponse, "planifier", "suggest_maintenance");

        @SuppressWarnings("unchecked")
        List<String> suggestions = (List<String>) reponse.getOrDefault("suggestions",
                reponse.getOrDefault("actions", Collections.emptyList()));

        String gravite = normaliserGravite(graviteRaw);

        // Nettoyer le markdown que le LLM pourrait quand même envoyer
        texte = nettoyerMarkdown(texte);

        // Mémoriser le thème depuis la réponse
        String theme = (String) reponse.getOrDefault("theme", "");
        if (!theme.isEmpty()) {
            themesActifs.add(theme);
            dernierThemeBot = theme;
        }

        // Suffixe contextuel après plusieurs échanges
        String suffix = construireSuffixeContextuel(gravite);
        if (!suffix.isEmpty()) texte = texte + "\n\n" + suffix;

        historique.add(new ConvEntry("bot", texte));
        nbEchanges++;
        mettreAJourCompteur();

        ajouterBulleIA(texte, gravite, planif,
                suggestions.isEmpty() ? null : suggestions);
        afficherSuggestions(suggestions);
    }

    public void poserQuestionRapide(String question) {
        if (txtInput != null) { txtInput.setText(question); envoyerMessage(); }
    }

    // ══════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ══════════════════════════════════════════════════════════════

    /** Nettoie le markdown non supporté par JavaFX Label */
    private String nettoyerMarkdown(String texte) {
        if (texte == null) return "";
        return texte
                .replaceAll("\\*\\*([^*]+)\\*\\*", "$1")
                .replaceAll("\\*([^*]+)\\*",         "$1")
                .replaceAll("#{1,3}\\s*",             "")
                .replaceAll("`([^`]+)`",              "$1");
    }

    private String normaliserGravite(String g) {
        if (g == null) return "NORMAL";
        return switch (g.toUpperCase().trim()) {
            case "CRITIQUE", "CRITICAL"        -> "CRITIQUE";
            case "ELEVE", "HIGH"               -> "ELEVE";
            case "MOYEN", "MEDIUM", "MODERATE" -> "MOYEN";
            default                            -> "NORMAL";
        };
    }

    private String getString(Map<String, Object> map, String... cles) {
        for (String cle : cles) {
            Object val = map.get(cle);
            if (val instanceof String s && !s.isEmpty()) return s;
        }
        return "Je n'ai pas compris.";
    }

    private boolean getBool(Map<String, Object> map, String... cles) {
        for (String cle : cles) {
            if (Boolean.TRUE.equals(map.get(cle))) return true;
        }
        return false;
    }

    private String construireSuffixeContextuel(String niveau) {
        if (nbEchanges == 3 && !themesActifs.isEmpty()) {
            return "💡 Nous avons abordé plusieurs points ("
                    + String.join(", ", themesActifs)
                    + "). Souhaitez-vous un résumé ou planifier une intervention ?";
        }
        if ("CRITIQUE".equals(niveau) && nbEchanges > 1) {
            return "⚠️ Rappel : ne remettez pas le matériel en marche avant l'intervention d'un technicien.";
        }
        return "";
    }

    private String buildHistoriqueTexte() {
        if (historique.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        int debut = Math.max(0, historique.size() - 6);
        for (int i = debut; i < historique.size(); i++) {
            ConvEntry e = historique.get(i);
            String prefix  = e.role().equals("user") ? "Utilisateur: " : "Assistant: ";
            String extrait = e.texte().length() > 200
                    ? e.texte().substring(0, 200) + "..."
                    : e.texte();
            sb.append(prefix).append(extrait).append("\n");
        }
        return sb.toString();
    }

    private String normaliser(String texte) {
        if (texte == null) return "";
        return java.text.Normalizer.normalize(texte, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();
    }

    private void mettreAJourCompteur() {
        if (lblMsgCount != null)
            lblMsgCount.setText(nbEchanges + " échange" + (nbEchanges > 1 ? "s" : ""));
    }

    // ══════════════════════════════════════════════════════════════
    //  COMPOSANTS UI (identiques à la v3)
    // ══════════════════════════════════════════════════════════════

    private Label creerIndicateurTyping() {
        Label dots = new Label("● ● ●");
        dots.setFont(Font.font("Arial", 11));
        dots.setTextFill(Color.web("#2e7d32"));
        dots.setStyle("-fx-padding: 8 14; -fx-background-color: rgba(200,230,160,0.3);" +
                "-fx-background-radius: 16; -fx-border-color: #2d4a1a;" +
                "-fx-border-radius: 16; -fx-border-width: 1;");

        Label label = new Label("ARDHI IA réfléchit...");
        label.setFont(Font.font("Segoe UI", 10));
        label.setTextFill(Color.web("#4caf50"));

        Label typing = new Label();
        typing.setGraphic(new HBox(8, dots, label));
        VBox.setMargin(typing, new Insets(2, 60, 2, 0));
        return typing;
    }

    private void ajouterBulleUtilisateur(String texte) {
        HBox conteneur = new HBox();
        conteneur.setAlignment(Pos.CENTER_RIGHT);

        Label bulle = new Label(texte);
        bulle.setWrapText(true);
        bulle.setMaxWidth(420);
        bulle.setFont(Font.font("Segoe UI", 13));
        bulle.setTextFill(Color.web("#ffffff"));
        bulle.setPadding(new Insets(11, 16, 11, 16));
        bulle.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #43a047, #2e7d32);" +
                        "-fx-background-radius: 18 18 4 18;" +
                        "-fx-effect: dropshadow(gaussian, rgba(74,122,34,0.4), 10, 0, 0, 3);");

        Label heure = new Label(LocalTime.now().format(HH_MM));
        heure.setFont(Font.font("Segoe UI", 9));
        heure.setTextFill(Color.web("#66bb6a"));

        VBox wrapper = new VBox(3, bulle, heure);
        wrapper.setAlignment(Pos.CENTER_RIGHT);
        wrapper.setMaxWidth(460);

        conteneur.getChildren().add(wrapper);
        VBox.setMargin(conteneur, new Insets(2, 0, 2, 80));
        vboxConversation.getChildren().add(conteneur);
    }

    private void ajouterBulleIA(String texte, String niveau, boolean avecPlanifier,
                                List<String> suggestions) {
        String couleur, nomNiveau, bgColor;
        switch (niveau != null ? niveau.toUpperCase() : "NORMAL") {
            case "CRITIQUE" -> { couleur = "#c62828"; nomNiveau = "🔴 CRITIQUE";      bgColor = "#fff5f5"; }
            case "ELEVE"    -> { couleur = "#e65100"; nomNiveau = "🟠 À SURVEILLER";  bgColor = "#fff8f0"; }
            case "MOYEN"    -> { couleur = "#f57f17"; nomNiveau = "🟡 ATTENTION";     bgColor = "#fffde7"; }
            default         -> { couleur = "#2e7d32"; nomNiveau = "🟢 INFO";          bgColor = "#f1f8e9"; }
        }

        HBox conteneur = new HBox(10);
        conteneur.setAlignment(Pos.TOP_LEFT);

        // Avatar
        StackPane avatarPane = new StackPane();
        avatarPane.setMinWidth(36); avatarPane.setMinHeight(36);
        avatarPane.setMaxWidth(36); avatarPane.setMaxHeight(36);
        Circle cercle = new Circle(18);
        cercle.setStyle("-fx-fill: radial-gradient(center 40% 35%, radius 55%, #66bb6a, #388e3c);" +
                "-fx-stroke: " + couleur + "; -fx-stroke-width: 1.5;");
        Label avatarLabel = new Label("🌿");
        avatarLabel.setFont(Font.font(16));
        avatarPane.getChildren().addAll(cercle, avatarLabel);

        // Bulle
        VBox bulleWrapper = new VBox(5);
        bulleWrapper.setMaxWidth(480);

        Label badge = new Label(nomNiveau);
        badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        badge.setTextFill(Color.web(couleur));
        badge.setPadding(new Insets(2, 8, 2, 8));
        badge.setStyle("-fx-background-color:" + couleur + "22;" +
                "-fx-border-color:" + couleur + "55;" +
                "-fx-border-radius:8; -fx-background-radius:8; -fx-border-width:1;");

        Label bulle = new Label(texte);
        bulle.setWrapText(true);
        bulle.setMaxWidth(480);
        bulle.setFont(Font.font("Segoe UI", 13));
        bulle.setTextFill(Color.web("#1a3a1a"));
        bulle.setPadding(new Insets(12, 16, 12, 16));
        bulle.setStyle("-fx-background-color:" + bgColor + ";" +
                "-fx-background-radius: 4 18 18 18;" +
                "-fx-border-color:" + couleur + "44;" +
                "-fx-border-radius: 4 18 18 18;" +
                "-fx-border-width: 0 0 0 3;");

        Label heure = new Label(LocalTime.now().format(HH_MM) + " — ARDHI IA (Groq)");
        heure.setFont(Font.font("Segoe UI", 9));
        heure.setTextFill(Color.web("#81c784"));

        bulleWrapper.getChildren().addAll(badge, bulle, heure);

        // Bouton planifier
        if (avecPlanifier && onPlanifierMaintenance != null) {
            Button btn = new Button("📅  Planifier une intervention");
            btn.setStyle("-fx-background-color: linear-gradient(to right," + couleur + "cc," +
                    couleur + "88);" +
                    "-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;" +
                    "-fx-padding: 8 16; -fx-background-radius: 20; -fx-cursor: hand;" +
                    "-fx-font-family: 'Segoe UI';");
            btn.setOnAction(e -> onPlanifierMaintenance.run());
            VBox.setMargin(btn, new Insets(4, 0, 0, 0));
            bulleWrapper.getChildren().add(btn);
        }

        // Chips suggestions
        if (suggestions != null && !suggestions.isEmpty()) {
            HBox chipsBox = new HBox(6);
            chipsBox.setStyle("-fx-padding: 6 0 0 0;");
            for (String sugg : suggestions) {
                Button chip = new Button(sugg);
                chip.setStyle("-fx-background-color: rgba(232,245,233,0.9);" +
                        "-fx-text-fill: #2e7d32; -fx-font-size: 10px;" +
                        "-fx-padding: 4 10; -fx-background-radius: 12;" +
                        "-fx-border-color: #a5d6a7; -fx-border-radius: 12;" +
                        "-fx-border-width: 1; -fx-cursor: hand; -fx-background-insets: 0;" +
                        "-fx-font-family: 'Segoe UI';");
                chip.setOnAction(e -> poserQuestionRapide(sugg));
                chipsBox.getChildren().add(chip);
            }
            bulleWrapper.getChildren().add(chipsBox);
        }

        conteneur.getChildren().addAll(avatarPane, bulleWrapper);
        VBox.setMargin(conteneur, new Insets(2, 80, 2, 0));
        vboxConversation.getChildren().add(conteneur);
    }

    private void afficherSuggestions(List<String> suggestions) {
        if (hboxSuggestions == null || suggestions == null || suggestions.isEmpty()) return;
        Platform.runLater(() -> {
            hboxSuggestions.getChildren().clear();
            Label label = new Label("Suggestions :");
            label.setFont(Font.font("Segoe UI", 10));
            label.setTextFill(Color.web("#81c784"));
            hboxSuggestions.getChildren().add(label);

            for (String sugg : suggestions) {
                Button chip = new Button(sugg);
                chip.setStyle("-fx-background-color: #e8f5e9;" +
                        "-fx-text-fill: #2e7d32; -fx-font-size: 10px;" +
                        "-fx-padding: 4 12; -fx-background-radius: 14;" +
                        "-fx-border-color: #a5d6a7; -fx-border-radius: 14;" +
                        "-fx-cursor: hand; -fx-background-insets: 0;");
                chip.setOnAction(e -> poserQuestionRapide(sugg));
                hboxSuggestions.getChildren().add(chip);
            }
        });
    }

    private void cacherSuggestions() {
        if (hboxSuggestions != null)
            Platform.runLater(() -> hboxSuggestions.getChildren().clear());
    }

    private void scrollToBottom() {
        if (scrollPane != null)
            Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }
}
