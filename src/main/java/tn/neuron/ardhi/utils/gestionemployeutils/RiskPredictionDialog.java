package tn.neuron.ardhi.utils.gestionemployeutils;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import tn.neuron.ardhi.models.gestionemployemodel.Employe;
import tn.neuron.ardhi.models.gestionemployemodel.Tache;
import tn.neuron.ardhi.services.gestionemployeservice.TacheRiskAnalyzer;
import tn.neuron.ardhi.services.gestionemployeservice.TacheRiskAnalyzer.RiskResult;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║       DIALOG IA – AFFICHAGE PRÉDICTION RISQUE DE RETARD         ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * Fenêtre modale JavaFX qui affiche l'analyse IA complète :
 *  – Score de risque animé (jauge circulaire)
 *  – Décomposition des 5 facteurs avec barres de progression
 *  – Liste des recommandations intelligentes
 */
public class RiskPredictionDialog {

    private static final String DARK_BG     = "#1a1a2e";
    private static final String CARD_BG     = "#16213e";
    private static final String ACCENT_BLUE = "#0f3460";
    private static final String TEXT_LIGHT  = "#e0e0e0";
    private static final String TEXT_DIM    = "#a0a0b0";

    /**
     * Ouvre la fenêtre d'analyse IA pour la tâche sélectionnée.
     *
     * @param tache    La tâche à analyser
     * @param employe  L'employé assigné (peut être null)
     */
    public static void show(Tache tache, Employe employe) {
        String nomEmploye = (employe != null)
                ? employe.getPrenom() + " " + employe.getNom()
                : "Non assigné";

        // Lancement de l'analyse IA
        RiskResult result = TacheRiskAnalyzer.analyser(tache, nomEmploye);

        // ── Fenêtre principale ─────────────────────────────────────────────
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setResizable(false);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + DARK_BG + "; -fx-background-radius: 16;");
        root.setEffect(new javafx.scene.effect.DropShadow(40, Color.BLACK));

        // ── En-tête ────────────────────────────────────────────────────────
        root.getChildren().add(buildHeader(tache, nomEmploye, stage));

        // ── Contenu principal ──────────────────────────────────────────────
        HBox body = new HBox(20);
        body.setPadding(new Insets(20, 24, 10, 24));

        // Colonne gauche : jauge + score global
        body.getChildren().add(buildScorePanel(result));

        // Colonne droite : facteurs + recommandations
        VBox rightCol = new VBox(14);
        rightCol.setPrefWidth(380);
        rightCol.getChildren().add(buildFacteursPanel(result));
        rightCol.getChildren().add(buildRecommandationsPanel(result));
        body.getChildren().add(rightCol);

        root.getChildren().add(body);

        // ── Pied de page ───────────────────────────────────────────────────
        root.getChildren().add(buildFooter(stage));

