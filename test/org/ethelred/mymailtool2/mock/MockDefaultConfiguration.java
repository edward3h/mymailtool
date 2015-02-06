package org.ethelred.mymailtool2.mock;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.ethelred.mymailtool2.ApplyMatchOperationsTask;
import org.ethelred.mymailtool2.FileConfigurationHandler;
import org.ethelred.mymailtool2.MailToolConfiguration;
import org.ethelred.mymailtool2.Task;
import org.ethelred.mymailtool2.javascript.JavascriptFileConfigurationHandler;
import org.ethelred.mymailtool2.propertiesfile.PropertiesFileConfigurationHandler;

/**
 *
 * @author edward
 */
public class MockDefaultConfiguration implements MailToolConfiguration
{
    private List<String> defaultFileLocations = Lists.newArrayList();

    private List<FileConfigurationHandler> defaultFileHandlers = Lists.newArrayList();
    
    @Override
    public String getPassword()
    {
        return null;
    }

    @Override
    public Map<String, String> getMailProperties()
    {
        return ImmutableMap.of("mail.store.protocol", "imap",
                "mail.debug", "true");

    }

    @Override
    public String getUser()
    {
        return null;
    }

    @Override
    public Iterable<String> getFileLocations()
    {
        return defaultFileLocations;
    }

    @Override
    public Task getTask()
    {
        return ApplyMatchOperationsTask.create();
    }

    @Override
    public int getOperationLimit()
    {
        return 100;
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
    public String getMinAge()
    {
        return "1 month";
    }

    @Override
    public Iterable<FileConfigurationHandler> getFileHandlers()
    {
        return defaultFileHandlers;
    }

    @Override
    public String getTimeLimit()
    {
        return null;
    }

    @Override
    public boolean verbose()
    {
        return false;
    }

    public void addFileHandler(FileConfigurationHandler fileConfigurationHandler)
    {
        defaultFileHandlers.add(fileConfigurationHandler);
    }

    public void addFile(String file)
    {
        defaultFileLocations.add(file);
    }
}
