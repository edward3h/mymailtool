package org.ethelred.mymailtool2.javascript;

import org.mozilla.javascript.NativeObject;

/**
 * warning - uses undocumented internal classes
 * @author edward
 */
class MozillaObjectAdapter implements IJSObject
{
    
    static IJSObject wrap(Object obj)
    {
        return wrap((NativeObject) obj);
    }
    
    static MozillaObjectAdapter wrap(NativeObject no)
    {
        return new MozillaObjectAdapter(no);
    }
    
    private final NativeObject no;
    
    private MozillaObjectAdapter(NativeObject no)
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
                    if(i > 1)
                    {
                        nested.append(".");
                    }
                    nested.append(parts[i]);
                }
                return MozillaObjectAdapter.wrap((NativeObject) o).get(nested.toString());
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

    @Override
    public String getString(String propertyName)
    {
        Object obj = get(propertyName);
        return obj == null ? null : String.valueOf(obj);
    }
}
