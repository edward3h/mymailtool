package org.ethelred.mymailtool;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

/**
 *
 * @author eharman
 */
class DateSplitTask extends MessageOpTask {

    DateTimeFormatter monthPart = new DateTimeFormatterBuilder().appendMonthOfYear(2).appendLiteral('-').appendMonthOfYearShortText().appendLiteral(
            '-').appendYear(4, 4).toFormatter();
    protected DateSplitTask(Properties p)
    {
        super(p);
    }


    @Override
    protected String getPrefix()
    {
        return "split.";
    }

    @Override
    protected boolean processMessage(Message m, Store store, Folder folder)
    {
        try
        {
            LocalDate received = new LocalDate(m.getReceivedDate());
            String folderName = getSubFolderName(folder, received);
            Folder moveTo = getFolder(store, folderName);
            folder.copyMessages(new Message[]{m}, moveTo);
            System.out.printf("Moving message %s to %s%n", m, moveTo);
            m.setFlag(Flag.DELETED, true);
            return true;
        }
        catch(MessagingException ex)
        {
            Logger.getLogger(DateSplitTask.class.getName()).log(Level.SEVERE, null, ex);
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
