/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
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

package org.glassfish.wasp.compiler;

import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.JspFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.tools.JavaFileObject;

import org.glassfish.wasp.Constants;
import org.glassfish.wasp.JspCompilationContext;
import org.glassfish.wasp.Options;
import org.glassfish.wasp.runtime.JspFactoryImpl;
import org.glassfish.wasp.servlet.JspCServletContext;
import org.glassfish.wasp.servlet.JspServletWrapper;

import static java.util.logging.Level.SEVERE;

/**
 * Class for tracking JSP compile time file dependencies when the <code>&lt;@include file="..."&gt;</code> directive is used.
 *
 *<p>
 * A background thread periodically checks the files a JSP page is dependent upon. If a dependent file changes the JSP
 * page which included it is recompiled.
 *
 * Only used if a web application context is a directory.
 *
 * @author Glenn L. Nielsen
 * @version $Revision: 1.13 $
 */
public final class JspRuntimeContext implements Runnable {

    // Logger
    private static Logger log = Logger.getLogger(JspRuntimeContext.class.getName());

    /*
     * Counts how many times the webapp's JSPs have been reloaded.
     */
    private AtomicInteger jspReloadCount = new AtomicInteger(0);

    static {
        JspFactory.setDefaultFactory(new JspFactoryImpl());
    }

    // ----------------------------------------------------------- Constructors

    /**
     * Create a JspRuntimeContext for a web application context.
     *
     * Loads in any previously generated dependencies from file.
     *
     * @param context ServletContext for web application
     */
    public JspRuntimeContext(ServletContext context, Options options) {

        this.context = context;
        this.options = options;

        int hashSize = options.getInitialCapacity();
        jsps = new ConcurrentHashMap<>(hashSize);

        bytecodes = new ConcurrentHashMap<>(hashSize);
        bytecodeBirthTimes = new ConcurrentHashMap<>(hashSize);
        packageMap = new ConcurrentHashMap<>();

        if (log.isLoggable(Level.FINEST)) {
            ClassLoader parentClassLoader = getParentClassLoader();
            if (parentClassLoader != null) {
                log.finest(Localizer.getMessage("jsp.message.parent_class_loader_is", parentClassLoader.toString()));
            } else {
                log.finest(Localizer.getMessage("jsp.message.parent_class_loader_is", "<none>"));
            }
        }

        initClassPath();

        if (context instanceof JspCServletContext) {
            return;
        }

        // If this web application context is running from a
        // directory, start the background compilation thread
        String appBase = context.getRealPath("/");
        if (!options.getDevelopment() && appBase != null && options.getCheckInterval() > 0 && !options.getUsePrecompiled()) {
            if (appBase.endsWith(File.separator)) {
                appBase = appBase.substring(0, appBase.length() - 1);
            }
            String directory = appBase.substring(appBase.lastIndexOf(File.separator));
            threadName = threadName + "[" + directory + "]";
            threadStart();
        }
    }

    // ----------------------------------------------------- Instance Variables

    /**
     * This web applications ServletContext
     */
    private ServletContext context;
    private Options options;
    private String classpath;

    /**
     * Maps JSP pages to their JspServletWrapper's
     */
    private Map<String, JspServletWrapper> jsps;

    /**
     * Maps class names to in-memory bytecodes
     */
    private Map<String, byte[]> bytecodes;
    private Map<String, Long> bytecodeBirthTimes;

    /**
     * Maps classes in packages compiled by the JSP compiler. Used only by Jsr199Compiler.
     */
    private Map<String, Map<String, JavaFileObject>> packageMap;

    /**
     * The background thread.
     */
    private Thread thread;

    /**
     * The background thread completion semaphore.
     */
    private boolean threadDone;

    /**
     * Name to register for the background thread.
     */
    private String threadName = "JspRuntimeContext";

    // ------------------------------------------------------ Public Methods

