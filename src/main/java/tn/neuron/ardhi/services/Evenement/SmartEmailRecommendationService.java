package tn.neuron.ardhi.services.Evenement;

import tn.neuron.ardhi.models.Evenement.Evenement;
import tn.neuron.ardhi.models.Evenement.Participation;
import tn.neuron.ardhi.services.Evenement.InactiveParticipantDetectionService.ParticipantRiskProfile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service d'emails personnalisés - VERSION HTML LISIBLE
 */
public class SmartEmailRecommendationService {

    private final EvenementService evenementService;
    private final ParticipationService participationService;
    private final EmailsService emailsService;

    public SmartEmailRecommendationService() {
        this.evenementService = new EvenementService();
        this.participationService = new ParticipationService();
        this.emailsService = new EmailsService();
    }

    /**
     * Envoie une relance avec HTML propre et lisible
     */
    public void envoyerRelancePersonnalisee(ParticipantRiskProfile profile) {
        UserPreferences preferences = analyserHistoriqueUtilisateur(profile.getUserId());
        List<Evenement> evenementsRecommandes = recommanderEvenements(preferences);

        String sujet = genererSujetPersonnalise(profile, preferences);
        String messageHTML = genererEmailHTML(profile, preferences, evenementsRecommandes);

        emailsService.envoyerEmail(profile.getEmail(), sujet, messageHTML);
    }

