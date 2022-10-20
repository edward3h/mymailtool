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
    String PROTOCOL = "mail.store.protocol";
    String USER = "mail.user";
    String HOST = "mail.host";
    String PORT = "mail.port";
    Pattern MAIL_PROPERTY_PATTERN = Pattern.compile("mail\\..+");
    Iterable<String> ALL_MAIL_PROPERTIES =
          ImmutableList.of(PROTOCOL, USER, HOST);
    int PRIMITIVE_DEFAULT = -1; // this is nasty :-)

    String getPassword();

    Map<String, String> getMailProperties();

    String getUser();

    Iterable<String> getFileLocations();

    Task getTask() throws Exception;

    int getOperationLimit();

    String getMinAge();

    Iterable<FileConfigurationHandler> getFileHandlers();

    String getTimeLimit();

    boolean verbose();

    int getChunkSize();

    boolean randomTraversal();
}
