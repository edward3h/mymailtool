package org.ethelred.mymailtool2;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 *
 * @author edward
 */
public class SplitOperation implements MessageOperation
{

    private static final Logger LOGGER = LogManager.getLogger();

    private static final DateTimeFormatter monthPart = DateTimeFormatter.ofPattern("MM-MMM-yyyy", Locale.ENGLISH);

    @Override
    public boolean apply(MailToolContext context, Message m)
    {
        try
        {
            LocalDate received = m.getReceivedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            Folder startingFolder = m.getFolder();
            Folder moveTo = context.getFolder(getSubFolderName(startingFolder, received));
            startingFolder.copyMessages(new Message[]{m}, moveTo);
            m.setFlag(Flags.Flag.DELETED, true);
            LOGGER.info("Move message {} from {} to {}", MailUtil.supplyString(m), startingFolder::getFullName, moveTo::getFullName);
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
        String month = monthPart.format(received);
        return folder.getFullName() + sep + year + sep + month;
    }
}
