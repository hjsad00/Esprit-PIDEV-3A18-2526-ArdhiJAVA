package tn.neuron.ardhi.controllers.UserAndDiag;

import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
    @FXML
    private TextField tfPasswordVisible;
    @FXML
    private Button btnTogglePassword;

    private boolean passwordVisible = false;

    private static String redirectFxml;

    public static void setRedirect(String fxml, String title) {
        redirectFxml = fxml;
    }

    @FXML
    private javafx.scene.control.Button btnBiometric; // Biometric Login Button

    // Use the BiometricScanner interface (using Real SourceAFIS implementation)
    private tn.neuron.ardhi.services.UserAndDiag.BiometricScanner biometricScanner = new tn.neuron.ardhi.services.UserAndDiag.SourceAfisScanner();

    // Face Recognition dependencies
    @FXML
    private javafx.scene.control.Button btnFaceLogin;
    @FXML
    private javafx.scene.layout.VBox paneCameraLogin;
    @FXML
    private javafx.scene.image.ImageView cameraViewLogin;

    private tn.neuron.ardhi.services.UserAndDiag.CameraService cameraService = new tn.neuron.ardhi.services.UserAndDiag.CameraService();
    private tn.neuron.ardhi.services.UserAndDiag.FaceRecognitionService faceRecognitionService = new tn.neuron.ardhi.services.UserAndDiag.JavaCVFaceRecognizer();
    private javafx.animation.Timeline cameraLoop;

    @FXML
    public void initialize() {
        // Show button only if scanner is available
        boolean isAvailable = biometricScanner.isAvailable();
        if (btnBiometric != null) {
            btnBiometric.setVisible(isAvailable);
            btnBiometric.setManaged(isAvailable);
        }

        // Bind password fields bidirectionally
        if (tfPasswordVisible != null && pfMdp != null) {
            tfPasswordVisible.textProperty().bindBidirectional(pfMdp.textProperty());
        }
    }

    @FXML
    void togglePasswordVisibility(ActionEvent event) {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            tfPasswordVisible.setVisible(true);
            pfMdp.setVisible(false);
            btnTogglePassword.setText("🔒");
            tfPasswordVisible.requestFocus();
            tfPasswordVisible.positionCaret(tfPasswordVisible.getText().length());
        } else {
            tfPasswordVisible.setVisible(false);
            pfMdp.setVisible(true);
            btnTogglePassword.setText("👁");
            pfMdp.requestFocus();
            pfMdp.positionCaret(pfMdp.getText().length());
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

    @FXML
    void loginWithFace(ActionEvent event) {
        String email = tfEmail.getText();
        if (email == null || email.trim().isEmpty()) {
            WindowUtils.showAlert(Alert.AlertType.WARNING, "Email Requis",
                    "Veuillez entrer votre adresse email pour la reconnaissance faciale.");
            return;
        }

        try {
            UserService us = new UserService();
            User u = us.chercherParEmail(email);
            if (u == null) {
                WindowUtils.showAlert(Alert.AlertType.ERROR, "Utilisateur Inconnu",
                        "Aucun compte associé à cet email.");
                return;
            }

            if (u.getFaceSignature() == null || u.getFaceSignature().isEmpty()) {
                WindowUtils.showAlert(Alert.AlertType.WARNING, "Visage non configuré",
                        "Aucun visage n'est associé à ce compte.\n" +
                                "Veuillez vous connecter avec votre mot de passe et l'enregistrer dans votre profil.");
                return;
            }

            // Show Camera View
            paneCameraLogin.setVisible(true);
            paneCameraLogin.setManaged(true);
            tfEmail.setDisable(true);
            pfMdp.setDisable(true);
            btnFaceLogin.setDisable(true);

            WindowUtils.showCameraSourcePrompt(cameraService,
                    this::startCameraLoopLogin,
                    () -> cancelFaceLogin(null));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startCameraLoopLogin() {
        cameraLoop = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.millis(33), e -> {
            javafx.scene.image.Image img = cameraService.takeSnapshot();
            if (img != null) {
                cameraViewLogin.setImage(img);
            }
        }));
        cameraLoop.setCycleCount(javafx.animation.Animation.INDEFINITE);
        cameraLoop.play();
    }

    @FXML
    void captureAndVerifyFace(ActionEvent event) {
        if (!cameraService.isOpen())
            return;

        javafx.scene.image.Image capturedObj = cameraService.takeSnapshot();
        if (capturedObj != null) {
            cancelFaceLogin(null); // Stop camera & close preview

            String email = tfEmail.getText();
            try {
                UserService us = new UserService();
                User u = us.chercherParEmail(email);

                boolean match = faceRecognitionService.verifyFace(capturedObj, u.getFaceSignature());
                if (match) {
                    processSuccessfulLogin(u);
                } else {
                    WindowUtils.showAlert(Alert.AlertType.ERROR, "Echec Authentification", "Visage non reconnu.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur", "Vérification échouée.");
            }
        }
    }

    @FXML
    void cancelFaceLogin(ActionEvent event) {
        if (cameraLoop != null)
            cameraLoop.stop();
        cameraService.stopCamera();
        paneCameraLogin.setVisible(false);
        paneCameraLogin.setManaged(false);
        tfEmail.setDisable(false);
        pfMdp.setDisable(false);
        btnFaceLogin.setDisable(false);
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
                            "/fxml/UserAndDiag/AdminDashboard.fxml", "Ardhi - Administration");
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

    @FXML
    void goToLandingPage(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/LandingPage.fxml", "Ardhi - Accueil");
    }
}