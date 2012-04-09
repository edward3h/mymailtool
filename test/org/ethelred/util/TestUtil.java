package org.ethelred.util;

import com.google.common.collect.Iterables;

import static org.junit.Assert.assertTrue;

/**
 * utils for tests
 */
public class TestUtil
{
    public static void assertEmpty(String value)
    {
        assertTrue("not empty [" + value + "]", "".equals(value));
    }

    public static void assertEmpty(Iterable<?> value)
    {
        assertTrue("not empty [" + Iterables.toString(value) + "]", Iterables.isEmpty(value));

    }
}
