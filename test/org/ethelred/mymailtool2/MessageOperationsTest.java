package org.ethelred.mymailtool2;

import java.util.Date;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

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

    @Before
    public void setup()
    {
        msg = context.mock(Message.class);
        startingFolder = context.mock(Folder.class);
        mailContext = context.mock(MailToolContext.class);
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
        catch(MessagingException e)
        {
            fail("unexpected exception");
        }
    }

    @Test
    public void testMove()
    {
        final Folder moveTo = context.mock(Folder.class, "moveTo");
        try
        {
            context.checking(new Expectations(){{
                allowing(moveTo).getFullName(); will(returnValue("MoveTo"));
                oneOf(startingFolder).copyMessages(with(hasItemInArray(msg)), with(equal(moveTo)));
                oneOf(msg).setFlag(Flags.Flag.DELETED, true);
            }});
            MessageOperation move = new MoveOperation(moveTo.getFullName());
            assertTrue(move.apply(mailContext, msg));
            context.assertIsSatisfied();
        }
        catch(MessagingException e)
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
                oneOf(msg).getReceivedDate(); will(returnValue(new LocalDate(2012, 4, 8).toDate()));
                oneOf(mailContext).getFolder("folder.04-Apr-2012"); will(returnValue(moveTo));
                oneOf(startingFolder.getFullName()); will(returnValue("folder"));
                //allowing(moveTo).getFullName(); will(returnValue("folder.04-Apr-2012"));
                oneOf(startingFolder).copyMessages(with(hasItemInArray(msg)), with(equal(moveTo)));
                oneOf(msg).setFlag(Flags.Flag.DELETED, true);
            }});
            MessageOperation split = new SplitOperation();
            assertTrue(split.apply(mailContext, msg));
            context.assertIsSatisfied();
        }
        catch(MessagingException e)
        {
            fail("unexpected exception");
        }
    }
}
