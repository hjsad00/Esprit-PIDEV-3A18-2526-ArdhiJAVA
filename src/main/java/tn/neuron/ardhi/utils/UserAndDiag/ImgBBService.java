package tn.neuron.ardhi.utils.UserAndDiag;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
// Removed org.json dependency to avoid compilation errors

public class ImgBBService {

    private static final String API_KEY = AppConfig.get("imgbb.api.key");
    private static final String UPLOAD_URL = "https://api.imgbb.com/1/upload";

    /**
     * Uploads an image file to ImgBB and returns the direct display URL.
     * 
     * @param imageFile The file to upload
     * @return The direct URL of the uploaded image, or null if upload fails.
     */
    public static String uploadImage(File imageFile) {
        if (imageFile == null || !imageFile.exists()) {
            LogUtils.error(ImgBBService.class, "File is null or does not exist.");
            return null;
        }

        try {
            // Encode image to Base64
            byte[] fileContent = Files.readAllBytes(imageFile.toPath());
            String encodedString = Base64.getEncoder().encodeToString(fileContent);

            // Construct URL parameters
            String urlParameters = "key=" + API_KEY + "&image="
                    + URLEncoder.encode(encodedString, StandardCharsets.UTF_8.toString());

            byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
            int postDataLength = postData.length;

            URL url = new URL(UPLOAD_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("charset", "utf-8");
            conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
            conn.setUseCaches(false);

            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.write(postData);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine = null;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    // Manual JSON Parsing to avoid dependency issues
                    // We are looking for "url":"https://..."
                    String jsonResponse = response.toString();
                    return extractUrlFromJson(jsonResponse);
                }
            } else {
                LogUtils.error(ImgBBService.class, "ImgBB HTTP Error: " + responseCode);
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine = null;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    LogUtils.error(ImgBBService.class, "Error Details: " + response.toString());
                }
            }

        } catch (Exception e) {
            LogUtils.error(ImgBBService.class, "Exception during image upload to ImgBB", e);
        }

        return null;
    }

    private static String extractUrlFromJson(String json) {
        // Simple manual parser for: ... "url": "https://i.ibb.co/..." ...
        // This is robust enough for this specific API response structure
        try {
            String searchKey = "\"url\":\"";
            int startIndex = json.indexOf(searchKey);
            if (startIndex != -1) {
                startIndex += searchKey.length();
                int endIndex = json.indexOf("\"", startIndex);
                if (endIndex != -1) {
                    String url = json.substring(startIndex, endIndex);
                    // The URL might have escaped slashes like "\/", remove backslashes
                    return url.replace("\\/", "/");
                }
            }
        } catch (Exception e) {
            LogUtils.error(ImgBBService.class, "Error parsing ImgBB response", e);
        }
        return null;
    }
}