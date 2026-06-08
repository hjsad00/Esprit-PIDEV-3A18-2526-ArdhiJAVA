package tn.neuron.ardhi.utils.MaterielEtMaintenance;

import tn.neuron.ardhi.models.MaterielEtMaintenance.Materiel;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe utilitaire pour gérer les alertes de maintenance
 * Calcule automatiquement quand un matériel nécessite une maintenance
 */
public class MaintenanceAlertUtils {

    /**
     * Vérifie si un matériel nécessite une maintenance urgente
     * Basé sur la date d'achat + fréquence de maintenance
     */
    public static boolean needsMaintenanceUrgent(Materiel materiel) {
        if (materiel.getDate_achat() == null) {
            return false; // Pas de date d'achat, on ne peut pas calculer
        }

        // Si une maintenance est déjà planifiée et à jour, pas d'alerte
        if (materiel.getDate_prochaine_maintenance() != null) {
            LocalDate prochaineMaintenance = materiel.getDate_prochaine_maintenance();
            if (prochaineMaintenance.isAfter(LocalDate.now())) {
                return false; // Maintenance déjà planifiée dans le futur
            }
        }

        // Calculer la date théorique de maintenance
        LocalDate dateAchat = materiel.getDate_achat();
        int frequenceMois = materiel.getFrequence_maintenance_mois(); // Par défaut 12 mois

        LocalDate dateMaintenanceDue = dateAchat.plusMonths(frequenceMois);

        // Si la dernière maintenance a été faite, recalculer à partir de là
        if (materiel.getDerniere_maintenance() != null) {
            dateMaintenanceDue = materiel.getDerniere_maintenance().plusMonths(frequenceMois);
        }

        // Alerte si la date est dépassée ou dans moins de 30 jours
        return LocalDate.now().isAfter(dateMaintenanceDue.minusDays(30));
    }

    /**
     * Retourne le nombre de jours avant la prochaine maintenance
     * Négatif = en retard
     */
    public static long getDaysUntilMaintenance(Materiel materiel) {
        if (materiel.getDate_achat() == null) {
            return 999; // Pas de date, pas d'urgence
        }

        LocalDate dateMaintenanceDue = calculerDateMaintenanceDue(materiel);
        return ChronoUnit.DAYS.between(LocalDate.now(), dateMaintenanceDue);
    }

    /**
     * Calcule la date théorique de la prochaine maintenance
     */
    public static LocalDate calculerDateMaintenanceDue(Materiel materiel) {
        if (materiel.getDate_achat() == null) {
            return LocalDate.now().plusYears(1); // Par défaut dans 1 an
        }

        // Si une maintenance est déjà planifiée, la retourner
        if (materiel.getDate_prochaine_maintenance() != null) {
            return materiel.getDate_prochaine_maintenance();
        }

        // Sinon, calculer à partir de la date d'achat ou de la dernière maintenance
        LocalDate dateReference = materiel.getDerniere_maintenance() != null
                ? materiel.getDerniere_maintenance()
                : materiel.getDate_achat();

        return dateReference.plusMonths(materiel.getFrequence_maintenance_mois());
    }

    /**
     * Retourne le niveau d'urgence de la maintenance
     */
    public static String getMaintenanceUrgencyLevel(Materiel materiel) {
        long daysUntil = getDaysUntilMaintenance(materiel);

        if (daysUntil < 0) {
            return "URGENT"; // En retard !
        } else if (daysUntil <= 7) {
            return "CETTE_SEMAINE";
        } else if (daysUntil <= 30) {
            return "CE_MOIS";
        } else if (daysUntil <= 90) {
            return "BIENTOT";
        } else {
            return "OK";
        }
    }

    /**
     * Retourne un emoji selon le niveau d'urgence
     */
    public static String getUrgencyEmoji(Materiel materiel) {
        String urgency = getMaintenanceUrgencyLevel(materiel);

        switch (urgency) {
            case "URGENT": return "🚨";
            case "CETTE_SEMAINE": return "⚠️";
            case "CE_MOIS": return "⏰";
            case "BIENTOT": return "📅";
            default: return "✅";
        }
    }

    /**
     * Retourne un message descriptif
     */
    public static String getMaintenanceMessage(Materiel materiel) {
        long daysUntil = getDaysUntilMaintenance(materiel);
        String emoji = getUrgencyEmoji(materiel);

        if (daysUntil < 0) {
            return emoji + " EN RETARD de " + Math.abs(daysUntil) + " jours !";
        } else if (daysUntil == 0) {
            return emoji + " AUJOURD'HUI !";
        } else if (daysUntil <= 7) {
            return emoji + " Dans " + daysUntil + " jours";
        } else if (daysUntil <= 30) {
            return emoji + " Dans " + daysUntil + " jours (" + (daysUntil / 7) + " semaines)";
        } else {
            return emoji + " Dans " + (daysUntil / 30) + " mois";
        }
    }

    /**
     * Filtre les matériels qui nécessitent une maintenance urgente
     */
    public static List<Materiel> filterMaterielsNeedingMaintenance(List<Materiel> materiels) {
        List<Materiel> materielsUrgents = new ArrayList<>();

        for (Materiel materiel : materiels) {
            if (needsMaintenanceUrgent(materiel)) {
                materielsUrgents.add(materiel);
            }
        }

        return materielsUrgents;
    }

    /**
     * Suggère une date de maintenance optimale
     * (1 an après l'achat ou après la dernière maintenance)
     */
    public static LocalDate suggererDateMaintenance(Materiel materiel) {
        LocalDate dateReference;

        if (materiel.getDerniere_maintenance() != null) {
            // Partir de la dernière maintenance
            dateReference = materiel.getDerniere_maintenance();
        } else if (materiel.getDate_achat() != null) {
            // Partir de la date d'achat
            dateReference = materiel.getDate_achat();
        } else {
            // Par défaut, dans 1 an
            return LocalDate.now().plusYears(1);
        }

        // Ajouter la fréquence de maintenance
        return dateReference.plusMonths(materiel.getFrequence_maintenance_mois());
    }

    /**
     * Génère un message de notification pour l'agriculteur
     */
    public static String genererNotificationMaintenance(Materiel materiel) {
        StringBuilder notification = new StringBuilder();

        notification.append("🔧 RAPPEL DE MAINTENANCE\n\n");
        notification.append("Matériel : ").append(materiel.getNom()).append("\n");
        notification.append("Type : ").append(materiel.getType()).append("\n");
        notification.append("État : ").append(materiel.getEtat()).append("\n\n");

        if (materiel.getDate_achat() != null) {
            long moisDepuisAchat = ChronoUnit.MONTHS.between(materiel.getDate_achat(), LocalDate.now());
            notification.append("Acheté il y a : ").append(moisDepuisAchat).append(" mois\n");
        }

        if (materiel.getDerniere_maintenance() != null) {
            long moisDepuisMaintenance = ChronoUnit.MONTHS.between(materiel.getDerniere_maintenance(), LocalDate.now());
            notification.append("Dernière maintenance : il y a ").append(moisDepuisMaintenance).append(" mois\n");
        }

        notification.append("\n").append(getMaintenanceMessage(materiel)).append("\n\n");
        notification.append("💡 Action recommandée : Planifiez une maintenance dès que possible.");

        return notification.toString();
    }
}
