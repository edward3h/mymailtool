/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ethelred.mymailtool2.javascript;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.ethelred.mymailtool2.FileConfigurationHandler;
import org.ethelred.mymailtool2.MailToolConfiguration;
import org.ethelred.mymailtool2.Task;
import sun.org.mozilla.javascript.internal.Scriptable;

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

    public String getPassword()
    {
        return topLevel.getString("mymailtool.password");
    }

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

    public String getUser()
    {
        return topLevel.getString(USER);
    }

    public Iterable<String> getFileLocations()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Task getTask() throws Exception
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getOperationLimit()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getMinAge()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Iterable<FileConfigurationHandler> getFileHandlers()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
