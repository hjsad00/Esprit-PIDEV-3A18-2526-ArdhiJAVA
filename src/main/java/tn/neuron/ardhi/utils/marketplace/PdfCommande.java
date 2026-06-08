package tn.neuron.ardhi.utils.marketplace;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import tn.neuron.ardhi.models.marketplace.Commande;
import tn.neuron.ardhi.models.marketplace.DetailsCommande;
import tn.neuron.ardhi.models.marketplace.Produit;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.services.marketplace.ProduitService;
import tn.neuron.ardhi.services.UserAndDiag.UserService;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

public class PdfCommande {

    // Couleurs modernes
    private static final BaseColor PRIMARY_COLOR = new BaseColor(46, 204, 113); // Vert moderne
    private static final BaseColor SECONDARY_COLOR = new BaseColor(52, 73, 94); // Gris foncé
    private static final BaseColor LIGHT_GRAY = new BaseColor(236, 240, 241);
    private static final BaseColor WHITE = BaseColor.WHITE;

    // Chemin du logo (à adapter selon votre structure)
    private static final String LOGO_PATH = "resources/logo.png"; // Modifiez ce chemin

    public static void genererPdf(Commande commande, List<DetailsCommande> details) {
        try {
            // Création du dossier factures
            File dir = new File("factures");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String dest = "factures/facture_" + commande.getIdCommande() + ".pdf";
            Document document = new Document(PageSize.A4, 40, 40, 60, 60);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(dest));

            // Ajouter des événements pour en-tête et pied de page
            HeaderFooterPageEvent event = new HeaderFooterPageEvent();
            writer.setPageEvent(event);

            document.open();

            // === EN-TÊTE AVEC BANDE DE COULEUR ===
            ajouterEnTete(document);

            // === INFORMATIONS COMMANDE ET CLIENT ===
            ajouterInfosCommandeClient(document, commande);

            // === TABLEAU DES PRODUITS MODERNE ===
            ajouterTableauProduits(document, details);

            // === SECTION TOTAUX ===
            ajouterSectionTotaux(document, commande, details);

            // === PIED DE PAGE ===
            ajouterPiedDePage(document);

            document.close();
            System.out.println("✓ PDF généré avec succès : " + dest);

        } catch (Exception e) {
            System.err.println("✗ Erreur lors de la génération du PDF : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void ajouterEnTete(Document document) throws DocumentException {
        // Bande de couleur en haut
        PdfPTable headerBand = new PdfPTable(1);
        headerBand.setWidthPercentage(100);
        PdfPCell bandCell = new PdfPCell();
        bandCell.setBackgroundColor(PRIMARY_COLOR);
        bandCell.setFixedHeight(15);
        bandCell.setBorder(Rectangle.NO_BORDER);
        headerBand.addCell(bandCell);
        document.add(headerBand);

        document.add(new Paragraph(" "));

        // Titre et logo
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[] { 1, 1 });

        // Logo/Nom entreprise (côté gauche)
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);

