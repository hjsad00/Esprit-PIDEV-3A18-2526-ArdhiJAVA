package tn.neuron.ardhi.services.UserAndDiag;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import tn.neuron.ardhi.utils.UserAndDiag.AppConfig;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;

/**
 * SMS verification service using Twilio REST API (no SDK).
 * Uses HTTP POST directly to avoid module/dependency issues.
 */
public class SmsService {

    // Loaded from config.properties — see config.properties.example
    private static final String ACCOUNT_SID = AppConfig.get("twilio.account.sid");
    private static final String AUTH_TOKEN = AppConfig.get("twilio.auth.token");
    private static final String FROM_NUMBER = AppConfig.get("twilio.from.number");

    private static final SecureRandom RANDOM = new SecureRandom();

    private String lastGeneratedCode;

    /**
     * Generates a 6-digit verification code and sends it via SMS.
     * 
     * @param toPhoneNumber Full phone number with country code (e.g.,
     *                      "+21612345678")
     * @return true if SMS was sent successfully
     */
    public boolean sendVerificationCode(String toPhoneNumber) {
        lastGeneratedCode = String.format("%06d", RANDOM.nextInt(1000000));

        String message = "Votre code de vérification Ardhi est : " + lastGeneratedCode
                + "\nCe code expire dans 5 minutes.";

        return sendSms(toPhoneNumber, message);
    }

    /**
     * Verifies the code entered by the user.
     */
    public boolean verifyCode(String enteredCode) {
        if (lastGeneratedCode == null || enteredCode == null)
            return false;
        return lastGeneratedCode.equals(enteredCode.trim());
    }

    /**
     * Returns the last generated code (for testing/debug purposes only).
     */
    public String getLastGeneratedCode() {
        return lastGeneratedCode;
    }

    /**
     * Sends an SMS via Twilio REST API using plain HTTP.
     */
    private boolean sendSms(String to, String body) {
        try {
            String urlStr = "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + "/Messages.json";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            // Basic Auth
            String auth = ACCOUNT_SID + ":" + AUTH_TOKEN;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // POST body
            String postData = "To=" + URLEncoder.encode(to, "UTF-8")
                    + "&From=" + URLEncoder.encode(FROM_NUMBER, "UTF-8")
                    + "&Body=" + URLEncoder.encode(body, "UTF-8");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 201 || responseCode == 200) {
                LogUtils.info(SmsService.class, "SMS sent successfully to " + to);
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
                System.err.println("[SMS] Twilio error (" + responseCode + "): " + errorBody);

                if (responseCode == 401) {
                    System.err.println(
                            "[SMS] → 401 = Invalid credentials. Check ACCOUNT_SID and AUTH_TOKEN in SmsService.java");
                    System.err.println("[SMS] → Find them at: https://console.twilio.com/ (Account Info section)");
                }
                if (errorBody.contains("21608") || errorBody.contains("is not a valid")) {
                    System.err
                            .println("[SMS] → FROM_NUMBER must be a Twilio-purchased number, NOT your personal phone!");
                    System.err.println(
                            "[SMS] → Get one at: https://console.twilio.com/us1/develop/phone-numbers/manage/incoming");
                }
                return false;
            }
        } catch (Exception e) {
            LogUtils.error(SmsService.class, "Failed to send SMS", e);
            return false;
        }
    }
}
