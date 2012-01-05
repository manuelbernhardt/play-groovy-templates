package play.templates.exceptions;

import java.util.Arrays;
import java.util.List;

import play.templates.Template;

public abstract class TemplateException extends RuntimeException implements UsefulPlayException {

    private Template template;
    private Integer lineNumber;

    public TemplateException(Template template, Integer lineNumber, String message) {
        super(message);
        this.template = template;
        this.lineNumber = lineNumber;
    }

    public TemplateException(Template template, Integer lineNumber, String message, Throwable cause) {
        super(message, cause);
        this.template = template;
        this.lineNumber = lineNumber;
    }

    public Template getTemplate() {
        return template;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public List<String> getSource() {
        return Arrays.asList(template.source.split("\n"));
    }

    public String getSourceFile() {
        return template.name;
    }

    public boolean isSourceAvailable() {
        return true;
    }

}
