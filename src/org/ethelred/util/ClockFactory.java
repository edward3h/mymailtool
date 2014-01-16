package org.ethelred.util;

import com.google.common.annotations.VisibleForTesting;

/**
 * assists with getting time in an indirect way, so we can test classes which depend on time
 */
public class ClockFactory
{
    private final static Clock SYSTEM = new Clock()
    {
        @Override
        public long currentTimeMillis()
        {
            return System.currentTimeMillis();
        }
    };

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
        instance = new Clock()
        {
            @Override
            public long currentTimeMillis()
            {
                return nMillis;
            }
        };
    }

    @VisibleForTesting public static void reset()
    {
        instance = SYSTEM;
    }

}