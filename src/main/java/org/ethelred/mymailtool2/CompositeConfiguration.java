package org.ethelred.mymailtool2;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author edward
 */
class CompositeConfiguration implements MailToolConfiguration
{
    private static final Logger LOGGER = LogManager.getLogger();
    /**
     * most important at the start of the list, least important (i.e. built in defaults) at the end
     */
    private List<MailToolConfiguration> configs;
    private List<MailToolConfiguration> iterationConfigs; // same but inserts at end

    CompositeConfiguration(MailToolConfiguration... initial)
    {
        configs = Lists.newArrayList(initial);
        iterationConfigs = Lists.newArrayList(initial);
    }

    @Override
    public String getPassword()
    {
        return (String) first("getPassword");
    }

    @Override
    public Map<String, String> getMailProperties()
    {
        Map<String, String> combined = Maps.newHashMap();
        for(MailToolConfiguration conf: Lists.reverse(configs))
        {
            combined.putAll(conf.getMailProperties());
        }
        return combined;
    }

    @Override
    public String getUser()
    {
        return (String) first("getUser");
    }

    void insert(MailToolConfiguration fileConfig)
    {
        // adds just above the bottom 
        configs.add(configs.size() - 1, fileConfig);
        iterationConfigs.add(fileConfig); // adds at end
    }

    private static Function<MailToolConfiguration, Iterable<String>> FILE_LOCATIONS_ACCESSOR
            = new Function<MailToolConfiguration, Iterable<String>>() {

        @Override
        public Iterable<String> apply(MailToolConfiguration f)
        {
            return f.getFileLocations();
        }
                
            };
    @Override
    public Iterable<String> getFileLocations()
    {
        // we want the iterators to report files as they are added by other files
        return new LazyCombinedIterable(FILE_LOCATIONS_ACCESSOR);
    }

    @Override
    public int getOperationLimit()
    {
        Object v = first("getOperationLimit");
        if(v instanceof Integer)
        {
            return (Integer) v;
        }
        return PRIMITIVE_DEFAULT;
    }

    @Override
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

        @Override
        public Iterable<FileConfigurationHandler> apply(MailToolConfiguration f)
        {
            return f.getFileHandlers();
        }
                
            };
    @Override
    public Iterable<FileConfigurationHandler> getFileHandlers()
    {
        return new LazyCombinedIterable(FILE_HANDLERS_ACCESSOR);
    }

    @Override
    public String getTimeLimit()
    {
        return (String) first("getTimeLimit");
    }

    @Override
    public boolean verbose()
    {
        for(MailToolConfiguration subConf: configs)
        {
            if(subConf.verbose())
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean randomTraversal() {

        for(MailToolConfiguration subConf: configs)
        {
            if(subConf.randomTraversal())
            {
                return true;
            }
        }
        return false;
    }

    private class LazyCombinedIterable<T> implements Iterable<T>
    {
        private final Function<MailToolConfiguration, Iterable<T>> accessor;
        
        LazyCombinedIterable(Function<MailToolConfiguration, Iterable<T>> accessor)
        {
            this.accessor = accessor;
        }

        @Override
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

        @Override
        public boolean hasNext()
        {
            while((current == null || !current.hasNext()) && cindex < iterationConfigs.size())
            {
                current = accessor.apply(iterationConfigs.get(cindex++)).iterator();
            }
            return current != null && current.hasNext();
        }

        @Override
        public T next()
        {
            if(hasNext())
            {
                return current.next();
            }
            return null;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    
    }

    @Override
    public Task getTask()
    {
        return (Task) first("getTask");
    }

    @Override
    public int getChunkSize()
    {
        Object v = first("getChunkSize");
        if(v instanceof Integer)
        {
            return (Integer) v;
        }
        return PRIMITIVE_DEFAULT;
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
                    LOGGER.error("Unknown", ex);
                }
                catch (IllegalArgumentException ex)
                {
                    LOGGER.error("Unknown", ex);
                }
                catch (InvocationTargetException ex)
                {
                    LOGGER.error("Unknown", ex);
                }
            }
            
        }
        catch (NoSuchMethodException ex)
        {
            LOGGER.error("Unknown", ex);
        }
        catch (SecurityException ex)
        {
            LOGGER.error("Unknown", ex);
        }
        
        return null;
        
    }
    
}
