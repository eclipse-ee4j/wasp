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
import org.glassfish.wasp.taglibs.standard.tag.common.fmt.RequestEncodingSupport;

import jakarta.servlet.jsp.JspException;

/**
 * <p>
 * A handler for &lt;requestEncoding&gt; that accepts attributes as Strings and evaluates them as expressions at
 * runtime.
 * </p>
 *
 * @author Jan Luehe
 */

public class RequestEncodingTag extends RequestEncodingSupport {

    // *********************************************************************
    // 'Private' state (implementation details)

    private String value_; // stores EL-based property

    // *********************************************************************
    // Constructor

    /**
     * Constructs a new RequestEncodingTag. As with TagSupport, subclasses should not provide other constructors and are
     * expected to call the superclass constructor
     */
    public RequestEncodingTag() {
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
        /*
         * Note: we don't check for type mismatches here; we assume the expression evaluator will return the expected type (by
         * virtue of knowledge we give it about what that type is). A ClassCastException here is truly unexpected, so we let it
         * propagate up.
         */

        if (value_ != null) {
            value = (String) ExpressionEvaluatorManager.evaluate("value", value_, String.class, this, pageContext);
        }
    }
}
