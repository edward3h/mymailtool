/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ethelred.mymailtool2.javascript;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.ethelred.mymailtool2.FileConfigurationHandler;
import org.ethelred.mymailtool2.MailToolConfiguration;
import org.ethelred.mymailtool2.Task;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 *
 * @author edward
 */
class JavascriptFileConfiguration implements MailToolConfiguration
{
    private IJSObject config;

    private Context ctx;
    private static final String SETUP_CALLBACK = "for(var fn in callback) {\n" +
            "  if(typeof callback[fn] === 'function') {\n" +
            "    this[fn] = (function() {\n" +
            "      var method = callback[fn];\n" +
            "      return function() {\n" +
            "         return method.apply(callback,arguments);\n" +
            "      };\n" +
            "    })();\n" +
            "  }\n" +
            "}";

    public JavascriptFileConfiguration(File f) throws IOException
    {
        ctx = Context.enter();
        Scriptable scope = ctx.initStandardObjects();
        ScriptableObject.putProperty(scope, "callback", Context.javaToJS(new Callback(), scope));
        ctx.evaluateString(scope, SETUP_CALLBACK, "setup", 1, null);
        ctx.evaluateReader(scope, new FileReader(f), f.getName(), 1, null);
    }

    public class Callback
    {
        @SuppressWarnings("UnusedDeclaration")
        public void config(Object conf)
        {
            config = JSObjectWrapper.wrap(conf);
        }
    }

    @Override
    public String getPassword()
    {
        return config.getString("mymailtool.password");
    }

    @Override
    public Map<String, String> getMailProperties()
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for(String k: ALL_MAIL_PROPERTIES) 
        {
            String v = config.getString(k);
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
        return config.getString(USER);
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
        return config.getString("minage");
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