    /**
     * Génère un email HTML PROPRE et LISIBLE
     */
    private String genererEmailHTML(
            ParticipantRiskProfile profile,
            UserPreferences prefs,
            List<Evenement> recommandations) {

        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'></head><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;'>");

        // HEADER
        html.append("<div style='background: linear-gradient(135deg, #667A3F, #8BC34A); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0;'>");
        html.append("<h1 style='margin: 0; font-size: 28px;'>🌱 ARDHI</h1>");
        html.append("<p style='margin: 10px 0 0 0; font-size: 14px;'>Plateforme Agricole</p>");
        html.append("</div>");

        // GREETING
        html.append("<div style='background: white; padding: 30px; border: 1px solid #e0e0e0;'>");
        html.append(String.format("<p style='font-size: 18px; margin-bottom: 20px;'>Bonjour <strong>%s</strong>,</p>", profile.getPrenom()));

        // HISTORIQUE
        if (prefs.getNombreParticipationsPassees() > 0) {
            html.append("<div style='background: #F8F9FA; padding: 20px; border-radius: 8px; margin: 20px 0;'>");
            html.append("<h3 style='color: #667A3F; margin-top: 0;'>📊 Votre historique</h3>");
            html.append(String.format("<p>✓ Vous avez participé à <strong>%d événement%s</strong><br>",
                    prefs.getNombreParticipationsPassees(),
                    prefs.getNombreParticipationsPassees() > 1 ? "s" : ""));
            html.append("✓ Merci de votre confiance et fidélité!</p>");
            html.append("</div>");
        }

        // RAISON
        html.append("<div style='background: #E8F5E9; padding: 20px; border-left: 4px solid #4CAF50; margin: 20px 0;'>");
        html.append("<h3 style='margin-top: 0; color: #2E7D32;'>💬 Pourquoi cet email ?</h3>");

        if (profile.getRiskScore() >= 80) {
            html.append("<p>🎯 Nous avons remarqué votre absence récente<br>");
            html.append("🎁 <strong>OFFRE SPÉCIALE</strong>: Inscription GRATUITE à votre prochain événement!</p>");
        } else if (profile.getRiskScore() >= 60) {
            html.append("<p>📢 Nous travaillons à améliorer nos événements<br>");
            html.append("💡 Votre avis compte énormément pour nous<br>");
            html.append("📧 Répondez à cet email pour nous dire ce qui vous intéresserait</p>");
        } else {
            html.append("<p>🌟 Nous avons sélectionné des événements spécialement pour VOUS!</p>");
        }
        html.append("</div>");

        // ÉVÉNEMENTS RECOMMANDÉS
        if (!recommandations.isEmpty()) {
            html.append("<div style='margin: 30px 0;'>");
            html.append("<h2 style='color: #667A3F; border-bottom: 3px solid #8BC34A; padding-bottom: 10px;'>🎯 Vos événements recommandés</h2>");

            if (!prefs.getTypesPreferences().isEmpty()) {
                html.append(String.format("<p style='color: #666; margin-bottom: 25px;'>Basé sur votre intérêt pour: <strong>%s</strong></p>",
                        prefs.getTypesPreferences().stream()
                                .map(this::traduireType)
                                .collect(Collectors.joining(" et "))));
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            for (int i = 0; i < recommandations.size(); i++) {
                Evenement evt = recommandations.get(i);

                String emoji = switch (evt.getType()) {
                    case "FOIRE" -> "🏪";
                    case "FORMATION" -> "📚";
                    case "CONFERENCE" -> "🎤";
                    case "ATELIER" -> "🔧";
                    default -> "🌾";
                };

                html.append("<div style='background: #FFFFFF; border: 2px solid #E0E0E0; border-radius: 10px; padding: 20px; margin: 15px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>");

                html.append(String.format("<h3 style='color: #667A3F; margin-top: 0; border-bottom: 2px solid #8BC34A; padding-bottom: 10px;'>%s Événement #%d</h3>", emoji, i + 1));

                html.append(String.format("<p style='margin: 15px 0;'><strong>📌 TITRE:</strong> %s</p>", evt.getTitre()));
                html.append(String.format("<p style='margin: 15px 0;'><strong>📍 LIEU:</strong> %s</p>", evt.getLieu()));
                html.append(String.format("<p style='margin: 15px 0;'><strong>📅 DATE:</strong> %s</p>",
                        evt.getDateDebut() != null ? evt.getDateDebut().format(formatter) : "À confirmer"));
                html.append(String.format("<p style='margin: 15px 0;'><strong>👥 PLACES:</strong> %d places disponibles</p>", evt.getNombrePlacesMax()));

                if (evt.getDescription() != null && !evt.getDescription().isEmpty()) {
                    String desc = evt.getDescription().length() > 200 ?
                            evt.getDescription().substring(0, 200) + "..." :
                            evt.getDescription();
                    html.append(String.format("<div style='background: #F8F9FA; padding: 15px; border-radius: 5px; margin-top: 15px;'><strong>📝 DESCRIPTION:</strong><br>%s</div>", desc));
                }

                html.append("</div>");
            }

            html.append("<p style='text-align: center; margin-top: 25px;'>");
            html.append("<a href='http://www.ardhi-platform.tn' style='display: inline-block; background: linear-gradient(135deg, #667A3F, #8BC34A); color: white; padding: 15px 40px; text-decoration: none; border-radius: 25px; font-weight: bold;'>🔗 Voir tous les événements</a>");
            html.append("</p>");

        } else {
            html.append("<div style='background: #FFF3CD; border: 2px solid #FFC107; border-radius: 8px; padding: 20px; margin: 20px 0;'>");
            html.append("<h3 style='color: #856404; margin-top: 0;'>⚠️ Aucun événement prévu actuellement</h3>");
            html.append("<p>📬 Nous vous informerons dès qu'un nouvel événement correspondant à vos intérêts sera organisé.</p>");
            html.append("</div>");
        }

        // CALL TO ACTION
        html.append("<div style='background: #E3F2FD; padding: 20px; border-radius: 8px; margin: 25px 0;'>");
        html.append("<h3 style='color: #1565C0; margin-top: 0;'>💡 Comment s'inscrire ?</h3>");

        if (profile.getRiskScore() >= 60) {
            html.append("<ol style='margin: 15px 0;'>");
            html.append("<li>📧 Répondez directement à cet email</li>");
            html.append("<li>📱 Appelez-nous au: +216 XX XXX XXX</li>");
            html.append("<li>💻 Connectez-vous sur www.ardhi-platform.tn</li>");
            html.append("</ol>");
        } else {
            html.append("<ol style='margin: 15px 0;'>");
            html.append("<li>Connectez-vous à votre compte Ardhi</li>");
            html.append("<li>Consultez la section 'Événements'</li>");
            html.append("<li>Cliquez sur 'S'inscrire'</li>");
            html.append("<li>Confirmez votre participation</li>");
            html.append("</ol>");
        }
        html.append("</div>");

        html.append("</div>"); // Fin content

        // FOOTER
        html.append("<div style='background: #F8F9FA; padding: 25px; text-align: center; border-radius: 0 0 10px 10px; border: 1px solid #e0e0e0; border-top: none;'>");
        html.append("<p style='margin: 10px 0; font-size: 14px;'>Cordialement,<br><strong>🌱 L'équipe Ardhi</strong><br>Votre partenaire pour l'agriculture moderne</p>");
        html.append("<p style='margin: 15px 0; font-size: 13px; color: #666;'>");
        html.append("📧 support@ardhi-platform.tn<br>");
        html.append("🌐 www.ardhi-platform.tn<br>");
        html.append("📱 +216 XX XXX XXX");
        html.append("</p>");

        if (!prefs.getLieuxPreferences().isEmpty()) {
            html.append(String.format("<p style='margin: 15px 0; font-size: 12px; color: #888;'>💡 Nous organisons régulièrement des événements à %s, votre région préférée!</p>",
                    prefs.getLieuxPreferences().get(0)));
        }

        html.append("<p style='margin-top: 20px; font-size: 12px; color: #999;'>Merci de faire partie de notre communauté! 🌾</p>");
        html.append("</div>");

        html.append("</body></html>");

        return html.toString();
    }

    // (Reste du code inchangé...)

    private UserPreferences analyserHistoriqueUtilisateur(int userId) {
        UserPreferences prefs = new UserPreferences();
        List<Participation> participations = participationService.getAllParticipations().stream()
                .filter(p -> p.getIdUtilisateur() == userId)
                .toList();

        if (participations.isEmpty()) return prefs;

        Map<String, Long> typesCount = new HashMap<>();
        Map<String, Long> lieuxCount = new HashMap<>();

        for (Participation p : participations) {
            Evenement evt = evenementService.getEvenementById(p.getIdEvenement());
            if (evt != null) {
                typesCount.merge(evt.getType(), 1L, Long::sum);
                lieuxCount.merge(evt.getLieu(), 1L, Long::sum);
                if ("PRESENT".equals(p.getStatut())) {
                    typesCount.merge(evt.getType(), 2L, Long::sum);
                }
            }
        }

        List<String> typesPreferences = typesCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(2)
                .toList();

        List<String> lieuxPreferences = lieuxCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(2)
                .toList();

        prefs.setTypesPreferences(typesPreferences);
        prefs.setLieuxPreferences(lieuxPreferences);
        prefs.setNombreParticipationsPassees(participations.size());

        return prefs;
    }

    private List<Evenement> recommanderEvenements(UserPreferences preferences) {
        List<Evenement> evenementsAVenir = evenementService.getEvenementsByStatut("A_VENIR");
        if (evenementsAVenir.isEmpty()) return new ArrayList<>();

        List<EvenementScore> scores = new ArrayList<>();

        for (Evenement evt : evenementsAVenir) {
            double score = 0.0;
            if (preferences.getTypesPreferences().contains(evt.getType())) {
                score += 50.0;
                if (preferences.getTypesPreferences().get(0).equals(evt.getType())) score += 20.0;
            }
            for (String lieuPref : preferences.getLieuxPreferences()) {
                if (evt.getLieu().toLowerCase().contains(lieuPref.toLowerCase())) {
                    score += 30.0;
                    break;
                }
            }
            if (evt.getDateDebut() != null) {
                long joursAvant = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), evt.getDateDebut());
                if (joursAvant > 0 && joursAvant <= 30) score += 20.0;
                else if (joursAvant > 30 && joursAvant <= 60) score += 10.0;
            }
            if (evt.getNombrePlacesMax() < 20) score -= 10.0;
            scores.add(new EvenementScore(evt, score));
        }

