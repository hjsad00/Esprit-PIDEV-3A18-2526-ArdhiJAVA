package tn.neuron.ardhi.services.Evenement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class UnsplashImageService {

    private static final String UNSPLASH_ACCESS_KEY = "QLOSI6_S3p9kjO0uI8rKjw_4mpQzrzFgasED-jeQnfo";
    private static final String UNSPLASH_API_URL = "https://api.unsplash.com/search/photos";

    // ── Symfony public uploads folder (absolute path) ─────────────────────────
    public static final String SYMFONY_PUBLIC_PATH =
            "C:\\Users\\hp\\ArdhiWEB\\public\\uploads\\evenements";
    // ──────────────────────────────────────────────────────────────────────────

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String rechercherImage(String type) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {

            String query = switch (type) {
                case "FOIRE"      -> "agricultural fair farming market";
                case "FORMATION"  -> "farming training agriculture education";
                case "CONFERENCE" -> "agriculture conference business";
                case "ATELIER"    -> "farm workshop hands-on";
                default           -> "agriculture farming";
            };

            String url = String.format(
                    "%s?query=%s&per_page=1&client_id=%s",
                    UNSPLASH_API_URL,
                    query.replace(" ", "+"),
                    UNSPLASH_ACCESS_KEY
            );

            HttpGet request = new HttpGet(url);
            String response = EntityUtils.toString(client.execute(request).getEntity());

            JsonNode root = objectMapper.readTree(response);

            if (root.has("results") && root.get("results").size() > 0) {
                String imageUrl = root.get("results").get(0)
                        .get("urls").get("regular").asText();
                return telechargerImage(imageUrl, type);
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String telechargerImage(String imageUrl, String type) throws Exception {
        // Save directly into Symfony's public/uploads/evenements/
        Path uploadsDir = Paths.get(SYMFONY_PUBLIC_PATH);
        if (!Files.exists(uploadsDir)) {
            Files.createDirectories(uploadsDir);
        }

        String fileName = "unsplash_" + type.toLowerCase() + "_" +
                System.currentTimeMillis() + ".jpg";
        Path destination = uploadsDir.resolve(fileName);

        try (InputStream in = new URL(imageUrl).openStream()) {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
        }

        System.out.println("✅ Image sauvegardée dans Symfony public/: " + destination);

        // Return web-accessible path with leading slash
        return "/uploads/evenements/" + fileName;
    }
}