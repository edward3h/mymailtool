package org.ethelred.mymailtool2;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;

import com.google.common.collect.Lists;
import org.ethelred.mymailtool2.mock.MockAuthenticator;
import org.ethelred.mymailtool2.mock.MockData;
import org.ethelred.mymailtool2.mock.MockMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link UidRangeMessageIterable}.
 */
public class UidRangeMessageIterableTest
{
    private static final String FOLDER_NAME = "UidRangeTestFolder";

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

    private void addMessages(MockData data, int count)
    {
        for (int i = 1; i <= count; i++)
        {
            data.addMessage(FOLDER_NAME, MockMessage.create("2024-01-0" + i, "sender" + i + "@example.com", "message " + i));
        }
    }

    private List<Message> drain(Iterable<Message> iterable)
    {
        return Lists.newArrayList(iterable);
    }

    @Test
    public void iteratesAllMessagesAscendingFromStartUidOne() throws MessagingException
    {
        MockData data = MockData.getInstance();
        data.setUidCapable(FOLDER_NAME, true);
        addMessages(data, 5);

        Store store = getStore();
        Folder folder = store.getFolder(FOLDER_NAME);

        UidRangeMessageIterable iterable = new UidRangeMessageIterable(folder, 1L);
        List<Message> messages = drain(iterable);

        assertThat(messages).hasSize(5);
        for (int i = 0; i < 5; i++)
        {
            assertThat(((UIDFolder) folder).getUID(messages.get(i))).isEqualTo(i + 1L);
        }
    }

    @Test
    public void iteratesOnlyMessagesFromPartwayStartUid() throws MessagingException
    {
        MockData data = MockData.getInstance();
        data.setUidCapable(FOLDER_NAME, true);
        addMessages(data, 5);

        Store store = getStore();
        Folder folder = store.getFolder(FOLDER_NAME);

        UidRangeMessageIterable iterable = new UidRangeMessageIterable(folder, 3L);
        List<Message> messages = drain(iterable);

        assertThat(messages).hasSize(3);
        assertThat(((UIDFolder) folder).getUID(messages.get(0))).isEqualTo(3L);
        assertThat(((UIDFolder) folder).getUID(messages.get(1))).isEqualTo(4L);
        assertThat(((UIDFolder) folder).getUID(messages.get(2))).isEqualTo(5L);
    }

    @Test
    public void emptyRangeWhenStartUidAtOrPastSnapshotYieldsNoMessages() throws MessagingException
    {
        MockData data = MockData.getInstance();
        data.setUidCapable(FOLDER_NAME, true);
        addMessages(data, 3);

        Store store = getStore();
        Folder folder = store.getFolder(FOLDER_NAME);

        // uidNext is 4 after adding 3 messages; starting at 4 (or beyond) means nothing new
        UidRangeMessageIterable iterable = new UidRangeMessageIterable(folder, 4L);

        Iterator<Message> it = iterable.iterator();
        assertThat(it.hasNext()).isFalse();
    }

    @Test
    public void emptyFolderYieldsNoMessages() throws MessagingException
    {
        MockData data = MockData.getInstance();
        data.setUidCapable(FOLDER_NAME, true);

        Store store = getStore();
        Folder folder = store.getFolder(FOLDER_NAME);

        UidRangeMessageIterable iterable = new UidRangeMessageIterable(folder, 1L);

        assertThat(drain(iterable)).isEmpty();
    }

    @Test
    public void messagesAddedAfterConstructionAreExcludedFromIteration() throws MessagingException
    {
        MockData data = MockData.getInstance();
        data.setUidCapable(FOLDER_NAME, true);
        addMessages(data, 3);

        Store store = getStore();
        Folder folder = store.getFolder(FOLDER_NAME);

        UidRangeMessageIterable iterable = new UidRangeMessageIterable(folder, 1L);
        long snapshotEnd = iterable.getEndUidExclusive();

        // simulate new mail arriving between construction and iteration
        data.addMessage(FOLDER_NAME, MockMessage.create("2024-02-01", "late@example.com", "late arrival"));

        List<Message> messages = drain(iterable);

        assertThat(messages).hasSize(3);
        for (Message m : messages)
        {
            long uid = ((UIDFolder) folder).getUID(m);
            assertThat(uid).isLessThan(snapshotEnd);
        }
        for (Message m : messages)
        {
            assertThat(m.getSubject()).isNotEqualTo("late arrival");
        }
    }

    @Test
    public void getEndUidExclusiveIsSnapshotAndDoesNotChange() throws MessagingException
    {
        MockData data = MockData.getInstance();
        data.setUidCapable(FOLDER_NAME, true);
        addMessages(data, 2);

        Store store = getStore();
        Folder folder = store.getFolder(FOLDER_NAME);

        UidRangeMessageIterable iterable = new UidRangeMessageIterable(folder, 1L);
        long before = iterable.getEndUidExclusive();
        assertThat(before).isEqualTo(3L);

        data.addMessage(FOLDER_NAME, MockMessage.create("2024-03-01", "another@example.com", "another"));

        long after = iterable.getEndUidExclusive();
        assertThat(after).isEqualTo(before);
    }

    @Test
    public void nonUidFolderThrowsIllegalArgumentException() throws MessagingException
    {
        // do not mark folder as UID-capable, so store returns a plain (non-UIDFolder) MockFolder
        Store store = getStore();
        Folder folder = store.getFolder(FOLDER_NAME);

        assertThrows(IllegalArgumentException.class, () -> new UidRangeMessageIterable(folder, 1L));
    }

    @Test
    public void iteratorRemoveThrowsUnsupportedOperationException() throws MessagingException
    {
        MockData data = MockData.getInstance();
        data.setUidCapable(FOLDER_NAME, true);
        addMessages(data, 2);

        Store store = getStore();
        Folder folder = store.getFolder(FOLDER_NAME);

        UidRangeMessageIterable iterable = new UidRangeMessageIterable(folder, 1L);
        Iterator<Message> it = iterable.iterator();
        it.next();

        assertThrows(UnsupportedOperationException.class, it::remove);
    }
}
