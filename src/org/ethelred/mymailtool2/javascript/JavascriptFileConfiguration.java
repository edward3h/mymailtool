/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ethelred.mymailtool2.javascript;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import javax.script.*;

import com.google.common.collect.ImmutableMap;
import org.ethelred.mymailtool2.FileConfigurationHandler;
import org.ethelred.mymailtool2.MailToolConfiguration;
import org.ethelred.mymailtool2.Task;

/**
 *
 * @author edward
 */
class JavascriptFileConfiguration implements MailToolConfiguration
{
    private final static String TLON = "xyzTopLevelObjzyx";
    private final JSObjectAdapter topLevel;

    private final static String INIT_SCRIPT = "";
    
    public JavascriptFileConfiguration(File f) throws ScriptException, IOException
    {
            ScriptEngine se = new ScriptEngineManager().getEngineByName("JavaScript");
            Bindings bindings = se.getBindings(ScriptContext.ENGINE_SCOPE);
            se.eval(INIT_SCRIPT);
            se.eval(new FileReader(f));
            se.eval("var " + TLON + " = {}; for(p in this){if(p != '" + TLON + "'){" + TLON + "[p] = this[p];}};"); // copy top level properties into an object we can access
            topLevel = JSObjectAdapter.wrap(bindings.get(TLON));
        
    }

    @Override
    public String getPassword()
    {
        return topLevel.getString("mymailtool.password");
    }

    @Override
    public Map<String, String> getMailProperties()
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for(String k: ALL_MAIL_PROPERTIES) 
        {
            String v = topLevel.getString(k);
            if(v != null)
            {
                builder.put(k, v);
            }
        }
        return builder.build();    
    }

    @Override
    public String getUser()
    {
        return topLevel.getString(USER);
    }

    @Override
    public Iterable<String> getFileLocations()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Task getTask() throws Exception
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getOperationLimit()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getMinAge()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Iterable<FileConfigurationHandler> getFileHandlers()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getTimeLimit()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
