package tn.neuron.ardhi.controllers.MaterielEtMaintenance;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import tn.neuron.ardhi.models.UserAndDiag.Role;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

/**
 * Contrôleur pour la page Matériels & Maintenance.
 * Gère la navigation et l'affichage des services.
 */
public class MaterielsEtMaintenanceController {

    // --- FXML Fields matching MaterielsEtMaintenance.fxml ---
    @FXML
    private Label lblBienvenue;

    @FXML
    private Label lblRole;

    // Cards (HBox in FXML)
    @FXML
    private HBox materielsCard;

    @FXML
    private HBox maintenanceCard;

    // Nav Buttons
    @FXML
    private Button btnNavDiagnostics;

    @FXML
    private Button btnNavMarketplace;

    @FXML
    private Button btnNavMaintenance;

    @FXML
    private Button btnNavCultures;

    @FXML
    private Button btnNavEvents;

    @FXML
    private Button btnNavEmployees;

    @FXML
    public void initialize() {
        UserSession session = UserSession.getInstance();
        if (session != null && session.getUser() != null) {
            User user = session.getUser();
            lblBienvenue.setText("Bienvenue, " + user.getPrenom() + " " + user.getNom());

            if (lblRole != null) {
                lblRole.setText("Matériels & Maintenance · " + formatRole(user.getRole()));
            }
        }
    }

    private String formatRole(Role role) {
        if (role == null)
            return "Utilisateur";
        String name = role.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }

    // ===================== NAVIGATION HANDLERS =====================

    @FXML
    void goToDashboard(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/LandingPage.fxml", "Ardhi - Accueil");
    }

    @FXML
    void logout(ActionEvent event) {
        UserSession session = UserSession.getInstance();
        if (session != null) {
            session.cleanUserSession();
        }
        WindowUtils.loadScene(event, "/fxml/LandingPage.fxml", "Ardhi - Accueil");
    }

    // --- Navigation Bar ---

    @FXML
    void goToDiagnostics(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/ClientDashboard.fxml", "Ardhi - Diagnostics");
    }

    @FXML
    void goToMarketplace(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/marketplace/Marketplace.fxml", "Ardhi - Marketplace");
    }

    @FXML
    void goToMaintenance(ActionEvent event) {
        // Déjà sur cette page, on ne fait rien (ou on recharge)
        WindowUtils.loadScene(event, "/fxml/MaterielEtMaintenance/MaterielsEtMaintenance.fxml", "Ardhi - Matériels & Maintenance");
    }

    @FXML
    void goToCultures(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/parcelles/AgriculteurDashboard.fxml", "Ardhi - Cultures");
    }

    @FXML
    void goToEvents(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/Evenement/Navigationevenements.fxml", "Ardhi - Évènements");
    }

    @FXML
    void goToEmployees(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/gestionemploye/Dashboard_employe_agriculture.fxml", "Ardhi - Employés");
    }

    // --- Card Clicks (MouseEvent) ---

    @FXML
    void goToMaterielsFromCard(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/MaterielEtMaintenance/materiels.fxml", "Ardhi - Mes Matériels");
    }

    @FXML
    void goToMaintenanceFromCard(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/MaterielEtMaintenance/maintenance.fxml", "Ardhi - Maintenance");
    }
}
