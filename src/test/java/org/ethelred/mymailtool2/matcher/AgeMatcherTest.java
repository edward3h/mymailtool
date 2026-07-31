package org.ethelred.mymailtool2.matcher;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.ethelred.util.ClockFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.function.Predicate;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * test age-based matching, in particular duration unit parsing
 */
public class AgeMatcherTest
{
    private static final Instant FIXED_NOW = Instant.parse("2024-01-10T00:00:00Z");

    @AfterEach
    public void resetClock()
    {
        ClockFactory.reset();
    }

    @Test
    public void oneMonthIsNotSatisfiedByATenDayOldMessage() throws MessagingException
    {
        ClockFactory.setClock(FIXED_NOW.toEpochMilli());
        Message msg = mock(Message.class);
        when(msg.getReceivedDate()).thenReturn(Date.from(FIXED_NOW.minus(Duration.ofDays(10))));

        Predicate<Message> matcher = new AgeMatcher("1 month", true, null);

        assertThat(matcher.test(msg)).isFalse();
    }

    @Test
    public void oneMonthIsSatisfiedByAFortyDayOldMessage() throws MessagingException
    {
        ClockFactory.setClock(FIXED_NOW.toEpochMilli());
        Message msg = mock(Message.class);
        when(msg.getReceivedDate()).thenReturn(Date.from(FIXED_NOW.minus(Duration.ofDays(40))));

        Predicate<Message> matcher = new AgeMatcher("1 month", true, null);

        assertThat(matcher.test(msg)).isTrue();
    }

    @Test
    public void oneYearIsNotSatisfiedByASixMonthOldMessage() throws MessagingException
    {
        ClockFactory.setClock(FIXED_NOW.toEpochMilli());
        Message msg = mock(Message.class);
        when(msg.getReceivedDate()).thenReturn(Date.from(FIXED_NOW.minus(Duration.ofDays(180))));

        Predicate<Message> matcher = new AgeMatcher("1 year", true, null);

        assertThat(matcher.test(msg)).isFalse();
    }

    @Test
    public void oneYearIsSatisfiedByAThirteenMonthOldMessage() throws MessagingException
    {
        ClockFactory.setClock(FIXED_NOW.toEpochMilli());
        Message msg = mock(Message.class);
        when(msg.getReceivedDate()).thenReturn(Date.from(FIXED_NOW.minus(Duration.ofDays(395))));

        Predicate<Message> matcher = new AgeMatcher("1 year", true, null);

        assertThat(matcher.test(msg)).isTrue();
    }

    @Test
    public void pluralMonthsAreParsed() throws MessagingException
    {
        ClockFactory.setClock(FIXED_NOW.toEpochMilli());
        Message msg = mock(Message.class);
        when(msg.getReceivedDate()).thenReturn(Date.from(FIXED_NOW.minus(Duration.ofDays(70))));

        Predicate<Message> matcher = new AgeMatcher("2 months", true, null);

        assertThat(matcher.test(msg)).isTrue();
    }

    @Test
    public void unrecognizedDurationUnitThrowsWithAClearMessage()
    {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new AgeMatcher("1 fortnight", true, null));

        assertThat(e).hasMessageThat().contains("1 fortnight");
    }
}
