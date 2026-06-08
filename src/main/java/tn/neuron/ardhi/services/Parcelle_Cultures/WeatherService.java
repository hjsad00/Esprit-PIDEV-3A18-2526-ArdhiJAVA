package tn.neuron.ardhi.services.Parcelle_Cultures;

import tn.neuron.ardhi.models.Parcelle_Cultures.RecommendationResult.DonneesMeteo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Service météo utilisant l'API Open-Meteo (gratuite, sans clé API).
 * Documentation: https://open-meteo.com/en/docs
 *
 * Retourne : température moyenne, humidité, précipitations
 */
public class WeatherService {

    private static final Logger LOGGER = Logger.getLogger(WeatherService.class.getName());

    // URL de base de l'API Open-Meteo
    private static final String OPEN_METEO_BASE =
            "https://api.open-meteo.com/v1/forecast" +
            "?latitude=%s&longitude=%s" +
            "&current=temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m" +
            "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum" +
            "&forecast_days=1&timezone=auto";

    /**
     * Récupère les données météo actuelles pour les coordonnées GPS données.
     *
     * @param latitude  Latitude de la parcelle
     * @param longitude Longitude de la parcelle
     * @return DonneesMeteo ou données par défaut si l'API est inaccessible
     */
    public DonneesMeteo getMeteo(double latitude, double longitude) {
        DonneesMeteo meteo = new DonneesMeteo();

        try {
            String urlStr = String.format(OPEN_METEO_BASE,
                    String.valueOf(latitude).replace(',', '.'),
                    String.valueOf(longitude).replace(',', '.'));

            LOGGER.info("Appel API Open-Meteo: " + urlStr);

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "Ardhi-Agricultural-App/1.0");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                meteo = parseOpenMeteoResponse(sb.toString());
                LOGGER.info("Meteo recuperee: T=" + meteo.getTemperatureMoyenne() +
                        "C, H=" + meteo.getHumidite() + "%, P=" + meteo.getPrecipitations() + "mm, V=" + meteo.getVitesseVent() + "km/h");
            } else {
                LOGGER.warning("Erreur API Open-Meteo: HTTP " + responseCode);
                meteo = getDefaultMeteo();
            }
            conn.disconnect();

        } catch (Exception e) {
            LOGGER.warning("Impossible de contacter Open-Meteo: " + e.getMessage() +
                    " -> utilisation des donnees par defaut");
            meteo = getDefaultMeteo();
        }

        return meteo;
    }

    /**
     * Parse la réponse JSON de l'API Open-Meteo.
     * Extraction manuelle simple sans dépendance externe.
     * Note: On isole d'abord le bloc "current":{...} car "current_units" contient
     * les mêmes clés mais avec des valeurs texte ("°C", "%"), pas des nombres.
     */
    private DonneesMeteo parseOpenMeteoResponse(String json) {
        DonneesMeteo meteo = new DonneesMeteo();

        try {
            // Isoler le bloc "current":{...} (pas "current_units")
            String currentBlock = extractJsonBlock(json, "\"current\"");
            if (currentBlock == null || currentBlock.isEmpty()) {
                LOGGER.warning("Bloc 'current' introuvable dans la réponse Open-Meteo");
                return getDefaultMeteo();
            }

            LOGGER.info("Bloc current extrait: " + currentBlock.substring(0, Math.min(200, currentBlock.length())));

            // Extraction temperature_2m (current)
            meteo.setTemperatureMoyenne(extractDouble(currentBlock, "\"temperature_2m\":"));

            // Extraction relative_humidity_2m (current)
            meteo.setHumidite(extractDouble(currentBlock, "\"relative_humidity_2m\":"));

            // Extraction precipitation (current)
            meteo.setPrecipitations(extractDouble(currentBlock, "\"precipitation\":"));

            // Extraction wind_speed_10m (current)
            double wind = extractDouble(currentBlock, "\"wind_speed_10m\":");
            meteo.setVitesseVent(wind);

            // Description contextuelle
            meteo.setDescription(buildDescription(meteo.getTemperatureMoyenne(), meteo.getHumidite(), 
                    meteo.getPrecipitations(), wind));

        } catch (Exception e) {
            LOGGER.warning("Erreur parsing JSON météo: " + e.getMessage());
            meteo = getDefaultMeteo();
        }

        return meteo;
    }

    /**
     * Extrait le contenu d'un bloc JSON pour la clé "current" (pas "current_units").
     * Retourne le sous-string {...} du bloc current.
     */
    private String extractJsonBlock(String json, String key) {
        // Chercher "current" qui n'est PAS suivi de _units
        String exactKey = "\"current\"";
        int searchFrom = 0;
        while (searchFrom < json.length()) {
            int idx = json.indexOf(exactKey, searchFrom);
            if (idx < 0) return null;

            // Vérifier que le caractère APRÈS "current" (après le guillemet fermant)
            // n'est pas un _, ce qui indiquerait "current_units" etc.
            // Mais comme on cherche exactement "current" avec guillemets, c'est OK.
            // On vérifie juste que la clé n'est pas en fait "current_units" :
            // "current_units" contiendrait le _ avant le guillemet fermant.
            // Puisqu'on cherche "current" (avec le " fermant), on est sûr.

            int afterKey = idx + exactKey.length();
            // Sauter espaces
            while (afterKey < json.length() && Character.isWhitespace(json.charAt(afterKey))) {
                afterKey++;
            }
            // Doit être ':'
            if (afterKey >= json.length() || json.charAt(afterKey) != ':') {
                searchFrom = afterKey;
                continue;
            }
            afterKey++;
            // Sauter espaces
            while (afterKey < json.length() && Character.isWhitespace(json.charAt(afterKey))) {
                afterKey++;
            }
            // Doit être '{'
            if (afterKey >= json.length() || json.charAt(afterKey) != '{') {
                searchFrom = afterKey;
                continue;
            }

            // Trouver l'accolade fermante correspondante
            int braceStart = afterKey;
            int braceCount = 1;
            int pos = braceStart + 1;
            while (pos < json.length() && braceCount > 0) {
                if (json.charAt(pos) == '{') braceCount++;
                else if (json.charAt(pos) == '}') braceCount--;
                pos++;
            }
            return json.substring(braceStart, pos);
        }
        return null;
    }

    /**
     * Extrait la première valeur numérique après une clé JSON.
     */
    private double extractDouble(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return 0.0;
        int start = idx + key.length();
        // Sauter les espaces et guillemets éventuels sur les clés
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\n' || json.charAt(start) == ':')) {
            start++;
        }
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end))
                || json.charAt(end) == '.' || json.charAt(end) == '-')) {
            end++;
        }
        if (start == end) return 0.0;
        try {
            return Double.parseDouble(json.substring(start, end));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String buildDescription(double temp, double humidity, double precip, double wind) {
        StringBuilder desc = new StringBuilder();
        if (temp < 0) desc.append("Gel fevere ");
        else if (temp < 3) desc.append("Risque de gel ");
        else if (temp < 15) desc.append("Frais ");
        else if (temp < 25) desc.append("Tempere ");
        else if (temp < 35) desc.append("Chaud ");
        else desc.append("Tres chaud ");

        if (precip > 30) desc.append("| Pluie excessive ");
        else if (precip > 5) desc.append("| Pluvieux ");
        else if (precip > 0) desc.append("| Legere pluie ");
        else desc.append("| Sec ");

        if (wind > 40) desc.append("| Vent fort ");
        else if (wind > 20) desc.append("| Vent modere ");

        return desc.toString();
    }

    /**
     * Valeurs par défaut si l'API est inaccessible (Tunisie en hiver).
     */
    private DonneesMeteo getDefaultMeteo() {
        DonneesMeteo meteo = new DonneesMeteo();
        meteo.setTemperatureMoyenne(15.0);
        meteo.setHumidite(60.0);
        meteo.setPrecipitations(2.0);
        meteo.setDescription("Données par défaut (API indisponible) 🌤️");
        return meteo;
    }

    /**
     * Calcule un score météo (0.0 - 1.0) basé sur les données.
     * Utilisé par SplitOptimizer pour le calcul de score culture.
     *
     * @param meteo     Données météo
     * @param typeCulture Type de culture (ex: "Céréale", "Légume")
     */
    public double calculerScoreMeteo(DonneesMeteo meteo, String typeCulture) {
        double score = 0.5; // Score de base

        double temp = meteo.getTemperatureMoyenne();
        double precip = meteo.getPrecipitations();
        double humidity = meteo.getHumidite();

        // Température optimale selon le type de culture
        if (typeCulture != null && typeCulture.toLowerCase().contains("céréale")) {
            // Céréales préfèrent 10-20°C
            score = getTemperatureScore(temp, 10, 20);
        } else if (typeCulture != null && (typeCulture.toLowerCase().contains("légume")
                || typeCulture.toLowerCase().contains("maraîcher"))) {
            // Légumes préfèrent 15-25°C
            score = getTemperatureScore(temp, 15, 25);
        } else if (typeCulture != null && typeCulture.toLowerCase().contains("fruit")) {
            // Fruitiers préfèrent 18-28°C
            score = getTemperatureScore(temp, 18, 28);
        } else {
            // Défaut: score proportionnel à 15-22°C
            score = getTemperatureScore(temp, 15, 22);
        }

        // Pénalité gel
        if (temp < 3.0) score *= 0.3;
        // Pénalité pluie excessive
        if (precip > 30) score *= 0.5;
        // Bonus humidité modérée
        if (humidity >= 40 && humidity <= 70) score *= 1.1;

        return Math.min(1.0, Math.max(0.0, score));
    }

    private double getTemperatureScore(double temp, double min, double max) {
        if (temp < min - 5 || temp > max + 10) return 0.2;
        if (temp >= min && temp <= max) return 1.0;
        if (temp < min) return 0.5 + (temp - (min - 5)) / (min - (min - 5)) * 0.5;
        return 0.5 + (temp - max) / (max + 10 - max) * (-0.3);
    }
}
