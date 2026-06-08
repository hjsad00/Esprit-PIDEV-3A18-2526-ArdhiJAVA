package tn.neuron.ardhi.controllers.Evenement;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.neuron.ardhi.models.Evenement.Evenement;
import tn.neuron.ardhi.models.Evenement.Participation;
import tn.neuron.ardhi.services.Evenement.EvenementService;
import tn.neuron.ardhi.services.Evenement.ParticipationService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.scene.Node;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

/**
 * Contrôleur pour gérer les inscriptions de l'utilisateur
 * Affiche "Mes Événements" - les événements auxquels l'utilisateur est inscrit
 */
public class MesEvenementsController implements Initializable {

    @FXML
    private VBox eventsContainer;

    @FXML
    private Label totalInscriptionsLabel;

    @FXML
    private Label nextEventLabel;

    @FXML
    private ComboBox<String> filterStatutCombo;

    private EvenementService evenementService;
    private ParticipationService participationService;
    private int userId;
    private String userRole;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        evenementService = new EvenementService();
        participationService = new ParticipationService();

        // Récupérer userId ET userRole
        if (UserSession.getInstance() != null && UserSession.getInstance().getUser() != null) {
            userId = UserSession.getInstance().getUser().getId();
            userRole = UserSession.getInstance().getUser().getRole().name();
        } else {
            userId = 0;
            userRole = "CLIENT";
        }

        // Initialiser le filtre
        filterStatutCombo.getItems().addAll("Tous", "A_VENIR", "EN_COURS", "TERMINE");
        filterStatutCombo.setValue("Tous");
        filterStatutCombo.setOnAction(e -> loadMyEvents());

