package org.ethelred.mymailtool2.matcher;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;

import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * test message matchers
 */
public class MatchersTest
{
    Message msg;
    Message msg2;
    Message msg3;

    @BeforeEach
    public void setup()
    {
        msg = mock(Message.class);
        msg2 = mock(Message.class, "Message2");
        msg3 = mock(Message.class, "Message3");
    }

    @Test
    public void testToMatcher()
    {
        final Address[] add1 = mockAddresses("edward@foobar.com");
        final Address[] add2 = mockAddresses();
        final Address[] add3 = null;

        try
        {
            when(msg.getRecipients(Message.RecipientType.TO)).thenReturn(add1);
            when(msg2.getRecipients(Message.RecipientType.TO)).thenReturn(add2);
            when(msg3.getRecipients(Message.RecipientType.TO)).thenReturn(add3);

            Predicate<Message> matcher = new ToAddressMatcher(true, "edward@foobar.com");
            assertThat(matcher.test(msg)).isTrue();

            assertThat(matcher.test(msg2)).isFalse();
            assertThat(matcher.test(msg3)).isFalse();
        }
        catch (MessagingException e)
        {
            fail("unexpected exception");
        }
    }

    @Test
    public void testSubjectMatcher()
    {
        try
        {
            when(msg.getSubject()).thenReturn("test Subject");
            when(msg2.getSubject()).thenReturn(null);

            Predicate<Message> matcher = new SubjectMatcher(".*subject.*");
            assertThat(matcher.test(msg)).isTrue();
            assertThat(matcher.test(msg2)).isFalse();
        }
        catch (MessagingException e)
        {
            fail("unexpected exception");
        }
    }


    private Address[] mockAddresses(String... addresses)
    {
        Address[] result = new Address[addresses.length];
        for (int i = 0; i < addresses.length; i++)
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
