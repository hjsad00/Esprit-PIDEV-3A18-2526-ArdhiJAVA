package tn.neuron.ardhi.models.Parcelle_Cultures;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Modèle Culture enrichi avec surface_utilisee et rendement_estime
 * pour le système d'aide à la décision agricole Ardhi Premium.
 */
public class Culture {
    private int id;
    private String nomCulture;
    private String typeCulture;
    private String saison;
    private Date datePlantation;
    private Date dateRecoltePrevue;
    private String etatCulture; // en_croissance, récoltée
    private int parcelleId;

    // ---- NOUVEAU : Surface utilisée par cette culture ----
    private double surfaceUtilisee; // en hectares

    // ---- NOUVEAU : Rendement estimé ----
    private double rendementEstime; // en tonnes/hectare

    // Champs temporaires pour l'affichage (remplis par jointure SQL)
    private String nomParcelleTemp;
    private String nomAgriculteurTemp;

    // ==================== CONSTRUCTEURS ====================

    public Culture() {}

    /** Constructeur sans ID (pour insertion) */
    public Culture(String nomCulture, String typeCulture, String saison,
                   Date datePlantation, Date dateRecoltePrevue,
                   String etatCulture, int parcelleId) {
        this.nomCulture = nomCulture;
        this.typeCulture = typeCulture;
        this.saison = saison;
        this.datePlantation = datePlantation;
        this.dateRecoltePrevue = dateRecoltePrevue;
        this.etatCulture = etatCulture;
        this.parcelleId = parcelleId;
    }

    /** Constructeur complet */
    public Culture(int id, String nomCulture, String typeCulture, String saison,
                   Date datePlantation, Date dateRecoltePrevue,
                   String etatCulture, int parcelleId) {
        this.id = id;
        this.nomCulture = nomCulture;
        this.typeCulture = typeCulture;
        this.saison = saison;
        this.datePlantation = datePlantation;
        this.dateRecoltePrevue = dateRecoltePrevue;
        this.etatCulture = etatCulture;
        this.parcelleId = parcelleId;
    }

    /** Constructeur complet avec surface et rendement */
    public Culture(int id, String nomCulture, String typeCulture, String saison,
                   Date datePlantation, Date dateRecoltePrevue,
                   String etatCulture, int parcelleId,
                   double surfaceUtilisee, double rendementEstime) {
        this(id, nomCulture, typeCulture, saison, datePlantation, dateRecoltePrevue, etatCulture, parcelleId);
        this.surfaceUtilisee = surfaceUtilisee;
        this.rendementEstime = rendementEstime;
    }

    // ==================== GETTERS / SETTERS ====================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNomCulture() { return nomCulture; }
    public void setNomCulture(String nomCulture) { this.nomCulture = nomCulture; }

    public String getTypeCulture() { return typeCulture; }
    public void setTypeCulture(String typeCulture) { this.typeCulture = typeCulture; }

    public String getSaison() { return saison; }
    public void setSaison(String saison) { this.saison = saison; }

    public String getEtatCulture() { return etatCulture; }
    public void setEtatCulture(String etatCulture) { this.etatCulture = etatCulture; }

    public Date getDatePlantation() { return datePlantation; }
    public void setDatePlantation(Date datePlantation) { this.datePlantation = datePlantation; }

    public Date getDateRecoltePrevue() { return dateRecoltePrevue; }
    public void setDateRecoltePrevue(Date dateRecoltePrevue) { this.dateRecoltePrevue = dateRecoltePrevue; }

    public int getParcelleId() { return parcelleId; }
    public void setParcelleId(int parcelleId) { this.parcelleId = parcelleId; }

    // ---- Surface & Rendement ----
    public double getSurfaceUtilisee() { return surfaceUtilisee; }
    public void setSurfaceUtilisee(double surfaceUtilisee) { this.surfaceUtilisee = surfaceUtilisee; }

    public double getRendementEstime() { return rendementEstime; }
    public void setRendementEstime(double rendementEstime) { this.rendementEstime = rendementEstime; }

    // ---- Jointure ----
    public String getNomParcelleTemp() { return nomParcelleTemp; }
    public void setNomParcelleTemp(String nomParcelleTemp) { this.nomParcelleTemp = nomParcelleTemp; }

    public String getNomAgriculteurTemp() { return nomAgriculteurTemp; }
    public void setNomAgriculteurTemp(String nomAgriculteurTemp) { this.nomAgriculteurTemp = nomAgriculteurTemp; }

    // ==================== MÉTIER ====================

    /**
     * Valide la contrainte temporelle métier:
     * date_plantation < date_recolte_prevue
     */
    public boolean isValidDates() {
        if (datePlantation == null || dateRecoltePrevue == null) return false;
        return datePlantation.before(dateRecoltePrevue);
    }

    /**
     * Calcule la durée de culture en jours
     */
    public long getDureeCultureJours() {
        if (datePlantation == null || dateRecoltePrevue == null) return 0;
        return ChronoUnit.DAYS.between(datePlantation.toLocalDate(), dateRecoltePrevue.toLocalDate());
    }

    /**
     * Vérifie si la récolte est proche (< 7 jours)
     */
    public boolean isRecolteProche() {
        if (dateRecoltePrevue == null) return false;
        LocalDate today = LocalDate.now();
        LocalDate recolte = dateRecoltePrevue.toLocalDate();
        long jours = ChronoUnit.DAYS.between(today, recolte);
        return jours >= 0 && jours <= 7;
    }

    /**
     * Calcule la production estimée = surface × rendement (tonnes)
     */
    public double getProductionEstimee() {
        return surfaceUtilisee * rendementEstime;
    }

    @Override
    public String toString() {
        return nomCulture + " (" + typeCulture + ") - " + saison;
    }
}
