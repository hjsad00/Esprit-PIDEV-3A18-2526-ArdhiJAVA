package tn.neuron.ardhi.services.Evenement;

import tn.neuron.ardhi.models.Evenement.Evenement;
import tn.neuron.ardhi.models.Evenement.Participation;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service de prédiction basé sur Machine Learning simple
 * pour estimer le nombre de participants à un événement
 */
public class ParticipationPredictionService {

    private final ParticipationService participationService;
    private final EvenementService evenementService;

    // Poids des facteurs (ajustables selon l'historique)
    private static final double POIDS_TYPE = 0.30;
    private static final double POIDS_SAISON = 0.25;
    private static final double POIDS_LIEU = 0.20;
    private static final double POIDS_HISTORIQUE = 0.25;

    public ParticipationPredictionService() {
        this.participationService = new ParticipationService();
        this.evenementService = new EvenementService();
    }

    /**
     * Prédit le nombre de participants pour un événement
     */
    public PredictionResult predireParticipation(Evenement evenement) {
        PredictionResult result = new PredictionResult();

        // 1. Analyser l'historique
        Map<String, Double> historiqueStats = analyserHistorique(evenement.getType());

        // 2. Calculer le score de base selon le type
        double scoreType = getScoreType(evenement.getType());

        // 3. Calculer le facteur saisonnier
        double scoreSaison = getScoreSaison(evenement.getDateDebut());

        // 4. Calculer le facteur géographique
        double scoreLieu = getScoreLieu(evenement.getLieu());

        // 5. Score historique
        double scoreHistorique = historiqueStats.getOrDefault("moyenne", 50.0);

        // Calcul de la prédiction pondérée
        double prediction = (scoreType * POIDS_TYPE) +
                (scoreSaison * POIDS_SAISON) +
                (scoreLieu * POIDS_LIEU) +
                (scoreHistorique * POIDS_HISTORIQUE);

        // Ajustements basés sur les tendances
        prediction = ajusterSelonContenu(prediction, evenement);

        // Arrondir et limiter
        int participantsPredits = (int) Math.round(prediction);
        participantsPredits = Math.max(10, Math.min(participantsPredits, evenement.getNombrePlacesMax()));

        // Calculer les recommandations
        result.setParticipantsPredits(participantsPredits);
        result.setConfiance(calculerConfiance(historiqueStats));
        result.setRecommandations(genererRecommandations(participantsPredits, evenement));
        result.setFacteurs(construireFacteurs(scoreType, scoreSaison, scoreLieu, scoreHistorique));

        return result;
    }

    /**
     * Analyse l'historique des événements similaires
     */
    private Map<String, Double> analyserHistorique(String type) {
        Map<String, Double> stats = new HashMap<>();

        List<Evenement> evenementsPasses = evenementService.getEvenementsByStatut("TERMINE")
                .stream()
                .filter(e -> e.getType().equals(type))
                .toList();

        if (evenementsPasses.isEmpty()) {
            stats.put("moyenne", 50.0);
            stats.put("max", 100.0);
            stats.put("min", 20.0);
            return stats;
        }

        double somme = 0;
        int max = 0;
        int min = Integer.MAX_VALUE;

        for (Evenement evt : evenementsPasses) {
            int nbParticipants = participationService.getNombreParticipants(evt.getId(), "PRESENT");
            somme += nbParticipants;
            max = Math.max(max, nbParticipants);
            min = Math.min(min, nbParticipants);
        }

        stats.put("moyenne", somme / evenementsPasses.size());
        stats.put("max", (double) max);
        stats.put("min", (double) min);

        return stats;
    }

    /**
     * Score selon le type d'événement
     */
    private double getScoreType(String type) {
        return switch (type) {
            case "FOIRE" -> 80.0;      // Les foires attirent beaucoup
            case "FORMATION" -> 60.0;  // Formations moyennement populaires
            case "CONFERENCE" -> 50.0; // Conférences plus sélectives
            case "ATELIER" -> 40.0;    // Ateliers en petits groupes
            default -> 50.0;
        };
    }

    /**
     * Score saisonnier (agriculture en Tunisie)
     */
    private double getScoreSaison(LocalDate date) {
        if (date == null) return 50.0;

        Month mois = date.getMonth();

        // Haute saison agricole en Tunisie
        return switch (mois) {
            case MARCH, APRIL, MAY -> 90.0;        // Printemps - haute saison
            case SEPTEMBER, OCTOBER, NOVEMBER -> 85.0; // Automne - récoltes
            case JUNE, JULY -> 60.0;               // Été - moins d'activité
            case DECEMBER, JANUARY, FEBRUARY -> 55.0; // Hiver - basse saison
            default -> 70.0;
        };
    }

    /**
     * Score géographique (zones agricoles importantes)
     */
    private double getScoreLieu(String lieu) {
        if (lieu == null) return 50.0;

        String lieuLower = lieu.toLowerCase();

        // Grandes zones agricoles de Tunisie
        if (lieuLower.contains("bizerte") || lieuLower.contains("béja")) return 85.0;
        if (lieuLower.contains("tunis") || lieuLower.contains("ariana")) return 80.0;
        if (lieuLower.contains("sousse") || lieuLower.contains("monastir")) return 75.0;
        if (lieuLower.contains("sfax") || lieuLower.contains("kairouan")) return 75.0;
        if (lieuLower.contains("nabeul") || lieuLower.contains("zaghouan")) return 70.0;

        return 60.0; // Autres régions
    }

