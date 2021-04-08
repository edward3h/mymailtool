package org.ethelred.mymailtool2;

import java.util.Iterator;
import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * iterate over messages in a folder in reverse order (most recent first)
 */
public class RecentMessageIterable implements Iterable<Message>
{
    private final Folder folder;
    private final boolean newestFirst;
    private final int chunkSize;

    private final static int DEFAULT_CHUNK_SIZE = 100;

    public RecentMessageIterable(Folder f, boolean newestFirst, int chunkSize)
    {
        this.folder = f;
        this.newestFirst = newestFirst;
        this.chunkSize = chunkSize <= 0 ? DEFAULT_CHUNK_SIZE : chunkSize;
    }

    public RecentMessageIterable(Folder folder, boolean newestFirst)
    {
        this(folder, newestFirst, DEFAULT_CHUNK_SIZE);
    }

    @Override
    public Iterator<Message> iterator()
    {
        try
        {
            if(newestFirst)
            {
                return new NewestFirstRecentMessageIterator(folder, chunkSize);
            }
            else
            {
                return new OldestFirstRecentMessageIterator(folder, chunkSize);
            }
        }
        catch(MessagingException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static class NewestFirstRecentMessageIterator implements Iterator<Message>
    {
        private final Folder folder;
        private int messageNumber;
        private int arrayIndex;
        private Message[] messages;
        private FetchProfile fp;
        private final int chunkSize;

        public NewestFirstRecentMessageIterator(Folder folder, int chunkSize) throws MessagingException
        {
            this.folder = folder;
            this.chunkSize = chunkSize;
            this.messageNumber = this.folder.getMessageCount();
            if(messageNumber < 0)
            {
                throw new MessagingException("Could not get message count from folder" + folder);
            }
            fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            _loadChunk();
        }

        private void _loadChunk()
        {
            int chunkSize = Math.min(this.chunkSize, messageNumber);
            int[] ids = new int[chunkSize];
            for(int i = 0; i < chunkSize; i++)
            {
                ids[i] = messageNumber--;
            }
            try
            {
                messages = folder.getMessages(ids);
                folder.fetch(messages, fp);
                arrayIndex = 0;
            }
            catch(MessagingException e)
            {
                throw  new RuntimeException(e);
            }
        }

        @Override
        public boolean hasNext()
        {
            return messageNumber > 0 || arrayIndex < messages.length;
        }

        @Override
        public Message next()
        {
            Message result = messages[arrayIndex++];
            if(arrayIndex >= messages.length)
            {
                _loadChunk();
            }

            return result;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }


    private static class OldestFirstRecentMessageIterator implements Iterator<Message>
    {
        private final Folder folder;
        private final int chunkSize;
        private int messageNumber;
        private final int messageCount;
        private Message[] messages;
        private FetchProfile fp;

        public OldestFirstRecentMessageIterator(Folder folder, int chunkSize) throws MessagingException
        {
            this.folder = folder;
            this.chunkSize = chunkSize;
            this.messageCount = folder.getMessageCount();
            this.messageNumber = 0;
            fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            _loadChunk();
        }

        private void _loadChunk()
        {
            int chunkSize = Math.min(this.chunkSize, messageCount - messageNumber);
            int[] ids = new int[chunkSize];
            int filler = messageNumber;
            for(int i = 0; i < chunkSize; i++)
            {
                ids[i] = filler++ + 1;
            }
            try
            {
                //System.err.println("Reading messages " + Arrays.toString(ids));
                messages = folder.getMessages(ids);
                folder.fetch(messages, fp);
            }
            catch(MessagingException e)
            {
                throw  new RuntimeException(e);
            }
        }

        @Override
        public boolean hasNext()
        {
            return messageNumber < messageCount;
        }

        @Override
        public Message next()
        {
            int arrayIndex = messageNumber++ % DEFAULT_CHUNK_SIZE;
            Message result = messages[arrayIndex];
            if(arrayIndex == messages.length - 1)
            {
                _loadChunk();
            }
            return result;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
