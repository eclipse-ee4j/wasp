/*
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

package org.glassfish.wasp.runtime;

import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.parseBoolean;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;
import static org.glassfish.wasp.Constants.JSP_TLD_URI_TO_LOCATION_MAP;
import static org.glassfish.wasp.Constants.XML_BLOCK_EXTERNAL_INIT_PARAM;
import static org.glassfish.wasp.compiler.Localizer.getMessage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;

import org.glassfish.wasp.WaspException;
import org.glassfish.wasp.compiler.Localizer;
import org.glassfish.wasp.xmlparser.ParserUtils;
import org.glassfish.wasp.xmlparser.TreeNode;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.descriptor.JspConfigDescriptor;
import jakarta.servlet.descriptor.TaglibDescriptor;

/**
 * A container for all tag libraries that are defined "globally" for the web application.
 *
 * <p>
 * Tag Libraries can be defined globally in one of two ways: 1. Via <taglib> elements in web.xml: the uri and location
 * of the tag-library are specified in the <taglib> element. 2. Via packaged jar files that contain .tld files within
 * the META-INF directory, or some subdirectory of it. The taglib is 'global' if it has the <uri> element defined.
 * 
 * <p>
 * A mapping between the taglib URI and its associated TaglibraryInfoImpl is maintained in this container. Actually,
 * that's what we'd like to do. However, because of the way the classes TagLibraryInfo and TagInfo have been defined, it
 * is not currently possible to share an instance of TagLibraryInfo across page invocations. A bug has been submitted to
 * the spec lead. In the mean time, all we do is save the 'location' where the TLD associated with a taglib URI can be
 * found.
 *
 * <p>
 * When a JSP page has a taglib directive, the mappings in this container are first searched (see method getLocation()).
 * If a mapping is found, then the location of the TLD is returned. If no mapping is found, then the uri specified in
 * the taglib directive is to be interpreted as the location for the TLD of this tag library.
 *
 * @author Pierre Delisle
 * @author Jan Luehe
 * @author Kin-man Chung servlet 3.0 JSP plugin, tld cache etc
 * @author Arjan Tijms
 */
public class TldScanner implements ServletContainerInitializer {

    // Logger
    private static Logger log = Logger.getLogger(TldScanner.class.getName());

    /**
     * The types of URI one may specify for a tag library
     */
    public static final int ABS_URI = 0;
    public static final int ROOT_REL_URI = 1;
    public static final int NOROOT_REL_URI = 2;

    private static final String FILE_PROTOCOL = "file:";
    private static final String JAR_FILE_SUFFIX = ".jar";

    // Names of system Uri's that are ignored if referred in WEB-INF/web.xml
    private static Set<String> systemUris = new HashSet<>();
    private static Set<String> systemUrisJsf = new HashSet<>();

    // A Cache is used for system jar files.
    // The key is the name of the jar file, the value is an array of
    // TldInfo, one for each of the TLD in the jar file
    private static Map<String, TldInfo[]> jarTldCache = new ConcurrentHashMap<>();

    private static final String EAR_LIB_CLASSLOADER = "org.glassfish.javaee.full.deployment.EarLibClassLoader";

    private static final String IS_STANDALONE_ATTRIBUTE_NAME = "org.glassfish.jsp.isStandaloneWebapp";

    /**
     * The mapping of the 'global' tag library URI (as defined in the tld) to the location (resource path) of the TLD
     * associated with that tag library. The location is returned as a String array: [0] The location of the tld file or the
     * jar file that contains the tld [1] If the location is a jar file, this is the location of the tld.
     */
    private Map<String, String[]> mappings;

    /**
     * A local cache for keeping track which jars have been scanned.
     */
    private Map<String, TldInfo[]> jarTldCacheLocal = new HashMap<>();

    private ServletContext servletContext;
    private boolean isValidationEnabled;
    private boolean useMyFaces;
    private boolean useMultiJarScanAlgo;
    private boolean scanListeners; // true if scan tlds for listeners
    private boolean doneScanning; // true if all tld scanning done
    private boolean blockExternal; // Don't allow external entities

    // *********************************************************************
    // Constructor and Initilizations

    /*
     * Initializes the set of JARs that are known not to contain any TLDs
     */
    static {
        systemUrisJsf.add("http://java.sun.com/jsf/core");
        systemUrisJsf.add("http://java.sun.com/jsf/html");
        systemUris.add("http://java.sun.com/jsp/jstl/core");
    }

