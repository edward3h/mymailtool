package org.ethelred.mymailtool2.mock;

import java.util.List;
import java.util.NoSuchElementException;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;

/**
 * A {@link MockFolder} that also implements {@link UIDFolder}, for tests that need
 * IMAP-UID-capable folder behaviour.
 */
public class MockUidFolder extends MockFolder implements UIDFolder
{
    public MockUidFolder(Store store, MockData data, String name)
    {
        super(store, data, name);
    }

    @Override
    public long getUIDValidity() throws MessagingException
    {
        return data.getUidValidity(name);
    }

    @Override
    public long getUIDNext() throws MessagingException
    {
        return data.getUidNext(name);
    }

    @Override
    public long getUID(Message message) throws MessagingException
    {
        MockMessage mm = MockMessage.getOuter(message);
        if (mm == null)
        {
            throw new NoSuchElementException("Message is not from this folder");
        }
        return mm.getUid();
    }

    @Override
    public Message getMessageByUID(long uid) throws MessagingException
    {
        MockMessage mm = data.findMessageByUid(name, uid);
        if (mm == null)
        {
            return null;
        }
        return toMessage(mm);
    }

    @Override
    public Message[] getMessagesByUID(long start, long end) throws MessagingException
    {
        List<MockMessage> messages = data.getMessagesByUidRange(name, start, end);
        Message[] result = new Message[messages.size()];
        for (int i = 0; i < messages.size(); i++)
        {
            result[i] = toMessage(messages.get(i));
        }
        return result;
    }

    @Override
    public Message[] getMessagesByUID(long[] uids) throws MessagingException
    {
        Message[] result = new Message[uids.length];
        for (int i = 0; i < uids.length; i++)
        {
            result[i] = getMessageByUID(uids[i]);
        }
        return result;
    }

    private Message toMessage(MockMessage mm) throws MessagingException
    {
        int seqNum = data.indexOfMessage(name, mm);
        return mm.getMimeMessage(this, seqNum);
    }
}
