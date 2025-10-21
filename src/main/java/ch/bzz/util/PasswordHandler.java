package ch.bzz.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

/**
 * Utility class for password hashing and verification using PBKDF2.
 */
public class PasswordHandler {
    
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 32;
    
    /**
     * Generates a random salt for password hashing.
     *
     * @return byte array containing the salt
     */
    public static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }
    
    /**
     * Hashes a password using PBKDF2 with the provided salt.
     *
     * @param password the plain text password
     * @param salt     the salt to use for hashing
     * @return byte array containing the hashed password
     * @throws RuntimeException if hashing fails
     */
    public static byte[] hashPassword(String password, byte[] salt) {
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
    
    /**
     * Verifies a password against a stored hash and salt.
     *
     * @param password   the plain text password to verify
     * @param storedHash the stored password hash
     * @param storedSalt the stored salt
     * @return true if the password is correct, false otherwise
     */
    public static boolean verifyPassword(String password, byte[] storedHash, byte[] storedSalt) {
        try {
            byte[] computedHash = hashPassword(password, storedSalt);
            return Arrays.equals(computedHash, storedHash);
        } catch (Exception e) {
            // Log error and return false for security
            System.err.println("Error verifying password: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Convenience method to hash a password with a new random salt.
     *
     * @param password the plain text password
     * @return PasswordResult containing the hash and salt
     */
    public static PasswordResult hashPasswordWithSalt(String password) {
        byte[] salt = generateSalt();
        byte[] hash = hashPassword(password, salt);
        return new PasswordResult(hash, salt);
    }
    
    /**
     * Result class containing password hash and salt.
     */
    public static class PasswordResult {
        private final byte[] hash;
        private final byte[] salt;
        
        public PasswordResult(byte[] hash, byte[] salt) {
            this.hash = hash;
            this.salt = salt;
        }
        
        public byte[] getHash() {
            return hash;
        }
        
        public byte[] getSalt() {
            return salt;
        }
    }
}
