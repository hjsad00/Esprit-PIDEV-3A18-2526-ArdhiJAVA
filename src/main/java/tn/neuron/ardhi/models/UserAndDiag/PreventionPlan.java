package tn.neuron.ardhi.models.UserAndDiag;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PreventionPlan {
    private int id;
    private int reportId;
    private int vulnerabilityId;
    private String title;
    private String problemSummary;
    private String steps; // pipe-delimited steps text
    private int timelineDays;
    private float estimatedCost;
    private String expectedOutcome;
    private String impactLevel; // HIGH, MEDIUM, LOW
    private PlanStatus status; // reuses ACTIVE/COMPLETED/ABANDONED
    private Date startDate;
    private Timestamp createdAt;

    // Child tasks (populated via service)
    private List<PreventionTask> tasks;

    // Display helpers
    private String vulnerabilityThreat;
    private int totalTasks;
    private int completedTasks;

    public PreventionPlan() {
        this.status = PlanStatus.ACTIVE;
        this.tasks = new ArrayList<>();
    }

    public PreventionPlan(int reportId, int vulnerabilityId, String title, String problemSummary) {
        this.reportId = reportId;
        this.vulnerabilityId = vulnerabilityId;
        this.title = title;
        this.problemSummary = problemSummary;
        this.status = PlanStatus.ACTIVE;
        this.tasks = new ArrayList<>();
    }

    // Getters & Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getReportId() {
        return reportId;
    }

    public void setReportId(int reportId) {
        this.reportId = reportId;
    }

    public int getVulnerabilityId() {
        return vulnerabilityId;
    }

    public void setVulnerabilityId(int vulnerabilityId) {
        this.vulnerabilityId = vulnerabilityId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getProblemSummary() {
        return problemSummary;
    }

    public void setProblemSummary(String problemSummary) {
        this.problemSummary = problemSummary;
    }

    public String getSteps() {
        return steps;
    }

    public void setSteps(String steps) {
        this.steps = steps;
    }

    public int getTimelineDays() {
        return timelineDays;
    }

    public void setTimelineDays(int timelineDays) {
        this.timelineDays = timelineDays;
    }

    public float getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(float estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public String getExpectedOutcome() {
        return expectedOutcome;
    }

    public void setExpectedOutcome(String expectedOutcome) {
        this.expectedOutcome = expectedOutcome;
    }

    public String getImpactLevel() {
        return impactLevel;
    }

    public void setImpactLevel(String impactLevel) {
        this.impactLevel = impactLevel;
    }

    public PlanStatus getStatus() {
        return status;
    }

    public void setStatus(PlanStatus status) {
        this.status = status;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public List<PreventionTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<PreventionTask> tasks) {
        this.tasks = tasks;
    }

    public void addTask(PreventionTask task) {
        this.tasks.add(task);
    }

    public String getVulnerabilityThreat() {
        return vulnerabilityThreat;
    }

    public void setVulnerabilityThreat(String vulnerabilityThreat) {
        this.vulnerabilityThreat = vulnerabilityThreat;
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
