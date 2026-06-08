package tn.neuron.ardhi.services.Evenement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import tn.neuron.ardhi.utils.UserAndDiag.AppConfig;

/**
 * Service GRATUIT avec Groq (texte) + Unsplash (images)
 */
public class GeminiAIEventService {

    private static final String GROQ_API_KEY = AppConfig.get("groq.evenement.api.key");
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final ObjectMapper objectMapper;
    private final UnsplashImageService unsplashService;

    public GeminiAIEventService() {
        this.objectMapper = new ObjectMapper();
        this.unsplashService = new UnsplashImageService();
    }

    /**
     * Génère une description via Groq (GRATUIT - 14400 req/jour)
     */
    public String genererDescription(String titre, String type, String lieu) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(GROQ_URL);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Authorization", "Bearer " + GROQ_API_KEY);

            String prompt = construirePrompt(titre, type, lieu);

            String jsonBody = String.format("""
                {
                    "model": "llama-3.1-8b-instant",
                    "messages": [{
                        "role": "user",
                        "content": "%s"
                    }],
                    "max_tokens": 300
                }
                """, prompt.replace("\"", "\\\"").replace("\n", "\\n"));

            request.setEntity(new StringEntity(jsonBody, "UTF-8"));

            HttpResponse response = client.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());

            System.out.println("📝 Groq Response Status: " + statusCode);

            if (statusCode != 200) {
                System.err.println("❌ Erreur Groq: " + responseBody);
                return genererDescriptionFallback(titre, type, lieu);
            }

            JsonNode rootNode = objectMapper.readTree(responseBody);
            if (rootNode.has("choices") && rootNode.get("choices").size() > 0) {
                String generatedText = rootNode.get("choices").get(0)
                        .get("message").get("content").asText().trim();
                System.out.println("✅ Description Groq générée avec succès!");
                return generatedText;
            }

            return genererDescriptionFallback(titre, type, lieu);

        } catch (Exception e) {
            System.err.println("❌ Groq error: " + e.getMessage());
            return genererDescriptionFallback(titre, type, lieu);
        }
    }

    /**
     * Génère une image via Unsplash (photos professionnelles réelles)
     */
    public String genererImage(String titre, String type) {
        try {
            System.out.println("🎨 Recherche image Unsplash pour type: " + type);

            String imagePath = unsplashService.rechercherImage(type);

            if (imagePath != null) {
                System.out.println("✅ Image Unsplash téléchargée: " + imagePath);
                return imagePath;
            } else {
                System.out.println("⚠️ Aucune image trouvée sur Unsplash");
                return null;
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur téléchargement image: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Construit le prompt
     */
    private String construirePrompt(String titre, String type, String lieu) {
        String typeDescription = switch (type) {
            case "FOIRE"      -> "une foire agricole";
            case "FORMATION"  -> "une formation agricole professionnelle";
            case "CONFERENCE" -> "une conférence agricole";
            case "ATELIER"    -> "un atelier pratique agricole";
            default           -> "un événement agricole";
        };

        return String.format("""
            Écris une description professionnelle et engageante de 3 à 4 phrases pour %s 
            intitulé "%s" qui se tiendra à %s en Tunisie.
            
            La description doit:
            - Être en français professionnel
            - Mettre en avant les bénéfices concrets pour les agriculteurs tunisiens
            - Être motivante et donner envie de participer
            - Mentionner l'aspect pratique et applicable
            - Ne PAS inclure de date ni d'horaire
            
            Écris uniquement la description, sans titre ni introduction.
            """, typeDescription, titre, lieu);
    }

    /**
     * Génération locale en cas d'échec API (fallback)
     */
    private String genererDescriptionFallback(String titre, String type, String lieu) {
        System.out.println("⚠️ Utilisation du fallback local");

        String template = switch (type) {
            case "FOIRE" ->
                    "Rejoignez-nous à %s pour %s, un événement incontournable qui rassemble agriculteurs, " +
                            "fournisseurs et experts du secteur agricole. Découvrez les dernières innovations, " +
                            "échangez avec des professionnels et explorez de nouvelles opportunités pour votre exploitation. " +
                            "Une occasion unique de développer votre réseau et d'accéder à des solutions adaptées au contexte tunisien.";

            case "FORMATION" ->
                    "Cette formation pratique à %s vous permettra de maîtriser les techniques essentielles pour %s. " +
                            "Bénéficiez de l'expertise de formateurs qualifiés et d'ateliers pratiques adaptés aux réalités " +
                            "de l'agriculture tunisienne. Repartez avec des compétences concrètes et directement applicables " +
                            "pour améliorer la productivité et la durabilité de votre exploitation.";

            case "CONFERENCE" ->
                    "Participez à %s, une conférence qui réunit les acteurs clés du secteur agricole à %s. " +
                            "Au programme: présentations d'experts, études de cas inspirantes et débats sur les enjeux actuels. " +
                            "Une opportunité d'apprentissage et de réflexion sur l'avenir de l'agriculture en Tunisie, " +
                            "avec des perspectives concrètes et des solutions innovantes.";

            case "ATELIER" ->
                    "Cet atelier pratique à %s vous offre une expérience hands-on sur %s. " +
                            "Travaillez en petits groupes avec des encadrants expérimentés, " +
                            "pratiquez sur le terrain et posez toutes vos questions. Format interactif qui garantit " +
                            "une assimilation rapide des techniques et un partage d'expérience enrichissant entre participants.";

            default ->
                    "Découvrez %s, un événement agricole à %s conçu pour vous accompagner dans le développement " +
                            "de vos activités. Programme riche en contenu pratique et en rencontres professionnelles, " +
                            "adapté aux besoins spécifiques des agriculteurs tunisiens.";
        };

        return String.format(template, lieu, titre);
    }

    /**
     * Génère description + image en une seule opération
     */
    public EventGenerationResult genererEvenementComplet(String titre, String type, String lieu) {
        EventGenerationResult result = new EventGenerationResult();

        System.out.println("🤖 Génération avec Groq + Unsplash...");

        System.out.println("📝 Génération de la description (Groq llama-3.1-8b-instant)...");
        result.setDescription(genererDescription(titre, type, lieu));

        System.out.println("🎨 Téléchargement d'image professionnelle (Unsplash)...");
        String imagePath = genererImage(titre, type);

        if (imagePath != null) {
            result.setImagePath(imagePath);
            System.out.println("✅ Image téléchargée avec succès!");
        } else {
            System.out.println("⚠️ Aucune image trouvée. Description générée avec succès.");
        }

        System.out.println("✅ Génération terminée!");

        return result;
    }

    /**
     * Classe pour retourner les résultats de génération
     */
    public static class EventGenerationResult {
        private String description;
        private String imagePath;

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getImagePath() { return imagePath; }
        public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    }

    /**
     * Test de la connexion à l'API Groq
     */
    public boolean testerConnexion() {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(GROQ_URL);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Authorization", "Bearer " + GROQ_API_KEY);
            request.setEntity(new StringEntity("""
                {
                    "model": "llama-3.1-8b-instant",
                    "messages": [{"role": "user", "content": "Test"}],
                    "max_tokens": 10
                }
                """, "UTF-8"));

            HttpResponse response = client.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();

            System.out.println("🔗 Test connexion Groq - Status: " + statusCode);

            if (statusCode == 200) {
                System.out.println("✅ Connexion Groq OK!");
                return true;
            } else {
                String responseBody = EntityUtils.toString(response.getEntity());
                System.err.println("❌ Erreur connexion Groq: " + responseBody);
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur test connexion: " + e.getMessage());
            return false;
        }
    }
}