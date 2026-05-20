package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.service.WorkshopService;
import com.project.artconnect.util.ConnectionManager;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class WorkshopController {

    @FXML private TableView<Workshop> workshopTable;
    @FXML private TableColumn<Workshop, String> titleColumn;
    @FXML private TableColumn<Workshop, LocalDateTime> dateColumn;
    @FXML private TableColumn<Workshop, String> instructorColumn;
    @FXML private TableColumn<Workshop, Double> priceColumn;
    @FXML private TableColumn<Workshop, String> levelColumn;
    @FXML private TableColumn<Workshop, String> participantsColumn;

    private final WorkshopService workshopService =
            ServiceProvider.getWorkshopService();
    private final ArtistService artistService =
            ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(
                new PropertyValueFactory<>("title"));
        dateColumn.setCellValueFactory(
                new PropertyValueFactory<>("date"));
        priceColumn.setCellValueFactory(
                new PropertyValueFactory<>("price"));
        levelColumn.setCellValueFactory(
                new PropertyValueFactory<>("level"));
        instructorColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getInstructor() != null
                                ? cellData.getValue().getInstructor()
                                .getName()
                                : "Unknown"));
        participantsColumn.setCellValueFactory(cellData -> {
            Workshop w = cellData.getValue();
            int booked = getParticipantCount(w.getTitle());
            int max = w.getMaxParticipants();
            return new SimpleStringProperty(
                    booked + " / " + max);
        });
        refreshTable();
    }

    // ─── ADD ────────────────────────────────────────────────
    @FXML
    private void handleAdd() {
        Dialog<Workshop> dialog = buildWorkshopDialog(null);
        Optional<Workshop> result = dialog.showAndWait();
        result.ifPresent(workshop -> {
            saveWorkshop(workshop);
            refreshTable();
            showInfo("Workshop \"" + workshop.getTitle()
                    + "\" added successfully.");
        });
    }

    // ─── EDIT ───────────────────────────────────────────────
    @FXML
    private void handleEdit() {
        Workshop selected = workshopTable.getSelectionModel()
                .getSelectedItem();
        if (selected == null) {
            showWarning("Please select a workshop to edit.");
            return;
        }
        Dialog<Workshop> dialog =
                buildWorkshopDialog(selected);
        Optional<Workshop> result = dialog.showAndWait();
        result.ifPresent(workshop -> {
            updateWorkshop(workshop);
            refreshTable();
            showInfo("Workshop updated successfully.");
        });
    }

    // ─── DELETE ─────────────────────────────────────────────
    @FXML
    private void handleDelete() {
        Workshop selected = workshopTable.getSelectionModel()
                .getSelectedItem();
        if (selected == null) {
            showWarning("Please select a workshop to delete.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Workshop");
        confirm.setHeaderText("Delete \""
                + selected.getTitle() + "\"?");
        confirm.setContentText(
                "This will also delete all bookings for " +
                        "this workshop. This cannot be undone.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteWorkshop(selected.getTitle());
                refreshTable();
                showInfo("Workshop deleted successfully.");
            }
        });
    }

    // ─── DIALOG BUILDER ─────────────────────────────────────
    private Dialog<Workshop> buildWorkshopDialog(
            Workshop workshop) {
        Dialog<Workshop> dialog = new Dialog<>();
        dialog.setTitle(workshop == null
                ? "Add New Workshop" : "Edit Workshop");
        dialog.setHeaderText(workshop == null
                ? "Fill in the workshop details"
                : "Modify the workshop details");

        List<Artist> artists = artistService.getAllArtists();

        TextField titleField = new TextField();
        titleField.setPromptText("Workshop title");
        TextField dateField = new TextField();
        dateField.setPromptText(
                "Date (YYYY-MM-DDTHH:MM, e.g. 2025-09-01T10:00)");
        TextField priceField = new TextField();
        priceField.setPromptText("Price");
        TextField maxField = new TextField();
        maxField.setPromptText("Max participants");
        TextField locationField = new TextField();
        locationField.setPromptText("Location");
        TextField durationField = new TextField();
        durationField.setPromptText("Duration in minutes");

        ComboBox<String> levelBox = new ComboBox<>();
        levelBox.getItems().addAll(
                "beginner", "intermediate", "advanced");
        levelBox.setValue("beginner");

        ComboBox<String> instructorBox = new ComboBox<>();
        artists.forEach(a -> instructorBox.getItems()
                .add(a.getName()));

        if (workshop != null) {
            titleField.setText(workshop.getTitle());
            titleField.setEditable(false);
            if (workshop.getDate() != null)
                dateField.setText(workshop.getDate().toString()
                        .replace("T", "T").substring(0, 16));
            priceField.setText(String.valueOf(
                    workshop.getPrice()));
            maxField.setText(String.valueOf(
                    workshop.getMaxParticipants()));
            locationField.setText(workshop.getLocation());
            durationField.setText(String.valueOf(
                    workshop.getDurationMinutes()));
            if (workshop.getLevel() != null)
                levelBox.setValue(workshop.getLevel());
            if (workshop.getInstructor() != null)
                instructorBox.setValue(
                        workshop.getInstructor().getName());
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Instructor:"), 0, 1);
        grid.add(instructorBox, 1, 1);
        grid.add(new Label("Date:"), 0, 2);
        grid.add(dateField, 1, 2);
        grid.add(new Label("Price:"), 0, 3);
        grid.add(priceField, 1, 3);
        grid.add(new Label("Max Participants:"), 0, 4);
        grid.add(maxField, 1, 4);
        grid.add(new Label("Location:"), 0, 5);
        grid.add(locationField, 1, 5);
        grid.add(new Label("Duration (min):"), 0, 6);
        grid.add(durationField, 1, 6);
        grid.add(new Label("Level:"), 0, 7);
        grid.add(levelBox, 1, 7);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                if (titleField.getText().trim().isEmpty()) {
                    showWarning("Title is required.");
                    return null;
                }
                if (instructorBox.getValue() == null) {
                    showWarning(
                            "Please select an instructor.");
                    return null;
                }
                Workshop result = new Workshop();
                result.setTitle(titleField.getText().trim());
                result.setLocation(
                        locationField.getText().trim());
                result.setLevel(levelBox.getValue());

                Artist instructor = new Artist();
                instructor.setName(instructorBox.getValue());
                result.setInstructor(instructor);

                try {
                    result.setDate(LocalDateTime.parse(
                            dateField.getText().trim()));
                } catch (Exception e) {
                    showWarning("Date format must be " +
                            "YYYY-MM-DDTHH:MM");
                    return null;
                }
                try {
                    if (!priceField.getText().trim().isEmpty())
                        result.setPrice(Double.parseDouble(
                                priceField.getText()
                                        .trim()
                                        .replace(",", ".")));
                    if (!maxField.getText().trim().isEmpty())
                        result.setMaxParticipants(
                                Integer.parseInt(
                                        maxField.getText()
                                                .trim()));
                    if (!durationField.getText().trim().isEmpty())
                        result.setDurationMinutes(
                                Integer.parseInt(
                                        durationField.getText()
                                                .trim()));
                } catch (NumberFormatException e) {
                    showWarning(
                            "Price, max participants and " +
                                    "duration must be numbers.");
                    return null;
                }
                return result;
            }
            return null;
        });

        return dialog;
    }

    // ─── DIRECT JDBC ────────────────────────────────────────
    private void saveWorkshop(Workshop workshop) {
        String findArtist =
                "SELECT artist_id FROM artist WHERE name = ?";
        String sql = """
                INSERT INTO workshop
                    (title, date, duration_minutes,
                     max_participants, price, location,
                     level, instructor_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement findStmt =
                     conn.prepareStatement(findArtist)) {
            findStmt.setString(1,
                    workshop.getInstructor().getName());
            ResultSet rs = findStmt.executeQuery();
            if (rs.next()) {
                int instructorId = rs.getInt("artist_id");
                try (PreparedStatement stmt =
                             conn.prepareStatement(sql)) {
                    stmt.setString(1, workshop.getTitle());
                    stmt.setTimestamp(2, Timestamp.valueOf(
                            workshop.getDate()));
                    stmt.setInt(3,
                            workshop.getDurationMinutes());
                    stmt.setInt(4,
                            workshop.getMaxParticipants());
                    stmt.setDouble(5, workshop.getPrice());
                    stmt.setString(6, workshop.getLocation());
                    stmt.setString(7, workshop.getLevel());
                    stmt.setInt(8, instructorId);
                    stmt.executeUpdate();
                }
            }
        } catch (Exception e) {
            showWarning("Error saving workshop: "
                    + e.getMessage());
        }
    }

    private void updateWorkshop(Workshop workshop) {
        String sql = """
                UPDATE workshop
                SET date = ?, duration_minutes = ?,
                    max_participants = ?, price = ?,
                    location = ?, level = ?
                WHERE title = ?
                """;
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(
                    workshop.getDate()));
            stmt.setInt(2, workshop.getDurationMinutes());
            stmt.setInt(3, workshop.getMaxParticipants());
            stmt.setDouble(4, workshop.getPrice());
            stmt.setString(5, workshop.getLocation());
            stmt.setString(6, workshop.getLevel());
            stmt.setString(7, workshop.getTitle());
            stmt.executeUpdate();
        } catch (Exception e) {
            showWarning("Error updating workshop: "
                    + e.getMessage());
        }
    }

    private void deleteWorkshop(String title) {
        String sql =
                "DELETE FROM workshop WHERE title = ?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {
            stmt.setString(1, title);
            stmt.executeUpdate();
        } catch (Exception e) {
            showWarning("Error deleting workshop: "
                    + e.getMessage());
        }
    }

    private int getParticipantCount(String workshopTitle) {
        String sql = """
                SELECT fn_count_workshop_participants(
                    workshop_id) AS participant_count
                FROM workshop WHERE title = ?
                """;
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {
            stmt.setString(1, workshopTitle);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return rs.getInt("participant_count");
        } catch (Exception e) {
            System.err.println(
                    "Error getting participant count: "
                            + e.getMessage());
        }
        return 0;
    }

    private void refreshTable() {
        workshopTable.setItems(FXCollections.observableArrayList(
                workshopService.getAllWorkshops()));
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setContentText(message);
        alert.showAndWait();
    }
}