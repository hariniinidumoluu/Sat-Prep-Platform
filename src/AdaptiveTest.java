import java.time.LocalDateTime;
import java.util.ArrayList;
/**
 * Stores one student's full-length adaptive test.
 */
public class AdaptiveTest {
    private String username;
    private String date;
    private int stage;
    private int rwMod1Score;
    private int mathMod1Score;
    private ArrayList<Question> currentQuestions;
    private ArrayList<Question> questions;
    private ArrayList<String> answers;

    public AdaptiveTest(String username) {
        this.username = username;
        date = LocalDateTime.now().toString();
        stage = 1;
        rwMod1Score = 0;
        mathMod1Score = 0;
        currentQuestions = new ArrayList<>();
        questions = new ArrayList<>();
        answers = new ArrayList<>();
    }
    public ArrayList<String> getAnswers() {
        return answers;
    }
    public ArrayList<Question> getCurrentQuestions() {
        return currentQuestions;
    }
    public String getDate() {
        return date;
    }
    public ArrayList<Question> getQuestions() {
        return questions;
    }
    public int getStage() {
        return stage;
    }
    public String getUsername() {
        return username;
    }
    public void setCurrentQuestions(ArrayList<Question> questions) {
        currentQuestions = questions;
    }

    public boolean useHardReadingMod() {
        return rwMod1Score >= 19;
    }
    public boolean useHardMathMod() {
        return mathMod1Score >= 16;
    }

    /**
     * Saves the current module's answers and advances the test.
     */
    public void finishedMod(ArrayList<String> submittedAnswers) {
        int score = 0;
        for (int i = 0; i < currentQuestions.size(); i++) {
            Question question = currentQuestions.get(i);
            String answer = submittedAnswers.get(i);
            questions.add(question);
            answers.add(answer);
            if (question.getAnswer().equals(answer)) {
                score++;
            }
        }
        if (stage == 1) {
            rwMod1Score = score;
        } else if (stage == 3) {
            mathMod1Score = score;
        }
        stage++;
        currentQuestions = new ArrayList<>();
    }
}