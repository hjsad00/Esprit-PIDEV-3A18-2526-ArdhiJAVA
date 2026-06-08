package tn.neuron.ardhi.models.gestionemployemodel;

import java.time.LocalDateTime;

public class Notification {

    // Types de notifications
    public static final String TYPE_TACHE_RETARD      = "TACHE_RETARD";
    public static final String TYPE_EMPLOYE_INACTIF    = "EMPLOYE_INACTIF";
    public static final String TYPE_EMPLOYE_SURCHARGE  = "EMPLOYE_SURCHARGE";
    public static final String TYPE_CUSTOM             = "CUSTOM";
    public static final String TYPE_TACHE_BLOQUEE      = "tache_bloquee";
    // 🌦️ Types météo — négatifs (risques)
    public static final String TYPE_METEO_PLUIE        = "METEO_PLUIE";
    public static final String TYPE_METEO_CHALEUR      = "METEO_CHALEUR";
    public static final String TYPE_METEO_VENT         = "METEO_VENT";
    public static final String TYPE_METEO_INFO         = "METEO_INFO";
    // 🟢 Type météo — positif (recommandation favorable)
    public static final String TYPE_METEO_POSITIVE     = "METEO_POSITIVE";


    // Priorités
    public static final String PRIORITE_INFO = "INFO";
    public static final String PRIORITE_WARNING = "WARNING";
    public static final String PRIORITE_CRITICAL = "CRITICAL";

    private int id;
    private String type;
    private String priorite;
    private String titre;
    private String message;
    private Integer idAgriculteur;
    private Integer idTache;
    private Integer idEmploye;
    private boolean lue;
    private boolean archivee;
    private LocalDateTime dateCreation;
    private LocalDateTime dateLecture;

    // Constructeur vide
    public Notification() {
        this.lue = false;
        this.archivee = false;
        this.dateCreation = LocalDateTime.now();
    }

    // Constructeur complet
    public Notification(String type, String priorite, String titre, String message, Integer idAgriculteur) {
        this();
        this.type = type;
        this.priorite = priorite;
        this.titre = titre;
        this.message = message;
        this.idAgriculteur = idAgriculteur;
    }

    // Constructeur avec références
    public Notification(String type, String priorite, String titre, String message,
                        Integer idAgriculteur, Integer idTache, Integer idEmploye) {
        this(type, priorite, titre, message, idAgriculteur);
        this.idTache = idTache;
        this.idEmploye = idEmploye;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPriorite() { return priorite; }
    public void setPriorite(String priorite) { this.priorite = priorite; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Integer getIdAgriculteur() { return idAgriculteur; }
    public void setIdAgriculteur(Integer idAgriculteur) { this.idAgriculteur = idAgriculteur; }

    public Integer getIdTache() { return idTache; }
    public void setIdTache(Integer idTache) { this.idTache = idTache; }

    public Integer getIdEmploye() { return idEmploye; }
    public void setIdEmploye(Integer idEmploye) { this.idEmploye = idEmploye; }

    public boolean isLue() { return lue; }
    public void setLue(boolean lue) { this.lue = lue; }

    public boolean isArchivee() { return archivee; }
    public void setArchivee(boolean archivee) { this.archivee = archivee; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateLecture() { return dateLecture; }
    public void setDateLecture(LocalDateTime dateLecture) { this.dateLecture = dateLecture; }

    public String getIcone() {
        switch (type) {
            case TYPE_TACHE_RETARD:      return "⏰";
            case TYPE_TACHE_BLOQUEE:     return "⚠️";
            case TYPE_EMPLOYE_INACTIF:   return "💤";
            case TYPE_EMPLOYE_SURCHARGE: return "🔥";
            case TYPE_METEO_PLUIE:       return "🌧️";
            case TYPE_METEO_CHALEUR:     return "🌡️";
            case TYPE_METEO_VENT:        return "💨";
            case TYPE_METEO_INFO:        return "🌤️";
            case TYPE_METEO_POSITIVE:    return "✅";
            default:                     return "🔔";
        }
    }

    public String getCouleurPriorite() {
        switch (type) {
            case TYPE_TACHE_RETARD:      return "#e74c3c"; // 🔴
            case TYPE_TACHE_BLOQUEE:     return "#f39c12"; // 🟡
            case TYPE_METEO_PLUIE:       return "#2980b9"; // 🔵
            case TYPE_METEO_CHALEUR:     return "#e67e22"; // 🟠
            case TYPE_METEO_VENT:        return "#8e44ad"; // 🟣
            case TYPE_METEO_INFO:        return "#27ae60"; // 🌿
            case TYPE_METEO_POSITIVE:    return "#2ecc71"; // 🟢
            default:
                switch (priorite != null ? priorite : "") {
                    case PRIORITE_CRITICAL: return "#e74c3c";
                    case PRIORITE_WARNING:  return "#f39c12";
                    case PRIORITE_INFO:     return "#3498db";
                    default:               return "#3498db";
                }
        }
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", priorite='" + priorite + '\'' +
                ", titre='" + titre + '\'' +
                ", lue=" + lue +
                ", dateCreation=" + dateCreation +
                '}';
    }
}