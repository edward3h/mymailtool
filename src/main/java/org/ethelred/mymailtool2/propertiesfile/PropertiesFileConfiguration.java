package org.ethelred.mymailtool2.propertiesfile;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.mail.Message;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethelred.mymailtool2.*;
import org.ethelred.mymailtool2.matcher.AgeMatcher;
import org.ethelred.mymailtool2.matcher.FromAddressMatcher;
import org.ethelred.mymailtool2.matcher.HasAttachmentMatcher;
import org.ethelred.mymailtool2.matcher.HasFlagMatcher;
import org.ethelred.mymailtool2.matcher.SubjectMatcher;
import org.ethelred.mymailtool2.matcher.ToAddressMatcher;
import org.ethelred.util.MapWithDefault;

/**
 *
 * @author edward
 */
class PropertiesFileConfiguration implements MailToolConfiguration
{
    private static final Logger LOGGER = LogManager.getLogger();
    private Properties delegate;
    private List<String> fileLocations = Lists.newArrayList();
    private List<FileConfigurationHandler> fileHandlers = Lists.newArrayList();
    private Map<String, Map<String, String>> rulesTemp = MapWithDefault.wrap(new HashMap<>(), () -> Maps.newHashMap());
    
    public PropertiesFileConfiguration(File f) throws IOException
    {
        init(f);
    }

    private void init(File f) throws IOException
    {
            delegate = new Properties();
            delegate.load(new FileReader(f));
            
            for (String k : delegate.stringPropertyNames())
            {
                if (k.startsWith("include") || k.startsWith("load"))
                {
                    addFileLocations(delegate.getProperty(k));
                }
                if (k.startsWith("handler"))
                {
                    addFileHandlers(delegate.getProperty(k));
                }
                if (k.startsWith("rule"))
                {
                    addMessageRule(k, delegate.getProperty(k));
                }
            }
    }

    @Override
    public String getPassword()
    {
        return delegate.getProperty("mymailtool.password");
    }

