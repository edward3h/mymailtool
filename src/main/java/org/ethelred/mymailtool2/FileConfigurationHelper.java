package org.ethelred.mymailtool2;

import com.google.common.collect.MapMaker;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author edward
 */
public final class FileConfigurationHelper
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static Map<String, FileConfigurationHandler> handlers = 
            new MapMaker().makeMap();
    
    private static Pattern commentPattern = Pattern.compile(".* (\\p{Alnum}+) .*");

    static void loadFileConfiguration(CompositeConfiguration temp, String fileLocation)
    {
        // allow configurations to override the set of handlers
        for (FileConfigurationHandler h : temp.getFileHandlers())
        {
            registerHandler(h);
        }
        
        // does file exist?
        File f = new File(fileLocation);
        if (!f.exists() || !f.canRead())
        {
            return;
        }
        FileConfigurationHandler handler = null;
        // does file have an extension?
        String ext = getFileExtension(f);
        if (ext != null)
        {
            handler = handlers.get(ext);
        }
        
        // does file have special comment?
        if (handler == null)
        {
            String firstLine = readFirstLine(f);
            Matcher m = commentPattern.matcher(firstLine);
            if (m.matches())
            {
                ext = m.group(1);
                handler = handlers.get(ext);
            }
        }
        
        if (handler != null)
        {
            MailToolConfiguration conf = handler.readConfiguration(f);
            if (conf != null)
            {
                temp.insert(conf);
            }
        }
    }
    
    static void registerHandler(FileConfigurationHandler h)
    {
        for (String ext : h.getExtensions())
        {
            if (!handlers.containsKey(ext))
            {
                LOGGER.info("register handler for {} {}", ext, h);
                handlers.put(ext, h);
            }
        }
    }
    
    private static String readFirstLine(File f)
    {
        String line = null;
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(f));
            line = br.readLine();
        }
                catch (IOException e)
                {
                    LOGGER.error("Unknown", e);
                }
        finally
        {
            try
            {
                if (br != null)
                {
                    br.close();
                }
            }
            catch (IOException ex)
            {
                LOGGER.error("Unknown", ex);
            }
        }
         
        return line;
    }
    
    private static Pattern fileExtPattern = Pattern.compile("\\.?.+\\.(\\p{Alnum}+)");
    private static String getFileExtension(File f)
    {
        Matcher m = fileExtPattern.matcher(f.getName());
        if (m.matches())
        {
            return m.group(1);
        }
        return null;
    }
    
    
    public static FileConfigurationHandler getHandlerForClassName(String className)
    {
        try
        {
            return instantiateHandler(className);
        }
        catch (ClassNotFoundException e)
        {
            try
            {
                return instantiateHandler(FileConfigurationHelper.class.getPackage().getName() + "." + className);
            }
            catch (ClassNotFoundException e2)
            {
                LOGGER.error(String.format("Could not find class %s or %s", className, FileConfigurationHelper.class.getPackage().getName() + "." + className));
            }
        }
        return null;
    }
    
    
    private static FileConfigurationHandler instantiateHandler(String className) throws ClassNotFoundException
    {
        try
        {
            Class<? extends FileConfigurationHandler> klass = 
                    (Class<? extends FileConfigurationHandler>) Class.forName(className);
            return klass.newInstance();
        }
        catch (InstantiationException ex)
        {
            LOGGER.error("Unknown", ex);
        }
        catch (IllegalAccessException ex)
        {
            LOGGER.error("Unknown", ex);
        }
        return null;
    }

    private FileConfigurationHelper() {
    }


}
