package tn.neuron.ardhi.services.marketplace;

import com.google.gson.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

import tn.neuron.ardhi.utils.UserAndDiag.AppConfig;

/**
 * Service de reconnaissance d'image via Groq API (GRATUIT + ULTRA RAPIDE).
 *
 * Obtenir une clé gratuite : https://console.groq.com → API Keys → Create API Key
 * Modèle utilisé : meta-llama/llama-4-scout-17b-16e-instruct (vision)
 *
 * Limites free tier Groq :
 *   - 30 requêtes/minute
 *   - 6000 tokens/minute
 *   Largement suffisant pour un marketplace.
 */
public class VisionService {

    // Loaded from config.properties — see config.properties.example
    private static final String API_KEY = AppConfig.get("groq.marketplace.api.key");

    // Modèle Groq avec support vision (le plus récent et gratuit)
    private static final String MODEL = "meta-llama/llama-4-scout-17b-16e-instruct";

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    // ── Catégories Ardhi Marketplace ──────────────────────────────────────────
    private static final String CATEGORIES_LIST =
            "Fruits, Légumes, Céréales, Herbes & Épices, " +
                    "Produits Laitiers, Viandes & Volailles, Huiles & Graisses, " +
                    "Miel & Produits de la Ruche, Plantes & Semences, Autre";

    // ══════════════════════════════════════════════════════════════════════════
    // Classe de retour
    // ══════════════════════════════════════════════════════════════════════════

    public static class ProductInfo {
        private final String nom;
        private final String categorie;
        private final String description;

        public ProductInfo(String nom, String categorie, String description) {
            this.nom         = nom;
            this.categorie   = categorie;
            this.description = description;
        }

