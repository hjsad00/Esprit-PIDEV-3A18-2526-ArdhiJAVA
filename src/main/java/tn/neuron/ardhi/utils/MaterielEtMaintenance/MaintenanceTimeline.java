package tn.neuron.ardhi.utils.MaterielEtMaintenance;

import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.util.Duration;
import tn.neuron.ardhi.models.MaterielEtMaintenance.Maintenance;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * ================================================================
 *  MaintenanceTimeline.java
 *  Timeline visuelle style "suivi colis Aramex"
 *  Basée sur statut_maintenance de ta table BDD
 * ================================================================
 *
 *  STATUTS BDD supportés :
 *    "planifiee"   →  étape calculée selon date_maintenance
 *    "en_cours"    →  🔧 En cours  (violet)
 *    "en_attente"  →  ⏳ En attente (orange)
 *    "terminee"    →  ✅ Terminé   (vert)
 *    "annulee"     →  ❌ Annulée   (gris)
 *
 *  UTILISATION dans MaintenanceController :
 *
 *    // Récupère les maintenances du matériel
 *    List<Maintenance> maintenances = maintenanceService.getMaintenancesByMaterielId(materielId);
 *    for (Maintenance m : maintenances) {
 *        MaintenanceTimeline card = new MaintenanceTimeline(m);
 *        timelineContainer.getChildren().add(card);
 *    }
 *
 * ================================================================
 */
public class MaintenanceTimeline extends VBox {

    // ── Couleurs ─────────────────────────────────────────────────
    private static final String C_BLEU   = "#2196F3";
    private static final String C_ORANGE = "#FF9800";
    private static final String C_VIOLET = "#9C27B0";
    private static final String C_CYAN   = "#00BCD4";
    private static final String C_VERT   = "#4CAF50";
    private static final String C_ROUGE  = "#F44336";
    private static final String C_GRIS   = "#BDBDBD";

    // ── Les 5 étapes de la timeline ──────────────────────────────
    // { "Nom", "emoji", "couleur" }
    private static final String[][] ETAPES = {
            { "Planifié",         "📅", C_BLEU   },
            { "En attente",       "⏳", C_ORANGE },
            { "En cours",         "🔧", C_VIOLET },
            { "Contrôle\nqualité","🔍", C_CYAN   },
            { "Terminé",          "✅", C_VERT   },
    };

    private final Maintenance maintenance;
    private final int etapeIndex; // 0-4 = étape active, -1 = EN RETARD

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ────────────────────────────────────────────────────────────
    //  CONSTRUCTEUR  — passe directement l'objet Maintenance
    // ────────────────────────────────────────────────────────────
    public MaintenanceTimeline(Maintenance maintenance) {
        this.maintenance = maintenance;
        this.etapeIndex  = resoudreEtape();
        construireUI();
    }

    // ────────────────────────────────────────────────────────────
    //  RÉSOLUTION DE L'ÉTAPE
    //  Priorité : statut_maintenance BDD > calcul par date
    // ────────────────────────────────────────────────────────────
    private int resoudreEtape() {
        String statut = maintenance.getStatut_maintenance();
        if (statut == null) statut = "planifiee";
        statut = statut.toLowerCase().trim();

        switch (statut) {
            case "terminee":
            case "terminée":
                return 4;
            case "en_cours":
            case "en cours":
                return 2;
            case "en_attente":
            case "en attente":
                return 1;
            case "annulee":
            case "annulée":
                return -2; // cas spécial annulé
            case "planifiee":
            case "planifiée":
            default:
                // Calcul automatique par rapport à date_maintenance
                return calculerEtapeParDate();
        }
    }

    /**
     * Calcul automatique basé sur date_maintenance (colonne BDD)
     * La logique reflète ton système actuel : maintenance = 1 an après achat
     */
    private int calculerEtapeParDate() {
        LocalDate dateMaintenance = maintenance.getDate_maintenance(); // ta colonne date_maintenance
        if (dateMaintenance == null) return 0;

        long j = ChronoUnit.DAYS.between(LocalDate.now(), dateMaintenance);

        if (j > 30)  return 0;  // 📅 Planifié   (plus d'un mois)
        if (j > 0)   return 1;  // ⏳ En attente  (dans le mois)
        if (j == 0)  return 2;  // 🔧 En cours    (aujourd'hui)
        if (j >= -3) return 3;  // 🔍 Contrôle    (lendemain à J+3)
        if (j >= -7) return 4;  // ✅ Terminé     (J+4 à J+7)
        return -1;               // ⚠️ EN RETARD   (plus de 7 jours de dépassement)
    }

