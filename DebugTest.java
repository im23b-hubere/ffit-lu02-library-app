import ch.bzz.persistence.UserPersistor;
import ch.bzz.util.TestDataUtil;

/**
 * Simple debug test to check database connectivity and user creation.
 */
public class DebugTest {
    public static void main(String[] args) {
        System.out.println("=== Debug Test ===");
        
        try {
            // Test 1: Create table
            System.out.println("1. Creating users table...");
            UserPersistor.getInstance().createTableIfNotExists();
            System.out.println("   ✓ Table creation successful");
            
            // Test 2: Create test users
            System.out.println("2. Creating test users...");
            TestDataUtil.createTestUsers();
            System.out.println("   ✓ Test users creation successful");
            
            // Test 3: Find test user
            System.out.println("3. Finding test user...");
            var user = UserPersistor.getInstance().findByEmail("test@example.com");
            if (user != null) {
                System.out.println("   ✓ Found user: " + user.getEmail());
            } else {
                System.out.println("   ✗ User not found");
            }
            
        } catch (Exception e) {
            System.err.println("Error during debug test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
