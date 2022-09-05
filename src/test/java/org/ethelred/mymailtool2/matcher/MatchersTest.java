package org.ethelred.mymailtool2.matcher;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;

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
    Message msg3;

    @Before
    public void setup()
    {
        msg = context.mock(Message.class);
        msg2 = context.mock(Message.class, "Message2");
        msg3 = context.mock(Message.class, "Message3");
    }

    @Test
    public void testToMatcher()
    {
        final Address[] add1 = mockAddresses("edward@foobar.com");
        final Address[] add2 = mockAddresses();
        final Address[] add3 = null;

        try
        {
            context.checking(new Expectations(){{
//                oneOf(msg).getAllRecipients(); will(returnValue(add1));
//                oneOf(msg2).getAllRecipients(); will(returnValue(add2));
//                oneOf(msg3).getAllRecipients(); will(returnValue(add3));
                oneOf(msg).getRecipients(Message.RecipientType.TO); will(returnValue(add1));
                oneOf(msg2).getRecipients(Message.RecipientType.TO); will(returnValue(add2));
                oneOf(msg3).getRecipients(Message.RecipientType.TO); will(returnValue(add3));
            }});

            Predicate<Message> matcher = new ToAddressMatcher(true, "edward@foobar.com");
            assertTrue(matcher.apply(msg));

            assertFalse(matcher.apply(msg2));
            assertFalse(matcher.apply(msg3));
            context.assertIsSatisfied();
        }
        catch(MessagingException e)
        {
            fail("unexpected exception");
        }
    }

    @Test
    public void testSubjectMatcher()
    {
        try
        {
            context.checking(new Expectations(){{
                oneOf(msg).getSubject(); will(returnValue("test Subject"));
                oneOf(msg2).getSubject(); will(returnValue(null));
            }});

            Predicate<Message> matcher = new SubjectMatcher(".*subject.*");
            assertTrue(matcher.apply(msg));
            assertFalse(matcher.apply(msg2));
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
