package tn.neuron.ardhi.services.Evenement;

import tn.neuron.ardhi.models.Evenement.Evenement;
import tn.neuron.ardhi.models.Evenement.Participation;

import java.util.*;

/**
 * Service pour générer des statistiques sur les événements et participations
 */
public class StatistiquesService {

    private EvenementService evenementService;
    private ParticipationService participationService;

    public StatistiquesService() {
        this.evenementService = new EvenementService();
        this.participationService = new ParticipationService();
    }

    /**
     * Statistiques globales des événements
     */
    public Map<String, Object> getStatistiquesGlobales() {
        Map<String, Object> stats = new HashMap<>();

        List<Evenement> allEvents = evenementService.getAllEvenements();

        stats.put("totalEvenements", allEvents.size());
        stats.put("evenementsAVenir", allEvents.stream().filter(e -> "A_VENIR".equals(e.getStatut())).count());
        stats.put("evenementsEnCours", allEvents.stream().filter(e -> "EN_COURS".equals(e.getStatut())).count());
        stats.put("evenementsTermines", allEvents.stream().filter(e -> "TERMINE".equals(e.getStatut())).count());
        stats.put("evenementsAnnules", allEvents.stream().filter(e -> "ANNULE".equals(e.getStatut())).count());

        // Statistiques par type
        Map<String, Long> parType = new HashMap<>();
        parType.put("FOIRE", allEvents.stream().filter(e -> "FOIRE".equals(e.getType())).count());
        parType.put("FORMATION", allEvents.stream().filter(e -> "FORMATION".equals(e.getType())).count());
        parType.put("CONFERENCE", allEvents.stream().filter(e -> "CONFERENCE".equals(e.getType())).count());
        parType.put("ATELIER", allEvents.stream().filter(e -> "ATELIER".equals(e.getType())).count());
        stats.put("parType", parType);

        // Total participants
        int totalParticipants = allEvents.stream().mapToInt(Evenement::getNombreParticipants).sum();
        stats.put("totalParticipants", totalParticipants);

        // Moyenne participants par événement
        double moyenneParticipants = allEvents.isEmpty() ? 0 : (double) totalParticipants / allEvents.size();
        stats.put("moyenneParticipants", Math.round(moyenneParticipants * 100.0) / 100.0);

        return stats;
    }

    /**
     * Statistiques détaillées pour un événement spécifique
     */
    public Map<String, Object> getStatistiquesEvenement(int idEvenement) {
        Map<String, Object> stats = new HashMap<>();

        Evenement evenement = evenementService.getEvenementById(idEvenement);
        if (evenement == null) {
            return stats;
        }

        List<Participation> participations = participationService.getParticipationsByEvenement(idEvenement);

        stats.put("evenement", evenement.getTitre());
        stats.put("totalInscrits", participations.size());

        // Par statut
        long confirmes = participations.stream().filter(p -> "CONFIRME".equals(p.getStatut())).count();
        long enAttente = participations.stream().filter(p -> "EN_ATTENTE".equals(p.getStatut())).count();
        long annules = participations.stream().filter(p -> "ANNULE".equals(p.getStatut())).count();
        long presents = participations.stream().filter(p -> "PRESENT".equals(p.getStatut())).count();

        stats.put("confirmes", confirmes);
        stats.put("enAttente", enAttente);
        stats.put("annules", annules);
        stats.put("presents", presents);

        // Taux de présence
        double tauxPresence = confirmes > 0 ? (double) presents / confirmes * 100 : 0;
        stats.put("tauxPresence", Math.round(tauxPresence * 100.0) / 100.0);

        // Taux de remplissage
        double tauxRemplissage = evenement.getNombrePlacesMax() > 0
                ? (double) confirmes / evenement.getNombrePlacesMax() * 100
                : 0;
        stats.put("tauxRemplissage", Math.round(tauxRemplissage * 100.0) / 100.0);

        // Note moyenne
        double noteMoyenne = participationService.getNoteMoyenneEvenement(idEvenement);
        stats.put("noteMoyenne", Math.round(noteMoyenne * 100.0) / 100.0);

        // Nombre d'avis
        long nombreAvis = participations.stream().filter(p -> p.getNote() > 0).count();
        stats.put("nombreAvis", nombreAvis);

        // Distribution des notes
        Map<Integer, Long> distributionNotes = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            final int note = i;
            long count = participations.stream().filter(p -> p.getNote() == note).count();
            distributionNotes.put(i, count);
        }
        stats.put("distributionNotes", distributionNotes);

