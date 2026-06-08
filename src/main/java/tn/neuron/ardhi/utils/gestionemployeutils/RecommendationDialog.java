package tn.neuron.ardhi.utils.gestionemployeutils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import tn.neuron.ardhi.models.gestionemployemodel.Employe;
import tn.neuron.ardhi.services.gestionemployeservice.MatchingService;
import tn.neuron.ardhi.services.gestionemployeservice.MatchingService.RecommandationResult;

import java.util.List;
import java.util.Optional;

/**
 * 🎯 Dialogue de recommandation AI pour choisir le meilleur employé
 */
public class RecommendationDialog {

    /**
     * Afficher le dialogue de recommandation et retourner l'employé choisi
     */
    public static Optional<Employe> showAndWait(int idTache) {
        Dialog<Employe> dialog = new Dialog<>();
        dialog.setTitle("🤖 Recommandation AI");
        dialog.setHeaderText(null);
        dialog.initModality(Modality.APPLICATION_MODAL);

        // Conteneur principal
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setPrefWidth(700);
        mainContainer.setStyle("-fx-background-color: #f5f7fa;");

        // En-tête
        VBox header = creerEntete();
        mainContainer.getChildren().add(header);

        // Progress indicator pendant le chargement
        ProgressIndicator loading = new ProgressIndicator();
        loading.setPrefSize(60, 60);
        VBox loadingBox = new VBox(loading);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(40));
        mainContainer.getChildren().add(loadingBox);

        dialog.getDialogPane().setContent(mainContainer);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        // Charger les recommandations de manière asynchrone
        new Thread(() -> {
            try {
                MatchingService matchingService = new MatchingService();
                List<RecommandationResult> recommandations =
                        matchingService.recommanderEmployes(idTache, 3);

                javafx.application.Platform.runLater(() -> {
                    mainContainer.getChildren().remove(loadingBox);

                    if (recommandations.isEmpty()) {
                        mainContainer.getChildren().add(creerMessageVide());
                    } else {
                        VBox cardsContainer = creerCartesRecommandations(recommandations, dialog);
                        mainContainer.getChildren().add(cardsContainer);
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    mainContainer.getChildren().remove(loadingBox);
                    mainContainer.getChildren().add(creerMessageErreur());
                });
                e.printStackTrace();
            }
        }).start();

        return dialog.showAndWait();
    }

