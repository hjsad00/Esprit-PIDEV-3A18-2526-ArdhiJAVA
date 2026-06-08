package tn.neuron.ardhi.interfaces.marketplace;

import tn.neuron.ardhi.models.marketplace.Commande;
import tn.neuron.ardhi.models.marketplace.DetailsCommande;
import tn.neuron.ardhi.models.marketplace.EtatCommande;

import java.util.List;

public interface ICommandeService {

    // Gestion des commandes
    boolean creerCommande(Commande commande);

    List<Commande> getCommandesByUser(int idUser);

    List<Commande> getAllCommandes();

    List<Commande> getCommandesByStatut(EtatCommande statut);

    // Commandes pour un vendeur (agriculteur) donné
    List<Commande> getCommandesByVendeur(int idVendeur);

    boolean updateStatutCommande(int idCommande, EtatCommande nouveauStatut);

    Commande getCommandeById(int idCommande);

    // Gestion des détails de commande
    boolean ajouterDetailsCommande(DetailsCommande detailsCommande);

    List<DetailsCommande> getDetailsByCommande(int idCommande);

    // Méthode utilitaire pour créer une ou plusieurs commandes à partir d'un panier
    // (une commande par vendeur)
    List<Commande> creerCommandeFromPanier(int idPanier, int idUser, String codeCoupon);

    boolean deleteCommande(int idCommande);

    boolean updateCommande(Commande commande);
}