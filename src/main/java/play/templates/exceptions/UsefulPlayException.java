package play.templates.exceptions;

import java.util.List;

/**
 * Interoperability Exception. It has no ID so this needs to be provided when wrapping.
 */
public interface UsefulPlayException {

    String getErrorTitle();

    String getErrorDescription();

    boolean isSourceAvailable();

    Integer getLineNumber();

    String getSourceFile();

    // ~~~ source

    List<String> getSource();

}
