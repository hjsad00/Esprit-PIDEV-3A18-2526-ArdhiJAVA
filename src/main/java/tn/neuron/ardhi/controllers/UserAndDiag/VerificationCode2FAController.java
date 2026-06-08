package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import tn.neuron.ardhi.models.UserAndDiag.Role;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.services.UserAndDiag.UserService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.sql.SQLException;

public class VerificationCode2FAController {

    @FXML
    private TextField tfCode;

    private User pendingUser;
    private UserService us = new UserService();

    public void initData(User user) {
        this.pendingUser = user;
    }

    @FXML
    void verifierCode(ActionEvent event) {
        String code = tfCode.getText().trim();
        if (code.isEmpty()) {
            WindowUtils.showAlert("Erreur", "Veuillez entrer le code.");
            return;
        }

        try {
            if (us.verify2FACode(pendingUser, code)) {
                // SUCCESS: Complete Login
                finishLogin(event);
            } else {
                WindowUtils.showAlert("Echec", "Code incorrect ou expiré.");
            }
        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", "Erreur serveur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void finishLogin(javafx.event.Event event) {
        try {
            // Generate JWT Token
            String token = tn.neuron.ardhi.utils.UserAndDiag.JwtUtils.generateToken(pendingUser);

            // Initialize Session with Token
            UserSession.getInstance().login(pendingUser, token);

            System.out.println("2FA Login successful. User: " + pendingUser.getEmail());

            // Navigation logic
            if (pendingUser.getRole() == Role.ADMIN) {
                WindowUtils.loadScene(event, "/fxml/AdminLandingPage.fxml", "Ardhi - Administration");
            } else {
                WindowUtils.loadScene(event, "/fxml/LandingPage.fxml", "Accueil");
            }

        } catch (Exception e) {
            WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur post-login: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void annuler(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/Login.fxml", "Connexion");
    }
}
