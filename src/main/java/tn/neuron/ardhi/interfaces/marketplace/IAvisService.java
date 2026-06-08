package tn.neuron.ardhi.interfaces.marketplace;

import tn.neuron.ardhi.models.marketplace.Avis;
import java.util.List;

public interface IAvisService {

    boolean ajouterAvis(Avis avis);

    List<Avis> getAvisByProduit(int idProduit);

    double getNoteMoyenne(int idProduit);

    int getNombreAvis(int idProduit);
}
