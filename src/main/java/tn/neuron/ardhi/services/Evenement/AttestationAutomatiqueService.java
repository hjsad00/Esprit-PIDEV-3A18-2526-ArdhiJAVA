package tn.neuron.ardhi.services.Evenement;

import tn.neuron.ardhi.models.Evenement.Evenement;
import tn.neuron.ardhi.models.Evenement.Participation;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.*;

import tn.neuron.ardhi.utils.UserAndDiag.AppConfig;

/**
 * Service automatique d'envoi d'attestations
 * FIXÉ: Utilise maintenant AttestationPDFService pour générer de vrais PDFs
 */
public class AttestationAutomatiqueService {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String EMAIL_FROM = AppConfig.get("email.evenement.username");
    private static final String EMAIL_PASSWORD = AppConfig.get("email.evenement.password");

    private Properties properties;
    private EvenementService evenementService;
    private ParticipationService participationService;
    private AttestationPDFService pdfService; // ✅ AJOUTÉ

    public AttestationAutomatiqueService() {
        this.evenementService = new EvenementService();
        this.participationService = new ParticipationService();
        this.pdfService = new AttestationPDFService(); // ✅ INITIALISÉ
        setupEmailProperties();
    }

    private void setupEmailProperties() {
        properties = new Properties();
        properties.put("mail.smtp.host", SMTP_HOST);
        properties.put("mail.smtp.port", SMTP_PORT);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.ssl.trust", SMTP_HOST);
        properties.put("mail.smtp.ssl.protocols", "TLSv1.2");
    }

    public Map<String, Integer> envoyerAttestationsAutomatiques() {
        Map<String, Integer> stats = new HashMap<>();
        int emailsEnvoyes = 0;
        int erreurs = 0;

        List<Evenement> evenementsTermines = evenementService.getEvenementsByStatut("TERMINE");

        System.out.println("\n🔄 Vérification des attestations à envoyer...");
        System.out.println("📊 " + evenementsTermines.size() + " événements terminés trouvés");

        for (Evenement evenement : evenementsTermines) {
            List<Participation> participants = participationService.getParticipationsByEvenement(evenement.getId());

            for (Participation participant : participants) {
                if ("PRESENT".equals(participant.getStatut()) && !participant.isAttestationEnvoyee()) {

                    System.out.println("📧 Envoi attestation pour: " + participant.getNomComplet());

                    if (envoyerAttestationAvecAvis(evenement, participant)) {
                        participationService.marquerAttestationEnvoyee(participant.getId());
                        emailsEnvoyes++;
                        System.out.println("   ✅ Envoyé avec succès");
                    } else {
                        erreurs++;
                        System.out.println("   ❌ Erreur d'envoi");
                    }
                }
            }
        }

        stats.put("envoyes", emailsEnvoyes);
        stats.put("erreurs", erreurs);

        System.out.println("\n📈 Résultats:");
        System.out.println("   ✅ Emails envoyés: " + emailsEnvoyes);
        System.out.println("   ❌ Erreurs: " + erreurs);

        return stats;
    }

