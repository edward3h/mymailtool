package org.ethelred.mymailtool2;

import jakarta.mail.Flags;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FlagOperationTest
{
    @Mock Message m;
    @Mock MailToolContext mailContext;

    @Test
    public void testAddFlagWhenAbsent() throws MessagingException
    {
        when(m.getFlags()).thenReturn(new Flags());

        FlagOperation op = new FlagOperation(true, "myflag");
        boolean result = op.apply(mailContext, m);

        assertThat(result).isTrue();
        ArgumentCaptor<Flags> captor = ArgumentCaptor.forClass(Flags.class);
        verify(m).setFlags(captor.capture(), eq(true));
        assertThat(captor.getValue().contains("myflag")).isTrue();
    }

    @Test
    public void testAddFlagWhenAlreadyPresent() throws MessagingException
    {
        Flags existing = new Flags();
        existing.add("myflag");
        when(m.getFlags()).thenReturn(existing);

        FlagOperation op = new FlagOperation(true, "myflag");
        boolean result = op.apply(mailContext, m);

        assertThat(result).isFalse();
        verify(m, never()).setFlags(any(Flags.class), anyBoolean());
    }

    @Test
    public void testRemoveFlagWhenPresent() throws MessagingException
    {
        Flags existing = new Flags();
        existing.add("myflag");
        when(m.getFlags()).thenReturn(existing);

        FlagOperation op = new FlagOperation(false, "myflag");
        boolean result = op.apply(mailContext, m);

        assertThat(result).isTrue();
        ArgumentCaptor<Flags> captor = ArgumentCaptor.forClass(Flags.class);
        verify(m).setFlags(captor.capture(), eq(false));
        assertThat(captor.getValue().contains("myflag")).isTrue();
    }

    @Test
    public void testRemoveFlagWhenAlreadyAbsent() throws MessagingException
    {
        when(m.getFlags()).thenReturn(new Flags());

        FlagOperation op = new FlagOperation(false, "myflag");
        boolean result = op.apply(mailContext, m);

        assertThat(result).isFalse();
        verify(m, never()).setFlags(any(Flags.class), anyBoolean());
    }

    @Test
    public void testMessagingExceptionReturnsFalse() throws MessagingException
    {
        when(m.getFlags()).thenThrow(new MessagingException("boom"));

        FlagOperation op = new FlagOperation(true, "myflag");
        boolean result = op.apply(mailContext, m);

        assertThat(result).isFalse();
        verifyNoInteractions(mailContext);
    }

    @Test
    public void testFinishApplyingReturnsFalse()
    {
        FlagOperation op = new FlagOperation(true, "myflag");
        assertThat(op.finishApplying()).isFalse();
    }
}
