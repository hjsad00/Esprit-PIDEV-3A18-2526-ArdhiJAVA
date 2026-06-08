package tn.neuron.ardhi.utils.UserAndDiag;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import tn.neuron.ardhi.models.UserAndDiag.Abonnement;
import tn.neuron.ardhi.models.UserAndDiag.User;

import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PDFGenerator {

    public void genererFacturePDF(String path, User user, Abonnement abo) {
        Document document = new Document();

        try {
            PdfWriter.getInstance(document, new FileOutputStream(path));
            document.open();

            // 1. En-tête (Titre)
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, BaseColor.GREEN);
            Paragraph title = new Paragraph("Facture ARDHI", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph("\n")); // Saut de ligne

            // 2. Infos de la société (Simulé)
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);
            document.add(new Paragraph("Ardhi Inc.", normalFont));
            document.add(new Paragraph("123 Avenue de l'Agriculture", normalFont));
            document.add(new Paragraph("Tunis, Tunisie", normalFont));
            document.add(new Paragraph("Email: contact@ardhi.tn", normalFont));

            document.add(new Paragraph("\n----------------------------------------------------------\n"));

            // 3. Infos Client
            document.add(new Paragraph("Facturé à :", FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            document.add(new Paragraph("Nom : " + user.getNom().toUpperCase() + " " + user.getPrenom(), normalFont));
            document.add(new Paragraph("Email : " + user.getEmail(), normalFont));

            // Date de la facture
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            document.add(new Paragraph("Date : " + sdf.format(new Date()), normalFont));

            document.add(new Paragraph("\n\n"));

            // 4. Tableau des détails
            PdfPTable table = new PdfPTable(3); // 3 Colonnes
            table.setWidthPercentage(100);

            // En-têtes du tableau
            addTableHeader(table, "Description");
            addTableHeader(table, "Durée");
            addTableHeader(table, "Montant");

            // Données
            addTableCell(table, abo.getType());
            addTableCell(table, "Du " + abo.getDateDebut() + " au " + abo.getDateFin());
            addTableCell(table, abo.getPrix() + " DT");

            document.add(table);

            // 5. Total
            document.add(new Paragraph("\n"));
            Paragraph total = new Paragraph("Total Payé : " + abo.getPrix() + " DT",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.RED));
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            // Pied de page
            document.add(new Paragraph("\n\n\nMerci de votre confiance !",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE)));

            document.close();

        } catch (DocumentException | FileNotFoundException e) {
            System.err.println("Erreur lors de la génération du PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addTableHeader(PdfPTable table, String headerTitle) {
        PdfPCell header = new PdfPCell();
        header.setBackgroundColor(BaseColor.LIGHT_GRAY);
        header.setBorderWidth(1);
        header.setPhrase(new Phrase(headerTitle));
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        header.setPadding(5);
        table.addCell(header);
    }

    private void addTableCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setPadding(5);
        table.addCell(cell);
    }
}