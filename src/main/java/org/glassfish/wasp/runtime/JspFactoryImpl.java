/*
 * Copyright (c) 2024 Contributors to Eclipse Foundation.
 * Copyright (c) 1997, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.security.PrivilegedAction;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.wasp.Constants;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.jsp.JspApplicationContext;
import jakarta.servlet.jsp.JspEngineInfo;
import jakarta.servlet.jsp.JspFactory;
import jakarta.servlet.jsp.PageContext;

/**
 * Implementation of JspFactory.
 *
 * @author Anil K. Vijendran
 * @author Kin-man Chung
 */
public class JspFactoryImpl extends JspFactory {

    // Logger
    private static Logger log = Logger.getLogger(JspFactoryImpl.class.getName());

    private static final String SPEC_VERSION = "4.0";

    // Pooling PageContextImpl intances are known to leak memories, see
    // https://glassfish.dev.java.net/issues/show_bug.cgi?id=8601
    // So pooling is off by default. If for any reason, backwards
    // compatibility is required, set the system property to true.
    private static final boolean USE_POOL = Boolean.getBoolean("org.glassfish.wasp.runtime.JspFactoryImpl.USE_POOL");

    // Per-thread pool of PageContext objects
    private ThreadLocal<LinkedList<PageContext>> pool = new ThreadLocal<LinkedList<PageContext>>() {
        @Override
        protected synchronized LinkedList<PageContext> initialValue() {
            return new LinkedList<>();
        }
    };

    @Override
    public PageContext getPageContext(Servlet servlet, ServletRequest request, ServletResponse response, String errorPageURL, boolean needsSession,
            int bufferSize, boolean autoflush) {

        return internalGetPageContext(servlet, request, response, errorPageURL, needsSession, bufferSize, autoflush);
    }

    @Override
    public void releasePageContext(PageContext pageContext) {
        if (pageContext == null) {
            return;
        }

        internalReleasePageContext(pageContext);
    }

    @Override
    public JspEngineInfo getEngineInfo() {
        return new JspEngineInfo() {
            @Override
            public String getSpecificationVersion() {
                return SPEC_VERSION;
            }
        };
    }

    @Override
    public JspApplicationContext getJspApplicationContext(ServletContext context) {
        return JspApplicationContextImpl.findJspApplicationContext(context);
    }

    private PageContext internalGetPageContext(Servlet servlet, ServletRequest request, ServletResponse response, String errorPageURL, boolean needsSession,
            int bufferSize, boolean autoflush) {
        try {
            PageContext pc = null;
            if (USE_POOL) {
                LinkedList<PageContext> pcPool = pool.get();
                if (!pcPool.isEmpty()) {
                    pc = pcPool.removeFirst();
                }
                if (pc == null) {
                    pc = new PageContextImpl(this);
                }
            } else {
                pc = new PageContextImpl(this);
            }
            pc.initialize(servlet, request, response, errorPageURL, needsSession, bufferSize, autoflush);
            return pc;
        } catch (Throwable ex) {
            /* FIXME: need to do something reasonable here!! */
            log.log(Level.SEVERE, "Exception initializing page context", ex);
            return null;
        }
    }

    private void internalReleasePageContext(PageContext pc) {
        pc.release();
        if (USE_POOL && pc instanceof PageContextImpl) {
            LinkedList<PageContext> pcPool = pool.get();
            pcPool.addFirst(pc);
        }
    }

    private class PrivilegedGetPageContext implements PrivilegedAction<PageContext> {

        private JspFactoryImpl factory;
        private Servlet servlet;
        private ServletRequest request;
        private ServletResponse response;
        private String errorPageURL;
        private boolean needsSession;
        private int bufferSize;
        private boolean autoflush;

        PrivilegedGetPageContext(JspFactoryImpl factory, Servlet servlet, ServletRequest request, ServletResponse response, String errorPageURL,
                boolean needsSession, int bufferSize, boolean autoflush) {
            this.factory = factory;
            this.servlet = servlet;
            this.request = request;
            this.response = response;
            this.errorPageURL = errorPageURL;
            this.needsSession = needsSession;
            this.bufferSize = bufferSize;
            this.autoflush = autoflush;
        }

        @Override
        public PageContext run() {
            return factory.internalGetPageContext(servlet, request, response, errorPageURL, needsSession, bufferSize, autoflush);
        }
    }

    private class PrivilegedReleasePageContext implements PrivilegedAction<Object> {

        private JspFactoryImpl factory;
        private PageContext pageContext;

        PrivilegedReleasePageContext(JspFactoryImpl factory, PageContext pageContext) {
            this.factory = factory;
            this.pageContext = pageContext;
        }

        @Override
        public Object run() {
            factory.internalReleasePageContext(pageContext);
            return null;
        }
    }
}
