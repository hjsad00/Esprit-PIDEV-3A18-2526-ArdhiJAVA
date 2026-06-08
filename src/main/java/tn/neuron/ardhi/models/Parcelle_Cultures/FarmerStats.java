package tn.neuron.ardhi.models.Parcelle_Cultures;

import java.util.Map;

/**
 * Indicateurs statistiques pour un agriculteur donné.
 * Utilisé par le module de tableau de bord parcelles/cultures.
 */
public class FarmerStats {

    // Agrégats de base
    private double surfaceTotale;          // ha
    private double productionTotale;       // tonnes
    private double tauxParcellesActives;   // %

    // Diversification / rendement
    private double diversification;        // 0..1 (types distincts / cultures totales)
    private double rendementMoyen;         // t/ha

    // Scores issus d'autres services
    private double optimisationEau;        // 0..10
    private double rentabilite;            // 0..10

    // Score global
    private double scoreGlobal;            // 0..10
    private String niveauPerformance;      // Faible / Moyen / Excellent

    // Répartition des types de cultures (type -> %)
    private Map<String, Double> repartitionTypesPourcent;

    public double getSurfaceTotale() {
        return surfaceTotale;
    }

    public void setSurfaceTotale(double surfaceTotale) {
        this.surfaceTotale = surfaceTotale;
    }

    public double getProductionTotale() {
        return productionTotale;
    }

    public void setProductionTotale(double productionTotale) {
        this.productionTotale = productionTotale;
    }

    public double getTauxParcellesActives() {
        return tauxParcellesActives;
    }

    public void setTauxParcellesActives(double tauxParcellesActives) {
        this.tauxParcellesActives = tauxParcellesActives;
    }

    public double getDiversification() {
        return diversification;
    }

    public void setDiversification(double diversification) {
        this.diversification = diversification;
    }

    public double getRendementMoyen() {
        return rendementMoyen;
    }

    public void setRendementMoyen(double rendementMoyen) {
        this.rendementMoyen = rendementMoyen;
    }

    public double getOptimisationEau() {
        return optimisationEau;
    }

    public void setOptimisationEau(double optimisationEau) {
        this.optimisationEau = optimisationEau;
    }

    public double getRentabilite() {
        return rentabilite;
    }

    public void setRentabilite(double rentabilite) {
        this.rentabilite = rentabilite;
    }

    public double getScoreGlobal() {
        return scoreGlobal;
    }

    public void setScoreGlobal(double scoreGlobal) {
        this.scoreGlobal = scoreGlobal;
    }

    public String getNiveauPerformance() {
        return niveauPerformance;
    }

    public void setNiveauPerformance(String niveauPerformance) {
        this.niveauPerformance = niveauPerformance;
    }

    public Map<String, Double> getRepartitionTypesPourcent() {
        return repartitionTypesPourcent;
    }

    public void setRepartitionTypesPourcent(Map<String, Double> repartitionTypesPourcent) {
        this.repartitionTypesPourcent = repartitionTypesPourcent;
    }
}

