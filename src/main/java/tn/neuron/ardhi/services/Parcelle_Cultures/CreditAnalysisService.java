package tn.neuron.ardhi.services.Parcelle_Cultures;

import tn.neuron.ardhi.models.Parcelle_Cultures.*;
import tn.neuron.ardhi.models.UserAndDiag.User;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service Premium — Analyse de Crédit Agricole.
 *
 * Architecture : Controller → CreditAnalysisService → FinancialService / CultureService / StatisticsService → DB
 *
 * Formules implémentées :
 *   Production        = Surface × Rendement
 *   CA                = Production × PrixUnitaire
 *   Marge             = CA − CoûtsTotaux
 *   ROI               = (Marge / CoûtsTotaux) × 100
 *   CapacitéRembours. = Marge × 0.6
 *   MontantPrêtMax    = CapacitéRemb. × DuréeAnnées
 *   ScoreRisque       = 0.4×Rentabilité + 0.3×StabilitéClimat + 0.2×Diversification + 0.1×Historique
 */
public class CreditAnalysisService {

    private static final Logger LOGGER = Logger.getLogger(CreditAnalysisService.class.getName());

    private final ParcelleService parcelleService;
    private final CultureService cultureService;
    private final FinancialService financialService;
    private final AgricultureStatisticsService statsService;

    // Paramètres par défaut pour le calcul ROI (estimations moyennes Tunisie)
    private static final double PRIX_VENTE_DEFAUT     = 800.0;   // DT/tonne
    private static final double COUT_SEMENCES_DEFAUT   = 500.0;   // DT
    private static final double COUT_ENGRAIS_DEFAUT    = 400.0;   // DT
    private static final double COUT_MAIN_OEUVRE_DEFAUT = 600.0;  // DT
    private static final double COUT_IRRIGATION_DEFAUT = 300.0;   // DT
    private static final double COUT_AUTRES_DEFAUT     = 200.0;   // DT

    // Coefficient d'affectation au remboursement (60% de la marge)
    private static final double COEFF_REMBOURSEMENT = 0.60;

    public CreditAnalysisService() {
        this.parcelleService = new ParcelleService();
        this.cultureService = new CultureService();
        this.financialService = new FinancialService();
        this.statsService = new AgricultureStatisticsService();
    }

