package org.ethelred.mymailtool2;

import java.io.File;

public abstract class BaseFileConfiguration implements MailToolConfiguration {
    private final File file;

    protected BaseFileConfiguration(File file) {
        this.file = file;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' + _shortFile(file) + '}';
    }

    private String _shortFile(File file) {
        var s = file.toString();
        var home = System.getProperty("user.home");
        if (home != null && s.startsWith(home)) {
            return s.replace(home, "~");
        }
        return s;
    }
}
