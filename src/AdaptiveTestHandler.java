import java.io.*;
import java.util.*;
/**
 * Runs the full-length adaptive test using HTML templates.
 */
public class AdaptiveTestHandler {
    private static AdaptiveTest test;
    /**
     * Clears the current test so another profile cannot use it.
     */
    public static void clear() {
        test = null;
    }
    /**
     * Handles starting the test, displaying the modules, and submitting answers.
     * Makes sure the student has selected a profile first.
     */
    public static boolean handle(String requestLine, PrintWriter output) throws IOException {
        if (requestLine == null) {
            return false;
        }
        String[] parts = requestLine.split(" ");
        if (parts.length < 2) {
            return false;
        }
        String method = parts[0];
        String requestPath = parts[1];
        String path = requestPath.split("\\?", 2)[0];
        
        if (!path.equals("/full-test")
                && !path.equals("/full-start")
                && !path.equals("/full-module")
                && !path.equals("/full-submit")
                && !path.equals("/full-results")) {
            return false;
        }
        if (!StudentProfile.isSelected()) {
            showMessage("Please select a student profile first.", output);
            return true;
        }
        if (path.equals("/full-test")) {
            showTemplate("full-test.html", "", true, output);
            return true;
        }
        if (path.equals("/full-start")) {
            String username = SubjectHandler.getParameter(requestPath, "username");
            if (!method.equals("POST")
                    || !username.equals(StudentProfile.getUsername())) {
                showMessage("Please reopen the test menu.", output);
                return true;
            }
            checkBanks();
            AdaptiveTest newTest = new AdaptiveTest(StudentProfile.getUsername());
            newTest.setCurrentQuestions(loadMod(findSubject("reading-writing"), "module1"));
            test = newTest;
            showMessage("Your test is ready. Select Current Test to begin.", output);
            return true;
        }
        if (test == null
                || !test.getUsername().equals(StudentProfile.getUsername())) {
            clear();
            showMessage("Please start a test using your current profile.", output);
            return true;    
        }
        if (path.equals("/full-submit")) {
            if (!method.equals("POST")) {
                showMessage("Please submit answers using the module form.", output);
                return true;
            }
            if (!submitMod(requestPath)) {
                showMessage("This submission is incomplete or belongs " + "to an older module. Return to your current test.", output);
                return true;
            }
        }
        if (test.getStage() == 5) {
            showResults(output);
        } else {
            showMod(output);
        }
        return true;
    }
    /**
     * Gets the subject information needed to load the right questions.
     */
    private static Subject findSubject(String code) throws IOException {
        for (Subject sub : SubjectCatalog.load()) {
            if (sub.getCode().equals(code)) {
                return sub;
            }
        }
        throw new IOException("Subject not found: " + code);
    }
    /**
     * Loads the questions for a module.
     * Checks the question count, topics, and IDs before using the bank.
     */
    private static ArrayList<Question> loadMod(Subject subject, String module) throws IOException {
        String file = "data/" + subject.getCode() + "-full-" + module +".txt";
        ArrayList<Question> questions = QuestionBank.load(file);
        int expected = 27;
        if (subject.getCode().equals("math")) {
            expected = 22;
        }
        if (questions.size() != expected) {
            throw new IOException(file + " needs " + expected + " questions.");
        }
        ArrayList<String> ids = new ArrayList<>();
        for (Question q : questions) {
            if (!q.getSubject().equals(subject.getCode())
                    || QuizHandler.findTopic(subject, q.getTopic()) == -1) {
                throw new IOException("Invalid subject or topic: " + q.getId());
            }
            if (q.getId().contains(",") || ids.contains(q.getId())) {
                throw new IOException("Invalid or duplicate id: " + q.getId());
            }
            ids.add(q.getId());
        }
        return questions;
    }
    /**
     * Checks all six banks before a test starts.
     * This prevents a student from getting stuck because of a 
     * missing or invalid bank later in the test.
     */
    private static void checkBanks() throws IOException {
        String[] codes = {"reading-writing", "math"};
        String[] modules = {"module1", "module2-easy", "module2-hard"};
        ArrayList<String> ids = new ArrayList<>();
        for (String c : codes) {
            Subject subject = findSubject(c);
            for (String mod : modules) {
                for (Question q : loadMod(subject, mod)) {
                    if (ids.contains(q.getId())) {
                        throw new IOException("Repeated full-test ID: " + q.getId());
                    }
                    ids.add(q.getId());
                } 
            }
            
        }
    }
    /**
     * Shows the questions for the student's current module.
     * Uses their Module 1 performance to choose the easier or harder Module 2.
     */
    private static void showMod(PrintWriter output) throws IOException {
        int stage = test.getStage();
        String code = "reading-writing";
        if (stage >= 3) {
            code = "math";
        }
        Subject subject = findSubject(code);
        String module = "module1";
        int num = 1;

        if (stage == 2 || stage == 4) {
            num = 2;
            module = "module2-easy";
            if ((stage == 2 && test.useHardReadingMod()) 
                    || (stage == 4 && test.useHardMathMod())) {
                module = "module2-hard";
            }
        }
        if (test.getCurrentQuestions().isEmpty()) {
            ArrayList<Question> questions = loadMod(subject, module);
            for (Question q : questions) {
                for (Question previous : test.getQuestions()) {
                    if (q.getId().equals(previous.getId())) {
                        throw new IOException("Repeated question: " + q.getId());
                    }
                }
            }
            test.setCurrentQuestions(questions);
        }
        String title = subject.getName() + " - Module " + num;
        showTemplate("full-module.html", title, true, output);
        for (Question question : test.getCurrentQuestions()) {
            QuizHandler.showQuestion(question, output);
        }
        showTemplate("full-module.html", title, false, output);
    }
    /**
     * Checks that all answers belong to the current profile and module.
     * Rejects old or incomplete submissions so they cannot change the test.
     */
    private static boolean submitMod(String requestPath) {
        String username = SubjectHandler.getParameter(requestPath, "username");
        String date = SubjectHandler.getParameter(requestPath, "attemptDate");
        String stage = SubjectHandler.getParameter(requestPath, "stage");
        if (!username.equals(test.getUsername())
                || !date.equals(test.getDate())
                || !stage.equals(String.valueOf(test.getStage()))
                || test.getStage() == 5
                || test.getCurrentQuestions().isEmpty()) {
            return false;
        }
        ArrayList<String> answers = new ArrayList<>();
        for (Question q : test.getCurrentQuestions()) {
            String answer = SubjectHandler.getParameter(requestPath, "answer_" + q.getId());
            if (answer.length() != 1
                    || answer.charAt(0) < 'A'
                    || answer.charAt(0) >= 'A' + q.getChoices().length) {
                return false;  
            }
            answers.add(answer);
        }
        test.finishedMod(answers);
        return true;
    }
    /**
     * Saves the Math and Reading & Writing results separately.
     * Then shows the student's total score and answer explanations.
     */
    private static void showResults(PrintWriter output) throws IOException{
        if (test.getQuestions().size() != 98 
                || test.getAnswers().size() != 98) {
            throw new IOException("A completed test needs 98 answers.");
        }

        Subject reading = findSubject("reading-writing");
        Subject math = findSubject("math");
        QuizResult readingResult = createResult(reading);
        QuizResult mathResult = createResult(math);
        ProgressStore.save(readingResult);
        ProgressStore.save(mathResult);

        String title = "Full-Length Test Results";
        showTemplate("full-results.html", title, true, output);
        output.println("<h2>Total: " + (readingResult.getScore() + mathResult.getScore()) + " / 98</h2>");
        output.println("<p> These are raw practice scores, " + "not official SAT scaled scores.</p>");
        showSubjectResults(reading, readingResult, output);
        showSubjectResults(math, mathResult, output);
        showTemplate("full-results.html", title, false, output);
    }
    /**
     * Calculates one subject's score using both completed modules.
     * Also stores missed questions and scores for each topic.
     */
    private static QuizResult createResult(Subject subject) throws IOException{
        int score = 0;
        int total = 0;
        String missedIds = "";

        int[] correct = new int[subject.getTopicCodes().length];
        int[] totals = new int[correct.length];
        for (int i = 0; i < test.getQuestions().size(); i++) {
            Question q = test.getQuestions().get(i);
            if (!q.getSubject().equals(subject.getCode())) {
                continue;
            }
            int topic = QuizHandler.findTopic(subject, q.getTopic());
            if (topic == -1) {
                throw new IOException("Unknown topic: " + q.getId());
            }
            total++;
            totals[topic]++;
            if (q.getAnswer().equals(test.getAnswers().get(i))) {
                score++;
                correct[topic]++;
            } else {
                if (!missedIds.isEmpty()) {
                    missedIds += ",";
                }
                missedIds += q.getId();
            }
        }
        int expected = 54;
        if (subject.getCode().equals("math")) {
            expected = 44;
        }
        if (total != expected) {
            throw new IOException("Incorrect subject question count.");
        }
        return new QuizResult(test.getUsername(), subject.getCode(), "full-length", test.getDate(), score, total, subject.getTopicCodes(), correct, totals, missedIds);
    }
    /**
     * Shows the subject score, topic breakdown, and submitted answers.
     * Reuses the feedback code from the regular practice quizzes.
     */
    private static void showSubjectResults(Subject subject, QuizResult result, PrintWriter output) {
        output.println("<h2>" + SubjectHandler.escape(subject.getName()) + ": " + result.getScore() + " / " + result.getTotal() + "</h2>");
        QuizHandler.showPerformance(subject, result.getTopicCorrect(), result.getTopicTotal(), output);
        for (int i = 0; i < test.getQuestions().size(); i++) {
            Question q = test.getQuestions().get(i);
            if (q.getSubject().equals(subject.getCode())) {
                String answer = test.getAnswers().get(i);
                output.println("<p>Your Answer: " + SubjectHandler.escape(answer) + "</p>");
                QuizHandler.showFeedback(q, q.getAnswer().equals(answer), output);
            }
        }
    }
    /**
     * Reads an HTML template and fills in the test information.
     * Keeps the page layout in HTML instead of Java.
     */
    private static void showTemplate(String file, String title, boolean beforeContent, PrintWriter output) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader("web/" + file))) {
            boolean passedContent = false;
            String line = reader.readLine();
            while (line != null) {
                if (line.contains("<!-- CONTENT -->")) {
                    passedContent = true;
                    if (beforeContent) {
                        break;
                    }
                } else if (beforeContent || passedContent) {
                    line = line.replace("{{TITLE}}", SubjectHandler.escape(title));
                    line = line.replace("{{USERNAME}}", SubjectHandler.escape(StudentProfile.getUsername()));
                    if (test != null) {
                        line = line.replace("{{DATE}}", SubjectHandler.escape(test.getDate()));
                        line = line.replace("{{STAGE}}", String.valueOf(test.getStage()));
                        line = line.replace("{{COUNT}}", String.valueOf(test.getCurrentQuestions().size()));
                    }
                    output.println(line);
                }
                line = reader.readLine();
            }
        }
    }
    /**
     * Displays test messages using the same results page layout.
     */
    private static void showMessage(String message, PrintWriter output) throws IOException {
        showTemplate("full-results.html", "Full-Length Test", true, output);
        output.println("<p>" + SubjectHandler.escape(message) + "</p>");
        showTemplate("full-results.html", "Full-Length Test", false, output);
    }
}