package org.jvnet.hudson.l10n;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * @author Kohsuke Kawaguchi
 */
public class WebAppMain implements ServletContextListener {
    public void contextInitialized(ServletContextEvent sce) {
        sce.getServletContext().setAttribute("app",new App());
    }

    public void contextDestroyed(ServletContextEvent sce) {
    }
}
