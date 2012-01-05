package play.templates;

/**
 * Interoperability interface between Play 1 and Play 2 for VirtualFile
 */
public interface PlayVirtualFile {

    String relativePath();

    String contentAsString();

    Long lastModified();

    boolean exists();

    String getName();

    boolean isDirectory();

}
