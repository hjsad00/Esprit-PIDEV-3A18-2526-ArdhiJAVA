package tn.neuron.ardhi.models.marketplace;

import java.sql.Date;

public class Coupon {

    private int idCoupon;
    private String code;
    private TypeReduction typeReduction;
    private double valeur;
    private Date dateDebut;
    private Date dateFin;
    private int utilisationMax;
    private int utilisationActuelle;
    private boolean actif;
    private double montantMin;
    private int limiteParUser;

    public Coupon(int idCoupon, String code, TypeReduction typeReduction, double valeur, Date dateDebut, Date dateFin, int utilisationMax, int utilisationActuelle, boolean actif, double montantMin, int limiteParUser) {
        this.idCoupon = idCoupon;
        this.code = code;
        this.typeReduction = typeReduction;
        this.valeur = valeur;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.utilisationMax = utilisationMax;
        this.utilisationActuelle = utilisationActuelle;
        this.actif = actif;
        this.montantMin = montantMin;
        this.limiteParUser = limiteParUser;
    }

    public Coupon() {}

    public int getIdCoupon() {
        return idCoupon;
    }

    public void setIdCoupon(int idCoupon) {
        this.idCoupon = idCoupon;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public TypeReduction getTypeReduction() {
        return typeReduction;
    }

    public void setTypeReduction(TypeReduction typeReduction) {
        this.typeReduction = typeReduction;
    }

    public double getValeur() {
        return valeur;
    }

    public void setValeur(double valeur) {
        this.valeur = valeur;
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

    public int getUtilisationMax() {
        return utilisationMax;
    }

    public void setUtilisationMax(int utilisationMax) {
        this.utilisationMax = utilisationMax;
    }

    public int getUtilisationActuelle() {
        return utilisationActuelle;
    }

    public void setUtilisationActuelle(int utilisationActuelle) {
        this.utilisationActuelle = utilisationActuelle;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public double getMontantMin() {
        return montantMin;
    }

    public void setMontantMin(double montantMin) {
        this.montantMin = montantMin;
    }

    public int getLimiteParUser() {
        return limiteParUser;
    }

    public void setLimiteParUser(int limiteParUser) {
        this.limiteParUser = limiteParUser;
    }

    @Override
    public String toString() {
        return "Coupon{" +
                "idCoupon=" + idCoupon +
                ", code='" + code + '\'' +
                ", typeReduction=" + typeReduction +
                ", valeur=" + valeur +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", utilisationMax=" + utilisationMax +
                ", utilisationActuelle=" + utilisationActuelle +
                ", actif=" + actif +
                ", montantMin=" + montantMin +
                ", limiteParUser=" + limiteParUser +
                '}';
    }
}