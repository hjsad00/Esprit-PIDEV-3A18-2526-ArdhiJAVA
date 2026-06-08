package tn.neuron.ardhi.services.Parcelle_Cultures;

import tn.neuron.ardhi.models.Parcelle_Cultures.Parcelle;
import tn.neuron.ardhi.models.Parcelle_Cultures.RecommendationResult;
import tn.neuron.ardhi.models.Parcelle_Cultures.RecommendationResult.CultureRecommandee;
import tn.neuron.ardhi.models.Parcelle_Cultures.RecommendationResult.DonneesMeteo;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;
import tn.neuron.ardhi.utils.UserAndDiag.AppConfig;

/**
 * Service IA utilisant l'API Google Gemini pour les recommandations de cultures.
 *
 * Clé API non requise en mode démo — peut être configurée via
 * la variable d'environnement GEMINI_API_KEY ou le fichier
 * config.properties (clé: gemini.api.key) via AppConfig.
 *
 * Documentation: https://ai.google.dev/gemini-api/docs
 */
public class AIRecommendationService {

    private static final Logger LOGGER = Logger.getLogger(AIRecommendationService.class.getName());

    // URL de l'API Gemini (modèle gemini-pro)
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=%s";

    private String apiKey;
    private boolean disponible;

    public AIRecommendationService() {
        // Chercher la clé API dans les propriétés système ou fichier de config
        this.apiKey = chargerApiKey();
        this.disponible = (apiKey != null && !apiKey.isBlank() && !apiKey.equals("VOTRE_CLE_ICI"));
        if (!disponible) {
            LOGGER.info("Clé API Gemini non configurée — mode fallback algorithmique activé");
        }
    }

    public AIRecommendationService(String apiKey) {
        this.apiKey = apiKey;
        this.disponible = (apiKey != null && !apiKey.isBlank());
    }

    /**
     * Vérifie si le service IA est disponible (clé API configurée)
     */
    public boolean isDisponible() {
        return disponible;
    }

    /**
     * Génère un prompt dynamique et envoie à l'API Gemini.
     * Retourne une RecommendationResult ou null si erreur/indiponible.
     *
     * @param parcelle         La parcelle à diviser
     * @param meteo            Données météo actuelles
     * @param historiqueNoms   Noms des cultures précédentes (pour rotation)
     * @param saisonActuelle   Saison courante (ex: "Hiver")
     */
    public RecommendationResult recommander(Parcelle parcelle, DonneesMeteo meteo,
                                             List<String> historiqueNoms, String saisonActuelle, boolean diviser) {
        if (!isDisponible()) {
            LOGGER.warning("Service IA indisponible (pas de CLÉ API)");
            return null;
        }

        try {
            String prompt = construirePrompt(parcelle, meteo, historiqueNoms, saisonActuelle, diviser);
            LOGGER.info("Envoi prompt à Gemini (" + prompt.length() + " chars)");

            String reponse = appellerGemini(prompt);
            if (reponse == null) return null;

            return parseReponseGemini(reponse, parcelle.getSurface(), meteo, saisonActuelle, diviser);

        } catch (Exception e) {
            LOGGER.warning("Erreur service IA Gemini: " + e.getMessage());
            return null;
        }
    }

