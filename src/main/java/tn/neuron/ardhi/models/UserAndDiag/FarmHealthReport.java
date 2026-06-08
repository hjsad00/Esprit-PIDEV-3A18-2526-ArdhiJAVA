package tn.neuron.ardhi.models.UserAndDiag;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class FarmHealthReport {
    private int id;
    private int scanId;
    private int healthScore; // 0-100
    private int biodiversityScore; // 0-100
    private String llavaAnalysis; // raw AI output (all 6 photo analyses)
    private Timestamp generatedAt;

    // Display helpers (populated via service)
    private List<Vulnerability> vulnerabilities;
    private List<PreventionPlan> preventionPlans;

    // Scan metadata for display
    private String cropType;
    private String growthStage;

    public FarmHealthReport() {
        this.vulnerabilities = new ArrayList<>();
        this.preventionPlans = new ArrayList<>();
    }

    public FarmHealthReport(int scanId, int healthScore, int biodiversityScore, String llavaAnalysis) {
        this.scanId = scanId;
        this.healthScore = healthScore;
        this.biodiversityScore = biodiversityScore;
        this.llavaAnalysis = llavaAnalysis;
        this.vulnerabilities = new ArrayList<>();
        this.preventionPlans = new ArrayList<>();
    }

    // Getters & Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getScanId() {
        return scanId;
    }

    public void setScanId(int scanId) {
        this.scanId = scanId;
    }

    public int getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(int healthScore) {
        this.healthScore = healthScore;
    }

    public int getBiodiversityScore() {
        return biodiversityScore;
    }

    public void setBiodiversityScore(int biodiversityScore) {
        this.biodiversityScore = biodiversityScore;
    }

    public String getLlavaAnalysis() {
        return llavaAnalysis;
    }

    public void setLlavaAnalysis(String llavaAnalysis) {
        this.llavaAnalysis = llavaAnalysis;
    }

    public Timestamp getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Timestamp generatedAt) {
        this.generatedAt = generatedAt;
    }

    public List<Vulnerability> getVulnerabilities() {
        return vulnerabilities;
    }

    public void setVulnerabilities(List<Vulnerability> vulnerabilities) {
        this.vulnerabilities = vulnerabilities;
    }

    public List<PreventionPlan> getPreventionPlans() {
        return preventionPlans;
    }

    public void setPreventionPlans(List<PreventionPlan> preventionPlans) {
        this.preventionPlans = preventionPlans;
    }

    public String getCropType() {
        return cropType;
    }

    public void setCropType(String cropType) {
        this.cropType = cropType;
    }

    public String getGrowthStage() {
        return growthStage;
    }

    public void setGrowthStage(String growthStage) {
        this.growthStage = growthStage;
    }

    /**
     * Returns a color hex for the health score gauge.
     */
    public String getScoreColor() {
        if (healthScore >= 80)
            return "#27ae60"; // green
        if (healthScore >= 50)
            return "#f39c12"; // orange
        return "#e74c3c"; // red
    }

    /**
     * Returns a label for the health score.
     */
    public String getScoreLabel() {
        if (healthScore >= 80)
            return "Excellent";
        if (healthScore >= 60)
            return "Bon";
        if (healthScore >= 40)
            return "Moyen";
        if (healthScore >= 20)
            return "À risque";
        return "Critique";
    }
}
