package tn.neuron.ardhi.services.UserAndDiag;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tn.neuron.ardhi.utils.UserAndDiag.AppConfig;

public class GroqService {

    private static final String API_KEY = AppConfig.get("groq.api.key");
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    // ── Helper method for HTTP requests to reduce duplication ──
    private String sendRequest(String jsonPayload) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            // Groq nécessite une authentification Bearer
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15_000); // 15s connect timeout
            conn.setReadTimeout(90_000); // 90s read timeout

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                try (BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null)
                        errorResponse.append(errorLine);
                    return "ERREUR_API_" + responseCode + ": " + errorResponse.toString();
                }
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null)
                    response.append(line);

                return extractionContent(response.toString());
            }

        } catch (Exception e) {
            return "ERREUR_TECHNIQUE: " + e.getMessage();
        }
    }

    private String escapeJson(String input) {
        if (input == null)
            return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String extractionContent(String json) {
        // Groq renvoie la réponse dans choices[0].message.content
        Pattern pattern = Pattern.compile("\"content\"\\s*:\\s*\"(.*?)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            // THE FIX: Preserve newlines so we can split tasks correctly
            return matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").trim();
        }
        return "ERREUR_FORMAT";
    }

    // ── Public API Methods ──

    public String analyserImage(File imageFile) {
        try {
            byte[] fileContent = Files.readAllBytes(imageFile.toPath());
            String imageBase64 = Base64.getEncoder().encodeToString(fileContent);

            String promptText = "Tu es un expert agronome spécialisé dans l'agriculture. " +
                    "Analyse cette image de plante. " +
                    "INSTRUCTIONS PRIORITAIRES : " +
                    "1. Identifie la plante et la maladie. " +
                    "2. Confiance (0-100). " +
                    "3. Pour le nom du produit, utilise des marques disponibles en Tunisie." +
                    "4. Type de traitement: FONGICIDE, HERBICIDE, INSECTICIDE, BACTERICIDE, NEMATICIDE, VIRUCIDE, NUTRIMENT, REGULATEUR_CROISSANCE, ou AUTRE. "
                    +
                    "5. Description : Sois précis sur le dosage et le moment (ex: à l'aube, éviter le vent). " +
                    "6. Niveau de gravité: CRITICAL (maladie grave, action urgente), MEDIUM (modéré, traitement conseillé), LOW (léger ou plante saine). "
                    +
                    "FORMAT DE RÉPONSE (Strictement une seule ligne) : " +
                    "PLANTE|MALADIE|CONFIANCE|NOM_PRODUIT|TYPE_TRAITEMENT|DESCRIPTION_DOSAGE_ET_APPLICATION|SEVERITY";

            String jsonPayload = "{"
                    + "\"model\": \"meta-llama/llama-4-scout-17b-16e-instruct\","
                    + "\"messages\": [{"
                    + "  \"role\": \"user\","
                    + "  \"content\": ["
                    + "    {\"type\": \"text\", \"text\": \"" + promptText + "\"},"
                    + "    {\"type\": \"image_url\", \"image_url\": {\"url\": \"data:image/jpeg;base64," + imageBase64
                    + "\"}}"
                    + "  ]"
                    + "}]"
                    + "}";

            return sendRequest(jsonPayload);

        } catch (Exception e) {
            return "ERREUR_TECHNIQUE: " + e.getMessage();
        }
    }

    public String sendChatMessage(String userMessage, String systemContext) {
        // Clean inputs
        String cleanContext = systemContext.replace("\"", "'").replace("\n", " ");
        String cleanMessage = userMessage.replace("\"", "'").replace("\n", " ");

        String jsonPayload = "{"
                + "\"model\": \"meta-llama/llama-4-scout-17b-16e-instruct\","
                + "\"messages\": [{"
                + "  \"role\": \"system\","
                + "  \"content\": \"" + cleanContext + "\""
                + "}, {"
                + "  \"role\": \"user\","
                + "  \"content\": \"" + cleanMessage + "\""
                + "}]"
                + "}";

        return sendRequest(jsonPayload);
    }

    public String getChatCompletion(String prompt) {
        String jsonPayload = "{"
                + "\"model\": \"llama-3.3-70b-versatile\"," // Updated to latest stable mapping if needed, or stick to
                                                            // 3.1
                + "\"messages\": [{"
                + "  \"role\": \"user\","
                + "  \"content\": \"" + escapeJson(prompt) + "\""
                + "}]"
                + "}";

        return sendRequest(jsonPayload);
    }

    public String analyzeRecovery(File oldImage, File newImage, String oldDisease) {
        try {
            byte[] oldContent = Files.readAllBytes(oldImage.toPath());
            String oldBase64 = Base64.getEncoder().encodeToString(oldContent);

            byte[] newContent = Files.readAllBytes(newImage.toPath());
            String newBase64 = Base64.getEncoder().encodeToString(newContent);

            String promptText = "Compare these two images of a plant to assess recovery from " + oldDisease + ". " +
                    "Image 1 represents the initial infection state. " +
                    "Image 2 represents the current state after treatment. " +
                    "Assess the progress objectively. " +
                    "Reply STRICTLY in this format: " +
                    "STATUS|DETAILS " +
                    "Where STATUS must be one of: " +
                    "- 'HEALED': Total absence of symptoms, the plant looks completely healthy. " +
                    "- 'RECOVERING': Visible improvement, fewer spots/necrosis, but still some traces. " +
                    "- 'UNCHANGED': No visible difference. " +
                    "- 'WORSENING': The disease has spread or worsened. " +
                    "And DETAILS is a short justification (max 20 words).";

            String jsonPayload = "{"
                    + "\"model\": \"meta-llama/llama-4-scout-17b-16e-instruct\"," // Fixed model ID
                    + "\"messages\": [{"
                    + "  \"role\": \"user\","
                    + "  \"content\": ["
                    + "    {\"type\": \"text\", \"text\": \"" + escapeJson(promptText) + "\"},"
                    + "    {\"type\": \"image_url\", \"image_url\": {\"url\": \"data:image/jpeg;base64," + oldBase64
                    + "\"}},"
                    + "    {\"type\": \"image_url\", \"image_url\": {\"url\": \"data:image/jpeg;base64," + newBase64
                    + "\"}}"
                    + "  ]"
                    + "}]"
                    + "}";

            return sendRequest(jsonPayload);

        } catch (Exception e) {
            return "ERREUR_TECHNIQUE: " + e.getMessage();
        }
    }

    public String generateUpdatedPlan(File oldImage, File newImage, String diseaseName) {
        try {
            byte[] oldContent = Files.readAllBytes(oldImage.toPath());
            String oldBase64 = Base64.getEncoder().encodeToString(oldContent);

            byte[] newContent = Files.readAllBytes(newImage.toPath());
            String newBase64 = Base64.getEncoder().encodeToString(newContent);

            String promptText = "Tu es un expert agronome. " +
                    "Compare ces deux images (Image 1: état initial de " + diseaseName + ", Image 2: état actuel). " +
                    "D'abord, évalue si les images montrent un changement significatif. " +
                    "Si les images sont très similaires et le traitement actuel semble approprié, réponds juste 'UNCHANGED'. "
                    +
                    "Si la plante est complètement guérie, réponds juste 'HEALED'. " +
                    "SEULEMENT si un changement de plan est nécessaire, propose 3 à 5 nouvelles tâches au format : 'JOUR|DESCRIPTION'. "
                    +
                    "Les JOUR doivent commencer à 1 (relatif). Exemple: " +
                    "1|Continuer le traitement X.\n" +
                    "4|Ajouter un engrais de soutien.\n" +
                    "7|Maintenir l'hydratation.\n" +
                    "Une tâche par ligne.\n" +
                    "INTERDICTION STRICTE : Ne jamais proposer de tâche demandant de scanner, prendre une photo, ou réévaluer la plante via l'application. Ce bouton existe déjà.";

            String jsonPayload = "{"
                    + "\"model\": \"meta-llama/llama-4-scout-17b-16e-instruct\","
                    + "\"messages\": [{"
                    + "  \"role\": \"user\","
                    + "  \"content\": ["
                    + "    {\"type\": \"text\", \"text\": \"" + escapeJson(promptText) + "\"},"
                    + "    {\"type\": \"image_url\", \"image_url\": {\"url\": \"data:image/jpeg;base64," + oldBase64
                    + "\"}},"
                    + "    {\"type\": \"image_url\", \"image_url\": {\"url\": \"data:image/jpeg;base64," + newBase64
                    + "\"}}"
                    + "  ]"
                    + "}]"
                    + "}";

            return sendRequest(jsonPayload);

        } catch (Exception e) {
            return "ERREUR_TECHNIQUE";
        }
    }

    public String generateTreatmentPlan(String diseaseName) {
        String prompt = "Génère un plan de traitement agricole complet et détaillé pour la maladie : " + diseaseName
                + ". " +
                "Le plan doit s'étendre sur 10 jours. " +
                "Génère au moins 3 à 5 tâches différentes réparties sur ces 10 jours. " +
                "Réponds UNIQUEMENT avec une liste de tâches au format : 'JOUR|DESCRIPTION'. " +
                "Exemple: " +
                "1|Isoler la plante et couper les feuilles infectées.\n" +
                "3|Appliquer un traitement fongicide ciblé.\n" +
                "7|Vérifier l'état visuel (sans scanner).\n" +
                "Ne mets pas de texte avant ou après. Une tâche par ligne.\n" +
                "INTERDICTION STRICTE : Ne jamais utiliser de blocs de code Markdown (```). Réponds en texte brut seulement.\n"
                +
                "INTERDICTION STRICTE : Ne jamais proposer de tâche demandant de scanner, prendre une photo, ou réévaluer la plante via l'application. Ce bouton existe déjà.";

        String jsonPayload = "{"
                + "\"model\": \"llama-3.3-70b-versatile\","
                + "\"messages\": [{"
                + "  \"role\": \"user\","
                + "  \"content\": \"" + escapeJson(prompt) + "\""
                + "}]"
                + "}";

        return sendRequest(jsonPayload);
    }

    // ── Health Scan Methods ──

    public String checkConsistency(File imageFile, String context) {
        try {
            byte[] fileContent = Files.readAllBytes(imageFile.toPath());
            String imageBase64 = Base64.getEncoder().encodeToString(fileContent);

            String promptText = "Tu es un expert agronome. " +
                    "Vérifie si cette image est cohérente avec le contexte suivant : " + context + ". " +
                    "Si l'image correspond à la plante ou à la maladie mentionnée, réponds 'CONSISTENT'. " +
                    "Si l'image ne correspond pas du tout (par exemple une autre espèce de plante, ou un objet non pertinent), réponds 'MISMATCH|Raison'. "
                    +
                    "Raison doit être courte (max 10 mots).";

            String jsonPayload = "{"
                    + "\"model\": \"meta-llama/llama-4-scout-17b-16e-instruct\","
                    + "\"messages\": [{"
                    + "  \"role\": \"user\","
                    + "  \"content\": ["
                    + "    {\"type\": \"text\", \"text\": \"" + escapeJson(promptText) + "\"},"
                    + "    {\"type\": \"image_url\", \"image_url\": {\"url\": \"data:image/jpeg;base64," + imageBase64
                    + "\"}}"
                    + "  ]"
                    + "}]"
                    + "}";

            return sendRequest(jsonPayload);

        } catch (Exception e) {
            return "ERREUR_TECHNIQUE";
        }
    }

    /**
     * Analyzes a farm health photo for potential vulnerabilities.
     */
    public String analyzeHealthPhoto(File imageFile, String photoType, String cropContext) {
        try {
            byte[] fileContent = Files.readAllBytes(imageFile.toPath());
            String imageBase64 = Base64.getEncoder().encodeToString(fileContent);

            String promptText = "Tu es un expert agronome spécialisé dans la prévention agricole. " +
                    "Contexte du champ : " + cropContext + ". " +
                    "Type de photo analysée : " + photoType + ". " +
                    "INSTRUCTIONS : " +
                    "1. Analyse cette image pour identifier les RISQUES POTENTIELS (pas les maladies existantes, mais les vulnérabilités). "
                    +
                    "2. Cherche des signes de : ravageurs potentiels, conditions favorables aux maladies, " +
                    "carences nutritives, problèmes de pollinisation, dégradation du sol. " +
                    "3. Pour chaque risque, donne un résultat au format ci-dessous. " +
                    "FORMAT DE RÉPONSE (une ligne par risque, séparée par des pipes) : " +
                    "TYPE|MENACE|SEVERITY|DESCRIPTION " +
                    "Où TYPE est parmi : PEST_OUTBREAK_RISK, DISEASE_RISK, NUTRIENT_DEFICIENCY, LOW_POLLINATION, SOIL_DEGRADATION. "
                    +
                    "Où SEVERITY est parmi : CRITICAL, MEDIUM, LOW. " +
                    "Si aucun risque n'est identifié, réponds : AUCUN_RISQUE " +
                    "Ne mets pas de texte avant ou après. Une ligne par risque.";

            String jsonPayload = "{"
                    + "\"model\": \"meta-llama/llama-4-scout-17b-16e-instruct\","
                    + "\"messages\": [{"
                    + "  \"role\": \"user\","
                    + "  \"content\": ["
                    + "    {\"type\": \"text\", \"text\": \"" + promptText.replace("\"", "\\\"") + "\"},"
                    + "    {\"type\": \"image_url\", \"image_url\": {\"url\": \"data:image/jpeg;base64," + imageBase64
                    + "\"}}"
                    + "  ]"
                    + "}]"
                    + "}";

            return sendRequest(jsonPayload);

        } catch (Exception e) {
            e.printStackTrace();
            return "ERREUR_ANALYSE";
        }
    }

    /**
     * Analyzes a health photo from an ImgBB URL instead of a local file.
     */
    public String analyzeHealthPhotoFromUrl(String imageUrl, String photoType, String cropContext) {
        try {
            String promptText = "Tu es un expert agronome spécialisé dans la prévention agricole. " +
                    "Contexte du champ : " + cropContext + ". " +
                    "Type de photo analysée : " + photoType + ". " +
                    "INSTRUCTIONS : " +
                    "1. Analyse cette image pour identifier les RISQUES POTENTIELS. " +
                    "2. Cherche des signes de : ravageurs potentiels, conditions favorables aux maladies, " +
                    "carences nutritives, problèmes de pollinisation, dégradation du sol. " +
                    "3. Pour chaque risque, donne un résultat au format ci-dessous. " +
                    "FORMAT DE RÉPONSE (une ligne par risque) : " +
                    "TYPE|MENACE|SEVERITY|DESCRIPTION " +
                    "Où TYPE est parmi : PEST_OUTBREAK_RISK, DISEASE_RISK, NUTRIENT_DEFICIENCY, LOW_POLLINATION, SOIL_DEGRADATION. "
                    +
                    "Où SEVERITY est parmi : CRITICAL, MEDIUM, LOW. " +
                    "Si aucun risque : répondre AUCUN_RISQUE. " +
                    "Pas de texte avant ou après.";

            String jsonPayload = "{"
                    + "\"model\": \"meta-llama/llama-4-scout-17b-16e-instruct\","
                    + "\"messages\": [{"
                    + "  \"role\": \"user\","
                    + "  \"content\": ["
                    + "    {\"type\": \"text\", \"text\": \"" + promptText.replace("\"", "\\\"") + "\"},"
                    + "    {\"type\": \"image_url\", \"image_url\": {\"url\": \"" + imageUrl + "\"}}"
                    + "  ]"
                    + "}]"
                    + "}";

            return sendRequest(jsonPayload);

        } catch (Exception e) {
            e.printStackTrace();
            return "ERREUR_ANALYSE";
        }
    }

    public String generateHealthPreventionPlan(String vulnerability, String cropType) {
        String prompt = "Génère un plan de prévention agricole détaillé pour le risque suivant : " + vulnerability
                + ". Culture : " + cropType + ". " +
                "Le plan doit s'étendre sur 14 jours. " +
                "Génère 5 à 8 tâches préventives réparties sur ces 14 jours. " +
                "Réponds UNIQUEMENT avec une liste de tâches au format : 'JOUR|DESCRIPTION'. " +
                "Exemple: " +
                "1|Inspecter visuellement les feuilles et tiges pour détecter tout signe précoce.\n" +
                "3|Appliquer un traitement préventif bio.\n" +
                "7|Vérifier la présence d'insectes bénéfiques.\n" +
                "Ne mets pas de texte avant ou après via Markdown. Une tâche par ligne.";

        String systemCtx = "Tu es un agronome expert en agriculture préventive en Tunisie. " +
                "Tu donnes des conseils pratiques et utilisables avec des produits locaux.";
        return sendChatMessage(prompt, systemCtx);
    }
}
