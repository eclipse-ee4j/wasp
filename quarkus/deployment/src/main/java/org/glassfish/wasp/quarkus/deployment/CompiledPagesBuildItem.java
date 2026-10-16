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
package org.glassfish.wasp.quarkus.deployment;

import java.util.List;
import java.util.Set;

import org.glassfish.wasp.quarkus.runtime.TldLocation;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * What the runtime needs to know about the pages compiled at build time to validate them again.
 */
final class CompiledPagesBuildItem extends SimpleBuildItem {

    private final Set<String> pagesUsingTranslationValidators;
    private final Set<String> pagesUsingJarResources;
    private final List<TldLocation> tldLocations;
    private final Set<String> webInfFiles;

    CompiledPagesBuildItem(Set<String> pagesUsingTranslationValidators, Set<String> pagesUsingJarResources,
            List<TldLocation> tldLocations, Set<String> webInfFiles) {
        this.pagesUsingTranslationValidators = pagesUsingTranslationValidators;
        this.pagesUsingJarResources = pagesUsingJarResources;
        this.tldLocations = tldLocations;
        this.webInfFiles = webInfFiles;
    }

    /**
     * @return the URIs of the pages that compiled and use tag extra infos or tag library validators
     */
    Set<String> getPagesUsingTranslationValidators() {
        return pagesUsingTranslationValidators;
    }

    /**
     * @return the URIs of the pages that compiled and whose translation read a tag library descriptor or a tag file from
     *         a jar, which a native executable doesn't have
     */
    Set<String> getPagesUsingJarResources() {
        return pagesUsingJarResources;
    }

    /**
     * @return the locations of the tag library descriptors, as resolved when the pages were compiled
     */
    List<TldLocation> getTldLocations() {
        return tldLocations;
    }

    /**
     * @return the files in WEB-INF of the application roots (not in META-INF/resources), e.g. WEB-INF/tags/a.tag
     */
    Set<String> getWebInfFiles() {
        return webInfFiles;
    }
}
