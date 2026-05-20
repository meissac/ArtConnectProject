package com.project.artconnect.util;

import com.project.artconnect.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utility class to manage JDBC connections.
 * Provides a single method to obtain a connection
 * to the ArtConnect MySQL database.
 */
public class ConnectionManager {

    /**
     * Opens and returns a new JDBC connection
     * to the artconnect_db MySQL database.
     *
     * @return a live Connection object
     * @throws SQLException if the connection fails
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                DatabaseConfig.URL,
                DatabaseConfig.USER,
                DatabaseConfig.PASSWORD
        );
    }
}