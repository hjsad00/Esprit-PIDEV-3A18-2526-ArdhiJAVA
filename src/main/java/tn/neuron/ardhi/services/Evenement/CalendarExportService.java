package tn.neuron.ardhi.services.Evenement;

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
import tn.neuron.ardhi.models.Evenement.Evenement;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import tn.neuron.ardhi.utils.UserAndDiag.AppConfig;

/**
 * Service pour exporter des événements au format iCalendar (.ics)
 * ET via l'API Google Calendar officielle
 * Utilise ses propres credentials (projet ardhi-486800 - module Événements)
 */
public class CalendarExportService {

    // ══════════════════════════════════════════════
    // CONFIG API GOOGLE CALENDAR (ton propre projet)
    // ══════════════════════════════════════════════
    private static final String APPLICATION_NAME = "ARDHI - Module Événements";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    // Dossier tokens séparé de celui de ton amie (Matériel)
    private static final String TOKENS_DIRECTORY_PATH = "tokens_evenements";
    // Ton propre fichier (chemin issu de config.properties)
    private static final String CREDENTIALS_FILE = AppConfig.get("google.credentials.evenement.path", "credentials_evenements.json");

    // ══════════════════════════════════════════════
    // CONFIG .ICS (fallback)
    // ══════════════════════════════════════════════
    private static final String ICS_DIR = "calendar_exports";

    private Calendar calendarService;
    private String currentUserEmail;
    private boolean isConnected = false;

    // ──────────────────────────────────────────────
    // CONSTRUCTEURS
    // ──────────────────────────────────────────────

    /**
     * Constructeur avec email utilisateur → utilise l'API Google Calendar
     */
    public CalendarExportService(String userEmail) {
        this.currentUserEmail = userEmail;

        // Créer dossier .ics
        File dir = new File(ICS_DIR);
        if (!dir.exists()) dir.mkdirs();

        // Initialiser API Google Calendar
        try {
            System.out.println("📅 Initialisation Google Calendar Événements pour: " + userEmail);
            initialize();
            this.isConnected = true;
            System.out.println("✅ Google Calendar Événements initialisé pour: " + userEmail);
        } catch (Exception e) {
            System.err.println("⚠️ Google Calendar non disponible: " + e.getMessage());
            System.err.println("→ Fallback sur fichier .ics");
            this.isConnected = false;
        }
    }

    /**
     * Constructeur sans email → mode .ics uniquement
     */
    public CalendarExportService() {
        this.currentUserEmail = null;
        this.isConnected = false;

        File dir = new File(ICS_DIR);
        if (!dir.exists()) dir.mkdirs();
    }

    // ──────────────────────────────────────────────
    // INITIALISATION API
    // ──────────────────────────────────────────────

    private void initialize() throws Exception {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        calendarService = new Calendar.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws Exception {
        // Chercher credentials_evenements.json (ton fichier à toi)
        File credentialsFile = new File(CREDENTIALS_FILE);
        if (!credentialsFile.exists()) {
            // Fallback sur credentials.json si ton fichier renommé n'existe pas encore
            credentialsFile = new File("credentials.json");
            if (!credentialsFile.exists()) {
                throw new Exception("credentials_evenements.json non trouvé à: "
                        + System.getProperty("user.dir"));
            }
        }

        System.out.println("✓ credentials trouvé: " + credentialsFile.getName());

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                JSON_FACTORY,
                new InputStreamReader(new FileInputStream(credentialsFile))
        );

        // Dossier tokens SÉPARÉ pour ne pas conflicte avec le module Matériel
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

        // Port différent de celui du module Matériel (8888) → pas de conflit
        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8889)
                .build();

