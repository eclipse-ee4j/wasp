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
 * Rejects the page when the system property {@value #REJECT_PROPERTY} is set, which the tests only set for the
 * application, not for the build: the page translates, but doesn't validate in the running application.
 */
public class RejectingTEI extends TagExtraInfo {

    public static final String REJECT_PROPERTY = "wasp.test.reject";

    @Override
    public ValidationMessage[] validate(TagData data) {
        if (System.getProperty(REJECT_PROPERTY) != null) {
            return new ValidationMessage[] { new ValidationMessage(data.getId(), "Rejected at runtime") };
        }
        return null;
    }
}
