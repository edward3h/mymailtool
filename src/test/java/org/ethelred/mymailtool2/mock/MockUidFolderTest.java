package org.ethelred.mymailtool2.mock;

import java.util.NoSuchElementException;
import java.util.Properties;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the UID-capable mock folder infrastructure.
 */
public class MockUidFolderTest
{
    private static final String FOLDER_NAME = "UidTestFolder";

    private Store getStore() throws MessagingException
    {
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imap");
        Session ss = Session.getDefaultInstance(props, new MockAuthenticator());
        return ss.getStore();
    }

    @BeforeEach
    public void setUp()
    {
        MockData.clear();
    }

    @AfterEach
    public void tearDown()
    {
        MockData.clear();
    }

    @Test
    public void nonUidCapableFolderIsPlainMockFolder() throws MessagingException
    {
        Store store = getStore();
        Folder f = store.getFolder(FOLDER_NAME);

        assertThat(f).isInstanceOf(MockFolder.class);
        assertThat(f).isNotInstanceOf(UIDFolder.class);
    }

    @Test
    public void uidCapableFolderIsMockUidFolder() throws MessagingException
    {
        MockData.getInstance().setUidCapable(FOLDER_NAME, true);
        Store store = getStore();
        Folder f = store.getFolder(FOLDER_NAME);

        assertThat(f).isInstanceOf(MockFolder.class);
        assertThat(f).isInstanceOf(UIDFolder.class);
    }

    @Test
    public void messagesGetSequentialUidsInAddOrderNotDateOrder() throws MessagingException
    {
        MockData data = MockData.getInstance();
        data.setUidCapable(FOLDER_NAME, true);

        // add out of date order: second message has an earlier date than first
        MockMessage first = MockMessage.create("2024-06-01", "a@example.com", "first added");
        MockMessage second = MockMessage.create("2024-01-01", "b@example.com", "second added, earlier date");
        MockMessage third = MockMessage.create("2024-12-01", "c@example.com", "third added");

        data.addMessage(FOLDER_NAME, first);
        data.addMessage(FOLDER_NAME, second);
        data.addMessage(FOLDER_NAME, third);

        assertThat(first.getUid()).isEqualTo(1L);
        assertThat(second.getUid()).isEqualTo(2L);
        assertThat(third.getUid()).isEqualTo(3L);
    }

    @Test
    public void getUIDNextReflectsOneMoreThanHighestAssigned() throws MessagingException
    {
        MockData data = MockData.getInstance();
        data.setUidCapable(FOLDER_NAME, true);

        Store store = getStore();
        UIDFolder f = (UIDFolder) store.getFolder(FOLDER_NAME);

        assertThat(f.getUIDNext()).isEqualTo(1L);

        data.addMessage(FOLDER_NAME, MockMessage.create("2024-01-01", "a@example.com", "one"));
        data.addMessage(FOLDER_NAME, MockMessage.create("2024-01-02", "b@example.com", "two"));

        assertThat(f.getUIDNext()).isEqualTo(3L);
    }

    @Test
    public void getUIDValidityDefaultsAndCanBeChanged() throws MessagingException
    {
        MockData data = MockData.getInstance();
        data.setUidCapable(FOLDER_NAME, true);

        Store store = getStore();
        UIDFolder f = (UIDFolder) store.getFolder(FOLDER_NAME);

        long defaultValidity = f.getUIDValidity();
        assertThat(defaultValidity).isEqualTo(1L);

        data.setUidValidity(FOLDER_NAME, 42L);
        assertThat(f.getUIDValidity()).isEqualTo(42L);
    }

    @Test
    public void getMessageByUIDReturnsRightMessageOrNull() throws MessagingException
    {
        MockData data = MockData.getInstance();
        data.setUidCapable(FOLDER_NAME, true);
        data.addMessage(FOLDER_NAME, MockMessage.create("2024-01-01", "a@example.com", "one"));
        data.addMessage(FOLDER_NAME, MockMessage.create("2024-01-02", "b@example.com", "two"));

        Store store = getStore();
        UIDFolder f = (UIDFolder) store.getFolder(FOLDER_NAME);

        Message m = f.getMessageByUID(2L);
        assertThat(m).isNotNull();
        assertThat(m.getSubject()).isEqualTo("two");

        assertThat(f.getMessageByUID(999L)).isNull();
    }

