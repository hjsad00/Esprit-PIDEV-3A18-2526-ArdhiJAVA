package tn.neuron.ardhi.services.MaterielEtMaintenance;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service de génération du PDF de confirmation de maintenance.
 * Design sobre et professionnel — fond blanc dominant, une seule page.
 * Utilise iTextPDF 5.5.x.
 */
public class MaintenancePdfService {

    // ── Palette épurée ───────────────────────────────────────────
    private static final BaseColor VERT        = new BaseColor( 46, 125,  50);
    private static final BaseColor VERT_LIGHT  = new BaseColor(232, 245, 233);
    private static final BaseColor VERT_LINE   = new BaseColor(165, 214, 167);
    private static final BaseColor ORANGE      = new BaseColor(230,  81,   0);
    private static final BaseColor NOIR        = new BaseColor( 26,  26,  26);
    private static final BaseColor GRIS_TITRE  = new BaseColor( 66,  66,  66);
    private static final BaseColor GRIS_LABEL  = new BaseColor(158, 158, 158);
    private static final BaseColor GRIS_ROW    = new BaseColor(250, 250, 250);
    private static final BaseColor GRIS_BORDER = new BaseColor(224, 224, 224);

    private static final DateTimeFormatter FMT_DATE     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_TIME     = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_DATETIME = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm");
    private static final DateTimeFormatter FMT_REF      = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    // ════════════════════════════════════════════════════════════
    //  MÉTHODE PRINCIPALE
    // ════════════════════════════════════════════════════════════
    public File genererPdfConfirmation(
            String nomAgriculteur,
            String emailAgriculteur,
            String nomMateriel,
            String typeMateriel,
            String etatMateriel,
            String typeMaintenance,
            LocalDateTime dateTime,
            String description,
            String googleEventId) {

        String reference = "ARDHI-" + dateTime.format(FMT_REF);
        String nomFichier = "Confirmation_" + nomMateriel.replaceAll("[^a-zA-Z0-9]", "_")
                + "_" + dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

        File outputFile = new File(System.getProperty("java.io.tmpdir"), nomFichier);

        try {
            Document doc = new Document(PageSize.A4, 45, 45, 45, 55);
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(outputFile));
            writer.setPageEvent(new PiedDePage(reference, nomAgriculteur));
            doc.open();

            dessinerEnTete(writer, reference);
            ajouterCartesDatesHeure(doc, dateTime);
            ajouterSalutation(doc, nomAgriculteur);
            ajouterTableauDetails(doc, nomMateriel, typeMateriel, etatMateriel,
                    typeMaintenance, emailAgriculteur, dateTime, googleEventId);
            ajouterDescription(doc, description);
            ajouterRappels(doc);
            ajouterMessageFinal(doc, nomAgriculteur);

            doc.close();
            System.out.println("PDF genere : " + outputFile.getAbsolutePath());
            return outputFile;

        } catch (Exception e) {
            System.err.println("Erreur PDF : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ════════════════════════════════════════════════════════════
    //  1. EN-TÊTE dessiné sur le canvas
    //     Barre verte 6px + logo + pill + référence
    // ════════════════════════════════════════════════════════════
    private void dessinerEnTete(PdfWriter writer, String reference) throws DocumentException {
        try {
            PdfContentByte cb = writer.getDirectContent();
            float W  = PageSize.A4.getWidth();
            float H  = PageSize.A4.getHeight();
            float mg = 45f;

            BaseFont bfBold   = BaseFont.createFont(BaseFont.HELVETICA_BOLD,   BaseFont.WINANSI, false);
            BaseFont bfNormal = BaseFont.createFont(BaseFont.HELVETICA,        BaseFont.WINANSI, false);

            // Barre verte top
            cb.setColorFill(VERT);
            cb.rectangle(0, H - 6, W, 6);
            cb.fill();

            // Nom ARDHI
            cb.setColorFill(VERT);
            cb.setFontAndSize(bfBold, 28);
            cb.beginText();
            cb.setTextMatrix(mg, H - 44);
            cb.showText("ARDHI");
            cb.endText();

            // Tagline
            cb.setColorFill(GRIS_LABEL);
            cb.setFontAndSize(bfNormal, 9);
            cb.beginText();
            cb.setTextMatrix(mg, H - 57);
            cb.showText("Plateforme de Gestion Agricole Intelligente");
            cb.endText();

            // Pill "CONFIRMATION DE MAINTENANCE"
            cb.setColorFill(VERT_LIGHT);
            cb.roundRectangle(mg, H - 82, 178, 16, 8);
            cb.fill();
            cb.setColorFill(VERT);
            cb.setFontAndSize(bfBold, 7);
            cb.beginText();
            cb.setTextMatrix(mg + 8, H - 77);
            cb.showText("CONFIRMATION DE MAINTENANCE");
            cb.endText();

            // Référence côté droit
            cb.setColorFill(GRIS_LABEL);
            cb.setFontAndSize(bfNormal, 7);
            cb.beginText();
            cb.setTextMatrix(W - mg - 130, H - 38);
            cb.showText("Ref. confirmation");
            cb.endText();

            cb.setColorFill(NOIR);
            cb.setFontAndSize(bfBold, 8);
            cb.beginText();
            cb.setTextMatrix(W - mg - 130, H - 50);
            cb.showText(reference);
            cb.endText();

            cb.setColorFill(GRIS_LABEL);
            cb.setFontAndSize(bfNormal, 7);
            cb.beginText();
            cb.setTextMatrix(W - mg - 130, H - 64);
            cb.showText("Emis le  " + LocalDateTime.now().format(FMT_DATE));
            cb.endText();

            // Ligne séparatrice bas header
            cb.setColorStroke(GRIS_BORDER);
            cb.setLineWidth(0.8f);
            cb.moveTo(mg, H - 95);
            cb.lineTo(W - mg, H - 95);
            cb.stroke();

        } catch (Exception e) {
            throw new DocumentException(e);
        }
    }

    // ════════════════════════════════════════════════════════════
    //  2. CARTES DATE / HEURE
    //     Fond gris très clair + barre accent colorée à gauche
    // ════════════════════════════════════════════════════════════
    private void ajouterCartesDatesHeure(Document doc, LocalDateTime dt) throws DocumentException {
        // Spacer pour compenser l'en-tête dessiné manuellement (95pt header)
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingAfter(60);
        doc.add(spacer);

        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setSpacingAfter(12);
        t.addCell(creerCarteInfo("DATE DU RENDEZ-VOUS", dt.format(FMT_DATE), VERT));
        t.addCell(creerCarteInfo("HEURE D'INTERVENTION", dt.format(FMT_TIME), ORANGE));
        doc.add(t);
    }

    private PdfPCell creerCarteInfo(String label, String valeur, BaseColor accent) {
        // Table interne : barre accent (3pt) + contenu
        PdfPTable inner = new PdfPTable(new float[]{3f, 100f});
        inner.setWidthPercentage(100);

        PdfPCell bar = new PdfPCell();
        bar.setBackgroundColor(accent);
        bar.setBorder(Rectangle.NO_BORDER);
        bar.setPadding(0);
        inner.addCell(bar);

        Font fLabel = new Font(Font.FontFamily.HELVETICA,  7, Font.NORMAL, GRIS_LABEL);
        Font fVal   = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD,   NOIR);

        PdfPCell content = new PdfPCell();
        content.setBorder(Rectangle.NO_BORDER);
        content.setBackgroundColor(GRIS_ROW);
        content.setPaddingLeft(10);
        content.setPaddingTop(8);
        content.setPaddingBottom(8);
        content.addElement(new Paragraph(label, fLabel));
        Paragraph pv = new Paragraph(valeur, fVal);
        pv.setSpacingBefore(2);
        content.addElement(pv);
        inner.addCell(content);

        PdfPCell outer = new PdfPCell();
        outer.setBorder(Rectangle.NO_BORDER);
        outer.setBackgroundColor(GRIS_ROW);
        outer.setPadding(0);
        outer.addElement(inner);
        return outer;
    }

    // ════════════════════════════════════════════════════════════
    //  3. SALUTATION
    // ════════════════════════════════════════════════════════════
    private void ajouterSalutation(Document doc, String nom) throws DocumentException {
        Font fBold = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,   NOIR);
        Font fSub  = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, GRIS_LABEL);

