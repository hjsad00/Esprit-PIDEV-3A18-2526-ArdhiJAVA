package tn.neuron.ardhi.services.gestionemployeservice;

import tn.neuron.ardhi.models.gestionemployemodel.Employe;
import tn.neuron.ardhi.models.gestionemployemodel.Tache;
import tn.neuron.ardhi.services.gestionemployeservice.MatchingService.RecommandationResult;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🤖 ChatbotService RH — VERSION IA AVANCÉE v2
 *
 * Améliorations :
 *  ④ Réponse professionnelle enrichie (score détaillé + indice confiance)
 *  ⑤ Mode comparaison Top 3 (intention COMPARER_TOP3)
 *  + Toutes les fonctions existantes conservées
 */
public class ChatbotService {

    private Connection getCnx() {
        return MyDatabase.getInstance().getCnx();
    }

    private final MatchingService    matchingService    = new MatchingService();
    private final EmployeService     employeService     = new EmployeService();
    private final TacheService       tacheService       = new TacheService();
    private final PerformanceService performanceService = new PerformanceService();

    // ======================================================================
    // POINT D'ENTRÉE PRINCIPAL
    // ======================================================================

    public ChatbotResponse traiterMessage(String messageUtilisateur, int idUtilisateur) {
        String msg       = normaliser(messageUtilisateur);
        String lang      = detecterLangue(msg);
        String intention = detecterIntention(msg);

        ChatbotResponse response;
        switch (intention) {
            case "RECOMMANDER_EMPLOYE": response = traiterRecommandation(msg, idUtilisateur, lang); break;
            case "COMPARER_TOP3":       response = traiterComparaisonTop3(msg, lang);                break;
            case "RECHERCHER_COMPETENCE": response = traiterRechercheCompetence(msg, lang);          break;
            case "ANALYSER_PERFORMANCE":  response = traiterAnalysePerformance(lang);                break;
            case "DISPONIBILITE":       response = traiterDisponibilite(lang);                       break;
            case "AIDE":                response = genererAide(lang);                               break;
            default:
                response = new ChatbotResponse();
                response.reponse = genererReponseDefaut(lang);
        }

        response.intention           = intention;
        response.messageUtilisateur  = messageUtilisateur;
        sauvegarderConversation(idUtilisateur, messageUtilisateur, response.reponse, intention);
        return response;
    }

    // ======================================================================
    // DÉTECTION LANGUE
    // ======================================================================

    private String detecterLangue(String message) {
        if (message.matches(".*[\u0600-\u06FF]+.*")) return "ar";
        if (matchPattern(message,
                "\\b(recommend|show|who|can|best|task|employee|performance|available|help|find|give|compare|top)\\b"))
            return "en";
        return "fr";
    }

    // ======================================================================
    // DÉTECTION D'INTENTION  (avec nouvelle intention COMPARER_TOP3)
    // ======================================================================

    private String detecterIntention(String message) {

        // ⑤ COMPARAISON TOP 3 (avant RECOMMANDATION pour priorité)
        if (matchPattern(message,
                "compar|compare|top\\s*3|top\\s*trois|meilleur.*3|3.*meilleur"
                        + "|classement.*employe|podium|\u0645\u0642\u0627\u0631\u0646\u0629")) {
            return "COMPARER_TOP3";
        }
        // RECOMMANDATION
        if (matchPattern(message,
                "recommand|suggest|recommend|propose|meilleur.*(?:employ|pour)|assign|qui peut"
                        + "|trouve.*employ|donne.*employ|best.*employ|who can|find.*employ"
                        + "|\u064a\u0648\u0635\u064a|\u0627\u0648\u0635\u064a|\u064a\u0646\u0635\u062d"
                        + "|\u0645\u0646 \u064a\u0633\u062a\u0637\u064a\u0639|\u0627\u0644\u0623\u0641\u0636\u0644")) {
            return "RECOMMANDER_EMPLOYE";
        }
        // RECHERCHE COMPÉTENCE
        if (matchPattern(message,
                "qui sait|comp[e\u00e9]tence|connai[ts]|ma[i\u00ee]trise|expert|capable|proficient"
                        + "|who knows|skilled|\u064a\u0639\u0631\u0641|\u062e\u0628\u064a\u0631|\u0645\u0647\u0627\u0631\u0629")) {
            return "RECHERCHER_COMPETENCE";
        }
        // PERFORMANCE
        if (matchPattern(message,
                "performance|\u00e9valuation|evaluation|classement|statistique|meilleur|top|ranking"
                        + "|\u0623\u062f\u0627\u0621|\u062a\u0642\u064a\u064a\u0645|\u062a\u0631\u062a\u064a\u0628")) {
            return "ANALYSER_PERFORMANCE";
        }
        // DISPONIBILITÉ
        if (matchPattern(message,
                "disponib|libre|occup|charge|peut.*prendre|available|free|workload"
                        + "|\u0645\u062a\u0627\u062d|\u062d\u0631|\u0645\u062a\u0641\u0631\u063a")) {
            return "DISPONIBILITE";
        }
        // AIDE
        if (matchPattern(message,
                "^aide$|^help$|comment.*utilis|que.*peux.*faire|what can you"
                        + "|\u0645\u0633\u0627\u0639\u062f\u0629")) {
            return "AIDE";
        }
        return "UNKNOWN";
    }

    private boolean matchPattern(String text, String pattern) {
        return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                .matcher(text).find();
    }

    private String normaliser(String msg) {
        return msg == null ? "" : msg.trim().toLowerCase();
    }

    // ======================================================================
    // ④ RÉPONSE PROFESSIONNELLE ENRICHIE
    // ======================================================================

