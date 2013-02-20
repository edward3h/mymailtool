package org.ethelred.mymailtool2.javascript;

import java.io.File;
import java.net.URL;

import org.ethelred.mymailtool2.ApplyMatchOperationsTask;
import org.ethelred.mymailtool2.FileConfigurationHandler;
import org.ethelred.mymailtool2.MailToolConfiguration;
import org.ethelred.mymailtool2.Task;
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

        Task t = conf.getTask();
        assertNotNull(t);
        assertTrue(t instanceof ApplyMatchOperationsTask);
    }
}
