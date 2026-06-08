package tn.neuron.ardhi.services.Parcelle_Cultures;

import tn.neuron.ardhi.models.Parcelle_Cultures.Culture;
import tn.neuron.ardhi.models.Parcelle_Cultures.IrrigationResult;
import tn.neuron.ardhi.models.Parcelle_Cultures.Parcelle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Service Métier Premium — Planificateur Intelligent des Besoins en Eau.
 *
 * Architecture : IrrigationService → Open-Meteo API (météo) → Calculs agronomiques
 *
 * Formules implémentées :
 *   ET0 = 0.0023 × (Tmoy + 17.8) × √(Tmax − Tmin)       [Hargreaves simplifié]
 *   Besoin brut  = Kc × ET0
 *   Besoin net   = max(0, Besoin − Précipitations)
 *   Volume eau   = BesoinNet × Surface(ha) × 10 000       [1 mm/ha = 10 000 L]
 *   Stress index = Tmoy / (Humidité% + 1)
 *   Efficacité   = Rendement(t/ha) / Volume(MLB)
 */
public class IrrigationService {

    private static final Logger LOGGER = Logger.getLogger(IrrigationService.class.getName());

    // Seuils de stress hydrique
    private static final double SEUIL_TEMP_STRESS    = 35.0;  // °C
    private static final double SEUIL_PRECIP_STRESS  = 5.0;   // mm
    private static final double SEUIL_INDEX_STRESS   = 0.6;   // Stress = T/(Humidité+1) normalisé

    // Seuils de niveau d'irrigation (litres/semaine)
    private static final double SEUIL_FAIBLE         = 5_000;
    private static final double SEUIL_MODERE         = 50_000;

    // ==================== BASE Kc PAR CULTURE ====================

    /**
     * Coefficients culturaux Kc (mm besoin hydrique moyen en mm/semaine)
     * adaptés aux cultures tunisiennes et nord-africaines.
     */
    private static final Map<String, Double> KC_CATALOGUE = new HashMap<>();

    static {
        // Céréales
        KC_CATALOGUE.put("blé",        25.0);
        KC_CATALOGUE.put("ble",        25.0);
        KC_CATALOGUE.put("blé dur",    25.0);
        KC_CATALOGUE.put("ble dur",    25.0);
        KC_CATALOGUE.put("orge",       20.0);
        KC_CATALOGUE.put("avoine",     22.0);
        KC_CATALOGUE.put("maïs",       35.0);
        KC_CATALOGUE.put("mais",       35.0);
        KC_CATALOGUE.put("sorgho",     28.0);
        KC_CATALOGUE.put("triticale",  23.0);

        // Légumineuses
        KC_CATALOGUE.put("pois chiche",28.0);
        KC_CATALOGUE.put("fève",       22.0);
        KC_CATALOGUE.put("feve",       22.0);
        KC_CATALOGUE.put("féverole",   22.0);
        KC_CATALOGUE.put("lentille",   18.0);
        KC_CATALOGUE.put("haricot",    30.0);
        KC_CATALOGUE.put("soja",       32.0);

        // Maraîchage
        KC_CATALOGUE.put("tomate",     40.0);
        KC_CATALOGUE.put("piment",     38.0);
        KC_CATALOGUE.put("poivron",    36.0);
        KC_CATALOGUE.put("aubergine",  37.0);
        KC_CATALOGUE.put("courgette",  35.0);
        KC_CATALOGUE.put("concombre",  42.0);
        KC_CATALOGUE.put("pastèque",   30.0);
        KC_CATALOGUE.put("pasteque",   30.0);
        KC_CATALOGUE.put("melon",      32.0);

        // Oléagineux
        KC_CATALOGUE.put("olive",      12.0);
        KC_CATALOGUE.put("olivier",    12.0);
        KC_CATALOGUE.put("tournesol",  30.0);
        KC_CATALOGUE.put("colza",      27.0);

        // Fourrage
        KC_CATALOGUE.put("luzerne",    45.0);
        KC_CATALOGUE.put("trèfle",     38.0);
        KC_CATALOGUE.put("trefle",     38.0);

        // Par défaut (inconnu)
        KC_CATALOGUE.put("__default__", 28.0);
    }

    // ==================== MOYENNES CLIMATIQUES SAISONNIÈRES (Mode dégradé) ====================

