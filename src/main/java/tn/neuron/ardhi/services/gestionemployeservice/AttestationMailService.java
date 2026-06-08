package tn.neuron.ardhi.services.gestionemployeservice;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import tn.neuron.ardhi.models.gestionemployemodel.Employe;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;

import tn.neuron.ardhi.utils.UserAndDiag.AppConfig;

/**
 * Service d'envoi d'attestations de travail par email.
 *
 * ✅ PDF généré via iText — pièce jointe imprimable
 * ✅ Logo intégré dans le PDF et dans l'email (CID)
 * ✅ Mise en page A4 professionnelle
 */
public class AttestationMailService {

    // ── ⚙️ Configuration SMTP ──────────────────────────────────────────────
    private static final String SMTP_HOST     = "smtp.gmail.com";
    private static final int    SMTP_PORT     = 587;
    private static final String SMTP_USER     = AppConfig.get("email.attestation.username");
    private static final String SMTP_PASSWORD = AppConfig.get("email.attestation.password");

    // ── 🖼️ Chemin du logo dans le classpath ───────────────────────────────
    private static final String LOGO_RESOURCE_PATH = "/img/ardhi_logo.png";
    private static final String LOGO_CID = "logo_ardhi@ardhi.tn";

    // ── Couleurs PDF ───────────────────────────────────────────────────────
    private static final BaseColor PRIMARY = new BaseColor(26,  58,  92);  // bleu marine
    private static final BaseColor ACCENT  = new BaseColor(39, 174,  96);  // vert

    // ── 📧 Point d'entrée principal ────────────────────────────────────────

    /**
     * Envoie une attestation de travail officielle à l'employé donné.
     * L'email contient :
     *   - un corps HTML lisible dans la boîte de réception
     *   - une pièce jointe PDF imprimable générée avec iText
     *
     * @param employe L'employé destinataire
     * @throws AttestationMailException si l'envoi échoue
     */
    public void envoyerAttestation(Employe employe) throws AttestationMailException {
        try {
            byte[] logoBytes = chargerLogo();
            byte[] pdfBytes  = genererPdfBytes(employe, logoBytes);
            Session session  = creerSession();
            MimeMessage message = construireMessage(session, employe, logoBytes, pdfBytes);
            Transport.send(message);
            System.out.println("[Attestation] ✅ Email + PDF envoyé à : " + employe.getEmail());
        } catch (MessagingException | IOException | DocumentException e) {
            throw new AttestationMailException(
                    "Échec de l'envoi de l'attestation à " + employe.getEmail(), e
            );
        }
    }

    // ── 🔧 Chargement du logo ──────────────────────────────────────────────

    private byte[] chargerLogo() throws IOException {
        try (InputStream is = getClass().getResourceAsStream(LOGO_RESOURCE_PATH)) {
            if (is != null) return is.readAllBytes();
        }
        throw new IOException("Logo introuvable : " + LOGO_RESOURCE_PATH);
    }

    // ── 📄 Génération du PDF avec iText ───────────────────────────────────