        return new AuthorizationCodeInstalledApp(flow, receiver).authorize(currentUserEmail);
    }

    // ──────────────────────────────────────────────
    // MÉTHODE PRINCIPALE : Ajouter au calendrier
    // ──────────────────────────────────────────────

    /**
     * Ajoute l'événement DIRECTEMENT dans Google Calendar via API
     * Si API non disponible → fallback sur fichier .ics
     */
    public boolean ajouterAuCalendrier(Evenement evenement) {
        if (isConnected && calendarService != null) {
            return ajouterViaAPI(evenement);
        } else {
            System.out.println("→ Fallback .ics (API non disponible)");
            return ajouterViaICS(evenement);
        }
    }

    /**
     * Ajoute directement via l'API Google Calendar
     * L'événement apparaît AUTOMATIQUEMENT dans le calendrier (pas de clic)
     */
    private boolean ajouterViaAPI(Evenement evenement) {
        try {
            Event event = new Event()
                    .setSummary("🎪 " + evenement.getTitre())
                    .setLocation(evenement.getLieu())
                    .setDescription(construireDescriptionAPI(evenement));

            // Dates
            LocalDateTime dateDebut = evenement.getDateDebut().atTime(9, 0);
            LocalDateTime dateFin = evenement.getDateFin().atTime(17, 0);

            DateTime startDateTime = new DateTime(
                    Date.from(dateDebut.atZone(ZoneId.systemDefault()).toInstant()));
            DateTime endDateTime = new DateTime(
                    Date.from(dateFin.atZone(ZoneId.systemDefault()).toInstant()));

            event.setStart(new EventDateTime()
                    .setDateTime(startDateTime)
                    .setTimeZone("Africa/Tunis"));
            event.setEnd(new EventDateTime()
                    .setDateTime(endDateTime)
                    .setTimeZone("Africa/Tunis"));

            // Rappels
            EventReminder[] reminderOverrides = new EventReminder[]{
                    new EventReminder().setMethod("email").setMinutes(24 * 60), // 24h avant
                    new EventReminder().setMethod("popup").setMinutes(60),      // 1h avant
            };
            event.setReminders(new Event.Reminders()
                    .setUseDefault(false)
                    .setOverrides(Arrays.asList(reminderOverrides)));

            // Couleur verte (agricole)
            event.setColorId("10");

            // Insérer dans le calendrier
            Event created = calendarService.events()
                    .insert("primary", event)
                    .execute();

            System.out.println("✅ Événement ajouté dans Google Calendar !");
            System.out.println("🔗 Lien: " + created.getHtmlLink());
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur API Calendar: " + e.getMessage());
            // Fallback sur .ics si l'API échoue
            return ajouterViaICS(evenement);
        }
    }

    // ──────────────────────────────────────────────
    // FALLBACK : Fichier .ICS
    // ──────────────────────────────────────────────

    private boolean ajouterViaICS(Evenement evenement) {
        String icsPath = genererFichierICS(evenement);
        if (icsPath == null) return false;
        return ouvrirDansCalendrier(icsPath);
    }

    public String genererFichierICS(Evenement evenement) {
        try {
            String filename = ICS_DIR + "/event_" + evenement.getId() + "_"
                    + System.currentTimeMillis() + ".ics";

            File icsFile = new File(filename);
            FileWriter writer = new FileWriter(icsFile);
            writer.write(construireContenuICS(evenement));
            writer.close();

            System.out.println("✓ Fichier .ics créé: " + filename);
            return icsFile.getAbsolutePath();

        } catch (IOException e) {
            System.err.println("✗ Erreur création .ics: " + e.getMessage());
            return null;
        }
    }

    public boolean ouvrirDansCalendrier(String icsFilePath) {
        try {
            File icsFile = new File(icsFilePath);
            if (!icsFile.exists()) return false;

            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(icsFile);
                return true;
            }
            return false;

        } catch (IOException e) {
            System.err.println("✗ Erreur ouverture .ics: " + e.getMessage());
            return false;
        }
    }

    // ──────────────────────────────────────────────
    // UTILITAIRES
    // ──────────────────────────────────────────────

    private String construireContenuICS(Evenement evenement) {
        DateTimeFormatter icsFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
        LocalDateTime dateDebut = evenement.getDateDebut().atTime(9, 0);
        LocalDateTime dateFin = evenement.getDateFin().atTime(17, 0);
        String uid = UUID.randomUUID().toString() + "@ardhi.tn";
        String dtstamp = LocalDateTime.now().format(icsFormatter);
        String titre = evenement.getTitre().replaceAll("[\\n\\r]", " ");
        String lieu = evenement.getLieu().replaceAll("[\\n\\r]", " ");

        return String.format("""
BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//ARDHI//Plateforme Agricole//FR
CALSCALE:GREGORIAN
METHOD:PUBLISH
BEGIN:VEVENT
UID:%s
DTSTAMP:%s
DTSTART:%s
DTEND:%s
SUMMARY:%s
LOCATION:%s
STATUS:CONFIRMED
BEGIN:VALARM
TRIGGER:-PT24H
ACTION:DISPLAY
DESCRIPTION:Rappel: %s demain
END:VALARM
END:VEVENT
END:VCALENDAR
""",
                uid, dtstamp,
                dateDebut.format(icsFormatter),
                dateFin.format(icsFormatter),
                titre, lieu, titre);
    }

    private String construireDescriptionAPI(Evenement evenement) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        StringBuilder desc = new StringBuilder();
        desc.append("🎪 ÉVÉNEMENT AGRICOLE ARDHI\n\n");
        desc.append("📅 Du ").append(evenement.getDateDebut().format(formatter));
        desc.append(" au ").append(evenement.getDateFin().format(formatter)).append("\n");
        desc.append("📍 Lieu: ").append(evenement.getLieu()).append("\n");
        desc.append("🏷️ Type: ").append(evenement.getType()).append("\n");
        desc.append("👤 Organisateur: ").append(evenement.getOrganisateur()).append("\n");
        desc.append("👥 Places: ").append(evenement.getNombreParticipants())
                .append(" / ").append(evenement.getNombrePlacesMax()).append("\n");
        if (evenement.getDescription() != null && !evenement.getDescription().isEmpty()) {
            desc.append("\n📝 ").append(evenement.getDescription());
        }
        desc.append("\n\n🌾 Plateforme ARDHI - Événements Agricoles");
        return desc.toString();
    }

    private String sanitizeEmail(String email) {
        return email.replaceAll("[^a-zA-Z0-9]", "_");
    }

    public boolean isConnected() {
        return isConnected;
    }

    public boolean estEnModeSimulation() {
        return !isConnected;
    }
}