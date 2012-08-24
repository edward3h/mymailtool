package org.ethelred.mymailtool2;

import java.util.Calendar;
import java.util.Date;
import javax.mail.Message;
import javax.mail.MessagingException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * tests for MailUtil
 */
public class MailUtilTest
{
    Mockery context = new Mockery(){{setImposteriser(ClassImposteriser.INSTANCE);}};

    @Test
    public void messageToString() throws MessagingException
    {
        final Message m = context.mock(Message.class);
        Calendar c = Calendar.getInstance();
        c.set(2012, Calendar.APRIL, 17);
        final Date sentDate = c.getTime();

        context.checking(new Expectations(){{
            allowing(m).getSentDate(); will(returnValue(sentDate));
            allowing(m).getSubject(); will(returnValue("test subject"));
        }});

        assertEquals("2012-04-17:test subject", MailUtil.toString(m));

        context.assertIsSatisfied();
    }
}
