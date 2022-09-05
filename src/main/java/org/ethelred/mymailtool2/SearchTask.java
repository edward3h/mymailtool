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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.ethelred.mymailtool2.matcher.HasAttachmentMatcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * search in a folder and sub-folders
 */
public class SearchTask extends TaskBase
{
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
    private boolean printAttach = false;
    private File outputDirectory;

    public SearchTask(String folderName)
    {
        super();
        this.folderName = folderName;
    }

    @Override
    public void run()
    {
        if(this.matcher == null)
        {
            throw new IllegalStateException("No search spec provided to SearchTask");
        }

        try
        {
            traverseFolder(folderName, recursive, true);
        }
        catch(MessagingException | IOException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    protected void runMessage(Folder f, Message m) throws MessagingException, IOException
    {
        if(matcher.apply(m))
        {
            _printMatch(f, m);
        }
        else
        {
            _debugPrintNoMatch(f, m);
        }
    }

    @Override
    protected void status(Folder f)
    {
        System.err.println("Searching " + f + " with " + matcher);
    }

    private void _printMatch(Folder f, Message m) throws MessagingException, IOException
    {
        Address[] fromA = m.getFrom();
        System.out.printf(
                "MATCH %20.20s - %tY-%<tm-%<td - %20.20s : %s%n",
                f.getFullName(),
                m.getSentDate(),
                _printAddress(fromA),
                m.getSubject()
        );
        _printFlags(m);
        if(printAttach)
        {
            Multipart mm = (Multipart) m.getContent();
            for(int i = 0; i < mm.getCount(); i++)
            {
                BodyPart part = mm.getBodyPart(i);
                if(Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) && !Strings.isNullOrEmpty(part.getFileName()))
                {
                    System.out.println(Strings.repeat(" ", 27) + part.getFileName());
                    _tryDownload(part);
                }
            }
        }
    }

    private void _tryDownload(BodyPart part) throws MessagingException
    {
        if (outputDirectory != null)
        {
            File outputFile = new File(outputDirectory, part.getFileName());
            if (outputFile.exists())
            {
                System.out.println("File " + outputFile + " already exists, skipping.");
            }
            else
            {
                try (FileOutputStream out = new FileOutputStream(outputFile))
                {
                    part.getInputStream().transferTo(out);
                }
                catch (IOException e)
                {
                    Logger.getLogger(getClass().getName()).log(Level.WARNING, "Failed to download attachment", e);
                }
            }
        }
    }

    private void _printFlags(Message m) throws MessagingException
    {
        System.out.print(Strings.repeat(" ", 27));
        Flags ff = m.getFlags();
        for(Flags.Flag f: ff.getSystemFlags())
        {
            System.out.print(_flagToString(f));
            System.out.print(" ");
        }
        for(String f: ff.getUserFlags())
        {
            System.out.print(f);
            System.out.print(" ");
        }
        System.out.println();
    }

    private String _flagToString(Flags.Flag f)
    {
        return SYSTEM_FLAG_STRINGS.get(f);
    }

    private String _printAddress(Address[] from)
    {
        if(from == null || from.length < 1)
        {
            return "[unknown]";
        }
        Address f = from[0];
        if(f instanceof InternetAddress)
        {
            return ((InternetAddress) f).toUnicodeString();
        }
        return f.toString();
    }

    private void _debugPrintNoMatch(Folder f, Message m) throws MessagingException
    {
        /*Address[] fromA = m.getFrom();
        String from = fromA.length > 0 ? fromA[0].toString() : "[unknown]";
        System.out.printf(
                "%20.20s - %tY-%<tm-%<td - %20.20s : %s%n",
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
        if(this.matcher == null)
        {
            this.matcher = matcher;
        }
        else
        {
            this.matcher = Predicates.and(this.matcher, matcher);
        }

        if(matcher instanceof HasAttachmentMatcher)
        {
            printAttach = true;
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
