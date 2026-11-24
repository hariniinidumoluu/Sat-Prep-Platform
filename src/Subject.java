public class Subject {
    private String code;
    private String name;
    private String diagnosticPath;
    private String practicePath;
    private String[] topicCodes;
    private String[] topicNames;
    private String targetedPath;

    public Subject(String code, String name, String diagnosticPath, 
                    String practicePath, String targetedPath, 
                    String[] topicCodes, String[] topicNames) {
        this.code = code;
        this.name = name;
        this.diagnosticPath = diagnosticPath;
        this.practicePath = practicePath;
        this.targetedPath = targetedPath;
        this.topicCodes = topicCodes;
        this.topicNames = topicNames;
    }
    public String getCode() {
        return code;
    }
    public String getDiagnosticPath() {
        return diagnosticPath;
    }
    public String getName() {
        return name;
    }
    public String getPracticePath() {
        return practicePath;
    }
    public String[] getTopicCodes() {
        return topicCodes;
    }
    public String[] getTopicNames() {
        return topicNames;
    }
    public String getTargetedPath() {
        return targetedPath;
    }
}