package com.project.artconnect.persistence;

import com.project.artconnect.dao.ArtworkDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of ArtworkDao.
 */
public class JdbcArtworkDao implements ArtworkDao {

    /**
     * Retrieves all artworks with their associated artist.
     */
    @Override
    public List<Artwork> findAll() {
        String sql = """
                SELECT aw.artwork_id, aw.title, aw.creation_year,
                       aw.type, aw.medium, aw.dimensions,
                       aw.description, aw.price, aw.status,
                       a.name AS artist_name, a.city AS artist_city
                FROM artwork aw
                JOIN artist a ON aw.artist_id = a.artist_id
                ORDER BY aw.artwork_id
                """;

        List<Artwork> artworks = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                artworks.add(mapRowToArtwork(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error fetching artworks: " + e.getMessage());
        }

        return artworks;
    }

    /**
     * Saves a new artwork to the database.
     * Looks up the artist_id using the artist's name.
     */
    @Override
    public void save(Artwork artwork) {
        // First get the artist_id from the artist name
        String findArtist = "SELECT artist_id FROM artist WHERE name = ?";
        String sql = """
                INSERT INTO artwork
                    (title, creation_year, type, medium,
                     dimensions, description, price, status, artist_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement findStmt =
                     conn.prepareStatement(findArtist)) {

            findStmt.setString(1, artwork.getArtist().getName());
            ResultSet rs = findStmt.executeQuery();

            if (rs.next()) {
                int artistId = rs.getInt("artist_id");

                try (PreparedStatement insertStmt =
                             conn.prepareStatement(sql)) {
                    insertStmt.setString(1, artwork.getTitle());
                    insertStmt.setInt(2, artwork.getCreationYear() != null
                            ? artwork.getCreationYear() : 0);
                    insertStmt.setString(3, artwork.getType());
                    insertStmt.setString(4, artwork.getMedium());
                    insertStmt.setString(5, artwork.getDimensions());
                    insertStmt.setString(6, artwork.getDescription());
                    insertStmt.setDouble(7, artwork.getPrice());
                    insertStmt.setString(8,
                            artwork.getStatus() != null
                                    ? artwork.getStatus().name()
                                    : "FOR_SALE");
                    insertStmt.setInt(9, artistId);
                    insertStmt.executeUpdate();
                }
            } else {
                System.err.println("Artist not found: "
                        + artwork.getArtist().getName());
            }

        } catch (SQLException e) {
            System.err.println("Error saving artwork: " + e.getMessage());
        }
    }

    /**
     * Updates an existing artwork identified by its title.
     */
    @Override
    public void update(Artwork artwork) {
        String sql = """
                UPDATE artwork
                SET creation_year = ?, type = ?, medium = ?,
                    dimensions = ?, description = ?,
                    price = ?, status = ?
                WHERE title = ?
                """;

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, artwork.getCreationYear() != null
                    ? artwork.getCreationYear() : 0);
            stmt.setString(2, artwork.getType());
            stmt.setString(3, artwork.getMedium());
            stmt.setString(4, artwork.getDimensions());
            stmt.setString(5, artwork.getDescription());
            stmt.setDouble(6, artwork.getPrice());
            stmt.setString(7, artwork.getStatus() != null
                    ? artwork.getStatus().name() : "FOR_SALE");
            stmt.setString(8, artwork.getTitle());

            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error updating artwork: " + e.getMessage());
        }
    }

    /**
     * Deletes an artwork by title.
     */
    @Override
    public void delete(String title) {
        String sql = "DELETE FROM artwork WHERE title = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, title);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error deleting artwork: " + e.getMessage());
        }
    }

    /**
     * Returns all artworks by a specific artist name.
     */
    @Override
    public List<Artwork> findByArtistName(String artistName) {
        String sql = """
                SELECT aw.artwork_id, aw.title, aw.creation_year,
                       aw.type, aw.medium, aw.dimensions,
                       aw.description, aw.price, aw.status,
                       a.name AS artist_name, a.city AS artist_city
                FROM artwork aw
                JOIN artist a ON aw.artist_id = a.artist_id
                WHERE a.name = ?
                ORDER BY aw.artwork_id
                """;

        List<Artwork> artworks = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, artistName);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                artworks.add(mapRowToArtwork(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error fetching artworks by artist: "
                    + e.getMessage());
        }

        return artworks;
    }

    /**
     * Helper method: maps one ResultSet row to one Artwork object.
     * Avoids repeating the same mapping code in every method.
     */
    private Artwork mapRowToArtwork(ResultSet rs) throws SQLException {
        Artwork artwork = new Artwork();
        artwork.setTitle(rs.getString("title"));
        artwork.setCreationYear(rs.getInt("creation_year"));
        artwork.setType(rs.getString("type"));
        artwork.setMedium(rs.getString("medium"));
        artwork.setDimensions(rs.getString("dimensions"));
        artwork.setDescription(rs.getString("description"));
        artwork.setPrice(rs.getDouble("price"));

        // Convert the status string from DB back to the Java enum
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            artwork.setStatus(Artwork.Status.valueOf(statusStr));
        }

        // Build a minimal Artist object with just the name
        // (enough for the UI to display)
        Artist artist = new Artist();
        artist.setName(rs.getString("artist_name"));
        artist.setCity(rs.getString("artist_city"));
        artwork.setArtist(artist);

        return artwork;
    }
}