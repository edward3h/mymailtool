/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ethelred.mymailtool2.javascript;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import jakarta.mail.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.ethelred.mymailtool2.*;
import org.ethelred.mymailtool2.matcher.AgeMatcher;
import org.ethelred.mymailtool2.matcher.FromAddressMatcher;
import org.ethelred.mymailtool2.matcher.HasAttachmentMatcher;
import org.ethelred.mymailtool2.matcher.HasFlagMatcher;
import org.ethelred.mymailtool2.matcher.SubjectMatcher;
import org.ethelred.mymailtool2.matcher.ToAddressMatcher;
import org.ethelred.util.Predicates;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 *
 * @author edward
 */
class JavascriptFileConfiguration extends BaseFileConfiguration
{
    private static final Logger LOGGER = LogManager.getLogger(JavascriptFileConfiguration.class);
    private IJSObject config;

    private static final String SETUP_CALLBACK = "for(var fn in callback) {\n"
            + "  if(typeof callback[fn] === 'function') {\n"
            + "    this[fn] = (function() {\n"
            + "      var method = callback[fn];\n"
            + "      return function() {\n"
            + "         return method.apply(callback,arguments);\n"
            + "      };\n"
            + "    })();\n"
            + "  }\n"
            + "}";

    private List<String> fileLocations = Lists.newArrayList();
    private List<OperationBuilder> deferredRules = Lists.newArrayList();

    private static class DeferredTask implements Task
    {
        private Task delegate;

        @Override
        public void run()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void init(MailToolContext ctx)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean orderNewestFirst()
        {
            return delegate.orderNewestFirst();
        }

        public void setDelegate(ApplyMatchOperationsTask delegate)
        {
            this.delegate = delegate;
        }
    }

    private DeferredTask deferredTask = new DeferredTask();

    public JavascriptFileConfiguration(File f) throws IOException
    {
        super(f);
        Context ctx = Context.enter();
        Scriptable scope = ctx.initStandardObjects();
        ScriptableObject.putProperty(scope, "callback", Context.javaToJS(new Callback(), scope));
        ctx.evaluateString(scope, SETUP_CALLBACK, "setup", 1, null);
        ctx.evaluateReader(scope, new FileReader(f), f.getName(), 1, null);
    }

    @SuppressWarnings("UnusedDeclaration")
    public class Callback
    {
        public void config(Object conf)
        {
            config = JSObjectWrapper.wrap(conf);
        }

        public void print(Object value)
        {
            System.out.println(value);
        }

        public void include(String fileName)
        {
            fileLocations.add(fileName);
        }

        public MoveBuilder move(String folderName)
        {
            return new MoveBuilder(folderName);
        }

        public SplitBuilder split(String folderName)
        {
            return new SplitBuilder(folderName);
        }

        public DeleteBuilder deleteFrom(String folderName)
        {
            return new DeleteBuilder(folderName);
        }

        public FlagBuilder addFlag(String flagname)
        {
            return new FlagBuilder(true, flagname);
        }

        public Predicate<Message> isFrom(String... regex)
        {
            return new FromAddressMatcher(false, first(regex), rest(regex));
        }

        public Predicate<Message> matchesSubject(String regex)
        {
            return new SubjectMatcher(regex);
        }

        public Predicate<Message> isTo(String... regex)
        {
            return new ToAddressMatcher(false, first(regex), rest(regex));
        }

        public Predicate<Message> hasAttachment(String regex)
        {
            return new HasAttachmentMatcher(regex);
        }

        public Predicate<Message> hasFlag(String flag)
        {
            return new HasFlagMatcher(flag);
        }

        public Predicate<Message> isOlderThan(String age)
        {
            return new AgeMatcher(age, true, deferredTask);
        }

        public Predicate<Message> isNewerThan(String age)
        {
            return new AgeMatcher(age, false, deferredTask);
        }
    }

    private String[] rest(String[] strings)
    {
        if (strings.length > 1)
        {
            String[] result = new String[strings.length - 1];
            System.arraycopy(strings, 1, result, 0, strings.length - 1);
            return result;
        }
        return new String[0];
    }

    private String first(String[] strings)
    {
        if (strings.length > 0)
        {
            return strings[0];
        }
        return null;
    }

