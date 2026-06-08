package tn.neuron.ardhi.controllers.MaterielEtMaintenance;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import tn.neuron.ardhi.models.MaterielEtMaintenance.Materiel;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;

import java.net.URL;
import java.util.ResourceBundle;

public class DetailMaterielController implements Initializable {

    @FXML
    private Label labelNomMateriel;
    @FXML
    private Label labelReference;
    @FXML
    private Label labelEtat;
    @FXML
    private Label labelType;
    @FXML
    private Label labelProprietaire;

    private Materiel materiel;

    public void setMateriel(Materiel materiel) {
        this.materiel = materiel;
        afficherDetails();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Rien ici
    }

    private void afficherDetails() {
        if (materiel != null) {
            // Informations principales
            labelNomMateriel.setText(materiel.getNom());
            labelReference.setText("ID: #" + materiel.getId_materiel() + " | Type: " + materiel.getType());
            labelEtat.setText("État: " + materiel.getEtat());
            labelType.setText("Type: " + materiel.getType());

            // Nom de l'agriculteur connecté
            String nomComplet = UserSession.getInstance().getUser().getPrenom() + " " +
                    UserSession.getInstance().getUser().getNom();
            labelProprietaire.setText("Propriétaire: " + nomComplet);
        }
    }

    @FXML
    private void handlePlanifierMaintenance(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Planifier Maintenance");
        alert.setHeaderText("Fonctionnalité à venir");
        alert.setContentText("Planification de maintenance pour : " + materiel.getNom());
        alert.show();
    }

    @FXML
    private void handleGenererRapport(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Générer Rapport");
        alert.setHeaderText("Fonctionnalité à venir");
        alert.setContentText("Rapport PDF pour " + materiel.getNom());
        alert.show();
    }

    @FXML
    private void handleRetour(ActionEvent event) {
        // Fermer cette fenêtre
        labelNomMateriel.getScene().getWindow().hide();
    }
}
