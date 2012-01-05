package play.templates.exceptions;

import play.templates.Template;

public class TemplateCompilationException extends TemplateException {

    public TemplateCompilationException(Template template, Integer lineNumber, String message) {
        super(template, lineNumber, message);
    }

    public String getErrorTitle() {
        return String.format("Template compilation error");
    }

    public String getErrorDescription() {
        return String.format("The template <strong>%s</strong> does not compile : <strong>%s</strong>", getTemplate().name, getMessage());
    }
}
