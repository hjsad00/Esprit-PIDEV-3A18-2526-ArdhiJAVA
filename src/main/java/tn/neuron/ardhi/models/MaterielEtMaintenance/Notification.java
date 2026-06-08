package tn.neuron.ardhi.models.MaterielEtMaintenance;

import java.time.LocalDateTime;

/**
 * Modèle représentant une notification dans l'application Ardhi.
 * Emplacement : src/main/java/tn/neuron/ardhi/models/MaterielEtMaintenance/Notification.java
 */
public class Notification {

    // ── Enums ────────────────────────────────────────────────
    public enum TypeNotification {
        J_MOINS_30, J_MOINS_7, J_MOINS_1, J_ZERO,
        RETARD, STATUT_CHANGE, RENDEZ_VOUS_CONFIRME,
        RAPPEL_J_MOINS_3, INFO
    }

    public enum NiveauUrgence {
        OK, BIENTOT, CE_MOIS, CETTE_SEMAINE, URGENT
    }

    // ── Champs ───────────────────────────────────────────────
    private int id;
    private int userId;
    private int materielId;
    private int maintenanceId;
    private TypeNotification type;
    private String titre;
    private String message;
    private NiveauUrgence niveauUrgence;
    private boolean lu;
    private LocalDateTime createdAt;

    // ── Constructeur vide ────────────────────────────────────
    public Notification() {}

    // ── Constructeur complet ─────────────────────────────────
    public Notification(int userId, int materielId, int maintenanceId,
                        TypeNotification type, String titre, String message,
                        NiveauUrgence niveauUrgence) {
        this.userId = userId;
        this.materielId = materielId;
        this.maintenanceId = maintenanceId;
        this.type = type;
        this.titre = titre;
        this.message = message;
        this.niveauUrgence = niveauUrgence;
        this.lu = false;
        this.createdAt = LocalDateTime.now();
    }

    // ── Getters / Setters ────────────────────────────────────
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getMaterielId() { return materielId; }
    public void setMaterielId(int materielId) { this.materielId = materielId; }

    public int getMaintenanceId() { return maintenanceId; }
    public void setMaintenanceId(int maintenanceId) { this.maintenanceId = maintenanceId; }

    public TypeNotification getType() { return type; }
    public void setType(TypeNotification type) { this.type = type; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public NiveauUrgence getNiveauUrgence() { return niveauUrgence; }
    public void setNiveauUrgence(NiveauUrgence niveauUrgence) { this.niveauUrgence = niveauUrgence; }

    public boolean isLu() { return lu; }
    public void setLu(boolean lu) { this.lu = lu; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ── Méthode utilitaire pour l'emoji ─────────────────────
    public String getEmoji() {
        if (niveauUrgence == null) return "🔔";
        switch (niveauUrgence) {
            case URGENT:        return "🚨";
            case CETTE_SEMAINE: return "⚠️";
            case CE_MOIS:       return "⏰";
            case BIENTOT:       return "📅";
            default:            return "✅";
        }
    }
}
