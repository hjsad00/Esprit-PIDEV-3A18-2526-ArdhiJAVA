package tn.neuron.ardhi.models.UserAndDiag;

/**
 * Représente le type de vote pour les likes/dislikes sur les posts et
 * commentaires.
 * Correspond à l'ENUM MySQL: ENUM('LIKE', 'DISLIKE')
 */
public enum VoteType {
    LIKE,
    DISLIKE;

    /**
     * Convertit une chaîne de la base de données en VoteType.
     * 
     * @param value La valeur de la DB ('LIKE' ou 'DISLIKE')
     * @return Le VoteType correspondant, ou null si invalide
     */
    public static VoteType fromString(String value) {
        if (value == null)
            return null;
        try {
            return VoteType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
