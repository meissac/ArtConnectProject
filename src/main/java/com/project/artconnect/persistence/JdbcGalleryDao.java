package com.project.artconnect.persistence;

import com.project.artconnect.dao.GalleryDao;
import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC implementation of GalleryDao.
 */
public class JdbcGalleryDao implements GalleryDao {

    @Override
    public Optional<Gallery> findById(Long id) {
        String sql = """
                SELECT g.gallery_id, g.name, g.address,
                       g.owner_name, g.opening_hours,
                       g.contact_phone, g.rating, g.website,
                       e.exhibition_id, e.title AS exhibition_title,
                       e.start_date, e.end_date, e.theme
                FROM gallery g
                LEFT JOIN exhibition e ON g.gallery_id = e.gallery_id
                WHERE g.gallery_id = ?
                """;

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            Gallery gallery = null;
            while (rs.next()) {
                if (gallery == null) {
                    gallery = mapRowToGallery(rs);
                }
                // Add exhibition if present
                int exhibitionId = rs.getInt("exhibition_id");
                if (exhibitionId != 0) {
                    Exhibition exhibition = new Exhibition();
                    exhibition.setTitle(rs.getString("exhibition_title"));
                    exhibition.setStartDate(rs.getDate("start_date")
                            .toLocalDate());
                    exhibition.setEndDate(rs.getDate("end_date")
                            .toLocalDate());
                    exhibition.setTheme(rs.getString("theme"));
                    exhibition.setGallery(gallery);
                    gallery.getExhibitions().add(exhibition);
                }
            }
            return Optional.ofNullable(gallery);

        } catch (SQLException e) {
            System.err.println("Error fetching gallery by id: "
                    + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<Gallery> findAll() {
        String sql = """
                SELECT g.gallery_id, g.name, g.address,
                       g.owner_name, g.opening_hours,
                       g.contact_phone, g.rating, g.website,
                       e.exhibition_id, e.title AS exhibition_title,
                       e.start_date, e.end_date, e.theme
                FROM gallery g
                LEFT JOIN exhibition e ON g.gallery_id = e.gallery_id
                ORDER BY g.gallery_id
                """;

        Map<Integer, Gallery> galleryMap = new LinkedHashMap<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int galleryId = rs.getInt("gallery_id");

                if (!galleryMap.containsKey(galleryId)) {
                    galleryMap.put(galleryId, mapRowToGallery(rs));
                }

                int exhibitionId = rs.getInt("exhibition_id");
                if (exhibitionId != 0) {
                    Gallery gallery = galleryMap.get(galleryId);
                    Exhibition exhibition = new Exhibition();
                    exhibition.setTitle(rs.getString("exhibition_title"));
                    exhibition.setStartDate(rs.getDate("start_date")
                            .toLocalDate());
                    exhibition.setEndDate(rs.getDate("end_date")
                            .toLocalDate());
                    exhibition.setTheme(rs.getString("theme"));
                    exhibition.setGallery(gallery);
                    gallery.getExhibitions().add(exhibition);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching galleries: " + e.getMessage());
        }

        return new ArrayList<>(galleryMap.values());
    }

    private Gallery mapRowToGallery(ResultSet rs) throws SQLException {
        Gallery gallery = new Gallery();
        gallery.setName(rs.getString("name"));
        gallery.setAddress(rs.getString("address"));
        gallery.setOwnerName(rs.getString("owner_name"));
        gallery.setOpeningHours(rs.getString("opening_hours"));
        gallery.setContactPhone(rs.getString("contact_phone"));
        gallery.setRating(rs.getDouble("rating"));
        gallery.setWebsite(rs.getString("website"));
        return gallery;
    }
}