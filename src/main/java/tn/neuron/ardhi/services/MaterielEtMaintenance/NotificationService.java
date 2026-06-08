package tn.neuron.ardhi.services.MaterielEtMaintenance;

import tn.neuron.ardhi.models.MaterielEtMaintenance.Notification;
import tn.neuron.ardhi.models.MaterielEtMaintenance.Notification.NiveauUrgence;
import tn.neuron.ardhi.models.MaterielEtMaintenance.Notification.TypeNotification;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service JDBC pour la gestion des notifications.
 *
 * FIX DOUBLONS (v2) :
 *  existeDejaNotificationMateriel() — deux comportements selon le type :
 *   - RETARD  : fenêtre de 24h  (on notifie une fois par jour)
 *   - Autres  : permanente       (on notifie une seule fois au total)
 */
public class NotificationService {

    private Connection getCnx() {
        return MyDatabase.getInstance().getCnx();
    }

    // ════════════════════════════════════════════════════════
    //  CRÉER UNE NOTIFICATION
    // ════════════════════════════════════════════════════════

    public boolean sauvegarderNotification(Notification notification) {
        String sql = "INSERT INTO notifications " +
                "(user_id, materiel_id, maintenance_id, type, titre, message, niveau_urgence, canal, lu, envoye) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 'IN_APP', 0, 0)";

        try (PreparedStatement stmt = getCnx().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, notification.getUserId());

            if (notification.getMaterielId() > 0)
                stmt.setInt(2, notification.getMaterielId());
            else
                stmt.setNull(2, Types.INTEGER);

            if (notification.getMaintenanceId() > 0)
                stmt.setInt(3, notification.getMaintenanceId());
            else
                stmt.setNull(3, Types.INTEGER);

            stmt.setString(4, notification.getType() != null ? notification.getType().name() : "INFO");
            stmt.setString(5, notification.getTitre());
            stmt.setString(6, notification.getMessage());
            stmt.setString(7, notification.getNiveauUrgence() != null
                    ? notification.getNiveauUrgence().name() : "OK");

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) notification.setId(keys.getInt(1));
                System.out.println("✅ Notification sauvegardée [id=" + notification.getId()
                        + "] : " + notification.getTitre());
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("❌ Erreur sauvegarderNotification : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ════════════════════════════════════════════════════════
    //  RÉCUPÉRER
    // ════════════════════════════════════════════════════════

    public List<Notification> getNotificationsByUserId(int userId) {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 50";
        try (PreparedStatement stmt = getCnx().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) notifications.add(extraireNotification(rs));
            System.out.println("📋 " + notifications.size() + " notification(s) pour user_id=" + userId);
        } catch (SQLException e) {
            System.err.println("❌ Erreur getNotificationsByUserId : " + e.getMessage());
        }
        return notifications;
    }

    public int getNombreNonLues(int userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND lu = 0";
        try (PreparedStatement stmt = getCnx().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("❌ Erreur getNombreNonLues : " + e.getMessage());
        }
        return 0;
    }

