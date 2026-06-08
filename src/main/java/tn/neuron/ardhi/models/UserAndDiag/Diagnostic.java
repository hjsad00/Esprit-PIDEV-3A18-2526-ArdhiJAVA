package tn.neuron.ardhi.models.UserAndDiag;

import java.sql.Timestamp;

public class Diagnostic {
    private int id;
    private Timestamp dateScan;
    private String imageScannee; // Chemin de l'image uploadée par le user
    private String resultatIA; // Réponse complète de Gemini
    private float confiance; // Pourcentage (ex: 95.5)
    private int userId;

    // Location fields for disease mapping
    private Double latitude;
    private Double longitude;
    private String locationLabel; // e.g., "Tunis, Tunisia"

    // Severity/danger level determined by AI
    private Severity severity;

    // URL to the AI-generated 3D model (GLB/OBJ)
    private String model3dUrl;

    public Diagnostic() {
    }

    // Constructeur complet
    public Diagnostic(int id, Timestamp dateScan, String imageScannee, String resultatIA, float confiance, int userId) {
        this.id = id;
        this.dateScan = dateScan;
        this.imageScannee = imageScannee;
        this.resultatIA = resultatIA;
        this.confiance = confiance;
        this.userId = userId;
    }

    // Constructeur pour Création (Avant insertion)
    public Diagnostic(String imageScannee, String resultatIA, float confiance, int userId) {
        this.imageScannee = imageScannee;
        this.resultatIA = resultatIA;
        this.confiance = confiance;
        this.userId = userId;
    }

    // Getters & Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Timestamp getDateScan() {
        return dateScan;
    }

    public void setDateScan(Timestamp dateScan) {
        this.dateScan = dateScan;
    }

    public String getImageScannee() {
        return imageScannee;
    }

    public void setImageScannee(String imageScannee) {
        this.imageScannee = imageScannee;
    }

    public String getResultatIA() {
        return resultatIA;
    }

    public void setResultatIA(String resultatIA) {
        this.resultatIA = resultatIA;
    }

    public float getConfiance() {
        return confiance;
    }

    public void setConfiance(float confiance) {
        this.confiance = confiance;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    // Location Getters & Setters
    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getLocationLabel() {
        return locationLabel;
    }

    public void setLocationLabel(String locationLabel) {
        this.locationLabel = locationLabel;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    // Convenience method to set severity from String (for DB/AI parsing)
    public void setSeverityFromString(String severityStr) {
        this.severity = Severity.fromString(severityStr);
    }

    // Convenience method to get severity as String (for DB storage)
    public String getSeverityAsString() {
        return severity != null ? severity.name() : null;
    }

    public String getModel3dUrl() {
        return model3dUrl;
    }

    public void setModel3dUrl(String model3dUrl) {
        this.model3dUrl = model3dUrl;
    }
}