    // ────────────────────────────────────────────────────────────
    //  CONSTRUCTION UI
    // ────────────────────────────────────────────────────────────
    private void construireUI() {
        setSpacing(0);
        setPadding(new Insets(8, 12, 8, 12));
        setMaxWidth(Double.MAX_VALUE);

        VBox carte = new VBox(14);
        carte.setPadding(new Insets(18, 22, 18, 22));
        carte.setMaxWidth(Double.MAX_VALUE);
        carte.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #EEEEEE;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);"
        );

        // Barre colorée en haut selon statut
        carte.setStyle(carte.getStyle() +
                "-fx-border-color: " + getCouleurStatut() + " #EEEEEE #EEEEEE #EEEEEE;" +
                "-fx-border-width: 3 1 1 1;"
        );

        carte.getChildren().addAll(
                creerEntete(),
                creerDivider(),
                creerTimelineLigne(),
                creerMessageStatut()
        );

        // Si annulé, overlay grisé
        if (etapeIndex == -2) {
            carte.setOpacity(0.6);
        }

        getChildren().add(carte);
        animerEntree(carte);
    }

    // ── En-tête : infos maintenance + badge ──────────────────────
    private HBox creerEntete() {
        HBox entete = new HBox(12);
        entete.setAlignment(Pos.CENTER_LEFT);

        // Icône type maintenance
        String typeEmoji = "preventive".equalsIgnoreCase(maintenance.getType_maintenance()) ? "🛡️" : "🔩";
        Label iconeType = new Label(typeEmoji);
        iconeType.setStyle("-fx-font-size: 24px;");

        // Infos centre
        VBox infos = new VBox(4);

        String typeTxt = maintenance.getType_maintenance() != null
                ? capitalize(maintenance.getType_maintenance())
                : "Préventive";
        Label typeLabel = new Label("Maintenance " + typeTxt);
        typeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #212121;");

        HBox dateRow = new HBox(16);
        dateRow.setAlignment(Pos.CENTER_LEFT);

        if (maintenance.getDate_maintenance() != null) {
            Label datePlanLabel = new Label("📅 Prévue : " + maintenance.getDate_maintenance().format(FMT));
            datePlanLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
            dateRow.getChildren().add(datePlanLabel);
        }

        if (maintenance.getDate_realisee() != null) {
            Label dateRealLabel = new Label("✅ Réalisée : " + maintenance.getDate_realisee().format(FMT));
            dateRealLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #4CAF50;");
            dateRow.getChildren().add(dateRealLabel);
        }

        if (maintenance.getCout() > 0) {
            Label coutLabel = new Label("💰 " + String.format("%.2f TND", maintenance.getCout()));
            coutLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
            dateRow.getChildren().add(coutLabel);
        }

        infos.getChildren().addAll(typeLabel, dateRow);

        // Description courte
        if (maintenance.getDescription() != null && !maintenance.getDescription().isEmpty()) {
            String desc = maintenance.getDescription().replace("\n", " ");
            if (desc.length() > 60) desc = desc.substring(0, 60) + "...";
            Label descLabel = new Label(desc);
            descLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999; -fx-font-style: italic;");
            infos.getChildren().add(descLabel);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        entete.getChildren().addAll(iconeType, infos, spacer, creerBadgeStatut());
        return entete;
    }

    // ── Badge coloré statut ───────────────────────────────────────
    private Label creerBadgeStatut() {
        String texte = getBadgeTexte();
        String couleur = getCouleurStatut();

        Label badge = new Label(texte);
        badge.setPadding(new Insets(5, 14, 5, 14));
        badge.setStyle(
                "-fx-background-color: " + couleur + ";" +
                        "-fx-background-radius: 20;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-weight: bold;"
        );

        if (etapeIndex == -1) animerPulsation(badge);
        return badge;
    }

    // ── Séparateur fin ────────────────────────────────────────────
    private Region creerDivider() {
        Region div = new Region();
        div.setPrefHeight(1);
        div.setMaxWidth(Double.MAX_VALUE);
        div.setStyle("-fx-background-color: #F0F0F0;");
        return div;
    }

    // ── La ligne des 5 étapes ─────────────────────────────────────
    private HBox creerTimelineLigne() {
        HBox ligne = new HBox(0);
        ligne.setAlignment(Pos.CENTER);
        ligne.setPadding(new Insets(6, 0, 6, 0));

        for (int i = 0; i < ETAPES.length; i++) {
            ligne.getChildren().add(creerNoeud(i));
            if (i < ETAPES.length - 1) {
                ligne.getChildren().add(creerConnecteur(i));
            }
        }
        return ligne;
    }

    // ── Noeud d'une étape ─────────────────────────────────────────
    private VBox creerNoeud(int i) {
        boolean enRetard  = (etapeIndex == -1);
        boolean annule    = (etapeIndex == -2);
        boolean estPassee = (!enRetard && !annule && i < etapeIndex);
        boolean estActive = (!annule && i == etapeIndex) || (enRetard && i == 0);
        boolean estFuture = (!estPassee && !estActive);

        String couleur;
        if (annule)       couleur = C_GRIS;
        else if (enRetard) couleur = (i == 0) ? C_ROUGE : C_GRIS;
        else if (estPassee || estActive) couleur = ETAPES[i][2];
        else               couleur = C_GRIS;

        // ── Stack : anneau animé + cercle + icône ──
        StackPane stack = new StackPane();
        stack.setMinWidth(68);
        stack.setAlignment(Pos.CENTER);

        // Anneau pulsant pour étape courante uniquement
        if (estActive && !annule) {
            Circle anneau = new Circle(28);
            anneau.setFill(Color.TRANSPARENT);
            anneau.setStroke(Color.web(couleur));
            anneau.setStrokeWidth(1.5);
            anneau.setOpacity(0.3);
            stack.getChildren().add(anneau);
            animerAnneau(anneau);
        }

        // Cercle principal
        double rayon = estActive ? 22 : 18;
        Circle cercle = new Circle(rayon);
        if (estFuture || annule) {
            cercle.setFill(Color.web("#F5F5F5"));
            cercle.setStroke(Color.web(C_GRIS));
            cercle.setStrokeWidth(1.5);
        } else {
            cercle.setFill(Color.web(couleur));
        }

        // Icône dans le cercle
        String emoji;
        if (enRetard && i == 0) emoji = "⚠️";
        else if (estPassee)     emoji = "✓";
        else                    emoji = ETAPES[i][1];

        Label iconeLabel = new Label(emoji);
        iconeLabel.setStyle(
                "-fx-font-size: " + (estActive ? "15" : "12") + "px;" +
                        (estPassee ? "-fx-text-fill: white; -fx-font-weight: bold;" : "")
        );

        stack.getChildren().addAll(cercle, iconeLabel);

        // ── Texte sous le cercle ──
        Label nomEtape = new Label(ETAPES[i][0]);
        nomEtape.setMaxWidth(66);
        nomEtape.setWrapText(true);
        nomEtape.setTextAlignment(TextAlignment.CENTER);
        nomEtape.setAlignment(Pos.CENTER);
        nomEtape.setStyle(
                "-fx-font-size: 9px;" +
                        "-fx-font-weight: " + (estActive ? "bold" : "normal") + ";" +
                        "-fx-text-fill: " + (estFuture || annule ? C_GRIS : couleur) + ";" +
                        "-fx-text-alignment: center;"
        );

        VBox node = new VBox(6);
        node.setAlignment(Pos.CENTER);
        node.getChildren().addAll(stack, nomEtape);

        animerEtapeApparition(node, i * 100);
        return node;
    }

    // ── Connecteur horizontal entre 2 noeuds ─────────────────────
    private StackPane creerConnecteur(int indexGauche) {
        boolean complet = (etapeIndex > 0 && indexGauche < etapeIndex && etapeIndex != -1);

        Rectangle rect = new Rectangle(28, 3);
        rect.setArcWidth(3);
        rect.setArcHeight(3);
        rect.setFill(Color.web(complet ? C_VERT : C_GRIS));
        rect.setOpacity(complet ? 1.0 : 0.3);

        StackPane sp = new StackPane(rect);
        sp.setAlignment(Pos.CENTER);
        HBox.setHgrow(sp, Priority.ALWAYS);
        return sp;
    }

    // ── Message de statut coloré en bas ──────────────────────────
    private HBox creerMessageStatut() {
        LocalDate dateMaint = maintenance.getDate_maintenance();
        long j = (dateMaint != null) ? ChronoUnit.DAYS.between(LocalDate.now(), dateMaint) : 0;

        String message, bg;

        switch (etapeIndex) {
            case -2 -> { message = "❌  Maintenance annulée."; bg = "#F5F5F5"; }
            case -1 -> {
                long retard = Math.abs(j);
                message = "⚠️  En retard de " + retard + " jour(s). Veuillez reprogrammer cette maintenance.";
                bg = "#FFF3F3";
            }
            case  0 -> { message = "📅  Planifiée dans " + j + " jours (" + (dateMaint != null ? dateMaint.format(FMT) : "") + ")."; bg = "#E3F2FD"; }
            case  1 -> { message = "⏳  Échéance dans " + j + " jours. Préparez votre matériel !"; bg = "#FFF8E1"; }
            case  2 -> { message = "🔧  Intervention prévue aujourd'hui."; bg = "#F3E5F5"; }
            case  3 -> { message = "🔍  Contrôle qualité post-intervention en cours."; bg = "#E0F7FA"; }
            case  4 -> { message = "✅  Maintenance terminée avec succès !"; bg = "#E8F5E9"; }
            default  -> { message = ""; bg = "#F5F5F5"; }
        }

        Label msg = new Label(message);
        msg.setWrapText(true);
        msg.setStyle("-fx-font-size: 11px; -fx-text-fill: #424242;");
        HBox.setHgrow(msg, Priority.ALWAYS);

        HBox box = new HBox(msg);
        box.setPadding(new Insets(10, 14, 10, 14));
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 8;");
        return box;
    }

    // ── Helpers ───────────────────────────────────────────────────
    private String getCouleurStatut() {
        return switch (etapeIndex) {
            case -1 -> C_ROUGE;
            case -2 -> C_GRIS;
            case  0 -> C_BLEU;
            case  1 -> C_ORANGE;
            case  2 -> C_VIOLET;
            case  3 -> C_CYAN;
            case  4 -> C_VERT;
            default  -> C_GRIS;
        };
    }

    private String getBadgeTexte() {
        return switch (etapeIndex) {
            case -1 -> {
                long r = (maintenance.getDate_maintenance() != null)
                        ? ChronoUnit.DAYS.between(maintenance.getDate_maintenance(), LocalDate.now()) : 0;
                yield "⚠ EN RETARD " + r + "j";
            }
            case -2 -> "❌ ANNULÉE";
            case  0 -> "📅 PLANIFIÉ";
            case  1 -> "⏳ EN ATTENTE";
            case  2 -> "🔧 EN COURS";
            case  3 -> "🔍 CONTRÔLE";
            case  4 -> "✅ TERMINÉ";
            default  -> "—";
        };
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    // ═══════════════════════════════════════════════════════════
    //  ANIMATIONS
    // ═══════════════════════════════════════════════════════════

    private void animerEntree(VBox carte) {
        carte.setOpacity(0);
        carte.setTranslateY(12);
        FadeTransition fade = new FadeTransition(Duration.millis(350), carte);
        fade.setFromValue(0); fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(350), carte);
        slide.setFromY(12); slide.setToY(0);
        new ParallelTransition(fade, slide).play();
    }

    private void animerEtapeApparition(VBox node, int delaiMs) {
        node.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(280), node);
        fade.setDelay(Duration.millis(180 + delaiMs));
        fade.setFromValue(0); fade.setToValue(1);
        fade.play();
    }

    private void animerAnneau(Circle anneau) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(950), anneau);
        scale.setFromX(0.7); scale.setFromY(0.7);
        scale.setToX(1.4);   scale.setToY(1.4);
        scale.setCycleCount(Animation.INDEFINITE);
        scale.setAutoReverse(true);
        FadeTransition fade = new FadeTransition(Duration.millis(950), anneau);
        fade.setFromValue(0.5); fade.setToValue(0.05);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.setAutoReverse(true);
        new ParallelTransition(scale, fade).play();
    }

    private void animerPulsation(Label badge) {
        FadeTransition fade = new FadeTransition(Duration.millis(600), badge);
        fade.setFromValue(1.0); fade.setToValue(0.35);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.setAutoReverse(true);
        fade.play();
    }
}
