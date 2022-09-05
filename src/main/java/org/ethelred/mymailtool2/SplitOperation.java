package org.ethelred.mymailtool2;

import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;

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

    @Override
    public boolean apply(MailToolContext context, Message m)
    {
        try
        {
            LocalDate received = new LocalDate(m.getReceivedDate());
            Folder startingFolder = m.getFolder();
            Folder moveTo = context.getFolder(getSubFolderName(startingFolder, received));
            startingFolder.copyMessages(new Message[]{m}, moveTo);
            m.setFlag(Flags.Flag.DELETED, true);
            MailUtil.log("Move message %s from %s to %s", MailUtil.toString(m), startingFolder.getFullName(), moveTo.getFullName());
            return true;
        }
        catch(MessagingException e)
        {
            Logger.getLogger(SplitOperation.class.getName()).log(Level.SEVERE, "Error in Split", e);
        }
        return false;
    }

    @Override
    public boolean finishApplying()
    {
        return true;
    }

    @Override
    public String toString()
    {
        return "Split";
    }

    private String getSubFolderName(Folder folder, LocalDate received) throws MessagingException
    {
        char sep = folder.getSeparator();
        String year = String.valueOf(received.getYear());
        String month = monthPart.print(received);
        return folder.getFullName() + sep + year + sep + month;
    }
}
