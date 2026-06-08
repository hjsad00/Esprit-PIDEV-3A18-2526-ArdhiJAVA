package tn.neuron.ardhi.services.MaterielEtMaintenance;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import tn.neuron.ardhi.utils.UserAndDiag.AppConfig;

/**
 * GrokService — Appel direct à l'API Groq
 * Modèle : llama-3.3-70b-versatile
 * Prédictions 100% dynamiques basées sur données réelles calculées
 */
public class GrokService {

    private static final String API_KEY = AppConfig.get("groq.maintenance.api.key");
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL   = "llama-3.3-70b-versatile";

    private static final String SYSTEM_PROMPT_CHAT =
            "Tu es ARDHI, expert en maintenance de materiel agricole. " +
                    "Reponds UNIQUEMENT en JSON valide, sans texte avant ou apres, sans balises ```.\n\n" +
                    "Format JSON obligatoire :\n" +
                    "{\"texte\":\"ta reponse (\\n pour sauts de ligne, pas de ** ni *)\",\"gravite\":\"CRITIQUE|ELEVE|MOYEN|FAIBLE\",\"planifier\":true|false,\"actions\":[\"action1\"],\"suggestions\":[\"suggestion1\",\"suggestion2\"],\"emoji\":\"emoji\"}\n\n" +
                    "Niveaux : CRITIQUE=danger immediat arret, ELEVE=7jours, MOYEN=ce mois, FAIBLE=preventif.\n" +
                    "planifier=true si maintenance urgente. suggestions=2-4 mots courts cliquables.\n" +
                    "Reponds en francais, max 200 mots. Pas de markdown dans texte.";

    private final HttpClient httpClient;

    public GrokService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // ══════════════════════════════════════════════════════════
    //  CHATBOT — envoyerMessage
    // ══════════════════════════════════════════════════════════