        loadMyEvents();
        updateStatistics();
    }

    private void loadMyEvents() {
        eventsContainer.getChildren().clear();

        // Récupérer les participations de l'utilisateur
        List<Participation> participations = participationService.getParticipationsByUtilisateur(userId);

        if (participations.isEmpty()) {
            showEmptyState();
            return;
        }

        String filterStatut = filterStatutCombo.getValue();

        for (Participation participation : participations) {
            // Ignorer les participations annulées
            if ("ANNULE".equals(participation.getStatut())) {
                continue;
            }

            // Récupérer l'événement associé
            Evenement event = evenementService.getEvenementById(participation.getIdEvenement());
            if (event == null) continue;

            // Appliquer le filtre
            if (!"Tous".equals(filterStatut) && !filterStatut.equals(event.getStatut())) {
                continue;
            }

            VBox eventCard = createMyEventCard(event, participation);
            eventsContainer.getChildren().add(eventCard);
        }

        if (eventsContainer.getChildren().isEmpty()) {
            Label noResultLabel = new Label("Aucun événement trouvé avec ce filtre");
            noResultLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #999;");
            eventsContainer.getChildren().add(noResultLabel);
        }
    }

    private VBox createMyEventCard(Evenement event, Participation participation) {
        VBox card = new VBox(15);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));
        card.setStyle("-fx-border-color: #6b7a4f; -fx-border-width: 2; -fx-border-radius: 10; " +
                "-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        // Header avec titre et badges
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(event.getTitre());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label typeBadge = new Label(event.getType());
        typeBadge.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; " +
                "-fx-padding: 5 15; -fx-background-radius: 15; -fx-font-size: 12px;");

        Label statutBadge = new Label(getStatutLabel(event.getStatut()));
        String statutColor = switch (event.getStatut()) {
            case "A_VENIR" -> "#50C878";
            case "EN_COURS" -> "#F39C12";
            case "TERMINE" -> "#95A5A6";
            case "ANNULE" -> "#E74C3C";
            default -> "#3498DB";
        };
        statutBadge.setStyle("-fx-background-color: " + statutColor + "; -fx-text-fill: white; " +
                "-fx-padding: 5 15; -fx-background-radius: 15; -fx-font-size: 12px;");

        headerBox.getChildren().addAll(titleLabel, typeBadge, statutBadge);

        // Informations de l'événement
        VBox infoBox = new VBox(8);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String dateStr = event.getDateDebut().format(formatter);
        if (!event.getDateDebut().equals(event.getDateFin())) {
            dateStr += " - " + event.getDateFin().format(formatter);
        }

        Label dateLabel = new Label("📅 " + dateStr);
        Label lieuLabel = new Label("📍 " + event.getLieu());
        Label organisateurLabel = new Label("👤 " + event.getOrganisateur());

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        Label inscriptionLabel = new Label("✓ Inscrit le: " +
                participation.getDateInscription().format(dateTimeFormatter));
        inscriptionLabel.setStyle("-fx-text-fill: #50c878; -fx-font-weight: bold;");

        Label nbPersonnesLabel = new Label("👥 Nombre de personnes: " + participation.getNombrePersonnes());
        nbPersonnesLabel.setStyle("-fx-font-weight: bold;");

        infoBox.getChildren().addAll(dateLabel, lieuLabel, organisateurLabel, inscriptionLabel, nbPersonnesLabel);

        // Commentaire d'inscription si présent
        if (participation.getCommentaire() != null && !participation.getCommentaire().isEmpty()) {
            Label commentLabel = new Label("💬 " + participation.getCommentaire());
            commentLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #666;");
            commentLabel.setWrapText(true);
            infoBox.getChildren().add(commentLabel);
        }

        // Avis si présent
        if (participation.getNote() > 0) {
            HBox avisBox = new HBox(10);
            avisBox.setAlignment(Pos.CENTER_LEFT);

            Label avisLabel = new Label("⭐ Votre note: ");
            HBox starsBox = new HBox(2);
            for (int i = 1; i <= 5; i++) {
                Label star = new Label(i <= participation.getNote() ? "⭐" : "☆");
                starsBox.getChildren().add(star);
            }

            avisBox.getChildren().addAll(avisLabel, starsBox);
            infoBox.getChildren().add(avisBox);

            if (participation.getAvis() != null && !participation.getAvis().isEmpty()) {
                Label avisTextLabel = new Label(participation.getAvis());
                avisTextLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #666;");
                avisTextLabel.setWrapText(true);
                infoBox.getChildren().add(avisTextLabel);
            }
        }

        // Boutons d'action
        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER_LEFT);

        Button detailsButton = new Button("📋 Voir détails");
        detailsButton.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; " +
                "-fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");
        detailsButton.setOnAction(e -> showEventDetails(event));

        buttonsBox.getChildren().add(detailsButton);

        // Bouton annuler si événement pas encore passé
        if ("A_VENIR".equals(event.getStatut())) {
            Button cancelButton = new Button("✗ Annuler inscription");
            cancelButton.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; " +
                    "-fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");
            cancelButton.setOnAction(e -> cancelInscription(event, participation));
            buttonsBox.getChildren().add(cancelButton);
        }

        // Bouton avis si événement terminé et pas encore d'avis
        if ("TERMINE".equals(event.getStatut()) && participation.getNote() == 0) {
            Button avisButton = new Button("⭐ Donner mon avis");
            avisButton.setStyle("-fx-background-color: #F39C12; -fx-text-fill: white; " +
                    "-fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");
            avisButton.setOnAction(e -> addAvis(participation));
            buttonsBox.getChildren().add(avisButton);
        }

        card.getChildren().addAll(headerBox, new Separator(), infoBox, buttonsBox);
        return card;
    }

    private void showEmptyState() {
        VBox emptyBox = new VBox(20);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(50));

        Label iconLabel = new Label("📅");
        iconLabel.setStyle("-fx-font-size: 64px;");

        Label messageLabel = new Label("Vous n'êtes inscrit à aucun événement");
        messageLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #666;");

        Label hintLabel = new Label("Parcourez les événements disponibles et inscrivez-vous!");
        hintLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #999;");

        emptyBox.getChildren().addAll(iconLabel, messageLabel, hintLabel);
        eventsContainer.getChildren().add(emptyBox);
    }

    private void showEventDetails(Evenement event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Evenement/EvenementDetails.fxml"));
            Parent root = loader.load();

            EvenementDetailsController controller = loader.getController();
            controller.setEvenement(event);

            Stage stage = new Stage();
            stage.setTitle("Détails de l'Événement");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // Rafraîchir après fermeture
            loadMyEvents();
            updateStatistics();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors de l'ouverture des détails");
        }
    }

    private void cancelInscription(Evenement event, Participation participation) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Annuler votre inscription");
        alert.setContentText("Êtes-vous sûr de vouloir annuler votre inscription à \"" +
                event.getTitre() + "\" ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (participationService.annulerParticipation(event.getId(), userId)) {
                showSuccess("Inscription annulée avec succès");
                loadMyEvents();
                updateStatistics();
            } else {
                showError("Erreur lors de l'annulation");
            }
        }
    }

    /**
     * ✅ FIXED: Now properly uses AjouterAvisController and AjouterAvis.fxml
     */
    private void addAvis(Participation participation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Evenement/AjouterAvis.fxml"));
            Parent root = loader.load();

            AjouterAvisController controller = loader.getController();

            // Get the event to pass its title
            Evenement event = evenementService.getEvenementById(participation.getIdEvenement());
            if (event != null) {
                controller.setEventTitle(event.getTitre());
            }
            controller.setParticipation(participation);

            Stage stage = new Stage();
            stage.setTitle("Donner mon avis");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // Refresh after closing
            loadMyEvents();
            updateStatistics();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors de l'ouverture du formulaire d'avis");
        }
    }

    private void updateStatistics() {
        int total = participationService.getNombreParticipationsByUtilisateur(userId);
        totalInscriptionsLabel.setText(String.valueOf(total));

        // Trouver le prochain événement
        List<Participation> participations = participationService.getParticipationsByUtilisateur(userId);
        Evenement nextEvent = null;

        for (Participation p : participations) {
            if (!"CONFIRME".equals(p.getStatut())) continue;

            Evenement event = evenementService.getEvenementById(p.getIdEvenement());
            if (event != null && "A_VENIR".equals(event.getStatut())) {
                if (nextEvent == null || event.getDateDebut().isBefore(nextEvent.getDateDebut())) {
                    nextEvent = event;
                }
            }
        }

        if (nextEvent != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            nextEventLabel.setText(nextEvent.getTitre() + " - " +
                    nextEvent.getDateDebut().format(formatter));
        } else {
            nextEventLabel.setText("Aucun événement à venir");
        }
    }

    @FXML
    private void handleRefresh() {
        loadMyEvents();
        updateStatistics();
    }

    /**
     * Modifier le nombre de personnes pour une participation
     */
    private void modifierNombrePersonnes(Participation participation) {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(participation.getNombrePersonnes()));
        dialog.setTitle("Modifier le nombre de personnes");
        dialog.setHeaderText("Modification de l'inscription");
        dialog.setContentText("Nombre de personnes:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(nbStr -> {
            try {
                int nouveauNombre = Integer.parseInt(nbStr);

                if (nouveauNombre < 1) {
                    showError("Le nombre de personnes doit être au moins 1");
                    return;
                }

                if (nouveauNombre > 50) {
                    showError("Le nombre de personnes ne peut pas dépasser 50");
                    return;
                }

                if (participationService.updateNombrePersonnes(participation.getId(), nouveauNombre)) {
                    showSuccess("Nombre de personnes mis à jour: " + nouveauNombre);
                    loadMyEvents();
                    updateStatistics();
                } else {
                    showError("Erreur lors de la mise à jour");
                }
            } catch (NumberFormatException e) {
                showError("Veuillez entrer un nombre valide");
            }
        });
    }

    private String getStatutLabel(String statut) {
        return switch (statut) {
            case "A_VENIR" -> "À venir";
            case "EN_COURS" -> "En cours";
            case "TERMINE" -> "Terminé";
            case "ANNULE" -> "Annulé";
            default -> statut;
        };
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleRetour(javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/Evenement/NavigationEvenements.fxml")
            );
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            stage.setScene(new Scene(
                    root,
                    WindowUtils.APP_WIDTH,
                    WindowUtils.APP_HEIGHT
            ));
            stage.setTitle("Ardhi - Module Événements");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}