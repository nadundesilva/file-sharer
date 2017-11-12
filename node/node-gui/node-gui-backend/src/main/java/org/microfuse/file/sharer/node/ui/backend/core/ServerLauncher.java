package org.microfuse.file.sharer.node.ui.backend.core;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.microfuse.file.sharer.node.ui.backend.commons.ServerConstants;
import org.microfuse.file.sharer.node.ui.backend.core.api.endpoint.*;
import org.microfuse.file.sharer.node.ui.backend.core.filter.CORSFilter;
import org.microfuse.file.sharer.node.ui.backend.core.utils.FileSharerHolder;
import org.microfuse.file.sharer.node.ui.backend.core.utils.FileSharerMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import javax.servlet.ServletException;

/**
 * Launcher for the node.
 */
public class ServerLauncher {
    private static final Logger logger = LoggerFactory.getLogger(ServerLauncher.class);

    private static final String MAIN_SERVLET_NAME = "main-servlet";
    private static final String CORS_FILTER_NAME = "cors-filter";
    private static final Class<?>[] endpointClassList = new Class<?>[]{
            ConfigEndPoint.class, QueryEndPoint.class, OverlayNetworkEndPoint.class, ResourcesEndPoint.class,
            SystemEndPoint.class, TraceEndPoint.class
    };

    public static void main(String[] args) {
        boolean isTracer = false;

        // Reading console parameters
        for (int i = 0; i < args.length;) {
            if (Objects.equals(args[i], ServerConstants.CONSOLE_ARGUMENT_KEY_TRACER)) {
                isTracer = true;
                i += 2;
            }
        }

        if (isTracer) {
            FileSharerHolder.setMode(FileSharerMode.TRACER);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> FileSharerHolder.getTracer().shutdown()));
            FileSharerHolder.getTracer().start();
        } else {
            FileSharerHolder.setMode(FileSharerMode.FILE_SHARER);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> FileSharerHolder.getFileSharer().shutdown()));
            FileSharerHolder.getFileSharer().start();
        }
        startTomcatServer(ServerConstants.WEB_APP_PORT);
    }

    private static void startTomcatServer(int port) {
        Thread thread = new Thread(() -> {
            try {
                Tomcat tomcat = new Tomcat();
                tomcat.setPort(port);
                tomcat.setSilent(true);

                // Creating the context
                Context context = tomcat.addWebapp("", new File(ServerConstants.WEB_APP_DIRECTORY).getAbsolutePath());

                // Adding the main servlet
                ServletContainer servletContainer = new ServletContainer(new ResourceConfig(endpointClassList));
                tomcat.addServlet("", MAIN_SERVLET_NAME, servletContainer);
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

                String appURI = "http://localhost:" + port + "/";
                logger.info("File Sharer running at " + appURI);
                try {
                    Desktop.getDesktop().browse(new URI(appURI));
                } catch (IOException | URISyntaxException ignored) {
                }

                tomcat.getServer().await();
            } catch (LifecycleException | ServletException e) {
                logger.error("Failed to startInThread server : " + e.getMessage());
            }
        });
        thread.setPriority(Thread.NORM_PRIORITY);
        thread.start();
    }
}
