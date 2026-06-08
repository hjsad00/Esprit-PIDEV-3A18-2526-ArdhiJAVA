package tn.neuron.ardhi.models.gestionemployemodel;

import java.time.LocalDateTime;

/**
 * Modèle représentant une évaluation de performance d'un employé
 */
public class EvaluationPerformance {

    private int idEvaluation;
    private int idEmploye;
    private Integer idTache; // Peut être null si évaluation générale
    private int noteQualite;      // 1-5
    private int noteRapidite;     // 1-5
    private int noteAutonomie;    // 1-5
    private int noteCommunication; // 1-5
    private double noteGlobale;    // Calculée automatiquement
    private String commentaire;
    private String evaluateur;
    private LocalDateTime dateEvaluation;

    // Constructeurs
    public EvaluationPerformance() {}

    public EvaluationPerformance(int idEmploye, int noteQualite, int noteRapidite,
                                 int noteAutonomie, int noteCommunication) {
        this.idEmploye = idEmploye;
        this.noteQualite = noteQualite;
        this.noteRapidite = noteRapidite;
        this.noteAutonomie = noteAutonomie;
        this.noteCommunication = noteCommunication;
        this.noteGlobale = calculerNoteGlobale();
    }

    // Getters et Setters
    public int getIdEvaluation() {
        return idEvaluation;
    }

    public void setIdEvaluation(int idEvaluation) {
        this.idEvaluation = idEvaluation;
    }

    public int getIdEmploye() {
        return idEmploye;
    }

    public void setIdEmploye(int idEmploye) {
        this.idEmploye = idEmploye;
    }

    public Integer getIdTache() {
        return idTache;
    }

    public void setIdTache(Integer idTache) {
        this.idTache = idTache;
    }

    public int getNoteQualite() {
        return noteQualite;
    }

    public void setNoteQualite(int noteQualite) {
        this.noteQualite = noteQualite;
        this.noteGlobale = calculerNoteGlobale();
    }

    public int getNoteRapidite() {
        return noteRapidite;
    }

    public void setNoteRapidite(int noteRapidite) {
        this.noteRapidite = noteRapidite;
        this.noteGlobale = calculerNoteGlobale();
    }

    public int getNoteAutonomie() {
        return noteAutonomie;
    }

    public void setNoteAutonomie(int noteAutonomie) {
        this.noteAutonomie = noteAutonomie;
        this.noteGlobale = calculerNoteGlobale();
    }

    public int getNoteCommunication() {
        return noteCommunication;
    }

    public void setNoteCommunication(int noteCommunication) {
        this.noteCommunication = noteCommunication;
        this.noteGlobale = calculerNoteGlobale();
    }

    public double getNoteGlobale() {
        return noteGlobale;
    }

    public void setNoteGlobale(double noteGlobale) {
        this.noteGlobale = noteGlobale;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public String getEvaluateur() {
        return evaluateur;
    }

    public void setEvaluateur(String evaluateur) {
        this.evaluateur = evaluateur;
    }

    public LocalDateTime getDateEvaluation() {
        return dateEvaluation;
    }

    public void setDateEvaluation(LocalDateTime dateEvaluation) {
        this.dateEvaluation = dateEvaluation;
    }

    // Méthodes utilitaires
    private double calculerNoteGlobale() {
        return (noteQualite + noteRapidite + noteAutonomie + noteCommunication) / 4.0;
    }

    public String getAppreciation() {
        if (noteGlobale >= 4.5) return "Excellent";
        if (noteGlobale >= 4.0) return "Très bien";
        if (noteGlobale >= 3.5) return "Bien";
        if (noteGlobale >= 3.0) return "Satisfaisant";
        if (noteGlobale >= 2.5) return "Moyen";
        return "Insuffisant";
    }

    public String getEmoji() {
        if (noteGlobale >= 4.5) return "🌟";
        if (noteGlobale >= 4.0) return "⭐";
        if (noteGlobale >= 3.5) return "✨";
        if (noteGlobale >= 3.0) return "👍";
        if (noteGlobale >= 2.5) return "🤔";
        return "⚠️";
    }

    @Override
    public String toString() {
        return String.format("%s Note: %.2f/5 (%s)",
                getEmoji(), noteGlobale, getAppreciation());
    }
}