    private boolean envoyerAttestationAvecAvis(Evenement evenement, Participation participant) {
        try {
            // ✅ FIXÉ: Utilise AttestationPDFService pour générer un VRAI PDF
            File pdfFile = genererPDFAttestation(evenement, participant);

            if (pdfFile == null || !pdfFile.exists()) {
                System.err.println("❌ Impossible de générer le PDF");
                return false;
            }

            String sujet = "✅ Attestation de participation - " + evenement.getTitre();
            String contenu = construireEmailHTML(participant, evenement);

            return envoyerEmailAvecPieceJointe(
                    participant.getEmailUtilisateur(),
                    sujet,
                    contenu,
                    pdfFile
            );

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi attestation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ✅ FIXÉ: Génère un VRAI PDF avec AttestationPDFService
     */
    private File genererPDFAttestation(Evenement evenement, Participation participant) {
        try {
            String fileName = "attestation_" + evenement.getId() + "_" + participant.getId() + ".pdf";
            String fullPath = System.getProperty("java.io.tmpdir") + File.separator + fileName;

            System.out.println("📄 Génération PDF: " + fullPath);

            // Utiliser AttestationPDFService existant
            boolean success = pdfService.genererAttestation(participant, evenement, fullPath);

            if (success) {
                File pdfFile = new File(fullPath);
                System.out.println("✅ PDF généré: " + pdfFile.length() + " bytes");
                return pdfFile;
            } else {
                System.err.println("❌ Échec génération PDF");
                return null;
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur génération PDF: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String construireEmailHTML(Participation participant, Evenement evenement) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f5f5f0;">
                    <div style="background-color: #6b7a4f; color: white; padding: 30px; text-align: center;">
                        <h1 style="margin: 0; font-size: 28px;">🎓 ATTESTATION DE PARTICIPATION</h1>
                        <p style="margin: 10px 0 0 0; font-size: 14px;">ARDHI - Plateforme Agricole</p>
                    </div>

                    <div style="background-color: white; padding: 40px; margin-top: 20px;">
                        <p style="font-size: 16px;">Bonjour <strong>%s</strong>,</p>
                        
                        <p>Félicitations pour votre participation à l'événement :</p>

                        <div style="background-color: #f0f8f0; padding: 20px; margin: 25px 0; border-left: 5px solid #6b7a4f; border-radius: 5px;">
                            <h2 style="margin-top: 0; color: #6b7a4f; font-size: 22px;">%s</h2>
                            <p style="margin: 8px 0;"><strong>📅 Date :</strong> %s</p>
                            <p style="margin: 8px 0;"><strong>📍 Lieu :</strong> %s</p>
                            <p style="margin: 8px 0;"><strong>🎪 Type :</strong> %s</p>
                            <p style="margin: 8px 0;"><strong>👤 Organisateur :</strong> %s</p>
                        </div>

                        <p style="background-color: #e8f5e9; padding: 15px; border-radius: 5px; text-align: center;">
                            <strong>✅ Votre attestation de participation est jointe à cet email (PDF)</strong>
                        </p>

                        <hr style="margin: 30px 0; border: none; border-top: 1px solid #e0e0e0;">

                        <div style="text-align: center; padding: 20px; background-color: #fff9e6; border-radius: 10px; margin: 20px 0;">
                            <h3 style="color: #f5a623; margin-top: 0;">⭐ VOTRE AVIS COMPTE !</h3>
                            <p>Aidez-nous à améliorer nos événements en partageant votre expérience.</p>
                            
                            <div style="margin: 25px 0;">
                                <p style="font-size: 18px; margin: 10px 0;">Comment évaluez-vous cet événement ?</p>
                                <div style="font-size: 32px; letter-spacing: 5px;">⭐ ⭐ ⭐ ⭐ ⭐</div>
                            </div>

                            <a href="mailto:%s?subject=Avis sur %s&body=Bonjour,%%0A%%0AMon avis sur l'événement:%%0A%%0ANote (1-5): %%0A%%0ACommentaires: %%0A%%0A"
                               style="display: inline-block; background-color: #6b7a4f; color: white; padding: 15px 40px; text-decoration: none; border-radius: 25px; font-weight: bold; margin-top: 15px;">
                                📝 Donner mon avis par email
                            </a>

                            <p style="font-size: 12px; color: #666; margin-top: 15px;">
                                Ou connectez-vous sur ARDHI pour laisser votre avis
                            </p>
                        </div>

                        <p style="margin-top: 30px;">Merci encore pour votre participation !</p>
                        <p>À très bientôt pour nos prochains événements. 🌾</p>

                        <p style="margin-top: 30px; font-style: italic; color: #666;">
                            L'équipe ARDHI
                        </p>
                    </div>

                    <div style="text-align: center; padding: 20px; color: #666; font-size: 12px;">
                        <p style="margin: 5px 0;">© 2026 ARDHI - Plateforme Agricole Intelligente</p>
                        <p style="margin: 5px 0;">Tous droits réservés</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                participant.getNomComplet(),
                evenement.getTitre(),
                evenement.getDateDebut().format(formatter),
                evenement.getLieu(),
                evenement.getType(),
                evenement.getOrganisateur(),
                EMAIL_FROM,
                evenement.getTitre()
        );
    }

    private boolean envoyerEmailAvecPieceJointe(String destinataire, String sujet, String contenu, File pieceJointe) {
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

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(contenu, "text/html; charset=utf-8");

            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.attachFile(pieceJointe);
            attachmentPart.setFileName("attestation_participation.pdf");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(htmlPart);
            multipart.addBodyPart(attachmentPart);

            message.setContent(multipart);

            Transport.send(message);
            System.out.println("✅ Email envoyé à: " + destinataire);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}