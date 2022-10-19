package org.ethelred.mymailtool2;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class SplitTask extends TaskBase {
    private static final Logger LOGGER = LogManager.getLogger();
    private SplitOperation operation = new SplitOperation();
    private final String folderName;

    public SplitTask(String folderName) {
        this.folderName = folderName;
    }

    @Override
    public void run() {
        try {
            traverseFolder(folderName, false, true);
        } catch (MessagingException | IOException e) {
            LOGGER.error("Failed during task run", e);
        }
    }

    @Override
    protected void runMessage(Folder f, Message m) throws MessagingException, IOException {
        context.countMessage();
        if (operation.apply(context, m)) {
            context.countOperation();
        }
    }

    @Override
    protected void status(Folder f) {
        LOGGER.info("Split folder {}", f);
    }

    @Override
    protected int openMode() {
        return Folder.READ_WRITE;
    }
}
