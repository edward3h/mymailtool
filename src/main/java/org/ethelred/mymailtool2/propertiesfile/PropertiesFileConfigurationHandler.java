package org.ethelred.mymailtool2.propertiesfile;

import com.google.common.collect.ImmutableList;
import java.io.File;
import org.ethelred.mymailtool2.BaseFileConfigurationHandler;
import org.ethelred.mymailtool2.MailToolConfiguration;

/**
 *
 * @author edward
 */
public class PropertiesFileConfigurationHandler extends BaseFileConfigurationHandler
{

    @Override
    public Iterable<String> getExtensions()
    {
        return ImmutableList.of("properties");
    }

    @Override
    public MailToolConfiguration readConfiguration(File f)
    {
        try
        {
            return new PropertiesFileConfiguration(f);
        }
        catch (Exception e)
        {
            return null;
        }
    }
    
}
