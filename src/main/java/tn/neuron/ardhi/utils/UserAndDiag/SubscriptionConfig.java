package tn.neuron.ardhi.utils.UserAndDiag;

import java.io.*;
import java.util.Properties;

/**
 * Classe utilitaire pour gérer la configuration du tier gratuit.
 * Les limites sont stockées dans un fichier properties modifiable par l'admin.
 */
public class SubscriptionConfig {

    private SubscriptionConfig() {
        // Class utilitaire
    }

    private static final String CONFIG_FILE = "subscription_config.properties";
    private static Properties properties = new Properties();

    // Free tier limits (pour utilisateurs sans abonnement)
    private static int freeDiagnosticsParHeure = 3; // -1 = unlimited
    private static boolean freeAccesTraitement = false;
    private static boolean freeAccesPlanTraitement = false;

    static {
        chargerConfiguration();
    }

    private static void chargerConfiguration() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
                freeDiagnosticsParHeure = Integer.parseInt(properties.getProperty("free.diagnostics.par.heure", "3"));
                freeAccesTraitement = Boolean.parseBoolean(properties.getProperty("free.acces.traitement", "false"));
                freeAccesPlanTraitement = Boolean
                        .parseBoolean(properties.getProperty("free.acces.plan.traitement", "false"));
            } catch (IOException | NumberFormatException e) {
                System.err.println("Erreur chargement config: " + e.getMessage());
            }
        }
    }

    public static void sauvegarderConfiguration() {
        properties.setProperty("free.diagnostics.par.heure", String.valueOf(freeDiagnosticsParHeure));
        properties.setProperty("free.acces.traitement", String.valueOf(freeAccesTraitement));
        properties.setProperty("free.acces.plan.traitement", String.valueOf(freeAccesPlanTraitement));

        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "Configuration des prix d'abonnement Ardhi");
        } catch (IOException e) {
            System.err.println("Erreur sauvegarde config: " + e.getMessage());
        }
    }

    // Getters
    public static int getFreeDiagnosticsParHeure() {
        return freeDiagnosticsParHeure;
    }

    public static boolean isFreeAccesTraitement() {
        return freeAccesTraitement;
    }

    public static boolean isFreeAccesPlanTraitement() {
        return freeAccesPlanTraitement;
    }

    // Setters (pour l'admin)
    public static void setFreeDiagnosticsParHeure(int limit) {
        freeDiagnosticsParHeure = limit;
        sauvegarderConfiguration();
    }

    public static void setFreeAccesTraitement(boolean acces) {
        freeAccesTraitement = acces;
        sauvegarderConfiguration();
    }

    public static void setFreeAccesPlanTraitement(boolean acces) {
        freeAccesPlanTraitement = acces;
        sauvegarderConfiguration();
    }
}