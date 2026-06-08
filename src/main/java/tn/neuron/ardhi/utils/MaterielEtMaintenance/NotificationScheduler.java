package tn.neuron.ardhi.utils.MaterielEtMaintenance;

import tn.neuron.ardhi.models.MaterielEtMaintenance.Maintenance;
import tn.neuron.ardhi.models.MaterielEtMaintenance.Materiel;
import tn.neuron.ardhi.models.MaterielEtMaintenance.Notification;
import tn.neuron.ardhi.models.MaterielEtMaintenance.Notification.NiveauUrgence;
import tn.neuron.ardhi.models.MaterielEtMaintenance.Notification.TypeNotification;
import tn.neuron.ardhi.services.MaterielEtMaintenance.MaintenanceService;
import tn.neuron.ardhi.services.MaterielEtMaintenance.MaterielService;
import tn.neuron.ardhi.services.MaterielEtMaintenance.NotificationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler pour les alertes automatiques de maintenance.
 *
 * Constructeur Notification utilisé :
 *   new Notification(userId, materielId, maintenanceId, type, titre, message, niveauUrgence)
 */
public class NotificationScheduler {

    private static NotificationScheduler instance;
    private ScheduledExecutorService scheduler;
    private int currentUserId = -1;

    public static NotificationScheduler getInstance() {
        if (instance == null) instance = new NotificationScheduler();
        return instance;
    }

    private NotificationScheduler() {}

    public void demarrer(int userId) {
        if (scheduler != null && !scheduler.isShutdown() && currentUserId == userId) {
            System.out.println("INFO NotificationScheduler deja actif pour user_id=" + userId);
            return;
        }
        arreter();
        currentUserId = userId;
        scheduler = Executors.newScheduledThreadPool(1);

        // Scan immediat apres 2 secondes
        scheduler.schedule(() -> scannerTout(userId), 2, TimeUnit.SECONDS);

        // Puis toutes les 24h a 08h00
        long delaiInitial = calculerDelaiJusquaHeure(8, 0);
        scheduler.scheduleAtFixedRate(
                () -> scannerTout(userId),
                delaiInitial,
                24 * 60 * 60,
                TimeUnit.SECONDS
        );
        System.out.println("OK NotificationScheduler demarre pour user_id=" + userId);
    }

