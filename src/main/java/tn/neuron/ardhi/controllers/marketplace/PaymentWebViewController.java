package tn.neuron.ardhi.controllers.marketplace;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Contrôleur de la fenêtre WebView Stripe.
 *
 * Ouvre l'URL Stripe Checkout dans un WebView intégré.
 * Surveille les changements d'URL pour détecter la redirection
 * localhost:9000/payment/success.
 * Si détecté → ferme la fenêtre (le LocalPaymentServer déclenche le vrai
 * callback).
 */
public class PaymentWebViewController implements Initializable {

    @FXML
    private WebView webView;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label lblStatus;
    @FXML
    private Button btnAnnuler;

    private String checkoutUrl;
    private Consumer<Boolean> onCancelCallback; // appelé si annulation depuis le bouton

    // -------------------------------------------------------------------------

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Barre de progression liée au chargement du WebView
        progressBar.progressProperty().bind(
                webView.getEngine().getLoadWorker().progressProperty());

        // Mise à jour du label de statut
        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.RUNNING) {
                lblStatus.setText("Chargement...");
            } else if (newState == Worker.State.SUCCEEDED) {
                lblStatus.setText("Page chargée");
                progressBar.setVisible(false);
            } else if (newState == Worker.State.FAILED) {
                lblStatus.setText("Erreur de chargement");
            }
        });

        // Surveiller les redirections de l'engine (Stripe → localhost:9000)
        webView.getEngine().locationProperty().addListener((obs, oldUrl, newUrl) -> {
            if (newUrl != null && (newUrl.contains("localhost:9000/payment/success")
                    || newUrl.contains("localhost:9000/payment/cancel"))) {
                // Le LocalPaymentServer s'en occupe ; on ferme juste la WebView
                fermerFenetre();
            }
        });
    }

    /**
     * Initialise et charge l'URL Stripe.
     * Doit être appelé AVANT d'afficher la fenêtre.
     */
    public void loadCheckoutUrl(String url, Consumer<Boolean> cancelCallback) {
        this.checkoutUrl = url;
        this.onCancelCallback = cancelCallback;
        webView.getEngine().load(url);
        progressBar.setVisible(true);
    }

    @FXML
    private void annulerPaiement() {
        fermerFenetre();
        if (onCancelCallback != null) {
            onCancelCallback.accept(false);
        }
    }

    private void fermerFenetre() {
        Stage stage = (Stage) btnAnnuler.getScene().getWindow();
        if (stage != null)
            stage.close();
    }
}
