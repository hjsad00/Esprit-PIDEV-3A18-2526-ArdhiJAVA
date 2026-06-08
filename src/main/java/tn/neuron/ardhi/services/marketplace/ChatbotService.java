package tn.neuron.ardhi.services.marketplace;

import tn.neuron.ardhi.interfaces.marketplace.IAvisService;
import tn.neuron.ardhi.interfaces.marketplace.IPanierService;
import tn.neuron.ardhi.interfaces.marketplace.IProduitService;
import tn.neuron.ardhi.models.marketplace.ChatIntent;
import tn.neuron.ardhi.models.marketplace.Panier;
import tn.neuron.ardhi.models.marketplace.PanierProduit;
import tn.neuron.ardhi.models.marketplace.Produit;
import tn.neuron.ardhi.models.marketplace.ProduitDemande;

import java.util.Comparator;
import java.util.List;

/**
 * Service métier du chatbot marketplace.
 *
 * v3 : retourne ChatIntent directement (texteReponse inclus dans l'objet)
 *      Pas besoin de classe ChatbotResponse séparée.
 */
public class ChatbotService {

    private final IProduitService produitService;
    private final IPanierService panierService;
    private final IAvisService avisService;
    private final AIService aiService;

    public ChatbotService() {
        this.produitService = new ProduitService();
        this.panierService = new PanierService();
        this.avisService = new AvisService();
        this.aiService = new AIService();
    }

    /**
     * Point d'entrée principal.
     * Retourne le ChatIntent enrichi avec texteReponse.
     * Le controller lit intent.getTexteReponse() pour afficher le message
     * et intent.getIntention() pour appliquer les filtres si "filtrer".
     */
    public ChatIntent traiterMessage(String message, int idUser) {
        ChatIntent intent = aiService.analyser(message);
        System.out.println("[ChatbotService] Intent reçu : " + intent);

        if (intent == null) {
            ChatIntent erreur = new ChatIntent();
            erreur.setIntention("erreur");
            erreur.setTexteReponse("❌ Une erreur interne est survenue. Veuillez réessayer.");
            return erreur;
        }

        String texte;
        switch (intent.getIntention()) {
            case "achat":
                texte = traiterAchat(intent, idUser);
                break;
            case "disponibilite":
                texte = traiterDisponibilite(intent);
                break;
            case "vider_panier":
                texte = traiterViderPanier(idUser);
                break;
            case "supprimer_produit":
                texte = traiterSupprimerProduit(intent, idUser);
                break;
            case "filtrer":
                texte = traiterFiltrage(intent);
                break;
            case "hors_sujet":
                texte = "🌾 Je suis l'assistant du **Marketplace Agricole**. Je peux vous aider à :\n" +
                        "• 🛒 Acheter des produits\n" +
                        "• 📦 Vérifier la disponibilité\n" +
                        "• 🔍 Filtrer le catalogue (ex: \"montre moi les fruits\", \"produits moins de 10 DT\")\n" +
                        "• 🗑️ Supprimer un article du panier\n" +
                        "• 🧹 Vider votre panier";
                break;
            case "erreur":
                texte = "⚠️ Impossible de contacter l'assistant IA. Vérifiez qu'Ollama est démarré :\n  ollama serve";
                break;
            default:
                texte = "❓ Je n'ai pas compris votre demande. Pouvez-vous reformuler ?";
        }

        intent.setTexteReponse(texte);
        return intent;
    }

    // -------------------------------------------------------------------------
    // Traitement FILTRAGE
    // -------------------------------------------------------------------------

