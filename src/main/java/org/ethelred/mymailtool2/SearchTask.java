package org.ethelred.mymailtool2;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMultipart;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.ethelred.mymailtool2.matcher.HasAttachmentMatcher;
import org.ethelred.mymailtool2.matcher.HasFlagMatcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * search in a folder and sub-folders
 */
public class SearchTask extends TaskBase
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<Flags.Flag, String> SYSTEM_FLAG_STRINGS = ImmutableMap.<Flags.Flag, String>builder()
            .put(Flags.Flag.ANSWERED, "ANSWERED")
            .put(Flags.Flag.DELETED, "DELETED")
            .put(Flags.Flag.DRAFT, "DRAFT")
            .put(Flags.Flag.FLAGGED, "FLAGGED")
            .put(Flags.Flag.RECENT, "RECENT")
            .put(Flags.Flag.SEEN, "SEEN")
            .put(Flags.Flag.USER, "USER").build();
    private final String folderName;
    private Predicate<Message> matcher;
    private boolean recursive = true;
    private boolean printAttach;
    private File outputDirectory;
    private boolean printFlags;

    public SearchTask(String folderName)
    {
        super();
        this.folderName = folderName;
    }

    @Override
    public void run()
    {
        if (this.matcher == null)
        {
            throw new IllegalStateException("No search spec provided to SearchTask");
        }

        try
        {
            traverseFolder(folderName, recursive, true);
        }
        catch (MessagingException | IOException e)
        {
            LOGGER.error("Exception", e);  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    protected void runMessage(Folder f, Message m) throws MessagingException, IOException
    {
        if (matcher.apply(m))
        {
            printMatch(f, m);
        }
        else
        {
            debugPrintNoMatch(f, m);
        }
    }

    @Override
    protected void status(Folder f)
    {
        LOGGER.error("Searching {} with {}", f, matcher);
    }

    private void printMatch(Folder f, Message m) throws MessagingException, IOException
    {
        Address[] fromA = m.getFrom();
        LOGGER.info(
                "MATCH {} - {} - {} : {}",
                f.getFullName(),
                m.getSentDate(),
                printAddress(fromA),
                m.getSubject()
        );
        if (printFlags) {
            printFlags(m);
        }
        if (printAttach)
        {
            Multipart mm = (Multipart) m.getContent();
            for (int i = 0; i < mm.getCount(); i++)
            {
                BodyPart part = mm.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) && !Strings.isNullOrEmpty(part.getFileName()))
                {
                    LOGGER.info("{}{}", Strings.repeat(" ", 27), part.getFileName());
                    tryDownload(part);
                }
            }
        }
    }

    private void tryDownload(BodyPart part) throws MessagingException
    {
        if (outputDirectory != null)
        {
            File outputFile = new File(outputDirectory, part.getFileName());
            if (outputFile.exists())
            {
                LOGGER.info("File {} already exists, skipping.", outputFile);
            }
            else
            {
                try (FileOutputStream out = new FileOutputStream(outputFile))
                {
                    part.getInputStream().transferTo(out);
                }
                catch (IOException e)
                {
                    LOGGER.warn("Failed to download attachment", e);
                }
            }
        }
    }

    private void printFlags(Message m) throws MessagingException
    {
        LOGGER.info(Strings.repeat(" ", 27));
        Flags ff = m.getFlags();
        for (Flags.Flag f : ff.getSystemFlags())
        {
            LOGGER.info(flagToString(f));
            LOGGER.info(" ");
        }
        for (String f : ff.getUserFlags())
        {
            LOGGER.info(f);
            LOGGER.info(" ");
        }
        System.out.println();
    }

    private String flagToString(Flags.Flag f)
    {
        return SYSTEM_FLAG_STRINGS.get(f);
    }

    private String printAddress(Address[] from)
    {
        if (from == null || from.length < 1)
        {
            return "[unknown]";
        }
        Address f = from[0];
        if (f instanceof InternetAddress)
        {
            return ((InternetAddress) f).toUnicodeString();
        }
        return f.toString();
    }

    private void debugPrintNoMatch(Folder f, Message m) throws MessagingException
    {
        /*Address[] fromA = m.getFrom();
        String from = fromA.length > 0 ? fromA[0].toString() : "[unknown]";
        LOGGER.info(
                "{} - {} - {} : {}",
                f.getFullName(),
                m.getSentDate(),
                from,
                m.getSubject()
        );*/
    }

    public static Task create(String folderName)
    {
        return new SearchTask(folderName);
    }

    public void addMatcher(Predicate<Message> matcher)
    {
        if (this.matcher == null)
        {
            this.matcher = matcher;
        }
        else
        {
            this.matcher = Predicates.and(this.matcher, matcher);
        }

        if (matcher instanceof HasAttachmentMatcher)
        {
            printAttach = true;
        }

        if (matcher instanceof HasFlagMatcher)
        {
            printFlags = true;
        }
    }

    public void setRecursive(boolean recursive)
    {
        this.recursive = recursive;
    }

    public void setDownloadAttachmentDirectory(File outputDirectory)
    {
        this.outputDirectory = outputDirectory;
    }
}
