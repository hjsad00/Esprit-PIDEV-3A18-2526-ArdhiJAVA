package tn.neuron.ardhi.services.UserAndDiag;

import com.calendarfx.model.Calendar;
import com.calendarfx.model.Calendar.Style;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import tn.neuron.ardhi.models.UserAndDiag.PreventionPlan;
import tn.neuron.ardhi.models.UserAndDiag.PreventionTask;
import tn.neuron.ardhi.models.UserAndDiag.TaskStatus;

import java.time.LocalDate;
import java.util.List;

public class PreventionCalendarService {

    private PreventionPlanService planService = new PreventionPlanService();
    private PreventionTaskService taskService = new PreventionTaskService();

    private static final Style[] STYLES = {
            Style.STYLE1, Style.STYLE2, Style.STYLE3, Style.STYLE4,
            Style.STYLE5, Style.STYLE6, Style.STYLE7
    };

    /**
     * Generates a CalendarSource for ALL prevention plans of a specific scan
     * report.
     */
    public CalendarSource getCalendarSourceForReport(int reportId) {
        CalendarSource source = new CalendarSource("Prévention");
        List<PreventionPlan> plans = planService.getByReport(reportId);
        int idx = 0;
        for (PreventionPlan plan : plans) {
            String calName = plan.getTitle() != null ? plan.getTitle() : "Plan #" + plan.getId();
            Calendar cal = new Calendar(calName);
            cal.setStyle(STYLES[idx % STYLES.length]);
            idx++;
            List<PreventionTask> tasks = taskService.getByPlan(plan.getId());
            populateCalendar(cal, tasks, plan);
            source.getCalendars().add(cal);
        }
        return source;
    }

    /**
     * Generates a CalendarSource for ALL active prevention plans of a user.
     */
    public CalendarSource getCalendarSourceForUser(int userId) {
        CalendarSource source = new CalendarSource("Prévention");
        List<PreventionPlan> plans = planService.getActivePlansByUser(userId);
        int idx = 0;
        for (PreventionPlan plan : plans) {
            String calName = plan.getTitle() != null ? plan.getTitle() : "Plan #" + plan.getId();
            Calendar cal = new Calendar(calName);
            cal.setStyle(STYLES[idx % STYLES.length]);
            idx++;
            List<PreventionTask> tasks = taskService.getByPlan(plan.getId());
            populateCalendar(cal, tasks, plan);
            source.getCalendars().add(cal);
        }
        return source;
    }

    private void populateCalendar(Calendar calendar, List<PreventionTask> tasks, PreventionPlan plan) {
        // Use startDate if available, otherwise fall back to createdAt
        LocalDate planStart;
        if (plan.getStartDate() != null) {
            planStart = plan.getStartDate().toLocalDate();
        } else if (plan.getCreatedAt() != null) {
            planStart = plan.getCreatedAt().toLocalDateTime().toLocalDate();
        } else {
            return; // No date at all, skip
        }
        String planTitle = plan.getTitle() != null ? plan.getTitle() : "Prévention";

        for (PreventionTask task : tasks) {
            LocalDate taskDate = planStart.plusDays(task.getDayOffset());

            Entry<String> entry = new Entry<>(planTitle + ": " + task.getTaskDescription());
            entry.setInterval(taskDate);
            entry.setFullDay(true);

            if (task.getStatus() == TaskStatus.COMPLETED) {
                entry.setTitle("\u2705 " + entry.getTitle());
                entry.getStyleClass().add("entry-completed");
            } else if (task.getStatus() == TaskStatus.MISSED) {
                entry.setTitle("\u274C " + entry.getTitle());
                entry.getStyleClass().add("entry-missed");
            }

            calendar.addEntry(entry);
        }
    }
}
