/*
 * Copyright (c) 1997-2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.wasp.taglibs.standard.tag.common.core;

import java.util.LinkedList;
import java.util.List;

import org.glassfish.wasp.taglibs.standard.resources.Resources;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspTagException;
import jakarta.servlet.jsp.tagext.BodyTagSupport;
import jakarta.servlet.jsp.tagext.Tag;

/**
 * <p>
 * Support for tag handlers for &lt;param&gt;, the URL parameter subtag for &lt;import&gt; in JSTL 1.0.
 * </p>
 *
 * @see ParamParent
 * @see ImportSupport
 * @see UrlSupport
 * @author Shawn Bayern
 */

public abstract class ParamSupport extends BodyTagSupport {

    // *********************************************************************
    // Protected state

    protected String name; // 'name' attribute
    protected String value; // 'value' attribute

    /**
     * There used to be an 'encode' attribute; I've left this as a vestige in case custom subclasses want to use our
     * functionality but NOT encode parameters.
     */
    protected boolean encode = true;

    // *********************************************************************
    // Constructor and initialization

    public ParamSupport() {
        super();
        init();
    }

    private void init() {
        name = value = null;
    }

    // *********************************************************************
    // Tag logic

    // simply send our name and value to our appropriate ancestor
    @Override
    public int doEndTag() throws JspException {
        Tag t = findAncestorWithClass(this, ParamParent.class);
        if (t == null) {
            throw new JspTagException(Resources.getMessage("PARAM_OUTSIDE_PARENT"));
        }

        // take no action for null or empty names
        if (name == null || name.equals("")) {
            return EVAL_PAGE;
        }

        // send the parameter to the appropriate ancestor
        ParamParent parent = (ParamParent) t;
        String value = this.value;
        if (value == null) {
            if (bodyContent == null || bodyContent.getString() == null) {
                value = "";
            } else {
                value = bodyContent.getString().trim();
            }
        }
        if (encode) {
            // FIXME: revert to java.net.URLEncoder.encode(s, enc) once
            // we have a dependency on J2SE 1.4+.
            String enc = pageContext.getResponse().getCharacterEncoding();
            parent.addParameter(Util.URLEncode(name, enc), Util.URLEncode(value, enc));
        } else {
            parent.addParameter(name, value);
        }
        return EVAL_PAGE;
    }

    // Releases any resources we may have (or inherit)
    @Override
    public void release() {
        init();
    }

    // *********************************************************************
    // Support for parameter management

    /**
     * Provides support for aggregating query parameters in URLs. Specifically, accepts a series of parameters, ensuring
     * that - newer parameters will precede older ones in the output URL - all supplied parameters precede those in the
     * input URL
     */
    public static class ParamManager {

        // *********************************
        // Private state

        private List<String> names = new LinkedList<>();
        private List<String> values = new LinkedList<>();
        private boolean done = false;

        // *********************************
        // Public interface

        /** Adds a new parameter to the list. */
        public void addParameter(String name, String value) {
            if (done) {
                throw new IllegalStateException();
            }
            if (name != null) {
                names.add(name);
                if (value != null) {
                    values.add(value);
                } else {
                    values.add("");
                }
            }
        }

        /**
         * Produces a new URL with the stored parameters, in the appropriate order.
         */
        public String aggregateParams(String url) {
            /*
             * Since for efficiency we're destructive to the param lists, we don't want to run multiple times.
             */
            if (done) {
                throw new IllegalStateException();
            }
            done = true;

            //// reverse the order of our two lists
            // Collections.reverse(this.names);
            // Collections.reverse(this.values);

            // build a string from the parameter list
            StringBuffer newParams = new StringBuffer();
            for (int i = 0; i < names.size(); i++) {
                newParams.append(names.get(i)).append("=").append(values.get(i));
                if (i < (names.size() - 1)) {
                    newParams.append("&");
                }
            }

            // insert these parameters into the URL as appropriate
            if (newParams.length() > 0) {
                int questionMark = url.indexOf('?');
                if (questionMark == -1) {
                    return (url + "?" + newParams);
                } else {
                    StringBuilder workingUrl = new StringBuilder(url);
                    workingUrl.insert(questionMark + 1, (newParams + "&"));
                    return workingUrl.toString();
                }
            } else {
                return url;
            }
        }
    }
}
