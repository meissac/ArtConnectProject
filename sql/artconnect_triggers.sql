-- ================================================
-- ArtConnect Triggers
-- ================================================

USE artconnect_db;

-- Change delimiter so MySQL doesn't confuse
-- the semicolons inside triggers with the end
-- of the CREATE TRIGGER statement
DELIMITER $$

-- ------------------------------------------------
-- Trigger 1: trg_check_exhibition_dates
-- Fires: BEFORE INSERT on exhibition
-- Purpose: Ensures the end date of an exhibition
-- is always after its start date.
-- Prevents data inconsistency at the database level.
-- ------------------------------------------------
CREATE TRIGGER trg_check_exhibition_dates
BEFORE INSERT ON exhibition
FOR EACH ROW
BEGIN
    IF NEW.end_date <= NEW.start_date THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Error: Exhibition end_date must be after start_date';
    END IF;
END$$

-- ------------------------------------------------
-- Trigger 2: trg_check_booking_capacity
-- Fires: BEFORE INSERT on booking
-- Purpose: Ensures a workshop cannot be booked
-- beyond its maximum number of participants.
-- Counts only non-cancelled bookings.
-- ------------------------------------------------
CREATE TRIGGER trg_check_booking_capacity
BEFORE INSERT ON booking
FOR EACH ROW
BEGIN
    DECLARE current_bookings INT;
    DECLARE max_cap INT;

    -- Count existing non-cancelled bookings
    SELECT COUNT(*) INTO current_bookings
    FROM booking
    WHERE workshop_id = NEW.workshop_id
    AND payment_status != 'CANCELLED';

    -- Get the workshop capacity
    SELECT max_participants INTO max_cap
    FROM workshop
    WHERE workshop_id = NEW.workshop_id;

    -- Block the insert if already full
    IF current_bookings >= max_cap THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Error: This workshop is fully booked';
    END IF;
END$$

-- ------------------------------------------------
-- Trigger 3: trg_check_review_rating
-- Fires: BEFORE INSERT on review
-- Purpose: Ensures the rating value is between
-- 1 and 5. Adds a second layer of validation
-- on top of the CHECK constraint.
-- Also sets review_date to today if not provided.
-- ------------------------------------------------
CREATE TRIGGER trg_check_review_rating
BEFORE INSERT ON review
FOR EACH ROW
BEGIN
    IF NEW.rating < 1 OR NEW.rating > 5 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Error: Rating must be between 1 and 5';
    END IF;

    IF NEW.review_date IS NULL THEN
        SET NEW.review_date = CURRENT_DATE();
    END IF;
END$$

-- Reset delimiter back to normal
DELIMITER ;