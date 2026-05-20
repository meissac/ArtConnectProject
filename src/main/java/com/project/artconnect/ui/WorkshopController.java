package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.service.CommunityService;
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
import java.util.ArrayList;
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
    private final CommunityService communityService =
            ServiceProvider.getCommunityService();

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
                        cellData.getValue().getInstructor()
                                != null
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
                "This will also delete all bookings " +
                        "for this workshop. This cannot be undone.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteWorkshop(selected.getTitle());
                refreshTable();
                showInfo("Workshop deleted successfully.");
            }
        });
    }

    // ─── BOOK MEMBER ────────────────────────────────────────
    @FXML
    private void handleBookMember() {
        Workshop selected = workshopTable.getSelectionModel()
                .getSelectedItem();
        if (selected == null) {
            showWarning(
                    "Please select a workshop first.");
            return;
        }

        int currentBookings =
                getParticipantCount(selected.getTitle());
        int maxParticipants = selected.getMaxParticipants();

        if (currentBookings >= maxParticipants) {
            showWarning(
                    "This workshop is already full. ("
                            + currentBookings + " / "
                            + maxParticipants + " participants)");
            return;
        }

        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Book a Member");
        dialog.setHeaderText(
                "Register a member for:\n\""
                        + selected.getTitle() + "\"\n"
                        + "Available spots: "
                        + (maxParticipants - currentBookings)
                        + " / " + maxParticipants);

        List<CommunityMember> members =
                communityService.getAllMembers();

        ComboBox<String> memberBox = new ComboBox<>();
        members.forEach(m -> memberBox.getItems()
                .add(m.getName()
                        + " (" + m.getEmail() + ")"));
        memberBox.setPromptText("Select a member...");
        memberBox.setPrefWidth(300);

        ComboBox<String> paymentBox = new ComboBox<>();
        paymentBox.getItems().addAll("PAID", "PENDING");
        paymentBox.setValue("PAID");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 50, 10, 10));
        grid.add(new Label("Member:"), 0, 0);
        grid.add(memberBox, 1, 0);
        grid.add(new Label("Payment status:"), 0, 1);
        grid.add(paymentBox, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                if (memberBox.getValue() == null) {
                    showWarning("Please select a member.");
                    return null;
                }
                String selectedMember =
                        memberBox.getValue();
                String email = selectedMember.substring(
                        selectedMember.indexOf("(") + 1,
                        selectedMember.indexOf(")"));
                return new String[]{
                        email,
                        paymentBox.getValue()
                };
            }
            return null;
        });

        Optional<String[]> result = dialog.showAndWait();
        result.ifPresent(data -> {
            String memberEmail = data[0];
            String paymentStatus = data[1];

            boolean success = registerMemberToWorkshop(
                    selected.getTitle(),
                    memberEmail,
                    paymentStatus);

            if (success) {
                refreshTable();
                String memberName = members.stream()
                        .filter(m -> m.getEmail()
                                .equals(memberEmail))
                        .map(CommunityMember::getName)
                        .findFirst()
                        .orElse(memberEmail);
                showInfo(
                        memberName + " has been successfully"
                                + " registered for \""
                                + selected.getTitle() + "\".\n"
                                + "Payment status: "
                                + paymentStatus);
            }
        });
    }

    // ─── CANCEL BOOKING ─────────────────────────────────────
    @FXML
    private void handleCancelBooking() {
        Workshop selected = workshopTable.getSelectionModel()
                .getSelectedItem();
        if (selected == null) {
            showWarning(
                    "Please select a workshop first.");
            return;
        }

        String sql = """
                SELECT cm.member_id, cm.name,
                       cm.email, b.booking_id,
                       b.payment_status
                FROM booking b
                JOIN community_member cm
                    ON b.member_id = cm.member_id
                JOIN workshop w
                    ON b.workshop_id = w.workshop_id
                WHERE w.title = ?
                AND b.payment_status != 'CANCELLED'
                ORDER BY cm.name
                """;

        List<String[]> bookings = new ArrayList<>();

        try (Connection conn =
                     ConnectionManager.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {

            stmt.setString(1, selected.getTitle());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                bookings.add(new String[]{
                        String.valueOf(
                                rs.getInt("booking_id")),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("payment_status")
                });
            }

        } catch (Exception e) {
            showWarning("Error loading bookings: "
                    + e.getMessage());
            return;
        }

        if (bookings.isEmpty()) {
            showWarning(
                    "No active bookings found for \""
                            + selected.getTitle() + "\".");
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Cancel Booking");
        dialog.setHeaderText(
                "Select a member to cancel their booking\n"
                        + "for: \"" + selected.getTitle() + "\"");

        ComboBox<String> memberBox = new ComboBox<>();
        for (String[] booking : bookings) {
            memberBox.getItems().add(
                    booking[1]
                            + " (" + booking[2] + ")"
                            + " — " + booking[3]);
        }
        memberBox.setPromptText("Select a member...");
        memberBox.setPrefWidth(380);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 50, 10, 10));
        grid.add(new Label("Member:"), 0, 0);
        grid.add(memberBox, 1, 0);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                if (memberBox.getValue() == null) {
                    showWarning("Please select a member.");
                    return null;
                }
                int selectedIndex =
                        memberBox.getSelectionModel()
                                .getSelectedIndex();
                if (selectedIndex >= 0) {
                    return bookings.get(selectedIndex)[0];
                }
                return null;
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(bookingId -> {
            String memberName = bookings.stream()
                    .filter(b -> b[0].equals(bookingId))
                    .map(b -> b[1])
                    .findFirst()
                    .orElse("this member");

            Alert confirm = new Alert(
                    Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Cancel Booking");
            confirm.setHeaderText(
                    "Cancel booking for "
                            + memberName + "?");
            confirm.setContentText(
                    "This will set their booking status "
                            + "to CANCELLED for \""
                            + selected.getTitle() + "\".\n"
                            + "This action cannot be undone.");

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    cancelBooking(
                            Integer.parseInt(bookingId));
                    refreshTable();
                    showInfo(
                            "Booking for " + memberName
                                    + " has been cancelled.\n"
                                    + "The spot is now "
                                    + "available again.");
                }
            });
        });
    }

    // ─── REGISTER MEMBER (STORED PROCEDURE) ─────────────────
    /**
     * Calls sp_register_member_to_workshop stored procedure.
     * The capacity check trigger fires automatically.
     */
    private boolean registerMemberToWorkshop(
            String workshopTitle,
            String memberEmail,
            String paymentStatus) {

        String getWorkshopId =
                "SELECT workshop_id FROM workshop " +
                        "WHERE title = ?";
        String getMemberId =
                "SELECT member_id FROM community_member " +
                        "WHERE email = ?";
        String callProcedure =
                "CALL sp_register_member_to_workshop(?,?,?)";

        try (Connection conn =
                     ConnectionManager.getConnection()) {

            int workshopId = -1;
            try (PreparedStatement stmt =
                         conn.prepareStatement(
                                 getWorkshopId)) {
                stmt.setString(1, workshopTitle);
                ResultSet rs = stmt.executeQuery();
                if (rs.next())
                    workshopId = rs.getInt("workshop_id");
            }

            if (workshopId == -1) {
                showWarning(
                        "Workshop not found in database.");
                return false;
            }

            int memberId = -1;
            try (PreparedStatement stmt =
                         conn.prepareStatement(
                                 getMemberId)) {
                stmt.setString(1, memberEmail);
                ResultSet rs = stmt.executeQuery();
                if (rs.next())
                    memberId = rs.getInt("member_id");
            }

            if (memberId == -1) {
                showWarning(
                        "Member not found in database.");
                return false;
            }

            try (PreparedStatement stmt =
                         conn.prepareStatement(
                                 callProcedure)) {
                stmt.setInt(1, workshopId);
                stmt.setInt(2, memberId);
                stmt.setString(3, paymentStatus);
                stmt.execute();
            }

            return true;

        } catch (SQLException e) {
            showWarning("Booking failed: "
                    + e.getMessage());
            return false;
        } catch (Exception e) {
            showWarning("Unexpected error: "
                    + e.getMessage());
            return false;
        }
    }

    // ─── CANCEL BOOKING IN DATABASE ─────────────────────────
    /**
     * Sets booking status to CANCELLED instead of deleting.
     * Preserves history and frees the spot automatically
     * since the trigger counts only non-CANCELLED bookings.
     */
    private void cancelBooking(int bookingId) {
        String sql = """
                UPDATE booking
                SET payment_status = 'CANCELLED'
                WHERE booking_id = ?
                """;
        try (Connection conn =
                     ConnectionManager.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {
            stmt.setInt(1, bookingId);
            stmt.executeUpdate();
        } catch (Exception e) {
            showWarning("Error cancelling booking: "
                    + e.getMessage());
        }
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
                "Date (YYYY-MM-DDTHH:MM)");
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
                dateField.setText(
                        workshop.getDate().toString()
                                .substring(0, 16));
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
                result.setTitle(
                        titleField.getText().trim());
                result.setLocation(
                        locationField.getText().trim());
                result.setLevel(levelBox.getValue());

                Artist instructor = new Artist();
                instructor.setName(
                        instructorBox.getValue());
                result.setInstructor(instructor);

                try {
                    result.setDate(LocalDateTime.parse(
                            dateField.getText().trim()));
                } catch (Exception e) {
                    showWarning(
                            "Date format must be " +
                                    "YYYY-MM-DDTHH:MM");
                    return null;
                }
                try {
                    if (!priceField.getText()
                            .trim().isEmpty())
                        result.setPrice(Double.parseDouble(
                                priceField.getText()
                                        .trim()
                                        .replace(",",
                                                ".")));
                    if (!maxField.getText()
                            .trim().isEmpty())
                        result.setMaxParticipants(
                                Integer.parseInt(
                                        maxField.getText()
                                                .trim()));
                    if (!durationField.getText()
                            .trim().isEmpty())
                        result.setDurationMinutes(
                                Integer.parseInt(
                                        durationField
                                                .getText()
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
                     max_participants, price,
                     location, level, instructor_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn =
                     ConnectionManager.getConnection();
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
        try (Connection conn =
                     ConnectionManager.getConnection();
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
        try (Connection conn =
                     ConnectionManager.getConnection();
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
        try (Connection conn =
                     ConnectionManager.getConnection();
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

    // ─── HELPERS ────────────────────────────────────────────
    private void refreshTable() {
        workshopTable.setItems(
                FXCollections.observableArrayList(
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