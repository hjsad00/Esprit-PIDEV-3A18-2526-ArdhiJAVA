package tn.neuron.ardhi.interfaces.marketplace;

import tn.neuron.ardhi.models.marketplace.Panier;
import tn.neuron.ardhi.models.marketplace.PanierProduit;
import java.util.List;

public interface IPanierService {

    Panier getPanierActif(int idUser);

    boolean ajouterProduit(int idPanier, int idProduit, int quantite);

    boolean updateQuantite(int idPanier, int idProduit, int nouvelleQuantite);

    List<PanierProduit> getProduitsParPanier(int idPanier);

    boolean supprimerProduit(int idPanier, int idProduit);

    boolean viderPanier(int idPanier);

    float calculerTotal(int idPanier);

    int compterProduits(int idPanier);
}