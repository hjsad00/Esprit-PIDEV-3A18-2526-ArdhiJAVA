package tn.neuron.ardhi.models.UserAndDiag;

import java.sql.Timestamp;

public class PreventionTask {
    private int id;
    private int preventionPlanId;
    private int dayOffset; // same pattern as TreatmentTask
    private String taskDescription;
    private TaskStatus status; // reuses PENDING/COMPLETED/MISSED
    private String proofPhotoUrl; // optional ImgBB URL
    private Timestamp completedAt;

    public PreventionTask() {
        this.status = TaskStatus.PENDING;
    }

    public PreventionTask(int preventionPlanId, int dayOffset, String taskDescription) {
        this.preventionPlanId = preventionPlanId;
        this.dayOffset = dayOffset;
        this.taskDescription = taskDescription;
        this.status = TaskStatus.PENDING;
    }

    // Getters & Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPreventionPlanId() {
        return preventionPlanId;
    }

    public void setPreventionPlanId(int preventionPlanId) {
        this.preventionPlanId = preventionPlanId;
    }

    public int getDayOffset() {
        return dayOffset;
    }

    public void setDayOffset(int dayOffset) {
        this.dayOffset = dayOffset;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getProofPhotoUrl() {
        return proofPhotoUrl;
    }

    public void setProofPhotoUrl(String proofPhotoUrl) {
        this.proofPhotoUrl = proofPhotoUrl;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }
}
