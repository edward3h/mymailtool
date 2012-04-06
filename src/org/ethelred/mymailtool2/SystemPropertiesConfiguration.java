package org.ethelred.mymailtool2;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Map;

/**
 *
 * @author edward
 */
class SystemPropertiesConfiguration implements MailToolConfiguration
{

    public String getPassword()
    {
        return null;
    }

    public Map<String, String> getMailProperties()
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for(String k: ALL_MAIL_PROPERTIES) 
        {
            String v = System.getProperty(k);
            if(v != null)
            {
                builder.put(k, v);
            }
        }
        return builder.build();
    }

    public String getUser()
    {
        return System.getProperty(USER);
    }

    public Iterable<String> getFileLocations()
    {
        return Collections.emptyList();
    }

    public Task getTask() throws Exception
    {
        return null;
    }

    public int getOperationLimit()
    {
        return PRIMITIVE_DEFAULT;
    }

    public String getMinAge()
    {
        return null;
    }

    public Iterable<FileConfigurationHandler> getFileHandlers()
    {
        return Collections.emptyList();
    }
    
}
