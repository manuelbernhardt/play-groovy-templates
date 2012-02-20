package play.templates;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.List;
import java.util.UUID;

/**
 * Abstracted utility methods for interoperability between Play 1 & Play 2.
 * 
 * This is based on Play 1 so let's see how the implementation in Play 2 goes...
 * 
 */
public abstract class TemplateUtils {

    // ~~~ Per-request stuff

    public abstract String getLang();

    public abstract String getMessage(Object key, Object... args);

    public abstract String getCurrentResponseEncoding();

    public abstract String getAuthenticityToken();

    // ~~~ Logger

    public abstract void logWarn(String message, Object... args);

    public abstract void logWarn(Throwable e, String message, Object... args);

    public abstract void logError(String message, Object... args);

    public abstract void logError(Throwable e, String message);
    
    public abstract void logTraceIfEnabled(String message, Object... args);

    // ~~~ Play

    public abstract boolean isDevMode();

    public abstract boolean usePrecompiled();

    public abstract String getDefaultWebEncoding();

    // ~~~ Files

    public abstract PlayVirtualFile findTemplateWithPath(String path);

    public abstract PlayVirtualFile findFileWithPath(String path);

    public abstract List<PlayVirtualFile> list(PlayVirtualFile parent);

    // ~~~ libs

    public abstract String encodeBASE64(byte[] value);
    
    public abstract byte[] decodeBASE64(String value);
    
    public String UUID() {
        return UUID.randomUUID().toString();
    }

    public abstract byte[] serialize(Object o) throws Exception;

    public abstract Object deserialize(byte[] b) throws Exception;

    public abstract String getDateFormat();

    public abstract String getCurrencySymbol(String currencyCode);

    public abstract String htmlEscape(String value);

    public abstract Integer parseDuration(String duration);

    // TODO for this one we might want to have some kind of Wrapper here
    public abstract Object getMessages();

    public abstract Object getPlay();

    // ~~~ cache

    public abstract Object getCached(String key);

    public abstract void setCached(String key, String data, Integer duration);

    // ~~~ misc, mostly classloading
    
    public abstract ClassLoader getClassLoader();
    
    public abstract List<Class> getAssignableClasses(Class clazz);

    public abstract List<Class> getAllClasses();

    public ProtectionDomain getProtectionDomain() {
        try {
            CodeSource codeSource = new CodeSource(new URL("file:" + getAbsoluteApplicationPath()), (Certificate[]) null);
            Permissions permissions = new Permissions();
            permissions.add(new AllPermission());
            return new ProtectionDomain(codeSource, permissions);
        } catch(MalformedURLException e) {
            throw new TemplateEngineException(TemplateEngineException.ExceptionType.UNEXPECTED, e);
        }
    }

    protected abstract String getAbsoluteApplicationPath();

}
