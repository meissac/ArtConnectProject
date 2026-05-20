package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.util.ConnectionManager;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleIntegerProperty;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ArtistController {

    @FXML private TextField searchField;
    @FXML private ComboBox<Discipline> disciplineFilter;
    @FXML private TableView<Artist> artistTable;
    @FXML private TableColumn<Artist, String> nameColumn;
    @FXML private TableColumn<Artist, String> cityColumn;
    @FXML private TableColumn<Artist, String> emailColumn;
    @FXML private TableColumn<Artist, Integer> yearColumn;
    @FXML private TableColumn<Artist, String> disciplinesColumn;
    @FXML private TableColumn<Artist, Number> artworkCountColumn;

    private final ArtistService artistService =
            ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(
                new PropertyValueFactory<>("name"));
        cityColumn.setCellValueFactory(
                new PropertyValueFactory<>("city"));
        emailColumn.setCellValueFactory(
                new PropertyValueFactory<>("contactEmail"));
        yearColumn.setCellValueFactory(
                new PropertyValueFactory<>("birthYear"));

        disciplinesColumn.setCellValueFactory(cellData -> {
            String disciplines = cellData.getValue()
                    .getDisciplines().stream()
                    .map(Discipline::getName)
                    .collect(Collectors.joining(", "));
            return new SimpleStringProperty(disciplines);
        });

        artworkCountColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(
                        getArtworkCount(
                                cellData.getValue().getName())));

        disciplineFilter.setItems(FXCollections
                .observableArrayList(
                        artistService.getAllDisciplines()));
        refreshTable();
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText();
        Discipline d = disciplineFilter.getValue();
        String dName = (d != null) ? d.getName() : null;
        artistTable.setItems(FXCollections.observableArrayList(
                artistService.searchArtists(query, dName, null)));
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        disciplineFilter.setValue(null);
        refreshTable();
    }

    // ─── ADD ────────────────────────────────────────────────
    @FXML
    private void handleAdd() {
        Dialog<Artist> dialog = buildArtistDialog(null);
        Optional<Artist> result = dialog.showAndWait();
        result.ifPresent(artist -> {
            artistService.createArtist(artist);
            refreshTable();
            showInfo("Artist \"" + artist.getName()
                    + "\" added successfully.");
        });
    }

    // ─── EDIT ───────────────────────────────────────────────
    @FXML
    private void handleEdit() {
        Artist selected = artistTable.getSelectionModel()
                .getSelectedItem();
        if (selected == null) {
            showWarning("Please select an artist to edit.");
            return;
        }
        Dialog<Artist> dialog = buildArtistDialog(selected);
        Optional<Artist> result = dialog.showAndWait();
        result.ifPresent(artist -> {
            artistService.updateArtist(artist);
            // Update disciplines separately
            updateArtistDisciplines(artist);
            refreshTable();
            showInfo("Artist \"" + artist.getName()
                    + "\" updated successfully.");
        });
    }

    // ─── DELETE ─────────────────────────────────────────────
    @FXML
    private void handleDelete() {
        Artist selected = artistTable.getSelectionModel()
                .getSelectedItem();
        if (selected == null) {
            showWarning("Please select an artist to delete.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Artist");
        confirm.setHeaderText("Delete \""
                + selected.getName() + "\"?");
        confirm.setContentText(
                "This will also delete all their artworks. " +
                        "This action cannot be undone.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                artistService.deleteArtist(selected.getName());
                refreshTable();
                showInfo("Artist deleted successfully.");
            }
        });
    }

    // ─── DIALOG BUILDER ─────────────────────────────────────
    private Dialog<Artist> buildArtistDialog(Artist artist) {
        Dialog<Artist> dialog = new Dialog<>();
        dialog.setTitle(artist == null
                ? "Add New Artist" : "Edit Artist");
        dialog.setHeaderText(artist == null
                ? "Fill in the artist details"
                : "Modify the artist details");

        // Basic fields
        TextField nameField = new TextField();
        nameField.setPromptText("Full name");
        TextField cityField = new TextField();
        cityField.setPromptText("City");
        TextField emailField = new TextField();
        emailField.setPromptText("Email address");
        TextField yearField = new TextField();
        yearField.setPromptText("Birth year (e.g. 1990)");
        TextField bioField = new TextField();
        bioField.setPromptText("Short biography");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone number");

        // Discipline multi-selection list
        // Load all available disciplines from the database
        List<String> allDisciplineNames =
                loadAllDisciplineNames();
        ListView<String> disciplineListView =
                new ListView<>();
        disciplineListView.getItems().addAll(allDisciplineNames);
        disciplineListView.getSelectionModel()
                .setSelectionMode(SelectionMode.MULTIPLE);
        disciplineListView.setPrefHeight(120);

        // Pre-fill fields if editing
        if (artist != null) {
            nameField.setText(artist.getName());
            nameField.setEditable(false);
            cityField.setText(artist.getCity());
            emailField.setText(artist.getContactEmail());
            if (artist.getBirthYear() != null)
                yearField.setText(String.valueOf(
                        artist.getBirthYear()));
            bioField.setText(artist.getBio());
            phoneField.setText(artist.getPhone());

            // Pre-select existing disciplines
            List<String> existing = artist.getDisciplines()
                    .stream()
                    .map(Discipline::getName)
                    .collect(Collectors.toList());
            for (int i = 0; i < allDisciplineNames.size(); i++) {
                if (existing.contains(
                        allDisciplineNames.get(i))) {
                    disciplineListView.getSelectionModel()
                            .select(i);
                }
            }
        }

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("City:"), 0, 1);
        grid.add(cityField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Birth Year:"), 0, 3);
        grid.add(yearField, 1, 3);
        grid.add(new Label("Bio:"), 0, 4);
        grid.add(bioField, 1, 4);
        grid.add(new Label("Phone:"), 0, 5);
        grid.add(phoneField, 1, 5);
        grid.add(new Label("Disciplines\n(Ctrl+click\nto select multiple):"),
                0, 6);
        grid.add(disciplineListView, 1, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK, ButtonType.CANCEL);

        // Convert result to Artist on OK
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                if (nameField.getText().trim().isEmpty()) {
                    showWarning("Name is required.");
                    return null;
                }
                Artist result = new Artist();
                result.setName(nameField.getText().trim());
                result.setCity(cityField.getText().trim());
                result.setContactEmail(
                        emailField.getText().trim());
                result.setBio(bioField.getText().trim());
                result.setPhone(phoneField.getText().trim());
                result.setActive(true);

                try {
                    if (!yearField.getText().trim().isEmpty())
                        result.setBirthYear(Integer.parseInt(
                                yearField.getText().trim()));
                } catch (NumberFormatException e) {
                    showWarning("Birth year must be a number.");
                    return null;
                }

                // Collect selected disciplines
                List<String> selectedDisciplines =
                        disciplineListView.getSelectionModel()
                                .getSelectedItems();
                for (String dName : selectedDisciplines) {
                    result.getDisciplines()
                            .add(new Discipline(dName));
                }

                return result;
            }
            return null;
        });

        return dialog;
    }

    // ─── UPDATE DISCIPLINES IN DATABASE ─────────────────────
    /**
     * When editing an artist, we:
     * 1. Delete all their existing discipline links
     * 2. Re-insert the newly selected ones
     * This is the simplest and safest approach.
     */
    private void updateArtistDisciplines(Artist artist) {
        String getArtistId =
                "SELECT artist_id FROM artist WHERE name = ?";
        String deleteSql =
                "DELETE FROM artist_discipline " +
                        "WHERE artist_id = ?";
        String getDisciplineId =
                "SELECT discipline_id FROM discipline " +
                        "WHERE name = ?";
        String insertSql =
                "INSERT IGNORE INTO artist_discipline " +
                        "(artist_id, discipline_id) VALUES (?, ?)";

        try (Connection conn =
                     ConnectionManager.getConnection()) {

            // Step 1: get artist_id
            int artistId = -1;
            try (PreparedStatement stmt =
                         conn.prepareStatement(getArtistId)) {
                stmt.setString(1, artist.getName());
                ResultSet rs = stmt.executeQuery();
                if (rs.next())
                    artistId = rs.getInt("artist_id");
            }

            if (artistId == -1) return;

            // Step 2: delete existing discipline links
            try (PreparedStatement stmt =
                         conn.prepareStatement(deleteSql)) {
                stmt.setInt(1, artistId);
                stmt.executeUpdate();
            }

            // Step 3: re-insert selected disciplines
            for (Discipline discipline :
                    artist.getDisciplines()) {
                try (PreparedStatement findStmt =
                             conn.prepareStatement(
                                     getDisciplineId)) {
                    findStmt.setString(1,
                            discipline.getName());
                    ResultSet rs = findStmt.executeQuery();
                    if (rs.next()) {
                        int disciplineId =
                                rs.getInt("discipline_id");
                        try (PreparedStatement insertStmt =
                                     conn.prepareStatement(
                                             insertSql)) {
                            insertStmt.setInt(1, artistId);
                            insertStmt.setInt(2, disciplineId);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }

        } catch (Exception e) {
            showWarning("Error updating disciplines: "
                    + e.getMessage());
        }
    }

    // ─── LOAD ALL DISCIPLINES FROM DB ───────────────────────
    private List<String> loadAllDisciplineNames() {
        List<String> names = new ArrayList<>();
        String sql =
                "SELECT name FROM discipline ORDER BY name";
        try (Connection conn =
                     ConnectionManager.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next())
                names.add(rs.getString("name"));
        } catch (Exception e) {
            System.err.println(
                    "Error loading disciplines: "
                            + e.getMessage());
        }
        return names;
    }

    // ─── HELPERS ────────────────────────────────────────────
    private void refreshTable() {
        artistTable.setItems(FXCollections.observableArrayList(
                artistService.getAllArtists()));
    }

    private int getArtworkCount(String artistName) {
        String sql = """
                SELECT total_artworks
                FROM view_artist_summary
                WHERE artist_name = ?
                """;
        try (Connection conn =
                     ConnectionManager.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql)) {
            stmt.setString(1, artistName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return rs.getInt("total_artworks");
        } catch (Exception e) {
            System.err.println("Error getting artwork count: "
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