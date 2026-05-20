-- ================================================
-- ArtConnect Sample Data
-- ================================================

USE artconnect_db;

-- ------------------------------------------------
-- Disciplines
-- ------------------------------------------------
INSERT INTO discipline (name) VALUES
('Painting'),
('Sculpture'),
('Photography'),
('Digital Art'),
('Music');

-- ------------------------------------------------
-- Artwork Tags
-- ------------------------------------------------
INSERT INTO artwork_tag (name) VALUES
('impressionist'),
('abstract'),
('portrait'),
('landscape'),
('contemporary');

-- ------------------------------------------------
-- Galleries
-- ------------------------------------------------
INSERT INTO gallery (name, address, owner_name, opening_hours, contact_phone, rating, website) VALUES
('Galerie Lumière', '12 Rue de la Paix, Paris', 'Sophie Martin', 'Mon-Sat 10:00-19:00', '01-42-33-44-55', 4.8, 'www.galerielumiere.fr'),
('The Modern Space', '45 Oxford Street, London', 'James Wright', 'Tue-Sun 09:00-18:00', '+44-20-7946-0958', 4.5, 'www.themodernspace.co.uk'),
('Casa Arte', 'Via Roma 88, Milan', 'Giulia Rossi', 'Mon-Fri 10:00-20:00', '+39-02-1234-5678', 4.6, 'www.casaarte.it');

-- ------------------------------------------------
-- Artists
-- ------------------------------------------------
INSERT INTO artist (name, bio, birth_year, contact_email, phone, city, website, social_media, is_active) VALUES
('Leonardo Vinci', 'Renaissance master and polymath, pioneer of painting and sculpture.', 1452, 'leo@vincistudio.it', '+39-055-123456', 'Florence', 'www.vincistudio.it', '@leonardovinci', TRUE),
('Claude Monet', 'Founder of French Impressionist painting, famous for his water lilies series.', 1840, 'claude@monet.fr', '+33-1-23-45-67-89', 'Giverny', 'www.monetatelier.fr', '@claudemonet', TRUE),
('Ansel Adams', 'American landscape photographer and environmentalist known for black and white photography.', 1902, 'ansel@adams.co', '+1-415-555-0123', 'San Francisco', 'www.anseladams.com', '@anseladams', TRUE),
('Frida Kahlo', 'Mexican painter known for her many portraits, self-portraits and works inspired by nature.', 1907, 'frida@kahlo.mx', '+52-55-1234-5678', 'Mexico City', 'www.fridakahlo.mx', '@fridakahlo', TRUE),
('Auguste Rodin', 'French sculptor considered the founder of modern sculpture.', 1840, 'auguste@rodin.fr', '+33-1-44-18-61-10', 'Paris', 'www.musee-rodin.fr', '@augusterodin', TRUE);

-- ------------------------------------------------
-- Artist Disciplines
-- ------------------------------------------------
INSERT INTO artist_discipline (artist_id, discipline_id) VALUES
(1, 1), -- Leonardo: Painting
(1, 2), -- Leonardo: Sculpture
(2, 1), -- Monet: Painting
(3, 3), -- Ansel: Photography
(4, 1), -- Frida: Painting
(5, 2); -- Rodin: Sculpture

-- ------------------------------------------------
-- Artworks
-- ------------------------------------------------
INSERT INTO artwork (title, creation_year, type, medium, dimensions, description, price, status, artist_id) VALUES
('Mona Lisa', 1503, 'Painting', 'Oil on poplar panel', '77 x 53 cm', 'Portrait of Lisa Gherardini, the wife of Francesco del Giocondo.', 850000000.00, 'EXHIBITED', 1),
('The Last Supper', 1498, 'Painting', 'Tempera on gesso', '460 x 880 cm', 'A mural depicting the last meal Jesus shared with his apostles.', 450000000.00, 'EXHIBITED', 1),
('Water Lilies', 1919, 'Painting', 'Oil on canvas', '100 x 200 cm', 'Series of approximately 250 oil paintings depicting Monets flower garden.', 40000000.00, 'EXHIBITED', 2),
('Impression Sunrise', 1872, 'Painting', 'Oil on canvas', '48 x 63 cm', 'The painting that gave the Impressionist movement its name.', 35000000.00, 'FOR_SALE', 2),
('Monolith The Face of Half Dome', 1927, 'Photography', 'Black and white print', '40 x 50 cm', 'Iconic photograph of Half Dome in Yosemite National Park.', 100000.00, 'FOR_SALE', 3),
('The Two Fridas', 1939, 'Painting', 'Oil on canvas', '173 x 173 cm', 'Double self-portrait showing two versions of Frida connected by veins.', 5000000.00, 'EXHIBITED', 4),
('Self Portrait with Thorn Necklace', 1940, 'Painting', 'Oil on canvas', '47 x 39 cm', 'Self-portrait featuring a black cat, hummingbird and thorn necklace.', 3500000.00, 'FOR_SALE', 4),
('The Thinker', 1904, 'Sculpture', 'Bronze', '186 x 98 x 140 cm', 'Bronze sculpture depicting a man in deep contemplation.', 15000000.00, 'EXHIBITED', 5);

-- ------------------------------------------------
-- Artwork Tags
-- ------------------------------------------------
INSERT INTO artwork_artwork_tag (artwork_id, tag_id) VALUES
(1, 3), -- Mona Lisa: portrait
(2, 3), -- Last Supper: portrait
(3, 1), -- Water Lilies: impressionist
(3, 4), -- Water Lilies: landscape
(4, 1), -- Impression Sunrise: impressionist
(4, 4), -- Impression Sunrise: landscape
(5, 4), -- Monolith: landscape
(6, 3), -- Two Fridas: portrait
(7, 3), -- Self Portrait: portrait
(8, 5); -- The Thinker: contemporary

