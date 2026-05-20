-- ================================================
-- ArtConnect Transaction Scenario
-- ================================================

USE artconnect_db;

-- ------------------------------------------------
-- Scenario: Register member 2 (Bob Martin) to
-- workshops 1 and 2 at the same time.
-- Both bookings must succeed together or
-- both must be cancelled.
-- If successful, upgrade Bob to premium membership.
-- ------------------------------------------------

START TRANSACTION;

-- Step 1: Book workshop 1 for Bob (member_id = 2)
INSERT INTO booking (workshop_id, member_id, payment_status)
VALUES (1, 2, 'PAID');

-- Step 2: Book workshop 2 for Bob (member_id = 2)
INSERT INTO booking (workshop_id, member_id, payment_status)
VALUES (2, 2, 'PAID');

-- Step 3: Upgrade Bob's membership to premium
-- because he booked multiple workshops
UPDATE community_member
SET membership_type = 'premium'
WHERE member_id = 2;

-- If all 3 steps succeeded, confirm everything
COMMIT;

-- Verify the result
SELECT
    cm.name,
    cm.membership_type,
    w.title AS workshop,
    b.payment_status
FROM booking b
JOIN community_member cm ON b.member_id = cm.member_id
JOIN workshop w ON b.workshop_id = w.workshop_id
WHERE cm.member_id = 2;

-- ------------------------------------------------
-- ROLLBACK Example: Demonstrate that if one
-- booking fails, everything is cancelled
-- ------------------------------------------------

START TRANSACTION;

-- Step 1: Valid booking
INSERT INTO booking (workshop_id, member_id, payment_status)
VALUES (3, 4, 'PAID');

-- Step 2: This will FAIL because workshop_id 999
-- does not exist (foreign key violation)
INSERT INTO booking (workshop_id, member_id, payment_status)
VALUES (999, 4, 'PAID');

-- Because step 2 failed, we roll back step 1 too
-- Nothing is saved
ROLLBACK;

-- Verify nothing was inserted for member 4
-- in workshop 3 from this transaction
SELECT * FROM booking WHERE member_id = 4 AND workshop_id = 3;