    /** Données saisonnières de repli pour la Tunisie centrale (Tunis / Sfax moyen) */
    private static final Map<String, double[]> METEO_SAISONNIERE = new HashMap<>();
    // Format : {Tmoy, Tmax, Tmin, Precip_mm_semaine, Humidite%}
    static {
        METEO_SAISONNIERE.put("Printemps", new double[]{18.0, 24.0, 12.0, 8.0,  60.0});
        METEO_SAISONNIERE.put("Été",       new double[]{30.0, 37.0, 23.0, 1.0,  45.0});
        METEO_SAISONNIERE.put("Automne",   new double[]{20.0, 26.0, 14.0, 12.0, 65.0});
        METEO_SAISONNIERE.put("Hiver",     new double[]{11.0, 16.0,  6.0, 18.0, 75.0});
    }

    // ==================== API Open-Meteo ====================

    private static final String OPEN_METEO_URL =
            "https://api.open-meteo.com/v1/forecast?"
          + "latitude=%s&longitude=%s"
          + "&hourly=temperature_2m,precipitation,relativehumidity_2m"
          + "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum"
          + "&forecast_days=7"
          + "&timezone=Africa%%2FTunis";

    // ==================== MÉTHODE PRINCIPALE ====================

    /**
     * Calcule le plan d'irrigation hebdomadaire pour une Culture sur une Parcelle.
     *
     * @param culture  Culture sélectionnée (Kc déduit du nom)
     * @param parcelle Parcelle avec coordonnées GPS
     * @return IrrigationResult complet avec tous les indicateurs
     */
    public IrrigationResult calculerPlanIrrigation(Culture culture, Parcelle parcelle) {
        LOGGER.info("▶ Calcul irrigation : " + culture.getNomCulture() + " sur " + parcelle.getLocalisation());

        IrrigationResult result = new IrrigationResult();
        result.setSurfaceHectares(culture.getSurfaceUtilisee() > 0
                ? culture.getSurfaceUtilisee()
                : parcelle.getSurface());

        // 1. Récupération météo (temps réel ou fallback)
        boolean meteoOk = false;
        if (parcelle.hasGpsCoordinates()) {
            meteoOk = remplirMeteoopenMeteo(result, parcelle.getLatitude(), parcelle.getLongitude());
        }
        if (!meteoOk) {
            remplirMeteoFallback(result);
        }

        // 2. Coefficient cultural Kc
        double kc = detecterKc(culture.getNomCulture());
        result.setKcCulture(kc);

        // 3. ET0 — Hargreaves simplifié
        double tDelta = result.getTemperatureMax() - result.getTemperatureMin();
        double et0 = 0.0023
                * (result.getTemperatureMoyenne() + 17.8)
                * Math.sqrt(Math.max(0, tDelta));
        result.setEt0(et0);

        // 4. Besoin brut
        double besoinBrut = kc * et0;
        result.setBesoinBrut(besoinBrut);

        // 5. Besoin net (soustraction des précipitations)
        double besoinNet = Math.max(0, besoinBrut - result.getPrecipitationsSemaine());
        result.setBesoinNet(besoinNet);

        // 6. Volume total en litres
        double litres = besoinNet * result.getSurfaceHectares() * 10_000.0;
        result.setVolumeEauLitres(litres);

        // 7. Niveau d'irrigation
        result.setNiveauIrrigation(calculerNiveauIrrigation(litres));

        // 8. Détection stress hydrique
        detecterStressHydrique(result);

        // 9. Efficacité hydrique
        if (litres > 0 && culture.getRendementEstime() > 0) {
            // Efficacité = (Rendement total en tonnes) / (Volume en mégalitres)
            double prodTotale = culture.getRendementEstime() * result.getSurfaceHectares();
            double efficacite = prodTotale / (litres / 1_000_000.0);
            result.setEfficaciteHydrique(efficacite);
        }

        // 10. Conseil principal
        result.setConseilPrincipal(genererConseil(result, culture, parcelle));

        LOGGER.info(String.format("✅ Plan calculé : BesoinNet=%.1f mm | Volume=%.0f L | Stress=%s",
                besoinNet, litres, result.isStressHydriqueDetecte() ? "OUI" : "NON"));

        return result;
    }

    // ==================== MÉTÉO OPEN-METEO ====================

