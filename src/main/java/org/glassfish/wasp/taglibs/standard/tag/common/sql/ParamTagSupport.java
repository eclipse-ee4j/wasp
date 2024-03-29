/*
 * Copyright (c) 1997, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.wasp.taglibs.standard.tag.common.sql;

import org.glassfish.wasp.taglibs.standard.resources.Resources;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspTagException;
import jakarta.servlet.jsp.jstl.sql.SQLExecutionTag;
import jakarta.servlet.jsp.tagext.BodyTagSupport;

/**
 * <p>
 * Tag handler for &lt;Param&gt; in JSTL, used to set parameter values for a SQL statement.
 * </p>
 *
 * @author Hans Bergsten
 */

public abstract class ParamTagSupport extends BodyTagSupport {
    protected Object value;

    // *********************************************************************
    // Tag logic

    @Override
    public int doEndTag() throws JspException {
        SQLExecutionTag parent = (SQLExecutionTag) findAncestorWithClass(this, SQLExecutionTag.class);
        if (parent == null) {
            throw new JspTagException(Resources.getMessage("SQL_PARAM_OUTSIDE_PARENT"));
        }

        Object paramValue = null;
        if (value != null) {
            paramValue = value;
        } else if (bodyContent != null) {
            paramValue = bodyContent.getString().trim();
            if (((String) paramValue).trim().length() == 0) {
                paramValue = null;
            }
        }

        parent.addSQLParameter(paramValue);
        return EVAL_PAGE;
    }
}
