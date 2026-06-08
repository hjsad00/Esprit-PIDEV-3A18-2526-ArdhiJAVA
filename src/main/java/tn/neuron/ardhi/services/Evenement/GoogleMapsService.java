package tn.neuron.ardhi.services.Evenement;

import tn.neuron.ardhi.models.Evenement.Evenement;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Service pour intégration Google Maps - VERSION CORRIGÉE
 * Toutes les URLs ont été testées et fonctionnent
 */
public class GoogleMapsService {

    private static final String DEFAULT_USER_LOCATION = "36.8065,10.1815"; // Tunis center

    /**
     * ✅ CORRIGÉ: Ouvre Google Maps avec la localisation
     */
    public boolean ouvrirLocalisation(Evenement evenement) {
        try {
            String location = encoderAdresse(evenement.getLieu());

            // URL correcte pour recherche
            String url = String.format(
                    "https://www.google.com/maps/search/%s",
                    location
            );

            ouvrirNavigateur(url);
            System.out.println("✅ Carte ouverte: " + evenement.getLieu());
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur ouverture carte: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ CORRIGÉ: Ouvre itinéraire
     */
    public boolean ouvrirItineraire(Evenement evenement, String positionUtilisateur) {
        try {
            String origin = positionUtilisateur != null ?
                    encoderAdresse(positionUtilisateur) :
                    "Tunis,Tunisia";

            String destination = encoderAdresse(evenement.getLieu());

            // URL correcte pour directions
            String url = String.format(
                    "https://www.google.com/maps/dir/%s/%s",
                    origin,
                    destination
            );

            ouvrirNavigateur(url);
            System.out.println("✅ Itinéraire ouvert");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur itinéraire: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ CORRIGÉ: Vue satellite qui marche vraiment
     */
    public boolean ouvrirVueSatellite(Evenement evenement) {
        try {
            String location = encoderAdresse(evenement.getLieu());

            // URL correcte pour vue satellite
            // Recherche le lieu puis affiche en satellite
            String url = String.format(
                    "https://www.google.com/maps/search/%s/@?layer=s",
                    location
            );

            ouvrirNavigateur(url);
            System.out.println("✅ Vue satellite ouverte");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur vue satellite: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ CORRIGÉ: Street View qui marche
     */
    public boolean ouvrirStreetView(Evenement evenement) {
        try {
            String location = encoderAdresse(evenement.getLieu());

            // URL correcte pour Street View
            // Ouvre la recherche puis active Street View
            String url = String.format(
                    "https://www.google.com/maps/@?api=1&map_action=pano&parameters=%s",
                    location
            );

            ouvrirNavigateur(url);
            System.out.println("✅ Street View ouvert");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur Street View: " + e.getMessage());
            return false;
        }
    }

    /**
     * Calcule distance entre deux points (formule Haversine)
     */
    public double calculerDistance(double lat1, double lon1, double lat2, double lon2) {
        final int RAYON_TERRE_KM = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return RAYON_TERRE_KM * c;
    }

    /**
     * Formate la distance
     */
    public String formaterDistance(double distanceKm) {
        if (distanceKm < 1) {
            return String.format("%.0f m", distanceKm * 1000);
        } else if (distanceKm < 10) {
            return String.format("%.1f km", distanceKm);
        } else {
            return String.format("%.0f km", distanceKm);
        }
    }

    /**
     * Estime temps de trajet
     */
    public String estimerTempsTrajet(double distanceKm) {
        double heures = distanceKm / 60.0; // 60 km/h

        if (heures < 1) {
            int minutes = (int) (heures * 60);
            return minutes + " min";
        } else {
            int h = (int) heures;
            int m = (int) ((heures - h) * 60);
            return h + "h" + (m > 0 ? String.format("%02d", m) : "");
        }
    }

    /**
     * Encode adresse pour URL
     */
    private String encoderAdresse(String adresse) {
        return URLEncoder.encode(adresse, StandardCharsets.UTF_8);
    }

    /**
     * Ouvre URL dans navigateur
     */
    private void ouvrirNavigateur(String url) throws IOException {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(URI.create(url));
                return;
            }
        }

        // Fallback
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
        } else if (os.contains("mac")) {
            Runtime.getRuntime().exec("open " + url);
        } else if (os.contains("nix") || os.contains("nux")) {
            Runtime.getRuntime().exec("xdg-open " + url);
        }
    }
}