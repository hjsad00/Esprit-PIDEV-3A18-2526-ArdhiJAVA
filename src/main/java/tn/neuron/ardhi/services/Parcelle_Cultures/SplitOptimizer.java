package tn.neuron.ardhi.services.Parcelle_Cultures;

import tn.neuron.ardhi.models.Parcelle_Cultures.Parcelle;
import tn.neuron.ardhi.models.Parcelle_Cultures.RecommendationResult;
import tn.neuron.ardhi.models.Parcelle_Cultures.RecommendationResult.CultureRecommandee;
import tn.neuron.ardhi.models.Parcelle_Cultures.RecommendationResult.DonneesMeteo;

import java.util.*;
import java.util.logging.Logger;

/**
 * Optimiseur algorithmique de répartition de cultures (fallback sans IA).
 *
 * Score = 0.4 * scoreSol + 0.3 * scoreMétéo + 0.2 * scoreRendement + 0.1 * scoreRotation
 *
 * Sélectionne les 2 meilleures cultures avec répartition proportionnelle aux scores.
 * Contrainte C1 ≠ C2 et pénalité de rotation si répétition de la culture dominante.
 */
public class SplitOptimizer {

    private static final Logger LOGGER = Logger.getLogger(SplitOptimizer.class.getName());

    // ==================== BASE DE DONNÉES DES CULTURES ====================

    /**
     * Fiche d'une culture candidate avec ses caractéristiques agronomiques.
     */
    public static class FicheCulture {
        final String nom;
        final String type;
        final double rendementTha;    // Rendement moyen t/ha
        final String[] solsCompatibles; // Types de sol compatibles
        final double tempMin;           // Température min °C
        final double tempMax;           // Température max °C
        final double precipMin;         // Précipitations min mm/j
        final double precipMax;         // Précipitations max mm/j
        final String[] saisonsOk;       // Saisons favorables

        FicheCulture(String nom, String type, double rendementTha,
                     String[] solsCompatibles, double tempMin, double tempMax,
                     double precipMin, double precipMax, String[] saisonsOk) {
            this.nom = nom;
            this.type = type;
            this.rendementTha = rendementTha;
            this.solsCompatibles = solsCompatibles;
            this.tempMin = tempMin;
            this.tempMax = tempMax;
            this.precipMin = precipMin;
            this.precipMax = precipMax;
            this.saisonsOk = saisonsOk;
        }
    }

    // Catalogue de cultures adaptées à la Tunisie et Afrique du Nord
    private static final List<FicheCulture> CATALOGUE = Arrays.asList(
            new FicheCulture("Blé dur", "Céréale", 3.5,
                    new String[]{"Argileux", "Limoneux", "Calcaire"},
                    5, 25, 0, 15,
                    new String[]{"Automne", "Hiver"}),

            new FicheCulture("Orge", "Céréale", 3.0,
                    new String[]{"Argileux", "Sableux", "Calcaire"},
                    3, 22, 0, 10,
                    new String[]{"Automne", "Hiver"}),

            new FicheCulture("Pois chiche", "Légumineuse", 1.8,
                    new String[]{"Argileux", "Limoneux", "Sableux"},
                    8, 28, 0, 8,
                    new String[]{"Printemps", "Automne"}),

            new FicheCulture("Lentilles", "Légumineuse", 1.5,
                    new String[]{"Argileux", "Limoneux"},
                    5, 24, 2, 12,
                    new String[]{"Hiver", "Printemps"}),

            new FicheCulture("Tomate", "Maraîcher", 40.0,
                    new String[]{"Limoneux", "Sableux", "Sablo-limoneux"},
                    15, 32, 2, 20,
                    new String[]{"Printemps", "Été", "Automne"}),

            new FicheCulture("Pomme de terre", "Maraîcher", 25.0,
                    new String[]{"Limoneux", "Sableux"},
                    10, 22, 3, 15,
                    new String[]{"Printemps", "Automne"}),

            new FicheCulture("Oignon", "Maraîcher", 30.0,
                    new String[]{"Limoneux", "Argileux"},
                    12, 28, 2, 12,
                    new String[]{"Hiver", "Printemps"}),

            new FicheCulture("Tournesol", "Oléagineux", 2.5,
                    new String[]{"Limoneux", "Argileux", "Sableux"},
                    15, 30, 2, 10,
                    new String[]{"Printemps", "Été"}),

            new FicheCulture("Colza", "Oléagineux", 2.0,
                    new String[]{"Argileux", "Limoneux"},
                    5, 18, 3, 15,
                    new String[]{"Automne", "Hiver"}),

            new FicheCulture("Sorgho", "Fourrage", 6.0,
                    new String[]{"Argileux", "Limoneux", "Sableux"},
                    18, 35, 1, 10,
                    new String[]{"Printemps", "Été"}),

            new FicheCulture("Luzerne", "Fourrage", 10.0,
                    new String[]{"Argileux", "Limoneux"},
                    10, 30, 3, 15,
                    new String[]{"Printemps", "Automne", "Hiver", "Été"}),

            new FicheCulture("Haricot vert", "Légumineuse", 8.0,
                    new String[]{"Limoneux", "Sableux"},
                    15, 28, 3, 15,
                    new String[]{"Printemps", "Été"}),

            new FicheCulture("Piment", "Maraîcher", 15.0,
                    new String[]{"Limoneux", "Argileux"},
                    18, 32, 2, 12,
                    new String[]{"Printemps", "Été"}),

            new FicheCulture("Féverole", "Légumineuse", 2.5,
                    new String[]{"Argileux", "Limoneux"},
                    5, 20, 3, 15,
                    new String[]{"Automne", "Hiver", "Printemps"}),

            new FicheCulture("Pastèque", "Maraîcher", 30.0,
                    new String[]{"Sableux", "Sablo-limoneux"},
                    20, 38, 0, 8,
                    new String[]{"Printemps", "Été"})
    );

