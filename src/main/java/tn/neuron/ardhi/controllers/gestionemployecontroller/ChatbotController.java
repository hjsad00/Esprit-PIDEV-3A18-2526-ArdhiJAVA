package tn.neuron.ardhi.controllers.gestionemployecontroller;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import tn.neuron.ardhi.services.gestionemployeservice.ChatbotService;
import tn.neuron.ardhi.services.gestionemployeservice.ChatbotService.ChatbotResponse;
import tn.neuron.ardhi.services.gestionemployeservice.ChatbotService.ChatbotResponse.DisponibiliteEntry;
import tn.neuron.ardhi.services.gestionemployeservice.MatchingService.RecommandationResult;
import tn.neuron.ardhi.services.gestionemployeservice.TacheService;
import tn.neuron.ardhi.models.gestionemployemodel.Employe;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.gestionemployeutils.LanguageManager;
import tn.neuron.ardhi.utils.gestionemployeutils.NotificationToast;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 🤖 ChatbotController — Design "SmartFarm Manager – IA RH Chatbot"
 *
 * Fonctionnalités :
 *  ① Bulle bot (blanche, avatar robot) + bulle user (vert)
 *  ② Animation "typing" 3 points avant chaque réponse bot
 *  ③ Carte employé recommandé avec photo / score / barres de progression
 *  ④ Boutons "Oui, assigner" / "Non, choisir un autre"
 *  ⑤ Panneau latéral "Suggestions" dynamique (nom, durée, météo)
 *  ⑥ 4 chips de suggestions rapides
 *  ⑦ Indice de confiance IA affiché dans la carte
 */
public class ChatbotController implements Initializable {

    // ── FXML ────────────────────────────────────────────────────────────────
    @FXML private VBox        chatContainer;
    @FXML private ScrollPane  scrollPane;
    @FXML private TextField   txtMessage;
    @FXML private Button      btnSend;
    @FXML private Button      btnVoice;
    @FXML private Label       lblStatus;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Button      btnClearChat;

    // Chips suggestions
    @FXML private Label lblSuggestion1;
    @FXML private Label lblSuggestion2;
    @FXML private Label lblSuggestion3;
    @FXML private Label lblSuggestion4;

    // Panneau latéral
    @FXML private VBox        sidePanelContent;
    @FXML private ImageView   sideContextImage;
    @FXML private StackPane   sideImageContainer;
    @FXML private Button      btnRefreshSide;

    // ── Services ─────────────────────────────────────────────────────────────
    private ChatbotService chatbotService;
    private TacheService   tacheService;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // Dernière recommandation en attente d'assignation
    private RecommandationResult pendingRecommandation = null;
    private Integer              pendingIdTache        = null;

    // ══════════════════════════════════════════════════════════════════════════
    // INIT
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chatbotService = new ChatbotService();
        tacheService   = new TacheService();

