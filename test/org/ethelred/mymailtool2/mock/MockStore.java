package org.ethelred.mymailtool2.mock;

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
    public final static String DEFAULT_FOLDER_NAME = "Inbox";

    private static MockData data = MockData.getInstance();

    public MockStore(Session session, URLName urlname)
    {
        super(session, urlname);
    }

    @Override
    public Folder getDefaultFolder() throws MessagingException
    {
        return getFolder(DEFAULT_FOLDER_NAME);
    }

    @Override
    public Folder getFolder(String s) throws MessagingException
    {
        return new MockFolder(this, data, s);
    }

    @Override
    public Folder getFolder(URLName urlName) throws MessagingException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean protocolConnect(String host, int port, String user, String password) throws MessagingException
    {
        return true;
    }
}
