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

import java.io.IOException;

import org.glassfish.wasp.Constants;
import org.glassfish.wasp.servlet.JspServlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Serves the page of a Servlet declared with a jsp-file. WaSP's {@link JspServlet} serves the page named in the
 * request attribute {@link Constants#JSP_FILE} (the convention of GlassFish and Tomcat), and removes it again.
 */
public class JspFileServlet extends JspServlet {

    private static final long serialVersionUID = 1L;

    /** The init parameter with the jsp-file */
    public static final String JSP_FILE_INIT_PARAM = "org.glassfish.wasp.quarkus.jspFile";

    /** The init parameter that's set (to "true") when the Servlet is loaded on startup */
    public static final String LOAD_ON_STARTUP_INIT_PARAM = "org.glassfish.wasp.quarkus.loadOnStartup";

    private String jspFile;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        jspFile = config.getInitParameter(JSP_FILE_INIT_PARAM);

        // A Servlet loaded on startup initializes its page as well
        if (Boolean.parseBoolean(config.getInitParameter(LOAD_ON_STARTUP_INIT_PARAM))) {
            try {
                preload(jspFile);
            } catch (IOException e) {
                throw new ServletException(e);
            }
        }
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute(Constants.JSP_FILE, jspFile);
        super.service(request, response);
    }
}
