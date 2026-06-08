package tn.neuron.ardhi.services.Parcelle_Cultures;

import tn.neuron.ardhi.models.Parcelle_Cultures.Culture;
import tn.neuron.ardhi.models.Parcelle_Cultures.Parcelle;
import tn.neuron.ardhi.models.Parcelle_Cultures.RecommendationResult;
import tn.neuron.ardhi.models.Parcelle_Cultures.RecommendationResult.DonneesMeteo;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Orchestrateur principal du système de recommandation agricole.
 *
 * Architecture:
 *   Controller → ParcelleRecommendationService
 *                    → LocationService (géocodage si GPS manquant)
 *                    → WeatherService (météo temps réel)
 *                    → AIRecommendationService (Gemini)
 *                         ↘ Fallback: SplitOptimizer (algorithmique)
 *
 * Gère également les règles métier:
 *   - Alerte gel (T < 3°C)
 *   - Alerte pluie excessive (> 30mm)
 *   - Détection récolte proche (< 7 jours)
 *   - Contrainte surface: somme cultures ≤ surface parcelle
 *   - Calcul score qualité parcelle
 */
public class ParcelleRecommendationService {

    private static final Logger LOGGER = Logger.getLogger(ParcelleRecommendationService.class.getName());

    private final WeatherService weatherService;
    private final LocationService locationService;
    private final AIRecommendationService aiService;
    private final SplitOptimizer splitOptimizer;
    private final CultureService cultureService;
    private final ParcelleService parcelleService;

    public ParcelleRecommendationService() {
        this.weatherService = new WeatherService();
        this.locationService = new LocationService();
        this.aiService = new AIRecommendationService();
        this.splitOptimizer = new SplitOptimizer();
        this.cultureService = new CultureService();
        this.parcelleService = new ParcelleService();
    }

    // ==================== MÉTHODE PRINCIPALE ====================

    /**
     * Génère une recommandation complète pour diviser une parcelle en 2 cultures.
     *
     * @param parcelle La parcelle à analyser
     * @return RecommendationResult avec les 2 cultures recommandées, alertes, score qualité
     */
    public RecommendationResult genererRecommandation(Parcelle parcelle) {
        return genererRecommandation(parcelle, true);
    }

    /**
     * Génère une recommandation pour une parcelle, avec option de division.
     *
     * @param parcelle La parcelle à analyser
     * @param diviser  true si on veut 2 cultures, false pour 1 seule
     * @return RecommendationResult
     */
    public RecommendationResult genererRecommandation(Parcelle parcelle, boolean diviser) {
        LOGGER.info("▶ Démarrage recommandation pour parcelle #" + parcelle.getId() + " (Division: " + diviser + ")");

        try {
            // === Étape 1: Résoudre les coordonnées GPS ===
            parcelle = resoudreCoordonnees(parcelle);

            // === Étape 2: Récupérer la météo ===
            double lat = parcelle.hasGpsCoordinates() ? parcelle.getLatitude() : 36.8065;
            double lon = parcelle.hasGpsCoordinates() ? parcelle.getLongitude() : 10.1815;
            DonneesMeteo meteo = weatherService.getMeteo(lat, lon);
            LOGGER.info("Météo: " + meteo.getDescription());

            // === Étape 3: Récupérer l'historique des cultures ===
            List<Culture> historiqueCultures = getHistoriqueCultures(parcelle.getId());
            List<String> historiqueNoms = historiqueCultures.stream()
                    .map(Culture::getNomCulture)
                    .collect(Collectors.toList());

            // === Étape 4: Déterminer la saison ===
            String saisonActuelle = getSaisonActuelle();
            LOGGER.info("Saison actuelle: " + saisonActuelle);

            // === Étape 5: Appel IA (Gemini) ===
            RecommendationResult result = null;
            if (aiService.isDisponible()) {
                LOGGER.info("Appel API Gemini...");
                result = aiService.recommander(parcelle, meteo, historiqueNoms, saisonActuelle, diviser);
                if (result != null) {
                    LOGGER.info("✅ Recommandation IA obtenue via Gemini");
                }
            }

            // === Étape 6: Fallback algorithmique ===
            if (result == null) {
                LOGGER.info("⚡ Fallback: utilisation de l'algorithme SplitOptimizer");
                result = splitOptimizer.optimiser(parcelle, meteo, historiqueNoms, saisonActuelle, diviser);
            }

            if (result == null) {
                return RecommendationResult.erreur(
                        "Impossible de générer une recommandation. Veuillez réessayer.");
            }

            // === Étape 7: Appliquer les règles métier et alertes ===
            enrichirResultat(result, parcelle, meteo, historiqueCultures, saisonActuelle);

            // === Étape 8: Attacher la météo au résultat ===
            result.setMeteo(meteo);

            LOGGER.info("✅ Recommandation finalisée: " + result.getSource());
            return result;

        } catch (Exception e) {
            LOGGER.severe("Erreur dans genererRecommandation: " + e.getMessage());
            return RecommendationResult.erreur("Erreur lors de la génération: " + e.getMessage());
        }
    }

    // ==================== ÉTAPES DU PIPELINE ====================

