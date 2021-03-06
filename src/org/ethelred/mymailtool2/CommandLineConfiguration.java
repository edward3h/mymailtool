package org.ethelred.mymailtool2;

import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.ethelred.mymailtool2.matcher.AgeMatcher;
import org.ethelred.mymailtool2.matcher.FromAddressMatcher;
import org.ethelred.mymailtool2.matcher.HasAttachmentMatcher;
import org.ethelred.mymailtool2.matcher.HasFlagMatcher;
import org.ethelred.mymailtool2.matcher.SubjectMatcher;
import org.ethelred.mymailtool2.matcher.ToAddressMatcher;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

import javax.mail.Message;

/**
 *
 * @author edward
 */
class CommandLineConfiguration implements MailToolConfiguration
{
    
    private String user;
    
    @Option(name = "--help", usage = "Show help.", aliases = {"-h", "-?"})
    private boolean showUsage = false;

    @Option(name = "--limit", usage = "Operation limit count", aliases = {"-O"})
    private int limit = PRIMITIVE_DEFAULT;

    @Option(name = "--time-limit", usage = "Time limit for running app")
    private String runTimeLimit;
    
    
    @Option(name = "--min-age", usage = "Minimum age of mail to process", aliases = {"-t"})
    private String minAge;
    
    private Map<String, String> mailProperties = Maps.newHashMap();
    
    private List<String> fileLocations = Lists.newArrayList();

    private List<FileConfigurationHandler> fileHandlers = Lists.newArrayList();
    
    private Task task;

    private boolean invertNextMatcher;

    @Option(name = "--verbose", usage = "Verbose (debugging) output", aliases = {"-v"})
    private boolean verbose = false;

    @Option(name = "--chunk", usage = "How many messages to grab in a batch")
    private int chunkSize = PRIMITIVE_DEFAULT;

    @Option(name = "--random", usage = "Traverse folders in random order")
    private boolean random = false;

    @Override
    public int getChunkSize()
    {
        return chunkSize;
    }

    @Override
    public boolean randomTraversal() {
        return random;
    }

    @Override
    public Iterable<FileConfigurationHandler> getFileHandlers()
    {
        return fileHandlers;
    }

    @Override
    public boolean verbose()
    {
        return verbose;
    }

    @Option(name = "--config", usage = "Specify config file. (default $HOME/.mymailtoolrc)", aliases = {"-c"})
    private void setConfigFile(String fileName) {
        fileLocations.add(fileName);
    }
    
    @Option(name = "--handler", usage = "Specify a file handler class to load alternate config file language")
    private void setFileHandlerClass(String className)
    {
        FileConfigurationHandler handler = FileConfigurationHelper.getHandlerForClassName(className);
        if(handler != null)
        {
            fileHandlers.add(handler);
        }
    }
    
    @Option(name = "--list", usage = "List folders.", aliases = {"-l"})
    private void taskListFolders(boolean fake) throws CmdLineException
    {
        task = ListFoldersTask.create();
    }

    @Option(name = "--fromCount", usage = "Count occurrences of from addresses", aliases = {"-f"})
    private void taskFromCount(String folderName) throws CmdLineException
    {
        throw new CmdLineException("Task FromCount is not supported yet");
    }

        @Option(name = "--toCount", usage = "Count occurrences of to (and CC) addresses", aliases = {"-o"})
    private void taskToCount(String folderName) throws CmdLineException
        {
            throw new CmdLineException("Task ToCount is not supported yet");
    }

    @Option(name = "--apply", usage = "Apply rules - define rules in config file", aliases = {"-a"})
    private void taskApplyRules(String folderName) throws CmdLineException
    {
        throw new CmdLineException("Task Apply is not supported from command line, please use config file");
    }

    @Option(name = "--search", usage = "Search for matching messages", aliases = {"-s"})
    private void taskSearch(String folderName) {
        task = SearchTask.create(folderName);
    }

