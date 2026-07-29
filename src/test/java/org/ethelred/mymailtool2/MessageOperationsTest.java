package org.ethelred.mymailtool2;

import java.util.Calendar;
import java.util.Date;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;

import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * test message operations
 */
@ExtendWith(MockitoExtension.class)
public class MessageOperationsTest
{
    @Mock Message msg;
    @Mock Folder startingFolder;
    @Mock MailToolContext mailContext;
    private Date sentDate;

    @BeforeEach
    public void setup() throws MessagingException
    {
        Calendar c = Calendar.getInstance();
        c.set(2012, Calendar.APRIL, 17);
        sentDate = c.getTime();

        lenient().when(msg.getSentDate()).thenReturn(sentDate);
        lenient().when(msg.getSubject()).thenReturn("test subject");
    }

    @Test
    public void testDelete()
    {
        try
        {
            MessageOperation del = new DeleteOperation();
            assertThat(del.apply(mailContext, msg)).isTrue();
            verify(msg).setFlag(Flags.Flag.DELETED, true);
        }
        catch (MessagingException e)
        {
            fail("unexpected exception");
        }
    }

    @Test
    public void testMove()
    {
        final Folder moveTo = mock(Folder.class, "moveTo");
        final String moveToName = "MoveTo";
        try
        {
            when(msg.getFolder()).thenReturn(startingFolder);
            when(mailContext.getFolder(moveToName)).thenReturn(moveTo);
            when(startingFolder.getFullName()).thenReturn("folder");
            when(moveTo.getFullName()).thenReturn(moveToName);

            MessageOperation move = new MoveOperation(moveToName);
            assertThat(move.apply(mailContext, msg)).isTrue();

            verify(startingFolder).copyMessages(argThat(a -> java.util.Arrays.asList(a).contains(msg)), eq(moveTo));
            verify(msg).setFlag(Flags.Flag.DELETED, true);
            verify(startingFolder).getFullName();
            verify(moveTo).getFullName();
        }
        catch (MessagingException e)
        {
            fail("unexpected exception");
        }
    }


    @Test
    public void testSplit()
    {
        final Folder moveTo = mock(Folder.class, "moveTo");
        try
        {
            when(msg.getFolder()).thenReturn(startingFolder);
            when(startingFolder.getSeparator()).thenReturn('.');
            when(startingFolder.getFullName()).thenReturn("folder");
            when(msg.getReceivedDate()).thenReturn(Date.from(LocalDate.of(2012, 4, 8).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            when(mailContext.getFolder("folder.2012.04-Apr-2012")).thenReturn(moveTo);
            when(moveTo.getFullName()).thenReturn("folder.2012.04-Apr-2012");

            MessageOperation split = new SplitOperation();
            assertThat(split.apply(mailContext, msg)).isTrue();

            verify(startingFolder).copyMessages(argThat(a -> java.util.Arrays.asList(a).contains(msg)), eq(moveTo));
            verify(msg).setFlag(Flags.Flag.DELETED, true);
            verify(startingFolder, times(2)).getFullName();
            verify(moveTo).getFullName();
        }
        catch (MessagingException e)
        {
            fail("unexpected exception");
        }
    }
}
