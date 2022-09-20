package org.ethelred.mymailtool2;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

/**
 *
 * @author edward
 */
public class SplitOperation implements MessageOperation
{

    private static final Logger LOGGER = LogManager.getLogger();

    private static final DateTimeFormatter monthPart = new DateTimeFormatterBuilder().appendMonthOfYear(2).appendLiteral('-').appendMonthOfYearShortText().appendLiteral(
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
            LOGGER.info("Move message {} from {} to {}", MailUtil.supplyString(m), startingFolder.getFullName(), moveTo.getFullName());
            return true;
        }
        catch (MessagingException e)
        {
            LOGGER.error("Error in Split", e);
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
