import java.io.*;
import java.net.*;
/**
 * Runs the SAT Prep website using a Java server.
 * Handles browser requests and sends back HTML or CSS pages.
 */
public class SatServer {
    public static void main(String[] args) {
        try {
            ServerSocket server = new ServerSocket(8080);
            IO.println("SAT SERVER OPENED PORT 8080");
            while (true) {
                Socket client = server.accept();
                try (client) {
                    client.setSoTimeout(5000);
                    BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String requestLine = input.readLine();
                    int contentLength = 0;
                    String line = input.readLine();
                    while (line != null && !line.isEmpty()) {
                        if (line.toLowerCase().startsWith("content-length:")) {
                            contentLength = Integer.parseInt(line.substring(15).trim());
                            if (contentLength < 0 || contentLength > 100000) {
                                throw new IOException("Invalid form length.");
                            }
                        }
                        line = input.readLine();
                    }
                    // Reads submitted form data so the correct handler can process it.
                    if (requestLine != null
                            && (requestLine.startsWith("POST /check")
                            || requestLine.startsWith("POST /full-start")
                            || requestLine.startsWith("POST /full-submit"))) {
                        String body = "";
                        for (int i = 0; i < contentLength; i++) {
                            int character = input.read();
                            if (character == -1) {
                                throw new IOException("Incomplete quiz submission.");
                            }
                            body += (char) character;
                        } 
                        String submittedPath = requestLine.split(" ")[1];
                        
                        if (submittedPath.equals("/check")) {
                            requestLine = "GET /check?" + body + " HTTP/1.1";
                        } else {
                            requestLine = "POST " + submittedPath + "?" + body + " HTTP/1.1";
                        }
                    }
                    PrintWriter output = new PrintWriter(client.getOutputStream(), true);
                    if (requestLine != null && requestLine.startsWith("GET /style.css ")) {
                        output.print("HTTP/1.1 200 OK\r\n");
                        output.print("Content-Type: text/css; charset=UTF-8\r\n");
                        output.print("Connection: close\r\n");
                        output.print("\r\n");
                        try {
                            showPage("web/style.css", "", output);
                        } catch (IOException e) {
                            IO.println("Stylesheet error: " + e.getMessage());
                        }
                        output.flush();
                        continue;
                    }
                    output.print("HTTP/1.1 200 OK\r\n");
                    output.print("Content-Type: text/html; charset=UTF-8\r\n");
                    output.print("Connection: close\r\n");
                    output.print("\r\n");

                    if (requestLine != null) {
                        try {
                            showRequestedPage(requestLine, output);
                        } catch (IOException e) {
                            IO.println("Page error: " + e.getMessage());
                            output.println("<p>This page could not be completed because " + "a required file could not be read or written.</p>");
                            output.println("<link rel=\"stylesheet\" href=\"/style.css\">");
                            output.println("<p><a class=\"button\" href=\"/\">Back to home</a></p>");
                        }
                    } 
                    output.flush();
                } catch (Exception e) {
                    IO.println("Request error: " + e.getMessage());
                }
            } 
        } catch (Exception e) {
                IO.println("Server error: " + e.getMessage());
        }
    }
    /**
     * Handles profile selection and sends requests to the correct handler.
     * Clears the current test when a student switches profiles.
     */
    private static void showRequestedPage(String requestLine, PrintWriter output) throws IOException {
        String[] parts = requestLine.split(" ");
        if (parts.length < 2 || (!parts[0].equals("GET") && !parts[0].equals("POST"))) {
            output.println("<p>Invalid request.</p>");
            return;
        }
        String requestPath = parts[1];
        String path = requestPath.split("\\?", 2)[0];
        if (path.equals("/switch-profile")) {
            AdaptiveTestHandler.clear();
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
            AdaptiveTestHandler.clear();
            showPage("web/index.html", "", output);
            return;
        }
        if (!StudentProfile.isSelected()) {
            showPage("web/profile.html", "", output);
            return;
        }
        if (AdaptiveTestHandler.handle(requestLine, output)) {
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
