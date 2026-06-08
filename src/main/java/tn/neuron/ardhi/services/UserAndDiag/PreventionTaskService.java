package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.models.UserAndDiag.PreventionTask;
import tn.neuron.ardhi.models.UserAndDiag.TaskStatus;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PreventionTaskService {

    private Connection cnx = MyDatabase.getInstance().getCnx();

    /**
     * Saves a single task and returns the generated ID.
     */
    public int save(PreventionTask task) {
        String sql = "INSERT INTO prevention_task (prevention_plan_id, day_offset, task_description, status) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, task.getPreventionPlanId());
            pst.setInt(2, task.getDayOffset());
            pst.setString(3, task.getTaskDescription());
            pst.setString(4, task.getStatus().name());
            pst.executeUpdate();

            ResultSet keys = pst.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                task.setId(id);
                return id;
            }
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error saving prevention task", e);
        }
        return -1;
    }

    /**
     * Saves multiple tasks for a plan.
     */
    public void saveAll(List<PreventionTask> tasks, int planId) {
        for (PreventionTask task : tasks) {
            task.setPreventionPlanId(planId);
            save(task);
        }
    }

    /**
     * Gets all tasks for a plan, ordered by day offset.
     */
    public List<PreventionTask> getByPlan(int planId) {
        List<PreventionTask> tasks = new ArrayList<>();
        String sql = "SELECT * FROM prevention_task WHERE prevention_plan_id = ? ORDER BY day_offset ASC";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, planId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                tasks.add(mapTask(rs));
            }
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error getting tasks for plan", e);
        }
        return tasks;
    }

    /**
     * Updates a task's status (mirrors TreatmentTaskService.updateTaskStatus).
     */
    public void updateTaskStatus(int taskId, TaskStatus status) {
        String sql = "UPDATE prevention_task SET status = ? WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, status.name());
            pst.setInt(2, taskId);
            pst.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error updating prevention task status", e);
        }
    }

    /**
     * Replaces all PENDING tasks with new ones (for AI reevaluation).
     */
    public void updateRemainingTasks(int planId, List<PreventionTask> newTasks) {
        // 1. Delete all PENDING tasks for this plan
        String deleteReq = "DELETE FROM prevention_task WHERE prevention_plan_id = ? AND status = 'PENDING'";
        try (PreparedStatement ps = cnx.prepareStatement(deleteReq)) {
            ps.setInt(1, planId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error deleting pending prevention tasks", e);
        }
        // 2. Insert new tasks
        for (PreventionTask task : newTasks) {
            task.setPreventionPlanId(planId);
            save(task);
        }
    }

    /**
     * Marks a task as completed with optional proof photo.
     */
    public void completeTask(int taskId, String proofPhotoUrl) {
        String sql = "UPDATE prevention_task SET status = 'COMPLETED', proof_photo_url = ?, " +
                "completed_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, proofPhotoUrl);
            pst.setInt(2, taskId);
            pst.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error completing task", e);
        }
    }

    /**
     * Gets the progress percentage for a plan (0-100).
     */
    public int getProgress(int planId) {
        String sql = "SELECT COUNT(*) as total, " +
                "SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as done " +
                "FROM prevention_task WHERE prevention_plan_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, planId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                int total = rs.getInt("total");
                int done = rs.getInt("done");
                if (total == 0)
                    return 0;
                return (done * 100) / total;
            }
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error getting plan progress", e);
        }
        return 0;
    }

    // ─── Admin CRUD ───

    public List<PreventionTask> getAllTasks() {
        List<PreventionTask> tasks = new ArrayList<>();
        String sql = "SELECT * FROM prevention_task ORDER BY prevention_plan_id DESC, day_offset ASC";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                tasks.add(mapTask(rs));
            }
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error fetching all prevention tasks", e);
        }
        return tasks;
    }

    public List<PreventionTask> searchTasks(String keyword) {
        List<PreventionTask> tasks = new ArrayList<>();
        String sql = "SELECT * FROM prevention_task WHERE " +
                "CAST(id AS CHAR) LIKE ? OR CAST(prevention_plan_id AS CHAR) LIKE ? " +
                "OR task_description LIKE ? OR status LIKE ? " +
                "ORDER BY prevention_plan_id DESC, day_offset ASC";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            String like = "%" + keyword + "%";
            pst.setString(1, like);
            pst.setString(2, like);
            pst.setString(3, like);
            pst.setString(4, like);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                tasks.add(mapTask(rs));
            }
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error searching prevention tasks", e);
        }
        return tasks;
    }

    public void updateTask(PreventionTask task) {
        String sql = "UPDATE prevention_task SET prevention_plan_id = ?, day_offset = ?, " +
                "task_description = ?, status = ? WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, task.getPreventionPlanId());
            pst.setInt(2, task.getDayOffset());
            pst.setString(3, task.getTaskDescription());
            pst.setString(4, task.getStatus().name());
            pst.setInt(5, task.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error updating prevention task", e);
        }
    }

    /**
     * Delete a specific task (used by expert).
     */
    public void deleteTask(int taskId) {
        String sql = "DELETE FROM prevention_task WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, taskId);
            pst.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error deleting prevention task", e);
        }
    }

    /**
     * Update a task's description (used by expert).
     */
    public void updateTaskDescription(int taskId, String newDescription) {
        String sql = "UPDATE prevention_task SET task_description = ? WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, newDescription);
            pst.setInt(2, taskId);
            pst.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error updating prevention task description", e);
        }
    }

    private PreventionTask mapTask(ResultSet rs) throws SQLException {
        PreventionTask task = new PreventionTask();
        task.setId(rs.getInt("id"));
        task.setPreventionPlanId(rs.getInt("prevention_plan_id"));
        task.setDayOffset(rs.getInt("day_offset"));
        task.setTaskDescription(rs.getString("task_description"));

        String statusStr = rs.getString("status");
        try {
            task.setStatus(TaskStatus.valueOf(statusStr));
        } catch (Exception e) {
            task.setStatus(TaskStatus.PENDING);
        }

        task.setProofPhotoUrl(rs.getString("proof_photo_url"));
        task.setCompletedAt(rs.getTimestamp("completed_at"));
        return task;
    }
}
