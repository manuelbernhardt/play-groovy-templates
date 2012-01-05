package play.templates;

/**
 * Meta-exception for template engine exceptions. This is mostly a wrapper for implementation-specific types the engine itself doesn't need to know about.
 */
public class TemplateEngineException extends RuntimeException {

    public enum ExceptionType { NO_ROUTE_FOUND, UNEXPECTED, JAVA_EXECUTION, PLAY}

    private final ExceptionType exceptionType;

    public TemplateEngineException(ExceptionType exceptionType) {
        this.exceptionType = exceptionType;
    }
    
    public TemplateEngineException(ExceptionType exceptionType, Throwable t) {
        super(t);
        this.exceptionType = exceptionType;
    }
    
    public TemplateEngineException(ExceptionType exceptionType, String message, Throwable t) {
        super(message, t);
        this.exceptionType = exceptionType;
    }

    public ExceptionType getExceptionType() {
        return this.exceptionType;
    }

}
