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
package org.glassfish.wasp.benchmark;

import java.util.Collection;
import java.util.Locale;

/** The Expression Language functions of the benchmark tag library. */
public final class Functions {

    private Functions() {
    }

    public static String upper(String text) {
        return text.toUpperCase(Locale.ROOT);
    }

    public static String join(Collection<?> items, String separator) {
        StringBuilder joined = new StringBuilder();
        for (Object item : items) {
            if (!joined.isEmpty()) {
                joined.append(separator);
            }
            joined.append(item);
        }
        return joined.toString();
    }
}
