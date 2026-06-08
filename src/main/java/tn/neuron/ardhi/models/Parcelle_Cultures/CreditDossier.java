package tn.neuron.ardhi.models.Parcelle_Cultures;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Modèle du Dossier de Crédit Agricole — regroupe toutes les données nécessaires
 * pour générer un PDF bancaire professionnel.
 *
 * Architecture : CreditDossierController → CreditAnalysisService → CreditDossier → PdfCreditExportService
 */
public class CreditDossier {

    // ==================== EXPLOITANT ====================
    private String nomExploitant;
    private String prenomExploitant;
    private String emailExploitant;
    private int    idExploitant;

    // ==================== PARCELLE ====================
    private int    parcelleId;
    private double surface;            // ha
    private String localisation;
    private String typeSol;
    private String systemeIrrigation;
    private String statutParcelle;
    private Double latitude;
    private Double longitude;

    // ==================== CULTURES ====================
    private List<CultureInfo> cultures = new ArrayList<>();

    // ==================== ANALYSE FINANCIÈRE ====================
    private double coutsTotaux;         // DT
    private double chiffreAffaires;     // DT
    private double margeBrute;          // DT
    private double roi;                 // %
    private double facteurClimatique;

    // ==================== CAPACITÉ DE REMBOURSEMENT ====================
    private double capaciteRemboursement; // DT/an
    private int    dureeEmpruntAnnees;
    private double montantPretMax;        // DT

    // ==================== SCORE DE RISQUE ====================
    private double scoreRisque;           // 0..10
    private String niveauRisque;          // Faible / Modéré / Élevé
    private double scoreRentabilite;
    private double scoreStabiliteClimat;
    private double scoreDiversification;
    private double scoreHistorique;

    // ==================== META ====================
    private LocalDate dateGeneration;
    private String    langue;             // "fr" | "ar" | "en"

    public CreditDossier() {
        this.dateGeneration = LocalDate.now();
        this.dureeEmpruntAnnees = 5;
        this.langue = "fr";
    }

    // ==================== INNER CLASS ====================
    public static class CultureInfo {
        private String nom;
        private String type;
        private String saison;
        private String etat;
        private double surfaceUtilisee;  // ha
        private double rendement;        // t/ha
        private double productionEstimee; // t

        public CultureInfo() {}

        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getSaison() { return saison; }
        public void setSaison(String saison) { this.saison = saison; }
        public String getEtat() { return etat; }
        public void setEtat(String etat) { this.etat = etat; }
        public double getSurfaceUtilisee() { return surfaceUtilisee; }
        public void setSurfaceUtilisee(double surfaceUtilisee) { this.surfaceUtilisee = surfaceUtilisee; }
        public double getRendement() { return rendement; }
        public void setRendement(double rendement) { this.rendement = rendement; }
        public double getProductionEstimee() { return productionEstimee; }
        public void setProductionEstimee(double productionEstimee) { this.productionEstimee = productionEstimee; }
    }

    // ==================== GETTERS / SETTERS ====================

    public String getNomExploitant() { return nomExploitant; }
    public void setNomExploitant(String v) { this.nomExploitant = v; }

    public String getPrenomExploitant() { return prenomExploitant; }
    public void setPrenomExploitant(String v) { this.prenomExploitant = v; }

    public String getEmailExploitant() { return emailExploitant; }
    public void setEmailExploitant(String v) { this.emailExploitant = v; }

    public int getIdExploitant() { return idExploitant; }
    public void setIdExploitant(int v) { this.idExploitant = v; }

    public int getParcelleId() { return parcelleId; }
    public void setParcelleId(int v) { this.parcelleId = v; }

    public double getSurface() { return surface; }
    public void setSurface(double v) { this.surface = v; }

    public String getLocalisation() { return localisation; }
    public void setLocalisation(String v) { this.localisation = v; }

    public String getTypeSol() { return typeSol; }
    public void setTypeSol(String v) { this.typeSol = v; }

    public String getSystemeIrrigation() { return systemeIrrigation; }
    public void setSystemeIrrigation(String v) { this.systemeIrrigation = v; }

    public String getStatutParcelle() { return statutParcelle; }
    public void setStatutParcelle(String v) { this.statutParcelle = v; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double v) { this.latitude = v; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double v) { this.longitude = v; }

    public List<CultureInfo> getCultures() { return cultures; }
    public void setCultures(List<CultureInfo> v) { this.cultures = v; }

    public double getCoutsTotaux() { return coutsTotaux; }
    public void setCoutsTotaux(double v) { this.coutsTotaux = v; }

    public double getChiffreAffaires() { return chiffreAffaires; }
    public void setChiffreAffaires(double v) { this.chiffreAffaires = v; }

    public double getMargeBrute() { return margeBrute; }
    public void setMargeBrute(double v) { this.margeBrute = v; }

    public double getRoi() { return roi; }
    public void setRoi(double v) { this.roi = v; }

    public double getFacteurClimatique() { return facteurClimatique; }
    public void setFacteurClimatique(double v) { this.facteurClimatique = v; }

    public double getCapaciteRemboursement() { return capaciteRemboursement; }
    public void setCapaciteRemboursement(double v) { this.capaciteRemboursement = v; }

    public int getDureeEmpruntAnnees() { return dureeEmpruntAnnees; }
    public void setDureeEmpruntAnnees(int v) { this.dureeEmpruntAnnees = v; }

    public double getMontantPretMax() { return montantPretMax; }
    public void setMontantPretMax(double v) { this.montantPretMax = v; }

    public double getScoreRisque() { return scoreRisque; }
    public void setScoreRisque(double v) { this.scoreRisque = v; }

    public String getNiveauRisque() { return niveauRisque; }
    public void setNiveauRisque(String v) { this.niveauRisque = v; }

    public double getScoreRentabilite() { return scoreRentabilite; }
    public void setScoreRentabilite(double v) { this.scoreRentabilite = v; }

    public double getScoreStabiliteClimat() { return scoreStabiliteClimat; }
    public void setScoreStabiliteClimat(double v) { this.scoreStabiliteClimat = v; }

    public double getScoreDiversification() { return scoreDiversification; }
    public void setScoreDiversification(double v) { this.scoreDiversification = v; }

    public double getScoreHistorique() { return scoreHistorique; }
    public void setScoreHistorique(double v) { this.scoreHistorique = v; }

    public LocalDate getDateGeneration() { return dateGeneration; }
    public void setDateGeneration(LocalDate v) { this.dateGeneration = v; }

    public String getLangue() { return langue; }
    public void setLangue(String v) { this.langue = v; }

    /** Nom complet de l'exploitant */
    public String getNomComplet() {
        return (prenomExploitant != null ? prenomExploitant : "") + " "
             + (nomExploitant != null ? nomExploitant : "");
    }

    /** Production totale estimée (somme de toutes les cultures) */
    public double getProductionTotale() {
        return cultures.stream().mapToDouble(CultureInfo::getProductionEstimee).sum();
    }
}