        Paragraph p1 = new Paragraph("Bonjour " + nom + ",", fBold);
        p1.setSpacingAfter(3);
        doc.add(p1);

        Paragraph p2 = new Paragraph(
                "Votre rendez-vous de maintenance est confirme. Conservez ce document pour vos archives.", fSub);
        p2.setSpacingAfter(10);
        doc.add(p2);
    }

    // ════════════════════════════════════════════════════════════
    //  4. TABLEAU DÉTAILS — 4 rangées × 2 colonnes (label + valeur)
    // ════════════════════════════════════════════════════════════
    private void ajouterTableauDetails(Document doc,
                                       String nomMateriel, String typeMateriel,
                                       String etatMateriel, String typeMaintenance,
                                       String email, LocalDateTime dt,
                                       String googleId) throws DocumentException {

        doc.add(creerTitre("DETAILS DE L'INTERVENTION"));

        String[][] data = {
                {"Materiel",       nomMateriel,             "Type de materiel",    typeMateriel},
                {"Etat actuel",    etatMateriel,            "Type de maintenance", typeMaintenance},
                {"Date",           dt.format(FMT_DATETIME), "Duree estimee",       "2 heures"},
                {"Technicien",     email,                   "Ref. Google Cal.",    tronquer(googleId, 30)},
        };

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.0f, 1.6f, 1.1f, 1.6f});
        table.setSpacingAfter(10);

        Font fLabel = new Font(Font.FontFamily.HELVETICA,  7, Font.BOLD,   GRIS_LABEL);
        Font fVal   = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, NOIR);

        for (int i = 0; i < data.length; i++) {
            BaseColor bg = (i % 2 == 0) ? GRIS_ROW : BaseColor.WHITE;
            for (int col = 0; col < 4; col++) {
                boolean isLabel = (col % 2 == 0);
                String txt = isLabel
                        ? data[i][col].toUpperCase()
                        : (data[i][col] != null ? data[i][col] : "-");

                PdfPCell cell = new PdfPCell();
                cell.setBackgroundColor(bg);
                cell.setBorderColor(GRIS_BORDER);
                cell.setBorderWidth(0.3f);
                cell.setPaddingTop(5);
                cell.setPaddingBottom(6);
                cell.setPaddingLeft(8);
                cell.setPaddingRight(6);
                cell.addElement(new Paragraph(txt, isLabel ? fLabel : fVal));
                table.addCell(cell);
            }
        }
        doc.add(table);
    }

    // ════════════════════════════════════════════════════════════
    //  5. DESCRIPTION TRAVAUX
    //     Barre verte gauche (3px) + fond gris très clair
    // ════════════════════════════════════════════════════════════
    private void ajouterDescription(Document doc, String desc) throws DocumentException {
        doc.add(creerTitre("DESCRIPTION DES TRAVAUX"));

        PdfPTable t = new PdfPTable(new float[]{3f, 100f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(10);

        PdfPCell bar = new PdfPCell();
        bar.setBackgroundColor(VERT);
        bar.setBorder(Rectangle.NO_BORDER);
        bar.setPadding(0);
        t.addCell(bar);

        Font f = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, GRIS_TITRE);
        PdfPCell content = new PdfPCell();
        content.setBackgroundColor(GRIS_ROW);
        content.setBorderColor(GRIS_BORDER);
        content.setBorderWidth(0.4f);
        content.setBorderWidthLeft(0);
        content.setPaddingLeft(12);
        content.setPaddingTop(10);
        content.setPaddingBottom(10);
        content.setPaddingRight(10);
        content.addElement(new Paragraph(desc != null ? desc : "-", f));
        t.addCell(content);
        doc.add(t);
    }

    // ════════════════════════════════════════════════════════════
    //  6. RAPPELS — 4 chips grises épurées
    // ════════════════════════════════════════════════════════════
    private void ajouterRappels(Document doc) throws DocumentException {
        doc.add(creerTitre("RAPPELS AUTOMATIQUES"));

        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setSpacingAfter(10);

        Font f = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, GRIS_TITRE);
        String[] chips = {"Email 24h avant", "Notif. 1h avant", "Google Calendar", "Archivez ce PDF"};

        for (String chip : chips) {
            PdfPCell cell = new PdfPCell();
            cell.setBackgroundColor(GRIS_ROW);
            cell.setBorderColor(GRIS_BORDER);
            cell.setBorderWidth(0.5f);
            cell.setPaddingTop(9);
            cell.setPaddingBottom(9);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph p = new Paragraph(chip, f);
            p.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(p);
            t.addCell(cell);
        }
        doc.add(t);
    }

    // ════════════════════════════════════════════════════════════
    //  7. MESSAGE FINAL — seul bloc coloré (vert)
    // ════════════════════════════════════════════════════════════
    private void ajouterMessageFinal(Document doc, String nom) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(4);

        Font fMain = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   BaseColor.WHITE);
        Font fSub  = new Font(Font.FontFamily.HELVETICA,  8, Font.NORMAL, new BaseColor(165, 214, 167));

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(VERT);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(14);
        cell.setPaddingBottom(12);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph p1 = new Paragraph(
                "Bienvenue, " + nom + " !  L'equipe ARDHI vous souhaite une excellente maintenance.", fMain);
        p1.setAlignment(Element.ALIGN_CENTER);

        Paragraph p2 = new Paragraph("Pour toute question, contactez votre conseiller ARDHI.", fSub);
        p2.setAlignment(Element.ALIGN_CENTER);
        p2.setSpacingBefore(4);

        cell.addElement(p1);
        cell.addElement(p2);
        t.addCell(cell);
        doc.add(t);
    }

    // ════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ════════════════════════════════════════════════════════════
    private Paragraph creerTitre(String titre) {
        Font f = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, GRIS_TITRE);
        Paragraph p = new Paragraph(titre, f);
        p.setSpacingBefore(6);
        p.setSpacingAfter(5);
        p.add(new Chunk(new LineSeparator(0.5f, 100f, GRIS_BORDER, Element.ALIGN_LEFT, -3)));
        return p;
    }

    private String tronquer(String s, int max) {
        if (s == null) return "-";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    // ════════════════════════════════════════════════════════════
    //  PIED DE PAGE
    // ════════════════════════════════════════════════════════════
    private static class PiedDePage extends PdfPageEventHelper {
        private final String ref, nom;

        PiedDePage(String ref, String nom) {
            this.ref = ref;
            this.nom = nom;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document doc) {
            try {
                PdfContentByte cb = writer.getDirectContent();
                float left  = doc.leftMargin();
                float right = doc.getPageSize().getWidth() - doc.rightMargin();
                float y     = doc.bottomMargin() + 20;

                cb.setColorStroke(new BaseColor(224, 224, 224));
                cb.setLineWidth(0.5f);
                cb.moveTo(left, y + 8);
                cb.lineTo(right, y + 8);
                cb.stroke();

                Font f = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL,
                        new BaseColor(158, 158, 158));
                ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                        new Phrase("ARDHI — Confirmation " + ref + "  |  " + nom, f),
                        left, y, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                        new Phrase("Document genere automatiquement — Page 1 / 1", f),
                        right, y, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
