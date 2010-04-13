/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ethelred.mymailtool;

import java.util.Properties;
import javax.mail.Store;

/**
 *
 * @author edward
 */
abstract class Task {

    private Store store;

    private String folder;

    protected Properties props;

    Task(Properties props)
    {
        this.props = props;
    }

    void setStore(Store store) {
        this.store = store;
    }

    protected Store getStore() {
        return store;
    }

    protected void run() {
        if (store == null || !store.isConnected()) {
            throw new IllegalStateException("Not connected");
        }
        storeRun(store);
    }

    protected String getFolder()
    {
        if(this.folder != null && this.folder.length() > 0)
        {
            return this.folder;
        }
        else
        {
            return props.getProperty(Main.FOLDER);
        }
    }

    protected void setFolder(String folder)
    {
        if(folder != null && folder.trim().length() > 0)
        {
            this.folder = folder.trim();
        }
    }

    protected abstract void storeRun(Store store);
}
