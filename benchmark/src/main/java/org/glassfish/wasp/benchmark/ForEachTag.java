/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
package org.glassfish.wasp.benchmark;

import java.util.Iterator;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.TagSupport;

/**
 * A classic iteration tag: exposes each item of a collection, and its index, as page attributes. Classic tags are
 * pooled, so this also exercises tag handler reuse.
 */
public class ForEachTag extends TagSupport {

    private static final long serialVersionUID = 1L;

    private Iterable<?> items;
    private String var;
    private String indexVar;

    private Iterator<?> iterator;
    private int index;

    public void setItems(Iterable<?> items) {
        this.items = items;
    }

    public void setVar(String var) {
        this.var = var;
    }

    public void setIndexVar(String indexVar) {
        this.indexVar = indexVar;
    }

    @Override
    public int doStartTag() throws JspException {
        iterator = items.iterator();
        index = 0;
        return next() ? EVAL_BODY_INCLUDE : SKIP_BODY;
    }

    @Override
    public int doAfterBody() throws JspException {
        index++;
        return next() ? EVAL_BODY_AGAIN : SKIP_BODY;
    }

    @Override
    public int doEndTag() throws JspException {
        pageContext.removeAttribute(var);
        if (indexVar != null) {
            pageContext.removeAttribute(indexVar);
        }
        iterator = null;
        return EVAL_PAGE;
    }

    private boolean next() {
        if (!iterator.hasNext()) {
            return false;
        }
        pageContext.setAttribute(var, iterator.next());
        if (indexVar != null) {
            pageContext.setAttribute(indexVar, index);
        }
        return true;
    }

    @Override
    public void release() {
        super.release();
        items = null;
        var = null;
        indexVar = null;
    }
}
