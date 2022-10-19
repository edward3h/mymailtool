package org.ethelred.mymailtool2;

import org.ethelred.mymailtool2.mock.MockData;
import org.ethelred.mymailtool2.mock.MockDefaultConfiguration;
import org.ethelred.mymailtool2.mock.MockMessage;
import org.ethelred.mymailtool2.mock.MockStore;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * tests for base functionality
 */
public class TaskBaseTest
{
    private static final Logger LOGGER = LogManager.getLogger();
    MailToolContext mockContext;

    @Before
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

    @After
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
        assertEquals(3, tb.messageCounter);

    }


    @Test
    public void testNoShortcut() throws IOException, MessagingException
    {
        MockTaskBase tb = new MockTaskBase();
        tb.giveUpAfter = Integer.MAX_VALUE;
        tb.init(mockContext);
        tb.traverseFolder("Folder", false, true);
        assertEquals(MockData.getInstance().folderSize("Folder"), tb.messageCounter);

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
}
