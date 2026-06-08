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
import tn.neuron.ardhi.utils.UserAndDiag.PasswordUtils;
import tn.neuron.ardhi.utils.UserAndDiag.PasswordUtils.PasswordStrength;
import tn.neuron.ardhi.utils.UserAndDiag.ValidationUtils;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class InscriptionController implements Initializable {

    @FXML
    private TextField tfNom;
    @FXML
    private TextField tfPrenom;
    @FXML
    private TextField tfEmail;
    @FXML
    private PasswordField pfMdp;
    @FXML
    private ComboBox<String> cbRole;

    // Phone / SMS
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

    // Password
    @FXML
    private Button btnTogglePassword;
    @FXML
    private TextField tfPasswordVisible;
    @FXML
    private ProgressBar pbPasswordStrength;
    @FXML
    private Label lblPasswordStrength;
    @FXML
    private PasswordField pfConfirmMdp;
    @FXML
    private Label lblConfirmStatus;

    // Location
    @FXML
    private TextField tfLocation;
    @FXML
    private ListView<String> lvCitySuggestions;

    // Face registration
    @FXML
    private javafx.scene.control.Button btnRegisterFace;
    @FXML
    private javafx.scene.control.Label lblFaceStatus;
    @FXML
    private javafx.scene.layout.VBox paneCameraRegister;
    @FXML
    private javafx.scene.image.ImageView cameraViewRegister;

    private tn.neuron.ardhi.services.UserAndDiag.CameraService cameraService = new tn.neuron.ardhi.services.UserAndDiag.CameraService();
    private tn.neuron.ardhi.services.UserAndDiag.FaceRecognitionService faceRecognitionService = new tn.neuron.ardhi.services.UserAndDiag.JavaCVFaceRecognizer();
    private javafx.animation.Timeline cameraLoop;

    private String capturedFaceSignature = null;

    // Services
    private final CountryCodeService countryCodeService = new CountryCodeService();
    private final SmsService smsService = new SmsService();
    private final CityAutocompleteService cityService = new CityAutocompleteService();

    private boolean phoneVerified = false;
    private boolean passwordVisible = false;
    private PauseTransition citySearchDebounce;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Roles
        cbRole.setItems(FXCollections.observableArrayList(
                "Agriculteur",
                "Agronome",
                "Client"));

        // --- Country Code ComboBox ---
        setupCountryCodeComboBox();

        // --- Password Strength Meter ---
        setupPasswordStrengthMeter();

        // --- Location Autocomplete ---
        setupLocationAutocomplete();
    }

    // ========================
    // COUNTRY CODE SETUP
    // ========================
    private void setupCountryCodeComboBox() {
        List<CountryCode> codes = countryCodeService.getAllCountryCodes();
        ObservableList<CountryCode> items = FXCollections.observableArrayList(codes);
        cbCountryCode.setItems(items);

        // Custom display: show flag-like display with code
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

        // Custom cell factory to highlight auto-detected country
        final CountryCode[] detectedCode = { null };

        // Detect country in background thread
        new Thread(() -> {
            CountryCode detected = countryCodeService.detectCountryCode();
            detectedCode[0] = detected;
            Platform.runLater(() -> {
                cbCountryCode.setValue(detected);
                // Update cell factory now that we have detection result
                cbCountryCode.setCellFactory(lv -> new ListCell<CountryCode>() {
                    @Override
                    protected void updateItem(CountryCode item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(item.getDialCode() + " " + item.getName());
                            if (detectedCode[0] != null && item.getIsoCode().equals(detectedCode[0].getIsoCode())) {
                                setStyle(
                                        "-fx-font-weight: bold; -fx-background-color: #EAFAF1; -fx-text-fill: #27ae60;");
                            } else {
                                setStyle("");
                            }
                        }
                    }
                });
            });
        }, "CountryCodeDetector").start();
    }

    // ========================
    // PHONE / SMS VERIFICATION
    // ========================
    @FXML
    void verifyPhone(ActionEvent event) {
        String phoneNumber = tfPhone.getText().trim();
        if (phoneNumber.isEmpty()) {
            WindowUtils.showAlert("Erreur", "Veuillez entrer votre numéro de téléphone.");
            return;
        }

        CountryCode cc = cbCountryCode.getValue();
        if (cc == null) {
            WindowUtils.showAlert("Erreur", "Veuillez sélectionner un indicatif pays.");
            return;
        }

        String fullNumber = cc.getDialCode() + phoneNumber;
        btnVerifyPhone.setDisable(true);
        btnVerifyPhone.setText("Envoi...");

        // Send SMS in background
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
                    // For demo purposes: show the code in a dialog if SMS sending fails
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
            WindowUtils.showAlert("Erreur", "Veuillez entrer le code de vérification.");
            return;
        }

        if (smsService.verifyCode(code)) {
            phoneVerified = true;
            lblPhoneStatus.setText("✅ Numéro vérifié");
            lblPhoneStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            hboxSmsCode.setVisible(false);
            hboxSmsCode.setManaged(false);
            tfPhone.setDisable(true);
            cbCountryCode.setDisable(true);
            btnVerifyPhone.setDisable(true);
            btnVerifyPhone.setText("Vérifié ✓");
            btnVerifyPhone.setStyle(
                    "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-cursor: default; -fx-background-radius: 8; -fx-font-size: 10; -fx-font-weight: bold;");
        } else {
            lblPhoneStatus.setText("❌ Code incorrect");
            lblPhoneStatus.setStyle("-fx-text-fill: #e74c3c;");
        }
    }

    // ========================
    // PASSWORD STRENGTH & GEN
    // ========================
    private void setupPasswordStrengthMeter() {
        // Bidirectional bind for in-place toggle
        tfPasswordVisible.textProperty().bindBidirectional(pfMdp.textProperty());

        pfMdp.textProperty().addListener((obs, oldVal, newVal) -> {
            updatePasswordStrength(newVal);
            checkConfirmMatch();
        });

        // Real-time confirm password match feedback
        pfConfirmMdp.textProperty().addListener((obs, oldVal, newVal) -> {
            checkConfirmMatch();
        });
    }

    private void checkConfirmMatch() {
        String pwd = pfMdp.getText();
        String confirm = pfConfirmMdp.getText();
        if (confirm == null || confirm.isEmpty()) {
            lblConfirmStatus.setText("");
            return;
        }
        if (pwd.equals(confirm)) {
            lblConfirmStatus.setText("✅ Les mots de passe correspondent");
            lblConfirmStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        } else {
            lblConfirmStatus.setText("❌ Les mots de passe ne correspondent pas");
            lblConfirmStatus.setStyle("-fx-text-fill: #e74c3c;");
        }
    }

    private void updatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            pbPasswordStrength.setProgress(0);
            lblPasswordStrength.setText("");
            return;
        }

        PasswordStrength strength = PasswordUtils.evaluateStrength(password);
        pbPasswordStrength.setProgress(strength.getProgress());
        lblPasswordStrength.setText(strength.getLabel());
        lblPasswordStrength.setStyle("-fx-text-fill: " + strength.getColor() + "; -fx-font-weight: bold;");

        // Update progress bar color via inline style
        pbPasswordStrength.setStyle("-fx-accent: " + strength.getColor() + ";");
    }

    @FXML
    void generatePassword(ActionEvent event) {
        String generated = PasswordUtils.generatePassword(14);
        pfMdp.setText(generated);
        pfConfirmMdp.setText(generated);

        // Show the generated password by switching to visible mode
        passwordVisible = true;
        tfPasswordVisible.setVisible(true);
        pfMdp.setVisible(false);
        btnTogglePassword.setText("🔒");

        updatePasswordStrength(generated);

        // Auto-hide after 10 seconds
        PauseTransition hideDelay = new PauseTransition(Duration.seconds(10));
        hideDelay.setOnFinished(e -> {
            passwordVisible = false;
            tfPasswordVisible.setVisible(false);
            pfMdp.setVisible(true);
            btnTogglePassword.setText("👁");
        });
        hideDelay.play();
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

            // Debounce: wait 400ms after last keystroke
            citySearchDebounce.setOnFinished(e -> searchCities(newVal.trim()));
            citySearchDebounce.playFromStart();
        });

        // When user selects a city from the list
        lvCitySuggestions.setOnMouseClicked(e -> {
            String selected = lvCitySuggestions.getSelectionModel().getSelectedItem();
            if (selected != null) {
                tfLocation.setText(selected);
                hideCitySuggestions();
            }
        });

        // Hide suggestions when focus is lost
        tfLocation.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                // Small delay to allow click on list item to register
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
                    System.out.println("[CityUI] Showing " + cities.size() + " suggestions");
                    lvCitySuggestions.setItems(FXCollections.observableArrayList(cities));
                    int displayCount = Math.min(cities.size(), 5);
                    double height = displayCount * 28.0 + 6.0;
                    lvCitySuggestions.setPrefHeight(height);
                    lvCitySuggestions.setMinHeight(height);
                    lvCitySuggestions.setVisible(true);
                    lvCitySuggestions.setManaged(true);
                    // Force layout refresh
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

    // ========================
    // REGISTRATION
    // ========================
    @FXML
    void inscrire(ActionEvent event) {
        String nom = tfNom.getText();
        String prenom = tfPrenom.getText();
        String email = tfEmail.getText();
        String mdp = pfMdp.getText();
        String roleSelectionne = cbRole.getValue();
        String locationText = tfLocation.getText();

        // 1. Champs vides (phone and location are optional for now)
        if (ValidationUtils.champsVides(nom, prenom, email, mdp)) {
            WindowUtils.showAlert("Erreur", "Nom, Prénom, Email et Mot de passe sont obligatoires.");
            return;
        }

        if (roleSelectionne == null || roleSelectionne.isEmpty()) {
            WindowUtils.showAlert("Erreur", "Veuillez sélectionner un rôle.");
            return;
        }

        // 2. Format Email
        if (!ValidationUtils.validerEmail(email)) {
            WindowUtils.showAlert("Erreur", "Format d'email invalide.");
            return;
        }

        // 3. Format Nom/Prénom
        if (!ValidationUtils.validerNom(nom) || !ValidationUtils.validerNom(prenom)) {
            WindowUtils.showAlert("Erreur", "Nom et Prénom : Lettres uniquement (pas de chiffres).");
            return;
        }

        // 4. Force du Mot de passe
        if (!ValidationUtils.validerMotDePasse(mdp)) {
            WindowUtils.showAlert("Erreur", "Le mot de passe est trop court (min. 6 caractères).");
            return;
        }

        // 4b. Confirm password match
        String confirmMdp = pfConfirmMdp.getText();
        if (!mdp.equals(confirmMdp)) {
            WindowUtils.showAlert("Erreur", "Les mots de passe ne correspondent pas.");
            return;
        }

        // 5. Phone verification check (only if phone was entered)
        String phoneText = tfPhone.getText().trim();
        if (!phoneText.isEmpty() && !phoneVerified) {
            WindowUtils.showAlert("Erreur", "Veuillez vérifier votre numéro de téléphone avant de vous inscrire.");
            return;
        }

        // Convertir le rôle en format Enum
        Role roleEnum = convertirRole(roleSelectionne);

        UserService us = new UserService();
        User u = new User(email, roleEnum, mdp, nom, prenom);

        // Set phone with country code
        if (!phoneText.isEmpty() && cbCountryCode.getValue() != null) {
            u.setPhone(cbCountryCode.getValue().getDialCode() + phoneText);
        }

        // Set location
        if (locationText != null && !locationText.trim().isEmpty()) {
            u.setLocation(locationText.trim());
        }

        if (capturedFaceSignature != null && !capturedFaceSignature.isEmpty()) {
            u.setFaceSignature(capturedFaceSignature);
        }

        try {
            us.ajouter(u);
            WindowUtils.showAlert("Succès", "Compte créé ! Redirection vers la connexion...");
            WindowUtils.loadScene(event, "/fxml/UserAndDiag/Login.fxml", "Connexion");
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                WindowUtils.showAlert("Erreur", "Cet email existe déjà.");
            } else {
                WindowUtils.showAlert("Erreur", "Erreur technique : " + e.getMessage());
            }
        }
    }

    private Role convertirRole(String roleAffiche) {
        switch (roleAffiche) {
            case "Agriculteur":
                return Role.AGRICULTEUR;
            case "Client":
                return Role.CLIENT;
            default:
                return Role.CLIENT;
        }
    }

    @FXML
    void goToLogin(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/Login.fxml", "Ardhi - Connexion");
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.goBack(event);
    }

    // ========================
    // FACE REGISTRATION (existing)
    // ========================
    @FXML
    void registerFace(ActionEvent event) {
        paneCameraRegister.setVisible(true);
        paneCameraRegister.setManaged(true);
        btnRegisterFace.setDisable(true);

        WindowUtils.showCameraSourcePrompt(cameraService,
                this::startCameraLoopRegister,
                () -> cancelFaceRegister(null));
    }

    private void startCameraLoopRegister() {
        cameraLoop = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.millis(33), e -> {
            javafx.scene.image.Image img = cameraService.takeSnapshot();
            if (img != null) {
                cameraViewRegister.setImage(img);
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
            cancelFaceRegister(null); // Stop camera & close preview

            String signature = faceRecognitionService.extractFaceSignature(capturedObj);
            if (signature != null && !signature.isEmpty()) {
                capturedFaceSignature = signature;
                lblFaceStatus.setText("Visage configuré avec succès ! ✅");
                lblFaceStatus.setStyle("-fx-text-fill: #27ae60;");
                WindowUtils.showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Succès",
                        "Visage capturé et validé.");
            } else {
                WindowUtils.showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur",
                        "Aucun visage détecté. Veuillez réessayer dans un meilleur éclairage.");
            }
        }
    }

    @FXML
    void cancelFaceRegister(ActionEvent event) {
        if (cameraLoop != null)
            cameraLoop.stop();
        cameraService.stopCamera();
        paneCameraRegister.setVisible(false);
        paneCameraRegister.setManaged(false);
        btnRegisterFace.setDisable(false);
    }
}