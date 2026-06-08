package tn.neuron.ardhi.models.Parcelle_Cultures;

import java.util.ArrayList;
import java.util.List;

/**
 * Résultat complet du Calculateur Intelligent de Rendement et Revenus (ROI Agricole).
 *
 * Formules implémentées :
 *   FacteurClimatique = 1 − (0.01×JoursCanicule) − (0.005×JoursExcèsPluie) − (0.02×JoursGel) [min 0.5]
 *   ProductionRéelle  = Surface × RendementThéorique × FacteurClimatique
 *   CoûtTotal         = Semences + Engrais + MainOeuvre + Irrigation + AutresCharges
 *   MargeBrute        = (ProductionRéelle × PrixVente) − CoûtTotal
 *   PrixSeuil         = CoûtTotal / ProductionRéelle
 *   ScoreROI          = (MargeBrute / CoûtTotal) × 100  [%]
 */
public class RoiResult {

    // ==================== DONNÉES CLIMATIQUES ====================
    private int    joursCanicule;       // jours T > 35°C
    private int    joursExcesPluie;     // jours précip > 30 mm
    private int    joursGel;            // jours T < 0°C
    private double facteurClimatique;   // 0.5 → 1.0
    private boolean donneeClimatFallback;

    // ==================== PRODUCTION ====================
    private double surfaceHectares;
    private double rendementTheorique;   // t/ha
    private double productionTheorique;  // = Surface × Rendement
    private double productionReelle;     // ajustée par FacteurClimatique (t)
    private double perteProdClimat;      // Production perdue à cause du climat (t)

    // ==================== COÛTS (DT) ====================
    private double coutSemences;
    private double coutEngrais;
    private double coutMainOeuvre;
    private double coutIrrigation;
    private double coutAutres;
    private double coutTotal;

    // ==================== REVENUS & MARGES ====================
    private double prixVente;           // DT/t
    private double revenuBrut;          // ProductionRéelle × PrixVente
    private double margeBrute;          // RevenuBrut − CoûtTotal
    private double prixSeuil;           // CoûtTotal / ProductionRéelle
    private double scoreRoi;            // (MargeBrute / CoûtTotal) × 100 [%]

    // ==================== INDICATEURS ====================
    private String statut;              // "PERTE" | "ÉQUILIBRE" | "PROFIT"
    private String couleurStatut;       // Hex CSS
    private String conseil;
    private List<String> alertes = new ArrayList<>();

    // ==================== CONSTRUCTEUR ====================
    public RoiResult() {}

    // ==================== GETTERS / SETTERS ====================

    public int getJoursCanicule() { return joursCanicule; }
    public void setJoursCanicule(int v) { this.joursCanicule = v; }

    public int getJoursExcesPluie() { return joursExcesPluie; }
    public void setJoursExcesPluie(int v) { this.joursExcesPluie = v; }

    public int getJoursGel() { return joursGel; }
    public void setJoursGel(int v) { this.joursGel = v; }

    public double getFacteurClimatique() { return facteurClimatique; }
    public void setFacteurClimatique(double v) { this.facteurClimatique = v; }

    public boolean isDonneeClimatFallback() { return donneeClimatFallback; }
    public void setDonneeClimatFallback(boolean v) { this.donneeClimatFallback = v; }

    public double getSurfaceHectares() { return surfaceHectares; }
    public void setSurfaceHectares(double v) { this.surfaceHectares = v; }

    public double getRendementTheorique() { return rendementTheorique; }
    public void setRendementTheorique(double v) { this.rendementTheorique = v; }

    public double getProductionTheorique() { return productionTheorique; }
    public void setProductionTheorique(double v) { this.productionTheorique = v; }

    public double getProductionReelle() { return productionReelle; }
    public void setProductionReelle(double v) { this.productionReelle = v; }

    public double getPerteProdClimat() { return perteProdClimat; }
    public void setPerteProdClimat(double v) { this.perteProdClimat = v; }

    public double getCoutSemences() { return coutSemences; }
    public void setCoutSemences(double v) { this.coutSemences = v; }

    public double getCoutEngrais() { return coutEngrais; }
    public void setCoutEngrais(double v) { this.coutEngrais = v; }

    public double getCoutMainOeuvre() { return coutMainOeuvre; }
    public void setCoutMainOeuvre(double v) { this.coutMainOeuvre = v; }

    public double getCoutIrrigation() { return coutIrrigation; }
    public void setCoutIrrigation(double v) { this.coutIrrigation = v; }

    public double getCoutAutres() { return coutAutres; }
    public void setCoutAutres(double v) { this.coutAutres = v; }

    public double getCoutTotal() { return coutTotal; }
    public void setCoutTotal(double v) { this.coutTotal = v; }

    public double getPrixVente() { return prixVente; }
    public void setPrixVente(double v) { this.prixVente = v; }

    public double getRevenuBrut() { return revenuBrut; }
    public void setRevenuBrut(double v) { this.revenuBrut = v; }

    public double getMargeBrute() { return margeBrute; }
    public void setMargeBrute(double v) { this.margeBrute = v; }

    public double getPrixSeuil() { return prixSeuil; }
    public void setPrixSeuil(double v) { this.prixSeuil = v; }

    public double getScoreRoi() { return scoreRoi; }
    public void setScoreRoi(double v) { this.scoreRoi = v; }

    public String getStatut() { return statut; }
    public void setStatut(String v) { this.statut = v; }

    public String getCouleurStatut() { return couleurStatut; }
    public void setCouleurStatut(String v) { this.couleurStatut = v; }

    public String getConseil() { return conseil; }
    public void setConseil(String v) { this.conseil = v; }

    public List<String> getAlertes() { return alertes; }
    public void setAlertes(List<String> v) { this.alertes = v; }
    public void addAlerte(String a) { this.alertes.add(a); }

    // ==================== FORMATAGE ====================

    public String getFacteurClimatiqueFormate() {
        return String.format("%.2f (%.0f%% du potentiel)", facteurClimatique, facteurClimatique * 100);
    }

    public String getMargeBruteFormatee() {
        return String.format("%s%.2f DT", margeBrute >= 0 ? "+" : "", margeBrute);
    }

    public String getScoreRoiFormate() {
        return String.format("%s%.1f %%", scoreRoi >= 0 ? "+" : "", scoreRoi);
    }
}