    public void arreter() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            System.out.println("NotificationScheduler arrete.");
        }
    }

    /** Forcer un scan immediat (apres planification d'une maintenance). */
    public void scannerMaintenant(int userId) {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.submit(() -> scannerTout(userId));
        }
    }

    // ════════════════════════════════════════════════════════
    //  SCAN PRINCIPAL
    // ════════════════════════════════════════════════════════

    private void scannerTout(int userId) {
        System.out.println("[SCHEDULER] Scan complet " + LocalDateTime.now() + " user_id=" + userId);

        MaintenanceService  maintenanceService  = new MaintenanceService();
        MaterielService     materielService     = new MaterielService();
        NotificationService notificationService = new NotificationService();

        scannerMaintenancesPlanifiees(userId, maintenanceService, materielService, notificationService);
        scannerRetardsMateriels(userId, materielService, notificationService);
    }

    // ════════════════════════════════════════════════════════
    //  SCAN 1 — Maintenances planifiees (table maintenance)
    // ════════════════════════════════════════════════════════

    private void scannerMaintenancesPlanifiees(int userId,
                                               MaintenanceService maintenanceService,
                                               MaterielService materielService,
                                               NotificationService notificationService) {
        try {
            List<Maintenance> maintenances = maintenanceService.getMaintenancesPlanifiees(userId);
            System.out.println("[SCHEDULER] " + maintenances.size() + " maintenance(s) planifiee(s)");

            for (Maintenance maintenance : maintenances) {
                try {
                    Materiel materiel = materielService.getById(maintenance.getMateriel_id());
                    if (materiel == null) continue;

                    LocalDate datePlanifiee = maintenance.getDate_planifiee();
                    if (datePlanifiee == null) datePlanifiee = maintenance.getDate_maintenance();
                    if (datePlanifiee == null) continue;

                    // La deduplication est geree dans traiterNotificationMaintenance
                    notificationService.traiterNotificationMaintenance(
                            userId, materiel.getId_materiel(),
                            maintenance.getId_maintenance(),
                            materiel.getNom(), datePlanifiee);

                } catch (Exception e) {
                    System.err.println("[SCHEDULER] Erreur maintenance #"
                            + maintenance.getId_maintenance() + " : " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[SCHEDULER] Erreur scan planifiees : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════
    //  SCAN 2 — Materiels en retard (date_prochaine_maintenance dans materiel)
    //
    //  ORDRE CONSTRUCTEUR : new Notification(
    //      userId, materielId, maintenanceId,   <- int, int, int
    //      type,                                <- TypeNotification
    //      titre,                               <- String
    //      message,                             <- String
    //      niveauUrgence                        <- NiveauUrgence
    //  )
    // ════════════════════════════════════════════════════════

    private void scannerRetardsMateriels(int userId,
                                         MaterielService materielService,
                                         NotificationService notificationService) {
        try {
            List<Materiel> materiels = materielService.getMaterielsByUserId(userId);
            System.out.println("[SCHEDULER] Scan retards : " + materiels.size() + " materiel(s)");

            LocalDate aujourdhui = LocalDate.now();

            for (Materiel materiel : materiels) {
                LocalDate dateMaint = materiel.getDate_prochaine_maintenance();
                if (dateMaint == null) dateMaint = calculerDateDepuisAchat(materiel);
                if (dateMaint == null) continue;

                long jours = ChronoUnit.DAYS.between(aujourdhui, dateMaint);
                if (jours > 30) continue;

                int mid = materiel.getId_materiel();
                String nom = materiel.getNom();

                if (jours < 0) {
                    // EN RETARD — une notif par jour max (fenetre 24h)
                    if (!notificationService.existeDejaNotificationMateriel(mid, TypeNotification.RETARD)) {
                        long retard = Math.abs(jours);
                        notificationService.sauvegarderNotification(new Notification(
                                userId, mid, 0,
                                TypeNotification.RETARD,
                                "RETARD de " + retard + " j - " + nom,
                                "La maintenance de " + nom + " est EN RETARD de " + retard
                                        + " jour(s) ! (prevue le " + dateMaint + ")",
                                NiveauUrgence.URGENT
                        ));
                        System.out.println("[SCHEDULER] Retard notifie : " + nom);
                    } else {
                        System.out.println("[SCHEDULER] Retard deja notifie aujourd'hui : " + nom);
                    }

                } else if (jours == 0) {
                    if (!notificationService.existeDejaNotificationMateriel(mid, TypeNotification.J_ZERO)) {
                        notificationService.sauvegarderNotification(new Notification(
                                userId, mid, 0,
                                TypeNotification.J_ZERO,
                                "Maintenance AUJOURD'HUI - " + nom,
                                "La maintenance de " + nom + " est prevue AUJOURD'HUI. Agissez maintenant.",
                                NiveauUrgence.URGENT
                        ));
                    }

                } else if (jours == 1) {
                    if (!notificationService.existeDejaNotificationMateriel(mid, TypeNotification.J_MOINS_1)) {
                        notificationService.sauvegarderNotification(new Notification(
                                userId, mid, 0,
                                TypeNotification.J_MOINS_1,
                                "Maintenance DEMAIN - " + nom,
                                "La maintenance de " + nom + " est prevue DEMAIN (" + dateMaint + ").",
                                NiveauUrgence.CETTE_SEMAINE
                        ));
                    }

                } else if (jours <= 7) {
                    if (!notificationService.existeDejaNotificationMateriel(mid, TypeNotification.J_MOINS_7)) {
                        notificationService.sauvegarderNotification(new Notification(
                                userId, mid, 0,
                                TypeNotification.J_MOINS_7,
                                "Maintenance dans " + jours + " jours - " + nom,
                                "La maintenance de " + nom + " est dans " + jours
                                        + " jour(s) (le " + dateMaint + ").",
                                NiveauUrgence.CETTE_SEMAINE
                        ));
                    }

                } else { // jours <= 30
                    if (!notificationService.existeDejaNotificationMateriel(mid, TypeNotification.J_MOINS_30)) {
                        notificationService.sauvegarderNotification(new Notification(
                                userId, mid, 0,
                                TypeNotification.J_MOINS_30,
                                "Maintenance dans " + jours + " jours - " + nom,
                                "La maintenance de " + nom + " est prevue dans " + jours
                                        + " jour(s) (le " + dateMaint + ").",
                                NiveauUrgence.CE_MOIS
                        ));
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("[SCHEDULER] Erreur scan retards : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ════════════════════════════════════════════════════════

    private LocalDate calculerDateDepuisAchat(Materiel materiel) {
        if (materiel.getDate_achat() == null) return null;
        int freq = materiel.getFrequence_maintenance_mois();
        if (freq <= 0) freq = 12;
        return materiel.getDate_achat().plusMonths(freq);
    }

    private long calculerDelaiJusquaHeure(int heure, int minute) {
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime cible = maintenant.toLocalDate().atTime(LocalTime.of(heure, minute));
        if (maintenant.isAfter(cible)) cible = cible.plusDays(1);
        return ChronoUnit.SECONDS.between(maintenant, cible);
    }
}
