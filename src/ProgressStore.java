import java.io.*;
import java.util.ArrayList;

public class ProgressStore {
    /**
     * Adds a completed quiz without deleting previous results.
     */
    public static void save(QuizResult result) throws IOException {
        File folder = new File("data");
        if (!folder.isDirectory() && !folder.mkdir()) {
            throw new IOException("Could not create the data folder.");
        }
        try (PrintWriter writer = new PrintWriter(new FileWriter("data/progress.txt", true))) {
            writer.println(result.getUsername());
            writer.println(result.getSubject());
            writer.println(result.getMode());
            writer.println(result.getDate());
            writer.println(result.getScore());
            writer.println(result.getTotal());
            writer.println(result.getMissedIds());

            String[] codes = result.getTopicCodes();
            int[] correct = result.getTopicCorrect();
            int[] totals = result.getTopicTotal();

            writer.println(codes.length);
            for (int i = 0; i < codes.length; i++) {
                writer.println(codes[i]);
                writer.println(correct[i]);
                writer.println(totals[i]);
            }
            if (writer.checkError()) {
                throw new IOException("Could not save quiz results.");
            }
        }
    }
    /**
     * Reads the saved quiz results into a list.
     */
    public static ArrayList<QuizResult> load() throws IOException {
        ArrayList<QuizResult> results = new ArrayList<>();
        File file = new File("data/progress.txt");
        
        // No file means no quizzes have been saved yet
        if (!file.exists()) {
            return results;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String username = reader.readLine();
            while (username != null) {
                if (username.isBlank()) {
                    throw new IOException("Missing profile in progress file.");
                }
                String subject = readField(reader);
                String mode = readField(reader);
                String date = readField(reader);
                int score = Integer.parseInt(readField(reader));
                int total = Integer.parseInt(readField(reader));

                String missedIds = readField(reader);
                int topicCount = Integer.parseInt(readField(reader));
                if (total < 1 || score < 0 || score > total
                        || topicCount < 1 || topicCount > 100) {
                    throw new IOException("Invalid saved quiz results.");   
                }
                String[] codes = new String[topicCount];
                int[] correct = new int[topicCount];
                int[] totals = new int[topicCount];
                int correctSum = 0;
                int totalSum = 0;

                for (int i = 0; i < topicCount; i++) {
                    codes[i] = readField(reader);
                    correct[i] = Integer.parseInt(readField(reader));
                    totals[i] = Integer.parseInt(readField(reader));
                    if (codes[i].isBlank() || totals[i] < 0
                            || correct[i] < 0 || correct[i] > totals[i]) {
                        throw new IOException("Invalid saved topic score.");    
                    }
                    correctSum += correct[i];
                    totalSum += totals[i];
                }
                if (correctSum != score || totalSum != total) {
                    throw new IOException("Saved scores do not match.");
                }
                results.add(new QuizResult(username, subject, mode, date, score, total, codes, correct, totals, missedIds));
                username = reader.readLine();
            }
        } catch (NumberFormatException e) {
            throw new IOException("Invalid number in progress file.", e);    
        }
        return results;
    }
    /**
     * Checks that the next line exists before using it.
     */
    private static String readField(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("Incomplete progress file.");    
        }
        return line;
    }
}
