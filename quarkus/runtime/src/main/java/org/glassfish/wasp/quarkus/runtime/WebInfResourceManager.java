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

import io.undertow.httpcore.OutputChannel;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.util.ETag;
import io.undertow.util.MimeMappings;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Adds the WEB-INF content of the application (e.g. tag library descriptors, tag files, included fragments and files
 * the application reads) to the resources of the ServletContext, as in a web archive, so the application can read them
 * and pages can be parsed again at runtime. The resources of the web application come first. Undertow doesn't serve
 * WEB-INF paths to clients.
 *
 * <p>
 * The directories can be listed (e.g. for the tag files of a tag directory): the files are known from the build.
 */
class WebInfResourceManager implements ResourceManager {

    private static final String WEB_INF = "/WEB-INF/";

    private final ResourceManager webResources;
    private final ClassPathResourceManager classPathResources;

    /** The files and directories, e.g. /WEB-INF/tags/a.tag and /WEB-INF/tags */
    private final TreeSet<String> files = new TreeSet<>();
    private final TreeSet<String> directories = new TreeSet<>();

    /**
     * @param webResources the resources of the web application, or null
     * @param webInfFiles the paths of the files in WEB-INF of the application, e.g. {@code WEB-INF/tags/a.tag}
     * @param classLoader the class loader to load them with
     */
    WebInfResourceManager(ResourceManager webResources, Set<String> webInfFiles, ClassLoader classLoader) {
        this.webResources = webResources;
        this.classPathResources = new ClassPathResourceManager(classLoader, "");

        for (String file : webInfFiles) {
            String path = file.startsWith("/") ? file : "/" + file;
            files.add(path);
            for (int slash = path.lastIndexOf('/'); slash > 0; slash = path.lastIndexOf('/', slash - 1)) {
                directories.add(path.substring(0, slash));
            }
        }
    }

    @Override
    public Resource getResource(String path) throws IOException {
        Resource resource = webResources == null ? null : webResources.getResource(path);
        if (resource != null || path == null || !path.startsWith(WEB_INF)) {
            return resource;
        }

        String normalized = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        if (directories.contains(normalized)) {
            return new DirectoryResource(normalized);
        }

        if (files.contains(normalized)) {
            return classPathResources.getResource(normalized);
        }

        return null;
    }

    @Override
    public void close() throws IOException {
        classPathResources.close();
        if (webResources != null) {
            webResources.close();
        }
    }

    /**
     * A directory, listing its files and directories. The ServletContext lists the resources by their file path, so
     * these have their path in the web application as file path.
     */
    private class DirectoryResource implements Resource {

        private final String path;

        DirectoryResource(String path) {
            this.path = path;
        }

        @Override
        public List<Resource> list() {
            List<Resource> children = new ArrayList<>();
            String prefix = path + "/";
            for (TreeSet<String> paths : List.of(files, directories)) {
                for (String child : paths.tailSet(prefix, false)) {
                    if (!child.startsWith(prefix)) {
                        break;
                    }

                    if (child.indexOf('/', prefix.length()) >= 0) {
                        continue;
                    }

                    try {
                        Resource resource = paths == directories ? new DirectoryResource(child) : classPathResources.getResource(child);
                        if (resource != null) {
                            children.add(paths == directories ? resource : new ListedResource(child, resource));
                        }
                    } catch (IOException e) {
                        // Not listed
                    }
                }
            }
            return children;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public String getName() {
            return path.substring(path.lastIndexOf('/') + 1);
        }

        @Override
        public boolean isDirectory() {
            return true;
        }

        @Override
        public Path getFilePath() {
            return Path.of(path);
        }

        @Override
        public Date getLastModified() {
            return null;
        }

        @Override
        public String getLastModifiedString() {
            return null;
        }

        @Override
        public ETag getETag() {
            return null;
        }

        @Override
        public String getContentType(MimeMappings mimeMappings) {
            return null;
        }

        @Override
        public void serveBlocking(OutputStream outputStream, HttpServerExchange exchange) throws IOException {
            throw new IOException("Cannot serve directory");
        }

        @Override
        public void serveAsync(OutputChannel stream, HttpServerExchange exchange) {
            exchange.setStatusCode(500);
            exchange.endExchange();
        }

        @Override
        public Long getContentLength() {
            return null;
        }

        @Override
        public String getCacheKey() {
            return null;
        }

        @Override
        public File getFile() {
            return null;
        }

        @Override
        public File getResourceManagerRoot() {
            return null;
        }

        @Override
        public Path getResourceManagerRootPath() {
            return null;
        }

        @Override
        public URL getUrl() {
            return null;
        }
    }

    /**
     * A file as listed in a directory: with its path in the web application as file path.
     */
    private record ListedResource(String path, Resource resource) implements Resource {

        @Override
        public Path getFilePath() {
            return Path.of(path);
        }

        @Override
        public Path getResourceManagerRootPath() {
            return null;
        }

        @Override
        public File getResourceManagerRoot() {
            return null;
        }

        @Override
        public String getPath() {
            return resource.getPath();
        }

        @Override
        public Date getLastModified() {
            return resource.getLastModified();
        }

        @Override
        public String getLastModifiedString() {
            return resource.getLastModifiedString();
        }

        @Override
        public ETag getETag() {
            return resource.getETag();
        }

        @Override
        public String getName() {
            return resource.getName();
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public List<Resource> list() {
            return resource.list();
        }

        @Override
        public String getContentType(MimeMappings mimeMappings) {
            return resource.getContentType(mimeMappings);
        }

        @Override
        public void serveBlocking(OutputStream outputStream, HttpServerExchange exchange) throws IOException {
            resource.serveBlocking(outputStream, exchange);
        }

        @Override
        public void serveAsync(OutputChannel stream, HttpServerExchange exchange) {
            resource.serveAsync(stream, exchange);
        }

        @Override
        public Long getContentLength() {
            return resource.getContentLength();
        }

        @Override
        public String getCacheKey() {
            return resource.getCacheKey();
        }

        @Override
        public File getFile() {
            return resource.getFile();
        }

        @Override
        public URL getUrl() {
            return resource.getUrl();
        }
    }
}
