package ch.bzz.model;

import jakarta.persistence.*;

/**
 * Represents a user in the library system for authentication.
 */
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @Column(name = "password_salt", nullable = false)
    private String passwordSalt;
    
    // Default constructor for JPA
    public User() {}
    
    /**
     * Constructor for creating a User object.
     *
     * @param email        the email address of the user
     * @param passwordHash the hashed password
     * @param passwordSalt the salt used for password hashing
     */
    public User(String email, String passwordHash, String passwordSalt) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
    }
    
    // Getters
    public Integer getId() {
        return id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public String getPasswordSalt() {
        return passwordSalt;
    }
    
    // Setters
    public void setId(Integer id) {
        this.id = id;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    
    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }
    
    @Override
    public String toString() {
        return String.format("User{id=%d, email='%s'}", id, email);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return id != null && id.equals(user.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
