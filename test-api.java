import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Simple test to verify the Javalin API is working.
 * Run this after starting the Javalin server.
 */
public class TestApi {
    public static void main(String[] args) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            
            // Test 1: Get all books
            HttpRequest request1 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:7070/books"))
                .GET()
                .build();
                
            HttpResponse<String> response1 = client.send(request1, HttpResponse.BodyHandlers.ofString());
            System.out.println("GET /books:");
            System.out.println("Status: " + response1.statusCode());
            System.out.println("Body: " + response1.body());
            System.out.println();
            
            // Test 2: Get books with limit
            HttpRequest request2 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:7070/books?limit=1"))
                .GET()
                .build();
                
            HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());
            System.out.println("GET /books?limit=1:");
            System.out.println("Status: " + response2.statusCode());
            System.out.println("Body: " + response2.body());
            
        } catch (Exception e) {
            System.err.println("Error testing API: " + e.getMessage());
            System.err.println("Make sure the Javalin server is running on port 7070");
        }
    }
}

