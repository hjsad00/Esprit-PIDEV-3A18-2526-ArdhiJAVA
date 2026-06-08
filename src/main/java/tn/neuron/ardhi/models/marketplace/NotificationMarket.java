package tn.neuron.ardhi.models.marketplace;

import java.time.LocalDateTime;

public class NotificationMarket {

    // ── Types ────────────────────────────────────────────────────────────────
    public static final String TYPE_ACHAT = "ACHAT";
    public static final String TYPE_AVIS  = "AVIS";

    // ── Champs ───────────────────────────────────────────────────────────────
    private int           idNotif;
    private int           idUser;       // le vendeur destinataire
    private String        type;         // TYPE_ACHAT | TYPE_AVIS
    private String        titre;
    private String        message;
    private Integer       idProduit;
    private Integer       idCommande;
    private boolean       lue;
    private LocalDateTime dateCreation;

    // ── Constructeur vide ────────────────────────────────────────────────────
    public NotificationMarket() {
        this.lue          = false;
        this.dateCreation = LocalDateTime.now();
    }

    // ── Constructeur ACHAT ───────────────────────────────────────────────────
    public NotificationMarket(int idUser, String type, String titre, String message,
                              Integer idProduit, Integer idCommande) {
        this();
        this.idUser     = idUser;
        this.type       = type;
        this.titre      = titre;
        this.message    = message;
        this.idProduit  = idProduit;
        this.idCommande = idCommande;
    }

    // ── Constructeur AVIS (pas de commande) ──────────────────────────────────
    public NotificationMarket(int idUser, String type, String titre, String message,
                              Integer idProduit) {
        this(idUser, type, titre, message, idProduit, null);
    }

    // ── Icône selon le type ──────────────────────────────────────────────────
    public String getIcone() {
        switch (type != null ? type : "") {
            case TYPE_ACHAT: return "🛒";
            case TYPE_AVIS:  return "⭐";
            default:         return "🔔";
        }
    }

    // ── Couleur selon le type ────────────────────────────────────────────────
    public String getCouleur() {
        switch (type != null ? type : "") {
            case TYPE_ACHAT: return "#2d7a4f"; // vert
            case TYPE_AVIS:  return "#f39c12"; // orange
            default:         return "#3498db"; // bleu
        }
    }

    // ── Getters / Setters ────────────────────────────────────────────────────
    public int getIdNotif()                        { return idNotif; }
    public void setIdNotif(int idNotif)            { this.idNotif = idNotif; }

    public int getIdUser()                         { return idUser; }
    public void setIdUser(int idUser)              { this.idUser = idUser; }

    public String getType()                        { return type; }
    public void setType(String type)               { this.type = type; }

    public String getTitre()                       { return titre; }
    public void setTitre(String titre)             { this.titre = titre; }

    public String getMessage()                     { return message; }
    public void setMessage(String message)         { this.message = message; }

    public Integer getIdProduit()                  { return idProduit; }
    public void setIdProduit(Integer idProduit)    { this.idProduit = idProduit; }

    public Integer getIdCommande()                 { return idCommande; }
    public void setIdCommande(Integer idCommande)  { this.idCommande = idCommande; }

    public boolean isLue()                         { return lue; }
    public void setLue(boolean lue)                { this.lue = lue; }

    public LocalDateTime getDateCreation()                       { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation)      { this.dateCreation = dateCreation; }

    @Override
    public String toString() {
        return "NotificationMarket{" +
                "idNotif=" + idNotif +
                ", idUser=" + idUser +
                ", type='" + type + '\'' +
                ", titre='" + titre + '\'' +
                ", lue=" + lue +
                ", dateCreation=" + dateCreation +
                '}';
    }
}