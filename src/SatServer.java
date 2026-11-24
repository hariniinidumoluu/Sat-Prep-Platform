import java.io.*;
import java.net.*;
/**
 * Runs the SAT Prep server and displays the website.
 * Uses the shared SubjectHandler to handle Math and 
 * Reading & Writing pages, quizzes, and results.
 */
public class SatServer {
    public static void main(String[] args) {
        try {
            ServerSocket server = new ServerSocket(8080);
            IO.println("SAT SERVER OPENED PORT 8080");
            while (true) {
                Socket client = server.accept();
                IO.println("Browser connected");
                BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String requestLine = input.readLine();
                String line = requestLine;
                while (line != null && !line.isEmpty()) {
                    line = input.readLine();
                }
                PrintWriter output = new PrintWriter(client.getOutputStream(), true);
                output.print("HTTP/1.1 200 OK\r\n");
                output.print("Content-Type: text/html; charset=UTF-8\r\n");
                output.print("Connection: close\r\n");
                output.print("\r\n");

                if (SubjectHandler.handle(requestLine, output)) {
                    client.close();
                    continue;
                }

                try (BufferedReader page = new BufferedReader(new FileReader("web/index.html"))) {
                    String pageLine = page.readLine();
                    while (pageLine != null) {
                        output.println(pageLine);
                        pageLine = page.readLine();
                    }
                }
                client.close();
            }
        } catch (Exception e) {
            IO.println("Server error: " + e.getMessage());
        }
    }
}
