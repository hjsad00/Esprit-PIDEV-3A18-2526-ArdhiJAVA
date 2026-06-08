package tn.neuron.ardhi.controllers.MaterielEtMaintenance;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import tn.neuron.ardhi.models.MaterielEtMaintenance.Materiel;
import tn.neuron.ardhi.services.MaterielEtMaintenance.GrokService;
import tn.neuron.ardhi.utils.MaterielEtMaintenance.MaintenanceAlertUtils;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class PanelIAController {

    private GrokService        grokService;
    private int                userId;
    private VBox               container;
    private List<Materiel>     tousLesMateriels = new ArrayList<>();
    private boolean            modeTopRisque    = false;

    public void initialiser(int userId, VBox container) {
        this.userId      = userId;
        this.container   = container;
        this.grokService = new GrokService();
        afficherEnTete();
    }

    // ══════════════════════════════════════════════════════════
    //  EN-TÊTE avec statut Groq
    // ══════════════════════════════════════════════════════════

    private void afficherEnTete() {
        container.getChildren().clear();

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titre = new Label("🤖 Intelligence Artificielle — Prédictions");
        titre.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        titre.setTextFill(Color.web("#2d3748"));
        titre.setWrapText(true);

        Label statutLabel = new Label("● Vérification...");
        statutLabel.setFont(Font.font("Arial", 10));
        statutLabel.setTextFill(Color.web("#a0aec0"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(titre, spacer, statutLabel);
        container.getChildren().add(header);

        Task<Boolean> checkTask = new Task<>() {
            @Override protected Boolean call() { return grokService.isServerAvailable(); }
        };
        checkTask.setOnSucceeded(e -> {
            boolean ok = checkTask.getValue();
            Platform.runLater(() -> {
                statutLabel.setText(ok ? "● Groq IA actif" : "● Mode local");
                statutLabel.setTextFill(Color.web(ok ? "#38a169" : "#d69e2e"));
            });
        });
        new Thread(checkTask, "groq-health").start();
    }

    // ══════════════════════════════════════════════════════════
    //  ANALYSER TOUS
    // ══════════════════════════════════════════════════════════

    public void analyserTous(List<Materiel> materiels) {
        if (materiels == null || materiels.isEmpty()) { afficherAucunMateriel(); return; }
        this.tousLesMateriels = new ArrayList<>(materiels);
        this.modeTopRisque    = false;
        lancerAnalyse(materiels, false);
    }

    // ══════════════════════════════════════════════════════════
    //  TOP RISQUE — matériels urgents uniquement
    // ══════════════════════════════════════════════════════════

    public void afficherTopRisque() {
        if (tousLesMateriels.isEmpty()) { afficherAucunMateriel(); return; }

        List<Materiel> enRetard = new ArrayList<>();
        for (Materiel m : tousLesMateriels) {
            String n = MaintenanceAlertUtils.getMaintenanceUrgencyLevel(m);
            if ("URGENT".equals(n) || "CETTE_SEMAINE".equals(n)) enRetard.add(m);
        }

        if (enRetard.isEmpty()) {
            List<Materiel> tries = new ArrayList<>(tousLesMateriels);
            tries.sort(Comparator.comparingLong(m -> {
                if (m.getDate_prochaine_maintenance() == null) return Long.MAX_VALUE;
                return ChronoUnit.DAYS.between(LocalDate.now(), m.getDate_prochaine_maintenance());
            }));
            enRetard.addAll(tries.subList(0, Math.min(3, tries.size())));
        }

        this.modeTopRisque = true;
        lancerAnalyse(enRetard, true);
    }

    // ══════════════════════════════════════════════════════════
    //  LANCER ANALYSE via Groq
    // ══════════════════════════════════════════════════════════

    private void lancerAnalyse(List<Materiel> materiels, boolean modeRetard) {
        afficherLoader(materiels.size() + " matériel(s) — Analyse IA en cours...");

        Task<Map<String, Object>> task = new Task<>() {
            @Override protected Map<String, Object> call() {
                Map<Integer, List<Map<String, Object>>> histoMap = new HashMap<>();
                for (Materiel m : materiels)
                    histoMap.put(m.getId_materiel(), chargerHistorique(m.getId_materiel()));
                return grokService.recommanderPriorites(materiels, histoMap);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() ->
                afficherRecommandations(task.getValue(), materiels, modeRetard)));
        task.setOnFailed(e -> Platform.runLater(() ->
                afficherAnalyseLocale(materiels, modeRetard)));

        Thread t = new Thread(task, "groq-analyse");
        t.setDaemon(true);
        t.start();
    }

    // ══════════════════════════════════════════════════════════
    //  AFFICHAGE PRINCIPAL — résultats Groq
    // ══════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private void afficherRecommandations(Map<String, Object> resultat,
                                         List<Materiel> materiels, boolean modeRetard) {
        while (container.getChildren().size() > 1) container.getChildren().remove(1);

        if (resultat.containsKey("error")) { afficherAnalyseLocale(materiels, modeRetard); return; }

        List<Map<String, Object>> recs =
                (List<Map<String, Object>>) resultat.getOrDefault("recommandations", Collections.emptyList());
        if (recs.isEmpty()) { afficherAucunMateriel(); return; }

        // Badge mode (retard ou tout)
        afficherBadgeMode(modeRetard, materiels.size());

        // Résumé global généré par Groq
        String resumeGlobal = str(resultat.get("resume_global"));
        if (!resumeGlobal.isEmpty()) {
            container.getChildren().add(creerResumeGlobal(resumeGlobal));
        }

        // Bandeau alertes critiques
        int critiques = toInt(resultat.get("nb_critiques"));
        int eleves    = toInt(resultat.get("nb_eleves"));
        if (critiques > 0 || eleves > 0)
            container.getChildren().add(creerBandeauAlerte(critiques, eleves));

        // Cards matériels
        for (Map<String, Object> rec : recs) {
            Materiel mat = materiels.stream()
                    .filter(m -> m.getId_materiel() == toInt(rec.get("materiel_id")))
                    .findFirst().orElse(null);
            container.getChildren().add(creerCardEnrichi(rec, mat));
        }

        ajouterBoutonsNavigation(modeRetard);
    }

    // ══════════════════════════════════════════════════════════
    //  FALLBACK LOCAL sans API
    // ══════════════════════════════════════════════════════════

    private void afficherAnalyseLocale(List<Materiel> materiels, boolean modeRetard) {
        while (container.getChildren().size() > 1) container.getChildren().remove(1);
        afficherBadgeMode(modeRetard, materiels.size());

        Label lblFallback = new Label("⚠️ IA indisponible — analyse locale");
        lblFallback.setFont(Font.font("Arial", FontPosture.ITALIC, 10));
        lblFallback.setTextFill(Color.web("#d69e2e"));
        lblFallback.setWrapText(true);
        VBox.setMargin(lblFallback, new Insets(0, 0, 4, 0));
        container.getChildren().add(lblFallback);

        List<Materiel> tries = new ArrayList<>(materiels);
        tries.sort(Comparator.comparingInt(m ->
                urgenceScore(MaintenanceAlertUtils.getMaintenanceUrgencyLevel(m))));

        for (Materiel m : tries) container.getChildren().add(creerCardLocale(m));
        ajouterBoutonsNavigation(modeRetard);
    }

    private int urgenceScore(String n) {
        return switch (n) {
            case "URGENT"       -> 0;
            case "CETTE_SEMAINE"-> 1;
            case "CE_MOIS"      -> 2;
            case "BIENTOT"      -> 3;
            default             -> 4;
        };
    }

    // ══════════════════════════════════════════════════════════
    //  BOUTONS DE NAVIGATION
    // ══════════════════════════════════════════════════════════

    private void ajouterBoutonsNavigation(boolean modeRetard) {
        if (modeRetard) {
            Button btnTout = new Button("← Tous les matériels");
            btnTout.setMaxWidth(Double.MAX_VALUE);
            btnTout.setStyle("-fx-background-color: #718096; -fx-text-fill: white;" +
                    "-fx-background-radius: 8; -fx-padding: 7 12; -fx-cursor: hand;" +
                    "-fx-font-size: 11px; -fx-border-width: 0; -fx-background-insets: 0;");
            btnTout.setOnAction(e -> analyserTous(tousLesMateriels));
            VBox.setMargin(btnTout, new Insets(4, 0, 2, 0));
            container.getChildren().add(btnTout);
        }

        Button btnRefresh = new Button("🔄 Réanalyser");
        btnRefresh.setMaxWidth(Double.MAX_VALUE);
        btnRefresh.setStyle("-fx-background-color: #6B7F3F; -fx-text-fill: white;" +
                "-fx-background-radius: 8; -fx-padding: 7 12; -fx-cursor: hand;" +
                "-fx-font-size: 11px; -fx-border-width: 0; -fx-background-insets: 0;");
        btnRefresh.setOnAction(e -> {
            if (modeRetard) afficherTopRisque();
            else analyserTous(tousLesMateriels);
        });
        VBox.setMargin(btnRefresh, new Insets(2, 0, 0, 0));
        container.getChildren().add(btnRefresh);
    }

    // ══════════════════════════════════════════════════════════
    //  COMPOSANTS UI
    // ══════════════════════════════════════════════════════════

    /** Résumé global généré par Groq */
    private HBox creerResumeGlobal(String texte) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(8, 10, 8, 10));
        box.setStyle("-fx-background-color: #EBF8FF; -fx-background-radius: 8;" +
                "-fx-border-color: #90CDF4; -fx-border-radius: 8; -fx-border-width: 1;");
        VBox.setMargin(box, new Insets(2, 0, 4, 0));

        Label ico = new Label("🤖");
        ico.setFont(Font.font(14));

        Label lbl = new Label(texte);
        lbl.setFont(Font.font("Arial", FontPosture.ITALIC, 10));
        lbl.setTextFill(Color.web("#2b6cb0"));
        lbl.setWrapText(true);
        HBox.setHgrow(lbl, Priority.ALWAYS);

        box.getChildren().addAll(ico, lbl);
        return box;
    }

    private void afficherBadgeMode(boolean modeRetard, int nb) {
        HBox badge = new HBox(6);
        badge.setAlignment(Pos.CENTER_LEFT);
        badge.setPadding(new Insets(5, 8, 5, 8));
        badge.setStyle(modeRetard
                ? "-fx-background-color: #FFF5F5; -fx-background-radius: 6;" +
                "-fx-border-color: #FC8181; -fx-border-radius: 6; -fx-border-width: 1;"
                : "-fx-background-color: #EBF8FF; -fx-background-radius: 6;" +
                "-fx-border-color: #90CDF4; -fx-border-radius: 6; -fx-border-width: 1;");
        VBox.setMargin(badge, new Insets(2, 0, 4, 0));

        Label lbl = new Label(modeRetard
                ? "⚠️ " + nb + " matériel(s) en retard/urgent"
                : "📊 " + nb + " matériel(s) analysé(s)");
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        lbl.setTextFill(Color.web(modeRetard ? "#c53030" : "#2b6cb0"));
        lbl.setWrapText(true);
        badge.getChildren().add(lbl);
        container.getChildren().add(badge);
    }

    private HBox creerBandeauAlerte(int critiques, int eleves) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(8, 10, 8, 10));
        box.setStyle("-fx-background-color: #FFF5F5; -fx-background-radius: 8;" +
                "-fx-border-color: #FC8181; -fx-border-radius: 8; -fx-border-width: 1;");
        VBox.setMargin(box, new Insets(2, 0, 2, 0));

        StringBuilder txt = new StringBuilder("🚨 ");
        if (critiques > 0) txt.append(critiques).append(" CRITIQUE(S) ! ");
        if (eleves > 0)    txt.append(eleves).append(" risque élevé.");

        Label lbl = new Label(txt.toString());
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        lbl.setTextFill(Color.web("#c53030"));
        lbl.setWrapText(true);
        box.getChildren().add(lbl);
        return box;
    }

    // ══════════════════════════════════════════════════════════
    //  CARD ENRICHIE — données dynamiques Groq
    // ══════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private VBox creerCardEnrichi(Map<String, Object> rec, Materiel materiel) {
        String nom     = str(rec.get("nom"));
        String niveau  = str(rec.get("niveau"));
        double pct     = toDouble(rec.get("pourcentage"));
        String couleur = str(rec.get("couleur"));
        String emoji   = str(rec.get("emoji"));
        String action  = str(rec.get("action"));
        String raison  = str(rec.get("raison"));
        List<String> conseils = rec.get("conseils") instanceof List
                ? (List<String>) rec.get("conseils") : new ArrayList<>();

        VBox card = new VBox(5);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8;" +
                "-fx-border-color: " + couleur + "; -fx-border-radius: 8;" +
                "-fx-border-width: 0 0 0 4;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 6, 0, 0, 2);");
        VBox.setMargin(card, new Insets(2, 0, 2, 0));

        // ── Ligne 1 : nom + badge niveau ────────────────────
        HBox ligne1 = new HBox(6);
        ligne1.setAlignment(Pos.CENTER_LEFT);

        Label lblNom = new Label(emoji + " " + nom);
        lblNom.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        lblNom.setTextFill(Color.web("#2d3748"));
        lblNom.setWrapText(true);
        lblNom.setMaxWidth(130);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label badge = new Label(niveau);
        badge.setFont(Font.font("Arial", FontWeight.BOLD, 9));
        badge.setTextFill(Color.WHITE);
        badge.setPadding(new Insets(2, 6, 2, 6));
        badge.setStyle("-fx-background-color: " + couleur + "; -fx-background-radius: 8;");
        ligne1.getChildren().addAll(lblNom, sp, badge);

        // ── Barre de risque ──────────────────────────────────
        HBox barreBox = new HBox(6);
        barreBox.setAlignment(Pos.CENTER_LEFT);

        ProgressBar barre = new ProgressBar(pct / 100.0);
        barre.setPrefWidth(Double.MAX_VALUE);
        barre.setPrefHeight(7);
        barre.setStyle("-fx-accent: " + couleur + ";");
        HBox.setHgrow(barre, Priority.ALWAYS);

        Label lblPct = new Label(String.format("%.0f%%", pct));
        lblPct.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        lblPct.setTextFill(Color.web(couleur));
        lblPct.setMinWidth(34);
        barreBox.getChildren().addAll(barre, lblPct);

        // ── Action principale (Groq) ─────────────────────────
        Label lblAction = new Label("💡 " + action);
        lblAction.setFont(Font.font("Arial", 10));
        lblAction.setTextFill(Color.web("#718096"));
        lblAction.setWrapText(true);

        card.getChildren().addAll(ligne1, barreBox, lblAction);

        // ── Raison du score (Groq) ────────────────────────────
        if (!raison.isEmpty()) {
            Label lblRaison = new Label("📊 " + raison);
            lblRaison.setFont(Font.font("Arial", FontPosture.ITALIC, 10));
            lblRaison.setTextFill(Color.web("#4a5568"));
            lblRaison.setWrapText(true);
            VBox.setMargin(lblRaison, new Insets(1, 0, 1, 0));
            card.getChildren().add(lblRaison);
        }

        // ── Conseils spécifiques (générés par Groq) ──────────
        if (!conseils.isEmpty()) {
            Separator sep = new Separator();
            sep.setStyle("-fx-opacity: 0.25;");
            VBox.setMargin(sep, new Insets(4, 0, 3, 0));
            card.getChildren().add(sep);

            for (String conseil : conseils) {
                Label lc = new Label(conseil);
                lc.setFont(Font.font("Arial", 10));
                lc.setTextFill(Color.web("#4a5568"));
                lc.setWrapText(true);
                lc.setStyle("-fx-background-color: #F7FAFC; -fx-background-radius: 4; -fx-padding: 3 6;");
                VBox.setMargin(lc, new Insets(1, 0, 1, 0));
                card.getChildren().add(lc);
            }
        }

        return card;
    }

    // ══════════════════════════════════════════════════════════
    //  CARD LOCALE — fallback sans API
    // ══════════════════════════════════════════════════════════

    private VBox creerCardLocale(Materiel m) {
        String niv    = MaintenanceAlertUtils.getMaintenanceUrgencyLevel(m);
        String couleur = switch (niv) {
            case "URGENT"        -> "#e53e3e";
            case "CETTE_SEMAINE" -> "#dd6b20";
            case "CE_MOIS"       -> "#d69e2e";
            case "BIENTOT"       -> "#3182ce";
            default              -> "#38a169";
        };
        String emoji = switch (niv) {
            case "URGENT"        -> "🔴";
            case "CETTE_SEMAINE" -> "🟠";
            case "CE_MOIS"       -> "🟡";
            default              -> "🟢";
        };

        VBox card = new VBox(5);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8;" +
                "-fx-border-color: " + couleur + "; -fx-border-radius: 8;" +
                "-fx-border-width: 0 0 0 4;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 6, 0, 0, 2);");
        VBox.setMargin(card, new Insets(2, 0, 2, 0));

        HBox ligne1 = new HBox(6);
        ligne1.setAlignment(Pos.CENTER_LEFT);

        Label lblNom = new Label(emoji + " " + m.getNom());
        lblNom.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        lblNom.setTextFill(Color.web("#2d3748"));
        lblNom.setWrapText(true);
        lblNom.setMaxWidth(150);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label badge = new Label(m.getEtat() != null ? m.getEtat() : "—");
        badge.setFont(Font.font("Arial", FontWeight.BOLD, 9));
        badge.setTextFill(Color.WHITE);
        badge.setPadding(new Insets(2, 6, 2, 6));
        badge.setStyle("-fx-background-color: " + couleur + "; -fx-background-radius: 8;");
        ligne1.getChildren().addAll(lblNom, sp, badge);

        Label lblMsg = new Label("💡 " + MaintenanceAlertUtils.getMaintenanceMessage(m));
        lblMsg.setFont(Font.font("Arial", 10));
        lblMsg.setTextFill(Color.web("#718096"));
        lblMsg.setWrapText(true);

        card.getChildren().addAll(ligne1, lblMsg);
        return card;
    }

    // ══════════════════════════════════════════════════════════
    //  HISTORIQUE BDD
    // ══════════════════════════════════════════════════════════

    private List<Map<String, Object>> chargerHistorique(int materielId) {
        List<Map<String, Object>> liste = new ArrayList<>();
        try {
            Connection conn = MyDatabase.getInstance().getCnx();
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT date_maintenance, cout FROM maintenance " +
                            "WHERE materiel_id = ? ORDER BY date_maintenance ASC");
            stmt.setInt(1, materielId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> entry = new HashMap<>();
                if (rs.getDate("date_maintenance") != null)
                    entry.put("date", rs.getDate("date_maintenance").toLocalDate().toString());
                entry.put("cout", rs.getDouble("cout"));
                liste.add(entry);
            }
            stmt.close();
        } catch (Exception e) {
            System.err.println("PanelIA historique " + materielId + ": " + e.getMessage());
        }
        return liste;
    }

    // ══════════════════════════════════════════════════════════
    //  HELPERS AFFICHAGE
    // ══════════════════════════════════════════════════════════

    private void afficherLoader(String texte) {
        while (container.getChildren().size() > 1) container.getChildren().remove(1);
        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(22, 22);
        Label lbl = new Label(texte);
        lbl.setFont(Font.font("Arial", 11));
        lbl.setTextFill(Color.web("#718096"));
        lbl.setWrapText(true);
        HBox box = new HBox(8, pi, lbl);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10, 0, 8, 0));
        container.getChildren().add(box);
    }

    private void afficherAucunMateriel() {
        while (container.getChildren().size() > 1) container.getChildren().remove(1);
        VBox info = new VBox(6);
        info.setAlignment(Pos.CENTER);
        info.setPadding(new Insets(20, 8, 8, 8));
        Label ico = new Label(modeTopRisque ? "✅" : "🔍");
        ico.setFont(Font.font(26));
        Label lbl = new Label(modeTopRisque
                ? "Aucun retard !\nTous vos matériels sont à jour."
                : "Aucun matériel à analyser.");
        lbl.setTextFill(Color.web("#a0aec0"));
        lbl.setFont(Font.font("Arial", 11));
        lbl.setWrapText(true);
        lbl.setStyle("-fx-text-alignment: center;");
        info.getChildren().addAll(ico, lbl);
        container.getChildren().add(info);
    }

    // ══════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ══════════════════════════════════════════════════════════

    private String str(Object o)      { return o != null ? o.toString() : ""; }
    private double toDouble(Object o) {
        if (o == null) return 0;
        try { return ((Number) o).doubleValue(); } catch (Exception e) { return 0; }
    }
    private int toInt(Object o) {
        if (o == null) return 0;
        try { return ((Number) o).intValue(); } catch (Exception e) { return 0; }
    }
}
