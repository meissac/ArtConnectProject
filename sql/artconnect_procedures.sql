-- ================================================
-- ArtConnect Stored Procedures and Functions
-- ================================================

USE artconnect_db;

DELIMITER $$

-- ------------------------------------------------
-- Procedure 1: sp_register_member_to_workshop
-- Purpose: Registers a community member to a
-- workshop by creating a booking record.
-- Checks that the member and workshop exist first.
-- The capacity check is handled by the trigger.
-- ------------------------------------------------
CREATE PROCEDURE sp_register_member_to_workshop(
    IN p_workshop_id INT,
    IN p_member_id INT,
    IN p_payment_status VARCHAR(20)
)
BEGIN
    DECLARE member_exists INT;
    DECLARE workshop_exists INT;

    -- Check if member exists
    SELECT COUNT(*) INTO member_exists
    FROM community_member
    WHERE member_id = p_member_id;

    -- Check if workshop exists
    SELECT COUNT(*) INTO workshop_exists
    FROM workshop
    WHERE workshop_id = p_workshop_id;

    IF member_exists = 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Error: Member not found';
    ELSEIF workshop_exists = 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Error: Workshop not found';
    ELSE
        -- Create the booking
        -- The trg_check_booking_capacity trigger
        -- will automatically check capacity
        INSERT INTO booking (workshop_id, member_id, payment_status)
        VALUES (p_workshop_id, p_member_id, p_payment_status);

        SELECT 'Booking created successfully' AS message,
               LAST_INSERT_ID() AS booking_id;
    END IF;
END$$

-- ------------------------------------------------
-- Procedure 2: sp_get_artist_portfolio
-- Purpose: Returns the complete portfolio of an
-- artist: their info, artworks, and exhibitions
-- they participated in.
-- Useful for the artist profile page.
-- ------------------------------------------------
CREATE PROCEDURE sp_get_artist_portfolio(
    IN p_artist_name VARCHAR(100)
)
BEGIN
    -- Artist information
    SELECT
        a.artist_id,
        a.name,
        a.bio,
        a.city,
        a.contact_email,
        a.website
    FROM artist a
    WHERE a.name = p_artist_name;

    -- Their artworks
    SELECT
        aw.title,
        aw.type,
        aw.creation_year,
        aw.price,
        aw.status
    FROM artwork aw
    JOIN artist a ON aw.artist_id = a.artist_id
    WHERE a.name = p_artist_name;

    -- Exhibitions their artworks appear in
    SELECT DISTINCT
        e.title AS exhibition_title,
        e.start_date,
        e.end_date,
        g.name AS gallery_name
    FROM exhibition e
    JOIN gallery g ON e.gallery_id = g.gallery_id
    JOIN exhibition_artwork ea ON e.exhibition_id = ea.exhibition_id
    JOIN artwork aw ON ea.artwork_id = aw.artwork_id
    JOIN artist a ON aw.artist_id = a.artist_id
    WHERE a.name = p_artist_name;
END$$

-- ------------------------------------------------
-- Function: fn_count_workshop_participants
-- Purpose: Returns the number of active (non-cancelled)
-- bookings for a given workshop.
-- Can be used directly in SELECT queries.
-- ------------------------------------------------
CREATE FUNCTION fn_count_workshop_participants(
    p_workshop_id INT
)
RETURNS INT
DETERMINISTIC
BEGIN
    DECLARE participant_count INT;

    SELECT COUNT(*) INTO participant_count
    FROM booking
    WHERE workshop_id = p_workshop_id
    AND payment_status != 'CANCELLED';

    RETURN participant_count;
END$$

DELIMITER ;