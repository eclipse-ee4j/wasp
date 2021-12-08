/*
 * Copyright (c) 1997, 2020 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.glassfish.wasp;

import java.io.File;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.wasp.compiler.JspConfig;
import org.glassfish.wasp.compiler.Localizer;
import org.glassfish.wasp.compiler.TagPluginManager;
import org.glassfish.wasp.runtime.TldScanner;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

/**
 * A class to hold all init parameters specific to the JSP engine.
 *
 * @author Anil K. Vijendran
 * @author Hans Bergsten
 * @author Pierre Delisle
 */
public final class EmbeddedServletOptions implements Options {

    // Logger
    private static Logger log = Logger.getLogger(EmbeddedServletOptions.class.getName());

    private Properties settings = new Properties();

    /**
     * Is Wasp being used in development mode?
     */
    private boolean development = true;

    /**
     * Should Ant fork its java compiles of JSP pages.
     */
    public boolean fork = true;

    /**
     * Do you want to keep the generated Java files around?
     */
    private boolean keepGenerated;

    /**
     * If class files are generated as byte arrays, should they be saved to disk at the end of compilations?
     */
    private boolean saveBytecode;

    /**
     * Should white spaces between directives or actions be trimmed?
     */
    private boolean trimSpaces;

    /**
     * Determines whether tag handler pooling is enabled.
     */
    private boolean isPoolingEnabled = true;

    /**
     * Do you want support for "mapped" files? This will generate servlet that has a print statement per line of the JSP
     * file. This seems like a really nice feature to have for debugging.
     */
    private boolean mappedFile = true;

    /**
     * Do you want stack traces and such displayed in the client's browser? If this is false, such messages go to the
     * standard error or a log file if the standard error is redirected.
     */
    private boolean sendErrorToClient;

    /**
     * Do we want to include debugging information in the class file?
     */
    private boolean classDebugInfo = true;

    /**
     * Background compile thread check interval in seconds.
     */
    private int checkInterval = 0;

    /**
     * Is the generation of SMAP info for JSR45 debuggin suppressed?
     */
    private boolean isSmapSuppressed;

    /**
     * Should SMAP info for JSR45 debugging be dumped to a file?
     */
    private boolean isSmapDumped;

    /**
     * Are Text strings to be generated as char arrays?
     */
    private boolean genStringAsCharArray;

    private boolean genStringAsByteArray = true;

    private boolean defaultBufferNone;

    private boolean errorOnUseBeanInvalidClassAttribute;

    /**
     * I want to see my generated servlets. Which directory are they in?
     */
    private File scratchDir;

    /**
     * Need to have this as is for versions 4 and 5 of IE. Can be set from the initParams so if it changes in the future all
     * that is needed is to have a jsp initParam of type ieClassId="<value>"
     */
    private String ieClassId = "clsid:8AD9C840-044E-11D1-B3E9-00805F499D93";

    /**
     * What classpath should I use while compiling generated servlets?
     */
    private String classpath;

    private String sysClassPath;

    /**
     * Compiler to use.
     */
    private String compiler;

    /**
     * Compiler target VM.
     */
    private String compilerTargetVM = "1.8";

    /**
     * The compiler source VM.
     */
    private String compilerSourceVM = "1.8";

    /**
     * The compiler class name.
     */
    private String compilerClassName;

    /**
     * Cache for the TLD locations
     */
    private TldScanner tldScanner;

    /**
     * Jsp config information
     */
    private JspConfig jspConfig;

    /**
     * TagPluginManager
     */
    private TagPluginManager tagPluginManager;

    /**
     * Java platform encoding to generate the Jakarta Pages page servlet.
     */
    private String javaEncoding = "UTF8";

    /**
     * Modification test interval.
     */
    private int modificationTestInterval = 0;

    /**
     * Is generation of X-Powered-By response header enabled/disabled?
     */
    private boolean xpoweredBy;

    private boolean usePrecompiled;

    private boolean isValidationEnabled;

    /*
     * Initial capacity of HashMap which maps JSPs to their corresponding servlets
     */
    private int initialCapacity = Constants.DEFAULT_INITIAL_CAPACITY;

    public String getProperty(String name) {
        return settings.getProperty(name);
    }

    public void setProperty(String name, String value) {
        if (name != null && value != null) {
            settings.setProperty(name, value);
        }
    }

    /**
     * Are we keeping generated code around?
     */
    @Override
    public boolean getKeepGenerated() {
        return keepGenerated;
    }

    @Override
    public boolean getSaveBytecode() {
        return this.saveBytecode;
    }