    private final WeatherService weatherService;

    public SplitOptimizer() {
        this.weatherService = new WeatherService();
    }

    // ==================== MÉTHODE PRINCIPALE ====================

    /**
     * Calcule la répartition optimale de deux cultures sur une parcelle.
     *
     * @param parcelle       Parcelle à diviser
     * @param meteo          Données météo actuelles
     * @param historiqueNoms Liste des noms de cultures précédentes
     * @param saisonActuelle Saison courante
     * @return RecommendationResult avec les deux meilleures cultures
     */
    public RecommendationResult optimiser(Parcelle parcelle, DonneesMeteo meteo,
                                          List<String> historiqueNoms, String saisonActuelle, boolean diviser) {
        LOGGER.info("Optimisation algorithmique pour parcelle " + parcelle.getId()
                + " (" + parcelle.getSurface() + " ha)");

        double surfaceTotale = parcelle.getSurface();
        String typeSol = parcelle.getTypeSol() != null ? parcelle.getTypeSol() : "Limoneux";

        // Calculer le score pour chaque culture du catalogue
        List<CultureScore> scores = new ArrayList<>();
        for (FicheCulture fiche : CATALOGUE) {
            double score = calculerScore(fiche, typeSol, meteo, historiqueNoms, saisonActuelle);
            scores.add(new CultureScore(fiche, score));
            LOGGER.fine("Score " + fiche.nom + ": " + String.format("%.3f", score));
        }

        // Trier par score décroissant
        scores.sort((a, b) -> Double.compare(b.score, a.score));

        // Sélectionner les meilleures cultures
        CultureScore top1 = scores.get(0);
        CultureScore top2 = null;
        
        if (diviser) {
            for (int i = 1; i < scores.size(); i++) {
                if (!scores.get(i).fiche.nom.equalsIgnoreCase(top1.fiche.nom)) {
                    top2 = scores.get(i);
                    break;
                }
            }

            if (top2 == null) {
                LOGGER.warning("Impossible de trouver 2 cultures différentes");
                top2 = scores.get(1);
            }
        }

        CultureRecommandee c1;
        CultureRecommandee c2 = null;

        if (diviser && top2 != null) {
            // Répartition proportionnelle aux scores
            double scoreTotal = top1.score + top2.score;
            double surface1 = Math.round((top1.score / scoreTotal) * surfaceTotale * 10.0) / 10.0;
            double surface2 = Math.round((surfaceTotale - surface1) * 10.0) / 10.0;

            if (surface1 <= 0) surface1 = surfaceTotale * 0.6;
            if (surface2 <= 0) surface2 = surfaceTotale - surface1;

            c1 = new CultureRecommandee(
                    top1.fiche.nom, top1.fiche.type, saisonActuelle,
                    surface1, top1.score, top1.fiche.rendementTha);

            c2 = new CultureRecommandee(
                    top2.fiche.nom, top2.fiche.type, saisonActuelle,
                    surface2, top2.score, top2.fiche.rendementTha);

            LOGGER.info(String.format("Recommandation finale: %s (%.1f ha, score=%.2f) + %s (%.1f ha, score=%.2f)",
                    c1.getNom(), surface1, top1.score, c2.getNom(), surface2, top2.score));
        } else {
            c1 = new CultureRecommandee(
                    top1.fiche.nom, top1.fiche.type, saisonActuelle,
                    surfaceTotale, top1.score, top1.fiche.rendementTha);
            
            LOGGER.info(String.format("Recommandation finale (sans division): %s (%.1f ha, score=%.2f)",
                    c1.getNom(), surfaceTotale, top1.score));
        }

        String justification = generateJustification(c1, c2, top1, top2, meteo, typeSol, saisonActuelle);

        return RecommendationResult.success(c1, c2, meteo, justification, "ALGORITHME_FALLBACK");
    }

