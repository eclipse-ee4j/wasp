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
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Property groups (an include prelude), include, forward, an error page, and no exposure of page sources or WEB-INF.
 */
@ExtendWith(ArquillianExtension.class)
class ConfigAndDispatchTest {

    @ArquillianResource
    private URL base;

    @Deployment(testable = false)
    static WebArchive deployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "config.war")
                .addAsWebResource(new File("src/main/webapp/config/pg/page.jsp"), "pg/page.jsp")
                .addAsWebInfResource(new File("src/main/webapp/config/WEB-INF/prelude.jspf"), "prelude.jspf")
                .setWebXML(new File("src/main/webapp/config/web.xml"));
        for (String page : new String[] { "includer.jsp", "included.jsp", "forwarder.jsp", "forwarded.jsp", "thrower.jsp", "error.jsp" }) {
            archive.addAsWebResource(new File("src/main/webapp/config/" + page), page);
        }
        return archive;
    }

    @Test
    void includePrelude() throws Exception {
        assertEquals("prelude|\npage", Http.get(base, "pg/page.jsp").body().trim());
    }

    @Test
    void include() throws Exception {
        assertEquals("before|included\n|after", Http.get(base, "includer.jsp").body().trim());
    }

    @Test
    void forward() throws Exception {
        assertEquals("forwarded", Http.get(base, "forwarder.jsp").body().trim());
    }

    @Test
    void errorPage() throws Exception {
        assertEquals("error:thrown", Http.get(base, "thrower.jsp").body().trim());
    }

    @Test
    void noSourceOrWebInfExposure() throws Exception {
        assertFalse(Http.get(base, "includer.jsp").body().contains("<%"));
        assertEquals(404, Http.get(base, "WEB-INF/prelude.jspf").statusCode());
        assertEquals(404, Http.get(base, "WEB-INF/web.xml").statusCode());
    }
}