    /**
     * Construit le prompt dynamique pour l'analyse agricole.
     */
    String construirePrompt(Parcelle parcelle, DonneesMeteo meteo,
                            List<String> historiqueNoms, String saisonActuelle, boolean diviser) {
        String historique = historiqueNoms.isEmpty() ? "Aucune culture précédente connue"
                : String.join(", ", historiqueNoms);

        String structureJson;
        String missionTxt;
        if (diviser) {
            missionTxt = "MISSION : Propose exactement 2 cultures différentes pour diviser cette parcelle de manière optimisée.\n" +
                         "CONTRAINTES OBLIGATOIRES :\n" +
                         "1. Les deux cultures doivent être DIFFÉRENTES\n" +
                         "2. La somme des surfaces allouées doit être EXACTEMENT égale à " + parcelle.getSurface() + " ha\n" +
                         "3. Respecter la rotation culturale (ne pas répéter la culture dominante)\n" +
                         "4. Adapter aux conditions météo et au type de sol\n";
            structureJson = """
            RÉPONDRE EN JSON STRICT avec ce format (rien d'autre !) :
            {
              "culture1": {
                "nom": "Nom de la culture 1",
                "type": "Céréale|Légumineuse|Maraîcher|Oléagineux|Fourrage",
                "surface": <nombre_hectares>,
                "rendement_estime": <tonnes_par_hectare>,
                "justification": "Courte justification"
              },
              "culture2": {
                "nom": "Nom de la culture 2",
                "type": "Céréale|Légumineuse|Maraîcher|Oléagineux|Fourrage",
                "surface": <nombre_hectares>,
                "rendement_estime": <tonnes_par_hectare>,
                "justification": "Courte justification"
              },
              "explication_globale": "Explication détaillée de la recommandation en 2-3 phrases"
            }
            """;
        } else {
            missionTxt = "MISSION : Propose exactement 1 seule culture optimale pour englober la totalité de cette parcelle.\n" +
                         "CONTRAINTES OBLIGATOIRES :\n" +
                         "1. La surface allouée doit être EXACTEMENT égale à " + parcelle.getSurface() + " ha\n" +
                         "2. Respecter la rotation culturale (ne pas répéter la dernière culture)\n" +
                         "3. Adapter aux conditions météo et au type de sol\n";
            structureJson = """
            RÉPONDRE EN JSON STRICT avec ce format (rien d'autre !) :
            {
              "culture1": {
                "nom": "Nom de la culture",
                "type": "Céréale|Légumineuse|Maraîcher|Oléagineux|Fourrage",
                "surface": <nombre_hectares>,
                "rendement_estime": <tonnes_par_hectare>,
                "justification": "Courte justification"
              },
              "explication_globale": "Explication détaillée de la recommandation en 2-3 phrases"
            }
            """;
        }

        return String.format("""
            Tu es un expert agricole spécialisé en Tunisie et Afrique du Nord.
            
            Une parcelle agricole présente les caractéristiques suivantes :
            - Surface totale : %.1f hectares
            - Type de sol : %s
            - Système d'irrigation : %s
            - Localisation GPS : lat=%.4f, lon=%.4f (%s)
            - Saison actuelle : %s
            
            Données météo temps réel :
            - Température moyenne : %.1f°C
            - Humidité : %.0f%%
            - Précipitations : %.1f mm/jour
            - Conditions : %s
            
            Historique de cultures précédentes : %s
            
            %s
            %s
            """,
                parcelle.getSurface(), parcelle.getTypeSol(), parcelle.getSystemeIrrigation(),
                parcelle.getLatitude(), parcelle.getLongitude(), parcelle.getLocalisation(),
                saisonActuelle,
                meteo.getTemperatureMoyenne(), meteo.getHumidite(), meteo.getPrecipitations(),
                meteo.getDescription(),
                historique,
                missionTxt, structureJson);
    }

