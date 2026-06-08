package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.services.UserAndDiag.UserService;
import tn.neuron.ardhi.utils.UserAndDiag.ValidationUtils;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;
import tn.neuron.ardhi.services.UserAndDiag.EmailService;
import java.sql.SQLException;

public class MdpOublieController {

    @FXML
    private TextField tfEmail;

    private UserService us = new UserService();
    private EmailService emailService = new EmailService();

    @FXML
    void envoyerCode(ActionEvent event) {
        String email = tfEmail.getText();

        if (!ValidationUtils.validerEmail(email)) {
            WindowUtils.showAlert("Erreur", "Veuillez entrer un email valide.");
            return;
        }

        try {
            // 1. Vérifier si l'utilisateur existe
            User u = us.chercherParEmail(email);
            if (u == null) {
                WindowUtils.showAlert("Erreur", "Aucun compte trouvé avec cet email.");
                return;
            }

            // 2. Générer un code aléatoire à 4 chiffres (Stored in DB)
            us.generateResetPasswordCode(u);
            // 3. Envoyer l'email (In background thread to avoid freezing UI)
            new Thread(() -> {
                try {
                    emailService.sendEmail(email, "Réinitialisation Mot de Passe",
                            "Votre code de vérification est : " + u.getResetPasswordCode());
                } catch (Exception e) {
                    e.printStackTrace();
                    javafx.application.Platform.runLater(() -> WindowUtils.showAlert("Erreur Email",
                            "Impossible d'envoyer le code : " + e.getMessage()));
                }
            }).start();

            // 4. Passer à la vue suivante (VerifierCode)
            WindowUtils.loadScene(event, "/fxml/UserAndDiag/VerificationCodeMdpOublie.fxml", "Vérification du code",
                    (VerificationCodeMdpOublieController controller) -> controller.setDonnees(email));

        } catch (SQLException | RuntimeException e) {
            WindowUtils.showAlert("Erreur", e.getMessage());
        }
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.goBack(event);
    }
}