    /**
     * Add a new JspServletWrapper.
     *
     * @param jspUri JSP URI
     * @param jsw Servlet wrapper for JSP
     */
    public void addWrapper(String jspUri, JspServletWrapper jsw) {
        jsps.remove(jspUri);
        jsps.put(jspUri, jsw);
    }

    /**
     * Get an already existing JspServletWrapper.
     *
     * @param jspUri JSP URI
     * @return JspServletWrapper for JSP
     */
    public JspServletWrapper getWrapper(String jspUri) {
        return jsps.get(jspUri);
    }

    /**
     * Remove a JspServletWrapper.
     *
     * @param jspUri JSP URI of JspServletWrapper to remove
     */
    public void removeWrapper(String jspUri) {
        jsps.remove(jspUri);
    }

    /**
     * Returns the number of JSPs for which JspServletWrappers exist, i.e., the number of JSPs that have been loaded into
     * the webapp.
     *
     * @return The number of JSPs that have been loaded into the webapp
     */
    public int getJspCount() {
        return jsps.size();
    }

    /**
     * Get the parent class loader.
     *
     * @return ClassLoader parent
     */
    public ClassLoader getParentClassLoader() {
        ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
        if (parentClassLoader == null) {
            parentClassLoader = this.getClass().getClassLoader();
        }
        return parentClassLoader;
    }

    /**
     * Process a "destory" event for this web application context.
     */
    public void destroy() {

        threadStop();

        for (JspServletWrapper jsw : jsps.values()) {
            jsw.destroy();
        }
    }

    /**
     * Increments the JSP reload counter.
     */
    public void incrementJspReloadCount() {
        jspReloadCount.incrementAndGet();
    }

    /**
     * Resets the JSP reload counter.
     *
     * @param count Value to which to reset the JSP reload counter
     */
    public void setJspReloadCount(int count) {
        jspReloadCount.set(count);
    }

    /**
     * Gets the current value of the JSP reload counter.
     *
     * @return The current value of the JSP reload counter
     */
    public int getJspReloadCount() {
        return jspReloadCount.get();
    }

    /**
     * Save the bytecode for the class in a map. The current time is noted.
     *
     * @param name The name of the class
     * @param bytecode The bytecode in byte array
     */
    public void setBytecode(String name, byte[] bytecode) {
        if (bytecode == null) {
            bytecodes.remove(name);
            bytecodeBirthTimes.remove(name);
            return;
        }
        bytecodes.put(name, bytecode);
        bytecodeBirthTimes.put(name, System.currentTimeMillis());
    }

    public void adjustBytecodeTime(String name, long reference) {
        Long time = bytecodeBirthTimes.get(name);
        if (time == null) {
            return;
        }

        if (time.longValue() < reference) {
            bytecodeBirthTimes.put(name, reference);
        }
    }

    /**
     * Get the class-name to bytecode map
     */
    public Map<String, byte[]> getBytecodes() {
        return bytecodes;
    }

    /**
     * Retrieve the bytecode associated with the class
     */
    public byte[] getBytecode(String name) {
        return bytecodes.get(name);
    }

    /**
     * Retrieve the time the bytecode for a class was created
     */
    public long getBytecodeBirthTime(String name) {
        Long time = bytecodeBirthTimes.get(name);
        return time != null ? time : 0;
    }

    /**
     * The packageMap keeps track of the bytecode files in a package generated by a java compiler. This is in turn loaded by
     * the java compiler during compilation. This is gets around the fact that JSR199 API does not provide a way for the
     * compiler use current classloader.
     */
    public Map<String, Map<String, JavaFileObject>> getPackageMap() {
        return packageMap;
    }

    /**
     * Save the bytecode for a class to disk.
     */
    public void saveBytecode(String className, String classFileName) {
        byte[] bytecode = getBytecode(className);
        if (bytecode != null) {
            try {
                FileOutputStream fos = new FileOutputStream(classFileName);
                fos.write(bytecode);
                fos.close();
            } catch (IOException ex) {
                context.log("Error in saving bytecode for " + className + " to " + classFileName, ex);
            }
        }
    }

