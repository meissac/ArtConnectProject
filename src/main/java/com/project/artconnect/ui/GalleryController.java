package com.project.artconnect.ui;

import com.project.artconnect.model.Gallery;
import com.project.artconnect.service.GalleryService;
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

public class GalleryController {

    @FXML private TableView<Gallery> galleryTable;
    @FXML private TableColumn<Gallery, String> nameColumn;
    @FXML private TableColumn<Gallery, String> addressColumn;
    @FXML private TableColumn<Gallery, Double> ratingColumn;
    @FXML private TableColumn<Gallery, Number> exhibitionCountColumn;

    private final GalleryService galleryService =
            ServiceProvider.getGalleryService();

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(
                new PropertyValueFactory<>("name"));
        addressColumn.setCellValueFactory(
                new PropertyValueFactory<>("address"));
        ratingColumn.setCellValueFactory(
                new PropertyValueFactory<>("rating"));
        exhibitionCountColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(getExhibitionCount(
                        cellData.getValue().getName())));
        refreshTable();
    }

    // ─── ADD ────────────────────────────────────────────────
    @FXML
    private void handleAdd() {
        Dialog<Gallery> dialog = buildGalleryDialog(null);
        Optional<Gallery> result = dialog.showAndWait();
        result.ifPresent(gallery -> {
            saveGallery(gallery);
            refreshTable();
            showInfo("Gallery \"" + gallery.getName()
                    + "\" added successfully.");
        });
    }

    // ─── EDIT ───────────────────────────────────────────────
    @FXML
    private void handleEdit() {
        Gallery selected = galleryTable.getSelectionModel()
                .getSelectedItem();
        if (selected == null) {
            showWarning("Please select a gallery to edit.");
            return;
        }
        Dialog<Gallery> dialog = buildGalleryDialog(selected);
        Optional<Gallery> result = dialog.showAndWait();
        result.ifPresent(gallery -> {
            updateGallery(gallery);
            refreshTable();
            showInfo("Gallery updated successfully.");
        });
    }

    // ─── DELETE ─────────────────────────────────────────────
    @FXML
    private void handleDelete() {
        Gallery selected = galleryTable.getSelectionModel()
                .getSelectedItem();
        if (selected == null) {
            showWarning("Please select a gallery to delete.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Gallery");
        confirm.setHeaderText("Delete \""
                + selected.getName() + "\"?");
        confirm.setContentText(
                "This will also delete all exhibitions " +
                        "in this gallery. This cannot be undone.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteGallery(selected.getName());
                refreshTable();
                showInfo("Gallery deleted successfully.");
            }
        });
    }

    // ─── DIALOG BUILDER ─────────────────────────────────────
    private Dialog<Gallery> buildGalleryDialog(Gallery gallery) {
        Dialog<Gallery> dialog = new Dialog<>();
        dialog.setTitle(gallery == null
                ? "Add New Gallery" : "Edit Gallery");
        dialog.setHeaderText(gallery == null
                ? "Fill in the gallery details"
                : "Modify the gallery details");

        TextField nameField = new TextField();
        nameField.setPromptText("Gallery name");
        TextField addressField = new TextField();
        addressField.setPromptText("Full address");
        TextField ownerField = new TextField();
        ownerField.setPromptText("Owner name");
        TextField hoursField = new TextField();
        hoursField.setPromptText("Opening hours");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Contact phone");
        TextField ratingField = new TextField();
        ratingField.setPromptText("Rating (0.0 - 5.0)");
        TextField websiteField = new TextField();
        websiteField.setPromptText("Website URL");

        if (gallery != null) {
            nameField.setText(gallery.getName());
            nameField.setEditable(false);
            addressField.setText(gallery.getAddress());
            ownerField.setText(gallery.getOwnerName());
            hoursField.setText(gallery.getOpeningHours());
            phoneField.setText(gallery.getContactPhone());
            ratingField.setText(String.valueOf(
                    gallery.getRating()));
            websiteField.setText(gallery.getWebsite());
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Address:"), 0, 1);
        grid.add(addressField, 1, 1);
        grid.add(new Label("Owner:"), 0, 2);
        grid.add(ownerField, 1, 2);
        grid.add(new Label("Opening Hours:"), 0, 3);
        grid.add(hoursField, 1, 3);
        grid.add(new Label("Phone:"), 0, 4);
        grid.add(phoneField, 1, 4);
        grid.add(new Label("Rating:"), 0, 5);
        grid.add(ratingField, 1, 5);
        grid.add(new Label("Website:"), 0, 6);
        grid.add(websiteField, 1, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                if (nameField.getText().trim().isEmpty()) {
                    showWarning("Name is required.");
                    return null;
                }
                Gallery result = new Gallery();
                result.setName(nameField.getText().trim());
                result.setAddress(addressField.getText().trim());
                result.setOwnerName(ownerField.getText().trim());
                result.setOpeningHours(hoursField.getText().trim());
                result.setContactPhone(phoneField.getText().trim());
                result.setWebsite(websiteField.getText().trim());
                try {
                    if (!ratingField.getText().trim().isEmpty()) {
                        result.setRating(Double.parseDouble(
                                ratingField.getText()
                                        .trim()
                                        .replace(",", ".")));
                    }
                } catch (NumberFormatException e) {
                    showWarning("Rating must be a number.");
                    return null;
                }
                return result;
            }
            return null;
        });

        return dialog;
    }

    // ─── DIRECT JDBC FOR GALLERY ─────────────────────────────
    // GalleryService doesn't have save/update/delete yet
    // so we handle it directly here with JDBC
    private void saveGallery(Gallery gallery) {
        String sql = """
                INSERT INTO gallery
                    (name, address, owner_name, opening_hours,
                     contact_phone, rating, website)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {
            stmt.setString(1, gallery.getName());
            stmt.setString(2, gallery.getAddress());
            stmt.setString(3, gallery.getOwnerName());
            stmt.setString(4, gallery.getOpeningHours());
            stmt.setString(5, gallery.getContactPhone());
            stmt.setDouble(6, gallery.getRating());
            stmt.setString(7, gallery.getWebsite());
            stmt.executeUpdate();
        } catch (Exception e) {
            showWarning("Error saving gallery: "
                    + e.getMessage());
        }
    }

    private void updateGallery(Gallery gallery) {
        String sql = """
                UPDATE gallery
                SET address = ?, owner_name = ?,
                    opening_hours = ?, contact_phone = ?,
                    rating = ?, website = ?
                WHERE name = ?
                """;
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {
            stmt.setString(1, gallery.getAddress());
            stmt.setString(2, gallery.getOwnerName());
            stmt.setString(3, gallery.getOpeningHours());
            stmt.setString(4, gallery.getContactPhone());
            stmt.setDouble(5, gallery.getRating());
            stmt.setString(6, gallery.getWebsite());
            stmt.setString(7, gallery.getName());
            stmt.executeUpdate();
        } catch (Exception e) {
            showWarning("Error updating gallery: "
                    + e.getMessage());
        }
    }

    private void deleteGallery(String name) {
        String sql = "DELETE FROM gallery WHERE name = ?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
        } catch (Exception e) {
            showWarning("Error deleting gallery: "
                    + e.getMessage());
        }
    }

    // ─── HELPERS ────────────────────────────────────────────
    private void refreshTable() {
        galleryTable.setItems(FXCollections.observableArrayList(
                galleryService.getAllGalleries()));
    }

    private int getExhibitionCount(String galleryName) {
        String sql = """
                SELECT COUNT(*) AS exhibition_count
                FROM view_exhibition_details
                WHERE gallery_name = ?
                """;
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {
            stmt.setString(1, galleryName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("exhibition_count");
        } catch (Exception e) {
            System.err.println("Error getting exhibition count: "
                    + e.getMessage());
        }
        return 0;
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