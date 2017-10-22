package org.microfuse.file.sharer.node.ui.backend.core.utils;

import org.microfuse.file.sharer.node.core.FileSharer;

public class FileSharerHolder {
    private static FileSharer instance;

    public static FileSharer getFileSharer() {
        if (instance == null) {
            instance = new FileSharer();
        }
        return instance;
    }

    private FileSharerHolder() {    // Preventing instantiation
    }
}
