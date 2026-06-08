package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import tn.neuron.ardhi.models.UserAndDiag.Diagnostic;
import tn.neuron.ardhi.models.UserAndDiag.Severity;
import tn.neuron.ardhi.services.UserAndDiag.DiagnosticService;
import tn.neuron.ardhi.services.UserAndDiag.UserService;
import tn.neuron.ardhi.utils.UserAndDiag.ImgBBService;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;
import javafx.concurrent.Task;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import java.text.SimpleDateFormat;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AdminDiagnosticsController implements Initializable {

    @FXML
    private TableView<Diagnostic> tableDiagnostics;
    @FXML
    private TableColumn<Diagnostic, Integer> colId;
    @FXML
    private TableColumn<Diagnostic, String> colDate;
    @FXML
    private TableColumn<Diagnostic, Integer> colUserId;
    @FXML
    private TableColumn<Diagnostic, Float> colConfiance;
    @FXML
    private TableColumn<Diagnostic, String> colResultatIA;
    @FXML
    private TableColumn<Diagnostic, String> colLocation;
    @FXML
    private TableColumn<Diagnostic, String> colSeverity;

    @FXML
    private TextField tfRecherche;
    @FXML
    private Label lblInfo;

    @FXML
    private TextField tfUserId;
    @FXML
    private DatePicker dpDateScan;
    @FXML
    private TextField tfResultatIA;
    @FXML
    private TextField tfConfiance;
    @FXML
    private ImageView imgPreview;
    @FXML
    private Label lblTitreFormulaire;
    @FXML
    private VBox mainContainer;

    // Location and Severity fields
    @FXML
    private TextField tfLatitude;
    @FXML
    private TextField tfLongitude;
    @FXML
    private TextField tfLocationLabel;
    @FXML
    private ComboBox<Severity> cbSeverity;

    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;

    private DiagnosticService ds = new DiagnosticService();
    private UserService us = new UserService();

    private File selectedFile;
    private List<Diagnostic> allDiagnostics = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colDate.setCellValueFactory(cellData -> {
                if (cellData.getValue().getDateScan() != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                    return new SimpleStringProperty(dateFormat.format(cellData.getValue().getDateScan()));
                } else {
                    return new SimpleStringProperty("");
                }
            });
            colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
            colConfiance.setCellValueFactory(new PropertyValueFactory<>("confiance"));
            colResultatIA.setCellValueFactory(new PropertyValueFactory<>("resultatIA"));

            // Map new columns
            colLocation.setCellValueFactory(new PropertyValueFactory<>("locationLabel"));
            colSeverity.setCellValueFactory(cellData -> {
                Severity s = cellData.getValue().getSeverity();
                return new SimpleStringProperty(s != null ? s.name() : "-");
            });

            chargerDiagnostics();
            updateButtonState(false);

            // Initialize severity ComboBox
            cbSeverity.setItems(FXCollections.observableArrayList(Severity.values()));

            // Real-time search listener
            tfRecherche.textProperty().addListener((observable, oldValue, newValue) -> {
                rechercherDiagnostics(newValue);
            });

            tableDiagnostics.getSelectionModel().selectedItemProperty()
                    .addListener((obs, oldSelection, newSelection) -> {

                        if (newSelection != null) {
                            // IMPORTANT: Reset selectedFile to avoid carrying over an image from a previous
                            // action
                            selectedFile = null;

                            lblTitreFormulaire.setText("Correction du diagnostic #" + newSelection.getId());
                            tfUserId.setText(String.valueOf(newSelection.getUserId()));
                            tfResultatIA.setText(newSelection.getResultatIA());
                            tfConfiance.setText(String.valueOf(newSelection.getConfiance()));

                            if (newSelection.getDateScan() != null) {
                                dpDateScan.setValue(newSelection.getDateScan().toLocalDateTime().toLocalDate());
                            }

                            if (newSelection.getImageScannee() != null
                                    && !newSelection.getImageScannee().isEmpty()) {
                                try {
                                    String url = newSelection.getImageScannee();
                                    if (url.startsWith("http") || url.startsWith("file:")) {
                                        imgPreview.setImage(new Image(url));
                                    } else {
                                        imgPreview.setImage(new Image(new File(url).toURI().toString()));
                                    }
                                } catch (Exception e) {
                                    imgPreview.setImage(null);
                                }
                            } else {
                                imgPreview.setImage(null);
                            }

                            // Load location and severity
                            tfLatitude.setText(
                                    newSelection.getLatitude() != null ? String.valueOf(newSelection.getLatitude())
                                            : "");
                            tfLongitude.setText(
                                    newSelection.getLongitude() != null ? String.valueOf(newSelection.getLongitude())
                                            : "");
                            tfLocationLabel.setText(
                                    newSelection.getLocationLabel() != null ? newSelection.getLocationLabel() : "");
                            cbSeverity.setValue(newSelection.getSeverity());

                            updateButtonState(true);
                        } else {
                            lblTitreFormulaire.setText("Ajouter un nouveau diagnostic");
                            viderChamps();
                            updateButtonState(false);
                        }
                    });

            tableDiagnostics.getSelectionModel().clearSelection();

            // Setup table deselection
            WindowUtils.setupTableDeselection(tableDiagnostics, mainContainer);

        } catch (Exception e) {
            WindowUtils.showAlert("Erreur Fatal", "Impossible d'initialiser la vue diagnostic : " + e.getMessage());
        }
    }

    private void chargerDiagnostics() {
        try {
            allDiagnostics = ds.recuperer();
            tableDiagnostics.setItems(FXCollections.observableArrayList(allDiagnostics));
            mettreAJourInfo(allDiagnostics.size());
        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", "Impossible de charger l'historique : " + e.getMessage());
        }
    }

    private void rechercherDiagnostics(String keyword) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                allDiagnostics = ds.recuperer();
            } else {
                allDiagnostics = ds.rechercher(keyword.trim());
            }
            tableDiagnostics.setItems(FXCollections.observableArrayList(allDiagnostics));
            mettreAJourInfo(allDiagnostics.size());
        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", "Erreur lors de la recherche : " + e.getMessage());
        }
    }

    private void mettreAJourInfo(int count) {
        WindowUtils.updateInfoLabel(lblInfo, count, "diagnostic");
    }

    private void updateButtonState(boolean isEditMode) {
        if (btnAjouter != null) {
            btnAjouter.setVisible(!isEditMode);
            btnAjouter.setManaged(!isEditMode);
        }
        if (btnModifier != null) {
            btnModifier.setVisible(isEditMode);
            btnModifier.setManaged(isEditMode);
        }
        if (btnSupprimer != null) {
            btnSupprimer.setVisible(isEditMode);
            btnSupprimer.setManaged(isEditMode);
        }
    }

    // Clic events handled by WindowUtils.setupTableDeselection

    @FXML
    void choisirImage(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image pour le diagnostic");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        selectedFile = fc.showOpenDialog(null);

        if (selectedFile != null) {
            imgPreview.setImage(new Image(selectedFile.toURI().toString()));
        }
    }

    // --- Helper Method for Image Saving ---

    @FXML
    void ajouterDiagnostic(ActionEvent event) {
        int userId;
        float confiance;
        try {
            userId = Integer.parseInt(tfUserId.getText().trim());
            confiance = Float.parseFloat(tfConfiance.getText().trim());
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "Champs invalides.");
            return;
        }

        if (userId <= 0) {
            WindowUtils.showAlert("Erreur", "L'ID utilisateur doit être valide.");
            return;
        }

        // Disable UI
        if (btnAjouter != null)
            btnAjouter.setDisable(true);

        int finalUserId = userId;
        float finalConfiance = confiance;
        String resultatIA = tfResultatIA.getText();
        String latitudeStr = tfLatitude.getText().trim();
        String longitudeStr = tfLongitude.getText().trim();
        String locationLabel = tfLocationLabel.getText().trim();
        Severity severity = cbSeverity.getValue();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Check user existence in background or UI thread?
                // Creating UserService in background might be safer if it uses DB.
                // But `us` is already instantiated. Let's assume it's thread-safe enough or use
                // Platform.runLater if needed.
                // Actually, best to do checks inside call() to avoid freezing.
                if (!us.existe(finalUserId)) {
                    throw new Exception("L'utilisateur avec l'ID " + finalUserId + " n'existe pas.");
                }

                String imagePath = null;
                if (selectedFile != null) {
                    try {
                        // Try ImgBB first
                        String uploadedUrl = ImgBBService.uploadImage(selectedFile);
                        if (uploadedUrl != null) {
                            imagePath = uploadedUrl;
                        } else {
                            // Fallback to local path without copying
                            LogUtils.warn(getClass(), "ImgBB upload failed, falling back to local path.");
                            imagePath = selectedFile.getAbsolutePath();
                        }
                    } catch (Exception e) {
                        LogUtils.error(getClass(), "Image processing error", e);
                        if (imagePath == null) {
                            imagePath = selectedFile.getAbsolutePath();
                        }
                    }
                }

                Diagnostic d = new Diagnostic(imagePath, resultatIA, finalConfiance, finalUserId);

                if (!latitudeStr.isEmpty()) {
                    d.setLatitude(Double.parseDouble(latitudeStr));
                }
                if (!longitudeStr.isEmpty()) {
                    d.setLongitude(Double.parseDouble(longitudeStr));
                }
                d.setLocationLabel(locationLabel.isEmpty() ? null : locationLabel);
                d.setSeverity(severity);

                ds.ajouter(d);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            if (btnAjouter != null)
                btnAjouter.setDisable(false);
            WindowUtils.showAlert("Succès", "Diagnostic ajouté !");
            chargerDiagnostics();
            viderChamps();
            tableDiagnostics.getSelectionModel().clearSelection();
        });

        task.setOnFailed(e -> {
            if (btnAjouter != null)
                btnAjouter.setDisable(false);
            Throwable ex = task.getException();
            WindowUtils.showAlert("Erreur", "Erreur : " + ex.getMessage());
        });

        new Thread(task).start();
    }

    @FXML
    void modifierDiagnostic(ActionEvent event) {
        Diagnostic selected = tableDiagnostics.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        int userId;
        float confiance;
        try {
            userId = Integer.parseInt(tfUserId.getText().trim());
            confiance = Float.parseFloat(tfConfiance.getText().trim());
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "Champs invalides.");
            return;
        }

        if (userId <= 0) {
            WindowUtils.showAlert("Erreur", "L'ID utilisateur doit être valide.");
            return;
        }

        // Disable UI
        if (btnModifier != null)
            btnModifier.setDisable(true);

        int finalUserId = userId;
        float finalConfiance = confiance;
        String resultatIA = tfResultatIA.getText();
        String latitudeStr = tfLatitude.getText().trim();
        String longitudeStr = tfLongitude.getText().trim();
        String locationLabel = tfLocationLabel.getText().trim();
        Severity severity = cbSeverity.getValue();
        java.time.LocalDate localDate = dpDateScan.getValue();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (!us.existe(finalUserId)) {
                    throw new Exception("L'utilisateur avec l'ID " + finalUserId + " n'existe pas.");
                }

                if (selectedFile != null) {
                    try {
                        String newPath;
                        // Try ImgBB
                        String uploadedUrl = ImgBBService.uploadImage(selectedFile);
                        if (uploadedUrl != null) {
                            newPath = uploadedUrl;
                        } else {
                            LogUtils.warn(getClass(), "ImgBB upload failed, falling back to local path.");
                            newPath = selectedFile.getAbsolutePath();
                        }
                        selected.setImageScannee(newPath);
                    } catch (Exception e) {
                        LogUtils.error(getClass(), "Image update error", e);
                        selected.setImageScannee(selectedFile.getAbsolutePath());
                    }
                }

                selected.setUserId(finalUserId);
                selected.setResultatIA(resultatIA);
                selected.setConfiance(finalConfiance);

                if (localDate != null) {
                    selected.setDateScan(Timestamp.valueOf(LocalDateTime.of(localDate, LocalTime.now())));
                }

                if (!latitudeStr.isEmpty()) {
                    selected.setLatitude(Double.parseDouble(latitudeStr));
                } else {
                    selected.setLatitude(null);
                }
                if (!longitudeStr.isEmpty()) {
                    selected.setLongitude(Double.parseDouble(longitudeStr));
                } else {
                    selected.setLongitude(null);
                }
                selected.setLocationLabel(locationLabel.isEmpty() ? null : locationLabel);
                selected.setSeverity(severity);

                ds.modifier(selected);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            if (btnModifier != null)
                btnModifier.setDisable(false);
            WindowUtils.showAlert("Succès", "Diagnostic modifié !");
            chargerDiagnostics();
            tableDiagnostics.getSelectionModel().clearSelection();
        });

        task.setOnFailed(e -> {
            if (btnModifier != null)
                btnModifier.setDisable(false);
            Throwable ex = task.getException();
            WindowUtils.showAlert("Erreur", "Erreur : " + ex.getMessage());
        });

        new Thread(task).start();
    }

    @FXML
    void supprimerDiagnostic(ActionEvent event) {
        Diagnostic selected = tableDiagnostics.getSelectionModel().getSelectedItem();
        if (selected == null) {
            WindowUtils.showAlert("Attention", "Sélectionnez un diagnostic à supprimer.");
            return;
        }

        if (WindowUtils.showConfirmation("Supprimer ce diagnostic ?",
                "Cette action effacera l'historique pour cet utilisateur.")) {
            try {
                ds.supprimer(selected.getId());
                chargerDiagnostics();
                tableDiagnostics.getSelectionModel().clearSelection();
                WindowUtils.showAlert("Succès", "Diagnostic supprimé !");
            } catch (SQLException e) {
                WindowUtils.showAlert("Erreur", "Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    @FXML
    void rafraichir(ActionEvent event) {
        tfRecherche.clear();
        chargerDiagnostics();
        tableDiagnostics.getSelectionModel().clearSelection();
    }

    @FXML
    void voirStatistiques(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/DiagnosticStatistiques.fxml", "Statistiques Diagnostics");
    }

    @FXML
    void viderChamps(ActionEvent event) {
        viderChamps();
        tableDiagnostics.getSelectionModel().clearSelection();
    }

    private void viderChamps() {
        tfUserId.clear();
        tfResultatIA.clear();
        tfConfiance.clear();
        dpDateScan.setValue(null);
        imgPreview.setImage(null);
        selectedFile = null;
        // Clear location and severity fields
        tfLatitude.clear();
        tfLongitude.clear();
        tfLocationLabel.clear();
        cbSeverity.setValue(null);
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Admin Dashboard");
    }
}