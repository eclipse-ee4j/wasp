/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
package org.glassfish.wasp.quarkus.runtime;

import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;

import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.JspFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.glassfish.wasp.runtime.JspFactoryImpl;
import org.glassfish.wasp.servlet.JspServlet;
import org.jboss.logging.Logger;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.list;
import static java.util.Collections.unmodifiableSet;
import static org.glassfish.wasp.Constants.JSP_TLD_URI_TO_LOCATION_MAP;
import static org.glassfish.wasp.Constants.JSP_TLD_URI_TO_LOCATION_MAP_AUTHORITATIVE;
import static org.glassfish.wasp.Constants.PRECOMPILED_PAGES_TO_VALIDATE;
import static org.glassfish.wasp.quarkus.runtime.JspFileServlet.JSP_FILE_INIT_PARAM;
import static org.glassfish.wasp.quarkus.runtime.JspFileServlet.LOAD_ON_STARTUP_INIT_PARAM;

/**
 * Integrates WaSP (Jakarta Pages) into the Undertow based Servlet container of Quarkus.
 *
 * <p>
 * The pages are compiled when the application is built. At runtime WaSP's own {@link JspServlet} serves them, with
 * <code>usePrecompiled</code>: it loads the page classes instead of compiling the pages.
 */
public class WaspServletExtension implements ServletExtension {

    private static final Logger LOG = Logger.getLogger(WaspServletExtension.class);

    /** The name of the Servlet that serves the pages, as in GlassFish and Tomcat */
    public static final String JSP_SERVLET_NAME = "jsp";

    /** The URL patterns of the pages, passed in at build time */
    private Set<String> jspUrlPatterns = new LinkedHashSet<>(List.of("*.jsp", "*.jspx"));

    /** The Servlets declared with a jsp-file, passed in at build time */
    private List<JspFileServletDefinition> jspFileServlets = new ArrayList<>();

    /** The pages to validate again before their first use; empty when that's disabled */
    private Set<String> pagesToValidate = new LinkedHashSet<>();

    /** The locations of the tag library descriptors, as resolved when the pages were translated */
    private List<TldLocation> tldLocations = new ArrayList<>();

    /** The files in WEB-INF of the application (not in META-INF/resources), e.g. WEB-INF/tags/a.tag */
    private Set<String> webInfFiles = new LinkedHashSet<>();

    @Override
    public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
        // The factory the generated pages get their page context from. WaSP sets it when it compiles pages, which
        // doesn't happen here.
        JspFactory.setDefaultFactory(new JspFactoryImpl());

        boolean validatePrecompiled = !pagesToValidate.isEmpty();
        if (validatePrecompiled) {
            initValidation(deploymentInfo, servletContext);
        }

        ServletInfo jspServlet = Servlets.servlet(JSP_SERVLET_NAME, JspServlet.class);
        addPrecompiledInitParams(jspServlet, validatePrecompiled);

        // When the URL pattern of a jsp-property-group is identical to a Servlet mapping, the property group takes
        // precedence (Jakarta Pages, JSP.3.3.2); for other patterns the most specific mapping applies, as usual
        for (String pattern : jspUrlPatterns) {
            for (ServletInfo servlet : deploymentInfo.getServlets().values()) {
                if (servlet.getMappings().contains(pattern)) {
                    removeMapping(servlet, pattern);
                }
            }
            for (JspFileServletDefinition definition : jspFileServlets) {
                definition.getMappings().remove(pattern);
            }
            jspServlet.addMapping(pattern);
        }

        deploymentInfo.addServlet(jspServlet);

