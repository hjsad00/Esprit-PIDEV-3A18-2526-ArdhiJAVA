package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.models.UserAndDiag.*;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TreatmentTaskService {

    private Connection cnx = MyDatabase.getInstance().getCnx();
    private GroqService groqService = new GroqService();

    public void generateAndSaveTasks(int planId, String diseaseName) {
        List<TreatmentTask> tasks = generateTasksForDisease(planId, diseaseName);
        for (TreatmentTask task : tasks) {
            saveTask(task);
        }
    }

    public void saveTask(TreatmentTask task) {

        String req = "INSERT INTO treatment_task (treatment_plan_id, day_offset, task_description, status, tech_x, tech_y) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, task.getTreatmentPlanId());
            ps.setInt(2, task.getDayOffset());
            ps.setString(3, task.getTaskDescription());
            ps.setString(4, task.getStatus().name());
            ps.setDouble(5, task.getTechX());
            ps.setDouble(6, task.getTechY());
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(TreatmentTaskService.class, "Error saving treatment task", e);
        }
    }

    public List<TreatmentTask> getTasksForPlan(int planId) {
        List<TreatmentTask> tasks = new ArrayList<>();
        String req = "SELECT * FROM treatment_task WHERE treatment_plan_id = ? ORDER BY day_offset ASC";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, planId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tasks.add(mapTask(rs));
            }
        } catch (SQLException e) {
            LogUtils.error(TreatmentTaskService.class, "Error fetching tasks", e);
        }
        return tasks;
    }

    public void updateTaskStatus(int taskId, TaskStatus status) {
        String query = "UPDATE treatment_task SET status = ? WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setString(1, status.name());
            pst.setInt(2, taskId);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isReadyForReview(int planId) {
        String req = "SELECT status FROM treatment_task WHERE treatment_plan_id = ? AND day_offset = 10";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            LogUtils.error(TreatmentTaskService.class, "Error checking review status", e);
        }
        return false;
    }

    public void updateRemainingTasks(int planId, List<TreatmentTask> newTasks) {
        // 1. Delete all PENDING tasks for this plan
        String deleteReq = "DELETE FROM treatment_task WHERE treatment_plan_id = ? AND status = 'PENDING'";
        try (PreparedStatement ps = cnx.prepareStatement(deleteReq)) {
            ps.setInt(1, planId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(TreatmentTaskService.class, "Error deleting pending tasks", e);
        }

        // 2. Insert new tasks
        for (TreatmentTask task : newTasks) {
            saveTask(task);
        }
    }

    // --- Expert Task Management ---

    /**
     * Delete a specific task (used by expert).
     */
    public void deleteTask(int taskId) {
        String sql = "DELETE FROM treatment_task WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(TreatmentTaskService.class, "Error deleting task", e);
        }
    }

    /**
     * Update a task's description (used by expert).
     */
    public void updateTaskDescription(int taskId, String newDescription) {
        String sql = "UPDATE treatment_task SET task_description = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, newDescription);
            ps.setInt(2, taskId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(TreatmentTaskService.class, "Error updating task description", e);
        }
    }

    private List<TreatmentTask> generateTasksForDisease(int planId, String diseaseName) {
        List<TreatmentTask> tasks = new ArrayList<>();

        // 1. Try to fetch from AI
        String aiResponse = groqService.generateTreatmentPlan(diseaseName);
        boolean aiSuccess = false;

        if (aiResponse != null && !aiResponse.startsWith("ERREUR")) {
            // Clean up Markdown code blocks if present
            aiResponse = aiResponse.replaceAll("```(\\w+)?", "").trim();

            // Split by literal \n or real newline
            String[] lines = aiResponse.split("\\r?\\n");

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty())
                    continue;

                // Expected format: DAY|DESCRIPTION
                if (line.contains("|")) {
                    String[] parts = line.split("\\|", 2);
                    try {
                        int day = Integer.parseInt(parts[0].trim());
                        String desc = parts[1].trim();
                        // Remove any leading "- " or "* " bullet points commonly added by LLMs
                        desc = desc.replaceAll("^[-*]\\s+", "");

                        tasks.add(new TreatmentTask(planId, day, desc, TaskStatus.PENDING));
                        aiSuccess = true;
                    } catch (NumberFormatException e) {
                        LogUtils.warn(TreatmentTaskService.class, "Skipping malformed task line: " + line);
                    }
                }
            }
        }

        // 2. Fallback
        if (!aiSuccess || tasks.isEmpty()) {
            tasks = generateFallbackTasks(planId, diseaseName);
        }

        return tasks;
    }

    // ─── Admin CRUD ───

    public List<TreatmentTask> getAllTasks() {
        List<TreatmentTask> tasks = new ArrayList<>();
        String req = "SELECT * FROM treatment_task ORDER BY treatment_plan_id DESC, day_offset ASC";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tasks.add(mapTask(rs));
            }
        } catch (SQLException e) {
            LogUtils.error(TreatmentTaskService.class, "Error fetching all tasks", e);
        }
        return tasks;
    }

    public List<TreatmentTask> searchTasks(String keyword) {
        List<TreatmentTask> tasks = new ArrayList<>();
        String req = "SELECT * FROM treatment_task WHERE " +
                "CAST(id AS CHAR) LIKE ? OR CAST(treatment_plan_id AS CHAR) LIKE ? " +
                "OR task_description LIKE ? OR status LIKE ? " +
                "ORDER BY treatment_plan_id DESC, day_offset ASC";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tasks.add(mapTask(rs));
            }
        } catch (SQLException e) {
            LogUtils.error(TreatmentTaskService.class, "Error searching tasks", e);
        }
        return tasks;
    }

    public void updateTask(TreatmentTask task) {
        String req = "UPDATE treatment_task SET treatment_plan_id = ?, day_offset = ?, " +
                "task_description = ?, status = ?, tech_x = ?, tech_y = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, task.getTreatmentPlanId());
            ps.setInt(2, task.getDayOffset());
            ps.setString(3, task.getTaskDescription());
            ps.setString(4, task.getStatus().name());
            ps.setDouble(5, task.getTechX());
            ps.setDouble(6, task.getTechY());
            ps.setInt(7, task.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtils.error(TreatmentTaskService.class, "Error updating task", e);
        }
    }

    private TreatmentTask mapTask(ResultSet rs) throws SQLException {
        TreatmentTask task = new TreatmentTask();
        task.setId(rs.getInt("id"));
        task.setTreatmentPlanId(rs.getInt("treatment_plan_id"));
        task.setDayOffset(rs.getInt("day_offset"));
        task.setTaskDescription(rs.getString("task_description"));
        task.setStatus(TaskStatus.valueOf(rs.getString("status")));
        try {
            task.setTechX(rs.getDouble("tech_x"));
            task.setTechY(rs.getDouble("tech_y"));
        } catch (SQLException e) {
            // Column might not exist
        }
        return task;
    }

    private List<TreatmentTask> generateFallbackTasks(int planId, String diseaseName) {
        List<TreatmentTask> tasks = new ArrayList<>();
        String lowerName = diseaseName.toLowerCase();

        String day1Action = "Retirez les feuilles visiblement infectées.";
        if (lowerName.contains("mildiou") || lowerName.contains("blight")) {
            day1Action = "Coupez toutes les parties noircies et brûlez-les (ne pas composter).";
        } else if (lowerName.contains("oidium") || lowerName.contains("powdery mildew")) {
            day1Action = "Essuyez les feuilles avec un chiffon humide et aérez la plante.";
        }
        tasks.add(new TreatmentTask(planId, 1, day1Action, TaskStatus.PENDING));

        String day3Action = "Appliquez le traitement recommandé (voir détail diagnostic).";
        if (lowerName.contains("fongique") || lowerName.contains("fungal")) {
            day3Action = "Appliquez un fongicide à base de cuivre ou de soufre.";
        } else if (lowerName.contains("insect")) {
            day3Action = "Pulvérisez une solution de savon noir ou d'huile de neem.";
        }
        tasks.add(new TreatmentTask(planId, 3, day3Action, TaskStatus.PENDING));

        tasks.add(new TreatmentTask(planId, 7,
                "Inspectez la plante. Si les symptômes persistent, répétez le traitement.", TaskStatus.PENDING));

        return tasks;
    }
}
