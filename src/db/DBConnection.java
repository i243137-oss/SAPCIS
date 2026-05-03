package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * // GRASP Pattern: Low Coupling
 * Single connection point for all DAOs across both members.
 * BUG FIX: Singleton was returning the SAME connection which gets closed by
 * try-with-resources.
 * Now returns a new connection each time to avoid "connection is closed"
 * errors.
 */
public class DBConnection {

    // UPDATE THIS: Use your actual SQL Server 'sa' password
    private static final String URL = "jdbc:sqlserver://localhost:1433;databaseName=sapcis;encrypt=true;trustServerCertificate=true;";
    private static final String USER = "sa";
    private static final String PASS = "*2404233";

    private DBConnection() {
    }

    /**
     * Returns a fresh Connection each call.
     * Callers are responsible for closing the connection.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    /**
     * @deprecated Use getConnection() instead. Kept for backward compatibility.
     *             Returns a shared singleton connection (only for use in
     *             transactional controllers
     *             that manage their own lifecycle).
     */
    private static Connection sharedInstance = null;

    public static Connection getInstance() throws SQLException {
        if (sharedInstance == null || sharedInstance.isClosed()) {
            sharedInstance = DriverManager.getConnection(URL, USER, PASS);
        }
        return sharedInstance;
    }
}
