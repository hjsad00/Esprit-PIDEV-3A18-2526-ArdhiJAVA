package tn.neuron.ardhi.models.marketplace;

import java.time.LocalDate;

public class Panier {
    private int idPanier;
    private LocalDate dateCreation;
    private float totalMontant;
    private int totalProduits;
    private int idUser;

    // Constructeurs
    public Panier() {}

    public Panier(int idPanier, LocalDate dateCreation, float totalMontant,
                  int totalProduits, int idUser) {
        this.idPanier = idPanier;
        this.dateCreation = dateCreation;
        this.totalMontant = totalMontant;
        this.totalProduits = totalProduits;
        this.idUser = idUser;
    }

    // Getters et Setters
    public int getIdPanier() { return idPanier; }
    public void setIdPanier(int idPanier) { this.idPanier = idPanier; }

    public LocalDate getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDate dateCreation) { this.dateCreation = dateCreation; }

    public float getTotalMontant() { return totalMontant; }
    public void setTotalMontant(float totalMontant) { this.totalMontant = totalMontant; }

    public int getTotalProduits() { return totalProduits; }
    public void setTotalProduits(int totalProduits) { this.totalProduits = totalProduits; }

    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }

    @Override
    public String toString() {
        return "Panier{" +
                "idPanier=" + idPanier +
                ", totalMontant=" + totalMontant +
                ", totalProduits=" + totalProduits +
                '}';
    }
}