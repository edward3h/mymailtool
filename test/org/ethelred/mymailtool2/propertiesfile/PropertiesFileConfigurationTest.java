package org.ethelred.mymailtool2.propertiesfile;

import java.io.File;
import java.io.IOException;

import org.ethelred.mymailtool2.ApplyMatchOperationsTask;
import org.ethelred.mymailtool2.MailToolConfiguration;
import org.ethelred.mymailtool2.Main;
import org.ethelred.mymailtool2.Task;
import org.ethelred.mymailtool2.javascript.JavascriptFileConfigurationHandler;
import org.ethelred.mymailtool2.mock.MockData;
import org.ethelred.mymailtool2.mock.MockDefaultConfiguration;
import org.ethelred.mymailtool2.mock.MockMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class PropertiesFileConfigurationTest
{
    File confFile;

    @Before
    public void setup()
    {
        confFile = new File(getClass().getResource("testproperties.properties").getFile());
    }

    @After
    public void cleanup()
    {
        MockData.clear();
    }

    @Test
    public void testProperties() throws Exception
    {
        MailToolConfiguration conf = new PropertiesFileConfiguration(confFile);
        assertEquals("operation limit", 300, conf.getOperationLimit());

        Task t = conf.getTask();
        assertNotNull(t);
        assertTrue(t instanceof ApplyMatchOperationsTask);

    }

    @Test
    public void testPropertiesMain() throws Exception
    {
        MockDefaultConfiguration conf = new MockDefaultConfiguration();
        conf.addFileHandler(new PropertiesFileConfigurationHandler());
        conf.addFile(this.getClass().getResource("testproperties.properties").getFile());

        MockData data = MockData.getInstance();
        data.addFolder("Inbox");
        data.addFolder("archive");
        data.addFolder("test");

        data.addMessage("Inbox", MockMessage.create("2012-01-01", "from1@example.com", "Hello world"));
        data.addMessage("Inbox", MockMessage.create("2012-01-01", "from1@example.com", "Hello subject1 world"));
        data.addMessage("Inbox", MockMessage.create("2012-01-01", "from1@example.com", "Hello subject2 world"));

        assertEquals(3, data.folderSize("Inbox"));
        assertEquals(0, data.folderSize("archive"));
        assertEquals(-1, data.folderSize("archive.2012.01-Jan-2012"));

        Main main = new Main();
        main.setDefaultConfiguration(conf);
        main.init(new String[]{});
        main.run();


        assertEquals(0, data.folderSize("Inbox"));
        assertEquals(0, data.folderSize("archive"));
        assertEquals(3, data.folderSize("archive.2012.01-Jan-2012"));
    }
}
