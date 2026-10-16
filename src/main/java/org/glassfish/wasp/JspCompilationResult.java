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
package org.glassfish.wasp;

/**
 * The result of compiling (translating) one page with {@link JspC}.
 *
 * @param jspUri the URI of the page, relative to the web application root
 * @param className the fully qualified name of the class generated for the page, which is also the name the runtime
 *        loads for it; null if the translation failed before the name was known
 * @param error the translation error, or null if the page compiled
 * @param usesTranslationValidators true if translating the page invoked a TagExtraInfo or a TagLibraryValidator
 * @param usesJarResources true if translating the page read a tag library descriptor or a tag file from a jar
 */
public record JspCompilationResult(String jspUri, String className, WaspException error, boolean usesTranslationValidators,
        boolean usesJarResources) {

    public JspCompilationResult(String jspUri, String className, WaspException error) {
        this(jspUri, className, error, false, false);
    }

    /**
     * @return true if the page compiled
     */
    public boolean isSuccess() {
        return error == null;
    }
}
