package tn.neuron.ardhi.utils.gestionemployeutils;

import javafx.geometry.NodeOrientation;
import javafx.scene.Parent;

import java.util.*;
import java.util.prefs.Preferences;

/**
 * 🌐 Gestionnaire de langue (Singleton)
 *
 * Gère la locale active et persiste le choix de l'utilisateur.
 * Supporte : Français (fr), Anglais (en), Arabe (ar)
 */
public class LanguageManager {

    private static LanguageManager instance;

    private static final String PREF_KEY = "gestionemploye.language";
    private static final String BUNDLE_BASE = "i18n.gestionemploye.messages";

    /** Locales supportées */
    public static final Locale LOCALE_FR = Locale.FRENCH;
    public static final Locale LOCALE_EN = Locale.ENGLISH;
    public static final Locale LOCALE_AR = new Locale("ar");

    private Locale currentLocale;
    private ResourceBundle currentBundle;
    private final List<Runnable> listeners = new ArrayList<>();

    private LanguageManager() {
        // Charger la langue persistée ou défaut FR
        String savedLang = Preferences.userNodeForPackage(LanguageManager.class).get(PREF_KEY, "fr");
        this.currentLocale = Locale.forLanguageTag(savedLang);
        this.currentBundle = ResourceBundle.getBundle(BUNDLE_BASE, currentLocale);
    }

    public static LanguageManager getInstance() {
        if (instance == null) {
            instance = new LanguageManager();
        }
        return instance;
    }

    // ── Getters ──────────────────────────────────────────────────────────

    public Locale getLocale() {
        return currentLocale;
    }

    public ResourceBundle getBundle() {
        return currentBundle;
    }

    /**
     * Raccourci pour bundle.getString(key)
     */
    public String get(String key) {
        try {
            return currentBundle.getString(key);
        } catch (MissingResourceException e) {
            System.err.println("⚠️ Clé i18n manquante: " + key);
            return "!" + key + "!";
        }
    }

    // ── Changement de langue ─────────────────────────────────────────────

    public void setLocale(Locale newLocale) {
        if (newLocale.equals(currentLocale)) return;

        this.currentLocale = newLocale;
        this.currentBundle = ResourceBundle.getBundle(BUNDLE_BASE, newLocale);

        // Persister le choix
        Preferences.userNodeForPackage(LanguageManager.class).put(PREF_KEY, newLocale.getLanguage());

        // Notifier les listeners
        listeners.forEach(Runnable::run);

        System.out.println("🌐 Langue changée → " + newLocale.getDisplayLanguage());
    }

    public void switchToFrench()  { setLocale(LOCALE_FR); }
    public void switchToEnglish() { setLocale(LOCALE_EN); }
    public void switchToArabic()  { setLocale(LOCALE_AR); }

    // ── Listeners ────────────────────────────────────────────────────────

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    // ── RTL Support ──────────────────────────────────────────────────────

    /**
     * Retourne true si la locale courante est RTL (arabe)
     */
    public boolean isRTL() {
        return "ar".equals(currentLocale.getLanguage());
    }

    /**
     * Applique l'orientation (LTR ou RTL) à un nœud JavaFX
     */
    public void applyOrientation(Parent root) {
        if (root != null) {
            root.setNodeOrientation(
                    isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT
            );
        }
    }

    /**
     * Retourne les 3 locales supportées
     */
    public static List<Locale> getSupportedLocales() {
        return List.of(LOCALE_FR, LOCALE_EN, LOCALE_AR);
    }
}
