package org.ethelred.mymailtool2.javascript;

import java.io.File;

import com.google.common.collect.ImmutableList;
import org.ethelred.mymailtool2.BaseFileConfigurationHandler;
import org.ethelred.mymailtool2.MailToolConfiguration;

/**
 *
 * @author edward
 */
public class JavascriptFileConfigurationHandler extends BaseFileConfigurationHandler
{

    @Override
    public Iterable<String> getExtensions()
    {
        return ImmutableList.of("js", "javascript");
    }

    @Override
    public MailToolConfiguration readConfiguration(File f)
    {
        try
        {
            return new JavascriptFileConfiguration(f);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
}