    /**
     * Default Constructor. This is only used for implementing ServletContainerInitializer. ServletContext will be supplied
     * in the method onStartUp;
     */
    public TldScanner() {
    }

    /**
     * Constructor used in WaSP
     */
    public TldScanner(ServletContext servletContext, boolean isValidationEnabled) {
        this.servletContext = servletContext;
        this.isValidationEnabled = isValidationEnabled;
        useMyFaces = TRUE.equals(servletContext.getAttribute("com.sun.faces.useMyFaces"));
        useMultiJarScanAlgo = TRUE.equals(servletContext.getAttribute("org.glassfish.wasp.useMultiJarScanAlgo"));
        blockExternal = parseBoolean(servletContext.getInitParameter(XML_BLOCK_EXTERNAL_INIT_PARAM));
    }

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext servletContext) throws ServletException {
        this.servletContext = servletContext;
        useMyFaces = TRUE.equals(servletContext.getAttribute("com.sun.faces.useMyFaces"));
        useMultiJarScanAlgo = TRUE.equals(servletContext.getAttribute("org.glassfish.wasp.useMultiJarScanAlgo"));
        
        ServletRegistration jspServletRegistration = servletContext.getServletRegistration("jsp");
        if (jspServletRegistration == null) {
            return;
        }
        
        String validating = jspServletRegistration.getInitParameter("validating");
        isValidationEnabled = "true".equals(validating);

        scanListeners = true;
        scanTlds();

        servletContext.setAttribute(JSP_TLD_URI_TO_LOCATION_MAP, mappings);
    }

    /**
     * Gets the 'location' of the TLD associated with the given taglib 'uri'.
     *
     * Returns null if the uri is not associated with any tag library 'exposed' in the web application. A tag library is
     * 'exposed' either explicitly in web.xml or implicitly via the uri tag in the TLD of a taglib deployed in a jar file
     * (WEB-INF/lib).
     *
     * @param uri The taglib uri
     *
     * @return An array of two Strings: The first element denotes the real path to the TLD. If the path to the TLD points to
     * a jar file, then the second element denotes the name of the TLD entry in the jar file. Returns null if the uri is not
     * associated with any tag library 'exposed' in the web application.
     *
     * This method may be called when the scanning is in one of states: 1. Called from jspc script, then a full tld scan is
     * required. 2. The is the first call after servlet initialization, then system jars that are knwon to have tlds but not
     * listeners need to be scanned. 3. Sebsequent calls, no need to scans.
     */

    @SuppressWarnings("unchecked")
    public String[] getLocation(String uri) throws WaspException {
        if (mappings == null) {
            // Recovering the map done in onStart.
            mappings = (Map<String, String[]>) servletContext.getAttribute(JSP_TLD_URI_TO_LOCATION_MAP);
        }

        if (mappings != null && mappings.get(uri) != null) {
            // if the uri is in, return that, and don't bother to do full scan
            return mappings.get(uri);
        }

        if (!doneScanning) {
            scanListeners = false;
            scanTlds();
            doneScanning = true;
        }
        if (mappings == null) {
            // Should never happend
            return null;
        }
        return mappings.get(uri);
    }

    /**
     * Returns the type of a URI: ABS_URI ROOT_REL_URI NOROOT_REL_URI
     */
    public static int uriType(String uri) {
        if (uri.indexOf(':') != -1) {
            return ABS_URI;
        }
        
        if (uri.startsWith("/")) {
            return ROOT_REL_URI;
        }
        
        return NOROOT_REL_URI;
    }

    /**
     * Scan the all the tlds accessible in the web app. For performance reasons, this is done in two stages. At servlet
     * initialization time, we only scan the jar files for listeners. The container passes a list of system jar files that
     * are known to contain tlds with listeners. The rest of the jar files will be scanned when a JSP page with a tld
     * referenced is compiled.
     */
    private void scanTlds() throws WaspException {
        mappings = new HashMap<>();

        // Make a local copy of the system jar cache
        jarTldCacheLocal.putAll(jarTldCache);

        try {
            processWebDotXml();
            scanJars();
            processTldsInFileSystem("/WEB-INF/");
        } catch (WaspException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new WaspException(getMessage("jsp.error.internal.tldinit"), ex);
        }
    }

    /*
     * Populates taglib map described in web.xml.
     */
    private void processWebDotXml() throws Exception {

        // Skip if we are only looking for listeners
        if (scanListeners) {
            return;
        }

        JspConfigDescriptor jspConfig = servletContext.getJspConfigDescriptor();
        if (jspConfig == null) {
            return;
        }

        for (TaglibDescriptor taglib : jspConfig.getTaglibs()) {
            if (taglib == null) {
                continue;
            }
            
            String taglibURI = taglib.getTaglibURI();
            String taglibLocation = taglib.getTaglibLocation();
            if (taglibURI == null || taglibLocation == null) {
                continue;
            }
            
            // Ignore system tlds in web.xml, for backward compatibility
            if (systemUris.contains(taglibURI) || !useMyFaces && systemUrisJsf.contains(taglibURI)) {
                continue;
            }
            
            // Save this location if appropriate
            if (uriType(taglibLocation) == NOROOT_REL_URI) {
                taglibLocation = "/WEB-INF/" + taglibLocation;
            }
            String tagLoc2 = null;
            if (taglibLocation.endsWith(JAR_FILE_SUFFIX)) {
                taglibLocation = servletContext.getResource(taglibLocation).toString();
                tagLoc2 = "META-INF/taglib.tld";
            }
            if (log.isLoggable(FINE)) {
                log.fine("Add tld map from web.xml: " + taglibURI + "=>" + taglibLocation + "," + tagLoc2);
            }
            mappings.put(taglibURI, new String[] { taglibLocation, tagLoc2 });
        }
    }

    /*
     * Searches the filesystem under /WEB-INF for any TLD files, and scans them for <uri> and <listener> elements.
     */
    private void processTldsInFileSystem(String startPath) throws WaspException {
        Set<String> dirList = servletContext.getResourcePaths(startPath);
        if (dirList != null) {
            for (String path : dirList) {
                if (path.endsWith("/")) {
                    processTldsInFileSystem(path);
                }
                if (!path.endsWith(".tld")) {
                    continue;
                }
                if (path.startsWith("/WEB-INF/tags/") && !path.endsWith("implicit.tld")) {
                    throw new WaspException(Localizer.getMessage("jsp.error.tldinit.tldInWebInfTags", path));
                }
                InputStream stream = servletContext.getResourceAsStream(path);
                TldInfo tldInfo = scanTld(path, null, stream);
                
                // Add listeners or to map tldlocations for this TLD
                if (scanListeners) {
                    addListener(tldInfo, true);
                }
                
                mapTldLocation(path, tldInfo, true);
            }
        }
    }

    /**
     * Scan the given TLD for uri and listeners elements.
     *
     * @param resourcePath the resource path for the jar file or the tld file.
     * @param entryName If the resource path is a jar file, then the name of the tld file in the jar, else should be null.
     * @param stream The input stream for the tld
     * @return The TldInfo for this tld
     */
    private TldInfo scanTld(String resourcePath, String entryName, InputStream stream) throws WaspException {
        try {
            // Parse the tag library descriptor at the specified resource path
            TreeNode tld = new ParserUtils(blockExternal).parseXMLDocument(resourcePath, stream, isValidationEnabled);

            String uri = null;
            TreeNode uriNode = tld.findChild("uri");
            if (uriNode != null) {
                uri = uriNode.getBody();
            }

            List<String> listeners = new ArrayList<>();

            Iterator<TreeNode> listenerNodes = tld.findChildren("listener");
            while (listenerNodes.hasNext()) {
                TreeNode listener = listenerNodes.next();
                TreeNode listenerClass = listener.findChild("listener-class");
                if (listenerClass != null) {
                    String listenerClassName = listenerClass.getBody();
                    if (listenerClassName != null) {
                        listeners.add(listenerClassName);
                    }
                }
            }

            return new TldInfo(uri, entryName, listeners.toArray(new String[listeners.size()]));

        } finally {
            closeSilently(stream);
        }
    }

    /*
     * Scans all JARs accessible to the webapp's classloader and its parent classloaders for TLDs.
     *
     * <p>
     * If <code>useMultiJarScanAlgo</code> is false, the following algorithm will be used:
     * <p>
     * The list of JARs always includes the JARs under WEB-INF/lib, as well as all shared JARs in the classloader delegation
     * chain of the webapp's classloader.
     *
     * <p>
     * Considering JARs in the classloader delegation chain constitutes a Tomcat-specific extension to the TLD search order
     * defined in the JSP spec. It allows tag libraries packaged as JAR files to be shared by web applications by simply
     * dropping them in a location that all web applications have access to (e.g., <CATALINA_HOME>/common/lib).
     */
    private void scanJars() throws Exception {
        boolean isStandalone = TRUE.equals(servletContext.getAttribute(IS_STANDALONE_ATTRIBUTE_NAME));
        
        if (useMultiJarScanAlgo) {
            for (URL url : Classpath.search("META-INF/", ".tld")) {
                scanJar(url, isStandalone);
            }
            
            return;
        }

        ClassLoader webappLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader loader = webappLoader;

        Map<URI, List<String>> tldMap;
        if (scanListeners) {
            tldMap = getTldListenerMap();
        } else {
            tldMap = getTldMap();
        }

        while (loader != null) {
            if (loader instanceof URLClassLoader) {
                URLClassLoader urlClassLoader = (URLClassLoader) loader;
                
                boolean isLocal = loader == webappLoader;
                List<String> manifestClassPathJars = new ArrayList<>();

                for (URL url : urlClassLoader.getURLs()) {
                    JarURLConnection jarURLConnection = getJarURLConnection(url);
                    
                    if (jarURLConnection != null) {
                        jarURLConnection.setUseCaches(false);
                        if (isLocal) {
                            // For local jars, collect the jar files in the
                            // Manifest Class-Path, to be scanned later.
                            addManifestClassPath(null, manifestClassPathJars, jarURLConnection);
                        }
                        scanJar(jarURLConnection, null, isLocal);
                    }
                }

                // Scan the jars collected from manifest class-path. Expand
                // the list to include jar files from their manifest classpath.
                if (!manifestClassPathJars.isEmpty()) {
                    List<String> newJars;
                    do {
                        newJars = new ArrayList<>();
                        for (String manifestClassPathJar : manifestClassPathJars) {
                            JarURLConnection jarURLConnection = (JarURLConnection) new URL("jar:" + manifestClassPathJar + "!/").openConnection();
                            jarURLConnection.setUseCaches(false);
                            if (addManifestClassPath(manifestClassPathJars, newJars, jarURLConnection)) {
                                scanJar(jarURLConnection, null, true);
                            }
                        }
                        manifestClassPathJars.addAll(newJars);
                    } while (newJars.size() != 0);
                }
            }

            if (tldMap != null && (isStandalone || EAR_LIB_CLASSLOADER.equals(loader.getClass().getName()))) {
                break;
            }

            loader = loader.getParent();
        }

        if (tldMap != null) {
            for (URI uri : tldMap.keySet()) {
                scanJar((JarURLConnection) new URL("jar:" + uri.toString() + "!/").openConnection(), tldMap.get(uri), false);
            }
        }
    }
    
    /**
     * Scans the given URL for the TLD file META-INF ((or a subdirectory of it) that it represents. If the scanning in is
     * done as part of the ServletContextInitializer, the listeners in the tlds in this jar file are added to the servlet
     * context, and for any TLD that has a <uri> element, an implicit map entry is added to the taglib map.
     *
     * @param URL url the URL to the TLD to process
     * @param isLocal True if the jar file is under WEB-INF false otherwise
     */
    private void scanJar(URL url, boolean isLocal) throws WaspException {
        String resourcePath = url.toString();
        TldInfo[] tldInfos = jarTldCacheLocal.get(resourcePath);

        // Optimize for most common cases: jars known to NOT have tlds
        if (tldInfos != null && tldInfos.length == 0) {
            return;
        }

        // Scan the tld if the jar has not been cached.
        if (tldInfos == null) {
            List<TldInfo> tldInfoA = new ArrayList<>();
            try {
                tldInfoA.add(scanTld(resourcePath, url.getFile(), url.openStream()));
            } catch (IOException ex) {
                logOrThrow(resourcePath, ex);
            }

            tldInfos = tldInfoA.toArray(new TldInfo[tldInfoA.size()]);

            // Update the jar TLD cache
            updateTldCache(resourcePath, tldInfos, isLocal);
        }

        // Iterate over tldinfos to add listeners or to map tldlocations
        for (TldInfo tldInfo : tldInfos) {
            if (scanListeners) {
                addListener(tldInfo, isLocal);
            }
            mapTldLocation(resourcePath, tldInfo, isLocal);
        }
    }
    
    
    /**
     * Scans the given JarURLConnection for TLD files located in META-INF (or a subdirectory of it). If the scanning in is
     * done as part of the ServletContextInitializer, the listeners in the tlds in this jar file are added to the servlet
     * context, and for any TLD that has a <uri> element, an implicit map entry is added to the taglib map.
     *
     * @param jarURLConnection The JarURLConnection to the JAR file to scan
     * @param tldNames the list of tld element to scan. The null value indicates all the tlds in this case.
     * @param isLocal True if the jar file is under WEB-INF false otherwise
     */
    private void scanJar(JarURLConnection jarURLConnection, List<String> tldNames, boolean isLocal) throws WaspException {
        String resourcePath = jarURLConnection.getJarFileURL().toString();
        TldInfo[] tldInfos = jarTldCacheLocal.get(resourcePath);

        // Optimize for most common cases: jars known to NOT have tlds
        if (tldInfos != null && tldInfos.length == 0) {
            closeSilently(jarURLConnection);
            return;
        }

        // Scan the tld if the jar has not been cached.
        if (tldInfos == null) {
            JarFile jarFile = null;
            List<TldInfo> tldInfoA = new ArrayList<>();
            try {
                jarFile = jarURLConnection.getJarFile();
                
                for (String tldFileName : getTLDFileNames(jarFile, tldNames)) {
                    tldInfoA.add(scanTld(resourcePath, tldFileName, jarFile.getInputStream(jarFile.getJarEntry(tldFileName))));
                }
            } catch (IOException ex) {
                logOrThrow(resourcePath, ex);
            } finally {
                closeSilently(jarFile);
            }
            
            tldInfos = tldInfoA.toArray(new TldInfo[tldInfoA.size()]);
            
            // Update the jar TLD cache
            updateTldCache(resourcePath, tldInfos, isLocal);
        }

        // Iterate over tldinfos to add listeners or to map tldlocations
        for (TldInfo tldInfo : tldInfos) {
            if (scanListeners) {
                addListener(tldInfo, isLocal);
            }
            mapTldLocation(resourcePath, tldInfo, isLocal);
        }
    }
    
    private List<String> getTLDFileNames(JarFile jarFile, List<String> tldNames) {
        if (tldNames != null) {
            return tldNames;
        }
        
        List<String> collectedTldNames = new ArrayList<>();
        
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            String name = entries.nextElement().getName();
            
            if (name.startsWith("META-INF/") && name.endsWith(".tld")) {
                collectedTldNames.add(name);
            }
        }
        
        return collectedTldNames;
    }
    
    private void logOrThrow(String resourcePath, IOException ex) throws WaspException {
        if (resourcePath.startsWith(FILE_PROTOCOL) && !new File(resourcePath).exists()) {
            log.log(WARNING, ex, () -> Localizer.getMessage("jsp.warn.nojar", resourcePath));
        } else {
            throw new WaspException(Localizer.getMessage("jsp.error.jar.io", resourcePath), ex);
        }
    }
    
    private void updateTldCache(String resourcePath, TldInfo[] tldInfos, boolean isLocal) {
        jarTldCacheLocal.put(resourcePath, tldInfos);
        if (!isLocal) {
            // Also update the global cache;
            jarTldCache.put(resourcePath, tldInfos);
        }
    }
    
    @SuppressWarnings("unchecked")
    Map<URI, List<String>> getTldMap() {
        /*
         * System jars with tlds may be passed as a special ServletContext attribute Map key: a JarURI Map value: list of tlds
         * in the jar file
         */
        return (Map<URI, List<String>>) servletContext.getAttribute("com.sun.appserv.tld.map");
    }

    @SuppressWarnings("unchecked")
    Map<URI, List<String>> getTldListenerMap() {
        /*
         * System jars with tlds that are known to contain a listener, and may be passed as a special ServletContext attribute
         * Map key: a JarURI Map value: list of tlds in the jar file
         */
        return (Map<URI, List<String>>) servletContext.getAttribute("com.sun.appserv.tldlistener.map");
    }
    
    private void addListener(TldInfo tldInfo, boolean isLocal) {
        String uri = tldInfo.getUri();
        if (!systemUrisJsf.contains(uri) || isLocal && useMyFaces || !isLocal && !useMyFaces) {
            for (String listenerClassName : tldInfo.getListeners()) {
                log.log(FINE, () -> "Add tld listener " + listenerClassName);
                servletContext.addListener(listenerClassName);
            }
        }
    }

    private void mapTldLocation(String resourcePath, TldInfo tldInfo, boolean isLocal) {
        String uri = tldInfo.getUri();
        if (uri == null) {
            return;
        }

        if (isLocal
                // Local tld files override the tlds in the jar files,
                // unless it is in a system jar (except when using myfaces)
                && mappings.get(uri) == null && !systemUris.contains(uri) && (!systemUrisJsf.contains(uri) || useMyFaces)
                || !isLocal
                        // Jars are scanned bottom up, so jars in WEB-INF override
                        // thos in the system (except when using myfaces)
                        && (mappings.get(uri) == null || systemUris.contains(uri) || systemUrisJsf.contains(uri) && !useMyFaces)) {
            String entryName = tldInfo.getEntryName();
            if (log.isLoggable(FINE)) {
                log.fine("Add tld map from tld in " + (isLocal ? "WEB-INF" : "jar: ") + uri + "=>" + resourcePath + "," + entryName);
            }
            mappings.put(uri, new String[] { resourcePath, entryName });
        }
    }
    
    private JarURLConnection getJarURLConnection(URL url) throws IOException {
        URLConnection urlConnection = url.openConnection();
        
        if (urlConnection instanceof JarURLConnection) {
            return (JarURLConnection) urlConnection;
        } 
        
        String urlStr = url.toString();
        if (urlStr.startsWith(FILE_PROTOCOL) && urlStr.endsWith(JAR_FILE_SUFFIX)) {
            return (JarURLConnection) new URL("jar:" + urlStr + "!/").openConnection();
        }
        
        return null;
    }

    /*
     * Add the jars in the manifest Class-Path to the list "jars"
     *
     * @param scannedJars List of jars that has been previously scanned
     *
     * @param newJars List of jars from Manifest Class-Path
     *
     * @return true is the jar file exists
     */
    private boolean addManifestClassPath(List<String> scannedJars, List<String> newJars, JarURLConnection jarURLConnection) {
        Manifest manifest;
        try {
            manifest = jarURLConnection.getManifest();
        } catch (IOException ex) {
            // Maybe non existing jar, ignored
            return false;
        }

        String file = jarURLConnection.getJarFileURL().toString();
        if (!file.contains("WEB-INF")) {
            // Only jar in WEB-INF is considered here
            return true;
        }

        if (manifest == null) {
            return true;
        }

        Attributes attributes = manifest.getMainAttributes();
        String classPath = attributes.getValue("Class-Path");
        if (classPath == null) {
            return true;
        }

        String[] paths = classPath.split(" ");
        int lastIndex = file.lastIndexOf('/');
        if (lastIndex < 0) {
            lastIndex = file.lastIndexOf('\\');
        }
        String baseDir = "";
        if (lastIndex > 0) {
            baseDir = file.substring(0, lastIndex + 1);
        }
        for (String path : paths) {
            String p;
            if (path.startsWith("/") || path.startsWith("\\")) {
                p = "file:" + path;
            } else {
                p = baseDir + path;
            }
            if ((scannedJars == null || !scannedJars.contains(p)) && !newJars.contains(p)) {
                newJars.add(p);
            }
        }
        return true;
    }
    
    private void closeSilently(JarURLConnection jarURLConnection) {
        try {
            jarURLConnection.getJarFile().close();
        } catch (IOException ex) {
            // ignored
        }
    }
    
    private void closeSilently(JarFile jarFile) {
        if (jarFile != null) {
            try {
                jarFile.close();
            } catch (Throwable t) {
                // ignore
            }
        }
    }
    
    private void closeSilently(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Throwable t) {
                // do nothing
            }
        }
    }

    static class TldInfo {
        private final String entryName; // The name of the tld file
        private final String uri; // The uri name for the tld
        private final String[] listeners; // The listeners in the tld

        public TldInfo(String uri, String entryName, String[] listeners) {
            this.uri = uri;
            this.entryName = entryName;
            this.listeners = listeners;
        }

        public String getEntryName() {
            return entryName;
        }

        public String getUri() {
            return uri;
        }

        public String[] getListeners() {
            return listeners;
        }
    }
}
