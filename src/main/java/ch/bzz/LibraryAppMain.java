package ch.bzz;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class LibraryAppMain {
    
    // Book constants as specified in the requirements
    private static final Book BOOK_1 = new Book(1, "978-3-8362-9544-4", "Java ist auch eine Insel", "Christian Ullenboom", 2023);
    private static final Book BOOK_2 = new Book(2, "978-3-658-43573-8", "Grundkurs Java", "Dietmar Abts", 2024);
    
    // Database configuration
    private Properties config;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;

    public static void main(String[] args) {
        LibraryAppMain app = new LibraryAppMain();
        app.run();
    }

    public void run() {
        // Load configuration
        loadConfig();
        
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the Library App!");
        System.out.println("Type 'help' for available commands or 'quit' to exit.");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                continue;
            }

            String[] parts = input.split("\\s+");
            String command = parts[0].toLowerCase();

            switch (command) {
                case "quit":
                    System.out.println("Goodbye!");
                    scanner.close();
                    return;
                case "help":
                    showHelp();
                    break;
                case "listbooks":
                    if (parts.length >= 2) {
                        try {
                            int limit = Integer.parseInt(parts[1]);
                            listBooks(limit);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid limit number: " + parts[1]);
                            System.out.println("Usage: listBooks [limit]");
                        }
                    } else {
                        listBooks();
                    }
                    break;
                case "importbooks":
                    if (parts.length < 2) {
                        System.out.println("Usage: importBooks <FILE_PATH>");
                    } else {
                        importBooks(parts[1]);
                    }
                    break;
                default:
                    System.out.println("Unknown command: " + command);
                    System.out.println("Type 'help' for available commands.");
                    break;
            }
        }
    }

    private void showHelp() {
        System.out.println("Available commands:");
        System.out.println("  help                    - Show this help message");
        System.out.println("  listBooks [limit]       - List all available books (optionally limit results)");
        System.out.println("  importBooks <FILE_PATH> - Import books from TSV file");
        System.out.println("  quit                    - Exit the application");
    }

    /**
     * Lists all books in the library.
     */
    private void listBooks() {
        listBooks(-1); // -1 means no limit
    }

    /**
     * Lists books in the library with an optional limit.
     *
     * @param limit maximum number of books to display (-1 for no limit)
     */
    private void listBooks(int limit) {
        try {
            List<Book> books = loadBooksFromDatabase();
            if (books.isEmpty()) {
                System.out.println("No books found in the database. Showing hardcoded books:");
                List<Book> hardcodedBooks = List.of(BOOK_1, BOOK_2);
                displayBooks(hardcodedBooks, limit);
            } else {
                System.out.println("Available books:");
                displayBooks(books, limit);
            }
        } catch (Exception e) {
            System.out.println("Error accessing database. Showing hardcoded books:");
            List<Book> hardcodedBooks = List.of(BOOK_1, BOOK_2);
            displayBooks(hardcodedBooks, limit);
            System.err.println("Database error: " + e.getMessage());
        }
    }

    /**
     * Displays a list of books with an optional limit.
     *
     * @param books the list of books to display
     * @param limit maximum number of books to display (-1 for no limit)
     */
    private void displayBooks(List<Book> books, int limit) {
        int count = 0;
        for (Book book : books) {
            if (limit > 0 && count >= limit) {
                break;
            }
            System.out.println(book);
            count++;
        }
        
        if (limit > 0 && books.size() > limit) {
            System.out.println("... and " + (books.size() - limit) + " more books.");
        }
    }

    /**
     * Loads configuration from config.properties file.
     */
    private void loadConfig() {
        config = new Properties();
        try (FileInputStream input = new FileInputStream("config.properties")) {
            config.load(input);
            dbUrl = config.getProperty("DB_URL");
            dbUser = config.getProperty("DB_USER");
            dbPassword = config.getProperty("DB_PASSWORD");
        } catch (IOException e) {
            System.err.println("Error loading config.properties: " + e.getMessage());
            e.printStackTrace();
            System.err.println("Using default database configuration.");
            // Fallback to default values
            dbUrl = "jdbc:postgresql://localhost:5432/localdb";
            dbUser = "localuser";
            dbPassword = "localpassword";
        }
    }

    /**
     * Loads books from the database.
     *
     * @return List of books from the database
     * @throws SQLException if database access fails
     */
    private List<Book> loadBooksFromDatabase() throws SQLException {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT id, isbn, title, author, publication_year FROM books";

        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String isbn = resultSet.getString("isbn");
                String title = resultSet.getString("title");
                String author = resultSet.getString("author");
                int year = resultSet.getInt("publication_year");

                books.add(new Book(id, isbn, title, author, year));
            }
        }

        return books;
    }

    /**
     * Imports books from a TSV file into the database.
     *
     * @param filePath path to the TSV file
     */
    private void importBooks(String filePath) {
        try {
            List<Book> books = readBooksFromTSV(filePath);
            saveBooksToDatabase(books);
            System.out.println("Successfully imported " + books.size() + " books from " + filePath);
        } catch (IOException e) {
            System.err.println("Error reading file " + filePath + ": " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Error saving books to database: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error during import: " + e.getMessage());
        }
    }

    /**
     * Reads books from a TSV file.
     *
     * @param filePath path to the TSV file
     * @return List of books read from the file
     * @throws IOException if file cannot be read
     */
    private List<Book> readBooksFromTSV(String filePath) throws IOException {
        List<Book> books = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = reader.readLine()) != null) {
                // Skip header line
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // Split by tab character
                String[] parts = line.split("\t");
                
                if (parts.length >= 5) {
                    try {
                        int id = Integer.parseInt(parts[0].trim());
                        String isbn = parts[1].trim();
                        String title = parts[2].trim();
                        String author = parts[3].trim();
                        int year = Integer.parseInt(parts[4].trim());
                        
                        books.add(new Book(id, isbn, title, author, year));
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid number format in line: " + line);
                    }
                } else {
                    System.err.println("Invalid line format (expected 5 columns): " + line);
                }
            }
        }
        
        return books;
    }

    /**
     * Saves a list of books to the database.
     * Books with the same ID will be updated (upsert operation).
     *
     * @param books List of books to save
     * @throws SQLException if database operation fails
     */
    private void saveBooksToDatabase(List<Book> books) throws SQLException {
        String sql = "INSERT INTO books (id, isbn, title, author, publication_year) VALUES (?, ?, ?, ?, ?) " +
                     "ON CONFLICT (id) DO UPDATE SET " +
                     "isbn = EXCLUDED.isbn, " +
                     "title = EXCLUDED.title, " +
                     "author = EXCLUDED.author, " +
                     "publication_year = EXCLUDED.publication_year";

        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            for (Book book : books) {
                statement.setInt(1, book.getId());
                statement.setString(2, book.getIsbn());
                statement.setString(3, book.getTitle());
                statement.setString(4, book.getAuthor());
                statement.setInt(5, book.getYear());
                statement.addBatch();
            }

            statement.executeBatch();
        }
    }
}
