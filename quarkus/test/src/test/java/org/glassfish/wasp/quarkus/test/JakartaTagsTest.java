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

import org.glassfish.wasp.quarkus.test.library.LibraryBean;
import org.glassfish.wasp.quarkus.test.library.LibraryMessages;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The Jakarta Tags that WaSP implements: core, formatting and functions, with a loop status in the Expression Language,
 * and XML, with XPath and XSLT. Also a bean and a resource bundle of a library, which pages use by reflection.
 */
@ExtendWith(ArquillianExtension.class)
class JakartaTagsTest {

    @ArquillianResource
    private URL base;

    @Deployment(testable = false)
    static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "jstl.war")
                         .addAsWebResource(new File("src/main/webapp/jstl/jstl.jsp"), "jstl.jsp")
                         .addAsWebResource(new File("src/main/webapp/jstl/xml.jsp"), "xml.jsp")
                         .addAsWebResource(new File("src/main/webapp/jstl/library.jsp"), "library.jsp")
                         .addAsLibraries(ShrinkWrap.create(JavaArchive.class, "library.jar")
                                                   .addClasses(LibraryBean.class, LibraryMessages.class));
    }

    @Test
    void coreFormattingAndFunctions() throws Exception {
        var response = Http.get(base, "jstl.jsp");
        assertEquals(200, response.statusCode(), response.body());
        assertEquals("1,2,3|TAGS|4|&lt;b&gt;|1,234.5", response.body().trim());
    }

    @Test
    void xml() throws Exception {
        var response = Http.get(base, "xml.jsp");
        assertEquals(200, response.statusCode(), response.body());
        assertEquals("one,two,|2-param", response.body().trim());
    }

    @Test
    void libraryBeanAndBundle() throws Exception {
        var response = Http.get(base, "library.jsp");
        assertEquals(200, response.statusCode(), response.body());
        assertEquals("library|Hello from a library", response.body().trim());
    }
}
