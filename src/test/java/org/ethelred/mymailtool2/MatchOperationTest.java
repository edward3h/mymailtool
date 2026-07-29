package org.ethelred.mymailtool2;

import jakarta.mail.Message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Predicate;

import static org.mockito.Mockito.*;

/**
 * unit test MatchOperation
 */
@ExtendWith(MockitoExtension.class)
public class MatchOperationTest
{
    @Mock Predicate<Message> matcher;
    @Mock MessageOperation operation;
    @Mock MailToolContext mailContext;
    @Mock Message m;

    @Test
    public void testSuccess()
    {
        when(matcher.test(m)).thenReturn(true);
        when(operation.apply(mailContext, m)).thenReturn(true);
        lenient().when(operation.finishApplying()).thenReturn(true);

        MatchOperation test = new MatchOperation(matcher, operation, 1);
        test.testApply(m, mailContext);

        verify(matcher).test(m);
        verify(operation).apply(mailContext, m);
        verify(mailContext).countOperation();
    }


    @Test
    public void testOpFailure()
    {
        when(matcher.test(m)).thenReturn(true);
        when(operation.apply(mailContext, m)).thenReturn(false);
        lenient().when(operation.finishApplying()).thenReturn(true);

        MatchOperation test = new MatchOperation(matcher, operation, 1);
        test.testApply(m, mailContext);

        verify(matcher).test(m);
        verify(operation).apply(mailContext, m);
    }

    @Test
    public void testMatchFailure()
    {
        when(matcher.test(m)).thenReturn(false);
        lenient().when(operation.finishApplying()).thenReturn(true);

        MatchOperation test = new MatchOperation(matcher, operation, 1);
        test.testApply(m, mailContext);

        verify(matcher).test(m);
    }
}
