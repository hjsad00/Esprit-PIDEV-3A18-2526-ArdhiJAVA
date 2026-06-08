package tn.neuron.ardhi.models.Parcelle_Cultures;

import java.util.List;

/**
 * Indicateurs statistiques globaux pour l'administrateur
 * (vue plateforme sur les parcelles et cultures).
 */
public class AdminAgricultureStats {

    private double surfaceTotalePlateforme;   // ha
    private int nbAgriculteursActifs;
    private double productionGlobale;         // tonnes
    private double rendementMoyenGlobal;      // t/ha

    private List<TopCulture> topCultures;     // Top 5 par fréquence

    public static class TopCulture {
        private String nom;
        private String type;
        private long frequence;

        public String getNom() {
            return nom;
        }

        public void setNom(String nom) {
            this.nom = nom;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public long getFrequence() {
            return frequence;
        }

        public void setFrequence(long frequence) {
            this.frequence = frequence;
        }
    }

    public double getSurfaceTotalePlateforme() {
        return surfaceTotalePlateforme;
    }

    public void setSurfaceTotalePlateforme(double surfaceTotalePlateforme) {
        this.surfaceTotalePlateforme = surfaceTotalePlateforme;
    }

    public int getNbAgriculteursActifs() {
        return nbAgriculteursActifs;
    }

    public void setNbAgriculteursActifs(int nbAgriculteursActifs) {
        this.nbAgriculteursActifs = nbAgriculteursActifs;
    }

    public double getProductionGlobale() {
        return productionGlobale;
    }

    public void setProductionGlobale(double productionGlobale) {
        this.productionGlobale = productionGlobale;
    }

    public double getRendementMoyenGlobal() {
        return rendementMoyenGlobal;
    }

    public void setRendementMoyenGlobal(double rendementMoyenGlobal) {
        this.rendementMoyenGlobal = rendementMoyenGlobal;
    }

    public List<TopCulture> getTopCultures() {
        return topCultures;
    }

    public void setTopCultures(List<TopCulture> topCultures) {
        this.topCultures = topCultures;
    }
}

