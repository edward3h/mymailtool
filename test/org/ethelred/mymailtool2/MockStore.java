package org.ethelred.mymailtool2;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

/**
 * Store for tests
 */
public class MockStore extends Store
{
    public MockStore(Session session, URLName urlname)
    {
        super(session, urlname);
    }

    @Override
    public Folder getDefaultFolder() throws MessagingException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Folder getFolder(String s) throws MessagingException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Folder getFolder(URLName urlName) throws MessagingException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
