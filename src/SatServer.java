import java.io.*;
import java.net.*;
/**
 * Serves the SAT Prep webpages and loads Math questions from a text file.
 * Checks submitted answers using question IDs and displays feedback with 
 * explanations.
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
                /*
                 * Gets the submitted answers against the question file and
                 * displays feedback, explanations, and the total score.
                 */
                if (requestLine != null && requestLine.startsWith("GET /check-math?")) {
                    String requestPath = requestLine.split(" ")[1];
                    String query = requestPath.substring(requestPath.indexOf("?") + 1);
                    String[] parameters = query.split("&");
                    int score = 0;
                    int total = 0;

                    BufferedReader questions = new BufferedReader(new FileReader("data/math-questions.txt"));
                    String id = questions.readLine();
                    while (id != null) {
                        String module = questions.readLine();
                        String topic = questions.readLine();
                        String questionText = questions.readLine();
                        String a = questions.readLine();
                        String b = questions.readLine();
                        String c = questions.readLine();
                        String d = questions.readLine();
                        String answer = questions.readLine();
                        String explanation = questions.readLine();

                        if (answer == null || explanation == null) {
                            throw new IOException("Incomplete question entry: " + id);
                        } 
                        String submittedAnswer = "";
                        for (String parameter : parameters) {
                            String[] parts = parameter.split("=", 2);
                            if (parts.length == 2 && parts[0].equals(id.trim())) {
                                submittedAnswer = parts[1];
                                break;
                            } 
                        }
                        total++;
                        output.println("<h3>" + id + "</h3>");
                        if (answer.trim().equals(submittedAnswer)) {
                            score++;
                            output.println("<p>Correct!</p>");
                        } else {
                            output.println("<p>Incorrect! Correct answer: " + answer.trim() + "</p>");
                        }
                        output.println("<p>" + explanation + "</p>");
                        id = questions.readLine();
                    }
                    questions.close();
                    output.println("<h2>Score: " + score + " / " + total + "</h2>");
                    output.println("<a href=\"/math\">Back to SAT Math</a>");
                    client.close();
                    continue;
                }
                String pagePath = "web/index.html";
                if (requestLine != null && requestLine.startsWith("GET /math ")) {
                    pagePath = "web/math.html";
                } else if (requestLine != null && requestLine.startsWith("GET /reading-writing ")) {
                    pagePath = "web/reading-writing.html";
                }
                BufferedReader page = new BufferedReader(new FileReader(pagePath));
                String pageLine = page.readLine();
                /*
                 * Displays the webpage and inserts Math questions with answer choices
                 * into one form so students can submit all answers together. 
                 */
                while (pageLine != null) {
                    if (pagePath.equals("web/math.html")
                            && pageLine.contains("<!-- MATH_QUESTIONS -->")) {
                        BufferedReader questions = new BufferedReader(new FileReader("data/math-questions.txt"));
                        String id = questions.readLine();

                        output.println("<form action=\"/check-math\" method=\"GET\">");
                        while (id != null) {
                            String module = questions.readLine();
                            String topic = questions.readLine();
                            String questionText = questions.readLine();
                            String a = questions.readLine();
                            String b = questions.readLine();
                            String c = questions.readLine();
                            String d = questions.readLine();
                            String answer = questions.readLine();
                            String explanation = questions.readLine();
                            
                            output.println("<h3>" + id + " - " + topic + "</h3>");
                            output.println("<p>" + questionText + "</p>");
                            
                            output.println("<input type=\"radio\" name=\"" + id + "\" value=\"A\" required> A) " + a + "<br>");
                            output.println("<input type=\"radio\" name=\"" + id + "\" value=\"B\" > B) " + b + "<br>");
                            output.println("<input type=\"radio\" name=\"" + id + "\" value=\"C\" > C) " + c + "<br>");
                            output.println("<input type=\"radio\" name=\"" + id + "\" value=\"D\" > D) " + d + "<br>");
                            output.println("<hr>");
                            id = questions.readLine();
                        }
                        questions.close();
                        output.println("<button type=\"submit\">Submit Test</button>");
                        output.println("</form>");
                    } else {
                        output.println(pageLine);
                    }
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
