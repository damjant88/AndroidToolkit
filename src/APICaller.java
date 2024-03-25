import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

    public class APICaller {

        public static void main(String[] args) {
            // Create an instance of HttpClient
            HttpClient httpClient = HttpClient.newHttpClient();

            // Define the request URL
            String apiUrl = "https://simple-books-api.glitch.me/status";

            // Create an instance of HttpRequest
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .build();

            // Send the request and handle the response asynchronously
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(System.out::println) // Print the response body
                    .join(); // Wait for the response
        }
    }
