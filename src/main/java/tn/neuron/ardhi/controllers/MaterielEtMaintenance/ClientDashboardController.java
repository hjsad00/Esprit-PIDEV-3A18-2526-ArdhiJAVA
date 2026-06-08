package tn.neuron.ardhi.controllers.MaterielEtMaintenance;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.models.UserAndDiag.Role;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ClientDashboardController implements Initializable {

    @FXML
    private Label lblBienvenue;
    @FXML
    private Label lblRole;
    @FXML
    private VBox productManagementCard;
    @FXML
    private VBox ordersReceivedCard;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User currentUser = UserSession.getInstance().getUser();
        if (currentUser != null) {
            lblBienvenue.setText("Bonjour, " + currentUser.getPrenom() + " " + currentUser.getNom());

            // Formater le rôle pour l'affichage
            Role role = currentUser.getRole();
            String roleName = (role != null) ? role.name() : "CLIENT";

            String roleNettoye = roleName.substring(0, 1).toUpperCase() + roleName.substring(1).toLowerCase();
            lblRole.setText("Espace " + roleNettoye);

            // Afficher la gestion des produits et commandes reçues seulement pour les
            // agriculteurs
            if (role == Role.AGRICULTEUR) {
                if (productManagementCard != null) {
                    productManagementCard.setVisible(true);
                    productManagementCard.setManaged(true);
                }
                if (ordersReceivedCard != null) {
                    ordersReceivedCard.setVisible(true);
                    ordersReceivedCard.setManaged(true);
                }
            }
        }
    }

    @FXML
    void goToProfil(MouseEvent event) {
        naviguerVers(event, "/fxml/UserAndDiag/Profil.fxml", "Mon Profil");
    }

    @FXML
    void goToAbonnement(MouseEvent event) {
        naviguerVers(event, "/fxml/Sabonner.fxml", "Nos Offres Premium");
    }

    @FXML
    void goToDiagnostics(MouseEvent event) {
        naviguerVers(event, "/fxml/UserDiagnostic.fxml", "Diagnostic IA de vos Plantes");
    }

    @FXML
    void goToHistoriqueDiagnostics(MouseEvent event) {
        naviguerVers(event, "/fxml/DiagnosticHistory.fxml", "Historique des Diagnostics");
    }

    @FXML
    private void goToMarketplace(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/marketplace/CatalogueProduit.fxml"));

            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToProductManagement(MouseEvent event) {
        // Vérifier que l'utilisateur est bien un agriculteur
        User currentUser = UserSession.getInstance().getUser();
        if (currentUser == null || currentUser.getRole() != Role.AGRICULTEUR) {
            return; // Ne rien faire si ce n'est pas un agriculteur
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/marketplace/GestionProduits.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, WindowUtils.APP_WIDTH, WindowUtils.APP_HEIGHT));
            stage.setTitle("Mes Produits");

            stage.setResizable(true);
            WindowUtils.applyStandardSize(stage);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur de navigation vers la gestion des produits");
        }
    }

    @FXML
    private void goToCommandes(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/marketplace/Commandes.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, WindowUtils.APP_WIDTH, WindowUtils.APP_HEIGHT));
            stage.setTitle("Mes Commandes");

            stage.setResizable(true);
            WindowUtils.applyStandardSize(stage);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur de navigation vers les commandes");
        }
    }

    @FXML
    private void goToCommandesVendeur(MouseEvent event) {
        // Vérifier que l'utilisateur est bien un agriculteur
        User currentUser = UserSession.getInstance().getUser();
        if (currentUser == null || currentUser.getRole() != Role.AGRICULTEUR) {
            return; // Ne rien faire si ce n'est pas un agriculteur
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/marketplace/CommandesVendeur.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, WindowUtils.APP_WIDTH, WindowUtils.APP_HEIGHT));
            stage.setTitle("Commandes reçues");

            stage.setResizable(true);
            WindowUtils.applyStandardSize(stage);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur de navigation vers les commandes vendeur");
        }
    }

    @FXML
    void goToParcelles(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/AgriculteurParcelles.fxml", "Mes Parcelles");
    }

    @FXML
    void goToCultures(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/AgriculteurCultures.fxml", "Mes Cultures");
    }

    @FXML
    void logout(ActionEvent event) {
        UserSession.getInstance().cleanUserSession();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserAndDiag/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, WindowUtils.APP_WIDTH, WindowUtils.APP_HEIGHT));
            stage.setTitle("Connexion");

            stage.setResizable(true);
            WindowUtils.applyStandardSize(stage);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void naviguerVers(MouseEvent event, String fxmlPath, String titre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, WindowUtils.APP_WIDTH, WindowUtils.APP_HEIGHT));
            stage.setTitle(titre);

            stage.setResizable(true);
            WindowUtils.applyStandardSize(stage);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur de navigation vers : " + fxmlPath);
        }
    }

    @FXML
    void goToEvenements(MouseEvent event) {
        naviguerVers(event, "/fxml/NavigationEvenements.fxml", "Événements & Communauté");
    }
}
