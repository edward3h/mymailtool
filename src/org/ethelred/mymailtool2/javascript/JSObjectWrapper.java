package org.ethelred.mymailtool2.javascript;

/**
 *
 */
public class JSObjectWrapper
{
    public static IJSObject wrap(Object o)
    {
        return MozillaObjectAdapter.wrap(o);
    }
}
