package org.microfuse.file.sharer.node.server;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.microfuse.file.sharer.node.server.api.QueryEndPoint;
import org.microfuse.file.sharer.node.server.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.ServletException;

/**
 * The Server Launcher Class.
 */
public class ServerLauncher {
    private static final Logger logger = LoggerFactory.getLogger(ServerLauncher.class);

    private static final String MAIN_SERVLET_MAIN = "main-servlet";
    private static final Class<?>[] endpointClassList = new Class<?>[]{
            QueryEndPoint.class
    };

    public static void main(String[] args) {
        startTomcatServer();
    }

    private static void startTomcatServer() {
        try {
            Tomcat tomcat = new Tomcat();
            tomcat.setPort(Constants.WEB_APP_PORT);

            // Adding the main servlet
            Context context = tomcat.addWebapp("", new File(Constants.WEB_APP_DIRECTORY).getAbsolutePath());
            ServletContainer servletContainer = new ServletContainer(new ResourceConfig(endpointClassList));
            Tomcat.addServlet(context, MAIN_SERVLET_MAIN, servletContainer);
            context.addServletMapping(Constants.WEB_APP_API_URL + "/*", MAIN_SERVLET_MAIN);

            tomcat.start();

            String appURI = "http://localhost:" + Constants.WEB_APP_PORT + "/";
            logger.info("File Sharer running at " + appURI);
            try {
                Desktop.getDesktop().browse(new URI(appURI));
            } catch (IOException | URISyntaxException ignored) {
            }

            tomcat.getServer().await();
        } catch (LifecycleException | ServletException e) {
            logger.error("Failed to start server : " + e.getMessage());
        }
    }
}
