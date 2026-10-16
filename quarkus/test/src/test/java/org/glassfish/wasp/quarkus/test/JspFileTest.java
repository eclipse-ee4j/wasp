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
package org.glassfish.wasp.quarkus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Servlets declared with a jsp-file: their own name and init params, a page with a non-standard extension, and
 * load-on-startup.
 */
@ExtendWith(ArquillianExtension.class)
class JspFileTest {

    @ArquillianResource
    private URL base;

    @Deployment(testable = false)
    static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "jspfile.war")
                         .addAsWebResource(new File("src/main/webapp/jspfile/views/report.page"), "views/report.page")
                         .addAsWebResource(new File("src/main/webapp/jspfile/views/startup.jsp"), "views/startup.jsp")
                         .addAsWebResource(new File("src/main/webapp/jspfile/check.jsp"), "check.jsp")
                         .setWebXML(new File("src/main/webapp/jspfile/web.xml"));
    }

    @Test
    void servletsSharingPageKeepTheirOwnConfig() throws Exception {
        assertEquals("reportA:red", Http.get(base, "a").body().trim());
        assertEquals("reportB:blue", Http.get(base, "b").body().trim());
    }

    @Test
    void loadOnStartupInitializesPage() throws Exception {
        assertEquals("startupInit=yes", Http.get(base, "check.jsp").body().trim());
    }
}
