package org.ethelred.mymailtool2;

import com.google.common.collect.ImmutableList;
import org.ethelred.mymailtool2.propertiesfile.PropertiesFileConfigurationHandler;

import java.io.File;
import java.util.Collections;
import java.util.Map;

/**
 *
 * @author edward
 */
class DefaultConfiguration implements MailToolConfiguration
{
    private Iterable<String> defaultFileLocations = ImmutableList.of(new File(System.getProperty("user.home"), ".mymailtoolrc.properties").getAbsolutePath(), "/etc/mymailtoolrc.properties");

    private Iterable<FileConfigurationHandler> defaultFileHandlers
            = ImmutableList.of((FileConfigurationHandler) new PropertiesFileConfigurationHandler());
    
    @Override
    public String getPassword()
    {
        return null;
    }

    @Override
    public Map<String, String> getMailProperties()
    {
        return Collections.emptyMap();
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

}
