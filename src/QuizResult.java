public class QuizResult {

    private String username;
    private String subject;
    private String mode;
    private String date;
    private int score;
    private int total;
    private String[] topicCodes;
    private int[] topicCorrect;
    private int[] topicTotal;
    private String missedIds;

    public QuizResult(String username, String subject, String mode, String date, int score, int total, String[] topicCodes, int[] topicCorrect, int[] topicTotal, String missedIds) {
        this.username = username;
        this.subject = subject;
        this.mode = mode;
        this.date = date;
        this.score = score;
        this.total = total;
        this.topicCodes = topicCodes;
        this.topicCorrect = topicCorrect;
        this.topicTotal = topicTotal;
        this.missedIds = missedIds; 
    }
    
    public String getDate() {
        return date;
    }
    public String getMissedIds() {
        return missedIds;
    }
    public String getMode() {
        return mode;
    }
    public int getScore() {
        return score;
    }
    public String getSubject() {
        return subject;
    }
    public String[] getTopicCodes() {
        return topicCodes;
    }
    public int[] getTopicCorrect() {
        return topicCorrect;
    }
    public int[] getTopicTotal() {
        return topicTotal;
    }
    public int getTotal() {
        return total;
    }
    public String getUsername() {
        return username;
    }
}