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

package org.glassfish.wasp.taglibs.standard.tag.el.fmt;

import org.glassfish.wasp.taglibs.standard.lang.support.ExpressionEvaluatorManager;
import org.glassfish.wasp.taglibs.standard.tag.common.fmt.TimeZoneSupport;

import jakarta.servlet.jsp.JspException;

/**
 * <p>
 * A handler for &lt;timeZone&gt; that accepts attributes as Strings and evaluates them as expressions at runtime.
 * </p>
 *
 * @author Shawn Bayern
 * @author Jan Luehe
 */

public class TimeZoneTag extends TimeZoneSupport {

    // *********************************************************************
    // 'Private' state (implementation details)

    private String value_; // stores EL-based property

    // *********************************************************************
    // Constructor

    /**
     * Constructs a new TimeZoneTag. As with TagSupport, subclasses should not provide other constructors and are expected
     * to call the superclass constructor
     */
    public TimeZoneTag() {
        super();
        init();
    }

    // *********************************************************************
    // Tag logic

    // evaluates expression and chains to parent
    @Override
    public int doStartTag() throws JspException {

        // evaluate any expressions we were passed, once per invocation
        evaluateExpressions();

        // chain to the parent implementation
        return super.doStartTag();
    }

    // Releases any resources we may have (or inherit)
    @Override
    public void release() {
        super.release();
        init();
    }

    // *********************************************************************
    // Accessor methods

    // for EL-based attribute
    public void setValue(String value_) {
        this.value_ = value_;
    }

    // *********************************************************************
    // Private (utility) methods

    // (re)initializes state (during release() or construction)
    private void init() {
        // null implies "no expression"
        value_ = null;
    }

    // Evaluates expressions as necessary
    private void evaluateExpressions() throws JspException {

        // 'value' attribute (mandatory)
        value = ExpressionEvaluatorManager.evaluate("value", value_, Object.class, this, pageContext);
    }
}
