package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.services.UserAndDiag.UserService;
import tn.neuron.ardhi.utils.UserAndDiag.ValidationUtils;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.sql.SQLException;

public class VerificationCodeMdpOublieController {

    @FXML
    private TextField tfCode;
    @FXML
    private PasswordField tfNouveauMdp;

    private String emailUser;
    private UserService us = new UserService();

    // Cette méthode est appelée par le contrôleur précédent pour recevoir les infos
    public void setDonnees(String email) {
        this.emailUser = email;
    }

    @FXML
    void validerReset(ActionEvent event) {
        // 1. Vérifier le code
        String codeSaisiStr = tfCode.getText();
        String nouveauMdp = tfNouveauMdp.getText();

        if (codeSaisiStr.isEmpty() || nouveauMdp.isEmpty()) {
            WindowUtils.showAlert("Erreur", "Remplissez tous les champs.");
            return;
        }

        try {
            // 2. Vérifier le code en BDD
            User u = us.chercherParEmail(emailUser);
            if (u == null) {
                WindowUtils.showAlert("Erreur", "Utilisateur introuvable.");
                return;
            }

            if (!us.verifyResetPasswordCode(u, codeSaisiStr)) {
                WindowUtils.showAlert("Erreur", "Code incorrect ou expiré ! Vérifiez votre email.");
                return;
            }

            if (!ValidationUtils.validerMotDePasse(nouveauMdp)) {
                WindowUtils.showAlert("Erreur", "Mot de passe trop court (min 6).");
                return;
            }

            // 3. Si le code est bon -> On met à jour le mot de passe en BDD
            // On utilise la méthode de modification sécurisée qu'on a créée tout à l'heure
            us.modifierMotDePasse(u.getId(), nouveauMdp);

            WindowUtils.showAlert("Succès", "Mot de passe modifié avec succès ! Connectez-vous.");

            // 3. Retour au Login
            // 3. Retour au Login
            WindowUtils.loadScene(event, "/fxml/UserAndDiag/Login.fxml", "Login");

        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "Le code doit être un nombre.");
        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", e.getMessage());
        }
    }
}