package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.service.ArtworkService;
import com.project.artconnect.util.ConnectionManager;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ArtworkController {

    @FXML private TableView<Artwork> artworkTable;
    @FXML private TableColumn<Artwork, String> titleColumn;
    @FXML private TableColumn<Artwork, String> typeColumn;
    @FXML private TableColumn<Artwork, Double> priceColumn;
    @FXML private TableColumn<Artwork, Artwork.Status> statusColumn;
    @FXML private TableColumn<Artwork, String> artistColumn;
    @FXML private TableColumn<Artwork, String> avgRatingColumn;

    private final ArtworkService artworkService =
            ServiceProvider.getArtworkService();
    private final ArtistService artistService =
            ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(
                new PropertyValueFactory<>("title"));
        typeColumn.setCellValueFactory(
                new PropertyValueFactory<>("type"));
        priceColumn.setCellValueFactory(
                new PropertyValueFactory<>("price"));
        artistColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getArtist() != null
                                ? cellData.getValue().getArtist().getName()
                                : "Unknown"));

        // Color-coded status column
        statusColumn.setCellValueFactory(
                new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(column ->
                new TableCell<Artwork, Artwork.Status>() {
                    @Override
                    protected void updateItem(Artwork.Status status,
                                              boolean empty) {
                        super.updateItem(status, empty);
                        if (empty || status == null) {
                            setText(null); setStyle("");
                        } else {
                            setText(status.name());
                            switch (status) {
                                case FOR_SALE -> setStyle(
                                        "-fx-text-fill: #27ae60; " +
                                                "-fx-font-weight: bold;");
                                case SOLD -> setStyle(
                                        "-fx-text-fill: #e74c3c; " +
                                                "-fx-font-weight: bold;");
                                case EXHIBITED -> setStyle(
                                        "-fx-text-fill: #2980b9; " +
                                                "-fx-font-weight: bold;");
                                default -> setStyle("");
                            }
                        }
                    }
                });

        // Average rating column
        avgRatingColumn.setCellValueFactory(cellData -> {
            double avg = getAverageRating(
                    cellData.getValue().getTitle());
            String display = avg > 0
                    ? String.format(Locale.US, "%.1f / 5", avg)
                    : "No reviews";
            return new SimpleStringProperty(display);
        });
        avgRatingColumn.setCellFactory(column ->
                new TableCell<Artwork, String>() {
                    @Override
                    protected void updateItem(String rating,
                                              boolean empty) {
                        super.updateItem(rating, empty);
                        if (empty || rating == null) {
                            setText(null); setStyle("");
                        } else {
                            setText(rating);
                            if (rating.equals("No reviews")) {
                                setStyle("-fx-text-fill: #999999;");
                            } else {
                                try {
                                    double v = Double.parseDouble(
                                            rating.split(" ")[0]);
                                    if (v >= 4.5)
                                        setStyle("-fx-text-fill: " +
                                                "#27ae60; " +
                                                "-fx-font-weight: bold;");
                                    else if (v >= 3.0)
                                        setStyle("-fx-text-fill: " +
                                                "#f39c12; " +
                                                "-fx-font-weight: bold;");
                                    else
                                        setStyle("-fx-text-fill: " +
                                                "#e74c3c; " +
                                                "-fx-font-weight: bold;");
                                } catch (NumberFormatException e) {
                                    setStyle("");
                                }
                            }
                        }
                    }
                });

        refreshTable();
    }

    // ─── ADD ────────────────────────────────────────────────
    @FXML
    private void handleAdd() {
        Dialog<Artwork> dialog = buildArtworkDialog(null);
        Optional<Artwork> result = dialog.showAndWait();
        result.ifPresent(artwork -> {
            artworkService.createArtwork(artwork);
            refreshTable();
            showInfo("Artwork \"" + artwork.getTitle()
                    + "\" added successfully.");
        });
    }

    // ─── EDIT ───────────────────────────────────────────────
    @FXML
    private void handleEdit() {
        Artwork selected = artworkTable.getSelectionModel()
                .getSelectedItem();
        if (selected == null) {
            showWarning("Please select an artwork to edit.");
            return;
        }
        Dialog<Artwork> dialog = buildArtworkDialog(selected);
        Optional<Artwork> result = dialog.showAndWait();
        result.ifPresent(artwork -> {
            artworkService.updateArtwork(artwork);
            refreshTable();
            showInfo("Artwork updated successfully.");
        });
    }

    // ─── DELETE ─────────────────────────────────────────────
    @FXML
    private void handleDelete() {
        Artwork selected = artworkTable.getSelectionModel()
                .getSelectedItem();
        if (selected == null) {
            showWarning("Please select an artwork to delete.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Artwork");
        confirm.setHeaderText("Delete \""
                + selected.getTitle() + "\"?");
        confirm.setContentText(
                "This action cannot be undone.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                artworkService.deleteArtwork(
                        selected.getTitle());
                refreshTable();
                showInfo("Artwork deleted successfully.");
            }
        });
    }

    // ─── VIEW REVIEWS ───────────────────────────────────────
    @FXML
    private void handleViewReviews() {
        Artwork selected = artworkTable.getSelectionModel()
                .getSelectedItem();
        if (selected == null) {
            showWarning(
                    "Please select an artwork to view " +
                            "its reviews.");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Reviews for: "
                + selected.getTitle());
        dialog.setHeaderText(
                "All reviews for \""
                        + selected.getTitle() + "\"");

        // Build review table
        TableView<String[]> reviewTable = new TableView<>();
        reviewTable.setPrefWidth(620);
        reviewTable.setPrefHeight(300);

        TableColumn<String[], String> memberCol =
                new TableColumn<>("Member");
        memberCol.setPrefWidth(150);
        memberCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[0]));

        TableColumn<String[], String> ratingCol =
                new TableColumn<>("Rating");
        ratingCol.setPrefWidth(80);
        ratingCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[1]));

        // Color-code ratings
        ratingCol.setCellFactory(col ->
                new TableCell<String[], String>() {
                    @Override
                    protected void updateItem(String rating,
                                              boolean empty) {
                        super.updateItem(rating, empty);
                        if (empty || rating == null) {
                            setText(null); setStyle("");
                        } else {
                            setText(rating + " / 5");
                            try {
                                int r = Integer.parseInt(rating);
                                if (r >= 4)
                                    setStyle(
                                            "-fx-text-fill: #27ae60; " +
                                                    "-fx-font-weight: bold;");
                                else if (r == 3)
                                    setStyle(
                                            "-fx-text-fill: #f39c12; " +
                                                    "-fx-font-weight: bold;");
                                else
                                    setStyle(
                                            "-fx-text-fill: #e74c3c; " +
                                                    "-fx-font-weight: bold;");
                            } catch (NumberFormatException e) {
                                setStyle("");
                            }
                        }
                    }
                });

        TableColumn<String[], String> commentCol =
                new TableColumn<>("Comment");
        commentCol.setPrefWidth(280);
        commentCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[2]));

        // Wrap comment text so long comments are readable
        commentCol.setCellFactory(col ->
                new TableCell<String[], String>() {
                    @Override
                    protected void updateItem(String comment,
                                              boolean empty) {
                        super.updateItem(comment, empty);
                        if (empty || comment == null) {
                            setText(null);
                        } else {
                            setText(comment);
                            setWrapText(true);
                        }
                    }
                });

        TableColumn<String[], String> dateCol =
                new TableColumn<>("Date");
        dateCol.setPrefWidth(110);
        dateCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[3]));

        reviewTable.getColumns().addAll(
                memberCol, ratingCol, commentCol, dateCol);

        // Load reviews from database
        String sql = """
                SELECT cm.name AS member_name,
                       r.rating,
                       r.comment,
                       r.review_date
                FROM review r
                JOIN community_member cm
                    ON r.member_id = cm.member_id
                JOIN artwork a
                    ON r.artwork_id = a.artwork_id
                WHERE a.title = ?
                ORDER BY r.review_date DESC
                """;

        try (Connection conn =
                     ConnectionManager.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {

            stmt.setString(1, selected.getTitle());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String[] row = {
                        rs.getString("member_name"),
                        String.valueOf(rs.getInt("rating")),
                        rs.getString("comment") != null
                                ? rs.getString("comment") : "",
                        rs.getDate("review_date").toString()
                };
                reviewTable.getItems().add(row);
            }

        } catch (Exception e) {
            showWarning("Error loading reviews: "
                    + e.getMessage());
            return;
        }

        // Build dialog content
        VBox content;
        if (reviewTable.getItems().isEmpty()) {
            // Show average rating summary + no reviews message
            Label noReviews = new Label(
                    "No reviews yet for \""
                            + selected.getTitle() + "\".");
            noReviews.setStyle(
                    "-fx-text-fill: #999999; " +
                            "-fx-font-style: italic;");
            content = new VBox(10, noReviews);
        } else {
            // Show count + average above the table
            int count = reviewTable.getItems().size();
            double avg = reviewTable.getItems().stream()
                    .mapToInt(row ->
                            Integer.parseInt(row[1]))
                    .average()
                    .orElse(0);

            Label summary = new Label(
                    count + " review(s) — Average rating: "
                            + String.format(Locale.US,
                            "%.1f / 5", avg));
            summary.setStyle(
                    "-fx-font-weight: bold; " +
                            "-fx-font-size: 13px;");

            content = new VBox(10, summary, reviewTable);
        }

        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes()
                .add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    // ─── DIALOG BUILDER ─────────────────────────────────────
    private Dialog<Artwork> buildArtworkDialog(
            Artwork artwork) {
        Dialog<Artwork> dialog = new Dialog<>();
        dialog.setTitle(artwork == null
                ? "Add New Artwork" : "Edit Artwork");
        dialog.setHeaderText(artwork == null
                ? "Fill in the artwork details"
                : "Modify the artwork details");

        List<Artist> artists = artistService.getAllArtists();

        TextField titleField = new TextField();
        titleField.setPromptText("Artwork title");
        TextField typeField = new TextField();
        typeField.setPromptText("Type (e.g. Painting)");
        TextField mediumField = new TextField();
        mediumField.setPromptText(
                "Medium (e.g. Oil on canvas)");
        TextField priceField = new TextField();
        priceField.setPromptText("Price (e.g. 5000.00)");
        TextField yearField = new TextField();
        yearField.setPromptText("Creation year");

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll(
                "FOR_SALE", "SOLD", "EXHIBITED");
        statusBox.setValue("FOR_SALE");

        ComboBox<String> artistBox = new ComboBox<>();
        artists.forEach(a ->
                artistBox.getItems().add(a.getName()));

        if (artwork != null) {
            titleField.setText(artwork.getTitle());
            titleField.setEditable(false);
            typeField.setText(artwork.getType());
            mediumField.setText(artwork.getMedium());
            priceField.setText(String.valueOf(
                    artwork.getPrice()));
            if (artwork.getCreationYear() != null)
                yearField.setText(String.valueOf(
                        artwork.getCreationYear()));
            if (artwork.getStatus() != null)
                statusBox.setValue(artwork.getStatus().name());
            if (artwork.getArtist() != null)
                artistBox.setValue(
                        artwork.getArtist().getName());
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Artist:"), 0, 1);
        grid.add(artistBox, 1, 1);
        grid.add(new Label("Type:"), 0, 2);
        grid.add(typeField, 1, 2);
        grid.add(new Label("Medium:"), 0, 3);
        grid.add(mediumField, 1, 3);
        grid.add(new Label("Price:"), 0, 4);
        grid.add(priceField, 1, 4);
        grid.add(new Label("Year:"), 0, 5);
        grid.add(yearField, 1, 5);
        grid.add(new Label("Status:"), 0, 6);
        grid.add(statusBox, 1, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                if (titleField.getText().trim().isEmpty()) {
                    showWarning("Title is required.");
                    return null;
                }
                if (artistBox.getValue() == null) {
                    showWarning("Please select an artist.");
                    return null;
                }
                Artwork result = new Artwork();
                result.setTitle(titleField.getText().trim());
                result.setType(typeField.getText().trim());
                result.setMedium(mediumField.getText().trim());

                Artist a = new Artist();
                a.setName(artistBox.getValue());
                result.setArtist(a);

                try {
                    if (!priceField.getText().trim().isEmpty())
                        result.setPrice(Double.parseDouble(
                                priceField.getText()
                                        .trim()
                                        .replace(",", ".")));
                } catch (NumberFormatException e) {
                    showWarning("Price must be a number.");
                    return null;
                }
                try {
                    if (!yearField.getText().trim().isEmpty())
                        result.setCreationYear(
                                Integer.parseInt(
                                        yearField.getText()
                                                .trim()));
                } catch (NumberFormatException e) {
                    showWarning("Year must be a number.");
                    return null;
                }
                if (statusBox.getValue() != null)
                    result.setStatus(Artwork.Status.valueOf(
                            statusBox.getValue()));
                return result;
            }
            return null;
        });

        return dialog;
    }

    // ─── HELPERS ────────────────────────────────────────────
    private void refreshTable() {
        artworkTable.setItems(FXCollections.observableArrayList(
                artworkService.getAllArtworks()));
    }

    private double getAverageRating(String artworkTitle) {
        String sql = """
                SELECT AVG(r.rating) AS avg_rating
                FROM review r
                JOIN artwork a
                    ON r.artwork_id = a.artwork_id
                WHERE a.title = ?
                """;
        try (Connection conn =
                     ConnectionManager.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {
            stmt.setString(1, artworkTitle);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return rs.getDouble("avg_rating");
        } catch (Exception e) {
            System.err.println("Error getting avg rating: "
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