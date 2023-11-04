/*
 * Copyright (c) 2022, 2023 Contributors to the Eclipse Foundation.
 * Copyright (c) 1997, 2020 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.wasp.JspCompilationContext;
import org.glassfish.wasp.Options;
import org.glassfish.wasp.WaspException;
import org.glassfish.wasp.servlet.JspServletWrapper;

/**
 * Main JSP compiler class.
 *
 * @author Anil K. Vijendran
 * @author Mandar Raje
 * @author Pierre Delisle
 * @author Kin-man Chung
 * @author Remy Maucherat
 * @author Mark Roth
 */

public class Compiler {

    // ----------------------------------------------------------------- Static

    /*
     * System jars should be exclude from the classpath for javac.
     */
    private static String systemJars[] = { "jstl.jar" };

    private static String systemFacesJars[] = { "jsf-api.jar", "jsf-impl.jar" };

    // ----------------------------------------------------- Instance Variables

    protected JspCompilationContext pagesCompilationContext;

    private ErrorDispatcher errDispatcher;
    private PageInfo pageInfo;
    private JspServletWrapper pagesServletWrapper;
    private TagFileProcessor tagFileProcessor;
    private JavaCompiler javaCompiler;
    private Logger log;
    private boolean jspcMode;
    private SmapUtil smapUtil;
    private Options options;
    private Node.Nodes pageNodes;
    private long jspModTime;
    private boolean javaCompilerOptionsSet;

    // ------------------------------------------------------------ Constructor

    // Compiler for parsing only, needed by netbeans
    public Compiler(JspCompilationContext ctxt, JspServletWrapper jsw) {
        this.pagesServletWrapper = jsw;
        this.pagesCompilationContext = ctxt;
        this.jspcMode = false;
        this.options = ctxt.getOptions();
        this.log = Logger.getLogger(Compiler.class.getName());
        this.smapUtil = new SmapUtil(ctxt);
        this.errDispatcher = new ErrorDispatcher(jspcMode);
        this.javaCompiler = new NullJavaCompiler();
        javaCompiler.init(ctxt, errDispatcher, jspcMode);
        this.javaCompilerOptionsSet = false;
    }

