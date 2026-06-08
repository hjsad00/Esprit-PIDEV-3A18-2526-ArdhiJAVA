package tn.neuron.ardhi.controllers.Evenement;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.neuron.ardhi.models.Evenement.Evenement;
import tn.neuron.ardhi.services.Evenement.*;
import tn.neuron.ardhi.services.Evenement.GeminiAIEventService.EventGenerationResult;
import tn.neuron.ardhi.services.Evenement.ParticipationPredictionService.PredictionResult;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Map;
import java.util.ResourceBundle;

public class EvenementFormController implements Initializable {

    @FXML private TextField titreField;
    @FXML private TextArea descriptionArea;   // fixed typo
    @FXML private TextField lieuField;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private ComboBox<String> typeCombo;
    @FXML private Spinner<Integer> placesSpinner;
    @FXML private TextField organisateurField;
    @FXML private Label imageFileLabel;
    @FXML private ImageView imagePreview;
    @FXML private Button uploadImageButton;
    @FXML private ComboBox<String> statutCombo;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Label formTitleLabel;
    @FXML private Label errorLabel;
    @FXML private Button btnGenerateAI;
    @FXML private Button btnPredictParticipation;
    @FXML private Label predictionLabel;
    @FXML private TextArea predictionDetailsArea;

    private EvenementService evenementService;
    private Evenement evenement;
    private EvenementListController evenementListController;
    private boolean isEditMode = false;
    private String selectedImagePath = "";

    private GeminiAIEventService openAIService;
    private ParticipationPredictionService predictionService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        evenementService = new EvenementService();
        openAIService = new GeminiAIEventService();
        predictionService = new ParticipationPredictionService();

        typeCombo.getItems().addAll("FOIRE", "FORMATION", "CONFERENCE", "ATELIER");
        typeCombo.setValue("FOIRE");

