/*
 * Copyright (c) 1997-2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2004 The Apache Software Foundation
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

package org.glassfish.wasp.taglibs.standard.tag.rt.core;

import org.glassfish.wasp.taglibs.standard.tag.common.core.SetSupport;

/**
 * Tag handler for &lt;set&gt; in JSTL's rtexprvalue library.
 *
 * @author Shawn Bayern
 */
public class SetTag extends SetSupport {

    private static final long serialVersionUID = 1L;

    // *********************************************************************
    // Accessors

    // for tag attribute
    public void setValue(Object value) {
        this.value = value;
        this.valueSpecified = true;
    }

    // for tag attribute
    public void setTarget(Object target) {
        this.target = target;
    }

    // for tag attribute
    public void setProperty(String property) {
        this.property = property;
    }
}
