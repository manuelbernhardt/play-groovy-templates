package play.templates;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.GroovyShell;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;
import org.apache.commons.lang.StringEscapeUtils;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilationUnit.GroovyClassOperation;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.tools.GroovyClass;
import play.exceptions.TagInternalException;
import play.templates.exceptions.TemplateCompilationException;
import play.templates.exceptions.TemplateExecutionException;
import play.templates.exceptions.TemplateExecutionException.DoBodyException;
import play.templates.exceptions.TemplateNotFoundException;

/**
 * A template
 */
public abstract class GroovyTemplate extends BaseTemplate {

    static {
        new GroovyShell().evaluate("java.lang.String.metaClass.if = { condition -> if(condition) delegate; else '' }");
    }

    public GroovyTemplate(String name, String source) {
        super(name, source);
    }

    public GroovyTemplate(String source) {
        super(source);
    }

    public static class TClassLoader extends GroovyClassLoader {

        public TClassLoader() {
            super(TemplateEngine.utils.getClassLoader());
        }

        public Class defineTemplate(String name, byte[] byteCode) {
            return defineClass(name, byteCode, 0, byteCode.length, TemplateEngine.utils.getProtectionDomain());
        }
    }

    @SuppressWarnings("unchecked")
    void directLoad(byte[] code) throws Exception {
        TClassLoader tClassLoader = new TClassLoader();
        String[] lines = new String(code, "utf-8").split("\n");
        this.linesMatrix = (HashMap<Integer, Integer>) utils.deserialize(utils.decodeBASE64(lines[1]));
        this.doBodyLines = (HashSet<Integer>) utils.deserialize(utils.decodeBASE64(lines[3]));
        for (int i = 4; i < lines.length; i = i + 2) {
            String className = lines[i];
            byte[] byteCode = utils.decodeBASE64(lines[i + 1]);
            Class c = tClassLoader.defineTemplate(className, byteCode);
            if (compiledTemplate == null) {
                compiledTemplate = c;
            }
        }
    }

    public void compile() {
        try {
            if (compiledTemplate == null) {
                try {
                    long start = System.currentTimeMillis();

                    TClassLoader tClassLoader = new TClassLoader();

                    // Let's compile the groovy source
                    final List<GroovyClass> groovyClassesForThisTemplate = new ArrayList<GroovyClass>();
                    // ~~~ Please !
                    CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
                    compilerConfiguration.setSourceEncoding("utf-8"); // ouf
                    CompilationUnit compilationUnit = new CompilationUnit(compilerConfiguration);
                    compilationUnit.addSource(new SourceUnit(name, compiledSource, compilerConfiguration, tClassLoader, compilationUnit.getErrorCollector()));
                    Field phasesF = compilationUnit.getClass().getDeclaredField("phaseOperations");
                    phasesF.setAccessible(true);
                    LinkedList[] phases = (LinkedList[]) phasesF.get(compilationUnit);
                    LinkedList<GroovyClassOperation> output = new LinkedList<GroovyClassOperation>();
                    phases[Phases.OUTPUT] = output;
                    output.add(new GroovyClassOperation() {
                        public void call(GroovyClass gclass) {
                            groovyClassesForThisTemplate.add(gclass);
                        }
                    });
                    compilationUnit.compile();
                    // ouf

                    // Define script classes
                    StringBuilder sb = new StringBuilder();
                    sb.append("LINESMATRIX" + "\n");
                    sb.append(utils.encodeBASE64(utils.serialize(linesMatrix)).replaceAll("\\s", ""));
                    sb.append("\n");
                    sb.append("DOBODYLINES" + "\n");
                    sb.append(utils.encodeBASE64(utils.serialize(doBodyLines)).replaceAll("\\s", ""));
                    sb.append("\n");
                    for (GroovyClass gclass : groovyClassesForThisTemplate) {
                        tClassLoader.defineTemplate(gclass.getName(), gclass.getBytes());
                        sb.append(gclass.getName());
                        sb.append("\n");
                        sb.append(utils.encodeBASE64(gclass.getBytes()).replaceAll("\\s", ""));
                        sb.append("\n");
                    }
                    // Cache
                    TemplateEngine.engine.cacheBytecode(sb.toString().getBytes("utf-8"), name, source);
                    compiledTemplate = tClassLoader.loadClass(groovyClassesForThisTemplate.get(0).getName());
                    if (System.getProperty("precompile") != null) {
                        try {
                            // emit bytecode to standard class layout as well
                            File f = TemplateEngine.engine.getPrecompiledTemplate(name.replaceAll("\\{(.*)\\}", "from_$1").replace(":", "_").replace("..", "parent"));
                            f.getParentFile().mkdirs();
                            FileOutputStream fos = new FileOutputStream(f);
                            fos.write(sb.toString().getBytes("utf-8"));
                            fos.close();
                        } catch (Exception e) {
                            utils.logWarn(e, "Unexpected");
                        }
                    }

                    utils.logTraceIfEnabled("%sms to compile template %s to %d classes", System.currentTimeMillis() - start, name, groovyClassesForThisTemplate.size());

                } catch (MultipleCompilationErrorsException e) {
                    if (e.getErrorCollector().getLastError() != null) {
                        SyntaxErrorMessage errorMessage = (SyntaxErrorMessage) e.getErrorCollector().getLastError();
                        SyntaxException syntaxException = errorMessage.getCause();
                        Integer line = this.linesMatrix.get(syntaxException.getLine());
                        if (line == null) {
                            line = 0;
                        }
                        String message = syntaxException.getMessage();
                        if (message.indexOf("@") > 0) {
                            message = message.substring(0, message.lastIndexOf("@"));
                        }
                        throw new TemplateCompilationException(this, line, message);
                    }
                    throw new TemplateEngineException(TemplateEngineException.ExceptionType.UNEXPECTED, e);
                } catch (Exception e) {
                    throw new TemplateEngineException(TemplateEngineException.ExceptionType.UNEXPECTED, e);
                }
            }
            compiledTemplateName = compiledTemplate.getName();
        } catch(Throwable t) {
            TemplateEngine.engine.handleException(t);
        }
    }

