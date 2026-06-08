package tn.neuron.ardhi.services.gestionemployeservice;

import tn.neuron.ardhi.models.gestionemployemodel.Notification;
import tn.neuron.ardhi.models.gestionemployemodel.Tache;
import tn.neuron.ardhi.models.gestionemployemodel.WeatherData;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationService {

    private final TacheService tacheService;

    public NotificationService() {
        this.tacheService = new TacheService();
    }

    private Connection getCnx() {
        return MyDatabase.getInstance().getCnx();
    }

    // ── CRUD ─────────────────────────────────────────────────────────────

    /**
     * Vérifie si une notification du même type pour la même tâche existe déjà
     * (non lue ou créée aujourd'hui) → évite les doublons
     */
    private boolean existsNotification(String type, Integer idTache, Integer idAgriculteur) {
        String query = "SELECT COUNT(*) FROM notification WHERE type = ? AND id_tache = ? AND id_agriculteur = ? " +
                "AND DATE(date_creation) = CURDATE()";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setString(1, type);
            setIntOrNull(stmt, 2, idTache);
            setIntOrNull(stmt, 3, idAgriculteur);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Vérifie si une notification du même type pour le même employé existe déjà aujourd'hui
     */
    private boolean existsNotificationEmploye(String type, Integer idEmploye, Integer idAgriculteur) {
        String query = "SELECT COUNT(*) FROM notification WHERE type = ? AND id_employe = ? AND id_agriculteur = ? " +
                "AND DATE(date_creation) = CURDATE()";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setString(1, type);
            setIntOrNull(stmt, 2, idEmploye);
            setIntOrNull(stmt, 3, idAgriculteur);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean createNotification(Notification notif) {
        String query = "INSERT INTO notification (type, priorite, titre, message, id_agriculteur, " +
                "id_tache, id_employe, lue, archivee, date_creation) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = getCnx().prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, notif.getType());
            stmt.setString(2, notif.getPriorite());
            stmt.setString(3, notif.getTitre());
            stmt.setString(4, notif.getMessage());
            setIntOrNull(stmt, 5, notif.getIdAgriculteur());
            setIntOrNull(stmt, 6, notif.getIdTache());
            setIntOrNull(stmt, 7, notif.getIdEmploye());
            stmt.setBoolean(8, notif.isLue());
            stmt.setBoolean(9, notif.isArchivee());
            stmt.setTimestamp(10, Timestamp.valueOf(notif.getDateCreation() != null ? notif.getDateCreation() : LocalDateTime.now()));

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) notif.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Notification> getNotificationsByAgriculteur(Integer idAgriculteur) {
        List<Notification> list = new ArrayList<>();
        String query = "SELECT * FROM notification WHERE id_agriculteur = ? ORDER BY date_creation DESC";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idAgriculteur);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToNotification(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }




    public boolean markAsRead(int idNotification) {
        String query = "UPDATE notification SET lue = true, date_lecture = ? WHERE id_notification = ?";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(2, idNotification);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean markAllAsRead(Integer idAgriculteur) {
        String query = "UPDATE notification SET lue = true, date_lecture = ? WHERE id_agriculteur = ? AND lue = false";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(2, idAgriculteur);
            return stmt.executeUpdate() >= 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int countUnread(Integer idAgriculteur) {
        String query = "SELECT COUNT(*) FROM notification WHERE id_agriculteur = ? AND lue = false";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idAgriculteur);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ── ANALYSE INTELLIGENTE ─────────────────────────────────────────────

    public void analyserNotifications(Integer idAgriculteur) {
        System.out.println("🔔 ANALYSE NOTIFICATIONS (Agriculteur #" + idAgriculteur + ")");

        List<Tache> taches = tacheService.getAllTaches();
        LocalDate aujourdhui = LocalDate.now();
        Set<String> dejaCreees = new HashSet<>();

        int nbRetard = 0, nbBloquee = 0;

        for (Tache t : taches) {
            // 🔴 CAS 1 : Tâche en retard
            if (t.getDateFin() != null && t.getDateFin().isBefore(aujourdhui) && !estTerminee(t)) {
                String cle = "retard-" + t.getId();
                if (!dejaCreees.contains(cle) && !existsNotification(Notification.TYPE_TACHE_RETARD, t.getId(), idAgriculteur)) {
                    long joursRetard = ChronoUnit.DAYS.between(t.getDateFin(), aujourdhui);
                    Notification notif = new Notification(
                            Notification.TYPE_TACHE_RETARD,
                            Notification.PRIORITE_CRITICAL,
                            "⏰ Tâche en retard : " + t.getTitre(),
                            String.format("Date limite : %s | Retard : %d jour(s)",
                                    formatDate(t.getDateFin()), joursRetard),
                            idAgriculteur,
                            t.getId(),
                            t.getIdEmploye()
                    );
                    createNotification(notif);
                    dejaCreees.add(cle);
                    nbRetard++;
                }
            }

            // 🟡 CAS 2 : Tâche bloquée (inactive depuis 2 jours)
            if (estEnCours(t) && !modificationRecent(t)) {
                String cle = "bloquee-" + t.getId();
                if (!dejaCreees.contains(cle) && !existsNotification(Notification.TYPE_TACHE_BLOQUEE, t.getId(), idAgriculteur)) {
                    LocalDateTime lastModif = t.getDateModification();
                    if (lastModif == null) {
                        lastModif = t.getDateDebut() != null ? t.getDateDebut().atStartOfDay() : LocalDateTime.now().minusDays(10);
                    }
                    long joursInactif = ChronoUnit.DAYS.between(lastModif.toLocalDate(), LocalDate.now());

                    Notification notif = new Notification(
                            Notification.TYPE_TACHE_BLOQUEE,
                            Notification.PRIORITE_WARNING,
                            "⚠️ Tâche bloquée : " + t.getTitre(),
                            String.format("Statut : En cours | Inactive depuis %d jour(s)", joursInactif),
                            idAgriculteur,
                            t.getId(),
                            t.getIdEmploye()
                    );
                    createNotification(notif);
                    dejaCreees.add(cle);
                    nbBloquee++;
                }
            }
        }

        System.out.println("📊 RÉSUMÉ : Retard = " + nbRetard + " | Bloquée = " + nbBloquee);

        // Nettoyer les notifications obsolètes
        nettoyerNotificationsObsoletes(idAgriculteur);

        // 🌦️ Analyse météo agricole
        analyserMeteo(idAgriculteur);

    }

    private void nettoyerNotificationsObsoletes(Integer idAgriculteur) {
        List<Notification> toutes = getNotificationsByAgriculteur(idAgriculteur);
        for (Notification n : toutes) {
            if (n.getIdTache() != null) {
                Tache t = tacheService.getTacheById(n.getIdTache());
                if (t == null) {
                    deleteNotification(n.getId());
                    continue;
                }
                if (n.getType().equals(Notification.TYPE_TACHE_RETARD) && estTerminee(t)) {
                    deleteNotification(n.getId());
                }
                if (n.getType().equals(Notification.TYPE_TACHE_BLOQUEE) && (!estEnCours(t) || modificationRecent(t))) {
                    deleteNotification(n.getId());
                }
            }
        }
    }

    // ── ANALYSE MÉTÉO AGRICOLE ───────────────────────────────────────────────

    /**
     * Récupère la météo courante et génère des notifications d'alerte
     * pour toutes les tâches actives sensibles aux conditions météo.
     */
    public void analyserMeteo(Integer idAgriculteur) {
        System.out.println("🌦️ ANALYSE MÉTÉO pour Agriculteur #" + idAgriculteur);
        try {
            WeatherService weatherService = new WeatherService();
            WeatherData weather = weatherService.getCurrentWeather();

            if (!weather.isAvailable()) {
                System.out.println("⚠️ Météo indisponible, analyse annulée.");
                return;
            }

            System.out.println("🌡️ Météo : " + weather.getSummary()
                    + " | Pluie: " + weather.isRainExpected()
                    + " | Vent: " + (int) weather.getWindSpeed() + " km/h");

            List<Tache> taches = tacheService.getAllTaches();
            int nbPositives = 0, nbNegatives = 0;

            for (Tache t : taches) {
                if (estTerminee(t)) continue;

                List<WeatherService.Recommandation> recos =
                        weatherService.analyserConditionsPourTache(t, weather);

                for (WeatherService.Recommandation reco : recos) {
                    // Anti-spam : 1 notification par type par tâche par jour
                    if (existsNotificationToday(reco.notifType, t.getId(), idAgriculteur)) continue;

                    String priorite;
                    String titre;
                    switch (reco.niveau) {
                        case POSITIVE:
                            priorite = Notification.PRIORITE_INFO;
                            titre    = "✅ Recommandé : " + t.getTitre();
                            nbPositives++;
                            break;
                        case DANGER:
                            priorite = Notification.PRIORITE_CRITICAL;
                            titre    = "🚫 Déconseillé : " + t.getTitre();
                            nbNegatives++;
                            break;
                        default: // WARNING
                            priorite = Notification.PRIORITE_WARNING;
                            titre    = "⚠️ Attention : " + t.getTitre();
                            nbNegatives++;
                            break;
                    }

                    createNotification(new Notification(
                            reco.notifType,
                            priorite,
                            titre,
                            reco.message,
                            idAgriculteur,
                            t.getId(),
                            t.getIdEmploye()
                    ));
                }
            }

            System.out.println("🌦️ Résumé : " + nbPositives + " recommandations ✅ | " + nbNegatives + " alertes ⚠️");

        } catch (Exception e) {
            System.err.println("⚠️ Erreur analyse météo : " + e.getMessage());
            e.printStackTrace();
        }
    }


    private String determineTypeAlerte(String message) {
        if (message.contains("🌧️")) return Notification.TYPE_METEO_PLUIE;
        if (message.contains("🔥")) return Notification.TYPE_METEO_CHALEUR;
        if (message.contains("💨")) return Notification.TYPE_METEO_VENT;
        return Notification.TYPE_METEO_INFO;
    }

    private boolean existsNotificationToday(String type, Integer idAgriculteur) {
        String query = "SELECT COUNT(*) FROM notification WHERE type = ? AND id_agriculteur = ? " +
                "AND DATE(date_creation) = CURDATE()";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setString(1, type);
            setIntOrNull(stmt, 2, idAgriculteur);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Anti-spam par tâche : 1 notification du même type par tâche par jour. */
    private boolean existsNotificationToday(String type, Integer idTache, Integer idAgriculteur) {
        String query = "SELECT COUNT(*) FROM notification WHERE type = ? AND id_tache = ? " +
                "AND id_agriculteur = ? AND DATE(date_creation) = CURDATE()";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setString(1, type);
            setIntOrNull(stmt, 2, idTache);
            setIntOrNull(stmt, 3, idAgriculteur);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }



    // ── Helpers ──────────────────────────────────────────────────────────


    private boolean estTerminee(Tache t) {
        String s = t.getStatut();
        return s != null && (s.equalsIgnoreCase("Terminé") || s.equalsIgnoreCase("Validé"));
    }

    private boolean estEnCours(Tache t) {
        String s = t.getStatut();
        return s != null && s.equalsIgnoreCase("En cours");
    }

    private boolean modificationRecent(Tache t) {
        LocalDateTime modif = t.getDateModification();
        if (modif == null) return false;
        return modif.isAfter(LocalDateTime.now().minusDays(2));
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A";
    }

    private void setIntOrNull(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value != null) stmt.setInt(index, value);
        else stmt.setNull(index, Types.INTEGER);
    }

    private Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id_notification"));
        n.setType(rs.getString("type"));
        n.setPriorite(rs.getString("priorite"));
        n.setTitre(rs.getString("titre"));
        n.setMessage(rs.getString("message"));

        int idAgri = rs.getInt("id_agriculteur");
        if (!rs.wasNull()) n.setIdAgriculteur(idAgri);

        int idTache = rs.getInt("id_tache");
        if (!rs.wasNull()) n.setIdTache(idTache);

        int idEmp = rs.getInt("id_employe");
        if (!rs.wasNull()) n.setIdEmploye(idEmp);

        n.setLue(rs.getBoolean("lue"));
        n.setArchivee(rs.getBoolean("archivee"));

        Timestamp ts = rs.getTimestamp("date_creation");
        if (ts != null) n.setDateCreation(ts.toLocalDateTime());

        ts = rs.getTimestamp("date_lecture");
        if (ts != null) n.setDateLecture(ts.toLocalDateTime());

        return n;
    }
    public boolean deleteNotification(int id) {
        String query = "DELETE FROM notification WHERE id_notification = ?";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}