package tn.neuron.ardhi.services.Evenement;

import tn.neuron.ardhi.models.Evenement.Participation;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service de détection des participants inactifs avec scoring de risque
 * et recommandations de relances automatiques
 */
public class InactiveParticipantDetectionService {

    private final ParticipationService participationService;

    public InactiveParticipantDetectionService() {
        this.participationService = new ParticipationService();
    }

    /**
     * Analyse tous les participants et identifie les profils à risque
     */
    public List<ParticipantRiskProfile> detecterParticipantsInactifs() {
        List<ParticipantRiskProfile> profiles = new ArrayList<>();

        // Récupérer toutes les participations
        List<Participation> allParticipations = participationService.getAllParticipations();

        // Grouper par utilisateur
        Map<Integer, List<Participation>> participationsParUtilisateur = allParticipations.stream()
                .collect(Collectors.groupingBy(Participation::getIdUtilisateur));

        // Analyser chaque utilisateur
        for (Map.Entry<Integer, List<Participation>> entry : participationsParUtilisateur.entrySet()) {
            int userId = entry.getKey();
            List<Participation> userParticipations = entry.getValue();

            ParticipantRiskProfile profile = analyserUtilisateur(userId, userParticipations);

            // Ajouter TOUS les profils pour analyse complète
            profiles.add(profile);
        }

        // Trier par score de risque décroissant
        profiles.sort((p1, p2) -> Double.compare(p2.getRiskScore(), p1.getRiskScore()));

        return profiles;
    }

    /**
     * Analyse un utilisateur et calcule son score de risque
     */
    private ParticipantRiskProfile analyserUtilisateur(int userId, List<Participation> participations) {
        ParticipantRiskProfile profile = new ParticipantRiskProfile();
        profile.setUserId(userId);

        // Récupérer infos utilisateur
        if (!participations.isEmpty()) {
            Participation first = participations.get(0);
            profile.setNom(first.getNomUtilisateur());
            profile.setPrenom(first.getPrenomUtilisateur());
            profile.setEmail(first.getEmailUtilisateur());
        }

        double riskScore = 0.0;
        List<String> indicateurs = new ArrayList<>();

        // 1. Taux d'annulation
        long annulations = participations.stream()
                .filter(p -> "ANNULE".equals(p.getStatut()))
                .count();
        double tauxAnnulation = (double) annulations / participations.size() * 100;

        if (tauxAnnulation > 50) {
            riskScore += 30;
            indicateurs.add("Taux d'annulation très élevé: " + String.format("%.1f%%", tauxAnnulation));
        } else if (tauxAnnulation > 30) {
            riskScore += 20;
            indicateurs.add("Taux d'annulation élevé: " + String.format("%.1f%%", tauxAnnulation));
        }

        // 2. Absences (confirmé mais pas présent)
        long confirmes = participations.stream()
                .filter(p -> "CONFIRME".equals(p.getStatut()))
                .count();
        long presents = participations.stream()
                .filter(p -> "PRESENT".equals(p.getStatut()))
                .count();

        if (confirmes > 0) {
            double tauxAbsence = (double) (confirmes - presents) / confirmes * 100;
            if (tauxAbsence > 60) {
                riskScore += 25;
                indicateurs.add("Taux d'absence très élevé: " + String.format("%.1f%%", tauxAbsence));
            } else if (tauxAbsence > 40) {
                riskScore += 15;
                indicateurs.add("Taux d'absence élevé: " + String.format("%.1f%%", tauxAbsence));
            }
        }

        // 3. Inactivité récente
        LocalDateTime derniereInscription = participations.stream()
                .map(Participation::getDateInscription)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusYears(1));

        long joursSanActivite = ChronoUnit.DAYS.between(derniereInscription, LocalDateTime.now());

        if (joursSanActivite > 180) {
            riskScore += 20;
            indicateurs.add("Inactif depuis " + joursSanActivite + " jours");
        } else if (joursSanActivite > 90) {
            riskScore += 10;
            indicateurs.add("Peu actif (dernière activité il y a " + joursSanActivite + " jours)");
        }

        // 4. Participation en attente non confirmée
        long enAttente = participations.stream()
                .filter(p -> "EN_ATTENTE".equals(p.getStatut()))
                .filter(p -> ChronoUnit.DAYS.between(p.getDateInscription(), LocalDateTime.now()) > 7)
                .count();

        if (enAttente > 0) {
            riskScore += 15;
            indicateurs.add(enAttente + " inscription(s) en attente non confirmée(s)");
        }

        // 5. Historique court (nouvel utilisateur peu engagé)
        if (participations.size() == 1 && joursSanActivite > 30) {
            riskScore += 10;
            indicateurs.add("Nouvel utilisateur peu engagé");
        }

        profile.setRiskScore(Math.min(100, riskScore));
        profile.setTotalParticipations(participations.size());
        profile.setAnnulations((int) annulations);
        profile.setPresences((int) presents);
        profile.setJoursSansActivite((int) joursSanActivite);
        profile.setIndicateurs(indicateurs);
        profile.setRecommandation(genererRecommandation(profile));

