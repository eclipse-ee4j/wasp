/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
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

package org.eclipse.wasp.runtime;

import static java.util.Arrays.asList;
import static org.eclipse.wasp.runtime.Classpath.SearchAdvice.AllMatches;
import static org.eclipse.wasp.runtime.Classpath.SearchAdvice.FirstMatchOnly;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

/**
 * @author Jacob Hookom
 * @author Roland Huss
 * @author Ales Justin (ales.justin@jboss.org)
 */
public final class Classpath {

    // Discard any urls that begin with rar: and sar: or end with their counterparts.
    // These should not be looked at for Pages related content.
    private static final String[] PREFIXES_TO_EXCLUDE = { "rar:", "sar:" };
    private static final String[] EXTENSIONS_TO_EXCLUDE = { ".rar", ".sar" };

    public enum SearchAdvice {
        FirstMatchOnly, AllMatches
    }

    public static URL[] search(String prefix, String suffix) throws IOException {
        return search(Thread.currentThread().getContextClassLoader(), prefix, suffix, AllMatches);
    }

    public static URL[] search(ClassLoader cl, String prefix, String suffix) throws IOException {
        return search(cl, prefix, suffix, AllMatches);
    }

    public static URL[] search(ClassLoader classLoader, String prefix, String suffix, SearchAdvice advice) throws IOException {
        List<Enumeration<URL>> urlResources = asList(classLoader.getResources(prefix), classLoader.getResources(prefix + "MANIFEST.MF"));
        Set<URL> allUrls = new LinkedHashSet<URL>();
        
        for (Enumeration<URL> urlResource : urlResources) {
            while (urlResource.hasMoreElements()) {
                URL url = urlResource.nextElement();
                
                // Due to issue 13045 this collection can contain URLs that have their spaces incorrectly escaped
                // by having %20 replaced with %2520. 
                // This quick conditional check catches this particular case and averts it.
                String str = url.getPath();
                if (-1 != str.indexOf("%2520")) {
                    str = url.toExternalForm();
                    str = str.replace("%2520", "%20");
                    url = new URL(str);
                }
                
                JarFile jarFile = getJarFile(url);
                
                if (jarFile != null) {
                    // Strategy 1: search in jar archive
                    searchJar(classLoader, allUrls, jarFile, prefix, suffix, advice);
                } else {
                    // Strategy 2: search in file system directory 
                    boolean searchDone = searchDir(allUrls, new File(URLDecoder.decode(url.getFile(), "UTF-8")), suffix);
                    if (!searchDone) {
                        // Strategy 3: search in URL
                        searchFromURL(allUrls, prefix, suffix, url);
                    }
                }
            }
        }
        
        return allUrls.toArray(new URL[allUrls.size()]);
    }
    
    private static JarFile getJarFile(URL url) throws IOException {
        URLConnection urlConnection = url.openConnection();
        urlConnection.setUseCaches(false);
        
        if (urlConnection instanceof JarURLConnection) {
            return ((JarURLConnection) urlConnection).getJarFile();
        }
        
        return getAlternativeJarFile(url);
    }
    
    /**
     * For URLs to JARs that do not use JarURLConnection - allowed by the servlet spec - attempt to produce a JarFile object
     * all the same. Known servlet engines that function like this include Weblogic and OC4J. This is not a full solution,
     * since an unpacked WAR or EAR will not have JAR "files" as such.
     */
    private static JarFile getAlternativeJarFile(URL url) throws IOException {
        return getAlternativeJarFile(url.getFile());
    }

    static JarFile getAlternativeJarFile(String urlFile) throws IOException {
        JarFile alternativeJarFile = null;
        
        // Trim off any suffix - which is prefixed by "!/" on Weblogic
        int bangSlash = urlFile.indexOf("!/");
        
        // Try the less safe "!", used on OC4J
        int bang = urlFile.indexOf('!');
        int separatorIndex = -1;

        // If either are found, take the first one.
        if (-1 != bangSlash || -1 != bang) {
            if (bangSlash < bang) {
                separatorIndex = bangSlash;
            } else {
                separatorIndex = bang;
            }
        }
        
        if (separatorIndex == -1) {
            return null;
        }

        String jarFileUrl = urlFile.substring(0, separatorIndex);
        
        // And trim off any "file:" prefix.
        if (jarFileUrl.startsWith("file:")) {
            jarFileUrl = jarFileUrl.substring("file:".length());
            jarFileUrl = URLDecoder.decode(jarFileUrl, "UTF-8");
        }
        
        boolean foundExclusion = false;
        for (int i = 0; i < PREFIXES_TO_EXCLUDE.length; i++) {
            if (jarFileUrl.startsWith(PREFIXES_TO_EXCLUDE[i]) || jarFileUrl.endsWith(EXTENSIONS_TO_EXCLUDE[i])) {
                foundExclusion = true;
                break;
            }
        }
        
        if (!foundExclusion) {
            try {
                alternativeJarFile = new JarFile(jarFileUrl);
            } catch (ZipException ze) {
                alternativeJarFile = null;
            }
        }

        return alternativeJarFile;
    }
    
