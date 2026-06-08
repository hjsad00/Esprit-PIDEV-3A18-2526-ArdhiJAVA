package tn.neuron.ardhi.utils.marketplace;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Service singleton de conversion de devises.
 * Utilise l'API gratuite exchangerate-api.com (sans clé API).
 * Les taux sont mis en cache pendant 1 heure pour limiter les appels réseau.
 *
 * Devises supportées : TND (Dinar Tunisien), EUR, USD
 * Note : "DT" dans l'application correspond au code ISO "TND".
 */
public class CurrencyService {

    private static final String API_URL = "https://api.exchangerate-api.com/v4/latest/";
    private static final long CACHE_TTL_MS = 60 * 60 * 1000L; // 1 heure

    /** Mapping interne : nom affiché → code ISO */
    private static final Map<String, String> DEVISE_TO_ISO = new HashMap<>();
    static {
        DEVISE_TO_ISO.put("DT", "TND");
        DEVISE_TO_ISO.put("EUR", "EUR");
        DEVISE_TO_ISO.put("USD", "USD");
        DEVISE_TO_ISO.put("TND", "TND");
    }

    // Cache : baseCurrency (ISO) → { targetCurrency (ISO) → rate }
    private final Map<String, Map<String, Double>> rateCache = new HashMap<>();
    // Timestamp du dernier fetch par devise de base
    private final Map<String, Long> cacheTimestamps = new HashMap<>();

    // Singleton
    private static CurrencyService instance;

    private CurrencyService() {
    }

    public static CurrencyService getInstance() {
        if (instance == null) {
            instance = new CurrencyService();
        }
        return instance;
    }

    /**
     * Retourne le taux de change de {@code from} vers {@code to}.
     * {@code from} et {@code to} peuvent être des codes ISO (EUR, USD, TND)
     * ou les noms internes de l'application (DT, EUR, USD).
     *
     * @return le taux de change, ou 1.0 en cas d'erreur réseau.
     */
    public double getRate(String from, String to) {
        String isoFrom = toIso(from);
        String isoTo = toIso(to);

        if (isoFrom == null || isoTo == null) {
            System.err.println("[CurrencyService] Devise inconnue : " + from + " ou " + to);
            return 1.0;
        }

        if (isoFrom.equalsIgnoreCase(isoTo)) {
            return 1.0;
        }

        // Vérifier le cache
        if (isCacheValid(isoFrom)) {
            Map<String, Double> rates = rateCache.get(isoFrom);
            if (rates != null && rates.containsKey(isoTo)) {
                return rates.get(isoTo);
            }
        }

        // Appel API
        return fetchRate(isoFrom, isoTo);
    }

    /**
     * Convertit un montant de la devise {@code from} vers la devise {@code to}.
     *
     * @param amount montant à convertir
     * @param from   devise source (code ISO ou nom interne)
     * @param to     devise cible (code ISO ou nom interne)
     * @return montant converti, ou {@code amount} en cas d'erreur.
     */
    public double convert(double amount, String from, String to) {
        double rate = getRate(from, to);
        return amount * rate;
    }

    // -------------------------------------------------------------------------
    // Méthodes privées
    // -------------------------------------------------------------------------

    private String toIso(String devise) {
        if (devise == null)
            return null;
        String upper = devise.trim().toUpperCase();
        return DEVISE_TO_ISO.getOrDefault(upper, upper.length() == 3 ? upper : null);
    }

    private boolean isCacheValid(String isoBase) {
        Long ts = cacheTimestamps.get(isoBase);
        return ts != null && (System.currentTimeMillis() - ts) < CACHE_TTL_MS;
    }

    private double fetchRate(String isoFrom, String isoTo) {
        try {
            String urlStr = API_URL + isoFrom;
            System.out.println("[CurrencyService] Appel API : " + urlStr);

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            if (status != 200) {
                System.err.println("[CurrencyService] Erreur HTTP " + status);
                return 1.0;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            String json = sb.toString();
            Map<String, Double> rates = parseRates(json);

            // Mettre en cache
            rateCache.put(isoFrom, rates);
            cacheTimestamps.put(isoFrom, System.currentTimeMillis());

            Double rate = rates.get(isoTo);
            if (rate == null) {
                System.err.println("[CurrencyService] Taux introuvable pour " + isoTo);
                return 1.0;
            }
            return rate;

        } catch (Exception e) {
            System.err.println("[CurrencyService] Erreur réseau : " + e.getMessage());
            return 1.0;
        }
    }

    /**
     * Parse manuellement le champ "rates" du JSON retourné par l'API.
     * Format attendu : { ..., "rates": { "EUR": 0.91, "USD": 1.08, ... }, ... }
     */
    private Map<String, Double> parseRates(String json) {
        Map<String, Double> rates = new HashMap<>();

        int ratesIdx = json.indexOf("\"rates\"");
        if (ratesIdx < 0)
            return rates;

        int braceOpen = json.indexOf('{', ratesIdx);
        int braceClose = json.indexOf('}', braceOpen);
        if (braceOpen < 0 || braceClose < 0)
            return rates;

        String ratesBlock = json.substring(braceOpen + 1, braceClose);

        // Chaque entrée : "CODE":VALUE
        String[] entries = ratesBlock.split(",");
        for (String entry : entries) {
            entry = entry.trim();
            int colonIdx = entry.indexOf(':');
            if (colonIdx < 0)
                continue;

            String key = entry.substring(0, colonIdx)
                    .replace("\"", "").trim();
            String valStr = entry.substring(colonIdx + 1).trim();

            try {
                double val = Double.parseDouble(valStr);
                rates.put(key, val);
            } catch (NumberFormatException ignored) {
                // ignorer les entrées malformées
            }
        }

        return rates;
    }
}