    private boolean remplirMeteoopenMeteo(IrrigationResult result, double lat, double lon) {
        try {
            String urlStr = String.format(OPEN_METEO_URL,
                    String.valueOf(lat).replace(",", "."),
                    String.valueOf(lon).replace(",", "."));

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(8000);

            if (conn.getResponseCode() != 200) {
                LOGGER.warning("Open-Meteo HTTP " + conn.getResponseCode());
                conn.disconnect();
                return false;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            conn.disconnect();

            String json = sb.toString();
            LOGGER.fine("Open-Meteo response OK (" + json.length() + " chars)");

            // Parse JSON manuel des champs daily
            double tmax = parseFirstDailyValue(json, "temperature_2m_max");
            double tmin = parseFirstDailyValue(json, "temperature_2m_min");
            double tmoy = (tmax + tmin) / 2.0;
            double precip7j = parseSumDailyValues(json, "precipitation_sum", 7);
            double humidite = parseHourlyMean(json, "relativehumidity_2m", 24);

            result.setTemperatureMax(tmax);
            result.setTemperatureMin(tmin);
            result.setTemperatureMoyenne(tmoy);
            result.setPrecipitationsSemaine(precip7j);
            result.setHumidite(humidite);
            result.setDescriptionMeteo(buildDescriptionMeteo(tmoy, precip7j, humidite));
            result.setDonneesFallback(false);
            return true;

        } catch (Exception e) {
            LOGGER.warning("Erreur Open-Meteo : " + e.getMessage());
            return false;
        }
    }

    /** Parse la première valeur d'un tableau daily dans le JSON Open-Meteo */
    private double parseFirstDailyValue(String json, String key) {
        int idx = json.indexOf("\"" + key + "\":[");
        if (idx < 0) return 20.0;
        int start = json.indexOf('[', idx) + 1;
        int end = json.indexOf(',', start);
        if (end < 0) end = json.indexOf(']', start);
        try {
            return Double.parseDouble(json.substring(start, end).trim());
        } catch (Exception e) {
            return 20.0;
        }
    }

    /** Somme des N premières valeurs daily */
    private double parseSumDailyValues(String json, String key, int n) {
        int idx = json.indexOf("\"" + key + "\":[");
        if (idx < 0) return 5.0;
        int arrStart = json.indexOf('[', idx) + 1;
        double sum = 0;
        int pos = arrStart;
        for (int i = 0; i < n; i++) {
            int comma = json.indexOf(',', pos);
            int bracket = json.indexOf(']', pos);
            int end = (comma >= 0 && comma < bracket) ? comma : bracket;
            if (end < 0) break;
            try {
                String val = json.substring(pos, end).trim();
                if (!val.equals("null")) sum += Double.parseDouble(val);
            } catch (Exception ignored) {}
            pos = end + 1;
            if (pos >= bracket) break;
        }
        return sum;
    }

    /** Moyenne des N premières valeurs horaires */
    private double parseHourlyMean(String json, String key, int n) {
        int idx = json.indexOf("\"" + key + "\":[");
        if (idx < 0) return 60.0;
        int arrStart = json.indexOf('[', idx) + 1;
        double sum = 0;
        int count = 0;
        int pos = arrStart;
        for (int i = 0; i < n; i++) {
            int comma = json.indexOf(',', pos);
            int bracket = json.indexOf(']', pos);
            int end = (comma >= 0 && comma < bracket) ? comma : bracket;
            if (end < 0) break;
            try {
                sum += Double.parseDouble(json.substring(pos, end).trim());
                count++;
            } catch (Exception ignored) {}
            pos = end + 1;
            if (pos >= bracket) break;
        }
        return count > 0 ? sum / count : 60.0;
    }

    // ==================== FALLBACK SAISONNIER ====================

    private void remplirMeteoFallback(IrrigationResult result) {
        String saison = getSaisonActuelle();
        double[] vals = METEO_SAISONNIERE.getOrDefault(saison, METEO_SAISONNIERE.get("Printemps"));
        result.setTemperatureMoyenne(vals[0]);
        result.setTemperatureMax(vals[1]);
        result.setTemperatureMin(vals[2]);
        result.setPrecipitationsSemaine(vals[3]);
        result.setHumidite(vals[4]);
        result.setDescriptionMeteo("Données climatiques saisonnières (" + saison + " — Tunisie)");
        result.setDonneesFallback(true);
        LOGGER.info("Mode dégradé activé — saison: " + saison);
    }

    private String getSaisonActuelle() {
        int mois = LocalDate.now().getMonthValue();
        if (mois >= 3 && mois <= 5)  return "Printemps";
        if (mois >= 6 && mois <= 8)  return "Été";
        if (mois >= 9 && mois <= 11) return "Automne";
        return "Hiver";
    }

    // ==================== DÉTECTION Kc ====================

    /**
     * Détecte le coefficient Kc à partir du nom de la culture
     * (comparaison insensible à la casse, préfixe).
     */
    public double detecterKc(String nomCulture) {
        if (nomCulture == null || nomCulture.isBlank()) return KC_CATALOGUE.get("__default__");
        String lower = nomCulture.toLowerCase().trim();
        // Correspondance exacte
        if (KC_CATALOGUE.containsKey(lower)) return KC_CATALOGUE.get(lower);
        // Correspondance partielle
        for (Map.Entry<String, Double> entry : KC_CATALOGUE.entrySet()) {
            if (!entry.getKey().equals("__default__") && lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return KC_CATALOGUE.get("__default__");
    }

    /** Retourne le catalogue Kc entier pour affichage */
    public Map<String, Double> getKcCatalogue() {
        Map<String, Double> display = new HashMap<>(KC_CATALOGUE);
        display.remove("__default__");
        return display;
    }

    // ==================== NIVEAU IRRIGATION ====================

    private String calculerNiveauIrrigation(double litres) {
        if (litres <= 0)            return "AUCUN";
        if (litres < SEUIL_FAIBLE)  return "FAIBLE";
        if (litres < SEUIL_MODERE)  return "MODÉRÉ";
        return "ÉLEVÉ";
    }

    // ==================== STRESS HYDRIQUE ====================

    private void detecterStressHydrique(IrrigationResult result) {
        boolean stress = false;
        StringBuilder cause = new StringBuilder();

        // Règle 1 : Canicule + sécheresse
        if (result.getTemperatureMoyenne() > SEUIL_TEMP_STRESS
                && result.getPrecipitationsSemaine() < SEUIL_PRECIP_STRESS) {
            stress = true;
            cause.append("⚠️ Canicule (T > 35°C) avec précipitations insuffisantes (< 5 mm). ");
        }

        // Règle 2 : Indice de stress = Tmoy / (Humidité% + 1) normalisé
        double stressIndex = result.getTemperatureMoyenne() / (result.getHumidite() + 1.0);
        // Normalisé : seuil critique ≈ 35/(60+1)=0.57 → on seuille à 0.5
        if (stressIndex > 0.5) {
            stress = true;
            cause.append(String.format(
                    "⚠️ Indice de stress hydrique élevé (T/Hum = %.2f > 0.50). ", stressIndex));
        }

        result.setStressHydriqueDetecte(stress);
        result.setCauseStress(stress ? cause.toString().trim()
                : "✅ Aucun stress hydrique détecté — conditions favorables.");
    }

    // ==================== CONSEILS ====================

    private String genererConseil(IrrigationResult result, Culture culture, Parcelle parcelle) {
        StringBuilder sb = new StringBuilder();

        if (result.isStressHydriqueDetecte()) {
            sb.append("🔴 ALERTE STRESS HYDRIQUE — Irriguer en priorité (matin ou soir pour limiter l'évaporation). ");
        }

        if ("ÉLEVÉ".equals(result.getNiveauIrrigation())) {
            sb.append("Fractionner les apports en plusieurs séances dans la semaine. ");
        }

        if ("Goutte à goutte".equalsIgnoreCase(parcelle.getSystemeIrrigation())
                || (parcelle.getSystemeIrrigation() != null
                    && parcelle.getSystemeIrrigation().toLowerCase().contains("goutte"))) {
            sb.append("✅ Votre système goutte-à-goutte est optimal — efficacité estimée +30%. ");
        } else if ("AUCUN".equals(result.getNiveauIrrigation())) {
            sb.append("Les précipitations couvrent le besoin cette semaine — aucune irrigation nécessaire. ");
        } else {
            sb.append("Envisagez le goutte-à-goutte pour réduire la consommation d'eau de 30%. ");
        }

        if (result.getBesoinNet() > 0) {
            sb.append(String.format("Volume recommandé : %s cette semaine pour %.1f ha de %s.",
                    result.getVolumeFormate(), result.getSurfaceHectares(), culture.getNomCulture()));
        }

        return sb.toString();
    }

    private String buildDescriptionMeteo(double tmoy, double precip, double humidite) {
        String etat;
        if (precip > 20)      etat = "Pluvieux";
        else if (precip > 5)  etat = "Partiellement nuageux";
        else if (tmoy > 30)   etat = "Chaud et sec";
        else if (tmoy < 10)   etat = "Frais";
        else                  etat = "Ensoleillé";
        return String.format("%s — %.0f°C | %.0f mm/sem | Hum. %.0f%%", etat, tmoy, precip, humidite);
    }
}
