/*
 * Copyright (c) 1997-2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 * Copyright (c) 2020 Payara Services Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.glassfish.wasp.taglibs.standard.extra.spath;

import java.util.List;

/**
 * <p>
 * Represents a 'step' in an SPath expression.
 * </p>
 *
 * @author Shawn Bayern
 */
public class Step {

    private boolean depthUnlimited;
    private String name;
    private List<Predicate> predicates;

    // Record a few things for efficiency...
    private String uri, localPart;

    /**
     * Constructs a new Step object, given a name and a (possibly null) list of predicates.
     *
     * <p>
     * A boolean is also passed, indicating whether this particular Step is relative to the 'descendent-or-self'
     * axis of the node courrently under consideration.
     * If true, it is; if false, then this Step is rooted as a direct child of the node under consideration.
     */
    public Step(boolean depthUnlimited, String name, List<Predicate> predicates) {
        if (name == null) {
            throw new IllegalArgumentException("non-null name required");
        }

        this.depthUnlimited = depthUnlimited;
        this.name = name;
        this.predicates = predicates;
    }

    /**
     * Returns true if the given name matches the Step object's name, taking into account the Step object's wildcards;
     * returns false otherwise.
     */
    public boolean isMatchingName(String uri, String localPart) {
        // Check and normalize arguments
        if (localPart == null) {
            throw new IllegalArgumentException("need non-null localPart");
        }
        if (uri != null && uri.equals("")) {
            uri = null;
        }

        // Split name into uri/localPart if we haven't done so already
        if (this.localPart == null && this.uri == null) {
            parseStepName();
        }

        // Generic wildcard
        if (this.uri == null && this.localPart.equals("*")) {
            return true;
        }

        // Match will null namespace
        if (uri == null && this.uri == null && localPart.equals(this.localPart)) {
            return true;
        }

        if (uri != null && this.uri != null && uri.equals(this.uri)) {
            // Exact match
            // namespace-specific wildcard
            if (localPart.equals(this.localPart) || this.localPart.equals("*")) {
                return true;
            }
        }

        // No match
        return false;
    }

    /** Returns true if the Step's depth is unlimited, false otherwise. */
    public boolean isDepthUnlimited() {
        return depthUnlimited;
    }

    /** Returns the Step's node name. */
    public String getName() {
        return name;
    }

    /** Returns a list of this Step object's predicates. */
    public List<Predicate> getPredicates() {
        return predicates;
    }

    /** Lazily computes some information about our name. */
    private void parseStepName() {
        String prefix;
        int colonIndex = name.indexOf(":");

        if (colonIndex == -1) {
            // No colon, so localpart is simply name (even if it's "*")
            prefix = null;
            localPart = name;
        } else {
            prefix = name.substring(0, colonIndex);
            localPart = name.substring(colonIndex + 1);
        }

        uri = mapPrefix(prefix);
    }

    /** Returns a URI for the given prefix, given our mappings. */
    private String mapPrefix(String prefix) {
        // Ability to specify a mapping is, as of yet, unimplemented
        if (prefix == null) {
            return null;
        }

        throw new IllegalArgumentException("unknown prefix '" + prefix + "'");
    }
}
