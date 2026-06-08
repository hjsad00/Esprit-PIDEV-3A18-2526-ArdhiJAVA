package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;

public class SporeCastService {

    private WeatherService weatherService = new WeatherService();

    // Contagious diseases that trigger SporeCast
    private static final String[] CONTAGIOUS_DISEASES = {
            "mildiou", "late blight", "early blight", "oïdium", "powdery mildew",
            "rouille", "rust", "tavelure", "apple scab", "alternaria", "alternariose"
    };

    /**
     * Entry point to evaluate the blast radius for a given map location and
     * disease.
     * Called on-demand when a map marker is clicked.
     */
    public SporeCastResult analyzeSourceLocation(double lat, double lon, String diseaseName) {
        boolean isContagious = false;

        if (diseaseName != null) {
            String lowerName = diseaseName.toLowerCase();
            for (String disease : CONTAGIOUS_DISEASES) {
                if (lowerName.contains(disease)) {
                    isContagious = true;
                    break;
                }
            }
        }

        if (!isContagious) {
            return new SporeCastResult(false, 0, "Non Contagieux", 0);
        }

        LogUtils.info(getClass(),
                "SporeCast interactive radar activated for: " + diseaseName + " at " + lat + "," + lon);

        // 1. Fetch exact weather at source location
        WeatherService.WeatherData weather = weatherService.getCurrentWeather(lat, lon);
        if (weather == null) {
            return new SporeCastResult(false, 0, "Erreur Météo", 0);
        }

        // Wind direction is where the wind is coming FROM.
        // Spores will travel in the OPPOSITE direction.
        double windFromDir = weather.windDirection;
        double sporeTravelDir = (windFromDir + 180) % 360;

        // Blast radius for visual rendering (e.g. 25km cone)
        double blastRadiusKm = 25.0;

        return new SporeCastResult(true, sporeTravelDir, weather.getWindDirectionString(), blastRadiusKm);
    }

    public static class SporeCastResult {
        private boolean contagious;
        private double travelAngle; // Direction the spores are blowing towards (0-360)
        private String windDirectionLabel;
        private double blastRadiusKm;

        public SporeCastResult(boolean contagious, double travelAngle, String windDirectionLabel,
                double blastRadiusKm) {
            this.contagious = contagious;
            this.travelAngle = travelAngle;
            this.windDirectionLabel = windDirectionLabel;
            this.blastRadiusKm = blastRadiusKm;
        }

        // Getters are required for the JS bridge to access the values
        public boolean isContagious() {
            return contagious;
        }

        public double getTravelAngle() {
            return travelAngle;
        }

        public String getWindDirectionLabel() {
            return windDirectionLabel;
        }

        public double getBlastRadiusKm() {
            return blastRadiusKm;
        }
    }
}
