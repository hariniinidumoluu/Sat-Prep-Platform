public class Question {
    private String subject;
    private String topic;
    private String skill;
    private String difficulty;
    private String id;
    private String text;
    private String[] choices;
    private String answer;
    private String explanation;

    public Question(String subject, String topic, String skill, 
                    String difficulty, String id, String text,
                    String[] choices, String answer, String explanation) {
        this.subject = subject;
        this.topic = topic;
        this.skill = skill;
        this.difficulty = difficulty;
        this.id = id;
        this.text = text;
        this.choices = choices;
        this.answer = answer;
        this.explanation = explanation;
    }
    public String getAnswer() {
        return answer;
    }
    public String[] getChoices() {
        return choices;
    }
    public String getDifficulty() {
        return difficulty;
    }
    public String getExplanation() {
        return explanation;
    }
    public String getId() {
        return id;
    }
    public String getSkill() {
        return skill;
    }
    public String getSubject() {
        return subject;
    }
    public String getText() {
        return text;
    }
    public String getTopic() {
        return topic;
    }
}