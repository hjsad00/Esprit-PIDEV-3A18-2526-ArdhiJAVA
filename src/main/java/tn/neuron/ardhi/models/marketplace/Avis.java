package tn.neuron.ardhi.models.marketplace;

import java.sql.Timestamp;

public class Avis {
    private int idAvis;
    private int idUser;
    private int idProduit;
    private int note;
    private String commentaire;
    private Timestamp dateAvis;
    private String nomUser; // Pour l'affichage

    public Avis() {}

    public Avis(int idAvis, int idUser, int idProduit, int note, String commentaire, Timestamp dateAvis) {
        this.idAvis = idAvis;
        this.idUser = idUser;
        this.idProduit = idProduit;
        this.note = note;
        this.commentaire = commentaire;
        this.dateAvis = dateAvis;
    }

    public Avis(int idUser, int idProduit, int note, String commentaire) {
        this.idUser = idUser;
        this.idProduit = idProduit;
        this.note = note;
        this.commentaire = commentaire;
    }

    public int getIdAvis() { return idAvis; }
    public void setIdAvis(int idAvis) { this.idAvis = idAvis; }

    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }

    public int getIdProduit() { return idProduit; }
    public void setIdProduit(int idProduit) { this.idProduit = idProduit; }

    public int getNote() { return note; }
    public void setNote(int note) { this.note = note; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public Timestamp getDateAvis() { return dateAvis; }
    public void setDateAvis(Timestamp dateAvis) { this.dateAvis = dateAvis; }

    public String getNomUser() { return nomUser; }
    public void setNomUser(String nomUser) { this.nomUser = nomUser; }
}
