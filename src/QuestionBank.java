import java.io.*;
import java.util.*;
/**
 * Reads questions from a text file.
 */
public class QuestionBank {
    public static ArrayList<Question> load(String filePath) throws IOException {
        ArrayList<Question> questions = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))){
            String subject = reader.readLine();
            while (subject != null) {
                if (subject.trim().isEmpty()) {
                    throw new IOException("Missing question subject.");
                }
                String topic = readField(reader);
                String skill = readField(reader);
                String difficulty = readField(reader);
                String id = readField(reader);
                String text = readField(reader);

                int numChoices;
                try {
                    numChoices = Integer.parseInt(readField(reader).trim());
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid choice count: " + id);
                }

                if (numChoices < 2 || numChoices > 26) {
                    throw new IOException("Choice count must be 2-26: " + id);
                }
                String[] choices = new String[numChoices];
                for (int i = 0; i < choices.length; i++) {
                    choices[i] = readField(reader);
                }
                String answer = readField(reader).trim();
                String explanation = readField(reader);

                if (answer.length() != 1
                    || answer.charAt(0) < 'A'
                    || answer.charAt(0) >= 'A' + numChoices) {
                    throw new IOException("Invalid answer letter: " + id);
                }

                Question question = new Question(subject, topic, skill, difficulty, id, text, choices, answer, explanation);
                questions.add(question);
                subject = reader.readLine();
            }
        }
        return questions;
    }

    private static String readField(BufferedReader reader) throws IOException{
        String line = reader.readLine();
        if (line == null || line.trim().isEmpty()) {
            throw new IOException("Missing or empty question field.");
        }
        return line;
    }
}