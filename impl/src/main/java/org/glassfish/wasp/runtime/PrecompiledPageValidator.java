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
package org.glassfish.wasp.runtime;

import static org.glassfish.wasp.Constants.PRECOMPILED_PAGES_TO_VALIDATE;

import jakarta.servlet.ServletContext;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.glassfish.wasp.WaspException;

/**
 * Validates precompiled pages again in the running application, once per page per web application: parsing and
 * validating a page invokes its tag extra infos and tag library validators, which otherwise only ran when the page was
 * precompiled.
 *
 * <p>
 * The outcome is shared by all servlets serving the page. Requests that arrive while the page is being validated wait
 * for the outcome; a failure is kept, so the page keeps failing.
 */
public final class PrecompiledPageValidator {

    private static final String ATTRIBUTE = PrecompiledPageValidator.class.getName();

    private final Set<String> pagesToValidate;
    private final ConcurrentHashMap<String, FutureTask<Void>> validations = new ConcurrentHashMap<>();

    private PrecompiledPageValidator(Set<String> pagesToValidate) {
        this.pagesToValidate = pagesToValidate;
    }

    /**
     * @return the validator of the web application
     */
    @SuppressWarnings("unchecked")
    public static PrecompiledPageValidator getInstance(ServletContext context) {
        synchronized (PrecompiledPageValidator.class) {
            PrecompiledPageValidator validator = (PrecompiledPageValidator) context.getAttribute(ATTRIBUTE);
            if (validator == null) {
                validator = new PrecompiledPageValidator((Set<String>) context.getAttribute(PRECOMPILED_PAGES_TO_VALIDATE));
                context.setAttribute(ATTRIBUTE, validator);
            }

            return validator;
        }
    }

    /**
     * Validates the page, unless it has been validated before or doesn't need validating.
     *
     * @param jspUri the URI of the page
     * @param validation validates the page
     * @throws WaspException when the page didn't validate, now or before
     */
    public void validate(String jspUri, Callable<Void> validation) throws WaspException {
        if (pagesToValidate != null && !pagesToValidate.contains(jspUri)) {
            return;
        }

        FutureTask<Void> task = validations.computeIfAbsent(jspUri, uri -> new FutureTask<>(validation));

        // Validates the page in this thread, unless another thread is validating it or did
        task.run();

        try {
            task.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof WaspException waspException) {
                throw waspException;
            }
            throw new WaspException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WaspException(e);
        }
    }
}
