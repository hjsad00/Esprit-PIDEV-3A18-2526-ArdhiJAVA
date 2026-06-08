package tn.neuron.ardhi.models.UserAndDiag;

import java.sql.Timestamp;

public class Review {
    private int id;
    private int diagnosticId;
    private Integer treatmentPlanId; // nullable — only for PROGRESS reviews
    private Integer preventionPlanId; // nullable — for prevention plan PROGRESS reviews
    private Integer expertId; // nullable — assigned when expert picks it up
    private ReviewType reviewType;
    private ReviewStatus status;
    private String photoUrl; // follow-up photo for progress reviews
    private String aiAnalysis; // AI's analysis for progress reviews
    private String expertNotes; // expert's written feedback
    private ExpertVerdict expertVerdict; // CONTINUE, HEALED, WORSENED (progress only)
    private String expertDiseaseName; // expert-corrected disease name (diagnosis only)
    private FarmerResponse farmerResponse; // farmer's decision on the feedback
    private String aiProposedPlan; // AI's proposed plan text (preserved even if rejected)
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Display helpers (filled via JOIN)
    private String farmerName;
    private String diagnosisResult;
    private String originalImageUrl;

    public Review() {
    }

    public Review(int diagnosticId, ReviewType reviewType) {
        this.diagnosticId = diagnosticId;
        this.reviewType = reviewType;
        this.status = ReviewStatus.PENDING;
    }

    // --- Getters & Setters ---

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

    public Integer getTreatmentPlanId() {
        return treatmentPlanId;
    }

    public void setTreatmentPlanId(Integer treatmentPlanId) {
        this.treatmentPlanId = treatmentPlanId;
    }

    public Integer getPreventionPlanId() {
        return preventionPlanId;
    }

    public void setPreventionPlanId(Integer preventionPlanId) {
        this.preventionPlanId = preventionPlanId;
    }

    public Integer getExpertId() {
        return expertId;
    }

    public void setExpertId(Integer expertId) {
        this.expertId = expertId;
    }

    public ReviewType getReviewType() {
        return reviewType;
    }

    public void setReviewType(ReviewType reviewType) {
        this.reviewType = reviewType;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getAiAnalysis() {
        return aiAnalysis;
    }

    public void setAiAnalysis(String aiAnalysis) {
        this.aiAnalysis = aiAnalysis;
    }

    public String getExpertNotes() {
        return expertNotes;
    }

    public void setExpertNotes(String expertNotes) {
        this.expertNotes = expertNotes;
    }

    public ExpertVerdict getExpertVerdict() {
        return expertVerdict;
    }

    public void setExpertVerdict(ExpertVerdict expertVerdict) {
        this.expertVerdict = expertVerdict;
    }

    public String getExpertDiseaseName() {
        return expertDiseaseName;
    }

    public void setExpertDiseaseName(String expertDiseaseName) {
        this.expertDiseaseName = expertDiseaseName;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    // --- Display helpers ---

    public String getFarmerName() {
        return farmerName;
    }

    public void setFarmerName(String farmerName) {
        this.farmerName = farmerName;
    }

    public String getDiagnosisResult() {
        return diagnosisResult;
    }

    public void setDiagnosisResult(String diagnosisResult) {
        this.diagnosisResult = diagnosisResult;
    }

    public String getOriginalImageUrl() {
        return originalImageUrl;
    }

    public void setOriginalImageUrl(String originalImageUrl) {
        this.originalImageUrl = originalImageUrl;
    }

    public FarmerResponse getFarmerResponse() {
        return farmerResponse;
    }

    public void setFarmerResponse(FarmerResponse farmerResponse) {
        this.farmerResponse = farmerResponse;
    }

    public String getAiProposedPlan() {
        return aiProposedPlan;
    }

    public void setAiProposedPlan(String aiProposedPlan) {
        this.aiProposedPlan = aiProposedPlan;
    }

    @Override
    public String toString() {
        String label = reviewType == ReviewType.DIAGNOSIS ? "Diagnostic" : "Suivi";
        String farmer = farmerName != null ? farmerName : "Agriculteur #" + diagnosticId;
        return "[" + label + "] " + farmer + " — " + status.name();
    }
}
