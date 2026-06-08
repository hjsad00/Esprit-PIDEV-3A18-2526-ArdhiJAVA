package tn.neuron.ardhi.models.MaterielEtMaintenance;

import java.time.LocalDate;

public class Maintenance {
    private int id_maintenance;
    private int materiel_id;
    private String description;
    private LocalDate date_maintenance;  // Date réelle de la maintenance
    private double cout;

    // ⭐ NOUVEAUX ATTRIBUTS
    private String google_calendar_event_id;
    private String statut_maintenance;  // planifiee, en_cours, terminee, annulee
    private LocalDate date_planifiee;   // Date prévue
    private LocalDate date_realisee;    // Date effective
    private String type_maintenance;    // preventive, corrective, urgente

    // 🔗 Pour afficher le nom du matériel (JOIN)
    private String materiel_nom;

    // ========== CONSTRUCTEURS ==========

    public Maintenance() {
        this.statut_maintenance = "planifiee";
        this.type_maintenance = "preventive";
    }

    // Constructeur ancien (compatible)
    public Maintenance(int materiel_id, String description, LocalDate date_maintenance, double cout) {
        this.materiel_id = materiel_id;
        this.description = description;
        this.date_maintenance = date_maintenance;
        this.cout = cout;
        this.statut_maintenance = "planifiee";
        this.type_maintenance = "preventive";
    }

    // ⭐ NOUVEAU Constructeur complet
    public Maintenance(int id_maintenance, int materiel_id, String description,
                       LocalDate date_maintenance, double cout,
                       String google_calendar_event_id, String statut_maintenance,
                       LocalDate date_planifiee, LocalDate date_realisee,
                       String type_maintenance) {
        this.id_maintenance = id_maintenance;
        this.materiel_id = materiel_id;
        this.description = description;
        this.date_maintenance = date_maintenance;
        this.cout = cout;
        this.google_calendar_event_id = google_calendar_event_id;
        this.statut_maintenance = statut_maintenance;
        this.date_planifiee = date_planifiee;
        this.date_realisee = date_realisee;
        this.type_maintenance = type_maintenance;
    }

    // ========== GETTERS & SETTERS ==========

    public int getId_maintenance() { return id_maintenance; }
    public void setId_maintenance(int id_maintenance) { this.id_maintenance = id_maintenance; }

    public int getMateriel_id() { return materiel_id; }
    public void setMateriel_id(int materiel_id) { this.materiel_id = materiel_id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDate_maintenance() { return date_maintenance; }
    public void setDate_maintenance(LocalDate date_maintenance) { this.date_maintenance = date_maintenance; }

    public double getCout() { return cout; }
    public void setCout(double cout) { this.cout = cout; }

    // ⭐ NOUVEAUX GETTERS/SETTERS

    public String getGoogle_calendar_event_id() { return google_calendar_event_id; }
    public void setGoogle_calendar_event_id(String google_calendar_event_id) {
        this.google_calendar_event_id = google_calendar_event_id;
    }

    public String getStatut_maintenance() { return statut_maintenance; }
    public void setStatut_maintenance(String statut_maintenance) {
        this.statut_maintenance = statut_maintenance;
    }

    public LocalDate getDate_planifiee() { return date_planifiee; }
    public void setDate_planifiee(LocalDate date_planifiee) {
        this.date_planifiee = date_planifiee;
    }

    public LocalDate getDate_realisee() { return date_realisee; }
    public void setDate_realisee(LocalDate date_realisee) {
        this.date_realisee = date_realisee;
    }

    public String getType_maintenance() { return type_maintenance; }
    public void setType_maintenance(String type_maintenance) {
        this.type_maintenance = type_maintenance;
    }

    public String getMateriel_nom() { return materiel_nom; }
    public void setMateriel_nom(String materiel_nom) {
        this.materiel_nom = materiel_nom;
    }

    // ========== MÉTHODES UTILITAIRES ==========

    public boolean isEnRetard() {
        if (date_planifiee == null || "terminee".equals(statut_maintenance)) {
            return false;
        }
        return LocalDate.now().isAfter(date_planifiee);
    }

    public boolean isPlanifiee() {
        return "planifiee".equals(statut_maintenance);
    }

    public boolean isTerminee() {
        return "terminee".equals(statut_maintenance);
    }

    public String getStatutEmoji() {
        switch (statut_maintenance) {
            case "planifiee": return "📅";
            case "en_cours": return "🔧";
            case "terminee": return "✅";
            case "annulee": return "❌";
            default: return "❓";
        }
    }

    @Override
    public String toString() {
        return "Maintenance #" + id_maintenance +
                " - Matériel ID: " + materiel_id +
                " - Statut: " + statut_maintenance +
                " - Date planifiée: " + date_planifiee +
                " - Coût: " + cout + " TND";
    }
}