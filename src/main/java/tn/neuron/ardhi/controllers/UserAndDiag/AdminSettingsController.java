package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import tn.neuron.ardhi.utils.UserAndDiag.SubscriptionConfig;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la page de paramètres admin.
 * Permet de configurer les limites du tier gratuit.
 */
public class AdminSettingsController implements Initializable {

    @FXML
    private TextField tfDiagnosticsParHeure;
    @FXML
    private CheckBox cbAccesTraitement;
    @FXML
    private CheckBox cbAccesPlanTraitement;
    @FXML
    private Label lblStatus;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chargerValeurs();
    }

    private void chargerValeurs() {
        // Load current values from config
        tfDiagnosticsParHeure.setText(String.valueOf(SubscriptionConfig.getFreeDiagnosticsParHeure()));
        cbAccesTraitement.setSelected(SubscriptionConfig.isFreeAccesTraitement());
        cbAccesPlanTraitement.setSelected(SubscriptionConfig.isFreeAccesPlanTraitement());
        lblStatus.setText("");
    }

    @FXML
    void sauvegarder(ActionEvent event) {
        try {
            // Validate and save diagnostics per hour
            int diagnostics = Integer.parseInt(tfDiagnosticsParHeure.getText().trim());
            if (diagnostics < -1) {
                WindowUtils.showAlert("Erreur", "Le nombre de diagnostics doit être >= -1 (-1 = illimité)");
                return;
            }

            // Save values
            SubscriptionConfig.setFreeDiagnosticsParHeure(diagnostics);
            SubscriptionConfig.setFreeAccesTraitement(cbAccesTraitement.isSelected());
            SubscriptionConfig.setFreeAccesPlanTraitement(cbAccesPlanTraitement.isSelected());

            lblStatus.setText("✅ Paramètres sauvegardés avec succès !");
            lblStatus.setStyle("-fx-text-fill: #27ae60;");

        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "Veuillez entrer une valeur numérique valide pour les diagnostics.");
        }
    }

    @FXML
    void reinitialiser(ActionEvent event) {
        // Reset to defaults
        tfDiagnosticsParHeure.setText("3");
        cbAccesTraitement.setSelected(false);
        cbAccesPlanTraitement.setSelected(false);
        lblStatus.setText("↺ Valeurs réinitialisées (non sauvegardées)");
        lblStatus.setStyle("-fx-text-fill: #C4A574;");
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Admin Dashboard");
    }
}
