package play.templates;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A template
 */
public abstract class BaseTemplate extends Template {

    protected final TemplateUtils utils;

    public String compiledSource;
    public Map<Integer, Integer> linesMatrix = new HashMap<Integer, Integer>();
    public Set<Integer> doBodyLines = new HashSet<Integer>();
    public Class compiledTemplate;
    public String compiledTemplateName;
    public Long timestamp = System.currentTimeMillis();

    public BaseTemplate(String name, String source) {
        this.utils = TemplateEngine.utils;
        this.name = name;
        this.source = source;
    }

    public BaseTemplate(String source) {
        this.utils = TemplateEngine.utils;
        this.name = utils.UUID();
        this.source = source;
    }

    public void loadPrecompiled() {
        try {
            byte[] code = TemplateEngine.engine.loadPrecompiledTemplate(name);
            directLoad(code);
        } catch (Exception e) {
            throw new RuntimeException("Cannot load precompiled template " + name);
        }
    }

    public boolean loadFromCache() {
        try {
            long start = System.currentTimeMillis();
            byte[] bc = TemplateEngine.engine.getCachedTemplate(name, source);
            if (bc != null) {
                directLoad(bc);
                utils.logTraceIfEnabled("%sms to load template %s from cache", System.currentTimeMillis() - start, name);
                return true;
            }
        } catch (Exception e) {
            utils.logWarn(e, "Cannot load %s from cache", name);
        }
        return false;
    }

    abstract void directLoad(byte[] code) throws Exception;

    protected abstract void handleException(TemplateEngineException e);
    protected abstract void throwException(Throwable e);
    protected abstract Throwable cleanStackTrace(Throwable e);
    public static ThreadLocal<BaseTemplate> layout = new ThreadLocal<BaseTemplate>();
    public static ThreadLocal<Map<Object, Object>> layoutData = new ThreadLocal<Map<Object, Object>>();
    public static ThreadLocal<BaseTemplate> currentTemplate = new ThreadLocal<BaseTemplate>();

    public static class RawData {

        public String data;

        public RawData(Object val) {
            if (val == null) {
                data = "";
            } else {
                data = val.toString();
            }
        }

        @Override
        public String toString() {
            return data;
        }
    }
}
