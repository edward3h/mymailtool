package org.ethelred.mymailtool2;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.joda.time.DateTime;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 test for main app class
 */
public class MainTest
{
    Mockery my = new Mockery();

    @Test
    public void testAgeCompare()
    {
        final MailToolConfiguration conf = my.mock(MailToolConfiguration.class);

        my.checking(new Expectations(){{
            oneOf(conf).getMinAge(); will(returnValue("3 months"));
        }});
        Main app = new Main();
        app.config = conf;

        DateTime dt = app.getAgeCompare();
        long delta = Math.abs(dt.toDate().getTime() - System.currentTimeMillis());
        delta = delta / (1000L * 60 * 60 * 24);
        assertTrue(delta > 89 && delta < 95);

        my.assertIsSatisfied();
    }

    @Test
    public void testOperationLimit()
    {
        final MailToolConfiguration conf = my.mock(MailToolConfiguration.class);

        my.checking(new Expectations(){{
            exactly(1).of(conf).getOperationLimit(); will(returnValue(3));
            exactly(1).of(conf).getTimeLimit(); will(returnValue("50 days"));
        }});
        Main app = new Main();
        app.config = conf;
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