-- ------------------------------------------------
-- Exhibitions
-- ------------------------------------------------
INSERT INTO exhibition (title, start_date, end_date, description, curator_name, theme, gallery_id) VALUES
('Renaissance Masters', '2025-03-01', '2025-06-30', 'A celebration of Renaissance art featuring works by Leonardo Vinci.', 'Marie Dupont', 'Renaissance', 1),
('Impressions of Nature', '2025-04-15', '2025-08-15', 'Exploring the relationship between Impressionism and the natural world.', 'John Smith', 'Impressionism', 2),
('Modern Sculpture Today', '2025-05-01', '2025-09-30', 'Contemporary and classical sculpture from around the world.', 'Anna Ferrari', 'Sculpture', 3),
('Frida and her World', '2025-06-01', '2025-10-31', 'A deep dive into the life and work of Frida Kahlo.', 'Carlos Rivera', 'Mexican Art', 1);

-- ------------------------------------------------
-- Exhibition Artworks
-- ------------------------------------------------
INSERT INTO exhibition_artwork (exhibition_id, artwork_id) VALUES
(1, 1), -- Renaissance Masters: Mona Lisa
(1, 2), -- Renaissance Masters: Last Supper
(2, 3), -- Impressions of Nature: Water Lilies
(2, 4), -- Impressions of Nature: Impression Sunrise
(3, 8), -- Modern Sculpture: The Thinker
(4, 6), -- Frida and her World: The Two Fridas
(4, 7); -- Frida and her World: Self Portrait

-- ------------------------------------------------
-- Workshops
-- ------------------------------------------------
INSERT INTO workshop (title, date, duration_minutes, max_participants, price, location, description, level, instructor_id) VALUES
('Introduction to Oil Painting', '2025-07-10 10:00:00', 180, 15, 75.00, 'Galerie Lumière Studio, Paris', 'Learn the basics of oil painting techniques used by the masters.', 'beginner', 2),
('Black and White Photography', '2025-07-20 14:00:00', 120, 10, 50.00, 'The Modern Space, London', 'Discover the art of black and white photography and darkroom techniques.', 'intermediate', 3),
('Sculpture with Clay', '2025-08-05 09:00:00', 240, 8, 90.00, 'Casa Arte Studio, Milan', 'Hands-on workshop sculpting with clay inspired by Rodin techniques.', 'beginner', 5),
('Advanced Portrait Painting', '2025-08-15 10:00:00', 300, 6, 120.00, 'Galerie Lumière Studio, Paris', 'Advanced techniques for capturing emotion and realism in portrait painting.', 'advanced', 4);

-- ------------------------------------------------
-- Community Members
-- ------------------------------------------------
INSERT INTO community_member (name, email, birth_year, phone, city, membership_type) VALUES
('Alice Dubois', 'alice.dubois@email.fr', 1990, '+33-6-12-34-56-78', 'Paris', 'premium'),
('Bob Martin', 'bob.martin@email.fr', 1985, '+33-6-98-76-54-32', 'Lyon', 'free'),
('Clara Schmidt', 'clara.schmidt@email.de', 1995, '+49-30-1234-5678', 'Berlin', 'premium'),
('David Lopez', 'david.lopez@email.es', 1988, '+34-91-123-4567', 'Madrid', 'free'),
('Emma Wilson', 'emma.wilson@email.co.uk', 1992, '+44-20-7946-1234', 'London', 'premium');

-- ------------------------------------------------
-- Member Disciplines (favorites)
-- ------------------------------------------------
INSERT INTO member_discipline (member_id, discipline_id) VALUES
(1, 1), -- Alice: Painting
(1, 3), -- Alice: Photography
(2, 2), -- Bob: Sculpture
(3, 1), -- Clara: Painting
(3, 4), -- Clara: Digital Art
(4, 5), -- David: Music
(5, 3), -- Emma: Photography
(5, 1); -- Emma: Painting

-- ------------------------------------------------
-- Bookings
-- ------------------------------------------------
INSERT INTO booking (workshop_id, member_id, booking_date, payment_status) VALUES
(1, 1, '2025-06-01 09:00:00', 'PAID'),
(1, 3, '2025-06-02 10:00:00', 'PAID'),
(2, 5, '2025-06-05 11:00:00', 'PAID'),
(2, 1, '2025-06-06 14:00:00', 'PENDING'),
(3, 2, '2025-06-10 09:00:00', 'PAID'),
(4, 3, '2025-06-15 10:00:00', 'PENDING');

-- ------------------------------------------------
-- Reviews
-- ------------------------------------------------
INSERT INTO review (member_id, artwork_id, rating, comment, review_date) VALUES
(1, 1, 5, 'Absolutely breathtaking. The Mona Lisa is even more impressive than I imagined.', '2025-05-01'),
(2, 8, 4, 'The Thinker is a masterpiece of emotion captured in bronze. Truly powerful.', '2025-05-10'),
(3, 3, 5, 'Water Lilies is pure poetry on canvas. Monet was a genius.', '2025-05-15'),
(4, 6, 5, 'The Two Fridas moved me deeply. Raw emotion and incredible technique.', '2025-05-20'),
(5, 5, 4, 'Stunning black and white composition. Adams had an extraordinary eye.', '2025-05-25');