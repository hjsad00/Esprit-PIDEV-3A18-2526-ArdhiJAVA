package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Micro-Climate Contextualization service.
 * Combines weather forecasts with diagnostic data to explain why diseases
 * appear and to optimize treatment timing.
 */
public class WeatherAlertService {

    private final WeatherService weatherService = new WeatherService();

    // ─── Data Classes ───

    /**
     * A disease-risk alert based on weather conditions.
     */
    public static class DiseaseRiskAlert {
        public String riskLevel; // "Élevé", "Modéré", "Faible"
        public String diseaseType; // e.g. "Maladies fongiques", "Stress hydrique"
        public String reason; // Weather explanation
        public String advice; // Recommended action

        public DiseaseRiskAlert(String riskLevel, String diseaseType, String reason, String advice) {
            this.riskLevel = riskLevel;
            this.diseaseType = diseaseType;
            this.reason = reason;
            this.advice = advice;
        }

        public String getIcon() {
            if ("Élevé".equals(riskLevel))
                return "🔴";
            if ("Modéré".equals(riskLevel))
                return "🟡";
            return "🟢";
        }
    }

    /**
     * Treatment timing suggestion based on weather forecast.
     */
    public static class TreatmentTimingSuggestion {
        public String sprayWindow;
        public String rainWarning;
        public String overallAdvice;
    }

    // ─── Disease Risk from Weather ───

    /**
     * Analyzes the weather forecast for a location and returns disease risk alerts.
     * Checks conditions known to favor specific plant diseases.
     */
    /**
     * Analyzes the weather forecast for a location and returns disease risk alerts.
     * Checks conditions known to favor specific plant diseases within the specified
     * timeframe.
     * 
     * @param hours Number of hours to analyze (e.g., 24, 48, 72).
     */
    public List<DiseaseRiskAlert> getDiseaseRiskAlerts(double lat, double lon, int hours) {
        List<DiseaseRiskAlert> alerts = new ArrayList<>();

        try {
            WeatherService.WeatherData current = weatherService.getCurrentWeather(lat, lon);
            WeatherService.ForecastData forecast = weatherService.getForecast(lat, lon);

            if (current == null)
                return alerts;

            // ── Check current conditions ──

            // High humidity + warm = fungal paradise
            if (current.humidity >= 80 && current.temperature >= 18) {
                String reason = String.format(
                        "Humidité élevée (%d%%) + températures chaudes (%.1f°C) — conditions idéales pour les infections fongiques.",
                        current.humidity, current.temperature);
                alerts.add(new DiseaseRiskAlert("Élevé", "Maladies fongiques",
                        reason,
                        "Inspectez vos cultures pour des signes de mildiou ou oïdium. Envisagez un traitement fongicide préventif."));
            } else if (current.humidity >= 70 && current.temperature >= 15) {
                String reason = String.format(
                        "Humidité modérée (%d%%) + températures douces (%.1f°C) — risque modéré de maladies fongiques.",
                        current.humidity, current.temperature);
                alerts.add(new DiseaseRiskAlert("Modéré", "Maladies fongiques",
                        reason,
                        "Surveillez vos cultures et assurez une bonne aération entre les plants."));
            }

            // Very warm + dry = spider mites & stress
            if (current.temperature >= 30 && current.humidity < 40) {
                alerts.add(new DiseaseRiskAlert("Modéré", "Stress hydrique & Acariens",
                        String.format(
                                "Chaleur élevée (%.1f°C) + faible humidité (%d%%) — risque de stress hydrique et prolifération d'acariens.",
                                current.temperature, current.humidity),
                        "Arrosez en soirée. Inspectez le dessous des feuilles pour détecter les acariens."));
            }

            // ── Check forecast for upcoming risks ──
            if (forecast != null && !forecast.hourly.isEmpty()) {
                // Limit to the requested timeframe
                int limit = Math.min(hours, forecast.hourly.size());
                List<WeatherService.HourlyForecast> analysisWindow = forecast.hourly.subList(0, limit);

                // Count consecutive humid hours in forecast
                int highHumidityRun = 0;
                int maxHumidityRun = 0;
                for (WeatherService.HourlyForecast h : analysisWindow) {
                    if (h.humidity >= 80 && h.temperature >= 15) {
                        highHumidityRun++;
                        maxHumidityRun = Math.max(maxHumidityRun, highHumidityRun);
                    } else {
                        highHumidityRun = 0;
                    }
                }

                if (maxHumidityRun >= 12 && alerts.stream()
                        .noneMatch(a -> "Élevé".equals(a.riskLevel) && a.diseaseType.contains("fongique"))) {
                    alerts.add(new DiseaseRiskAlert("Élevé", "Prévision: Maladies fongiques",
                            String.format(
                                    "Les prévisions indiquent %dh consécutives de forte humidité (>80%%) dans les %d prochaines heures.",
                                    maxHumidityRun, hours),
                            "Envisagez un traitement fongicide préventif avant cette période humide."));
                } else if (maxHumidityRun >= 6 && alerts.stream().noneMatch(a -> a.diseaseType.contains("fongique"))) {
                    alerts.add(new DiseaseRiskAlert("Modéré", "Prévision: Risque fongique",
                            String.format(
                                    "Les prévisions indiquent %dh de forte humidité dans les %d prochaines heures.",
                                    maxHumidityRun, hours),
                            "Assurez une bonne circulation d'air et surveillez vos cultures."));
                }

                // Check for frost risk in forecast
                boolean frostRisk = analysisWindow.stream().anyMatch(h -> h.temperature <= 2);
                if (frostRisk) {
                    alerts.add(new DiseaseRiskAlert("Élevé", "Risque de gel",
                            String.format("Des températures proches de 0°C sont prévues dans les %d prochaines heures.",
                                    hours),
                            "Protégez les cultures sensibles avec des voiles de protection. Évitez de planter."));
                }
            }

        } catch (Exception e) {
            LogUtils.error(WeatherAlertService.class, "Error generating disease risk alerts", e);
        }

        return alerts;
    }