    /**
     * Appelle l'API Gemini avec le prompt donné.
     */
    private String appellerGemini(String prompt) {
        try {
            String urlStr = String.format(GEMINI_URL, apiKey);
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setDoOutput(true);

            // Corps de la requête
            String body = String.format("""
                    {
                      "contents": [{
                        "parts": [{"text": %s}]
                      }],
                      "generationConfig": {
                        "temperature": 0.3,
                        "maxOutputTokens": 800
                      }
                    }
                    """, jsonEscape(prompt));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                conn.disconnect();
                return sb.toString();
            } else {
                LOGGER.warning("Gemini API error: HTTP " + code);
                // Lire le message d'erreur
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    LOGGER.warning("Gemini error body: " + sb.toString().substring(0, Math.min(200, sb.length())));
                }
                conn.disconnect();
                return null;
            }
        } catch (Exception e) {
            LOGGER.warning("Erreur connexion Gemini: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parse la réponse JSON de Gemini et construit un RecommendationResult.
     */
    private RecommendationResult parseReponseGemini(String geminiResponse, double surfaceTotale,
                                                     DonneesMeteo meteo, String saison, boolean diviser) {
        try {
            // Extraire le texte du content de Gemini
            String text = extractGeminiText(geminiResponse);
            if (text == null || text.isEmpty()) return null;

            // Extraire le JSON de la réponse (entre { et })
            int debut = text.indexOf('{');
            int fin = text.lastIndexOf('}');
            if (debut < 0 || fin < 0) {
                LOGGER.warning("Pas de JSON trouvé dans la réponse Gemini");
                return null;
            }
            String json = text.substring(debut, fin + 1);

            // Parser culture1
            String json1 = extraireObjetJson(json, "culture1");
            String explication = extraireValeurString(json, "explication_globale");

            if (json1 == null) {
                LOGGER.warning("culture1 manquante dans la réponse IA");
                return null;
            }
            CultureRecommandee c1 = parserCulture(json1, saison);
            if (c1 == null) return null;

            CultureRecommandee c2 = null;

            if (diviser) {
                String json2 = extraireObjetJson(json, "culture2");
                if (json2 == null) {
                    LOGGER.warning("culture2 manquante dans la réponse IA mais mode division actif");
                    return null;
                }
                c2 = parserCulture(json2, saison);
                if (c2 == null) return null;

                // Vérifier que les noms sont différents
                if (c1.getNom().equalsIgnoreCase(c2.getNom())) {
                    LOGGER.warning("IA a recommandé deux fois la même culture — rejet");
                    return null;
                }

                // Ajuster les surfaces
                double somme = c1.getSurface() + c2.getSurface();
                if (Math.abs(somme - surfaceTotale) > 0.5) {
                    double ratio = surfaceTotale / somme;
                    c1.setSurface(Math.round(c1.getSurface() * ratio * 10.0) / 10.0);
                    c2.setSurface(surfaceTotale - c1.getSurface());
                }
            } else {
                c1.setSurface(surfaceTotale); // S'assurer que ça prend toute la surface
            }

            RecommendationResult result = RecommendationResult.success(
                    c1, c2, meteo,
                    explication.isBlank() ? "Recommandation basée sur l'analyse IA Gemini" : explication,
                    "IA_GEMINI");

            return result;

        } catch (Exception e) {
            LOGGER.warning("Erreur parsing réponse Gemini: " + e.getMessage());
            return null;
        }
    }

    // ==================== UTILITAIRES DE PARSING ====================

    private String extractGeminiText(String json) {
        // Format Gemini: {"candidates":[{"content":{"parts":[{"text":"..."}]}}]}
        int textIdx = json.indexOf("\"text\":");
        if (textIdx < 0) return null;
        int start = json.indexOf("\"", textIdx + 7) + 1;
        if (start <= 0) return null;

        StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case 'n': sb.append('\n'); i += 2; continue;
                    case 't': sb.append('\t'); i += 2; continue;
                    case '"': sb.append('"'); i += 2; continue;
                    case '\\': sb.append('\\'); i += 2; continue;
                    default: sb.append(next); i += 2; continue;
                }
            }
            if (c == '"') break;
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private String extraireObjetJson(String json, String cle) {
        String key = "\"" + cle + "\"";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int debut = json.indexOf('{', idx);
        if (debut < 0) return null;
        int depth = 0;
        int fin = debut;
        for (int i = debut; i < json.length(); i++) {
            if (json.charAt(i) == '{') depth++;
            else if (json.charAt(i) == '}') {
                depth--;
                if (depth == 0) { fin = i; break; }
            }
        }
        return json.substring(debut, fin + 1);
    }

    private String extraireValeurString(String json, String cle) {
        String key = "\"" + cle + "\":\"";
        int idx = json.indexOf(key);
        if (idx < 0) return "";
        int start = idx + key.length();
        StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) { i += 2; continue; }
            if (c == '"') break;
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private double extraireValeurDouble(String json, String cle) {
        String key = "\"" + cle + "\":";
        int idx = json.indexOf(key);
        if (idx < 0) return 0.0;
        int start = idx + key.length();
        while (start < json.length() && json.charAt(start) == ' ') start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end))
                || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
        if (start == end) return 0.0;
        try {
            return Double.parseDouble(json.substring(start, end));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private CultureRecommandee parserCulture(String json, String saison) {
        String nom = extraireValeurString(json, "nom");
        String type = extraireValeurString(json, "type");
        double surface = extraireValeurDouble(json, "surface");
        double rendement = extraireValeurDouble(json, "rendement_estime");

        if (nom.isBlank() || surface <= 0) return null;
        if (type.isBlank()) type = "Autre";
        if (rendement <= 0) rendement = 2.5;

        return new CultureRecommandee(nom, type, saison, surface, 0.8, rendement);
    }

    private String jsonEscape(String text) {
        text = text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + text + "\"";
    }

    private String chargerApiKey() {
        // 1. Variable d'environnement
        String key = System.getenv("GEMINI_API_KEY");
        if (key != null && !key.isBlank()) return key;

        // 2. Fichier de configuration centralisé (config.properties) via AppConfig
        key = AppConfig.get("gemini.api.key", "").trim();
        if (!key.isBlank() && !key.equals("VOTRE_CLE_ICI")) return key;

        return null;
    }
}