    /**
     * Ajustements basés sur les tendances actuelles
     */
    private double ajusterAvecTendances(double prediction, Evenement evenement) {
        // Bonus si événement récent (dans les 2 prochaines semaines)
        if (evenement.getDateDebut() != null) {
            long joursAvant = java.time.temporal.ChronoUnit.DAYS.between(
                    LocalDate.now(), evenement.getDateDebut());

            if (joursAvant < 14) {
                prediction *= 0.9; // Moins de temps = moins de participants potentiels
            }
        }

        // Pénalité si trop de places (événement paraît vide)
        if (evenement.getNombrePlacesMax() > 200) {
            prediction *= 0.85;
        }

        return prediction;
    }

    /**
     * Calcule le niveau de confiance de la prédiction
     */
    private double calculerConfiance(Map<String, Double> historiqueStats) {
        double moyenne = historiqueStats.get("moyenne");
        double max = historiqueStats.get("max");
        double min = historiqueStats.get("min");

        if (max - min < 20) {
            return 0.9; // Très stable = haute confiance
        } else if (max - min < 50) {
            return 0.75; // Modérément stable
        } else {
            return 0.6; // Volatil = faible confiance
        }
    }

    /**
     * Génère des recommandations pratiques
     */
    private Map<String, String> genererRecommandations(int participants, Evenement evenement) {
        Map<String, String> recommandations = new HashMap<>();

        // Matériel
        recommandations.put("materiel", String.format(
                "Prévoir %d chaises, %d tables, %d kits de documentation",
                participants + 5, (participants / 8) + 1, participants + 10
        ));

        // Restauration
        recommandations.put("restauration", String.format(
                "Commander %d repas/collations (avec marge de sécurité)",
                (int) (participants * 1.1)
        ));

        // Espace
        int surfaceNecessaire = participants * 2; // 2m² par personne
        recommandations.put("espace", String.format(
                "Prévoir un espace d'au moins %dm² (salle ou terrain)",
                surfaceNecessaire
        ));

        // Semences/matériel agricole (pour ateliers)
        if ("ATELIER".equals(evenement.getType())) {
            recommandations.put("semences", String.format(
                    "Prévoir des semences/plants pour %d participants + 15%% de marge",
                    participants
            ));
        }

        // Staff
        int staffNecessaire = Math.max(2, participants / 20);
        recommandations.put("staff", String.format(
                "Mobiliser %d personnes pour l'encadrement",
                staffNecessaire
        ));

        return recommandations;
    }

    /**
     * Construit le détail des facteurs de prédiction
     */
    private Map<String, Double> construireFacteurs(double type, double saison, double lieu, double historique) {
        Map<String, Double> facteurs = new HashMap<>();
        facteurs.put("type", type);
        facteurs.put("saison", saison);
        facteurs.put("lieu", lieu);
        facteurs.put("historique", historique);
        return facteurs;
    }

    /**
     * Classe résultat de prédiction
     */
    public static class PredictionResult {
        private int participantsPredits;
        private double confiance;
        private Map<String, String> recommandations;
        private Map<String, Double> facteurs;

        public int getParticipantsPredits() { return participantsPredits; }
        public void setParticipantsPredits(int participantsPredits) {
            this.participantsPredits = participantsPredits;
        }

        public double getConfiance() { return confiance; }
        public void setConfiance(double confiance) { this.confiance = confiance; }

        public Map<String, String> getRecommandations() { return recommandations; }
        public void setRecommandations(Map<String, String> recommandations) {
            this.recommandations = recommandations;
        }

        public Map<String, Double> getFacteurs() { return facteurs; }
        public void setFacteurs(Map<String, Double> facteurs) { this.facteurs = facteurs; }

        public String getConfianceTexte() {
            if (confiance >= 0.8) return "Très élevée";
            if (confiance >= 0.7) return "Élevée";
            if (confiance >= 0.6) return "Moyenne";
            return "Faible";
        }
    }
    /**
     * Ajustement intelligent basé sur le contenu du titre et de la description
     */
    private double ajusterSelonContenu(double prediction, Evenement evenement) {
        String titre = evenement.getTitre().toLowerCase();
        String description = evenement.getDescription() != null ?
                evenement.getDescription().toLowerCase() : "";

        // Pénalité pour contenu nonsensique ou test
        if (titre.length() < 10 || titre.matches(".*[a-z]{20,}.*")) {
            prediction *= 0.3; // Réduire drastiquement si titre bizarre
        }

        // Pénalité si description vide ou trop courte
        if (description.length() < 50) {
            prediction *= 0.5;
        }

        // Bonus si mots-clés professionnels
        String[] motsCleProfessionnels = {
                "formation", "expert", "certifié", "officiel",
                "ministère", "national", "agricole", "technique"
        };

        for (String mot : motsCleProfessionnels) {
            if (titre.contains(mot) || description.contains(mot)) {
                prediction *= 1.2; // Bonus 20%
                break;
            }
        }

        return prediction;
    }
}