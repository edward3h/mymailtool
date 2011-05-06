package org.ethelred.mymailtool2;

import java.util.Map;

/**
 *
 * @author edward
 */
public interface MailToolConfiguration
{

    public String getPassword();

    public Map<String, String> getMailProperties();

    public String getUser();

    public Iterable<String> getFileLocations();

    public Task getTask();
    
}
