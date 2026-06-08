package tn.neuron.ardhi.models.MaterielEtMaintenance;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Materiel {
    private int id_materiel;
    private String nom;
    private String type;
    private String etat;
    private int user_id;
    private LocalDate date_achat;
    private LocalDate date_prochaine_maintenance;
    private String google_calendar_event_id;
    private LocalDate derniere_maintenance;
    private int frequence_maintenance_mois = 12;

    // ========== CONSTRUCTEURS ==========

    // 1. Constructeur par défaut (vide)
    public Materiel() {}

    // 2. Constructeur pour AdminMaterielController (4 paramètres)
    public Materiel(String nom, String type, String etat, int user_id) {
        this.nom = nom;
        this.type = type;
        this.etat = etat;
        this.user_id = user_id;
        this.date_achat = null;
        this.frequence_maintenance_mois = 12;
    }

    // 3. Constructeur avec date d'achat (5 paramètres)
    public Materiel(String nom, String type, String etat, int user_id, LocalDate date_achat) {
        this.nom = nom;
        this.type = type;
        this.etat = etat;
        this.user_id = user_id;
        this.date_achat = date_achat;
        this.frequence_maintenance_mois = 12;
    }

    // 4. Constructeur complet avec ID (9 paramètres)
    public Materiel(int id_materiel, String nom, String type, String etat, int user_id,
                    LocalDate date_achat, LocalDate date_prochaine_maintenance,
                    String google_calendar_event_id, LocalDate derniere_maintenance,
                    int frequence_maintenance_mois) {
        this.id_materiel = id_materiel;
        this.nom = nom;
        this.type = type;
        this.etat = etat;
        this.user_id = user_id;
        this.date_achat = date_achat;
        this.date_prochaine_maintenance = date_prochaine_maintenance;
        this.google_calendar_event_id = google_calendar_event_id;
        this.derniere_maintenance = derniere_maintenance;
        this.frequence_maintenance_mois = frequence_maintenance_mois;
    }

    // ========== GETTERS & SETTERS ==========
    public int getId_materiel() {
        return id_materiel;
    }

    public void setId_materiel(int id_materiel) {
        this.id_materiel = id_materiel;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEtat() {
        return etat;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public LocalDate getDate_achat() {
        return date_achat;
    }

    public void setDate_achat(LocalDate date_achat) {
        this.date_achat = date_achat;
    }

    public LocalDate getDate_prochaine_maintenance() {
        return date_prochaine_maintenance;
    }

    public void setDate_prochaine_maintenance(LocalDate date_prochaine_maintenance) {
        this.date_prochaine_maintenance = date_prochaine_maintenance;
    }

    public String getGoogle_calendar_event_id() {
        return google_calendar_event_id;
    }

    public void setGoogle_calendar_event_id(String google_calendar_event_id) {
        this.google_calendar_event_id = google_calendar_event_id;
    }

    public LocalDate getDerniere_maintenance() {
        return derniere_maintenance;
    }

    public void setDerniere_maintenance(LocalDate derniere_maintenance) {
        this.derniere_maintenance = derniere_maintenance;
    }

    public int getFrequence_maintenance_mois() {
        return frequence_maintenance_mois;
    }

    public void setFrequence_maintenance_mois(int frequence_maintenance_mois) {
        this.frequence_maintenance_mois = frequence_maintenance_mois;
    }

    // ========== MÉTHODES UTILITAIRES ==========
    public boolean needsMaintenance() {
        if (date_prochaine_maintenance == null) {
            return false;
        }
        return LocalDate.now().isAfter(date_prochaine_maintenance) ||
                LocalDate.now().isEqual(date_prochaine_maintenance);
    }

    public long getDaysUntilMaintenance() {
        if (date_prochaine_maintenance == null) {
            // Si pas de maintenance planifiée, utiliser la fréquence
            if (derniere_maintenance != null) {
                LocalDate next = derniere_maintenance.plusMonths(frequence_maintenance_mois);
                return ChronoUnit.DAYS.between(LocalDate.now(), next);
            } else if (date_achat != null) {
                LocalDate next = date_achat.plusMonths(frequence_maintenance_mois);
                return ChronoUnit.DAYS.between(LocalDate.now(), next);
            }
            return 365; // Valeur par défaut
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), date_prochaine_maintenance);
    }

    public boolean isMaintenanceProche() {
        long days = getDaysUntilMaintenance();
        return days >= 0 && days <= 30;
    }

    @Override
    public String toString() {
        return "Materiel{" +
                "id_materiel=" + id_materiel +
                ", nom='" + nom + '\'' +
                ", type='" + type + '\'' +
                ", etat='" + etat + '\'' +
                ", user_id=" + user_id +
                ", date_achat=" + date_achat +
                ", date_prochaine_maintenance=" + date_prochaine_maintenance +
                '}';
    }
}