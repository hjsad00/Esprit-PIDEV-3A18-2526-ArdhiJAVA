package tn.neuron.ardhi.utils.UserAndDiag;

import tn.neuron.ardhi.models.UserAndDiag.User;

public class UserSession {

    // L'instance unique de la session
    private static UserSession instance;

    // L'utilisateur actuellement connecté
    private User user;
    private String token; // JWT Token

    // Constructeur privé
    private UserSession() {
    }

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void login(User user, String token) {
        this.user = user;
        this.token = token;
    }

    public void logout() {
        this.user = null;
        this.token = null;
        instance = null; // Also clear the instance on logout
    }

    // Méthode pour récupérer l'utilisateur connecté
    public User getUser() {
        return user;
    }

    // Méthode pour récupérer le token JWT
    public String getToken() {
        return token;
    }

    // Méthode pour se déconnecter (Logout)
    public void cleanUserSession() {
        user = null;
        instance = null;
    }

    @Override
    public String toString() {
        return "UserSession{" + "user=" + user + '}';
    }
}