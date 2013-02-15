package org.ethelred.mymailtool2;

import java.util.Arrays;
import java.util.Iterator;
import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

import com.google.common.base.Joiner;

/**
 * iterate over messages in a folder in reverse order (most recent first)
 */
public class RecentMessageIterable implements Iterable<Message>
{
    private final Folder folder;
    private final boolean newestFirst;

    public RecentMessageIterable(Folder f, boolean newestFirst)
    {
        this.folder = f;
        this.newestFirst = newestFirst;
    }

    @Override
    public Iterator<Message> iterator()
    {
        try
        {
            if(newestFirst)
            {
                return new NewestFirstRecentMessageIterator(folder);
            }
            else
            {
                return new OldestFirstRecentMessageIterator(folder);
            }
        }
        catch(MessagingException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static class NewestFirstRecentMessageIterator implements Iterator<Message>
    {
        private final static int MAX_CHUNK = 100;
        private final Folder folder;
        private int messageNumber;
        private int arrayIndex;
        private Message[] messages;
        private FetchProfile fp;

        public NewestFirstRecentMessageIterator(Folder folder) throws MessagingException
        {
            this.folder = folder;
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
            int chunkSize = Math.min(MAX_CHUNK, messageNumber);
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
            return messageNumber > 0;
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
        private final static int MAX_CHUNK = 100;
        private final Folder folder;
        private int messageNumber;
        private final int messageCount;
        private Message[] messages;
        private FetchProfile fp;

        public OldestFirstRecentMessageIterator(Folder folder) throws MessagingException
        {
            this.folder = folder;
            this.messageCount = folder.getMessageCount();
            this.messageNumber = 0;
            fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            _loadChunk();
        }

        private void _loadChunk()
        {
            int chunkSize = Math.min(MAX_CHUNK, messageCount - messageNumber);
            int[] ids = new int[chunkSize];
            int filler = messageNumber;
            for(int i = 0; i < chunkSize; i++)
            {
                ids[i] = filler++ + 1;
            }
            try
            {
                System.err.println("Reading messages " + Arrays.toString(ids));
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
            int arrayIndex = messageNumber++ % MAX_CHUNK;
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
