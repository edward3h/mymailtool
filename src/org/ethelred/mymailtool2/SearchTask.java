package org.ethelred.mymailtool2;

import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * search in a folder and sub-folders
 */
public class SearchTask extends TaskBase
{
    private final String folderName;
    private Predicate<Message> matcher;

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
            traverseFolder(folderName, true);
        }
        catch(MessagingException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    protected void runMessage(Folder f, Message m, boolean includeSubFolders, String originalName) throws MessagingException
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

    private void _printMatch(Folder f, Message m) throws MessagingException
    {
        Address[] fromA = m.getFrom();
        String from = fromA.length > 0 ? fromA[0].toString() : "[unknown]";
        System.out.printf(
            "%n MATCH %20.20s - %tY-%<tm-%<td - %20.20s : %s%n%n",
                f.getFullName(),
                m.getSentDate(),
                from,
                m.getSubject()
        );
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
    }
}