    private String traiterFiltrage(ChatIntent intent) {
        StringBuilder sb = new StringBuilder("🔍 **Filtres appliqués :**\n\n");
        boolean auMoinsUnFiltre = false;

        if (intent.getRecherche() != null && !intent.getRecherche().isBlank()) {
            sb.append("  • Recherche : **").append(intent.getRecherche()).append("**\n");
            auMoinsUnFiltre = true;
        }

        if (intent.getCategorie() != null && !intent.getCategorie().isBlank()) {
            List<String> categoriesDispos = produitService.getAllCategories();
            String categorieValidee = categoriesDispos.stream()
                    .filter(c -> c.equalsIgnoreCase(intent.getCategorie()))
                    .findFirst().orElse(null);

            if (categorieValidee != null) {
                intent.setCategorie(categorieValidee);
                sb.append("  • Catégorie : **").append(categorieValidee).append("**\n");
                auMoinsUnFiltre = true;
            } else {
                sb.append("  ⚠️ Catégorie **").append(intent.getCategorie())
                        .append("** non trouvée. Disponibles : ")
                        .append(String.join(", ", categoriesDispos)).append("\n");
                intent.setCategorie(null);
            }
        }

        if (intent.getPrixMin() != null) {
            sb.append("  • Prix minimum : **").append(String.format("%.0f DT", intent.getPrixMin())).append("**\n");
            auMoinsUnFiltre = true;
        }

        if (intent.getPrixMax() != null) {
            sb.append("  • Prix maximum : **").append(String.format("%.0f DT", intent.getPrixMax())).append("**\n");
            auMoinsUnFiltre = true;
        }

        if (intent.getCritere() != null) {
            String triLabel;
            switch (intent.getCritere()) {
                case "prix_asc":  triLabel = "Prix croissant"; break;
                case "prix_desc": triLabel = "Prix décroissant"; break;
                case "avis":      triLabel = "Meilleures notes"; break;
                default:          triLabel = intent.getCritere();
            }
            sb.append("  • Tri : **").append(triLabel).append("**\n");
            auMoinsUnFiltre = true;
        }

        if (!auMoinsUnFiltre) {
            return "❓ Je n'ai pas compris quels filtres appliquer. Essayez :\n" +
                    "  • \"Montre moi les fruits\"\n" +
                    "  • \"Cherche des tomates\"\n" +
                    "  • \"Produits entre 5 et 20 DT\"\n" +
                    "  • \"Les légumes les moins chers\"";
        }

        sb.append("\n✅ Le catalogue a été mis à jour !");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Traitement ACHAT
    // -------------------------------------------------------------------------

    private String traiterAchat(ChatIntent intent, int idUser) {
        if (intent.getProduits() == null || intent.getProduits().isEmpty())
            return "❓ Je n'ai pas identifié de produits dans votre demande. Pouvez-vous préciser ?";

        Panier panier = panierService.getPanierActif(idUser);
        if (panier == null) return "❌ Impossible d'accéder à votre panier. Veuillez réessayer.";

        StringBuilder recap = new StringBuilder("🛒 **Récapitulatif de votre commande :**\n\n");
        int produitsAjoutes = 0;
        StringBuilder nonTrouves = new StringBuilder();

        for (ProduitDemande demande : intent.getProduits()) {
            String nomDemande = demande.getNom();
            int quantite = Math.max(1, demande.getQuantite());

            List<Produit> resultats = produitService.searchProduits(nomDemande);
            if (resultats.isEmpty()) resultats = produitService.searchProduits(nomDemande.toLowerCase());
            if (resultats.isEmpty()) resultats = produitService.searchProduits(normaliserNom(nomDemande));

            if (resultats.isEmpty()) {
                nonTrouves.append("  ❌ **").append(nomDemande).append("** — produit introuvable dans le catalogue\n");
                continue;
            }

            resultats.removeIf(p -> p.getIdUser() == idUser);
            if (resultats.isEmpty()) {
                nonTrouves.append("  🚫 **").append(nomDemande).append("** — vous ne pouvez pas acheter vos propres produits\n");
                continue;
            }

            resultats.removeIf(p -> p.getQuantiteStock() < quantite);
            if (resultats.isEmpty()) {
                nonTrouves.append("  ⚠️ **").append(nomDemande)
                        .append("** — stock insuffisant (quantité demandée : ").append(quantite).append(")\n");
                continue;
            }

            Produit choisi = selectionnerProduit(resultats, intent.getCritere());
            boolean success = panierService.ajouterProduit(panier.getIdPanier(), choisi.getIdProduit(), quantite);

            if (success) {
                produitsAjoutes++;
                float prixAffiche = choisi.getPrixApresRemise();
                recap.append("  ✅ **").append(choisi.getNom()).append("**\n");
                recap.append("     • Quantité : ").append(quantite).append("\n");
                recap.append("     • Prix unitaire : ").append(String.format("%.2f DT", prixAffiche)).append("\n");
                recap.append("     • Total : ").append(String.format("%.2f DT", prixAffiche * quantite)).append("\n");
                if ("avis".equals(intent.getCritere())) {
                    double note = avisService.getNoteMoyenne(choisi.getIdProduit());
                    int nbAvis = avisService.getNombreAvis(choisi.getIdProduit());
                    recap.append("     • Note : ").append(String.format("%.1f/5 (%d avis)", note, nbAvis)).append("\n");
                }
                recap.append("\n");
            } else {
                nonTrouves.append("  ❌ **").append(choisi.getNom()).append("** — erreur lors de l'ajout au panier\n");
            }
        }

        if (nonTrouves.length() > 0)
            recap.append("**Produits non ajoutés :**\n").append(nonTrouves).append("\n");

        if (produitsAjoutes > 0) {
            float total = panierService.calculerTotal(panier.getIdPanier());
            recap.append("─────────────────────────\n");
            recap.append("🧺 **").append(produitsAjoutes).append(" produit(s) ajouté(s) au panier**\n");
            recap.append("💰 **Total panier : ").append(String.format("%.2f DT", total)).append("**\n");
            recap.append("\nOuvrez votre panier pour finaliser la commande.");
        } else {
            recap = new StringBuilder("😔 Aucun produit n'a pu être ajouté au panier.\n\n");
            if (nonTrouves.length() > 0) recap.append(nonTrouves);
        }
        return recap.toString();
    }

    // -------------------------------------------------------------------------
    // Traitement DISPONIBILITÉ
    // -------------------------------------------------------------------------

    private String traiterDisponibilite(ChatIntent intent) {
        if (intent.getProduits() == null || intent.getProduits().isEmpty())
            return "❓ Quel produit souhaitez-vous vérifier ?";

        StringBuilder sb = new StringBuilder("📦 **Disponibilité des produits :**\n\n");
        for (ProduitDemande demande : intent.getProduits()) {
            String nomDemande = demande.getNom();
            List<Produit> resultats = produitService.searchProduits(nomDemande);
            if (resultats.isEmpty()) resultats = produitService.searchProduits(nomDemande.toLowerCase());
            if (resultats.isEmpty()) resultats = produitService.searchProduits(normaliserNom(nomDemande));

            if (resultats.isEmpty()) {
                sb.append("  ❌ **").append(nomDemande).append("** — non disponible dans le catalogue\n");
            } else {
                Produit meilleur = resultats.stream()
                        .max(Comparator.comparingInt(Produit::getQuantiteStock))
                        .orElse(resultats.get(0));
                if (meilleur.getQuantiteStock() > 0) {
                    sb.append("  ✅ **").append(meilleur.getNom())
                            .append("** — En stock (").append(meilleur.getQuantiteStock()).append(" disponibles)")
                            .append(" — ").append(String.format("%.2f DT", meilleur.getPrix())).append("\n");
                } else {
                    sb.append("  ⚠️ **").append(meilleur.getNom()).append("** — Rupture de stock\n");
                }
            }
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Sélection produit selon critère
    // -------------------------------------------------------------------------

    private Produit selectionnerProduit(List<Produit> produits, String critere) {
        if ("prix_asc".equals(critere))
            return produits.stream().min(Comparator.comparingDouble(Produit::getPrixApresRemise)).orElse(produits.get(0));
        else if ("prix_desc".equals(critere))
            return produits.stream().max(Comparator.comparingDouble(Produit::getPrixApresRemise)).orElse(produits.get(0));
        else if ("avis".equals(critere))
            return produits.stream().max(Comparator.comparingDouble(p -> avisService.getNoteMoyenne(p.getIdProduit()))).orElse(produits.get(0));
        else
            return produits.get(0);
    }

    // -------------------------------------------------------------------------
    // Traitement VIDER PANIER
    // -------------------------------------------------------------------------

    private String traiterViderPanier(int idUser) {
        Panier panier = panierService.getPanierActif(idUser);
        if (panier == null) return "❌ Impossible d'accéder à votre panier. Veuillez réessayer.";
        int nbProduits = panierService.compterProduits(panier.getIdPanier());
        if (nbProduits == 0) return "🛒 Votre panier est déjà vide.";
        boolean success = panierService.viderPanier(panier.getIdPanier());
        return success
                ? "✅ Votre panier a été vidé avec succès.\n" + nbProduits + " article(s) supprimé(s)."
                : "❌ Une erreur est survenue lors du vidage du panier. Veuillez réessayer.";
    }

    // -------------------------------------------------------------------------
    // Traitement SUPPRIMER_PRODUIT
    // -------------------------------------------------------------------------

    private String traiterSupprimerProduit(ChatIntent intent, int idUser) {
        if (intent.getProduits() == null || intent.getProduits().isEmpty())
            return "❓ Je n'ai pas identifié le produit à supprimer. Pouvez-vous préciser son nom ?";

        Panier panier = panierService.getPanierActif(idUser);
        if (panier == null) return "❌ Impossible d'accéder à votre panier. Veuillez réessayer.";

        List<PanierProduit> articlesPanier = panierService.getProduitsParPanier(panier.getIdPanier());
        if (articlesPanier.isEmpty()) return "🛒 Votre panier est déjà vide, il n'y a rien à supprimer.";

        StringBuilder resultat = new StringBuilder();
        int supprimes = 0;

        for (ProduitDemande demande : intent.getProduits()) {
            String nomRecherche = demande.getNom().toLowerCase().trim();
            PanierProduit articleTrouve = articlesPanier.stream()
                    .filter(pp -> pp.getProduit() != null &&
                            pp.getProduit().getNom().toLowerCase().contains(nomRecherche))
                    .findFirst().orElse(null);

            if (articleTrouve == null) {
                resultat.append("  ❌ **").append(demande.getNom()).append("** — non trouvé dans votre panier\n");
                continue;
            }

            boolean success = panierService.supprimerProduit(panier.getIdPanier(), articleTrouve.getIdProduit());
            if (success) {
                supprimes++;
                resultat.append("  ✅ **").append(articleTrouve.getProduit().getNom()).append("** — supprimé du panier\n");
            } else {
                resultat.append("  ❌ **").append(articleTrouve.getProduit().getNom()).append("** — erreur lors de la suppression\n");
            }
        }

        if (supprimes > 0) {
            float total = panierService.calculerTotal(panier.getIdPanier());
            int restants = panierService.compterProduits(panier.getIdPanier());
            return "🗑️ **Suppression effectuée :**\n\n" + resultat +
                    "\n─────────────────────────\n" +
                    "🧺 Articles restants : **" + restants + "**\n" +
                    "💰 Nouveau total : **" + String.format("%.2f DT", total) + "**";
        }
        return "😔 Aucun article n'a pu être supprimé.\n\n" + resultat;
    }

    // -------------------------------------------------------------------------
    // Normalisation des noms
    // -------------------------------------------------------------------------

    private String normaliserNom(String nom) {
        if (nom == null) return "";
        String n = nom.toLowerCase().trim();
        n = n.replace("apple", "appel").replace("tomato", "tomate").replace("tomatoes", "tomate")
                .replace("potato", "pomme de terre").replace("onion", "oignon").replace("carrot", "carotte")
                .replace("orange", "orange").replace("lemon", "citron").replace("garlic", "ail")
                .replace("pepper", "poivron").replace("wheat", "blé").replace("corn", "maïs")
                .replace("olive", "olive").replace("fig", "figue").replace("grape", "raisin")
                .replace("watermelon", "pastèque").replace("melon", "melon");
        return n;
    }
}