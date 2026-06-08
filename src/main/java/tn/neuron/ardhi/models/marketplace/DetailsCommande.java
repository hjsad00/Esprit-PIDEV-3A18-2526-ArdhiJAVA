package tn.neuron.ardhi.models.marketplace;

public class DetailsCommande {

    private int idDetails;
    private int idCommande;
    private int idProduit;

    private int quantite;
    private float prixUnitaire;

    //  Constructeurs
    public DetailsCommande() {}

    public DetailsCommande(int idDetails, int idCommande, int idProduit,
                           int quantite, float prixUnitaire) {
        this.idDetails = idDetails;
        this.idCommande = idCommande;
        this.idProduit = idProduit;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
    }

    // Getters & Setters
    public int getIdDetails() {
        return idDetails;
    }

    public void setIdDetails(int idDetails) {
        this.idDetails = idDetails;
    }

    public int getIdCommande() {
        return idCommande;
    }

    public void setIdCommande(int idCommande) {
        this.idCommande = idCommande;
    }

    public int getIdProduit() {
        return idProduit;
    }

    public void setIdProduit(int idProduit) {
        this.idProduit = idProduit;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public float getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(float prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    @Override
    public String toString() {
        return "DetailsCommande{" +
                "idDetails=" + idDetails +
                ", idCommande=" + idCommande +
                ", idProduit=" + idProduit +
                ", quantite=" + quantite +
                ", prixUnitaire=" + prixUnitaire +
                '}';
    }
}
