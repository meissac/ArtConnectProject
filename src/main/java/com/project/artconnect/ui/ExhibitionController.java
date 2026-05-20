package com.project.artconnect.ui;

import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.service.GalleryService;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExhibitionController {

    @FXML private TableView<Exhibition> exhibitionTable;
    @FXML private TableColumn<Exhibition, String> titleColumn;
    @FXML private TableColumn<Exhibition, String> galleryColumn;
    @FXML private TableColumn<Exhibition, String> dateColumn;
    @FXML private TableColumn<Exhibition, String> themeColumn;

    private final GalleryService galleryService =
            ServiceProvider.getGalleryService();

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(
                new PropertyValueFactory<>("title"));
        galleryColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getGallery() != null
                                ? cellData.getValue().getGallery().getName()
                                : "Unknown"));
        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getStartDate() != null
                                ? cellData.getValue().getStartDate()
                                .toString()
                                : ""));
        themeColumn.setCellValueFactory(
                new PropertyValueFactory<>("theme"));
        refreshTable();
    }

    // ─── ADD ────────────────────────────────────────────────
    @FXML
    private void handleAdd() {
        Dialog<Exhibition> dialog =
                buildExhibitionDialog(null);
        Optional<Exhibition> result = dialog.showAndWait();
        result.ifPresent(exhibition -> {
            saveExhibition(exhibition);
            refreshTable();
            showInfo("Exhibition \"" + exhibition.getTitle()
                    + "\" added successfully.");
        });
    }

    // ─── EDIT ───────────────────────────────────────────────
    @FXML
    private void handleEdit() {
        Exhibition selected = exhibitionTable
                .getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning(
                    "Please select an exhibition to edit.");
            return;
        }
        Dialog<Exhibition> dialog =
                buildExhibitionDialog(selected);
        Optional<Exhibition> result = dialog.showAndWait();
        result.ifPresent(exhibition -> {
            updateExhibition(exhibition);
            refreshTable();
            showInfo("Exhibition updated successfully.");
        });
    }

    // ─── DELETE ─────────────────────────────────────────────
    @FXML
    private void handleDelete() {
        Exhibition selected = exhibitionTable
                .getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning(
                    "Please select an exhibition to delete.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Exhibition");
        confirm.setHeaderText("Delete \""
                + selected.getTitle() + "\"?");
        confirm.setContentText(
                "This action cannot be undone.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteExhibition(selected.getTitle());
                refreshTable();
                showInfo("Exhibition deleted successfully.");
            }
        });
    }

    // ─── DIALOG BUILDER ─────────────────────────────────────
    private Dialog<Exhibition> buildExhibitionDialog(
            Exhibition exhibition) {
        Dialog<Exhibition> dialog = new Dialog<>();
        dialog.setTitle(exhibition == null
                ? "Add New Exhibition" : "Edit Exhibition");
        dialog.setHeaderText(exhibition == null
                ? "Fill in the exhibition details"
                : "Modify the exhibition details");

        List<Gallery> galleries =
                galleryService.getAllGalleries();

        TextField titleField = new TextField();
        titleField.setPromptText("Exhibition title");
        TextField startField = new TextField();
        startField.setPromptText("Start date (YYYY-MM-DD)");
        TextField endField = new TextField();
        endField.setPromptText("End date (YYYY-MM-DD)");
        TextField themeField = new TextField();
        themeField.setPromptText("Theme");
        TextField curatorField = new TextField();
        curatorField.setPromptText("Curator name");

        ComboBox<String> galleryBox = new ComboBox<>();
        galleries.forEach(g -> galleryBox.getItems()
                .add(g.getName()));

        if (exhibition != null) {
            titleField.setText(exhibition.getTitle());
            titleField.setEditable(false);
            if (exhibition.getStartDate() != null)
                startField.setText(
                        exhibition.getStartDate().toString());
            if (exhibition.getEndDate() != null)
                endField.setText(
                        exhibition.getEndDate().toString());
            themeField.setText(exhibition.getTheme());
            curatorField.setText(exhibition.getCuratorName());
            if (exhibition.getGallery() != null)
                galleryBox.setValue(
                        exhibition.getGallery().getName());
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Gallery:"), 0, 1);
        grid.add(galleryBox, 1, 1);
        grid.add(new Label("Start Date:"), 0, 2);
        grid.add(startField, 1, 2);
        grid.add(new Label("End Date:"), 0, 3);
        grid.add(endField, 1, 3);
        grid.add(new Label("Theme:"), 0, 4);
        grid.add(themeField, 1, 4);
        grid.add(new Label("Curator:"), 0, 5);
        grid.add(curatorField, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                if (titleField.getText().trim().isEmpty()) {
                    showWarning("Title is required.");
                    return null;
                }
                if (galleryBox.getValue() == null) {
                    showWarning("Please select a gallery.");
                    return null;
                }
                Exhibition result = new Exhibition();
                result.setTitle(titleField.getText().trim());
                result.setTheme(themeField.getText().trim());
                result.setCuratorName(
                        curatorField.getText().trim());
                try {
                    result.setStartDate(LocalDate.parse(
                            startField.getText().trim()));
                    result.setEndDate(LocalDate.parse(
                            endField.getText().trim()));
                } catch (Exception e) {
                    showWarning(
                            "Dates must be in YYYY-MM-DD format.");
                    return null;
                }
                Gallery g = new Gallery();
                g.setName(galleryBox.getValue());
                result.setGallery(g);
                return result;
            }
            return null;
        });

        return dialog;
    }

    // ─── DIRECT JDBC ────────────────────────────────────────
    private void saveExhibition(Exhibition exhibition) {
        String findGallery =
                "SELECT gallery_id FROM gallery WHERE name = ?";
        String sql = """
                INSERT INTO exhibition
                    (title, start_date, end_date,
                     theme, curator_name, gallery_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement findStmt =
                     conn.prepareStatement(findGallery)) {
            findStmt.setString(1,
                    exhibition.getGallery().getName());
            ResultSet rs = findStmt.executeQuery();
            if (rs.next()) {
                int galleryId = rs.getInt("gallery_id");
                try (PreparedStatement stmt =
                             conn.prepareStatement(sql)) {
                    stmt.setString(1, exhibition.getTitle());
                    stmt.setDate(2, Date.valueOf(
                            exhibition.getStartDate()));
                    stmt.setDate(3, Date.valueOf(
                            exhibition.getEndDate()));
                    stmt.setString(4, exhibition.getTheme());
                    stmt.setString(5,
                            exhibition.getCuratorName());
                    stmt.setInt(6, galleryId);
                    stmt.executeUpdate();
                }
            }
        } catch (Exception e) {
            showWarning("Error saving exhibition: "
                    + e.getMessage());
        }
    }

    private void updateExhibition(Exhibition exhibition) {
        String sql = """
                UPDATE exhibition
                SET start_date = ?, end_date = ?,
                    theme = ?, curator_name = ?
                WHERE title = ?
                """;
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(
                    exhibition.getStartDate()));
            stmt.setDate(2, Date.valueOf(
                    exhibition.getEndDate()));
            stmt.setString(3, exhibition.getTheme());
            stmt.setString(4, exhibition.getCuratorName());
            stmt.setString(5, exhibition.getTitle());
            stmt.executeUpdate();
        } catch (Exception e) {
            showWarning("Error updating exhibition: "
                    + e.getMessage());
        }
    }

    private void deleteExhibition(String title) {
        String sql =
                "DELETE FROM exhibition WHERE title = ?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {
            stmt.setString(1, title);
            stmt.executeUpdate();
        } catch (Exception e) {
            showWarning("Error deleting exhibition: "
                    + e.getMessage());
        }
    }

    private void refreshTable() {
        exhibitionTable.setItems(
                FXCollections.observableArrayList(
                        loadAllExhibitions()));
    }

    private List<Exhibition> loadAllExhibitions() {
        String sql = """
                SELECT e.title, e.start_date, e.end_date,
                       e.theme, e.curator_name,
                       g.name AS gallery_name
                FROM exhibition e
                JOIN gallery g
                    ON e.gallery_id = g.gallery_id
                ORDER BY e.start_date
                """;
        List<Exhibition> list = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Exhibition e = new Exhibition();
                e.setTitle(rs.getString("title"));
                e.setStartDate(rs.getDate("start_date")
                        .toLocalDate());
                e.setEndDate(rs.getDate("end_date")
                        .toLocalDate());
                e.setTheme(rs.getString("theme"));
                e.setCuratorName(rs.getString("curator_name"));
                Gallery g = new Gallery();
                g.setName(rs.getString("gallery_name"));
                e.setGallery(g);
                list.add(e);
            }
        } catch (Exception e) {
            System.err.println("Error loading exhibitions: "
                    + e.getMessage());
        }
        return list;
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