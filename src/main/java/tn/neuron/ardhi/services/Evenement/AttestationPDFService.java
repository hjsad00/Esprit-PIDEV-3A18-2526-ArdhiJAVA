package tn.neuron.ardhi.services.Evenement;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import tn.neuron.ardhi.models.Evenement.Evenement;
import tn.neuron.ardhi.models.Evenement.Participation;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AttestationPDFService {

    private static final BaseColor PRIMARY_COLOR = new BaseColor(107, 122, 63);

    private static final Font TITLE_FONT =
            new Font(Font.FontFamily.HELVETICA, 26, Font.BOLD, PRIMARY_COLOR);

    private static final Font NORMAL_FONT =
            new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.BLACK);

    private static final Font BOLD_FONT =
            new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.BLACK);

    private static final Font SMALL_ITALIC =
            new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, BaseColor.GRAY);

    public boolean genererAttestation(Participation participation,
                                      Evenement evenement,
                                      String outputPath) {

        Document document = new Document(PageSize.A4, 50, 50, 50, 50);

        try {

            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputPath));
            document.open();

            addCertificateBorder(writer);
            addWatermark(writer);
            addLogo(document);
            addHeader(document);

            document.add(new Paragraph("\n"));

            Paragraph title = new Paragraph("ATTESTATION DE PRÉSENCE", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(20);
            title.setSpacingAfter(20);
            document.add(title);

            addLine(document);

            document.add(new Paragraph("\n"));

            Paragraph intro = new Paragraph(
                    "Je soussigné(e), organisateur de l'événement mentionné ci-dessous, atteste que :",
                    NORMAL_FONT);
            intro.setAlignment(Element.ALIGN_CENTER);
            document.add(intro);

            document.add(new Paragraph("\n\n"));

            Paragraph participant = new Paragraph();
            participant.add(new Chunk("M./Mme. ", NORMAL_FONT));
            participant.add(new Chunk(participation.getNomComplet(), BOLD_FONT));
            participant.setAlignment(Element.ALIGN_CENTER);
            participant.setSpacingAfter(15);
            document.add(participant);

            Paragraph participationText = new Paragraph(
                    "a bien participé à l'événement suivant :",
                    NORMAL_FONT);
            participationText.setAlignment(Element.ALIGN_CENTER);
            document.add(participationText);

            document.add(new Paragraph("\n"));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(80);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);
            table.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.setWidths(new float[]{1.5f, 3f});

            addTableRow(table, "Événement :", evenement.getTitre());
            addTableRow(table, "Type :", evenement.getType());

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String dateStr = evenement.getDateDebut().format(dateFormatter);

            if (!evenement.getDateDebut().equals(evenement.getDateFin())) {
                dateStr += " - " + evenement.getDateFin().format(dateFormatter);
            }

            addTableRow(table, "Date :", dateStr);
            addTableRow(table, "Lieu :", evenement.getLieu());
            addTableRow(table, "Organisateur :", evenement.getOrganisateur());

            document.add(table);

            if (participation.getNote() > 0) {
                document.add(new Paragraph("\n"));
                Paragraph note = new Paragraph(
                        "Note d'évaluation : " + participation.getNote() + "/5 ⭐",
                        NORMAL_FONT);
                note.setAlignment(Element.ALIGN_CENTER);
                document.add(note);
            }

            document.add(new Paragraph("\n\n"));

            DateTimeFormatter dateTimeFormatter =
                    DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

            String dateGeneration =
                    LocalDateTime.now().format(dateTimeFormatter);

            Paragraph date = new Paragraph(
                    "Fait à Tunis, le " + dateGeneration,
                    NORMAL_FONT);
            date.setAlignment(Element.ALIGN_RIGHT);
            document.add(date);

            document.add(new Paragraph("\n\n"));

            LineSeparator separator = new LineSeparator();
            separator.setPercentage(30);
            separator.setAlignment(Element.ALIGN_RIGHT);
            document.add(new Chunk(separator));

            Paragraph signature = new Paragraph(
                    evenement.getOrganisateur(),
                    BOLD_FONT);
            signature.setAlignment(Element.ALIGN_RIGHT);
            document.add(signature);

            document.add(new Paragraph("\n\n\n"));

            addLine(document);

            Paragraph footer = new Paragraph(
                    "🌾 ARDHI - Plateforme Agricole Intelligente",
                    SMALL_ITALIC);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void addLogo(Document document)
            throws IOException, DocumentException {

        try {
            Image logo = Image.getInstance("../../img/ardhi_logo.png");
            logo.scaleToFit(100, 100);
            logo.setAlignment(Element.ALIGN_CENTER);
            document.add(logo);
        } catch (Exception ignored) {
        }
    }

    private void addHeader(Document document)
            throws DocumentException {

        Paragraph header = new Paragraph(
                "ARDHI",
                new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, PRIMARY_COLOR));
        header.setAlignment(Element.ALIGN_CENTER);
        document.add(header);

        Paragraph sub = new Paragraph(
                "Plateforme Agricole Intelligente",
                SMALL_ITALIC);
        sub.setAlignment(Element.ALIGN_CENTER);
        document.add(sub);
    }

    private void addCertificateBorder(PdfWriter writer) {

        PdfContentByte canvas = writer.getDirectContent();
        Rectangle rect = new Rectangle(20, 20,
                PageSize.A4.getWidth() - 20,
                PageSize.A4.getHeight() - 20);

        rect.setBorder(Rectangle.BOX);
        rect.setBorderWidth(3);
        rect.setBorderColor(PRIMARY_COLOR);
        canvas.rectangle(rect);
    }

    private void addWatermark(PdfWriter writer) {

        PdfContentByte canvas = writer.getDirectContentUnder();

        Font watermarkFont = new Font(
                Font.FontFamily.HELVETICA,
                60,
                Font.BOLD,
                new BaseColor(230, 230, 230));

        Phrase watermark = new Phrase("ARDHI", watermarkFont);

        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_CENTER,
                watermark,
                297,
                421,
                45);
    }

    private void addLine(Document document)
            throws DocumentException {

        LineSeparator line = new LineSeparator();
        line.setLineColor(PRIMARY_COLOR);
        document.add(new Chunk(line));
    }

    private void addTableRow(PdfPTable table,
                             String label,
                             String value) {

        PdfPCell labelCell = new PdfPCell(new Phrase(label, BOLD_FONT));
        labelCell.setBorderWidthBottom(1);
        labelCell.setBorderColorBottom(new BaseColor(200, 200, 200));
        labelCell.setPadding(8);
        labelCell.setBackgroundColor(new BaseColor(245, 245, 240));

        PdfPCell valueCell = new PdfPCell(new Phrase(value, NORMAL_FONT));
        valueCell.setBorderWidthBottom(1);
        valueCell.setBorderColorBottom(new BaseColor(200, 200, 200));
        valueCell.setPadding(8);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }
}