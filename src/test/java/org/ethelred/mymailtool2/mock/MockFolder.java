package org.ethelred.mymailtool2.mock;

import java.util.List;
import java.util.Map;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 *
 */
public class MockFolder extends Folder
{
    protected final String name;

    protected final MockData data;
    private Map<Integer, Message> msgCache = Maps.newHashMap();

    public MockFolder(Store store, MockData data, String name)
    {
        super(store);
        this.name = name;
        this.data = data;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getFullName()
    {
        return name;
    }

    @Override
    public Folder getParent() throws MessagingException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean exists() throws MessagingException
    {
        return data.hasFolder(name);
    }

    @Override
    public Folder[] list(String s) throws MessagingException
    {
        return new Folder[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public char getSeparator() throws MessagingException
    {
        return '.';
    }

    @Override
    public int getType() throws MessagingException
    {
        return HOLDS_MESSAGES;
    }

    @Override
    public boolean create(int i) throws MessagingException
    {
        data.addFolder(name);
        return true;
    }

    @Override
    public boolean hasNewMessages() throws MessagingException
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Folder getFolder(String s) throws MessagingException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean delete(boolean b) throws MessagingException
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean renameTo(Folder folder) throws MessagingException
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void open(int i) throws MessagingException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void close(boolean b) throws MessagingException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isOpen()
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Flags getPermanentFlags()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getMessageCount() throws MessagingException
    {
        return data.folderSize(name);
    }

    @Override
    public Message getMessage(int i) throws MessagingException
    {
        MockMessage mm = data.getMessage(name, i);
        if (mm == null) {
            throw new MessagingException();
        }
        return getOrCreateCachedMessage(i, mm);
    }

    /**
     * Returns the cached {@link Message} wrapper for the given sequence number, creating and
     * caching it via {@link MockMessage#getMimeMessage} if not already cached. Any code path that
     * wraps a {@link MockMessage} into a {@link Message} for this folder (whether addressed by
     * sequence number or by some other means, e.g. UID) must go through this method so that
     * {@link #expunge()} (which only inspects {@link #msgCache}) can find it later.
     */
    protected Message getOrCreateCachedMessage(int seqNum, MockMessage mm) throws MessagingException
    {
        if (msgCache.containsKey(seqNum)) {
            return msgCache.get(seqNum);
        }
        Message result = mm.getMimeMessage(this, seqNum);
        msgCache.put(seqNum, result);
        return result;
    }

    @Override
    public void appendMessages(Message[] messages) throws MessagingException
    {
        for (Message m : messages)
        {
            data.addMessage(name, MockMessage.get(m));
        }
    }

    @Override
    public Message[] expunge() throws MessagingException
    {
        List<Message> result = Lists.newArrayList();
        for (Message m : msgCache.values())
        {
            if (m.isSet(Flags.Flag.DELETED) && data.deleteMessage(name, MockMessage.getOuter(m)))
            {
                result.add(m);
            }
        }
        return result.toArray(new Message[result.size()]);
    }
}
