package tn.neuron.ardhi.models.UserAndDiag;

/**
 * Tracks the farmer's decision after receiving AI or expert feedback.
 */
public enum FarmerResponse {
    /**
     * Farmer accepted the recommendation (applied new plan / terminated protocol)
     */
    ACCEPTED,
    /**
     * Farmer rejected the recommendation (kept original plan / continued despite
     * HEALED)
     */
    REJECTED,
    /**
     * Farmer acknowledged the feedback without a binary accept/reject (e.g.
     * CONTINUE or WORSENED verdicts)
     */
    ACKNOWLEDGED
}
