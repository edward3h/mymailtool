package org.ethelred.util;

import com.google.common.collect.Iterables;

import static org.junit.Assert.assertTrue;

/**
 * utils for tests
 */
public final class TestUtil
{
    public static void assertEmpty(String value)
    {
        assertTrue("not empty [" + value + "]", "".equals(value));
    }

    public static void assertEmpty(Iterable<?> value)
    {
        assertTrue("not empty [" + Iterables.toString(value) + "]", Iterables.isEmpty(value));

    }

    public static <T> void assertEquals(Iterable<T> expected, Iterable<T> actual)
    {
        assertTrue("Iterables do not match", Iterables.elementsEqual(expected, actual));
    }

    private TestUtil() {
    }
}