    // ─── Weather-Disease Contextualization ───

    /**
     * Generates a natural-language explanation of why the current weather
     * conditions
     * may have contributed to a diagnosed disease.
     */
    public String getWeatherDiagnosticContext(WeatherService.WeatherData weather, String diseaseName) {
        if (weather == null || diseaseName == null)
            return null;

        String lowerDisease = diseaseName.toLowerCase();
        StringBuilder ctx = new StringBuilder();

        // Fungal diseases
        if (containsAny(lowerDisease, "mildew", "mildiou", "blight", "rust", "rouille", "botrytis", "anthracnose",
                "septoria", "scab", "spot")) {
            if (weather.humidity >= 70) {
                ctx.append(String.format("🔬 Humidité élevée (%d%%) ", weather.humidity));
                if (weather.temperature >= 18 && weather.temperature <= 28) {
                    ctx.append(String.format(
                            "+ températures chaudes (%.1f°C) — conditions idéales pour cette infection fongique.",
                            weather.temperature));
                } else {
                    ctx.append("— facteur favorable au développement de cette maladie fongique.");
                }
            } else if (weather.precipitation > 0) {
                ctx.append(String.format(
                        "🌧️ Précipitations récentes (%.1fmm) — l'humidité résiduelle favorise les infections fongiques.",
                        weather.precipitation));
            }
        }

        // Bacterial diseases
        if (containsAny(lowerDisease, "bacterial", "bactéri", "wilt", "flétrissement", "fire blight", "canker")) {
            if (weather.temperature >= 25 && weather.humidity >= 60) {
                ctx.append(String.format(
                        "🦠 Chaleur (%.1f°C) + humidité (%d%%) — environnement propice aux infections bactériennes.",
                        weather.temperature, weather.humidity));
            }
        }

        // Pest-related
        if (containsAny(lowerDisease, "aphid", "puceron", "mite", "acarien", "thrip", "whitefly", "mouche")) {
            if (weather.temperature >= 25 && weather.humidity < 60) {
                ctx.append(String.format(
                        "🐛 Temps chaud et sec (%.1f°C, %d%% humidité) — conditions favorables à la prolifération des ravageurs.",
                        weather.temperature, weather.humidity));
            }
        }

        if (ctx.length() == 0) {
            return null; // No specific weather context to add
        }
        return ctx.toString();
    }

    // ─── Treatment Timing ───

    /**
     * Generates treatment timing suggestions based on the 72h forecast.
     */
    public TreatmentTimingSuggestion getTreatmentTiming(double lat, double lon) {
        TreatmentTimingSuggestion suggestion = new TreatmentTimingSuggestion();

        try {
            WeatherService.ForecastData forecast = weatherService.getForecast(lat, lon);
            if (forecast == null) {
                suggestion.overallAdvice = "Impossible de récupérer les prévisions météo.";
                return suggestion;
            }

            // Spray window (need at least 4 dry hours)
            suggestion.sprayWindow = forecast.getSprayWindow(4);

            // Rain warning (check next 6 hours)
            suggestion.rainWarning = forecast.getRainWarning(6);

            // Overall advice
            if (suggestion.rainWarning != null) {
                suggestion.overallAdvice = suggestion.rainWarning;
            } else {
                suggestion.overallAdvice = suggestion.sprayWindow;
            }

        } catch (Exception e) {
            LogUtils.error(WeatherAlertService.class, "Error getting treatment timing", e);
            suggestion.overallAdvice = "Erreur lors du calcul du timing de traitement.";
        }

        return suggestion;
    }

    // ─── Utility ───

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw))
                return true;
        }
        return false;
    }
}
