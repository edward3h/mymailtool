package org.ethelred.mymailtool2.javascript;

/**
 *
 */
public final class JSObjectWrapper
{
    public static IJSObject wrap(Object o)
    {
        return MozillaObjectAdapter.wrap(o);
    }

    private JSObjectWrapper() {
    }
}
