package ch.bzz.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Utility class for JWT token creation and validation.
 */
public class JwtHandler {
    
    // Secret key for JWT signing (in production, this should be loaded from environment variables)
    private static final String SECRET_KEY = "MySecretKeyForJWTTokenSigningThatIsAtLeast32BytesLong123456789";
    private static final SecretKey JWT_KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    
    // Token expiration time (24 hours in milliseconds)
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000; // 24 hours
    
    /**
     * Creates a JWT token for the given user.
     *
     * @param email  the user's email
     * @param userId the user's ID
     * @return JWT token string
     */
    public static String createJwt(String email, Integer userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_TIME);
        
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(JWT_KEY)
                .compact();
    }
    
    /**
     * Validates and parses a JWT token.
     *
     * @param token the JWT token string
     * @return Claims object containing the token data
     * @throws RuntimeException if token is invalid or expired
     */
    public static Claims validateAndParseJwt(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(JWT_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new RuntimeException("Invalid or expired JWT token", e);
        }
    }
    
    /**
     * Extracts the user ID from a JWT token.
     *
     * @param token the JWT token string
     * @return user ID
     * @throws RuntimeException if token is invalid
     */
    public static Integer getUserIdFromToken(String token) {
        Claims claims = validateAndParseJwt(token);
        return claims.get("userId", Integer.class);
    }
    
    /**
     * Extracts the email from a JWT token.
     *
     * @param token the JWT token string
     * @return email address
     * @throws RuntimeException if token is invalid
     */
    public static String getEmailFromToken(String token) {
        Claims claims = validateAndParseJwt(token);
        return claims.getSubject();
    }
    
    /**
     * Checks if a JWT token is expired.
     *
     * @param token the JWT token string
     * @return true if token is expired, false otherwise
     */
    public static boolean isTokenExpired(String token) {
        try {
            Claims claims = validateAndParseJwt(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true; // Consider invalid tokens as expired
        }
    }
}
