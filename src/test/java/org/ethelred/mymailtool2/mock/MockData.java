package org.ethelred.mymailtool2.mock;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import org.ethelred.util.ClockFactory;
import org.ethelred.util.MapWithDefault;

import javax.annotation.Nullable;

/**
 * singleton to configure mock folders + messages
 */
public final class MockData
{
    private static final Comparator<? super MockMessage> DATE_SORT = Ordering.natural().onResultOf(new Function<MockMessage, Comparable>()
    {
        @Override
        public Comparable apply(@Nullable MockMessage mockMessage)
        {
            try
            {
                return mockMessage.getDate();
            }
            catch (Exception e)
            {
                throw new IllegalStateException("Bad date in message " + mockMessage);
            }

        }
    });

    public static MockData getInstance()
    {
        return SingletonHolder.INSTANCE;
    }

    private Map<String, List<MockMessage>> folderMessages = Maps.newHashMap();
    private Map<String, Long> uidValidity = Maps.newHashMap();
    private Map<String, Long> nextUid = Maps.newHashMap();
    private Map<String, Boolean> uidCapable = Maps.newHashMap();
    private Map<String, Boolean> uidNextFailure = Maps.newHashMap();

    private static final long DEFAULT_UID_VALIDITY = 1L;


    public void addFolder(String sName)
    {
        sName = _checkName(sName);
        if (!folderMessages.containsKey(sName))
        {
            folderMessages.put(sName, Lists.<MockMessage>newArrayList());
        }
    }

    public int folderSize(String sName)
    {
        sName = _checkName(sName);
        if (folderMessages.containsKey(sName))
        {
            return folderMessages.get(sName).size();
        }
        return -1;
    }

    public void addMessage(String folder, MockMessage message)
    {
        folder = _checkName(folder);
        addFolder(folder);
        long assignedUid = nextUid.getOrDefault(folder, 1L);
        message.setUid(assignedUid);
        nextUid.put(folder, assignedUid + 1);
        folderMessages.get(folder).add(message);
        Collections.sort(folderMessages.get(folder), DATE_SORT);
                        //System.err.println(folder + " add message " + message);
    }

    public long getUidValidity(String folderName)
    {
        folderName = _checkName(folderName);
        return uidValidity.getOrDefault(folderName, DEFAULT_UID_VALIDITY);
    }

    public void setUidValidity(String folderName, long value)
    {
        folderName = _checkName(folderName);
        uidValidity.put(folderName, value);
    }

    public long getUidNext(String folderName)
    {
        folderName = _checkName(folderName);
        return nextUid.getOrDefault(folderName, 1L);
    }

    public boolean isUidCapable(String folderName)
    {
        folderName = _checkName(folderName);
        return uidCapable.getOrDefault(folderName, false);
    }

    public void setUidCapable(String folderName, boolean capable)
    {
        folderName = _checkName(folderName);
        uidCapable.put(folderName, capable);
    }

    /**
     * Test hook: when set, {@link MockUidFolder#getUIDNext()} for {@code folderName} throws a
     * {@link jakarta.mail.MessagingException} instead of returning normally, simulating a
     * transient IMAP failure at exactly that call (e.g. the snapshot taken inside
     * {@code UidRangeMessageIterable}'s constructor).
     */
    public void setUidNextFailure(String folderName, boolean fail)
    {
        folderName = _checkName(folderName);
        uidNextFailure.put(folderName, fail);
    }

    public boolean shouldFailUidNext(String folderName)
    {
        folderName = _checkName(folderName);
        return uidNextFailure.getOrDefault(folderName, false);
    }

    public MockMessage findMessageByUid(String folderName, long uid)
    {
        folderName = _checkName(folderName);
        List<MockMessage> messages = folderMessages.get(folderName);
        if (messages == null)
        {
            return null;
        }
        for (MockMessage m : messages)
        {
            if (m.getUid() == uid)
            {
                return m;
            }
        }
        return null;
    }

    public List<MockMessage> getMessagesByUidRange(String folderName, long startUid, long endUidInclusive)
    {
        folderName = _checkName(folderName);
        List<MockMessage> messages = folderMessages.get(folderName);
        List<MockMessage> result = Lists.newArrayList();
        if (messages == null)
        {
            return result;
        }
        long end = endUidInclusive;
        if (end == jakarta.mail.UIDFolder.LASTUID)
        {
            end = Long.MAX_VALUE;
        }
        for (MockMessage m : messages)
        {
            if (m.getUid() >= startUid && m.getUid() <= end)
            {
                result.add(m);
            }
        }
        Collections.sort(result, Comparator.comparingLong(MockMessage::getUid));
        return result;
    }

    /**
     * Returns the 1-based sequence number of a message within a folder, as used by
     * {@link MockFolder#getMessage(int)}, or -1 if not found.
     */
    public int indexOfMessage(String folderName, MockMessage message)
    {
        folderName = _checkName(folderName);
        List<MockMessage> messages = folderMessages.get(folderName);
        if (messages == null)
        {
            return -1;
        }
        int idx = messages.indexOf(message);
        return idx < 0 ? -1 : idx + 1;
    }

    public boolean hasFolder(String name)
    {
        return folderMessages.containsKey(_checkName(name));
    }

    private String _checkName(String name)
    {
        if (MockStore.DEFAULT_FOLDER_NAME.equalsIgnoreCase(name))
        {
            return MockStore.DEFAULT_FOLDER_NAME;
        }
        return name.toLowerCase();
    }

    public MockMessage getMessage(String folderName, int index)
    {
        folderName = _checkName(folderName);
        if (folderMessages.containsKey(folderName))
        {
            return folderMessages.get(folderName).get(index - 1);
        }
        return null;
    }

    public boolean deleteMessage(String folderName, MockMessage mockMessage)
    {
        folderName = _checkName(folderName);
        if (folderMessages.containsKey(folderName))
        {
            List<MockMessage> messages = folderMessages.get(folderName);
            return messages.remove(mockMessage);
        }
        return false;  //To change body of created methods use File | Settings | File Templates.
    }

    public static void clear()
    {
        getInstance().folderMessages.clear();
        getInstance().uidValidity.clear();
        getInstance().nextUid.clear();
        getInstance().uidCapable.clear();
        getInstance().uidNextFailure.clear();
    }

    private static final class SingletonHolder
    {
        public static final MockData INSTANCE = new MockData();

        private SingletonHolder() {
        }
    }

    private MockData()
    {
    }
}
