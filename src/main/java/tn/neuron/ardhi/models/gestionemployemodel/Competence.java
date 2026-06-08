package tn.neuron.ardhi.models.gestionemployemodel;

import java.time.LocalDateTime;

/**
 * Modèle représentant une compétence dans le système
 */
public class Competence {

    private int idCompetence;
    private String nom;
    private String categorie;
    private String description;
    private LocalDateTime dateCreation;

    // Constructeurs
    public Competence() {}

    public Competence(int idCompetence, String nom, String categorie) {
        this.idCompetence = idCompetence;
        this.nom = nom;
        this.categorie = categorie;
    }

    public Competence(String nom, String categorie, String description) {
        this.nom = nom;
        this.categorie = categorie;
        this.description = description;
    }

    // Getters et Setters
    public int getIdCompetence() {
        return idCompetence;
    }

    public void setIdCompetence(int idCompetence) {
        this.idCompetence = idCompetence;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    @Override
    public String toString() {
        return nom + " (" + categorie + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Competence that = (Competence) obj;
        return idCompetence == that.idCompetence;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(idCompetence);
    }
}