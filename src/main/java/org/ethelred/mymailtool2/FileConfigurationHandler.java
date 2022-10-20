package org.ethelred.mymailtool2;

import java.io.File;

/**
 *
 * @author edward
 */
public interface FileConfigurationHandler
{

    Iterable<String> getExtensions();

    MailToolConfiguration readConfiguration(File f);
    
}