    public Compiler(JspCompilationContext ctxt, JspServletWrapper jsw, boolean jspcMode) throws WaspException {
        this.pagesServletWrapper = jsw;
        this.pagesCompilationContext = ctxt;
        this.jspcMode = jspcMode;
        this.options = ctxt.getOptions();
        this.log = Logger.getLogger(Compiler.class.getName());
        if (jspcMode) {
            log.setLevel(Level.OFF);
        }
        this.smapUtil = new SmapUtil(ctxt);
        this.errDispatcher = new ErrorDispatcher(jspcMode);
        initJavaCompiler();
        this.javaCompilerOptionsSet = false;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Compile the Pages file into equivalent servlet in java source
     */
    private void generateJava() throws Exception {
        long t1, t2, t3, t4;
        t1 = t2 = t3 = t4 = 0;

        if (log.isLoggable(FINE)) {
            t1 = System.currentTimeMillis();
        }

        // Setup page info area
        pageInfo = new PageInfo(new BeanRepository(pagesCompilationContext.getClassLoader(), errDispatcher), pagesCompilationContext.getJspFile());

        JspConfig jspConfig = options.getJspConfig();
        JspProperty jspProperty = jspConfig.findJspProperty(pagesCompilationContext.getJspFile());

        /*
         * If the current uri is matched by a pattern specified in a jsp-property-group in web.xml, initialize pageInfo with
         * those properties.
         */
        pageInfo.setELIgnored(JspUtil.booleanValue(jspProperty.isELIgnored()));
        pageInfo.setScriptingInvalid(JspUtil.booleanValue(jspProperty.isScriptingInvalid()));
        pageInfo.setTrimDirectiveWhitespaces(JspUtil.booleanValue(jspProperty.getTrimSpaces()));

        if (jspProperty.getErrorOnELNotFound() != null) {
            pageInfo.setErrorOnELNotFound(JspUtil.booleanValue(jspProperty.getErrorOnELNotFound()));
        }

        pageInfo.setDeferredSyntaxAllowedAsLiteral(JspUtil.booleanValue(jspProperty.getPoundAllowed()));
        pageInfo.setErrorOnUndeclaredNamespace(JspUtil.booleanValue(jspProperty.errorOnUndeclaredNamespace()));

        if (jspProperty.getIncludePrelude() != null) {
            pageInfo.setIncludePrelude(jspProperty.getIncludePrelude());
        }
        if (jspProperty.getIncludeCoda() != null) {
            pageInfo.setIncludeCoda(jspProperty.getIncludeCoda());
        }
        if (options.isDefaultBufferNone() && pageInfo.getBufferValue() == null) {
            // Set to unbuffered if not specified explicitly
            pageInfo.setBuffer(0);
        }

        String javaFileName = pagesCompilationContext.getServletJavaFileName();
        ServletWriter writer = null;

        try {
            // Setup the ServletWriter
            Writer javaWriter = javaCompiler.getJavaWriter(javaFileName, pagesCompilationContext.getOptions().getJavaEncoding());
            writer = new ServletWriter(new PrintWriter(javaWriter));
            pagesCompilationContext.setWriter(writer);

            // Reset the temporary variable counter for the generator.
            JspUtil.resetTemporaryVariableName();

            // Parse the file
            ParserController parserCtl = new ParserController(pagesCompilationContext, this);
            pageNodes = parserCtl.parse(pagesCompilationContext.getJspFile());

            if (pagesCompilationContext.isPrototypeMode()) {
                // generate prototype .java file for the tag file
                Generator.generate(writer, this, pageNodes);
                writer.close();
                writer = null;
                return;
            }

            // Validate and process attributes
            Validator.validate(this, pageNodes);

            if (log.isLoggable(FINE)) {
                t2 = System.currentTimeMillis();
            }

            // Collect page info
            Collector.collect(this, pageNodes);

            // Compile (if necessary) and load the tag files referenced in
            // this compilation unit.
            tagFileProcessor = new TagFileProcessor();
            tagFileProcessor.loadTagFiles(this, pageNodes);

            if (log.isLoggable(FINE)) {
                t3 = System.currentTimeMillis();
            }

            // Determine which custom tag needs to declare which scripting vars
            ScriptingVariabler.set(pageNodes, errDispatcher);

            // Optimizations by Tag Plugins
            TagPluginManager tagPluginManager = options.getTagPluginManager();
            tagPluginManager.apply(pageNodes, errDispatcher, pageInfo);

            // Optimization: concatenate contiguous template texts.
            TextOptimizer.concatenate(this, pageNodes);

            // Generate static function mapper codes.
            ELFunctionMapper.map(this, pageNodes);

            // generate servlet .java file
            Generator.generate(writer, this, pageNodes);
            writer.close();
            writer = null;

            // The writer is only used during the compile, dereference
            // it in the JspCompilationContext when done to allow it
            // to be GC'd and save memory.
            pagesCompilationContext.setWriter(null);

            if (log.isLoggable(FINE)) {
                t4 = System.currentTimeMillis();
                log.fine("Generated " + javaFileName + " total=" + (t4 - t1) + " generate=" + (t4 - t3) + " validate=" + (t2 - t1));
            }

        } catch (Exception e) {
            if (writer != null) {
                try {
                    writer.close();
                    writer = null;
                } catch (Exception e1) {
                    // do nothing
                }
            }
            // Remove the generated .java file
            javaCompiler.doJavaFile(false);
            throw e;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e2) {
                    // do nothing
                }
            }
        }

        // JSR45 Support
        if (!options.isSmapSuppressed()) {
            smapUtil.generateSmap(pageNodes);
        }

