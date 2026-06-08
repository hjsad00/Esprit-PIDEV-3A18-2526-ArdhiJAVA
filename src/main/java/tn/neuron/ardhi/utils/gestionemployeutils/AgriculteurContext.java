package tn.neuron.ardhi.utils.gestionemployeutils;

import tn.neuron.ardhi.utils.UserAndDiag.UserSession;

/**
 * Classe utilitaire pour gérer le contexte d'un agriculteur
 * Utilisée par l'ADMIN pour superviser un agriculteur spécifique
 *
 * Fonctionne comme UserSession mais pour stocker temporairement
 * l'ID de l'agriculteur sélectionné par l'admin
 *
 * @author Ardhi Team
 */
public class AgriculteurContext {

    // Instance unique (Singleton)
    private static AgriculteurContext instance;

    // ID de l'agriculteur actuellement supervisé par l'admin
    private Integer idAgriculteur;

    // Nom complet pour affichage
    private String nomCompletAgriculteur;

    // Constructeur privé
    private AgriculteurContext() {
        this.idAgriculteur = null;
        this.nomCompletAgriculteur = null;
    }

    /**
     * Récupérer l'instance unique
     */
    public static AgriculteurContext getInstance() {
        if (instance == null) {
            instance = new AgriculteurContext();
        }
        return instance;
    }

    /**
     * Définir l'agriculteur supervisé (appelé par l'admin)
     *
     * @param idAgriculteur ID de l'agriculteur
     * @param nomComplet    Nom complet pour affichage
     */
    public void setAgriculteur(Integer idAgriculteur, String nomComplet) {
        this.idAgriculteur = idAgriculteur;
        this.nomCompletAgriculteur = nomComplet;
        System.out.println("🔧 AgriculteurContext: Supervision de " + nomComplet + " (ID: " + idAgriculteur + ")");
    }

    /**
     * Obtenir l'ID de l'agriculteur supervisé
     *
     * @return ID agriculteur ou null si pas en mode supervision
     */
    public Integer getIdAgriculteur() {
        return idAgriculteur;
    }

    /**
     * Obtenir le nom complet de l'agriculteur supervisé
     *
     * @return Nom complet ou null
     */
    public String getNomCompletAgriculteur() {
        return nomCompletAgriculteur;
    }

    /**
     * Vérifier si on est en mode supervision admin
     *
     * @return true si admin supervise un agriculteur
     */
    public boolean isSupervisionMode() {
        return idAgriculteur != null;
    }

    /**
     * Nettoyer le contexte (sortir du mode supervision)
     */
    public void clear() {
        System.out.println("🔧 AgriculteurContext: Fin de supervision");
        this.idAgriculteur = null;
        this.nomCompletAgriculteur = null;
    }

    /**
     * Obtenir l'ID de l'agriculteur à utiliser dans les requêtes
     *
     * Si en mode supervision admin → retourne idAgriculteur supervisé
     * Sinon → retourne l'ID de l'utilisateur connecté
     *
     * @return ID de l'agriculteur pour filtrage
     */
    public static Integer getActiveAgriculteurId() {
        // Si mode supervision admin activé
        if (getInstance().isSupervisionMode()) {
            return getInstance().getIdAgriculteur();
        }

        // Sinon, utiliser l'utilisateur connecté (agriculteur normal)
        if (UserSession.getInstance() != null && UserSession.getInstance().getUser() != null) {
            return UserSession.getInstance().getUser().getId();
        }

        return null;
    }

    // NO::
    @Override
    public String toString() {
        return "AgriculteurContext{" +
                "idAgriculteur=" + idAgriculteur +
                ", nomComplet='" + nomCompletAgriculteur + '\'' +
                ", supervisionMode=" + isSupervisionMode() +
                '}';
    }
    // jkl
}