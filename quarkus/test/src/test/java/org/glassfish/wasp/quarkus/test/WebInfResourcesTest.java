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
 * The files in WEB-INF are resources of the ServletContext, as in a web archive, also when no page is validated again
 * at runtime. Clients can't request them.
 */
@ExtendWith(ArquillianExtension.class)
class WebInfResourcesTest {

    @ArquillianResource
    private URL base;

    @Deployment(testable = false)
    static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "webinf.war")
                         .addAsWebResource(new File("src/main/webapp/webinf/webinf.jsp"), "webinf.jsp")
                         .addAsWebInfResource(new File("src/main/webapp/webinf/WEB-INF/data.properties"), "data.properties");
    }

    @Test
    void pageReadsWebInfFile() throws Exception {
        var response = Http.get(base, "webinf.jsp");
        assertEquals(200, response.statusCode(), response.body());
        assertEquals("Hello from WEB-INF|true", response.body().trim());
    }

    @Test
    void clientCantRequestWebInfFile() throws Exception {
        assertEquals(404, Http.get(base, "WEB-INF/data.properties").statusCode());
    }
}
