package tn.neuron.ardhi.models.UserAndDiag;

import java.sql.Date;

public class Abonnement {
    private int id;
    private String type; // Gardé pour compatibilité, synchronisé avec offre.nom
    private float prix;
    private Date dateDebut;
    private Date dateFin;
    private String statut;
    private int userId;

    // Nouveau: lien vers l'offre
    private int offreId;
    private Offre offre; // Objet Offre complet (peuplé via JOIN si nécessaire)

    // 1. Constructeur vide
    public Abonnement() {
    }

    // 2. Constructeur sans ID (pour l'ajout) - ANCIEN (compatibilité)
    public Abonnement(String type, float prix, Date dateDebut, Date dateFin, String statut, int userId) {
        this.type = type;
        this.prix = prix;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.statut = statut;
        this.userId = userId;
    }

    // 2b. Constructeur sans ID avec offreId (NOUVEAU - recommandé)
    public Abonnement(int offreId, String type, float prix, Date dateDebut, Date dateFin, String statut, int userId) {
        this.offreId = offreId;
        this.type = type;
        this.prix = prix;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.statut = statut;
        this.userId = userId;
    }

    // 3. Constructeur complet avec offreId (NOUVEAU - pour récupération BDD)
    public Abonnement(int id, int offreId, String type, float prix, Date dateDebut, Date dateFin, String statut,
            int userId) {
        this.id = id;
        this.offreId = offreId;
        this.type = type;
        this.prix = prix;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.statut = statut;
        this.userId = userId;
    }

    // Getters & Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public float getPrix() {
        return prix;
    }

    public void setPrix(float prix) {
        this.prix = prix;
    }

    public Date getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(Date dateDebut) {
        this.dateDebut = dateDebut;
    }

    public Date getDateFin() {
        return dateFin;
    }

    public void setDateFin(Date dateFin) {
        this.dateFin = dateFin;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    // Nouveau: getters/setters pour offreId et offre
    public int getOffreId() {
        return offreId;
    }

    public void setOffreId(int offreId) {
        this.offreId = offreId;
    }

    public Offre getOffre() {
        return offre;
    }

    public void setOffre(Offre offre) {
        this.offre = offre;
        if (offre != null) {
            this.offreId = offre.getId();
            this.type = offre.getNom(); // Synchroniser le type avec le nom de l'offre
        }
    }

    /**
     * Calcule le prix total d'un abonnement basé sur le prix mensuel et la durée.
     * 
     * @param prixMensuel Le prix mensuel de l'offre
     * @param nombreMois  Le nombre de mois d'abonnement
     * @return Le prix total
     */
    public static float calculerPrixTotal(float prixMensuel, int nombreMois) {
        return prixMensuel * nombreMois;
    }

    @Override
    public String toString() {
        return "Abonnement{" + "type='" + type + '\'' + ", prix=" + prix + ", statut='" + statut + '\'' + '}';
    }
}