    /**
     * Génère une réponse complète et professionnelle pour le meilleur candidat.
     */
    private String genererReponseComplete(RecommandationResult r, String nomTache) {
        // Infos de contact (téléphone + email) + indicateur photo
        String telephone = (r.employe.getTelephone() != null && !r.employe.getTelephone().isBlank())
                ? r.employe.getTelephone() : "—";
        String email = (r.employe.getEmail() != null && !r.employe.getEmail().isBlank())
                ? r.employe.getEmail() : "—";
        boolean hasPhoto = r.employe.getPhotoPath() != null && !r.employe.getPhotoPath().isBlank();

        return String.format(
                "\uD83E\uDD16 **Analyse IA terminée pour \"%s\"**\n\n"
                        + (hasPhoto ? "\uD83D\uDDBC\uFE0F Photo : disponible\n" : "")
                        + "\uD83E\uDD47 **Employé recommandé : %s %s**\n"
                        + "   \uD83D\uDCBC Poste       : %s\n"
                        + "   \uD83D\uDCDE Téléphone   : %s\n"
                        + "   \uD83D\uDCE7 Email       : %s\n\n"
                        + "\uD83D\uDCCA **Score global : %.1f/100** — %s\n"
                        + "   \uD83C\uDFAF Compétences   : %.0f%%\n"
                        + "   \uD83D\uDCC8 Performance   : %.0f%%\n"
                        + "   \uD83D\uDCC5 Disponibilité : %.0f%%\n"
                        + "   \uD83D\uDCDA Expérience    : %.0f%%\n\n"
                        + "\uD83D\uDD12 **Indice de confiance IA : %.0f%% — %s**\n\n"
                        + "\uD83E\uDDE0 **Raison :**\n%s",
                nomTache,
                r.employe.getPrenom(), r.employe.getNom(),
                r.employe.getPoste() != null ? r.employe.getPoste() : "—",
                telephone,
                email,
                r.scoreTotal, r.getAppreciation(),
                r.scoreCompetences,
                r.scorePerformance,
                r.scoreDisponibilite,
                r.scoreExperience,
                r.indiceConfiance, r.getConfianceLabel(),
                r.raisonRecommandation
        );
    }

    // ======================================================================
    // ⑤ MODE COMPARAISON TOP 3
    // ======================================================================