    /**
     * Génère l'attestation de travail en PDF dans un ByteArrayOutputStream.
     * Retourne les octets du PDF prêts à être attachés à l'email.
     */
    private byte[] genererPdfBytes(Employe emp, byte[] logoBytes) throws DocumentException, IOException {

        String nomComplet     = emp.getPrenom() + " " + emp.getNom();
        String poste          = emp.getPoste() != null ? emp.getPoste() : "Employé";
        String dateAujourdhui = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH));
        String annee          = String.valueOf(LocalDate.now().getYear());
        String reference      = "ATT-" + annee + "-" + String.format("%04d", emp.getId());

        Font fontBody   = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.BLACK);
        Font fontBold   = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD,   PRIMARY);
        Font fontTitle  = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD,   PRIMARY);
        Font fontSmall  = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, BaseColor.GRAY);
        Font fontAccent = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,   ACCENT);
        Font fontHeader = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD,   BaseColor.WHITE);
        Font fontHeaderSub = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(180, 200, 220));

        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        document.open();

        // ── Bordure du certificat ──────────────────────────────────────────
        PdfContentByte canvas = writer.getDirectContent();
        Rectangle border = new Rectangle(20, 20,
                PageSize.A4.getWidth() - 20, PageSize.A4.getHeight() - 20);
        border.setBorder(Rectangle.BOX);
        border.setBorderWidth(3);
        border.setBorderColor(PRIMARY);
        canvas.rectangle(border);

        // ── Filigrane ──────────────────────────────────────────────────────
        PdfContentByte under = writer.getDirectContentUnder();
        Font wm = new Font(Font.FontFamily.HELVETICA, 70, Font.BOLD, new BaseColor(225, 225, 225));
        ColumnText.showTextAligned(under, Element.ALIGN_CENTER,
                new Phrase("ARDHI", wm), 297, 421, 45);

        // ── En-tête fond bleu (logo + titre société) ───────────────────────
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1f, 4f});
        headerTable.setSpacingAfter(0);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBackgroundColor(PRIMARY);
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setPadding(14);
        try {
            Image logo = Image.getInstance(logoBytes);
            logo.scaleToFit(56, 56);
            logoCell.setImage(logo);
        } catch (Exception ignored) {}
        headerTable.addCell(logoCell);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBackgroundColor(PRIMARY);
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        titleCell.setPadding(16);
        Paragraph hText = new Paragraph("Ardhi – Gestion Agricole\n", fontHeader);
        hText.add(new Chunk("Siège social : Tunis, Tunisie  |  contact@ardhi.tn", fontHeaderSub));
        titleCell.addElement(hText);
        headerTable.addCell(titleCell);
        document.add(headerTable);

        // Bande verte
        PdfPTable band = new PdfPTable(1);
        band.setWidthPercentage(100);
        PdfPCell bandCell = new PdfPCell(new Phrase(" "));
        bandCell.setBackgroundColor(ACCENT);
        bandCell.setBorder(Rectangle.NO_BORDER);
        bandCell.setFixedHeight(5);
        band.addCell(bandCell);
        document.add(band);

        document.add(new Paragraph("\n"));

        // ── Référence ──────────────────────────────────────────────────────
        Paragraph ref = new Paragraph("Réf. : " + reference, fontSmall);
        ref.setAlignment(Element.ALIGN_RIGHT);
        document.add(ref);

        document.add(new Paragraph("\n"));

        // ── Titre principal ────────────────────────────────────────────────
        Paragraph titre = new Paragraph("ATTESTATION DE TRAVAIL", fontTitle);
        titre.setAlignment(Element.ALIGN_CENTER);
        titre.setSpacingAfter(8);
        document.add(titre);
        LineSeparator ls = new LineSeparator();
        ls.setLineColor(PRIMARY);
        document.add(new Chunk(ls));

        document.add(new Paragraph("\n\n"));

        // ── Corps ──────────────────────────────────────────────────────────
        Paragraph intro = new Paragraph(
                "Nous soussignés, la Direction des Ressources Humaines de la société ", fontBody);
        intro.add(new Chunk("Ardhi", fontBold));
        intro.add(new Chunk(", attestons par la présente que :", fontBody));
        intro.setAlignment(Element.ALIGN_JUSTIFIED);
        document.add(intro);

        document.add(new Paragraph("\n"));

        Paragraph nomPara = new Paragraph();
        nomPara.add(new Chunk("M. / Mme.   ", fontBody));
        nomPara.add(new Chunk(nomComplet, fontAccent));
        nomPara.add(new Chunk(",   occupant le poste de   ", fontBody));
        nomPara.add(new Chunk(poste, fontBold));
        nomPara.setAlignment(Element.ALIGN_CENTER);
        nomPara.setSpacingBefore(8);
        nomPara.setSpacingAfter(8);
        document.add(nomPara);

        Paragraph para1 = new Paragraph(
                "est bien employé(e) au sein de notre structure et exerce ses fonctions de manière "
                + "régulière et continue à la date de délivrance de la présente attestation.",
                fontBody);
        para1.setAlignment(Element.ALIGN_JUSTIFIED);
        para1.setSpacingAfter(14);
        document.add(para1);

        Paragraph para2 = new Paragraph(
                "Cette attestation est délivrée à l'intéressé(e) pour servir et valoir ce que de droit.",
                fontBody);
        para2.setAlignment(Element.ALIGN_JUSTIFIED);
        document.add(para2);

        document.add(new Paragraph("\n\n"));

        // ── Date & Signature ───────────────────────────────────────────────
        PdfPTable sigTable = new PdfPTable(2);
        sigTable.setWidthPercentage(90);
        sigTable.setSpacingBefore(20);

        PdfPCell dateCell = new PdfPCell(
                new Phrase("Fait à Tunis, le " + dateAujourdhui, fontBody));
        dateCell.setBorder(Rectangle.NO_BORDER);
        dateCell.setVerticalAlignment(Element.ALIGN_TOP);

        PdfPCell sigCell = new PdfPCell();
        sigCell.setBorder(Rectangle.NO_BORDER);
        Paragraph sigContent = new Paragraph("Le Directeur des Ressources Humaines\n", fontBold);
        sigContent.add(new Chunk("Direction Générale – Ardhi\n\n\n", fontSmall));
        sigContent.add(new Chunk("_______________________________\n", fontSmall));
        sigContent.add(new Chunk("Signature & Cachet officiel", fontSmall));
        sigContent.setAlignment(Element.ALIGN_CENTER);
        sigCell.addElement(sigContent);

        sigTable.addCell(dateCell);
        sigTable.addCell(sigCell);
        document.add(sigTable);

        document.add(new Paragraph("\n\n"));
        document.add(new Chunk(ls));

        // ── Pied de page ───────────────────────────────────────────────────
        Paragraph footer = new Paragraph(
                "Document officiel · Généré le " + dateAujourdhui
                + " · Ardhi © " + annee + " · Toute reproduction non autorisée est interdite",
                fontSmall);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(8);
        document.add(footer);

        document.close();
        return baos.toByteArray();
    }

    // ── ⚙️ Session SMTP ────────────────────────────────────────────────────

    private Session creerSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            SMTP_PORT);
        props.put("mail.smtp.ssl.trust",       SMTP_HOST);

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASSWORD);
            }
        });
    }

    // ── 📨 Construction du message multipart/mixed (HTML + PDF joint) ──────

    private MimeMessage construireMessage(Session session, Employe emp,
                                          byte[] logoBytes, byte[] pdfBytes)
            throws MessagingException, IOException {

        String nomComplet  = emp.getPrenom() + " " + emp.getNom();
        String pdfFileName = "attestation_travail_"
                + emp.getNom().toLowerCase().replace(" ", "_") + ".pdf";

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SMTP_USER, "Ardhi – Ressources Humaines"));
        message.setRecipient(Message.RecipientType.TO,
                new InternetAddress(emp.getEmail(), nomComplet));
        message.setSubject("Attestation de Travail – " + nomComplet, "UTF-8");

        // MIXED : contient HTML (related) + PDF (attachment)
        MimeMultipart mixed = new MimeMultipart("mixed");

        // HTML + logo inline
        MimeMultipart related = new MimeMultipart("related");

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(genererHtmlSimple(emp), "text/html; charset=UTF-8");
        related.addBodyPart(htmlPart);

        MimeBodyPart logoPart = new MimeBodyPart();
        DataSource logoDs = new ByteArrayDataSource(logoBytes, "image/png");
        logoPart.setDataHandler(new DataHandler(logoDs));
        logoPart.setHeader("Content-ID", "<" + LOGO_CID + ">");
        logoPart.setDisposition(MimeBodyPart.INLINE);
        logoPart.setFileName("logo.png");
        related.addBodyPart(logoPart);

        MimeBodyPart relatedWrapper = new MimeBodyPart();
        relatedWrapper.setContent(related);
        mixed.addBodyPart(relatedWrapper);

        // PDF en pièce jointe
        MimeBodyPart pdfPart = new MimeBodyPart();
        DataSource pdfDs = new ByteArrayDataSource(pdfBytes, "application/pdf");
        pdfPart.setDataHandler(new DataHandler(pdfDs));
        pdfPart.setFileName(pdfFileName);
        pdfPart.setDisposition(MimeBodyPart.ATTACHMENT);
        mixed.addBodyPart(pdfPart);

        message.setContent(mixed);
        return message;
    }

    // ── 🎨 Corps HTML complet (testable) ─────────────────────────────────

    /**
     * Génère le corps HTML de l'attestation.
     * Cette méthode est package-private pour faciliter les tests unitaires.
     */
    String genererHtml(Employe emp) {
        String nomComplet     = emp.getPrenom() + " " + emp.getNom();
        String poste          = emp.getPoste() != null ? emp.getPoste() : "Employé";
        String dateAujourdhui = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH));
        String annee          = String.valueOf(LocalDate.now().getYear());
        String reference      = "ATT-" + annee + "-" + String.format("%04d", emp.getId());

        return "<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'>"
             + "<title>Attestation de Travail</title>"
             + "</head><body>"
             + "<p>Réf. : " + reference + "</p>"
             + "<h1>Attestation de Travail</h1>"
             + "<p>Société : <strong>Ardhi</strong></p>"
             + "<p>Employé(e) : <strong>" + nomComplet + "</strong></p>"
             + "<p>Poste : <strong>" + poste + "</strong></p>"
             + "<p>Date : " + dateAujourdhui + "</p>"
             + "<p>Ce document atteste que l'intéressé(e) est employé(e) au sein de la société Ardhi.</p>"
             + "</body></html>";
    }

    // ── 🎨 Corps HTML simplifié de l'email ────────────────────────────────

    private String genererHtmlSimple(Employe emp) {
        String nomComplet     = emp.getPrenom() + " " + emp.getNom();
        String poste          = emp.getPoste() != null ? emp.getPoste() : "Employé";
        String dateAujourdhui = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH));
        String logoSrc = "cid:" + LOGO_CID;

        return "<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'>"
             + "<style>"
             + "body{font-family:Georgia,serif;background:#e8e8e8;margin:0;padding:20px;}"
             + ".wrap{max-width:600px;margin:0 auto;background:#fff;border-radius:8px;"
             + "     box-shadow:0 4px 20px rgba(0,0,0,.15);overflow:hidden;}"
             + ".hd{background:#1a3a5c;padding:24px 32px;display:flex;align-items:center;gap:16px;}"
             + ".hd img{width:56px;height:56px;object-fit:contain;}"
             + ".hd h1{color:#fff;font-size:18px;letter-spacing:2px;margin:0;}"
             + ".hd p{color:rgba(255,255,255,.7);font-size:11px;margin:4px 0 0;}"
             + ".band{height:4px;background:linear-gradient(to right,#27ae60,#f0a500,#1a3a5c);}"
             + ".body{padding:32px;}"
             + ".body p{font-size:14px;line-height:1.8;color:#333;margin:0 0 14px;}"
             + ".name{font-weight:bold;color:#1a3a5c;font-size:15px;}"
             + ".note{background:#f0f9f4;border-left:4px solid #27ae60;"
             + "      padding:14px 18px;border-radius:4px;margin:20px 0;}"
             + ".note p{margin:0;color:#1d6e3e;font-size:13px;font-weight:bold;}"
             + ".ft{background:#f5f5f5;border-top:1px solid #ddd;padding:14px 32px;"
             + "    font-size:11px;color:#999;text-align:center;}"
             + "</style></head><body>"
             + "<div class='wrap'>"
             + "<div class='hd'>"
             + "  <img src='" + logoSrc + "' alt='Logo Ardhi'/>"
             + "  <div><h1>Ardhi – Ressources Humaines</h1><p>Gestion Agricole Intelligente</p></div>"
             + "</div>"
             + "<div class='band'></div>"
             + "<div class='body'>"
             + "<p>Madame, Monsieur <span class='name'>" + nomComplet + "</span>,</p>"
             + "<p>Nous avons le plaisir de vous adresser votre <strong>attestation de travail</strong> "
             + "officielle, établie à la date du <strong>" + dateAujourdhui + "</strong>.</p>"
             + "<p>Ce document confirme que vous occupez le poste de <strong>" + poste + "</strong> "
             + "au sein de notre société <strong>Ardhi</strong>, et que vous y exercez vos fonctions "
             + "de manière régulière et continue.</p>"
             + "<div class='note'><p>📎 Votre attestation de travail (PDF imprimable) est jointe à cet email.</p></div>"
             + "<p>Cordialement,<br><strong>La Direction des Ressources Humaines</strong><br>Ardhi – Gestion Agricole</p>"
             + "</div>"
             + "<div class='ft'>Document officiel · " + dateAujourdhui
             + " · Ardhi © " + LocalDate.now().getYear()
             + " · Toute reproduction non autorisée est interdite</div>"
             + "</div></body></html>";
    }

    // ── 🚨 Exception métier ────────────────────────────────────────────────

    public static class AttestationMailException extends Exception {
        public AttestationMailException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}