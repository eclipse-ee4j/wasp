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

import java.util.Locale;

import org.glassfish.wasp.taglibs.standard.tag.common.fmt.ParseNumberSupport;
import org.glassfish.wasp.taglibs.standard.tag.common.fmt.SetLocaleSupport;

import jakarta.servlet.jsp.JspTagException;

/**
 * A handler for &lt;parseNumber&gt; that supports rtexprvalue-based attributes.
 *
 * @author Jan Luehe
 */
public class ParseNumberTag extends ParseNumberSupport {

    private static final long serialVersionUID = 1L;

    // *********************************************************************
    // Accessor methods

    // 'value' attribute
    public void setValue(String value) throws JspTagException {
        this.value = value;
        this.valueSpecified = true;
    }

    // 'type' attribute
    public void setType(String type) throws JspTagException {
        this.type = type;
    }

    // 'pattern' attribute
    public void setPattern(String pattern) throws JspTagException {
        this.pattern = pattern;
    }

    // 'parseLocale' attribute
    public void setParseLocale(Object loc) throws JspTagException {
        if (loc != null) {
            if (loc instanceof Locale) {
                this.parseLocale = (Locale) loc;
            } else {
                if (!"".equals(loc)) {
                    this.parseLocale = SetLocaleSupport.parseLocale((String) loc);
                }
            }
        }
    }

    // 'integerOnly' attribute
    public void setIntegerOnly(boolean isIntegerOnly) throws JspTagException {
        this.isIntegerOnly = isIntegerOnly;
        this.integerOnlySpecified = true;
    }
}
