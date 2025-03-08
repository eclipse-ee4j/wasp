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

package org.glassfish.wasp.taglibs.standard.tag.common.fmt;

import java.io.IOException;
import java.util.Locale;
import java.util.TimeZone;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspTagException;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.TagSupport;

import org.glassfish.wasp.taglibs.standard.tag.common.core.Util;

/**
 * Support for tag handlers for &lt;formatDate&gt;, the date and time formatting tag in JSTL 1.0.
 *
 * @author Jan Luehe
 */

public abstract class FormatDateSupport extends TagSupport {

    // *********************************************************************
    // Protected state

    protected Object value; // 'value' attribute
    protected String type; // 'type' attribute
    protected String pattern; // 'pattern' attribute
    protected Object timeZone; // 'timeZone' attribute
    protected String dateStyle; // 'dateStyle' attribute
    protected String timeStyle; // 'timeStyle' attribute

    // *********************************************************************
    // Private state

    private String var; // 'var' attribute
    private int scope; // 'scope' attribute

    // *********************************************************************
    // Constructor and initialization

    public FormatDateSupport() {
        super();
        init();
    }

    private void init() {
        type = dateStyle = timeStyle = null;
        pattern = var = null;
        value = null;
        timeZone = null;
        scope = PageContext.PAGE_SCOPE;
    }

    // *********************************************************************
    // Tag attributes known at translation time

    public void setVar(String var) {
        this.var = var;
    }

    public void setScope(String scope) {
        this.scope = Util.getScope(scope);
    }

    // *********************************************************************
    // Tag logic

    /*
     * Formats the given date and time.
     */
    @Override
    public int doEndTag() throws JspException {

        String formatted = null;

        if (value == null) {
            if (var != null) {
                pageContext.removeAttribute(var, scope);
            }
            return EVAL_PAGE;
        }

        // Create formatter
        Locale locale = SetLocaleSupport.getFormattingLocale(pageContext, this, true, true);
        if (locale != null) {
            // Set up time zone
            TimeZone tz = TimeZoneSupport.getTimeZone(pageContext, this, timeZone, false);

            DateFormatSupport formatter = DateFormatSupport.createFormatter(locale, tz, type, dateStyle, timeStyle, pattern, false);

            formatted = formatter.format(value);
        } else {
            // no formatting locale available, use toString()
            formatted = value.toString();
        }

        if (var != null) {
            pageContext.setAttribute(var, formatted, scope);
        } else {
            try {
                pageContext.getOut().print(formatted);
            } catch (IOException ioe) {
                throw new JspTagException(ioe.toString(), ioe);
            }
        }

        return EVAL_PAGE;
    }

    // Releases any resources we may have (or inherit)
    @Override
    public void release() {
        init();
    }
}
