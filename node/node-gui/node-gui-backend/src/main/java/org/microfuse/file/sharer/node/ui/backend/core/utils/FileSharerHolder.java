package org.microfuse.file.sharer.node.ui.backend.core.utils;

import org.microfuse.file.sharer.node.core.FileSharer;

/**
 * Singleton file sharer instance holder.
 */
public class FileSharerHolder {
    private static FileSharer instance;

    public static synchronized FileSharer getFileSharer() {
        if (instance == null) {
            instance = new FileSharer();
        }
        return instance;
    }

    private FileSharerHolder() {    // Preventing instantiation
    }
}
