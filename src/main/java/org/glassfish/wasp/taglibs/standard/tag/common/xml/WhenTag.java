/*
 * Copyright (c) 1997-2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.wasp.taglibs.standard.tag.common.xml;

import org.glassfish.wasp.taglibs.standard.tag.common.core.WhenTagSupport;

import jakarta.servlet.jsp.JspTagException;

/**
 * <p>
 * Tag handler for &lt;if&gt; in JSTL's XML library.
 * </p>
 *
 * @author Shawn Bayern
 */

public class WhenTag extends WhenTagSupport {

    // *********************************************************************
    // Constructor and lifecycle management

    // initialize inherited and local state
    public WhenTag() {
        super();
        init();
    }

    // Releases any resources we may have (or inherit)
    @Override
    public void release() {
        super.release();
        init();
    }

    // *********************************************************************
    // Supplied conditional logic

    @Override
    protected boolean condition() throws JspTagException {
        XPathUtil xu = new XPathUtil(pageContext);
        return (xu.booleanValueOf(XPathUtil.getContext(this), select));
    }

    // *********************************************************************
    // Private state

    private String select; // the value of the 'test' attribute

    // *********************************************************************
    // Attribute accessors

    public void setSelect(String select) {
        this.select = select;
    }

    // *********************************************************************
    // Private utility methods

    // resets internal state
    private void init() {
        select = null;
    }
}
