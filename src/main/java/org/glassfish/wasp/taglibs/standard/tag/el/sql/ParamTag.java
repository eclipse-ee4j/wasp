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

package org.glassfish.wasp.taglibs.standard.tag.el.sql;

import org.glassfish.wasp.taglibs.standard.lang.support.ExpressionEvaluatorManager;
import org.glassfish.wasp.taglibs.standard.tag.common.sql.ParamTagSupport;

import jakarta.servlet.jsp.JspException;

/**
 * Subclass for the JSTL library with EL support.
 *
 * @author Hans Bergsten
 */
public class ParamTag extends ParamTagSupport {

    private String valueEL;

    public void setValue(String valueEL) {
        this.valueEL = valueEL;
    }

    @Override
    public int doStartTag() throws JspException {
        if (valueEL != null) {
            value = ExpressionEvaluatorManager.evaluate("value", valueEL, Object.class, this, pageContext);
        }
        return super.doStartTag();
    }
}