        // If any proto type .java and .class files was generated,
        // the prototype .java may have been replaced by the current
        // compilation (if the tag file is self referencing), but the
        // .class file need to be removed, to make sure that javac would
        // generate .class again from the new .java file just generated.
        tagFileProcessor.removeProtoTypeFiles(pagesCompilationContext.getClassFileName());
    }

    private void setJavaCompilerOptions() {

        if (javaCompilerOptionsSet) {
            return;
        }
        javaCompilerOptionsSet = true;

        String classpath = pagesCompilationContext.getClassPath();
        String sep = System.getProperty("path.separator");

        // Initializing classpath
        ArrayList<File> cpath = new ArrayList<>();
        HashSet<String> paths = new HashSet<>();

        // Process classpath, which includes system classpath from compiler
        // options, plus the context classpath from the classloader
        String sysClassPath = options.getSystemClassPath();
        if (sysClassPath != null) {
            StringTokenizer tokenizer = new StringTokenizer(sysClassPath, sep);
            while (tokenizer.hasMoreElements()) {
                String path = tokenizer.nextToken();
                if (!paths.contains(path) && !systemJarInWebinf(path)) {
                    paths.add(path);
                    cpath.add(new File(path));
                }
            }
        }
        if (classpath != null) {
            StringTokenizer tokenizer = new StringTokenizer(classpath, sep);
            while (tokenizer.hasMoreElements()) {
                String path = tokenizer.nextToken();
                if (!paths.contains(path) && !systemJarInWebinf(path)) {
                    paths.add(path);
                    cpath.add(new File(path));
                }
            }
        }
        if (log.isLoggable(FINE)) {
            log.fine("Using classpath: " + sysClassPath + sep + classpath);
        }
        javaCompiler.setClassPath(cpath);

        // Set debug info
        javaCompiler.setDebug(options.getClassDebugInfo());

        // Initialize and set java extensions
        String exts = System.getProperty("java.ext.dirs");
        if (exts != null) {
            javaCompiler.setExtdirs(exts);
        }

        if (options.getCompilerTargetVM() != null) {
            javaCompiler.setTargetVM(options.getCompilerTargetVM());
        }

        if (options.getCompilerSourceVM() != null) {
            javaCompiler.setSourceVM(options.getCompilerSourceVM());
        }

    }

    /**
     * Compile the servlet from .java file to .class file
     */
    private void generateClass() throws FileNotFoundException, WaspException, Exception {
        long t1 = 0;
        if (log.isLoggable(FINE)) {
            t1 = System.currentTimeMillis();
        }

        String javaFileName = pagesCompilationContext.getServletJavaFileName();

        setJavaCompilerOptions();

        // Start java compilation
        JavacErrorDetail[] javacErrors = javaCompiler.compile(pagesCompilationContext.getFullClassName(), pageNodes);

        if (javacErrors != null) {
            // If there are errors, always generate java files to disk.
            javaCompiler.doJavaFile(true);

            log.severe("Error compiling file: " + javaFileName);
            errDispatcher.javacError(javacErrors);
        }

        if (log.isLoggable(FINE)) {
            long t2 = System.currentTimeMillis();
            log.fine("Compiled " + javaFileName + " " + (t2 - t1) + "ms");
        }

        // Save or delete the generated Java files, depending on the
        // value of "keepgenerated" attribute
        javaCompiler.doJavaFile(pagesCompilationContext.keepGenerated());

        // JSR45 Support
        if (!pagesCompilationContext.isPrototypeMode() && !options.isSmapSuppressed()) {
            smapUtil.installSmap();
        }

        if (pagesServletWrapper != null && pagesServletWrapper.getServletClassLastModifiedTime() <= 0) {
            pagesServletWrapper.setServletClassLastModifiedTime(javaCompiler.getClassLastModified());
        }

        if (options.getSaveBytecode()) {
            javaCompiler.saveClassFile(pagesCompilationContext.getFullClassName(), pagesCompilationContext.getClassFileName());
        }

        // On some systems, due to file caching, the time stamp for the updated
        // Pages file may actually be greater than that of the newly created byte
        // codes in the cache. In such cases, adjust the cache time stamp to
        // Pages page time, to avoid unnecessary recompilations.
        pagesCompilationContext.getRuntimeContext().adjustBytecodeTime(pagesCompilationContext.getFullClassName(), jspModTime);
    }

    /**
     * Compile the jsp file from the current engine context. As an side- effect, tag files that are referenced by this page
     * are also compiled.
     *
     * @param compileClass If true, generate both .java and .class file If false, generate only .java file
     */
    public void compile(boolean compileClass) throws FileNotFoundException, WaspException, Exception {

        try {
            // Create the output directory for the generated files
            // Always try and create the directory tree, in case the generated
            // directories were deleted after the server was started.
            pagesCompilationContext.makeOutputDir(pagesCompilationContext.getOutputDir());

            // If errDispatcher is nulled from a previous compilation of the
            // same page, instantiate one here.
            if (errDispatcher == null) {
                errDispatcher = new ErrorDispatcher(jspcMode);
            }
            generateJava();
            if (compileClass) {
                generateClass();
            } else {
                // If called from jspc to only compile to .java files,
                // make sure that .java files are written to disk.
                javaCompiler.doJavaFile(pagesCompilationContext.keepGenerated());
            }
        } finally {
            if (tagFileProcessor != null) {
                tagFileProcessor.removeProtoTypeFiles(null);
            }
            javaCompiler.release();
            // Make sure these object which are only used during the
            // generation and compilation of the JSP page get
            // dereferenced so that they can be GC'd and reduce the
            // memory footprint.
            tagFileProcessor = null;
            errDispatcher = null;
            if (!jspcMode) {
                pageInfo = null;
            }

            pageNodes = null;
            if (pagesCompilationContext.getWriter() != null) {
                pagesCompilationContext.getWriter().close();
                pagesCompilationContext.setWriter(null);
            }
        }
    }

    /**
     * This is a protected method intended to be overridden by subclasses of Compiler. This is used by the compile method to
     * do all the compilation.
     */
    public boolean isOutDated() {
        return isOutDated(true);
    }

    /**
     * Determine if a compilation is necessary by checking the time stamp of the JSP page with that of the corresponding
     * .class or .java file. If the page has dependencies, the check is also extended to its dependeants, and so on. This
     * method can by overidden by a subclasses of Compiler.
     *
     * @param checkClass If true, check against .class file, if false, check against .java file.
     */
    public boolean isOutDated(boolean checkClass) {
        String pagesFile = pagesCompilationContext.getJspFile();

        if (pagesServletWrapper != null && pagesCompilationContext.getOptions().getModificationTestInterval() > 0) {

            if (pagesServletWrapper.getLastModificationTest() + pagesCompilationContext.getOptions().getModificationTestInterval() * 1000 > System.currentTimeMillis()) {
                return false;
            }

            pagesServletWrapper.setLastModificationTest(System.currentTimeMillis());
        }

        long jspRealLastModified = 0;
        File targetFile;

        if (checkClass) {
            targetFile = new File(pagesCompilationContext.getClassFileName());
        } else {
            targetFile = new File(pagesCompilationContext.getServletJavaFileName());
        }

        // Get the target file's last modified time.
        // getLastModifiedTime() returns 0 if the file does not exist.
        long targetLastModified = getLastModifiedTime(targetFile);

        // Check cached class file
        if (checkClass) {
            JspRuntimeContext runtimeContext = pagesCompilationContext.getRuntimeContext();
            String className = pagesCompilationContext.getFullClassName();
            long cachedTime = runtimeContext.getBytecodeBirthTime(className);
            if (cachedTime > targetLastModified) {
                targetLastModified = cachedTime;
            } else {
                // Remove from cache, since the bytecodes from the file is more
                // current, so that WaspLoader won't load the cached version
                runtimeContext.setBytecode(className, null);
            }
        }

        if (targetLastModified == 0L) {
            return true;
        }

        // Check if the Pages file exists in the filesystem (instead of a jar
        // or a remote location). If yes, then do a getLastModifiedTime()
        // to determine its last modified time. This is more performant
        // (fewer stat calls) than the ctxt.getResource() followed by
        // openConnection(). However, it only works for file system jsps.
        // If the file has indeed changed, then need to call URL.OpenConnection()
        // so that the cache loads the latest jsp file
        if (pagesServletWrapper != null) {
            File jspFile = pagesServletWrapper.getJspFile();
            if (jspFile != null) {
                jspRealLastModified = getLastModifiedTime(jspFile);
            }
        }

        if (jspRealLastModified == 0 || targetLastModified < jspRealLastModified) {
            try {
                URL jspUrl = pagesCompilationContext.getResource(pagesFile);
                if (jspUrl == null) {
                    pagesCompilationContext.incrementRemoved();
                    return false;
                }
                URLConnection uc = jspUrl.openConnection();
                if (uc instanceof JarURLConnection) {
                    jspRealLastModified = ((JarURLConnection) uc).getJarEntry().getTime();
                } else {
                    jspRealLastModified = uc.getLastModified();
                }
                uc.getInputStream().close();
            } catch (Exception e) {
                e.printStackTrace();
                return true;
            }
        }

        if (checkClass && pagesServletWrapper != null) {
            pagesServletWrapper.setServletClassLastModifiedTime(targetLastModified);
        }

        if (targetLastModified < jspRealLastModified) {
            // Remember Pages mod time
            jspModTime = jspRealLastModified;
            if (log.isLoggable(FINE)) {
                log.fine("Compiler: outdated: " + targetFile + " " + targetLastModified);
            }
            return true;
        }

        // determine if source dependent files (e.g. includes using include
        // directives) have been changed.
        if (pagesServletWrapper == null) {
            return false;
        }

        List<String> depends = pagesServletWrapper.getDependants();
        if (depends == null) {
            return false;
        }

        for (String include : depends) {
            try {
                URL includeUrl = pagesCompilationContext.getResource(include);
                if (includeUrl == null) {
                    return true;
                }

                URLConnection includeUconn = includeUrl.openConnection();
                long includeLastModified = 0;
                if (includeUconn instanceof JarURLConnection) {
                    includeUconn.setUseCaches(false);
                    includeLastModified = ((JarURLConnection) includeUconn).getJarEntry().getTime();
                } else {
                    includeLastModified = includeUconn.getLastModified();
                }
                includeUconn.getInputStream().close();

                if (includeLastModified > targetLastModified) {
                    // START GlassFish 750
                    if (include.endsWith(".tld")) {
                        pagesCompilationContext.clearTaglibs();
                        pagesCompilationContext.clearTagFileJarUrls();
                    }
                    // END GlassFish 750
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return true;
            }
        }

        return false;

    }

    /**
     * Returns a file's last modified time.
     *
     * <p>The {@code 0L} time stamp may be returned when:
     * <ul>
     *     <li>File does not exists.</li>
     *     <li>Basic attribute view is not available for {@code file}.</li>
     *     <li>Creation time stamp and last modified time stamp not implemented for {@code file}.</li>
     *     <li>An I/O error occurred.</li>
     * </ul>
     *
     * <p>If numeric overflow occurs, returns Long.MIN_VALUE if negative and Long.MAX_VALUE if positive.
     *
     * @param file file to read attributes from
     * @return a time stamp representing the time the file was last modified
     */
    private long getLastModifiedTime(File file) {
        if (!file.exists()) {
            return 0L;
        }

        BasicFileAttributeView attributeView = Files.getFileAttributeView(file.toPath(), BasicFileAttributeView.class);
        if (attributeView == null) {
            log.log(SEVERE, "Basic attribute view is not available for " + file.getAbsolutePath());
            return 0L;
        }

        BasicFileAttributes attributes;
        try {
            attributes = attributeView.readAttributes();
        } catch (IOException e) {
            log.log(SEVERE, "Failed to read attributes for file " + file.getAbsolutePath(), e);
            return 0L;
        }

        FileTime creationTime = attributes.creationTime();
        FileTime lastModifiedTime = attributes.lastModifiedTime();

        if (creationTime == null) {
            return lastModifiedTime != null ? lastModifiedTime.toMillis() : 0L;
        }

        if (lastModifiedTime == null) {
            return creationTime.toMillis();
        }

        return creationTime.compareTo(lastModifiedTime) <= 0 ? lastModifiedTime.toMillis() : creationTime.toMillis();
    }

    /**
     * Gets the error dispatcher.
     */
    public ErrorDispatcher getErrorDispatcher() {
        return errDispatcher;
    }

    /**
     * Gets the info about the page under compilation
     */
    public PageInfo getPageInfo() {
        return pageInfo;
    }

    /**
     * Sets the info about the page under compilation
     */
    public void setPageInfo(PageInfo pageInfo) {
        this.pageInfo = pageInfo;
    }

    public JspCompilationContext getCompilationContext() {
        return pagesCompilationContext;
    }

    /**
     * Remove generated files
     */
    public void removeGeneratedFiles() {
        try {
            String classFileName = pagesCompilationContext.getClassFileName();
            if (classFileName != null) {
                File classFile = new File(classFileName);
                if (log.isLoggable(FINE)) {
                    log.fine("Deleting " + classFile);
                }
                classFile.delete();
            }
        } catch (Exception e) {
            // Remove as much as possible, ignore possible exceptions
        }

        try {
            String javaFileName = pagesCompilationContext.getServletJavaFileName();
            if (javaFileName != null) {
                File javaFile = new File(javaFileName);
                if (log.isLoggable(FINE)) {
                    log.fine("Deleting " + javaFile);
                }
                javaFile.delete();
            }
        } catch (Exception e) {
            // Remove as much as possible, ignore possible exceptions
        }
    }

    public void removeGeneratedClassFiles() {
        try {
            String classFileName = pagesCompilationContext.getClassFileName();
            if (classFileName != null) {
                File classFile = new File(classFileName);
                if (log.isLoggable(FINE)) {
                    log.fine("Deleting " + classFile);
                }
                classFile.delete();
            }
        } catch (Exception e) {
            // Remove as much as possible, ignore possible exceptions
        }
    }

    /**
     * Get an instance of JavaCompiler. If a compiler is specified in Options else use a
     * Jsr199JavaCompiler that supports JSR199.
     */
    private void initJavaCompiler() throws WaspException {
        if (options.getCompilerClassName() != null) {
            Class<?> compilerClass = getClassFor(options.getCompilerClassName());
            try {
                javaCompiler = (JavaCompiler) compilerClass.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
            }
        }
        if (javaCompiler == null) {
            javaCompiler = new Jsr199JavaCompiler();
        }

        javaCompiler.init(pagesCompilationContext, errDispatcher, jspcMode);
    }

    private Class<?> getClassFor(String className) {
        try {
            return Class.forName(className, false, getClass().getClassLoader());
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }


    /**
     * Return true if the path refers to a jar file in WEB-INF and is a system jar.
     */
    private boolean systemJarInWebinf(String path) {
        if (path.indexOf("/WEB-INF/") < 0) {
            return false;
        }

        Boolean useMyFaces = (Boolean) pagesCompilationContext.getServletContext().getAttribute("com.sun.faces.useMyFaces");

        if (useMyFaces == null || !useMyFaces) {
            for (String jar : systemFacesJars) {
                if (path.indexOf(jar) > 0) {
                    return true;
                }
            }
        }

        for (String jar : systemJars) {
            if (path.indexOf(jar) > 0) {
                return true;
            }
        }

        return false;
    }
}
