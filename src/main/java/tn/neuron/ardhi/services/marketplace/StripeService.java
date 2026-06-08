package tn.neuron.ardhi.services.marketplace;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import tn.neuron.ardhi.utils.UserAndDiag.AppConfig;

/**
 * Service Stripe – crée une Checkout Session et retourne l'URL de paiement.
 * La clé secrète est lue depuis config.properties (jamais exposée au frontend).
 */
public class StripeService {

    /** Taux de conversion TND → EUR (modifiable ici) */
    private static final double TND_TO_EUR = 0.30;

    /** Montant minimum Stripe en centimes (50 cts d'EUR) */
    private static final long MIN_AMOUNT_CENTS = 50L;

    private final String successUrl;
    private final String cancelUrl;

    public StripeService() {
        String secretKey = AppConfig.get("stripe.secret.key");
        this.successUrl = AppConfig.get("stripe.success.url", "http://localhost:9000/payment/success");
        this.cancelUrl = AppConfig.get("stripe.cancel.url", "http://localhost:9000/payment/cancel");

        if (secretKey.isBlank() || secretKey.startsWith("sk_test_VOTRE")) {
            throw new IllegalStateException(
                    "Clé Stripe non configurée. Éditez config.properties (clé: stripe.secret.key).");
        }
        Stripe.apiKey = secretKey;
    }

    /**
     * Crée une Checkout Session Stripe.
     *
     * @param totalTND montant total en TND (sera converti en EUR)
     * @param label    libellé affiché dans Stripe Checkout (ex. "Commande Ardhi
     *                 #42")
     * @return URL de la page Checkout Stripe
     * @throws StripeException          si l'API Stripe échoue
     * @throws IllegalArgumentException si le montant est trop faible
     */
    public String createCheckoutSession(double totalTND, String label) throws StripeException {
        long montantEurCentimes = Math.round(totalTND * TND_TO_EUR * 100);

        if (montantEurCentimes < MIN_AMOUNT_CENTS) {
            throw new IllegalArgumentException(
                    String.format(
                            "Montant trop faible pour Stripe : %.2f TND = %d cts EUR (minimum %d cts).",
                            totalTND, montantEurCentimes, MIN_AMOUNT_CENTS));
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("eur")
                                                .setUnitAmount(montantEurCentimes)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(label)
                                                                .build())
                                                .build())
                                .build())
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }
}

