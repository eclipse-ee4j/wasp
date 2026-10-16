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

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The base of the class generated for a page that could not be translated when the application was built. It has the
 * name of the page class, so WaSP loads it as usual, and fails with the translation error: already in init, so a
 * precompile request (jsp_precompile) fails as well.
 */
public abstract class TranslationFailedPage extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final String translationError;

    protected TranslationFailedPage(String translationError) {
        this.translationError = translationError;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        throw new ServletException(translationError);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        throw new ServletException(translationError);
    }
}
