package tn.neuron.ardhi.services.Parcelle_Cultures;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import tn.neuron.ardhi.models.Parcelle_Cultures.CreditDossier;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Service Premium — Export PDF professionnel du Dossier de Crédit Agricole.
 *
 * Utilise iText 5.5.13.3.
 * Supporte 3 langues : Français (fr), العربية (ar — RTL), English (en).
 * Design professionnel conforme à la charte Ardhi (vert #6B7F3F, accent #C4A574).
 */
public class PdfCreditExportService {

    private static final Logger LOGGER = Logger.getLogger(PdfCreditExportService.class.getName());

    // ========================= COULEURS ARDHI =========================
    private static final BaseColor VERT_PRIMAIRE  = new BaseColor(107, 127, 63);   // #6B7F3F
    private static final BaseColor VERT_FONCE     = new BaseColor(74, 90, 43);      // #4A5A2B
    private static final BaseColor ACCENT_OR      = new BaseColor(196, 165, 116);   // #C4A574
    private static final BaseColor GRIS_CLAIR     = new BaseColor(245, 247, 240);   // #F5F7F0
    private static final BaseColor GRIS_TEXTE     = new BaseColor(60, 60, 60);
    private static final BaseColor BLANC          = BaseColor.WHITE;
    private static final BaseColor ROUGE_RISQUE   = new BaseColor(231, 76, 60);
    private static final BaseColor ORANGE_RISQUE  = new BaseColor(243, 156, 18);
    private static final BaseColor VERT_RISQUE    = new BaseColor(46, 204, 113);

    // ========================= POLICES =========================
    private Font fontTitrePrincipal;
    private Font fontTitreSection;
    private Font fontSousTitre;
    private Font fontCorps;
    private Font fontCorpsBold;
    private Font fontPetit;
    private Font fontPetitBold;
    private Font fontTableHeader;
    private Font fontTableCell;
    private Font fontFooter;
    private Font fontMontant;

    // Pour l'arabe
    private BaseFont arabicBaseFont;
    private Font fontArabicTitre;
    private Font fontArabicCorps;
    private Font fontArabicBold;
    private Font fontArabicPetit;

    // ========================= I18N =========================
    private final Map<String, Map<String, String>> translations = new HashMap<>();

    public PdfCreditExportService() {
        initTranslations();
    }

    /**
     * Génère le PDF du dossier de crédit et le sauvegarde au chemin spécifié.
     *
     * @param dossier    CreditDossier complet
     * @param outputPath Chemin absolu du fichier PDF de sortie
     */
    public void genererPdf(CreditDossier dossier, String outputPath) throws DocumentException, IOException {
        LOGGER.info("▶ Génération du PDF crédit → " + outputPath);

        String lang = dossier.getLangue() != null ? dossier.getLangue() : "fr";
        boolean isArabic = "ar".equals(lang);

        // Initialiser les polices
        initFonts(isArabic);

        Document document = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputPath));

        // Footer automatique
        writer.setPageEvent(new FooterPageEvent(lang, fontFooter));

        document.open();

        if (isArabic) {
            writer.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        }

        // === SECTIONS ===
        ajouterEntete(document, writer, dossier, lang);
        document.add(Chunk.NEWLINE);

        ajouterSectionExploitant(document, dossier, lang, isArabic);
        ajouterSectionParcelle(document, dossier, lang, isArabic);
        ajouterSectionCultures(document, dossier, lang, isArabic);
        ajouterSectionFinanciere(document, dossier, lang, isArabic);
        ajouterSectionRemboursement(document, dossier, lang, isArabic);
        ajouterSectionPretRecommande(document, dossier, lang, isArabic);
        ajouterSectionRisque(document, dossier, lang, isArabic);
        ajouterMentionsLegales(document, lang, isArabic);

        document.close();
        LOGGER.info("✅ PDF crédit généré avec succès : " + outputPath);
    }

    // ========================= INITIALISATION POLICES =========================

    private void initFonts(boolean isArabic) {
        try {
            // Polices latines (Helvetica intégrée)
            fontTitrePrincipal = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, VERT_PRIMAIRE);
            fontTitreSection   = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, VERT_FONCE);
            fontSousTitre      = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, ACCENT_OR);
            fontCorps          = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, GRIS_TEXTE);
            fontCorpsBold      = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, GRIS_TEXTE);
            fontPetit          = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.GRAY);
            fontPetitBold      = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, BaseColor.GRAY);
            fontTableHeader    = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BLANC);
            fontTableCell      = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, GRIS_TEXTE);
            fontFooter         = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY);
            fontMontant        = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, VERT_PRIMAIRE);

            if (isArabic) {
                // Charger une police compatible arabe (Arial, Tahoma, Segoe UI)
                String[] arabicFontPaths = {
                    "C:/Windows/Fonts/arial.ttf",
                    "C:/Windows/Fonts/tahoma.ttf",
                    "C:/Windows/Fonts/segoeui.ttf"
                };
                BaseFont bf = null;
                for (String path : arabicFontPaths) {
                    try {
                        bf = BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                        LOGGER.info("✅ Police arabe chargée: " + path);
                        break;
                    } catch (Exception ignored) {}
                }
                if (bf == null) {
                    LOGGER.warning("⚠ Police arabe non trouvée, le texte arabe ne s'affichera pas correctement");
                } else {
                    arabicBaseFont = bf;
                    // IMPORTANT: Remplacer TOUTES les polices par des versions Arabic-capable
                    fontTitrePrincipal = new Font(bf, 22, Font.BOLD, VERT_PRIMAIRE);
                    fontTitreSection   = new Font(bf, 14, Font.BOLD, VERT_FONCE);
                    fontSousTitre      = new Font(bf, 11, Font.NORMAL, ACCENT_OR);
                    fontCorps          = new Font(bf, 10, Font.NORMAL, GRIS_TEXTE);
                    fontCorpsBold      = new Font(bf, 10, Font.BOLD, GRIS_TEXTE);
                    fontPetit          = new Font(bf, 8, Font.NORMAL, BaseColor.GRAY);
                    fontPetitBold      = new Font(bf, 8, Font.BOLD, BaseColor.GRAY);
                    fontTableHeader    = new Font(bf, 9, Font.BOLD, BLANC);
                    fontTableCell      = new Font(bf, 9, Font.NORMAL, GRIS_TEXTE);
                    fontFooter         = new Font(bf, 8, Font.ITALIC, BaseColor.GRAY);
                    fontMontant        = new Font(bf, 16, Font.BOLD, VERT_PRIMAIRE);
                    // Anciennes références arabic aussi (au cas où)
                    fontArabicTitre = fontTitrePrincipal;
                    fontArabicCorps = fontCorps;
                    fontArabicBold  = fontCorpsBold;
                    fontArabicPetit = fontPetit;
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Erreur initialisation polices: " + e.getMessage());
        }
    }

    // ========================= EN-TÊTE =========================

    private void ajouterEntete(Document document, PdfWriter writer, CreditDossier dossier, String lang)
            throws DocumentException {
        // Barre verte en haut
        PdfContentByte cb = writer.getDirectContent();
        cb.setColorFill(VERT_PRIMAIRE);
        cb.rectangle(0, PageSize.A4.getHeight() - 8, PageSize.A4.getWidth(), 8);
        cb.fill();

        // Logo + Titre
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1, 4});

        // Logo
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try {
            InputStream logoStream = getClass().getResourceAsStream("/img/ardhi_logo.png");
            if (logoStream != null) {
                byte[] logoBytes = logoStream.readAllBytes();
                Image logo = Image.getInstance(logoBytes);
                logo.scaleToFit(70, 70);
                logoCell.addElement(logo);
            }
        } catch (Exception e) {
            LOGGER.warning("Logo non trouvé, PDF généré sans logo: " + e.getMessage());
            logoCell.addElement(new Phrase("ARDHI", fontTitrePrincipal));
        }
        headerTable.addCell(logoCell);

        // Titre
        boolean isRtl = "ar".equals(lang);
        PdfPCell titreCell = new PdfPCell();
        titreCell.setBorder(Rectangle.NO_BORDER);
        titreCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (isRtl) { titreCell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL); }

        Paragraph titre = new Paragraph(t(lang, "titre_principal"), fontTitrePrincipal);
        titre.setSpacingAfter(3);
        titreCell.addElement(titre);

        Paragraph sousTitre = new Paragraph(t(lang, "sous_titre"), fontSousTitre);
        titreCell.addElement(sousTitre);

        String dateStr = dossier.getDateGeneration().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        Paragraph dateP = new Paragraph(t(lang, "date_generation") + " : " + dateStr, fontPetit);
        dateP.setSpacingBefore(5);
        titreCell.addElement(dateP);

        Paragraph refP = new Paragraph(t(lang, "ref_dossier") + " : ARDHI-CR-"
                + dossier.getParcelleId() + "-" + dossier.getDateGeneration().getYear(), fontPetitBold);
        titreCell.addElement(refP);

        headerTable.addCell(titreCell);
        document.add(headerTable);

        // Ligne séparatrice
        ajouterLigneSeparatrice(document);
    }

    // ========================= SECTION EXPLOITANT =========================

    private void ajouterSectionExploitant(Document document, CreditDossier dossier, String lang, boolean rtl)
            throws DocumentException {
        ajouterTitreSection(document, "1", t(lang, "section_exploitant"), lang);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 2});
        table.setSpacingBefore(8);
        table.setSpacingAfter(12);

        ajouterLigneInfo(table, t(lang, "nom_complet"), dossier.getNomComplet(), rtl);
        ajouterLigneInfo(table, t(lang, "identifiant"), "AGR-" + String.format("%05d", dossier.getIdExploitant()), rtl);
        ajouterLigneInfo(table, t(lang, "email"), dossier.getEmailExploitant(), rtl);

        document.add(table);
    }

    // ========================= SECTION PARCELLE =========================

    private void ajouterSectionParcelle(Document document, CreditDossier dossier, String lang, boolean rtl)
            throws DocumentException {
        ajouterTitreSection(document, "2", t(lang, "section_parcelle"), lang);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 2});
        table.setSpacingBefore(8);
        table.setSpacingAfter(12);

        ajouterLigneInfo(table, t(lang, "ref_parcelle"), "P-" + dossier.getParcelleId(), rtl);
        ajouterLigneInfo(table, t(lang, "surface"), String.format("%.2f ha", dossier.getSurface()), rtl);
        ajouterLigneInfo(table, t(lang, "localisation"), dossier.getLocalisation(), rtl);
        ajouterLigneInfo(table, t(lang, "type_sol"), dossier.getTypeSol(), rtl);
        ajouterLigneInfo(table, t(lang, "irrigation"), dossier.getSystemeIrrigation(), rtl);
        ajouterLigneInfo(table, t(lang, "statut"), dossier.getStatutParcelle(), rtl);

        if (dossier.getLatitude() != null && dossier.getLongitude() != null) {
            ajouterLigneInfo(table, t(lang, "coordonnees_gps"),
                    String.format("%.5f, %.5f", dossier.getLatitude(), dossier.getLongitude()), rtl);
        }

        document.add(table);
    }

    // ========================= SECTION CULTURES =========================

    private void ajouterSectionCultures(Document document, CreditDossier dossier, String lang, boolean rtl)
            throws DocumentException {
        ajouterTitreSection(document, "3", t(lang, "section_cultures"), lang);

        if (dossier.getCultures().isEmpty()) {
            Paragraph vide = new Paragraph(t(lang, "aucune_culture"), fontCorps);
            vide.setSpacingBefore(8);
            vide.setSpacingAfter(12);
            ajouterParagrapheRtl(document, vide, rtl);
            return;
        }

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2, 1.2f, 1, 1, 1.2f, 1.2f});
        table.setSpacingBefore(8);
        table.setSpacingAfter(12);

        // Headers
        String[] headers = {
            t(lang, "culture_nom"), t(lang, "culture_type"), t(lang, "culture_saison"),
            t(lang, "culture_etat"), t(lang, "culture_surface"), t(lang, "culture_production")
        };
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fontTableHeader));
            cell.setBackgroundColor(VERT_PRIMAIRE);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBorderColor(VERT_FONCE);
            if (rtl) { cell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL); }
            table.addCell(cell);
        }

        // Rows
        boolean even = false;
        for (CreditDossier.CultureInfo ci : dossier.getCultures()) {
            BaseColor rowBg = even ? GRIS_CLAIR : BLANC;
            ajouterCelluleTableau(table, ci.getNom(), rowBg);
            ajouterCelluleTableau(table, ci.getType(), rowBg);
            ajouterCelluleTableau(table, ci.getSaison(), rowBg);
            ajouterCelluleTableau(table, ci.getEtat(), rowBg);
            ajouterCelluleTableau(table, String.format("%.2f ha", ci.getSurfaceUtilisee()), rowBg);
            ajouterCelluleTableau(table, String.format("%.1f t", ci.getProductionEstimee()), rowBg);
            even = !even;
        }

        // Total row
        PdfPCell totalLabel = new PdfPCell(new Phrase(t(lang, "total"), fontCorpsBold));
        totalLabel.setColspan(5);
        totalLabel.setPadding(6);
        totalLabel.setBackgroundColor(new BaseColor(232, 245, 233)); // #E8F5E9
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabel.setBorderColor(BaseColor.LIGHT_GRAY);
        if (rtl) { totalLabel.setRunDirection(PdfWriter.RUN_DIRECTION_RTL); }
        table.addCell(totalLabel);

        PdfPCell totalVal = new PdfPCell(new Phrase(
                String.format("%.1f t", dossier.getProductionTotale()), fontCorpsBold));
        totalVal.setPadding(6);
        totalVal.setBackgroundColor(new BaseColor(232, 245, 233));
        totalVal.setHorizontalAlignment(Element.ALIGN_CENTER);
        totalVal.setBorderColor(BaseColor.LIGHT_GRAY);
        table.addCell(totalVal);

        document.add(table);
    }

    // ========================= SECTION FINANCIÈRE =========================

    private void ajouterSectionFinanciere(Document document, CreditDossier dossier, String lang, boolean rtl)
            throws DocumentException {
        ajouterTitreSection(document, "4", t(lang, "section_financiere"), lang);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.5f, 1});
        table.setSpacingBefore(8);
        table.setSpacingAfter(12);

        ajouterLigneFinanciere(table, t(lang, "couts_totaux"), dossier.getCoutsTotaux(), "DT", false);
        ajouterLigneFinanciere(table, t(lang, "chiffre_affaires"), dossier.getChiffreAffaires(), "DT", false);

        // Marge brute  — mise en valeur
        PdfPCell labelMarge = new PdfPCell(new Phrase(t(lang, "marge_brute"), fontCorpsBold));
        labelMarge.setPadding(8);
        labelMarge.setBackgroundColor(new BaseColor(232, 245, 233));
        labelMarge.setBorderColor(BaseColor.LIGHT_GRAY);
        if (rtl) { labelMarge.setRunDirection(PdfWriter.RUN_DIRECTION_RTL); }
        table.addCell(labelMarge);

        BaseColor margeColor = dossier.getMargeBrute() >= 0 ? VERT_PRIMAIRE : ROUGE_RISQUE;
        Font fontMarge = (arabicBaseFont != null && rtl)
                ? new Font(arabicBaseFont, 11, Font.BOLD, margeColor)
                : new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, margeColor);
        PdfPCell valMarge = new PdfPCell(new Phrase(String.format("%,.2f DT", dossier.getMargeBrute()), fontMarge));
        valMarge.setPadding(8);
        valMarge.setBackgroundColor(new BaseColor(232, 245, 233));
        valMarge.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valMarge.setBorderColor(BaseColor.LIGHT_GRAY);
        table.addCell(valMarge);

        ajouterLigneFinanciere(table, t(lang, "roi"), dossier.getRoi(), "%", false);
        ajouterLigneFinanciere(table, t(lang, "facteur_climatique"),
                dossier.getFacteurClimatique() * 100, "%", false);

        document.add(table);
    }

    // ========================= SECTION REMBOURSEMENT =========================

    private void ajouterSectionRemboursement(Document document, CreditDossier dossier, String lang, boolean rtl)
            throws DocumentException {
        ajouterTitreSection(document, "5", t(lang, "section_remboursement"), lang);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.5f, 1});
        table.setSpacingBefore(8);
        table.setSpacingAfter(12);

        ajouterLigneFinanciere(table, t(lang, "capacite_annuelle"), dossier.getCapaciteRemboursement(), "DT/an", false);
        ajouterLigneInfo(table, t(lang, "duree_emprunt"), dossier.getDureeEmpruntAnnees() + " " + t(lang, "annees"), rtl);

        document.add(table);
    }

    // ========================= SECTION PRÊT RECOMMANDÉ =========================

    private void ajouterSectionPretRecommande(Document document, CreditDossier dossier, String lang, boolean rtl)
            throws DocumentException {
        ajouterTitreSection(document, "6", t(lang, "section_pret"), lang);

        // Encadré vert pour le montant
        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(70);
        box.setHorizontalAlignment(Element.ALIGN_CENTER);
        box.setSpacingBefore(10);
        box.setSpacingAfter(15);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(GRIS_CLAIR);
        cell.setBorderColor(VERT_PRIMAIRE);
        cell.setBorderWidth(2);
        cell.setPadding(20);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph labelP = new Paragraph(t(lang, "montant_pret_max"), fontCorpsBold);
        labelP.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(labelP);

        Paragraph montantP = new Paragraph(String.format("%,.2f DT", dossier.getMontantPretMax()), fontMontant);
        montantP.setAlignment(Element.ALIGN_CENTER);
        montantP.setSpacingBefore(8);
        cell.addElement(montantP);

        Paragraph formuleP = new Paragraph(
                t(lang, "formule_pret"), fontPetit);
        formuleP.setAlignment(Element.ALIGN_CENTER);
        formuleP.setSpacingBefore(6);
        cell.addElement(formuleP);
        if (rtl) { cell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL); }

        box.addCell(cell);
        document.add(box);
    }

    // ========================= SECTION RISQUE =========================

    private void ajouterSectionRisque(Document document, CreditDossier dossier, String lang, boolean rtl)
            throws DocumentException {
        ajouterTitreSection(document, "7", t(lang, "section_risque"), lang);

        // Score global avec indicateur coloré
        PdfPTable scoreTable = new PdfPTable(3);
        scoreTable.setWidthPercentage(80);
        scoreTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        scoreTable.setWidths(new float[]{1.5f, 1, 1.5f});
        scoreTable.setSpacingBefore(10);
        scoreTable.setSpacingAfter(10);

        // Label
        PdfPCell labelCell = new PdfPCell(new Phrase(t(lang, "score_global"), fontCorpsBold));
        labelCell.setPadding(12);
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (rtl) { labelCell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL); }
        scoreTable.addCell(labelCell);

        // Score numérique
        BaseColor scoreColor = getScoreColor(dossier.getScoreRisque());
        Font fontScore = (arabicBaseFont != null && rtl)
                ? new Font(arabicBaseFont, 24, Font.BOLD, scoreColor)
                : new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, scoreColor);
        PdfPCell scoreCell = new PdfPCell(new Phrase(String.format("%.1f/10", dossier.getScoreRisque()), fontScore));
        scoreCell.setPadding(12);
        scoreCell.setBorder(Rectangle.NO_BORDER);
        scoreCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        scoreCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        scoreTable.addCell(scoreCell);

        // Badge niveau
        String niveauTrad = t(lang, "risque_" + dossier.getNiveauRisque().toLowerCase());
        Font fontNiveau = (arabicBaseFont != null && rtl)
                ? new Font(arabicBaseFont, 12, Font.BOLD, BLANC)
                : new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BLANC);
        PdfPCell niveauCell = new PdfPCell(new Phrase(niveauTrad, fontNiveau));
        niveauCell.setBackgroundColor(scoreColor);
        niveauCell.setPadding(10);
        niveauCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        niveauCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        niveauCell.setBorderColor(scoreColor);
        if (rtl) { niveauCell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL); }
        scoreTable.addCell(niveauCell);

        document.add(scoreTable);

        // Détails des sous-scores
        PdfPTable detailTable = new PdfPTable(2);
        detailTable.setWidthPercentage(100);
        detailTable.setWidths(new float[]{2, 1});
        detailTable.setSpacingBefore(8);
        detailTable.setSpacingAfter(12);

        ajouterLigneScore(detailTable, t(lang, "score_rentabilite") + " (40%)", dossier.getScoreRentabilite());
        ajouterLigneScore(detailTable, t(lang, "score_climat") + " (30%)", dossier.getScoreStabiliteClimat());
        ajouterLigneScore(detailTable, t(lang, "score_diversification") + " (20%)", dossier.getScoreDiversification());
        ajouterLigneScore(detailTable, t(lang, "score_historique") + " (10%)", dossier.getScoreHistorique());

        document.add(detailTable);

        // Formule
        Paragraph formule = new Paragraph(
                t(lang, "formule_risque"), fontPetit);
        formule.setAlignment(Element.ALIGN_CENTER);
        formule.setSpacingAfter(10);
        ajouterParagrapheRtl(document, formule, rtl);
    }

    // ========================= MENTIONS LÉGALES =========================

    private void ajouterMentionsLegales(Document document, String lang, boolean rtl) throws DocumentException {
        ajouterLigneSeparatrice(document);

        Paragraph mentions = new Paragraph();
        mentions.setSpacingBefore(10);

        Font fontDisclaimer = (arabicBaseFont != null && rtl)
                ? new Font(arabicBaseFont, 7, Font.ITALIC, BaseColor.GRAY)
                : new Font(Font.FontFamily.HELVETICA, 7, Font.ITALIC, BaseColor.GRAY);
        mentions.add(new Chunk(t(lang, "mentions_1"), fontDisclaimer));
        mentions.add(Chunk.NEWLINE);
        mentions.add(new Chunk(t(lang, "mentions_2"), fontDisclaimer));
        mentions.add(Chunk.NEWLINE);
        mentions.add(new Chunk(t(lang, "mentions_3"), fontDisclaimer));

        ajouterParagrapheRtl(document, mentions, rtl);
    }

    // ========================= UTILITAIRES =========================

    /**
     * Ajoute un paragraphe au document en le wrappant dans une PdfPCell RTL si nécessaire.
     * Ceci est INDISPENSABLE pour le rendu correct de l'arabe (jointure des lettres) dans iText 5.
     */
    private void ajouterParagrapheRtl(Document document, Paragraph p, boolean rtl) throws DocumentException {
        if (rtl) {
            PdfPTable wrapper = new PdfPTable(1);
            wrapper.setWidthPercentage(100);
            wrapper.setSpacingBefore(p.getSpacingBefore());
            wrapper.setSpacingAfter(p.getSpacingAfter());
            p.setSpacingBefore(0);
            p.setSpacingAfter(0);
            PdfPCell cell = new PdfPCell();
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPadding(0);
            cell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
            cell.addElement(p);
            wrapper.addCell(cell);
            document.add(wrapper);
        } else {
            document.add(p);
        }
    }

    private void ajouterTitreSection(Document document, String numero, String titre, String lang)
            throws DocumentException {
        boolean rtl = "ar".equals(lang);
        PdfPTable sectionTable = new PdfPTable(1);
        sectionTable.setWidthPercentage(100);
        sectionTable.setSpacingBefore(10);

        PdfPCell cell = new PdfPCell();
        cell.setBorderWidthTop(0);
        cell.setBorderWidthRight(0);
        cell.setBorderWidthBottom(2);
        cell.setBorderWidthLeft(4);
        cell.setBorderColorLeft(VERT_PRIMAIRE);
        cell.setBorderColorBottom(GRIS_CLAIR);
        cell.setPadding(8);
        cell.setBackgroundColor(BLANC);

        Paragraph p = new Paragraph();
        Font numFont = (arabicBaseFont != null)
                ? new Font(arabicBaseFont, 12, Font.BOLD, ACCENT_OR)
                : new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, ACCENT_OR);
        p.add(new Chunk(numero + ".  ", numFont));
        p.add(new Chunk(titre, fontTitreSection));
        cell.addElement(p);
        if (rtl) { cell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL); }

        sectionTable.addCell(cell);
        document.add(sectionTable);
    }

    private void ajouterLigneInfo(PdfPTable table, String label, String valeur, boolean rtl) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, fontCorpsBold));
        labelCell.setPadding(6);
        labelCell.setBackgroundColor(GRIS_CLAIR);
        labelCell.setBorderColor(BaseColor.LIGHT_GRAY);
        if (rtl) { labelCell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL); }
        table.addCell(labelCell);

        PdfPCell valCell = new PdfPCell(new Phrase(valeur != null ? valeur : "—", fontCorps));
        valCell.setPadding(6);
        valCell.setBorderColor(BaseColor.LIGHT_GRAY);
        if (rtl) { valCell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL); }
        table.addCell(valCell);
    }

    private void ajouterLigneFinanciere(PdfPTable table, String label, double valeur, String unite, boolean highlight) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, fontCorpsBold));
        labelCell.setPadding(7);
        labelCell.setBackgroundColor(GRIS_CLAIR);
        labelCell.setBorderColor(BaseColor.LIGHT_GRAY);
        if (arabicBaseFont != null) { labelCell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL); }
        table.addCell(labelCell);

        String valStr = String.format("%,.2f %s", valeur, unite);
        Font f = highlight ? fontCorpsBold : fontCorps;
        PdfPCell valCell = new PdfPCell(new Phrase(valStr, f));
        valCell.setPadding(7);
        valCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valCell.setBorderColor(BaseColor.LIGHT_GRAY);
        table.addCell(valCell);
    }

    private void ajouterLigneScore(PdfPTable table, String label, double score) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, fontCorps));
        labelCell.setPadding(6);
        labelCell.setBackgroundColor(GRIS_CLAIR);
        labelCell.setBorderColor(BaseColor.LIGHT_GRAY);
        if (arabicBaseFont != null) { labelCell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL); }
        table.addCell(labelCell);

        BaseColor color = getScoreColor(score);
        Font f = (arabicBaseFont != null)
                ? new Font(arabicBaseFont, 10, Font.BOLD, color)
                : new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, color);
        PdfPCell valCell = new PdfPCell(new Phrase(String.format("%.1f / 10", score), f));
        valCell.setPadding(6);
        valCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        valCell.setBorderColor(BaseColor.LIGHT_GRAY);
        table.addCell(valCell);
    }

    private void ajouterCelluleTableau(PdfPTable table, String texte, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(texte != null ? texte : "—", fontTableCell));
        cell.setPadding(5);
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorderColor(BaseColor.LIGHT_GRAY);
        if (arabicBaseFont != null) { cell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL); }
        table.addCell(cell);
    }

    private void ajouterLigneSeparatrice(Document document) throws DocumentException {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        line.setSpacingBefore(5);
        line.setSpacingAfter(5);

        PdfPCell cell = new PdfPCell();
        cell.setBorderWidthTop(0);
        cell.setBorderWidthRight(0);
        cell.setBorderWidthBottom(1);
        cell.setBorderWidthLeft(0);
        cell.setBorderColorBottom(ACCENT_OR);
        cell.setFixedHeight(1);
        line.addCell(cell);

        document.add(line);
    }

    private BaseColor getScoreColor(double score) {
        if (score >= 7) return VERT_RISQUE;
        if (score >= 4) return ORANGE_RISQUE;
        return ROUGE_RISQUE;
    }

    // ========================= I18N =========================

    private String t(String lang, String key) {
        Map<String, String> langMap = translations.getOrDefault(lang, translations.get("fr"));
        return langMap.getOrDefault(key, key);
    }

    private void initTranslations() {
        // ======== FRANÇAIS ========
        Map<String, String> fr = new HashMap<>();
        fr.put("titre_principal", "Dossier de Crédit Agricole");
        fr.put("sous_titre", "Analyse financière et évaluation des risques — Plateforme Ardhi");
        fr.put("date_generation", "Date de génération");
        fr.put("ref_dossier", "Référence");
        fr.put("section_exploitant", "Informations de l'Exploitant");
        fr.put("nom_complet", "Nom complet");
        fr.put("identifiant", "Identifiant agriculteur");
        fr.put("email", "Email");
        fr.put("section_parcelle", "Caractéristiques de la Parcelle");
        fr.put("ref_parcelle", "Référence parcelle");
        fr.put("surface", "Surface exploitable");
        fr.put("localisation", "Localisation");
        fr.put("type_sol", "Type de sol");
        fr.put("irrigation", "Système d'irrigation");
        fr.put("statut", "Statut");
        fr.put("coordonnees_gps", "Coordonnées GPS");
        fr.put("section_cultures", "Production Agricole");
        fr.put("aucune_culture", "Aucune culture enregistrée sur cette parcelle.");
        fr.put("culture_nom", "Culture");
        fr.put("culture_type", "Type");
        fr.put("culture_saison", "Saison");
        fr.put("culture_etat", "État");
        fr.put("culture_surface", "Surface (ha)");
        fr.put("culture_production", "Production (t)");
        fr.put("total", "TOTAL");
        fr.put("section_financiere", "Analyse Financière");
        fr.put("couts_totaux", "Coûts totaux de production");
        fr.put("chiffre_affaires", "Chiffre d'affaires estimé");
        fr.put("marge_brute", "Marge brute");
        fr.put("roi", "Retour sur investissement (ROI)");
        fr.put("facteur_climatique", "Facteur climatique favorable");
        fr.put("section_remboursement", "Capacité de Remboursement");
        fr.put("capacite_annuelle", "Capacité annuelle de remboursement");
        fr.put("duree_emprunt", "Durée de l'emprunt");
        fr.put("annees", "ans");
        fr.put("section_pret", "Prêt Recommandé");
        fr.put("montant_pret_max", "Montant maximum du prêt recommandé");
        fr.put("formule_pret", "Calcul : Marge brute × 60% × Durée emprunt");
        fr.put("section_risque", "Évaluation du Risque Bancaire");
        fr.put("score_global", "Score de risque global");
        fr.put("risque_faible", "RISQUE FAIBLE");
        fr.put("risque_modéré", "RISQUE MODÉRÉ");
        fr.put("risque_élevé", "RISQUE ÉLEVÉ");
        fr.put("score_rentabilite", "Rentabilité");
        fr.put("score_climat", "Stabilité climatique");
        fr.put("score_diversification", "Diversification");
        fr.put("score_historique", "Historique exploitation");
        fr.put("formule_risque", "Score = 0.4×Rentabilité + 0.3×Climat + 0.2×Diversification + 0.1×Historique");
        fr.put("mentions_1", "Ce document est généré automatiquement par la plateforme Ardhi à titre indicatif.");
        fr.put("mentions_2", "Les données financières sont basées sur des estimations et ne constituent pas un engagement contractuel.");
        fr.put("mentions_3", "Document confidentiel — Toute reproduction non autorisée est interdite.");
        fr.put("footer_page", "Page");
        fr.put("footer_confidentiel", "CONFIDENTIEL — Dossier Crédit Agricole Ardhi");
        translations.put("fr", fr);

        // ======== ARABE ========
        Map<String, String> ar = new HashMap<>();
        ar.put("titre_principal", "ملف القرض الفلاحي");
        ar.put("sous_titre", "التحليل المالي وتقييم المخاطر — منصة أرضي");
        ar.put("date_generation", "تاريخ الإنشاء");
        ar.put("ref_dossier", "المرجع");
        ar.put("section_exploitant", "معلومات المستغل");
        ar.put("nom_complet", "الاسم الكامل");
        ar.put("identifiant", "معرّف الفلاح");
        ar.put("email", "البريد الإلكتروني");
        ar.put("section_parcelle", "خصائص القطعة الأرضية");
        ar.put("ref_parcelle", "مرجع القطعة");
        ar.put("surface", "المساحة القابلة للاستغلال");
        ar.put("localisation", "الموقع");
        ar.put("type_sol", "نوع التربة");
        ar.put("irrigation", "نظام الري");
        ar.put("statut", "الحالة");
        ar.put("coordonnees_gps", "الإحداثيات الجغرافية");
        ar.put("section_cultures", "الإنتاج الفلاحي");
        ar.put("aucune_culture", "لا توجد زراعات مسجلة على هذه القطعة.");
        ar.put("culture_nom", "الزراعة");
        ar.put("culture_type", "النوع");
        ar.put("culture_saison", "الموسم");
        ar.put("culture_etat", "الحالة");
        ar.put("culture_surface", "المساحة (هك)");
        ar.put("culture_production", "الإنتاج (ط)");
        ar.put("total", "المجموع");
        ar.put("section_financiere", "التحليل المالي");
        ar.put("couts_totaux", "إجمالي تكاليف الإنتاج");
        ar.put("chiffre_affaires", "رقم المعاملات المقدّر");
        ar.put("marge_brute", "الهامش الإجمالي");
        ar.put("roi", "العائد على الاستثمار");
        ar.put("facteur_climatique", "العامل المناخي الملائم");
        ar.put("section_remboursement", "القدرة على السداد");
        ar.put("capacite_annuelle", "القدرة السنوية على السداد");
        ar.put("duree_emprunt", "مدة القرض");
        ar.put("annees", "سنوات");
        ar.put("section_pret", "القرض الموصى به");
        ar.put("montant_pret_max", "الحد الأقصى للقرض الموصى به");
        ar.put("formule_pret", "الحساب: الهامش الإجمالي × 60% × مدة القرض");
        ar.put("section_risque", "تقييم المخاطر البنكية");
        ar.put("score_global", "نقاط المخاطر الإجمالية");
        ar.put("risque_faible", "مخاطر منخفضة");
        ar.put("risque_modéré", "مخاطر متوسطة");
        ar.put("risque_élevé", "مخاطر مرتفعة");
        ar.put("score_rentabilite", "الربحية");
        ar.put("score_climat", "الاستقرار المناخي");
        ar.put("score_diversification", "التنويع");
        ar.put("score_historique", "تاريخ الاستغلال");
        ar.put("formule_risque", "النقاط = 0.4×الربحية + 0.3×المناخ + 0.2×التنويع + 0.1×التاريخ");
        ar.put("mentions_1", "تم إنشاء هذا المستند تلقائيًا بواسطة منصة أرضي للاستئناس فقط.");
        ar.put("mentions_2", "البيانات المالية مبنية على تقديرات ولا تمثل التزامًا تعاقديًا.");
        ar.put("mentions_3", "وثيقة سرية — يُحظر أي نسخ غير مرخص.");
        ar.put("footer_page", "صفحة");
        ar.put("footer_confidentiel", "سري — ملف القرض الفلاحي أرضي");
        translations.put("ar", ar);

        // ======== ANGLAIS ========
        Map<String, String> en = new HashMap<>();
        en.put("titre_principal", "Agricultural Credit Report");
        en.put("sous_titre", "Financial Analysis & Risk Assessment — Ardhi Platform");
        en.put("date_generation", "Generation date");
        en.put("ref_dossier", "Reference");
        en.put("section_exploitant", "Farmer Information");
        en.put("nom_complet", "Full name");
        en.put("identifiant", "Farmer ID");
        en.put("email", "Email");
        en.put("section_parcelle", "Plot Characteristics");
        en.put("ref_parcelle", "Plot reference");
        en.put("surface", "Usable area");
        en.put("localisation", "Location");
        en.put("type_sol", "Soil type");
        en.put("irrigation", "Irrigation system");
        en.put("statut", "Status");
        en.put("coordonnees_gps", "GPS coordinates");
        en.put("section_cultures", "Agricultural Production");
        en.put("aucune_culture", "No crops registered on this plot.");
        en.put("culture_nom", "Crop");
        en.put("culture_type", "Type");
        en.put("culture_saison", "Season");
        en.put("culture_etat", "State");
        en.put("culture_surface", "Area (ha)");
        en.put("culture_production", "Production (t)");
        en.put("total", "TOTAL");
        en.put("section_financiere", "Financial Analysis");
        en.put("couts_totaux", "Total production costs");
        en.put("chiffre_affaires", "Estimated revenue");
        en.put("marge_brute", "Gross margin");
        en.put("roi", "Return on Investment (ROI)");
        en.put("facteur_climatique", "Favorable climate factor");
        en.put("section_remboursement", "Repayment Capacity");
        en.put("capacite_annuelle", "Annual repayment capacity");
        en.put("duree_emprunt", "Loan duration");
        en.put("annees", "years");
        en.put("section_pret", "Recommended Loan");
        en.put("montant_pret_max", "Maximum recommended loan amount");
        en.put("formule_pret", "Calculation: Gross margin × 60% × Loan duration");
        en.put("section_risque", "Bank Risk Assessment");
        en.put("score_global", "Overall risk score");
        en.put("risque_faible", "LOW RISK");
        en.put("risque_modéré", "MODERATE RISK");
        en.put("risque_élevé", "HIGH RISK");
        en.put("score_rentabilite", "Profitability");
        en.put("score_climat", "Climate stability");
        en.put("score_diversification", "Diversification");
        en.put("score_historique", "Operating history");
        en.put("formule_risque", "Score = 0.4×Profitability + 0.3×Climate + 0.2×Diversification + 0.1×History");
        en.put("mentions_1", "This document is automatically generated by the Ardhi platform for informational purposes only.");
        en.put("mentions_2", "Financial data is based on estimates and does not constitute a contractual commitment.");
        en.put("mentions_3", "Confidential document — Unauthorized reproduction is prohibited.");
        en.put("footer_page", "Page");
        en.put("footer_confidentiel", "CONFIDENTIAL — Ardhi Agricultural Credit Report");
        translations.put("en", en);
    }

    // ========================= FOOTER EVENT =========================

    /**
     * Événement iText pour ajouter un pied de page sur chaque page.
     */
    private static class FooterPageEvent extends PdfPageEventHelper {
        private final String lang;
        private final Font footerFont;

        FooterPageEvent(String lang, Font footerFont) {
            this.lang = lang;
            this.footerFont = footerFont;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            boolean isAr = "ar".equals(lang);

            // Barre verte en bas
            cb.setColorFill(new BaseColor(107, 127, 63));
            cb.rectangle(0, 0, PageSize.A4.getWidth(), 4);
            cb.fill();

            // Numéro de page
            String pageText;
            if (isAr) {
                pageText = "صفحة " + writer.getPageNumber();
            } else if ("en".equals(lang)) {
                pageText = "Page " + writer.getPageNumber();
            } else {
                pageText = "Page " + writer.getPageNumber();
            }

            // Mention confidentiel
            String confText;
            if (isAr) {
                confText = "سري — ملف القرض الفلاحي أرضي";
            } else if ("en".equals(lang)) {
                confText = "CONFIDENTIAL — Ardhi Agricultural Credit Report";
            } else {
                confText = "CONFIDENTIEL — Dossier Crédit Agricole Ardhi";
            }

            if (isAr) {
                // Pour l'arabe, utiliser ColumnText avec RTL pour la jointure correcte des lettres
                float y = document.bottom() - 20;

                // Page number (à gauche en RTL)
                ColumnText ctPage = new ColumnText(cb);
                ctPage.setSimpleColumn(document.right() - 120, y - 5, document.right(), y + 12);
                ctPage.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
                ctPage.setAlignment(Element.ALIGN_RIGHT);
                ctPage.addElement(new Phrase(pageText, footerFont));
                try { ctPage.go(); } catch (DocumentException ignored) {}

                // Confidential (à droite en RTL)
                ColumnText ctConf = new ColumnText(cb);
                ctConf.setSimpleColumn(document.left(), y - 5, document.left() + 300, y + 12);
                ctConf.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
                ctConf.setAlignment(Element.ALIGN_LEFT);
                ctConf.addElement(new Phrase(confText, footerFont));
                try { ctConf.go(); } catch (DocumentException ignored) {}
            } else {
                // Pour les langues latines, showTextAligned fonctionne bien
                Phrase pagePhrase = new Phrase(pageText, footerFont);
                ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, pagePhrase,
                        document.right(), document.bottom() - 20, 0);

                Phrase confPhrase = new Phrase(confText, footerFont);
                ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, confPhrase,
                        document.left(), document.bottom() - 20, 0);
            }
        }
    }
}
