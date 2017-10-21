package org.microfuse.file.sharer.node.ui.backend.core;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.microfuse.file.sharer.node.core.FileSharer;
import org.microfuse.file.sharer.node.ui.backend.commons.ServerConstants;
import org.microfuse.file.sharer.node.ui.backend.core.api.QueryEndPoint;
import org.microfuse.file.sharer.node.ui.backend.core.filter.CORSFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.ServletException;

/**
 * Launcher for the node.
 */
public class ServerLauncher {
    private static final Logger logger = LoggerFactory.getLogger(ServerLauncher.class);

    private static final String MAIN_SERVLET_NAME = "main-servlet";
    private static final String CORS_FILTER_NAME = "cors-filter";
    private static final Class<?>[] endpointClassList = new Class<?>[]{
            QueryEndPoint.class
    };

    public static void main(String[] args) {
        FileSharer fileSharer = new FileSharer();
        fileSharer.start();
        startTomcatServer();
    }

    private static void startTomcatServer() {
        Thread thread = new Thread(() -> {
            try {
                Tomcat tomcat = new Tomcat();
                tomcat.setPort(ServerConstants.WEB_APP_PORT);

                // Adding the main servlet
                Context context = tomcat.addWebapp("", new File(ServerConstants.WEB_APP_DIRECTORY).getAbsolutePath());
                ServletContainer servletContainer = new ServletContainer(new ResourceConfig(endpointClassList));
                Tomcat.addServlet(context, MAIN_SERVLET_NAME, servletContainer);
                context.addServletMapping(ServerConstants.WEB_APP_API_URL + "/*", MAIN_SERVLET_NAME);

                // Creating CORS filter definition
                FilterDef corsFilterDef = new FilterDef();
                corsFilterDef.setFilterName(CORS_FILTER_NAME);
                corsFilterDef.setFilterClass(CORSFilter.class.getCanonicalName());
                context.addFilterDef(corsFilterDef);

                // Creating CORS filter mapping for main servlet
                FilterMap mainServletFilter1mapping = new FilterMap();
                mainServletFilter1mapping.setFilterName(CORS_FILTER_NAME);
                mainServletFilter1mapping.addURLPattern(ServerConstants.WEB_APP_API_URL + "/*");
                context.addFilterMap(mainServletFilter1mapping);

                tomcat.start();

                String appURI = "http://localhost:" + ServerConstants.WEB_APP_PORT + "/";
                logger.info("File Sharer running at " + appURI);
                try {
                    Desktop.getDesktop().browse(new URI(appURI));
                } catch (IOException | URISyntaxException ignored) {
                }

                tomcat.getServer().await();
            } catch (LifecycleException | ServletException e) {
                logger.error("Failed to start server : " + e.getMessage());
            }
        });
        thread.setPriority(Thread.NORM_PRIORITY);
        thread.start();
    }
}
