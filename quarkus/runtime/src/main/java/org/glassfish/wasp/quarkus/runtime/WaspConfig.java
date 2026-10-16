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
package org.glassfish.wasp.quarkus.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Jakarta Pages (WaSP).
 */
@ConfigMapping(prefix = "quarkus.wasp")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface WaspConfig {

    /**
     * Whether pages that use tag extra infos (TEIs) or tag library validators (TLVs), also through the tag files they use,
     * are validated again in the running application, once, before their first use.
     * <p>
     * Pages are translated and compiled when the application is built, which is when their TEIs and TLVs run. With this
     * enabled, the page and the tag files it uses are also parsed and validated in the running application, which runs
     * their TEIs and TLVs there too.
     * When that fails, requests for the page fail (with status 500), as for a page that could not be translated.
     * <p>
     * Validating again doesn't change what was decided when the page was translated, such as the scripting variables a
     * TEI declares: those must not differ at runtime.
     * <p>
     * Only supported in JVM mode.
     */
    @WithDefault("true")
    boolean validateAtRuntime();
}
