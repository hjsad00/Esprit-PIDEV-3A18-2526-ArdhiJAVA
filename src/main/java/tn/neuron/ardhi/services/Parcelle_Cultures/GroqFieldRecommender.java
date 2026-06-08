package tn.neuron.ardhi.services.Parcelle_Cultures;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import tn.neuron.ardhi.utils.UserAndDiag.AppConfig;

/**
 * Service qui utilise Groq (même API que UserAndDiag) pour recommander les champs
 * du formulaire d'ajout d'une parcelle (type de sol, irrigation, surface)
 * en se basant sur la localisation et les coordonnées GPS.
 */
public class GroqFieldRecommender {

    private static final Logger LOG = Logger.getLogger(GroqFieldRecommender.class.getName());

    private static final String API_KEY = AppConfig.get("groq.api.key");
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private String derniereErreur;

    public GroqFieldRecommender() {
    }

    public boolean isDisponible() {
        return true;
    }

    public String getSourceUtilisee() {
        return "Groq IA";
    }

    // ==================== RÉSULTAT ====================

    public static class FieldRecommendation {
        public final String typeSol;
        public final String systemeIrrigation;
        public final String surfaceRecommandee;
        public final String explication;
        public final String erreur; // null si succès

        public FieldRecommendation(String typeSol, String systemeIrrigation,
                                   String surfaceRecommandee, String explication) {
            this.typeSol = typeSol;
            this.systemeIrrigation = systemeIrrigation;
            this.surfaceRecommandee = surfaceRecommandee;
            this.explication = explication;
            this.erreur = null;
        }

        /** Constructeur pour les erreurs */
        public FieldRecommendation(String erreur) {
            this.typeSol = "";
            this.systemeIrrigation = "";
            this.surfaceRecommandee = "";
            this.explication = "";
            this.erreur = erreur;
        }

        public boolean isSuccess() {
            return erreur == null;
        }
    }

    // ==================== RECOMMANDATION ====================

    /**
     * Génère des recommandations pour les champs du formulaire parcelle via Groq.
     */
    public FieldRecommendation recommander(String localisation, Double latitude, Double longitude) {
        String prompt = construirePrompt(localisation, latitude, longitude);

        try {
            String reponse = appellerGroq(prompt);

            if (reponse == null || reponse.isEmpty()) {
                return new FieldRecommendation(derniereErreur != null ? derniereErreur : "Pas de réponse de Groq");
            }

            // Parser la réponse JSON
            FieldRecommendation result = parseReponse(reponse);
            if (result != null) {
                return result;
            }
            return new FieldRecommendation("Impossible de parser la réponse Groq");

        } catch (Exception e) {
            derniereErreur = e.getMessage();
            LOG.warning("Erreur appel Groq: " + e.getMessage());
            return new FieldRecommendation("Erreur : " + e.getMessage());
        }
    }

    // ==================== PROMPT ====================

