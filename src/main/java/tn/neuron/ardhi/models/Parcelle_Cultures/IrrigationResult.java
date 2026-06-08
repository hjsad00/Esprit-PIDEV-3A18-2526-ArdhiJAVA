package tn.neuron.ardhi.models.Parcelle_Cultures;

/**
 * Résultat complet du Planificateur Intelligent des Besoins en Eau.
 *
 * Contient toutes les valeurs calculées selon les formules agronomiques :
 * - ET0 = 0.0023 × (T + 17.8) × √(Tmax − Tmin)  [Hargreaves simplifié]
 * - Besoin  = Kc × ET0
 * - BesoinNet = max(0, Besoin − Précipitations)
 * - Litres = BesoinNet × Surface(ha) × 10 000
 * - StressIndex = Température / (HumiditéSol + 1)
 * - Efficacité = RendementEstimé / VolumeEau (kg/L)
 */
public class IrrigationResult {

    // ==================== MÉTÉO ====================
    private double temperatureMoyenne;
    private double temperatureMax;
    private double temperatureMin;
    private double precipitationsSemaine; // mm
    private double humidite;              // %
    private String descriptionMeteo;
    private boolean donneesFallback;       // true si données climatiques saisonnières

    // ==================== CALCULS AGRONOMIQUES ====================
    private double kcCulture;             // Coefficient cultural
    private double et0;                   // Évapotranspiration de référence (mm)
    private double besoinBrut;            // Kc × ET0 (mm)
    private double besoinNet;             // max(0, Besoin - Précip) (mm)
    private double volumeEauLitres;       // BesoinNet × Surface × 10 000 (L)
    private double surfaceHectares;

    // ==================== INDICATEURS MÉTIER ====================
    private boolean stressHydriqueDetecte;
    private String causeStress;           // explication qualitative du stress
    private String niveauIrrigation;      // "FAIBLE" | "MODÉRÉ" | "ÉLEVÉ"
    private double efficaciteHydrique;    // RendementEstimé (t/ha) / VolumeEau (ML)
    private String conseilPrincipal;

    // ==================== CONSTRUCTEUR ====================

    public IrrigationResult() {}

    // ==================== GETTERS / SETTERS ====================

    public double getTemperatureMoyenne() { return temperatureMoyenne; }
    public void setTemperatureMoyenne(double v) { this.temperatureMoyenne = v; }

    public double getTemperatureMax() { return temperatureMax; }
    public void setTemperatureMax(double v) { this.temperatureMax = v; }

    public double getTemperatureMin() { return temperatureMin; }
    public void setTemperatureMin(double v) { this.temperatureMin = v; }

    public double getPrecipitationsSemaine() { return precipitationsSemaine; }
    public void setPrecipitationsSemaine(double v) { this.precipitationsSemaine = v; }

    public double getHumidite() { return humidite; }
    public void setHumidite(double v) { this.humidite = v; }

    public String getDescriptionMeteo() { return descriptionMeteo; }
    public void setDescriptionMeteo(String v) { this.descriptionMeteo = v; }

    public boolean isDonneesFallback() { return donneesFallback; }
    public void setDonneesFallback(boolean v) { this.donneesFallback = v; }

    public double getKcCulture() { return kcCulture; }
    public void setKcCulture(double v) { this.kcCulture = v; }

    public double getEt0() { return et0; }
    public void setEt0(double v) { this.et0 = v; }

    public double getBesoinBrut() { return besoinBrut; }
    public void setBesoinBrut(double v) { this.besoinBrut = v; }

    public double getBesoinNet() { return besoinNet; }
    public void setBesoinNet(double v) { this.besoinNet = v; }

    public double getVolumeEauLitres() { return volumeEauLitres; }
    public void setVolumeEauLitres(double v) { this.volumeEauLitres = v; }

    public double getSurfaceHectares() { return surfaceHectares; }
    public void setSurfaceHectares(double v) { this.surfaceHectares = v; }

    public boolean isStressHydriqueDetecte() { return stressHydriqueDetecte; }
    public void setStressHydriqueDetecte(boolean v) { this.stressHydriqueDetecte = v; }

    public String getCauseStress() { return causeStress; }
    public void setCauseStress(String v) { this.causeStress = v; }

    public String getNiveauIrrigation() { return niveauIrrigation; }
    public void setNiveauIrrigation(String v) { this.niveauIrrigation = v; }

    public double getEfficaciteHydrique() { return efficaciteHydrique; }
    public void setEfficaciteHydrique(double v) { this.efficaciteHydrique = v; }

    public String getConseilPrincipal() { return conseilPrincipal; }
    public void setConseilPrincipal(String v) { this.conseilPrincipal = v; }

    /** Volume en m³ (pour affichage compact) */
    public double getVolumeEauM3() { return volumeEauLitres / 1000.0; }

    /** Besoin net arrondi en mm/semaine */
    public String getBesoinNetFormate() {
        return String.format("%.1f mm/semaine", besoinNet);
    }

    /** Volume arrondi en milliers de litres */
    public String getVolumeFormate() {
        if (volumeEauLitres >= 1_000_000) {
            return String.format("%.2f ML", volumeEauLitres / 1_000_000);
        } else if (volumeEauLitres >= 1_000) {
            return String.format("%.1f m³", volumeEauLitres / 1000.0);
        }
        return String.format("%.0f L", volumeEauLitres);
    }
}
