package play.templates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import play.templates.exceptions.TemplateCompilationException;
import play.templates.exceptions.TemplateNotFoundException;

/**
 * Load templates
 */
public class GenericTemplateLoader {

    static TemplateUtils utils;

    static TemplateEngine engine;

    protected static Map<String, BaseTemplate> templates = new HashMap<String, BaseTemplate>();
    /**
     * See getUniqueNumberForTemplateFile() for more info
     */
    private static AtomicLong nextUniqueNumber = new AtomicLong(1000);//we start on 1000
    private static Map<String, String> templateFile2UniqueNumber = new HashMap<String, String>();

    /**
     * All loaded templates is cached in the templates-list using a key.
     * This key is included as part of the classname for the generated class for a specific template.
     * The key is included in the classname to make it possible to resolve the original template-file
     * from the classname, when creating cleanStackTrace
     *
     * This method returns a unique representation of the path which is usable as part of a classname
     *
     * @param path
     * @return
     */
    public static String getUniqueNumberForTemplateFile(String path) {
        //a path cannot be a valid classname so we have to convert it somehow.
        //If we did some encoding on the path, the result would be at least as long as the path.
        //Therefor we assign a unique number to each path the first time we see it, and store it..
        //This way, all seen paths gets a unique number. This number is our UniqueValidClassnamePart..

        String uniqueNumber = templateFile2UniqueNumber.get(path);
        if (uniqueNumber == null) {
            //this is the first time we see this path - must assign a unique number to it.
            uniqueNumber = Long.toString(nextUniqueNumber.getAndIncrement());
            templateFile2UniqueNumber.put(path, uniqueNumber);
        }
        return uniqueNumber;
    }

    /**
     * Load a template from a virtual file
     * @param file A VirtualFile
     * @return The executable template
     */
    public static Template load(PlayVirtualFile file) {
        // Use default engine
        final String key = getUniqueNumberForTemplateFile(file.relativePath());
        if (!templates.containsKey(key) || templates.get(key) == null || templates.get(key).compiledTemplate == null) {
            if (utils.usePrecompiled()) {
                BaseTemplate template = engine.createTemplate(file.relativePath().replaceAll("\\{(.*)\\}", "from_$1").replace(":", "_").replace("..", "parent"), file.contentAsString());
                try {
                    template.loadPrecompiled();
                    templates.put(key, template);
                    return template;
                } catch(Exception e) {
                    utils.logWarn("Precompiled template %s not found, trying to load it dynamically...", file.relativePath());
                }
            }
            BaseTemplate template = engine.createTemplate(file.relativePath(), file.contentAsString());
            if (template.loadFromCache()) {
                templates.put(key, template);
            } else {
                templates.put(key, engine.getTemplateCompiler().compile(file));
            }
        } else {
            BaseTemplate template = templates.get(key);
            if (utils.isDevMode() && template.timestamp < file.lastModified()) {
                templates.put(key, engine.getTemplateCompiler().compile(file));
            }
        }
        if (templates.get(key) == null) {
            throw new TemplateNotFoundException(file.relativePath());
        }
        return templates.get(key);
    }

    /**
     * Load a template from a String
     * @param key A unique identifier for the template, used for retreiving a cached template
     * @param source The template source
     * @return A Template
     */
    public static BaseTemplate load(String key, String source) {
        if (!templates.containsKey(key) || templates.get(key).compiledTemplate == null) {
            BaseTemplate template = engine.createTemplate(key, source);
            if (template.loadFromCache()) {
                templates.put(key, template);
            } else {
                templates.put(key, engine.getTemplateCompiler().compile(template));
            }
        } else {
            BaseTemplate template = engine.createTemplate(key, source);
            if (utils.isDevMode()) {
                templates.put(key, engine.getTemplateCompiler().compile(template));
            }
        }
        if (templates.get(key) == null) {
            throw new TemplateNotFoundException(key);
        }
        return templates.get(key);
    }

    /**
     * Clean the cache for that key
     * Then load a template from a String
     * @param key A unique identifier for the template, used for retreiving a cached template
     * @param source The template source
     * @return A Template
     */
    public static BaseTemplate load(String key, String source, boolean reload) {
        cleanCompiledCache(key);
        return load(key, source);
    }

    /**
     * Load template from a String, but don't cache it
     * @param source The template source
     * @return A Template
     */
    public static BaseTemplate loadString(String source) {
        BaseTemplate template = engine.createTemplate(source);
        return engine.getTemplateCompiler().compile(template);
    }

    /**
     * Cleans the cache for all templates
     */
    public static void cleanCompiledCache() {
        templates.clear();
    }

    /**
     * Cleans the specified key from the cache
     * @param key The template key
     */
    public static void cleanCompiledCache(String key) {
        templates.remove(key);
    }

    /**
     * Load a template
     * @param path The path of the template (ex: Application/index.html)
     * @return The executable template
     */
    public static Template load(String path) {
        Template template = null;
        PlayVirtualFile templateFile = utils.findTemplateWithPath(path);
        if(templateFile != null) {
            template = load(templateFile);
        }
        /*
        if (template == null) {
        //When using the old 'key = (file.relativePath().hashCode() + "").replace("-", "M");',
        //the next line never return anything, since all values written to templates is using the
        //above key.
        //when using just file.relativePath() as key, the next line start returning stuff..
        //therefor I have commented it out.
        template = templates.get(path);
        }
         */
        //TODO: remove ?
        if (template == null) {
            PlayVirtualFile tf = utils.findFileWithPath(path);
            if (tf != null && tf.exists()) {
                template = load(tf);
            } else {
                throw new TemplateNotFoundException(path);
            }
        }
        return template;
    }

}
