package org.ethelred.mymailtool2.mock;

import java.util.Properties;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Just tests loading the Mock providers
 */
public class MockTest
{
    @Test
    public void testMockStore() throws MessagingException
    {
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imap");
        props.setProperty("mail.debug", "true");
        Session ss = Session.getDefaultInstance(props, new MockAuthenticator());
        Store store = ss.getStore();

        assertEquals("MockStore", store.getClass().getSimpleName());

        Folder f = store.getDefaultFolder();
        assertEquals("Inbox", f.getName());
    }
}
