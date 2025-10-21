package ch.bzz.util;

import ch.bzz.model.User;
import ch.bzz.persistence.UserPersistor;
import java.sql.SQLException;
import java.util.Base64;

/**
 * Utility class for creating test data.
 */
public class TestDataUtil {
    
    /**
     * Creates a test user in the database if it doesn't exist.
     * Email: test@example.com
     * Password: password123
     */
    public static void createTestUser() {
        try {
            UserPersistor userPersistor = UserPersistor.getInstance();
            
            // Check if test user already exists
            User existingUser = userPersistor.findByEmail("test@example.com");
            if (existingUser != null) {
                System.out.println("Test user already exists: test@example.com");
                return;
            }
            
            // Create test user
            PasswordHandler.PasswordResult passwordResult = PasswordHandler.hashPasswordWithSalt("password123");
            
            User testUser = new User(
                "test@example.com",
                Base64.getEncoder().encodeToString(passwordResult.getHash()),
                Base64.getEncoder().encodeToString(passwordResult.getSalt())
            );
            
            userPersistor.save(testUser);
            System.out.println("Test user created successfully:");
            System.out.println("  Email: test@example.com");
            System.out.println("  Password: password123");
            
        } catch (SQLException e) {
            System.err.println("Error creating test user: " + e.getMessage());
        }
    }
    
    /**
     * Creates multiple test users for testing purposes.
     */
    public static void createTestUsers() {
        createTestUser();
        
        // Create additional test users
        String[][] testUsers = {
            {"admin@library.com", "admin123"},
            {"librarian@library.com", "librarian123"},
            {"user@library.com", "user123"}
        };
        
        UserPersistor userPersistor = UserPersistor.getInstance();
        
        for (String[] userData : testUsers) {
            try {
                String email = userData[0];
                String password = userData[1];
                
                // Check if user already exists
                User existingUser = userPersistor.findByEmail(email);
                if (existingUser != null) {
                    System.out.println("Test user already exists: " + email);
                    continue;
                }
                
                // Create user
                PasswordHandler.PasswordResult passwordResult = PasswordHandler.hashPasswordWithSalt(password);
                
                User user = new User(
                    email,
                    Base64.getEncoder().encodeToString(passwordResult.getHash()),
                    Base64.getEncoder().encodeToString(passwordResult.getSalt())
                );
                
                userPersistor.save(user);
                System.out.println("Test user created: " + email + " (password: " + password + ")");
                
            } catch (SQLException e) {
                System.err.println("Error creating test user " + userData[0] + ": " + e.getMessage());
            }
        }
    }
}