    @Test
    public void getMessagesByUIDRangeReturnsSubsetInAscendingOrder() throws MessagingException
    {
        MockData data = MockData.getInstance();
        data.setUidCapable(FOLDER_NAME, true);
        data.addMessage(FOLDER_NAME, MockMessage.create("2024-01-01", "a@example.com", "one"));
        data.addMessage(FOLDER_NAME, MockMessage.create("2024-01-02", "b@example.com", "two"));
        data.addMessage(FOLDER_NAME, MockMessage.create("2024-01-03", "c@example.com", "three"));

        Store store = getStore();
        UIDFolder f = (UIDFolder) store.getFolder(FOLDER_NAME);

        Message[] subset = f.getMessagesByUID(2L, 3L);
        assertThat(subset).hasLength(2);
        assertThat(subset[0].getSubject()).isEqualTo("two");
        assertThat(subset[1].getSubject()).isEqualTo("three");

        Message[] fromStart = f.getMessagesByUID(2L, UIDFolder.LASTUID);
        assertThat(fromStart).hasLength(2);
        assertThat(fromStart[0].getSubject()).isEqualTo("two");
        assertThat(fromStart[1].getSubject()).isEqualTo("three");
    }

    @Test
    public void getUIDRoundTripsForMessageFetchedBySeqNumOrUid() throws MessagingException
    {
        MockData data = MockData.getInstance();
        data.setUidCapable(FOLDER_NAME, true);
        data.addMessage(FOLDER_NAME, MockMessage.create("2024-01-01", "a@example.com", "one"));
        data.addMessage(FOLDER_NAME, MockMessage.create("2024-01-02", "b@example.com", "two"));

        Store store = getStore();
        UIDFolder uf = (UIDFolder) store.getFolder(FOLDER_NAME);
        MockFolder folder = (MockFolder) uf;

        Message bySeq = folder.getMessage(2);
        assertThat(uf.getUID(bySeq)).isEqualTo(2L);

        Message byUid = uf.getMessageByUID(1L);
        assertThat(uf.getUID(byUid)).isEqualTo(1L);
    }

    @Test
    public void expungeFindsMessageFetchedByUid() throws MessagingException
    {
        MockData data = MockData.getInstance();
        data.setUidCapable(FOLDER_NAME, true);
        data.addMessage(FOLDER_NAME, MockMessage.create("2024-01-01", "a@example.com", "one"));
        data.addMessage(FOLDER_NAME, MockMessage.create("2024-01-02", "b@example.com", "two"));

        Store store = getStore();
        UIDFolder uf = (UIDFolder) store.getFolder(FOLDER_NAME);
        MockFolder folder = (MockFolder) uf;

        // fetch only via UID, never via sequence number, then flag deleted and expunge
        Message m = uf.getMessageByUID(1L);
        m.setFlag(Flags.Flag.DELETED, true);

        Message[] expunged = folder.expunge();

        assertThat(expunged).hasLength(1);
        assertThat(expunged[0].getSubject()).isEqualTo("one");
        assertThat(data.folderSize(FOLDER_NAME)).isEqualTo(1);
        assertThat(data.getMessage(FOLDER_NAME, 1).toString()).contains("two");
    }

    @Test
    public void getUIDThrowsForMessageFromAnotherFolder() throws MessagingException
    {
        MockData data = MockData.getInstance();
        String otherFolderName = "OtherUidFolder";
        data.setUidCapable(FOLDER_NAME, true);
        data.setUidCapable(otherFolderName, true);
        data.addMessage(FOLDER_NAME, MockMessage.create("2024-01-01", "a@example.com", "in folder A"));
        data.addMessage(otherFolderName, MockMessage.create("2024-01-01", "b@example.com", "in folder B"));

        Store store = getStore();
        UIDFolder folderA = (UIDFolder) store.getFolder(FOLDER_NAME);
        UIDFolder folderB = (UIDFolder) store.getFolder(otherFolderName);

        Message messageFromB = folderB.getMessageByUID(1L);

        assertThrows(NoSuchElementException.class, () -> folderA.getUID(messageFromB));
    }

    @Test
    public void clearResetsUidTrackingState() throws MessagingException
    {
        MockData data = MockData.getInstance();
        data.setUidCapable(FOLDER_NAME, true);
        data.addMessage(FOLDER_NAME, MockMessage.create("2024-01-01", "a@example.com", "one"));
        data.addMessage(FOLDER_NAME, MockMessage.create("2024-01-02", "b@example.com", "two"));

        assertThat(data.getUidNext(FOLDER_NAME)).isEqualTo(3L);

        MockData.clear();

        MockMessage freshFirst = MockMessage.create("2024-01-01", "a@example.com", "fresh one");
        MockData.getInstance().addMessage(FOLDER_NAME, freshFirst);

        assertThat(freshFirst.getUid()).isEqualTo(1L);
    }
}
