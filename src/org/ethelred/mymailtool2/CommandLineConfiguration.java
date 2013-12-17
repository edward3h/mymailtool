package org.ethelred.mymailtool2;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.ethelred.mymailtool2.matcher.FromAddressMatcher;
import org.ethelred.mymailtool2.matcher.SubjectMatcher;
import org.ethelred.mymailtool2.matcher.ToAddressMatcher;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

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

    private String runTimeLimit;
    
    
    @Option(name = "--min-age", usage = "Minimum age of mail to process", aliases = {"-t"})
    private String minAge;
    
    private Map<String, String> mailProperties = Maps.newHashMap();
    
    private List<String> fileLocations = Lists.newArrayList();

    private List<FileConfigurationHandler> fileHandlers = Lists.newArrayList();
    
    private Task task;

    @Override
    public Iterable<FileConfigurationHandler> getFileHandlers()
    {
        return fileHandlers;
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

    @Option(name = "--to", usage = "Search messages matching To address")
    private void searchTo(String searchSpec)
    {
        if(task instanceof SearchTask)
        {
            ((SearchTask) task).addMatcher(new ToAddressMatcher(false, searchSpec));
        }
    }

    @Option(name = "--from", usage = "Search messages matching From address")
    private void searchFrom(String searchSpec)
    {
        if(task instanceof SearchTask)
        {
            ((SearchTask) task).addMatcher(new FromAddressMatcher(false, searchSpec));
        }
    }

    @Option(name = "--subject", usage = "Search messages matching Subject")
    private void searchSubject(String searchSpec)
    {
        if(task instanceof SearchTask)
        {
            ((SearchTask) task).addMatcher(new SubjectMatcher(searchSpec));
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
