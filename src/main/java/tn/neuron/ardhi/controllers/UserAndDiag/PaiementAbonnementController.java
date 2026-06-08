package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Alert;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import tn.neuron.ardhi.models.UserAndDiag.Abonnement;
import tn.neuron.ardhi.models.UserAndDiag.Offre;
import tn.neuron.ardhi.services.UserAndDiag.AbonnementService;
import tn.neuron.ardhi.services.UserAndDiag.StripeService;
import tn.neuron.ardhi.services.UserAndDiag.StripeService.CheckoutResult;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class PaiementAbonnementController {

    @FXML
    private Label lblTypePack;
    @FXML
    private Label lblPrixMensuel;
    @FXML
    private Label lblMontant;
    @FXML
    private DatePicker dpDateExpiration;
    @FXML
    private Button btnStripe;
    @FXML
    private HBox hboxStatus;
    @FXML
    private ProgressIndicator piLoading;
    @FXML
    private Label lblStatus;

    private String typePack;
    private float prixMensuel;
    private float prixTotal;

    private SabonnerController parentController;
    private int offreId = 0;

    private final StripeService stripeService = new StripeService();
    private Timeline pollingTimeline;
    private String currentSessionId;

    /**
     * Initialise le formulaire de paiement avec les données de l'offre.
     */
    public void setDonneesOffre(Offre offre, int moisDefaut, SabonnerController parent) {
        this.offreId = offre.getId();
        this.typePack = offre.getNom();
        this.prixMensuel = offre.getPrixMensuel();
        this.parentController = parent;

        lblTypePack.setText(offre.getNom());
        lblPrixMensuel.setText(String.format("Prix : %.2f DT / mois", prixMensuel));

        LocalDate today = LocalDate.now();
        LocalDate dateDefaut = today.plusMonths(moisDefaut);

        dpDateExpiration.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(today.plusDays(1)));
            }
        });

        dpDateExpiration.setValue(dateDefaut);
        dpDateExpiration.valueProperty().addListener((obs, oldVal, newVal) -> calculerPrix());
        calculerPrix();
    }

    private void calculerPrix() {
        LocalDate today = LocalDate.now();
        LocalDate dateExpiration = dpDateExpiration.getValue();

        if (dateExpiration != null && dateExpiration.isAfter(today)) {
            long jours = ChronoUnit.DAYS.between(today, dateExpiration);
            int mois = (int) Math.ceil(jours / 30.0);
            if (mois < 1)
                mois = 1;

            this.prixTotal = Abonnement.calculerPrixTotal(prixMensuel, mois);
            lblMontant.setText(String.format("Total à payer : %.2f DT (%d mois)", prixTotal, mois));
        } else {
            lblMontant.setText("Veuillez choisir une date valide");
            this.prixTotal = 0;
        }
    }

    /**
     * Opens Stripe Checkout in the system browser and starts polling for payment.
     */
    @FXML
    void payerAvecStripe(ActionEvent event) {
        LocalDate dateExpiration = dpDateExpiration.getValue();

        if (dateExpiration == null || !dateExpiration.isAfter(LocalDate.now())) {
            WindowUtils.showAlert("Erreur", "Veuillez choisir une date d'expiration valide.");
            return;
        }
        if (prixTotal <= 0) {
            WindowUtils.showAlert("Erreur", "Prix invalide. Veuillez choisir une date valide.");
            return;
        }

        // Disable button and show loading
        btnStripe.setDisable(true);
        btnStripe.setText("Redirection...");
        hboxStatus.setVisible(true);
        hboxStatus.setManaged(true);
        lblStatus.setText("Création de la session de paiement...");

        // Convert price to cents (smallest currency unit)
        long amountCents = Math.round(prixTotal * 100);

        // Create Checkout Session in background thread
        new Thread(() -> {
            CheckoutResult result = stripeService.createCheckoutSession(
                    typePack + " - Abonnement Ardhi",
                    amountCents,
                    "eur" // Use "eur" for broader compatibility; change to "tnd" if Stripe supports it
            );

            Platform.runLater(() -> {
                if (result.success) {
                    currentSessionId = result.sessionId;

                    // Open Stripe Checkout in system browser
                    try {
                        java.awt.Desktop.getDesktop().browse(java.net.URI.create(result.checkoutUrl));
                    } catch (Exception e) {
                        // Fallback: copy URL to clipboard
                        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                        content.putString(result.checkoutUrl);
                        clipboard.setContent(content);
                        WindowUtils.showAlert("Lien copié",
                                "Le lien de paiement a été copié dans votre presse-papiers.\nCollez-le dans votre navigateur pour continuer.");
                    }

                    lblStatus.setText("En attente du paiement... (vérifiez votre navigateur)");
                    btnStripe.setText("En attente du paiement...");

                    // Start polling for payment status every 3 seconds
                    startStatusPolling();

                } else {
                    lblStatus.setText("❌ " + result.error);
                    lblStatus.setStyle("-fx-text-fill: #e74c3c;");
                    btnStripe.setDisable(false);
                    btnStripe.setText("💳  Réessayer");
                }
            });
        }, "StripeCheckout").start();
    }

    /**
     * Polls Stripe every 3 seconds to check if payment was completed.
     * Stops after 5 minutes (100 checks).
     */
    private void startStatusPolling() {
        final int[] checkCount = { 0 };

        pollingTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            checkCount[0]++;

            // Timeout after 5 minutes
            if (checkCount[0] > 100) {
                stopPolling();
                lblStatus.setText("⏳ Délai expiré. Veuillez réessayer.");
                lblStatus.setStyle("-fx-text-fill: #e67e22;");
                btnStripe.setDisable(false);
                btnStripe.setText("💳  Réessayer");
                return;
            }

            // Check in background thread
            new Thread(() -> {
                String status = stripeService.checkSessionStatus(currentSessionId);
                Platform.runLater(() -> {
                    if ("paid".equals(status)) {
                        stopPolling();
                        onPaymentSuccess();
                    }
                    // "unpaid" = still waiting, continue polling
                });
            }, "StripeStatusCheck").start();
        }));

        pollingTimeline.setCycleCount(Timeline.INDEFINITE);
        pollingTimeline.play();
    }

    private void stopPolling() {
        if (pollingTimeline != null) {
            pollingTimeline.stop();
            pollingTimeline = null;
        }
        piLoading.setVisible(false);
    }

    /**
     * Called when Stripe confirms payment.
     */
    private void onPaymentSuccess() {
        lblStatus.setText("✅ Paiement confirmé !");
        lblStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");

        // Create the subscription in DB
        AbonnementService as = new AbonnementService();
        LocalDate today = LocalDate.now();
        LocalDate dateExpiration = dpDateExpiration.getValue();

        Abonnement abo = new Abonnement(
                offreId,
                typePack,
                prixTotal,
                Date.valueOf(today),
                Date.valueOf(dateExpiration),
                "ACTIF",
                UserSession.getInstance().getUser().getId());

        try {
            as.ajouter(abo);

            WindowUtils.showAlert("Succès",
                    "Paiement accepté ! Vous êtes maintenant abonné au " + typePack +
                            " jusqu'au " + dateExpiration + ".");

            if (parentController != null) {
                parentController.verifierAbonnement();
            }

            // Close the popup with a small delay so user sees the success message
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(Duration.seconds(1));
            delay.setOnFinished(e -> {
                if (btnStripe.getScene() != null) {
                    ((javafx.stage.Stage) btnStripe.getScene().getWindow()).close();
                }
            });
            delay.play();

        } catch (Exception e) {
            WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Paiement reçu mais erreur lors de l'activation : " + e.getMessage());
        }
    }

    @FXML
    void annuler(ActionEvent event) {
        stopPolling();
        WindowUtils.closeWindow(event);
    }
}