        return profile;
    }

    /**
     * Génère une recommandation d'action selon le profil de risque
     */
    private String genererRecommandation(ParticipantRiskProfile profile) {
        double score = profile.getRiskScore();

        if (score >= 80) {
            return "🔴 URGENT: Relance immédiate avec offre spéciale personnalisée";
        } else if (score >= 60) {
            return "🟠 IMPORTANT: Relance avec questionnaire de satisfaction";
        } else if (score >= 40) {
            return "🟡 ATTENTION: Email de réengagement avec recommandations";
        } else {
            return "🟢 Monitoring: Surveiller l'évolution";
        }
    }

    /**
     * Envoie des relances automatiques aux participants à risque
     */
    public Map<String, Integer> envoyerRelancesAutomatiques(List<ParticipantRiskProfile> profiles) {
        Map<String, Integer> stats = new HashMap<>();
        int relancesUrgentes = 0;
        int relancesImportantes = 0;
        int relancesStandard = 0;

        // Utiliser le service intelligent de recommandations
        SmartEmailRecommendationService smartEmailService = new SmartEmailRecommendationService();

        for (ParticipantRiskProfile profile : profiles) {
            if (profile.getRiskScore() >= 80) {
                smartEmailService.envoyerRelancePersonnalisee(profile);
                relancesUrgentes++;
            } else if (profile.getRiskScore() >= 60) {
                smartEmailService.envoyerRelancePersonnalisee(profile);
                relancesImportantes++;
            } else if (profile.getRiskScore() >= 30) {
                smartEmailService.envoyerRelancePersonnalisee(profile);
                relancesStandard++;
            }
        }

        stats.put("urgentes", relancesUrgentes);
        stats.put("importantes", relancesImportantes);
        stats.put("standard", relancesStandard);
        stats.put("total", relancesUrgentes + relancesImportantes + relancesStandard);

        return stats;
    }

    /**
     * Génère des statistiques globales sur l'inactivité
     */
    public InactivityStats genererStatistiques(List<ParticipantRiskProfile> profiles) {
        InactivityStats stats = new InactivityStats();

        stats.setTotalAnalyses(profiles.size());
        stats.setRisqueUrgent((int) profiles.stream().filter(p -> p.getRiskScore() >= 80).count());
        stats.setRisqueImportant((int) profiles.stream().filter(p -> p.getRiskScore() >= 60 && p.getRiskScore() < 80).count());
        stats.setRisqueModere((int) profiles.stream().filter(p -> p.getRiskScore() >= 40 && p.getRiskScore() < 60).count());

        double scoreMoyen = profiles.stream()
                .mapToDouble(ParticipantRiskProfile::getRiskScore)
                .average()
                .orElse(0.0);
        stats.setScoreMoyen(scoreMoyen);

        return stats;
    }

    /**
     * Classe représentant un profil de risque
     */
    public static class ParticipantRiskProfile {
        private int userId;
        private String nom;
        private String prenom;
        private String email;
        private double riskScore;
        private int totalParticipations;
        private int annulations;
        private int presences;
        private int joursSansActivite;
        private List<String> indicateurs;
        private String recommandation;

        // Getters et Setters
        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }

        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }

        public String getPrenom() { return prenom; }
        public void setPrenom(String prenom) { this.prenom = prenom; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public double getRiskScore() { return riskScore; }
        public void setRiskScore(double riskScore) { this.riskScore = riskScore; }

        public String getRiskLevel() {
            if (riskScore >= 80) return "URGENT";
            if (riskScore >= 60) return "IMPORTANT";
            if (riskScore >= 40) return "MODÉRÉ";
            return "FAIBLE";
        }

        public String getRiskColor() {
            if (riskScore >= 80) return "#E74C3C";
            if (riskScore >= 60) return "#F39C12";
            if (riskScore >= 40) return "#F1C40F";
            return "#27AE60";
        }

        public int getTotalParticipations() { return totalParticipations; }
        public void setTotalParticipations(int totalParticipations) {
            this.totalParticipations = totalParticipations;
        }

        public int getAnnulations() { return annulations; }
        public void setAnnulations(int annulations) { this.annulations = annulations; }

        public int getPresences() { return presences; }
        public void setPresences(int presences) { this.presences = presences; }

        public int getJoursSansActivite() { return joursSansActivite; }
        public void setJoursSansActivite(int joursSansActivite) {
            this.joursSansActivite = joursSansActivite;
        }

        public List<String> getIndicateurs() { return indicateurs; }
        public void setIndicateurs(List<String> indicateurs) { this.indicateurs = indicateurs; }

        public String getRecommandation() { return recommandation; }
        public void setRecommandation(String recommandation) { this.recommandation = recommandation; }

        public String getNomComplet() {
            return prenom + " " + nom;
        }
    }

    /**
     * Classe pour les statistiques globales
     */
    public static class InactivityStats {
        private int totalAnalyses;
        private int risqueUrgent;
        private int risqueImportant;
        private int risqueModere;
        private double scoreMoyen;

        public int getTotalAnalyses() { return totalAnalyses; }
        public void setTotalAnalyses(int totalAnalyses) { this.totalAnalyses = totalAnalyses; }

        public int getRisqueUrgent() { return risqueUrgent; }
        public void setRisqueUrgent(int risqueUrgent) { this.risqueUrgent = risqueUrgent; }

        public int getRisqueImportant() { return risqueImportant; }
        public void setRisqueImportant(int risqueImportant) { this.risqueImportant = risqueImportant; }

        public int getRisqueModere() { return risqueModere; }
        public void setRisqueModere(int risqueModere) { this.risqueModere = risqueModere; }

        public double getScoreMoyen() { return scoreMoyen; }
        public void setScoreMoyen(double scoreMoyen) { this.scoreMoyen = scoreMoyen; }
    }
}