package tn.neuron.ardhi.models.marketplace;

import java.time.LocalDateTime;

public class Reclamation {





    private int idReclamation;
    private String description;
    private TypeReclamation type;
    private StatutReclamation statut;
    private LocalDateTime dateReclamation;
    private String nomProduit;
    private int idProduit;
    private int idUser;

    // Constructeurs
    public Reclamation() {
        this.statut = StatutReclamation.EN_ATTENTE;
        this.dateReclamation = LocalDateTime.now();
    }

    public Reclamation(String description, TypeReclamation type, String nomProduit,
                       int idProduit, int idUser) {
        this.description = description;
        this.type = type;
        this.nomProduit = nomProduit;
        this.idProduit = idProduit;
        this.idUser = idUser;
        this.statut = StatutReclamation.EN_ATTENTE;
        this.dateReclamation = LocalDateTime.now();
    }

    // Getters et Setters
    public int getIdReclamation() { return idReclamation; }
    public void setIdReclamation(int idReclamation) { this.idReclamation = idReclamation; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TypeReclamation getType() { return type; }
    public void setType(TypeReclamation type) { this.type = type; }

    public StatutReclamation getStatut() { return statut; }
    public void setStatut(StatutReclamation statut) { this.statut = statut; }

    public LocalDateTime getDateReclamation() { return dateReclamation; }
    public void setDateReclamation(LocalDateTime dateReclamation) {
        this.dateReclamation = dateReclamation;
    }

    public String getNomProduit() { return nomProduit; }
    public void setNomProduit(String nomProduit) { this.nomProduit = nomProduit; }

    public int getIdProduit() { return idProduit; }
    public void setIdProduit(int idProduit) { this.idProduit = idProduit; }

    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }

    // Méthode utilitaire pour affichage convivial du type
    public String getTypeLibelle() {
        switch (type) {
            case QUALITE_RECOLTE: return "Qualité de récolte";
            case PRODUIT_AVARIE: return "Produit avarié";
            case QUANTITE_INCORRECTE: return "Quantité incorrecte";
            case PRIX_NON_CONFORME: return "Prix non conforme";
            case PRODUIT_NON_CONFORME: return "Produit non conforme";
            case RETARD_LIVRAISON: return "Retard de livraison";
            case AUTRE: return "Autre";
            default: return type.toString();
        }
    }

    @Override
    public String toString() {
        return "Reclamation{" +
                "idReclamation=" + idReclamation +
                ", type=" + type +
                ", statut=" + statut +
                ", nomProduit='" + nomProduit + '\'' +
                '}';
    }
}