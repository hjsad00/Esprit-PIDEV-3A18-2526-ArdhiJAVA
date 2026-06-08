package tn.neuron.ardhi.services.UserAndDiag;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;

public class WeatherService {

    // Tunis coordinates (Default fallback)
    private static final double DEFAULT_LAT = 36.8065;
    private static final double DEFAULT_LON = 10.1815;

    // ─── Inner Data Classes ───

    public static class WeatherData {
        public double temperature;
        public int humidity;
        public double precipitation;
        public int weatherCode;
        public String advice;
        public double latitude;
        public double longitude;
        public double windSpeed;
        public double windDirection; // angle in degrees
        public double apparentTemperature;

        public String getWindDirectionString() {
            String[] dirs = { "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                    "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW" };
            return dirs[(int) Math.round(((windDirection % 360) / 22.5)) % 16];
        }

        public String getWeatherIcon() {
            if (weatherCode == 0)
                return "☀️";
            if (weatherCode >= 1 && weatherCode <= 3)
                return "⛅";
            if (weatherCode >= 45 && weatherCode <= 48)
                return "🌫️";
            if (weatherCode >= 51 && weatherCode <= 55)
                return "🌦️";
            if (weatherCode >= 61 && weatherCode <= 65)
                return "🌧️";
            if (weatherCode >= 71 && weatherCode <= 77)
                return "❄️";
            if (weatherCode >= 80 && weatherCode <= 82)
                return "🌦️";
            if (weatherCode >= 95 && weatherCode <= 99)
                return "⛈️";
            return "🌡️";
        }

        public String getWeatherDescription() {
            if (weatherCode == 0)
                return "Ciel dégagé";
            if (weatherCode >= 1 && weatherCode <= 3)
                return "Partiellement nuageux";
            if (weatherCode >= 45 && weatherCode <= 48)
                return "Brouillard";
            if (weatherCode >= 51 && weatherCode <= 55)
                return "Bruine";
            if (weatherCode >= 61 && weatherCode <= 65)
                return "Pluie";
            if (weatherCode >= 71 && weatherCode <= 77)
                return "Neige";
            if (weatherCode >= 80 && weatherCode <= 82)
                return "Averses";
            if (weatherCode >= 95 && weatherCode <= 99)
                return "Orage";
            return "Conditions variables";
        }
    }

    /**
     * Hourly forecast data point.
     */
    public static class HourlyForecast {
        public String time; // ISO format: "2026-02-15T14:00"
        public double temperature;
        public int humidity;
        public double precipitation;
        public int weatherCode;
    }

    /**
     * Forecast data for the next 72 hours.
     */
    public static class ForecastData {
        public List<HourlyForecast> hourly = new ArrayList<>();
        public double latitude;
        public double longitude;

        /**
         * Finds the next dry window of at least minHours consecutive hours with no
         * rain.
         * Returns a human-readable message about when to spray.
         */
        public String getSprayWindow(int minHours) {
            int consecutiveDry = 0;
            String windowStart = null;

            for (HourlyForecast h : hourly) {
                if (h.precipitation <= 0.1) {
                    if (consecutiveDry == 0) {
                        windowStart = h.time;
                    }
                    consecutiveDry++;
                    if (consecutiveDry >= minHours) {
                        String displayTime = formatTime(windowStart);
                        return "✅ Fenêtre de traitement: " + displayTime + " (" + consecutiveDry
                                + "h+ sans pluie prévue)";
                    }
                } else {
                    consecutiveDry = 0;
                    windowStart = null;
                }
            }
            return "⚠️ Pas de fenêtre sèche de " + minHours
                    + "h dans les 72 prochaines heures. Reportez le traitement.";
        }

        /**
         * Checks the next N hours for rain and returns a warning if rain is imminent.
         */
        public String getRainWarning(int hoursAhead) {
            for (int i = 0; i < Math.min(hoursAhead, hourly.size()); i++) {
                HourlyForecast h = hourly.get(i);
                if (h.precipitation > 0.5) {
                    return "🌧️ Pluie prévue dans " + (i + 1)
                            + "h — retardez le traitement pour une meilleure efficacité.";
                }
            }
            return null; // No rain imminent
        }

        private String formatTime(String isoTime) {
            if (isoTime == null || isoTime.length() < 16)
                return isoTime;
            // "2026-02-15T14:00" → "15/02 à 14h00"
            try {
                String date = isoTime.substring(8, 10) + "/" + isoTime.substring(5, 7);
                String time = isoTime.substring(11, 13) + "h" + isoTime.substring(14, 16);
                return date + " à " + time;
            } catch (Exception e) {
                return isoTime;
            }
        }
    }

    // ─── Current Weather ───

    /**
     * Get current weather using default Tunis coordinates (backward-compatible).
     */
    public WeatherData getCurrentWeather() {
        return getCurrentWeather(DEFAULT_LAT, DEFAULT_LON);
    }

    /**
     * Get current weather for a specific location.
     */
    public WeatherData getCurrentWeather(double lat, double lon) {
        try {
            String apiUrl = String.format(java.util.Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                            + "&current=temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m,apparent_temperature&timezone=auto",
                    lat, lon);

            String json = httpGet(apiUrl);
            if (json == null)
                return null;

            WeatherData data = parseWeatherJson(json);
            if (data != null) {
                data.latitude = lat;
                data.longitude = lon;
            }
            return data;

        } catch (Exception e) {
            LogUtils.error(this.getClass(), "Erreur récupération météo", e);
            return null;
        }
    }

    // ─── 72-Hour Forecast ───