    public class MoveBuilder extends OperationBuilder
    {
        private String destinationName;

        public MoveBuilder(String folderName)
        {
            super(folderName);
            this.folderName = folderName;
        }

        @Override
        protected MessageOperation getOperation()
        {
            return new MoveOperation(destinationName);
        }

        public MoveBuilder to(String destinationName)
        {
            this.destinationName = destinationName;
            return this;
        }

    }

    @Override
    public String getPassword()
    {
        return config.getString("password");
    }

    @Override
    public Map<String, String> getMailProperties()
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for (String k : ALL_MAIL_PROPERTIES) 
        {
            String v = config.getString(k);
            if (v != null)
            {
                builder.put(k, v);
            }
        }
        return builder.build();    
    }

    @Override
    public String getUser()
    {
        return config.getString(USER);
    }

    @Override
    public Iterable<String> getFileLocations()
    {
        return fileLocations;
    }

    @Override
    public Task getTask() {
        LOGGER.info("getTask {}", deferredRules);
        ApplyMatchOperationsTask task = ApplyMatchOperationsTask.create();
        for (OperationBuilder builder : deferredRules)
        {
            builder.addToTask(task);
        }
        deferredTask.setDelegate(task);
        return task.hasRules() ? task : null;
    }

    @Override
    public int getOperationLimit()
    {
            return Integer.parseInt(config.getString("operations"));
    }

    @Override
    public int getChunkSize()
    {
        String v = config.getString("chunk");
        if (v == null)
        {
            return PRIMITIVE_DEFAULT;
        }
        try {
            return Integer.parseInt(v);
        }
        catch (Exception e)
        {
            return PRIMITIVE_DEFAULT;
        }
    }

    @Override
    public boolean randomTraversal() {
        return false;
    }

    @Override
    public String getMinAge()
    {
        return config.getString("minage");
    }

    @Override
    public Iterable<FileConfigurationHandler> getFileHandlers()
    {
        return Collections.emptyList();
    }

    @Override
    public String getTimeLimit()
    {
        return config.getString("runtime.limit");
    }

    @Override
    public boolean verbose()
    {
        return false;
    }

    private abstract class OperationBuilder
    {
        protected Predicate<Message> delegate;
        protected List<Predicate<Message>> predicates = Lists.newArrayList();
        protected int specificity;
        protected String folderName;
        protected boolean includeSubFolders;

        private OperationBuilder(String folderName)
        {
            this.delegate = Predicates.alwaysTrue();
            this.folderName = folderName;
            deferredRules.add(this);
        }

        public OperationBuilder ifIt(Predicate<Message> matcher)
        {
            return and(matcher);
        }

        public OperationBuilder and(Predicate<Message> matcher)
        {
            delegate = Predicates.and(delegate, matcher);
            predicates.add(matcher);
            specificity++;
            return this;
        }

        public OperationBuilder includeSubFolders()
        {
            includeSubFolders = true;
            return this;
        }


        public void addToTask(ApplyMatchOperationsTask task)
        {
            task.addRule(folderName, delegate, predicates, getOperation(), includeSubFolders);
        }

        protected abstract MessageOperation getOperation();

        public String toString()
        {
            return delegate.toString();
        }
    }

    public class SplitBuilder extends OperationBuilder
    {
        public SplitBuilder(String folderName)

        {
            super(folderName);
        }

        @Override
        protected MessageOperation getOperation()
        {
            return new SplitOperation();
        }

    }

    public class DeleteBuilder extends OperationBuilder
    {
        public DeleteBuilder(String folderName)
        {
            super(folderName);
        }

        @Override
        protected MessageOperation getOperation()
        {
            return new DeleteOperation();
        }
    }

    public class FlagBuilder extends OperationBuilder
    {
        private String userFlag;
        private boolean add;

        public FlagBuilder(boolean add, String flagname)
        {
            super("");
            this.add = add;
            this.userFlag = flagname;
        }

        public FlagBuilder inFolder(String folderName)
        {
            this.folderName = folderName;
            return this;
        }

        @Override
        protected MessageOperation getOperation()
        {
            return new FlagOperation(add, userFlag);
        }
    }
}
