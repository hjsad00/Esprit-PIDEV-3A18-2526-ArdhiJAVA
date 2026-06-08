package tn.neuron.ardhi.services.Evenement;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Service planificateur pour l'envoi automatique des emails
 * Tourne en arrière-plan et exécute les tâches automatiques
 */
public class EmailSchedulerService {

    private Timer timer;
    private AttestationAutomatiqueService attestationService;
    private EmailsService emailsService;
    private boolean isRunning = false;

    public EmailSchedulerService() {
        this.attestationService = new AttestationAutomatiqueService();
        this.emailsService = new EmailsService();
        this.timer = new Timer("EmailScheduler", true); // Daemon thread
    }

    /**
     * Démarre le planificateur
     * Exécute les tâches automatiques toutes les heures
     */
    public void start() {
        if (isRunning) {
            System.out.println("⚠️ Scheduler déjà en cours d'exécution");
            return;
        }

        System.out.println("\n🚀 Démarrage du planificateur d'emails automatiques...");

        // Tâche: Envoyer attestations automatiques
        TimerTask attestationTask = new TimerTask() {
            @Override
            public void run() {
                System.out.println("\n⏰ Tâche automatique: Envoi attestations");
                try {
                    Map<String, Integer> stats = attestationService.envoyerAttestationsAutomatiques();
                    System.out.println("✅ Attestations envoyées: " + stats.get("envoyes"));
                } catch (Exception e) {
                    System.err.println("❌ Erreur tâche attestations: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };

        // Tâche: Envoyer rappels événements
        TimerTask rappelTask = new TimerTask() {
            @Override
            public void run() {
                System.out.println("\n⏰ Tâche automatique: Envoi rappels");
                try {
                    Map<String, Integer> stats = emailsService.envoyerRappelsAutomatiques();
                    System.out.println("✅ Rappels 3 jours: " + stats.get("rappels3jours"));
                    System.out.println("✅ Rappels 1 jour: " + stats.get("rappels1jour"));
                } catch (Exception e) {
                    System.err.println("❌ Erreur tâche rappels: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };

        // Exécuter immédiatement au démarrage, puis toutes les heures
        long delayInitial = 0; // Démarrage immédiat
        long periode = TimeUnit.HOURS.toMillis(1); // Toutes les heures

        timer.scheduleAtFixedRate(attestationTask, delayInitial, periode);
        timer.scheduleAtFixedRate(rappelTask, delayInitial, periode);

        isRunning = true;
        System.out.println("✅ Scheduler démarré - Tâches exécutées toutes les heures");
        System.out.println("   📧 Attestations automatiques activées");
        System.out.println("   📧 Rappels automatiques activés");
    }

    /**
     * Arrête le planificateur
     */
    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
            isRunning = false;
            System.out.println("🛑 Scheduler arrêté");
        }
    }

    /**
     * Exécute manuellement la tâche d'attestations
     */
    public Map<String, Integer> executerAttestationsMaintenant() {
        System.out.println("\n▶️ Exécution manuelle: Attestations");
        return attestationService.envoyerAttestationsAutomatiques();
    }

    /**
     * Exécute manuellement la tâche de rappels
     */
    public Map<String, Integer> executerRappelsMaintenant() {
        System.out.println("\n▶️ Exécution manuelle: Rappels");
        return emailsService.envoyerRappelsAutomatiques();
    }

    /**
     * Retourne l'état du scheduler
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Configure la période d'exécution (en minutes)
     */
    public void setPeriodeMinutes(int minutes) {
        if (isRunning) {
            stop();
        }
        // Redémarrer avec nouvelle période
        // TODO: Implémenter si nécessaire
    }
}