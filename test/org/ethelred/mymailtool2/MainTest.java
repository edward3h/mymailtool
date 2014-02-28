package org.ethelred.mymailtool2;

import org.ethelred.util.ClockFactory;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.junit.Test;

import javax.mail.Message;
import javax.mail.MessagingException;

import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 test for main app class
 */
public class MainTest
{
    Mockery my = new Mockery(){{setImposteriser(ClassImposteriser.INSTANCE);}};

    @Test
    public void testAgeCompare() throws MessagingException
    {
        final MailToolConfiguration conf = my.mock(MailToolConfiguration.class);
        final Message m = my.mock(Message.class);

        ClockFactory.setClock(new DateMidnight(2014, 1, 1).getMillis());
        my.checking(new Expectations(){{
            oneOf(conf).getMinAge(); will(returnValue("3 months"));
            oneOf(m).getReceivedDate(); will(returnValue(new Date(new DateMidnight(2013,9,30).getMillis())));
            oneOf(m).getReceivedDate(); will(returnValue(new Date(new DateMidnight(2013,10,2).getMillis())));
        }});
        MailToolContext app = new DefaultContext(conf);

        assertTrue("First date is not old enough", app.isOldEnough(m));
        assertFalse("Second date is too old", app.isOldEnough(m));

        my.assertIsSatisfied();
    }

    @Test
    public void testOperationLimit()
    {
        final MailToolConfiguration conf = my.mock(MailToolConfiguration.class);

        ClockFactory.setClock(new DateMidnight(2014, 1, 1).getMillis());
        my.checking(new Expectations(){{
            exactly(2).of(conf).getOperationLimit(); will(returnValue(3));
            exactly(1).of(conf).getTimeLimit(); will(returnValue("50 days"));
        }});

        MailToolContext app = new DefaultContext(conf);
        app.countOperation();
        app.countOperation();
        app.countOperation();
        try
        {
            app.countOperation();
            fail("expected OperationLimitException");
        }
        catch(OperationLimitException e)
        {
            // expected - success
        }
        my.assertIsSatisfied();

    }
}
