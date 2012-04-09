package org.ethelred.mymailtool2.matcher;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;

import com.google.common.base.Predicate;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * test message matchers
 */
public class MatchersTest
{
    Mockery context = new Mockery(){{setImposteriser(ClassImposteriser.INSTANCE);}};

    Message msg;
    Message msg2;
    @Before
    public void setup()
    {
        msg = context.mock(Message.class);
        msg2 = context.mock(Message.class, "Message2");
    }

    @Test
    public void testToMatcher()
    {
        final Address[] add1 = mockAddresses("edward@foobar.com");
        final Address[] add2 = mockAddresses();

        try
        {
            context.checking(new Expectations(){{
                oneOf(msg).getAllRecipients(); will(returnValue(add1));
                oneOf(msg2).getAllRecipients(); will(returnValue(add2));
            }});

            Predicate<Message> matcher = new ToAddressMatcher("edward\\@foobar\\.com");
            assertTrue(matcher.apply(msg));

            assertFalse(matcher.apply(msg2));
            context.assertIsSatisfied();
        }
        catch(MessagingException e)
        {
            fail("unexpected exception");
        }
    }


    private Address[] mockAddresses(String... addresses)
    {
        Address[] result = new Address[addresses.length];
        for(int i = 0; i < addresses.length; i++)
        {
            result[i] = mockAddress(addresses[i]);
        }
        return result;
    }

    private Address mockAddress(final String address)
    {
        return new Address()
        {
            @Override
            public String getType()
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public String toString()
            {
                return address;
            }

            @Override
            public boolean equals(Object o)
            {
                throw new UnsupportedOperationException();
            }
        };
    }
}
