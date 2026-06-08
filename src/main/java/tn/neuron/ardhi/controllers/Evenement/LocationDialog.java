package tn.neuron.ardhi.controllers.Evenement;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.neuron.ardhi.models.Evenement.Evenement;
import tn.neuron.ardhi.services.Evenement.GoogleMapsService;

public class LocationDialog {

    private GoogleMapsService mapsService;
    private Evenement evenement;
    private String userLocation;

    public LocationDialog(Evenement evenement) {
        this.evenement = evenement;
        this.mapsService = new GoogleMapsService();
        this.userLocation = "Tunis, Tunisia";
    }

    public void show() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Localisation — " + evenement.getTitre());

        VBox root = new VBox(20);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(28));
        root.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #667A3F 0%, #8BC34A 100%);"
        );

        root.getChildren().addAll(createHeader(), createActions());

        Scene scene = new Scene(root, 520, 380);
        dialog.setScene(scene);
        dialog.show();
    }

    // ── Header card ──────────────────────────────────────────────────────
    private VBox createHeader() {
        VBox header = new VBox(12);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(28));
        header.setStyle(
                "-fx-background-color: rgba(255,255,255,0.95);" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 5);"
        );

        // Pin icon drawn with shapes (no emoji)
        StackPane pin = makePinIcon();

        Label title = new Label(evenement.getTitre());
        title.setStyle(
                "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;"
        );
        title.setWrapText(true);
        title.setMaxWidth(430);
        title.setAlignment(Pos.CENTER);

        Label location = new Label(evenement.getLieu());
        location.setStyle(
                "-fx-font-size: 14px; -fx-text-fill: #666; -fx-font-style: italic;"
        );
        location.setWrapText(true);
        location.setMaxWidth(430);
        location.setAlignment(Pos.CENTER);

        Label subtitle = new Label("Choisissez une option pour voir l'emplacement");
        subtitle.setStyle(
                "-fx-font-size: 12px; -fx-text-fill: #999; -fx-padding: 6 0 0 0;"
        );

        header.getChildren().addAll(pin, title, location, subtitle);
        return header;
    }

    /** Draws a map-pin icon using JavaFX shapes — no emoji needed */
    private StackPane makePinIcon() {
        // Circle (head of pin)
        Circle head = new Circle(14);
        head.setStyle("-fx-fill: #E74C3C;");

        // Inner dot
        Circle inner = new Circle(5);
        inner.setStyle("-fx-fill: white;");

        // Stem (line going down)
        Line stem = new Line(0, 0, 0, 22);
        stem.setStyle("-fx-stroke: #E74C3C; -fx-stroke-width: 3;");

        // Assemble
        StackPane pinStack = new StackPane();
        pinStack.setPrefSize(30, 50);
        pinStack.setAlignment(Pos.TOP_CENTER);

        VBox pinBox = new VBox();
        pinBox.setAlignment(Pos.TOP_CENTER);

        StackPane circleStack = new StackPane(head, inner);
        pinBox.getChildren().addAll(circleStack, stem);

        pinStack.getChildren().add(pinBox);
        return pinStack;
    }

    // ── Action buttons ───────────────────────────────────────────────────
    private VBox createActions() {
        VBox actions = new VBox(12);
        actions.setAlignment(Pos.CENTER);

        // Bouton 1: Voir sur la carte  (map icon = simple grid symbol)
        Button mapButton = createActionButton(
                "Voir sur la carte",
                "#3498DB",
                "\u25A6",   // ▦  grid-like map symbol
                () -> {
                    mapsService.ouvrirLocalisation(evenement);
                    showInfo("Google Maps ouvert dans votre navigateur.");
                }
        );

        // Bouton 2: Obtenir l'itinéraire  (arrow symbol)
        Button routeButton = createActionButton(
                "Obtenir l'itinéraire",
                "#27AE60",
                "\u2BC8",   // ⯈ right-pointing arrow
                () -> {
                    mapsService.ouvrirItineraire(evenement, userLocation);
                    showInfo("Itinéraire ouvert dans votre navigateur.");
                }
        );

        actions.getChildren().addAll(mapButton, routeButton);
        return actions;
    }

    private Button createActionButton(String text, String color, String icon, Runnable action) {
        // Use an HBox inside the button for reliable icon+text layout
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: white;");

        Label textLabel = new Label(text);
        textLabel.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;"
        );

        HBox content = new HBox(12, iconLabel, textLabel);
        content.setAlignment(Pos.CENTER);

        Button button = new Button();
        button.setGraphic(content);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(54);
        button.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 10, 0, 0, 3);"
        );

        // Hover effect
        String base = button.getStyle();
        button.setOnMouseEntered(e -> button.setStyle(base +
                "-fx-opacity: 0.88; -fx-scale-x: 1.015; -fx-scale-y: 1.015;"));
        button.setOnMouseExited(e  -> button.setStyle(base));

        button.setOnAction(e -> action.run());
        return button;
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setUserLocation(String location) {
        this.userLocation = location;
    }
}