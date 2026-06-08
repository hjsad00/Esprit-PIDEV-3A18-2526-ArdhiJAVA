package tn.neuron.ardhi.models.Parcelle_Cultures;

import java.util.List;

/**
 * DTO résultat du système de recommandation IA pour la division de parcelle.
 * Contient deux cultures recommandées avec leur répartition de surface.
 */
public class RecommendationResult {

    // ==================== CULTURE RECOMMANDÉE ====================

    public static class CultureRecommandee {
        private String nom;
        private String type;
        private String saison;
        private double surface;       // hectares alloués
        private double score;         // score calculé (0-1)
        private double rendementEstime; // tonnes/ha
        private double productionEstimee; // tonnes

        public CultureRecommandee() {}

        public CultureRecommandee(String nom, String type, String saison,
                                   double surface, double score, double rendementEstime) {
            this.nom = nom;
            this.type = type;
            this.saison = saison;
            this.surface = surface;
            this.score = score;
            this.rendementEstime = rendementEstime;
            this.productionEstimee = surface * rendementEstime;
        }

        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getSaison() { return saison; }
        public void setSaison(String saison) { this.saison = saison; }

        public double getSurface() { return surface; }
        public void setSurface(double surface) { this.surface = surface; }

        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }

        public double getRendementEstime() { return rendementEstime; }
        public void setRendementEstime(double rendementEstime) {
            this.rendementEstime = rendementEstime;
            this.productionEstimee = surface * rendementEstime;
        }

        public double getProductionEstimee() { return productionEstimee; }

        @Override
        public String toString() {
            return String.format("%s (%.1f ha, Rendement: %.1f t/ha, Production: %.1f t)",
                    nom, surface, rendementEstime, productionEstimee);
        }
    }

    // ==================== DONNÉES MÉTÉO ====================

    public static class DonneesMeteo {
        private double temperatureMoyenne; // °C
        private double humidite;           // %
        private double precipitations;     // mm/jour
        private double vitesseVent;        // km/h
        private String description;
        private boolean risqueGel;
        private boolean pluieExcessive;

        public DonneesMeteo() {}

        public double getTemperatureMoyenne() { return temperatureMoyenne; }
        public void setTemperatureMoyenne(double temperatureMoyenne) {
            this.temperatureMoyenne = temperatureMoyenne;
            this.risqueGel = temperatureMoyenne < 3.0;
        }

        public double getHumidite() { return humidite; }
        public void setHumidite(double humidite) { this.humidite = humidite; }

        public double getPrecipitations() { return precipitations; }
        public void setPrecipitations(double precipitations) {
            this.precipitations = precipitations;
            this.pluieExcessive = precipitations > 30.0;
        }

        public double getVitesseVent() { return vitesseVent; }
        public void setVitesseVent(double vitesseVent) { this.vitesseVent = vitesseVent; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public boolean isRisqueGel() { return risqueGel; }
        public boolean isPluieExcessive() { return pluieExcessive; }
    }

    // ==================== RÉSULTAT GLOBAL ====================

    private CultureRecommandee culture1;
    private CultureRecommandee culture2;
    private DonneesMeteo meteo;
    private String justification;
    private String source; // "IA_GEMINI", "ALGORITHME_FALLBACK"
    private List<String> alertes;
    private double scoreQualiteParcelle; // 0-10
    private boolean succes;
    private String messageErreur;

    // ==================== CONSTRUCTEURS ====================

    public RecommendationResult() {
        this.succes = false;
    }

    public static RecommendationResult success(CultureRecommandee c1, CultureRecommandee c2,
                                                DonneesMeteo meteo, String justification,
                                                String source) {
        RecommendationResult r = new RecommendationResult();
        r.culture1 = c1;
        r.culture2 = c2;
        r.meteo = meteo;
        r.justification = justification;
        r.source = source;
        r.succes = true;
        return r;
    }

    public static RecommendationResult erreur(String message) {
        RecommendationResult r = new RecommendationResult();
        r.succes = false;
        r.messageErreur = message;
        return r;
    }

    // ==================== GETTERS / SETTERS ====================

    public CultureRecommandee getCulture1() { return culture1; }
    public void setCulture1(CultureRecommandee culture1) { this.culture1 = culture1; }

    public CultureRecommandee getCulture2() { return culture2; }
    public void setCulture2(CultureRecommandee culture2) { this.culture2 = culture2; }

    public DonneesMeteo getMeteo() { return meteo; }
    public void setMeteo(DonneesMeteo meteo) { this.meteo = meteo; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public List<String> getAlertes() { return alertes; }
    public void setAlertes(List<String> alertes) { this.alertes = alertes; }

    public double getScoreQualiteParcelle() { return scoreQualiteParcelle; }
    public void setScoreQualiteParcelle(double scoreQualiteParcelle) {
        this.scoreQualiteParcelle = scoreQualiteParcelle;
    }

    public boolean isSucces() { return succes; }
    public void setSucces(boolean succes) { this.succes = succes; }

    public String getMessageErreur() { return messageErreur; }
    public void setMessageErreur(String messageErreur) { this.messageErreur = messageErreur; }

    /**
     * Surface totale allouée aux deux cultures
     */
    public double getSurfaceTotaleAllouee() {
        double s = 0;
        if (culture1 != null) s += culture1.getSurface();
        if (culture2 != null) s += culture2.getSurface();
        return s;
    }
}
