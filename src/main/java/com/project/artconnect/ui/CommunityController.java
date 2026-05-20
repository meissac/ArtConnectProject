package com.project.artconnect.ui;

import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.service.CommunityService;
import com.project.artconnect.util.ConnectionManager;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class CommunityController {

    @FXML private TableView<CommunityMember> memberTable;
    @FXML private TableColumn<CommunityMember, String> nameColumn;
    @FXML private TableColumn<CommunityMember, String> emailColumn;
    @FXML private TableColumn<CommunityMember, String> cityColumn;
    @FXML private TableColumn<CommunityMember, String> membershipColumn;
    @FXML private TableColumn<CommunityMember, Number> bookingsColumn;

    private final CommunityService communityService =
            ServiceProvider.getCommunityService();

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(
                new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(
                new PropertyValueFactory<>("email"));
        cityColumn.setCellValueFactory(
                new PropertyValueFactory<>("city"));

        membershipColumn.setCellValueFactory(
                new PropertyValueFactory<>("membershipType"));
        membershipColumn.setCellFactory(column ->
                new TableCell<CommunityMember, String>() {
                    @Override
                    protected void updateItem(String type, boolean empty) {
                        super.updateItem(type, empty);
                        if (empty || type == null) {
                            setText(null); setStyle("");
                        } else {
                            setText(type.toUpperCase());
                            switch (type.toLowerCase()) {
                                case "premium" -> setStyle(
                                        "-fx-text-fill: #f39c12; " +
                                                "-fx-font-weight: bold;");
                                case "free" -> setStyle(
                                        "-fx-text-fill: #999999;");
                                default -> setStyle("");
                            }
                        }
                    }
                });

        bookingsColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(getBookingCount(
                        cellData.getValue().getEmail())));

        refreshTable();
    }

    // ─── ADD ────────────────────────────────────────────────
    @FXML
    private void handleAdd() {
        Dialog<CommunityMember> dialog =
                buildMemberDialog(null);
        Optional<CommunityMember> result =
                dialog.showAndWait();
        result.ifPresent(member -> {
            saveMember(member);
            refreshTable();
            showInfo("Member \"" + member.getName()
                    + "\" added successfully.");
        });
    }

    // ─── EDIT ───────────────────────────────────────────────
    @FXML
    private void handleEdit() {
        CommunityMember selected = memberTable
                .getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a member to edit.");
            return;
        }
        Dialog<CommunityMember> dialog =
                buildMemberDialog(selected);
        Optional<CommunityMember> result =
                dialog.showAndWait();
        result.ifPresent(member -> {
            updateMember(member);
            refreshTable();
            showInfo("Member updated successfully.");
        });
    }

    // ─── DELETE ─────────────────────────────────────────────
    @FXML
    private void handleDelete() {
        CommunityMember selected = memberTable
                .getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a member to delete.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Member");
        confirm.setHeaderText("Delete \""
                + selected.getName() + "\"?");
        confirm.setContentText(
                "This will also delete all their bookings " +
                        "and reviews. This cannot be undone.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteMember(selected.getEmail());
                refreshTable();
                showInfo("Member deleted successfully.");
            }
        });
    }

    // ─── DIALOG BUILDER ─────────────────────────────────────
    private Dialog<CommunityMember> buildMemberDialog(
            CommunityMember member) {
        Dialog<CommunityMember> dialog = new Dialog<>();
        dialog.setTitle(member == null
                ? "Add New Member" : "Edit Member");
        dialog.setHeaderText(member == null
                ? "Fill in the member details"
                : "Modify the member details");

        TextField nameField = new TextField();
        nameField.setPromptText("Full name");
        TextField emailField = new TextField();
        emailField.setPromptText("Email address");
        TextField cityField = new TextField();
        cityField.setPromptText("City");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone number");
        TextField yearField = new TextField();
        yearField.setPromptText("Birth year");

        ComboBox<String> membershipBox = new ComboBox<>();
        membershipBox.getItems().addAll("free", "premium");
        membershipBox.setValue("free");

        if (member != null) {
            nameField.setText(member.getName());
            emailField.setText(member.getEmail());
            emailField.setEditable(false);
            cityField.setText(member.getCity());
            phoneField.setText(member.getPhone());
            if (member.getBirthYear() != null)
                yearField.setText(String.valueOf(
                        member.getBirthYear()));
            if (member.getMembershipType() != null)
                membershipBox.setValue(
                        member.getMembershipType());
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("City:"), 0, 2);
        grid.add(cityField, 1, 2);
        grid.add(new Label("Phone:"), 0, 3);
        grid.add(phoneField, 1, 3);
        grid.add(new Label("Birth Year:"), 0, 4);
        grid.add(yearField, 1, 4);
        grid.add(new Label("Membership:"), 0, 5);
        grid.add(membershipBox, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                if (nameField.getText().trim().isEmpty()) {
                    showWarning("Name is required.");
                    return null;
                }
                if (emailField.getText().trim().isEmpty()) {
                    showWarning("Email is required.");
                    return null;
                }
                CommunityMember result = new CommunityMember();
                result.setName(nameField.getText().trim());
                result.setEmail(emailField.getText().trim());
                result.setCity(cityField.getText().trim());
                result.setPhone(phoneField.getText().trim());
                result.setMembershipType(
                        membershipBox.getValue());
                try {
                    if (!yearField.getText().trim().isEmpty())
                        result.setBirthYear(Integer.parseInt(
                                yearField.getText().trim()));
                } catch (NumberFormatException e) {
                    showWarning("Birth year must be a number.");
                    return null;
                }
                return result;
            }
            return null;
        });

        return dialog;
    }

    // ─── DIRECT JDBC ────────────────────────────────────────
    private void saveMember(CommunityMember member) {
        String sql = """
                INSERT INTO community_member
                    (name, email, birth_year, phone,
                     city, membership_type)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {
            stmt.setString(1, member.getName());
            stmt.setString(2, member.getEmail());
            if (member.getBirthYear() != null)
                stmt.setInt(3, member.getBirthYear());
            else
                stmt.setNull(3, java.sql.Types.INTEGER);
            stmt.setString(4, member.getPhone());
            stmt.setString(5, member.getCity());
            stmt.setString(6, member.getMembershipType());
            stmt.executeUpdate();
        } catch (Exception e) {
            showWarning("Error saving member: "
                    + e.getMessage());
        }
    }

    private void updateMember(CommunityMember member) {
        String sql = """
                UPDATE community_member
                SET name = ?, city = ?, phone = ?,
                    membership_type = ?
                WHERE email = ?
                """;
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {
            stmt.setString(1, member.getName());
            stmt.setString(2, member.getCity());
            stmt.setString(3, member.getPhone());
            stmt.setString(4, member.getMembershipType());
            stmt.setString(5, member.getEmail());
            stmt.executeUpdate();
        } catch (Exception e) {
            showWarning("Error updating member: "
                    + e.getMessage());
        }
    }

    private void deleteMember(String email) {
        String sql =
                "DELETE FROM community_member WHERE email = ?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.executeUpdate();
        } catch (Exception e) {
            showWarning("Error deleting member: "
                    + e.getMessage());
        }
    }

    private int getBookingCount(String memberEmail) {
        String sql = """
                SELECT COUNT(*) AS booking_count
                FROM booking b
                JOIN community_member cm
                    ON b.member_id = cm.member_id
                WHERE cm.email = ?
                AND b.payment_status != 'CANCELLED'
                """;
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {
            stmt.setString(1, memberEmail);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return rs.getInt("booking_count");
        } catch (Exception e) {
            System.err.println("Error getting booking count: "
                    + e.getMessage());
        }
        return 0;
    }

    private void refreshTable() {
        memberTable.setItems(FXCollections.observableArrayList(
                communityService.getAllMembers()));
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