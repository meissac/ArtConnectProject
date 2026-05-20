-- ================================================
-- ArtConnect Database Schema
-- ================================================

CREATE DATABASE IF NOT EXISTS artconnect_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE artconnect_db;

-- ------------------------------------------------
-- Table: discipline
-- ------------------------------------------------
CREATE TABLE discipline (
    discipline_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- ------------------------------------------------
-- Table: artwork_tag
-- ------------------------------------------------
CREATE TABLE artwork_tag (
    tag_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- ------------------------------------------------
-- Table: gallery
-- ------------------------------------------------
CREATE TABLE gallery (
    gallery_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(200),
    owner_name VARCHAR(100),
    opening_hours VARCHAR(100),
    contact_phone VARCHAR(20),
    rating DECIMAL(3,1),
    website VARCHAR(200)
);

-- ------------------------------------------------
-- Table: artist
-- ------------------------------------------------
CREATE TABLE artist (
    artist_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    bio TEXT,
    birth_year INT,
    contact_email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    city VARCHAR(100),
    website VARCHAR(200),
    social_media VARCHAR(200),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- ------------------------------------------------
-- Table: artwork
-- ------------------------------------------------
CREATE TABLE artwork (
    artwork_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    creation_year INT,
    type VARCHAR(100),
    medium VARCHAR(100),
    dimensions VARCHAR(100),
    description TEXT,
    price DECIMAL(15,2),
    status ENUM('FOR_SALE','SOLD','EXHIBITED') NOT NULL DEFAULT 'FOR_SALE',
    artist_id INT NOT NULL,
    CONSTRAINT fk_artwork_artist
        FOREIGN KEY (artist_id) REFERENCES artist(artist_id)
        ON DELETE CASCADE
);

-- ------------------------------------------------
-- Table: exhibition
-- ------------------------------------------------
CREATE TABLE exhibition (
    exhibition_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    description TEXT,
    curator_name VARCHAR(100),
    theme VARCHAR(100),
    gallery_id INT NOT NULL,
    CONSTRAINT fk_exhibition_gallery
        FOREIGN KEY (gallery_id) REFERENCES gallery(gallery_id)
        ON DELETE CASCADE
);

-- ------------------------------------------------
-- Table: workshop
-- ------------------------------------------------
CREATE TABLE workshop (
    workshop_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    date DATETIME NOT NULL,
    duration_minutes INT,
    max_participants INT,
    price DECIMAL(10,2),
    location VARCHAR(200),
    description TEXT,
    level ENUM('beginner','intermediate','advanced'),
    instructor_id INT NOT NULL,
    CONSTRAINT fk_workshop_instructor
        FOREIGN KEY (instructor_id) REFERENCES artist(artist_id)
        ON DELETE CASCADE
);

-- ------------------------------------------------
-- Table: community_member
-- ------------------------------------------------
CREATE TABLE community_member (
    member_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    birth_year INT,
    phone VARCHAR(20),
    city VARCHAR(100),
    membership_type ENUM('free','premium') NOT NULL DEFAULT 'free'
);

-- ------------------------------------------------
-- Table: booking
-- ------------------------------------------------
CREATE TABLE booking (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    workshop_id INT NOT NULL,
    member_id INT NOT NULL,
    booking_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_status ENUM('PENDING','PAID','CANCELLED') NOT NULL DEFAULT 'PENDING',
    CONSTRAINT fk_booking_workshop
        FOREIGN KEY (workshop_id) REFERENCES workshop(workshop_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_booking_member
        FOREIGN KEY (member_id) REFERENCES community_member(member_id)
        ON DELETE CASCADE
);

-- ------------------------------------------------
-- Table: review
-- ------------------------------------------------
CREATE TABLE review (
    review_id INT AUTO_INCREMENT PRIMARY KEY,
    member_id INT NOT NULL,
    artwork_id INT NOT NULL,
    rating INT NOT NULL,
    comment TEXT,
    review_date DATE NOT NULL DEFAULT (CURRENT_DATE),
    CONSTRAINT fk_review_member
        FOREIGN KEY (member_id) REFERENCES community_member(member_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_review_artwork
        FOREIGN KEY (artwork_id) REFERENCES artwork(artwork_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_rating CHECK (rating BETWEEN 1 AND 5)
);

-- ------------------------------------------------
-- Junction: artist_discipline
-- ------------------------------------------------
CREATE TABLE artist_discipline (
    artist_id INT NOT NULL,
    discipline_id INT NOT NULL,
    PRIMARY KEY (artist_id, discipline_id),
    CONSTRAINT fk_ad_artist
        FOREIGN KEY (artist_id) REFERENCES artist(artist_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_ad_discipline
        FOREIGN KEY (discipline_id) REFERENCES discipline(discipline_id)
        ON DELETE CASCADE
);

-- ------------------------------------------------
-- Junction: member_discipline
-- ------------------------------------------------
CREATE TABLE member_discipline (
    member_id INT NOT NULL,
    discipline_id INT NOT NULL,
    PRIMARY KEY (member_id, discipline_id),
    CONSTRAINT fk_md_member
        FOREIGN KEY (member_id) REFERENCES community_member(member_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_md_discipline
        FOREIGN KEY (discipline_id) REFERENCES discipline(discipline_id)
        ON DELETE CASCADE
);

-- ------------------------------------------------
-- Junction: exhibition_artwork
-- ------------------------------------------------
CREATE TABLE exhibition_artwork (
    exhibition_id INT NOT NULL,
    artwork_id INT NOT NULL,
    PRIMARY KEY (exhibition_id, artwork_id),
    CONSTRAINT fk_ea_exhibition
        FOREIGN KEY (exhibition_id) REFERENCES exhibition(exhibition_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_ea_artwork
        FOREIGN KEY (artwork_id) REFERENCES artwork(artwork_id)
        ON DELETE CASCADE
);

-- ------------------------------------------------
-- Junction: artwork_artwork_tag
-- ------------------------------------------------
CREATE TABLE artwork_artwork_tag (
    artwork_id INT NOT NULL,
    tag_id INT NOT NULL,
    PRIMARY KEY (artwork_id, tag_id),
    CONSTRAINT fk_aat_artwork
        FOREIGN KEY (artwork_id) REFERENCES artwork(artwork_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_aat_tag
        FOREIGN KEY (tag_id) REFERENCES artwork_tag(tag_id)
        ON DELETE CASCADE
);