    // ==================== CALCUL DU SCORE ====================

    /**
     * Score = 0.4*scoreSol + 0.3*scoreMeteo + 0.2*scoreRendement + 0.1*scoreRotation
     */
    private double calculerScore(FicheCulture fiche, String typeSol, DonneesMeteo meteo,
                                  List<String> historique, String saison) {
        double scoreSol = calculerScoreSol(fiche, typeSol);
        double scoreMeteo = calculerScoreMeteo(fiche, meteo);
        double scoreRendement = calculerScoreRendement(fiche);
        double scoreRotation = calculerScoreRotation(fiche, historique, saison);

        double score = 0.4 * scoreSol + 0.3 * scoreMeteo + 0.2 * scoreRendement + 0.1 * scoreRotation;

        // Pénalité gel
        if (meteo.isRisqueGel() && fiche.tempMin > 5) score *= 0.4;
        // Pénalité pluie excessive
        if (meteo.isPluieExcessive() && fiche.precipMax < 15) score *= 0.6;

        return Math.max(0.01, Math.min(1.0, score));
    }

    private double calculerScoreSol(FicheCulture fiche, String typeSol) {
        if (typeSol == null || typeSol.isBlank()) return 0.5;
        String typeLower = typeSol.toLowerCase();
        for (String sol : fiche.solsCompatibles) {
            if (typeLower.contains(sol.toLowerCase()) || sol.toLowerCase().contains(typeLower)) {
                return 1.0;
            }
        }
        // Compatibilité partielle
        if (typeLower.contains("argil") && Arrays.asList(fiche.solsCompatibles).contains("Limoneux")) return 0.6;
        if (typeLower.contains("sabl") && Arrays.asList(fiche.solsCompatibles).contains("Limoneux")) return 0.5;
        return 0.2;
    }

    private double calculerScoreMeteo(FicheCulture fiche, DonneesMeteo meteo) {
        double temp = meteo.getTemperatureMoyenne();
        double precip = meteo.getPrecipitations();
        double scoreTemp;

        if (temp < fiche.tempMin - 5 || temp > fiche.tempMax + 10) {
            scoreTemp = 0.1;
        } else if (temp >= fiche.tempMin && temp <= fiche.tempMax) {
            scoreTemp = 1.0;
        } else if (temp < fiche.tempMin) {
            scoreTemp = 0.3 + (temp - (fiche.tempMin - 5)) / 5.0 * 0.7;
        } else {
            scoreTemp = 0.5 - (temp - fiche.tempMax) / 10.0 * 0.4;
        }

        double scorePrecip;
        if (precip < fiche.precipMin) scorePrecip = 0.6;
        else if (precip > fiche.precipMax) scorePrecip = 0.3;
        else scorePrecip = 1.0;

        return (scoreTemp * 0.7 + scorePrecip * 0.3);
    }

    private double calculerScoreRendement(FicheCulture fiche) {
        // Normaliser par rapport aux rendements max du catalogue
        double maxRendement = 40.0; // Tomate
        return Math.min(1.0, fiche.rendementTha / maxRendement);
    }

