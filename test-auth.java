import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Simple test class to demonstrate JWT authentication API usage.
 * Run this after starting the Javalin server.
 */
public class TestAuth {
    
    private static final String BASE_URL = "http://localhost:7070";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    public static void main(String[] args) {
        System.out.println("Testing JWT Authentication API...");
        System.out.println("Make sure the Javalin server is running on " + BASE_URL);
        System.out.println();
        
        try {
            // Test 1: Login
            String token = testLogin();
            if (token != null) {
                // Test 2: Change Password
                testChangePassword(token);
            }
            
            // Test 3: Books endpoint (should still work)
            testBooksEndpoint();
            
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String testLogin() throws IOException, InterruptedException {
        System.out.println("=== Testing Login ===");
        
        String loginJson = """
            {
                "email": "test@example.com",
                "password": "password123"
            }
            """;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(loginJson))
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("Status Code: " + response.statusCode());
        System.out.println("Response Body: " + response.body());
        
        if (response.statusCode() == 200) {
            // Extract token from response (simple parsing)
            String responseBody = response.body();
            if (responseBody.contains("\"token\":")) {
                int start = responseBody.indexOf("\"token\":\"") + 9;
                int end = responseBody.indexOf("\"", start);
                String token = responseBody.substring(start, end);
                System.out.println("Login successful! Token extracted.");
                System.out.println();
                return token;
            }
        }
        
        System.out.println("Login failed!");
        System.out.println();
        return null;
    }
    
    private static void testChangePassword(String token) throws IOException, InterruptedException {
        System.out.println("=== Testing Change Password ===");
        
        String changePasswordJson = """
            {
                "oldPassword": "password123",
                "newPassword": "newPassword456"
            }
            """;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/change-password"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .PUT(HttpRequest.BodyPublishers.ofString(changePasswordJson))
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("Status Code: " + response.statusCode());
        System.out.println("Response Body: " + response.body());
        System.out.println();
    }
    
    private static void testBooksEndpoint() throws IOException, InterruptedException {
        System.out.println("=== Testing Books Endpoint ===");
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/books?limit=3"))
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("Status Code: " + response.statusCode());
        System.out.println("Response Body: " + response.body());
        System.out.println();
    }
}
