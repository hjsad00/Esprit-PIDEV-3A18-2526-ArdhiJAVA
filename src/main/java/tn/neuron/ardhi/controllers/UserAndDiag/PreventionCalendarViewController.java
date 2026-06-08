package tn.neuron.ardhi.controllers.UserAndDiag;

import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import com.calendarfx.view.CalendarView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import tn.neuron.ardhi.services.UserAndDiag.PreventionCalendarService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class PreventionCalendarViewController implements Initializable {

    @FXML
    private BorderPane calendarContainer;

    @FXML
    private HBox topControls;

    private CalendarView calendarView;
    private PreventionCalendarService calendarService = new PreventionCalendarService();

    // If set, load plans for this specific report; otherwise load all user plans
    private static int reportIdToLoad = -1;
    private static int scanIdForBack = -1;

    /** Call before loading FXML to set the report context */
    public static void setReportContext(int reportId, int scanId) {
        reportIdToLoad = reportId;
        scanIdForBack = scanId;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        calendarView = new CalendarView();
        calendarView.setShowDeveloperConsole(false);
        calendarView.setShowPrintButton(false);
        calendarView.setShowAddCalendarButton(false);

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

        CalendarSource source;
        if (reportIdToLoad > 0) {
            source = calendarService.getCalendarSourceForReport(reportIdToLoad);
        } else {
            int userId = UserSession.getInstance().getUser().getId();
            source = calendarService.getCalendarSourceForUser(userId);
        }

        if (hideCompleted) {
            for (Calendar cal : source.getCalendars()) {
                List<Entry<?>> toRemove = new ArrayList<>();
                cal.findEntries("").forEach(entry -> {
                    if (entry.getTitle().contains("\u2705")) {
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
            if (scanIdForBack > 0) {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/UserAndDiag/ReportVulnerabilities.fxml"));
                Parent root = loader.load();
                ReportVulnerabilitiesController ctrl = loader.getController();
                ctrl.initData(scanIdForBack);
                calendarContainer.getScene().setRoot(root);
            } else {
                calendarContainer.getScene().setRoot(
                        FXMLLoader.load(getClass().getResource("/fxml/UserAndDiag/ClientDashboard.fxml")));
            }
            // Reset static state
            reportIdToLoad = -1;
            scanIdForBack = -1;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
