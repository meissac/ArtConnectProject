package com.project.artconnect.persistence;

import com.project.artconnect.dao.ArtistDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC implementation of ArtistDao.
 * Connects to MySQL and performs real CRUD operations
 * on the artist table.
 */
public class JdbcArtistDao implements ArtistDao {

    /**
     * Retrieves all artists from the database,
     * including their disciplines.
     */
    @Override
    public List<Artist> findAll() {
        // This query joins artist with their disciplines
        // We use LEFT JOIN so artists with no disciplines
        // are still returned
        String sql = """
                SELECT a.artist_id, a.name, a.bio, a.birth_year,
                       a.contact_email, a.phone, a.city,
                       a.website, a.social_media, a.is_active,
                       d.name AS discipline_name
                FROM artist a
                LEFT JOIN artist_discipline ad ON a.artist_id = ad.artist_id
                LEFT JOIN discipline d ON ad.discipline_id = d.discipline_id
                ORDER BY a.artist_id
                """;

        // We use a Map to group disciplines per artist
        // because one artist can appear on multiple rows
        // (one row per discipline)
        Map<Integer, Artist> artistMap = new LinkedHashMap<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int artistId = rs.getInt("artist_id");

                // If we haven't seen this artist yet, create them
                if (!artistMap.containsKey(artistId)) {
                    Artist artist = new Artist();
                    artist.setName(rs.getString("name"));
                    artist.setBio(rs.getString("bio"));
                    artist.setBirthYear(rs.getInt("birth_year"));
                    artist.setContactEmail(rs.getString("contact_email"));
                    artist.setPhone(rs.getString("phone"));
                    artist.setCity(rs.getString("city"));
                    artist.setWebsite(rs.getString("website"));
                    artist.setSocialMedia(rs.getString("social_media"));
                    artist.setActive(rs.getBoolean("is_active"));
                    artistMap.put(artistId, artist);
                }

                // Add discipline if this row has one
                String disciplineName = rs.getString("discipline_name");
                if (disciplineName != null) {
                    artistMap.get(artistId)
                            .getDisciplines()
                            .add(new Discipline(disciplineName));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching artists: " + e.getMessage());
        }

        return new ArrayList<>(artistMap.values());
    }

    /**
     * Saves a new artist to the database.
     * Also saves their disciplines in artist_discipline.
     */
    @Override
    public void save(Artist artist) {
        String sql = """
                INSERT INTO artist
                    (name, bio, birth_year, contact_email,
                     phone, city, website, social_media, is_active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        // RETURN_GENERATED_KEYS lets us get the auto-generated
        // artist_id back after the INSERT
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            // Fill in the ? placeholders in order
            stmt.setString(1, artist.getName());
            stmt.setString(2, artist.getBio());
            stmt.setInt(3, artist.getBirthYear() != null
                    ? artist.getBirthYear() : 0);
            stmt.setString(4, artist.getContactEmail());
            stmt.setString(5, artist.getPhone());
            stmt.setString(6, artist.getCity());
            stmt.setString(7, artist.getWebsite());
            stmt.setString(8, artist.getSocialMedia());
            stmt.setBoolean(9, artist.isActive());

            stmt.executeUpdate();

            // Get the generated artist_id
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                int artistId = keys.getInt(1);
                // Save disciplines for this artist
                saveDisciplines(conn, artistId, artist);
            }

        } catch (SQLException e) {
            System.err.println("Error saving artist: " + e.getMessage());
        }
    }

    /**
     * Updates an existing artist in the database.
     * We identify the artist by their name since
     * the Java model has no id field.
     */
    @Override
    public void update(Artist artist) {
        String sql = """
                UPDATE artist
                SET bio = ?, birth_year = ?, contact_email = ?,
                    phone = ?, city = ?, website = ?,
                    social_media = ?, is_active = ?
                WHERE name = ?
                """;

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, artist.getBio());
            stmt.setInt(2, artist.getBirthYear() != null
                    ? artist.getBirthYear() : 0);
            stmt.setString(3, artist.getContactEmail());
            stmt.setString(4, artist.getPhone());
            stmt.setString(5, artist.getCity());
            stmt.setString(6, artist.getWebsite());
            stmt.setString(7, artist.getSocialMedia());
            stmt.setBoolean(8, artist.isActive());
            stmt.setString(9, artist.getName());

            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error updating artist: " + e.getMessage());
        }
    }

    /**
     * Deletes an artist by name from the database.
     * CASCADE constraints handle related artworks
     * and disciplines automatically.
     */
    @Override
    public void delete(String artistName) {
        String sql = "DELETE FROM artist WHERE name = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, artistName);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error deleting artist: " + e.getMessage());
        }
    }

    /**
     * Returns all artists from a specific city.
     */
    @Override
    public List<Artist> findByCity(String city) {
        String sql = """
                SELECT a.artist_id, a.name, a.bio, a.birth_year,
                       a.contact_email, a.phone, a.city,
                       a.website, a.social_media, a.is_active,
                       d.name AS discipline_name
                FROM artist a
                LEFT JOIN artist_discipline ad ON a.artist_id = ad.artist_id
                LEFT JOIN discipline d ON ad.discipline_id = d.discipline_id
                WHERE a.city = ?
                ORDER BY a.artist_id
                """;

        Map<Integer, Artist> artistMap = new LinkedHashMap<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, city);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int artistId = rs.getInt("artist_id");

                if (!artistMap.containsKey(artistId)) {
                    Artist artist = new Artist();
                    artist.setName(rs.getString("name"));
                    artist.setBio(rs.getString("bio"));
                    artist.setBirthYear(rs.getInt("birth_year"));
                    artist.setContactEmail(rs.getString("contact_email"));
                    artist.setPhone(rs.getString("phone"));
                    artist.setCity(rs.getString("city"));
                    artist.setWebsite(rs.getString("website"));
                    artist.setSocialMedia(rs.getString("social_media"));
                    artist.setActive(rs.getBoolean("is_active"));
                    artistMap.put(artistId, artist);
                }

                String disciplineName = rs.getString("discipline_name");
                if (disciplineName != null) {
                    artistMap.get(artistId)
                            .getDisciplines()
                            .add(new Discipline(disciplineName));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching artists by city: "
                    + e.getMessage());
        }

        return new ArrayList<>(artistMap.values());
    }

    /**
     * Helper method: saves the disciplines of an artist
     * into the artist_discipline junction table.
     * Uses the same connection to avoid opening a new one.
     */
    private void saveDisciplines(Connection conn,
                                 int artistId,
                                 Artist artist) throws SQLException {
        // First find the discipline_id for each discipline name
        String findDiscipline =
                "SELECT discipline_id FROM discipline WHERE name = ?";
        String insertLink =
                "INSERT IGNORE INTO artist_discipline " +
                        "(artist_id, discipline_id) VALUES (?, ?)";

        for (Discipline discipline : artist.getDisciplines()) {
            try (PreparedStatement findStmt =
                         conn.prepareStatement(findDiscipline)) {

                findStmt.setString(1, discipline.getName());
                ResultSet rs = findStmt.executeQuery();

                if (rs.next()) {
                    int disciplineId = rs.getInt("discipline_id");
                    try (PreparedStatement insertStmt =
                                 conn.prepareStatement(insertLink)) {
                        insertStmt.setInt(1, artistId);
                        insertStmt.setInt(2, disciplineId);
                        insertStmt.executeUpdate();
                    }
                }
            }
        }
    }
}