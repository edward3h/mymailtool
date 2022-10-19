package org.ethelred.util;

/**
 * abstraction of how to get time, so we can test
 * @see ClockFactory
 */
public interface Clock
{
    long currentTimeMillis();
}
