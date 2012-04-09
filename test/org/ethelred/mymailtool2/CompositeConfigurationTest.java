package org.ethelred.mymailtool2;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.junit.Test;

import static org.ethelred.util.TestUtil.assertEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class CompositeConfigurationTest
{
    @Test
    public void testEmptyConfiguration()
    {
        MailToolConfiguration empty = new CompositeConfiguration();
        assertEquals(0, empty.getOperationLimit());
        assertNull(empty.getUser());
        assertNull(empty.getMinAge());
        assertNull(empty.getPassword());
        assertEmpty(empty.getFileLocations());
        assertEmpty(empty.getFileHandlers());
    }

    public void testSingleConfiguration()
    {

        MailToolConfiguration mock = new MailToolConfiguration()
        {
            @Override
            public String getPassword()
            {
                return "password";
            }

            @Override
            public Map<String, String> getMailProperties()
            {
                return Collections.singletonMap("test", "mail");
            }

            @Override
            public String getUser()
            {
                return "user";
            }

            @Override
            public Iterable<String> getFileLocations()
            {
                return Lists.newArrayList("file1");
            }

            @Override
            public Task getTask() throws Exception
            {
                return null;
            }

            @Override
            public int getOperationLimit()
            {
                return 1000;
            }

            @Override
            public String getMinAge()
            {
                return "3 months";
            }

            @Override
            public Iterable<FileConfigurationHandler> getFileHandlers()
            {
                return null;
            }
        };

        MailToolConfiguration comp = new CompositeConfiguration(mock);
        assertEquals(mock.getOperationLimit(), comp.getOperationLimit());
        assertEquals(mock.getUser(), comp.getUser());
        assertEquals(mock.getMinAge(), comp.getMinAge());
        assertEquals(mock.getPassword(), comp.getPassword());
        assertEquals(mock.getFileLocations(), comp.getFileLocations());
        assertEquals(mock.getFileHandlers(), comp.getFileHandlers());
    }
}