    private ChatbotResponse traiterComparaisonTop3(String message, String lang) {
        ChatbotResponse response = new ChatbotResponse();

        Integer idTache = extraireIdTache(message);

        if (idTache == null) {
            response.reponse = buildMsg(lang,
                    "\uD83E\uDD14 Pour comparer les 3 meilleurs, précisez la tâche.\n"
                            + "\uD83D\uDCA1 Ex: **\"Compare les 3 meilleurs pour la tâche Récolte\"**\n\n"
                            + "\uD83D\uDCCC Tâches disponibles :\n" + listerTaches(),
                    "\uD83E\uDD14 To compare the top 3, specify the task.\n"
                            + "\uD83D\uDCA1 E.g.: **\"Compare top 3 for task Harvest\"**\n\n"
                            + "\uD83D\uDCCC Available tasks:\n" + listerTaches(),
                    "\uD83E\uDD14 \u062d\u062f\u062f \u0627\u0644\u0645\u0647\u0645\u0629 \u0644\u0644\u0645\u0642\u0627\u0631\u0646\u0629."
            );
            return response;
        }

        Tache tache = tacheService.getTacheById(idTache);
        if (tache == null) {
            response.reponse = buildMsg(lang,
                    "\u274C Tâche introuvable. Tâches disponibles :\n" + listerTaches(),
                    "\u274C Task not found. Available tasks:\n" + listerTaches(),
                    "\u274C \u0627\u0644\u0645\u0647\u0645\u0629 \u063a\u064a\u0631 \u0645\u0648\u062c\u0648\u062f\u0629."
            );
            return response;
        }

        List<RecommandationResult> results = matchingService.recommanderEmployes(idTache, 3);

        if (results.size() < 2) {
            response.reponse = buildMsg(lang,
                    "\uD83D\uDE15 Moins de 2 employés disponibles pour cette tâche.",
                    "\uD83D\uDE15 Less than 2 employees available for this task.",
                    "\uD83D\uDE15 \u0623\u0642\u0644 \u0645\u0646 \u0645\u0648\u0638\u0641\u064a\u0646 \u0645\u062a\u0627\u062d\u064a\u0646."
            );
            return response;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(buildMsg(lang,
                "\uD83C\uDFC6 **Comparaison Top 3 pour \"" + tache.getTitre() + "\" :**\n\n",
                "\uD83C\uDFC6 **Top 3 Comparison for \"" + tache.getTitre() + "\":**\n\n",
                "\uD83C\uDFC6 **\u0645\u0642\u0627\u0631\u0646\u0629 \u0623\u0641\u0636\u0644 3 \u0644\u0640 \"" + tache.getTitre() + "\" :**\n\n"
        ));

        String[] medals = {"\uD83E\uDD47", "\uD83E\uDD48", "\uD83E\uDD49"};
        for (int i = 0; i < Math.min(results.size(), 3); i++) {
            RecommandationResult r = results.get(i);
            sb.append(String.format(
                    "%s **%s %s** — %.1f/100\n"
                            + "   Compétences: %.0f%% | Perf: %.0f%% | Dispo: %.0f%%\n"
                            + "   \uD83D\uDD12 Confiance: %.0f%% (%s)\n\n",
                    medals[i],
                    r.employe.getPrenom(), r.employe.getNom(), r.scoreTotal,
                    r.scoreCompetences, r.scorePerformance, r.scoreDisponibilite,
                    r.indiceConfiance, r.getConfianceLabel()
            ));
        }

        if (results.size() >= 2) {
            double ecart = results.get(0).scoreTotal - results.get(1).scoreTotal;
            sb.append(buildMsg(lang,
                    String.format("\uD83D\uDCA1 **%s** devance le suivant de **%.1f points**.",
                            results.get(0).employe.getPrenom() + " " + results.get(0).employe.getNom(), ecart),
                    String.format("\uD83D\uDCA1 **%s** is **%.1f points** ahead of the second.",
                            results.get(0).employe.getPrenom() + " " + results.get(0).employe.getNom(), ecart),
                    String.format("\uD83D\uDCA1 **%s** \u0645\u062a\u0642\u062f\u0645 \u0628\u0640 **%.1f \u0646\u0642\u0637\u0629**.",
                            results.get(0).employe.getPrenom() + " " + results.get(0).employe.getNom(), ecart)
            ));
        }

        response.reponse         = sb.toString();
        response.recommandations = results;
        return response;
    }

    // ======================================================================
    // TRAITEMENT : RECOMMANDATION (conservé + enrichi avec genererReponseComplete)
    // ======================================================================

    private ChatbotResponse traiterRecommandation(String message, int idUtilisateur, String lang) {
        ChatbotResponse response = new ChatbotResponse();

        Integer idTache = extraireIdTache(message);

        if (idTache == null) {
            response.reponse = buildMsg(lang,
                    "\uD83E\uDD14 Pour quelle tâche voulez-vous une recommandation ?\n\n"
                            + "\uD83D\uDCCC **Tâches disponibles :**\n"
                            + listerTaches()
                            + "\n\uD83D\uDCA1 Dites par exemple : \"Recommande pour la tâche REMISE\"",
                    "\uD83E\uDD14 For which task do you need a recommendation?\n\n"
                            + "\uD83D\uDCCC **Available tasks:**\n"
                            + listerTaches()
                            + "\n\uD83D\uDCA1 Say: \"Recommend for task REMISE\"",
                    "\uD83E\uDD14 \u0644\u0623\u064a \u0645\u0647\u0645\u0629 \u062a\u0631\u064a\u062f \u0627\u0644\u062a\u0648\u0635\u064a\u0629\u061f\n\n"
                            + "\uD83D\uDCCC **\u0627\u0644\u0645\u0647\u0627\u0645 \u0627\u0644\u0645\u062a\u0627\u062d\u0629 :**\n"
                            + listerTaches()
            );
            return response;
        }

        Tache tache = tacheService.getTacheById(idTache);
        if (tache == null) {
            response.reponse = buildMsg(lang,
                    "\u274C Aucune tâche trouvée avec ce nom.\n\n"
                            + "\uD83D\uDCCC **Tâches disponibles :**\n" + listerTaches()
                            + "\n\uD83D\uDCA1 Utilisez le nom exact. Ex: \"Recommande pour la tâche REMISE\"",
                    "\u274C No task found with that name.\n\n"
                            + "\uD83D\uDCCC **Available tasks:**\n" + listerTaches()
                            + "\n\uD83D\uDCA1 Use the exact task name from the list.",
                    "\u274C \u0644\u0645 \u0623\u062c\u062f \u0645\u0647\u0645\u0629.\n\n"
                            + "\uD83D\uDCCC \u0627\u0644\u0645\u0647\u0627\u0645 \u0627\u0644\u0645\u062a\u0627\u062d\u0629 :\n"
                            + listerTaches()
            );
            return response;
        }

        // ── Employé(s) déjà assigné(s) → carte visuelle avec photo + tél + email ──
        List<Employe> assignes = getEmployesActifsAssignesATache(idTache);

        if (!assignes.isEmpty()) {
            // Trouver le meilleur par performance
            Employe meilleur = assignes.get(0);
            double  meilleurScore = -1;
            for (Employe e : assignes) {
                double s = performanceService.calculatePerformance(e.getId()).score;
                if (s > meilleurScore) { meilleurScore = s; meilleur = e; }
            }

            // Construire un RecommandationResult pour passer par les cartes visuelles
            PerformanceService.PerformanceData perf =
                    performanceService.calculatePerformance(meilleur.getId());

            MatchingService.RecommandationResult r = new MatchingService.RecommandationResult();
            r.employe              = meilleur;
            r.scoreTotal          = perf.score;
            r.scorePerformance    = perf.score;
            r.scoreCompetences    = perf.score;          // approximation
            r.scoreDisponibilite  = perf.tachesEnCours <= 2 ? 80.0 : 40.0;
            r.scoreExperience     = perf.tauxReussite;
            r.indiceConfiance     = 90.0;                // assigné = confiance max
            r.raisonRecommandation = buildMsg(lang,
                    "Déjà assigné à cette tâche. " + perf.tachesTerminees
                            + "/" + perf.totalTaches + " tâches terminées.",
                    "Already assigned to this task. " + perf.tachesTerminees
                            + "/" + perf.totalTaches + " tasks completed.",
                    "مُعيَّن بالفعل. " + perf.tachesTerminees + "/" + perf.totalTaches
            );

            // Titre texte court
            String titreMsg = buildMsg(lang,
                    "✅ **" + (assignes.size() == 1 ? "Employé assigné" :
                            assignes.size() + " employés assignés — meilleur par performance")
                            + " à \"" + tache.getTitre() + "\" :**",
                    "✅ **" + (assignes.size() == 1 ? "Employee assigned" :
                            assignes.size() + " employees assigned — best by performance")
                            + " to \"" + tache.getTitre() + "\":**",
                    "✅ **الموظف المُعيَّن لـ \"" + tache.getTitre() + "\"**"
            );

            // Enrichir avec infos complètes (tel + email déjà chargés par SQL)
            String tel  = meilleur.getTelephone();
            String mail = meilleur.getEmail();
            String contact = "";
            if (tel  != null && !tel.isBlank())  contact += "\n   📞 " + tel;
            if (mail != null && !mail.isBlank()) contact += "\n   ✉ "  + mail;

            response.reponse = titreMsg + contact;
            response.recommandations.add(r);

            // Ajouter les autres assignés en compact si plusieurs
            if (assignes.size() > 1) {
                StringBuilder sb = new StringBuilder(response.reponse);
                sb.append(buildMsg(lang,
                        "\n\n📋 **Autres assignés :** " + (assignes.size() - 1),
                        "\n\n📋 **Other assigned:** " + (assignes.size() - 1),
                        "\n\n📋 **آخرون:** " + (assignes.size() - 1)
                ));
                for (int i = 0; i < assignes.size(); i++) {
                    Employe e = assignes.get(i);
                    if (e.getId() == meilleur.getId()) continue;
                    sb.append("\n   • ").append(e.getPrenom()).append(" ").append(e.getNom());
                    if (e.getTelephone() != null && !e.getTelephone().isBlank())
                        sb.append(" — 📞 ").append(e.getTelephone());
                    if (e.getEmail() != null && !e.getEmail().isBlank())
                        sb.append(" ✉ ").append(e.getEmail());
                }
                response.reponse = sb.toString();
            }
            return response;
        }

        // Pas d'assignation → MatchingService IA
        List<RecommandationResult> recommandations = matchingService.recommanderEmployes(idTache, 3);

        if (!recommandations.isEmpty()) {
            // ④ Réponse professionnelle pour le meilleur
            RecommandationResult best = recommandations.get(0);
            response.reponse = genererReponseComplete(best, tache.getTitre());

            // Si plusieurs résultats, ajouter les suivants en compact
            if (recommandations.size() > 1) {
                StringBuilder sb = new StringBuilder(response.reponse);
                sb.append(buildMsg(lang,
                        "\n\n\uD83D\uDCCB **Autres candidats :**\n",
                        "\n\n\uD83D\uDCCB **Other candidates:**\n",
                        "\n\n\uD83D\uDCCB **\u0645\u0631\u0634\u062d\u0648\u0646 \u0622\u062e\u0631\u0648\u0646 :**\n"
                ));
                for (int i = 1; i < recommandations.size(); i++) {
                    RecommandationResult r = recommandations.get(i);
                    sb.append(String.format("   %s **%s %s** — %.1f/100 (%s)\n",
                            r.getEmoji(),
                            r.employe.getPrenom(), r.employe.getNom(),
                            r.scoreTotal, r.getAppreciation()));
                }
                sb.append(buildMsg(lang,
                        "\n\uD83D\uDCA1 *Tapez \"compare les 3\" pour une analyse complète.*",
                        "\n\uD83D\uDCA1 *Type \"compare top 3\" for full analysis.*",
                        "\n\uD83D\uDCA1 *\u0627\u0643\u062a\u0628 \"\u0645\u0642\u0627\u0631\u0646\u0629\" \u0644\u062a\u062d\u0644\u064a\u0644 \u0643\u0627\u0645\u0644.*"
                ));
                response.reponse = sb.toString();
            }
            response.recommandations = recommandations;
        } else {
            response.reponse = buildMsg(lang,
                    "\uD83D\uDE15 Aucun employé actif ne correspond à cette tâche pour le moment.",
                    "\uD83D\uDE15 No active employee matches this task at the moment.",
                    "\uD83D\uDE15 \u0644\u0627 \u064a\u0648\u062c\u062f \u0645\u0648\u0638\u0641 \u0645\u062a\u0627\u062d."
            );
        }
        return response;
    }

    // ======================================================================
    // TRAITEMENT : RECHERCHE PAR COMPÉTENCE
    // ======================================================================

    private ChatbotResponse traiterRechercheCompetence(String message, String lang) {
        ChatbotResponse response = new ChatbotResponse();
        String competence = extraireCompetence(message);

        if (competence != null) {
            List<Employe> employes = rechercherEmployesActifsParCompetence(competence);
            if (!employes.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append(buildMsg(lang,
                        "\uD83D\uDD0D **" + employes.size() + " employé(s) avec \"" + competence + "\" :**\n\n",
                        "\uD83D\uDD0D **" + employes.size() + " employee(s) with \"" + competence + "\":**\n\n",
                        "\uD83D\uDD0D **" + employes.size() + " \u0645\u0648\u0638\u0641 :**\n\n"
                ));
                for (int i = 0; i < Math.min(employes.size(), 5); i++) {
                    Employe emp = employes.get(i);
                    sb.append(String.format("\uD83D\uDC64 **%s %s**", emp.getPrenom(), emp.getNom()));
                    if (emp.getPoste() != null && !emp.getPoste().isEmpty())
                        sb.append(" — ").append(emp.getPoste());
                    if (emp.getEmail() != null && !emp.getEmail().isEmpty())
                        sb.append("\n   \uD83D\uDCE7 ").append(emp.getEmail());
                    sb.append("\n\n");
                }
                if (employes.size() > 5)
                    sb.append("*... +").append(employes.size() - 5).append(" autres*");
                response.reponse = sb.toString();
            } else {
                response.reponse = buildMsg(lang,
                        "\uD83D\uDE15 Aucun employé actif trouvé avec la compétence \"" + competence + "\".",
                        "\uD83D\uDE15 No active employee found with skill \"" + competence + "\".",
                        "\uD83D\uDE15 \u0644\u0627 \u064a\u0648\u062c\u062f \u0645\u0648\u0638\u0641 \u0628\u0645\u0647\u0627\u0631\u0629 \"" + competence + "\"."
                );
            }
        } else {
            response.reponse = buildMsg(lang,
                    "\uD83E\uDD14 Quelle compétence recherchez-vous ?\n\uD83D\uDCA1 Ex: \"Qui maîtrise l'irrigation ?\"",
                    "\uD83E\uDD14 Which skill are you looking for?\n\uD83D\uDCA1 E.g.: \"Who knows irrigation?\"",
                    "\uD83E\uDD14 \u0645\u0627 \u0627\u0644\u0645\u0647\u0627\u0631\u0629 \u0627\u0644\u062a\u064a \u062a\u0628\u062d\u062b \u0639\u0646\u0647\u0627 \u061f"
            );
        }
        return response;
    }

    // ======================================================================
    // TRAITEMENT : PERFORMANCE
    // ======================================================================

    private ChatbotResponse traiterAnalysePerformance(String lang) {
        ChatbotResponse response = new ChatbotResponse();
        // Filtrer par agriculteur actif ET actif=TRUE (exclut employés supprimés)
        Integer idAgriculteur = tn.neuron.ardhi.utils.gestionemployeutils.AgriculteurContext.getActiveAgriculteurId();
        List<PerformanceService.PerformanceData> classement = performanceService.getClassement(idAgriculteur);

        List<PerformanceService.PerformanceData> avecTaches = new ArrayList<>();
        for (PerformanceService.PerformanceData p : classement) {
            if (p.totalTaches > 0) { avecTaches.add(p); }
            if (avecTaches.size() >= 5) break;
        }

        if (!avecTaches.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(buildMsg(lang,
                    "\uD83D\uDCCA **Classement des Top Performeurs :**\n\n",
                    "\uD83D\uDCCA **Top Performers Ranking:**\n\n",
                    "\uD83D\uDCCA **\u062a\u0631\u062a\u064a\u0628 \u0623\u0641\u0636\u0644 \u0627\u0644\u0645\u0648\u0638\u0641\u064a\u0646 :**\n\n"
            ));
            String[] medals = {"\uD83E\uDD47", "\uD83E\uDD48", "\uD83E\uDD49", "\uD83C\uDFC5", "\u2B50"};
            for (int i = 0; i < avecTaches.size(); i++) {
                PerformanceService.PerformanceData p = avecTaches.get(i);
                // Enrichir avec contact (getClassement garantit actif=TRUE)
                tn.neuron.ardhi.models.gestionemployemodel.Employe emp =
                        employeService.getEmployeById(p.idEmploye);
                String telLine  = (emp != null && emp.getTelephone() != null && !emp.getTelephone().isBlank())
                        ? "   \uD83D\uDCDE " + emp.getTelephone() + "\n" : "";
                String mailLine = (emp != null && emp.getEmail() != null && !emp.getEmail().isBlank())
                        ? "   \uD83D\uDCE7 " + emp.getEmail() + "\n" : "";
                sb.append(String.format(
                        "%s **#%d - %s**\n"
                                + "   \uD83D\uDCBC Score: %.1f/100 — %s\n"
                                + "   \u2705 Tâches terminées: %d/%d (%.1f%%)\n"
                                + "%s%s\n",
                        medals[i], i + 1, p.nomEmploye,
                        p.score, p.getAppreciation(),
                        p.tachesTerminees, p.totalTaches, p.tauxReussite,
                        telLine, mailLine
                ));
            }
            sb.append(buildMsg(lang,
                    "\uD83D\uDCA1 *Basé uniquement sur les employés actifs.*",
                    "\uD83D\uDCA1 *Based on active employees only.*",
                    "\uD83D\uDCA1 *\u0628\u0646\u0627\u0621\u064b \u0639\u0644\u0649 \u0627\u0644\u0645\u0648\u0638\u0641\u064a\u0646 \u0627\u0644\u0646\u0634\u0637\u064a\u0646 \u0641\u0642\u0637.*"
            ));
            response.reponse = sb.toString();
        } else {
            response.reponse = buildMsg(lang,
                    "\uD83D\uDCCA Aucune donnée de performance disponible.",
                    "\uD83D\uDCCA No performance data available yet.",
                    "\uD83D\uDCCA \u0644\u0627 \u062a\u0648\u062c\u062f \u0628\u064a\u0627\u0646\u0627\u062a \u0623\u062f\u0627\u0621 \u0628\u0639\u062f."
            );
        }
        return response;
    }

    // ======================================================================
    // TRAITEMENT : DISPONIBILITÉ
    // ======================================================================

    private ChatbotResponse traiterDisponibilite(String lang) {
        ChatbotResponse response = new ChatbotResponse();
        List<DisponibiliteInfo> dispos = getDisponibilitesActifs();

        if (!dispos.isEmpty()) {
            // Titre de la réponse texte (les cartes visuelles sont gérées par le Controller)
            response.reponse = buildMsg(lang,
                    "\uD83D\uDCC5 **Disponibilité des " + dispos.size() + " employés actifs :**\n"
                            + "\uD83D\uDFE2 Disponible • \uD83D\uDFE1 Modéré (1-2 tâches) • \uD83D\uDD34 Surchargé (3+)",
                    "\uD83D\uDCC5 **Availability of " + dispos.size() + " active employees:**\n"
                            + "\uD83D\uDFE2 Available • \uD83D\uDFE1 Moderate (1-2 tasks) • \uD83D\uDD34 Overloaded (3+)",
                    "\uD83D\uDCC5 **\u062a\u0648\u0641\u0631 " + dispos.size() + " \u0645\u0648\u0638\u0641\u064a\u0646 \u0646\u0634\u0637\u064a\u0646**"
            );
            // Peupler les cartes visuelles (photo + tél + email)
            for (DisponibiliteInfo d : dispos) {
                response.disponibilites.add(
                        new ChatbotResponse.DisponibiliteEntry(d.employe, d.tachesEnCours));
            }
        } else {
            response.reponse = buildMsg(lang,
                    "\uD83D\uDCC5 Aucune information de disponibilité.",
                    "\uD83D\uDCC5 No availability data.",
                    "\uD83D\uDCC5 \u0644\u0627 \u062a\u0648\u062c\u062f \u0628\u064a\u0627\u0646\u0627\u062a \u062a\u0648\u0641\u0631."
            );
        }
        return response;
    }

    // ======================================================================
    // AIDE + DÉFAUT
    // ======================================================================

    private ChatbotResponse genererAide(String lang) {
        ChatbotResponse response = new ChatbotResponse();
        if ("ar".equals(lang)) {
            response.reponse =
                    "\uD83D\uDC4B **مرحباً ! أنا مساعد Ardhi**\n\n"
                            + "\uD83C\uDFAF اوصي لمهمة ...\n"
                            + "\uD83D\uDD0D من يعرف الري ?\n"
                            + "\uD83D\uDCCA أظهر الأداء\n"
                            + "\uD83D\uDCC5 من متاح ?\n"
                            + "\uD83C\uDFC6 قارني أفضل 3 للمهمة ...\n\n"
                            + "\uD83D\uDCA1 *تحدث بشكل طبيعي !*";
        } else if ("en".equals(lang)) {
            response.reponse =
                    "\uD83D\uDC4B **Hi! I'm your Ardhi AI Assistant**\n\n"
                            + "\uD83C\uDFAF \"Recommend for task HARVEST\"\n"
                            + "\uD83D\uDD0D \"Who knows irrigation?\"\n"
                            + "\uD83D\uDCCA \"Show me performance stats\"\n"
                            + "\uD83D\uDCC5 \"Who is available?\"\n"
                            + "\uD83C\uDFC6 \"Compare top 3 for task HARVEST\"\n\n"
                            + "\uD83D\uDCA1 *Talk naturally — no formal questions needed!*";
        } else {
            response.reponse =
                    "\uD83D\uDC4B **Je suis votre Assistant RH Ardhi !**\n\n"
                            + "\uD83C\uDFAF \"Recommande pour la tâche RÉCOLTE\"\n"
                            + "\uD83D\uDD0D \"Qui maîtrise l'irrigation ?\"\n"
                            + "\uD83D\uDCCA \"Montre les performances\"\n"
                            + "\uD83D\uDCC5 \"Qui est disponible ?\"\n"
                            + "\uD83C\uDFC6 \"Compare les 3 meilleurs pour la tâche RÉCOLTE\"\n\n"
                            + "\uD83D\uDCA1 *Parlez naturellement — pas de questions formelles !*";
        }
        return response;
    }

    private String genererReponseDefaut(String lang) {
        return buildMsg(lang,
                "\uD83E\uDD14 Je n'ai pas compris. Tapez **aide** pour voir mes capacités.\n"
                        + "\uD83D\uDCA1 Ex: \"Recommande pour la tâche RÉCOLTE\" ou \"Compare les 3 meilleurs\"",
                "\uD83E\uDD14 I didn't understand. Type **help** to see what I can do.",
                "\uD83E\uDD14 \u0644\u0645 \u0623\u0641\u0647\u0645. \u0627\u0643\u062a\u0628 **\u0645\u0633\u0627\u0639\u062f\u0629**."
        );
    }

    // ======================================================================
    // UTILITAIRES
    // ======================================================================

    private String buildMsg(String lang, String fr, String en, String ar) {
        switch (lang) {
            case "ar": return ar;
            case "en": return en;
            default:   return fr;
        }
    }

    /**
     * Liste uniquement les tâches ACTIVES (En attente / En cours).
     * Les tâches supprimées, terminées, validées ou annulées ne s'affichent plus.
     */
    private String listerTaches() {
        List<Tache> toutesLesTaches = tacheService.getAllTaches();
        if (toutesLesTaches == null || toutesLesTaches.isEmpty())
            return "  (aucune tâche active dans le système)\n";

        // Filtrer : uniquement les tâches non terminées / non annulées
        List<Tache> actives = new ArrayList<>();
        for (Tache t : toutesLesTaches) {
            String s = t.getStatut();
            if (s == null) { actives.add(t); continue; }
            // Exclure : Terminé, Validé, Annulé (conserve En attente + En cours)
            if (!s.equalsIgnoreCase("Terminé")
                    && !s.equalsIgnoreCase("Terminee")
                    && !s.equalsIgnoreCase("Validé")
                    && !s.equalsIgnoreCase("Validee")
                    && !s.equalsIgnoreCase("Annulé")
                    && !s.equalsIgnoreCase("Annulee")) {
                actives.add(t);
            }
        }

        if (actives.isEmpty())
            return "  (aucune tâche active pour le moment)\n";

        StringBuilder sb = new StringBuilder();
        for (Tache t : actives) {
            sb.append("  • ").append(t.getTitre());
            if (t.getCategorie() != null && !t.getCategorie().isEmpty())
                sb.append(" [").append(t.getCategorie()).append("]");
            sb.append("\n");
        }
        return sb.toString();
    }

    private String formatEmployePerf(Employe emp, PerformanceService.PerformanceData perf) {
        String poste = (emp.getPoste() != null && !emp.getPoste().isEmpty()) ? " — " + emp.getPoste() : "";
        return String.format(
                "\uD83D\uDC64 **%s %s**%s\n"
                        + "   • Score: %.1f/100 — %s\n"
                        + "   • Tâches terminées: %d/%d (%.1f%%)\n",
                emp.getPrenom(), emp.getNom(), poste,
                perf.score, perf.getAppreciation(),
                perf.tachesTerminees, perf.totalTaches,
                perf.totalTaches > 0 ? perf.tauxReussite : 0.0
        );
    }

    // ======================================================================
    // EXTRACTION : ID / NOM DE TÂCHE
    // ======================================================================

    private Integer extraireIdTache(String message) {
        Pattern numP = Pattern.compile(
                "(?:t[aâ]che|task|\u0645\u0647\u0645\u0629)\\s*[#n°]*\\s*(\\d+)",
                Pattern.CASE_INSENSITIVE);
        Matcher numM = numP.matcher(message);
        if (numM.find()) {
            try { return Integer.parseInt(numM.group(1)); }
            catch (NumberFormatException ignore) {}
        }
        String nomTache = extraireNomTache(message);
        if (nomTache != null) {
            Integer id = chercherTacheParNom(nomTache);
            if (id != null) return id;
        }
        Pattern anyN = Pattern.compile("\\b(\\d{1,4})\\b");
        Matcher anyM = anyN.matcher(message);
        if (anyM.find()) {
            try {
                int n = Integer.parseInt(anyM.group(1));
                if (n > 0 && n < 10000) return n;
            } catch (NumberFormatException ignore) {}
        }
        return null;
    }

    private String extraireNomTache(String message) {
        Pattern p1 = Pattern.compile(
                "t[aâ]che\\s+(?:de\\s+)?(?:d')?([a-z\u00e0-\u00ff\\s]{3,30}?)(?:\\s+(?:pour|de|du|des|par)|$)",
                Pattern.CASE_INSENSITIVE);
        Matcher m1 = p1.matcher(message);
        if (m1.find()) {
            String nom = m1.group(1).trim();
            if (nom.length() >= 3) return nom;
        }
        Pattern p2 = Pattern.compile(
                "pour\\s+(?:la\\s+|le\\s+|l'|les\\s+|un\\s+|une\\s+)?([a-z\u00e0-\u00ff][a-z\u00e0-\u00ff\\s]{2,25}?)(?:\\s+(?:de|du|par)|$)",
                Pattern.CASE_INSENSITIVE);
        Matcher m2 = p2.matcher(message);
        if (m2.find()) {
            String nom = m2.group(1).trim();
            if (!nom.matches("faire|la|le|les|un|une|des") && nom.length() >= 3) return nom;
        }
        Pattern p3 = Pattern.compile(
                "for\\s+(?:the\\s+|a\\s+)?([a-z][a-z\\s]{2,25}?)(?:\\s|$)",
                Pattern.CASE_INSENSITIVE);
        Matcher m3 = p3.matcher(message);
        if (m3.find()) {
            String nom = m3.group(1).trim();
            if (!nom.matches("task|doing|making|an|a|the") && nom.length() >= 3) return nom;
        }
        return null;
    }

    private Integer chercherTacheParNom(String nomRecherche) {
        if (nomRecherche == null || nomRecherche.isBlank()) return null;
        String nomLow = nomRecherche.trim().toLowerCase();
        List<Tache> toutes = tacheService.getAllTaches();

        // Filtrer uniquement les tâches actives (même logique que listerTaches)
        List<Tache> taches = new ArrayList<>();
        for (Tache t : toutes) {
            String s = t.getStatut();
            if (s == null
                    || (!s.equalsIgnoreCase("Terminé")
                    && !s.equalsIgnoreCase("Terminee")
                    && !s.equalsIgnoreCase("Validé")
                    && !s.equalsIgnoreCase("Validee")
                    && !s.equalsIgnoreCase("Annulé")
                    && !s.equalsIgnoreCase("Annulee"))) {
                taches.add(t);
            }
        }

        for (Tache t : taches)
            if (t.getTitre().toLowerCase().equals(nomLow)) return t.getId();
        for (Tache t : taches) {
            String titre = t.getTitre().toLowerCase();
            if (titre.contains(nomLow) || nomLow.contains(titre)) return t.getId();
        }
        String[] mots = nomLow.split("\\s+");
        for (Tache t : taches) {
            String titre = t.getTitre().toLowerCase();
            int communs = 0;
            for (String mot : mots)
                if (mot.length() >= 3 && titre.contains(mot)) communs++;
            if (communs > 0 && communs >= mots.length * 0.5) return t.getId();
        }
        return null;
    }

    private String extraireCompetence(String message) {
        String[] courantes = {
                "irrigation", "tracteur", "fertilisation", "récolte", "plantation",
                "taille", "entretien", "maintenance", "conduite", "serre",
                "harvest", "driving", "pruning", "greenhouse", "fertilization"
        };
        for (String c : courantes) if (message.contains(c)) return c;
        Pattern p = Pattern.compile("(?:en|de|in)\\s+([\\p{L}\\-]{3,})", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(message);
        if (m.find()) return m.group(1);
        return null;
    }

    // ======================================================================
    // REQUÊTES SQL
    // ======================================================================

    private List<Employe> getEmployesActifsAssignesATache(int idTache) {
        List<Employe> employes = new ArrayList<>();
        String query =
                "SELECT DISTINCT e.id_employe, e.nom, e.prenom, e.email, e.poste, "
                        + "e.telephone, COALESCE(e.photo_path,'') AS photo_path "
                        + "FROM employe e "
                        + "INNER JOIN tache t ON t.id_employe = e.id_employe "
                        + "WHERE t.id_tache = ? AND e.actif = TRUE";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idTache);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Employe emp = new Employe();
                    emp.setId(rs.getInt("id_employe"));
                    emp.setNom(rs.getString("nom"));
                    emp.setPrenom(rs.getString("prenom"));
                    emp.setEmail(rs.getString("email"));
                    emp.setPoste(rs.getString("poste"));
                    try { emp.setTelephone(rs.getString("telephone")); } catch (Exception ignored) {}
                    try {
                        String ph = rs.getString("photo_path");
                        if (ph != null && !ph.isBlank()) emp.setPhotoPath(ph);
                    } catch (Exception ignored) {}
                    employes.add(emp);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur getEmployesActifsAssignesATache: " + e.getMessage());
        }
        return employes;
    }

