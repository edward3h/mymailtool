package org.ethelred.util;

import com.google.common.annotations.VisibleForTesting;

/**
 * assists with getting time in an indirect way, so we can test classes which depend on time
 */
public final class ClockFactory
{
    private static final Clock SYSTEM = System::currentTimeMillis;

    private static Clock instance = SYSTEM;

    public static Clock getClock()
    {
        return instance;
    }

    @VisibleForTesting public static void setClock(Clock c)
    {
        instance = c;
    }

    @VisibleForTesting public static void setClock(final long nMillis)
    {
        instance = () -> nMillis;
    }

    @VisibleForTesting public static void reset()
    {
        instance = SYSTEM;
    }

    private ClockFactory() {
    }

}
