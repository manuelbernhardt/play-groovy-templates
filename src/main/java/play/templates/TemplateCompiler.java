package play.templates;

import java.util.Stack;

import play.templates.exceptions.TemplateCompilationException;

public abstract class TemplateCompiler {

    public BaseTemplate compile(BaseTemplate template) {
        try {
            long start = System.currentTimeMillis();
            generate(template);
            TemplateEngine.utils.logTraceIfEnabled("%sms to parse template %s", System.currentTimeMillis() - start, template.name);
            return template;
        } catch (Throwable t) {
            TemplateEngine.engine.handleException(t);
            // the above will have thrown the Exception so null will never be returned
            return null;
        }
    }

    public BaseTemplate compile(PlayVirtualFile file) {
        return compile(TemplateEngine.engine.createTemplate(file.relativePath(), file.contentAsString()));
    }

    StringBuilder compiledSource = new StringBuilder();
    protected BaseTemplate template;
    protected TemplateParser parser;
    boolean doNextScan = true;
    TemplateParser.Token state;
    Stack<Tag> tagsStack = new Stack<Tag>();
    int tagIndex;
    boolean skipLineBreak;
    int currentLine = 1;

    static class Tag {
        String name;
        int startLine;
        boolean hasBody;
    }

    void generate(BaseTemplate template) {
        this.template = template;
        String source = source();
        this.parser = new TemplateParser(source);

        // Class header
        head();

        // Parse
        loop:
        for (;;) {

            if (doNextScan) {
                state = parser.nextToken();
            } else {
                doNextScan = true;
            }

            switch (state) {
                case EOF:
                    break loop;
                case PLAIN:
                    plain();
                    break;
                case SCRIPT:
                    script();
                    break;
                case EXPR:
                    expr();
                    break;
                case MESSAGE:
                    message();
                    break;
                case ACTION:
                    action(false);
                    break;
                case ABS_ACTION:
                    action(true);
                    break;
                case COMMENT:
                    skipLineBreak = true;
                    break;
                case START_TAG:
                    startTag();
                    break;
                case END_TAG:
                    endTag();
                    break;
            }
        }

        // Class end
        end();

        // Check tags imbrication
        if (!tagsStack.empty()) {
            Tag tag = tagsStack.peek();
            throw new TemplateCompilationException(template, tag.startLine, "#{" + tag.name + "} is not closed.");
        }

        // Done !
        template.compiledSource = compiledSource.toString();

        TemplateEngine.utils.logTraceIfEnabled("%s is compiled to %s", template.name, template.compiledSource);

    }

    abstract String source();

    abstract void head();

    abstract void end();

    abstract void plain();

    abstract void script();

    abstract void expr();

    abstract void message();

    protected abstract void action(boolean absolute);

    abstract void startTag();

    abstract void endTag();

    protected void markLine(int line) {
        compiledSource.append("// line ").append(line);
        template.linesMatrix.put(currentLine, line);
    }

    protected void println() {
        compiledSource.append("\n");
        currentLine++;
    }

    protected void print(String text) {
        compiledSource.append(text);
    }

    protected void println(String text) {
        compiledSource.append(text);
        println();
    }
}