    /**
     * Should white spaces between directives or actions be trimmed?
     */
    @Override
    public boolean getTrimSpaces() {
        return trimSpaces;
    }

    @Override
    public boolean isPoolingEnabled() {
        return isPoolingEnabled;
    }

    /**
     * Are we supporting HTML mapped servlets?
     */
    @Override
    public boolean getMappedFile() {
        return mappedFile;
    }

    /**
     * Should errors be sent to client or thrown into stderr?
     */
    @Override
    public boolean getSendErrorToClient() {
        return sendErrorToClient;
    }

    /**
     * Should class files be compiled with debug information?
     */
    @Override
    public boolean getClassDebugInfo() {
        return classDebugInfo;
    }

    /**
     * Background JSP compile thread check intervall
     */
    @Override
    public int getCheckInterval() {
        return checkInterval;
    }

    /**
     * Modification test interval.
     */
    @Override
    public int getModificationTestInterval() {
        return modificationTestInterval;
    }

    /**
     * Is Wasp being used in development mode?
     */
    @Override
    public boolean getDevelopment() {
        return development;
    }

    /**
     * Is the generation of SMAP info for JSR45 debugging suppressed?
     */
    @Override
    public boolean isSmapSuppressed() {
        return isSmapSuppressed;
    }

    /**
     * Should SMAP info for JSR45 debugging be dumped to a file?
     */
    @Override
    public boolean isSmapDumped() {
        return isSmapDumped;
    }

    /**
     * Are Text strings to be generated as char arrays?
     */
    @Override
    public boolean genStringAsCharArray() {
        return this.genStringAsCharArray;
    }

    @Override
    public boolean genStringAsByteArray() {
        return this.genStringAsByteArray;
    }

    @Override
    public boolean isDefaultBufferNone() {
        return defaultBufferNone;
    }

    /**
     * Class ID for use in the plugin tag when the browser is IE.
     */
    @Override
    public String getIeClassId() {
        return ieClassId;
    }

    /**
     * What is my scratch dir?
     */
    @Override
    public File getScratchDir() {
        return scratchDir;
    }

    /**
     * What classpath should I use while compiling the servlets generated from JSP files?
     */
    @Override
    public String getClassPath() {
        return classpath;
    }

    // START PWC 1.2 6311155
    /**
     * Gets the system class path.
     *
     * @return The system class path
     */
    @Override
    public String getSystemClassPath() {
        return sysClassPath;
    }
    // END PWC 1.2 6311155

    /**
     * Is generation of X-Powered-By response header enabled/disabled?
     */
    @Override
    public boolean isXpoweredBy() {
        return xpoweredBy;
    }

    /**
     * Compiler to use.
     */
    @Override
    public String getCompiler() {
        return compiler;
    }

    /**
     * @see Options#getCompilerTargetVM
     */
    @Override
    public String getCompilerTargetVM() {
        return compilerTargetVM;
    }

    /**
     * @see Options#getCompilerSourceVM
     */
    @Override
    public String getCompilerSourceVM() {
        return compilerSourceVM;
    }

    /**
     * @see Options#getCompiler
     */
    @Override
    public String getCompilerClassName() {
        return compilerClassName;
    }

    @Override
    public boolean getErrorOnUseBeanInvalidClassAttribute() {
        return errorOnUseBeanInvalidClassAttribute;
    }

    public void setErrorOnUseBeanInvalidClassAttribute(boolean b) {
        errorOnUseBeanInvalidClassAttribute = b;
    }

    @Override
    public TldScanner getTldScanner() {
        return tldScanner;
    }

    @Override
    public String getJavaEncoding() {
        return javaEncoding;
    }

    @Override
    public boolean getFork() {
        return fork;
    }

    @Override
    public JspConfig getJspConfig() {
        return jspConfig;
    }

    @Override
    public TagPluginManager getTagPluginManager() {
        return tagPluginManager;
    }

    /**
     * Gets initial capacity of HashMap which maps JSPs to their corresponding servlets.
     */
    @Override
    public int getInitialCapacity() {
        return initialCapacity;
    }

    /**
     * Returns the value of the usePrecompiled (or use-precompiled) init param.
     */
    @Override
    public boolean getUsePrecompiled() {
        return usePrecompiled;
    }

    @Override
    public boolean isValidationEnabled() {
        return isValidationEnabled;
    }

