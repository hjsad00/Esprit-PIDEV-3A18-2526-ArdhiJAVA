package tn.neuron.ardhi.controllers.Parcelle_Cultures;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.util.ResourceBundle;

public class AgriculteurDashboardController implements Initializable {

    @FXML private StackPane rootPane;
    @FXML private ImageView bgImageView;
    @FXML private Rectangle bgOverlay;
    @FXML private AnchorPane heroSection;
    @FXML private Label lblBienvenue;
    @FXML private Label lblRole;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Bind background image and overlay to fill the entire window
        if (bgImageView != null) {
            bgImageView.fitWidthProperty().bind(rootPane.widthProperty());
            bgImageView.fitHeightProperty().bind(rootPane.heightProperty());
        }
        if (bgOverlay != null) {
            bgOverlay.widthProperty().bind(rootPane.widthProperty());
            bgOverlay.heightProperty().bind(rootPane.heightProperty());
        }
        if (heroSection != null) {
            heroSection.minHeightProperty().bind(rootPane.heightProperty());
        }

        UserSession session = UserSession.getInstance();
        if (session != null && session.getUser() != null) {
            User user = session.getUser();
            if (lblBienvenue != null) {
                lblBienvenue.setText("Bienvenue, " + user.getPrenom() + " " + user.getNom());
            }
            if (lblRole != null) {
                lblRole.setText("Cultures & Parcelles · " + formatRole(user.getRole()));
            }
        }
    }

    private String formatRole(tn.neuron.ardhi.models.UserAndDiag.Role role) {
        if (role == null) return "Utilisateur";
        String name = role.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }

    // ===================== SERVICE CARDS =====================

    @FXML
    void goToParcelles(javafx.event.Event event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/AgriculteurParcelles.fxml", "Ardhi - Mes Parcelles");
    }

    @FXML
    void goToCultures(javafx.event.Event event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/AgriculteurCultures.fxml", "Ardhi - Mes Cultures");
    }

    @FXML
    void goToStatistiques(javafx.event.Event event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/AgriculteurStats.fxml", "Ardhi - Statistiques");
    }

    @FXML
    void goToIrrigations(javafx.event.Event event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/IrrigationPlanner.fxml", "Ardhi - Irrigations");
    }

    @FXML
    void goToRoiFinancier(javafx.event.Event event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/RoiCalculator.fxml", "Ardhi - ROI Financier");
    }

    @FXML
    void goToCreditsAgricole(javafx.event.Event event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/CreditDossier.fxml", "Ardhi - Crédits Agricole");
    }

    // ===================== HEADER/FOOTER =====================

    @FXML
    void retourLanding(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/LandingPage.fxml", "Ardhi - Accueil");
    }

    @FXML
    void logout(ActionEvent event) {
        UserSession.getInstance().logout();
        WindowUtils.loadScene(event, "/fxml/LandingPage.fxml", "Ardhi - Accueil");
    }

    // ===================== NAVBAR NAVIGATION =====================

    @FXML
    void navDiagnostic(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/UserDiagAndComm.fxml", "Ardhi - Diagnostic IA");
    }

    @FXML
    void navMarketplace(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/marketplace/Marketplace.fxml", "Ardhi - Marketplace");
    }

    @FXML
    void navMaintenance(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/MaterielEtMaintenance/MaterielsEtMaintenance.fxml", "Ardhi - Matériels & Maintenance");
    }

    @FXML
    void navEvenements(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/Evenement/Navigationevenements.fxml", "Ardhi - Évènements");
    }

    @FXML
    void navEmployes(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/gestionemploye/Dashboard_employe_agriculture.fxml", "Ardhi - Employés");
    }
}