        try {
            // Tenter de charger le logo
            File logoFile = new File(LOGO_PATH);
            if (logoFile.exists()) {
                Image logo = Image.getInstance(LOGO_PATH);
                logo.scaleToFit(100, 50); // Ajuster la taille du logo
                logoCell.addElement(logo);
            } else {
                // Si le logo n'existe pas, afficher le nom
                Font companyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, PRIMARY_COLOR);
                Paragraph company = new Paragraph("ARDHI", companyFont);
                logoCell.addElement(company);
            }
        } catch (Exception e) {
            // En cas d'erreur, afficher le nom
            Font companyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, PRIMARY_COLOR);
            Paragraph company = new Paragraph("ARDHI", companyFont);
            logoCell.addElement(company);
        }

        Font taglineFont = FontFactory.getFont(FontFactory.HELVETICA, 10, SECONDARY_COLOR);
        Paragraph tagline = new Paragraph("Votre partenaire de confiance", taglineFont);
        logoCell.addElement(tagline);
        headerTable.addCell(logoCell);

        // Titre FACTURE (côté droit)
        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Font invoiceFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, SECONDARY_COLOR);
        Paragraph invoiceTitle = new Paragraph("FACTURE", invoiceFont);
        titleCell.addElement(invoiceTitle);
        headerTable.addCell(titleCell);

        document.add(headerTable);
        document.add(new Paragraph(" "));

        // Ligne de séparation
        ajouterLigneSeparation(document, PRIMARY_COLOR);
        document.add(new Paragraph(" "));
    }

    private static void ajouterInfosCommandeClient(Document document, Commande commande) throws DocumentException {
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[] { 1, 1 });

        // Colonne gauche - Info Client
        PdfPCell clientCell = creerCelluleInfo("INFORMATIONS CLIENT", getInfoClient(commande));
        infoTable.addCell(clientCell);

        // Colonne droite - Info Facture
        String dateStr = commande.getDateCommande() != null
                ? commande.getDateCommande().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : new SimpleDateFormat("dd/MM/yyyy").format(new Date());

        String modeLivraisonStr = (commande.getModeLivraison() != null
                && commande.getModeLivraison() == tn.neuron.ardhi.models.marketplace.ModeLivraison.LIVRAISON)
                        ? "Livraison a domicile"
                        : "Recuperation sur place";
        String infoFacture = "N Facture: " + commande.getIdCommande() + "\n" +
                "Date: " + dateStr + "\n" +
                "Statut: " + (commande.getEtat() != null ? commande.getEtat() : "En cours") + "\n" +
                "Mode: " + modeLivraisonStr;

        PdfPCell factureCell = creerCelluleInfo("DÉTAILS FACTURE", infoFacture);
        infoTable.addCell(factureCell);

        document.add(infoTable);
        document.add(new Paragraph(" "));
    }

    private static PdfPCell creerCelluleInfo(String titre, String contenu) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(12);
        cell.setBackgroundColor(LIGHT_GRAY);
        cell.setBorder(Rectangle.NO_BORDER);

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, PRIMARY_COLOR);
        Paragraph title = new Paragraph(titre, titleFont);
        title.setSpacingAfter(8);
        cell.addElement(title);

        Font contentFont = FontFactory.getFont(FontFactory.HELVETICA, 10, SECONDARY_COLOR);
        Paragraph content = new Paragraph(contenu, contentFont);
        cell.addElement(content);

        return cell;
    }

    private static String getInfoClient(Commande commande) {
        UserService userService = new UserService();
        User client = userService.chercherParId(commande.getIdUser());
        if (client != null) {
            return client.getNom() + " " + client.getPrenom() + "\n" +
                    "Email: " + client.getEmail() + "\n";
        }
        return "Client ID: " + commande.getIdUser();
    }

    private static void ajouterTableauProduits(Document document, List<DetailsCommande> details)
            throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 3, 1, 1.5f, 1.5f });
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        // En-têtes avec style moderne
        String[] headers = { "PRODUIT", "QTÉ", "PRIX UNIT.", "TOTAL" };
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, WHITE);

        for (String headerTitle : headers) {
            PdfPCell header = new PdfPCell(new Phrase(headerTitle, headerFont));
            header.setBackgroundColor(PRIMARY_COLOR);
            header.setPadding(10);
            header.setHorizontalAlignment(Element.ALIGN_CENTER);
            header.setVerticalAlignment(Element.ALIGN_MIDDLE);
            header.setBorder(Rectangle.NO_BORDER);
            table.addCell(header);
        }

        // Lignes de produits avec alternance de couleurs
        ProduitService produitService = new ProduitService();
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10, SECONDARY_COLOR);
        Font cellFontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, SECONDARY_COLOR);

        boolean alternate = false;
        for (DetailsCommande detail : details) {
            BaseColor rowColor = alternate ? WHITE : LIGHT_GRAY;

            // Nom produit
            String nomProduit = "Produit #" + detail.getIdProduit();
            Produit p = produitService.getProduitById(detail.getIdProduit());
            if (p != null) {
                nomProduit = p.getNom();
            }

            PdfPCell cellNom = new PdfPCell(new Phrase(nomProduit, cellFontBold));
            cellNom.setBackgroundColor(rowColor);
            cellNom.setPadding(10);
            cellNom.setBorder(Rectangle.NO_BORDER);
            table.addCell(cellNom);

            // Quantité
            PdfPCell cellQty = new PdfPCell(new Phrase(String.valueOf(detail.getQuantite()), cellFont));
            cellQty.setBackgroundColor(rowColor);
            cellQty.setPadding(10);
            cellQty.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellQty.setBorder(Rectangle.NO_BORDER);
            table.addCell(cellQty);

            // Prix unitaire
            PdfPCell cellPrice = new PdfPCell(new Phrase(String.format("%.2f DT", detail.getPrixUnitaire()), cellFont));
            cellPrice.setBackgroundColor(rowColor);
            cellPrice.setPadding(10);
            cellPrice.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cellPrice.setBorder(Rectangle.NO_BORDER);
            table.addCell(cellPrice);

            // Sous-total
            float sousTotal = detail.getQuantite() * detail.getPrixUnitaire();
            PdfPCell cellSubTotal = new PdfPCell(new Phrase(String.format("%.2f DT", sousTotal), cellFontBold));
            cellSubTotal.setBackgroundColor(rowColor);
            cellSubTotal.setPadding(10);
            cellSubTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cellSubTotal.setBorder(Rectangle.NO_BORDER);
            table.addCell(cellSubTotal);

            alternate = !alternate;
        }

        document.add(table);
    }

    private static void ajouterSectionTotaux(Document document, Commande commande, List<DetailsCommande> details)
            throws DocumentException {
        PdfPTable totauxTable = new PdfPTable(1);
        totauxTable.setWidthPercentage(100);
        totauxTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totauxTable.setSpacingBefore(10);

        // Calculer le sous-total avant réduction
        double sousTotal = 0.0;
        for (DetailsCommande detail : details) {
            sousTotal += detail.getQuantite() * detail.getPrixUnitaire();
        }

        float fraisLivraison = commande.getFraisLivraison();
        double totalCommande = commande.getTotal();
        // Réduction = sous-total - (total - frais livraison)
        double reduction = sousTotal - (totalCommande - fraisLivraison);

        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, SECONDARY_COLOR);
        Font redFont = FontFactory.getFont(FontFactory.HELVETICA, 12, new BaseColor(231, 76, 60));
        Font blueFont = FontFactory.getFont(FontFactory.HELVETICA, 12, new BaseColor(41, 128, 185));

        if (reduction > 0.01) {
            ajouterLigneTotaux(totauxTable, "Sous-total :", String.format("%.2f DT", sousTotal), normalFont, normalFont,
                    WHITE);
            ajouterLigneTotaux(totauxTable, "Réduction :", String.format("-%.2f DT", reduction), redFont, redFont,
                    WHITE);
        }

        // Frais de livraison
        if (fraisLivraison > 0.0f) {
            ajouterLigneTotaux(totauxTable, "Frais de livraison :",
                    String.format("+%.2f DT", fraisLivraison), blueFont, blueFont, WHITE);
        }

        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, SECONDARY_COLOR);
        String totalText = String.format("TOTAL                    %.2f DT", totalCommande);
        PdfPCell totalCell = new PdfPCell(new Phrase(totalText, totalFont));
        totalCell.setBackgroundColor(LIGHT_GRAY);
        totalCell.setPadding(12);
        totalCell.setBorder(Rectangle.NO_BORDER);
        totalCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        totauxTable.addCell(totalCell);
        document.add(totauxTable);
    }

    private static void ajouterLigneTotaux(PdfPTable table, String label, String value, Font labelFont, Font valueFont,
            BaseColor bgColor) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBackgroundColor(bgColor);
        labelCell.setPadding(8);
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBackgroundColor(bgColor);
        valueCell.setPadding(8);
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private static void ajouterPiedDePage(Document document) throws DocumentException {
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        ajouterLigneSeparation(document, LIGHT_GRAY);

        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new BaseColor(127, 140, 141));
        Font footerBoldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, PRIMARY_COLOR);

        Paragraph merci = new Paragraph("Merci pour votre confiance !", footerBoldFont);
        merci.setAlignment(Element.ALIGN_CENTER);
        merci.setSpacingBefore(10);
        document.add(merci);

        Paragraph contact = new Paragraph("Pour toute question, contactez-nous : contact@ardhi.tn | +216 90 722 197",
                footerFont);
        contact.setAlignment(Element.ALIGN_CENTER);
        contact.setSpacingAfter(10);
        document.add(contact);
    }

    private static void ajouterLigneSeparation(Document document, BaseColor color) throws DocumentException {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBackgroundColor(color);
        lineCell.setFixedHeight(2);
        lineCell.setBorder(Rectangle.NO_BORDER);
        line.addCell(lineCell);
        document.add(line);
    }

    // Classe interne pour gérer les numéros de page
    static class HeaderFooterPageEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfPTable footer = new PdfPTable(1);
            try {
                footer.setWidths(new int[] { 1 });
                footer.setTotalWidth(527);
                footer.setLockedWidth(true);
                footer.getDefaultCell().setBorder(Rectangle.NO_BORDER);
                footer.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);

                Font pageFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new BaseColor(127, 140, 141));
                footer.addCell(new Phrase("Page " + writer.getPageNumber(), pageFont));

                footer.writeSelectedRows(0, -1, 34, 30, writer.getDirectContent());
            } catch (DocumentException de) {
                throw new ExceptionConverter(de);
            }
        }
    }
}