    /**
     * Get hourly forecast for the next 72 hours at a specific location.
     * Includes temperature, humidity, precipitation, and weather codes.
     */
    public ForecastData getForecast(double lat, double lon) {
        try {
            String apiUrl = String.format(java.util.Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                            + "&hourly=temperature_2m,relative_humidity_2m,precipitation,weather_code"
                            + "&forecast_days=3&timezone=auto",
                    lat, lon);

            String json = httpGet(apiUrl);
            if (json == null)
                return null;

            return parseForecastJson(json, lat, lon);

        } catch (Exception e) {
            LogUtils.error(this.getClass(), "Erreur récupération prévisions", e);
            return null;
        }
    }

    // ─── HTTP & Parsing ───

    private String httpGet(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) {
                LogUtils.error(this.getClass(), "Erreur API Météo: " + conn.getResponseCode());
                return null;
            }

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return response.toString();

        } catch (Exception e) {
            LogUtils.error(this.getClass(), "HTTP GET error", e);
            return null;
        }
    }

    private WeatherData parseWeatherJson(String json) {
        WeatherData data = new WeatherData();
        try {
            data.temperature = Double.parseDouble(extractValue(json, "temperature_2m"));
            data.humidity = Integer.parseInt(extractValue(json, "relative_humidity_2m"));
            data.precipitation = Double.parseDouble(extractValue(json, "precipitation"));
            data.weatherCode = Integer.parseInt(extractValue(json, "weather_code"));
            data.windSpeed = Double.parseDouble(extractValue(json, "wind_speed_10m"));
            data.windDirection = Double.parseDouble(extractValue(json, "wind_direction_10m"));
            data.apparentTemperature = Double.parseDouble(extractValue(json, "apparent_temperature"));
            generateAgriculturalAdvice(data);
            return data;
        } catch (Exception e) {
            LogUtils.error(this.getClass(), "Erreur parsing météo", e);
            return null;
        }
    }

    private ForecastData parseForecastJson(String json, double lat, double lon) {
        ForecastData forecast = new ForecastData();
        forecast.latitude = lat;
        forecast.longitude = lon;

        try {
            // Extract hourly arrays from JSON
            // Format: "time":["2026-02-15T00:00","2026-02-15T01:00",...]
            List<String> times = extractArray(json, "time");
            List<String> temps = extractNumericArray(json, "temperature_2m");
            List<String> humids = extractNumericArray(json, "relative_humidity_2m");
            List<String> precips = extractNumericArray(json, "precipitation");
            List<String> codes = extractNumericArray(json, "weather_code");

            int count = Math.min(times.size(), Math.min(temps.size(),
                    Math.min(humids.size(), Math.min(precips.size(), codes.size()))));

            for (int i = 0; i < count; i++) {
                HourlyForecast h = new HourlyForecast();
                h.time = times.get(i);
                h.temperature = parseDoubleSafe(temps.get(i));
                h.humidity = (int) parseDoubleSafe(humids.get(i));
                h.precipitation = parseDoubleSafe(precips.get(i));
                h.weatherCode = (int) parseDoubleSafe(codes.get(i));
                forecast.hourly.add(h);
            }

            return forecast;
        } catch (Exception e) {
            LogUtils.error(this.getClass(), "Erreur parsing prévisions", e);
            return null;
        }
    }

    /**
     * Extracts a JSON array of strings (for "time" field).
     * Pattern: "key":["val1","val2",...]
     */
    private List<String> extractArray(String json, String key) {
        List<String> values = new ArrayList<>();
        // Find the array after "key":[
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[([^\\]]+)\\]");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            String arrayContent = matcher.group(1);
            Pattern valPattern = Pattern.compile("\"([^\"]+)\"");
            Matcher valMatcher = valPattern.matcher(arrayContent);
            while (valMatcher.find()) {
                values.add(valMatcher.group(1));
            }
        }
        return values;
    }

    /**
     * Extracts a JSON array of numbers.
     * Pattern: "key":[1.2,3.4,5.6,...]
     */
    private List<String> extractNumericArray(String json, String key) {
        List<String> values = new ArrayList<>();
        // Find the array for the key within the "hourly" block
        // We need to be careful to match the right key in the hourly section
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[([^\\]]+)\\]");
        Matcher matcher = pattern.matcher(json);

        // Skip the first match if it's in "current" or "current_units" section
        // We want the one inside "hourly"
        String arrayContent = null;
        while (matcher.find()) {
            arrayContent = matcher.group(1); // Take the last match (hourly section)
        }

        if (arrayContent != null) {
            Pattern valPattern = Pattern.compile("([\\d.\\-]+)");
            Matcher valMatcher = valPattern.matcher(arrayContent);
            while (valMatcher.find()) {
                values.add(valMatcher.group(1));
            }
        }
        return values;
    }

    private String extractValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\":\\s*([0-9.]+)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "0";
    }

    private double parseDoubleSafe(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private void generateAgriculturalAdvice(WeatherData data) {
        if (data.humidity > 80 && data.temperature > 20) {
            data.advice = "⚠️ Alerte Humidité: Risque élevé de maladies fongiques (Mildiou, Oïdium). Évitez l'irrigation par aspersion.";
        } else if (data.precipitation > 5) {
            data.advice = "🌧️ Pluie prévue: Suspendez les traitements phytosanitaires et l'irrigation.";
        } else if (data.temperature > 35) {
            data.advice = "🔥 Canicule: Assurez une irrigation suffisante en début ou fin de journée pour éviter le stress hydrique.";
        } else if (data.temperature < 5) {
            data.advice = "❄️ Risque de gel: Protégez les cultures sensibles et les semis.";
        } else if (data.weatherCode >= 95) {
            data.advice = "⛈️ Orages: Évitez les travaux aux champs et mettez le matériel à l'abri.";
        } else {
            data.advice = "✅ Conditions favorables: Bon moment pour les inspections de routine et l'entretien.";
        }
    }
}