        // Quarkus doesn't register Servlets declared with a jsp-file. They're registered here, before the deployment
        // initializes, with their name, mappings, init params and security role references (which for instance
        // Jakarta Authorization turns into permissions).
        for (JspFileServletDefinition definition : jspFileServlets) {
            ServletInfo servletInfo = Servlets.servlet(definition.getName(), JspFileServlet.class);

            definition.getInitParams().forEach(servletInfo::addInitParam);
            addPrecompiledInitParams(servletInfo, validatePrecompiled);
            servletInfo.addInitParam(JSP_FILE_INIT_PARAM, definition.getJspFile());
            servletInfo.addMappings(definition.getMappings());

            if (definition.getLoadOnStartup() >= 0) {
                servletInfo.setLoadOnStartup(definition.getLoadOnStartup());
                servletInfo.addInitParam(LOAD_ON_STARTUP_INIT_PARAM, "true");
            }

            definition.getSecurityRoleRefs().forEach(servletInfo::addSecurityRoleRef);

            deploymentInfo.addServlet(servletInfo);
        }
    }

    /**
     * Removes a mapping from a Servlet Quarkus registered from web.xml. Undertow's ServletInfo only exposes its mappings
     * read-only, and the Servlet can't be registered differently, as Quarkus registers it.
     */
    private static void removeMapping(ServletInfo servlet, String pattern) {
        try {
            Field mappings = ServletInfo.class.getDeclaredField("mappings");
            mappings.setAccessible(true);
            ((List<?>) mappings.get(servlet)).remove(pattern);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warnf("Could not give the jsp-property-group URL pattern %s precedence over the mapping of Servlet %s: %s",
                    pattern, servlet.getName(), e);
        }
    }

    /**
     * Configures a WaSP Servlet to load the pages compiled at build time, and never compile.
     *
     * @param validatePrecompiled whether to validate pages again before their first use
     */
    static void addPrecompiledInitParams(ServletInfo servletInfo, boolean validatePrecompiled) {
        servletInfo.addInitParam("usePrecompiled", "true");
        servletInfo.addInitParam("development", "false");
        if (validatePrecompiled) {
            servletInfo.addInitParam("validatePrecompiled", "true");
        }
    }

    /**
     * Tells WaSP which pages to validate again, and where the tag library descriptors they use are. WaSP doesn't scan
     * for them here: its scan only works with a URLClassLoader, and the pages must use the same descriptors as when they
     * were translated.
     */
    private void initValidation(DeploymentInfo deploymentInfo, ServletContext servletContext) {
        ClassLoader classLoader = deploymentInfo.getClassLoader() != null
                ? deploymentInfo.getClassLoader()
                : Thread.currentThread().getContextClassLoader();

        Map<String, String[]> locations = new HashMap<>();
        for (TldLocation tldLocation : tldLocations) {
            String[] location = resolve(tldLocation, classLoader);
            if (location != null) {
                locations.put(tldLocation.getUri(), location);
            }
        }

        deploymentInfo.setResourceManager(new WebInfResourceManager(deploymentInfo.getResourceManager(), webInfFiles, classLoader));

        // On the context itself: Undertow copied the attributes of the deployment info before the extensions ran
        servletContext.setAttribute(PRECOMPILED_PAGES_TO_VALIDATE, unmodifiableSet(pagesToValidate));
        servletContext.setAttribute(JSP_TLD_URI_TO_LOCATION_MAP, locations);
        servletContext.setAttribute(JSP_TLD_URI_TO_LOCATION_MAP_AUTHORITATIVE, TRUE);
    }

    /**
     * @return the location of the descriptor the way WaSP expects it: its path in the web application, or the URL of the
     *         jar file and the name of its entry; null if the jar can't be found
     */
    static String[] resolve(TldLocation tldLocation, ClassLoader classLoader) {
        if (tldLocation.getEntryName() == null) {
            return new String[] { tldLocation.getPath(), null };
        }

        List<String> jarUrls = new ArrayList<>();
        try {
            for (URL url : list(classLoader.getResources(tldLocation.getEntryName()))) {
                String externalForm = url.toExternalForm();
                int separator = externalForm.indexOf("!/");
                if (externalForm.startsWith("jar:") && separator > 0) {
                    jarUrls.add(externalForm.substring("jar:".length(), separator));
                }
            }
        } catch (IOException e) {
            LOG.errorf("Could not find the tag library descriptor %s for %s: %s", tldLocation.getEntryName(), tldLocation.getUri(), e);
            return null;
        }

        // The jar the descriptor was found in at build time; otherwise, the only one with the entry (e.g. an uber-jar)
        String jarUrl = null;
        if (tldLocation.getJarFileName() != null) {
            jarUrl =
                jarUrls.stream()
                       .filter(url -> url.endsWith("/" + tldLocation.getJarFileName()))
                       .findFirst()
                       .orElse(null);
        }

        if (jarUrl == null && jarUrls.size() == 1) {
            jarUrl = jarUrls.get(0);
        }

        if (jarUrl == null) {
            LOG.errorf("Could not find the jar with tag library descriptor %s for %s (found %s); pages using it fail",
                    tldLocation.getEntryName(), tldLocation.getUri(), jarUrls);
            return null;
        }

        return new String[] { jarUrl, tldLocation.getEntryName() };
    }

    public List<JspFileServletDefinition> getJspFileServlets() {
        return jspFileServlets;
    }

    public void setJspFileServlets(List<JspFileServletDefinition> jspFileServlets) {
        this.jspFileServlets = jspFileServlets;
    }

    public Set<String> getPagesToValidate() {
        return pagesToValidate;
    }

    public void setPagesToValidate(Set<String> pagesToValidate) {
        this.pagesToValidate = pagesToValidate;
    }

    public List<TldLocation> getTldLocations() {
        return tldLocations;
    }

    public void setTldLocations(List<TldLocation> tldLocations) {
        this.tldLocations = tldLocations;
    }

    public Set<String> getWebInfFiles() {
        return webInfFiles;
    }

    public void setWebInfFiles(Set<String> webInfFiles) {
        this.webInfFiles = webInfFiles;
    }

    public Set<String> getJspUrlPatterns() {
        return jspUrlPatterns;
    }

    public void setJspUrlPatterns(Set<String> jspUrlPatterns) {
        this.jspUrlPatterns = jspUrlPatterns;
    }
}
