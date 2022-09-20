package org.ethelred.mymailtool2;

import com.google.common.collect.ImmutableList;
import java.util.Map;
import java.util.regex.Pattern;

/**
 *
 * @author edward
 */
public interface MailToolConfiguration
{
    public static final String PROTOCOL = "mail.store.protocol";
    public static final String USER = "mail.user";
    public static final String HOST = "mail.host";
    public static final String PORT = "mail.port";
    public static final Pattern MAIL_PROPERTY_PATTERN = Pattern.compile("mail\\..+");
    public static final Iterable<String> ALL_MAIL_PROPERTIES = 
          ImmutableList.of(PROTOCOL, USER, HOST);
    public static final int PRIMITIVE_DEFAULT = -1; // this is nasty :-)

    public String getPassword();

    public Map<String, String> getMailProperties();

    public String getUser();

    public Iterable<String> getFileLocations();

    public Task getTask() throws Exception;

    public int getOperationLimit();

    public String getMinAge();

    Iterable<FileConfigurationHandler> getFileHandlers();

    public String getTimeLimit();

    boolean verbose();

    int getChunkSize();

    boolean randomTraversal();
}
