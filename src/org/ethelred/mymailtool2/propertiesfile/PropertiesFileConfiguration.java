package org.ethelred.mymailtool2.propertiesfile;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Message;
import org.ethelred.mymailtool2.ApplyMatchOperationsTask;
import org.ethelred.mymailtool2.DeleteOperation;
import org.ethelred.mymailtool2.FileConfigurationHandler;
import org.ethelred.mymailtool2.FileConfigurationHelper;
import org.ethelred.mymailtool2.MailToolConfiguration;
import org.ethelred.mymailtool2.MatchOperation;
import org.ethelred.mymailtool2.MessageOperation;
import org.ethelred.mymailtool2.MoveOperation;
import org.ethelred.mymailtool2.SplitOperation;
import org.ethelred.mymailtool2.Task;
import org.ethelred.mymailtool2.matcher.FromAddressMatcher;
import org.ethelred.mymailtool2.matcher.ToAddressMatcher;

/**
 *
 * @author edward
 */
class PropertiesFileConfiguration implements MailToolConfiguration
{
    private Properties delegate;
    private List<String> fileLocations = Lists.newArrayList();
    private List<FileConfigurationHandler> fileHandlers = Lists.newArrayList();
    private Map<String, Map<String, String>> rulesTemp
            = new MapMaker().makeComputingMap(new Function<String, Map<String, String>>() {
        public Map<String, String> apply(String f)
        {
            return Maps.newHashMap();
        }
    });
    
    public PropertiesFileConfiguration(File f) throws IOException
    {
        init(f);
    }

    private void init(File f) throws IOException
    {
            delegate = new Properties();
            delegate.load(new FileReader(f));
            
            for(String k: delegate.stringPropertyNames())
            {
                if(k.startsWith("include") || k.startsWith("load"))
                {
                    _addFileLocations(delegate.getProperty(k));
                }
                if(k.startsWith("handler"))
                {
                    _addFileHandlers(delegate.getProperty(k));
                }
                if(k.startsWith("rule"))
                {
                    _addMessageRule(k, delegate.getProperty(k));
                }
            }
    }

    public String getPassword()
    {
        return delegate.getProperty("mymailtool.password");
    }

    public Map<String, String> getMailProperties()
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for(String k: ALL_MAIL_PROPERTIES) 
        {
            String v = delegate.getProperty(k);
            if(v != null)
            {
                builder.put(k, v);
            }
        }
        return builder.build();    }

    public String getUser()
    {
        return delegate.getProperty(USER);
    }

    public Iterable<String> getFileLocations()
    {
        return fileLocations;
    }

    public Task getTask() throws Exception
    {
        ApplyMatchOperationsTask task = ApplyMatchOperationsTask.create();
        //want rules to operate in predictable order
        List<String> ruleNames = Lists.newArrayList(rulesTemp.keySet());
        Collections.sort(ruleNames);
        for(String name: ruleNames)
        {
            Map<String, String> entry = rulesTemp.get(name);
            String sourceFolder = entry.get("source");
            String type = entry.get("type");
            MessageOperation operation = null;
            Predicate<Message> matcher = null;
            if("split".equals(type))
            {
                operation = new SplitOperation();
            }
            if("move".equals(type) && entry.containsKey("dest"))
            {
                operation = new MoveOperation(entry.get("dest"));
            }
            if("delete".equals(type))
            {
                operation = new DeleteOperation();
            }
            
            List<Predicate<Message>> matchers = Lists.newArrayList();

            String test = entry.get("match");
            if(!Strings.isNullOrEmpty(test) && !"*".equals(test.trim()))
            {
                matchers.add(new FromAddressMatcher(test));
            }
            test = entry.get("from");
            if(!Strings.isNullOrEmpty(test) && !"*".equals(test.trim()))
            {
                matchers.add(new FromAddressMatcher(test));
            }
            
            test = entry.get("to");
            if(!Strings.isNullOrEmpty(test) && !"*".equals(test.trim()))
            {
                matchers.add(new ToAddressMatcher(test));
            }
            
            
            if(!matchers.isEmpty())
            {
                matcher = Predicates.and(matchers);
            }
            else
            {
                matcher = Predicates.alwaysTrue();
            }
            if(sourceFolder != null && matcher != null && operation != null)
            {
                task.addRule(sourceFolder, new MatchOperation(matcher, operation));
            }
        }
        
        return task;
    }

    public int getOperationLimit()
    {
        if(delegate.contains("operation.limit"))
        {
            String sValue = delegate.getProperty("operation.limit");
            try
            {
                return Integer.parseInt(sValue);
            }
            catch(NumberFormatException e)
            {
                // ignore, return default
            }
        }
        
        return PRIMITIVE_DEFAULT;
    }

    public String getMinAge()
    {
        return delegate.getProperty("mymailtool.minage");
    }

    public Iterable<FileConfigurationHandler> getFileHandlers()
    {
        return fileHandlers;
    }

    private void _addFileLocations(String filenames)
    {
        for(String fn: filenames.split(","))
        {
            fileLocations.add(fn.trim());
        }
    }

    private void _addFileHandlers(String classNames)
    {
        for(String cn: classNames.split(","))
        {
            FileConfigurationHandler handler = FileConfigurationHelper.getHandlerForClassName(cn.trim());
            if(handler != null)
            {
                fileHandlers.add(handler);
            }
        }
    }

    private static Pattern ruleMatch = Pattern.compile("(rule.*)\\.([a-z]+)");
    private void _addMessageRule(String key, String value)
    {
        Matcher m = ruleMatch.matcher(key);
        if(m.matches())
        {
            rulesTemp.get(m.group(1)).put(m.group(2), value);
        }
    }
    
}
