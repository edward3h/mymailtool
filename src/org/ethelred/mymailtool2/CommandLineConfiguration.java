package org.ethelred.mymailtool2;

import java.util.Map;

/**
 *
 * @author edward
 */
class CommandLineConfiguration implements MailToolConfiguration
{

    boolean isShowUsage()
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public String getPassword()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Map<String, String> getMailProperties()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getUser()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Iterable<String> getFileLocations()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Task getTask()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
