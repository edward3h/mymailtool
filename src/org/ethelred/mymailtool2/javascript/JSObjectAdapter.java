package org.ethelred.mymailtool2.javascript;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.MapMaker;
import sun.org.mozilla.javascript.NativeObject;

/**
 * warning - uses undocumented internal classes
 * @author edward
 */
class JSObjectAdapter
{
    private static ConcurrentMap<NativeObject, JSObjectAdapter> cache = 
            new MapMaker().makeMap();
    
    static JSObjectAdapter wrap(Object obj)
    {
        return wrap((NativeObject) obj);
    }
    
    static synchronized JSObjectAdapter wrap(NativeObject no)
    {
        JSObjectAdapter result = cache.get(no);
        if(result == null)
        {
            result = new JSObjectAdapter(no);
            result = cache.putIfAbsent(no, result);
        }
        return result;
    }
    
    private final NativeObject no;
    
    private JSObjectAdapter(NativeObject no)
    {
        this.no = no;
    }
    
    Object get(String propertyName)
    {
        if(propertyName.contains("."))
        {
            String[] parts = propertyName.split("\\.");
            Object o = NativeObject.getProperty(no, parts[0]);
            if(o instanceof NativeObject)
            {
                StringBuilder nested = new StringBuilder();
                for(int i = 1; i < parts.length; i++)
                {
                    nested.append(parts[i]);
                }
                return JSObjectAdapter.wrap((NativeObject) o).get(nested.toString());
            }
            else
            {
                return null;
            }
        }
        else
        {
            return NativeObject.getProperty(no, propertyName);
        }
    }

    String getString(String propertyName)
    {
        Object obj = get(propertyName);
        return obj == null ? null : String.valueOf(obj);
    }
}
