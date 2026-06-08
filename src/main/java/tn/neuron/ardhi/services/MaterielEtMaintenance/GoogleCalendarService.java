package tn.neuron.ardhi.services.MaterielEtMaintenance;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import tn.neuron.ardhi.models.MaterielEtMaintenance.Materiel;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import tn.neuron.ardhi.utils.UserAndDiag.AppConfig;

public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "ARDHI - Gestion Agricole";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private Calendar calendarService;
    private String currentUserEmail;
    private boolean isConnected = false;

    public GoogleCalendarService(String userEmail) {
        this.currentUserEmail = userEmail;
        try {
            System.out.println("Initialisation Google Calendar pour: " + userEmail);
            initialize();
            this.isConnected = true;
            System.out.println("Service Google Calendar initialise pour: " + userEmail);
        } catch (Exception e) {
            System.err.println("Erreur Google Calendar pour " + userEmail + ": " + e.getMessage());
            System.err.println("Mode simulation active");
            this.isConnected = false;
        }
    }

    public GoogleCalendarService() {
        this.currentUserEmail = "simulation@ardhi.local";
        this.isConnected = false;
    }

    private void initialize() throws Exception {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        calendarService = new Calendar.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws Exception {
        String path = AppConfig.get("google.credentials.maintenance.path", "credentials.json");
        File credentialsFile = new File(path);
        if (!credentialsFile.exists()) {
            throw new Exception("Fichier Google Credentials introuvable à : " + path);
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(new FileInputStream(credentialsFile)));

        String userTokensPath = TOKENS_DIRECTORY_PATH + "/" + sanitizeEmail(currentUserEmail);
        File tokensDir = new File(userTokensPath);
        if (!tokensDir.exists()) {
            tokensDir.mkdirs();
        }

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(tokensDir))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8888)
                .build();

        return new AuthorizationCodeInstalledApp(flow, receiver).authorize(currentUserEmail);
    }

    private String sanitizeEmail(String email) {
        return email.replaceAll("[^a-zA-Z0-9]", "_");
    }

    // ════════════════════════════════════════════════════════
    //  ✅ testConnexion — NE déclenche PAS de réauthentification
    //  Vérifie juste si le service est prêt
    // ════════════════════════════════════════════════════════

    public boolean testConnexion() {
        // ✅ On vérifie juste si le service est initialisé — pas d'appel réseau
        // qui pourrait déclencher une nouvelle fenêtre navigateur
        if (calendarService == null || !isConnected) {
            System.out.println("Service Google Calendar non connecte");
            return false;
        }
        System.out.println("Connexion Google Calendar OK pour: " + currentUserEmail);
        return true;
    }

    // ════════════════════════════════════════════════════════
    //  PLANIFIER MAINTENANCE
    // ════════════════════════════════════════════════════════

    public String planifierMaintenance(Materiel materiel, LocalDateTime dateTime,
                                       String description) {
        try {
            if (calendarService == null || !isConnected) {
                System.out.println("Mode simulation active");
                return genererIdSimulation(materiel);
            }

            Event event = new Event()
                    .setSummary("Maintenance: " + materiel.getNom())
                    .setLocation("Exploitation Agricole - " + currentUserEmail)
                    .setDescription(creerDescription(materiel, description));

            configurerDates(event, dateTime);

            EventReminder[] reminderOverrides = new EventReminder[]{
                    new EventReminder().setMethod("email").setMinutes(24 * 60),
                    new EventReminder().setMethod("popup").setMinutes(60),
            };
            Event.Reminders reminders = new Event.Reminders()
                    .setUseDefault(false)
                    .setOverrides(Arrays.asList(reminderOverrides));
            event.setReminders(reminders);
            event.setColorId("10");

            Event createdEvent = calendarService.events()
                    .insert("primary", event)
                    .setSendNotifications(true)
                    .execute();

            String eventId = createdEvent.getId();
            String lienDirect = "https://calendar.google.com/calendar/event?eid="
                    + java.util.Base64.getEncoder()
                    .encodeToString((eventId + " " + currentUserEmail).getBytes())
                    .replaceAll("=", "")
                    + "&authuser=" + currentUserEmail;

            System.out.println("Evenement cree: " + createdEvent.getSummary());
            System.out.println("Lien: " + lienDirect); // ← ouvre directement le bon compte
            return createdEvent.getId();

        } catch (Exception e) {
            System.err.println("Erreur Google Calendar: " + e.getMessage());
            e.printStackTrace();
            return genererIdSimulation(materiel);
        }
    }

    private String genererIdSimulation(Materiel materiel) {
        return "SIM_" + materiel.getId_materiel() + "_" + System.currentTimeMillis();
    }

    private String creerDescription(Materiel materiel, String description) {
        StringBuilder desc = new StringBuilder();
        desc.append("MAINTENANCE MATERIEL AGRICOLE\n\n");
        desc.append("Materiel: ").append(materiel.getNom()).append("\n");
        desc.append("Type: ").append(materiel.getType()).append("\n");
        desc.append("Etat actuel: ").append(materiel.getEtat()).append("\n");

        if (materiel.getDate_achat() != null)
            desc.append("Date d'achat: ").append(materiel.getDate_achat()).append("\n");

        if (materiel.getDerniere_maintenance() != null)
            desc.append("Derniere maintenance: ").append(materiel.getDerniere_maintenance()).append("\n");

        desc.append("Frequence: tous les ")
                .append(materiel.getFrequence_maintenance_mois()).append(" mois\n");

        desc.append("\nDESCRIPTION:\n").append(description).append("\n\n");
        desc.append("Planifie par: ").append(currentUserEmail).append("\n");
        desc.append("Plateforme: ARDHI - Gestion Agricole\n\n");
        desc.append("N'oubliez pas de mettre a jour l'etat du materiel apres la maintenance!");

        return desc.toString();
    }

    private void configurerDates(Event event, LocalDateTime dateTime) {
        DateTime startDateTime = new DateTime(
                Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant()));
        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone("Africa/Tunis");

        DateTime endDateTime = new DateTime(
                Date.from(dateTime.plusHours(2).atZone(ZoneId.systemDefault()).toInstant()));
        EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone("Africa/Tunis");

        event.setStart(start);
        event.setEnd(end);
    }

    public boolean annulerMaintenance(String eventId) {
        try {
            if (calendarService == null || eventId.startsWith("SIM_")) {
                return true;
            }
            calendarService.events().delete("primary", eventId).execute();
            return true;
        } catch (Exception e) {
            System.err.println("Erreur suppression: " + e.getMessage());
            return false;
        }
    }

    public void deconnecterUtilisateur() {
        String userTokensPath = TOKENS_DIRECTORY_PATH + "/" + sanitizeEmail(currentUserEmail);
        File tokensDir = new File(userTokensPath);
        if (tokensDir.exists()) {
            deleteDirectory(tokensDir);
        }
        this.isConnected = false;
        this.calendarService = null;
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) deleteDirectory(file);
                else file.delete();
            }
        }
        directory.delete();
    }

    public boolean isConnected()         { return isConnected; }
    public String getCurrentUserEmail()  { return currentUserEmail; }
    public boolean estEnModeSimulation() { return !isConnected; }
}