package tn.neuron.ardhi.models.marketplace;

import java.time.LocalDateTime;

/**
 * Représente un produit sauvegardé en favori par un utilisateur.
 */
public class Wishlist {

    private int idWishlist;
    private int idUser;
    private int idProduit;
    private LocalDateTime dateAjout;

    /** Produit résolu (chargé par jointure) — non persisté directement */
    private Produit produit;

    public Wishlist() {
    }

    public Wishlist(int idUser, int idProduit) {
        this.idUser = idUser;
        this.idProduit = idProduit;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public int getIdWishlist() {
        return idWishlist;
    }

    public void setIdWishlist(int idWishlist) {
        this.idWishlist = idWishlist;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public int getIdProduit() {
        return idProduit;
    }

    public void setIdProduit(int idProduit) {
        this.idProduit = idProduit;
    }

    public LocalDateTime getDateAjout() {
        return dateAjout;
    }

    public void setDateAjout(LocalDateTime dateAjout) {
        this.dateAjout = dateAjout;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    @Override
    public String toString() {
        return "Wishlist{idUser=" + idUser + ", idProduit=" + idProduit + ", dateAjout=" + dateAjout + "}";
    }
}
