package tn.neuron.ardhi.models.gestionemployemodel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class Tache {

    private int id;
    private String titre;
    private String description;
    private String statut;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Integer idEmploye;      // Peut être null
    private Integer idAgriculteur;

    // 🆕 NOUVEAUX CHAMPS
    private Integer priorite;       // 1:Basse, 2:Moyenne, 3:Haute, 4:Critique
    private String categorie;       // Plantation, Récolte, Irrigation, etc.
    private LocalDateTime dateModification;  // Date dernière modification
    private String googleEventId;            // ID événement Google Calendar (null = non sync)

    // ── Constructeurs ──────────────────────────────────────────────────────

    // Constructeur vide (NÉCESSAIRE pour JavaFX)
    public Tache() {
    }

    // Constructeur complet avec ID
    public Tache(int id, String titre, String description, String statut,
                 LocalDate dateDebut, LocalDate dateFin, Integer idEmploye,
                 Integer idAgriculteur,
                 Integer priorite, String categorie) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.statut = statut;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.idEmploye = idEmploye;
        this.idAgriculteur = idAgriculteur;
        this.priorite = priorite;
        this.categorie = categorie;
    }

    // Constructeur sans ID (pour création)
    public Tache(String titre, String description, String statut,
                 LocalDate dateDebut, LocalDate dateFin, Integer idEmploye,
                 Integer idAgriculteur,
                 Integer priorite, String categorie) {
        this.titre = titre;
        this.description = description;
        this.statut = statut;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.idEmploye = idEmploye;
        this.idAgriculteur = idAgriculteur;
        this.priorite = priorite;
        this.categorie = categorie;
    }

    // ── Getters / Setters ────────────────────────────────────────────────
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public Integer getIdEmploye() { return idEmploye; }
    public void setIdEmploye(Integer idEmploye) { this.idEmploye = idEmploye; }

    public Integer getIdAgriculteur() { return idAgriculteur; }
    public void setIdAgriculteur(Integer idAgriculteur) { this.idAgriculteur = idAgriculteur; }

    public Integer getPriorite() { return priorite; }
    public void setPriorite(Integer priorite) { this.priorite = priorite; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    // ── Google Calendar / Météo ──────────────────────────────────────
    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime d) { this.dateModification = d; }

    public String getGoogleEventId() { return googleEventId; }
    public void setGoogleEventId(String id) { this.googleEventId = id; }



    // ── TypeTache : enum interne déduit depuis la catégorie ───────────
    /**
     * Types de tâches agricoles utilisés par WeatherService
     * pour adapter les recommandations météo.
     */
    public enum TypeTache {
        TRAITEMENT,   // Traitements phytosanitaires, pesticides
        IRRIGATION,   // Arrosage, irrigation
        RECOLTE,      // Récolte
        PLANTATION,   // Plantation, semis
        LABOUR,       // Labour, travail du sol
        MAINTENANCE,  // Entretien matériel / infrastructure
        AUTRE         // Tout le reste
    }

    /**
     * Déduit le TypeTache depuis le champ categorie (insensible à la casse).
     * Retourne TypeTache.AUTRE si la catégorie est inconnue ou null.
     */
    public TypeTache getTypeTache() {
        if (categorie == null) return TypeTache.AUTRE;
        switch (categorie.trim().toLowerCase()) {
            case "traitement":
            case "traitement phytosanitaire":
            case "pesticide":        return TypeTache.TRAITEMENT;

            case "irrigation":
            case "arrosage":         return TypeTache.IRRIGATION;

            case "recolte":
            case "récolte":          return TypeTache.RECOLTE;

            case "plantation":
            case "semis":            return TypeTache.PLANTATION;

            case "labour":
            case "travail du sol":   return TypeTache.LABOUR;

            case "maintenance":
            case "entretien":        return TypeTache.MAINTENANCE;

            default:                 return TypeTache.AUTRE;
        }
    }

    /**
     * Définit le TypeTache en mettant à jour le champ categorie correspondant.
     * Utilisé principalement pour les tests unitaires.
     */
    public void setTypeTache(TypeTache typeTache) {
        if (typeTache == null) { this.categorie = null; return; }
        switch (typeTache) {
            case TRAITEMENT:  this.categorie = "traitement";  break;
            case IRRIGATION:  this.categorie = "irrigation";  break;
            case RECOLTE:     this.categorie = "récolte";     break;
            case PLANTATION:  this.categorie = "plantation";  break;
            case LABOUR:      this.categorie = "labour";      break;
            case MAINTENANCE: this.categorie = "maintenance"; break;
            default:          this.categorie = "autre";       break;
        }
    }

    @Override
    public String toString() {
        return "Tache{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", statut='" + statut + '\'' +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", idEmploye=" + idEmploye +
                ", idAgriculteur=" + idAgriculteur +
                ", priorite=" + priorite +
                ", categorie='" + categorie + '\'' +
                '}';
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tache tache = (Tache) o;
        return id == tache.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}