    private static void searchJar(ClassLoader classLoader, Set<URL> urls, JarFile file, String prefix, String suffix, SearchAdvice advice) throws IOException {
        Enumeration<JarEntry> jarEntries = file.entries();
        
        while (jarEntries.hasMoreElements()) {
            JarEntry entry;
            try {
                entry = (JarEntry) jarEntries.nextElement();
            } catch (Throwable t) {
                continue;
            }
            
            String name = entry.getName();
            if (name.startsWith(prefix) && name.endsWith(suffix)) {
                Enumeration<URL> resourcesFromClassLoader = classLoader.getResources(name);
                while (resourcesFromClassLoader.hasMoreElements()) {
                    urls.add(resourcesFromClassLoader.nextElement());
                    if (advice == FirstMatchOnly) {
                        return;
                    }
                }
            }
        }
    }

    private static boolean searchDir(Set<URL> result, File directory, String suffix) throws IOException {
        if (directory.exists() && directory.isDirectory()) {
            File[] filesInDir = directory.listFiles();
            
            // Protect against Windows JDK bugs for listFiles -
            // If it's null (even though it shouldn't be) return false
            if (filesInDir == null) {
                return false;
            }

            for (File fileInDir : filesInDir) {
                if (fileInDir.isDirectory()) {
                    searchDir(result, fileInDir, suffix);
                } else if (fileInDir.getAbsolutePath().endsWith(suffix)) {
                    // result.add(new URL("file:/" + path));
                    result.add(fileInDir.toURL());
                }
            }
            return true;
        }
        
        return false;
    }

    /**
     * Search from URL. Fall back on prefix tokens if not able to read from original url param.
     *
     * @param result the result urls
     * @param prefix the current prefix
     * @param suffix the suffix to match
     * @param url the current url to start search
     *
     * @throws IOException for any error
     */
    private static void searchFromURL(Set<URL> result, String prefix, String suffix, URL url) throws IOException {
        boolean done = false;
        
        InputStream urlInputStream = getInputStream(url);
        if (urlInputStream != null) {
            try (ZipInputStream zipInputStream = urlInputStream instanceof ZipInputStream ? (ZipInputStream) urlInputStream : new ZipInputStream(urlInputStream)) {
                ZipEntry entry = zipInputStream.getNextEntry();
                
                // Initial entry should not be null if we assume this is some inner jar
                done = entry != null;
                while (entry != null) {
                    String entryName = entry.getName();
                    if (entryName.endsWith(suffix)) {
                        String urlString = url.toExternalForm();
                        result.add(new URL(urlString + entryName));
                    }
                    entry = zipInputStream.getNextEntry();
                }
            }
        }
        
        if (!done && prefix.length() > 0) {
            
            // we add '/' at the end since join adds it as well
            String urlString = url.toExternalForm() + "/";
            String[] split = prefix.split("/");
            prefix = join(split, true);
            String end = join(split, false);
            int p = urlString.lastIndexOf(end);

            if (p < 0) {
                return;
            }

            urlString = urlString.substring(0, p);
            for (String prefixToExclude : PREFIXES_TO_EXCLUDE) {
                if (urlString.startsWith(prefixToExclude)) {
                    return;
                }
            }
            
            searchFromURL(result, prefix, suffix, new URL(urlString));
        }
    }

    /**
     * Join tokens, exlude last if param equals true.
     *
     * @param tokens the tokens
     * @param excludeLast do we exclude last token
     *
     * @return joined tokens
     */
    private static String join(String[] tokens, boolean excludeLast) {
        StringBuffer join = new StringBuffer();
        for (int i = 0; i < tokens.length - (excludeLast ? 1 : 0); i++) {
            join.append(tokens[i]).append("/");
        }
        return join.toString();
    }

    /**
     * Open input stream from url. Ignore any errors.
     *
     * @param url the url to open
     *
     * @return input stream or null if not possible
     */
    private static InputStream getInputStream(URL url) {
        try {
            return url.openStream();
        } catch (Throwable t) {
            return null;
        }
    }

    

   

}
