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
import java.text.ParseException;
import java.util.Locale;
import java.util.TimeZone;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspTagException;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.BodyTagSupport;

import org.glassfish.wasp.taglibs.standard.resources.Resources;
import org.glassfish.wasp.taglibs.standard.tag.common.core.Util;

/**
 * Support for tag handlers for &lt;parseDate&gt;, the date and time parsing tag in JSTL 1.0.
 *
 * @author Jan Luehe
 */

public abstract class ParseDateSupport extends BodyTagSupport {

    // *********************************************************************
    // Private constants

    private static final String DATE = "date";
    private static final String TIME = "time";
    private static final String DATETIME = "both";

    // *********************************************************************
    // Protected state

    protected String value; // 'value' attribute
    protected boolean valueSpecified; // status
    protected String type; // 'type' attribute
    protected String pattern; // 'pattern' attribute
    protected Object timeZone; // 'timeZone' attribute
    protected Locale parseLocale; // 'parseLocale' attribute
    protected String dateStyle; // 'dateStyle' attribute
    protected String timeStyle; // 'timeStyle' attribute

    // *********************************************************************
    // Private state

    private String var; // 'var' attribute
    private int scope; // 'scope' attribute

    // *********************************************************************
    // Constructor and initialization

    public ParseDateSupport() {
        super();
        init();
    }

    private void init() {
        type = dateStyle = timeStyle = null;
        value = pattern = var = null;
        valueSpecified = false;
        timeZone = null;
        scope = PageContext.PAGE_SCOPE;
        parseLocale = null;
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

    @Override
    public int doEndTag() throws JspException {

        String input = null;

        // determine the input by...
        if (valueSpecified) {
            // ... reading 'value' attribute
            input = value;
        } else {
            // ... retrieving and trimming our body
            if (bodyContent != null && bodyContent.getString() != null) {
                input = bodyContent.getString().trim();
            }
        }

        if ((input == null) || input.equals("")) {
            if (var != null) {
                pageContext.removeAttribute(var, scope);
            }
            return EVAL_PAGE;
        }

        /*
         * Set up parsing locale: Use locale specified via the 'parseLocale' attribute (if present), or else determine page's
         * locale.
         */
        Locale locale = parseLocale;
        if (locale == null) {
            locale = SetLocaleSupport.getFormattingLocale(pageContext, this, true, false);
        }
        if (locale == null) {
            throw new JspException(Resources.getMessage("PARSE_DATE_NO_PARSE_LOCALE"));
        }

        // Set up time zone
        TimeZone tz = TimeZoneSupport.getTimeZone(pageContext, this, timeZone, true);
        
        // Create parser
        DateFormatSupport parser = DateFormatSupport.createFormatter(locale, tz, type, dateStyle, timeStyle, pattern, true);

        // Parse date
        Object parsed = null;
        try {
            parsed = parser.parse(input);
        } catch (ParseException pe) {
            throw new JspException(Resources.getMessage("PARSE_DATE_PARSE_ERROR", input), pe);
        }

        if (var != null) {
            pageContext.setAttribute(var, parsed, scope);
        } else {
            try {
                pageContext.getOut().print(parsed);
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
