package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import tn.neuron.ardhi.models.UserAndDiag.*;
import tn.neuron.ardhi.services.UserAndDiag.ReviewService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminReviewsController implements Initializable {

    @FXML
    private TableView<Review> tableReviews;
    @FXML
    private TableColumn<Review, Integer> colId;
    @FXML
    private TableColumn<Review, Integer> colDiagId;
    @FXML
    private TableColumn<Review, String> colType;
    @FXML
    private TableColumn<Review, String> colStatus;
    @FXML
    private TableColumn<Review, String> colVerdict;
    @FXML
    private TableColumn<Review, String> colExpertId;
    @FXML
    private TableColumn<Review, String> colFarmer;

    @FXML
    private TextField tfRecherche;
    @FXML
    private Label lblInfo;
    @FXML
    private TextField tfDiagId;
    @FXML
    private TextField tfPlanId;
    @FXML
    private TextField tfExpertId;
    @FXML
    private ComboBox<ReviewType> cbType;
    @FXML
    private ComboBox<ReviewStatus> cbStatus;
    @FXML
    private ComboBox<ExpertVerdict> cbVerdict;
    @FXML
    private ComboBox<FarmerResponse> cbFarmerResponse;
    @FXML
    private TextArea taNotes;
    @FXML
    private Label lblTitreFormulaire;
    @FXML
    private VBox mainContainer;

    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;

    private final ReviewService reviewService = new ReviewService();
    private List<Review> allReviews = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colDiagId.setCellValueFactory(new PropertyValueFactory<>("diagnosticId"));
            colType.setCellValueFactory(cellData -> {
                ReviewType t = cellData.getValue().getReviewType();
                return new SimpleStringProperty(t != null ? t.name() : "-");
            });
            colStatus.setCellValueFactory(cellData -> {
                ReviewStatus s = cellData.getValue().getStatus();
                return new SimpleStringProperty(s != null ? s.name() : "-");
            });
            colVerdict.setCellValueFactory(cellData -> {
                ExpertVerdict v = cellData.getValue().getExpertVerdict();
                return new SimpleStringProperty(v != null ? v.name() : "-");
            });
            colExpertId.setCellValueFactory(cellData -> {
                Integer eid = cellData.getValue().getExpertId();
                return new SimpleStringProperty(eid != null ? String.valueOf(eid) : "-");
            });
            colFarmer.setCellValueFactory(cellData -> {
                String name = cellData.getValue().getFarmerName();
                return new SimpleStringProperty(name != null ? name : "-");
            });

            cbType.setItems(FXCollections.observableArrayList(ReviewType.values()));
            cbStatus.setItems(FXCollections.observableArrayList(ReviewStatus.values()));
            cbVerdict.setItems(FXCollections.observableArrayList(ExpertVerdict.values()));
            cbFarmerResponse.setItems(FXCollections.observableArrayList(FarmerResponse.values()));

            chargerReviews();
            updateButtonState(false);

            tfRecherche.textProperty().addListener((obs, o, n) -> rechercherReviews(n));

            tableReviews.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
                if (sel != null) {
                    lblTitreFormulaire.setText("Revue #" + sel.getId());
                    tfDiagId.setText(String.valueOf(sel.getDiagnosticId()));
                    tfPlanId.setText(sel.getTreatmentPlanId() != null ? String.valueOf(sel.getTreatmentPlanId()) : "");
                    tfExpertId.setText(sel.getExpertId() != null ? String.valueOf(sel.getExpertId()) : "");
                    cbType.setValue(sel.getReviewType());
                    cbStatus.setValue(sel.getStatus());
                    cbVerdict.setValue(sel.getExpertVerdict());
                    cbFarmerResponse.setValue(sel.getFarmerResponse());
                    taNotes.setText(sel.getExpertNotes() != null ? sel.getExpertNotes() : "");
                    updateButtonState(true);
                } else {
                    lblTitreFormulaire.setText("Détails de la Revue");
                    viderChamps();
                    updateButtonState(false);
                }
            });

            tableReviews.getSelectionModel().clearSelection();
            WindowUtils.setupTableDeselection(tableReviews, mainContainer);
        } catch (Exception e) {
            WindowUtils.showAlert("Erreur", "Impossible d'initialiser la vue : " + e.getMessage());
        }
    }

    private void chargerReviews() {
        allReviews = reviewService.getAllReviews();
        tableReviews.setItems(FXCollections.observableArrayList(allReviews));
        WindowUtils.updateInfoLabel(lblInfo, allReviews.size(), "revue");
    }

    private void rechercherReviews(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            allReviews = reviewService.getAllReviews();
        } else {
            allReviews = reviewService.searchReviews(keyword.trim());
        }
        tableReviews.setItems(FXCollections.observableArrayList(allReviews));
        WindowUtils.updateInfoLabel(lblInfo, allReviews.size(), "revue");
    }

    private void updateButtonState(boolean isEditMode) {
        if (btnModifier != null) {
            btnModifier.setVisible(isEditMode);
            btnModifier.setManaged(isEditMode);
        }
        if (btnSupprimer != null) {
            btnSupprimer.setVisible(isEditMode);
            btnSupprimer.setManaged(isEditMode);
        }
    }

    @FXML
    void modifierReview(ActionEvent event) {
        Review selected = tableReviews.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        int diagId;
        try {
            diagId = Integer.parseInt(tfDiagId.getText().trim());
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "ID Diagnostic invalide.");
            return;
        }

        ReviewType type = cbType.getValue();
        ReviewStatus status = cbStatus.getValue();
        if (type == null || status == null) {
            WindowUtils.showAlert("Erreur", "Type et Statut sont obligatoires.");
            return;
        }

        selected.setDiagnosticId(diagId);
        selected.setReviewType(type);
        selected.setStatus(status);
        selected.setExpertVerdict(cbVerdict.getValue());
        selected.setExpertNotes(taNotes.getText().trim().isEmpty() ? null : taNotes.getText().trim());
        selected.setFarmerResponse(cbFarmerResponse.getValue());

        // Optional numeric fields
        String planIdStr = tfPlanId.getText().trim();
        if (!planIdStr.isEmpty()) {
            try {
                selected.setTreatmentPlanId(Integer.parseInt(planIdStr));
            } catch (NumberFormatException e) {
                WindowUtils.showAlert("Erreur", "ID Plan invalide.");
                return;
            }
        } else {
            selected.setTreatmentPlanId(null);
        }

        String expertIdStr = tfExpertId.getText().trim();
        if (!expertIdStr.isEmpty()) {
            try {
                selected.setExpertId(Integer.parseInt(expertIdStr));
            } catch (NumberFormatException e) {
                WindowUtils.showAlert("Erreur", "ID Expert invalide.");
                return;
            }
        } else {
            selected.setExpertId(null);
        }

        reviewService.updateReview(selected);
        WindowUtils.showAlert("Succès", "Revue modifiée !");
        chargerReviews();
        tableReviews.getSelectionModel().clearSelection();
    }

    @FXML
    void supprimerReview(ActionEvent event) {
        Review selected = tableReviews.getSelectionModel().getSelectedItem();
        if (selected == null) {
            WindowUtils.showAlert("Attention", "Sélectionnez une revue à supprimer.");
            return;
        }
        if (WindowUtils.showConfirmation("Supprimer cette revue ?",
                "Cette action est irréversible.")) {
            reviewService.deleteReview(selected.getId());
            chargerReviews();
            tableReviews.getSelectionModel().clearSelection();
            WindowUtils.showAlert("Succès", "Revue supprimée !");
        }
    }

    @FXML
    void rafraichir(ActionEvent event) {
        tfRecherche.clear();
        chargerReviews();
        tableReviews.getSelectionModel().clearSelection();
    }

    @FXML
    void viderChamps(ActionEvent event) {
        viderChamps();
        tableReviews.getSelectionModel().clearSelection();
    }

    private void viderChamps() {
        tfDiagId.clear();
        tfPlanId.clear();
        tfExpertId.clear();
        cbType.setValue(null);
        cbStatus.setValue(null);
        cbVerdict.setValue(null);
        cbFarmerResponse.setValue(null);
        taNotes.clear();
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Admin Dashboard");
    }
}