    private double calculerScoreRotation(FicheCulture fiche, List<String> historique, String saison) {
        if (historique.isEmpty()) return 1.0;

        // Vérification saison
        boolean saisonOk = false;
        for (String s : fiche.saisonsOk) {
            if (s.equalsIgnoreCase(saison)) { saisonOk = true; break; }
        }
        double scoreBase = saisonOk ? 1.0 : 0.5;

        // Pénalité si la même culture apparaît dans l'historique récent
        String derniere = historique.get(historique.size() - 1).toLowerCase();
        if (derniere.contains(fiche.nom.toLowerCase()) || fiche.nom.toLowerCase().contains(derniere)) {
            scoreBase *= 0.3; // Pénalité forte : répétition de la même culture
        }

        // Pénalité si même type de culture (ex: deux céréales consécutives)
        for (String h : historique) {
            if (h.toLowerCase().contains(fiche.type.toLowerCase())) {
                scoreBase *= 0.7; // Pénalité légère
                break;
            }
        }

        return Math.max(0.1, scoreBase);
    }

    // ==================== GÉNÉRER JUSTIFICATION ====================

    private String generateJustification(CultureRecommandee c1, CultureRecommandee c2,
                                          CultureScore top1, CultureScore top2,
                                          DonneesMeteo meteo, String typeSol, String saison) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analyse algorithmique (Ardhi Intelligence) :\n\n");

        sb.append(String.format("- %s (%.1f ha) - Score: %.2f\n", c1.getNom(), c1.getSurface(), top1.score));
        sb.append(String.format("  -> Rendement estime: %.1f t/ha | Production: %.1f t\n",
                c1.getRendementEstime(), c1.getProductionEstimee()));

        if (c2 != null && top2 != null) {
            sb.append(String.format("\n- %s (%.1f ha) - Score: %.2f\n", c2.getNom(), c2.getSurface(), top2.score));
            sb.append(String.format("  -> Rendement estime: %.1f t/ha | Production: %.1f t\n",
                    c2.getRendementEstime(), c2.getProductionEstimee()));
        }

        sb.append(String.format("\nSol: %s | Saison: %s | T: %.1f deg\n",
                typeSol, saison, meteo.getTemperatureMoyenne()));

        if (meteo.isRisqueGel()) sb.append("\nALERTE: Risque de gel detecte (T < 3C) !");
        if (meteo.isPluieExcessive()) sb.append("\nALERTE: Pluie excessive (> 30mm) detectee !");

        double prodTotale = c1.getProductionEstimee() + (c2 != null ? c2.getProductionEstimee() : 0);
        sb.append(String.format("\n\nProduction totale estimee: %.1f t", prodTotale));

        return sb.toString();
    }

    // ==================== SCORE QUALITÉ PARCELLE ====================

    /**
     * Calcule un score qualité global de la parcelle (0-10).
     * Basé sur sol, irrigation, météo et diversification.
     */
    public double calculerScoreQualite(Parcelle parcelle, DonneesMeteo meteo,
                                        int nombreCultures) {
        double score = 0;

        // Sol (3 points max)
        String sol = parcelle.getTypeSol() != null ? parcelle.getTypeSol().toLowerCase() : "";
        if (sol.contains("limoneux") || sol.contains("argilo-limoneux")) score += 3;
        else if (sol.contains("argileux")) score += 2.5;
        else if (sol.contains("sableux")) score += 1.5;
        else score += 1;

        // Irrigation (2 points max)
        String irr = parcelle.getSystemeIrrigation() != null ?
                parcelle.getSystemeIrrigation().toLowerCase() : "";
        if (irr.contains("goutte") || irr.contains("drip")) score += 2;
        else if (irr.contains("aspersion")) score += 1.5;
        else if (!irr.isBlank()) score += 1;

        // Météo (3 points max)
        double temp = meteo.getTemperatureMoyenne();
        if (temp >= 10 && temp <= 28 && !meteo.isRisqueGel() && !meteo.isPluieExcessive()) {
            score += 3;
        } else if (meteo.isRisqueGel() || meteo.isPluieExcessive()) {
            score += 1;
        } else {
            score += 2;
        }

        // Diversification (2 points max)
        if (nombreCultures >= 2) score += 2;
        else if (nombreCultures == 1) score += 1;

        return Math.min(10.0, score);
    }

    // ==================== CLASSE INTERNE ====================

    private static class CultureScore {
        final FicheCulture fiche;
        final double score;

        CultureScore(FicheCulture fiche, double score) {
            this.fiche = fiche;
            this.score = score;
        }
    }
}
