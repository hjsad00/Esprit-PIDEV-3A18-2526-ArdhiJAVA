package tn.neuron.ardhi.utils.gestionemployeutils;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class NotificationToast {

    public static final String SUCCESS = "success";
    public static final String ERROR = "error";
    public static final String WARNING = "warning";
    public static final String INFO = "info";

    public static void showNotification(String message, String type, int durationMs) {
        Stage toastStage = new Stage();
        toastStage.initStyle(StageStyle.TRANSPARENT);
        toastStage.setAlwaysOnTop(true);

        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);
        root.setMaxWidth(400);
        root.setStyle(getStyle(type));

        Label lblMessage = new Label(message);
        lblMessage.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-wrap-text: true;");
        lblMessage.setWrapText(true);

        root.getChildren().add(lblMessage);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        toastStage.setScene(scene);

        // Position en bas à droite
        toastStage.setX(javafx.stage.Screen.getPrimary().getVisualBounds().getMaxX() - 420);
        toastStage.setY(javafx.stage.Screen.getPrimary().getVisualBounds().getMaxY() - 100);

        // Animation d'entrée
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        // Pause
        PauseTransition pause = new PauseTransition(Duration.millis(durationMs));

        // Animation de sortie
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> toastStage.close());

        SequentialTransition sequence = new SequentialTransition(fadeIn, pause, fadeOut);
        toastStage.show();
        sequence.play();
    }

    private static String getStyle(String type) {
        switch (type) {
            case SUCCESS:
                return "-fx-background-color: rgba(46, 125, 50, 0.95); -fx-background-radius: 10; -fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 5);";
            case ERROR:
                return "-fx-background-color: rgba(198, 40, 40, 0.95); -fx-background-radius: 10; -fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 5);";
            case WARNING:
                return "-fx-background-color: rgba(255, 143, 0, 0.95); -fx-background-radius: 10; -fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 5);";
            case INFO:
            default:
                return "-fx-background-color: rgba(33, 150, 243, 0.95); -fx-background-radius: 10; -fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 5);";
        }
    }
}