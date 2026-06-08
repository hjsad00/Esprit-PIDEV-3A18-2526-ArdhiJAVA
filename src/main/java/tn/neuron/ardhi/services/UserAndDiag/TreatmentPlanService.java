package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.models.UserAndDiag.*;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TreatmentPlanService {

    private Connection cnx = MyDatabase.getInstance().getCnx();
    private TreatmentTaskService taskService = new TreatmentTaskService();

    /**
     * Creates a Treatment Plan based on a diagnosis.
     */
    public void createRecoveryPlan(Diagnostic diagnostic, String diseaseName) {
        // 1. Create the main plan
        TreatmentPlan plan = new TreatmentPlan();
        plan.setDiagnosticId(diagnostic.getId());
        plan.setStatus(PlanStatus.ACTIVE);

        int planId = savePlan(plan);
        if (planId == -1)
            return;

        // 2. Delegate Task Generation to TaskService
        taskService.generateAndSaveTasks(planId, diseaseName);
    }

    public int savePlan(TreatmentPlan plan) {
        String req = "INSERT INTO treatment_plan (diagnostic_id, status, start_date) VALUES (?, ?, NOW())";
        try (PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, plan.getDiagnosticId());
            ps.setString(2, plan.getStatus().name());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LogUtils.error(TreatmentPlanService.class, "Error creating treatment plan", e);
        }
        return -1;
    }

    /**
     * Gets a plan by its ID with full task list.
     */
    public TreatmentPlan getPlanById(int planId) {
        String req = "SELECT tp.*, d.resultat_ia, d.image_scannee FROM treatment_plan tp " +
                "JOIN diagnostic d ON tp.diagnostic_id = d.id WHERE tp.id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, planId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                TreatmentPlan plan = new TreatmentPlan();
                plan.setId(rs.getInt("id"));
                plan.setDiagnosticId(rs.getInt("diagnostic_id"));
                plan.setStartDate(rs.getTimestamp("start_date"));
                plan.setStatus(PlanStatus.valueOf(rs.getString("status")));
                plan.setInitialDiseaseName(rs.getString("resultat_ia"));
                plan.setPlantImageUrl(rs.getString("image_scannee"));

                plan.setTasks(taskService.getTasksForPlan(plan.getId()));
                return plan;
            }
        } catch (SQLException e) {
            LogUtils.error(TreatmentPlanService.class, "Error fetching plan by ID", e);
        }
        return null;
    }

    public List<TreatmentPlan> getActivePlansByUser(int userId) {
        List<TreatmentPlan> plans = new ArrayList<>();
        // Optimized query: Fetch plan + task counts in one go (No N+1 problem)
        String req = "SELECT tp.*, d.resultat_ia, d.image_scannee, " +
                "COUNT(tt.id) as total_tasks, " +
                "SUM(CASE WHEN tt.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_custom " +
                "FROM treatment_plan tp " +
                "JOIN diagnostic d ON tp.diagnostic_id = d.id " +
                "LEFT JOIN treatment_task tt ON tp.id = tt.treatment_plan_id " +
                "WHERE d.user_id = ? AND tp.status IN ('ACTIVE', 'COMPLETED') " +
                "GROUP BY tp.id " +
                "ORDER BY tp.start_date DESC";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TreatmentPlan plan = new TreatmentPlan();
                plan.setId(rs.getInt("id"));
                plan.setDiagnosticId(rs.getInt("diagnostic_id"));
                plan.setStartDate(rs.getTimestamp("start_date"));
                plan.setStatus(PlanStatus.valueOf(rs.getString("status")));
                plan.setInitialDiseaseName(rs.getString("resultat_ia"));
                plan.setPlantImageUrl(rs.getString("image_scannee"));

                // Set pre-calculated counts
                plan.setTotalTasks(rs.getInt("total_tasks"));
                // Handle potential null from SUM if no tasks (though count handled it?)
                // getInt returns 0 for null if we don't check wasNull, which is fine here.
                plan.setCompletedTasks(rs.getInt("completed_custom"));

                // NOTE: We do NOT fetch the full task list here anymore.
                // The list view doesn't need it. Details view will fetch it separately.

                plans.add(plan);
            }
        } catch (SQLException e) {
            LogUtils.error(TreatmentPlanService.class, "Error fetching user plans", e);
        }
        return plans;
    }

    public void completePlan(int planId) {
        String req = "UPDATE treatment_plan SET status = 'COMPLETED' WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, planId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(TreatmentPlanService.class, "Error completing plan", e);
        }
    }

    public void abandonPlan(int planId) {
        String req = "UPDATE treatment_plan SET status = 'ABANDONED' WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, planId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(TreatmentPlanService.class, "Error abandoning plan", e);
        }
    }

    // ─── Admin CRUD ───

    public List<TreatmentPlan> getAllPlans() {
        List<TreatmentPlan> plans = new ArrayList<>();
        String req = "SELECT tp.*, d.resultat_ia, d.image_scannee FROM treatment_plan tp " +
                "JOIN diagnostic d ON tp.diagnostic_id = d.id ORDER BY tp.id DESC";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                plans.add(mapPlan(rs));
            }
        } catch (SQLException e) {
            LogUtils.error(TreatmentPlanService.class, "Error fetching all plans", e);
        }
        return plans;
    }

    public List<TreatmentPlan> searchPlans(String keyword) {
        List<TreatmentPlan> plans = new ArrayList<>();
        String req = "SELECT tp.*, d.resultat_ia, d.image_scannee FROM treatment_plan tp " +
                "JOIN diagnostic d ON tp.diagnostic_id = d.id " +
                "WHERE CAST(tp.id AS CHAR) LIKE ? OR tp.status LIKE ? OR d.resultat_ia LIKE ? " +
                "ORDER BY tp.id DESC";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                plans.add(mapPlan(rs));
            }
        } catch (SQLException e) {
            LogUtils.error(TreatmentPlanService.class, "Error searching plans", e);
        }
        return plans;
    }

    public void updatePlan(TreatmentPlan plan) {
        String req = "UPDATE treatment_plan SET diagnostic_id = ?, status = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, plan.getDiagnosticId());
            ps.setString(2, plan.getStatus().name());
            ps.setInt(3, plan.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(TreatmentPlanService.class, "Error updating plan", e);
        }
    }

    public void deletePlan(int planId) {
        // Delete tasks first (cascade)
        String delTasks = "DELETE FROM treatment_task WHERE treatment_plan_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(delTasks)) {
            ps.setInt(1, planId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(TreatmentPlanService.class, "Error deleting plan tasks", e);
        }
        // Delete reviews linked to this plan
        String delReviews = "DELETE FROM review WHERE treatment_plan_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(delReviews)) {
            ps.setInt(1, planId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(TreatmentPlanService.class, "Error deleting plan reviews", e);
        }
        // Delete the plan
        String req = "DELETE FROM treatment_plan WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, planId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(TreatmentPlanService.class, "Error deleting plan", e);
        }
    }

    private TreatmentPlan mapPlan(ResultSet rs) throws SQLException {
        TreatmentPlan plan = new TreatmentPlan();
        plan.setId(rs.getInt("id"));
        plan.setDiagnosticId(rs.getInt("diagnostic_id"));
        plan.setStartDate(rs.getTimestamp("start_date"));
        plan.setStatus(PlanStatus.valueOf(rs.getString("status")));
        plan.setInitialDiseaseName(rs.getString("resultat_ia"));
        plan.setPlantImageUrl(rs.getString("image_scannee"));

        return plan;
    }
}
