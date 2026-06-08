package tn.neuron.ardhi.utils.UserAndDiag;

public class ValidationUtils {

    // Constructeur privé pour empêcher l'instanciation
    private ValidationUtils() {
    }

    public static boolean validerEmail(String email) {
        if (email == null)
            return false;
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }

    public static boolean validerNom(String nom) {
        if (nom == null)
            return false;
        return nom.matches("^[a-zA-Z\\s-]+$");
    }

    public static boolean validerMotDePasse(String mdp) {
        if (mdp == null)
            return false;
        return mdp.length() >= 6;
    }

    public static boolean champsVides(String... champs) {
        for (String champ : champs) {
            if (champ == null || champ.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vérifie si une chaîne est une couleur hexadécimale valide.
     * Accepte les formats: #RGB, #RRGGBB
     */
    public static boolean validerCouleurHex(String couleur) {
        if (couleur == null || couleur.isEmpty()) {
            return false;
        }
        return couleur.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");
    }

    public static boolean validerCarteBancaire(String carte) {
        if (carte == null)
            return false;
        return carte.matches("\\d{16}");
    }

    public static boolean validerCVC(String cvc) {
        if (cvc == null)
            return false;
        return cvc.matches("\\d{3}");
    }
}