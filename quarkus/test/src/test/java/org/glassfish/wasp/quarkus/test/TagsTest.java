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
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * A tag file, a tag handler from a library (WEB-INF/lib), and the listeners declared in tag library descriptors,
 * also of a library no page uses.
 */
@ExtendWith(ArquillianExtension.class)
class TagsTest {

    @ArquillianResource
    private URL base;

    @Deployment(testable = false)
    static WebArchive deployment() {
        JavaArchive library = ShrinkWrap.create(JavaArchive.class, "taglib.jar")
                .addClasses(ShoutTag.class, TagLibraryListener.class)
                .addAsManifestResource(new File("src/main/webapp/tags/lib/taglib.tld"), "taglib.tld");
        JavaArchive unusedLibrary = ShrinkWrap.create(JavaArchive.class, "unused-taglib.jar")
                .addClass(UnusedTagLibraryListener.class)
                .addAsManifestResource(new File("src/main/webapp/tags/lib/unused.tld"), "unused.tld");

        return ShrinkWrap.create(WebArchive.class, "tags.war")
                         .addAsWebResource(new File("src/main/webapp/tags/tags.jsp"), "tags.jsp")
                         .addAsWebInfResource(new File("src/main/webapp/tags/WEB-INF/tags/greet.tag"), "tags/greet.tag")
                         .addAsLibraries(library, unusedLibrary);
    }

    @Test
    void tagFileTagHandlerAndListeners() throws Exception {
        var response = Http.get(base, "tags.jsp");
        assertEquals(200, response.statusCode(), response.body());
        assertEquals("Hello tags|LOUD|registered|registered", response.body().trim());
    }
}
