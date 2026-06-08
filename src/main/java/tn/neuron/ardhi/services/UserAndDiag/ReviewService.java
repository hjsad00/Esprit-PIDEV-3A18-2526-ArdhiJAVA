package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.models.UserAndDiag.*;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewService {

    private Connection cnx = MyDatabase.getInstance().getCnx();

    /**
     * Creates a new review request.
     */
    public int createReview(Review r) {
        String sql = "INSERT INTO review (diagnostic_id, treatment_plan_id, prevention_plan_id, review_type, status, photo_url, ai_analysis) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getDiagnosticId());
            if (r.getTreatmentPlanId() != null) {
                ps.setInt(2, r.getTreatmentPlanId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            if (r.getPreventionPlanId() != null) {
                ps.setInt(3, r.getPreventionPlanId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setString(4, r.getReviewType().name());
            ps.setString(5, r.getStatus().name());
            ps.setString(6, r.getPhotoUrl());
            ps.setString(7, r.getAiAnalysis());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LogUtils.error(ReviewService.class, "Error creating review", e);
        }
        return -1;
    }

    /**
     * Gets all pending/in-progress reviews for the expert dashboard.
     * Joins with diagnostic and user tables for display info.
     */
    public List<Review> getPendingReviews() {
        List<Review> reviews = new ArrayList<>();
        String sql = "SELECT r.*, d.resultat_ia, d.image_scannee, " +
                "COALESCE(CONCAT(u.prenom, ' ', u.nom), 'Agriculteur') AS farmer_name " +
                "FROM review r " +
                "LEFT JOIN diagnostic d ON r.diagnostic_id = d.id " +
                "LEFT JOIN `user` u ON d.user_id = u.id " +
                "WHERE r.status IN ('PENDING', 'IN_PROGRESS') " +
                "ORDER BY r.created_at DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                reviews.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            LogUtils.error(ReviewService.class, "Error fetching pending reviews", e);
        }
        return reviews;
    }

    /**
     * Gets all reviews for a specific diagnostic.
     */
    public List<Review> getReviewsForDiagnostic(int diagnosticId) {
        List<Review> reviews = new ArrayList<>();
        String sql = "SELECT r.*, d.resultat_ia, d.image_scannee, " +
                "CONCAT(u.prenom, ' ', u.nom) AS farmer_name " +
                "FROM review r " +
                "JOIN diagnostic d ON r.diagnostic_id = d.id " +
                "JOIN `user` u ON d.user_id = u.id " +
                "WHERE r.diagnostic_id = ? ORDER BY r.created_at DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, diagnosticId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                reviews.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            LogUtils.error(ReviewService.class, "Error fetching reviews for diagnostic", e);
        }
        return reviews;
    }

    /**
     * Check if a pending review already exists for a diagnostic.
     */
    public boolean hasPendingReview(int diagnosticId, ReviewType type) {
        String sql = "SELECT COUNT(*) FROM review WHERE diagnostic_id = ? AND review_type = ? AND status != 'COMPLETED'";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, diagnosticId);
            ps.setString(2, type.name());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LogUtils.error(ReviewService.class, "Error checking pending review", e);
        }
        return false;
    }

    /**
     * Expert claims a review (sets their ID and status to IN_PROGRESS).
     */
    public void claimReview(int reviewId, int expertId) {
        String sql = "UPDATE review SET expert_id = ?, status = 'IN_PROGRESS' WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, expertId);
            ps.setInt(2, reviewId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(ReviewService.class, "Error claiming review", e);
        }
    }

    /**
     * Expert completes a DIAGNOSIS review.
     */
    public void completeDiagnosisReview(int reviewId, int expertId, String expertNotes, String expertDiseaseName) {
        String sql = "UPDATE review SET expert_id = ?, status = 'COMPLETED', expert_notes = ?, " +
                "expert_disease_name = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, expertId);
            ps.setString(2, expertNotes);
            ps.setString(3, expertDiseaseName);
            ps.setInt(4, reviewId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(ReviewService.class, "Error completing diagnosis review", e);
        }
    }

    /**
     * Expert completes a PROGRESS review with a verdict.
     */
    public void completeProgressReview(int reviewId, int expertId, String expertNotes, ExpertVerdict verdict) {
        String sql = "UPDATE review SET expert_id = ?, status = 'COMPLETED', expert_notes = ?, " +
                "expert_verdict = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, expertId);
            ps.setString(2, expertNotes);
            ps.setString(3, verdict.name());
            ps.setInt(4, reviewId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(ReviewService.class, "Error completing progress review", e);
        }
    }

    /**
     * Gets a single review by ID with all join data.
     */
    public Review getReviewById(int reviewId) {
        String sql = "SELECT r.*, d.resultat_ia, d.image_scannee, " +
                "CONCAT(u.prenom, ' ', u.nom) AS farmer_name " +
                "FROM review r " +
                "JOIN diagnostic d ON r.diagnostic_id = d.id " +
                "JOIN `user` u ON d.user_id = u.id " +
                "WHERE r.id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        } catch (SQLException e) {
            LogUtils.error(ReviewService.class, "Error fetching review by ID", e);
        }
        return null;
    }

    /**
     * Gets the latest completed PROGRESS review for a treatment plan.
     * Used to show expert feedback to the farmer.
     */
    public Review getCompletedReviewForPlan(int planId) {
        String sql = "SELECT r.*, d.resultat_ia, d.image_scannee, " +
                "CONCAT(u.prenom, ' ', u.nom) AS farmer_name " +
                "FROM review r " +
                "JOIN diagnostic d ON r.diagnostic_id = d.id " +
                "JOIN `user` u ON d.user_id = u.id " +
                "WHERE r.treatment_plan_id = ? AND r.status = 'COMPLETED' AND r.review_type = 'PROGRESS' " +
                "ORDER BY r.updated_at DESC LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, planId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        } catch (SQLException e) {
            LogUtils.error(ReviewService.class, "Error fetching completed review for plan", e);
        }
        return null;
    }

    /**
     * Records the farmer's response to a review (ACCEPTED, REJECTED, ACKNOWLEDGED).
     * Optionally stores the AI's proposed plan text for audit.
     */
    public void updateFarmerResponse(int reviewId, FarmerResponse response, String aiProposedPlan) {
        String sql = "UPDATE review SET farmer_response = ?, ai_proposed_plan = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, response.name());
            if (aiProposedPlan != null) {
                ps.setString(2, aiProposedPlan);
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            ps.setInt(3, reviewId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(ReviewService.class, "Error updating farmer response", e);
        }
    }

    // ─── Prevention Plan Reviews ───

    /**
     * Gets the latest completed PROGRESS review for a prevention plan.
     */
    public Review getCompletedReviewForPreventionPlan(int preventionPlanId) {
        String sql = "SELECT * FROM review WHERE prevention_plan_id = ? AND status = 'COMPLETED' " +
                "AND review_type = 'PREVENTION' ORDER BY updated_at DESC LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, preventionPlanId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetSimple(rs);
            }
        } catch (SQLException e) {
            LogUtils.error(ReviewService.class, "Error fetching completed review for prevention plan", e);
        }
        return null;
    }

    /**
     * Check if a pending review already exists for a prevention plan.
     */
    public boolean hasPendingPreventionReview(int preventionPlanId) {
        String sql = "SELECT COUNT(*) FROM review WHERE prevention_plan_id = ? AND review_type = 'PREVENTION' AND status != 'COMPLETED'";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, preventionPlanId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LogUtils.error(ReviewService.class, "Error checking pending prevention review", e);
        }
        return false;
    }

    /**
     * Gets all reviews for a prevention plan (for before/after comparison).
     */
    public List<Review> getReviewsForPreventionPlan(int preventionPlanId) {
        List<Review> reviews = new ArrayList<>();
        String sql = "SELECT * FROM review WHERE prevention_plan_id = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, preventionPlanId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                reviews.add(mapResultSetSimple(rs));
            }
        } catch (SQLException e) {
            LogUtils.error(ReviewService.class, "Error fetching reviews for prevention plan", e);
        }
        return reviews;
    }

    // ─── Admin CRUD ───

    public List<Review> getAllReviews() {
        List<Review> reviews = new ArrayList<>();
        String sql = "SELECT r.*, d.resultat_ia, d.image_scannee, " +
                "CONCAT(u.prenom, ' ', u.nom) AS farmer_name " +
                "FROM review r " +
                "JOIN diagnostic d ON r.diagnostic_id = d.id " +
                "JOIN `user` u ON d.user_id = u.id " +
                "ORDER BY r.id DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                reviews.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            LogUtils.error(ReviewService.class, "Error fetching all reviews", e);
        }
        return reviews;
    }

    public List<Review> searchReviews(String keyword) {
        List<Review> reviews = new ArrayList<>();
        String sql = "SELECT r.*, d.resultat_ia, d.image_scannee, " +
                "CONCAT(u.prenom, ' ', u.nom) AS farmer_name " +
                "FROM review r " +
                "JOIN diagnostic d ON r.diagnostic_id = d.id " +
                "JOIN `user` u ON d.user_id = u.id " +
                "WHERE CAST(r.id AS CHAR) LIKE ? OR r.review_type LIKE ? " +
                "OR r.status LIKE ? OR COALESCE(r.expert_verdict,'') LIKE ? " +
                "OR COALESCE(r.expert_notes,'') LIKE ? " +
                "ORDER BY r.id DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            ps.setString(5, like);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                reviews.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            LogUtils.error(ReviewService.class, "Error searching reviews", e);
        }
        return reviews;
    }

    public void updateReview(Review r) {
        String sql = "UPDATE review SET diagnostic_id = ?, review_type = ?, status = ?, " +
                "expert_id = ?, expert_notes = ?, expert_verdict = ?, " +
                "treatment_plan_id = ?, farmer_response = ?, ai_proposed_plan = ? " +
                "WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, r.getDiagnosticId());
            ps.setString(2, r.getReviewType().name());
            ps.setString(3, r.getStatus().name());
            if (r.getExpertId() != null) {
                ps.setInt(4, r.getExpertId());
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }
            ps.setString(5, r.getExpertNotes());
            ps.setString(6, r.getExpertVerdict() != null ? r.getExpertVerdict().name() : null);
            if (r.getTreatmentPlanId() != null) {
                ps.setInt(7, r.getTreatmentPlanId());
            } else {
                ps.setNull(7, java.sql.Types.INTEGER);
            }
            ps.setString(8, r.getFarmerResponse() != null ? r.getFarmerResponse().name() : null);
            ps.setString(9, r.getAiProposedPlan());
            ps.setInt(10, r.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(ReviewService.class, "Error updating review", e);
        }
    }

    public void deleteReview(int reviewId) {
        String sql = "DELETE FROM review WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(ReviewService.class, "Error deleting review", e);
        }
    }

    private Review mapResultSet(ResultSet rs) throws SQLException {
        Review r = mapResultSetSimple(rs);
        // Join fields (only available when JOINing with diagnostic/user tables)
        r.setDiagnosisResult(rs.getString("resultat_ia"));
        r.setOriginalImageUrl(rs.getString("image_scannee"));
        r.setFarmerName(rs.getString("farmer_name"));
        return r;
    }

    /**
     * Maps a ResultSet without requiring diagnostic/user JOIN columns.
     * Used for prevention plan reviews that don't have a diagnostic.
     */
    private Review mapResultSetSimple(ResultSet rs) throws SQLException {
        Review r = new Review();
        r.setId(rs.getInt("id"));
        r.setDiagnosticId(rs.getInt("diagnostic_id"));

        int tPlanId = rs.getInt("treatment_plan_id");
        r.setTreatmentPlanId(rs.wasNull() ? null : tPlanId);

        int pPlanId = rs.getInt("prevention_plan_id");
        r.setPreventionPlanId(rs.wasNull() ? null : pPlanId);

        int expId = rs.getInt("expert_id");
        r.setExpertId(rs.wasNull() ? null : expId);

        r.setReviewType(ReviewType.valueOf(rs.getString("review_type")));
        r.setStatus(ReviewStatus.valueOf(rs.getString("status")));
        r.setPhotoUrl(rs.getString("photo_url"));
        r.setAiAnalysis(rs.getString("ai_analysis"));
        r.setExpertNotes(rs.getString("expert_notes"));

        String verdict = rs.getString("expert_verdict");
        r.setExpertVerdict(verdict != null ? ExpertVerdict.valueOf(verdict) : null);

        r.setExpertDiseaseName(rs.getString("expert_disease_name"));

        String farmerResp = rs.getString("farmer_response");
        r.setFarmerResponse(farmerResp != null ? FarmerResponse.valueOf(farmerResp) : null);

        r.setAiProposedPlan(rs.getString("ai_proposed_plan"));

        r.setCreatedAt(rs.getTimestamp("created_at"));
        r.setUpdatedAt(rs.getTimestamp("updated_at"));

        return r;
    }
}
