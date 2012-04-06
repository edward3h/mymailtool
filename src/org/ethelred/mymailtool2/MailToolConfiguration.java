package org.ethelred.mymailtool2;

import com.google.common.collect.ImmutableList;
import java.util.Map;

/**
 *
 * @author edward
 */
public interface MailToolConfiguration
{
    public final static String PROTOCOL = "mail.store.protocol";
    public final static String USER = "mail.user";
    public final static String HOST = "mail.host";
    public final static String PORT = "mail.port";
    public final static Iterable<String> ALL_MAIL_PROPERTIES = 
          ImmutableList.of(PROTOCOL, USER, HOST, PORT);  
    public final static int PRIMITIVE_DEFAULT = -1; // this is nasty :-)

    public String getPassword();

    public Map<String, String> getMailProperties();

    public String getUser();

    public Iterable<String> getFileLocations();

    public Task getTask() throws Exception;

    public int getOperationLimit();

    public String getMinAge();

    Iterable<FileConfigurationHandler> getFileHandlers();
    
}