    @Override
    public String render(Map<String, Object> args) {
        try {
            return super.render(args);
        } finally {
            currentTemplate.remove();
        }
    }

    @Override
    protected String internalRender(Map<String, Object> args) {
        compile();
        Binding binding = new Binding(args);
        binding.setVariable("play", utils.getPlay());
        binding.setVariable("messages", utils.getMessages());
        binding.setVariable("lang", utils.getLang());
        // If current response-object is present, add _response_encoding'
        String currentResponseEncoding = TemplateEngine.engine.getCurrentResponseEncoding();
        if (currentResponseEncoding != null) {
            binding.setVariable("_response_encoding", currentResponseEncoding);
        }
        StringWriter writer = null;
        Boolean applyLayouts = false;

        // must check if this is the first template being rendered..
        // If this template is called from inside another template,
        // then args("out") have already been initialized

        if (!args.containsKey("out")) {
            // This is the first template being rendered.
            // We have to set up the PrintWriter that this (and all sub-templates) are going
            // to write the output to..
            applyLayouts = true;
            layout.set(null);
            writer = new StringWriter();
            binding.setProperty("out", new PrintWriter(writer));
            currentTemplate.set(this);
        }
        if (!args.containsKey("_body") && !args.containsKey("_isLayout") && !args.containsKey("_isInclude")) {
            layoutData.set(new HashMap<Object, Object>());
            TagContext.init();
        }
        ExecutableTemplate t = (ExecutableTemplate) InvokerHelper.createScript(compiledTemplate, binding);
        t.template = this;
        Monitor monitor = null;
        try {
            monitor = MonitorFactory.start(name);
            long start = System.currentTimeMillis();
            t.run();
            monitor.stop();
            monitor = null;
            utils.logTraceIfEnabled("%sms to render template %s", System.currentTimeMillis() - start, name);
        } catch (TemplateEngineException e) {
            handleException(e);
        } catch (DoBodyException e) {
            if (utils.isDevMode()) {
                compiledTemplate = null;
                TemplateEngine.engine.deleteBytecode(name);
            }
            Exception ex = (Exception) e.getCause();
            throwException(ex);
        } catch (Throwable e) {
            if (utils.isDevMode()) {
                compiledTemplate = null;
                TemplateEngine.engine.deleteBytecode(name);
            }
            throwException(e);
        } finally {
            if (monitor != null) {
                monitor.stop();
            }
        }
        if (applyLayouts && layout.get() != null) {
            Map<String, Object> layoutArgs = new HashMap<String, Object>(args);
            layoutArgs.remove("out");
            layoutArgs.put("_isLayout", true);
            String layoutR = layout.get().internalRender(layoutArgs);

            // Must replace '____%LAYOUT%____' inside the string layoutR with the content from writer..
            final String whatToFind = "____%LAYOUT%____";
            final int pos = layoutR.indexOf(whatToFind);
            if (pos >=0) {
                // prepending and appending directly to writer/buffer to prevent us
                // from having to duplicate the string.
                // this makes us use half of the memory!
                writer.getBuffer().insert(0,layoutR.substring(0,pos));
                writer.append(layoutR.substring(pos+whatToFind.length()));
                return writer.toString().trim();
            }
            return layoutR;
        }
        if (writer != null) {
            return writer.toString();
        }
        return null;
    }

