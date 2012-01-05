package play.templates;

import groovy.lang.Closure;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import play.exceptions.TagInternalException;
import play.templates.exceptions.TemplateExecutionException;
import play.templates.exceptions.TemplateNotFoundException;
import play.templates.BaseTemplate.RawData;
import play.templates.GroovyTemplate.ExecutableTemplate;

/**
 * Fast tags implementation
 */
public class FastTags {

    public static void _cache(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        String key = args.get("arg").toString();
        String duration = null;
        if (args.containsKey("for")) {
            duration = args.get("for").toString();
        }
        Object cached = TemplateEngine.utils.getCached(key);
        if (cached != null) {
            out.print(cached);
            return;
        }
        String result = JavaExtensions.toString(body);
        TemplateEngine.utils.setCached(key, result, TemplateEngine.utils.parseDuration(duration));
        out.print(result);
    }

    public static void _verbatim(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        out.println(JavaExtensions.toString(body));
    }

    public static void _jsAction(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        out.println("function(options) {var pattern = '" + args.get("arg").toString().replace("&amp;", "&") + "'; for(key in options) { pattern = pattern.replace(':'+key, options[key]); } return pattern }");
    }

    public static void _authenticityToken(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        out.println("<input type=\"hidden\" name=\"authenticityToken\" value=\"" + TemplateEngine.engine.getAuthenticityToken() + "\">");
    }

    public static void _option(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        Object value = args.get("arg");
        Object selectedValue = TagContext.parent("select").data.get("selected");
        boolean selected = selectedValue != null && value != null && (selectedValue.toString()).equals(value.toString());
        out.print("<option value=\"" + (value == null ? "" : value) + "\" " + (selected ? "selected=\"selected\"" : "") + " " + serialize(args, "selected", "value") + ">");
        out.println(JavaExtensions.toString(body));
        out.print("</option>");
    }

    static boolean _evaluateCondition(Object test) {
        if (test != null) {
            if (test instanceof Boolean) {
                return ((Boolean) test).booleanValue();
            } else if (test instanceof String) {
                return ((String) test).length() > 0;
            } else if (test instanceof Number) {
                return ((Number) test).intValue() != 0;
            } else if (test instanceof Collection) {
                return !((Collection) test).isEmpty();
            } else {
                return true;
            }
        }
        return false;
    }

    static String __safe(Template template, Object val) {
        if (val instanceof RawData) {
            return ((RawData) val).data;
        }
        if (!template.name.endsWith(".html") || TagContext.hasParentTag("verbatim")) {
            return val.toString();
        }
        return TemplateEngine.utils.htmlEscape(val.toString());
    }

    public static void _doLayout(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        out.print("____%LAYOUT%____");
    }

    public static void _get(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        Object name = args.get("arg");
        if (name == null) {
            throw new TemplateExecutionException(template.template, fromLine, "Specify a variable name", new TagInternalException("Specify a variable name"));
        }
        Object value = BaseTemplate.layoutData.get().get(name);
        if (value != null) {
            out.print(value);
        } else {
            if (body != null) {
                out.print(JavaExtensions.toString(body));
            }
        }
    }

    public static void _set(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        // Simple case : #{set title:'Yop' /}
        for (Map.Entry<?, ?> entry : args.entrySet()) {
            Object key = entry.getKey();
            if (!key.toString().equals("arg")) {
                BaseTemplate.layoutData.get().put(key, (entry.getValue() != null && entry.getValue() instanceof String) ? __safe(template.template, entry.getValue()) : entry.getValue());
                return;
            }
        }
        // Body case
        Object name = args.get("arg");
        if (name != null && body != null) {
            Object oldOut = body.getProperty("out");
            StringWriter sw = new StringWriter();
            body.setProperty("out", new PrintWriter(sw));
            body.call();
            BaseTemplate.layoutData.get().put(name, sw.toString());
            body.setProperty("out", oldOut);
        }
    }

    public static void _extends(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        try {
            if (!args.containsKey("arg") || args.get("arg") == null) {
                throw new TemplateExecutionException(template.template, fromLine, "Specify a template name", new TagInternalException("Specify a template name"));
            }
            String name = args.get("arg").toString();
            if (name.startsWith("./")) {
                String ct = BaseTemplate.currentTemplate.get().name;
                if (ct.matches("^/lib/[^/]+/app/views/.*")) {
                    ct = ct.substring(ct.indexOf("/", 5));
                }
                ct = ct.substring(0, ct.lastIndexOf("/"));
                name = ct + name.substring(1);
            }
            BaseTemplate.layout.set((BaseTemplate) GenericTemplateLoader.load(name));
        } catch (TemplateNotFoundException e) {
            throw new TemplateNotFoundException(e.getPath(), template.template, fromLine);
        }
    }

    @SuppressWarnings("unchecked")
    public static void _include(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        try {
            if (!args.containsKey("arg") || args.get("arg") == null) {
                throw new TemplateExecutionException(template.template, fromLine, "Specify a template name", new TagInternalException("Specify a template name"));
            }
            String name = args.get("arg").toString();
            if (name.startsWith("./")) {
                String ct = BaseTemplate.currentTemplate.get().name;
                if (ct.matches("^/lib/[^/]+/app/views/.*")) {
                    ct = ct.substring(ct.indexOf("/", 5));
                }
                ct = ct.substring(0, ct.lastIndexOf("/"));
                name = ct + name.substring(1);
            }
            BaseTemplate t = (BaseTemplate) GenericTemplateLoader.load(name);
            Map<String, Object> newArgs = new HashMap<String, Object>();
            newArgs.putAll(template.getBinding().getVariables());
            newArgs.put("_isInclude", true);
            t.internalRender(newArgs);
        } catch (TemplateNotFoundException e) {
            throw new TemplateNotFoundException(e.getPath(), template.template, fromLine);
        }
    }

    @SuppressWarnings("unchecked")
    public static void _render(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        try {
            if (!args.containsKey("arg") || args.get("arg") == null) {
                throw new TemplateExecutionException(template.template, fromLine, "Specify a template name", new TagInternalException("Specify a template name"));
            }
            String name = args.get("arg").toString();
            if (name.startsWith("./")) {
                String ct = BaseTemplate.currentTemplate.get().name;
                if (ct.matches("^/lib/[^/]+/app/views/.*")) {
                    ct = ct.substring(ct.indexOf("/", 5));
                }
                ct = ct.substring(0, ct.lastIndexOf("/"));
                name = ct + name.substring(1);
            }
            args.remove("arg");
            BaseTemplate t = (BaseTemplate) GenericTemplateLoader.load(name);
            Map<String, Object> newArgs = new HashMap<String, Object>();
            newArgs.putAll((Map<? extends String, ? extends Object>) args);
            newArgs.put("_isInclude", true);
            newArgs.put("out", out);
            t.internalRender(newArgs);
        } catch (TemplateNotFoundException e) {
            throw new TemplateNotFoundException(e.getPath(), template.template, fromLine);
        }
    }

    public static String serialize(Map<?, ?> args, String... unless) {
        StringBuilder attrs = new StringBuilder();
        Arrays.sort(unless);
        for (Object o : args.keySet()) {
            String attr = o.toString();
            String value = args.get(o) == null ? "" : args.get(o).toString();
            if (Arrays.binarySearch(unless, attr) < 0 && !attr.equals("arg")) {
                attrs.append(attr);
                attrs.append("=\"");
                attrs.append(value);
                attrs.append("\" ");
            }
        }
        return attrs.toString();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public static @interface Namespace {

        String value() default "";
    }
}
