package tn.neuron.ardhi.models.marketplace;

public class Produit {
    private int idProduit;
    private String nom;
    private String description;
    private float prix;
    private int quantiteStock;
    private String categorie;
    private int idUser;
    private UniteMesure uniteMesure;
    private String image;
    private float remise; // valeur de la remise (% ou DT)
    private TypeReduction typeRemise; // POURCENTAGE ou MONTANT_FIXE (null = aucune)

    // Constructeurs
    public Produit() {
    }

    public Produit(int idProduit, String nom, String description, float prix,
            int quantiteStock, String categorie, int idUser,
            UniteMesure uniteMesure, String image) {
        this.idProduit = idProduit;
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.quantiteStock = quantiteStock;
        this.categorie = categorie;
        this.idUser = idUser;
        this.uniteMesure = uniteMesure;
        this.image = image;
    }

    // ── Remise ──────────────────────────────────────────────────────────────

    /**
     * Calcule le prix final après application de la remise.
     * Si aucune remise n'est définie, retourne le prix original.
     */
    public float getPrixApresRemise() {
        if (typeRemise == null || remise <= 0)
            return prix;
        if (typeRemise == TypeReduction.POURCENTAGE) {
            return prix * (1f - remise / 100f);
        }
        // MONTANT_FIXE
        return Math.max(0, prix - remise);
    }

    /** Retourne true si une remise valide est configurée. */
    public boolean aUneRemise() {
        return typeRemise != null && remise > 0;
    }

    public float getRemise() {
        return remise;
    }

    public void setRemise(float remise) {
        this.remise = remise;
    }

    public TypeReduction getTypeRemise() {
        return typeRemise;
    }

    public void setTypeRemise(TypeReduction typeRemise) {
        this.typeRemise = typeRemise;
    }

    // ── Getters / Setters ──────────────────────────────────────────────────

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public UniteMesure getUniteMesure() {
        return uniteMesure;
    }

    public void setUniteMesure(UniteMesure uniteMesure) {
        this.uniteMesure = uniteMesure;
    }

    public int getIdProduit() {
        return idProduit;
    }

    public void setIdProduit(int idProduit) {
        this.idProduit = idProduit;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public float getPrix() {
        return prix;
    }

    public void setPrix(float prix) {
        this.prix = prix;
    }

    public int getQuantiteStock() {
        return quantiteStock;
    }

    public void setQuantiteStock(int quantiteStock) {
        this.quantiteStock = quantiteStock;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    @Override
    public String toString() {
        return "Produit{" +
                "idProduit=" + idProduit +
                ", nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                ", prix=" + prix +
                " DT" +
                ", remise=" + remise +
                ", typeRemise=" + typeRemise +
                ", quantiteStock=" + quantiteStock +
                ", categorie='" + categorie + '\'' +
                ", idUser=" + idUser +
                ", uniteMesure=" + uniteMesure +
                '}';
    }
}