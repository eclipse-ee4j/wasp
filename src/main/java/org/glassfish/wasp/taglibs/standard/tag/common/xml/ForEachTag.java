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

import java.util.List;

import org.glassfish.wasp.taglibs.standard.resources.Resources;

import jakarta.servlet.jsp.JspTagException;
import jakarta.servlet.jsp.jstl.core.LoopTagSupport;

/**
 * <p>
 * Support for the XML library's &lt;forEach&gt; tag.
 * </p>
 *
 * @see jakarta.servlet.jsp.jstl.core.LoopTagSupport
 * @author Shawn Bayern
 */
public class ForEachTag extends LoopTagSupport {

    // *********************************************************************
    // Private state

    private String select; // tag attribute
    private List nodes; // XPath result
    private int nodesIndex; // current index
    private org.w3c.dom.Node current; // current node

    // *********************************************************************
    // Iteration control methods

    // (We inherit semantics and Javadoc from LoopTagSupport.)

    @Override
    protected void prepare() throws JspTagException {
        nodesIndex = 0;
        XPathUtil xu = new XPathUtil(pageContext);
        nodes = xu.selectNodes(XPathUtil.getContext(this), select);
    }

    @Override
    protected boolean hasNext() throws JspTagException {
        return (nodesIndex < nodes.size());
    }

    @Override
    protected Object next() throws JspTagException {
        Object o = nodes.get(nodesIndex++);
        if (!(o instanceof org.w3c.dom.Node)) {
            throw new JspTagException(Resources.getMessage("FOREACH_NOT_NODESET"));
        }
        current = (org.w3c.dom.Node) o;
        return current;
    }

    // *********************************************************************
    // Tag logic and lifecycle management

    // Releases any resources we may have (or inherit)
    @Override
    public void release() {
        init();
        super.release();
    }

    // *********************************************************************
    // Attribute accessors

    public void setSelect(String select) {
        this.select = select;
    }

    public void setBegin(int begin) throws JspTagException {
        this.beginSpecified = true;
        this.begin = begin;
        validateBegin();
    }

    public void setEnd(int end) throws JspTagException {
        this.endSpecified = true;
        this.end = end;
        validateEnd();
    }

    public void setStep(int step) throws JspTagException {
        this.stepSpecified = true;
        this.step = step;
        validateStep();
    }

    // *********************************************************************
    // Public methods for subtags

    /* Retrieves the current context. */
    public org.w3c.dom.Node getContext() throws JspTagException {
        // expose the current node as the context
        return current;
    }

    // *********************************************************************
    // Private utility methods

    private void init() {
        select = null;
        nodes = null;
        nodesIndex = 0;
        current = null;
    }
}
