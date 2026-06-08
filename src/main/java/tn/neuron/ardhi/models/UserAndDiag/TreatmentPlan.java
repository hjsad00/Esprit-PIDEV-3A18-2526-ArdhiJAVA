package tn.neuron.ardhi.models.UserAndDiag;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TreatmentPlan {
    private int id;
    private int diagnosticId;
    private Timestamp startDate;
    private PlanStatus status;
    private List<TreatmentTask> tasks; // List of tasks in this plan

    // Helpful for display (fetched via JOIN)
    private String initialDiseaseName;
    private String plantImageUrl;

    // Optimization: Pre-calculated counts
    private int totalTasks;
    private int completedTasks;

    public TreatmentPlan() {
        this.tasks = new ArrayList<>();
    }

    public TreatmentPlan(int diagnosticId, Timestamp startDate, PlanStatus status) {
        this.diagnosticId = diagnosticId;
        this.startDate = startDate;
        this.status = status;
        this.tasks = new ArrayList<>();
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

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public PlanStatus getStatus() {
        return status;
    }

    public void setStatus(PlanStatus status) {
        this.status = status;
    }

    public List<TreatmentTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<TreatmentTask> tasks) {
        this.tasks = tasks;
    }

    public void addTask(TreatmentTask task) {
        this.tasks.add(task);
    }

    public String getInitialDiseaseName() {
        return initialDiseaseName;
    }

    public void setInitialDiseaseName(String initialDiseaseName) {
        this.initialDiseaseName = initialDiseaseName;
    }

    public String getPlantImageUrl() {
        return plantImageUrl;
    }

    public void setPlantImageUrl(String plantImageUrl) {
        this.plantImageUrl = plantImageUrl;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }

}
