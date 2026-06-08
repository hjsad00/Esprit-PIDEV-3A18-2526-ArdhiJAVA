package tn.neuron.ardhi.models.Evenement;

import java.time.LocalDateTime;

/**
 * Modèle représentant un favori d'événement
 */
public class EvenementFavori {

    private int id;
    private int idEvenement;
    private int idUtilisateur;
    private LocalDateTime dateAjout;

    // Informations supplémentaires (jointure)
    private String titreEvenement;
    private String typeEvenement;
    private String nomUtilisateur;
    private String prenomUtilisateur;

    // ==================== CONSTRUCTEURS ====================

    public EvenementFavori() {
    }

    public EvenementFavori(int idEvenement, int idUtilisateur) {
        this.idEvenement = idEvenement;
        this.idUtilisateur = idUtilisateur;
        this.dateAjout = LocalDateTime.now();
    }

    public EvenementFavori(int id, int idEvenement, int idUtilisateur, LocalDateTime dateAjout) {
        this.id = id;
        this.idEvenement = idEvenement;
        this.idUtilisateur = idUtilisateur;
        this.dateAjout = dateAjout;
    }

    // ==================== GETTERS & SETTERS ====================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdEvenement() {
        return idEvenement;
    }

    public void setIdEvenement(int idEvenement) {
        this.idEvenement = idEvenement;
    }

    public int getIdUtilisateur() {
        return idUtilisateur;
    }

    public void setIdUtilisateur(int idUtilisateur) {
        this.idUtilisateur = idUtilisateur;
    }

    public LocalDateTime getDateAjout() {
        return dateAjout;
    }

    public void setDateAjout(LocalDateTime dateAjout) {
        this.dateAjout = dateAjout;
    }

    public String getTitreEvenement() {
        return titreEvenement;
    }

    public void setTitreEvenement(String titreEvenement) {
        this.titreEvenement = titreEvenement;
    }

    public String getTypeEvenement() {
        return typeEvenement;
    }

    public void setTypeEvenement(String typeEvenement) {
        this.typeEvenement = typeEvenement;
    }

    public String getNomUtilisateur() {
        return nomUtilisateur;
    }

    public void setNomUtilisateur(String nomUtilisateur) {
        this.nomUtilisateur = nomUtilisateur;
    }

    public String getPrenomUtilisateur() {
        return prenomUtilisateur;
    }

    public void setPrenomUtilisateur(String prenomUtilisateur) {
        this.prenomUtilisateur = prenomUtilisateur;
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    @Override
    public String toString() {
        return "EvenementFavori{" +
                "id=" + id +
                ", idEvenement=" + idEvenement +
                ", idUtilisateur=" + idUtilisateur +
                ", dateAjout=" + dateAjout +
                ", titreEvenement='" + titreEvenement + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EvenementFavori that = (EvenementFavori) o;

        if (idEvenement != that.idEvenement) return false;
        return idUtilisateur == that.idUtilisateur;
    }

    @Override
    public int hashCode() {
        int result = idEvenement;
        result = 31 * result + idUtilisateur;
        return result;
    }
}