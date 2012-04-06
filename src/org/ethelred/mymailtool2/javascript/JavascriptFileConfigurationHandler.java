package org.ethelred.mymailtool2.javascript;

import org.ethelred.mymailtool2.propertiesfile.*;
import com.google.common.collect.ImmutableList;
import java.io.File;
import org.ethelred.mymailtool2.BaseFileConfigurationHandler;
import org.ethelred.mymailtool2.MailToolConfiguration;

/**
 *
 * @author edward
 */
public class JavascriptFileConfigurationHandler extends BaseFileConfigurationHandler
{

    public Iterable<String> getExtensions()
    {
        return ImmutableList.of("js", "javascript");
    }

    public MailToolConfiguration readConfiguration(File f)
    {
        try
        {
            return new JavascriptFileConfiguration(f);
        }
        catch(Exception e)
        {
            return null;
        }
    }
    
}
