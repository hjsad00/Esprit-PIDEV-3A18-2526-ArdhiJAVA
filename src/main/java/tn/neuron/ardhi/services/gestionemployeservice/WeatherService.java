package tn.neuron.ardhi.services.gestionemployeservice;

import tn.neuron.ardhi.models.gestionemployemodel.Tache;
import tn.neuron.ardhi.models.gestionemployemodel.WeatherData;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import tn.neuron.ardhi.utils.UserAndDiag.AppConfig;

/**
 * Service météo utilisant OpenWeatherMap API.
 * Analyse les conditions météo et génère des recommandations agricoles.
 */
public class WeatherService {

    private static final String API_KEY  = AppConfig.get("openweathermap.api.key");
    private static final String CITY     = "Tunis,TN";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final String LANG     = "fr";
    private static final String UNITS    = "metric";

    // Cache simple (5 minutes)
    private WeatherData cachedData    = null;
    private long        lastFetchTime = 0;
    private static final long CACHE_DURATION_MS = 5 * 60 * 1000;

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Récupère la météo actuelle (avec cache 5 min).
     */
    public WeatherData getCurrentWeather() {
        long now = System.currentTimeMillis();
        if (cachedData != null && (now - lastFetchTime) < CACHE_DURATION_MS) {
            return cachedData;
        }
        cachedData = fetchWeather();
        lastFetchTime = now;
        return cachedData;
    }

    // ══ RECOMMANDATION MÉTÉO INTELLIGENTE ════════════════════════════════════

    /** Niveau de recommandation. */
    public enum NiveauReco { POSITIVE, WARNING, DANGER }

    /** Recommandation structurée : niveau + message + type de notification. */
    public static class Recommandation {
        public final NiveauReco niveau;
        public final String message;
        public final String notifType; // correspond aux Notification.TYPE_METEO_*

        public Recommandation(NiveauReco niveau, String message, String notifType) {
            this.niveau    = niveau;
            this.message   = message;
            this.notifType = notifType;
        }

        public boolean isPositive() { return niveau == NiveauReco.POSITIVE; }
    }

