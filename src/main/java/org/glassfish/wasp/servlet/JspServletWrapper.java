/*
 * Copyright (c) 2021, 2024 Contributors to Eclipse Foundation.
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.glassfish.wasp.servlet;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.jsp.tagext.TagInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import org.glassfish.jsp.api.JspProbeEmitter;
import org.glassfish.wasp.JspCompilationContext;
import org.glassfish.wasp.Options;
import org.glassfish.wasp.WaspException;
import org.glassfish.wasp.compiler.JspRuntimeContext;
import org.glassfish.wasp.compiler.Localizer;
import org.glassfish.wasp.runtime.JspSourceDependent;

import static jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static jakarta.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static java.util.logging.Level.SEVERE;

/**
 * The JSP engine (a.k.a WaSP).
 *
 * The servlet container is responsible for providing a URLClassLoader for the web application context WaSP is being
 * used in. WaSP will try get the ServletContext attribute for its ServletContext class loader, if that fails,
 * it uses the parent class loader. In either case, it must be a URLClassLoader.
 *
 * @author Anil K. Vijendran
 * @author Harish Prabandham
 * @author Remy Maucherat
 * @author Kin-man Chung
 * @author Glenn Nielsen
 */
public class JspServletWrapper {

    // Logger
    private static Logger log = Logger.getLogger(JspServletWrapper.class.getName());

    private Servlet theServlet;
    private String jspUri;
    private Class<?> servletClass;
    private Class<?> tagHandlerClass;
    private JspCompilationContext ctxt;
    private long available = 0L;
    private ServletConfig config;
    private Options options;
    private boolean firstTime = true;
    private boolean reload = true;
    private boolean isTagFile;
    private int tripCount;
    private WaspException compileException;
    private JspProbeEmitter jspProbeEmitter;
    private long servletClassLastModifiedTime = 0L;
    private File jspFile;
    private long lastModificationTest = 0L;

    /*
     * JspServletWrapper for JSP pages.
     */
    JspServletWrapper(ServletConfig config, Options options, String jspUri, boolean isErrorPage, JspRuntimeContext rctxt) throws WaspException {
        this.isTagFile = false;
        this.config = config;
        this.options = options;
        this.jspUri = jspUri;
        this.jspProbeEmitter = (JspProbeEmitter) config.getServletContext().getAttribute("org.glassfish.jsp.monitor.probeEmitter");

        ctxt = new JspCompilationContext(jspUri, isErrorPage, options, config.getServletContext(), this, rctxt);
        String jspFilePath = ctxt.getRealPath(jspUri);
        if (jspFilePath != null) {
            jspFile = new File(jspFilePath);
        }
    }

    /*
     * JspServletWrapper for tag files.
     */
    public JspServletWrapper(ServletContext servletContext, Options options, String tagFilePath, TagInfo tagInfo, JspRuntimeContext rctxt, URL tagFileJarUrl)
            throws WaspException {

        this.isTagFile = true;
        this.config = null; // not used
        this.options = options;
        this.jspUri = tagFilePath;
        this.tripCount = 0;
        ctxt = new JspCompilationContext(jspUri, tagInfo, options, servletContext, this, rctxt, tagFileJarUrl);
    }

    public JspCompilationContext getJspEngineContext() {
        return ctxt;
    }

    public void setReload(boolean reload) {
        this.reload = reload;
    }

    public Servlet getServlet() throws ServletException, IOException, ClassNotFoundException {
        if (reload) {
            synchronized (this) {
                // Synchronizing on jsw enables simultaneous loading
                // of different pages, but not the same page.
                if (reload) {
                    // This is to maintain the original protocol.
                    destroy();

                    try {
                        servletClass = ctxt.load();
                        theServlet = (Servlet) servletClass.getDeclaredConstructor().newInstance();
                    } catch (ReflectiveOperationException ex1) {
                        throw new WaspException(ex1);
                    }

                    theServlet.init(config);

                    if (!firstTime) {
                        ctxt.getRuntimeContext().incrementJspReloadCount();
                        // Fire the jspReloadedEvent probe event
                        if (jspProbeEmitter != null) {
                            jspProbeEmitter.jspReloadedEvent(jspUri);
                        }
                    }

                    reload = false;

                    // Fire the jspLoadedEvent probe event
                    if (jspProbeEmitter != null) {
                        jspProbeEmitter.jspLoadedEvent(jspUri);
                    }
                }
            }
        }
        return theServlet;
    }

    public ServletContext getServletContext() {
        return config.getServletContext();
    }

    /**
     * Sets the compilation exception for this JspServletWrapper.
     *
     * @param je The compilation exception
     */
    public void setCompilationException(WaspException je) {
        this.compileException = je;
    }

    /**
     * Sets the last-modified time of the servlet class file associated with this JspServletWrapper.
     *
     * @param lastModified Last-modified time of servlet class
     */
    public void setServletClassLastModifiedTime(long lastModified) {
        if (this.servletClassLastModifiedTime < lastModified) {
            synchronized (this) {
                if (this.servletClassLastModifiedTime < lastModified) {
                    this.servletClassLastModifiedTime = lastModified;
                    reload = true;
                }
            }
        }
    }

