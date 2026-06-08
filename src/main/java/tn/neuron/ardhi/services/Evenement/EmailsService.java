package tn.neuron.ardhi.services.Evenement;

import javax.mail.*;
import javax.mail.internet.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import tn.neuron.ardhi.models.Evenement.Evenement;
import tn.neuron.ardhi.models.Evenement.Participation;
import tn.neuron.ardhi.utils.UserAndDiag.AppConfig;

/**
 * Service pour l'envoi automatisé d'emails de rappel pour les événements
 */
public class EmailsService {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String EMAIL_FROM = AppConfig.get("email.evenement.username");
    private static final String EMAIL_PASSWORD = AppConfig.get("email.evenement.password");

    private Properties properties;
    private EvenementService evenementService;
    private ParticipationService participationService;

    public EmailsService() {
        this.evenementService = new EvenementService();
        this.participationService = new ParticipationService();
        setupEmailProperties();
    }

    private void setupEmailProperties() {
        properties = new Properties();
        properties.put("mail.smtp.host", SMTP_HOST);
        properties.put("mail.smtp.port", SMTP_PORT);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.ssl.trust", "smtp.gmail.com");
    }

    /**
     * Envoie un email de confirmation d'inscription
     */
    public boolean envoyerConfirmationInscription(Participation participation, Evenement evenement) {
        String sujet = "✅ Confirmation d'inscription - " + evenement.getTitre();

        String contenu = construireEmailConfirmation(participation, evenement);

        return envoyerEmail(participation.getEmailUtilisateur(), sujet, contenu);
    }

