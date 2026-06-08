package tn.neuron.ardhi.models.marketplace;

/**
 * Représente un produit demandé par l'utilisateur dans le chatbot.
 */
public class ProduitDemande {

    private String nom;
    private int quantite;

    public ProduitDemande() {
    }

    public ProduitDemande(String nom, int quantite) {
        this.nom = nom;
        this.quantite = quantite;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    @Override
    public String toString() {
        return "ProduitDemande{nom='" + nom + "', quantite=" + quantite + "}";
    }
}