    Throwable cleanStackTrace(Throwable e) {
        List<StackTraceElement> cleanTrace = new ArrayList<StackTraceElement>();
        for (StackTraceElement se : e.getStackTrace()) {
            //Here we are parsing the classname to find the file on disk the template was generated from.
            //See GroovyTemplateCompiler.head() for more info.
            if (se.getClassName().startsWith("Template_")) {
                String tn = se.getClassName().substring(9);
                if (tn.indexOf("$") > -1) {
                    tn = tn.substring(0, tn.indexOf("$"));
                }
                BaseTemplate template = GenericTemplateLoader.templates.get(tn);
                if( template != null ) {
                    Integer line = template.linesMatrix.get(se.getLineNumber());
                    if (line != null) {
                        String ext = "";
                        if (tn.indexOf(".") > -1) {
                            ext = tn.substring(tn.indexOf(".") + 1);
                            tn = tn.substring(0, tn.indexOf("."));
                        }
                        StackTraceElement nse = new StackTraceElement(GenericTemplateLoader.templates.get(tn).name, ext, "line", line);
                        cleanTrace.add(nse);
                    }
                }
            }
            if (!se.getClassName().startsWith("org.codehaus.groovy.") && !se.getClassName().startsWith("groovy.") && !se.getClassName().startsWith("sun.reflect.") && !se.getClassName().startsWith("java.lang.reflect.") && !se.getClassName().startsWith("Template_")) {
                cleanTrace.add(se);
            }
        }
        e.setStackTrace(cleanTrace.toArray(new StackTraceElement[cleanTrace.size()]));
        return e;
    }

    /**
     * Groovy template
     */
    public static abstract class ExecutableTemplate extends Script {

        // Leave this field public to allow custom creation of TemplateExecutionException from different pkg
        public GroovyTemplate template;

        @Override
        public Object getProperty(String property) {
            try {
                if (property.equals("actionBridge")) {
                    return new ActionBridge(this);
                }
                return super.getProperty(property);
            } catch (MissingPropertyException mpe) {
                return null;
            }
        }

