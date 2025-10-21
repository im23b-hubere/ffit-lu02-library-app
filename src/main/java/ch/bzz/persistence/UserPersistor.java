package ch.bzz.persistence;

import ch.bzz.model.User;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Data access object for User entity operations.
 */
public class UserPersistor {
    
    private static UserPersistor instance;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    
    private UserPersistor() {
        loadConfig();
    }
    
    /**
     * Gets the singleton instance of UserPersistor.
     *
     * @return UserPersistor instance
     */
    public static UserPersistor getInstance() {
        if (instance == null) {
            instance = new UserPersistor();
        }
        return instance;
    }
    
    /**
     * Loads database configuration from config.properties file.
     */
    private void loadConfig() {
        Properties config = new Properties();
        try (FileInputStream input = new FileInputStream("config.properties")) {
            config.load(input);
            dbUrl = config.getProperty("DB_URL");
            dbUser = config.getProperty("DB_USER");
            dbPassword = config.getProperty("DB_PASSWORD");
            
            // Test PostgreSQL connection
            try (Connection testConnection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                System.out.println("PostgreSQL connection successful");
            } catch (SQLException e) {
                System.err.println("PostgreSQL connection failed, falling back to H2: " + e.getMessage());
                useH2Fallback();
            }
        } catch (IOException e) {
            System.err.println("Error loading config.properties: " + e.getMessage());
            System.err.println("Using H2 in-memory database as fallback");
            useH2Fallback();
        }
    }
    
    /**
     * Configures H2 in-memory database as fallback.
     */
    private void useH2Fallback() {
        dbUrl = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        dbUser = "sa";
        dbPassword = "";
        System.out.println("Using H2 in-memory database");
    }
    
    /**
     * Finds a user by email address.
     *
     * @param email the email address to search for
     * @return User object if found, null otherwise
     * @throws SQLException if database error occurs
     */
    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT id, email, password_hash, password_salt FROM users WHERE email = ?";
        
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, email);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    User user = new User();
                    user.setId(resultSet.getInt("id"));
                    user.setEmail(resultSet.getString("email"));
                    user.setPasswordHash(resultSet.getString("password_hash"));
                    user.setPasswordSalt(resultSet.getString("password_salt"));
                    return user;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Finds a user by ID.
     *
     * @param id the user ID to search for
     * @return User object if found, null otherwise
     * @throws SQLException if database error occurs
     */
    public User findById(Integer id) throws SQLException {
        String sql = "SELECT id, email, password_hash, password_salt FROM users WHERE id = ?";
        
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, id);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    User user = new User();
                    user.setId(resultSet.getInt("id"));
                    user.setEmail(resultSet.getString("email"));
                    user.setPasswordHash(resultSet.getString("password_hash"));
                    user.setPasswordSalt(resultSet.getString("password_salt"));
                    return user;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Saves or updates a user in the database.
     *
     * @param user the user to save
     * @throws SQLException if database error occurs
     */
    public void save(User user) throws SQLException {
        if (user.getId() == null) {
            // Insert new user
            String sql = "INSERT INTO users (email, password_hash, password_salt) VALUES (?, ?, ?) RETURNING id";
            
            try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, user.getEmail());
                statement.setString(2, user.getPasswordHash());
                statement.setString(3, user.getPasswordSalt());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        user.setId(resultSet.getInt("id"));
                    }
                }
            }
        } else {
            // Update existing user
            String sql = "UPDATE users SET email = ?, password_hash = ?, password_salt = ? WHERE id = ?";
            
            try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, user.getEmail());
                statement.setString(2, user.getPasswordHash());
                statement.setString(3, user.getPasswordSalt());
                statement.setInt(4, user.getId());
                
                statement.executeUpdate();
            }
        }
    }
    
    /**
     * Creates the users table if it doesn't exist.
     * This is a utility method for development/testing.
     */
    public void createTableIfNotExists() {
        String sql;
        
        // Different SQL for different databases
        if (dbUrl.contains("h2")) {
            sql = """
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    email VARCHAR(255) UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    password_salt TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
        } else {
            // PostgreSQL
            sql = """
                CREATE TABLE IF NOT EXISTS users (
                    id SERIAL PRIMARY KEY,
                    email VARCHAR(255) UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    password_salt TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
        }
        
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.executeUpdate();
            System.out.println("Users table created or already exists.");
            
        } catch (SQLException e) {
            System.err.println("Error creating users table: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
