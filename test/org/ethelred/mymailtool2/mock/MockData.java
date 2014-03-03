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
public class MockData
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
            catch(Exception e)
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


    public void addFolder(String sName)
    {
        sName = _checkName(sName);
        if(!folderMessages.containsKey(sName))
        {
            folderMessages.put(sName, Lists.<MockMessage>newArrayList());
        }
    }

    public int folderSize(String sName)
    {
        sName = _checkName(sName);
        if(folderMessages.containsKey(sName))
        {
            return folderMessages.get(sName).size();
        }
        return -1;
    }

    public void addMessage(String folder, MockMessage message)
    {
        folder = _checkName(folder);
        addFolder(folder);
        folderMessages.get(folder).add(message);
        Collections.sort(folderMessages.get(folder), DATE_SORT);
                        //System.err.println(folder + " add message " + message);
    }

    public boolean hasFolder(String name)
    {
        return folderMessages.containsKey(_checkName(name));
    }

    private String _checkName(String name)
    {
        if(MockStore.DEFAULT_FOLDER_NAME.equalsIgnoreCase(name))
        {
            return MockStore.DEFAULT_FOLDER_NAME;
        }
        return name.toLowerCase();
    }

    public MockMessage getMessage(String folderName, int index)
    {
        folderName = _checkName(folderName);
        if(folderMessages.containsKey(folderName))
        {
            return folderMessages.get(folderName).get(index - 1);
        }
        return null;
    }

    public boolean deleteMessage(String folderName, MockMessage mockMessage)
    {
        folderName = _checkName(folderName);
        if(folderMessages.containsKey(folderName))
        {
            List<MockMessage> messages = folderMessages.get(folderName);
            return messages.remove(mockMessage);
        }
        return false;  //To change body of created methods use File | Settings | File Templates.
    }

    public static void clear()
    {
        getInstance().folderMessages.clear();
    }

    private static class SingletonHolder
    {
        public static final MockData INSTANCE = new MockData();
    }

    private MockData()
    {
    }
}
