package tn.neuron.ardhi.models.UserAndDiag;

public class TreatmentTask {
    private int id;
    private int treatmentPlanId;
    private int dayOffset; // 1, 3, 10, etc.
    private String taskDescription;
    private TaskStatus status;

    private double techX; // 3D/Image X coordinate
    private double techY; // 3D/Image Y coordinate

    public TreatmentTask() {
    }

    public TreatmentTask(int treatmentPlanId, int dayOffset, String taskDescription, TaskStatus status) {
        this.treatmentPlanId = treatmentPlanId;
        this.dayOffset = dayOffset;
        this.taskDescription = taskDescription;
        this.status = status;
    }

    public TreatmentTask(int treatmentPlanId, int dayOffset, String taskDescription, TaskStatus status, double techX,
            double techY) {
        this.treatmentPlanId = treatmentPlanId;
        this.dayOffset = dayOffset;
        this.taskDescription = taskDescription;
        this.status = status;
        this.techX = techX;
        this.techY = techY;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTreatmentPlanId() {
        return treatmentPlanId;
    }

    public void setTreatmentPlanId(int treatmentPlanId) {
        this.treatmentPlanId = treatmentPlanId;
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

    public double getTechX() {
        return techX;
    }

    public void setTechX(double techX) {
        this.techX = techX;
    }

    public double getTechY() {
        return techY;
    }

    public void setTechY(double techY) {
        this.techY = techY;
    }
}
