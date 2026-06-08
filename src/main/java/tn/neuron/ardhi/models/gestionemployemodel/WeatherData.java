package tn.neuron.ardhi.models.gestionemployemodel;

/**
 * Modèle pour stocker les données météo récupérées depuis OpenWeatherMap.
 */
public class WeatherData {

    private double temperature;     // °C
    private double feelsLike;       // ressenti °C
    private String description;     // ex: "pluie légère"
    private String iconCode;        // ex: "10d"
    private boolean rainExpected;   // true si pluie détectée
    private double windSpeed;       // km/h
    private int humidity;           // %
    private String cityName;        // "Tunis"
    private boolean available;      // false si erreur API

    public WeatherData() {
        this.available = false;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getFeelsLike() { return feelsLike; }
    public void setFeelsLike(double feelsLike) { this.feelsLike = feelsLike; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconCode() { return iconCode; }
    public void setIconCode(String iconCode) { this.iconCode = iconCode; }

    public boolean isRainExpected() { return rainExpected; }
    public void setRainExpected(boolean rainExpected) { this.rainExpected = rainExpected; }

    public double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(double windSpeed) { this.windSpeed = windSpeed; }

    public int getHumidity() { return humidity; }
    public void setHumidity(int humidity) { this.humidity = humidity; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    /**
     * Retourne l'emoji météo correspondant aux conditions.
     */
    public String getWeatherEmoji() {
        if (!available) return "❓";
        if (rainExpected) return "🌧️";
        if (temperature > 40) return "🔥";
        if (temperature > 30) return "☀️";
        if (temperature > 20) return "⛅";
        if (windSpeed > 40) return "💨";
        return "🌤️";
    }

    /**
     * Résumé court pour l'affichage sur le dashboard.
     */
    public String getSummary() {
        if (!available) return "Météo indisponible";
        return String.format("%.0f°C · %s", temperature, capitalize(description));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    public String toString() {
        return "WeatherData{temp=" + temperature + ", rain=" + rainExpected +
               ", wind=" + windSpeed + ", humidity=" + humidity + "}";
    }
}
