package tn.neuron.ardhi.models.UserAndDiag;

public class User {
    private int id;
    private String email;
    private Role role; // Changed from String roles to Role role
    private String password;
    private String nom;
    private String prenom;
    private int points = 0;
    private int level = 1;
    private boolean twoFactorEnabled = false;
    private String twoFactorCode;
    private java.time.LocalDateTime twoFactorExpiresAt;
    private String fingerprintSignature;
    private String faceSignature;
    private String resetPasswordCode;
    private java.time.LocalDateTime resetPasswordExpiresAt;
    private String phone;
    private String location;
    private double pointsFidelite = 0.0;


    // Constructeur Vide (Obligatoire)
    public User() {
    }

    // Constructeur sans ID (Pour l'insertion, car l'ID est Auto-Increment)
    public User(String email, Role role, String password, String nom, String prenom) {
        this.email = email;
        this.role = role;
        this.password = password;
        this.nom = nom;
        this.prenom = prenom;
    }

    // Constructeur Complet (Pour la récupération/Affichage)
    public User(int id, String email, Role role, String password, String nom, String prenom) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.password = password;
        this.nom = nom;
        this.prenom = prenom;
    }

    // AJOUTE LES GETTERS ET SETTERS ICI (Alt+Insert sous IntelliJ)
    // C'est indispensable pour respecter l'encapsulation.
    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public void setTwoFactorEnabled(boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public String getTwoFactorCode() {
        return twoFactorCode;
    }

    public void setTwoFactorCode(String twoFactorCode) {
        this.twoFactorCode = twoFactorCode;
    }

    public java.time.LocalDateTime getTwoFactorExpiresAt() {
        return twoFactorExpiresAt;
    }

    public void setTwoFactorExpiresAt(java.time.LocalDateTime twoFactorExpiresAt) {
        this.twoFactorExpiresAt = twoFactorExpiresAt;
    }

    public String getFingerprintSignature() {
        return fingerprintSignature;
    }

    public void setFingerprintSignature(String fingerprintSignature) {
        this.fingerprintSignature = fingerprintSignature;
    }

    public String getFaceSignature() {
        return faceSignature;
    }

    public void setFaceSignature(String faceSignature) {
        this.faceSignature = faceSignature;
    }

    public String getResetPasswordCode() {
        return resetPasswordCode;
    }

    public void setResetPasswordCode(String resetPasswordCode) {
        this.resetPasswordCode = resetPasswordCode;
    }

    public java.time.LocalDateTime getResetPasswordExpiresAt() {
        return resetPasswordExpiresAt;
    }

    public void setResetPasswordExpiresAt(java.time.LocalDateTime resetPasswordExpiresAt) {
        this.resetPasswordExpiresAt = resetPasswordExpiresAt;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
    public double getPointsFidelite() {
        return pointsFidelite;
    }

    public void setPointsFidelite(double pointsFidelite) {
        this.pointsFidelite = pointsFidelite;
    }
}