package tn.neuron.ardhi.services.marketplace;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import tn.neuron.ardhi.utils.UserAndDiag.AppConfig;

/**
 * Service d'envoi de notifications WhatsApp via Twilio.
 * Utilisé pour alerter les utilisateurs lorsqu'un produit
 * de leur wishlist est modifié (prix, stock, promotion).
 */
public class WhatsAppService {

    // Loaded from config.properties — see config.properties.example
    private static final String ACCOUNT_SID = AppConfig.get("twilio.marketplace.account.sid");
    private static final String AUTH_TOKEN  = AppConfig.get("twilio.marketplace.auth.token");

    /** Numéro sandbox Twilio WhatsApp (ou votre numéro approuvé) */
    private static final String TWILIO_WHATSAPP_NUMBER = "whatsapp:+14155238886";

    // ── Initialisation Twilio (une seule fois) ────────────────────────────────
    static {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
    }

    // ── Méthode principale d'envoi ────────────────────────────────────────────

    /**
     * Envoie un message WhatsApp à un destinataire.
     *
     * @param toNumber Numéro du destinataire au format international (ex: "+21612345678")
     * @param message  Contenu du message à envoyer
     * @return true si l'envoi a réussi, false sinon
     */
    public boolean sendWhatsApp(String toNumber, String message) {
        if (toNumber == null || toNumber.isBlank()) {
            System.err.println("[WhatsAppService] Numéro destinataire invalide ou vide.");
            return false;
        }

        try {
            // Formatage du numéro : whatsapp:+216XXXXXXXX
            String formattedNumber = formatNumber(toNumber);

            Message msg = Message.creator(
                    new PhoneNumber(formattedNumber),      // To
                    new PhoneNumber(TWILIO_WHATSAPP_NUMBER), // From
                    message
            ).create();

            System.out.println("[WhatsAppService] ✅ Message envoyé à " + formattedNumber
                    + " | SID : " + msg.getSid());
            return true;

        } catch (Exception e) {
            System.err.println("[WhatsAppService] ❌ Échec envoi WhatsApp à " + toNumber
                    + " : " + e.getMessage());
            return false;
        }
    }

    // ── Utilitaire : formatage du numéro ──────────────────────────────────────
    private String formatNumber(String number) {
        // Déjà formaté pour WhatsApp
        if (number.startsWith("whatsapp:")) {
            return number;
        }
        // Ajouter "+" si absent
        if (!number.startsWith("+")) {
            number = "+" + number;
        }
        return "whatsapp:" + number;
    }
}