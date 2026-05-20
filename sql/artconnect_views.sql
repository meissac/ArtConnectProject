-- ================================================
-- ArtConnect Views
-- ================================================

USE artconnect_db;

-- ------------------------------------------------
-- View 1: view_artist_summary
-- Objective: Show each artist with their disciplines
-- and total number of artworks.
-- Simplifies complex joins for the application layer.
-- ------------------------------------------------
CREATE VIEW view_artist_summary AS
SELECT
    a.artist_id,
    a.name AS artist_name,
    a.city,
    a.is_active,
    GROUP_CONCAT(DISTINCT d.name ORDER BY d.name SEPARATOR ', ') AS disciplines,
    COUNT(DISTINCT aw.artwork_id) AS total_artworks
FROM artist a
LEFT JOIN artist_discipline ad ON a.artist_id = ad.artist_id
LEFT JOIN discipline d ON ad.discipline_id = d.discipline_id
LEFT JOIN artwork aw ON a.artist_id = aw.artist_id
GROUP BY a.artist_id, a.name, a.city, a.is_active;

-- ------------------------------------------------
-- View 2: view_exhibition_details
-- Objective: Show exhibitions with their gallery name,
-- date range, and number of artworks displayed.
-- Simplifies queries for the Discover and Exhibition tabs.
-- ------------------------------------------------
CREATE VIEW view_exhibition_details AS
SELECT
    e.exhibition_id,
    e.title AS exhibition_title,
    e.start_date,
    e.end_date,
    e.theme,
    e.curator_name,
    g.name AS gallery_name,
    g.address AS gallery_address,
    COUNT(DISTINCT ea.artwork_id) AS total_artworks
FROM exhibition e
JOIN gallery g ON e.gallery_id = g.gallery_id
LEFT JOIN exhibition_artwork ea ON e.exhibition_id = ea.exhibition_id
GROUP BY
    e.exhibition_id,
    e.title,
    e.start_date,
    e.end_date,
    e.theme,
    e.curator_name,
    g.name,
    g.address;

-- ------------------------------------------------
-- View 3: view_workshop_bookings
-- Objective: Show workshops with instructor name,
-- available spots, and booking count.
-- Hides sensitive member data while showing
-- useful capacity information.
-- ------------------------------------------------
CREATE VIEW view_workshop_bookings AS
SELECT
    w.workshop_id,
    w.title AS workshop_title,
    w.date,
    w.level,
    w.price,
    w.max_participants,
    a.name AS instructor_name,
    COUNT(DISTINCT b.booking_id) AS total_bookings,
    (w.max_participants - COUNT(DISTINCT b.booking_id)) AS available_spots
FROM workshop w
JOIN artist a ON w.instructor_id = a.artist_id
LEFT JOIN booking b ON w.workshop_id = b.workshop_id
    AND b.payment_status != 'CANCELLED'
GROUP BY
    w.workshop_id,
    w.title,
    w.date,
    w.level,
    w.price,
    w.max_participants,
    a.name;