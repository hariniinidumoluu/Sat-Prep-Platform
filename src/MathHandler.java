import java.io.*;
import java.net.*;
import java.util.*;

    public class MathHandler {
       /**
        * Handles Math diagnostic tests, grading,
        * topic practice, and targeted practice.
        */
        public static boolean handle(String requestLine, PrintWriter output) throws IOException{
            if (requestLine != null && requestLine.startsWith("GET /check-math?")) {
            String requestPath = requestLine.split(" ")[1];
            String query = requestPath.substring(requestPath.indexOf("?") + 1);
            String[] parameters = query.split("&");
            int score = 0;
            int total = 0;
            String[] topics = {
                "Algebra",
                "Advanced Math",
                "Problem-Solving & Data Analysis",
                "Geometry & Trigonometry"
            };
            int[] topicCorrect = new int[topics.length];
            int[] topicTotal = new int[topics.length];

            BufferedReader questions = new BufferedReader(new FileReader("data/math-questions.txt"));
            String id = questions.readLine();
            while (id != null) {
                    String module = questions.readLine();
                    String topic = questions.readLine();
                    String skill = questions.readLine();
                    String difficulty = questions.readLine();
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
                for (int i = 0; i < topics.length; i++) {
                    if (topics[i].equals(topic.trim())) {
                        topicTotal[i]++;
                        if (answer.trim().equals(submittedAnswer)) {
                            topicCorrect[i]++;
                        }
                    }
                }
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
            output.println("<h2>Performance by Topic</h2>");
            String weakTopics = "";
            for (int i = 0; i < topics.length; i++) {
                output.println("<h3>" + topics[i] + "</h3>");
                output.println("<p>" + topicCorrect[i] + " / " + topicTotal[i] + " correct</p>");
                if (topicTotal[i] == 0) {
                output.println("<p>No questions assessed.</p>");
                } else if (topicCorrect[i] == topicTotal[i]) {
                    output.println("<p>Strength in this diagnostic.</p>");
                } else {
                    output.println("<p>Recommended practice area.</p>");
                    if (!weakTopics.isEmpty()) {
                        weakTopics += ",";
                    }
                    weakTopics += i;
                }
            }
            if (!weakTopics.isEmpty()) {
                output.println("<p><a href=\"/math-targeted?topics="
                                + weakTopics + "\">Start Targeted Practice</a></p>");
            } else {
                output.println("<p>You answered every question correctly. " 
                                + "Choose a topic to keep practicing.</p>");
            }

            output.println("<a href=\"/math\">Back to SAT Math</a>");
            return true;
            }

            /**
             * Loads practice questions matching the selected topic.
             * Each array stores one question's 12 fields. 
             */
                if (requestLine != null 
                        && (requestLine.startsWith("GET /math-practice?topic=") 
                        || requestLine.startsWith("GET /math-targeted?topics="))) {
                    String requestPath = requestLine.split(" ")[1];
                    String topicCode = requestPath.substring(requestPath.indexOf("=") + 1);
                    String selectedTopic = "";
                    
                    boolean targeted = requestLine.startsWith("GET /math-targeted?topics=");
                    String[] topics = {
                        "Algebra",
                        "Advanced Math",
                        "Problem-Solving & Data Analysis",
                        "Geometry & Trigonometry"
                    };
                    boolean[] selectedTopics = new boolean[topics.length];
                    if (targeted) {
                        for (String code : topicCode.split(",")) {
                            for (int i = 0; i < topics.length; i++) {
                                if (code.equals("" + i)) {
                                    selectedTopics[i] = true;
                                }
                            }
                        }
                    }
                    if (topicCode.equals("algebra")) {
                        selectedTopic = "Algebra";
                    } else if (topicCode.equals("advanced")) {
                        selectedTopic = "Advanced Math";
                    } else if(topicCode.equals("data")) {
                        selectedTopic = "Problem-Solving & Data Analysis";
                    } else if (topicCode.equals("geometry")) {
                        selectedTopic = "Geometry & Trigonometry";
                    }

                    if (!targeted && selectedTopic.isEmpty()) {
                        output.println("<p>Topic not found.</p>");
                    } else {
                        ArrayList<String[]> practiceQs = new ArrayList<>();
                        BufferedReader questions = new BufferedReader(new FileReader("data/math-practice-by-topic.txt"));
                        String id = questions.readLine();
                        while (id != null) {
                            String[] question = new String[12];
                            question[0] = id;
                            for (int i = 1; i < question.length; i++) {
                                question[i] = questions.readLine();
                                if (question[i] == null) {
                                    throw new IOException("Incomplete question entry: " + id);
                                }
                            }
                            boolean matches = question[2].trim().equals(selectedTopic);
                            if (targeted) {
                                for (int i = 0; i < topics.length; i++) {
                                    if (selectedTopics[i]
                                            && question[2].trim().equals(topics[i])) {
                                        matches = true;
                                        break;
                                    }
                                }
                            }
                            if (matches) {
                                practiceQs.add(question);
                            }
                            id = questions.readLine();
                        }
                        questions.close();
                        if (targeted) {
                            output.println("<h1>Targeted Math Practice</h1>");
                        } else {
                            output.println("<h1>" + selectedTopic + " Practice</h1>");
                        }
                        if (practiceQs.isEmpty()) {
                            output.println("<p>No questions available for this topic.</p>");
                        } else {
                            int quizSize = Math.min(4, practiceQs.size());
                            Random random = new Random();

                            if (quizSize < 4) {
                                output.println("<p>Only " + quizSize + " questions are available.</p>");
                            }
                            output.println("<form action=\"/check-math-practice\" method=\"GET\">");
                            for (int i = 0; i < quizSize; i++) {
                                int index = random.nextInt(practiceQs.size());
                                String[] question = practiceQs.remove(index);
                                output.println("<fieldset>");
                                output.println("<legend>" + question[0] + " - " + question[5] + "</legend>");
                                String[] letters = {"A", "B", "C", "D"};
                                for (int j = 0; j < letters.length; j++) {
                                    output.println("<label><input type=\"radio\" name=\"" + 
                                                    question[0] + "\" value=\"" + letters[j]
                                                    + "\" required> " + letters[j] + ") "
                                                    + question[6 + j] + "</label><br>");
                                }
                                output.println("</fieldset><br>");
                            }
                            output.println("<button type=\"submit\">Submit Practice</button>");
                            output.println("</form>");
                        }
                    }
                    output.println("<a href=\"/math-topics\">Back to topics</a>");
                    return true;
                }

                /**
                 * Checks submitted practice answers against the separate practice bank
                 * and displays explanations and a score for the submitted questions.
                 */
                if (requestLine != null && requestLine.startsWith("GET /check-math-practice?")) {
                    String requestPath = requestLine.split(" ")[1];
                    String query = requestPath.substring(requestPath.indexOf("?") + 1);
                    String[] parameters = query.split("&");
                    int score = 0;
                    int total = 0;
                    BufferedReader questions = new BufferedReader(new FileReader("data/math-practice-by-topic.txt"));
                    String id = questions.readLine();
                    output.println("<h1>Math Practice Results</h1>");
                    while (id != null) {
                            String module = questions.readLine();
                            String topic = questions.readLine();
                            String skill = questions.readLine();
                            String difficulty = questions.readLine();
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
                        String submittedAnswer = null;
                        for (String parameter : parameters) {
                            String[] parts = parameter.split("=", 2);
                            if (parts.length == 2 && parts[0].equals(id.trim())) {
                                submittedAnswer = parts[1];
                                break;
                            } 
                        }
                        if (submittedAnswer != null) {
                            total++;
                            output.println("<h3>" + id + "</h3>");
                            output.println("<p>" + questionText + "</p>");
                        
                            if (answer.trim().equals(submittedAnswer)) {
                                score++;
                                output.println("<p>Correct!</p>");
                            } else {
                                output.println("<p>Incorrect! Correct answer: " + answer.trim() + "</p>");
                            }
                            output.println("<p>" + explanation + "</p>");
                        }
                        id = questions.readLine();
                    }                    
                    questions.close();
                    if (total == 0) {
                        output.println("<p>No matching practice answers were submitted.</p>");
                    } else {
                        output.println("<h2> Score: " + score + " / " + total + "</h2>");
                    }
                    output.println("<a href=\"/math\">Back to SAT Math</a>");
                    return true;
                }
                /**
                 * Displays the webpage and inserts Math questions with answer choices
                 * into one form so students can submit all answers together. 
                 */
                if (requestLine != null && requestLine.startsWith("GET /math-diagnostic ")) {
                    BufferedReader page = new BufferedReader(new FileReader("web/math-diagnostic.html"));
                    String pageLine = page.readLine();
                    while (pageLine != null) {
                        if (pageLine.contains("<!-- MATH_QUESTIONS -->")) {
                            BufferedReader questions = new BufferedReader(new FileReader("data/math-questions.txt"));
                            String id = questions.readLine();
                            output.println("<form action=\"/check-math\" method=\"GET\">");
                            while (id != null) {
                                String module = questions.readLine();
                                String topic = questions.readLine();
                                String skill = questions.readLine();
                                String difficulty = questions.readLine();
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
                    return true;
                }
                return false;
            }
        }