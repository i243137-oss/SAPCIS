package db;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * // GRASP Pattern: Low Coupling
 * Single connection point for all DAOs across both members.
 * Keeps all database connection secrets in external configuration (db.properties or env vars).
 */
public class DBConnection {

    private static String dbUrl = "jdbc:sqlserver://localhost:1433;databaseName=sapcis;encrypt=true;trustServerCertificate=true;";
    private static String dbUser = "sa";
    private static String dbPassword = "";

    static {
        loadConfiguration();
    }

    private static void loadConfiguration() {
        Properties props = new Properties();
        boolean loaded = false;

        // 1. Try db.properties in current directory
        File propFile = new File("db.properties");
        if (propFile.exists()) {
            try (InputStream in = new FileInputStream(propFile)) {
                props.load(in);
                loaded = true;
            } catch (Exception e) {
                System.err.println("Notice: Could not load db.properties from current directory: " + e.getMessage());
            }
        }

        // 2. Try classpath resource
        if (!loaded) {
            try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (in != null) {
                    props.load(in);
                    loaded = true;
                }
            } catch (Exception e) {
                System.err.println("Notice: Could not load db.properties from classpath: " + e.getMessage());
            }
        }

        // Apply properties if loaded
        if (loaded) {
            if (props.containsKey("db.url")) dbUrl = props.getProperty("db.url");
            if (props.containsKey("db.user")) dbUser = props.getProperty("db.user");
            if (props.containsKey("db.password")) dbPassword = props.getProperty("db.password");
        }

        // 3. Environment variables override properties
        String envUrl = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USER");
        String envPass = System.getenv("DB_PASSWORD");

        if (envUrl != null && !envUrl.trim().isEmpty()) dbUrl = envUrl;
        if (envUser != null && !envUser.trim().isEmpty()) dbUser = envUser;
        if (envPass != null && !envPass.trim().isEmpty()) dbPassword = envPass;
    }

    private DBConnection() {
    }

    /**
     * Returns a fresh Connection each call.
     * Callers are responsible for closing the connection.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    /**
     * Returns a valid Connection. Delegates to getConnection() to ensure
     * callers using try-with-resources do not invalidate shared connections.
     */
    public static Connection getInstance() throws SQLException {
        return getConnection();
    }
}
