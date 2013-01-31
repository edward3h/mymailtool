package org.ethelred.mymailtool2;

import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.ethelred.mymailtool2.matcher.ToAddressMatcher;

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
            Folder f = context.getFolder(folderName);
            if(f == null || !f.exists())
            {
                throw new IllegalStateException("Could not open folder " + folderName);
            }

            _searchFolder(f);
        }
        catch(MessagingException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void _searchFolder(Folder f) throws MessagingException
    {
        System.err.println("Searching " + f + " with " + matcher);

        if((f.getType() & Folder.HOLDS_MESSAGES) > 0)
        {
            f.open(Folder.READ_ONLY);
            for(Message m: _readMessages(f))
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
        }

        if((f.getType() & Folder.HOLDS_FOLDERS) > 0)
        {
            for(Folder child: f.list())
            {
                _searchFolder(child);
            }
        }
    }

    private Iterable<? extends Message> _readMessages(Folder f)
    {
        return new RecentMessageIterable(f);
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
        );  */
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
