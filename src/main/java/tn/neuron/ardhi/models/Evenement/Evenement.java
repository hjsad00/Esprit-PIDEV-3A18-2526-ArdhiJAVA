package tn.neuron.ardhi.models.Evenement;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Evenement {
    private int id;
    private String titre;
    private String description;
    private String lieu;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String type; // "FOIRE", "FORMATION", "CONFERENCE", "ATELIER"
    private int nombrePlacesMax;
    private int nombreParticipants;
    private String organisateur;
    private String imageUrl;
    private String statut; // "A_VENIR", "EN_COURS", "TERMINE", "ANNULE"
    private LocalDateTime dateCreation;
    private int idCreateur; // L'utilisateur qui a créé l'événement

    // Constructeur vide
    public Evenement() {
        this.dateCreation = LocalDateTime.now();
        this.nombreParticipants = 0;
        this.statut = "A_VENIR";
    }

    // Constructeur complet
    public Evenement(int id, String titre, String description, String lieu,
                     LocalDate dateDebut, LocalDate dateFin, String type,
                     int nombrePlacesMax, String organisateur, String imageUrl,
                     String statut, int idCreateur) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.lieu = lieu;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.type = type;
        this.nombrePlacesMax = nombrePlacesMax;
        this.organisateur = organisateur;
        this.imageUrl = imageUrl;
        this.statut = statut;
        this.dateCreation = LocalDateTime.now();
        this.idCreateur = idCreateur;
        this.nombreParticipants = 0;
    }

    // Constructeur sans ID (pour insertion)
    public Evenement(String titre, String description, String lieu,
                     LocalDate dateDebut, LocalDate dateFin, String type,
                     int nombrePlacesMax, String organisateur, String imageUrl, int idCreateur) {
        this.titre = titre;
        this.description = description;
        this.lieu = lieu;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.type = type;
        this.nombrePlacesMax = nombrePlacesMax;
        this.organisateur = organisateur;
        this.imageUrl = imageUrl;
        this.dateCreation = LocalDateTime.now();
        this.statut = "A_VENIR";
        this.nombreParticipants = 0;
        this.idCreateur = idCreateur;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getNombrePlacesMax() {
        return nombrePlacesMax;
    }

    public void setNombrePlacesMax(int nombrePlacesMax) {
        this.nombrePlacesMax = nombrePlacesMax;
    }

    public int getNombreParticipants() {
        return nombreParticipants;
    }

    public void setNombreParticipants(int nombreParticipants) {
        this.nombreParticipants = nombreParticipants;
    }

    public String getOrganisateur() {
        return organisateur;
    }

    public void setOrganisateur(String organisateur) {
        this.organisateur = organisateur;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public int getIdCreateur() {
        return idCreateur;
    }

    public void setIdCreateur(int idCreateur) {
        this.idCreateur = idCreateur;
    }

    // Méthodes utilitaires
    public boolean isComplet() {
        return nombreParticipants >= nombrePlacesMax;
    }

    public int getPlacesRestantes() {
        return nombrePlacesMax - nombreParticipants;
    }

    public boolean isActive() {
        return "A_VENIR".equals(statut) || "EN_COURS".equals(statut);
    }

    @Override
    public String toString() {
        return "Evenement{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", lieu='" + lieu + '\'' +
                ", dateDebut=" + dateDebut +
                ", type='" + type + '\'' +
                ", statut='" + statut + '\'' +
                '}';
    }
}