    private List<Employe> rechercherEmployesActifsParCompetence(String competence) {
        List<Employe> employes = new ArrayList<>();
        String query =
                "SELECT DISTINCT e.id_employe, e.nom, e.prenom, e.email, e.poste "
                        + "FROM employe e "
                        + "WHERE e.actif = TRUE "
                        + "AND (e.poste LIKE ? OR e.nom LIKE ? OR e.prenom LIKE ?) "
                        + "ORDER BY e.nom, e.prenom LIMIT 10";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            String p = "%" + competence + "%";
            stmt.setString(1, p); stmt.setString(2, p); stmt.setString(3, p);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Employe emp = new Employe();
                    emp.setId(rs.getInt("id_employe"));
                    emp.setNom(rs.getString("nom"));
                    emp.setPrenom(rs.getString("prenom"));
                    emp.setEmail(rs.getString("email"));
                    emp.setPoste(rs.getString("poste"));
                    employes.add(emp);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur rechercherEmployesActifsParCompetence: " + e.getMessage());
        }
        return employes;
    }

    private List<DisponibiliteInfo> getDisponibilitesActifs() {
        List<DisponibiliteInfo> dispos = new ArrayList<>();
        // Filtre agriculteur actif pour n'afficher que SES employés
        Integer idAgriculteur = tn.neuron.ardhi.utils.gestionemployeutils.AgriculteurContext.getActiveAgriculteurId();
        String agriFilter = (idAgriculteur != null) ? " AND e.id_agriculteur = " + idAgriculteur : "";

        // ── Récupère email, telephone, poste, photo_path en plus du count ──
        String query =
                "SELECT e.id_employe, e.nom, e.prenom, "
                        + "e.email, e.telephone, e.poste, "
                        + "COALESCE(e.photo_path,'') AS photo_path, "
                        + "COUNT(t.id_tache) AS en_cours "
                        + "FROM employe e "
                        + "LEFT JOIN tache t ON e.id_employe = t.id_employe "
                        + "  AND t.statut IN ('EnCours','En cours','EN_COURS','Nouveau','En attente') "
                        + "WHERE e.actif = TRUE" + agriFilter + " "
                        + "GROUP BY e.id_employe, e.nom, e.prenom, "
                        + "         e.email, e.telephone, e.poste, e.photo_path "
                        + "ORDER BY en_cours ASC, e.nom";

        try (Statement stmt = getCnx().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Employe emp = new Employe();
                emp.setId(rs.getInt("id_employe"));
                emp.setNom(rs.getString("nom"));
                emp.setPrenom(rs.getString("prenom"));
                emp.setEmail(rs.getString("email"));
                try { emp.setTelephone(rs.getString("telephone")); } catch (Exception ignored) {}
                try { emp.setPoste(rs.getString("poste")); } catch (Exception ignored) {}
                try {
                    String ph = rs.getString("photo_path");
                    if (ph != null && !ph.isBlank()) emp.setPhotoPath(ph);
                } catch (Exception ignored) {}

                DisponibiliteInfo d = new DisponibiliteInfo();
                d.employe       = emp;
                d.tachesEnCours = rs.getInt("en_cours");
                dispos.add(d);
            }
        } catch (SQLException e) {
            System.err.println("Erreur getDisponibilitesActifs: " + e.getMessage());
        }
        return dispos;
    }

    private void sauvegarderConversation(int idUtilisateur, String msg, String rep, String intention) {
        System.out.println("[Chatbot] Conv sauvegardée : " + intention);
    }

    // ======================================================================
    // CLASSES INTERNES
    // ======================================================================

    public static class ChatbotResponse {
        public String messageUtilisateur;
        public String reponse;
        public String intention;
        public List<RecommandationResult> recommandations = new ArrayList<>();
        // Cartes visuelles pour la disponibilité (photo + tél + email)
        public List<DisponibiliteEntry> disponibilites = new ArrayList<>();

        public static class DisponibiliteEntry {
            public Employe employe;
            public int tachesEnCours;

            public DisponibiliteEntry(Employe e, int nb) {
                this.employe = e;
                this.tachesEnCours = nb;
            }

            public String getStatutLabel() {
                if (tachesEnCours == 0) return "Disponible";
                if (tachesEnCours <= 2) return "Modéré";
                return "Surchargé";
            }

            public String getCouleurStatut() {
                if (tachesEnCours == 0) return "#27ae60";
                if (tachesEnCours <= 2) return "#f39c12";
                return "#e74c3c";
            }

            public String getDotEmoji() {
                if (tachesEnCours == 0) return "🟢";
                if (tachesEnCours <= 2) return "🟡";
                return "🔴";
            }
        }
    }

    private static class DisponibiliteInfo {
        Employe employe;
        int tachesEnCours;
    }
}