    // -------------------------------------------------------- Private Methods

    /**
     * Method used by background thread to check the JSP dependencies registered with this class for JSP's.
     */
    private void checkCompile() {
        for (JspServletWrapper jsw : jsps.values()) {
            if (jsw.isTagFile()) {
                // Skip tag files in background compiliations, since modified
                // tag files will be recompiled anyway when their client JSP
                // pages are compiled. This also avoids problems when the
                // tag files and their clients are not modified simultaneously.
                continue;
            }

            JspCompilationContext ctxt = jsw.getJspEngineContext();
            // JspServletWrapper also synchronizes on this when
            // it detects it has to do a reload
            synchronized (jsw) {
                try {
                    ctxt.compile();
                } catch (FileNotFoundException ex) {
                    ctxt.incrementRemoved();
                } catch (Throwable t) {
                    jsw.getServletContext().log(Localizer.getMessage("jsp.error.background.compile"), t);
                }
            }
        }
    }

    /**
     * The classpath that is passed off to the Java compiler.
     */
    public String getClassPath() {
        return classpath;
    }

    /**
     * Method used to initialize classpath for compiles.
     */
    private void initClassPath() {

        /*
         * Classpath can be specified in one of two ways, depending on whether the compilation is embedded or invoked from Jspc.
         * 1. Calculated by the web container, and passed to Wasp in the context attribute. 2. Jspc directly invoke
         * JspCompilationContext.setClassPath, in case the classPath initialzed here is ignored.
         */

        StringBuilder cpath = new StringBuilder();
        String sep = System.getProperty("path.separator");

        cpath.append(options.getScratchDir() + sep);

        String cp = (String) context.getAttribute(Constants.SERVLET_CLASSPATH);
        if (cp == null || cp.equals("")) {
            cp = options.getClassPath();
        }

        if (cp != null) {
            classpath = cpath.toString() + cp;
        }

        // START GlassFish Issue 845
        if (classpath != null) {
            try {
                classpath = URLDecoder.decode(classpath, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                if (log.isLoggable(Level.FINE)) {
                    log.log(Level.FINE, "Exception decoding classpath : " + classpath, e);
                }
            }
        }
        // END GlassFish Issue 845
    }


    // -------------------------------------------------------- Thread Support

    /**
     * Start the background thread that will periodically check for changes to compile time included files in a JSP.
     *
     * @exception IllegalStateException if we should not be starting a background thread now
     */
    protected void threadStart() {

        // Has the background thread already been started?
        if (thread != null) {
            return;
        }

        // Start the background thread
        threadDone = false;
        thread = new Thread(this, threadName);
        thread.setDaemon(true);
        thread.start();

    }

    /**
     * Stop the background thread that is periodically checking for changes to compile time included files in a JSP.
     */
    protected void threadStop() {

        if (thread == null) {
            return;
        }

        threadDone = true;
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {

        }

        thread = null;

    }

    /**
     * Sleep for the duration specified by the <code>checkInterval</code> property.
     */
    protected void threadSleep() {

        try {
            Thread.sleep(options.getCheckInterval() * 1000L);
        } catch (InterruptedException e) {

        }

    }

    // ------------------------------------------------------ Background Thread

    /**
     * The background thread that checks for changes to files included by a JSP and flags that a recompile is required.
     */
    @Override
    public void run() {

        // Loop until the termination semaphore is set
        while (!threadDone) {

            // Wait for our check interval
            threadSleep();

            // Check for included files which are newer than the
            // JSP which uses them.
            try {
                checkCompile();
            } catch (Throwable t) {
                t.printStackTrace();
                log.log(SEVERE, Localizer.getMessage("jsp.error.recompile"), t);
            }
        }

    }

}
