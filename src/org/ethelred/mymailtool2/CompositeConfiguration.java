package org.ethelred.mymailtool2;

import com.google.common.base.Function;
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

    CompositeConfiguration(MailToolConfiguration... initial)
    {
        configs = Lists.newArrayList(initial);
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

    private static Function<MailToolConfiguration, Iterable<String>> FILE_LOCATIONS_ACCESSOR
            = new Function<MailToolConfiguration, Iterable<String>>() {

        public Iterable<String> apply(MailToolConfiguration f)
        {
            return f.getFileLocations();
        }
                
            };
    public Iterable<String> getFileLocations()
    {
        // we want the iterators to report files as they are added by other files
        return new LazyCombinedIterable(FILE_LOCATIONS_ACCESSOR);
    }

    public int getOperationLimit()
    {
        Object v = first("getOperationLimit");
        if(v instanceof Integer)
        {
            return ((Integer) v).intValue();
        }
        return 0;
    }

    public String getMinAge()
    {
        return (String) first("getMinAge");
    }

    private boolean isPrimitiveDefault(Object result)
    {
        return result instanceof Number && ((Number) result).intValue() == PRIMITIVE_DEFAULT;
    }

        private static Function<MailToolConfiguration, Iterable<FileConfigurationHandler>> FILE_HANDLERS_ACCESSOR
            = new Function<MailToolConfiguration, Iterable<FileConfigurationHandler>>() {

        public Iterable<FileConfigurationHandler> apply(MailToolConfiguration f)
        {
            return f.getFileHandlers();
        }
                
            };
    public Iterable<FileConfigurationHandler> getFileHandlers()
    {
        return new LazyCombinedIterable(FILE_HANDLERS_ACCESSOR);
    }
    
    private class LazyCombinedIterable<T> implements Iterable<T>
    {
        private final Function<MailToolConfiguration, Iterable<T>> accessor;
        
        LazyCombinedIterable(Function<MailToolConfiguration, Iterable<T>> accessor)
        {
            this.accessor = accessor;
        }

        public Iterator<T> iterator()
        {
            return new LazyCombinedIterator<T>(accessor);
        }
        
    }
    
    private class LazyCombinedIterator<T> implements Iterator<T>
    {
        private final Function<MailToolConfiguration, Iterable<T>> accessor;
        int cindex = 0;
        Iterator<T> current;
        
        LazyCombinedIterator(Function<MailToolConfiguration, Iterable<T>> accessor)
        {
            this.accessor = accessor;
        }

        public boolean hasNext()
        {
            while((current == null || !current.hasNext()) && cindex < configs.size())
            {
                current = accessor.apply(configs.get(cindex++)).iterator();
            }
            return current == null ? false : current.hasNext();
        }

        public T next()
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
                    if(result != null && !isPrimitiveDefault(result))
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