    @Override
    public Map<String, String> getMailProperties()
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for (String k : delegate.stringPropertyNames())
        {
            if (MAIL_PROPERTY_PATTERN.matcher(k).matches())
            {
                String v = delegate.getProperty(k);
                if (v != null)
                {
                    builder.put(k, v);
                }
            }
        }
        return builder.build();
    }

    @Override
    public String getUser()
    {
        return delegate.getProperty(USER);
    }

    @Override
    public Iterable<String> getFileLocations()
    {
        return fileLocations;
    }

    @Override
    public int getChunkSize()
    {
        return PRIMITIVE_DEFAULT;
    }

    @Override
    public boolean randomTraversal() {
        return false;
    }

    @Override
    public Task getTask() throws Exception
    {
        ApplyMatchOperationsTask task = ApplyMatchOperationsTask.create();
        //want rules to operate in predictable order
        List<String> ruleNames = Lists.newArrayList(rulesTemp.keySet());
        Collections.sort(ruleNames);
        for (String name : ruleNames)
        {
            Map<String, String> entry = rulesTemp.get(name);
            String sourceFolder = entry.get("source");
            String type = entry.get("type");
            MessageOperation operation = null;

            Predicate<Message> matcher;
            if ("split".equals(type))
            {
                operation = new SplitOperation();
            }
            if ("move".equals(type) && entry.containsKey("dest"))
            {
                operation = new MoveOperation(entry.get("dest"));
            }
            if ("delete".equals(type))
            {
                operation = new DeleteOperation();
            }
            
            List<Predicate<Message>> matchers = Lists.newArrayList();

            String test = entry.get("match");
            if (!Strings.isNullOrEmpty(test) && !"*".equals(test.trim()))
            {
                matchers.add(new FromAddressMatcher(true, first(test), rest(test)));
            }
            test = entry.get("from");
            if (!Strings.isNullOrEmpty(test) && !"*".equals(test.trim()))
            {
                matchers.add(new FromAddressMatcher(true,  first(test), rest(test)));
            }
            
            test = entry.get("to");
            if (!Strings.isNullOrEmpty(test) && !"*".equals(test.trim()))
            {
                matchers.add(new ToAddressMatcher(true,  first(test), rest(test)));
            }

            test = entry.get("subject");
            if (!Strings.isNullOrEmpty(test) && !"*".equals(test.trim()))
            {
                matchers.add(new SubjectMatcher(test));
            }

            test = entry.get("attachment");
            if (!Strings.isNullOrEmpty(test))
            {
                matchers.add(new HasAttachmentMatcher(test));
            }

            test = entry.get("flag");
            if (!Strings.isNullOrEmpty(test))
            {
                matchers.add(new HasFlagMatcher(test));
            }

            test = entry.get("newer");
            if (!Strings.isNullOrEmpty(test))
            {
                matchers.add(new AgeMatcher(test, false, task));
            }

            test = entry.get("older");
            if (!Strings.isNullOrEmpty(test))
            {
                matchers.add(new AgeMatcher(test, true, task));
            }

            boolean includeSubFolders = false;
            test = entry.get("includeSubFolders");
            if ("true".equalsIgnoreCase(test))
            {
                includeSubFolders = true;
            }
            
            
            if (!matchers.isEmpty())
            {
                matcher = Predicates.and(matchers);
            }
            else
            {
                matcher = Predicates.alwaysTrue();
            }
            if (sourceFolder != null && matcher != null && operation != null)
            {
                task.addRule(sourceFolder, matcher, matchers, operation, includeSubFolders);
                LOGGER.info("Adding rule {} (folder {} matcher {} operation {})", name, sourceFolder, matcher, operation);
            }
            else
            {
                LOGGER.info("Skipping rule {} (folder {} matcher {} operation {})", name, sourceFolder, matcher, operation);
            }
        }
        
        return task.hasRules() ? task : null;
    }

    private String first(String test)
    {
        String[] parts = test.split("\\,\\s*");
        if (parts.length > 0)
        {
            return parts[0];
        }
        throw new IllegalArgumentException("Expected a match with at least one value");
    }

    private String[] rest(String test)
    {
        String[] parts = test.split("\\,\\s*");
        if (parts.length < 2)
        {
            return new String[0];
        }
        return Arrays.copyOfRange(parts, 1, parts.length);
    }


    @Override
    public int getOperationLimit()
    {
        if (delegate.containsKey("operation.limit"))
        {
            String sValue = delegate.getProperty("operation.limit");
            try
            {
                return Integer.parseInt(sValue);
            }
            catch (NumberFormatException e)
            {
                // ignore, return default
            }
        }
        
        return PRIMITIVE_DEFAULT;
    }

    @Override
    public String getMinAge()
    {
        return delegate.getProperty("mymailtool.minage");
    }

    @Override
    public Iterable<FileConfigurationHandler> getFileHandlers()
    {
        return fileHandlers;
    }

    @Override
    public String getTimeLimit()
    {
        return delegate.getProperty("runtime.limit");
    }

    @Override
    public boolean verbose()
    {
        return false;
    }

    private void addFileLocations(String filenames)
    {
        for (String fn : filenames.split(","))
        {
            fileLocations.add(fn.trim());
        }
    }

    private void addFileHandlers(String classNames)
    {
        for (String cn : classNames.split(","))
        {
            FileConfigurationHandler handler = FileConfigurationHelper.getHandlerForClassName(cn.trim());
            if (handler != null)
            {
                fileHandlers.add(handler);
            }
        }
    }

    private static Pattern ruleMatch = Pattern.compile("(rule\\..*)\\.([a-zA-Z]+)");
    private void addMessageRule(String key, String value)
    {
        Matcher m = ruleMatch.matcher(key);
        if (m.matches())
        {
            rulesTemp.get(m.group(1)).put(m.group(2), value);
        }
    }
    
}
