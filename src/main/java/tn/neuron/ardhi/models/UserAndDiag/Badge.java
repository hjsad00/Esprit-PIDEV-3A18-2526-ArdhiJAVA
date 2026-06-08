package tn.neuron.ardhi.models.UserAndDiag;

public class Badge {
    private int id;
    private String name;
    private String description;
    private String icon;
    private BadgeType conditionType; // e.g., DIAGNOSTIC, POINTS
    private int threshold;

    public Badge() {
        // Default constructor
    }

    public Badge(int id, String name, String description, String icon, BadgeType conditionType, int threshold) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.conditionType = conditionType;
        this.threshold = threshold;
    }

    public Badge(String name, String description, String icon, BadgeType conditionType, int threshold) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.conditionType = conditionType;
        this.threshold = threshold;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public BadgeType getConditionType() {
        return conditionType;
    }

    public void setConditionType(BadgeType conditionType) {
        this.conditionType = conditionType;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }
}
