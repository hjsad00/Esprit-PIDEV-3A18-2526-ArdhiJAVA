package tn.neuron.ardhi.models.gestionemployemodel;

import java.util.Objects;

public class Employe {

    private int     id;
    private String  nom;
    private String  prenom;
    private String  email;
    private String  poste;
    private String  telephone;
    private boolean actif;
    private Integer idAgriculteur;
    private String  photoPath; // ← chemin vers la photo de profil

    // ── Constructeurs ─────────────────────────────────────────────────────

    public Employe() { this.actif = true; }

    public Employe(int id, String nom, String prenom, String email, String poste,
                   String telephone, boolean actif, Integer idAgriculteur) {
        this.id = id; this.nom = nom; this.prenom = prenom;
        this.email = email; this.poste = poste; this.telephone = telephone;
        this.actif = actif; this.idAgriculteur = idAgriculteur;
    }

    public Employe(String nom, String prenom, String email, String poste,
                   String telephone, boolean actif, Integer idAgriculteur) {
        this.nom = nom; this.prenom = prenom; this.email = email;
        this.poste = poste; this.telephone = telephone;
        this.actif = actif; this.idAgriculteur = idAgriculteur;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────

    public int     getId()                     { return id; }
    public void    setId(int id)               { this.id = id; }
    public String  getNom()                    { return nom; }
    public void    setNom(String nom)          { this.nom = nom; }
    public String  getPrenom()                 { return prenom; }
    public void    setPrenom(String prenom)    { this.prenom = prenom; }
    public String  getEmail()                  { return email; }
    public void    setEmail(String email)      { this.email = email; }
    public String  getPoste()                  { return poste; }
    public void    setPoste(String poste)      { this.poste = poste; }
    public String  getTelephone()              { return telephone; }
    public void    setTelephone(String t)      { this.telephone = t; }
    public boolean isActif()                   { return actif; }
    public void    setActif(boolean actif)     { this.actif = actif; }
    public Integer getIdAgriculteur()          { return idAgriculteur; }
    public void    setIdAgriculteur(Integer i) { this.idAgriculteur = i; }

    // ── Photo profil ──────────────────────────────────────────────────────

    public String  getPhotoPath()             { return photoPath; }
    public void    setPhotoPath(String p)     { this.photoPath = p; }
    /** Retourne true si une photo de profil est definie */
    public boolean hasPhoto() {
        return photoPath != null && !photoPath.isBlank();
    }

    // ── Utilitaire ────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Employe{id=" + id + ", nom='" + nom + "', prenom='" + prenom +
                "', actif=" + actif + ", photo=" + (hasPhoto() ? "oui" : "non") + "}";
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employe)) return false;
        return id == ((Employe) o).id;
    }

    @Override public int hashCode() { return Objects.hash(id); }
}