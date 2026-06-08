package tn.neuron.ardhi.models.Evenement;

import java.time.LocalDateTime;


public class Participation {
    private int id;
    private int idEvenement;
    private int idUtilisateur;
    private String nomUtilisateur;
    private String prenomUtilisateur;
    private String emailUtilisateur;
    private LocalDateTime dateInscription;
    private String statut; // "CONFIRME", "EN_ATTENTE", "ANNULE", "PRESENT"
    private String commentaire;
    private int note;
    private String avis;
    private int nombrePersonnes; // Nombre de personnes pour cette participation
    private String titreEvenement; // Titre de l'événement (utilisé pour l'affichage)
    private boolean attestationEnvoyee;

    // Constructeur vide
    public Participation() {
        this.dateInscription = LocalDateTime.now();
        this.statut = "CONFIRME";
        this.note = 0;
        this.nombrePersonnes = 1; // Par défaut 1 personne
    }

    // Constructeur complet
    public Participation(int id, int idEvenement, int idUtilisateur,
                         String nomUtilisateur, String prenomUtilisateur,
                         String emailUtilisateur, LocalDateTime dateInscription,
                         String statut, String commentaire, int note, String avis) {
        this.id = id;
        this.idEvenement = idEvenement;
        this.idUtilisateur = idUtilisateur;
        this.nomUtilisateur = nomUtilisateur;
        this.prenomUtilisateur = prenomUtilisateur;
        this.emailUtilisateur = emailUtilisateur;
        this.dateInscription = dateInscription;
        this.statut = statut;
        this.commentaire = commentaire;
        this.note = note;
        this.avis = avis;
    }

    // Constructeur pour inscription (sans ID)
    public Participation(int idEvenement, int idUtilisateur, String commentaire) {
        this.idEvenement = idEvenement;
        this.idUtilisateur = idUtilisateur;
        this.commentaire = commentaire;
        this.dateInscription = LocalDateTime.now();
        this.statut = "CONFIRME";
        this.note = 0;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdEvenement() {
        return idEvenement;
    }

    public void setIdEvenement(int idEvenement) {
        this.idEvenement = idEvenement;
    }

    public int getIdUtilisateur() {
        return idUtilisateur;
    }

    public void setIdUtilisateur(int idUtilisateur) {
        this.idUtilisateur = idUtilisateur;
    }

    public String getNomUtilisateur() {
        return nomUtilisateur;
    }

    public void setNomUtilisateur(String nomUtilisateur) {
        this.nomUtilisateur = nomUtilisateur;
    }

    public String getPrenomUtilisateur() {
        return prenomUtilisateur;
    }

    public void setPrenomUtilisateur(String prenomUtilisateur) {
        this.prenomUtilisateur = prenomUtilisateur;
    }

    public String getEmailUtilisateur() {
        return emailUtilisateur;
    }

    public void setEmailUtilisateur(String emailUtilisateur) {
        this.emailUtilisateur = emailUtilisateur;
    }

    public LocalDateTime getDateInscription() {
        return dateInscription;
    }

    public void setDateInscription(LocalDateTime dateInscription) {
        this.dateInscription = dateInscription;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public int getNote() {
        return note;
    }

    public void setNote(int note) {
        if (note >= 0 && note <= 5) {
            this.note = note;
        }
    }

    public String getAvis() {
        return avis;
    }

    public void setAvis(String avis) {
        this.avis = avis;
    }

    public int getNombrePersonnes() {
        return nombrePersonnes;
    }

    public void setNombrePersonnes(int nombrePersonnes) {
        this.nombrePersonnes = nombrePersonnes;
    }

    // Méthodes utilitaires
    public String getNomComplet() {
        return prenomUtilisateur + " " + nomUtilisateur;
    }

    public boolean isConfirme() {
        return "CONFIRME".equals(statut);
    }

    public boolean isAnnule() {
        return "ANNULE".equals(statut);
    }

    public String getTitreEvenement() {
        return titreEvenement;
    }

    public void setTitreEvenement(String titreEvenement) {
        this.titreEvenement = titreEvenement;
    }

    public boolean isAttestationEnvoyee() {
        return attestationEnvoyee;
    }
    public void setAttestationEnvoyee(boolean attestationEnvoyee) {
        this.attestationEnvoyee = attestationEnvoyee;
    }

    @Override
    public String toString() {
        return "Participation{" +
                "id=" + id +
                ", idEvenement=" + idEvenement +
                ", idUtilisateur=" + idUtilisateur +
                ", nomComplet='" + getNomComplet() + '\'' +
                ", statut='" + statut + '\'' +
                ", dateInscription=" + dateInscription +
                '}';
    }
}