        statutCombo.getItems().addAll("A_VENIR", "EN_COURS", "TERMINE", "ANNULE");
        statutCombo.setValue("A_VENIR");

        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 50);
        placesSpinner.setValueFactory(valueFactory);
        placesSpinner.setEditable(true);

        dateDebutPicker.setValue(LocalDate.now().plusDays(7));
        dateFinPicker.setValue(LocalDate.now().plusDays(7));

        setupValidation();
        errorLabel.setVisible(false);

        if (predictionLabel != null)       predictionLabel.setVisible(false);
        if (predictionDetailsArea != null) predictionDetailsArea.setVisible(false);
        if (imagePreview != null)          imagePreview.setVisible(false);
    }

    private void setupValidation() {
        titreField.textProperty().addListener((obs, oldVal, newVal) -> {
            titreField.setStyle(newVal.trim().isEmpty() ? "-fx-border-color: red;" : "");
        });
        dateDebutPicker.valueProperty().addListener((obs, o, n) -> validateDates());
        dateFinPicker.valueProperty().addListener((obs, o, n)   -> validateDates());
    }

    private void validateDates() {
        if (dateDebutPicker.getValue() != null && dateFinPicker.getValue() != null) {
            if (dateFinPicker.getValue().isBefore(dateDebutPicker.getValue())) {
                dateFinPicker.setStyle("-fx-border-color: red;");
                errorLabel.setText("La date de fin doit être après la date de début");
                errorLabel.setVisible(true);
            } else {
                dateFinPicker.setStyle("");
                errorLabel.setVisible(false);
            }
        }
    }

    @FXML
    private void handleGenerateAI() {
        String titre = titreField.getText().trim();
        String type  = typeCombo.getValue();
        String lieu  = lieuField.getText().trim();

        if (titre.isEmpty()) { showError("Veuillez saisir un titre avant de générer le contenu IA"); return; }
        if (lieu.isEmpty())  { showError("Veuillez saisir un lieu avant de générer le contenu IA");  return; }

        btnGenerateAI.setDisable(true);
        btnGenerateAI.setText("⏳ Génération en cours...");

        new Thread(() -> {
            try {
                EventGenerationResult result = openAIService.genererEvenementComplet(titre, type, lieu);

                Platform.runLater(() -> {
                    // Fill description
                    if (result.getDescription() != null) {
                        descriptionArea.setText(result.getDescription());
                    }

                    // Handle image
                    if (result.getImagePath() != null) {
                        selectedImagePath = result.getImagePath();
                        imageFileLabel.setText("✓ Image IA générée");
                        imageFileLabel.setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");

                        // Build absolute path for preview
                        String fileName = result.getImagePath().replace("/uploads/evenements/", "");
                        File imageFile = new File(UnsplashImageService.SYMFONY_PUBLIC_PATH + "\\" + fileName);

                        // Debug prints - check your console
                        System.out.println("Preview path: " + imageFile.getAbsolutePath());
                        System.out.println("File exists: " + imageFile.exists());

                        if (imageFile.exists()) {
                            imagePreview.setImage(new Image(imageFile.toURI().toString()));
                            imagePreview.setVisible(true);
                        }
                    }

                    btnGenerateAI.setDisable(false);
                    btnGenerateAI.setText("🤖 Générer avec IA");

                    // Show success AFTER preview so dialog doesn't block rendering
                    showSuccess("Contenu généré avec succès par l'IA!");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnGenerateAI.setDisable(false);
                    btnGenerateAI.setText("🤖 Générer avec IA");

                    // Delay success dialog so JavaFX renders the image first
                    javafx.animation.PauseTransition pause =
                            new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
                    pause.setOnFinished(ev -> showSuccess("Contenu généré avec succès par l'IA!"));
                    pause.play();
                });
            }
        }).start();
    }

    @FXML
    private void handlePredictParticipation() {
        if (titreField.getText().trim().isEmpty() ||
                lieuField.getText().trim().isEmpty() ||
                dateDebutPicker.getValue() == null) {
            showError("Veuillez remplir le titre, le lieu et la date de début avant la prédiction");
            return;
        }

        Evenement tempEvent = new Evenement();
        tempEvent.setTitre(titreField.getText().trim());
        tempEvent.setType(typeCombo.getValue());
        tempEvent.setLieu(lieuField.getText().trim());
        tempEvent.setDateDebut(dateDebutPicker.getValue());
        tempEvent.setDateFin(dateFinPicker.getValue());
        tempEvent.setNombrePlacesMax(placesSpinner.getValue());

        btnPredictParticipation.setDisable(true);
        btnPredictParticipation.setText("⏳ Analyse...");

        new Thread(() -> {
            try {
                PredictionResult result = predictionService.predireParticipation(tempEvent);
                Platform.runLater(() -> {
                    afficherPrediction(result);
                    btnPredictParticipation.setDisable(false);
                    btnPredictParticipation.setText("🧠 Prédire Participation");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnPredictParticipation.setDisable(false);
                    btnPredictParticipation.setText("🧠 Prédire Participation");
                    showError("Erreur de prédiction: " + e.getMessage());
                });
            }
        }).start();
    }

    private void afficherPrediction(PredictionResult result) {
        if (predictionLabel != null) {
            predictionLabel.setText(String.format(
                    "📊 Prédiction: %d participants (%s confiance)",
                    result.getParticipantsPredits(), result.getConfianceTexte()));
            predictionLabel.setVisible(true);
            predictionLabel.setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold; -fx-font-size: 14px;");
        }
        if (predictionDetailsArea != null) {
            StringBuilder details = new StringBuilder();
            details.append("📊 PRÉDICTION DÉTAILLÉE\n━━━━━━━━━━━━━━━━━━━━━━\n\n");
            details.append(String.format("Participants prévus: %d personnes\n", result.getParticipantsPredits()));
            details.append(String.format("Confiance: %s (%.0f%%)\n\n", result.getConfianceTexte(), result.getConfiance() * 100));
            details.append("🎯 FACTEURS D'ANALYSE:\n");
            Map<String, Double> facteurs = result.getFacteurs();
            details.append(String.format("  • Type d'événement: %.0f/100\n", facteurs.get("type")));
            details.append(String.format("  • Saison/Période: %.0f/100\n",   facteurs.get("saison")));
            details.append(String.format("  • Localisation: %.0f/100\n",     facteurs.get("lieu")));
            details.append(String.format("  • Historique: %.0f/100\n\n",     facteurs.get("historique")));
            details.append("💡 RECOMMANDATIONS PRATIQUES:\n");
            for (Map.Entry<String, String> entry : result.getRecommandations().entrySet()) {
                String key = entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1);
                details.append(String.format("  • %s: %s\n", key, entry.getValue()));
            }
            predictionDetailsArea.setText(details.toString());
            predictionDetailsArea.setVisible(true);
        }
    }

    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));

        File selectedFile = fileChooser.showOpenDialog(uploadImageButton.getScene().getWindow());

        if (selectedFile != null) {
            try {
                Path uploadsDir = Paths.get(UnsplashImageService.SYMFONY_PUBLIC_PATH);
                if (!Files.exists(uploadsDir)) {
                    Files.createDirectories(uploadsDir);
                }

                String originalFileName = selectedFile.getName();
                String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
                String newFileName = "event_" + System.currentTimeMillis() + extension;

                Path destinationPath = uploadsDir.resolve(newFileName);
                Files.copy(selectedFile.toPath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

                selectedImagePath = "/uploads/evenements/" + newFileName;

                if (imageFileLabel != null) {
                    imageFileLabel.setText("✓ " + originalFileName);
                    imageFileLabel.setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
                }
                if (imagePreview != null) {
                    imagePreview.setImage(new Image(selectedFile.toURI().toString()));
                    imagePreview.setVisible(true);
                }

            } catch (IOException e) {
                e.printStackTrace();
                showError("Erreur lors du téléchargement de l'image: " + e.getMessage());
            }
        }
    }

    public void setEvenement(Evenement evenement) {
        this.evenement = evenement;
        this.isEditMode = true;
        formTitleLabel.setText("Modifier l'Événement");
        fillFormWithEventData();
    }

    private void fillFormWithEventData() {
        if (evenement != null) {
            titreField.setText(evenement.getTitre());
            descriptionArea.setText(evenement.getDescription()); // fixed
            lieuField.setText(evenement.getLieu());
            dateDebutPicker.setValue(evenement.getDateDebut());
            dateFinPicker.setValue(evenement.getDateFin());
            typeCombo.setValue(evenement.getType());
            placesSpinner.getValueFactory().setValue(evenement.getNombrePlacesMax());
            organisateurField.setText(evenement.getOrganisateur());
            statutCombo.setValue(evenement.getStatut());

            if (evenement.getImageUrl() != null && !evenement.getImageUrl().isEmpty()) {
                selectedImagePath = evenement.getImageUrl();
                if (imageFileLabel != null) {
                    imageFileLabel.setText("✓ Image existante");
                    imageFileLabel.setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
                }
                if (imagePreview != null) {
                    try {
                        String fileName = selectedImagePath.replace("/uploads/evenements/", "");
                        File imageFile = new File(
                                UnsplashImageService.SYMFONY_PUBLIC_PATH + "\\" + fileName);
                        if (imageFile.exists()) {
                            imagePreview.setImage(new Image(imageFile.toURI().toString()));
                            imagePreview.setVisible(true);
                        }
                    } catch (Exception e) {
                        System.err.println("Could not load image preview: " + e.getMessage());
                    }
                }
            }
        }
    }

    public void setEvenementListController(EvenementListController controller) {
        this.evenementListController = controller;
    }

    public void setTypeRestriction(String role) {
        typeCombo.getItems().clear();
        if ("AGRICULTEUR".equals(role)) {
            typeCombo.getItems().addAll("ATELIER", "FOIRE");
            typeCombo.setValue("ATELIER");
        } else {
            typeCombo.getItems().addAll("FOIRE", "FORMATION", "CONFERENCE", "ATELIER");
            typeCombo.setValue("FOIRE");
        }
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) return;

        try {
            if (isEditMode) {
                updateEvenementFromForm();
                if (evenementService.modifierEvenement(evenement)) {
                    showSuccess("Événement modifié avec succès");
                    closeWindow();
                } else {
                    showError("Erreur lors de la modification de l'événement");
                }
            } else {
                if (UserSession.getInstance() == null || UserSession.getInstance().getUser() == null) {
                    showError("Erreur: Utilisateur non connecté");
                    return;
                }
                Evenement newEvent = createEvenementFromForm();
                if (evenementService.creerEvenement(newEvent)) {
                    showSuccess("Événement créé avec succès");
                    closeWindow();
                } else {
                    showError("Erreur lors de la création de l'événement");
                }
            }
            if (evenementListController != null) evenementListController.refreshList();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Une erreur est survenue: " + e.getMessage());
        }
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();
        if (titreField.getText().trim().isEmpty())        errors.append("Le titre est obligatoire\n");
        if (lieuField.getText().trim().isEmpty())         errors.append("Le lieu est obligatoire\n");
        if (dateDebutPicker.getValue() == null)           errors.append("La date de début est obligatoire\n");
        if (dateFinPicker.getValue() == null)             errors.append("La date de fin est obligatoire\n");
        if (dateDebutPicker.getValue() != null && dateFinPicker.getValue() != null &&
                dateFinPicker.getValue().isBefore(dateDebutPicker.getValue()))
            errors.append("La date de fin doit être après la date de début\n");
        if (organisateurField.getText().trim().isEmpty()) errors.append("L'organisateur est obligatoire\n");
        if (UserSession.getInstance() == null || UserSession.getInstance().getUser() == null)
            errors.append("Erreur: Utilisateur non connecté\n");

        if (errors.length() > 0) {
            errorLabel.setText(errors.toString());
            errorLabel.setVisible(true);
            return false;
        }
        errorLabel.setVisible(false);
        return true;
    }

    private Evenement createEvenementFromForm() {
        int userId = UserSession.getInstance().getUser().getId();
        System.out.println("Création événement avec userId: " + userId);
        return new Evenement(
                titreField.getText().trim(),
                descriptionArea.getText().trim(), // fixed
                lieuField.getText().trim(),
                dateDebutPicker.getValue(),
                dateFinPicker.getValue(),
                typeCombo.getValue(),
                placesSpinner.getValue(),
                organisateurField.getText().trim(),
                selectedImagePath,
                userId
        );
    }

    private void updateEvenementFromForm() {
        evenement.setTitre(titreField.getText().trim());
        evenement.setDescription(descriptionArea.getText().trim()); // fixed
        evenement.setLieu(lieuField.getText().trim());
        evenement.setDateDebut(dateDebutPicker.getValue());
        evenement.setDateFin(dateFinPicker.getValue());
        evenement.setType(typeCombo.getValue());
        evenement.setNombrePlacesMax(placesSpinner.getValue());
        evenement.setOrganisateur(organisateurField.getText().trim());
        evenement.setImageUrl(selectedImagePath);
        evenement.setStatut(statutCombo.getValue());
    }

    @FXML
    private void handleCancel() { closeWindow(); }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleChooseLocation() {
        try {
            String currentLocation = lieuField.getText().trim();
            String searchQuery = currentLocation.isEmpty() ? "Tunis, Tunisia" : currentLocation;
            String encodedQuery = java.net.URLEncoder.encode(searchQuery, "UTF-8");
            String url = "https://www.google.com/maps/search/" + encodedQuery;
            if (java.awt.Desktop.isDesktopSupported())
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("📍 Sélectionner l'emplacement");
            alert.setHeaderText("Google Maps ouvert");
            TextArea instructions = new TextArea(
                    "ÉTAPES:\n\n1. Cherchez l'emplacement exact\n2. Cliquez dessus\n" +
                            "3. Copiez l'adresse complète\n4. Collez-la dans le champ 'Lieu'");
            instructions.setEditable(false);
            instructions.setWrapText(true);
            instructions.setPrefRowCount(6);
            alert.getDialogPane().setContent(instructions);
            alert.showAndWait();
            lieuField.requestFocus();

        } catch (Exception e) { showError("Erreur: " + e.getMessage()); }
    }
}