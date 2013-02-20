/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ethelred.mymailtool2.javascript;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import javax.mail.Message;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import org.ethelred.mymailtool2.ApplyMatchOperationsTask;
import org.ethelred.mymailtool2.DeleteOperation;
import org.ethelred.mymailtool2.FileConfigurationHandler;
import org.ethelred.mymailtool2.MailToolConfiguration;
import org.ethelred.mymailtool2.MatchOperation;
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

    private ApplyMatchOperationsTask task;

    public JavascriptFileConfiguration(File f) throws IOException
    {
        task = ApplyMatchOperationsTask.create();
        ctx = Context.enter();
        Scriptable scope = ctx.initStandardObjects();
        ScriptableObject.putProperty(scope, "callback", Context.javaToJS(new Callback(), scope));
        ctx.evaluateString(scope, SETUP_CALLBACK, "setup", 1, null);
        ctx.evaluateReader(scope, new FileReader(f), f.getName(), 1, null);
    }

    public class Callback
    {
        @SuppressWarnings("UnusedDeclaration")
        public void config(Object conf)
        {
            config = JSObjectWrapper.wrap(conf);
        }

        @SuppressWarnings("UnusedDeclaration")
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
        private final String folderName;

        public MoveBuilder(String folderName)
        {
            super();
            this.folderName = folderName;
        }

        public MoveBuilder to(String destinationName)
        {
            task.addRule(folderName, new MatchOperation(this, new MoveOperation(destinationName), /* TODO */ 0), false);
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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Task getTask() throws Exception
    {
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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getTimeLimit()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private class OperationBuilder implements Predicate<Message>
    {
        protected Predicate<Message> delegate;

        private OperationBuilder()
        {
            this.delegate = Predicates.alwaysTrue();
        }

        public OperationBuilder ifIt(Predicate<Message> matcher)
        {

            return and(matcher);
        }

        private OperationBuilder and(Predicate<Message> matcher)
        {
            delegate = Predicates.and(delegate, matcher);
            return this;
        }

        public boolean apply(@javax.annotation.Nullable Message message)
        {
            return delegate.apply(message);
        }
    }

    public class SplitBuilder extends OperationBuilder
    {
        public SplitBuilder(String folderName)

        {
            task.addRule(folderName, new MatchOperation(this, new SplitOperation(), /* TODO */ 0), false);
        }
    }

    public class DeleteBuilder extends OperationBuilder
    {
        public DeleteBuilder(String folderName)
        {
            task.addRule(folderName, new MatchOperation(this, new DeleteOperation(), /* TODO */ 0), false);
        }
    }
}