        return scores.stream()
                .sorted((e1, e2) -> Double.compare(e2.score, e1.score))
                .limit(3)
                .map(es -> es.evenement)
                .toList();
    }

    private String genererSujetPersonnalise(ParticipantRiskProfile profile, UserPreferences prefs) {
        if (profile.getRiskScore() >= 80) {
            return "🎁 " + profile.getPrenom() + ", une offre spéciale vous attend!";
        } else if (profile.getRiskScore() >= 60) {
            return "👋 " + profile.getPrenom() + ", nous aimerions votre avis";
        } else {
            if (!prefs.getTypesPreferences().isEmpty()) {
                String type = traduireType(prefs.getTypesPreferences().get(0));
                return "🌾 " + profile.getPrenom() + ", nouveaux " + type + " pour vous!";
            }
            return "🌱 " + profile.getPrenom() + ", événements sélectionnés pour vous";
        }
    }

    private String traduireType(String type) {
        return switch (type) {
            case "FOIRE" -> "foires agricoles";
            case "FORMATION" -> "formations";
            case "CONFERENCE" -> "conférences";
            case "ATELIER" -> "ateliers pratiques";
            default -> "événements";
        };
    }

    private static class EvenementScore {
        Evenement evenement;
        double score;
        EvenementScore(Evenement evenement, double score) {
            this.evenement = evenement;
            this.score = score;
        }
    }

    public static class UserPreferences {
        private List<String> typesPreferences = new ArrayList<>();
        private List<String> lieuxPreferences = new ArrayList<>();
        private int nombreParticipationsPassees = 0;

        public List<String> getTypesPreferences() { return typesPreferences; }
        public void setTypesPreferences(List<String> typesPreferences) { this.typesPreferences = typesPreferences; }
        public List<String> getLieuxPreferences() { return lieuxPreferences; }
        public void setLieuxPreferences(List<String> lieuxPreferences) { this.lieuxPreferences = lieuxPreferences; }
        public int getNombreParticipationsPassees() { return nombreParticipationsPassees; }
        public void setNombreParticipationsPassees(int nombreParticipationsPassees) {
            this.nombreParticipationsPassees = nombreParticipationsPassees;
        }
    }
}