package tn.neuron.ardhi.models.UserAndDiag;

/**
 * Enum representing the severity/danger level of a plant disease diagnosis.
 * Used for map markers and epidemiological tracking.
 */
public enum Severity {
    /**
     * Critical severity - Disease is severe, requires urgent action.
     * Example: Late blight, Fire blight, Panama disease
     */
    CRITICAL("Critique", "#e74c3c"),

    /**
     * Medium severity - Moderate concern, treatment recommended.
     * Example: Powdery mildew, Leaf spot, Minor fungal infections
     */
    MEDIUM("Moyen", "#f39c12"),

    /**
     * Low severity - Minor issue or healthy plant.
     * Example: Nutrient deficiency, cosmetic damage, healthy plant
     */
    LOW("Faible", "#27ae60");

    private final String displayName;
    private final String colorHex;

    Severity(String displayName, String colorHex) {
        this.displayName = displayName;
        this.colorHex = colorHex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorHex() {
        return colorHex;
    }

    /**
     * Converts a string to Severity enum, with fallback to LOW.
     */
    public static Severity fromString(String value) {
        if (value == null || value.isEmpty()) {
            return LOW;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Handle legacy values like "HIGH"
            if ("HIGH".equalsIgnoreCase(value)) {
                return CRITICAL;
            }
            return LOW;
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}