    /**
     * Complète les coordonnées GPS manquantes via géocodage Nominatim.
     */
    private Parcelle resoudreCoordonnees(Parcelle parcelle) {
        if (!parcelle.hasGpsCoordinates() && parcelle.getLocalisation() != null
                && !parcelle.getLocalisation().isBlank()) {
            LOGGER.info("GPS manquant — géocodage de: " + parcelle.getLocalisation());
            LocationService.GeocodageResult geo = locationService.geocoder(parcelle.getLocalisation());
            if (geo.isFound()) {
                parcelle.setLatitude(geo.getLatitude());
                parcelle.setLongitude(geo.getLongitude());

                // Sauvegarder en base
                try {
                    parcelleService.mettreAJourCoordonnees(
                            parcelle.getId(), geo.getLatitude(), geo.getLongitude());
                    LOGGER.info("Coordonnées sauvegardées: " + geo.getLatitude() + "," + geo.getLongitude());
                } catch (Exception e) {
                    LOGGER.warning("Impossible de sauvegarder les coordonnées: " + e.getMessage());
                }
            }
        }
        return parcelle;
    }

    /**
     * Enrichit le résultat avec alertes, score qualité, détection récolte.
     */
    private void enrichirResultat(RecommendationResult result, Parcelle parcelle,
                                   DonneesMeteo meteo, List<Culture> historique,
                                   String saison) {
        List<String> alertes = new ArrayList<>();

        // ---- Alertes météo ----
        if (meteo.isRisqueGel()) {
            alertes.add("⚠️ ALERTE GEL: Température " + String.format("%.1f", meteo.getTemperatureMoyenne())
                    + "°C < 3°C — Protégez vos cultures !");
        }
        if (meteo.isPluieExcessive()) {
            alertes.add("⚠️ ALERTE PLUIE: " + String.format("%.1f", meteo.getPrecipitations())
                    + " mm/jour > 30mm — Risque d'engorgement des sols !");
        }

        // ---- Détection récolte proche ----
        for (Culture c : historique) {
            if (c.isRecolteProche()) {
                long jours = c.getDureeCultureJours();
                alertes.add("🌾 RÉCOLTE IMMINENTE: " + c.getNomCulture()
                        + " dans moins de 7 jours !");
            }
        }

        // ---- Contrainte surface ----
        double surfaceAllouee = 0;
        try {
            surfaceAllouee = cultureService.getSurfaceUtiliseeParParcelle(parcelle.getId());
        } catch (Exception e) {
            LOGGER.warning("Impossible de calculer la surface utilisée: " + e.getMessage());
        }
        double surfaceDisponible = parcelle.getSurface() - surfaceAllouee;
        if (surfaceDisponible < parcelle.getSurface() * 0.1) {
            alertes.add("⚠️ Surface disponible limitée: " + String.format("%.2f", surfaceDisponible) + " ha");
        }

        // ---- Score qualité parcelle ----
        int nbCultures = historique.size();
        double scoreQualite = splitOptimizer.calculerScoreQualite(parcelle, meteo, nbCultures);
        result.setScoreQualiteParcelle(scoreQualite);

        result.setAlertes(alertes);
    }

    /**
     * Récupère l'historique des cultures pour une parcelle.
     */
    private List<Culture> getHistoriqueCultures(int parcelleId) {
        try {
            return cultureService.recupererParParcelle(parcelleId);
        } catch (SQLException e) {
            LOGGER.warning("Impossible de récupérer l'historique: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ==================== RÈGLES MÉTIER ====================

    /**
     * Détermine la saison actuelle.
     */
    public String getSaisonActuelle() {
        Month month = LocalDate.now().getMonth();
        return switch (month) {
            case DECEMBER, JANUARY, FEBRUARY -> "Hiver";
            case MARCH, APRIL, MAY -> "Printemps";
            case JUNE, JULY, AUGUST -> "Été";
            case SEPTEMBER, OCTOBER, NOVEMBER -> "Automne";
        };
    }

    /**
     * Vérifie la contrainte: somme surfaces cultures ≤ surface parcelle.
     *
     * @param parcelleId    ID de la parcelle
     * @param nouvelleSurface Surface de la nouvelle culture à ajouter
     * @return true si la contrainte est respectée
     */
    public boolean verifierContrainteSurface(int parcelleId, double nouvelleSurface)
            throws SQLException {
        Parcelle parcelle = parcelleService.recupererParId(parcelleId);
        if (parcelle == null) return false;

        double surfaceActuelle = cultureService.getSurfaceUtiliseeParParcelle(parcelleId);
        double surfaceRestante = parcelle.getSurface() - surfaceActuelle;

        boolean ok = nouvelleSurface <= surfaceRestante + 0.001; // tolérance float
        if (!ok) {
            LOGGER.warning(String.format(
                    "Contrainte surface violée: parcelle=%.1fha, utilisée=%.1fha, nouvelle=%.1fha",
                    parcelle.getSurface(), surfaceActuelle, nouvelleSurface));
        }
        return ok;
    }

    /**
     * Calcule la surface disponible restante pour une parcelle.
     */
    public double getSurfaceDisponible(int parcelleId) throws SQLException {
        Parcelle parcelle = parcelleService.recupererParId(parcelleId);
        if (parcelle == null) return 0;
        double utilise = cultureService.getSurfaceUtiliseeParParcelle(parcelleId);
        return Math.max(0, parcelle.getSurface() - utilise);
    }
}
