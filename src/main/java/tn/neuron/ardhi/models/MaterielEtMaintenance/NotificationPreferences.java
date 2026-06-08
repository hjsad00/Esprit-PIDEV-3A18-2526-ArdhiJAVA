package tn.neuron.ardhi.models.MaterielEtMaintenance;

public class NotificationPreferences {

    private int id;
    private int userId;
    private boolean emailEnabled = true;
    private boolean inappEnabled = true;
    private boolean alerteJ30    = true;
    private boolean alerteJ7     = true;
    private boolean alerteJ1     = true;
    private boolean alerteJZero  = true;
    private boolean alerteRetard = true;
    private boolean alerteStatut = true;

    public NotificationPreferences() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public boolean isEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(boolean v) { this.emailEnabled = v; }

    public boolean isInappEnabled() { return inappEnabled; }
    public void setInappEnabled(boolean v) { this.inappEnabled = v; }

    public boolean isAlerteJ30() { return alerteJ30; }
    public void setAlerteJ30(boolean v) { this.alerteJ30 = v; }

    public boolean isAlerteJ7() { return alerteJ7; }
    public void setAlerteJ7(boolean v) { this.alerteJ7 = v; }

    public boolean isAlerteJ1() { return alerteJ1; }
    public void setAlerteJ1(boolean v) { this.alerteJ1 = v; }

    public boolean isAlerteJZero() { return alerteJZero; }
    public void setAlerteJZero(boolean v) { this.alerteJZero = v; }

    public boolean isAlerteRetard() { return alerteRetard; }
    public void setAlerteRetard(boolean v) { this.alerteRetard = v; }

    public boolean isAlerteStatut() { return alerteStatut; }
    public void setAlerteStatut(boolean v) { this.alerteStatut = v; }
}
