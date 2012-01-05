package play.templates;

import java.io.File;
import java.util.List;

public abstract class TemplateEngine {

    public static TemplateUtils utils;

    static TemplateEngine engine;

    protected abstract TemplateEngine initEngineImplementation();

    protected abstract TemplateUtils initUtilsImplementation();

    public TemplateEngine() {

    }

    /**
     * Wire the static references to the TemplateEngine and TemplateUtils where needed.
     * Not sure if this is the best way but for the time being it works
     */
    public void startup() {
        TemplateEngine.engine = initEngineImplementation();
        TemplateEngine.utils = initUtilsImplementation();

        GenericTemplateLoader.utils = utils;
        GenericTemplateLoader.engine = engine;
    }

    public abstract void handleException(Throwable e);

    // ~~~ template loading

    public abstract BaseTemplate createTemplate(String source);

    public abstract BaseTemplate createTemplate(String key, String source);

    public abstract byte[] loadPrecompiledTemplate(String name);

    public abstract File getPrecompiledTemplate(String name);

    public abstract List<PlayVirtualFile> getTemplatePaths();

    // ~~~ HTTP

    // TODO this probably needs to go close to the template rendering call
    public abstract String getCurrentResponseEncoding();
    public abstract String getAuthenticityToken();

    // ~~~ routing

    public abstract Object handleActionInvocation(String controller, String name, Object param, boolean absoulte, GroovyTemplate.ExecutableTemplate template);

    public abstract String reverseWithCheck(String action, boolean absolute);

    // ~~~ Plugins

    public abstract List<String> addTemplateExtensions();

    public abstract String overrideTemplateSource(BaseTemplate template, String source);

    // Misc - let's see what to do with this

    public abstract byte[] getCachedTemplate(String name, String source);

    public abstract void cacheBytecode(byte[] byteCode, String name, String source);

    public abstract void deleteBytecode(String name);

    public abstract List<Class<? extends FastTags>> getFastTags();

    // ~~~ only Play 1

    public abstract void compileGroovyRoutes();


}
