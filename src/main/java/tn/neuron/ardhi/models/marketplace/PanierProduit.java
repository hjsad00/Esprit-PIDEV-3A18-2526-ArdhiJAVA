package tn.neuron.ardhi.models.marketplace;

public class PanierProduit {
    private int idPanier;
    private int idProduit;
    private int quantite;

    private Produit produit;

    // Constructeurs
    public PanierProduit() {}

    public PanierProduit(int idPanier, int idProduit, int quantite) {
        this.idPanier = idPanier;
        this.idProduit = idProduit;
        this.quantite = quantite;
    }

    public float getSousTotal() {
        if (produit != null) {
            return quantite * produit.getPrix();
        }
        return 0;
    }

    // Getters et Setters
    public int getIdPanier() { return idPanier; }
    public void setIdPanier(int idPanier) { this.idPanier = idPanier; }

    public int getIdProduit() { return idProduit; }
    public void setIdProduit(int idProduit) { this.idProduit = idProduit; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }

    @Override
    public String toString() {
        return "PanierProduit{" +
                "idPanier=" + idPanier +
                ", idProduit=" + idProduit +
                ", quantite=" + quantite +
                '}';
    }
}