import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

// Displays subject dashboards and topic menus using shared HTML pages
public class SubjectHandler {
    /**
     * Keeps page navigation here and sends quiz requests to QuizHandler.
     */
    public static boolean handle(String requestLine, PrintWriter output) throws IOException {
        if (requestLine == null) {
            return false;
        }
        String[] requestParts = requestLine.split(" ");
        if (requestParts.length < 2) {
            return false;
        }
        String requestPath = requestParts[1];
        String path = requestPath.split("\\?", 2)[0];

        boolean quizRequest = path.equals("/diagnostic")
                            || path.equals("/practice")
                            || path.equals("/targeted") 
                            || path.equals("/check");
        if (!path.equals("/subject") && !path.equals("/topics") && !path.equals("/progress") && !quizRequest) {
            return false;
        }
        Subject subject = null;
        try {
            subject = findSubject(getParameter(requestPath, "subject"));
            if (subject == null) {
                showMessage(null, "Subject not found", "Please choose a subject from the homepage.", output);
                return true;
            }
            if (quizRequest) {
                QuizHandler.handle(path, requestPath, subject, output);
            } else if(path.equals("/progress")) {
                showProgress(subject, output);
            } else {
                String pagePath = "web/subject.html";
                if (path.equals("/topics")) {
                    pagePath = "web/topics.html";
                }
                showPage(pagePath, subject, output);
            }
        } catch (IllegalArgumentException e) {
            showMessage(subject, "Invalid link", "Please check the link and try again.", output);
        }
        return true;
    }
    /**
     * Gets the subject settings without repeating the search.
     */
    private static Subject findSubject(String code) throws IOException {
        for (Subject subject : SubjectCatalog.load()) {
            if (subject.getCode().equals(code)) {
                return subject;
            }
        }
        return null;
    }
    /**
     * Fills the HTML page with the selected subject's information.
     */
    private static void showPage(String pagePath, Subject subject, PrintWriter output) throws IOException {
        try (BufferedReader page = new BufferedReader(new FileReader(pagePath))) {
            String line = page.readLine();
            while (line != null) {
                if (line.contains("<!-- TOPIC_LINKS -->")) {
                    showTopicLinks(subject, output);
                } else {
                    line = line.replace("{{SUBJECT_NAME}}",
                            escape(subject.getName()));
                    line = line.replace("{{SUBJECT_CODE}}",
                            encode(subject.getCode()));
                    output.println(line); 
                }
                line = page.readLine();
            }
        }
    }
    /**
     * Adds a practice link for each topic in the subject.
     */
    private static void showTopicLinks(Subject subject, PrintWriter output) {
        String[] codes = subject.getTopicCodes();
        String[] names = subject.getTopicNames();

        for (int i = 0; i < codes.length; i++) {
            output.println("<p><a class=\"button\" href =\"/practice?subject="
                            + encode(subject.getCode())
                            + "&amp;topic=" + encode(codes[i])
                            + "\">" + escape(names[i]) + "</a></p>");
        }
    }
    /**
     * Uses the HTML template so the page layout is not written in Java.
     */
    public  static void showTemplatePart(Subject subject, String title, boolean beforeContent, PrintWriter output) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader("web/quiz.html"))) {
            boolean passedContent = false;
            String line = reader.readLine();
            while (line != null) {
                if (line.contains("<!-- CONTENT -->")) {
                    passedContent = true;
                    if (beforeContent) {
                        break;
                    }
                } else if (beforeContent || passedContent) {
                    line = line.replace("{{PAGE_TITLE}}", escape(title));
                    String subjectCode = "";
                    if (subject != null) {
                        subjectCode = subject.getCode();
                    }
                    line = line.replace("{{SUBJECT_CODE}}", encode(subjectCode));
                    output.println(line);
                }
                line = reader.readLine();
            }
        }
    }
    /**
     * Uses the same layout when a requested page/quiz is unavailable. 
     */
    public static void showMessage(Subject subject, String title, String message, PrintWriter output) throws IOException {
        showTemplatePart(subject, title, true, output);
        output.println("<p>" + escape(message) + "</p>");
        output.println("<p><a href=\"/\">Back to home</a></p>");
        showTemplatePart(subject, title, false, output);
    }
    /**
     * Gets the requested value from the URL, such as the subject or topic.
     */
    public static String getParameter (String requestPath, String name) {
        String[] parts = requestPath.split("\\?", 2);
        if (parts.length < 2) {
            return "";
        }
        for (String parameter : parts[1].split("&")) {
            String[] pair = parameter.split("=", 2);

            if (pair.length == 2 && URLDecoder.decode(pair[0], StandardCharsets.UTF_8).equals(name)) {
                return URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
            }
        }
        return "";
    }
    /**
     * Converts spaces and special characters for use in links.
     */
    public static String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }
    /**
     * Makes sure special characters display correctly as text on the page.
     */
    public static String escape(String text) {
        return text.replace ("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
    /**
     * Shows saved quizzes for the current profile and selected subject.
     */
    private static void showProgress(Subject subject, PrintWriter output) throws IOException {
        ArrayList<QuizResult> results = ProgressStore.load();
        showTemplatePart(subject, subject.getName() + " Progress", true, output);
        boolean found = false;
        for (QuizResult result : results) {
            // Skips results belonging to another profile/subject.
            if (!result.getUsername().equals(StudentProfile.getUsername())
                    || !result.getSubject().equals(subject.getCode())) {
                continue;
            } 
            found = true;
            output.println("<section>");
            output.println("<h2>" + escape(result.getMode()) + "</h2>");
            output.println("<p>Date: " + escape(result.getDate()) + "</p>");
            output.println("<p>Score: " + result.getScore() + " / " + result.getTotal() + "</p>");

            String[] codes = result.getTopicCodes();
            int[] correct = result.getTopicCorrect();
            int[] totals = result.getTopicTotal();
            for (int i = 0; i < codes.length; i++) {
                if (totals[i] == 0) {
                    continue;
                }
                output.println("<p>Score: " + escape(codes[i]) + ": " + correct[i] + " / " + totals[i] + "</p>");
            }
            output.println("</section>");
        }
        if (!found) {
            output.println("<p>No saved quizzes for this subject yet.</p>");
        }
        showTemplatePart(subject, subject.getName() + " Progress", false, output);
    }
}
