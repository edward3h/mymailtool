package org.ethelred.util;

import static com.google.common.truth.Truth.assertThat;

public final class TestUtil
{
    public static void assertEmpty(String value)
    {
        assertThat(value).isEmpty();
    }

    public static void assertEmpty(Iterable<?> value)
    {
        assertThat(value).isEmpty();
    }

    public static <T> void assertEquals(Iterable<T> expected, Iterable<T> actual)
    {
        assertThat(actual).containsExactlyElementsIn(expected).inOrder();
    }

    private TestUtil() {
    }
}