    private String construirePrompt(String localisation, Double latitude, Double longitude) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tu es un expert agronome tunisien. ");
        sb.append("On veut créer une parcelle agricole à l'emplacement suivant :\n\n");
        sb.append("Localisation : ").append(localisation != null ? localisation : "non précisée").append("\n");
        if (latitude != null && longitude != null) {
            sb.append("Coordonnées GPS : ").append(latitude).append(", ").append(longitude).append("\n");
        }
        sb.append("\nEn te basant sur ta connaissance de cette région tunisienne ");
        sb.append("(climat, pluviométrie, géographie, pratiques locales), ");
        sb.append("recommande les valeurs suivantes pour la parcelle :\n\n");
        sb.append("Réponds UNIQUEMENT avec un JSON valide, sans texte avant ni après, dans ce format exact :\n");
        sb.append("{\"type_sol\": \"...\", \"systeme_irrigation\": \"...\", \"surface_recommandee\": \"...\", \"explication\": \"...\"}\n\n");
        sb.append("Règles :\n");
        sb.append("- type_sol : un seul mot parmi (Argileux, Sableux, Limoneux, Calcaire, Argilo-sableux, Argilo-limoneux, Tourbeux). ");
        sb.append("Choisis le plus courant dans cette zone géographique.\n");
        sb.append("- systeme_irrigation : un seul choix parmi (Goutte-à-goutte, Aspersion, Gravitaire, Pivot, Micro-aspersion, Pluvial). ");
        sb.append("Choisis le plus adapté au climat et au sol de la région.\n");
        sb.append("- surface_recommandee : surface typique en hectares pour une exploitation dans cette zone (juste le nombre, ex: \"2.5\").\n");
        sb.append("- explication : 1-2 phrases courtes en français expliquant pourquoi ces choix sont adaptés à cette localisation.\n");
        return sb.toString();
    }

    // ==================== APPEL API GROQ ====================

    private String appellerGroq(String prompt) {
        derniereErreur = null;
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);

            String jsonPayload = "{"
                    + "\"model\": \"llama-3.3-70b-versatile\","
                    + "\"messages\": [{"
                    + "  \"role\": \"system\","
                    + "  \"content\": \"Tu es un expert agronome. Reponds uniquement en JSON valide, sans markdown.\""
                    + "}, {"
                    + "  \"role\": \"user\","
                    + "  \"content\": \"" + escapeJson(prompt) + "\""
                    + "}],"
                    + "\"temperature\": 0.3,"
                    + "\"max_tokens\": 400"
                    + "}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    conn.disconnect();
                    return extraireContenuGroq(sb.toString());
                }
            } else {
                String errBody = "";
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    errBody = sb.toString();
                }
                conn.disconnect();
                if (code == 429) {
                    derniereErreur = "Quota Groq épuisé — réessayez dans quelques minutes";
                } else if (code == 401) {
                    derniereErreur = "Clé API Groq invalide";
                } else {
                    derniereErreur = "Erreur Groq (HTTP " + code + ")";
                }
                LOG.warning("Groq API error: HTTP " + code + " — " + errBody.substring(0, Math.min(150, errBody.length())));
            }
        } catch (java.net.SocketTimeoutException e) {
            derniereErreur = "Timeout — Groq n'a pas répondu dans les 30s";
            LOG.warning(derniereErreur);
        } catch (java.net.UnknownHostException e) {
            derniereErreur = "Pas de connexion internet";
            LOG.warning(derniereErreur);
        } catch (Exception e) {
            derniereErreur = e.getClass().getSimpleName() + ": " + e.getMessage();
            LOG.warning("Erreur connexion Groq: " + derniereErreur);
        }
        return null;
    }

    /**
     * Extrait le champ "content" de la réponse Groq en gérant correctement les guillemets échappés.
     * Format: {"choices":[{"message":{"content":"..."}}]}
     */
    private String extraireContenuGroq(String json) {
        // Chercher "content" : "
        String marker = "\"content\"";
        int idx = json.lastIndexOf(marker); // lastIndexOf pour prendre le message.content (pas le role)
        if (idx < 0) return null;

        int colon = json.indexOf(":", idx + marker.length());
        if (colon < 0) return null;

        // Trouver le début du string (premier " après le :)
        int start = json.indexOf("\"", colon + 1);
        if (start < 0) return null;
        start++; // passer le "

        // Parcourir en gérant les échappements \" 
        StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"': sb.append('"'); i += 2; continue;
                    case '\\': sb.append('\\'); i += 2; continue;
                    case 'n': sb.append('\n'); i += 2; continue;
                    case 'r': sb.append('\r'); i += 2; continue;
                    case 't': sb.append('\t'); i += 2; continue;
                    default: sb.append(c); sb.append(next); i += 2; continue;
                }
            }
            if (c == '"') break; // fin du string
            sb.append(c);
            i++;
        }
        return sb.toString().trim();
    }

    // ==================== PARSING ====================

    /**
     * Parse la réponse Groq (texte contenu extrait).
     */
    private FieldRecommendation parseReponse(String texte) {
        try {
            // Le texte peut contenir du markdown (```json ... ```) ou du texte avant/après le JSON
            String content = texte.replace("```json", "").replace("```", "").trim();

            int debut = content.indexOf('{');
            int fin = content.lastIndexOf('}');
            if (debut < 0 || fin < 0) return null;
            String json = content.substring(debut, fin + 1);

            String typeSol = extraireValeur(json, "type_sol");
            String irrigation = extraireValeur(json, "systeme_irrigation");
            String surface = extraireValeur(json, "surface_recommandee");
            String explication = extraireValeur(json, "explication");

            if (typeSol == null && irrigation == null) return null;

            return new FieldRecommendation(
                    typeSol != null ? typeSol : "",
                    irrigation != null ? irrigation : "",
                    surface != null ? surface : "",
                    explication != null ? explication : ""
            );
        } catch (Exception e) {
            LOG.warning("Erreur parsing réponse Groq: " + e.getMessage());
            return null;
        }
    }

    // ==================== UTILITAIRES ====================

    private String extraireValeur(String json, String cle) {
        String key = "\"" + cle + "\"";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int colon = json.indexOf(":", idx + key.length());
        if (colon < 0) return null;
        int qStart = json.indexOf("\"", colon + 1);
        if (qStart < 0) return null;
        int qEnd = json.indexOf("\"", qStart + 1);
        if (qEnd < 0) return null;
        return json.substring(qStart + 1, qEnd).trim();
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
