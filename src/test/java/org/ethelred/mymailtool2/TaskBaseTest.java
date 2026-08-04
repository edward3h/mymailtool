package org.ethelred.mymailtool2;

import org.ethelred.mymailtool2.mock.MockData;
import org.ethelred.mymailtool2.mock.MockDefaultConfiguration;
import org.ethelred.mymailtool2.mock.MockMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.CheckForNull;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * tests for base functionality
 */
public class TaskBaseTest
{
    private static final Logger LOGGER = LogManager.getLogger();
    MailToolContext mockContext;

    @BeforeEach
    public void setup()
    {

        MockData data = MockData.getInstance();
        data.addFolder("Folder");
        data.addMessage("Folder", MockMessage.create("2012-12-12", "foo@example.com", "test subject"));
        data.addMessage("Folder", MockMessage.create("2012-12-12", "foo@example.com", "test subject"));
        data.addMessage("Folder", MockMessage.create("2012-12-12", "foo@example.com", "test subject"));
        data.addMessage("Folder", MockMessage.create("2012-12-12", "foo@example.com", "test subject"));
        data.addMessage("Folder", MockMessage.create("2012-12-12", "foo@example.com", "test subject"));
        data.addMessage("Folder", MockMessage.create("2012-12-12", "foo@example.com", "test subject"));


        mockContext = new DefaultContext(new MockDefaultConfiguration());
        mockContext.connect();
    }

    @AfterEach
    public void cleanup()
    {
        mockContext.disconnect();
        MockData.clear();
    }

    @Test
    public void testShortcutSimple() throws IOException, MessagingException
    {
        MockTaskBase tb = new MockTaskBase();
        tb.init(mockContext);
        tb.traverseFolder("Folder", false, true);
        assertThat(tb.messageCounter).isEqualTo(3);

    }


    @Test
    public void testNoShortcut() throws IOException, MessagingException
    {
        MockTaskBase tb = new MockTaskBase();
        tb.giveUpAfter = Integer.MAX_VALUE;
        tb.init(mockContext);
        tb.traverseFolder("Folder", false, true);
        assertThat(tb.messageCounter).isEqualTo(MockData.getInstance().folderSize("Folder"));

    }

    @Test
    public void testOnFolderScanFinished_naturalCompletion() throws IOException, MessagingException
    {
        RecordingMockTaskBase tb = new RecordingMockTaskBase();
        tb.giveUpAfter = Integer.MAX_VALUE;
        tb.init(mockContext);
        tb.traverseFolder("Folder", false, true);

        assertThat(tb.finishedCallCount).isEqualTo(1);
        assertThat(tb.recordedCompletedFully).isTrue();
        assertThat(tb.recordedLastConsidered).isSameInstanceAs(tb.lastRunMessage);
    }

    @Test
    public void testOnFolderScanFinished_shortcut() throws IOException, MessagingException
    {
        RecordingMockTaskBase tb = new RecordingMockTaskBase();
        tb.init(mockContext);
        tb.traverseFolder("Folder", false, true);

        assertThat(tb.finishedCallCount).isEqualTo(1);
        assertThat(tb.recordedCompletedFully).isFalse();
        assertThat(tb.recordedLastConsidered).isSameInstanceAs(tb.lastRunMessage);
    }

    @Test
    public void testOnFolderScanFinished_nonShortcutExceptionPropagates() throws IOException, MessagingException
    {
        RecordingMockTaskBase tb = new RecordingMockTaskBase();
        tb.throwRuntimeExceptionAfter = 1;
        tb.init(mockContext);

        assertThrows(RuntimeException.class, () -> tb.traverseFolder("Folder", false, true));

        assertThat(tb.finishedCallCount).isEqualTo(1);
        assertThat(tb.recordedCompletedFully).isFalse();
        assertThat(tb.recordedLastConsidered).isSameInstanceAs(tb.lastRunMessage);
    }

    private class MockTaskBase extends TaskBase
    {
        int giveUpAfter = 1;
        int messageCounter;
        @Override
        protected void runMessage(Folder f, Message m) throws MessagingException, IOException
        {
            LOGGER.info("Check message {}", messageCounter);
            if (messageCounter++ > giveUpAfter)
            {
                throw new ShortcutFolderScanException();
            }
        }

        @Override
        protected void status(Folder f)
        {

            LOGGER.info("Status folder {}", f);
        }

        @Override
        public void run()
        {

        }
    }

    private class RecordingMockTaskBase extends TaskBase
    {
        int giveUpAfter = 1;
        int messageCounter;
        Integer throwRuntimeExceptionAfter;
        Message lastRunMessage;

        int finishedCallCount;
        boolean recordedCompletedFully;
        Message recordedLastConsidered;

        @Override
        protected void runMessage(Folder f, Message m) throws MessagingException, IOException
        {
            lastRunMessage = m;
            LOGGER.info("Check message {}", messageCounter);
            int count = messageCounter++;
            if (throwRuntimeExceptionAfter != null && count >= throwRuntimeExceptionAfter)
            {
                throw new RuntimeException("boom");
            }
            if (count > giveUpAfter)
            {
                throw new ShortcutFolderScanException();
            }
        }

        @Override
        protected void onFolderScanFinished(Folder f, boolean completedFully, @CheckForNull Message lastConsidered)
        {
            finishedCallCount++;
            recordedCompletedFully = completedFully;
            recordedLastConsidered = lastConsidered;
        }

        @Override
        protected void status(Folder f)
        {

            LOGGER.info("Status folder {}", f);
        }

        @Override
        public void run()
        {

        }
    }
}
