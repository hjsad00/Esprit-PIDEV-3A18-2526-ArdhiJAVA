package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.models.UserAndDiag.PreventionPlan;
import tn.neuron.ardhi.models.UserAndDiag.PreventionTask;
import tn.neuron.ardhi.models.UserAndDiag.TaskStatus;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds and sends WhatsApp prevention reminders.
 * Combines prevention plan data with weather-aware timing advice.
 */
public class PreventionReminderService {

    private final WhatsAppService whatsAppService = new WhatsAppService();
    private final WeatherAlertService weatherAlertService = new WeatherAlertService();
    private final LocationService locationService = new LocationService();

    /**
     * Sends a WhatsApp reminder for a prevention plan to the given phone number.
     *
     * @param plan        The prevention plan (must have tasks loaded)
     * @param tasks       The list of tasks for this plan
     * @param phoneNumber Full phone number with country code (e.g., "+21612345678")
     * @return true if the message was sent successfully
     */
    public boolean sendReminder(PreventionPlan plan, List<PreventionTask> tasks, String phoneNumber) {
        String message = buildReminderMessage(plan, tasks);
        return whatsAppService.sendWhatsAppMessage(phoneNumber, message);
    }

    /**
     * Builds a formatted WhatsApp reminder message from a prevention plan.
     * Includes vulnerability name, progress, pending tasks, and weather advice.
     */
    public String buildReminderMessage(PreventionPlan plan, List<PreventionTask> tasks) {
        StringBuilder msg = new StringBuilder();

        // Header
        msg.append("🛡️ *ARDHI — Rappel de Prévention*\n\n");

        // Vulnerability info
        String vulnerability = plan.getVulnerabilityThreat();
        if (vulnerability != null && !vulnerability.isEmpty()) {
            msg.append("⚠️ Risque: *").append(vulnerability).append("*\n");
        }
        String title = plan.getTitle();
        if (title != null && !title.isEmpty()) {
            msg.append("📋 Plan: *").append(title).append("*\n");
        }

        // Progress
        List<PreventionTask> pendingTasks = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.PENDING || t.getStatus() == TaskStatus.MISSED)
                .collect(Collectors.toList());

        List<PreventionTask> completedTasks = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                .collect(Collectors.toList());

        int total = tasks.size();
        int done = completedTasks.size();

        msg.append("📊 Progression: *").append(done).append("/").append(total).append("* tâches complétées\n");

        // Progress bar (visual emoji bar)
        int barLength = 10;
        int filled = total > 0 ? (done * barLength) / total : 0;
        msg.append("[");
        for (int i = 0; i < barLength; i++) {
            msg.append(i < filled ? "🟩" : "⬜");
        }
        msg.append("]\n\n");

        // Pending tasks
        if (pendingTasks.isEmpty()) {
            msg.append("✅ *Toutes les tâches préventives sont terminées !*\n");
            msg.append("Continuez de surveiller votre culture.\n");
        } else {
            msg.append("📋 *Tâches en attente:*\n");
            for (int i = 0; i < Math.min(pendingTasks.size(), 5); i++) {
                PreventionTask task = pendingTasks.get(i);
                String statusIcon = task.getStatus() == TaskStatus.MISSED ? "⚠️" : "⏳";
                msg.append(statusIcon).append(" Jour ").append(task.getDayOffset())
                        .append(": ").append(truncate(task.getTaskDescription(), 80)).append("\n");
            }
            if (pendingTasks.size() > 5) {
                msg.append("... et ").append(pendingTasks.size() - 5).append(" autre(s)\n");
            }
        }

        // Weather-aware treatment timing
        msg.append("\n");
        try {
            double lat = 36.8065; // Default
            double lon = 10.1815;
            LocationService.LocationData loc = locationService.detectLocation();
            if (loc != null && loc.latitude != null) {
                lat = loc.latitude;
                lon = loc.longitude;
            }

            WeatherAlertService.TreatmentTimingSuggestion timing = weatherAlertService.getTreatmentTiming(lat, lon);
            if (timing != null) {
                if (timing.rainWarning != null && !timing.rainWarning.isEmpty()) {
                    msg.append("🌧️ ").append(timing.rainWarning).append("\n");
                }
                if (timing.sprayWindow != null && !timing.sprayWindow.isEmpty()) {
                    msg.append("💨 ").append(timing.sprayWindow).append("\n");
                }
                if (timing.overallAdvice != null && !timing.overallAdvice.isEmpty()) {
                    msg.append("💡 ").append(timing.overallAdvice).append("\n");
                }
            }
        } catch (Exception e) {
            LogUtils.error(PreventionReminderService.class, "Failed to add weather context", e);
        }

        msg.append("\n— _Envoyé depuis Ardhi_ 🌿");
        return msg.toString();
    }

    /**
     * Truncates a string to the specified max length, appending "..." if truncated.
     */
    private String truncate(String text, int maxLen) {
        if (text == null)
            return "";
        if (text.length() <= maxLen)
            return text;
        return text.substring(0, maxLen - 3) + "...";
    }
}
