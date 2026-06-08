package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.models.UserAndDiag.*;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PreventionPlanService {

    private Connection cnx = MyDatabase.getInstance().getCnx();
    private PreventionTaskService taskService = new PreventionTaskService();

    /**
     * Saves a prevention plan and returns the generated ID.
     */
    public int save(PreventionPlan plan) {
        String sql = "INSERT INTO prevention_plan (report_id, vulnerability_id, title, problem_summary, " +
                "steps, timeline_days, estimated_cost, expected_outcome, impact_level, status, start_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURDATE())";
        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, plan.getReportId());
            if (plan.getVulnerabilityId() > 0)
                pst.setInt(2, plan.getVulnerabilityId());
            else
                pst.setNull(2, Types.INTEGER);
            pst.setString(3, plan.getTitle());
            pst.setString(4, plan.getProblemSummary());
            pst.setString(5, plan.getSteps());
            pst.setInt(6, plan.getTimelineDays());
            pst.setFloat(7, plan.getEstimatedCost());
            pst.setString(8, plan.getExpectedOutcome());
            pst.setString(9, plan.getImpactLevel());
            pst.setString(10, plan.getStatus().name());
            // start_date auto-set to CURDATE() in SQL
            pst.executeUpdate();

            ResultSet keys = pst.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                plan.setId(id);
                return id;
            }
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error saving prevention plan", e);
        }
        return -1;
    }

    /**
     * Gets a plan by ID with its tasks.
     */
    public PreventionPlan getById(int planId) {
        String sql = "SELECT pp.*, v.threat as vulnerability_threat FROM prevention_plan pp " +
                "LEFT JOIN vulnerability v ON pp.vulnerability_id = v.id WHERE pp.id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, planId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                PreventionPlan plan = mapPlan(rs);
                plan.setTasks(taskService.getByPlan(planId));
                return plan;
            }
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error getting prevention plan by ID", e);
        }
        return null;
    }

    /**
     * Gets all plans for a report.
     */
    public List<PreventionPlan> getByReport(int reportId) {
        List<PreventionPlan> plans = new ArrayList<>();
        String sql = "SELECT pp.*, v.threat as vulnerability_threat, " +
                "(SELECT COUNT(*) FROM prevention_task pt WHERE pt.prevention_plan_id = pp.id) as total_tasks, " +
                "(SELECT COUNT(*) FROM prevention_task pt WHERE pt.prevention_plan_id = pp.id AND pt.status = 'COMPLETED') as completed_tasks "
                +
                "FROM prevention_plan pp LEFT JOIN vulnerability v ON pp.vulnerability_id = v.id " +
                "WHERE pp.report_id = ? ORDER BY pp.created_at";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, reportId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                PreventionPlan plan = mapPlan(rs);
                plan.setTotalTasks(rs.getInt("total_tasks"));
                plan.setCompletedTasks(rs.getInt("completed_tasks"));
                plans.add(plan);
            }
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error getting plans for report", e);
        }
        return plans;
    }

    /**
     * Gets active prevention plans for a user (across all their scan reports).
     */
    public List<PreventionPlan> getActivePlansByUser(int userId) {
        List<PreventionPlan> plans = new ArrayList<>();
        String sql = "SELECT pp.*, v.threat as vulnerability_threat, " +
                "(SELECT COUNT(*) FROM prevention_task pt WHERE pt.prevention_plan_id = pp.id) as total_tasks, " +
                "(SELECT COUNT(*) FROM prevention_task pt WHERE pt.prevention_plan_id = pp.id AND pt.status = 'COMPLETED') as completed_tasks "
                +
                "FROM prevention_plan pp " +
                "LEFT JOIN vulnerability v ON pp.vulnerability_id = v.id " +
                "JOIN farm_health_report fhr ON pp.report_id = fhr.id " +
                "JOIN farm_health_scan fhs ON fhr.scan_id = fhs.id " +
                "WHERE fhs.user_id = ? AND pp.status = 'ACTIVE' ORDER BY pp.created_at DESC";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                PreventionPlan plan = mapPlan(rs);
                plan.setTotalTasks(rs.getInt("total_tasks"));
                plan.setCompletedTasks(rs.getInt("completed_tasks"));
                plans.add(plan);
            }
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error getting active plans for user", e);
        }
        return plans;
    }

    // startPlan removed — plans auto-start at creation with start_date = CURDATE()

    /**
     * Marks a plan as completed.
     */
    public void completePlan(int planId) {
        String sql = "UPDATE prevention_plan SET status = 'COMPLETED' WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, planId);
            pst.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error completing plan", e);
        }
    }

    /**
     * Marks a plan as abandoned.
     */
    public void abandonPlan(int planId) {
        String sql = "UPDATE prevention_plan SET status = 'ABANDONED' WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, planId);
            pst.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error abandoning plan", e);
        }
    }

    // ─── Admin CRUD ───

    public List<PreventionPlan> getAllPlans() {
        List<PreventionPlan> plans = new ArrayList<>();
        String sql = "SELECT pp.*, v.threat as vulnerability_threat FROM prevention_plan pp " +
                "LEFT JOIN vulnerability v ON pp.vulnerability_id = v.id ORDER BY pp.id DESC";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                plans.add(mapPlan(rs));
            }
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error fetching all prevention plans", e);
        }
        return plans;
    }

    public List<PreventionPlan> searchPlans(String keyword) {
        List<PreventionPlan> plans = new ArrayList<>();
        String sql = "SELECT pp.*, v.threat as vulnerability_threat FROM prevention_plan pp " +
                "LEFT JOIN vulnerability v ON pp.vulnerability_id = v.id " +
                "WHERE CAST(pp.id AS CHAR) LIKE ? OR pp.status LIKE ? OR pp.title LIKE ? " +
                "OR v.threat LIKE ? ORDER BY pp.id DESC";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            String like = "%" + keyword + "%";
            pst.setString(1, like);
            pst.setString(2, like);
            pst.setString(3, like);
            pst.setString(4, like);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                plans.add(mapPlan(rs));
            }
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error searching prevention plans", e);
        }
        return plans;
    }

    public void updatePlan(PreventionPlan plan) {
        String sql = "UPDATE prevention_plan SET report_id = ?, vulnerability_id = ?, title = ?, " +
                "problem_summary = ?, steps = ?, timeline_days = ?, estimated_cost = ?, " +
                "expected_outcome = ?, impact_level = ?, status = ? WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, plan.getReportId());
            if (plan.getVulnerabilityId() > 0)
                pst.setInt(2, plan.getVulnerabilityId());
            else
                pst.setNull(2, Types.INTEGER);
            pst.setString(3, plan.getTitle());
            pst.setString(4, plan.getProblemSummary());
            pst.setString(5, plan.getSteps());
            pst.setInt(6, plan.getTimelineDays());
            pst.setFloat(7, plan.getEstimatedCost());
            pst.setString(8, plan.getExpectedOutcome());
            pst.setString(9, plan.getImpactLevel());
            pst.setString(10, plan.getStatus().name());
            pst.setInt(11, plan.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error updating prevention plan", e);
        }
    }

    public void deletePlan(int planId) {
        // Delete tasks first (cascade)
        String delTasks = "DELETE FROM prevention_task WHERE prevention_plan_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(delTasks)) {
            pst.setInt(1, planId);
            pst.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error deleting prevention plan tasks", e);
        }
        // Delete reviews linked to this plan
        String delReviews = "DELETE FROM review WHERE prevention_plan_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(delReviews)) {
            pst.setInt(1, planId);
            pst.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error deleting prevention plan reviews", e);
        }
        // Delete the plan
        String sql = "DELETE FROM prevention_plan WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, planId);
            pst.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error deleting prevention plan", e);
        }
    }

    private PreventionPlan mapPlan(ResultSet rs) throws SQLException {
        PreventionPlan plan = new PreventionPlan();
        plan.setId(rs.getInt("id"));
        plan.setReportId(rs.getInt("report_id"));
        int vulnId = rs.getInt("vulnerability_id");
        if (!rs.wasNull())
            plan.setVulnerabilityId(vulnId);
        plan.setTitle(rs.getString("title"));
        plan.setProblemSummary(rs.getString("problem_summary"));
        plan.setSteps(rs.getString("steps"));
        plan.setTimelineDays(rs.getInt("timeline_days"));
        plan.setEstimatedCost(rs.getFloat("estimated_cost"));
        plan.setExpectedOutcome(rs.getString("expected_outcome"));
        plan.setImpactLevel(rs.getString("impact_level"));

        String statusStr = rs.getString("status");
        try {
            plan.setStatus(PlanStatus.valueOf(statusStr));
        } catch (Exception e) {
            plan.setStatus(PlanStatus.ACTIVE);
        }

        plan.setStartDate(rs.getDate("start_date"));
        plan.setCreatedAt(rs.getTimestamp("created_at"));

        // Display helper
        try {
            plan.setVulnerabilityThreat(rs.getString("vulnerability_threat"));
        } catch (SQLException ignored) {
        }

        return plan;
    }
}
