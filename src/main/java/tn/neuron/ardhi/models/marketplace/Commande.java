package tn.neuron.ardhi.models.marketplace;

import java.time.LocalDate;
import java.util.List;

public class Commande {

    private int idCommande;
    private LocalDate dateCommande;
    private EtatCommande etat;
    private float total;
    private int idUser;
    private float fraisLivraison = 0.0f;
    private ModeLivraison modeLivraison = ModeLivraison.RECUPERATION;
    private boolean payeeParPoints = false;

    // Liste des produits dans la commande (panier)
    private List<DetailsCommande> details;

    // Constructeurs
    public Commande() {
    }

    public Commande(int idCommande, LocalDate dateCommande,
                    EtatCommande etat, float total, int idUser) {
        this.idCommande = idCommande;
        this.dateCommande = dateCommande;
        this.etat = etat;
        this.total = total;
        this.idUser = idUser;
    }

    // Getters & Setters
    public int getIdCommande() { return idCommande; }
    public void setIdCommande(int idCommande) { this.idCommande = idCommande; }

    public LocalDate getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDate dateCommande) { this.dateCommande = dateCommande; }

    public EtatCommande getEtat() { return etat; }
    public void setEtat(EtatCommande etat) { this.etat = etat; }

    public float getTotal() { return total; }
    public void setTotal(float total) { this.total = total; }

    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }

    public float getFraisLivraison() { return fraisLivraison; }
    public void setFraisLivraison(float fraisLivraison) { this.fraisLivraison = fraisLivraison; }

    public ModeLivraison getModeLivraison() { return modeLivraison; }
    public void setModeLivraison(ModeLivraison modeLivraison) { this.modeLivraison = modeLivraison; }

    public List<DetailsCommande> getDetails() { return details; }
    public void setDetails(List<DetailsCommande> details) { this.details = details; }

    // ✅ nouveau getter/setter
    public boolean isPayeeParPoints() { return payeeParPoints; }
    public void setPayeeParPoints(boolean payeeParPoints) { this.payeeParPoints = payeeParPoints; }

    @Override
    public String toString() {
        return "Commande{" +
                "idCommande=" + idCommande +
                ", dateCommande=" + dateCommande +
                ", etat=" + etat +
                ", total=" + total +
                ", idUser=" + idUser +
                ", fraisLivraison=" + fraisLivraison +
                ", modeLivraison=" + modeLivraison +
                ", payeeParPoints=" + payeeParPoints + // ✅ nouveau
                '}';
    }
}