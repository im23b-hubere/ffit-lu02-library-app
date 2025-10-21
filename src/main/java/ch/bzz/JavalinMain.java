package ch.bzz;

import ch.bzz.model.User;
import ch.bzz.persistence.UserPersistor;
import ch.bzz.util.JwtHandler;
import ch.bzz.util.PasswordHandler;
import ch.bzz.util.TestDataUtil;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.jsonwebtoken.Claims;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Main class for the Javalin REST API server.
 * Provides REST endpoints for the Library App.
 */
public class JavalinMain {
    
    // Book constants as fallback
    private static final Book BOOK_1 = new Book(1, "978-3-8362-9544-4", "Java ist auch eine Insel", "Christian Ullenboom", 2023);
    private static final Book BOOK_2 = new Book(2, "978-3-658-43573-8", "Grundkurs Java", "Dietmar Abts", 2024);
    
    // Database configuration
    private static Properties config;
    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    public static void main(String[] args) {
        // Load configuration
        loadConfig();
        
        // Create Javalin app
        Javalin app = Javalin.create().start(7070);

        // Initialize database tables
        UserPersistor.getInstance().createTableIfNotExists();
        
        // Create test users for development
        TestDataUtil.createTestUsers();
        
        // Define routes
        app.get("/books", JavalinMain::getBooksHandler);
        app.post("/auth/login", JavalinMain::loginHandler);
        app.put("/auth/change-password", JavalinMain::changePasswordHandler);
        app.get("/debug/users", JavalinMain::debugUsersHandler);
        
        System.out.println("Javalin server started on http://localhost:7070");
        System.out.println("Try: http://localhost:7070/books?limit=10");
        System.out.println("Authentication endpoints:");
        System.out.println("  POST /auth/login");
        System.out.println("  PUT /auth/change-password");
    }