    public Map<String, Object> envoyerMessage(String message, String historiqueTexte) {
        try {
            String contenuUser;
            if (historiqueTexte != null && !historiqueTexte.trim().isEmpty()) {
                contenuUser = "Historique de la conversation :\n" + historiqueTexte
                        + "\n---\nNouveau message : " + message;
            } else {
                contenuUser = message;
            }

            String requestBody = buildRequest(SYSTEM_PROMPT_CHAT, contenuUser, 800, 0.3);
            HttpResponse<String> response = envoyerRequete(requestBody);

            if (response.statusCode() == 200) {
                return parseReponse(response.body());
            } else {
                System.err.println("[GrokService] HTTP " + response.statusCode() + " : " + response.body());
                return reponseErreur("Erreur API code " + response.statusCode());
            }

        } catch (java.net.http.HttpTimeoutException e) {
            return reponseErreur("Delai depasse — verifiez votre connexion");
        } catch (Exception e) {
            System.err.println("[GrokService] Exception envoyerMessage : " + e.getMessage());
            return reponseErreur(e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  PANEL IA — isServerAvailable
    // ══════════════════════════════════════════════════════════

    public boolean isServerAvailable() {
        try {
            String requestBody = "{"
                    + "\"model\":\"" + MODEL + "\","
                    + "\"messages\":["
                    + "{\"role\":\"user\",\"content\":\"ping\"}"
                    + "],"
                    + "\"max_tokens\":5"
                    + "}";
            HttpResponse<String> response = envoyerRequete(requestBody);
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════
    //  PANEL IA — recommanderPriorites
    //  Données réelles calculées envoyées à Groq pour prédictions dynamiques
    // ══════════════════════════════════════════════════════════

    public Map<String, Object> recommanderPriorites(
            List<tn.neuron.ardhi.models.MaterielEtMaintenance.Materiel> materiels,
            Map<Integer, List<Map<String, Object>>> histoMap) {

        try {
            LocalDate today = LocalDate.now();
            StringBuilder desc = new StringBuilder();

            for (tn.neuron.ardhi.models.MaterielEtMaintenance.Materiel m : materiels) {
                int id = m.getId_materiel();
                List<Map<String, Object>> histo = histoMap.getOrDefault(id, Collections.emptyList());

                // ── Calculs réels ────────────────────────────────
                long joursAvantMaintenance = 0;
                boolean enRetard = false;
                if (m.getDate_prochaine_maintenance() != null) {
                    joursAvantMaintenance = ChronoUnit.DAYS.between(today, m.getDate_prochaine_maintenance());
                    enRetard = joursAvantMaintenance < 0;
                }

                long ageMois = 0;
                if (m.getDate_achat() != null) {
                    ageMois = ChronoUnit.MONTHS.between(m.getDate_achat(), today);
                }

                int nbMaintenances = histo.size();
                double coutTotal   = histo.stream()
                        .mapToDouble(h -> h.get("cout") != null ? ((Number) h.get("cout")).doubleValue() : 0)
                        .sum();
                double coutMoyen   = nbMaintenances > 0 ? coutTotal / nbMaintenances : 0;

                // Fréquence moyenne entre maintenances (en jours)
                long freqMoyenneJours = 0;
                if (nbMaintenances >= 2) {
                    String premiere = (String) histo.get(0).get("date");
                    String derniere  = (String) histo.get(nbMaintenances - 1).get("date");
                    if (premiere != null && derniere != null) {
                        long totalJours = ChronoUnit.DAYS.between(
                                LocalDate.parse(premiere), LocalDate.parse(derniere));
                        freqMoyenneJours = totalJours / (nbMaintenances - 1);
                    }
                }

                desc.append("---\n")
                        .append("ID:").append(id).append("\n")
                        .append("Nom:").append(m.getNom()).append("\n")
                        .append("Type:").append(m.getType() != null ? m.getType() : "Inconnu").append("\n")
                        .append("Etat:").append(m.getEtat() != null ? m.getEtat() : "Inconnu").append("\n")
                        .append("AgeMois:").append(ageMois).append("\n")
                        .append("EnRetard:").append(enRetard ? "OUI" : "NON").append("\n")
                        .append("JoursAvantMaintenance:").append(joursAvantMaintenance).append("\n")
                        .append("NbMaintenancesHistorique:").append(nbMaintenances).append("\n")
                        .append("CoutMoyenMaintenance:").append(String.format("%.1f", coutMoyen)).append(" TND\n")
                        .append("CoutTotalHistorique:").append(String.format("%.1f", coutTotal)).append(" TND\n")
                        .append("FrequenceMoyenneJours:").append(freqMoyenneJours > 0 ? freqMoyenneJours : "N/A").append("\n");
            }

            // ── Prompt dynamique ─────────────────────────────────
            String systemReco =
                    "Tu es un expert en maintenance agricole predictive. " +
                            "Analyse les donnees reelles de chaque materiel et genere des recommandations PERSONNALISEES.\n" +
                            "Chaque recommandation doit etre basee sur les metriques fournies : retard, age, cout, etat, frequence.\n" +
                            "Le pourcentage de risque doit etre calcule depuis les donnees reelles (retard=+30pts, etat mauvais=+25pts, age>10ans=+20pts, cout eleve=+10pts, etc).\n" +
                            "L'action et les conseils doivent etre SPECIFIQUES au type de materiel et a son etat, jamais generiques.\n" +
                            "Reponds UNIQUEMENT en JSON valide sans texte avant ou apres, sans backticks.\n" +
                            "Format JSON obligatoire :\n" +
                            "{" +
                            "\"recommandations\":[{" +
                            "\"materiel_id\":1," +
                            "\"nom\":\"nom\"," +
                            "\"niveau\":\"CRITIQUE|ELEVE|MOYEN|FAIBLE\"," +
                            "\"pourcentage\":75," +
                            "\"couleur\":\"#e53e3e\"," +
                            "\"emoji\":\"emoji\"," +
                            "\"action\":\"action specifique et concrete basee sur les donnees\"," +
                            "\"raison\":\"explication courte du score (ex: retard 45j + etat mauvais)\"," +
                            "\"conseils\":[\"conseil 1 specifique\",\"conseil 2\",\"conseil 3\"]" +
                            "}]," +
                            "\"nb_critiques\":0," +
                            "\"nb_eleves\":0," +
                            "\"resume_global\":\"synthese en 1 phrase de l etat du parc\"" +
                            "}\n" +
                            "Couleurs : CRITIQUE=#e53e3e ELEVE=#dd6b20 MOYEN=#d69e2e FAIBLE=#38a169\n" +
                            "Emojis : CRITIQUE=🔴 ELEVE=🟠 MOYEN=🟡 FAIBLE=🟢\n" +
                            "Conseils : 2 a 3 conseils precis, actionables, specifiques au type et etat du materiel.\n" +
                            "Reponds en francais. Date du jour : " + today;

            String userReco = "Voici les donnees reelles des materiels a analyser :\n\n" + desc;

            String requestBody = buildRequest(systemReco, userReco, 2000, 0.2);
            HttpResponse<String> response = envoyerRequete(requestBody);

            if (response.statusCode() == 200) {
                return parseRecommandations(response.body(), materiels);
            } else {
                System.err.println("[GrokService] recommanderPriorites HTTP "
                        + response.statusCode() + " : " + response.body());
                Map<String, Object> err = new HashMap<>();
                err.put("error", "HTTP " + response.statusCode());
                return err;
            }

        } catch (Exception e) {
            System.err.println("[GrokService] recommanderPriorites : " + e.getMessage());
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return err;
        }
    }

    // ══════════════════════════════════════════════════════════
    //  CONSTRUCTION REQUÊTE
    // ══════════════════════════════════════════════════════════

    private String buildRequest(String system, String userMessage, int maxTokens, double temperature) {
        return "{"
                + "\"model\":\"" + MODEL + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":" + jsonString(system) + "},"
                + "{\"role\":\"user\",\"content\":" + jsonString(userMessage) + "}"
                + "],"
                + "\"max_tokens\":" + maxTokens + ","
                + "\"temperature\":" + temperature
                + "}";
    }

    private HttpResponse<String> envoyerRequete(String requestBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, java.nio.charset.StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(40))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // ══════════════════════════════════════════════════════════
    //  PARSING RÉPONSE CHATBOT
    // ══════════════════════════════════════════════════════════

    private Map<String, Object> parseReponse(String responseBody) {
        try {
            String content = extraireContent(responseBody);
            if (content == null || content.trim().isEmpty()) return reponseErreur("Reponse vide");

            content = content.trim()
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            int debut = content.indexOf('{');
            int fin   = content.lastIndexOf('}');
            if (debut != -1 && fin > debut) content = content.substring(debut, fin + 1);

            Map<String, Object> result = new HashMap<>();
            result.put("texte",       extraireString(content, "texte"));
            result.put("gravite",     extraireString(content, "gravite"));
            result.put("planifier",   extraireBoolean(content, "planifier"));
            result.put("actions",     extraireListe(content, "actions"));
            result.put("suggestions", extraireListe(content, "suggestions"));
            result.put("emoji",       extraireString(content, "emoji"));

            if (result.get("texte") == null || ((String) result.get("texte")).isEmpty())
                result.put("texte", content);
            if (result.get("gravite") == null || ((String) result.get("gravite")).isEmpty())
                result.put("gravite", "FAIBLE");
            if (result.get("emoji") == null)
                result.put("emoji", "🤖");

            return result;

        } catch (Exception e) {
            System.err.println("[GrokService] parseReponse : " + e.getMessage());
            return reponseErreur("Erreur parsing : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  PARSING RECOMMANDATIONS
    // ══════════════════════════════════════════════════════════

    private Map<String, Object> parseRecommandations(
            String responseBody,
            List<tn.neuron.ardhi.models.MaterielEtMaintenance.Materiel> materiels) {
        try {
            String content = extraireContent(responseBody);
            if (content == null || content.trim().isEmpty()) {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "Reponse vide");
                return err;
            }

            content = content.trim()
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            int debut = content.indexOf('{');
            int fin   = content.lastIndexOf('}');
            if (debut != -1 && fin > debut) content = content.substring(debut, fin + 1);

            // Extraire resume_global
            String resumeGlobal = extraireString(content, "resume_global");

            // Extraire le tableau recommandations
            List<Map<String, Object>> recs = new ArrayList<>();
            int arrayStart = content.indexOf("\"recommandations\"");
            if (arrayStart != -1) {
                int arrOpen  = content.indexOf('[', arrayStart);
                int arrClose = content.lastIndexOf(']');
                if (arrOpen != -1 && arrClose > arrOpen) {
                    String arrContent = content.substring(arrOpen + 1, arrClose);
                    int i = 0;
                    while (i < arrContent.length()) {
                        int objStart = arrContent.indexOf('{', i);
                        if (objStart == -1) break;
                        int depth = 0, objEnd = objStart;
                        for (int j = objStart; j < arrContent.length(); j++) {
                            if (arrContent.charAt(j) == '{') depth++;
                            else if (arrContent.charAt(j) == '}') {
                                depth--;
                                if (depth == 0) { objEnd = j; break; }
                            }
                        }
                        String obj = arrContent.substring(objStart, objEnd + 1);
                        Map<String, Object> rec = parseObjetRec(obj, materiels);
                        if (rec != null) recs.add(rec);
                        i = objEnd + 1;
                    }
                }
            }

            // Fallback si parsing échoue
            if (recs.isEmpty()) {
                LocalDate today = LocalDate.now();
                for (tn.neuron.ardhi.models.MaterielEtMaintenance.Materiel m : materiels) {
                    Map<String, Object> rec = new HashMap<>();
                    rec.put("materiel_id", m.getId_materiel());
                    rec.put("nom",         m.getNom());
                    rec.put("niveau",      "MOYEN");
                    rec.put("pourcentage", 50.0);
                    rec.put("couleur",     "#d69e2e");
                    rec.put("emoji",       "🟡");
                    rec.put("action",      "Planifier maintenance preventive");
                    rec.put("raison",      "Donnees insuffisantes pour analyse fine");
                    rec.put("conseils",    new ArrayList<>());
                    recs.add(rec);
                }
            }

            long nbCritiques = recs.stream().filter(r -> "CRITIQUE".equals(r.get("niveau"))).count();
            long nbEleves    = recs.stream().filter(r -> "ELEVE".equals(r.get("niveau"))).count();

            Map<String, Object> result = new HashMap<>();
            result.put("recommandations", recs);
            result.put("nb_critiques",   (int) nbCritiques);
            result.put("nb_eleves",      (int) nbEleves);
            result.put("resume_global",  resumeGlobal != null ? resumeGlobal : "");
            return result;

        } catch (Exception e) {
            System.err.println("[GrokService] parseRecommandations : " + e.getMessage());
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return err;
        }
    }

    private Map<String, Object> parseObjetRec(String obj,
                                              List<tn.neuron.ardhi.models.MaterielEtMaintenance.Materiel> materiels) {
        try {
            Map<String, Object> rec = new HashMap<>();
            int mid = parseIntFromJson(obj, "materiel_id");
            rec.put("materiel_id", mid);
            rec.put("nom",         extraireString(obj, "nom"));
            rec.put("niveau",      extraireString(obj, "niveau"));
            rec.put("pourcentage", parseDoubleFromJson(obj, "pourcentage"));
            rec.put("couleur",     extraireString(obj, "couleur"));
            rec.put("emoji",       extraireString(obj, "emoji"));
            rec.put("action",      extraireString(obj, "action"));
            rec.put("raison",      extraireString(obj, "raison"));
            rec.put("conseils",    extraireListeDepuisObj(obj, "conseils"));

            String niv = (String) rec.getOrDefault("niveau", "FAIBLE");
            if (niv == null || niv.isEmpty()) niv = "FAIBLE";

            if (rec.get("couleur") == null || ((String) rec.get("couleur")).isEmpty())
                rec.put("couleur", switch (niv) {
                    case "CRITIQUE" -> "#e53e3e"; case "ELEVE" -> "#dd6b20";
                    case "MOYEN"    -> "#d69e2e"; default      -> "#38a169";
                });
            if (rec.get("emoji") == null || ((String) rec.get("emoji")).isEmpty())
                rec.put("emoji", switch (niv) {
                    case "CRITIQUE" -> "🔴"; case "ELEVE" -> "🟠";
                    case "MOYEN"    -> "🟡"; default      -> "🟢";
                });
            if (rec.get("nom") == null || ((String) rec.get("nom")).isEmpty()) {
                materiels.stream().filter(m -> m.getId_materiel() == mid)
                        .findFirst().ifPresent(m -> rec.put("nom", m.getNom()));
            }
            double pct = (double) rec.get("pourcentage");
            if (pct == 0.0) {
                rec.put("pourcentage", switch (niv) {
                    case "CRITIQUE" -> 90.0; case "ELEVE" -> 70.0;
                    case "MOYEN"    -> 50.0; default      -> 25.0;
                });
            }
            return rec;
        } catch (Exception e) {
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════
    //  EXTRACTION CONTENU GROQ
    // ══════════════════════════════════════════════════════════

    private String extraireContent(String json) {
        try {
            int messageIdx  = json.indexOf("\"message\"");
            if (messageIdx == -1) messageIdx = 0;
            int contentIdx  = json.indexOf("\"content\"", messageIdx);
            if (contentIdx == -1) return null;
            int colonIdx    = json.indexOf(":", contentIdx);
            if (colonIdx == -1) return null;

            int debutVal = colonIdx + 1;
            while (debutVal < json.length() && json.charAt(debutVal) == ' ') debutVal++;
            if (debutVal >= json.length() || json.charAt(debutVal) != '"') return null;

            StringBuilder sb = new StringBuilder();
            int i = debutVal + 1;
            while (i < json.length()) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(i + 1);
                    switch (next) {
                        case 'n'  -> { sb.append('\n'); i += 2; }
                        case 't'  -> { sb.append('\t'); i += 2; }
                        case '"'  -> { sb.append('"');  i += 2; }
                        case '\\' -> { sb.append('\\'); i += 2; }
                        case 'r'  -> { i += 2; }
                        default   -> { sb.append(next); i += 2; }
                    }
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                    i++;
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════
    //  PARSEURS JSON LÉGERS
    // ══════════════════════════════════════════════════════════

    private String extraireString(String json, String cle) {
        try {
            int idx = json.indexOf("\"" + cle + "\"");
            if (idx == -1) return null;
            int colon = json.indexOf(":", idx);
            if (colon == -1) return null;

            int debut = colon + 1;
            while (debut < json.length() &&
                    (json.charAt(debut) == ' ' || json.charAt(debut) == '\n')) debut++;
            if (debut >= json.length()) return null;

            if (json.charAt(debut) != '"') {
                int fin = debut;
                while (fin < json.length() && json.charAt(fin) != ','
                        && json.charAt(fin) != '}' && json.charAt(fin) != '\n') fin++;
                return json.substring(debut, fin).trim();
            }

            StringBuilder sb = new StringBuilder();
            int i = debut + 1;
            while (i < json.length()) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(i + 1);
                    switch (next) {
                        case 'n'  -> { sb.append('\n'); i += 2; }
                        case 't'  -> { sb.append('\t'); i += 2; }
                        case '"'  -> { sb.append('"');  i += 2; }
                        case '\\' -> { sb.append('\\'); i += 2; }
                        default   -> { sb.append(next); i += 2; }
                    }
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                    i++;
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean extraireBoolean(String json, String cle) {
        String val = extraireString(json, cle);
        return "true".equalsIgnoreCase(val != null ? val.trim() : "");
    }

    private List<String> extraireListe(String json, String cle) {
        List<String> liste = new ArrayList<>();
        try {
            int idx   = json.indexOf("\"" + cle + "\"");
            if (idx == -1) return liste;
            int debut = json.indexOf("[", idx);
            int fin   = json.indexOf("]", debut);
            if (debut == -1 || fin == -1) return liste;
            String contenu = json.substring(debut + 1, fin).trim();
            if (contenu.isEmpty()) return liste;

            int i = 0;
            while (i < contenu.length()) {
                while (i < contenu.length() &&
                        (contenu.charAt(i) == ',' || contenu.charAt(i) == ' '
                                || contenu.charAt(i) == '\n' || contenu.charAt(i) == '\r')) i++;
                if (i >= contenu.length()) break;
                if (contenu.charAt(i) == '"') {
                    StringBuilder sb = new StringBuilder();
                    i++;
                    while (i < contenu.length()) {
                        char c = contenu.charAt(i);
                        if (c == '\\' && i + 1 < contenu.length()) {
                            char next = contenu.charAt(i + 1);
                            if (next == '"') { sb.append('"'); i += 2; }
                            else { sb.append(next); i += 2; }
                        } else if (c == '"') { i++; break; }
                        else { sb.append(c); i++; }
                    }
                    String val = sb.toString().trim();
                    if (!val.isEmpty()) liste.add(val);
                } else { i++; }
            }
        } catch (Exception ignored) {}
        return liste;
    }

    /**
     * Extrait un tableau de strings depuis un objet JSON —
     * utilisé pour extraire "conseils" depuis un objet recommandation
     */
    private List<String> extraireListeDepuisObj(String obj, String cle) {
        List<String> liste = new ArrayList<>();
        try {
            int idx = obj.indexOf("\"" + cle + "\"");
            if (idx == -1) return liste;
            int arrOpen = obj.indexOf("[", idx);
            if (arrOpen == -1) return liste;

            // Trouver la fermeture du tableau en respectant la profondeur
            int depth = 0;
            int arrClose = -1;
            for (int i = arrOpen; i < obj.length(); i++) {
                if (obj.charAt(i) == '[') depth++;
                else if (obj.charAt(i) == ']') {
                    depth--;
                    if (depth == 0) { arrClose = i; break; }
                }
            }
            if (arrClose == -1) return liste;

            String contenu = obj.substring(arrOpen + 1, arrClose).trim();
            if (contenu.isEmpty()) return liste;

            int i = 0;
            while (i < contenu.length()) {
                while (i < contenu.length() &&
                        (contenu.charAt(i) == ',' || contenu.charAt(i) == ' '
                                || contenu.charAt(i) == '\n' || contenu.charAt(i) == '\r')) i++;
                if (i >= contenu.length()) break;
                if (contenu.charAt(i) == '"') {
                    StringBuilder sb = new StringBuilder();
                    i++;
                    while (i < contenu.length()) {
                        char c = contenu.charAt(i);
                        if (c == '\\' && i + 1 < contenu.length()) {
                            char next = contenu.charAt(i + 1);
                            if (next == '"') { sb.append('"'); i += 2; }
                            else { sb.append(next); i += 2; }
                        } else if (c == '"') { i++; break; }
                        else { sb.append(c); i++; }
                    }
                    String val = sb.toString().trim();
                    if (!val.isEmpty()) liste.add(val);
                } else { i++; }
            }
        } catch (Exception ignored) {}
        return liste;
    }

    private int parseIntFromJson(String json, String cle) {
        try {
            String val = extraireString(json, cle);
            if (val == null) return 0;
            return Integer.parseInt(val.trim().replaceAll("[^0-9]", ""));
        } catch (Exception e) { return 0; }
    }

    private double parseDoubleFromJson(String json, String cle) {
        try {
            String val = extraireString(json, cle);
            if (val == null) return 0;
            return Double.parseDouble(val.trim().replaceAll("[^0-9.]", ""));
        } catch (Exception e) { return 0; }
    }

    private String jsonString(String s) {
        if (s == null) return "\"\"";
        return "\"" + s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t")
                + "\"";
    }

    private Map<String, Object> reponseErreur(String detail) {
        Map<String, Object> res = new HashMap<>();
        res.put("texte",
                "Service IA temporairement indisponible.\n\n" +
                        "Utilisez les boutons de gauche pour les cas courants,\n" +
                        "ou reessayez dans quelques instants.\n\n" +
                        "Detail : " + detail);
        res.put("gravite",     "FAIBLE");
        res.put("planifier",   false);
        res.put("actions",     Collections.emptyList());
        res.put("suggestions", List.of("Bruit bizarre", "Ne demarre pas",
                "Fuite detectee", "Planning saisonnier"));
        res.put("emoji",       "⚠️");
        res.put("error",       true);
        return res;
    }
}
