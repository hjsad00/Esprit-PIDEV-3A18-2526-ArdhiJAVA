package tn.neuron.ardhi.models.Parcelle_Cultures;

/**
 * Modèle Parcelle enrichi avec géolocalisation GPS
 * pour le système d'aide à la décision agricole Ardhi Premium.
 */
public class Parcelle {
    private int id;
    private double surface;
    private String localisation;
    private String typeSol;
    private String systemeIrrigation;
    private String statut; // active, repos
    private int agriculteurId;

    // ---- NOUVEAU : Coordonnées GPS ----
    private Double latitude;
    private Double longitude;

    // Champ temporaire pour affichage Admin (jointure)
    private String nomAgriculteurTemp;

    // ==================== CONSTRUCTEURS ====================

    public Parcelle() {
    }

    public Parcelle(double surface, String localisation, String typeSol,
                    String systemeIrrigation, String statut, int agriculteurId) {
        this.surface = surface;
        this.localisation = localisation;
        this.typeSol = typeSol;
        this.systemeIrrigation = systemeIrrigation;
        this.statut = statut;
        this.agriculteurId = agriculteurId;
    }

    public Parcelle(int id, double surface, String localisation, String typeSol,
                    String systemeIrrigation, String statut, int agriculteurId) {
        this.id = id;
        this.surface = surface;
        this.localisation = localisation;
        this.typeSol = typeSol;
        this.systemeIrrigation = systemeIrrigation;
        this.statut = statut;
        this.agriculteurId = agriculteurId;
    }

    /** Constructeur complet avec GPS */
    public Parcelle(int id, double surface, String localisation, String typeSol,
                    String systemeIrrigation, String statut, int agriculteurId,
                    Double latitude, Double longitude) {
        this(id, surface, localisation, typeSol, systemeIrrigation, statut, agriculteurId);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // ==================== GETTERS / SETTERS ====================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public double getSurface() { return surface; }
    public void setSurface(double surface) { this.surface = surface; }

    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }

    public String getTypeSol() { return typeSol; }
    public void setTypeSol(String typeSol) { this.typeSol = typeSol; }

    public String getSystemeIrrigation() { return systemeIrrigation; }
    public void setSystemeIrrigation(String systemeIrrigation) { this.systemeIrrigation = systemeIrrigation; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public int getAgriculteurId() { return agriculteurId; }
    public void setAgriculteurId(int agriculteurId) { this.agriculteurId = agriculteurId; }

    // ---- GPS ----
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    /** Vérifie si les coordonnées GPS sont disponibles */
    public boolean hasGpsCoordinates() {
        return latitude != null && longitude != null
                && latitude != 0.0 && longitude != 0.0;
    }

    // ---- Jointure Admin ----
    public String getNomAgriculteurTemp() { return nomAgriculteurTemp; }
    public void setNomAgriculteurTemp(String nomAgriculteurTemp) {
        this.nomAgriculteurTemp = nomAgriculteurTemp;
    }

    @Override
    public String toString() {
        return localisation + " (" + surface + " ha)";
    }
}
