package tn.neuron.ardhi.services.MaterielEtMaintenance;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import tn.neuron.ardhi.utils.UserAndDiag.AppConfig;

public class WhatsAppService {

    private static final String ACCOUNT_SID  = AppConfig.get("twilio.maintenance.account.sid");
    private static final String AUTH_TOKEN   = AppConfig.get("twilio.maintenance.auth.token");

    // Numéro Twilio Sandbox WhatsApp (ne pas changer pour le sandbox)
    private static final String FROM_WHATSAPP = "whatsapp:+14155238886";

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    private boolean initialized = false;

    // ════════════════════════════════════════════════════════
    //  CONSTRUCTEUR
    // ════════════════════════════════════════════════════════

    public WhatsAppService() {
        try {
            Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
            this.initialized = true;
            System.out.println("WhatsApp Service Twilio initialise");
        } catch (Exception e) {
            System.err.println("Erreur init Twilio: " + e.getMessage());
            this.initialized = false;
        }
    }

    // ════════════════════════════════════════════════════════
    //  CONFIRMATION — envoyée immédiatement après planification
    // ════════════════════════════════════════════════════════

    /**
     * Envoie un message WhatsApp de confirmation de maintenance
     *
     * @param numeroTelephone  numéro de l'agriculteur (ex: +21612345678)
     * @param nomAgriculteur   nom de l'agriculteur
     * @param nomMateriel      nom du matériel
     * @param typeMaintenance  type (Préventive, Corrective...)
     * @param dateTime         date et heure de la maintenance
     * @param description      description des travaux
     * @return true si envoyé avec succès
     */
    public boolean envoyerConfirmation(String numeroTelephone,
                                       String nomAgriculteur,
                                       String nomMateriel,
                                       String typeMaintenance,
                                       LocalDateTime dateTime,
                                       String description) {
        if (!initialized) {
            System.err.println("Twilio non initialise - confirmation non envoyee");
            return false;
        }

        try {
            String toWhatsApp = formaterNumero(numeroTelephone);
            String message    = construireMessageConfirmation(
                    nomAgriculteur, nomMateriel, typeMaintenance, dateTime, description);

            Message msg = Message.creator(
                    new PhoneNumber(toWhatsApp),
                    new PhoneNumber(FROM_WHATSAPP),
                    message
            ).create();

            System.out.println("WhatsApp confirmation envoye - SID: " + msg.getSid());
            System.out.println("Statut: " + msg.getStatus());
            return true;

        } catch (Exception e) {
            System.err.println("Erreur envoi WhatsApp confirmation: " + e.getMessage());
            return false;
        }
    }

    // ════════════════════════════════════════════════════════
    //  RAPPEL 24H — à appeler via un scheduler ou manuellement
    // ════════════════════════════════════════════════════════

    /**
     * Envoie un message WhatsApp de rappel 24h avant la maintenance
     */
    public boolean envoyerRappel24h(String numeroTelephone,
                                    String nomAgriculteur,
                                    String nomMateriel,
                                    String typeMaintenance,
                                    LocalDateTime dateTime) {
        if (!initialized) {
            System.err.println("Twilio non initialise - rappel non envoye");
            return false;
        }

        try {
            String toWhatsApp = formaterNumero(numeroTelephone);
            String message    = construireMessageRappel(
                    nomAgriculteur, nomMateriel, typeMaintenance, dateTime);

            Message msg = Message.creator(
                    new PhoneNumber(toWhatsApp),
                    new PhoneNumber(FROM_WHATSAPP),
                    message
            ).create();

            System.out.println("WhatsApp rappel envoye - SID: " + msg.getSid());
            return true;

        } catch (Exception e) {
            System.err.println("Erreur envoi WhatsApp rappel: " + e.getMessage());
            return false;
        }
    }

    // ════════════════════════════════════════════════════════
    //  MESSAGES
    // ════════════════════════════════════════════════════════

    private String construireMessageConfirmation(String nomAgriculteur,
                                                 String nomMateriel,
                                                 String typeMaintenance,
                                                 LocalDateTime dateTime,
                                                 String description) {
        return "*ARDHI - Confirmation Maintenance*\n\n"
                + "Bonjour " + nomAgriculteur + ",\n\n"
                + "Votre maintenance a ete planifiee avec succes !\n\n"
                + "*Details :*\n"
                + "Materiel    : " + nomMateriel + "\n"
                + "Type        : " + typeMaintenance + "\n"
                + "Date        : " + dateTime.format(FORMATTER) + "\n"
                + "Duree       : 2 heures\n\n"
                + "*Travaux prevus :*\n"
                + description + "\n\n"
                + "Un rappel vous sera envoye 24h avant.\n\n"
                + "_Plateforme ARDHI - Gestion Agricole_";
    }

    private String construireMessageRappel(String nomAgriculteur,
                                           String nomMateriel,
                                           String typeMaintenance,
                                           LocalDateTime dateTime) {
        return "*ARDHI - Rappel Maintenance DEMAIN*\n\n"
                + "Bonjour " + nomAgriculteur + ",\n\n"
                + "Rappel : vous avez une maintenance prevue demain !\n\n"
                + "*Details :*\n"
                + "Materiel    : " + nomMateriel + "\n"
                + "Type        : " + typeMaintenance + "\n"
                + "Date        : " + dateTime.format(FORMATTER) + "\n"
                + "Duree       : 2 heures\n\n"
                + "Pensez a preparer le materiel avant l'intervention.\n\n"
                + "_Plateforme ARDHI - Gestion Agricole_";
    }

    // ════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ════════════════════════════════════════════════════════

    /**
     * Formate le numéro pour WhatsApp
     * Ex: "21612345678" → "whatsapp:+21612345678"
     *     "+21612345678" → "whatsapp:+21612345678"
     *     "0612345678" → "whatsapp:+33612345678" (France)
     */
    private String formaterNumero(String numero) {
        // Nettoyer le numéro
        String clean = numero.replaceAll("[\\s\\-\\(\\)]", "");

        // Ajouter le préfixe whatsapp:
        if (clean.startsWith("whatsapp:")) {
            return clean;
        } else if (clean.startsWith("+")) {
            return "whatsapp:" + clean;
        } else if (clean.startsWith("00")) {
            return "whatsapp:+" + clean.substring(2);
        } else {
            // Tunisie par défaut (+216)
            return "whatsapp:+216" + clean;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}