        Scene scene = new Scene(root, 700, 580);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        // Animation d'ouverture
        root.setOpacity(0);
        root.setScaleX(0.85);
        root.setScaleY(0.85);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), root);
        fadeIn.setToValue(1);
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(250), root);
        scaleIn.setToX(1);
        scaleIn.setToY(1);
        ParallelTransition open = new ParallelTransition(fadeIn, scaleIn);
        open.play();

        stage.show();
        animateScore(result.riskScore, root);
    }

    // ── En-tête ────────────────────────────────────────────────────────────

    private static HBox buildHeader(Tache tache, String nomEmploye, Stage stage) {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 20, 18, 24));
        header.setStyle("-fx-background-color: " + ACCENT_BLUE +
                "; -fx-background-radius: 16 16 0 0;");

        VBox titre = new VBox(3);
        Label lblTitre = styled("🔮  Analyse IA — Prédiction de Risque", 15, FontWeight.BOLD, TEXT_LIGHT);
        Label lblSub   = styled("Tâche : « " + tache.getTitre() + " »  ·  Employé : " + nomEmploye,
                11, FontWeight.NORMAL, TEXT_DIM);
        titre.getChildren().addAll(lblTitre, lblSub);
        HBox.setHgrow(titre, Priority.ALWAYS);

        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: #aaa;" +
                "-fx-font-size: 16; -fx-cursor: hand;");
        btnClose.setOnAction(e -> stage.close());

        header.getChildren().addAll(titre, btnClose);
        return header;
    }

    // ── Panneau score central ──────────────────────────────────────────────

    private static VBox buildScorePanel(RiskResult result) {
        VBox panel = new VBox(12);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPrefWidth(240);
        panel.setPadding(new Insets(10, 0, 10, 0));

        // Jauge circulaire
        StackPane gauge = buildCircularGauge(result);
        panel.getChildren().add(gauge);

        // Probabilité de réussite
        Label lblSucces = styled(String.format("Réussite : %.0f%%", result.probabiliteReussite),
                13, FontWeight.BOLD, "#27ae60");
        lblSucces.setAlignment(javafx.geometry.Pos.CENTER);
        panel.getChildren().add(lblSucces);

        // Niveau de risque badge
        Label badge = new Label("  " + result.emoji + " " + result.niveau + "  ");
        badge.setFont(Font.font("System", FontWeight.BOLD, 13));
        badge.setTextFill(Color.WHITE);
        badge.setStyle("-fx-background-color: " + result.couleur +
                "; -fx-background-radius: 20; -fx-padding: 5 14 5 14;");
        panel.getChildren().add(badge);

        // Séparateur
        panel.getChildren().add(buildSep());

        // Mini-tableau des sous-scores
        panel.getChildren().add(buildMiniScores(result));

        return panel;
    }

    private static StackPane buildCircularGauge(RiskResult result) {
        StackPane stack = new StackPane();
        stack.setPrefSize(160, 160);

        // Cercle de fond
        Circle bg = new Circle(70);
        bg.setFill(Color.web(CARD_BG));
        bg.setStroke(Color.web("#2a2a4a"));
        bg.setStrokeWidth(2);

        // Arc de progression (animé)
        Arc arc = new Arc(80, 80, 60, 60, 90, 0);
        arc.setType(ArcType.OPEN);
        arc.setFill(Color.TRANSPARENT);
        arc.setStroke(Color.web(result.couleur));
        arc.setStrokeWidth(10);
        arc.setId("riskArc");

        // Texte score
        Label lblScore = new Label("0%");
        lblScore.setFont(Font.font("System", FontWeight.BOLD, 28));
        lblScore.setTextFill(Color.web(result.couleur));
        lblScore.setId("scoreLabel");

        Label lblRisque = styled("Risque", 10, FontWeight.NORMAL, TEXT_DIM);

        VBox center = new VBox(2, lblScore, lblRisque);
        center.setAlignment(Pos.CENTER);

        stack.getChildren().addAll(bg, arc, center);
        stack.setAlignment(Pos.CENTER);

        // Tag pour animation
        stack.setUserData(result);
        return stack;
    }

    private static VBox buildMiniScores(RiskResult result) {
        VBox box = new VBox(6);
        box.setStyle("-fx-background-color: " + CARD_BG +
                "; -fx-background-radius: 8; -fx-padding: 10;");
        box.getChildren().add(styled("Décomposition IA", 10, FontWeight.BOLD, TEXT_DIM));
        box.getChildren().add(miniScore("Historique",  result.scoreHistorique));
        box.getChildren().add(miniScore("Charge",       result.scoreCharge));
        box.getChildren().add(miniScore("Complexité",   result.scoreComplexite));
        box.getChildren().add(miniScore("Saison",       result.scoreSaison));
        box.getChildren().add(miniScore("Délai",        result.scoreDelai));
        return box;
    }

    private static HBox miniScore(String label, double score) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = styled(label, 10, FontWeight.NORMAL, TEXT_DIM);
        lbl.setPrefWidth(68);
        ProgressBar pb = new ProgressBar(score / 100.0);
        pb.setPrefWidth(90);
        pb.setPrefHeight(6);
        String color = score < 40 ? "#27ae60" : score < 70 ? "#f39c12" : "#e74c3c";
        pb.setStyle("-fx-accent: " + color + "; -fx-control-inner-background: #2a2a4a;");
        Label val = styled(String.format("%.0f", score), 10, FontWeight.BOLD, color);
        row.getChildren().addAll(lbl, pb, val);
        return row;
    }

    // ── Panneau facteurs ───────────────────────────────────────────────────

    private static VBox buildFacteursPanel(RiskResult result) {
        VBox panel = new VBox(8);
        panel.setStyle("-fx-background-color: " + CARD_BG +
                "; -fx-background-radius: 10; -fx-padding: 14;");

        panel.getChildren().add(styled("📊 Facteurs analysés", 12, FontWeight.BOLD, TEXT_LIGHT));

        for (String facteur : result.facteurs) {
            Label lbl = new Label(facteur);
            lbl.setFont(Font.font("System", FontWeight.NORMAL, 11));
            lbl.setTextFill(Color.web(TEXT_DIM));
            lbl.setWrapText(true);
            lbl.setMaxWidth(360);

            // Couleur selon contenu
            if (facteur.startsWith("✅")) lbl.setTextFill(Color.web("#27ae60"));
            else if (facteur.startsWith("🔴")) lbl.setTextFill(Color.web("#e74c3c"));
            else if (facteur.startsWith("⚠️") || facteur.startsWith("🔶")) lbl.setTextFill(Color.web("#f39c12"));

            panel.getChildren().add(lbl);
        }
        return panel;
    }

    // ── Panneau recommandations ────────────────────────────────────────────

    private static VBox buildRecommandationsPanel(RiskResult result) {
        VBox panel = new VBox(8);
        panel.setStyle("-fx-background-color: " + CARD_BG +
                "; -fx-background-radius: 10; -fx-padding: 14;");

        panel.getChildren().add(styled("💡 Recommandations IA", 12, FontWeight.BOLD, TEXT_LIGHT));

        for (String rec : result.recommandations) {
            Label lbl = new Label(rec);
            lbl.setFont(Font.font("System", FontWeight.NORMAL, 11));
            lbl.setTextFill(Color.web("#a0d4ff"));
            lbl.setWrapText(true);
            lbl.setMaxWidth(360);
            if (rec.startsWith("🚨")) lbl.setTextFill(Color.web("#e74c3c"));
            panel.getChildren().add(lbl);
        }
        return panel;
    }

    // ── Pied de page ───────────────────────────────────────────────────────

    private static HBox buildFooter(Stage stage) {
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 24, 18, 24));

        Label info = styled("Ardhi IA · Algorithme de scoring multi-facteurs · v1.0",
                10, FontWeight.NORMAL, TEXT_DIM);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button btnOk = new Button("  Fermer  ");
        btnOk.setStyle("-fx-background-color: #0f3460; -fx-text-fill: white;" +
                "-fx-background-radius: 8; -fx-font-size: 12; -fx-cursor: hand;" +
                "-fx-padding: 7 20 7 20;");
        btnOk.setOnAction(e -> stage.close());
        btnOk.setOnMouseEntered(e -> btnOk.setStyle(btnOk.getStyle().replace("#0f3460", "#1a5276")));
        btnOk.setOnMouseExited(e  -> btnOk.setStyle(btnOk.getStyle().replace("#1a5276", "#0f3460")));

        footer.getChildren().addAll(info, btnOk);
        return footer;
    }

    // ── Animation du score ─────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static void animateScore(double targetScore, VBox root) {
        // Trouver les noeuds par ID
        root.lookupAll("#riskArc").forEach(node -> {
            if (node instanceof Arc arc) {
                Timeline tl = new Timeline(
                        new KeyFrame(Duration.ZERO,            new KeyValue(arc.lengthProperty(), 0)),
                        new KeyFrame(Duration.millis(1200),
                                new KeyValue(arc.lengthProperty(), -(targetScore / 100.0) * 360,
                                        Interpolator.EASE_OUT))
                );
                tl.play();
            }
        });

        root.lookupAll("#scoreLabel").forEach(node -> {
            if (node instanceof Label lbl) {
                Timeline tl = new Timeline();
                int frames = 40;
                for (int i = 0; i <= frames; i++) {
                    final double val = targetScore * i / frames;
                    tl.getKeyFrames().add(new KeyFrame(
                            Duration.millis(1200.0 * i / frames),
                            e -> lbl.setText(String.format("%.0f%%", val))
                    ));
                }
                tl.play();
            }
        });
    }

    // ── Utilitaires ────────────────────────────────────────────────────────

    private static Label styled(String text, int size, FontWeight weight, String hexColor) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("System", weight, size));
        lbl.setTextFill(Color.web(hexColor));
        lbl.setWrapText(true);
        return lbl;
    }

    private static Region buildSep() {
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: #2a2a4a;");
        sep.setMaxWidth(Double.MAX_VALUE);
        return sep;
    }
}