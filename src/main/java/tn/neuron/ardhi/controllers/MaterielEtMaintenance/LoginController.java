package tn.neuron.ardhi.controllers.MaterielEtMaintenance;

import tn.neuron.ardhi.controllers.UserAndDiag.VerificationCode2FAController;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.models.UserAndDiag.Role;
import tn.neuron.ardhi.services.UserAndDiag.UserService;
import tn.neuron.ardhi.services.UserAndDiag.EmailService;

public class LoginController {

    @FXML
    private TextField tfEmail;
    @FXML
    private PasswordField pfMdp;

    private static String redirectFxml;

    public static void setRedirect(String fxml, String title) {
        redirectFxml = fxml;
    }

    @FXML
    private javafx.scene.control.Button btnBiometric; // Biometric Login Button

    // Use the BiometricScanner interface (using Real SourceAFIS implementation)
    private tn.neuron.ardhi.services.UserAndDiag.BiometricScanner biometricScanner = new tn.neuron.ardhi.services.UserAndDiag.SourceAfisScanner();

    @FXML
    public void initialize() {
        // Show button only if scanner is available
        boolean isAvailable = biometricScanner.isAvailable();
        if (btnBiometric != null) {
            btnBiometric.setVisible(isAvailable);
            btnBiometric.setManaged(isAvailable);
        }
    }

    @FXML
    void login(javafx.event.Event event) {
        String email = tfEmail.getText();
        String mdp = pfMdp.getText();

        UserService us = new UserService();
        try {
            User u = us.login(email, mdp);

            if (u != null) {
                processSuccessfulLogin(u);
            } else {
                WindowUtils.showAlert(Alert.AlertType.ERROR, "Echec", "Email ou mot de passe incorrect.");
            }
        } catch (Exception e) {
            WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur de Connexion",
                    "Une erreur est survenue lors de la connexion.\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void loginWithBiometric(ActionEvent event) {
        String email = tfEmail.getText();
        if (email == null || email.trim().isEmpty()) {
            WindowUtils.showAlert(Alert.AlertType.WARNING, "Email Requis",
                    "Veuillez entrer votre adresse email pour vérifier votre empreinte.");
            return;
        }

        UserService us = new UserService();
        try {
            User u = us.chercherParEmail(email);
            if (u == null) {
                WindowUtils.showAlert(Alert.AlertType.ERROR, "Utilisateur Inconnu",
                        "Aucun compte associé à cet email.");
                return;
            }

            // Check if user has enrolled fingerprint
            if (u.getFingerprintSignature() == null || u.getFingerprintSignature().isEmpty()) {
                WindowUtils.showAlert(Alert.AlertType.WARNING, "Empreinte non configurée",
                        "Aucune empreinte n'est associée à ce compte.\n" +
                                "Veuillez vous connecter avec votre mot de passe et enregistrer votre empreinte dans votre profil.");
                return;
            }

            // Trigger Biometric Scan
            biometricScanner.capture()
                    .thenAccept(capturedTemplate -> {
                        javafx.application.Platform.runLater(() -> {
                            if (capturedTemplate != null) {
                                // Verify against stored template
                                boolean match = biometricScanner.verify(capturedTemplate, u.getFingerprintSignature());

                                if (match) {
                                    processSuccessfulLogin(u);
                                } else {
                                    WindowUtils.showAlert(Alert.AlertType.ERROR, "Echec Authentification",
                                            "Empreinte non reconnue.");
                                }
                            } else {
                                // Handle null (cancelled or failed)
                                // Optionally show alert or just resume
                                WindowUtils.showAlert(Alert.AlertType.WARNING, "Annulé",
                                        "Capture d'empreinte annulée ou échouée.");
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        javafx.application.Platform.runLater(() -> {
                            WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                                    "Erreur lors de l'authentification : " + ex.getMessage());
                            ex.printStackTrace();
                        });
                        return null;
                    });

        } catch (Exception e) {
            WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Erreur lors de l'authentification : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processSuccessfulLogin(User u) {
        try {
            UserService us = new UserService();
            if (u.isTwoFactorEnabled()) {
                // 2FA Flow
                us.generate2FACode(u);

                // Send Email (in background thread to avoid freezing UI)
                new Thread(() -> {
                    try {
                        EmailService emailService = new EmailService();
                        String body = "Votre code de vérification est : " + u.getTwoFactorCode()
                                + "\n\nCe code expire dans 10 minutes.";
                        emailService.sendEmail(u.getEmail(), "Code de Vérification Ardhi", body);
                    } catch (Exception e) {
                        e.printStackTrace();
                        javafx.application.Platform.runLater(() -> WindowUtils.showAlert(Alert.AlertType.ERROR,
                                "Erreur Email", "Impossible d'envoyer le code : " + e.getMessage()));
                    }
                }).start();

                // Navigate to Verification
                // Navigate to Verification
                WindowUtils.loadScene(new javafx.event.ActionEvent(tfEmail, null),
                        "/fxml/UserAndDiag/VerificationCode2FA.fxml", "Verification",
                        (VerificationCode2FAController vc) -> {
                            vc.initData(u);
                        });

            } else {
                // Standard Flow
                // Generate JWT Token
                String token = tn.neuron.ardhi.utils.UserAndDiag.JwtUtils.generateToken(u);

                // Initialize Session with Token
                UserSession.getInstance().login(u, token);

                System.out.println("Login successful. Token: " + token); // Debug

                // Navigation logic
                // Navigation logic
                if (u.getRole() == Role.ADMIN) {
                    // Use WindowUtils to ensure proper history tracking and sizing
                    WindowUtils.loadScene(new javafx.event.ActionEvent(tfEmail, null),
                            "/fxml/UserAndDiag/AdminDashboard.fxml", "Ardhi - Admin Dashboard");
                } else {
                    // Check for pending redirect
                    if (redirectFxml != null) {
                        String fxml = redirectFxml;
                        // Clear redirect so it's not used again
                        redirectFxml = null;

                        // Use WindowUtils
                        WindowUtils.loadScene(new javafx.event.ActionEvent(tfEmail, null), fxml, "Ardhi");
                    } else {
                        // Use WindowUtils
                        WindowUtils.loadScene(new javafx.event.ActionEvent(tfEmail, null),
                                "/fxml/LandingPage.fxml", "Ardhi - Accueil");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Erreur lors du traitement de la connexion : " + e.getMessage());
        }
    }

    @FXML
    void goToInscription(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/Inscription.fxml", "Inscription");
    }

    @FXML
    void motDePasseOublie(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/MdpOublie.fxml", "Récupération de mot de passe");
    }
}