        return stats;
    }

    /**
     * Top événements par nombre de participants
     */
    public List<Map<String, Object>> getTopEvenements(int limit) {
        List<Map<String, Object>> topEvents = new ArrayList<>();

        List<Evenement> allEvents = evenementService.getAllEvenements();
        allEvents.sort((e1, e2) -> Integer.compare(e2.getNombreParticipants(), e1.getNombreParticipants()));

        for (int i = 0; i < Math.min(limit, allEvents.size()); i++) {
            Evenement e = allEvents.get(i);
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("id", e.getId());
            eventData.put("titre", e.getTitre());
            eventData.put("type", e.getType());
            eventData.put("participants", e.getNombreParticipants());
            eventData.put("placesMax", e.getNombrePlacesMax());
            eventData.put("tauxRemplissage",
                    Math.round((double) e.getNombreParticipants() / e.getNombrePlacesMax() * 10000.0) / 100.0);

            double noteMoyenne = participationService.getNoteMoyenneEvenement(e.getId());
            eventData.put("noteMoyenne", Math.round(noteMoyenne * 100.0) / 100.0);

            topEvents.add(eventData);
        }

        return topEvents;
    }

    /**
     * Événements les mieux notés
     */
    public List<Map<String, Object>> getEvenementsMieuxNotes(int limit) {
        List<Map<String, Object>> bestRated = new ArrayList<>();

        List<Evenement> allEvents = evenementService.getAllEvenements();

        // Calculer la note moyenne pour chaque événement
        List<Map<String, Object>> eventsWithRating = new ArrayList<>();
        for (Evenement e : allEvents) {
            double note = participationService.getNoteMoyenneEvenement(e.getId());
            if (note > 0) {
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("evenement", e);
                eventData.put("note", note);
                eventsWithRating.add(eventData);
            }
        }

        // Trier par note décroissante
        eventsWithRating.sort((e1, e2) -> Double.compare((Double) e2.get("note"), (Double) e1.get("note")));

        // Prendre les meilleurs
        for (int i = 0; i < Math.min(limit, eventsWithRating.size()); i++) {
            Map<String, Object> data = eventsWithRating.get(i);
            Evenement e = (Evenement) data.get("evenement");

            Map<String, Object> result = new HashMap<>();
            result.put("id", e.getId());
            result.put("titre", e.getTitre());
            result.put("type", e.getType());
            result.put("noteMoyenne", Math.round((Double) data.get("note") * 100.0) / 100.0);
            result.put("nombreParticipants", e.getNombreParticipants());

            bestRated.add(result);
        }

        return bestRated;
    }

    /**
     * Statistiques mensuelles (nombre d'événements par mois)
     */
    public Map<String, Integer> getStatistiquesMensuelles(int annee) {
        Map<String, Integer> stats = new LinkedHashMap<>();

        String[] mois = { "Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre",
                "Octobre", "Novembre", "Décembre" };

        List<Evenement> allEvents = evenementService.getAllEvenements();

        for (int i = 0; i < 12; i++) {
            final int moisNum = i + 1;
            long count = allEvents.stream()
                    .filter(e -> e.getDateDebut().getYear() == annee && e.getDateDebut().getMonthValue() == moisNum)
                    .count();
            stats.put(mois[i], (int) count);
        }

        return stats;
    }

    /**
     * Statistiques par organisateur
     */
    public List<Map<String, Object>> getStatistiquesParOrganisateur() {
        Map<String, Map<String, Object>> statsMap = new HashMap<>();

        List<Evenement> allEvents = evenementService.getAllEvenements();

        for (Evenement e : allEvents) {
            String org = e.getOrganisateur();

            if (!statsMap.containsKey(org)) {
                Map<String, Object> orgStats = new HashMap<>();
                orgStats.put("organisateur", org);
                orgStats.put("nombreEvenements", 0);
                orgStats.put("totalParticipants", 0);
                orgStats.put("noteMoyenne", 0.0);
                statsMap.put(org, orgStats);
            }

            Map<String, Object> orgStats = statsMap.get(org);
            orgStats.put("nombreEvenements", (int) orgStats.get("nombreEvenements") + 1);
            orgStats.put("totalParticipants", (int) orgStats.get("totalParticipants") + e.getNombreParticipants());
        }

        // Calculer la note moyenne par organisateur
        for (Map<String, Object> orgStats : statsMap.values()) {
            String org = (String) orgStats.get("organisateur");

            List<Evenement> orgEvents = allEvents.stream().filter(e -> org.equals(e.getOrganisateur())).toList();

            double totalNote = 0;
            int countNotes = 0;

            for (Evenement e : orgEvents) {
                double note = participationService.getNoteMoyenneEvenement(e.getId());
                if (note > 0) {
                    totalNote += note;
                    countNotes++;
                }
            }

            double moyenneNote = countNotes > 0 ? totalNote / countNotes : 0;
            orgStats.put("noteMoyenne", Math.round(moyenneNote * 100.0) / 100.0);
        }

        return new ArrayList<>(statsMap.values());
    }
}