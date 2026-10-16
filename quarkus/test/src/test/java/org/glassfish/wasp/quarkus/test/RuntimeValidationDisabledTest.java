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
 * With quarkus.wasp.validate-at-runtime=false, TEIs and TLVs only run when the pages are translated at build time.
 */
@ExtendWith(ArquillianExtension.class)
class RuntimeValidationDisabledTest {

    @ArquillianResource
    private URL base;

    @Deployment(testable = false)
    static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "validation-off.war")
                         .addAsWebResource(new File("src/main/webapp/validation/flag.jsp"), "flag.jsp")
                         .addAsWebResource(new File("src/main/webapp/validation/reject.jsp"), "reject.jsp")
                         .addAsResource(new File("src/main/webapp/validation/validation-off.properties"), "application.properties")
                         .addAsLibraries(RuntimeValidationTest.validationLibrary());
    }

    @Test
    void teiDoesNotRunInApplication() throws Exception {
        assertEquals("false", Http.get(base, "flag.jsp").body().trim());
    }

    @Test
    void pageRejectedOnlyAtRuntimeIsServed() throws Exception {
        var response = Http.get(base, "reject.jsp");
        assertEquals(200, response.statusCode(), response.body());
        assertEquals("ok", response.body().trim());
    }
}
