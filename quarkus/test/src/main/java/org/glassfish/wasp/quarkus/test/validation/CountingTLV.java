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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.jsp.tagext.PageData;
import jakarta.servlet.jsp.tagext.TagLibraryValidator;
import jakarta.servlet.jsp.tagext.ValidationMessage;

/** Counts its validations and the instances that did them */
public class CountingTLV extends TagLibraryValidator {

    private static final AtomicInteger VALIDATIONS = new AtomicInteger();
    private static final Set<Integer> INSTANCES = ConcurrentHashMap.newKeySet();

    @Override
    public ValidationMessage[] validate(String prefix, String uri, PageData page) {
        VALIDATIONS.incrementAndGet();
        INSTANCES.add(System.identityHashCode(this));
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    public static int getValidations() {
        return VALIDATIONS.get();
    }

    public static int getInstances() {
        return INSTANCES.size();
    }
}
