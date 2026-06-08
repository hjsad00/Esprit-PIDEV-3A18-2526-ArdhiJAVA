package tn.neuron.ardhi.services.Parcelle_Cultures;

import tn.neuron.ardhi.models.Parcelle_Cultures.Culture;
import tn.neuron.ardhi.models.Parcelle_Cultures.Parcelle;
import tn.neuron.ardhi.models.Parcelle_Cultures.RoiResult;
import tn.neuron.ardhi.models.Parcelle_Cultures.RecommendationResult.DonneesMeteo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Service Métier Premium — Calculateur Intelligent de Rendement et Revenus (ROI Agricole).
 *
 * Architecture : FinancialService → Open-Meteo (historique 7j) → Calculs économiques
 *
 * Formules implémentées :
 *   FacteurClimatique = max(0.5 ; 1 − 0.01×JoursCanicule − 0.005×JoursPluie − 0.02×JoursGel)
 *   ProductionRéelle  = Surface × RendementThéorique × FacteurClimatique
 *   MargeBrute        = (Prod × PrixVente) − CoûtTotal
 *   PrixSeuil         = CoûtTotal / ProductionRéelle
 *   ScoreROI          = (MargeBrute / CoûtTotal) × 100  [%]
 */
public class FinancialService {

    private static final Logger LOGGER = Logger.getLogger(FinancialService.class.getName());

    // Seuils météo pour le facteur climatique
    private static final double SEUIL_CANICULE   = 35.0;  // °C
    private static final double SEUIL_PLUIE      = 30.0;  // mm/jour
    private static final double SEUIL_GEL        = 0.0;   // °C

    // Pénalités par jour (exprimées en fraction du rendement)
    private static final double PENALITE_CANICULE  = 0.01;
    private static final double PENALITE_PLUIE     = 0.005;
    private static final double PENALITE_GEL       = 0.02;
    private static final double FACTEUR_MINIMUM    = 0.50;

    // ==================== DONNÉES SAISONNIÈRES FALLBACK ====================
    // Format : {joursCanicule, joursExcèsPluie, joursGel} pour saison de 7 jours
    private static final Map<String, int[]> ALEA_SAISONNIERS = new HashMap<>();
    static {
        ALEA_SAISONNIERS.put("Printemps", new int[]{0, 1, 0});
        ALEA_SAISONNIERS.put("Été",       new int[]{3, 0, 0});
        ALEA_SAISONNIERS.put("Automne",   new int[]{0, 1, 0});
        ALEA_SAISONNIERS.put("Hiver",     new int[]{0, 2, 1});
    }

    // URL Open-Meteo pour le forecast 7 jours (températures max/min + précipitations daily)
    private static final String OPEN_METEO_URL =
            "https://api.open-meteo.com/v1/forecast?"
          + "latitude=%s&longitude=%s"
          + "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum"
          + "&forecast_days=7&timezone=Africa%%2FTunis";

    // ==================== MÉTHODE PRINCIPALE ====================

    /**
     * Calcule le ROI complet pour une Culture sur une Parcelle avec les coûts saisis.
     *
     * @param culture        Culture sélectionnée
     * @param parcelle       Parcelle (GPS pour météo)
     * @param prixVente      Prix de vente estimé (DT/tonne)
     * @param coutSemences   Coût semences (DT)
     * @param coutEngrais    Coût engrais (DT)
     * @param coutMainOeuvre Coût main d'œuvre (DT)
     * @param coutIrrigation Coût irrigation (DT)
     * @param coutAutres     Autres charges (DT)
     */
    public RoiResult calculerRoi(Culture culture, Parcelle parcelle,
                                  double prixVente,
                                  double coutSemences, double coutEngrais,
                                  double coutMainOeuvre, double coutIrrigation,
                                  double coutAutres) {

        LOGGER.info("▶ Calcul ROI : " + culture.getNomCulture() + " | Prix=" + prixVente + " DT/t");

        RoiResult result = new RoiResult();

        // 1. Surface et rendement de base
        double surface = culture.getSurfaceUtilisee() > 0
                ? culture.getSurfaceUtilisee() : parcelle.getSurface();
        double rendement = culture.getRendementEstime() > 0
                ? culture.getRendementEstime() : 3.0; // Valeur par défaut 3 t/ha
        result.setSurfaceHectares(surface);
        result.setRendementTheorique(rendement);
        result.setProductionTheorique(surface * rendement);

        // 2. Données climatiques (aléas)
        boolean meteoOk = false;
        if (parcelle.hasGpsCoordinates()) {
            meteoOk = remplirAleaClimatiques(result, parcelle.getLatitude(), parcelle.getLongitude());
        }
        if (!meteoOk) {
            remplirAleaFallback(result);
        }

        // 3. Facteur climatique
        double facteur = 1.0
                - (PENALITE_CANICULE  * result.getJoursCanicule())
                - (PENALITE_PLUIE     * result.getJoursExcesPluie())
                - (PENALITE_GEL       * result.getJoursGel());
        facteur = Math.max(FACTEUR_MINIMUM, facteur);
        result.setFacteurClimatique(facteur);

        // 4. Production réelle
        double productionReelle = surface * rendement * facteur;
        result.setProductionReelle(productionReelle);
        result.setPerteProdClimat(result.getProductionTheorique() - productionReelle);

        // 5. Coûts
        double coutTotal = coutSemences + coutEngrais + coutMainOeuvre + coutIrrigation + coutAutres;
        result.setCoutSemences(coutSemences);
        result.setCoutEngrais(coutEngrais);
        result.setCoutMainOeuvre(coutMainOeuvre);
        result.setCoutIrrigation(coutIrrigation);
        result.setCoutAutres(coutAutres);
        result.setCoutTotal(coutTotal);
        result.setPrixVente(prixVente);

        // 6. Revenus & marges
        double revenuBrut = productionReelle * prixVente;
        double margeBrute = revenuBrut - coutTotal;
        result.setRevenuBrut(revenuBrut);
        result.setMargeBrute(margeBrute);

        // 7. Prix seuil de rentabilité
        if (productionReelle > 0) {
            result.setPrixSeuil(coutTotal / productionReelle);
        }

        // 8. Score ROI
        if (coutTotal > 0) {
            result.setScoreRoi((margeBrute / coutTotal) * 100.0);
        }

        // 9. Statut rentabilité
        calculerStatut(result);

        // 10. Alertes et conseils
        genererAlertesEtConseils(result, parcelle);

        LOGGER.info(String.format("✅ ROI calculé : Prod=%.1f t | Marge=%.0f DT | ROI=%.1f%%",
                productionReelle, margeBrute, result.getScoreRoi()));

        return result;
    }