    /**
     * Handler for GET /books endpoint.
     * Supports optional 'limit' query parameter.
     *
     * @param ctx Javalin context
     */
    private static void getBooksHandler(Context ctx) {
        try {
            // Get limit parameter (optional)
            String limitParam = ctx.queryParam("limit");
            int limit = -1; // -1 means no limit
            
            if (limitParam != null && !limitParam.isEmpty()) {
                try {
                    limit = Integer.parseInt(limitParam);
                    if (limit <= 0) {
                        ctx.status(400).json(new ErrorResponse("Limit must be a positive number"));
                        return;
                    }
                } catch (NumberFormatException e) {
                    ctx.status(400).json(new ErrorResponse("Invalid limit parameter: " + limitParam));
                    return;
                }
            }

            // Load books from database
            List<Book> books = loadBooksFromDatabase(limit);
            
            // If no books in database, use hardcoded books
            if (books.isEmpty()) {
                List<Book> hardcodedBooks = List.of(BOOK_1, BOOK_2);
                books = applyLimit(hardcodedBooks, limit);
            }
            
            // Return books as JSON
            ctx.json(new BooksResponse(books, books.size()));
            
        } catch (Exception e) {
            System.err.println("Error in getBooksHandler: " + e.getMessage());
            e.printStackTrace();
            ctx.status(500).json(new ErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Loads configuration from config.properties file.
     */
    private static void loadConfig() {
        config = new Properties();
        try (FileInputStream input = new FileInputStream("config.properties")) {
            config.load(input);
            dbUrl = config.getProperty("DB_URL");
            dbUser = config.getProperty("DB_USER");
            dbPassword = config.getProperty("DB_PASSWORD");
        } catch (IOException e) {
            System.err.println("Error loading config.properties: " + e.getMessage());
            System.err.println("Using default database configuration.");
            // Fallback to default values
            dbUrl = "jdbc:postgresql://localhost:5432/localdb";
            dbUser = "localuser";
            dbPassword = "localpassword";
        }
    }

    /**
     * Loads books from the database with optional limit.
     *
     * @param limit maximum number of books to load (-1 for no limit)
     * @return List of books from the database
     */
    private static List<Book> loadBooksFromDatabase(int limit) {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT id, isbn, title, author, publication_year FROM books";
        
        if (limit > 0) {
            sql += " LIMIT ?";
        }

        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (limit > 0) {
                statement.setInt(1, limit);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String isbn = resultSet.getString("isbn");
                    String title = resultSet.getString("title");
                    String author = resultSet.getString("author");
                    int year = resultSet.getInt("publication_year");

                    books.add(new Book(id, isbn, title, author, year));
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            // Return empty list, fallback will be used
        }

        return books;
    }

    /**
     * Applies limit to a list of books.
     *
     * @param books the original list of books
     * @param limit maximum number of books (-1 for no limit)
     * @return limited list of books
     */
    private static List<Book> applyLimit(List<Book> books, int limit) {
        if (limit <= 0 || limit >= books.size()) {
            return books;
        }
        return books.subList(0, limit);
    }

    /**
     * Response class for books endpoint.
     */
    public static class BooksResponse {
        private List<Book> books;
        private int count;

        public BooksResponse(List<Book> books, int count) {
            this.books = books;
            this.count = count;
        }

        public List<Book> getBooks() {
            return books;
        }

        public int getCount() {
            return count;
        }
    }

    /**
     * Response class for error messages.
     */
    public static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }
    }
    
    /**
     * Handler for POST /auth/login endpoint.
     * Authenticates user with email and password, returns JWT token.
     *
     * @param ctx Javalin context
     */
    private static void loginHandler(Context ctx) {
        try {
            // Validate request body
            var json = ctx.bodyValidator(Map.class)
                    .check(m -> m.containsKey("email"), "email is required")
                    .check(m -> m.containsKey("password"), "password is required")
                    .get();
            
            String inputEmail = (String) json.get("email");
            String inputPassword = (String) json.get("password");
            
            // Hardcoded test user for demonstration (development only)
            if (inputEmail.equals("test@example.com") && inputPassword.equals("password123")) {
                String jwt = JwtHandler.createJwt(inputEmail, 1);
                ctx.json(Map.of("token", jwt));
                System.out.println("Login successful for test user: " + inputEmail);
                return;
            }
            
            // Try database lookup for other users
            try {
                UserPersistor userPersistor = UserPersistor.getInstance();
                User user = userPersistor.findByEmail(inputEmail);
                
                if (user != null) {
                    // Verify password
                    byte[] storedSalt = Base64.getDecoder().decode(user.getPasswordSalt());
                    byte[] storedHash = Base64.getDecoder().decode(user.getPasswordHash());
                    
                    if (PasswordHandler.verifyPassword(inputPassword, storedHash, storedSalt)) {
                        String jwt = JwtHandler.createJwt(inputEmail, user.getId());
                        ctx.json(Map.of("token", jwt));
                        System.out.println("Login successful for database user: " + inputEmail);
                        return;
                    }
                }
            } catch (SQLException e) {
                System.err.println("Database error (using fallback): " + e.getMessage());
            }
            
            // Same error message for security
            ctx.status(401).json(Map.of("error", "Invalid email or password"));
            
        } catch (Exception e) {
            System.err.println("Error in loginHandler: " + e.getMessage());
            e.printStackTrace();
            ctx.status(400).json(Map.of("error", "Invalid request: " + e.getMessage()));
        }
    }
    
    /**
     * Handler for PUT /auth/change-password endpoint.
     * Changes user password after JWT authentication.
     *
     * @param ctx Javalin context
     */
    private static void changePasswordHandler(Context ctx) {
        try {
            // Validate Authorization header
            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.status(401).json(Map.of("error", "Authorization header missing or invalid"));
                return;
            }
            
            String token = authHeader.substring("Bearer ".length());
            
            // Validate and parse JWT
            Claims claims;
            try {
                claims = JwtHandler.validateAndParseJwt(token);
            } catch (Exception e) {
                ctx.status(401).json(Map.of("error", "Invalid or expired token"));
                return;
            }
            
            Integer userId = claims.get("userId", Integer.class);
            
            // Validate request body
            var json = ctx.bodyValidator(Map.class)
                    .check(m -> m.containsKey("oldPassword"), "oldPassword is required")
                    .check(m -> m.containsKey("newPassword"), "newPassword is required")
                    .get();
            
            String oldPassword = (String) json.get("oldPassword");
            String newPassword = (String) json.get("newPassword");
            
            // Find user by ID
            UserPersistor userPersistor = UserPersistor.getInstance();
            User user = userPersistor.findById(userId);
            
            if (user == null) {
                ctx.status(404).json(Map.of("error", "User not found"));
                return;
            }
            
            // Verify old password
            byte[] storedSalt = Base64.getDecoder().decode(user.getPasswordSalt());
            byte[] storedHash = Base64.getDecoder().decode(user.getPasswordHash());
            
            if (PasswordHandler.verifyPassword(oldPassword, storedHash, storedSalt)) {
                // Hash new password with existing salt
                byte[] newHash = PasswordHandler.hashPassword(newPassword, storedSalt);
                user.setPasswordHash(Base64.getEncoder().encodeToString(newHash));
                
                userPersistor.save(user);
                ctx.json(Map.of("message", "Password changed successfully"));
            } else {
                ctx.status(401).json(Map.of("error", "Invalid old password"));
            }
            
        } catch (SQLException e) {
            System.err.println("Database error in changePasswordHandler: " + e.getMessage());
            ctx.status(500).json(Map.of("error", "Internal server error"));
        } catch (Exception e) {
            System.err.println("Error in changePasswordHandler: " + e.getMessage());
            ctx.status(400).json(Map.of("error", "Invalid request: " + e.getMessage()));
        }
    }
    
    /**
     * Debug handler to check if users exist in database.
     *
     * @param ctx Javalin context
     */
    private static void debugUsersHandler(Context ctx) {
        try {
            UserPersistor userPersistor = UserPersistor.getInstance();
            User testUser = userPersistor.findByEmail("test@example.com");
            
            if (testUser != null) {
                ctx.json(Map.of(
                    "message", "Test user found",
                    "email", testUser.getEmail(),
                    "id", testUser.getId()
                ));
            } else {
                ctx.json(Map.of("message", "Test user NOT found - creating now..."));
                
                // Try to create test user again
                TestDataUtil.createTestUser();
                
                // Check again
                testUser = userPersistor.findByEmail("test@example.com");
                if (testUser != null) {
                    ctx.json(Map.of(
                        "message", "Test user created successfully",
                        "email", testUser.getEmail(),
                        "id", testUser.getId()
                    ));
                } else {
                    ctx.json(Map.of("message", "Failed to create test user"));
                }
            }
        } catch (Exception e) {
            System.err.println("Error in debugUsersHandler: " + e.getMessage());
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "Debug error: " + e.getMessage()));
        }
    }
}