    /**
     * Analyse intelligente : retourne des recommandations POSITIVES et NÉGATIVES
     * en fonction des conditions météo et du type de tâche.
     */
    public List<Recommandation> analyserConditionsPourTache(Tache tache, WeatherData w) {
        List<Recommandation> recos = new ArrayList<>();
        if (!w.isAvailable()) return recos;

        Tache.TypeTache type = tache.getTypeTache();
        double temp   = w.getTemperature();
        double vent   = w.getWindSpeed();
        boolean pluie = w.isRainExpected();
        boolean bonVent = vent >= 5 && vent <= 25;
        boolean bonne   = !pluie && temp >= 15 && temp <= 32 && vent <= 35;

        switch (type) {

            // ── TRAITEMENT ────────────────────────────────────────────────────
            case TRAITEMENT:
                if (pluie) {
                    recos.add(new Recommandation(NiveauReco.DANGER,
                        "🌧️ Pluie prévue : traitement « " + tache.getTitre() +
                        " » sera lavé, inefficace. Reporter à demain.",
                        "METEO_PLUIE"));
                } else if (vent > 40) {
                    recos.add(new Recommandation(NiveauReco.DANGER,
                        "💨 Vent fort (" + (int)vent + " km/h) : pulvérisation « " +
                        tache.getTitre() + " » déconseillée, risque de dérive.",
                        "METEO_VENT"));
                } else if (bonVent && !pluie && temp >= 15 && temp <= 30) {
                    recos.add(new Recommandation(NiveauReco.POSITIVE,
                        "✅ Conditions idéales pour le traitement « " + tache.getTitre() +
                        " » : " + (int)temp + "°C, vent " + (int)vent + " km/h — commencez maintenant.",
                        "METEO_POSITIVE"));
                }
                break;

            // ── IRRIGATION ────────────────────────────────────────────────────
            case IRRIGATION:
                if (pluie) {
                    recos.add(new Recommandation(NiveauReco.WARNING,
                        "🌧️ Pluie prévue : irrigation « " + tache.getTitre() +
                        " » non nécessaire. Économisez l'eau.",
                        "METEO_PLUIE"));
                } else if (temp > 35) {
                    recos.add(new Recommandation(NiveauReco.WARNING,
                        "🌡️ Chaleur (" + (int)temp + "°C) : doublez la fréquence d'arrosage pour « " +
                        tache.getTitre() + " ». Irrigation tôt le matin.",
                        "METEO_CHALEUR"));
                } else if (temp >= 18 && temp <= 30 && !pluie) {
                    recos.add(new Recommandation(NiveauReco.POSITIVE,
                        "✅ Idéal pour irriguer « " + tache.getTitre() + " » : " +
                        (int)temp + "°C, pas de pluie — évaporation modérée.",
                        "METEO_POSITIVE"));
                }
                break;

            // ── RÉCOLTE ──────────────────────────────────────────────────────
            case RECOLTE:
                if (pluie) {
                    recos.add(new Recommandation(NiveauReco.DANGER,
                        "🌧️ Pluie prévue : récolte « " + tache.getTitre() +
                        " » déconseillée — risque de moisissures et pertes.",
                        "METEO_PLUIE"));
                } else if (temp > 38) {
                    recos.add(new Recommandation(NiveauReco.WARNING,
                        "🌡️ Chaleur (" + (int)temp + "°C) : planifiez la récolte « " +
                        tache.getTitre() + " » avant 9h du matin.",
                        "METEO_CHALEUR"));
                } else if (bonne) {
                    recos.add(new Recommandation(NiveauReco.POSITIVE,
                        "✅ Excellent moment pour la récolte « " + tache.getTitre() +
                        " » : " + (int)temp + "°C, ciel dégagé — qualité optimale.",
                        "METEO_POSITIVE"));
                }
                break;

            // ── PLANTATION ───────────────────────────────────────────────────
            case PLANTATION:
                if (temp > 38) {
                    recos.add(new Recommandation(NiveauReco.DANGER,
                        "🌡️ Chaleur excessive (" + (int)temp + "°C) : plantation « " +
                        tache.getTitre() + " » risquée. Attendez le soir ou une journée fraîche.",
                        "METEO_CHALEUR"));
                } else if (pluie) {
                    recos.add(new Recommandation(NiveauReco.POSITIVE,
                        "✅ Pluie prévue : excellent moment pour planter « " + tache.getTitre() +
                        " » — humidité naturelle du sol assurée.",
                        "METEO_POSITIVE"));
                } else if (bonne) {
                    recos.add(new Recommandation(NiveauReco.POSITIVE,
                        "✅ Bonne journée pour la plantation « " + tache.getTitre() +
                        " » : " + (int)temp + "°C, conditions favorables.",
                        "METEO_POSITIVE"));
                }
                break;

            // ── LABOUR ───────────────────────────────────────────────────────
            case LABOUR:
                if (pluie) {
                    recos.add(new Recommandation(NiveauReco.DANGER,
                        "🌧️ Sol détrempé : labour « " + tache.getTitre() +
                        " » déconseillé — compaction et dommages sur la structure du sol.",
                        "METEO_PLUIE"));
                } else if (!pluie && temp <= 30) {
                    recos.add(new Recommandation(NiveauReco.POSITIVE,
                        "✅ Sol sec : conditions parfaites pour le labour « " + tache.getTitre() +
                        " » — profitez de cette fenêtre météo.",
                        "METEO_POSITIVE"));
                }
                break;

            // ── MAINTENANCE ──────────────────────────────────────────────────
            case MAINTENANCE:
                if (pluie || vent > 50) {
                    recos.add(new Recommandation(NiveauReco.WARNING,
                        "⚠️ Météo défavorable : reportez la maintenance extérieure « " +
                        tache.getTitre() + " » à une journée plus calme.",
                        "METEO_INFO"));
                } else if (bonne) {
                    recos.add(new Recommandation(NiveauReco.POSITIVE,
                        "✅ Bonne météo pour la maintenance « " + tache.getTitre() +
                        " » : conditions sûres pour travailler à l'extérieur.",
                        "METEO_POSITIVE"));
                }
                break;

            default:
                // Tâche générique — recommandation globale si conditions mauvaises
                if (pluie && vent > 30) {
                    recos.add(new Recommandation(NiveauReco.WARNING,
                        "⚠️ Pluie + vent : activités extérieures pour « " + tache.getTitre() + " » limitées.",
                        "METEO_INFO"));
                }
                break;
        }
        return recos;
    }

