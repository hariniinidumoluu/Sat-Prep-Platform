import java.io.*;
import java.net.*;
/**
 * Runs the SAT Prep server and displays the website.
 * Uses the SubjectHandler to handle the subject pages and quizzes.
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
                if (requestLine != null && requestLine.startsWith("GET /style.css ")) {
                    output.print("HTTP/1.1 200 OK\r\n");
                    output.print("Content-Type: text/css; charset=UTF-8\r\n");
                    output.print("Connection: close\r\n");
                    output.print("\r\n");
                    showPage("web/style.css", "", output);
                    output.flush();
                    client.close();
                    continue;
                }
                output.print("HTTP/1.1 200 OK\r\n");
                output.print("Content-Type: text/html; charset=UTF-8\r\n");
                output.print("Connection: close\r\n");
                output.print("\r\n");

                if (requestLine != null) {
                    showRequestedPage(requestLine, output);
                }
                output.flush();
                client.close();
            }
        } catch (Exception e) {
            IO.println("Server error: " + e.getMessage());
        }
    }
    /**
     * Handles profile selection before opening the usual website pages.
     */
    private static void showRequestedPage(String requestLine, PrintWriter output) throws IOException {
        String[] parts = requestLine.split(" ");
        if (parts.length < 2 || !parts[0].equals("GET")) {
            output.println("<p>Invalid request.</p>");
            return;
        }
        String requestPath = parts[1];
        String path = requestPath.split("\\?", 2)[0];
        if (path.equals("/switch-profile")) {
            StudentProfile.clear();
            showPage("web/profile.html", "", output);
            return;
        }
        if (path.equals("/profile")) {
            String username = SubjectHandler.getParameter(requestPath, "username");
            if (!StudentProfile.select(username)) {
                showPage("web/profile.html", "Use 3-20 letters, numbers, or underscores.", output);
                return;
            }
            showPage("web/index.html", "", output);
            return;
        }
        if (!StudentProfile.isSelected()) {
            showPage("web/profile.html", "", output);
            return;
        }
        if (SubjectHandler.handle(requestLine, output)) {
            return;
        }
        showPage("web/index.html", "", output);
    }
    /**
     * Reads a page and fills in the profile name or message.
     */
    private static void showPage(String filePath, String message, PrintWriter output) throws IOException {
        try (BufferedReader page = new BufferedReader(new FileReader(filePath))) {
            String line = page.readLine();
            while (line != null) {
                line = line.replace("{{MESSAGE}}", SubjectHandler.escape(message));
                line = line.replace("{{USERNAME}}", SubjectHandler.escape(StudentProfile.getUsername()));
                output.println(line);
                line = page.readLine();
            }
        }
    }
}
