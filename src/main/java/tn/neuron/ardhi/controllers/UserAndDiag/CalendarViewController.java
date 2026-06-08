package tn.neuron.ardhi.controllers.UserAndDiag;

import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import com.calendarfx.view.CalendarView;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class CalendarViewController implements Initializable {

    @FXML
    private BorderPane calendarContainer;

    @FXML
    private HBox topControls;

    private CalendarView calendarView;
    private tn.neuron.ardhi.services.UserAndDiag.CalendarService calendarService = new tn.neuron.ardhi.services.UserAndDiag.CalendarService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        calendarView = new CalendarView();
        calendarView.setShowDeveloperConsole(false);
        calendarView.setShowPrintButton(false);
        calendarView.setShowAddCalendarButton(false);

        // Add a custom toggle for completed tasks
        CheckBox hideCompleted = new CheckBox("Masquer les tâches terminées");
        hideCompleted.setSelected(false);
        hideCompleted.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-margin-left: 10;");
        hideCompleted.selectedProperty().addListener((obs, oldVal, newVal) -> loadCalendarData(newVal));

        if (topControls != null) {
            topControls.getChildren().add(hideCompleted);
        }

        calendarContainer.setCenter(calendarView);
        loadCalendarData(false);
    }

    private void loadCalendarData(boolean hideCompleted) {
        calendarView.getCalendarSources().clear();
        int userId = UserSession.getInstance().getUser().getId();

        // Delegate to Service
        CalendarSource source = calendarService.getCalendarSourceForUser(userId);

        // --- FILTERING & STYLING LOGIC ---
        for (Calendar cal : source.getCalendars()) {
            // 1. Color Coding based on Disease/Crop Name
            String name = cal.getName().toLowerCase();
            if (name.contains("tomate")) {
                cal.setStyle(Calendar.Style.STYLE1); // Red-ish
            } else if (name.contains("banane") || name.contains("blé") || name.contains("wheat")) {
                cal.setStyle(Calendar.Style.STYLE2); // Yellow-ish
            } else {
                cal.setStyle(Calendar.Style.STYLE4); // Green-ish/Other
            }

            // 2. Filter Completed Tasks
            if (hideCompleted) {
                List<Entry<?>> toRemove = new ArrayList<>();
                cal.findEntries("").forEach(entry -> {
                    // Service adds "✅" to completed tasks
                    if (entry.getTitle().contains("✅")) {
                        toRemove.add(entry);
                    }
                });
                cal.removeEntries(toRemove);
            }
        }

        calendarView.getCalendarSources().setAll(source);
        calendarView.setToday(LocalDate.now());
        calendarView.setTime(LocalTime.now());
    }

    @FXML
    void goBack(javafx.event.ActionEvent event) {
        try {
            WindowUtils.loadScene(event, "/fxml/UserAndDiag/TreatmentPlanList.fxml", "Mes Protocoles de Soin");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
