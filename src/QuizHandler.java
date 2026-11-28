import java.io.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
/**
 * Handles diagnostic, topic practice, and targeted practice quizzes for all subjects.
 */
public class QuizHandler {
   /**
    * Opens the correct quiz page based on what the student selects.
    */
    public static void handle(String path, String requestPath, Subject subject, PrintWriter output) throws IOException {
        if (path.equals("/diagnostic")) {
            showQuiz(subject, "diagnostic", loadQuestions(subject, "diagnostic"), subject.getName() + " Diagnostic", output);
        } else if (path.equals("/check")) {
            showResults(requestPath, subject, output);
        } else {
            showPractice(path, requestPath, subject, output);
        }
    }
    /**
     * Loads questions from the correct bank for the selected quiz.
     * Checks for invalid topics and duplicate IDs before using them.
     */
    private static ArrayList<Question> loadQuestions(Subject subject, String mode) throws IOException {
        String filePath;
        if (mode.equals("diagnostic")) {
            filePath = subject.getDiagnosticPath();
        } else if (mode.equals("practice")) {
            filePath = subject.getPracticePath();
        } else if (mode.equals("targeted")) {
            filePath = subject.getTargetedPath();
        } else {
            throw new IOException("Unknown quiz type: " + mode);
        }
        ArrayList<Question> questions = new ArrayList<>();
        for (Question question : QuestionBank.load(filePath)) {
            if (!question.getSubject().equals(subject.getCode())) {
                continue;
            } 
            if (findTopic(subject, question.getTopic()) == -1) {
                throw new IOException("Unknown topic in question: " + question.getId());
            }
            if (question.getId().contains(",")) {
                throw new IOException("Question IDS cannot contain commas: " + question.getId());
            }
            for (Question previous : questions) {
                if (previous.getId().equals(question.getId())) {
                    throw new IOException("Duplicate question ID: " + question.getId());
                }
            }
            questions.add(question);
        }
        return questions;
    }
    /**
     * Finds questions for the selected topics.
     * Shows the available quizzes or opens the quiz the student chooses. 
     */
    private static void showPractice(String path, String requestPath, 
                Subject subject, PrintWriter output) throws IOException {
        boolean targeted = path.equals("/targeted");
        String parameter = "topic";
        String title = subject.getName() + " Topic Practice";
        
        if (targeted) {
            parameter = "topics";
            title = subject.getName() + " Targeted Practice";
        }
        String topics = SubjectHandler.getParameter(requestPath, parameter);
        if (!validTopics(subject, topics)) {
            SubjectHandler.showMessage(subject, "Topic not found", "Please choose a topic from the topic menu.", output);
            return;
        }
        ArrayList<Question> matching = new ArrayList<>();
        String mode = "practice";
        if (targeted) {
            mode = "targeted";
        }
        for (Question question : loadQuestions(subject, mode)) {
            if (containsCode(topics, question.getTopic())) {
                matching.add(question);
            }
        }
        String quizValue = SubjectHandler.getParameter(requestPath, "quiz");
        if (quizValue.isEmpty()) {
            showQuizMenu(subject, topics, targeted, matching.size(), title, output);
            return;
        }
        int quizNumber;
        try {
            quizNumber = Integer.parseInt(quizValue);
        } catch (NumberFormatException e) {
            SubjectHandler.showMessage(subject, "Invalid quiz number", "Please select a quiz from the quiz menu.", output);
            return;
        }
        ArrayList<Question> selected = selectPractice(matching, quizNumber);
        if (selected.isEmpty()) {
            SubjectHandler.showMessage(subject, "Quiz not found", "There are no questions for this quiz.", output);
            return;
        }
        showQuiz(subject, mode, selected, title + " - Quiz " + quizNumber, output);
    }
    /**
     * Makes sure the selected topics are valid for the subject.
     */
    private static boolean validTopics(Subject subject, String topics) {
        if (topics.isEmpty()) {
            return false;
        }
        for (String code : topics.split(",", -1)) {
            if (findTopic(subject, code) == -1) {
                return false;
            }
        }
        return true;
    }
    /**
     * Calculates how many quizzes are needed when each quiz has up to 10 questions.
     */
    private static int getQuizCount(int total) {
        int quizCount = total / 10;
        if (total % 10 != 0) {
            quizCount++;
        }
        return quizCount;
    }
    /**
     * Displays links to the available practice quizzes.
     */
    private static void showQuizMenu(Subject subject, String topics, boolean targeted, int total, String title, PrintWriter output) throws IOException {
        SubjectHandler.showTemplatePart(subject, title, true, output);
        int quizCount = getQuizCount(total);
        if (quizCount == 0) {
            output.println("<p>No questions available yet.</p>");
        } else {
            String link = "/practice?subject="
                        + SubjectHandler.encode(subject.getCode())
                        + "&amp;topic=" + SubjectHandler.encode(topics);
            if (targeted) {
                link = "/targeted?subject="
                    + SubjectHandler.encode(subject.getCode())
                    + "&amp;topics=" + SubjectHandler.encode(topics);
            }

            for (int number = 1; number <= quizCount; number++) {
                int start = (number - 1) * 10;
                int size = Math.min(10, total - start);
                output.println("<p><a class=\"button\" href=\"" + link
                    + "&amp;quiz=" + number + "\">Quiz " + number
                    + "</a> - " + size + " questions</p>");
            }
        }
        SubjectHandler.showTemplatePart(subject, title, false, output);
    }
    /**
     * Keeps different quiz groups from repeating the same questions.
     */
    private static ArrayList<Question> selectPractice(ArrayList<Question> matching, int quizNumber) {
        ArrayList<Question> selected = new ArrayList<>();
        if (quizNumber < 1 || quizNumber > getQuizCount(matching.size())) {
            return selected;
        }
        int start = (quizNumber - 1) * 10;
        int end = Math.min(start + 10, matching.size());
        for (int i = start; i < end; i++) {
            selected.add(matching.get(i));
        }
        return selected;
    }
    /**
     * Displays the questions in an HTML format.
     * Includes the question IDs and attempt information for grading.
     */
    public static void showQuiz(Subject subject, String mode, ArrayList<Question> questions, String title, PrintWriter output) throws IOException{
        SubjectHandler.showTemplatePart(subject, title, true, output);
        if (questions.isEmpty()) {
            output.println("<p>No questions available yet.</p>");
        } else {
            output.println("<form action=\"/check\" method=\"POST\">");
            hiddenField("subject", subject.getCode(), output);
            hiddenField("mode", mode, output);
            // Remembers which profile opens this specific quiz.
            hiddenField("username", StudentProfile.getUsername(), output);
            // Keeps the same attempt date when the results page is refreshed.
            hiddenField("attemptDate", LocalDateTime.now().toString(), output);
            String ids = "";
            for (Question question : questions) {
                if (!ids.isEmpty()) {
                    ids += ",";
                }
                ids += question.getId();
                showQuestion(question, output);
            }
            hiddenField("ids", ids, output);
            output.println("<button type=\"submit\">Submit Answers</button>");
            output.println("</form>");
        }
        SubjectHandler.showTemplatePart(subject, title, false, output);
    }
    /**
     * Displays a question with its answer choices.
     * Uses the choices array so questions can have different numbers of options.
     */
    public static void showQuestion(Question question, PrintWriter output) {
        output.println("<fieldset>");
        output.println("<legend>" + SubjectHandler.escape(question.getId()) + "</legend>");
        output.println("<p>" + SubjectHandler.escape(question.getText()) + "</p>");
        String[] choices = question.getChoices();
        for (int i = 0; i < choices.length; i++) {
            char letter = (char) ('A' + i);
            output.println("<label><input type=\"radio\" name=\"answer_"
                + SubjectHandler.escape(question.getId())
                + "\" value=\"" + letter + "\" required> "
                + letter + ") " + SubjectHandler.escape(choices[i])
                + "</label>");
        }
        output.println("</fieldset><br>");
    }
    /**
     * Grades the submitted questions and calculates the student's score.
     * Checks the profile before saving so results go to the correct student.
     */
    private static void showResults(String requestPath, Subject subject, PrintWriter output) throws IOException {
        String username = SubjectHandler.getParameter(requestPath, "username");
        if (!username.equals(StudentProfile.getUsername())) {
            SubjectHandler.showMessage(subject, "Profile changed", "Please reopen the quiz using your selected profile", output);
            return;
        }
        String mode = SubjectHandler.getParameter(requestPath, "mode");
        if (!mode.equals("diagnostic") && !mode.equals("practice") && !mode.equals("targeted")) {
            SubjectHandler.showMessage(subject, "Quiz type not found", "Please open a quiz and submit your answers", output);
            return;
        }
        ArrayList<Question> questions = loadQuestions(subject, mode);
        String ids = SubjectHandler.getParameter(requestPath, "ids");
        String title = subject.getName() + " Results";
        SubjectHandler.showTemplatePart(subject, title, true, output); 

        int score = 0;
        int total = 0;
        String missedIds = "";

        int[] topicCorrect = new int[subject.getTopicCodes().length];
        int[] topicTotal = new int[topicCorrect.length];

        for (Question question : questions) {
            if (!containsCode(ids, question.getId())) {
                continue;
            }
            String submitted = SubjectHandler.getParameter(requestPath, "answer_" + question.getId());
            boolean correct = question.getAnswer().equals(submitted);
            total++;
            if (correct) {
                score++;
            } else {
                if (!missedIds.isEmpty()) {
                    missedIds += ",";
                }
                missedIds += question.getId();
            }
            int topicIndex = findTopic(subject, question.getTopic());
            topicTotal[topicIndex]++;
            if (correct) {
                topicCorrect[topicIndex]++;
            }
            showFeedback(question, correct, output);
        }
        if (total == 0) {
            output.println("<p>No matching quiz questions were submitted.</p>");
        } else {
            String date = SubjectHandler.getParameter(requestPath, "attemptDate");
            try {
                LocalDateTime.parse(date);
            } catch (DateTimeParseException e) {
                output.println("<p>Please reopen the quiz and submit again.</p>");
                SubjectHandler.showTemplatePart(subject, title, false, output);
                return;
            }
            QuizResult result = new QuizResult(StudentProfile.getUsername(), subject.getCode(), mode, date, score, total, subject.getTopicCodes(), topicCorrect, topicTotal, missedIds);
            ProgressStore.save(result);
            output.println("<h2>Score: " + score + " / " + total + "</h2>");
            showPerformance(subject, topicCorrect, topicTotal, output);
        }
        SubjectHandler.showTemplatePart(subject, title, false, output);
    }
    /**
     * Shows the question, whether the answer was correct, and its explanation.
     * Helps students review what they missed after finishing a quiz.
     */
    public static void showFeedback(Question question, boolean correct, PrintWriter output) {
        output.println("<h2>" + SubjectHandler.escape(question.getId()) + "</h2>");
        output.println("<p>" + SubjectHandler.escape(question.getText()) + "</p>");
        if (correct) {
            output.println("<p>Correct!</p>");
        } else {
            int answerIndex = question.getAnswer().charAt(0) - 'A';
            output.println("<p>Incorrect. Correct answer: "
                + SubjectHandler.escape(question.getAnswer()) + ") "
                + SubjectHandler.escape(question.getChoices()[answerIndex])
                + "</p>");
        }
        output.println("<p>" + SubjectHandler.escape(question.getExplanation()) + "</p>");
    }
    /**
     * Finds the position of a topic in the subject's topic list to keep 
     * track of scores for each topic.
     */
    public static int findTopic(Subject subject, String code) {
        String[] codes = subject.getTopicCodes();
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(code)) {
                return i;
            }
        }
        return -1;
    }
    /**
     * Recommends more practice for topics with missed questions.
     */
    public static void showPerformance(Subject subject, int[] correct, int[] totals, PrintWriter output) {
        String weakTopics = "";
        String[] codes = subject.getTopicCodes();
        String[] names = subject.getTopicNames();
        output.println("<h2>Performance by Topic</h2>");

        for (int i = 0; i < codes.length; i++) {
            output.println("<h3>" + SubjectHandler.escape(names[i]) + "</h3>");
            if (totals[i] == 0) {
                output.println("<p>No questions assessed.</p>");
                continue;
            }
            output.println("<p>" + correct[i] + " / "
                        + totals[i] + " correct</p>");
            if (correct[i] == totals[i]) {
                 output.println("<p>Strength in this quiz.</p>");
            } else {
                output.println("<p>Recommended practice area.</p>");
                if (!weakTopics.isEmpty()) {
                    weakTopics += ",";
                }
                weakTopics += codes[i];
            }
        }
        if (!weakTopics.isEmpty()) {
            output.println("<p><a class=\"button\" href=\"/targeted?subject="
                    + SubjectHandler.encode(subject.getCode())
                    + "&amp;topics=" + SubjectHandler.encode(weakTopics)
                    + "\">Practice These Topics</a></p>");
        }
    }
   /**
    * Checks exact topic or question IDs so similar codes do not get mixed up.
    */
    private static boolean containsCode(String list, String code) {
        for (String item : list.split(",")) {
            if (item.equals(code)) {
                return true;
            }
        }
        return false;
    }
    /**
     * Adds hidden information to the form so it gets submitted with the answers.
     */
    public static void hiddenField(String name, String value, PrintWriter output) {
        output.println("<input type=\"hidden\" name=\""
            + SubjectHandler.escape(name) + "\" value=\""
            + SubjectHandler.escape(value) + "\">");
    }
}
