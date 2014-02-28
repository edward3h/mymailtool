package org.ethelred.mymailtool2;

/**
 * this is an 'expected' exception. Uh-oh. Triggered to jump out of flow when
 * we hit the operation limit.
 * @author edward
 */
class OperationLimitException extends RuntimeException
{

    public OperationLimitException(String message)
    {
        super(message);
    }
    
}
