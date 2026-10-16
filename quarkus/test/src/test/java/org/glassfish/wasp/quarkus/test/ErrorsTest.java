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
 * Pages that compiled, pages that failed to translate, missing pages, and precompile requests (jsp_precompile).
 */
@ExtendWith(ArquillianExtension.class)
class ErrorsTest {

    @ArquillianResource
    private URL base;

    @Deployment(testable = false)
    static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "errors.war")
                         .addAsWebResource(new File("src/main/webapp/errors/ok.jsp"), "ok.jsp")
                         .addAsWebResource(new File("src/main/webapp/errors/broken.jsp"), "broken.jsp");
    }

    @Test
    void compiledPage() throws Exception {
        var response = Http.get(base, "ok.jsp");
        assertEquals(200, response.statusCode());
        assertEquals("executed", response.body().trim());
    }

    @Test
    void pageThatFailedTranslation() throws Exception {
        assertEquals(500, Http.get(base, "broken.jsp").statusCode());
    }

    @Test
    void missingPage() throws Exception {
        assertEquals(404, Http.get(base, "missing.jsp").statusCode());
    }

    @Test
    void precompileDoesNotExecute() throws Exception {
        for (String query : new String[] { "", "=true", "=false" }) {
            var response = Http.get(base, "ok.jsp?jsp_precompile" + query);
            assertEquals(200, response.statusCode(), query);
            assertEquals("", response.body().trim(), query);
        }
    }

    @Test
    void precompileWithInvalidValue() throws Exception {
        assertEquals(500, Http.get(base, "ok.jsp?jsp_precompile=maybe").statusCode());
    }

    @Test
    void precompileOfPageThatFailedTranslation() throws Exception {
        assertEquals(500, Http.get(base, "broken.jsp?jsp_precompile").statusCode());
    }
}
