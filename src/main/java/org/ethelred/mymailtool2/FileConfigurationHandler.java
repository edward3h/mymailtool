package org.ethelred.mymailtool2;

import java.io.File;

/**
 *
 * @author edward
 */
public interface FileConfigurationHandler
{

    public Iterable<String> getExtensions();

    public MailToolConfiguration readConfiguration(File f);
    
}
