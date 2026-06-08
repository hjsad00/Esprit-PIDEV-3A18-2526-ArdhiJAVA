package tn.neuron.ardhi.controllers.Evenement;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tn.neuron.ardhi.models.Evenement.Evenement;
import tn.neuron.ardhi.services.Evenement.EvenementService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CalendrierController implements Initializable {

    @FXML private Label monthYearLabel;
    @FXML private GridPane calendarGrid;
    @FXML private VBox eventDetailsBox;
    @FXML private Label selectedDateLabel;
    @FXML private VBox eventsList;

    private EvenementService evenementService;
    private YearMonth currentYearMonth;
    private LocalDate selectedDate;
    private Map<LocalDate, List<Evenement>> eventsMap;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        evenementService = new EvenementService();
        currentYearMonth = YearMonth.now();
        selectedDate = LocalDate.now();
        loadEvents();
        updateCalendar();
        showEventsForDate(selectedDate);
    }

    private void loadEvents() {
        eventsMap = new HashMap<>();
        for (Evenement event : evenementService.getAllEvenements()) {
            LocalDate current = event.getDateDebut();
            while (!current.isAfter(event.getDateFin())) {
                eventsMap.computeIfAbsent(current, k -> new ArrayList<>()).add(event);
                current = current.plusDays(1);
            }
        }
    }

    private void updateCalendar() {
        calendarGrid.getChildren().clear();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
        monthYearLabel.setText(currentYearMonth.format(formatter).toUpperCase());

        String[] daysOfWeek = {"L", "M", "M", "J", "V", "S", "D"};
        for (int i = 0; i < 7; i++) {
            Label dayLabel = new Label(daysOfWeek[i]);
            dayLabel.setStyle(
                    "-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #6B7F3F;" +
                            "-fx-background-color: #F8F9FA; -fx-background-radius: 8; -fx-padding: 8;");
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            dayLabel.setAlignment(Pos.CENTER);
            calendarGrid.add(dayLabel, i, 0);
        }

        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue();
        LocalDate date = firstOfMonth.minusDays(dayOfWeek - 1);

        for (int row = 1; row < 7; row++) {
            for (int col = 0; col < 7; col++) {
                calendarGrid.add(createDayCell(date), col, row);
                date = date.plusDays(1);
            }
        }
    }

    private VBox createDayCell(LocalDate date) {
        VBox cell = new VBox(4);
        cell.setAlignment(Pos.TOP_CENTER);
        cell.setPrefSize(80, 62);   // ← compact
        cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        boolean isCurrentMonth = date.getMonth() == currentYearMonth.getMonth();
        boolean isToday        = date.equals(LocalDate.now());
        boolean isSelected     = date.equals(selectedDate);

        String baseStyle =
                "-fx-border-color: " + (isSelected ? "#9B59B6" : "#E8E8E8") + "; " +
                        "-fx-border-width: " + (isSelected ? "2.5" : "1") + "; " +
                        "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 6; ";

        String bgStyle = isSelected    ? "-fx-background-color: rgba(155,89,182,0.1);"
                : isToday       ? "-fx-background-color: rgba(52,152,219,0.1);"
                : !isCurrentMonth ? "-fx-background-color: #FAFAFA;"
                : "-fx-background-color: white;";

        cell.setStyle(baseStyle + bgStyle);

        Label dayNumber = new Label(String.valueOf(date.getDayOfMonth()));
        dayNumber.setStyle(
                "-fx-font-size: " + (isToday ? "14px" : "13px") + "; " +
                        "-fx-font-weight: " + (isToday || isSelected ? "bold" : "normal") + "; " +
                        "-fx-text-fill: " + (isCurrentMonth ? "#2C3E50" : "#AAA") + ";");
        cell.getChildren().add(dayNumber);

        List<Evenement> dayEvents = eventsMap.getOrDefault(date, Collections.emptyList());
        if (!dayEvents.isEmpty()) {
            int maxDisplay = 2, displayed = 0;
            for (Evenement event : dayEvents) {
                if (displayed >= maxDisplay) {
                    Label more = new Label("+" + (dayEvents.size() - maxDisplay));
                    more.setStyle("-fx-font-size: 9px; -fx-text-fill: #666; -fx-font-weight: bold;");
                    cell.getChildren().add(more);
                    break;
                }
                Region badge = new Region();
                badge.setPrefSize(32, 3);
                badge.setMaxWidth(Double.MAX_VALUE);
                badge.setStyle("-fx-background-color: " + getEventColor(event.getStatut()) + "; -fx-background-radius: 2;");
                cell.getChildren().add(badge);
                displayed++;
            }
        }

        LocalDate cellDate = date;
        cell.setOnMouseClicked(e -> { selectedDate = cellDate; updateCalendar(); showEventsForDate(cellDate); });
        cell.setOnMouseEntered(e -> { if (!cellDate.equals(selectedDate)) cell.setStyle(baseStyle + "-fx-background-color: #F0F0F5; -fx-cursor: hand;"); });
        cell.setOnMouseExited(e  -> { if (!cellDate.equals(selectedDate)) cell.setStyle(baseStyle + bgStyle); });

        return cell;
    }

    private void showEventsForDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH);
        String fd = date.format(formatter);
        selectedDateLabel.setText(fd.substring(0,1).toUpperCase() + fd.substring(1));
        eventsList.getChildren().clear();

        List<Evenement> dayEvents = eventsMap.getOrDefault(date, Collections.emptyList());
        if (dayEvents.isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setStyle("-fx-padding: 24;");
            Label emoji = new Label("📭"); emoji.setStyle("-fx-font-size: 40px;");
            Label msg = new Label("Aucun événement ce jour");
            msg.setStyle("-fx-text-fill: #999; -fx-font-style: italic; -fx-font-size: 13px;");
            empty.getChildren().addAll(emoji, msg);
            eventsList.getChildren().add(empty);
            return;
        }
        for (Evenement event : dayEvents) eventsList.getChildren().add(createEventCard(event));
    }

    private VBox createEventCard(Evenement event) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-padding: 12; -fx-background-radius: 12;" +
                "-fx-border-color: #E8E8E8; -fx-border-radius: 12; -fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 6, 0, 0, 2);");

        Label title = new Label(event.getTitre());
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        title.setWrapText(true);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Label dateL = new Label("📅 " + event.getDateDebut().format(fmt));
        dateL.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        Label lieuL = new Label("📍 " + event.getLieu());
        lieuL.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        HBox badges = new HBox(6);
        Label typeL = new Label(event.getType());
        typeL.setStyle("-fx-background-color: rgba(52,152,219,0.15); -fx-text-fill: #3498DB;" +
                "-fx-padding: 3 8; -fx-background-radius: 8; -fx-font-size: 10px; -fx-font-weight: bold;");
        Label statL = new Label(getStatutLabel(event.getStatut()));
        statL.setStyle("-fx-background-color: " + getEventColor(event.getStatut()) + "; -fx-text-fill: white;" +
                "-fx-padding: 3 8; -fx-background-radius: 8; -fx-font-size: 10px; -fx-font-weight: bold;");
        badges.getChildren().addAll(typeL, statL);

        card.getChildren().addAll(title, dateL, lieuL, badges);
        return card;
    }

    private String getEventColor(String statut) {
        return switch (statut) {
            case "A_VENIR"  -> "#50C878";
            case "EN_COURS" -> "#F39C12";
            case "TERMINE"  -> "#95A5A6";
            case "ANNULE"   -> "#E74C3C";
            default         -> "#3498DB";
        };
    }

    private String getStatutLabel(String statut) {
        return switch (statut) {
            case "A_VENIR"  -> "À venir";
            case "EN_COURS" -> "En cours";
            case "TERMINE"  -> "Terminé";
            case "ANNULE"   -> "Annulé";
            default         -> statut;
        };
    }

    @FXML private void handlePreviousMonth() { currentYearMonth = currentYearMonth.minusMonths(1); loadEvents(); updateCalendar(); }
    @FXML private void handleNextMonth()     { currentYearMonth = currentYearMonth.plusMonths(1);  loadEvents(); updateCalendar(); }
    @FXML private void handleToday()         { currentYearMonth = YearMonth.now(); selectedDate = LocalDate.now(); loadEvents(); updateCalendar(); showEventsForDate(selectedDate); }
    @FXML private void handleRefresh()       { loadEvents(); updateCalendar(); showEventsForDate(selectedDate); }

    @FXML
    private void handleRetour(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Evenement/NavigationEvenements.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, WindowUtils.APP_WIDTH, WindowUtils.APP_HEIGHT));
            stage.setTitle("Ardhi - Module Événements");
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }
}