package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.utils.UserAndDiag.AppConfig;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * WhatsApp messaging service using Twilio WhatsApp API.
 * Uses the same Twilio REST API as SmsService but with the "whatsapp:" prefix.
 *
 * For development/testing, use the Twilio WhatsApp Sandbox:
 * 1. Go to https://console.twilio.com/us1/develop/sms/try-it-out/whatsapp-learn
 * 2. Send the join code to the Twilio WhatsApp number
 * 3. Use whatsapp:+14155238886 as FROM_NUMBER
 *
 * For production, register a WhatsApp Business number with Twilio.
 */
public class WhatsAppService {

    // Loaded from config.properties — see config.properties.example
    private static final String ACCOUNT_SID = AppConfig.get("twilio.account.sid");
    private static final String AUTH_TOKEN = AppConfig.get("twilio.auth.token");

    // Twilio WhatsApp Sandbox number (for development)
    // For production, replace with your registered WhatsApp Business number
    private static final String FROM_WHATSAPP = AppConfig.get("twilio.whatsapp.from", "whatsapp:+14155238886");

    /**
     * Sends a WhatsApp message to the specified phone number.
     *
     * @param toPhoneNumber Full phone number with country code (e.g.,
     *                      "+21612345678")
     * @param body          Message body (supports basic formatting: *bold*,
     *                      _italic_)
     * @return true if the message was sent successfully
     */
    public boolean sendWhatsAppMessage(String toPhoneNumber, String body) {
        // Ensure the number has the whatsapp: prefix
        String to = toPhoneNumber.startsWith("whatsapp:") ? toPhoneNumber : "whatsapp:" + toPhoneNumber;

        try {
            String urlStr = "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + "/Messages.json";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            // Basic Auth (same as SmsService)
            String auth = ACCOUNT_SID + ":" + AUTH_TOKEN;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // POST body — identical to SMS but with whatsapp: prefixed numbers
            String postData = "To=" + URLEncoder.encode(to, "UTF-8")
                    + "&From=" + URLEncoder.encode(FROM_WHATSAPP, "UTF-8")
                    + "&Body=" + URLEncoder.encode(body, "UTF-8");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 201 || responseCode == 200) {
                LogUtils.info(WhatsAppService.class, "WhatsApp message sent successfully to " + toPhoneNumber);
                return true;
            } else {
                // Read error response
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    sb.append(line);
                reader.close();
                String errorBody = sb.toString();
                System.err.println("[WhatsApp] Twilio error (" + responseCode + "): " + errorBody);

                if (responseCode == 401) {
                    System.err.println(
                            "[WhatsApp] → 401 = Invalid credentials. Check ACCOUNT_SID and AUTH_TOKEN.");
                }
                if (errorBody.contains("63007") || errorBody.contains("not a valid WhatsApp")) {
                    System.err.println(
                            "[WhatsApp] → The recipient hasn't joined the Twilio Sandbox.");
                    System.err.println(
                            "[WhatsApp] → They must first send 'join <keyword>' to +14155238886 on WhatsApp.");
                    System.err.println(
                            "[WhatsApp] → See: https://console.twilio.com/us1/develop/sms/try-it-out/whatsapp-learn");
                }
                return false;
            }
        } catch (Exception e) {
            LogUtils.error(WhatsAppService.class, "Failed to send WhatsApp message", e);
            return false;
        }
    }
}
