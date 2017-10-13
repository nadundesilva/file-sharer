package org.microfuse.file.sharer.bootstrap;

/**
 * Launcher for the bootstrap server.
 */
public class ServerLauncher {
    public static void main(String[] args) {
        new BootstrapServer().start();
    }
}
