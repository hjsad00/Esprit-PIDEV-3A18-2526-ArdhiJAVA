package tn.neuron.ardhi.services.gestionemployeservice;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import tn.neuron.ardhi.models.gestionemployemodel.Tache;
import tn.neuron.ardhi.utils.UserAndDiag.AppConfig;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service Google Calendar — Service Account avec délégation vers calendrier utilisateur.
 *
 * POURQUOI LES TÂCHES N'APPARAISSAIENT PAS :
 *   Un Service Account a son propre calendrier privé (invisible dans l'interface Google).
 *   Il faut lui dire d'écrire dans LE CALENDRIER DE L'UTILISATEUR via config.properties (google.calendar.id).
 *
 * SOLUTION : Configurer google.calendar.id dans config.properties avec l'email du calendrier cible
 *   + Partager le calendrier avec le Service Account (accès écriture)
 *
 * SETUP (si pas encore fait) :
 *   1. Google Calendar → Paramètres ⚙ → ton calendrier
 *   2. "Partager avec des personnes spécifiques" → Ajouter l'email du Service Account
 *   3. Permission : "Apporter des modifications aux événements" → Enregistrer
 */
public class GoogleCalendarService {

    private static final String APPLICATION_NAME  = "Ardhi - Gestion Employés";
    private static final JsonFactory JSON_FACTORY  = GsonFactory.getDefaultInstance();
    private static final String CREDENTIALS_PATH   = AppConfig.get("google.credentials.employe.path", "/google/credentials.json");
    private static final List<String> SCOPES =
            Collections.singletonList(CalendarScopes.CALENDAR);

    // ── CONFIGURATION CALENDRIER CIBLE ───────────────────────────────────────
    // Configuré via config.properties (clé: google.calendar.id)
    // "primary" = calendrier privé du Service Account (INVISIBLE pour l'utilisateur)
    private String calendarId = AppConfig.get("google.calendar.id", "primary");

    private static GoogleCalendarService instance;
    private Calendar calendarService;
    private boolean connected = false;
    private String lastError  = "";
    private String accountEmail = "";

    // Optionnel : injecté pour sauvegarder le googleEventId après création
    private TacheService tacheService;

    private GoogleCalendarService() {}

    /**
     * Permet à TacheController d'injecter TacheService pour la persistance de googleEventId.
     */
    public void setTacheService(TacheService ts) {
        this.tacheService = ts;
    }

    public static GoogleCalendarService getInstance() {
        if (instance == null) instance = new GoogleCalendarService();
        return instance;
    }

    // ── Connexion Service Account ─────────────────────────────────────────────

    public boolean connecter() {
        if (connected && calendarService != null) return true;
        try {
            InputStream in = getClass().getResourceAsStream(CREDENTIALS_PATH);
            if (in == null) {
                lastError = "credentials.json introuvable dans les ressources.\n"
                        + "Placer dans : src/main/resources/google/credentials.json";
                System.err.println("[GCal] ❌ " + lastError);
                return false;
            }

            GoogleCredentials credentials = ServiceAccountCredentials
                    .fromStream(in)
                    .createScoped(SCOPES);

            if (credentials instanceof ServiceAccountCredentials) {
                accountEmail = ((ServiceAccountCredentials) credentials).getClientEmail();
            }

            NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

            calendarService = new Calendar.Builder(transport, JSON_FACTORY, requestInitializer)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            connected = true;
            System.out.println("[GCal] ✅ Service Account connecté : " + accountEmail);
            System.out.println("[GCal] 📅 Calendrier cible : " + calendarId);
            return true;

        } catch (Exception e) {
            lastError = "Erreur connexion : " + e.getMessage();
            System.err.println("[GCal] ❌ " + lastError);
            e.printStackTrace();
            return false;
        }
    }

    public boolean isConnected()    { return connected && calendarService != null; }
    public String getLastError()    { return lastError; }
    public String getAccountEmail() { return accountEmail; }
    public String getCalendarId()   { return calendarId; }

    /**
     * Changer le calendrier cible.
     * Utiliser l'email Gmail de l'utilisateur dont le calendrier est partagé.
     * Ex: setCalendarId("user@gmail.com")
     */
    public void setCalendarId(String id) {
        this.calendarId = id;
        System.out.println("[GCal] 📅 Nouveau calendrier cible : " + id);
    }

    public void deconnecter() {
        connected = false;
        calendarService = null;
        System.out.println("[GCal] Déconnecté");
    }

    // ── CRUD Événements ──────────────────────────────────────────────────────

    public String creerEvenement(Tache tache) {
        if (!isConnected()) return null;
        try {
            Event event = tacheVersEvent(tache);
            Event created = calendarService.events()
                    .insert(calendarId, event)
                    .execute();
            String eventId = created.getId();
            System.out.println("[GCal] ✅ Créé dans '" + calendarId + "' : " + created.getSummary()
                    + " | ID: " + eventId);
            // 💾 Sauvegarder le googleEventId dans la base de données
            if (tacheService != null && eventId != null) {
                tache.setGoogleEventId(eventId); // mise à jour en mémoire
                tacheService.saveGoogleEventId(tache.getId(), eventId);
            }
            return eventId;
        } catch (Exception e) {
            lastError = "Erreur création : " + e.getMessage();
            System.err.println("[GCal] ❌ " + lastError);
            // Diagnostic spécifique
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                System.err.println("[GCal] ⚠️ Calendrier '" + calendarId + "' introuvable.");
                System.err.println("[GCal]    → Vérifier que le calendrier est partagé avec : " + accountEmail);
            } else if (e.getMessage() != null && e.getMessage().contains("403")) {
                System.err.println("[GCal] ⚠️ Accès refusé au calendrier '" + calendarId + "'.");
                System.err.println("[GCal]    → Le Service Account n'a pas la permission d'écriture.");
                System.err.println("[GCal]    → Google Calendar → Paramètres → Partager avec : " + accountEmail);
            }
            return null;
        }
    }

    public int creerEvenements(List<Tache> taches) {
        if (!isConnected()) return 0;
        int ok = 0;
        for (Tache t : taches) {
            if (creerEvenement(t) != null) ok++;
        }
        System.out.println("[GCal] Batch terminé : " + ok + "/" + taches.size() + " créés dans " + calendarId);
        return ok;
    }

    public boolean mettreAJourEvenement(String googleEventId, Tache tache) {
        if (!isConnected() || googleEventId == null) return false;
        try {
            Event event = tacheVersEvent(tache);
            calendarService.events().update(calendarId, googleEventId, event).execute();
            System.out.println("[GCal] ✅ Mis à jour : " + googleEventId);
            return true;
        } catch (Exception e) {
            lastError = "Erreur MAJ : " + e.getMessage();
            System.err.println("[GCal] ❌ " + lastError);
            return false;
        }
    }

    public boolean supprimerEvenement(String googleEventId) {
        if (!isConnected() || googleEventId == null) return false;
        try {
            calendarService.events().delete(calendarId, googleEventId).execute();
            System.out.println("[GCal] ✅ Supprimé : " + googleEventId);
            return true;
        } catch (Exception e) {
            lastError = "Erreur suppression : " + e.getMessage();
            System.err.println("[GCal] ❌ " + lastError);
            return false;
        }
    }

    /**
     * Nettoie les événements Google Calendar dont la tâche Ardhi n'existe plus en base.
     * Scanne tous les événements [Ardhi] sur une plage de 2 ans et supprime les orphelins.
     *
     * @param idsExistants  Ensemble des IDs de tâches encore présentes en base de données
     * @return  Nombre d'événements orphelins supprimés
     */
    public int nettoyerEvenementsOrphelins(java.util.Set<Integer> idsExistants) {
        if (!isConnected()) return 0;
        int supprimes = 0;
        try {
            // Scan large : 2 ans en arrière jusqu'à 1 an en avant
            LocalDate debut = LocalDate.now().minusYears(2);
            LocalDate fin   = LocalDate.now().plusYears(1);
            DateTime tMin = new DateTime(
                    debut.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
            DateTime tMax = new DateTime(
                    fin.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

            String pageToken = null;
            do {
                com.google.api.services.calendar.Calendar.Events.List req =
                        calendarService.events().list(calendarId)
                                .setTimeMin(tMin)
                                .setTimeMax(tMax)
                                .setOrderBy("startTime")
                                .setSingleEvents(true)
                                .setMaxResults(250);
                if (pageToken != null) req.setPageToken(pageToken);
                Events page = req.execute();
                List<Event> items = page.getItems();
                if (items == null) break;

                for (Event ev : items) {
                    String summary = ev.getSummary();
                    if (summary == null || !summary.toLowerCase().contains("[ardhi]")) continue;

                    // Extraire l'ID de tâche depuis la description : "ID tâche  : #<n>"
                    String desc = ev.getDescription();
                    Integer taskId = extraireIdTache(desc);
                    if (taskId == null) continue; // événement Ardhi sans ID → ignorer

                    if (!idsExistants.contains(taskId)) {
                        // La tâche a été supprimée de la BDD → supprimer l'événement Google
                        try {
                            calendarService.events().delete(calendarId, ev.getId()).execute();
                            supprimes++;
                            System.out.println("[GCal] 🧹 Orphelin supprimé : '" + summary + "' (tâche #" + taskId + ")");
                        } catch (Exception ex) {
                            System.err.println("[GCal] ❌ Impossible de supprimer " + ev.getId() + " : " + ex.getMessage());
                        }
                    }
                }
                pageToken = page.getNextPageToken();
            } while (pageToken != null);

        } catch (Exception e) {
            lastError = "Erreur nettoyage : " + e.getMessage();
            System.err.println("[GCal] ❌ " + lastError);
        }
        System.out.println("[GCal] 🧹 Nettoyage terminé : " + supprimes + " événement(s) orphelin(s) supprimé(s)");
        return supprimes;
    }

    /**
     * Extrait l'ID de tâche depuis la description d'un événement Google.
     * Format attendu dans la description : "ID tâche  : #<n>"
     */
    private Integer extraireIdTache(String description) {
        if (description == null) return null;
        // Rechercher le pattern "ID tâche  : #<n>" (insensible à la casse)
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "id\\s+t[aâ]che\\s*:\\s*#(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(description);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); }
            catch (NumberFormatException ignored) {}
        }
        return null;
    }


    public List<Event> getEvenements(LocalDate debut, LocalDate fin) {
        if (!isConnected()) return Collections.emptyList();
        try {
            DateTime tMin = new DateTime(
                    debut.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
            DateTime tMax = new DateTime(
                    fin.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

            Events events = calendarService.events().list(calendarId)
                    .setTimeMin(tMin)
                    .setTimeMax(tMax)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            List<Event> items = events.getItems();
            System.out.println("[GCal] " + (items != null ? items.size() : 0)
                    + " événement(s) dans '" + calendarId + "'");
            return items != null ? items : Collections.emptyList();
        } catch (Exception e) {
            lastError = "Erreur récupération : " + e.getMessage();
            System.err.println("[GCal] ❌ " + lastError);
            return Collections.emptyList();
        }
    }

    public String getNomCalendrier() {
        if (!isConnected()) return null;
        try {
            String nom = calendarService.calendars().get(calendarId).execute().getSummary();
            return nom;
        } catch (Exception e) {
            System.err.println("[GCal] getNom: " + e.getMessage());
            // Retourner l'email plutôt que null pour l'affichage dans l'UI
            return accountEmail.isEmpty() ? "Service Account" : "⬤ " + calendarId;
        }
    }

    // ── Conversion Tache → Google Event ──────────────────────────────────────

    private Event tacheVersEvent(Tache tache) {
        Event event = new Event();

        String emoji = emojiStatut(tache.getStatut());
        event.setSummary(emoji + " [Ardhi] " + tache.getTitre());

        StringBuilder desc = new StringBuilder();
        if (tache.getDescription() != null && !tache.getDescription().isBlank())
            desc.append(tache.getDescription()).append("\n\n");
        desc.append("─── Ardhi ───\n");
        desc.append("Statut    : ").append(tache.getStatut() != null ? tache.getStatut() : "—").append("\n");
        desc.append("Priorité  : ").append(labelPrio(tache.getPriorite())).append("\n");
        desc.append("Catégorie : ").append(tache.getCategorie() != null ? tache.getCategorie() : "—").append("\n");
        if (tache.getIdEmploye() != null)
            desc.append("Employé # : ").append(tache.getIdEmploye()).append("\n");
        desc.append("ID tâche  : #").append(tache.getId());
        event.setDescription(desc.toString());

        // Dates journée entière
        LocalDate debut = tache.getDateDebut() != null ? tache.getDateDebut() : LocalDate.now();
        LocalDate finD  = tache.getDateFin()   != null ? tache.getDateFin()   : debut;

        EventDateTime start = new EventDateTime()
                .setDate(new DateTime(debut.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .setTimeZone("Africa/Tunis");
        EventDateTime end = new EventDateTime()
                .setDate(new DateTime(finD.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .setTimeZone("Africa/Tunis");
        event.setStart(start);
        event.setEnd(end);

        // Couleur selon priorité
        if (tache.getPriorite() != null) {
            event.setColorId(switch (tache.getPriorite()) {
                case 4 -> "11"; // Tomate — Critique
                case 3 -> "6";  // Tangerine — Haute
                case 2 -> "1";  // Lavande — Moyenne
                default -> "2"; // Sauge — Basse
            });
        }

        event.setSource(new Event.Source()
                .setTitle("Ardhi - Tâche #" + tache.getId())
                .setUrl("https://ardhi.tn"));

        return event;
    }

    private String emojiStatut(String s) {
        if (s == null) return "⏳";
        return switch (s) {
            case "En cours" -> "🔵";
            case "Terminé"  -> "✅";
            case "Validé"   -> "✔";
            case "Annulé"   -> "❌";
            default         -> "⏳";
        };
    }

    private String labelPrio(Integer p) {
        if (p == null) return "—";
        return switch (p) {
            case 4 -> "🔴 Critique";
            case 3 -> "🟠 Haute";
            case 2 -> "🔵 Moyenne";
            default -> "🟢 Basse";
        };
    }
}