    /**
     * Génère un dossier de crédit complet pour une parcelle donnée.
     *
     * @param parcelle Parcelle sélectionnée
     * @param user     Utilisateur courant (agriculteur)
     * @param dureeAnnees Durée souhaitée du prêt (défaut: 5)
     * @param langue   Code langue (fr/ar/en)
     * @return CreditDossier rempli avec toutes les données calculées
     */
    public CreditDossier genererDossier(Parcelle parcelle, User user, int dureeAnnees, String langue) throws SQLException {
        LOGGER.info("▶ Génération dossier crédit pour parcelle #" + parcelle.getId());

        CreditDossier dossier = new CreditDossier();
        dossier.setLangue(langue);
        dossier.setDureeEmpruntAnnees(dureeAnnees);

        // === 1. Informations exploitant ===
        dossier.setIdExploitant(user.getId());
        dossier.setNomExploitant(user.getNom());
        dossier.setPrenomExploitant(user.getPrenom());
        dossier.setEmailExploitant(user.getEmail());

        // === 2. Informations parcelle ===
        dossier.setParcelleId(parcelle.getId());
        dossier.setSurface(parcelle.getSurface());
        dossier.setLocalisation(parcelle.getLocalisation());
        dossier.setTypeSol(parcelle.getTypeSol());
        dossier.setSystemeIrrigation(parcelle.getSystemeIrrigation());
        dossier.setStatutParcelle(parcelle.getStatut());
        dossier.setLatitude(parcelle.getLatitude());
        dossier.setLongitude(parcelle.getLongitude());

        // === 3. Cultures associées ===
        List<Culture> cultures = cultureService.recupererParParcelle(parcelle.getId());
        double coutTotalGlobal = 0;
        double caTotal = 0;

        for (Culture c : cultures) {
            CreditDossier.CultureInfo ci = new CreditDossier.CultureInfo();
            ci.setNom(c.getNomCulture());
            ci.setType(c.getTypeCulture());
            ci.setSaison(c.getSaison());
            ci.setEtat(c.getEtatCulture());
            ci.setSurfaceUtilisee(c.getSurfaceUtilisee());
            ci.setRendement(c.getRendementEstime());
            ci.setProductionEstimee(c.getSurfaceUtilisee() * c.getRendementEstime());
            dossier.getCultures().add(ci);

            // Calcul ROI par culture pour agréger les financials
            try {
                RoiResult roi = financialService.calculerRoi(c, parcelle,
                        PRIX_VENTE_DEFAUT, COUT_SEMENCES_DEFAUT, COUT_ENGRAIS_DEFAUT,
                        COUT_MAIN_OEUVRE_DEFAUT, COUT_IRRIGATION_DEFAUT, COUT_AUTRES_DEFAUT);

                coutTotalGlobal += roi.getCoutTotal();
                caTotal += roi.getRevenuBrut();
                dossier.setFacteurClimatique(roi.getFacteurClimatique());
            } catch (Exception e) {
                LOGGER.warning("ROI fallback pour culture " + c.getNomCulture() + ": " + e.getMessage());
                // Fallback: estimation simple
                double prod = c.getSurfaceUtilisee() * c.getRendementEstime();
                caTotal += prod * PRIX_VENTE_DEFAUT;
                coutTotalGlobal += COUT_SEMENCES_DEFAUT + COUT_ENGRAIS_DEFAUT
                        + COUT_MAIN_OEUVRE_DEFAUT + COUT_IRRIGATION_DEFAUT + COUT_AUTRES_DEFAUT;
            }
        }

        // Si aucune culture, estimations basées sur la surface
        if (cultures.isEmpty()) {
            double prodEstimee = parcelle.getSurface() * 3.0; // 3 t/ha par défaut
            caTotal = prodEstimee * PRIX_VENTE_DEFAUT;
            coutTotalGlobal = (COUT_SEMENCES_DEFAUT + COUT_ENGRAIS_DEFAUT
                    + COUT_MAIN_OEUVRE_DEFAUT + COUT_IRRIGATION_DEFAUT + COUT_AUTRES_DEFAUT)
                    * parcelle.getSurface();
            dossier.setFacteurClimatique(0.85);
        }

        // === 4. Analyse financière ===
        double marge = caTotal - coutTotalGlobal;
        double roiPct = coutTotalGlobal > 0 ? (marge / coutTotalGlobal) * 100 : 0;

        dossier.setCoutsTotaux(coutTotalGlobal);
        dossier.setChiffreAffaires(caTotal);
        dossier.setMargeBrute(marge);
        dossier.setRoi(roiPct);

        // === 5. Capacité de remboursement ===
        double capacite = Math.max(0, marge * COEFF_REMBOURSEMENT);
        double montantMax = capacite * dureeAnnees;

        dossier.setCapaciteRemboursement(capacite);
        dossier.setMontantPretMax(montantMax);

        // === 6. Score de risque bancaire ===
        calculerScoreRisque(dossier, user.getId(), roiPct);

        LOGGER.info("✅ Dossier crédit généré: marge=" + String.format("%.2f", marge)
                + " DT, prêt max=" + String.format("%.2f", montantMax) + " DT, risque=" + dossier.getNiveauRisque());

        return dossier;
    }

    /**
     * Calcule le score de risque bancaire pondéré.
     *
     * ScoreRisque = 0.4 × Rentabilité + 0.3 × StabilitéClimatique + 0.2 × Diversification + 0.1 × Historique
     *
     * Classement : ≥7 = Faible, 4-7 = Modéré, <4 = Élevé
     */
    private void calculerScoreRisque(CreditDossier dossier, int agriculteurId, double roiPct) {
        // Score Rentabilité (0..10) basé sur le ROI
        double scoreRentabilite = Math.min(10, Math.max(0, roiPct / 10));

        // Score Stabilité Climatique (0..10)
        double scoreClimat = dossier.getFacteurClimatique() * 10;

        // Score Diversification & Historique via le service de statistiques
        double scoreDiversification = 5.0; // défaut
        double scoreHistorique = 5.0;      // défaut

        try {
            FarmerStats stats = statsService.getFarmerStats(agriculteurId, 5.0, scoreRentabilite);
            scoreDiversification = stats.getDiversification() * 10; // 0..1 → 0..10
            scoreHistorique = stats.getScoreGlobal(); // déjà 0..10
        } catch (Exception e) {
            LOGGER.warning("Statistiques fallback pour le score de risque: " + e.getMessage());
        }

        double scoreTotal = 0.4 * scoreRentabilite
                          + 0.3 * scoreClimat
                          + 0.2 * scoreDiversification
                          + 0.1 * scoreHistorique;

        String niveau;
        if (scoreTotal >= 7) {
            niveau = "Faible";
        } else if (scoreTotal >= 4) {
            niveau = "Modéré";
        } else {
            niveau = "Élevé";
        }

        dossier.setScoreRentabilite(scoreRentabilite);
        dossier.setScoreStabiliteClimat(scoreClimat);
        dossier.setScoreDiversification(scoreDiversification);
        dossier.setScoreHistorique(scoreHistorique);
        dossier.setScoreRisque(scoreTotal);
        dossier.setNiveauRisque(niveau);
    }
}
