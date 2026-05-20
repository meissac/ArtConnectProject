-- ================================================
-- ArtConnect Indexes
-- ================================================

USE artconnect_db;

-- ------------------------------------------------
-- Index 1: idx_artwork_artist
-- Table: artwork
-- Column: artist_id
-- Reason: The application frequently queries artworks
-- by artist (e.g. "show all artworks by Monet").
-- This index speeds up that JOIN and WHERE clause.
-- ------------------------------------------------
CREATE INDEX idx_artwork_artist
    ON artwork(artist_id);

-- ------------------------------------------------
-- Index 2: idx_booking_workshop
-- Table: booking
-- Column: workshop_id
-- Reason: The application frequently looks up all
-- bookings for a given workshop to check capacity
-- and list participants.
-- ------------------------------------------------
CREATE INDEX idx_booking_workshop
    ON booking(workshop_id);

-- ------------------------------------------------
-- Index 3: idx_artist_city
-- Table: artist
-- Column: city
-- Reason: The application allows filtering artists
-- by city. This index speeds up that WHERE clause
-- significantly as the artist table grows.
-- ------------------------------------------------
CREATE INDEX idx_artist_city
    ON artist(city);