        setupUI();
        setupSuggestionChips();
        afficherMessageBienvenue();
        rafraichirPanneauLateral(null);
    }

    private void setupUI() {
        // Auto-scroll vers le bas
        chatContainer.heightProperty().addListener((obs, o, n) ->
                Platform.runLater(() -> scrollPane.setVvalue(1.0))
        );
        txtMessage.setOnAction(e -> handleSendMessage());

        if (loadingIndicator != null) loadingIndicator.setVisible(false);
        if (lblStatus != null) lblStatus.setText("En ligne");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CHIPS DE SUGGESTIONS RAPIDES
    // ══════════════════════════════════════════════════════════════════════════

    private void setupSuggestionChips() {
        setupChip(lblSuggestion1, "🎯 Recommander un employé",
                "Recommande-moi le meilleur employé pour la tâche");
        setupChip(lblSuggestion2, "📊 Performances",
                "Montre-moi les meilleures performances");
        setupChip(lblSuggestion3, "🏆 Comparer Top 3",
                "Compare les 3 meilleurs pour la tâche");
        setupChip(lblSuggestion4, "📅 Disponibilités",
                "Qui est disponible aujourd'hui ?");
    }

    private void setupChip(Label lbl, String texte, String msg) {
        if (lbl == null) return;
        lbl.setText(texte);
        lbl.setOnMouseClicked(e -> envoyerMessage(msg));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MESSAGE DE BIENVENUE
    // ══════════════════════════════════════════════════════════════════════════

    private void afficherMessageBienvenue() {
        LanguageManager lm = LanguageManager.getInstance();
        String lang = lm.getLocale().getLanguage();

        String bienvenue;
        if ("ar".equals(lang)) {
            bienvenue = lm.get("chatbot.greet") + "\n\nيمكنني مساعدتك في :\n"
                    + "• 🎯 اقتراح أفضل موظف لمهامك\n"
                    + "• 🏆 مقارنة أفضل 3 موظفين\n"
                    + "• 🔍 البحث عن موظف حسب المهارة\n"
                    + "• 📊 تحليل أداء فريقك\n"
                    + "• 📅 التحقق من توافر الموظفين\n\n"
                    + "💡 انقر على المقترحات أدناه !";
        } else if ("en".equals(lang)) {
            bienvenue = lm.get("chatbot.greet") + "\n\nI can help you with:\n"
                    + "• 🎯 Recommend the best employee for your tasks\n"
                    + "• 🏆 Compare Top 3 candidates\n"
                    + "• 🔍 Search employees by skill\n"
                    + "• 📊 Analyze your team's performance\n"
                    + "• 📅 Check availability\n\n"
                    + "💡 Click a suggestion below or ask your question!";
        } else {
            bienvenue = lm.get("chatbot.greet") + "\n\nJe peux vous aider à :\n"
                    + "• 🎯 Recommander le meilleur employé pour vos tâches\n"
                    + "• 🏆 Comparer les Top 3 candidats\n"
                    + "• 🔍 Rechercher des employés par compétence\n"
                    + "• 📊 Analyser les performances de votre équipe\n"
                    + "• 📅 Vérifier les disponibilités\n\n"
                    + "💡 Cliquez sur une suggestion ou posez votre question !";
        }

        ajouterMessageBot(bienvenue);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ENVOI DE MESSAGE
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void handleSendMessage() {
        String message = txtMessage.getText().trim();
        if (message.isEmpty()) return;
        envoyerMessage(message);
        txtMessage.clear();
    }

    private void envoyerMessage(String message) {
        ajouterMessageUtilisateur(message);

        txtMessage.setDisable(true);
        btnSend.setDisable(true);
        if (lblStatus != null) lblStatus.setText("Analyse en cours...");

        // Animation "typing"
        VBox typingNode = afficherTypingIndicator();

        new Thread(() -> {
            try {
                Thread.sleep(900);

                int idUser = UserSession.getInstance().getUser() != null
                        ? UserSession.getInstance().getUser().getId() : 0;

                ChatbotResponse response = chatbotService.traiterMessage(message, idUser);

                Platform.runLater(() -> {
                    // Retirer le typing indicator
                    chatContainer.getChildren().remove(typingNode);

                    // Réponse texte
                    ajouterMessageBot(response.reponse);

                    // Cartes de recommandation
                    if (!response.recommandations.isEmpty()) {
                        afficherCartesRecommandation(response.recommandations);
                    }

                    // Cartes de disponibilité (avec photo + tél + email)
                    if (!response.disponibilites.isEmpty()) {
                        afficherCartesDisponibilite(response.disponibilites);
                    }

                    // Mise à jour panneau latéral
                    rafraichirPanneauLateral(response);

                    reactiverInput();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    chatContainer.getChildren().remove(typingNode);
                    ajouterMessageBot("❌ Une erreur s'est produite. Veuillez réessayer.");
                    reactiverInput();
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void reactiverInput() {
        txtMessage.setDisable(false);
        btnSend.setDisable(false);
        if (lblStatus != null) lblStatus.setText("En ligne");
        txtMessage.requestFocus();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ② ANIMATION TYPING (3 points)
    // ══════════════════════════════════════════════════════════════════════════

    private VBox afficherTypingIndicator() {
        VBox container = new VBox(4);
        container.setPadding(new Insets(4, 10, 4, 10));
        container.setAlignment(Pos.CENTER_LEFT);

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        // Avatar bot
        row.getChildren().add(creerAvatarBot());

        // Bulle avec 3 points
        HBox bulle = new HBox(6);
        bulle.setPadding(new Insets(12, 16, 12, 16));
        bulle.setAlignment(Pos.CENTER);
        bulle.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 4 18 18 18;" +
                        "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),6,0,0,2);"
        );

        for (int i = 0; i < 3; i++) {
            Circle dot = new Circle(4);
            dot.getStyleClass().add("typing-dot");
            dot.setFill(Color.web("#40916c"));

            ScaleTransition st = new ScaleTransition(Duration.millis(500), dot);
            st.setFromX(0.6); st.setToX(1.0);
            st.setFromY(0.6); st.setToY(1.0);
            st.setAutoReverse(true);
            st.setCycleCount(Animation.INDEFINITE);
            st.setDelay(Duration.millis(i * 160));
            st.play();

            bulle.getChildren().add(dot);
        }

        row.getChildren().add(bulle);
        container.getChildren().add(row);

        chatContainer.getChildren().add(container);
        animerApparition(container);
        return container;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BULLES DE MESSAGE
    // ══════════════════════════════════════════════════════════════════════════

    private void ajouterMessageUtilisateur(String message) {
        VBox box = creerBulleMessage(message, true);
        chatContainer.getChildren().add(box);
        animerApparition(box);
    }

    private void ajouterMessageBot(String message) {
        VBox box = creerBulleMessage(message, false);
        chatContainer.getChildren().add(box);
        animerApparition(box);
    }

    private VBox creerBulleMessage(String contenu, boolean isUser) {
        VBox container = new VBox(4);
        container.setPadding(new Insets(4, 10, 4, 10));
        container.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        HBox row = new HBox(10);
        row.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        // Avatar
        Node avatar = isUser ? creerAvatarUser() : creerAvatarBot();

        // Bulle
        VBox bulle = new VBox(4);
        bulle.setPadding(new Insets(12, 16, 10, 16));
        bulle.setMaxWidth(480);

        if (isUser) {
            bulle.getStyleClass().add("bubble-user");
        } else {
            bulle.getStyleClass().add("bubble-bot");
        }

        // Texte
        Label lblTexte = new Label(contenu);
        lblTexte.setWrapText(true);
        lblTexte.setMaxWidth(460);
        lblTexte.getStyleClass().add(isUser ? "msg-text-user" : "msg-text-bot");

        // Heure
        Label lblHeure = new Label(LocalDateTime.now().format(TIME_FMT));
        lblHeure.getStyleClass().add(isUser ? "msg-time-user" : "msg-time");

        bulle.getChildren().addAll(lblTexte, lblHeure);

        if (isUser) {
            row.getChildren().addAll(bulle, avatar);
            row.setAlignment(Pos.CENTER_RIGHT);
        } else {
            row.getChildren().addAll(avatar, bulle);
        }

        container.getChildren().add(row);
        return container;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ① AVATARS
    // ══════════════════════════════════════════════════════════════════════════

    private StackPane creerAvatarBot() {
        StackPane sp = new StackPane();
        sp.getStyleClass().add("avatar-bot");
        sp.setStyle(
                "-fx-background-color: #e8f5e9; -fx-background-radius: 50%;" +
                        "-fx-min-width:40px; -fx-min-height:40px;" +
                        "-fx-max-width:40px; -fx-max-height:40px;"
        );
        Label ico = new Label("🤖");
        ico.setStyle("-fx-font-size:18px;");
        sp.getChildren().add(ico);
        return sp;
    }

    private StackPane creerAvatarUser() {
        StackPane sp = new StackPane();
        sp.setStyle(
                "-fx-background-color: #2d6a4f; -fx-background-radius: 50%;" +
                        "-fx-min-width:40px; -fx-min-height:40px;" +
                        "-fx-max-width:40px; -fx-max-height:40px;"
        );
        Label ico = new Label("👤");
        ico.setStyle("-fx-font-size:18px;");
        sp.getChildren().add(ico);
        return sp;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ③ CARTES RECOMMANDATION
    // ══════════════════════════════════════════════════════════════════════════

    private void afficherCartesRecommandation(List<RecommandationResult> recs) {
        VBox cardsContainer = new VBox(10);
        cardsContainer.setPadding(new Insets(6, 60, 10, 60));

        for (RecommandationResult rec : recs) {
            HBox card = creerCarteEmploye(rec);
            cardsContainer.getChildren().add(card);
            animerApparition(card);
        }

        chatContainer.getChildren().add(cardsContainer);
        animerApparition(cardsContainer);

        // ④ Boutons d'assignation pour le 1er candidat
        if (!recs.isEmpty()) {
            pendingRecommandation = recs.get(0);
            afficherBoutonsAssignation(recs.get(0));
        }
    }

    private HBox creerCarteEmploye(RecommandationResult rec) {
        HBox card = new HBox(14);
        card.getStyleClass().add("employee-card");
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setAlignment(Pos.CENTER_LEFT);

        // Photo ou initiales
        StackPane photoBox = creerPhotoEmploye(rec.employe);

        // Infos
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nom = new Label(rec.employe.getPrenom() + " " + rec.employe.getNom());
        nom.getStyleClass().add("employee-card-name");

        Label poste = new Label(rec.employe.getPoste() != null ? rec.employe.getPoste() : "");
        poste.getStyleClass().add("employee-card-post");

        // Téléphone
        String tel = (rec.employe.getTelephone() != null && !rec.employe.getTelephone().isBlank())
                ? rec.employe.getTelephone() : null;
        String mail = (rec.employe.getEmail() != null && !rec.employe.getEmail().isBlank())
                ? rec.employe.getEmail() : null;

        HBox contactBox = new HBox(14);
        contactBox.setAlignment(Pos.CENTER_LEFT);
        if (tel != null) {
            Label lblTel = new Label("📞 " + tel);
            lblTel.setStyle("-fx-font-size:11px; -fx-text-fill:#555555;");
            contactBox.getChildren().add(lblTel);
        }
        if (mail != null) {
            Label lblMail = new Label("✉ " + mail);
            lblMail.setStyle("-fx-font-size:11px; -fx-text-fill:#555555;");
            contactBox.getChildren().add(lblMail);
        }

        // Ligne compétences + disponibilité (icônes ✓)
        HBox details = new HBox(12);
        details.setAlignment(Pos.CENTER_LEFT);
        details.getChildren().addAll(
                creerDetailLine("Expérience", String.format("%.0f%%", rec.scoreExperience)),
                creerDetailLine("Compétences", String.format("%.0f%%", rec.scoreCompetences)),
                creerDetailLine("Disponibilité", String.format("%.0f%%", rec.scoreDisponibilite))
        );

        info.getChildren().addAll(nom, poste, contactBox, details);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Score + badge confiance
        VBox scoreBox = new VBox(4);
        scoreBox.setAlignment(Pos.CENTER);

        Label scoreVal = new Label(String.format("%.0f", rec.scoreTotal));
        scoreVal.getStyleClass().add("employee-card-score");
        scoreVal.setStyle("-fx-font-size:28px; -fx-font-weight:bold; -fx-text-fill:" + rec.getCouleur() + ";");

        Label scoreSub = new Label("/ 100");
        scoreSub.setStyle("-fx-font-size:11px; -fx-text-fill:#aaaaaa;");

        // Badge confiance IA
        Label confidenceBadge = new Label(String.format("🔒 %.0f%%", rec.indiceConfiance));
        confidenceBadge.getStyleClass().addAll("confidence-badge",
                rec.indiceConfiance >= 70 ? "confidence-high"
                        : rec.indiceConfiance >= 40 ? "confidence-medium" : "confidence-low");

        scoreBox.getChildren().addAll(scoreVal, scoreSub, confidenceBadge);

        // Barres de score
        VBox barsBox = new VBox(5);
        barsBox.setAlignment(Pos.CENTER_LEFT);
        barsBox.getChildren().addAll(
                creerBarreScore("Compétences", rec.scoreCompetences),
                creerBarreScore("Performance",  rec.scorePerformance),
                creerBarreScore("Disponibilité",rec.scoreDisponibilite),
                creerBarreScore("Expérience",   rec.scoreExperience)
        );

        card.getChildren().addAll(photoBox, info, barsBox, spacer, scoreBox);

        // Hover
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() +
                "-fx-scale-x:1.01; -fx-scale-y:1.01;"));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle() +
                "-fx-scale-x:1.0; -fx-scale-y:1.0;"));

        return card;
    }

    private StackPane creerPhotoEmploye(Employe emp) {
        StackPane sp = new StackPane();
        sp.setMinSize(64, 64); sp.setMaxSize(64, 64);
        sp.setStyle("-fx-background-radius:8px;");

        boolean photoOk = false;
        if (emp.getPhotoPath() != null && !emp.getPhotoPath().isBlank()) {
            try {
                ImageView iv = new ImageView(new Image("file:" + emp.getPhotoPath(), 64, 64, true, true));
                iv.setFitWidth(64); iv.setFitHeight(64);
                Rectangle clip = new Rectangle(64, 64);
                clip.setArcWidth(20); clip.setArcHeight(20);
                iv.setClip(clip);
                sp.getChildren().add(iv);
                photoOk = true;
            } catch (Exception ignored) { }
        }

        if (!photoOk) {
            sp.setStyle("-fx-background-color:#e8f5e9; -fx-background-radius:8px;");
            String initiales = "";
            if (emp.getPrenom() != null && !emp.getPrenom().isEmpty())
                initiales += emp.getPrenom().charAt(0);
            if (emp.getNom() != null && !emp.getNom().isEmpty())
                initiales += emp.getNom().charAt(0);
            Label lbl = new Label(initiales.toUpperCase());
            lbl.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#2d6a4f;");
            sp.getChildren().add(lbl);
        }
        return sp;
    }

    private HBox creerDetailLine(String label, String valeur) {
        HBox box = new HBox(4);
        box.setAlignment(Pos.CENTER_LEFT);
        Label ico  = new Label("✓");
        ico.setStyle("-fx-text-fill:#40916c; -fx-font-weight:bold; -fx-font-size:11px;");
        Label lbl  = new Label(label + " : ");
        lbl.setStyle("-fx-font-size:11px; -fx-text-fill:#888888;");
        Label val  = new Label(valeur);
        val.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#2d6a4f;");
        box.getChildren().addAll(ico, lbl, val);
        return box;
    }

    private HBox creerBarreScore(String label, double score) {
        HBox ligne = new HBox(6);
        ligne.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label + ":");
        lbl.setStyle("-fx-font-size:10px; -fx-text-fill:#999999;");
        lbl.setPrefWidth(72);

        ProgressBar bar = new ProgressBar(score / 100.0);
        bar.setPrefWidth(70); bar.setPrefHeight(5);
        bar.getStyleClass().add(score >= 70 ? "score-bar" : score >= 40 ? "score-bar-warn" : "score-bar-danger");

        Label val = new Label(String.format("%.0f%%", score));
        val.setStyle("-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#333333;");

        ligne.getChildren().addAll(lbl, bar, val);
        return ligne;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ④ BOUTONS OUI / NON
    // ══════════════════════════════════════════════════════════════════════════

    private void afficherBoutonsAssignation(RecommandationResult rec) {
        // Message de confirmation
        ajouterMessageBot("Voulez-vous assigner cette tâche à "
                + rec.employe.getPrenom() + " " + rec.employe.getNom() + " ?");

        VBox btnsContainer = new VBox(8);
        btnsContainer.setPadding(new Insets(4, 60, 10, 60));

        Button btnOui = new Button("Oui, assigner à " + rec.employe.getPrenom());
        btnOui.getStyleClass().add("btn-assign-yes");

        Button btnNon = new Button("Non, choisir un autre employé");
        btnNon.getStyleClass().add("btn-assign-no");

        btnOui.setOnAction(e -> {
            btnsContainer.setDisable(true);
            handleAssignerOui(rec);
        });
        btnNon.setOnAction(e -> {
            btnsContainer.setDisable(true);
            handleAssignerNon(rec);
        });

        btnsContainer.getChildren().addAll(btnOui, btnNon);
        chatContainer.getChildren().add(btnsContainer);
        animerApparition(btnsContainer);
    }

    private void handleAssignerOui(RecommandationResult rec) {
        if (pendingIdTache == null) {
            ajouterMessageBot("✅ Assignation enregistrée pour " +
                    rec.employe.getPrenom() + " " + rec.employe.getNom() + ".");
            pendingRecommandation = null;
            return;
        }
        new Thread(() -> {
            boolean ok = false;
            try {
                // Assigner via TacheService
                tn.neuron.ardhi.models.gestionemployemodel.Tache tache =
                        tacheService.getTacheById(pendingIdTache);
                if (tache != null) {
                    tache.setIdEmploye(rec.employe.getId());
                    ok = tacheService.updateTache(tache);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            final boolean success = ok;
            Platform.runLater(() -> {
                if (success) {
                    ajouterMessageBot("✅ " + rec.employe.getPrenom() + " " + rec.employe.getNom()
                            + " a été assigné(e) avec succès !");
                    NotificationToast.showNotification(
                            "✅ Assignation réussie", NotificationToast.SUCCESS, 3000);
                } else {
                    ajouterMessageBot("❌ Impossible d'assigner. Veuillez vérifier les droits ou réessayer.");
                }
                pendingRecommandation = null;
                pendingIdTache = null;
            });
        }).start();
    }

    private void handleAssignerNon(RecommandationResult rec) {
        ajouterMessageBot("D'accord ! Vous pouvez préciser d'autres critères ou demander "
                + "\"Compare les 3 meilleurs\" pour voir tous les candidats.");
        pendingRecommandation = null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ⑤ PANNEAU LATÉRAL "Suggestions"
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void handleRefreshSuggestions() {
        rafraichirPanneauLateral(null);
    }

    private void rafraichirPanneauLateral(ChatbotResponse derniereResponse) {
        if (sidePanelContent == null) return;
        sidePanelContent.getChildren().clear();

        if (derniereResponse != null && !derniereResponse.recommandations.isEmpty()) {
            // Suggestions basées sur la recommandation
            RecommandationResult best = derniereResponse.recommandations.get(0);
            ajouterLigneLaterale(
                    "👤",
                    best.employe.getPrenom() + " " + best.employe.getNom()
                            + " est disponible",
                    best.employe.getPoste() != null ? best.employe.getPoste() : "Employé",
                    () -> envoyerMessage("Infos sur " + best.employe.getPrenom())
            );
            ajouterLigneLaterale(
                    "🔒",
                    "Confiance IA : " + String.format("%.0f%%", best.indiceConfiance)
                            + " — " + best.getConfianceLabel(),
                    "Score global : " + String.format("%.1f/100", best.scoreTotal),
                    null
            );
            ajouterLigneLaterale(
                    "⏱",
                    "Durée estimée : variable",
                    "Selon la catégorie de la tâche",
                    null
            );
        } else {
            // Suggestions par défaut
            ajouterLigneLaterale("🎯",
                    "Recommander un employé",
                    "Demandez la meilleure correspondance",
                    () -> envoyerMessage("Recommande-moi le meilleur employé pour la tâche"));
            ajouterLigneLaterale("🏆",
                    "Comparer Top 3",
                    "Analyse comparative des candidats",
                    () -> envoyerMessage("Compare les 3 meilleurs pour la tâche"));
            ajouterLigneLaterale("📅",
                    "Disponibilités",
                    "Voir qui est libre aujourd'hui",
                    () -> envoyerMessage("Qui est disponible ?"));
            ajouterLigneLaterale("📊",
                    "Performances équipe",
                    "Classement des meilleurs performeurs",
                    () -> envoyerMessage("Montre les performances"));
        }

        // Image contextuelle (ferme locale)
        try {
            if (sideContextImage != null) {
                String imgPath = getClass().getResource("/images/farm_worker.png") != null
                        ? getClass().getResource("/images/farm_worker.png").toExternalForm()
                        : null;
                if (imgPath != null) {
                    sideContextImage.setImage(new Image(imgPath));
                    sideImageContainer.setVisible(true);
                } else {
                    sideImageContainer.setVisible(false);
                }
            }
        } catch (Exception ignored) {
            if (sideImageContainer != null) sideImageContainer.setVisible(false);
        }
    }

    private void ajouterLigneLaterale(String emoji, String titre, String sousTitre,
                                      Runnable onClick) {
        HBox row = new HBox(12);
        row.getStyleClass().add("side-suggestion-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(13, 16, 13, 16));

        // Puce colorée
        StackPane bullet = new StackPane();
        bullet.getStyleClass().add("side-bullet");
        bullet.setStyle("-fx-min-width:8px; -fx-min-height:8px; " +
                "-fx-max-width:8px; -fx-max-height:8px; " +
                "-fx-background-color:#40916c; -fx-background-radius:50%;");

        // Emoji
        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size:16px;");

        // Texte
        VBox textBox = new VBox(2);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label titleLbl = new Label(titre);
        titleLbl.getStyleClass().add("side-suggestion-title");
        titleLbl.setWrapText(true);

        Label subLbl = new Label(sousTitre);
        subLbl.getStyleClass().add("side-suggestion-sub");
        subLbl.setWrapText(true);

        textBox.getChildren().addAll(titleLbl, subLbl);
        row.getChildren().addAll(bullet, emojiLbl, textBox);

        if (onClick != null) {
            row.setCursor(javafx.scene.Cursor.HAND);
            row.setOnMouseClicked(e -> onClick.run());
            row.setOnMouseEntered(e ->
                    row.setStyle(row.getStyle() + "-fx-background-color:#f8faf8;"));
            row.setOnMouseExited(e ->
                    row.setStyle(row.getStyle().replace("-fx-background-color:#f8faf8;", "")));
        }

        sidePanelContent.getChildren().add(row);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ANIMATION D'APPARITION
    // ══════════════════════════════════════════════════════════════════════════

    private void animerApparition(Node node) {
        node.setOpacity(0);
        node.setTranslateY(15);

        FadeTransition fade = new FadeTransition(Duration.millis(280), node);
        fade.setFromValue(0); fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(280), node);
        slide.setFromY(15); slide.setToY(0);

        new ParallelTransition(fade, slide).play();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CARTES DISPONIBILITÉ (photo + tél + email)
    // ══════════════════════════════════════════════════════════════════════════

    private void afficherCartesDisponibilite(java.util.List<DisponibiliteEntry> dispos) {
        VBox cardsContainer = new VBox(10);
        cardsContainer.setPadding(new Insets(6, 60, 10, 60));

        for (DisponibiliteEntry d : dispos) {
            HBox card = creerCarteDisponibilite(d);
            cardsContainer.getChildren().add(card);
            animerApparition(card);
        }

        chatContainer.getChildren().add(cardsContainer);
        animerApparition(cardsContainer);
    }

    /**
     * Carte compacte pour un employé : photo · nom · statut · tél · email
     */
    private HBox creerCarteDisponibilite(DisponibiliteEntry d) {
        HBox card = new HBox(12);
        card.getStyleClass().add("employee-card");
        card.setPadding(new Insets(12, 14, 12, 14));
        card.setAlignment(Pos.CENTER_LEFT);

        // Photo / initiales
        StackPane photo = creerPhotoEmploye(d.employe);

        // Statut coloré (barre gauche)
        javafx.scene.shape.Rectangle statusBar = new javafx.scene.shape.Rectangle(4, 56);
        statusBar.setArcWidth(4); statusBar.setArcHeight(4);
        statusBar.setFill(Color.web(d.getCouleurStatut()));

        // Infos
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Nom + badge statut
        HBox nameRow = new HBox(8);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nom = new Label(d.employe.getPrenom() + " " + d.employe.getNom());
        nom.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#1a1a1a;");

        Label statutBadge = new Label(d.getDotEmoji() + " " + d.getStatutLabel());
        statutBadge.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:"
                + d.getCouleurStatut() + ";");
        nameRow.getChildren().addAll(nom, statutBadge);

        // Poste
        Label poste = new Label(d.employe.getPoste() != null ? d.employe.getPoste() : "");
        poste.setStyle("-fx-font-size:12px; -fx-text-fill:#888888;");

        // Tâches en cours
        Label taches = new Label(d.tachesEnCours + " tâche(s) en cours");
        taches.setStyle("-fx-font-size:11px; -fx-text-fill:#aaaaaa;");

        // Contact : tél + email
        HBox contactRow = new HBox(14);
        contactRow.setAlignment(Pos.CENTER_LEFT);
        String tel  = d.employe.getTelephone();
        String mail = d.employe.getEmail();
        if (tel  != null && !tel.isBlank()) {
            Label lblTel = new Label("📞 " + tel);
            lblTel.setStyle("-fx-font-size:11px; -fx-text-fill:#555555;");
            contactRow.getChildren().add(lblTel);
        }
        if (mail != null && !mail.isBlank()) {
            Label lblMail = new Label("✉ " + mail);
            lblMail.setStyle("-fx-font-size:11px; -fx-text-fill:#555555;");
            contactRow.getChildren().add(lblMail);
        }

        info.getChildren().addAll(nameRow, poste, taches, contactRow);

        card.getChildren().addAll(statusBar, photo, info);

        // Hover
        String baseStyle = "-fx-background-color:white; -fx-background-radius:12px; "
                + "-fx-border-color:#e0ece0; -fx-border-width:1px; -fx-border-radius:12px; "
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),10,0,0,3); "
                + "-fx-padding:12 14; -fx-cursor:hand;";
        card.setStyle(baseStyle);
        card.setOnMouseEntered(e -> card.setStyle(baseStyle
                + "-fx-border-color:" + d.getCouleurStatut() + ";"));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));

        return card;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // EFFACER LA CONVERSATION
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void handleClearChat() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Effacer la conversation");
        confirm.setHeaderText("Êtes-vous sûr ?");
        confirm.setContentText("Toute la conversation sera supprimée.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            chatContainer.getChildren().clear();
            pendingRecommandation = null;
            pendingIdTache        = null;
            afficherMessageBienvenue();
            rafraichirPanneauLateral(null);
            NotificationToast.showNotification("💬 Conversation effacée",
                    NotificationToast.INFO, 2000);
            //
        }
    }
}