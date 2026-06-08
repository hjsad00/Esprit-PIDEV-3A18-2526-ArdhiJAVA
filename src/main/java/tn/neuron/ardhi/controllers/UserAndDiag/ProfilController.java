package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.models.UserAndDiag.Role;
import tn.neuron.ardhi.services.UserAndDiag.CityAutocompleteService;
import tn.neuron.ardhi.services.UserAndDiag.CountryCodeService;
import tn.neuron.ardhi.services.UserAndDiag.CountryCodeService.CountryCode;
import tn.neuron.ardhi.services.UserAndDiag.SmsService;
import tn.neuron.ardhi.services.UserAndDiag.UserService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.ValidationUtils;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class ProfilController implements Initializable {

    @FXML
    private TextField tfNom;
    @FXML
    private TextField tfPrenom;
    @FXML
    private TextField tfEmail;
    @FXML
    private PasswordField tfAncienMdp;
    @FXML
    private PasswordField tfNouveauMdp;
    @FXML
    private ComboBox<String> cbRole;
    @FXML
    private CheckBox cbTwoFactor;
    // Phone verification
    @FXML
    private ComboBox<CountryCode> cbCountryCode;
    @FXML
    private TextField tfPhone;
    @FXML
    private Button btnVerifyPhone;
    @FXML
    private HBox hboxSmsCode;
    @FXML
    private TextField tfSmsCode;
    @FXML
    private Button btnValidateCode;
    @FXML
    private Label lblPhoneStatus;

    // Location
    @FXML
    private TextField tfLocation;
    @FXML
    private ListView<String> lvCitySuggestions;

    private User userCourant;
    private UserService us = new UserService();

    // private UserCourant userCourant;
    @FXML
    private javafx.scene.control.Button btnEnroll;
    @FXML
    private Label lblEnrollStatus;
    @FXML
    private javafx.scene.control.Button btnDeleteFingerprint;

    // Face Registration dependencies
    @FXML
    private javafx.scene.control.Button btnEnrollFace;
    @FXML
    private javafx.scene.control.Button btnDeleteFace;
    @FXML
    private Label lblFaceEnrollStatus;
    @FXML
    private javafx.scene.layout.VBox paneCameraFace;
    @FXML
    private javafx.scene.image.ImageView cameraViewFace;

    private tn.neuron.ardhi.services.UserAndDiag.CameraService cameraService = new tn.neuron.ardhi.services.UserAndDiag.CameraService();
    private tn.neuron.ardhi.services.UserAndDiag.FaceRecognitionService faceRecognitionService = new tn.neuron.ardhi.services.UserAndDiag.JavaCVFaceRecognizer();
    private javafx.animation.Timeline cameraLoop;

    private tn.neuron.ardhi.services.UserAndDiag.BiometricScanner biometricScanner = new tn.neuron.ardhi.services.UserAndDiag.SourceAfisScanner();

    private final CityAutocompleteService cityService = new CityAutocompleteService();
    private final CountryCodeService countryCodeService = new CountryCodeService();
    private final SmsService smsService = new SmsService();
    private boolean phoneVerified = false;
    private String originalPhone = null; // Track the phone saved in DB
    private PauseTransition citySearchDebounce;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialiser les rôles disponibles
        cbRole.setItems(FXCollections.observableArrayList(
                "Agriculteur",
                "Agronome",
                "Client"));

        userCourant = UserSession.getInstance().getUser();

        if (userCourant != null) {
            tfNom.setText(userCourant.getNom());
            tfPrenom.setText(userCourant.getPrenom());
            tfEmail.setText(userCourant.getEmail());

            // Afficher le rôle actuel
            if (userCourant.getRole() != null) {
                String roleName = userCourant.getRole().name();
                // Simple formatting: Capitalize first letter
                String roleAffiche = roleName.substring(0, 1).toUpperCase() + roleName.substring(1).toLowerCase();
                cbRole.setValue(roleAffiche);
            }
            cbTwoFactor.setSelected(userCourant.isTwoFactorEnabled());

            // Load location
            if (userCourant.getLocation() != null) {
                tfLocation.setText(userCourant.getLocation());
            }

            // Load phone — parse existing phone into country code + local number
            originalPhone = userCourant.getPhone();
            if (originalPhone != null && !originalPhone.isEmpty()) {
                phoneVerified = true; // Already saved = already verified
                parseExistingPhone(originalPhone);
                markPhoneAsVerified();
            }

            // Check enrollment status
            updateEnrollStatus();
            updateFaceEnrollStatus();
        }

        // Setup location autocomplete & phone country codes
        setupLocationAutocomplete();
        setupCountryCodeComboBox();
    }

    // ========================
    // COUNTRY CODE & PHONE VERIFICATION
    // ========================
    private void setupCountryCodeComboBox() {
        List<CountryCode> codes = countryCodeService.getAllCountryCodes();
        ObservableList<CountryCode> items = FXCollections.observableArrayList(codes);
        cbCountryCode.setItems(items);

        cbCountryCode.setConverter(new StringConverter<CountryCode>() {
            @Override
            public String toString(CountryCode cc) {
                if (cc == null)
                    return "";
                return cc.getDialCode() + " " + cc.getName();
            }

            @Override
            public CountryCode fromString(String string) {
                return null;
            }
        });

        // If no existing phone, auto-detect country code in background
        if (originalPhone == null || originalPhone.isEmpty()) {
            new Thread(() -> {
                CountryCode detected = countryCodeService.detectCountryCode();
                Platform.runLater(() -> cbCountryCode.setValue(detected));
            }, "CountryCodeDetector").start();
        }

        // When phone text changes, reset verification if number differs from saved
        tfPhone.textProperty().addListener((obs, oldVal, newVal) -> {
            if (phoneVerified && !isCurrentPhoneSameAsSaved()) {
                phoneVerified = false;
                resetPhoneVerificationUI();
            }
        });
    }

    /**
     * Parses an existing full phone number (e.g. "+21612345678") into the
     * country code ComboBox selection and the local number TextField.
     */
    private void parseExistingPhone(String fullPhone) {
        List<CountryCode> codes = countryCodeService.getAllCountryCodes();
        // Try to match the longest dial code first
        CountryCode matched = null;
        String localPart = fullPhone;
        for (CountryCode cc : codes) {
            if (fullPhone.startsWith(cc.getDialCode())) {
                if (matched == null || cc.getDialCode().length() > matched.getDialCode().length()) {
                    matched = cc;
                    localPart = fullPhone.substring(cc.getDialCode().length());
                }
            }
        }
        if (matched != null) {
            cbCountryCode.setValue(matched);
            tfPhone.setText(localPart);
        } else {
            tfPhone.setText(fullPhone);
        }
    }

    private void markPhoneAsVerified() {
        btnVerifyPhone.setText("Vérifié ✓");
        btnVerifyPhone.setStyle(
                "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-cursor: default; -fx-background-radius: 8; -fx-font-size: 11; -fx-font-weight: bold;");
        btnVerifyPhone.setDisable(true);
    }

    private void resetPhoneVerificationUI() {
        btnVerifyPhone.setText("Vérifier");
        btnVerifyPhone.setStyle(
                "-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 8; -fx-font-size: 11; -fx-font-weight: bold;");
        btnVerifyPhone.setDisable(false);
        hboxSmsCode.setVisible(false);
        hboxSmsCode.setManaged(false);
        lblPhoneStatus.setText("");
    }

    private boolean isCurrentPhoneSameAsSaved() {
        if (originalPhone == null || originalPhone.isEmpty())
            return false;
        String currentFull = buildFullPhoneNumber();
        return originalPhone.equals(currentFull);
    }

    private String buildFullPhoneNumber() {
        String phoneText = tfPhone.getText() != null ? tfPhone.getText().trim() : "";
        if (phoneText.isEmpty())
            return "";
        CountryCode cc = cbCountryCode.getValue();
        if (cc != null) {
            return cc.getDialCode() + phoneText;
        }
        return phoneText;
    }

    @FXML
    void verifyPhone(ActionEvent event) {
        String phoneNumber = tfPhone.getText().trim();
        if (phoneNumber.isEmpty()) {
            WindowUtils.showAlert(Alert.AlertType.WARNING, "Erreur", "Veuillez entrer votre numéro de téléphone.");
            return;
        }
        CountryCode cc = cbCountryCode.getValue();
        if (cc == null) {
            WindowUtils.showAlert(Alert.AlertType.WARNING, "Erreur", "Veuillez sélectionner un indicatif pays.");
            return;
        }

        String fullNumber = cc.getDialCode() + phoneNumber;
        btnVerifyPhone.setDisable(true);
        btnVerifyPhone.setText("Envoi...");

        new Thread(() -> {
            boolean sent = smsService.sendVerificationCode(fullNumber);
            Platform.runLater(() -> {
                if (sent) {
                    hboxSmsCode.setVisible(true);
                    hboxSmsCode.setManaged(true);
                    lblPhoneStatus.setText("Code envoyé !");
                    lblPhoneStatus.setStyle("-fx-text-fill: #3498db;");
                    btnVerifyPhone.setText("Renvoyer");
                    btnVerifyPhone.setDisable(false);
                } else {
                    // Demo mode: show the code in status
                    String code = smsService.getLastGeneratedCode();
                    hboxSmsCode.setVisible(true);
                    hboxSmsCode.setManaged(true);
                    lblPhoneStatus.setText("Mode démo — code: " + code);
                    lblPhoneStatus.setStyle("-fx-text-fill: #e67e22;");
                    btnVerifyPhone.setText("Renvoyer");
                    btnVerifyPhone.setDisable(false);
                }
            });
        }, "SmsSender").start();
    }

    @FXML
    void validateSmsCode(ActionEvent event) {
        String code = tfSmsCode.getText().trim();
        if (code.isEmpty()) {
            WindowUtils.showAlert(Alert.AlertType.WARNING, "Erreur", "Veuillez entrer le code de vérification.");
            return;
        }

        if (smsService.verifyCode(code)) {
            phoneVerified = true;
            lblPhoneStatus.setText("✅ Numéro vérifié");
            lblPhoneStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            hboxSmsCode.setVisible(false);
            hboxSmsCode.setManaged(false);
            markPhoneAsVerified();
        } else {
            lblPhoneStatus.setText("❌ Code incorrect");
            lblPhoneStatus.setStyle("-fx-text-fill: #e74c3c;");
        }
    }

    // ========================
    // LOCATION AUTOCOMPLETE
    // ========================
    private void setupLocationAutocomplete() {
        citySearchDebounce = new PauseTransition(Duration.millis(400));

        tfLocation.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().length() < 2) {
                hideCitySuggestions();
                return;
            }
            citySearchDebounce.setOnFinished(e -> searchCities(newVal.trim()));
            citySearchDebounce.playFromStart();
        });

        lvCitySuggestions.setOnMouseClicked(e -> {
            String selected = lvCitySuggestions.getSelectionModel().getSelectedItem();
            if (selected != null) {
                tfLocation.setText(selected);
                hideCitySuggestions();
            }
        });

        tfLocation.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                PauseTransition delay = new PauseTransition(Duration.millis(200));
                delay.setOnFinished(e -> hideCitySuggestions());
                delay.play();
            }
        });
    }

    private void searchCities(String query) {
        new Thread(() -> {
            List<String> cities = cityService.searchCities(query);
            Platform.runLater(() -> {
                if (cities.isEmpty()) {
                    hideCitySuggestions();
                } else {
                    lvCitySuggestions.setItems(FXCollections.observableArrayList(cities));
                    int displayCount = Math.min(cities.size(), 5);
                    double height = displayCount * 28.0 + 6.0;
                    lvCitySuggestions.setPrefHeight(height);
                    lvCitySuggestions.setMinHeight(height);
                    lvCitySuggestions.setVisible(true);
                    lvCitySuggestions.setManaged(true);
                    lvCitySuggestions.getParent().requestLayout();
                }
            });
        }, "CitySearch").start();
    }

    private void hideCitySuggestions() {
        lvCitySuggestions.setVisible(false);
        lvCitySuggestions.setManaged(false);
        lvCitySuggestions.setPrefHeight(0);
        lvCitySuggestions.setMinHeight(0);
    }

    // Conversion helpers
    private Role convertirRoleVersDB(String roleAffiche) {
        try {
            return Role.valueOf(roleAffiche.toUpperCase());
        } catch (Exception e) {
            return Role.CLIENT;
        }
    }

    // ... existing helper methods ...

    private void updateEnrollStatus() {
        boolean isEnrolled = userCourant.getFingerprintSignature() != null
                && !userCourant.getFingerprintSignature().isEmpty();

        if (isEnrolled) {
            lblEnrollStatus.setText("Empreinte configurée");
            lblEnrollStatus.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

            // Show Delete button, Hide Enroll button
            if (btnEnroll != null) {
                btnEnroll.setVisible(false);
                btnEnroll.setManaged(false);
            }
            if (btnDeleteFingerprint != null) {
                btnDeleteFingerprint.setVisible(true);
                btnDeleteFingerprint.setManaged(true);
            }

        } else {
            lblEnrollStatus.setText("Non configuré");
            lblEnrollStatus.setStyle("-fx-text-fill: #888; -fx-font-style: italic;");

            // Show Enroll button, Hide Delete button
            if (btnEnroll != null) {
                btnEnroll.setVisible(true);
                btnEnroll.setManaged(true);
                btnEnroll.setText("Configurer Empreinte"); // Clearer text
                btnEnroll.setStyle(
                        "-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
            }
            if (btnDeleteFingerprint != null) {
                btnDeleteFingerprint.setVisible(false);
                btnDeleteFingerprint.setManaged(false);
            }
        }
    }

    @FXML
    void deleteFingerprint(ActionEvent event) {
        if (WindowUtils.showConfirmation("Suppression Empreinte",
                "Êtes-vous sûr de vouloir supprimer votre empreinte biométrique ?")) {
            try {
                userCourant.setFingerprintSignature(null);
                us.modifier(userCourant);
                updateEnrollStatus();
                WindowUtils.showAlert(Alert.AlertType.INFORMATION, "Succès", "Empreinte supprimée avec succès.");
            } catch (SQLException e) {
                WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    @FXML
    void enrollFingerprint(ActionEvent event) {
        if (!biometricScanner.isAvailable()) {
            WindowUtils.showAlert(Alert.AlertType.ERROR, "Non Disponible", "Le scanner d'empreinte n'est pas détecté.");
            return;
        }

        // Simulate Capture
        biometricScanner.capture()
                .thenAccept(template -> {
                    javafx.application.Platform.runLater(() -> {
                        if (template != null) {
                            try {
                                // Save the captured template to the user profile
                                userCourant.setFingerprintSignature(template);
                                us.modifier(userCourant);

                                updateEnrollStatus();
                                WindowUtils.showAlert(Alert.AlertType.INFORMATION, "Succès",
                                        "Empreinte enregistrée avec succès !");
                            } catch (SQLException e) {
                                WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                                        "Erreur lors de l'enregistrement : " + e.getMessage());
                            }
                        } else {
                            WindowUtils.showAlert(Alert.AlertType.WARNING, "Echec",
                                    "La capture d'empreinte a échoué ou a été annulée.");
                        }
                    });
                })
                .exceptionally(ex -> {
                    javafx.application.Platform.runLater(() -> {
                        WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                                "Erreur inattendue : " + ex.getMessage());
                        ex.printStackTrace();
                    });
                    return null;
                });
    }

    private void updateFaceEnrollStatus() {
        boolean isEnrolled = userCourant.getFaceSignature() != null && !userCourant.getFaceSignature().isEmpty();

        if (isEnrolled) {
            lblFaceEnrollStatus.setText("Visage configuré");
            lblFaceEnrollStatus.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

            if (btnEnrollFace != null) {
                btnEnrollFace.setVisible(false);
                btnEnrollFace.setManaged(false);
            }
            if (btnDeleteFace != null) {
                btnDeleteFace.setVisible(true);
                btnDeleteFace.setManaged(true);
            }
        } else {
            lblFaceEnrollStatus.setText("Non configuré");
            lblFaceEnrollStatus.setStyle("-fx-text-fill: #888; -fx-font-style: italic;");

            if (btnEnrollFace != null) {
                btnEnrollFace.setVisible(true);
                btnEnrollFace.setManaged(true);
                btnEnrollFace.setText("Configurer Visage");
            }
            if (btnDeleteFace != null) {
                btnDeleteFace.setVisible(false);
                btnDeleteFace.setManaged(false);
            }
        }
    }

    @FXML
    void deleteFace(ActionEvent event) {
        if (WindowUtils.showConfirmation("Suppression Visage", "Êtes-vous sûr de vouloir supprimer votre visage ?")) {
            try {
                userCourant.setFaceSignature(null);
                us.modifier(userCourant);
                updateFaceEnrollStatus();
                WindowUtils.showAlert(Alert.AlertType.INFORMATION, "Succès", "Visage supprimé avec succès.");
            } catch (SQLException e) {
                WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    @FXML
    void enrollFace(ActionEvent event) {
        paneCameraFace.setVisible(true);
        paneCameraFace.setManaged(true);
        btnEnrollFace.setDisable(true);

        WindowUtils.showCameraSourcePrompt(cameraService,
                this::startCameraLoopFace,
                () -> cancelFaceEnroll(null));
    }

    private void startCameraLoopFace() {
        cameraLoop = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.millis(33), e -> {
            javafx.scene.image.Image img = cameraService.takeSnapshot();
            if (img != null) {
                cameraViewFace.setImage(img);
            }
        }));
        cameraLoop.setCycleCount(javafx.animation.Animation.INDEFINITE);
        cameraLoop.play();
    }

    @FXML
    void captureFace(ActionEvent event) {
        if (!cameraService.isOpen())
            return;

        javafx.scene.image.Image capturedObj = cameraService.takeSnapshot();
        if (capturedObj != null) {
            cancelFaceEnroll(null); // Stop camera & close preview

            String signature = faceRecognitionService.extractFaceSignature(capturedObj);
            if (signature != null && !signature.isEmpty()) {
                try {
                    userCourant.setFaceSignature(signature);
                    us.modifier(userCourant);
                    updateFaceEnrollStatus();
                    WindowUtils.showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Succès",
                            "Visage enregistré avec succès !");
                } catch (SQLException e) {
                    WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                            "Erreur lors de l'enregistrement : " + e.getMessage());
                }
            } else {
                WindowUtils.showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur",
                        "Aucun visage détecté. Veuillez réessayer dans un meilleur éclairage.");
            }
        }
    }

    @FXML
    void cancelFaceEnroll(ActionEvent event) {
        if (cameraLoop != null)
            cameraLoop.stop();
        cameraService.stopCamera();
        paneCameraFace.setVisible(false);
        paneCameraFace.setManaged(false);
        btnEnrollFace.setDisable(false);
    }

    @FXML
    void modifier(ActionEvent event) {
        String nom = tfNom.getText();
        String prenom = tfPrenom.getText();
        String email = tfEmail.getText();
        String ancienMdp = tfAncienMdp.getText();
        String nouveauMdp = tfNouveauMdp.getText();
        String roleSelectionne = cbRole.getValue();

        // 1. Contrôles de saisie
        if (ValidationUtils.champsVides(nom, prenom, email, roleSelectionne)) {
            WindowUtils.showAlert(Alert.AlertType.WARNING, "Attention", "Tous les champs sont obligatoires.");
            return;
        }
        if (!ValidationUtils.validerEmail(email)) {
            WindowUtils.showAlert(Alert.AlertType.WARNING, "Erreur", "Format email invalide.");
            return;
        }
        if (!ValidationUtils.validerNom(nom) || !ValidationUtils.validerNom(prenom)) {
            WindowUtils.showAlert(Alert.AlertType.WARNING, "Erreur", "Nom/Prénom invalides (lettres uniquement).");
            return;
        }

        // 2. Mise à jour Objet
        userCourant.setNom(nom);
        userCourant.setPrenom(prenom);
        userCourant.setEmail(email);
        userCourant.setRole(convertirRoleVersDB(roleSelectionne));
        userCourant.setTwoFactorEnabled(cbTwoFactor.isSelected());

        // Update phone (only if verified or unchanged)
        String phoneText = tfPhone.getText() != null ? tfPhone.getText().trim() : "";
        if (!phoneText.isEmpty()) {
            if (!phoneVerified) {
                WindowUtils.showAlert(Alert.AlertType.WARNING, "Erreur",
                        "Veuillez vérifier votre nouveau numéro de téléphone avant d'enregistrer.");
                return;
            }
            userCourant.setPhone(buildFullPhoneNumber());
        } else {
            userCourant.setPhone(null);
        }

        // Update location
        String locationText = tfLocation.getText() != null ? tfLocation.getText().trim() : "";
        userCourant.setLocation(locationText.isEmpty() ? null : locationText);
        // Biometrics handled separately by Enroll button

        try {
            // 3. Update Infos de base
            us.modifier(userCourant);

            // 4. Update Mot de Passe si nouveau mot de passe rempli
            if (!nouveauMdp.isEmpty()) {
                // Vérifier que l'ancien mot de passe est fourni
                if (ancienMdp.isEmpty()) {
                    WindowUtils.showAlert(Alert.AlertType.WARNING, "Erreur",
                            "L'ancien mot de passe est requis pour changer de mot de passe.");
                    return;
                }

                // Vérifier que l'ancien mot de passe est correct
                if (!us.verifierMotDePasse(userCourant.getId(), ancienMdp)) {
                    WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                            "L'ancien mot de passe est incorrect.");
                    return;
                }

                // Vérifier la longueur du nouveau mot de passe
                if (!ValidationUtils.validerMotDePasse(nouveauMdp)) {
                    WindowUtils.showAlert(Alert.AlertType.WARNING, "Erreur MDP",
                            "Le nouveau mot de passe doit faire 6 caractères minimum.");
                    return;
                }

                // Tout est OK, on change le mot de passe
                us.modifierMotDePasse(userCourant.getId(), nouveauMdp);
                tfAncienMdp.clear();
                tfNouveauMdp.clear();

                WindowUtils.showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "Profil et mot de passe mis à jour avec succès !");
            } else {
                WindowUtils.showAlert(Alert.AlertType.INFORMATION, "Succès", "Profil mis à jour avec succès !");
            }

        } catch (SQLException e) {
            WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur", "Echec : " + e.getMessage());
        }
    }

    @FXML
    void supprimer(ActionEvent event) {
        if (WindowUtils.showConfirmation("Suppression définitive", "Êtes-vous sûr ? Cette action est irréversible.")) {
            try {
                us.supprimer(userCourant.getId());
                UserSession.getInstance().cleanUserSession();
                WindowUtils.loadScene(event, "/fxml/UserAndDiag/Login.fxml", "Connexion");
            } catch (SQLException e) {
                WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        }
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.goBack(event);
    }

    @FXML
    void goToLandingPage(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/LandingPage.fxml", "Ardhi - Accueil");
    }

}