package tn.neuron.ardhi.models.marketplace;

import java.util.List;

/**
 * Représente l'intention analysée par l'IA (Ollama).
 *
 * v2 : ajout des champs de filtrage + texteReponse
 *      (évite de créer une classe ChatbotResponse séparée)
 */
public class ChatIntent {

    /** "achat", "disponibilite", "vider_panier", "supprimer_produit", "filtrer", "hors_sujet" */
    private String intention;

    /** Liste des produits demandés avec leurs quantités */
    private List<ProduitDemande> produits;

    /** "prix_asc", "prix_desc", "avis", ou null */
    private String critere;

    // ── Champs de filtrage catalogue ──────────────────────────────────────────
    private String recherche;
    private String categorie;
    private Double prixMin;
    private Double prixMax;

    // ── Texte de réponse à afficher dans le chat ──────────────────────────────
    // Rempli par ChatbotService, lu par CatalogueProduitController
    private String texteReponse;

    public ChatIntent() {}

    public ChatIntent(String intention, List<ProduitDemande> produits, String critere) {
        this.intention = intention;
        this.produits = produits;
        this.critere = critere;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getIntention() { return intention; }
    public void setIntention(String intention) { this.intention = intention; }

    public List<ProduitDemande> getProduits() { return produits; }
    public void setProduits(List<ProduitDemande> produits) { this.produits = produits; }

    public String getCritere() { return critere; }
    public void setCritere(String critere) { this.critere = critere; }

    public String getRecherche() { return recherche; }
    public void setRecherche(String recherche) { this.recherche = recherche; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public Double getPrixMin() { return prixMin; }
    public void setPrixMin(Double prixMin) { this.prixMin = prixMin; }

    public Double getPrixMax() { return prixMax; }
    public void setPrixMax(Double prixMax) { this.prixMax = prixMax; }

    public String getTexteReponse() { return texteReponse; }
    public void setTexteReponse(String texteReponse) { this.texteReponse = texteReponse; }

    @Override
    public String toString() {
        return "ChatIntent{intention='" + intention + "', produits=" + produits +
                ", critere='" + critere + "', recherche='" + recherche +
                "', categorie='" + categorie + "', prixMin=" + prixMin +
                ", prixMax=" + prixMax + "}";
    }
}