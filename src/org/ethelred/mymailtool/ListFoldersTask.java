/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ethelred.mymailtool;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Store;

/**
 *
 * @author edward
 */
class ListFoldersTask extends Task {

    @Override
    protected void storeRun(Store store) {
        try {
            Folder root = store.getDefaultFolder();
            printFolders(root, "");
        } catch (MessagingException ex) {
            Logger.getLogger(ListFoldersTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void printFolders(Folder folder, String spacing) {
        try {
            System.out.printf("%s- %s%n", spacing, folder.getFullName());
            for (Folder child : folder.list()) {
                printFolders(child, spacing + "  ");
            }
        } catch (MessagingException ex) {
            Logger.getLogger(ListFoldersTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
