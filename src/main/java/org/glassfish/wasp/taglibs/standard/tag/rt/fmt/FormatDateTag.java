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

package org.glassfish.wasp.taglibs.standard.tag.rt.fmt;

import java.util.Date;

import org.glassfish.wasp.taglibs.standard.tag.common.fmt.FormatDateSupport;

import jakarta.servlet.jsp.JspTagException;

/**
 * A handler for &lt;formatDate&gt; that supports rtexprvalue-based attributes.
 *
 * @author Jan Luehe
 */

public class FormatDateTag extends FormatDateSupport {

    private static final long serialVersionUID = 1L;

    // *********************************************************************
    // Accessor methods

    // 'value' attribute
    public void setValue(Date value) throws JspTagException {
        this.value = value;
    }

    // 'type' attribute
    public void setType(String type) throws JspTagException {
        this.type = type;
    }

    // 'dateStyle' attribute
    public void setDateStyle(String dateStyle) throws JspTagException {
        this.dateStyle = dateStyle;
    }

    // 'timeStyle' attribute
    public void setTimeStyle(String timeStyle) throws JspTagException {
        this.timeStyle = timeStyle;
    }

    // 'pattern' attribute
    public void setPattern(String pattern) throws JspTagException {
        this.pattern = pattern;
    }

    // 'timeZone' attribute
    public void setTimeZone(Object timeZone) throws JspTagException {
        this.timeZone = timeZone;
    }
}
