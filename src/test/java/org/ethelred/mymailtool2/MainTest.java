package org.ethelred.mymailtool2;

import org.ethelred.util.ClockFactory;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

/**
 test for main app class
 */
@ExtendWith(MockitoExtension.class)
public class MainTest
{
    @Mock MailToolConfiguration conf;

    @Test
    public void testOperationLimit()
    {
        ClockFactory.setClock(LocalDate.of(2014, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
        when(conf.getOperationLimit()).thenReturn(3);
        when(conf.getTimeLimit()).thenReturn("50 days");
        lenient().when(conf.verbose()).thenReturn(false);

        MailToolContext app = new DefaultContext(conf);
        app.countOperation();
        app.countOperation();
        app.countOperation();
        try
        {
            app.countOperation();
            fail("expected OperationLimitException");
        }
        catch (OperationLimitException e)
        {
            // expected - success
        }

        verify(conf, times(2)).getOperationLimit();
        verify(conf, times(1)).getTimeLimit();
    }
}
