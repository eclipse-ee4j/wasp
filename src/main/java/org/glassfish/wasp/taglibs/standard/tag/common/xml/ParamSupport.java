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

import org.glassfish.wasp.taglibs.standard.resources.Resources;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspTagException;
import jakarta.servlet.jsp.tagext.BodyTagSupport;
import jakarta.servlet.jsp.tagext.Tag;

/**
 * <p>
 * Support for tag handlers for &lt;param&gt;, the XML parameter subtag for &lt;transformt&lt;.
 * </p>
 *
 * @see TransformSupport
 * @author Shawn Bayern
 */

public abstract class ParamSupport extends BodyTagSupport {

    // *********************************************************************
    // Protected state

    protected String name; // 'name' attribute
    protected Object value; // 'value' attribute

    // *********************************************************************
    // Constructor and initialization

    public ParamSupport() {
        super();
        init();
    }

    private void init() {
        name = null;
        value = null;
    }

    // *********************************************************************
    // Tag logic

    // simply send our name and value to our parent <transform> tag
    @Override
    public int doEndTag() throws JspException {
        Tag t = findAncestorWithClass(this, TransformSupport.class);
        if (t == null) {
            throw new JspTagException(Resources.getMessage("PARAM_OUTSIDE_TRANSFORM"));
        }
        TransformSupport parent = (TransformSupport) t;

        Object value = this.value;
        if (value == null) {
            if (bodyContent == null || bodyContent.getString() == null) {
                value = "";
            } else {
                value = bodyContent.getString().trim();
            }
        }
        parent.addParameter(name, value);
        return EVAL_PAGE;
    }

    // Releases any resources we may have (or inherit)
    @Override
    public void release() {
        init();
    }
}
