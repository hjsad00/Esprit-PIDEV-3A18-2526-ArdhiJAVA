package tn.neuron.ardhi.controllers.MaterielEtMaintenance;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.neuron.ardhi.models.MaterielEtMaintenance.Materiel;
import tn.neuron.ardhi.services.MaterielEtMaintenance.MaterielService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class FormMaterielController implements Initializable {

    @FXML
    private Label lblTitre;
    @FXML
    private Label lblEtat;
    @FXML
    private Label lblInfo;
    @FXML
    private Label lblModifInfo;
    @FXML
    private TextField txtNom;
    @FXML
    private ComboBox<String> cmbType;
    @FXML
    private ComboBox<String> cmbEtat;
    @FXML
    private DatePicker dpDateAchat;
    @FXML
    private Button btnSauvegarder;

    private MaterielService materielService = new MaterielService();
    private Materiel materielToEdit;
    private boolean isEditMode = false;
    private MaterielsController parentController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialiser les ComboBox
        cmbType.setItems(FXCollections.observableArrayList(
                "Tracteur", "Moissonneuse", "Semoir", "Pulvérisateur",
                "Charrue", "Herse", "Autre"));

        cmbEtat.setItems(FXCollections.observableArrayList(
                "Neuf", "Bon", "Moyen", "En panne", "En maintenance"));
    }

    // Méthode appelée en mode modification
    public void setMateriel(Materiel materiel, MaterielsController parent) {
        this.materielToEdit = materiel;
        this.parentController = parent;
        this.isEditMode = true;

        // Changer le titre
        lblTitre.setText("Modifier le Matériel");
        btnSauvegarder.setText("💾 Mettre à jour");

        // Pré-remplir les champs
        txtNom.setText(materiel.getNom());
        cmbType.setValue(materiel.getType());
        cmbEtat.setValue(materiel.getEtat());
        dpDateAchat.setValue(materiel.getDate_achat());

        // Désactiver le champ État en mode modification
        cmbEtat.setDisable(true);
        lblEtat.setStyle("-fx-text-fill: #999;");

        // Afficher le message d'information
        lblModifInfo.setVisible(true);
        lblModifInfo.setManaged(true);
    }

    // Méthode pour le mode ajout
    public void setParentController(MaterielsController parent) {
        this.parentController = parent;
        this.isEditMode = false;
    }

    @FXML
    private void handleSauvegarder(ActionEvent event) {
        // Validation
        if (!validerFormulaire()) {
            return;
        }

        try {
            if (isEditMode) {
                // Mode modification
                materielToEdit.setNom(txtNom.getText().trim());
                materielToEdit.setType(cmbType.getValue());
                materielToEdit.setDate_achat(dpDateAchat.getValue());
                // Note: On ne modifie PAS l'état

                boolean success = materielService.modifierMateriel(materielToEdit);

                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès",
                            "Le matériel a été modifié avec succès !");
                    parentController.chargerMesMateriels();
                    fermerFenetre();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur",
                            "Impossible de modifier le matériel.");
                }
            } else {
                // Mode ajout
                Materiel nouveauMateriel = new Materiel();
                nouveauMateriel.setNom(txtNom.getText().trim());
                nouveauMateriel.setType(cmbType.getValue());
                nouveauMateriel.setEtat(cmbEtat.getValue());
                nouveauMateriel.setDate_achat(dpDateAchat.getValue());
                nouveauMateriel.setUser_id(UserSession.getInstance().getUser().getId());

                boolean success = materielService.ajouterMateriel(nouveauMateriel);

                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès",
                            "Le matériel a été ajouté avec succès !");
                    parentController.chargerMesMateriels();
                    fermerFenetre();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur",
                            "Impossible d'ajouter le matériel.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Une erreur s'est produite : " + e.getMessage());
        }
    }

    @FXML
    private void handleAnnuler(ActionEvent event) {
        fermerFenetre();
    }

    private boolean validerFormulaire() {
        // Vérifier le nom
        if (txtNom.getText() == null || txtNom.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation",
                    "Veuillez saisir le nom du matériel.");
            return false;
        }

        // Vérifier le type
        if (cmbType.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation",
                    "Veuillez sélectionner le type de matériel.");
            return false;
        }

        // Vérifier l'état (seulement en mode ajout)
        if (!isEditMode && cmbEtat.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation",
                    "Veuillez sélectionner l'état du matériel.");
            return false;
        }

        // Vérifier la date d'achat
        if (dpDateAchat.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation",
                    "Veuillez sélectionner la date d'achat.");
            return false;
        }

        // Vérifier que la date n'est pas dans le futur
        if (dpDateAchat.getValue().isAfter(LocalDate.now())) {
            showAlert(Alert.AlertType.WARNING, "Validation",
                    "La date d'achat ne peut pas être dans le futur.");
            return false;
        }

        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void fermerFenetre() {
        Stage stage = (Stage) txtNom.getScene().getWindow();
        stage.close();
    }
}