    /**
     * Create an EmbeddedServletOptions object using data available from ServletConfig and ServletContext.
     */
    public EmbeddedServletOptions(ServletConfig config, ServletContext context) {

        Enumeration enumeration = config.getInitParameterNames();
        while (enumeration.hasMoreElements()) {
            String k = (String) enumeration.nextElement();
            String v = config.getInitParameter(k);
            setProperty(k, v);
        }

        /*
         * SJSAS 6384538 // quick hack String validating=config.getInitParameter( "validating"); if( "false".equals( validating
         * )) ParserUtils.validating=false;
         */
        // START SJSAS 6384538
        String validating = config.getInitParameter("validating");
        if ("true".equals(validating)) {
            isValidationEnabled = true;
        }
        validating = config.getInitParameter("enableTldValidation");
        if ("true".equals(validating)) {
            isValidationEnabled = true;
        }
        // END SJSAS 6384538

        keepGenerated = getBoolean(config, keepGenerated, "keepgenerated");
        saveBytecode = getBoolean(config, saveBytecode, "saveBytecode");
        trimSpaces = getBoolean(config, trimSpaces, "trimSpaces");
        isPoolingEnabled = getBoolean(config, isPoolingEnabled, "enablePooling");
        mappedFile = getBoolean(config, mappedFile, "mappedfile");
        sendErrorToClient = getBoolean(config, sendErrorToClient, "sendErrToClient");
        classDebugInfo = getBoolean(config, classDebugInfo, "classdebuginfo");
        development = getBoolean(config, development, "development");
        isSmapSuppressed = getBoolean(config, isSmapSuppressed, "suppressSmap");
        isSmapDumped = getBoolean(config, isSmapDumped, "dumpSmap");
        genStringAsCharArray = getBoolean(config, genStringAsCharArray, "genStrAsCharArray");
        genStringAsByteArray = getBoolean(config, genStringAsByteArray, "genStrAsByteArray");
        defaultBufferNone = getBoolean(config, defaultBufferNone, "defaultBufferNone");
        errorOnUseBeanInvalidClassAttribute = getBoolean(config, errorOnUseBeanInvalidClassAttribute, "errorOnUseBeanInvalidClassAttribute");
        fork = getBoolean(config, fork, "fork");
        xpoweredBy = getBoolean(config, xpoweredBy, "xpoweredBy");

        String checkIntervalStr = config.getInitParameter("checkInterval");
        if (checkIntervalStr != null) {
            parseCheckInterval(checkIntervalStr);
        }

        String modificationTestIntervalStr = config.getInitParameter("modificationTestInterval");
        if (modificationTestIntervalStr != null) {
            parseModificationTestInterval(modificationTestIntervalStr);
        }

        String ieClassId = config.getInitParameter("ieClassId");
        if (ieClassId != null) {
            this.ieClassId = ieClassId;
        }

        String classpath = config.getInitParameter("classpath");
        if (classpath != null) {
            this.classpath = classpath;
        }

        // START PWC 1.2 6311155
        String sysClassPath = config.getInitParameter("com.sun.appserv.jsp.classpath");
        if (sysClassPath != null) {
            this.sysClassPath = sysClassPath;
            // END PWC 1.2 6311155
        }

        /*
         * scratchdir
         */
        String dir = config.getInitParameter("scratchdir");
        if (dir != null) {
            scratchDir = new File(dir);
        } else {
            // First try the Servlet 2.2 jakarta.servlet.context.tempdir property
            scratchDir = (File) context.getAttribute(Constants.TMP_DIR);
            if (scratchDir == null) {
                // Not running in a Servlet 2.2 container.
                // Try to get the JDK 1.2 java.io.tmpdir property
                dir = System.getProperty("java.io.tmpdir");
                if (dir != null) {
                    scratchDir = new File(dir);
                }
            }
        }
        if (this.scratchDir == null) {
            log.severe(Localizer.getMessage("jsp.error.no.scratch.dir"));
            return;
        }

        if (scratchDir.exists() && (!scratchDir.canRead() || !scratchDir.canWrite() || !scratchDir.isDirectory())) {
            log.severe(Localizer.getMessage("jsp.error.bad.scratch.dir", scratchDir.getAbsolutePath()));
        }

        this.compiler = config.getInitParameter("compiler");

        String compilerTargetVM = config.getInitParameter("compilerTargetVM");
        if (compilerTargetVM != null) {
            this.compilerTargetVM = compilerTargetVM;
        }

        String compilerSourceVM = config.getInitParameter("compilerSourceVM");
        if (compilerSourceVM != null) {
            this.compilerSourceVM = compilerSourceVM;
        }

        String javaEncoding = config.getInitParameter("javaEncoding");
        if (javaEncoding != null) {
            this.javaEncoding = javaEncoding;
        }

        String compilerClassName = config.getInitParameter("compilerClassName");
        if (compilerClassName != null) {
            this.compilerClassName = compilerClassName;
        }

        String reloadIntervalString = config.getInitParameter("reload-interval");
        if (reloadIntervalString != null) {
            int reloadInterval = 0;
            try {
                reloadInterval = Integer.parseInt(reloadIntervalString);
            } catch (NumberFormatException e) {
                if (log.isLoggable(Level.WARNING)) {
                    log.warning(Localizer.getMessage("jsp.warning.reloadInterval"));
                }
            }
            if (reloadInterval == -1) {
                // Never check JSPs for modifications, and disable
                // recompilation
                this.development = false;
                this.checkInterval = 0;
            } else {
                // Check JSPs for modifications at specified interval in
                // both (development and non-development) modes
                parseCheckInterval(reloadIntervalString);
                parseModificationTestInterval(reloadIntervalString);
            }
        }

        // BEGIN S1AS 6181923
        String usePrecompiled = config.getInitParameter("usePrecompiled");
        if (usePrecompiled == null) {
            usePrecompiled = config.getInitParameter("use-precompiled");
        }
        if (usePrecompiled != null) {
            if (usePrecompiled.equalsIgnoreCase("true")) {
                this.usePrecompiled = true;
            } else if (usePrecompiled.equalsIgnoreCase("false")) {
                this.usePrecompiled = false;
            } else {
                if (log.isLoggable(Level.WARNING)) {
                    log.warning(Localizer.getMessage("jsp.warning.usePrecompiled"));
                }
            }
        }
        // END S1AS 6181923

        // START SJSWS
        String capacity = config.getInitParameter("initialCapacity");
        if (capacity == null) {
            capacity = config.getInitParameter("initial-capacity");
        }
        if (capacity != null) {
            try {
                initialCapacity = Integer.parseInt(capacity);
                // Find a value that is power of 2 and >= the specified
                // initial capacity, in order to make Hashtable indexing
                // more efficient.
                int value = Constants.DEFAULT_INITIAL_CAPACITY;
                while (value < initialCapacity) {
                    value *= 2;
                }
                initialCapacity = value;
            } catch (NumberFormatException nfe) {
                if (log.isLoggable(Level.WARNING)) {
                    String msg = Localizer.getMessage("jsp.warning.initialcapacity");
                    msg = MessageFormat.format(msg, capacity, Integer.valueOf(Constants.DEFAULT_INITIAL_CAPACITY));
                    log.warning(msg);
                }
            }
        }

        String jspCompilerPlugin = config.getInitParameter("javaCompilerPlugin");
        if (jspCompilerPlugin != null) {
            if ("org.glassfish.wasp.compiler.JikesJavaCompiler".equals(jspCompilerPlugin)) {
                this.compiler = "jikes";
            } else if ("org.glassfish.wasp.compiler.SunJava14Compiler".equals(jspCompilerPlugin)) {
                // use default, do nothing
            } else {
                // use default, issue warning
                if (log.isLoggable(Level.WARNING)) {
                    String msg = Localizer.getMessage("jsp.warning.unsupportedJavaCompiler");
                    msg = MessageFormat.format(msg, jspCompilerPlugin);
                    log.warning(msg);
                }
            }
        }
        // END SJSWS

        // Setup the global Tag Libraries location cache for this
        // web-application.
        tldScanner = new TldScanner(context, isValidationEnabled);

        // Setup the jsp config info for this web app.
        jspConfig = new JspConfig(context);

        // Create a Tag plugin instance
        tagPluginManager = new TagPluginManager(context);
    }

    private void parseCheckInterval(String param) {
        try {
            this.checkInterval = Integer.parseInt(param);
        } catch (NumberFormatException ex) {
            if (log.isLoggable(Level.WARNING)) {
                log.warning(Localizer.getMessage("jsp.warning.checkInterval"));
            }
        }
    }

    private void parseModificationTestInterval(String param) {
        try {
            this.modificationTestInterval = Integer.parseInt(param);
        } catch (NumberFormatException ex) {
            if (log.isLoggable(Level.WARNING)) {
                log.warning(Localizer.getMessage("jsp.warning.modificationTestInterval"));
            }
        }
    }

    private boolean getBoolean(ServletConfig config, boolean init, String param) {

        String sParam = config.getInitParameter(param);
        if (sParam != null) {
            if (sParam.equalsIgnoreCase("true")) {
                return true;
            }
            if (sParam.equalsIgnoreCase("false")) {
                return false;
            }
            if (log.isLoggable(Level.WARNING)) {
                log.warning(Localizer.getMessage("jsp.warning.boolean", param, init ? "true" : "false"));
            }
        }
        return init;
    }

}