    @Option(name = "--not", usage = "Invert the next matcher")
    private void setInvertNextMatcher(boolean set)
    {
        invertNextMatcher = true;
    }

    @Option(name = "--to", usage = "Search messages matching To address")
    private void searchTo(String searchSpec)
    {
        if(task instanceof SearchTask)
        {
            ((SearchTask) task).addMatcher(nextMatcher(new ToAddressMatcher(false, searchSpec)));
        }
    }

    private Predicate<Message> nextMatcher(Predicate<Message> matcher)
    {
        if(invertNextMatcher)
        {
            invertNextMatcher = false;
            return Predicates.not(matcher);
        }
        else
        {
            return matcher;
        }
    }

    @Option(name = "--from", usage = "Search messages matching From address")
    private void searchFrom(String searchSpec)
    {
        if(task instanceof SearchTask)
        {
            ((SearchTask) task).addMatcher(nextMatcher(new FromAddressMatcher(false, searchSpec)));
        }
    }

    @Option(name = "--attach", usage = "Search messages which have an attachment with filename matching this pattern")
    private void searchAttachment(String pattern)
    {
        if(task instanceof SearchTask)
        {
            ((SearchTask) task).addMatcher(nextMatcher(new HasAttachmentMatcher(pattern)));
        }
    }

    @Option(name = "--flag", usage = "Search messages which have a flag with this name")
    private void searchFlag(String pattern)
    {
        if(task instanceof SearchTask)
        {
            ((SearchTask) task).addMatcher(nextMatcher(new HasFlagMatcher(pattern)));
        }
    }

    @Option(name = "--non-recursive", usage = "Search defaults to reading sub-folders - this stops it")
    private void searchNonRecursive(boolean set)
    {
        if(task instanceof SearchTask)
        {
            ((SearchTask) task).setRecursive(false);
        }
    }

    @Option(name = "--subject", usage = "Search messages matching Subject")
    private void searchSubject(String searchSpec)
    {
        if(task instanceof SearchTask)
        {
            ((SearchTask) task).addMatcher(nextMatcher(new SubjectMatcher(searchSpec)));
        }
    }

    @Option(name = "--newer", usage = "Search messages newer than age")
    private void searchNewer(String searchSpec)
    {
        if(task instanceof SearchTask)
        {
            ((SearchTask) task).addMatcher(nextMatcher(new AgeMatcher(searchSpec, false, task)));
        }
    }

    @Option(name = "--older", usage = "Search messages older than age")
    private void searchOlder(String searchSpec)
    {
        if(task instanceof SearchTask)
        {
            ((SearchTask) task).addMatcher(nextMatcher(new AgeMatcher(searchSpec, true, task)));
        }
    }

    @Option(name = "--host", usage = "mail hostname", aliases = {"-H"})
    private void setHost(String hostname) {
        mailProperties.put(HOST, hostname);
    }

    @Option(name = "--port", usage = "mail port. Default 143", aliases = {"-p"})
    private void setPort(int port) {
        mailProperties.put(PORT, String.valueOf(port));
    }

    @Option(name = "--user", usage = "mail user", aliases = {"-u"})
    private void setUser(String username) {
        mailProperties.put(USER, username);
        user = username;
    }
    
    boolean isShowUsage()
    {
        return showUsage;
    }

    @Override
    public String getPassword()
    {
        return null;
    }

    @Override
    public Map<String, String> getMailProperties()
    {
        return mailProperties;
    }

    @Override
    public String getUser()
    {
        return user;
    }

    @Override
    public Iterable<String> getFileLocations()
    {
        return fileLocations;
    }

    @Override
    public Task getTask() throws CmdLineException
    {
        return task;
    }

    @Override
    public int getOperationLimit()
    {
        return limit;
    }

    @Override
    public String getMinAge()
    {
        return minAge;
    }

    @Override
    public String getTimeLimit()
    {
        return runTimeLimit;
    }

}
