package org.ethelred.mymailtool2.propertiesfile;

import java.io.File;
import java.io.IOException;

import org.ethelred.mymailtool2.ApplyMatchOperationsTask;
import org.ethelred.mymailtool2.MailToolConfiguration;
import org.ethelred.mymailtool2.Task;
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

    @Test
    public void testProperties() throws Exception
    {
        MailToolConfiguration conf = new PropertiesFileConfiguration(confFile);
        assertEquals("operation limit", 300, conf.getOperationLimit());

        Task t = conf.getTask();
        assertNotNull(t);
        assertTrue(t instanceof ApplyMatchOperationsTask);

    }

}
