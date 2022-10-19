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

    @Override
    public String getPassword()
    {
        return null;
    }

    @Override
    public Map<String, String> getMailProperties()
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for (String k : System.getProperties().stringPropertyNames())
        {
            if (MAIL_PROPERTY_PATTERN.matcher(k).matches())
            {
                String v = System.getProperty(k);
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
        return System.getProperty(USER);
    }

    @Override
    public Iterable<String> getFileLocations()
    {
        return Collections.emptyList();
    }

    @Override
    public Task getTask() throws Exception
    {
        return null;
    }

    @Override
    public int getOperationLimit()
    {
        return PRIMITIVE_DEFAULT;
    }

    @Override
    public String getMinAge()
    {
        return null;
    }

    @Override
    public Iterable<FileConfigurationHandler> getFileHandlers()
    {
        return Collections.emptyList();
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
    public String toString() {
        return "SystemPropertiesConfiguration";
    }
}
