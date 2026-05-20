package com.project.artconnect.persistence;

import com.project.artconnect.dao.WorkshopDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of WorkshopDao.
 */
public class JdbcWorkshopDao implements WorkshopDao {

    @Override
    public Optional<Workshop> findById(Long id) {
        String sql = """
                SELECT w.workshop_id, w.title, w.date,
                       w.duration_minutes, w.max_participants,
                       w.price, w.location, w.description, w.level,
                       a.name AS instructor_name
                FROM workshop w
                JOIN artist a ON w.instructor_id = a.artist_id
                WHERE w.workshop_id = ?
                """;

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapRowToWorkshop(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error fetching workshop by id: "
                    + e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public List<Workshop> findAll() {
        String sql = """
                SELECT w.workshop_id, w.title, w.date,
                       w.duration_minutes, w.max_participants,
                       w.price, w.location, w.description, w.level,
                       a.name AS instructor_name
                FROM workshop w
                JOIN artist a ON w.instructor_id = a.artist_id
                ORDER BY w.date
                """;

        List<Workshop> workshops = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                workshops.add(mapRowToWorkshop(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error fetching workshops: " + e.getMessage());
        }

        return workshops;
    }

    private Workshop mapRowToWorkshop(ResultSet rs) throws SQLException {
        Workshop workshop = new Workshop();
        workshop.setTitle(rs.getString("title"));

        // Convert SQL DATETIME to Java LocalDateTime
        Timestamp ts = rs.getTimestamp("date");
        if (ts != null) {
            workshop.setDate(ts.toLocalDateTime());
        }

        workshop.setDurationMinutes(rs.getInt("duration_minutes"));
        workshop.setMaxParticipants(rs.getInt("max_participants"));
        workshop.setPrice(rs.getDouble("price"));
        workshop.setLocation(rs.getString("location"));
        workshop.setDescription(rs.getString("description"));
        workshop.setLevel(rs.getString("level"));

        // Build instructor (Artist) with just the name
        Artist instructor = new Artist();
        instructor.setName(rs.getString("instructor_name"));
        workshop.setInstructor(instructor);

        return workshop;
    }
}