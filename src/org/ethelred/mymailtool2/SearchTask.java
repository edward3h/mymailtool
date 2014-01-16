package org.ethelred.mymailtool2;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMultipart;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import org.ethelred.mymailtool2.matcher.HasAttachmentMatcher;

import java.io.IOException;

/**
 * search in a folder and sub-folders
 */
public class SearchTask extends TaskBase
{
    private final String folderName;
    private Predicate<Message> matcher;
    private boolean recursive = true;
    private boolean printAttach = false;

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
            traverseFolder(folderName, recursive);
        }
        catch(MessagingException | IOException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    protected void runMessage(Folder f, Message m, boolean includeSubFolders, String originalName) throws MessagingException, IOException
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
    protected void status(Folder f, String originalName)
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
        //_printFlags(m);
        if(printAttach)
        {
            Multipart mm = (Multipart) m.getContent();
            for(int i = 0; i < mm.getCount(); i++)
            {
                BodyPart part = mm.getBodyPart(i);
                if(Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) && !Strings.isNullOrEmpty(part.getFileName()))
                {
                    System.out.println(Strings.repeat(" ", 27) + part.getFileName());
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
            System.out.print(f);
            System.out.print(" ");
        }
        for(String f: ff.getUserFlags())
        {
            System.out.print(f);
            System.out.print(" ");
        }
        System.out.println();
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
}
