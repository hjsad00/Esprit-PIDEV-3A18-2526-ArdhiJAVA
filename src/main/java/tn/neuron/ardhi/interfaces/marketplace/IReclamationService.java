package tn.neuron.ardhi.interfaces.marketplace;

import tn.neuron.ardhi.models.marketplace.Reclamation;
import tn.neuron.ardhi.models.marketplace.StatutReclamation;
import java.util.List;

public interface IReclamationService {

    boolean creerReclamation(Reclamation reclamation);

    List<Reclamation> getReclamationsByUser(int idUser);

    List<Reclamation> getReclamationsByProduit(int idProduit);

    List<Reclamation> getAllReclamations();

    List<Reclamation> getReclamationsByStatut(StatutReclamation statut);

    boolean updateStatut(int idReclamation, StatutReclamation nouveauStatut);

    boolean supprimerReclamation(int idReclamation);
}