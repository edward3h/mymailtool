package org.ethelred.mymailtool2;

import java.util.Calendar;
import java.util.Date;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.lenient;

/**
 * tests for MailUtil
 */
@ExtendWith(MockitoExtension.class)
public class MailUtilTest
{
    @Mock Message m;

    @Test
    public void messageToString() throws MessagingException
    {
        Calendar c = Calendar.getInstance();
        c.set(2012, Calendar.APRIL, 17, 11, 55);
        final Date sentDate = c.getTime();

        lenient().when(m.getSentDate()).thenReturn(sentDate);
        lenient().when(m.getSubject()).thenReturn("test subject");

        assertThat(MailUtil.supplyString(m).get()).isEqualTo("@|cyan 2012-04-17 11:55|@: @|yellow test subject|@");
    }
}