    // ── Méthode de compatibilité (retourne texte brut) ─────────────────────────
    /** @deprecated Utilisez {@link #analyserConditionsPourTache(Tache, WeatherData)} qui retourne des Recommandation. */
    public List<String> analyserConditionsPourTacheTexte(Tache tache, WeatherData w) {
        List<String> msgs = new ArrayList<>();
        for (Recommandation r : analyserConditionsPourTache(tache, w)) msgs.add(r.message);
        return msgs;
    }



    // ── Parsing JSON ─────────────────────────────────────────────────────────

    private WeatherData fetchWeather() {
        WeatherData data = new WeatherData();
        try {
            String urlStr = BASE_URL
                    + "?q=" + CITY
                    + "&units=" + UNITS
                    + "&lang=" + LANG
                    + "&appid=" + API_KEY;

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            if (status != 200) {
                String reason;
                switch (status) {
                    case 401: reason = "Clé API non activée (401) — attendez 1-2h"; break;
                    case 404: reason = "Ville introuvable (404)"; break;
                    case 429: reason = "Quota API dépassé (429)"; break;
                    default:  reason = "Erreur serveur HTTP " + status; break;
                }
                data.setDescription(reason);
                System.err.println("⚠️ WeatherService: " + reason);
                return data;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            String json = sb.toString();
            System.out.println("🌦️ JSON reçu: " + json.substring(0, Math.min(json.length(), 200)));
            parseJson(json, data);
            data.setAvailable(true);

        } catch (java.net.UnknownHostException e) {
            data.setDescription("Pas de connexion internet");
            System.err.println("⚠️ WeatherService: pas de réseau - " + e.getMessage());
        } catch (java.net.SocketTimeoutException e) {
            data.setDescription("Délai dépassé (timeout)");
            System.err.println("⚠️ WeatherService: timeout");
        } catch (Exception e) {
            data.setDescription("Erreur: " + e.getMessage());
            System.err.println("⚠️ WeatherService: " + e.getMessage());
        }
        return data;
    }

    /**
     * Parser JSON manuel léger — évite une dépendance supplémentaire.
     */
    private void parseJson(String json, WeatherData data) {
        // température
        data.setTemperature(extractDouble(json, "\"temp\":"));
        data.setFeelsLike(extractDouble(json, "\"feels_like\":"));

        // humidité
        data.setHumidity((int) extractDouble(json, "\"humidity\":"));

        // vent (m/s → km/h)
        double windMs = extractDouble(json, "\"speed\":");
        data.setWindSpeed(windMs * 3.6);

        // description
        String desc = extractString(json, "\"description\":\"");
        data.setDescription(desc != null ? desc : "N/A");

        // icône
        String icon = extractString(json, "\"icon\":\"");
        data.setIconCode(icon != null ? icon : "01d");

        // ville
        String city = extractString(json, "\"name\":\"");
        data.setCityName(city != null ? city : CITY);

        // pluie : détectée via id météo (2xx=storm, 3xx=drizzle, 5xx=rain, 6xx=snow)
        double weatherId = extractDouble(json, "\"id\":");
        boolean rain = (weatherId >= 200 && weatherId < 700);
        data.setRainExpected(rain);
    }

    private double extractDouble(String json, String key) {
        try {
            int idx = json.indexOf(key);
            if (idx < 0) return 0;
            int start = idx + key.length();
            int end = start;
            while (end < json.length() &&
                    (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) {
                end++;
            }
            return Double.parseDouble(json.substring(start, end));
        } catch (Exception e) {
            return 0;
        }
    }

    private String extractString(String json, String key) {
        try {
            int idx = json.indexOf(key);
            if (idx < 0) return null;
            int start = idx + key.length();
            int end   = json.indexOf("\"", start);
            if (end < 0) return null;
            return json.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }
}
