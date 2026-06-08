package tn.neuron.ardhi.services.marketplace;

import tn.neuron.ardhi.models.marketplace.ChatIntent;
import tn.neuron.ardhi.models.marketplace.ProduitDemande;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de communication avec Ollama (IA locale).
 *
 * v3 : ajout de l'intention "filtrer" + champs recherche/categorie/prixMin/prixMax
 */
public class AIService {

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL = "mistral";
    private static final int TIMEOUT_MS = 30_000;

    private static final String SYSTEM_PROMPT =
            "Tu es un assistant pour un marketplace agricole. " +
                    "Analyse le message utilisateur et réponds UNIQUEMENT avec ce JSON (rien d'autre) :\n" +
                    "{\n" +
                    "  \"intention\": \"achat\" | \"disponibilite\" | \"vider_panier\" | \"supprimer_produit\" | \"filtrer\" | \"hors_sujet\",\n" +
                    "  \"produits\": [{\"nom\": \"...\", \"quantite\": N}],\n" +
                    "  \"critere\": \"prix_asc\" | \"prix_desc\" | \"avis\" | null,\n" +
                    "  \"recherche\": \"...\" | null,\n" +
                    "  \"categorie\": \"...\" | null,\n" +
                    "  \"prixMin\": N | null,\n" +
                    "  \"prixMax\": N | null\n" +
                    "}\n" +
                    "Règles :\n" +
                    "- Si l'utilisateur veut acheter des produits → intention = \"achat\"\n" +
                    "- Si l'utilisateur demande si un produit est disponible → intention = \"disponibilite\"\n" +
                    "- Si l'utilisateur veut vider son panier → intention = \"vider_panier\"\n" +
                    "- Si l'utilisateur veut supprimer UN article précis du panier → intention = \"supprimer_produit\"\n" +
                    "- Si l'utilisateur veut VOIR, CHERCHER, FILTRER des produits (ex: 'montre moi les fruits', 'cherche des tomates', 'produits moins de 10 DT', 'filtre par légumes') → intention = \"filtrer\"\n" +
                    "- Si le message n'a rien à voir → intention = \"hors_sujet\"\n" +
                    "Règles pour 'filtrer' :\n" +
                    "- recherche = mot-clé libre (ex: 'tomate') ou null\n" +
                    "- categorie = catégorie exacte (ex: 'Fruits', 'Légumes', 'Céréales') ou null\n" +
                    "- prixMin = prix minimum en DT (nombre) ou null\n" +
                    "- prixMax = prix maximum en DT (nombre) ou null\n" +
                    "- critere = \"prix_asc\" si moins cher, \"prix_desc\" si plus cher, \"avis\" si mieux noté, sinon null\n" +
                    "Exemples 'filtrer' :\n" +
                    "  'montre moi les fruits' → {\"intention\":\"filtrer\",\"produits\":[],\"critere\":null,\"recherche\":null,\"categorie\":\"Fruits\",\"prixMin\":null,\"prixMax\":null}\n" +
                    "  'cherche des tomates' → {\"intention\":\"filtrer\",\"produits\":[],\"critere\":null,\"recherche\":\"tomate\",\"categorie\":null,\"prixMin\":null,\"prixMax\":null}\n" +
                    "  'produits entre 5 et 20 DT' → {\"intention\":\"filtrer\",\"produits\":[],\"critere\":null,\"recherche\":null,\"categorie\":null,\"prixMin\":5,\"prixMax\":20}\n" +
                    "  'les légumes les moins chers' → {\"intention\":\"filtrer\",\"produits\":[],\"critere\":\"prix_asc\",\"recherche\":null,\"categorie\":\"Légumes\",\"prixMin\":null,\"prixMax\":null}\n" +
                    "Autres règles :\n" +
                    "- critere = \"prix_asc\" si l'utilisateur veut le moins cher\n" +
                    "- critere = \"prix_desc\" si le plus cher\n" +
                    "- critere = \"avis\" si le mieux noté\n" +
                    "- Si hors_sujet : {\"intention\":\"hors_sujet\",\"produits\":[],\"critere\":null,\"recherche\":null,\"categorie\":null,\"prixMin\":null,\"prixMax\":null}\n" +
                    "- Si vider_panier : {\"intention\":\"vider_panier\",\"produits\":[],\"critere\":null,\"recherche\":null,\"categorie\":null,\"prixMin\":null,\"prixMax\":null}\n" +
                    "Réponds UNIQUEMENT avec le JSON, sans markdown, sans texte avant ou après.";

    public ChatIntent analyser(String messageUtilisateur) {
        try {
            String prompt = SYSTEM_PROMPT + "\n\nMessage utilisateur : " + messageUtilisateur;
            String jsonBody = buildRequestBody(prompt);
            String rawResponse = sendRequest(jsonBody);
            String jsonResponse = extractJsonFromResponse(rawResponse);

            System.out.println("[AIService] Réponse brute Ollama : " + rawResponse);
            System.out.println("[AIService] JSON extrait : " + jsonResponse);

            return parseIntent(jsonResponse);

        } catch (Exception e) {
            System.err.println("[AIService] Erreur : " + e.getMessage());
            return buildErrorIntent();
        }
    }