    /**
     * Gets the last-modified time of the servlet class file associated with this JspServletWrapper.
     *
     * @return Last-modified time of servlet class
     */
    public long getServletClassLastModifiedTime() {
        return servletClassLastModifiedTime;
    }

    /**
     * Compile (if needed) and load a tag file
     */
    public Class<?> loadTagFile() throws WaspException {
        try {
            ctxt.compile();
            if (reload) {
                tagHandlerClass = ctxt.load();
            }
        } catch (ClassNotFoundException ex) {
        } catch (FileNotFoundException ex) {
            log.log(SEVERE, Localizer.getMessage("jsp.error.compiling"));
            throw new WaspException(ex);
        }
        return tagHandlerClass;
    }

    /**
     * Compile and load a prototype for the Tag file. This is needed when compiling tag files with circular dependencies. A
     * prototpe (skeleton) with no dependencies on other other tag files is generated and compiled.
     */
    public Class<?> loadTagFilePrototype() throws WaspException {
        ctxt.setPrototypeMode(true);

        try {
            return loadTagFile();
        } finally {
            ctxt.setPrototypeMode(false);
        }
    }

    /**
     * Get a list of files that the current page has source dependency on.
     */
    public List<String> getDependants() {
        try {
            Object target;
            if (isTagFile) {
                if (reload) {
                    tagHandlerClass = ctxt.load();
                }
                target = tagHandlerClass.getDeclaredConstructor().newInstance();
            } else {
                target = getServlet();
            }
            if (target instanceof JspSourceDependent) {
                return ((JspSourceDependent) target).getDependants();
            }
        } catch (Throwable ex) {
        }

        return null;
    }

    public boolean isTagFile() {
        return this.isTagFile;
    }

    public int incTripCount() {
        return tripCount++;
    }

    public int decTripCount() {
        return tripCount--;
    }

    public void service(HttpServletRequest request, HttpServletResponse response, boolean precompile) throws ServletException, IOException {
        try {
            if (ctxt.isRemoved()) {
                jspFileNotFound(request, response);
                return;
            }

            if (available > 0L && available < Long.MAX_VALUE) {
                response.setDateHeader("Retry-After", available);
                response.sendError(SC_SERVICE_UNAVAILABLE, Localizer.getMessage("jsp.error.unavailable"));
            }

            /*
             * (1) Compile
             */
            if (!options.getUsePrecompiled() && (options.getDevelopment() || firstTime)) {
                synchronized (this) {
                    firstTime = false;

                    // The following sets reload to true, if necessary
                    ctxt.compile();
                }
            } else {
                if (compileException != null) {
                    // Throw cached compilation exception
                    throw compileException;
                }
            }

            /*
             * (2) (Re)load servlet class file
             */
            try {
                getServlet();
            } catch (ClassNotFoundException ex) {
                // This can only happen when use-precomiled is set and a
                // supposedly pre-compiled class does not exist.
                jspFileNotFound(request, response);
                return;
            }

            // If a page is to be precompiled only, return.
            if (precompile) {
                return;
            }

            /*
             * (3) Service request
             */
            theServlet.service(request, response);

        } catch (UnavailableException ex) {
            String includeRequestUri = (String) request.getAttribute("jakarta.servlet.include.request_uri");
            if (includeRequestUri != null) {
                // This file was included.
                // Throw an exception as a response.sendError() will be ignored by the servlet engine.
                throw ex;
            }

            int unavailableSeconds = ex.getUnavailableSeconds();
            if (unavailableSeconds <= 0) {
                unavailableSeconds = 60; // Arbitrary default
            }

            available = System.currentTimeMillis() + unavailableSeconds * 1000L;
            response.sendError(SC_SERVICE_UNAVAILABLE, ex.getMessage());

        } catch (ServletException | IOException | IllegalStateException  ex) {
            throw ex;
        } catch (Exception ex) {
            throw new WaspException(ex);
        }
    }

    public void destroy() {
        if (theServlet != null) {
            theServlet.destroy();
            // Fire the jspDestroyedEvent probe event
            if (jspProbeEmitter != null) {
                jspProbeEmitter.jspDestroyedEvent(jspUri);
            }
        }
    }

    /**
     * @return Returns the lastModificationTest.
     */
    public long getLastModificationTest() {
        return lastModificationTest;
    }

    /**
     * @param lastModificationTest The lastModificationTest to set.
     */
    public void setLastModificationTest(long lastModificationTest) {
        this.lastModificationTest = lastModificationTest;
    }

    public File getJspFile() {
        return jspFile;
    }

    /*
     * Handles the case where a requested JSP file no longer exists.
     */
    private void jspFileNotFound(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        FileNotFoundException fnfe = new FileNotFoundException(jspUri);

        ctxt.incrementRemoved();
        String includeRequestUri = (String) request.getAttribute("jakarta.servlet.include.request_uri");
        if (includeRequestUri != null) {
            // This file was included.
            // Throw an exception as a response.sendError() will be ignored by the servlet engine.
            throw new ServletException(fnfe);
        }

        try {
            response.sendError(SC_NOT_FOUND, fnfe.getMessage());
        } catch (IllegalStateException ise) {
            log.log(SEVERE, Localizer.getMessage("jsp.error.file.not.found", fnfe.getMessage()), fnfe);
        }

    }

}
