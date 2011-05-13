package org.ethelred.mymailtool2;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.Map;

/**
 *
 * @author edward
 */
class DefaultConfiguration implements MailToolConfiguration
{
    private Iterable<String> defaultFileLocations = ImmutableList.of(new File(System.getProperty("user.home"), ".mymailtoolrc").getAbsolutePath(), "/etc/mymailtoolrc");
    
    public String getPassword()
    {
        return null;
    }

    public Map<String, String> getMailProperties()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getUser()
    {
        return null;
    }

    public Iterable<String> getFileLocations()
    {
        return defaultFileLocations;
    }

    public Task getTask()
    {
        return ApplyMatchOperationsTask.create();
    }

    public int getOperationLimit()
    {
        return 100;
    }

    public String getMinAge()
    {
        return "1 month";
    }
    
}
