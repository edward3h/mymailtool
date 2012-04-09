package org.ethelred.mymailtool2;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

/**
 *
 * @author edward
 */
public class SplitOperation implements MessageOperation
{

    private final static DateTimeFormatter monthPart = new DateTimeFormatterBuilder().appendMonthOfYear(2).appendLiteral('-').appendMonthOfYearShortText().appendLiteral(
            '-').appendYear(4, 4).toFormatter();

    public boolean apply(MailToolContext context, Message m)
    {
        try
        {
            LocalDate received = new LocalDate(m.getReceivedDate());
            Folder startingFolder = m.getFolder();
            Folder moveTo = context.getFolder(getSubFolderName(startingFolder, received));
            startingFolder.copyMessages(new Message[]{m}, moveTo);
            m.setFlag(Flags.Flag.DELETED, true);
            return true;
        }
        catch(MessagingException e)
        {
            Logger.getLogger(SplitOperation.class.getName()).log(Level.SEVERE, "Error in Split", e);
        }
        return false;
    }

    private String getSubFolderName(Folder folder, LocalDate received) throws MessagingException
    {
        char sep = folder.getSeparator();
        String year = String.valueOf(received.getYear());
        String month = monthPart.print(received);
        return folder.getFullName() + sep + year + sep + month;
    }
}