    /**
     * Envoie un rappel 3 jours avant l'événement
     */
    public int envoyerRappelsEvenement(Evenement evenement, int joursAvant) {
        List<Participation> participants = participationService.getParticipationsByEvenement(evenement.getId());
        int count = 0;

        for (Participation p : participants) {
            if ("CONFIRME".equals(p.getStatut())) {
                String sujet = "🔔 Rappel: " + evenement.getTitre() + " dans " + joursAvant + " jours";
                String contenu = construireEmailRappel(p, evenement, joursAvant);

                if (envoyerEmail(p.getEmailUtilisateur(), sujet, contenu)) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Envoie un email de demande d'avis après l'événement
     */
    public int envoyerDemandeAvis(Evenement evenement) {
        List<Participation> participants = participationService.getParticipationsByEvenement(evenement.getId());
        int count = 0;

        for (Participation p : participants) {
            if ("PRESENT".equals(p.getStatut()) && p.getNote() == 0) {
                String sujet = "⭐ Votre avis sur " + evenement.getTitre();
                String contenu = construireEmailDemandeAvis(p, evenement);

                if (envoyerEmail(p.getEmailUtilisateur(), sujet, contenu)) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Système automatisé: Vérifie et envoie les rappels pour tous les événements à
     * venir
     */
    public Map<String, Integer> envoyerRappelsAutomatiques() {
        Map<String, Integer> resultats = new HashMap<>();
        int rappels3jours = 0;
        int rappels1jour = 0;

        List<Evenement> evenementsAVenir = evenementService.getEvenementsByStatut("A_VENIR");

        for (Evenement evenement : evenementsAVenir) {
            LocalDate aujourdhui = LocalDate.now();
            long joursAvant = ChronoUnit.DAYS.between(aujourdhui, evenement.getDateDebut());

            // Rappel 3 jours avant
            if (joursAvant == 3) {
                rappels3jours += envoyerRappelsEvenement(evenement, 3);
            }
            // Rappel 1 jour avant
            else if (joursAvant == 1) {
                rappels1jour += envoyerRappelsEvenement(evenement, 1);
            }
            // Rappel le jour même (optionnel)
            else if (joursAvant == 0) {
                rappels1jour += envoyerRappelsEvenement(evenement, 0);
            }
        }

        resultats.put("rappels3jours", rappels3jours);
        resultats.put("rappels1jour", rappels1jour);

        return resultats;
    }

    /**
     * Envoie un email de changement de statut
     */
    public boolean envoyerNotificationStatut(Participation participation, Evenement evenement, String nouveauStatut) {
        String sujet = "";
        String contenu = "";

        switch (nouveauStatut) {
            case "CONFIRME":
                sujet = "✅ Inscription confirmée - " + evenement.getTitre();
                contenu = construireEmailValidation(participation, evenement);
                break;
            case "EN_ATTENTE":
                sujet = "⏳ Mise en liste d'attente - " + evenement.getTitre();
                contenu = construireEmailAttente(participation, evenement);
                break;
            case "ANNULE":
                sujet = "❌ Inscription annulée - " + evenement.getTitre();
                contenu = construireEmailAnnulation(participation, evenement);
                break;
        }

        return envoyerEmail(participation.getEmailUtilisateur(), sujet, contenu);
    }

    /**
     * Méthode générique d'envoi d'email
     */
    public boolean envoyerEmail(String destinataire, String sujet, String contenu) {
        try {
            Session session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM, "ARDHI Platform"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
            message.setSubject(sujet);
            message.setContent(contenu, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("✓ Email envoyé à: " + destinataire);
            return true;

        } catch (Exception e) {
            System.err.println("✗ Erreur envoi email à " + destinataire + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ============ Construction des emails HTML ============

    private String construireEmailConfirmation(Participation p, Evenement e) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        return String.format(
                """
                        <html>
                        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                            <div style="max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f5f5f0;">
                                <div style="background-color: #6b7a4f; color: white; padding: 20px; text-align: center;">
                                    <h1 style="margin: 0;">🌾 ARDHI</h1>
                                    <p style="margin: 5px 0;">Plateforme Agricole Intelligente</p>
                                </div>

                                <div style="background-color: white; padding: 30px; margin-top: 20px;">
                                    <h2 style="color: #6b7a4f;">Confirmation d'inscription</h2>
                                    <p>Bonjour <strong>%s</strong>,</p>
                                    <p>Votre inscription à l'événement suivant a bien été prise en compte :</p>

                                    <div style="background-color: #f5f5f0; padding: 15px; margin: 20px 0; border-left: 4px solid #6b7a4f;">
                                        <h3 style="margin-top: 0; color: #6b7a4f;">%s</h3>
                                        <p><strong>📅 Date :</strong> %s</p>
                                        <p><strong>📍 Lieu :</strong> %s</p>
                                        <p><strong>🎪 Type :</strong> %s</p>
                                        <p><strong>👤 Organisateur :</strong> %s</p>
                                    </div>

                                    <p><strong>Statut de votre inscription :</strong> <span style="color: #50c878;">✓ CONFIRMÉE</span></p>
                                    %s

                                    <p>Vous recevrez un rappel quelques jours avant l'événement.</p>
                                    <p>À bientôt !</p>
                                </div>

                                <div style="text-align: center; padding: 20px; color: #666; font-size: 12px;">
                                    <p>© 2026 ARDHI - Tous droits réservés</p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                p.getNomComplet(),
                e.getTitre(),
                e.getDateDebut().format(formatter),
                e.getLieu(),
                e.getType(),
                e.getOrganisateur(),
                p.getCommentaire() != null && !p.getCommentaire().isEmpty()
                        ? "<p><strong>Votre commentaire :</strong> <em>" + p.getCommentaire() + "</em></p>"
                        : "");
    }

    private String construireEmailRappel(Participation p, Evenement e, int joursAvant) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");
        String dateStr = e.getDateDebut().atStartOfDay().format(formatter);

        String rappelText = joursAvant == 0 ? "C'est aujourd'hui !"
                : joursAvant == 1 ? "C'est demain !" : "Dans " + joursAvant + " jours";

        return String.format(
                """
                        <html>
                        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                            <div style="max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f5f5f0;">
                                <div style="background-color: #f5a623; color: white; padding: 20px; text-align: center;">
                                    <h1 style="margin: 0;">🔔 RAPPEL</h1>
                                </div>

                                <div style="background-color: white; padding: 30px; margin-top: 20px;">
                                    <h2 style="color: #f5a623;">%s</h2>
                                    <p>Bonjour <strong>%s</strong>,</p>
                                    <p>Nous vous rappelons que vous êtes inscrit(e) à l'événement :</p>

                                    <div style="background-color: #fff4e6; padding: 20px; margin: 20px 0; border-left: 4px solid #f5a623;">
                                        <h3 style="margin-top: 0; color: #f5a623;">%s</h3>
                                        <p><strong>📅 Date et heure :</strong> %s</p>
                                        <p><strong>📍 Lieu :</strong> %s</p>
                                    </div>

                                    <p>N'oubliez pas de vous y rendre !</p>
                                    <p>À très bientôt ! 🌾</p>
                                </div>

                                <div style="text-align: center; padding: 20px; color: #666; font-size: 12px;">
                                    <p>© 2026 ARDHI</p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                rappelText,
                p.getNomComplet(),
                e.getTitre(),
                dateStr,
                e.getLieu());
    }

    private String construireEmailDemandeAvis(Participation p, Evenement e) {
        return String.format(
                """
                        <html>
                        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                            <div style="max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f5f5f0;">
                                <div style="background-color: #6b7a4f; color: white; padding: 20px; text-align: center;">
                                    <h1 style="margin: 0;">⭐ VOTRE AVIS COMPTE</h1>
                                </div>

                                <div style="background-color: white; padding: 30px; margin-top: 20px;">
                                    <p>Bonjour <strong>%s</strong>,</p>
                                    <p>Merci d'avoir participé à l'événement <strong>%s</strong> !</p>
                                    <p>Votre avis nous intéresse beaucoup. Pourriez-vous prendre quelques instants pour évaluer cet événement ?</p>

                                    <div style="text-align: center; margin: 30px 0;">
                                        <p style="font-size: 18px;">Évaluez de 1 à 5 étoiles ⭐</p>
                                        <p style="color: #666; font-size: 14px;">Connectez-vous à ARDHI pour donner votre avis</p>
                                    </div>

                                    <p>Votre retour nous aide à améliorer la qualité de nos événements.</p>
                                    <p>Merci et à bientôt sur ARDHI ! 🌾</p>
                                </div>

                                <div style="text-align: center; padding: 20px; color: #666; font-size: 12px;">
                                    <p>© 2026 ARDHI</p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                p.getNomComplet(),
                e.getTitre());
    }

    private String construireEmailValidation(Participation p, Evenement e) {
        return "<html><body><p>Votre participation a été validée pour " + e.getTitre() + "</p></body></html>";
    }

    private String construireEmailAttente(Participation p, Evenement e) {
        return "<html><body><p>Votre participation est en liste d'attente pour " + e.getTitre() + "</p></body></html>";
    }

    private String construireEmailAnnulation(Participation p, Evenement e) {
        return "<html><body><p>Votre participation a été annulée pour " + e.getTitre() + "</p></body></html>";
    }
}