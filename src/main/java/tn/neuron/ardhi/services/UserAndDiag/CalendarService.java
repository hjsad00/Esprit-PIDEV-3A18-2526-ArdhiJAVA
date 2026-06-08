package tn.neuron.ardhi.services.UserAndDiag;

import com.calendarfx.model.Calendar;
import com.calendarfx.model.Calendar.Style;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import tn.neuron.ardhi.models.UserAndDiag.TaskStatus;
import tn.neuron.ardhi.models.UserAndDiag.TreatmentPlan;
import tn.neuron.ardhi.models.UserAndDiag.TreatmentTask;

import java.time.LocalDate;
import java.util.List;

public class CalendarService {

    private TreatmentPlanService planService = new TreatmentPlanService();
    private TreatmentTaskService taskService = new TreatmentTaskService();

    /**
     * Generates a CalendarSource containing all treatment plans for a user.
     */
    public CalendarSource getCalendarSourceForUser(int userId) {
        CalendarSource myCalendarSource = new CalendarSource("Mes Plantes");

        List<TreatmentPlan> plans = planService.getActivePlansByUser(userId);

        for (TreatmentPlan plan : plans) {
            Calendar planCalendar = new Calendar(plan.getInitialDiseaseName());
            // Default style, will be overridden by Controller
            planCalendar.setStyle(Style.STYLE1);
            // getActivePlansByUser() doesn't load tasks (optimization for list view),
            // so we must load them explicitly here for the calendar.
            List<TreatmentTask> tasks = taskService.getTasksForPlan(plan.getId());
            populateCalendar(planCalendar, tasks, plan);
            myCalendarSource.getCalendars().add(planCalendar);
        }

        return myCalendarSource;
    }

    private void populateCalendar(Calendar calendar, List<TreatmentTask> tasks, TreatmentPlan plan) {
        if (plan.getStartDate() == null)
            return;

        LocalDate planStart = plan.getStartDate().toLocalDateTime().toLocalDate();
        String diseaseName = plan.getInitialDiseaseName();

        for (TreatmentTask task : tasks) {
            LocalDate taskDate = planStart.plusDays(task.getDayOffset());

            Entry<String> entry = new Entry<>(diseaseName + ": " + task.getTaskDescription());
            entry.setInterval(taskDate);
            entry.setFullDay(true);

            if (task.getStatus() == TaskStatus.COMPLETED) {
                entry.setTitle("✅ " + entry.getTitle());
                entry.getStyleClass().add("entry-completed");
            } else if (task.getStatus() == TaskStatus.MISSED) {
                entry.setTitle("❌ " + entry.getTitle());
                entry.getStyleClass().add("entry-missed");
            }

            calendar.addEntry(entry);
        }
    }
}
