package tn.neuron.ardhi.models.UserAndDiag;

public class Traitement {
    private int id;
    private int diagnosticId;
    private String solutionNom;
    private String descriptionDetaillee;
    private TypeTraitement typeTraitement;

    public Traitement() {
    }

    public Traitement(int diagnosticId, String solutionNom, String descriptionDetaillee,
            TypeTraitement typeTraitement) {
        this.diagnosticId = diagnosticId;
        this.solutionNom = solutionNom;
        this.descriptionDetaillee = descriptionDetaillee;
        this.typeTraitement = typeTraitement;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDiagnosticId() {
        return diagnosticId;
    }

    public void setDiagnosticId(int diagnosticId) {
        this.diagnosticId = diagnosticId;
    }

    public String getSolutionNom() {
        return solutionNom;
    }

    public void setSolutionNom(String solutionNom) {
        this.solutionNom = solutionNom;
    }

    public String getDescriptionDetaillee() {
        return descriptionDetaillee;
    }

    public void setDescriptionDetaillee(String descriptionDetaillee) {
        this.descriptionDetaillee = descriptionDetaillee;
    }

    public TypeTraitement getTypeTraitement() {
        return typeTraitement;
    }

    public void setTypeTraitement(TypeTraitement typeTraitement) {
        this.typeTraitement = typeTraitement;
    }
}