package org.ethelred.mymailtool2;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author edward
 */
class CompositeConfiguration implements MailToolConfiguration
{
    /**
     * most important at the start of the list, least important (i.e. built in defaults) at the end
     */
    private List<MailToolConfiguration> configs;

    CompositeConfiguration(MailToolConfiguration top, MailToolConfiguration bottom)
    {
        configs = Lists.newArrayList(top, bottom);
    }

    public String getPassword()
    {
        return (String) first("getPassword");
    }

    public Map<String, String> getMailProperties()
    {
        Map<String, String> combined = Maps.newHashMap();
        for(MailToolConfiguration conf: Lists.reverse(configs))
        {
            combined.putAll(conf.getMailProperties());
        }
        return combined;
    }

    public String getUser()
    {
        return (String) first("getUser");
    }

    void insert(MailToolConfiguration fileConfig)
    {
        // adds just above the bottom 
        configs.add(configs.size() - 1, fileConfig);
    }

    public Iterable<String> getFileLocations()
    {
        // we want the iterators to report files as they are added by other files
        return new LazyCombinedIterable();
    }

    public int getOperationLimit()
    {
        return (Integer) first("getOperationLimit");
    }

    public String getMinAge()
    {
        return (String) first("getMinAge");
    }
    
    private class LazyCombinedIterable implements Iterable<String>
    {

        public Iterator<String> iterator()
        {
            return new LazyCombinedIterator();
        }
        
    }
    
    private class LazyCombinedIterator implements Iterator<String>
    {
        int cindex = 0;
        Iterator<String> current;

        public boolean hasNext()
        {
            while((current == null || !current.hasNext()) && cindex < configs.size())
            {
                current = configs.get(cindex++).getFileLocations().iterator();
            }
            return current == null ? false : current.hasNext();
        }

        public String next()
        {
            if(hasNext())
            {
                return current.next();
            }
            return null;
        }

        public void remove()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    
    }

    public Task getTask()
    {
        return (Task) first("getTask");
    }
    
    private Object first(String method)
    {
        return first(method, new Class[0], new Object[0]);
    }

    private Object first(String string, Class[] ptypes, Object[] args)
    {
        try
        {
            Method m = MailToolConfiguration.class.getDeclaredMethod(string, ptypes);
            for(MailToolConfiguration subConf: configs)
            {
                try
                {
                    Object result = m.invoke(subConf, args);
                    if(result != null)
                    {
                        return result;
                    }
                }
                catch (IllegalAccessException ex)
                {
                    Logger.getLogger(CompositeConfiguration.class.getName()).log(Level.SEVERE, null, ex);
                }
                catch (IllegalArgumentException ex)
                {
                    Logger.getLogger(CompositeConfiguration.class.getName()).log(Level.SEVERE, null, ex);
                }
                catch (InvocationTargetException ex)
                {
                    Logger.getLogger(CompositeConfiguration.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
        }
        catch (NoSuchMethodException ex)
        {
            Logger.getLogger(CompositeConfiguration.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (SecurityException ex)
        {
            Logger.getLogger(CompositeConfiguration.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
        
    }
    
}
