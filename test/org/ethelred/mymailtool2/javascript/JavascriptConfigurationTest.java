package org.ethelred.mymailtool2.javascript;

import java.io.File;
import java.net.URL;

import org.ethelred.mymailtool2.ApplyMatchOperationsTask;
import org.ethelred.mymailtool2.FileConfigurationHandler;
import org.ethelred.mymailtool2.MailToolConfiguration;
import org.ethelred.mymailtool2.Main;
import org.ethelred.mymailtool2.Task;
import org.ethelred.mymailtool2.mock.MockData;
import org.ethelred.mymailtool2.mock.MockDefaultConfiguration;
import org.ethelred.mymailtool2.mock.MockMessage;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * test loading a js file
 */
public class JavascriptConfigurationTest
{
    @Test
    public void testLoadJavascript() throws Exception
    {
        FileConfigurationHandler h = new JavascriptFileConfigurationHandler();

        URL testFileLocation = this.getClass().getResource("testjavascript.js");
        assertNotNull(testFileLocation);
        File f = new File(testFileLocation.getFile());

        MailToolConfiguration conf = h.readConfiguration(f);

        assertNotNull(conf);
        assertEquals("edward", conf.getUser());
        assertEquals("3 months", conf.getMinAge());
        assertEquals(300, conf.getOperationLimit());
        assertEquals("imap", conf.getMailProperties().get("mail.store.protocol"));

        Task t = conf.getTask();
        assertNotNull(t);
        assertTrue(t instanceof ApplyMatchOperationsTask);
    }

    @Test
    public void testJSMain() throws Exception
    {
        MockDefaultConfiguration conf = new MockDefaultConfiguration();
        conf.addFileHandler(new JavascriptFileConfigurationHandler());
        conf.addFile(this.getClass().getResource("testjavascript.js").getFile());

        MockData data = MockData.getInstance();
        data.addFolder("Inbox");
        data.addFolder("archive");
        data.addFolder("test");

        data.addMessage("Inbox", MockMessage.create("2012-01-01", "from1@example.com", "Hello world"));
        data.addMessage("Inbox", MockMessage.create("2012-01-01", "from1@example.com", "Hello subject1 world"));
        data.addMessage("Inbox", MockMessage.create("2012-01-01", "from1@example.com", "Hello subject2 world"));
        data.addMessage("test", MockMessage.create("2012-01-01", "from1@example.com", "Hello subject1 world"));
        data.addMessage("test", MockMessage.create("2012-01-01", "from1@example.com", "Hello subject2 world world"));

        assertEquals(3, data.folderSize("Inbox"));
        assertEquals(0, data.folderSize("archive"));
        assertEquals(-1, data.folderSize("archive.2012.01-Jan-2012"));
        assertEquals(2, data.folderSize("test"));
        assertEquals(-1, data.folderSize("repeated"));

        Main main = new Main();
        main.setDefaultConfiguration(conf);
        main.init(new String[]{});
        main.run();


        assertEquals(0, data.folderSize("Inbox"));
        assertEquals(0, data.folderSize("archive"));
        assertEquals(1, data.folderSize("archive.2012.01-Jan-2012"));
        assertEquals(1, data.folderSize("test"));
        assertEquals(1, data.folderSize("repeated"));
    }
}
