package org.ethelred.mymailtool2.javascript;

/**
 * Javascript compatibility
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
