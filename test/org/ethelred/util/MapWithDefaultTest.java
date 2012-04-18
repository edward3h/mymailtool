package org.ethelred.util;

import java.util.Map;

import com.google.common.base.Supplier;
import com.google.common.collect.Maps;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class MapWithDefaultTest
{
    @Test
    public void basicTest()
    {
        Map<String, String> ms = Maps.newHashMap();
        assertNull(ms.get("key"));
        Supplier<String> def = new Supplier<String>()
        {
            @Override
            public String get()
            {
                return "def";
            }
        };
        ms = MapWithDefault.wrap(ms, def);
        assertFalse(ms.containsKey("key"));
        assertEquals("def", ms.get("key"));
        assertTrue(ms.containsKey("key"));
    }
}
