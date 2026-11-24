import java.io.*;
import java.util.*;

public class SubjectCatalog {
    public static ArrayList<Subject> load() throws IOException {
        ArrayList<Subject> subjects = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("data/subjects.txt"))) {
            String code = reader.readLine();
            
            while (code != null) {
                if (code.trim().isEmpty()) {
                    throw new IOException("Missing subject code.");
                }
                String name = readField(reader);
                String diagnosticPath = readField(reader);
                String practicePath = readField(reader);
                String targetedPath = readField(reader);
                
                int topicCount;
                try {
                    topicCount = Integer.parseInt(readField(reader).trim());
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid topic count: " + code);
                }
                if (topicCount < 1) {
                    throw new IOException("Subject needs topics: " + code);
                }

                String[] topicCodes = new String[topicCount];
                String[] topicNames = new String[topicCount];

                for (int i = 0; i < topicCount; i++) {
                    topicCodes[i] = readField(reader).trim();
                    topicNames[i] = readField(reader);
                }

                subjects.add(new Subject(code.trim(), name, diagnosticPath, practicePath, targetedPath, topicCodes, topicNames));
                code = reader.readLine();
            }
        }
        return subjects;
    }
    private static String readField(BufferedReader reader) throws IOException{
        String line = reader.readLine();
        if (line == null || line.trim().isEmpty()) {
            throw new IOException("Missing subject configuration field.");
        }
        return line;
    }
}