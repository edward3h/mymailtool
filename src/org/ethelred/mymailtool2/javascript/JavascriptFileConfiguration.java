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

import javax.mail.Message;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.ethelred.mymailtool2.ApplyMatchOperationsTask;
import org.ethelred.mymailtool2.DeleteOperation;
import org.ethelred.mymailtool2.FileConfigurationHandler;
import org.ethelred.mymailtool2.MailToolConfiguration;
import org.ethelred.mymailtool2.MatchOperation;
import org.ethelred.mymailtool2.MessageOperation;
import org.ethelred.mymailtool2.MoveOperation;
import org.ethelred.mymailtool2.SplitOperation;
import org.ethelred.mymailtool2.Task;
import org.ethelred.mymailtool2.matcher.FromAddressMatcher;
import org.ethelred.mymailtool2.matcher.SubjectMatcher;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 *
 * @author edward
 */
class JavascriptFileConfiguration implements MailToolConfiguration
{
    private IJSObject config;

    private Context ctx;
    private static final String SETUP_CALLBACK = "for(var fn in callback) {\n" +
            "  if(typeof callback[fn] === 'function') {\n" +
            "    this[fn] = (function() {\n" +
            "      var method = callback[fn];\n" +
            "      return function() {\n" +
            "         return method.apply(callback,arguments);\n" +
            "      };\n" +
            "    })();\n" +
            "  }\n" +
            "}";

    private List<String> fileLocations = Lists.newArrayList();
    private List<OperationBuilder> deferredRules = Lists.newArrayList();

    public JavascriptFileConfiguration(File f) throws IOException
    {
        ctx = Context.enter();
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

        public Predicate<Message> isFrom(String regex)
        {
            return new FromAddressMatcher(false, regex);
        }

        public Predicate<Message> matchesSubject(String regex)
        {
            return new SubjectMatcher(regex);
        }
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
        return config.getString("mymailtool.password");
    }

    @Override
    public Map<String, String> getMailProperties()
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for(String k: ALL_MAIL_PROPERTIES) 
        {
            String v = config.getString(k);
            if(v != null)
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
    public Task getTask() throws Exception
    {
        ApplyMatchOperationsTask task = ApplyMatchOperationsTask.create();
        for(OperationBuilder builder: deferredRules)
        {
            builder.addToTask(task);
        }
        return task;
    }

    @Override
    public int getOperationLimit()
    {
            return Integer.parseInt(config.getString("operations"));
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

    private abstract class OperationBuilder implements Predicate<Message>
    {
        protected Predicate<Message> delegate;
        protected int specificity = 0;
        protected String folderName;
        protected boolean includeSubFolders = false;

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
            specificity++;
            return this;
        }

        public OperationBuilder includeSubFolders()
        {
            includeSubFolders = true;
            return this;
        }

        @Override
        public boolean apply(@javax.annotation.Nullable Message message)
        {
            return delegate.apply(message);
        }

        public void addToTask(ApplyMatchOperationsTask task)
        {
            task.addRule(folderName, new MatchOperation(this, getOperation(), specificity), includeSubFolders);
        }

        protected abstract MessageOperation getOperation();
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
}
