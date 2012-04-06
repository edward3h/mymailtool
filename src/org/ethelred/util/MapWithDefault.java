package org.ethelred.util;

import java.util.Map;

import com.google.common.base.Supplier;
import com.google.common.collect.ForwardingMap;

/**
 * wraps a Map so that it fills in a default value on get calls
 */
public class MapWithDefault<K, V> extends ForwardingMap<K, V>
{
    private final Map<K, V> delegate;
    private final Supplier<V> defaultSupplier;

    public static <K, V> Map<K, V> wrap(Map<K, V> delegate, Supplier<V> defaultSupplier)
    {
        // don't double wrap
        if(delegate instanceof MapWithDefault)
        {
            return delegate;
        }
        return new MapWithDefault<K, V>(delegate, defaultSupplier);
    }

    private MapWithDefault(Map<K, V> delegate, Supplier<V> defaultSupplier)
    {
        this.delegate = delegate;
        this.defaultSupplier = defaultSupplier;
    }

    @Override
    protected Map<K, V> delegate()
    {
        return delegate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(Object key)
    {
        if(!containsKey(key))
        {
            put((K) key, defaultSupplier.get());
        }
        return super.get(key);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
