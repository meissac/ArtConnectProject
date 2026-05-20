# ArtConnect Pro

A Java desktop application for managing a local art community,
built as part of the Databases 2 course (TI603I) at INGE1-INT.

The application connects to a real MySQL relational database and
demonstrates a complete database project from conceptual design
to full Java integration.

---

## Course Information

| Field | Detail |
|---|---|
| Course | Databases 2 |
| Code | TI603I |
| Level | INGE1-INT |
| Year | 2025-2026 |

---

## Technologies Used

| Technology | Version | Purpose |
|---|---|---|
| Java | 17+ | Main programming language |
| JavaFX | 17 | Graphical user interface |
| MySQL | 8 | Relational database |
| JDBC | Built-in | Java-to-MySQL communication |
| Maven | 3.9+ | Project and dependency management |
| MySQL Workbench | 8 | Database administration |

---

## Database Overview

The ArtConnect database contains **14 tables** normalized to 3NF:

| Table | Type | Description |
|---|---|---|
| artist | Main entity | Artist profiles |
| artwork | Main entity | Artworks linked to artists |
| gallery | Main entity | Gallery information |
| exhibition | Main entity | Exhibitions linked to galleries |
| workshop | Main entity | Workshops led by artists |
| community_member | Main entity | Community member profiles |
| booking | Link table | Links members to workshops |
| review | Link table | Links members to artworks with ratings |
| discipline | Reference | Discipline names |
| artwork_tag | Reference | Artwork tag names |
| artist_discipline | Junction | Many-to-many: Artist ↔ Discipline |
| member_discipline | Junction | Many-to-many: Member ↔ Discipline |
| exhibition_artwork | Junction | Many-to-many: Exhibition ↔ Artwork |
| artwork_artwork_tag | Junction | Many-to-many: Artwork ↔ Tag |

### Advanced SQL Features

| Feature | Count | Details |
|---|---|---|
| Views | 3 | view_artist_summary, view_exhibition_details, view_workshop_bookings |
| Indexes | 3 | idx_artwork_artist, idx_booking_workshop, idx_artist_city |
| Triggers | 3 | trg_check_exhibition_dates, trg_check_booking_capacity, trg_check_review_rating |
| Stored Procedures | 2 | sp_register_member_to_workshop, sp_get_artist_portfolio |
| Stored Functions | 1 | fn_count_workshop_participants |
| Transactions | 1 | Atomic multi-workshop registration scenario |

---

## Application Features

### Data Display
- View artists with disciplines and artwork counts
- View artworks with color-coded status and average ratings
- View workshops with live participant counts
- View galleries with exhibition counts
- View community members with membership type and booking counts
- View reviews per artwork in a popup dialog

### CRUD Operations
Full Create, Read, Update, and Delete for all entities:
- Artists (with multi-discipline selection)
- Artworks (with artist and status selection)
- Galleries
- Exhibitions (with gallery selection)
- Workshops (with instructor and level selection)
- Community Members (with membership type selection)

---

## How to Run the Application

### Prerequisites
- Java 17 or higher
- Maven 3.9 or higher
- MySQL 8 server running locally

### Step 1 — Database Setup

Open MySQL Workbench and run the SQL scripts
in this exact order:

```sql
-- 1. Create the database and all tables
SOURCE sql/artconnect_schema.sql;

-- 2. Insert sample data
SOURCE sql/artconnect_data.sql;

-- 3. Create views
SOURCE sql/artconnect_views.sql;

-- 4. Create indexes
SOURCE sql/artconnect_indexes.sql;

-- 5. Create triggers
SOURCE sql/artconnect_triggers.sql;

-- 6. Create stored procedures and functions
SOURCE sql/artconnect_procedures.sql;
```

### Step 2 — Configure the Database Connection

Open the following file and set your MySQL password:
src/main/java/com/project/artconnect/config/DatabaseConfig.java

```java
public static final String URL =
    "jdbc:mysql://localhost:3306/artconnect_db" +
    "?useSSL=false&serverTimezone=UTC" +
    "&allowPublicKeyRetrieval=true";
public static final String USER = "root";
public static final String PASSWORD = "your_password_here";
```

### Step 3 — Run the Application

```bash
mvn clean javafx:run
```

### Step 4 — Switch Between Database and InMemory Mode

To run without a database (using fake in-memory data),
open ServiceProvider.java and set:

```java
private static final boolean USE_DATABASE = false;
```

Set it back to `true` to use the real MySQL database.

---

## Architecture

The application follows a four-layer architecture.

---

## Important Note

The password in `DatabaseConfig.java` has been replaced
with a placeholder for security reasons.
Set your own MySQL root password before running the app.