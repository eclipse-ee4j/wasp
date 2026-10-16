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
package org.glassfish.wasp.quarkus.test.validation;

import jakarta.servlet.jsp.tagext.TagData;
import jakarta.servlet.jsp.tagext.TagExtraInfo;
import jakarta.servlet.jsp.tagext.ValidationMessage;

/**
 * Sets a static flag when it validates, which the page reads and resets, the way the Jakarta Pages TCK checks that a
 * tag extra info was called.
 */
public class FlagTEI extends TagExtraInfo {

    private static volatile boolean called;

    @Override
    public ValidationMessage[] validate(TagData data) {
        called = true;
        return null;
    }

    /** @return whether the TEI was called since the last call of this method */
    public static boolean calledAndReset() {
        boolean wasCalled = called;
        called = false;
        return wasCalled;
    }
}
