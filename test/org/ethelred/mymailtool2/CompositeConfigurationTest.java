package org.ethelred.mymailtool2;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
    }
}
