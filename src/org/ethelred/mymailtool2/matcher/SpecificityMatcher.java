package org.ethelred.mymailtool2.matcher;

/**
 * A matcher exposes this interface if it knows how specific it is
 */
public interface SpecificityMatcher
{
    int getSpecificity();
}
