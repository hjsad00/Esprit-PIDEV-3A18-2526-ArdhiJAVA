package tn.neuron.ardhi.tests;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.io.IOException;

public class MainFX extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // On charge le fichier FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LandingPage.fxml"));
            Parent root = loader.load();

            // On crée la scène (la fenêtre)
            Scene scene = new Scene(root, WindowUtils.APP_WIDTH, WindowUtils.APP_HEIGHT);

            primaryStage.setTitle("Ardhi - Accueil");
            primaryStage.setScene(scene);

            // Taille homogène pour toute l'application
            primaryStage.setResizable(true);
            WindowUtils.applyStandardSize(primaryStage);
            primaryStage.centerOnScreen();
            primaryStage.show();

        } catch (IOException e) {
            System.out.println("Erreur de chargement du FXML : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}