        public void invokeTag(Integer fromLine, String tag, Map<String, Object> attrs, Closure body) {
            String templateName = tag.replace(".", "/");
            String callerExtension = "tag";
            if (template.name.indexOf(".") > 0) {
                callerExtension = template.name.substring(template.name.lastIndexOf(".") + 1);
            }
            BaseTemplate tagTemplate = null;
            try {
                tagTemplate = (BaseTemplate)GenericTemplateLoader.load("tags/" + templateName + "." + callerExtension);
            } catch (TemplateNotFoundException e) {
                try {
                    tagTemplate = (BaseTemplate)GenericTemplateLoader.load("tags/" + templateName + ".tag");
                } catch (TemplateNotFoundException ex) {
                    if (callerExtension.equals("tag")) {
                        throw new TemplateNotFoundException("tags/" + templateName + ".tag", template, fromLine);
                    }
                    throw new TemplateNotFoundException("tags/" + templateName + "." + callerExtension + " or tags/" + templateName + ".tag", template, fromLine);
                }
            }
            TagContext.enterTag(tag);
            Map<String, Object> args = new HashMap<String, Object>();
            args.put("session", getBinding().getVariables().get("session"));
            args.put("flash", getBinding().getVariables().get("flash"));
            args.put("request", getBinding().getVariables().get("request"));
            args.put("params", getBinding().getVariables().get("params"));
            args.put("play", getBinding().getVariables().get("play"));
            args.put("lang", getBinding().getVariables().get("lang"));
            args.put("messages", getBinding().getVariables().get("messages"));
            args.put("out", getBinding().getVariable("out"));
            args.put("_attrs", attrs);
            // all other vars are template-specific
            args.put("_caller", getBinding().getVariables());
            if (attrs != null) {
                for (Map.Entry<String, Object> entry : attrs.entrySet()) {
                    args.put("_" + entry.getKey(), entry.getValue());
                }
            }
            args.put("_body", body);
            try {
                tagTemplate.internalRender(args);
            } catch (TagInternalException e) {
                throw new TemplateExecutionException(template, fromLine, e.getMessage(), template.cleanStackTrace(e));
            } catch (TemplateNotFoundException e) {
                throw new TemplateNotFoundException(e.getPath(), template, fromLine);
            }
            TagContext.exitTag();
        }

        public Class _(String className) throws Exception {
            try {
                return TemplateEngine.utils.getClassLoader().loadClass(className);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        /**
         * This method is faster to call from groovy than __safe() since we only evaluate val.toString()
         * if we need to
         */
        public String __safeFaster(Object val) {
            if (val != null) {
                if (val instanceof RawData) {
                    return ((RawData) val).data;
                } else if (TagContext.hasParentTag("verbatim")) {
                    return val.toString();
                } else if (template.name.endsWith(".xml")) {
                    return StringEscapeUtils.escapeXml(val.toString());
                } else if (template.name.endsWith(".csv")) {
                    return StringEscapeUtils.escapeCsv(val.toString());
                } else if (template.name.endsWith(".html")) {
                    return TemplateEngine.utils.htmlEscape(val.toString());
                } else {
                    return val.toString();
                }
            } else {
                return "";
            }
        }

        public String __getMessage(Object[] val) {
            if (val == null) {
                throw new NullPointerException("You are trying to resolve a message with an expression " +
                        "that is resolved to null - " +
                        "have you forgotten quotes around the message-key?");
            }
            if (val.length == 1) {
                return TemplateEngine.utils.getMessage(val[0]);
            } else {
                // extract args from val
                Object[] args = new Object[val.length-1];
                for(int i = 1; i < val.length; i++) {
                    args[i - 1] = val[i];
                }
                return TemplateEngine.utils.getMessage(val[0], args);
            }
        }

        public String __reverseWithCheck_absolute_true(String action) {
            return __reverseWithCheck(action, true);
        }

        public String __reverseWithCheck_absolute_false(String action) {
            return __reverseWithCheck(action, false);
        }

        private String __reverseWithCheck(String action, boolean absolute) {
            return TemplateEngine.engine.reverseWithCheck(action, absolute);
        }

        public String __safe(Object val, String stringValue) {
            if (val instanceof RawData) {
                return ((RawData) val).data;
            }
            if (!template.name.endsWith(".html") || TagContext.hasParentTag("verbatim")) {
                return stringValue;
            }
            return TemplateEngine.utils.htmlEscape(stringValue);
        }

        public Object get(String key) {
            return GroovyTemplate.layoutData.get().get(key);
        }

        public static class ActionBridge extends GroovyObjectSupport {

            ExecutableTemplate template = null;
            String controller = null;
            boolean absolute = false;

            public ActionBridge(ExecutableTemplate template, String controllerPart, boolean absolute) {
                this.template = template;
                this.controller = controllerPart;
                this.absolute = absolute;
            }

            public ActionBridge(ExecutableTemplate template) {
                this.template = template;
            }

            @Override
            public Object getProperty(String property) {
                return new ActionBridge(template, controller == null ? property : controller + "." + property, absolute);
            }

            public Object _abs() {
                this.absolute = true;
                return this;
            }

            @Override
            @SuppressWarnings("unchecked")
            public Object invokeMethod(String name, Object param) {
              return TemplateEngine.engine.handleActionInvocation(controller, name, param, absolute, template);
            }
        }
    }
}
