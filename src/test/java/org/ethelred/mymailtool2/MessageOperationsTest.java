package org.ethelred.mymailtool2;

import java.util.Calendar;
import java.util.Date;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * test message operations
 */
public class MessageOperationsTest
{
    Mockery context = new Mockery(){{setImposteriser(ClassImposteriser.INSTANCE);}};

    Message msg;
    Folder startingFolder;
    MailToolContext mailContext;
    private Date sentDate;

    @Before
    public void setup() throws MessagingException
    {
        msg = context.mock(Message.class);
        startingFolder = context.mock(Folder.class);
        mailContext = context.mock(MailToolContext.class);
        Calendar c = Calendar.getInstance();
        c.set(2012, Calendar.APRIL, 17);
        sentDate = c.getTime();

        context.checking(new Expectations(){{
            allowing(msg).getSentDate(); will(returnValue(sentDate));
            allowing(msg).getSubject(); will(returnValue("test subject"));
        }});
    }

    @Test
    public void testDelete()
    {
        try
        {
            context.checking(new Expectations(){{
                oneOf(msg).setFlag(Flags.Flag.DELETED, true);
            }});
            MessageOperation del = new DeleteOperation();
            assertTrue(del.apply(mailContext, msg));
            context.assertIsSatisfied();
        }
        catch (MessagingException e)
        {
            fail("unexpected exception");
        }
    }

    @Test
    public void testMove()
    {
        final Folder moveTo = context.mock(Folder.class, "moveTo");
        final String moveToName = "MoveTo";
        try
        {
            context.checking(new Expectations(){{
                oneOf(msg).getFolder(); will(returnValue(startingFolder));
                oneOf(mailContext).getFolder(moveToName); will(returnValue(moveTo));
                oneOf(startingFolder).copyMessages(with(hasItemInArray(msg)), with(equal(moveTo)));
                oneOf(msg).setFlag(Flags.Flag.DELETED, true);
                oneOf(startingFolder).getFullName(); will(returnValue("folder"));
                oneOf(moveTo).getFullName(); will(returnValue(moveToName));
            }});
            MessageOperation move = new MoveOperation(moveToName);
            assertTrue(move.apply(mailContext, msg));
            context.assertIsSatisfied();
        }
        catch (MessagingException e)
        {
            fail("unexpected exception");
        }
    }


    @Test
    public void testSplit()
    {
        final Folder moveTo = context.mock(Folder.class, "moveTo");
        try
        {
            context.checking(new Expectations(){{
                oneOf(msg).getFolder(); will(returnValue(startingFolder));
                oneOf(startingFolder).getSeparator(); will(returnValue('.'));
                exactly(2).of(startingFolder).getFullName(); will(returnValue("folder"));
                oneOf(msg).getReceivedDate(); will(returnValue(new LocalDate(2012, 4, 8).toDate()));
                oneOf(mailContext).getFolder("folder.2012.04-Apr-2012"); will(returnValue(moveTo));
                //allowing(moveTo).getFullName(); will(returnValue("folder.04-Apr-2012"));
                oneOf(startingFolder).copyMessages(with(hasItemInArray(msg)), with(equal(moveTo)));
                oneOf(msg).setFlag(Flags.Flag.DELETED, true);
                oneOf(moveTo).getFullName(); will(returnValue("folder.2012.04-Apr-2012"));
            }});
            MessageOperation split = new SplitOperation();
            assertTrue(split.apply(mailContext, msg));
            context.assertIsSatisfied();
        }
        catch (MessagingException e)
        {
            fail("unexpected exception");
        }
    }
}