    // ==================== SIMULATION DYNAMIQUE ====================

    /**
     * Recalcule la marge brute et le ROI uniquement à partir d'un résultat existant
     * avec un nouveau prix de vente (pour la simulation dynamique en temps réel).
     */
    public void simulerAvecNouveauPrix(RoiResult result, double nouveauPrixVente) {
        result.setPrixVente(nouveauPrixVente);
        double revenu = result.getProductionReelle() * nouveauPrixVente;
        double marge  = revenu - result.getCoutTotal();
        result.setRevenuBrut(revenu);
        result.setMargeBrute(marge);
        if (result.getCoutTotal() > 0) {
            result.setScoreRoi((marge / result.getCoutTotal()) * 100.0);
        }
        calculerStatut(result);
        genererAlertesEtConseils(result, null);
    }

    /**
     * Recalcule complètement avec de nouveaux coûts (pour la simulation).
     */
    public void simulerAvecNouveauxCouts(RoiResult result,
                                          double semences, double engrais,
                                          double mainOeuvre, double irrigation,
                                          double autres, double prixVente) {
        double coutTotal = semences + engrais + mainOeuvre + irrigation + autres;
        result.setCoutSemences(semences);
        result.setCoutEngrais(engrais);
        result.setCoutMainOeuvre(mainOeuvre);
        result.setCoutIrrigation(irrigation);
        result.setCoutAutres(autres);
        result.setCoutTotal(coutTotal);
        result.setPrixVente(prixVente);

        double revenu = result.getProductionReelle() * prixVente;
        double marge  = revenu - coutTotal;
        result.setRevenuBrut(revenu);
        result.setMargeBrute(marge);
        if (coutTotal > 0) {
            result.setScoreRoi((marge / coutTotal) * 100.0);
        }
        if (result.getProductionReelle() > 0) {
            result.setPrixSeuil(coutTotal / result.getProductionReelle());
        }
        calculerStatut(result);
        genererAlertesEtConseils(result, null);
    }

    // ==================== MÉTÉO OPEN-METEO ====================

