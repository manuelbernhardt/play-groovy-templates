package play.templates.exceptions;

import java.util.Arrays;
import java.util.List;

import play.templates.Template;

public class TemplateNotFoundException extends RuntimeException implements UsefulPlayException {

    private String path;
    private String sourceFile;
    private List<String> source;
    private Integer line;

    public TemplateNotFoundException(String path) {
        super("Template not found : " + path);
        this.path = path;
    }

    public TemplateNotFoundException(String path, Template template, Integer line) {
        this(path);
        this.sourceFile = template.name;
        this.source = Arrays.asList(template.source.split("\n"));
        this.line = line;
    }

    public String getPath() {
        return path;
    }

    public String getErrorTitle() {
        return String.format("Template not found");
    }

    public String getErrorDescription() {
        return String.format("The template <strong>%s</strong> does not exist.", path);
    }

    public boolean isSourceAvailable() {
        return source != null;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public List<String> getSource() {
        return source;
    }

    public Integer getLineNumber() {
        return line;
    }

}