    /**
     * Créer l'en-tête du dialogue
     */
    private static VBox creerEntete() {
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 10, 0));

        Label icon = new Label("🤖");
        icon.setStyle("-fx-font-size: 48px;");

        Label title = new Label("Recommandation AI");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label subtitle = new Label("Sélectionnez le meilleur candidat pour cette tâche");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");

        header.getChildren().addAll(icon, title, subtitle);
        return header;
    }

    /**
     * Créer les cartes de recommandation
     */
    private static VBox creerCartesRecommandations(List<RecommandationResult> recommandations,
                                                   Dialog<Employe> dialog) {
        VBox container = new VBox(15);
        container.setPadding(new Insets(10, 0, 10, 0));

        for (int i = 0; i < recommandations.size(); i++) {
            RecommandationResult rec = recommandations.get(i);
            VBox card = creerCarteRecommandation(rec, i + 1, dialog);
            container.getChildren().add(card);
        }

        return container;
    }

    /**
     * Créer une carte de recommandation
     */
    private static VBox creerCarteRecommandation(RecommandationResult rec, int rang,
                                                 Dialog<Employe> dialog) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-border-color: " + rec.getCouleur() + ";" +
                        "-fx-border-width: 2px;" +
                        "-fx-border-radius: 12px;" +
                        "-fx-cursor: hand;"
        );
        card.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.1)));

        // Header de la carte
        HBox cardHeader = new HBox(15);
        cardHeader.setAlignment(Pos.CENTER_LEFT);

        // Badge de rang
        Label badgeRang = new Label(String.valueOf(rang));
        badgeRang.setMinSize(35, 35);
        badgeRang.setMaxSize(35, 35);
        badgeRang.setAlignment(Pos.CENTER);
        badgeRang.setStyle(
                "-fx-background-color: " + rec.getCouleur() + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 50%;"
        );

        // Emoji
        Label emoji = new Label(rec.getEmoji());
        emoji.setStyle("-fx-font-size: 32px;");

        // Infos employé
        VBox infoBox = new VBox(3);
        Label nom = new Label(rec.employe.getPrenom() + " " + rec.employe.getNom());
        nom.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label poste = new Label(rec.employe.getPoste());
        poste.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");

        Label email = new Label("📧 " + rec.employe.getEmail());
        email.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");

        infoBox.getChildren().addAll(nom, poste, email);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Score principal
        VBox scoreBox = new VBox(2);
        scoreBox.setAlignment(Pos.CENTER);
        Label scoreLabel = new Label(String.format("%.0f", rec.scoreTotal));
        scoreLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: " + rec.getCouleur() + ";");
        Label scoreText = new Label("/ 100");
        scoreText.setStyle("-fx-font-size: 12px; -fx-text-fill: #95a5a6;");
        scoreBox.getChildren().addAll(scoreLabel, scoreText);

        cardHeader.getChildren().addAll(badgeRang, emoji, infoBox, spacer, scoreBox);

        // Appréciation
        Label appreciation = new Label(rec.getAppreciation());
        appreciation.setStyle(
                "-fx-background-color: " + rec.getCouleur() + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 15px;" +
                        "-fx-padding: 6 15;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;"
        );
        HBox appreciationBox = new HBox(appreciation);
        appreciationBox.setAlignment(Pos.CENTER_LEFT);

        // Détails des scores
        GridPane scoresGrid = new GridPane();
        scoresGrid.setHgap(15);
        scoresGrid.setVgap(8);
        scoresGrid.setPadding(new Insets(10, 0, 0, 0));

        ajouterLigneScore(scoresGrid, 0, "🎯 Compétences", rec.scoreCompetences);
        ajouterLigneScore(scoresGrid, 1, "⭐ Expérience", rec.scoreExperience);
        ajouterLigneScore(scoresGrid, 2, "🏆 Performance", rec.scorePerformance);
        ajouterLigneScore(scoresGrid, 3, "📅 Disponibilité", rec.scoreDisponibilite);

        // Raison
        Label raisonLabel = new Label("💡 " + rec.raisonRecommandation);
        raisonLabel.setWrapText(true);
        raisonLabel.setStyle(
                "-fx-font-size: 12px;" +
                        "-fx-text-fill: #34495e;" +
                        "-fx-background-color: #ecf0f1;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-padding: 10;"
        );

        // Bouton de sélection
        Button btnSelect = new Button("✓ Choisir cet employé");
        btnSelect.setStyle(
                "-fx-background-color: " + rec.getCouleur() + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-padding: 10 20;" +
                        "-fx-cursor: hand;"
        );
        btnSelect.setMaxWidth(Double.MAX_VALUE);

        btnSelect.setOnAction(e -> {
            dialog.setResult(rec.employe);
            dialog.close();
        });

        // Hover effect sur la carte
        card.setOnMouseEntered(e -> {
            card.setStyle(card.getStyle() + "-fx-scale-x: 1.02; -fx-scale-y: 1.02;");
            card.setEffect(new DropShadow(15, Color.rgb(0, 0, 0, 0.2)));
        });
        card.setOnMouseExited(e -> {
            card.setStyle(card.getStyle() + "-fx-scale-x: 1.0; -fx-scale-y: 1.0;");
            card.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.1)));
        });

        card.getChildren().addAll(cardHeader, appreciationBox, scoresGrid, raisonLabel, btnSelect);
        return card;
    }

    /**
     * Ajouter une ligne de score dans la grille
     */
    private static void ajouterLigneScore(GridPane grid, int row, String label, double score) {
        Label lblLabel = new Label(label);
        lblLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        lblLabel.setPrefWidth(120);

        ProgressBar bar = new ProgressBar(score / 100.0);
        bar.setPrefWidth(200);
        bar.setPrefHeight(8);
        bar.setStyle("-fx-accent: #27ae60;");

        Label lblScore = new Label(String.format("%.0f%%", score));
        lblScore.setStyle("-fx-font-size: 12px; -fx-text-fill: #2c3e50; -fx-font-weight: bold;");
        lblScore.setPrefWidth(50);

        grid.add(lblLabel, 0, row);
        grid.add(bar, 1, row);
        grid.add(lblScore, 2, row);
    }

    /**
     * Message si aucune recommandation
     */
    private static VBox creerMessageVide() {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));

        Label icon = new Label("🤔");
        icon.setStyle("-fx-font-size: 64px;");

        Label message = new Label("Aucune recommandation disponible");
        message.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #7f8c8d;");

        Label details = new Label("Vérifiez que des compétences sont définies pour cette tâche");
        details.setStyle("-fx-font-size: 13px; -fx-text-fill: #95a5a6;");
        details.setWrapText(true);
        details.setMaxWidth(400);
        details.setAlignment(Pos.CENTER);

        box.getChildren().addAll(icon, message, details);
        return box;
    }

    /**
     * Message d'erreur
     */
    private static VBox creerMessageErreur() {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));

        Label icon = new Label("❌");
        icon.setStyle("-fx-font-size: 64px;");

        Label message = new Label("Erreur lors du calcul des recommandations");
        message.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");

        Label details = new Label("Veuillez réessayer ou contacter l'administrateur");
        details.setStyle("-fx-font-size: 13px; -fx-text-fill: #95a5a6;");

        box.getChildren().addAll(icon, message, details);
        return box;
    }
}