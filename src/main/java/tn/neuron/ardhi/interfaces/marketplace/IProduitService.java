package tn.neuron.ardhi.interfaces.marketplace;

import tn.neuron.ardhi.models.marketplace.Produit;
import java.util.List;

public interface IProduitService {

    List<Produit> getAllProduits();

    List<Produit> searchProduits(String searchTerm);

    Produit getProduitById(int idProduit);

    List<Produit> getProduitsByCategorie(String categorie);

    List<String> getAllCategories();

    boolean verifierStock(int idProduit, int quantiteDemandee);

    boolean diminuerStock(int idProduit, int quantite);

    boolean augmenterStock(int idProduit, int quantite);

    boolean ajouterProduit(Produit produit);

    boolean modifierProduit(Produit produit);

    boolean supprimerProduit(int idProduit);

    List<Produit> getProduitsByUser(int idUser);
}