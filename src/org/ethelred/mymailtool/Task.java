/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ethelred.mymailtool;

import javax.mail.Store;

/**
 *
 * @author edward
 */
abstract class Task {

    private Store store;

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

    protected abstract void storeRun(Store store);
}