    private boolean remplirAleaClimatiques(RoiResult result, double lat, double lon) {
        try {
            String urlStr = String.format(OPEN_METEO_URL,
                    String.valueOf(lat).replace(",", "."),
                    String.valueOf(lon).replace(",", "."));
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(8000);

            if (conn.getResponseCode() != 200) {
                conn.disconnect();
                return false;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            conn.disconnect();
            String json = sb.toString();

            // Parser les 7 valeurs daily Tmax, Tmin, Précip
            double[] tmaxArr = parseDailyArray(json, "temperature_2m_max", 7);
            double[] tminArr = parseDailyArray(json, "temperature_2m_min", 7);
            double[] precipArr = parseDailyArray(json, "precipitation_sum", 7);

            int canicule = 0, pluie = 0, gel = 0;
            for (int i = 0; i < 7; i++) {
                if (tmaxArr[i] > SEUIL_CANICULE)  canicule++;
                if (precipArr[i] > SEUIL_PLUIE)   pluie++;
                if (tminArr[i] < SEUIL_GEL)        gel++;
            }

            result.setJoursCanicule(canicule);
            result.setJoursExcesPluie(pluie);
            result.setJoursGel(gel);
            result.setDonneeClimatFallback(false);

            LOGGER.info(String.format("Aléas météo 7j : Canicule=%d, Pluie=%d, Gel=%d",
                    canicule, pluie, gel));
            return true;

        } catch (Exception e) {
            LOGGER.warning("Erreur Open-Meteo ROI : " + e.getMessage());
            return false;
        }
    }

    private double[] parseDailyArray(String json, String key, int n) {
        double[] arr = new double[n];
        int idx = json.indexOf("\"" + key + "\":[");
        if (idx < 0) return arr;
        int pos = json.indexOf('[', idx) + 1;
        for (int i = 0; i < n; i++) {
            int comma   = json.indexOf(',', pos);
            int bracket = json.indexOf(']', pos);
            int end = (comma >= 0 && comma < bracket) ? comma : bracket;
            if (end < 0) break;
            try {
                String val = json.substring(pos, end).trim();
                if (!val.equals("null")) arr[i] = Double.parseDouble(val);
            } catch (Exception ignored) {}
            pos = end + 1;
            if (pos >= bracket) break;
        }
        return arr;
    }

    // ==================== FALLBACK SAISONNIER ====================

    private void remplirAleaFallback(RoiResult result) {
        String saison = getSaisonActuelle();
        int[] vals = ALEA_SAISONNIERS.getOrDefault(saison, new int[]{0, 1, 0});
        result.setJoursCanicule(vals[0]);
        result.setJoursExcesPluie(vals[1]);
        result.setJoursGel(vals[2]);
        result.setDonneeClimatFallback(true);
        LOGGER.info("Aléas fallback saisonniers (" + saison + ")");
    }

    private String getSaisonActuelle() {
        int m = LocalDate.now().getMonthValue();
        if (m >= 3 && m <= 5)  return "Printemps";
        if (m >= 6 && m <= 8)  return "Été";
        if (m >= 9 && m <= 11) return "Automne";
        return "Hiver";
    }

    // ==================== STATUT RENTABILITÉ ====================

    private void calculerStatut(RoiResult result) {
        double marge = result.getMargeBrute();
        double roi   = result.getScoreRoi();
        if (marge < 0) {
            result.setStatut("PERTE");
            result.setCouleurStatut("#D32F2F");
        } else if (roi < 5) {
            result.setStatut("ÉQUILIBRE");
            result.setCouleurStatut("#F57C00");
        } else {
            result.setStatut("PROFIT");
            result.setCouleurStatut("#388E3C");
        }
    }

    // ==================== ALERTES & CONSEILS ====================

    private void genererAlertesEtConseils(RoiResult result, Parcelle parcelle) {
        result.getAlertes().clear();

        if (result.getJoursCanicule() > 2) {
            result.addAlerte("🔥 " + result.getJoursCanicule()
                    + " jours de canicule prévus — rendement réduit de "
                    + String.format("%.0f", result.getJoursCanicule() * PENALITE_CANICULE * 100) + "%");
        }
        if (result.getJoursGel() > 0) {
            result.addAlerte("❄️ " + result.getJoursGel()
                    + " jour(s) de gel — risque sévère sur la culture");
        }
        if (result.getJoursExcesPluie() > 2) {
            result.addAlerte("🌧️ " + result.getJoursExcesPluie()
                    + " jours d'excès de pluie — risque de maladies fongiques");
        }
        if (result.getFacteurClimatique() <= 0.60) {
            result.addAlerte("⚠️ Facteur climatique très bas ("
                    + String.format("%.0f", result.getFacteurClimatique() * 100)
                    + "%) — production fortement dégradée");
        }

        // Conseil principal
        StringBuilder conseil = new StringBuilder();
        switch (result.getStatut()) {
            case "PERTE" -> {
                conseil.append("🔴 Perte projetée de ")
                       .append(String.format("%.0f DT", Math.abs(result.getMargeBrute())))
                       .append(". Envisagez de vendre à ≥ ")
                       .append(String.format("%.2f DT/t", result.getPrixSeuil()))
                       .append(" pour atteindre l'équilibre. ");
                if (result.getJoursGel() > 0)
                    conseil.append("Protégez vos cultures du gel en priorité.");
            }
            case "ÉQUILIBRE" -> {
                conseil.append("🟠 Rentabilité marginale (ROI ")
                       .append(result.getScoreRoiFormate())
                       .append("). Prix seuil à respecter : ")
                       .append(String.format("%.2f DT/t", result.getPrixSeuil()))
                       .append(". Réduisez les coûts variables pour améliorer la marge.");
            }
            case "PROFIT" -> {
                conseil.append("✅ Bonne rentabilité (ROI ")
                       .append(result.getScoreRoiFormate())
                       .append("). Gain net estimé : ")
                       .append(String.format("%.0f DT", result.getMargeBrute()))
                       .append(". ");
                if (parcelle != null && parcelle.getSystemeIrrigation() != null
                        && parcelle.getSystemeIrrigation().toLowerCase().contains("goutte")) {
                    conseil.append("Votre irrigation économique optimise encore davantage le ROI.");
                } else {
                    conseil.append("Investir dans le goutte-à-goutte peut accroître la marge de 15-20%.");
                }
            }
        }
        result.setConseil(conseil.toString());
    }
}
