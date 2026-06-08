package tn.neuron.ardhi.interfaces.marketplace;

import tn.neuron.ardhi.models.marketplace.Produit;

import java.util.List;

/**
 * Interface du service de liste de souhaits (wishlist).
 */
public interface IWishlistService {

    /**
     * Ajoute un produit aux favoris d'un utilisateur. Retourne false si déjà
     * présent.
     */
    boolean ajouterFavori(int idUser, int idProduit);

    /**
     * Supprime un produit des favoris d'un utilisateur. Retourne false si
     * inexistant.
     */
    boolean supprimerFavori(int idUser, int idProduit);

    /** Retourne true si le produit est déjà dans les favoris de l'utilisateur. */
    boolean estFavori(int idUser, int idProduit);

    /** Retourne la liste de produits mis en favoris par l'utilisateur. */
    List<Produit> getFavoris(int idUser);
}
