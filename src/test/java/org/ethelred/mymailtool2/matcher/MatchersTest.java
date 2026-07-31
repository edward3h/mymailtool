package org.ethelred.mymailtool2.matcher;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;

import java.io.IOException;
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


    @Test
    public void testHasAttachmentMatcherNullMessage()
    {
        Predicate<Message> matcher = new HasAttachmentMatcher(".*");
        assertThat(matcher.test(null)).isFalse();
    }

    @Test
    public void testHasAttachmentMatcherNonMultipart() throws MessagingException
    {
        when(msg.isMimeType("multipart/mixed")).thenReturn(false);

        Predicate<Message> matcher = new HasAttachmentMatcher(".*");
        assertThat(matcher.test(msg)).isFalse();
    }

    @Test
    public void testHasAttachmentMatcherMatchingFilename() throws MessagingException, IOException
    {
        when(msg.isMimeType("multipart/mixed")).thenReturn(true);
        Multipart mp = mock(Multipart.class);
        BodyPart part = mock(BodyPart.class);
        when(msg.getContent()).thenReturn(mp);
        when(mp.getCount()).thenReturn(1);
        when(mp.getBodyPart(0)).thenReturn(part);
        when(part.getDisposition()).thenReturn(Part.ATTACHMENT);
        when(part.getFileName()).thenReturn("report.pdf");

        Predicate<Message> matcher = new HasAttachmentMatcher("report.*");
        assertThat(matcher.test(msg)).isTrue();
    }

    @Test
    public void testHasAttachmentMatcherFilenameDoesNotMatchPattern() throws MessagingException, IOException
    {
        when(msg.isMimeType("multipart/mixed")).thenReturn(true);
        Multipart mp = mock(Multipart.class);
        BodyPart part = mock(BodyPart.class);
        when(msg.getContent()).thenReturn(mp);
        when(mp.getCount()).thenReturn(1);
        when(mp.getBodyPart(0)).thenReturn(part);
        when(part.getDisposition()).thenReturn(Part.ATTACHMENT);
        when(part.getFileName()).thenReturn("other.txt");

        Predicate<Message> matcher = new HasAttachmentMatcher("report.*");
        assertThat(matcher.test(msg)).isFalse();
    }

    @Test
    public void testHasAttachmentMatcherNonAttachmentPart() throws MessagingException, IOException
    {
        when(msg.isMimeType("multipart/mixed")).thenReturn(true);
        Multipart mp = mock(Multipart.class);
        BodyPart part = mock(BodyPart.class);
        when(msg.getContent()).thenReturn(mp);
        when(mp.getCount()).thenReturn(1);
        when(mp.getBodyPart(0)).thenReturn(part);
        when(part.getDisposition()).thenReturn(Part.INLINE);

        Predicate<Message> matcher = new HasAttachmentMatcher(".*");
        assertThat(matcher.test(msg)).isFalse();
    }

    @Test
    public void testHasAttachmentMatcherMessagingException() throws MessagingException
    {
        when(msg.isMimeType("multipart/mixed")).thenThrow(new MessagingException("boom"));

        Predicate<Message> matcher = new HasAttachmentMatcher(".*");
        assertThat(matcher.test(msg)).isFalse();
    }

    @Test
    public void testHasFlagMatcherNullMessage()
    {
        Predicate<Message> matcher = new HasFlagMatcher("myflag");
        assertThat(matcher.test(null)).isFalse();
    }

    @Test
    public void testHasFlagMatcherFlagPresent() throws MessagingException
    {
        Flags flags = new Flags();
        flags.add("myflag");
        when(msg.getFlags()).thenReturn(flags);

        Predicate<Message> matcher = new HasFlagMatcher("myflag");
        assertThat(matcher.test(msg)).isTrue();
    }

    @Test
    public void testHasFlagMatcherFlagAbsent() throws MessagingException
    {
        when(msg.getFlags()).thenReturn(new Flags());

        Predicate<Message> matcher = new HasFlagMatcher("myflag");
        assertThat(matcher.test(msg)).isFalse();
    }

    @Test
    public void testHasFlagMatcherMessagingException() throws MessagingException
    {
        when(msg.getFlags()).thenThrow(new MessagingException("boom"));

        Predicate<Message> matcher = new HasFlagMatcher("myflag");
        assertThat(matcher.test(msg)).isFalse();
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