    public List<Notification> getNotificationsNonLues(int userId) {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND lu = 0 ORDER BY created_at DESC";
        try (PreparedStatement stmt = getCnx().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) notifications.add(extraireNotification(rs));
        } catch (SQLException e) {
            System.err.println("❌ Erreur getNotificationsNonLues : " + e.getMessage());
        }
        return notifications;
    }

    public List<Notification> getNotificationsUrgentes(int userId) {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND lu = 0 " +
                "AND niveau_urgence IN ('URGENT', 'CETTE_SEMAINE') ORDER BY created_at DESC";
        try (PreparedStatement stmt = getCnx().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) notifications.add(extraireNotification(rs));
        } catch (SQLException e) {
            System.err.println("❌ Erreur getNotificationsUrgentes : " + e.getMessage());
        }
        return notifications;
    }

    // ════════════════════════════════════════════════════════
    //  VÉRIFICATION DOUBLONS
    // ════════════════════════════════════════════════════════

    /**
     * Dédup pour les maintenances planifiées (table maintenance).
     * Vérifie sur maintenance_id — permanente (une seule fois par maintenance).
     */
    public boolean existeDejaNotification(int maintenanceId, TypeNotification type) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE maintenance_id = ? AND type = ?";
        try (PreparedStatement stmt = getCnx().prepareStatement(sql)) {
            stmt.setInt(1, maintenanceId);
            stmt.setString(2, type.name());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur existeDejaNotification : " + e.getMessage());
        }
        return false;
    }

    /**
     * Dédup pour les matériels sans maintenance planifiée (scan retards).
     *
     * Règle :
     *  - RETARD     → fenêtre 24h  : une notification de retard par jour max
     *  - J_ZERO     → permanente   : une seule fois le jour J
     *  - J_MOINS_1  → permanente
     *  - J_MOINS_7  → permanente
     *  - J_MOINS_30 → permanente
     *
     * La vérification se fait sur (materiel_id, type, maintenance_id IS NULL)
     * pour distinguer les notifs "matériel sans maintenance" des vraies maintenances.
     */
    public boolean existeDejaNotificationMateriel(int materielId, TypeNotification type) {
        String sql;

        if (type == TypeNotification.RETARD) {
            // Fenêtre 24h pour les retards (on notifie une fois par jour)
            sql = "SELECT COUNT(*) FROM notifications " +
                    "WHERE materiel_id = ? AND type = ? AND maintenance_id IS NULL " +
                    "AND created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)";
        } else {
            // Permanente pour les autres types (J-30, J-7, J-1, J-0)
            // On n'envoie qu'une seule fois par matériel par type
            sql = "SELECT COUNT(*) FROM notifications " +
                    "WHERE materiel_id = ? AND type = ? AND maintenance_id IS NULL";
        }

        try (PreparedStatement stmt = getCnx().prepareStatement(sql)) {
            stmt.setInt(1, materielId);
            stmt.setString(2, type.name());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur existeDejaNotificationMateriel : " + e.getMessage());
        }
        return false;
    }

    // ════════════════════════════════════════════════════════
    //  MARQUER COMME LU
    // ════════════════════════════════════════════════════════

    public boolean marquerCommeLue(int notificationId) {
        String sql = "UPDATE notifications SET lu = 1, date_lecture = NOW() WHERE id = ?";
        try (PreparedStatement stmt = getCnx().prepareStatement(sql)) {
            stmt.setInt(1, notificationId);
            boolean ok = stmt.executeUpdate() > 0;
            if (ok) System.out.println("✅ Notification #" + notificationId + " marquée lue");
            return ok;
        } catch (SQLException e) {
            System.err.println("❌ Erreur marquerCommeLue : " + e.getMessage());
            return false;
        }
    }

    public int marquerToutesCommeLues(int userId) {
        String sql = "UPDATE notifications SET lu = 1, date_lecture = NOW() WHERE user_id = ? AND lu = 0";
        try (PreparedStatement stmt = getCnx().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            int count = stmt.executeUpdate();
            System.out.println("✅ " + count + " notif(s) marquée(s) lues pour user_id=" + userId);
            return count;
        } catch (SQLException e) {
            System.err.println("❌ Erreur marquerToutesCommeLues : " + e.getMessage());
            return 0;
        }
    }

    // ════════════════════════════════════════════════════════
    //  LOGIQUE ALERTES AUTOMATIQUES (maintenances planifiées)
    // ════════════════════════════════════════════════════════

    public void traiterNotificationMaintenance(int userId, int materielId, int maintenanceId,
                                               String nomMateriel, LocalDate datePlanifiee) {
        if (datePlanifiee == null) return;

        long jours = ChronoUnit.DAYS.between(LocalDate.now(), datePlanifiee);
        String dateStr = datePlanifiee.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        if (jours > 30) {
            creerNotifSiAbsente(userId, materielId, maintenanceId,
                    TypeNotification.J_MOINS_30, NiveauUrgence.BIENTOT,
                    "📅 Maintenance prévue – " + nomMateriel,
                    "Une maintenance de " + nomMateriel + " est planifiée le " + dateStr
                            + " (dans " + jours + " jour(s)).");

        } else if (jours > 7) {
            creerNotifSiAbsente(userId, materielId, maintenanceId,
                    TypeNotification.J_MOINS_30, NiveauUrgence.CE_MOIS,
                    "📅 Maintenance dans " + jours + " jours – " + nomMateriel,
                    "La maintenance de " + nomMateriel + " est prévue le " + dateStr
                            + " (dans " + jours + " jour(s)).");

        } else if (jours > 1) {
            creerNotifSiAbsente(userId, materielId, maintenanceId,
                    TypeNotification.J_MOINS_7, NiveauUrgence.CETTE_SEMAINE,
                    "⚠️ Maintenance dans " + jours + " jours – " + nomMateriel,
                    "Attention ! La maintenance de " + nomMateriel
                            + " est dans " + jours + " jour(s) (le " + dateStr + ").");

        } else if (jours == 1) {
            creerNotifSiAbsente(userId, materielId, maintenanceId,
                    TypeNotification.J_MOINS_1, NiveauUrgence.CETTE_SEMAINE,
                    "⚠️ Maintenance DEMAIN – " + nomMateriel,
                    "Rappel urgent : la maintenance de " + nomMateriel + " est prévue DEMAIN (" + dateStr + ").");

        } else if (jours == 0) {
            creerNotifSiAbsente(userId, materielId, maintenanceId,
                    TypeNotification.J_ZERO, NiveauUrgence.URGENT,
                    "🚨 Maintenance AUJOURD'HUI – " + nomMateriel,
                    "La maintenance de " + nomMateriel + " est prévue AUJOURD'HUI. Veuillez agir immédiatement.");

        } else { // jours < 0 — retard
            long retard = Math.abs(jours);
            // Pour les maintenances planifiées en retard : pas de dédup (on crée à chaque scan)
            sauvegarderNotification(new Notification(userId, materielId, maintenanceId,
                    TypeNotification.RETARD,
                    "🚨 RETARD de " + retard + " jour(s) – " + nomMateriel,
                    "⚠️ EN RETARD de " + retard + " jour(s) ! Maintenance due le " + dateStr,
                    NiveauUrgence.URGENT));
        }
    }

    public void creerNotifChangementStatut(int userId, int materielId, int maintenanceId,
                                           String nomMateriel, String ancienStatut, String nouveauStatut) {
        sauvegarderNotification(new Notification(userId, materielId, maintenanceId,
                TypeNotification.STATUT_CHANGE,
                "🔄 Statut mis à jour – " + nomMateriel,
                "Le statut de la maintenance de " + nomMateriel
                        + " est passé de « " + ancienStatut + " » à « " + nouveauStatut + " ».",
                NiveauUrgence.OK));
    }

    // ════════════════════════════════════════════════════════
    //  UTILITAIRES PRIVÉS
    // ════════════════════════════════════════════════════════

    private void creerNotifSiAbsente(int userId, int materielId, int maintenanceId,
                                     TypeNotification type, NiveauUrgence urgence,
                                     String titre, String message) {
        if (existeDejaNotification(maintenanceId, type)) {
            System.out.println("ℹ️ Notif " + type + " déjà existante pour maintenance #" + maintenanceId);
            return;
        }
        sauvegarderNotification(new Notification(userId, materielId, maintenanceId,
                type, titre, message, urgence));
    }

    private Notification extraireNotification(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setUserId(rs.getInt("user_id"));

        int matId = rs.getInt("materiel_id");
        if (!rs.wasNull()) n.setMaterielId(matId);

        int maintId = rs.getInt("maintenance_id");
        if (!rs.wasNull()) n.setMaintenanceId(maintId);

        n.setTitre(rs.getString("titre"));
        n.setMessage(rs.getString("message"));
        n.setLu(rs.getBoolean("lu"));

        String type = rs.getString("type");
        if (type != null) {
            try { n.setType(TypeNotification.valueOf(type)); }
            catch (IllegalArgumentException e) { n.setType(TypeNotification.INFO); }
        }

        String urgence = rs.getString("niveau_urgence");
        if (urgence != null) {
            try { n.setNiveauUrgence(NiveauUrgence.valueOf(urgence)); }
            catch (IllegalArgumentException e) { n.setNiveauUrgence(NiveauUrgence.OK); }
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) n.setCreatedAt(createdAt.toLocalDateTime());

        return n;
    }
}
