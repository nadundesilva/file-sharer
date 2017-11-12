package org.microfuse.file.sharer.node.ui.backend.core.utils;

import org.microfuse.file.sharer.node.core.FileSharer;
import org.microfuse.file.sharer.node.core.FileSharerTracer;

/**
 * Singleton file sharer and tracer instance holder.
 */
public class FileSharerHolder {
    private static FileSharer fileSharer;
    private static FileSharerTracer tracer;
    private static FileSharerMode mode;

    /**
     * Get a singleton instance of the file sharer.
     *
     * @return The file sharer singleton instance
     */
    public static synchronized FileSharer getFileSharer() {
        if (fileSharer == null && mode == FileSharerMode.FILE_SHARER) {
            fileSharer = new FileSharer();
        }
        return fileSharer;
    }

    /**
     * Get a singleton instance of the tracer.
     *
     * @return The tracer singleton instance
     */
    public static synchronized FileSharerTracer getTracer() {
        if (tracer == null && mode == FileSharerMode.TRACER) {
            tracer = new FileSharerTracer();
        }
        return tracer;
    }

    public static synchronized void setMode(FileSharerMode mode) {
        FileSharerHolder.mode = mode;
    }

    public static synchronized FileSharerMode getMode() {
        return mode;
    }

    private FileSharerHolder() {    // Preventing instantiation
    }
}
