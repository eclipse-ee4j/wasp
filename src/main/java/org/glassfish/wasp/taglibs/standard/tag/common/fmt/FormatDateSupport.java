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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.glassfish.wasp.taglibs.standard.resources.Resources;
import org.glassfish.wasp.taglibs.standard.tag.common.core.Util;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspTagException;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.TagSupport;

/**
 * Support for tag handlers for &lt;formatDate&gt;, the date and time formatting tag in JSTL 1.0.
 *
 * @author Jan Luehe
 */

public abstract class FormatDateSupport extends TagSupport {

    // *********************************************************************
    // Private constants

    private static final String DATE = "date";
    private static final String TIME = "time";
    private static final String DATETIME = "both";

    // *********************************************************************
    // Protected state

    protected Date value; // 'value' attribute
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
            DateFormat formatter = createFormatter(locale);

            // Apply pattern, if present
            if (pattern != null) {
                try {
                    ((SimpleDateFormat) formatter).applyPattern(pattern);
                } catch (ClassCastException cce) {
                    formatter = new SimpleDateFormat(pattern, locale);
                }
            }

            // Set time zone
            TimeZone tz = null;
            if ((timeZone instanceof String) && ((String) timeZone).equals("")) {
                timeZone = null;
            }
            if (timeZone != null) {
                if (timeZone instanceof String) {
                    tz = TimeZone.getTimeZone((String) timeZone);
                } else if (timeZone instanceof TimeZone) {
                    tz = (TimeZone) timeZone;
                } else {
                    throw new JspTagException(Resources.getMessage("FORMAT_DATE_BAD_TIMEZONE"));
                }
            } else {
                tz = TimeZoneSupport.getTimeZone(pageContext, this);
            }
            if (tz != null) {
                formatter.setTimeZone(tz);
            }
            formatted = formatter.format(value);
        } else {
            // no formatting locale available, use Date.toString()
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

    // *********************************************************************
    // Private utility methods

    private DateFormat createFormatter(Locale loc) throws JspException {
        DateFormat formatter = null;

        if ((type == null) || DATE.equalsIgnoreCase(type)) {
            formatter = DateFormat.getDateInstance(Util.getStyle(dateStyle, "FORMAT_DATE_INVALID_DATE_STYLE"), loc);
        } else if (TIME.equalsIgnoreCase(type)) {
            formatter = DateFormat.getTimeInstance(Util.getStyle(timeStyle, "FORMAT_DATE_INVALID_TIME_STYLE"), loc);
        } else if (DATETIME.equalsIgnoreCase(type)) {
            formatter = DateFormat.getDateTimeInstance(Util.getStyle(dateStyle, "FORMAT_DATE_INVALID_DATE_STYLE"),
                    Util.getStyle(timeStyle, "FORMAT_DATE_INVALID_TIME_STYLE"), loc);
        } else {
            throw new JspException(Resources.getMessage("FORMAT_DATE_INVALID_TYPE", type));
        }

        return formatter;
    }
}