        public String getNom()         { return nom; }
        public String getCategorie()   { return categorie; }
        public String getDescription() { return description; }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Méthode principale
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Analyse une image produit avec Groq Vision (llama-4-scout).
     * Retourne nom, catégorie et description en français.
     *
     * @param imageFile Fichier image à analyser (PNG, JPG, JPEG)
     * @return ProductInfo avec nom, catégorie et description
     */
    public ProductInfo analyzeImage(File imageFile) {
        try {
            System.out.println("[VisionService] 📸 Analyse Groq de : " + imageFile.getName());

            // 1. Encoder l'image en Base64
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType    = detectMimeType(imageFile.getName());
            String dataUrl     = "data:" + mimeType + ";base64," + base64Image;

            System.out.println("[VisionService] ✅ Image encodée ("
                    + imageBytes.length + " bytes, " + mimeType + ")");

            // 2. Construire la requête
            String requestBody = buildGroqRequest(dataUrl);

            // 3. Appeler l'API Groq
            String response = callGroqApi(requestBody);
            if (response == null) return defaultProductInfo();

            // 4. Extraire le texte généré
            String generatedText = extractTextFromGroqResponse(response);
            if (generatedText == null || generatedText.isBlank()) {
                System.err.println("[VisionService] ❌ Réponse vide de Groq");
                return defaultProductInfo();
            }

            System.out.println("[VisionService] 📝 Réponse Groq :\n" + generatedText);

            // 5. Parser le JSON structuré
            return parseStructuredResponse(generatedText);

        } catch (Exception e) {
            System.err.println("[VisionService] ❌ Exception : " + e.getMessage());
            e.printStackTrace();
            return defaultProductInfo();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Construction de la requête Groq (format OpenAI compatible)
    // ══════════════════════════════════════════════════════════════════════════

    private String buildGroqRequest(String imageDataUrl) {
        String prompt =
                "Tu es un expert en produits agricoles tunisiens.\n" +
                        "Analyse cette image et identifie le produit agricole visible.\n\n" +
                        "Réponds UNIQUEMENT avec un objet JSON valide, sans texte avant ni après, sans balises markdown :\n" +
                        "{\n" +
                        "  \"nom\": \"Nom commercial en français (ex: Bananes Fraîches, Tomates Bio, Fraises Fraîches)\",\n" +
                        "  \"categorie\": \"Une seule valeur parmi : " + CATEGORIES_LIST + "\",\n" +
                        "  \"description\": \"Description commerciale courte en 2 phrases pour le marché tunisien\"\n" +
                        "}\n\n" +
                        "Si aucun produit agricole n'est visible : {\"nom\":\"Produit Agricole\",\"categorie\":\"Autre\",\"description\":\"Produit local de qualité.\"}";

        // ── Partie image ──────────────────────────────────────────────────────
        JsonObject imageUrl = new JsonObject();
        imageUrl.addProperty("url", imageDataUrl);

        JsonObject imageContent = new JsonObject();
        imageContent.addProperty("type", "image_url");
        imageContent.add("image_url", imageUrl);

        // ── Partie texte (prompt) ─────────────────────────────────────────────
        JsonObject textContent = new JsonObject();
        textContent.addProperty("type", "text");
        textContent.addProperty("text", prompt);

        // ── Message utilisateur avec image + texte ────────────────────────────
        JsonArray contentArray = new JsonArray();
        contentArray.add(imageContent);
        contentArray.add(textContent);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.add("content", contentArray);

        JsonArray messages = new JsonArray();
        messages.add(userMessage);

        // ── Corps de la requête final ─────────────────────────────────────────
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("model", MODEL);
        requestJson.add("messages", messages);
        requestJson.addProperty("max_tokens", 300);
        requestJson.addProperty("temperature", 0.1); // Réponses précises et constantes

        return new Gson().toJson(requestJson);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Appel HTTP vers Groq
    // ══════════════════════════════════════════════════════════════════════════

    private String callGroqApi(String requestBody) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(GROQ_API_URL);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + API_KEY);
            httpPost.setEntity(new StringEntity(requestBody, "UTF-8"));

            System.out.println("[VisionService] 🌐 Appel Groq API...");

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");

                System.out.println("[VisionService] HTTP Status : " + statusCode);

                if (statusCode == 200) {
                    return responseBody;
                } else {
                    System.err.println("[VisionService] ❌ Erreur Groq HTTP "
                            + statusCode + " :\n" + responseBody);
                    return null;
                }
            }
        } catch (Exception e) {
            System.err.println("[VisionService] ❌ Erreur connexion Groq : " + e.getMessage());
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Extraction du texte (format OpenAI compatible)
    // ══════════════════════════════════════════════════════════════════════════

    private String extractTextFromGroqResponse(String jsonResponse) {
        try {
            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();

            if (root.has("error")) {
                System.err.println("[VisionService] ❌ Erreur Groq : "
                        + root.getAsJsonObject("error").get("message").getAsString());
                return null;
            }

            // Format OpenAI : choices[0].message.content
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) return null;

            return choices.get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content")
                    .getAsString()
                    .trim();

        } catch (Exception e) {
            System.err.println("[VisionService] ❌ Erreur parsing réponse : " + e.getMessage());
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Parser la réponse JSON structurée
    // ══════════════════════════════════════════════════════════════════════════

    private ProductInfo parseStructuredResponse(String text) {
        try {
            // Nettoyer les balises markdown ```json ... ```
            String cleaned = text
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            // Extraire uniquement la partie JSON si du texte parasite est présent
            int start = cleaned.indexOf('{');
            int end   = cleaned.lastIndexOf('}');
            if (start != -1 && end != -1 && end > start) {
                cleaned = cleaned.substring(start, end + 1);
            }

            JsonObject json = JsonParser.parseString(cleaned).getAsJsonObject();

            String nom         = json.has("nom")         ? json.get("nom").getAsString().trim()         : "Produit Agricole";
            String categorie   = json.has("categorie")   ? json.get("categorie").getAsString().trim()   : "Autre";
            String description = json.has("description") ? json.get("description").getAsString().trim() : defaultDescription(nom);

            // Valider la catégorie
            if (!CATEGORIES_LIST.contains(categorie)) {
                System.out.println("[VisionService] ⚠️ Catégorie \"" + categorie + "\" inconnue → Autre");
                categorie = "Autre";
            }

            System.out.println("[VisionService] ✅ Résultat → Nom: \""
                    + nom + "\" | Catégorie: \"" + categorie + "\"");

            return new ProductInfo(nom, categorie, description);

        } catch (Exception e) {
            System.err.println("[VisionService] ⚠️ JSON invalide → fallback : " + e.getMessage());
            return parseTextFallback(text);
        }
    }

    /**
     * Fallback si le modèle répond en texte libre au lieu de JSON.
     */
    private ProductInfo parseTextFallback(String text) {
        System.out.println("[VisionService] 🔄 Fallback parsing texte...");
        String lower = text.toLowerCase();

        String[][] quickMap = {
                {"banana",     "Bananes Fraîches",   "Fruits"},
                {"banane",     "Bananes Fraîches",   "Fruits"},
                {"tomat",      "Tomates Fraîches",   "Légumes"},
                {"apple",      "Pommes Locales",     "Fruits"},
                {"pomme",      "Pommes Locales",     "Fruits"},
                {"strawberr",  "Fraises Fraîches",   "Fruits"},
                {"fraise",     "Fraises Fraîches",   "Fruits"},
                {"orange",     "Oranges de Saison",  "Fruits"},
                {"carrot",     "Carottes Bio",       "Légumes"},
                {"carotte",    "Carottes Bio",       "Légumes"},
                {"grape",      "Raisins de Qualité", "Fruits"},
                {"mango",      "Mangues Fraîches",   "Fruits"},
                {"mangue",     "Mangues Fraîches",   "Fruits"},
                {"watermelon", "Pastèques Bio",      "Fruits"},
                {"lemon",      "Citrons Frais",      "Fruits"},
                {"onion",      "Oignons Locaux",     "Légumes"},
                {"garlic",     "Ail Frais",          "Légumes"},
                {"pepper",     "Poivrons Colorés",   "Légumes"},
                {"cucumber",   "Concombres Frais",   "Légumes"},
                {"honey",      "Miel Naturel",       "Miel & Produits de la Ruche"},
                {"miel",       "Miel Naturel",       "Miel & Produits de la Ruche"},
                {"olive",      "Olives de Qualité",  "Huiles & Graisses"},
                {"wheat",      "Blé Dur Local",      "Céréales"},
                {"corn",       "Maïs Doré",          "Céréales"},
                {"egg",        "Œufs de Ferme",      "Viandes & Volailles"},
                {"milk",       "Lait Frais",         "Produits Laitiers"},
        };

        for (String[] entry : quickMap) {
            if (lower.contains(entry[0])) {
                return new ProductInfo(entry[1], entry[2], defaultDescription(entry[1]));
            }
        }

        return defaultProductInfo();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Utilitaires
    // ══════════════════════════════════════════════════════════════════════════

    private String detectMimeType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png"))                          return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif"))                          return "image/gif";
        if (lower.endsWith(".webp"))                         return "image/webp";
        return "image/jpeg";
    }

    private String defaultDescription(String nom) {
        return nom + " frais et de qualité, cultivé localement par des agriculteurs tunisiens passionnés. " +
                "Disponible en quantité limitée, livraison rapide.";
    }

    private ProductInfo defaultProductInfo() {
        return new ProductInfo(
                "Produit Agricole",
                "Autre",
                "Produit frais de qualité, cultivé localement par des agriculteurs tunisiens."
        );
    }
}