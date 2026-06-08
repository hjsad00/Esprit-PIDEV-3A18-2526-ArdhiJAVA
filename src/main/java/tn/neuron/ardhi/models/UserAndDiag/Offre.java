package tn.neuron.ardhi.models.UserAndDiag;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 * Représente une offre d'abonnement (ex: Pack Premium, Pack VIP).
 * Les offres sont configurables par l'admin et affichées dynamiquement aux
 * clients.
 */
public class Offre {
    private int id;
    private String nom;
    private String description;
    private float prixMensuel;
    private String avantages; // Stocké comme chaîne séparée par des pipes "|"
    private String couleurPrimaire;
    private String couleurSecondaire;
    private boolean estActive;
    private boolean estRecommandee;
    private Timestamp dateCreation;
    private int diagnosticsParHeure; // -1 = unlimited
    private boolean accesTraitement;
    private boolean accesPlanTraitement;

    // 1. Constructeur vide
    public Offre() {
    }

    // 2. Constructeur sans ID (pour l'ajout)
    public Offre(String nom, String description, float prixMensuel, String avantages,
            String couleurPrimaire, String couleurSecondaire, boolean estActive, boolean estRecommandee) {
        this.nom = nom;
        this.description = description;
        this.prixMensuel = prixMensuel;
        this.avantages = avantages;
        this.couleurPrimaire = couleurPrimaire;
        this.couleurSecondaire = couleurSecondaire;
        this.estActive = estActive;
        this.estRecommandee = estRecommandee;
        this.diagnosticsParHeure = 3; // Default
        this.accesTraitement = false; // Default
        this.accesPlanTraitement = false; // Default
    }

    // 3. Constructeur complet (pour récupération de la BDD)
    public Offre(int id, String nom, String description, float prixMensuel, String avantages,
            String couleurPrimaire, String couleurSecondaire, boolean estActive,
            boolean estRecommandee, Timestamp dateCreation, int diagnosticsParHeure, boolean accesTraitement,
            boolean accesPlanTraitement) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.prixMensuel = prixMensuel;
        this.avantages = avantages;
        this.couleurPrimaire = couleurPrimaire;
        this.couleurSecondaire = couleurSecondaire;
        this.estActive = estActive;
        this.estRecommandee = estRecommandee;
        this.dateCreation = dateCreation;
        this.diagnosticsParHeure = diagnosticsParHeure;
        this.accesTraitement = accesTraitement;
        this.accesPlanTraitement = accesPlanTraitement;
    }

    // Getters & Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public float getPrixMensuel() {
        return prixMensuel;
    }

    public void setPrixMensuel(float prixMensuel) {
        this.prixMensuel = prixMensuel;
    }

    public String getAvantages() {
        return avantages;
    }

    public void setAvantages(String avantages) {
        this.avantages = avantages;
    }

    public String getCouleurPrimaire() {
        return couleurPrimaire;
    }

    public void setCouleurPrimaire(String couleurPrimaire) {
        this.couleurPrimaire = couleurPrimaire;
    }

    public String getCouleurSecondaire() {
        return couleurSecondaire;
    }

    public void setCouleurSecondaire(String couleurSecondaire) {
        this.couleurSecondaire = couleurSecondaire;
    }

    public boolean isEstActive() {
        return estActive;
    }

    public void setEstActive(boolean estActive) {
        this.estActive = estActive;
    }

    public boolean isEstRecommandee() {
        return estRecommandee;
    }

    public void setEstRecommandee(boolean estRecommandee) {
        this.estRecommandee = estRecommandee;
    }

    public Timestamp getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Timestamp dateCreation) {
        this.dateCreation = dateCreation;
    }

    public int getDiagnosticsParHeure() {
        return diagnosticsParHeure;
    }

    public void setDiagnosticsParHeure(int diagnosticsParHeure) {
        this.diagnosticsParHeure = diagnosticsParHeure;
    }

    public boolean isAccesTraitement() {
        return accesTraitement;
    }

    public void setAccesTraitement(boolean accesTraitement) {
        this.accesTraitement = accesTraitement;
    }

    public boolean isAccesPlanTraitement() {
        return accesPlanTraitement;
    }

    public void setAccesPlanTraitement(boolean accesPlanTraitement) {
        this.accesPlanTraitement = accesPlanTraitement;
    }

    // --- Méthodes utilitaires ---

    /**
     * Retourne la liste des avantages sous forme de List<String>.
     * Les avantages sont stockés en BDD séparés par "|".
     */
    public List<String> getAvantagesAsList() {
        if (avantages == null || avantages.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(avantages.split("\\|"));
    }

    @Override
    public String toString() {
        return nom; // Pour affichage dans les ComboBox
    }
}
