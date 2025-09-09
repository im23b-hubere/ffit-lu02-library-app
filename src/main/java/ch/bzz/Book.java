package ch.bzz;

/**
 * Represents a book in the library system.
 */
public class Book {
    private int id;
    private String isbn;
    private String title;
    private String author;
    private int year;

    /**
     * Constructor for creating a Book object.
     *
     * @param id     the unique identifier of the book
     * @param isbn   the ISBN of the book
     * @param title  the title of the book
     * @param author the author of the book
     * @param year   the publication year of the book
     */
    public Book(int id, String isbn, String title, String author, int year) {
        this.id = id;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.year = year;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public int getYear() {
        return year;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setYear(int year) {
        this.year = year;
    }

    @Override
    public String toString() {
        return String.format("ID: %d | %s | %s by %s (%d)", id, isbn, title, author, year);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Book book = (Book) obj;
        return id == book.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}

