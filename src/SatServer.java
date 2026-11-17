import java.io.*;
import java.net.*;
public class SatServer {
    public static void main(String[] args) {
        try {
            ServerSocket server = new ServerSocket(8080);
            IO.println("SAT SERVER OPENED PORT 8080");
            while (true) {
                Socket client = server.accept();
                IO.println("Browser connected");
                BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String line = input.readLine();
                while (line != null && !line.isEmpty()) {
                    line = input.readLine();
                }
                PrintWriter output = new PrintWriter(client.getOutputStream(), true);
                output.print("HTTP/1.1 200 OK\r\n");
                output.print("Content-Type: text/html; charset=UTF-8\r\n");
                output.print("Connection: close\r\n");
                output.print("\r\n");
                BufferedReader page = new BufferedReader(new FileReader("web/index.html"));
                String pageLine = page.readLine();
                while (pageLine != null) {
                    output.println(pageLine);
                    pageLine = page.readLine();
                }
                page.close();
                client.close();
            }
        } catch (Exception e) {
            IO.println("Server error: " + e.getMessage());
        }
    }
}
