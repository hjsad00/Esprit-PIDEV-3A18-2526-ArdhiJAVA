package tn.neuron.ardhi.services.MaterielEtMaintenance;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import tn.neuron.ardhi.utils.UserAndDiag.AppConfig;

/**
 * Service SMTP pour l'envoi d'emails de confirmation de maintenance.
 * Utilise javax.mail (déjà dans le pom.xml : com.sun.mail:javax.mail:1.6.2)
 *
 * Configuration : Gmail SMTP avec TLS
 * L'email émetteur doit avoir "Mots de passe d'application" activé dans Google Account.
 */
public class EmailService {

    // ── Configuration SMTP ────────────────────────────────────
    // Loaded from config.properties
    private static final String SMTP_HOST     = "smtp.gmail.com";
    private static final int    SMTP_PORT     = 587;
    private static final String SMTP_USER     = AppConfig.get("email.maintenance.username");
    private static final String SMTP_PASSWORD = AppConfig.get("email.maintenance.password");

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HEURE_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    // ════════════════════════════════════════════════════════
    //  MÉTHODE PRINCIPALE : Envoyer email de confirmation
    // ════════════════════════════════════════════════════════

    /**
     * Envoie un email de confirmation de maintenance à l'agriculteur.
     *
     * @param destinataire  Email de l'agriculteur
     * @param nomAgriculteur Prénom/Nom de l'agriculteur
     * @param nomMateriel   Nom du matériel concerné
     * @param typeMateriel  Type du matériel (Tracteur, Semoir, etc.)
     * @param etatMateriel  État du matériel
     * @param typeMaintenance Type de maintenance (Préventive, Corrective, etc.)
     * @param dateTime      Date et heure du rendez-vous
     * @param description   Description des travaux
     * @param pdfFile       Fichier PDF à joindre (peut être null)
     * @return true si envoi réussi
     */
    public boolean envoyerConfirmationMaintenance(
            String destinataire,
            String nomAgriculteur,
            String nomMateriel,
            String typeMateriel,
            String etatMateriel,
            String typeMaintenance,
            LocalDateTime dateTime,
            String description,
            File pdfFile) {

        try {
            Session session = creerSession();

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_USER, "ARDHI - Gestion Agricole"));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(destinataire));
            message.setSubject("Confirmation de maintenance - " + nomMateriel
                    + " | " + dateTime.format(DATE_FORMAT), "UTF-8");

            // Corps du message (HTML + PDF en pièce jointe)
            MimeMultipart multipart = new MimeMultipart();

            // Partie HTML
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(construireCorpsHtml(
                            nomAgriculteur, nomMateriel, typeMateriel,
                            etatMateriel, typeMaintenance, dateTime, description),
                    "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);

            // Pièce jointe PDF (si disponible)
            if (pdfFile != null && pdfFile.exists()) {
                MimeBodyPart pdfPart = new MimeBodyPart();
                pdfPart.attachFile(pdfFile);
                pdfPart.setFileName(MimeUtility.encodeText(
                        "Confirmation_Maintenance_" + nomMateriel + "_"
                                + dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf",
                        "UTF-8", null));
                multipart.addBodyPart(pdfPart);
            }

            message.setContent(multipart);

            Transport.send(message);

            System.out.println("✅ Email de confirmation envoyé à : " + destinataire);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ════════════════════════════════════════════════════════
    //  CORPS HTML DE L'EMAIL
    // ════════════════════════════════════════════════════════

    private String construireCorpsHtml(
            String nomAgriculteur,
            String nomMateriel,
            String typeMateriel,
            String etatMateriel,
            String typeMaintenance,
            LocalDateTime dateTime,
            String description) {

        String dateStr  = dateTime.format(DATE_FORMAT);
        String heureStr = dateTime.format(HEURE_FORMAT);
        String dateTimeStr = dateTime.format(DATETIME_FORMAT);

        return "<!DOCTYPE html>" +
                "<html lang='fr'>" +
                "<head><meta charset='UTF-8'/>" +
                "<style>" +
                "  body { font-family: Arial, sans-serif; background:#f4f6f0; margin:0; padding:0; }" +
                "  .container { max-width:620px; margin:30px auto; background:white; border-radius:12px;" +
                "               overflow:hidden; box-shadow:0 4px 20px rgba(0,0,0,0.1); }" +
                "  .header { background:linear-gradient(135deg,#6B7F3F,#4A5A2B); padding:35px 30px;" +
                "            text-align:center; }" +
                "  .header h1 { color:white; margin:0; font-size:26px; letter-spacing:1px; }" +
                "  .header p  { color:rgba(255,255,255,0.85); margin:8px 0 0; font-size:14px; }" +
                "  .badge { display:inline-block; background:#4CAF50; color:white; padding:6px 18px;" +
                "           border-radius:20px; font-size:13px; margin-top:12px; font-weight:bold; }" +
                "  .body { padding:30px; }" +
                "  .greeting { font-size:18px; color:#2d3748; margin-bottom:20px; }" +
                "  .card { background:#f7fafc; border-left:4px solid #6B7F3F; border-radius:8px;" +
                "          padding:20px 24px; margin:20px 0; }" +
                "  .card h3 { margin:0 0 15px; color:#4A5A2B; font-size:15px; }" +
                "  .detail-row { display:flex; justify-content:space-between; padding:8px 0;" +
                "                border-bottom:1px solid #e2e8f0; font-size:14px; }" +
                "  .detail-row:last-child { border-bottom:none; }" +
                "  .detail-label { color:#718096; font-weight:bold; min-width:160px; }" +
                "  .detail-value { color:#2d3748; text-align:right; }" +
                "  .highlight { background:linear-gradient(135deg,#FFF3CD,#FFEAA7); border-left:4px solid #FF9800;" +
                "               border-radius:8px; padding:18px 24px; margin:20px 0; }" +
                "  .highlight h3 { margin:0 0 8px; color:#E65100; font-size:15px; }" +
                "  .highlight .datetime { font-size:22px; font-weight:bold; color:#BF360C; }" +
                "  .desc-box { background:#EBF8FF; border-radius:8px; padding:16px 20px; margin:20px 0;" +
                "              font-size:13px; color:#2d3748; line-height:1.6; }" +
                "  .info-box { background:#E8F5E9; border-radius:8px; padding:16px 20px; margin:20px 0; }" +
                "  .info-box p { margin:5px 0; font-size:13px; color:#2E7D32; }" +
                "  .cta { text-align:center; margin:25px 0; }" +
                "  .btn { display:inline-block; background:#6B7F3F; color:white; padding:13px 32px;" +
                "         border-radius:25px; text-decoration:none; font-weight:bold; font-size:14px;" +
                "         letter-spacing:0.5px; }" +
                "  .footer { background:#f7fafc; padding:20px 30px; text-align:center;" +
                "            border-top:1px solid #e2e8f0; }" +
                "  .footer p { margin:4px 0; font-size:12px; color:#a0aec0; }" +
                "  .footer strong { color:#6B7F3F; }" +
                "</style></head>" +
                "<body><div class='container'>" +

                // Header
                "<div class='header'>" +
                "  <h1>&#x1F33E; ARDHI</h1>" +
                "  <p>Plateforme de Gestion Agricole Intelligente</p>" +
                "  <span class='badge'>&#x2705; Maintenance Confirmée</span>" +
                "</div>" +

                // Corps
                "<div class='body'>" +
                "  <p class='greeting'>Bonjour <strong>" + escapeHtml(nomAgriculteur) + "</strong>,</p>" +
                "  <p style='color:#4a5568;font-size:14px;line-height:1.7;'>" +
                "    Nous avons le plaisir de vous confirmer que votre rendez-vous de maintenance" +
                "    a été enregistré avec succès sur votre Google Calendar." +
                "    Vous trouverez ci-dessous tous les détails de votre intervention." +
                "  </p>" +

                // Date mise en avant
                "  <div class='highlight'>" +
                "    <h3>&#x1F4C5; Rendez-vous planifié</h3>" +
                "    <div class='datetime'>" + dateStr + " &agrave; " + heureStr + "</div>" +
                "    <p style='margin:6px 0 0;font-size:13px;color:#795548;'>Durée estimée : 2 heures</p>" +
                "  </div>" +

                // Détails matériel
                "  <div class='card'>" +
                "    <h3>&#x1F527; Détails du Matériel</h3>" +
                "    <div class='detail-row'><span class='detail-label'>Matériel :</span>" +
                "      <span class='detail-value'>" + escapeHtml(nomMateriel) + "</span></div>" +
                "    <div class='detail-row'><span class='detail-label'>Type :</span>" +
                "      <span class='detail-value'>" + escapeHtml(typeMateriel) + "</span></div>" +
                "    <div class='detail-row'><span class='detail-label'>État actuel :</span>" +
                "      <span class='detail-value'>" + escapeHtml(etatMateriel) + "</span></div>" +
                "    <div class='detail-row'><span class='detail-label'>Type de maintenance :</span>" +
                "      <span class='detail-value'>" + escapeHtml(typeMaintenance) + "</span></div>" +
                "    <div class='detail-row'><span class='detail-label'>Date d'intervention :</span>" +
                "      <span class='detail-value'>" + dateTimeStr + "</span></div>" +
                "  </div>" +

                // Description
                "  <div class='desc-box'>" +
                "    <strong>&#x1F4DD; Description des travaux :</strong><br/><br/>" +
                escapeHtml(description).replace("\n", "<br/>") +
                "  </div>" +

                // Rappels
                "  <div class='info-box'>" +
                "    <p><strong>&#x1F514; Rappels automatiques programmés :</strong></p>" +
                "    <p>&#x2022; Email de rappel : 24h avant le rendez-vous</p>" +
                "    <p>&#x2022; Notification popup : 1h avant le rendez-vous</p>" +
                "    <p>&#x2022; L'événement est visible dans votre Google Calendar</p>" +
                "  </div>" +

                "  <p style='color:#718096;font-size:13px;line-height:1.7;'>" +
                "    Le PDF de confirmation est joint à cet email. Il contient tous les détails" +
                "    de votre maintenance et peut être conservé pour vos archives." +
                "  </p>" +

                // Bienvenue
                "  <div style='text-align:center;padding:20px;background:#f0f4e8;border-radius:8px;margin:20px 0;'>" +
                "    <p style='font-size:16px;color:#4A5A2B;font-weight:bold;margin:0;'>" +
                "      Nous vous souhaitons la bienvenue et une excellente maintenance !</p>" +
                "    <p style='font-size:13px;color:#718096;margin:8px 0 0;'>" +
                "      L'équipe ARDHI reste disponible pour toute question.</p>" +
                "  </div>" +

                "</div>" +

                // Footer
                "<div class='footer'>" +
                "  <p><strong>ARDHI</strong> - Plateforme de Gestion Agricole</p>" +
                "  <p>Cet email a été envoyé automatiquement suite à votre prise de rendez-vous.</p>" +
                "  <p>&#x1F4E7; " + SMTP_USER + "</p>" +
                "</div>" +

                "</div></body></html>";
    }

    // ════════════════════════════════════════════════════════
    //  CONFIGURATION SESSION SMTP
    // ════════════════════════════════════════════════════════

    private Session creerSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(SMTP_PORT));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust", SMTP_HOST);
        props.put("mail.mime.charset", "UTF-8");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASSWORD);
            }
        });
    }

    // ════════════════════════════════════════════════════════
    //  UTILITAIRE
    // ════════════════════════════════════════════════════════

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
