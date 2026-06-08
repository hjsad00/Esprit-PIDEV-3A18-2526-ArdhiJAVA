package tn.neuron.ardhi.services.Evenement;

import tn.neuron.ardhi.models.Evenement.Evenement;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * Service pour partager les événements (Version simplifiée - Sans serveur web)
 */
public class PartageService {

    /**
     * Génère un message d'information sur l'événement (pas un lien web)
     */
    public String genererLienEvenement(Evenement event) {
        return "Événement #" + event.getId() + " - " + event.getTitre() +
                " (" + event.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")";
    }

    // ==================== COPIER INFO ====================

    public boolean copierInfosEvenement(Evenement event) {
        try {
            String infos = genererMessageComplet(event);
            StringSelection selection = new StringSelection(infos);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);

            System.out.println("✓ Informations copiées dans le presse-papier");
            return true;

        } catch (Exception e) {
            System.err.println("✗ Erreur copie: " + e.getMessage());
            return false;
        }
    }

    /**
     * Ouvre le client email (Version corrigée - essaie Desktop.mail())
     */
    public boolean partagerParEmail(Evenement event, String emailDestinataire) {
        try {
            // ═══ MÉTHODE 1: Desktop.mail() (préféré) ═══
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();

                if (desktop.isSupported(Desktop.Action.MAIL)) {
                    // Utiliser Desktop.mail() au lieu de browse()
                    desktop.mail();
                    System.out.println("✓ Client email ouvert avec Desktop.mail()");

                    // Copier les infos dans le presse-papier
                    copierInfosEvenement(event);
                    System.out.println("✓ Infos copiées - Collez-les (Ctrl+V) dans l'email");

                    return true;
                }
            }

            // ═══ MÉTHODE 2: Runtime.exec (fallback) ═══
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                // Windows: Ouvrir Outlook ou Mail
                try {
                    Runtime.getRuntime().exec("cmd /c start outlook");
                    copierInfosEvenement(event);
                    System.out.println("✓ Outlook ouvert - Infos copiées");
                    return true;
                } catch (Exception e) {
                    System.err.println("Outlook non trouvé, essai Mail...");
                }
            } else if (os.contains("mac")) {
                // Mac: Ouvrir Mail.app
                Runtime.getRuntime().exec("open -a Mail");
                copierInfosEvenement(event);
                System.out.println("✓ Mail ouvert - Infos copiées");
                return true;
            } else if (os.contains("nix") || os.contains("nux")) {
                // Linux: Thunderbird ou Evolution
                try {
                    Runtime.getRuntime().exec("thunderbird");
                    copierInfosEvenement(event);
                    System.out.println("✓ Thunderbird ouvert - Infos copiées");
                    return true;
                } catch (Exception e) {
                    System.err.println("Thunderbird non trouvé");
                }
            }

            // ═══ MÉTHODE 3: Copier seulement (dernière option) ═══
            copierInfosEvenement(event);
            System.out.println("⚠ Client email non trouvé - Infos copiées dans le presse-papier");
            System.out.println("   Ouvrez votre client email manuellement et collez (Ctrl+V)");

            return true;  // Retourner true car les infos sont copiées

        } catch (Exception e) {
            System.err.println("✗ Erreur: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Construit le corps de l'email de partage
     */
    private String construireEmailPartage(Evenement event) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        return String.format(
                "Bonjour,\n\n" +
                        "Je souhaite partager avec vous cet événement agricole :\n\n" +
                        "═══════════════════════════════════\n" +
                        "🎪 %s\n" +
                        "═══════════════════════════════════\n\n" +
                        "📅 Date: %s\n" +
                        "📍 Lieu: %s\n" +
                        "🏷️ Type: %s\n" +
                        "👤 Organisateur: %s\n" +
                        "👥 Places disponibles: %d / %d\n\n" +
                        "📝 Description:\n%s\n\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "Pour vous inscrire, veuillez contacter l'organisateur\n" +
                        "ou consulter l'application ARDHI.\n\n" +
                        "🌾 Plateforme ARDHI - Événements Agricoles\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                event.getTitre(),
                event.getDateDebut().format(formatter) +
                        (event.getDateDebut().equals(event.getDateFin()) ? "" : " au " + event.getDateFin().format(formatter)),
                event.getLieu(),
                getTypeLabel(event.getType()),
                event.getOrganisateur(),
                event.getNombrePlacesMax() - event.getNombreParticipants(),
                event.getNombrePlacesMax(),
                event.getDescription() != null && !event.getDescription().isEmpty() ?
                        event.getDescription() : "Consultez l'application pour plus de détails"
        );
    }

    // ==================== GÉNÉRATION CONTENU ====================

    /**
     * Génère un message de partage complet avec tous les détails
     */
    public String genererMessageComplet(Evenement event) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", java.util.Locale.FRENCH);

        StringBuilder message = new StringBuilder();
        message.append("═══════════════════════════════════\n");
        message.append("🎪 ").append(event.getTitre().toUpperCase()).append("\n");
        message.append("═══════════════════════════════════\n\n");

        message.append("📅 Date: ").append(event.getDateDebut().format(formatter)).append("\n");
        if (!event.getDateDebut().equals(event.getDateFin())) {
            message.append("      au ").append(event.getDateFin().format(formatter)).append("\n");
        }

        message.append("📍 Lieu: ").append(event.getLieu()).append("\n");
        message.append("🏷️ Type: ").append(getTypeLabel(event.getType())).append("\n");
        message.append("👤 Organisateur: ").append(event.getOrganisateur()).append("\n");
        message.append("👥 Places: ").append(event.getNombreParticipants())
                .append(" / ").append(event.getNombrePlacesMax()).append("\n\n");

        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            message.append("📝 Description:\n");
            message.append(event.getDescription()).append("\n\n");
        }

        message.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        message.append("🌾 ARDHI - Plateforme Agricole Intelligente\n");
        message.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        return message.toString();
    }

    // ==================== UTILITAIRES ====================

    private String getTypeLabel(String type) {
        return switch (type) {
            case "FOIRE" -> "Foire Agricole";
            case "FORMATION" -> "Formation";
            case "CONFERENCE" -> "Conférence";
            case "ATELIER" -> "Atelier Pratique";
            default -> type;
        };
    }

    /**
     * Vérifie si le système supporte le partage
     */
    public boolean isPartageSupporte() {
        return Desktop.isDesktopSupported() &&
                Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
    }
}