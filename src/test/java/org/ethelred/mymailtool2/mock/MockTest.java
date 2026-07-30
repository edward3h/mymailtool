package org.ethelred.mymailtool2.mock;

import java.util.Properties;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;

import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

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

        assertThat(store.getClass().getSimpleName()).isEqualTo("MockStore");

        Folder f = store.getDefaultFolder();
        assertThat(f.getName()).isEqualTo("Inbox");
    }
}
