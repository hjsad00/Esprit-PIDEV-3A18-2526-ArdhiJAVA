package tn.neuron.ardhi.services.UserAndDiag;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;

public class ProfanityFilterService {

    private static final String API_URL = "https://www.purgomalum.com/service/containsprofanity?text=";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Checks if the given text contains profanity using the PurgoMalum API.
     *
     * @param text The text to check. If null or empty, it returns false.
     * @return true if profanity is detected, false otherwise (or on error).
     */
    public boolean containsProfanity(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        try {
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + encodedText))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body().trim();
                return "true".equalsIgnoreCase(body);
            } else {
                LogUtils.warn(ProfanityFilterService.class,
                        "Profanity API returned status code: " + response.statusCode());
            }
        } catch (Exception e) {
            LogUtils.error(ProfanityFilterService.class, "Error checking profanity", e);
        }

        // Default to false if API fails so we don't block user unexpectedly
        return false;
    }
}
