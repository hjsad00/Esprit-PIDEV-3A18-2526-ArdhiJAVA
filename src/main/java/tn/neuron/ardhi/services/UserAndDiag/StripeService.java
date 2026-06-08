package tn.neuron.ardhi.services.UserAndDiag;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import tn.neuron.ardhi.utils.UserAndDiag.AppConfig;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;

/**
 * Stripe payment service using REST API directly (no SDK).
 * Creates Checkout Sessions and checks payment status.
 */
public class StripeService {

    private static final String STRIPE_SECRET_KEY = AppConfig.get("stripe.secret.key");

    private static final String API_BASE = "https://api.stripe.com/v1";

    /**
     * Result of creating a Checkout Session.
     */
    public static class CheckoutResult {
        public final String sessionId;
        public final String checkoutUrl;
        public final boolean success;
        public final String error;

        private CheckoutResult(String sessionId, String checkoutUrl, boolean success, String error) {
            this.sessionId = sessionId;
            this.checkoutUrl = checkoutUrl;
            this.success = success;
            this.error = error;
        }

        public static CheckoutResult ok(String sessionId, String checkoutUrl) {
            return new CheckoutResult(sessionId, checkoutUrl, true, null);
        }

        public static CheckoutResult fail(String error) {
            return new CheckoutResult(null, null, false, error);
        }
    }

    /**
     * Creates a Stripe Checkout Session for a one-time payment.
     *
     * @param productName Name of the offer/pack (e.g., "Pack Premium")
     * @param amountCents Total amount in smallest currency unit (e.g., 2999 for
     *                    29.99 DT)
     * @param currency    Currency code (e.g., "tnd" for Tunisian Dinar, or "eur")
     * @return CheckoutResult with session ID and URL
     */
    public CheckoutResult createCheckoutSession(String productName, long amountCents, String currency) {
        try {
            String urlStr = API_BASE + "/checkout/sessions";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            // Auth
            String encodedKey = Base64.getEncoder()
                    .encodeToString((STRIPE_SECRET_KEY + ":").getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encodedKey);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Build form data for Checkout Session
            StringBuilder postData = new StringBuilder();
            postData.append("mode=payment");
            postData.append("&success_url=").append(
                    URLEncoder.encode("https://ardhi.tn/payment-success?session_id={CHECKOUT_SESSION_ID}", "UTF-8"));
            postData.append("&cancel_url=").append(URLEncoder.encode("https://ardhi.tn/payment-cancelled", "UTF-8"));
            postData.append("&line_items[0][price_data][currency]=").append(URLEncoder.encode(currency, "UTF-8"));
            postData.append("&line_items[0][price_data][product_data][name]=")
                    .append(URLEncoder.encode(productName, "UTF-8"));
            postData.append("&line_items[0][price_data][unit_amount]=").append(amountCents);
            postData.append("&line_items[0][quantity]=1");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData.toString().getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                String responseBody = readResponse(conn);
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

                String sessionId = json.get("id").getAsString();
                String checkoutUrl = json.get("url").getAsString();

                System.out.println("[Stripe] Checkout session created: " + sessionId);
                return CheckoutResult.ok(sessionId, checkoutUrl);

            } else {
                String errorBody = readErrorResponse(conn);
                System.err.println("[Stripe] Error (" + responseCode + "): " + errorBody);

                // Parse Stripe error message
                try {
                    JsonObject errorJson = JsonParser.parseString(errorBody).getAsJsonObject();
                    if (errorJson.has("error")) {
                        String message = errorJson.getAsJsonObject("error").get("message").getAsString();
                        return CheckoutResult.fail(message);
                    }
                } catch (Exception ignored) {
                }

                return CheckoutResult.fail("Erreur Stripe (" + responseCode + ")");
            }

        } catch (Exception e) {
            System.err.println("[Stripe] Exception: " + e.getMessage());
            LogUtils.error(StripeService.class, "Failed to create checkout session", e);
            return CheckoutResult.fail("Erreur de connexion : " + e.getMessage());
        }
    }

    /**
     * Checks the payment status of a Checkout Session.
     *
     * @param sessionId The Stripe Checkout Session ID
     * @return "paid", "unpaid", "expired", or "error"
     */
    public String checkSessionStatus(String sessionId) {
        try {
            String urlStr = API_BASE + "/checkout/sessions/" + sessionId;
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            String encodedKey = Base64.getEncoder()
                    .encodeToString((STRIPE_SECRET_KEY + ":").getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encodedKey);

            if (conn.getResponseCode() == 200) {
                String responseBody = readResponse(conn);
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

                String paymentStatus = json.get("payment_status").getAsString();
                System.out.println("[Stripe] Session " + sessionId + " status: " + paymentStatus);
                return paymentStatus; // "paid", "unpaid", "no_payment_required"

            } else {
                System.err.println("[Stripe] Status check failed: " + conn.getResponseCode());
                return "error";
            }

        } catch (Exception e) {
            System.err.println("[Stripe] Status check exception: " + e.getMessage());
            return "error";
        }
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
            sb.append(line);
        reader.close();
        return sb.toString();
    }

    private String readErrorResponse(HttpURLConnection conn) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
            sb.append(line);
        reader.close();
        return sb.toString();
    }
}