    // -------------------------------------------------------------------------
    // Construction de la requête HTTP
    // -------------------------------------------------------------------------

    private String buildRequestBody(String prompt) {
        String escaped = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return "{\"model\":\"" + MODEL + "\",\"prompt\":\"" + escaped + "\",\"stream\":false}";
    }

    private String sendRequest(String jsonBody) throws IOException {
        URL url = new URL(OLLAMA_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        if (status != 200) {
            throw new IOException("Ollama HTTP " + status);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Extraction du JSON depuis la réponse Ollama
    // -------------------------------------------------------------------------

    private String extractJsonFromResponse(String ollamaResponse) {
        int idx = ollamaResponse.indexOf("\"response\":");
        if (idx < 0) return ollamaResponse;

        int start = ollamaResponse.indexOf('"', idx + 11) + 1;
        if (start <= 0) return ollamaResponse;

        StringBuilder sb = new StringBuilder();
        for (int i = start; i < ollamaResponse.length(); i++) {
            char c = ollamaResponse.charAt(i);
            if (c == '\\' && i + 1 < ollamaResponse.length()) {
                char next = ollamaResponse.charAt(i + 1);
                if (next == '"') { sb.append('"'); i++; }
                else if (next == 'n') { sb.append('\n'); i++; }
                else if (next == '\\') { sb.append('\\'); i++; }
                else sb.append(c);
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }

        String extracted = sb.toString().trim();
        int jsonStart = extracted.indexOf('{');
        int jsonEnd = extracted.lastIndexOf('}');
        if (jsonStart >= 0 && jsonEnd > jsonStart)
            return extracted.substring(jsonStart, jsonEnd + 1);
        return extracted;
    }

    // -------------------------------------------------------------------------
    // Parsing manuel du JSON ChatIntent
    // -------------------------------------------------------------------------

    private ChatIntent parseIntent(String json) {
        ChatIntent intent = new ChatIntent();
        intent.setProduits(new ArrayList<>());

        if (json == null || json.isBlank()) return buildErrorIntent();

        // intention
        String intention = extractStringField(json, "intention");
        intent.setIntention(intention != null ? intention : "hors_sujet");

        // critere
        String critere = extractStringField(json, "critere");
        intent.setCritere("null".equals(critere) ? null : critere);

        // recherche
        String recherche = extractStringField(json, "recherche");
        intent.setRecherche("null".equals(recherche) ? null : recherche);

        // categorie
        String categorie = extractStringField(json, "categorie");
        intent.setCategorie("null".equals(categorie) ? null : categorie);

        // prixMin
        String prixMinStr = extractStringField(json, "prixMin");
        if (prixMinStr != null && !"null".equals(prixMinStr)) {
            try { intent.setPrixMin(Double.parseDouble(prixMinStr)); } catch (NumberFormatException ignored) {}
        }

        // prixMax
        String prixMaxStr = extractStringField(json, "prixMax");
        if (prixMaxStr != null && !"null".equals(prixMaxStr)) {
            try { intent.setPrixMax(Double.parseDouble(prixMaxStr)); } catch (NumberFormatException ignored) {}
        }

        // produits
        int produitsStart = json.indexOf("\"produits\"");
        if (produitsStart >= 0) {
            int arrStart = json.indexOf('[', produitsStart);
            int arrEnd = json.indexOf(']', arrStart);
            if (arrStart >= 0 && arrEnd > arrStart) {
                String arrayContent = json.substring(arrStart + 1, arrEnd);
                intent.setProduits(parseProduitsArray(arrayContent));
            }
        }

        return intent;
    }

    private List<ProduitDemande> parseProduitsArray(String arrayContent) {
        List<ProduitDemande> list = new ArrayList<>();
        int i = 0;
        while (i < arrayContent.length()) {
            int objStart = arrayContent.indexOf('{', i);
            int objEnd = arrayContent.indexOf('}', objStart);
            if (objStart < 0 || objEnd < 0) break;

            String obj = arrayContent.substring(objStart + 1, objEnd);
            String nom = extractStringField("{" + obj + "}", "nom");
            String quantiteStr = extractStringField("{" + obj + "}", "quantite");

            if (nom != null && !nom.isBlank()) {
                int quantite = 1;
                if (quantiteStr != null) {
                    try { quantite = Integer.parseInt(quantiteStr.trim()); } catch (NumberFormatException ignored) {}
                }
                list.add(new ProduitDemande(nom.trim(), quantite));
            }
            i = objEnd + 1;
        }
        return list;
    }

    private String extractStringField(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx < 0) return null;

        int colonIdx = json.indexOf(':', idx + key.length());
        if (colonIdx < 0) return null;

        int valStart = colonIdx + 1;
        while (valStart < json.length() && json.charAt(valStart) == ' ') valStart++;
        if (valStart >= json.length()) return null;

        char first = json.charAt(valStart);
        if (first == '"') {
            int end = json.indexOf('"', valStart + 1);
            if (end < 0) return null;
            return json.substring(valStart + 1, end);
        } else {
            int end = valStart;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
            return json.substring(valStart, end).trim();
        }
    }

    private ChatIntent buildErrorIntent() {
        ChatIntent intent = new ChatIntent();
        intent.setIntention("erreur");
        intent.setProduits(new ArrayList<>());
        return intent;
    }
}