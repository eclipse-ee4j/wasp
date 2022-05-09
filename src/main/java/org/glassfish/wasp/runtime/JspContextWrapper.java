/*
 * Copyright (c) 2022, 2022 Contributors to the Eclipse Foundation.
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

package org.glassfish.wasp.runtime;

import static jakarta.servlet.jsp.tagext.VariableInfo.AT_BEGIN;
import static jakarta.servlet.jsp.tagext.VariableInfo.AT_END;
import static jakarta.servlet.jsp.tagext.VariableInfo.NESTED;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.glassfish.wasp.compiler.Localizer;

import jakarta.el.ELContext;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.JspContext;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.el.ExpressionEvaluator;
import jakarta.servlet.jsp.el.VariableResolver;
import jakarta.servlet.jsp.tagext.BodyContent;
import jakarta.servlet.jsp.tagext.JspTag;

/**
 * Implementation of a JSP Context Wrapper.
 *
 * The JSP Context Wrapper is a JspContext created and maintained by a tag handler implementation. It wraps the Invoking
 * JSP Context, that is, the JspContext instance passed to the tag handler by the invoking page via setJspContext().
 *
 * @author Kin-man Chung
 * @author Jan Luehe
 */
public class JspContextWrapper extends PageContext {

    // Tag for this wrapper is created
    private JspTag jspTag;

    // Invoking JSP context
    private PageContext invokingJspCtxt;

    private Hashtable<String, Object> pageAttributes;

    // ArrayList of NESTED scripting variables
    private List<String> nestedVars;

    // ArrayList of AT_BEGIN scripting variables
    private List<String> atBeginVars;

    // ArrayList of AT_END scripting variables
    private List<String> atEndVars;

    private Map<String, String> aliases;
    private Map<String, Object> originalNestedVars;
    private ELContext elContext;

    public JspContextWrapper(JspTag jspTag, JspContext jspContext, ArrayList<String> nestedVars, ArrayList<String> atBeginVars, ArrayList<String> atEndVars,
            Map<String, String> aliases) {
        this.jspTag = jspTag;
        this.invokingJspCtxt = (PageContext) jspContext;
        this.nestedVars = nestedVars;
        this.atBeginVars = atBeginVars;
        this.atEndVars = atEndVars;
        this.pageAttributes = new Hashtable<>(16);
        this.aliases = aliases;

        if (nestedVars != null) {
            this.originalNestedVars = new HashMap<>(nestedVars.size());
        }
        syncBeginTagFile();
    }

    @Override
    public void initialize(Servlet servlet, ServletRequest request, ServletResponse response, String errorPageURL, boolean needsSession, int bufferSize,
            boolean autoFlush) throws IOException, IllegalStateException, IllegalArgumentException {
    }

    @Override
    public Object getAttribute(String name) {
        if (name == null) {
            throw new NullPointerException(Localizer.getMessage("jsp.error.attribute.null_name"));
        }

        return pageAttributes.get(name);
    }

    @Override
    public Object getAttribute(String name, int scope) {
        if (name == null) {
            throw new NullPointerException(Localizer.getMessage("jsp.error.attribute.null_name"));
        }

        if (scope == PAGE_SCOPE) {
            return pageAttributes.get(name);
        }

        return invokingJspCtxt.getAttribute(name, scope);
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (name == null) {
            throw new NullPointerException(Localizer.getMessage("jsp.error.attribute.null_name"));
        }

        if (value != null) {
            pageAttributes.put(name, value);
        } else {
            removeAttribute(name, PAGE_SCOPE);
        }
    }

    @Override
    public void setAttribute(String name, Object value, int scope) {
        if (name == null) {
            throw new NullPointerException(Localizer.getMessage("jsp.error.attribute.null_name"));
        }

        if (scope == PAGE_SCOPE) {
            if (value != null) {
                pageAttributes.put(name, value);
            } else {
                removeAttribute(name, PAGE_SCOPE);
            }
        } else {
            invokingJspCtxt.setAttribute(name, value, scope);
        }
    }

    @Override
    public Object findAttribute(String name) {
        if (name == null) {
            throw new NullPointerException(Localizer.getMessage("jsp.error.attribute.null_name"));
        }

        Object attribute = pageAttributes.get(name);
        if (attribute == null) {
            attribute = invokingJspCtxt.getAttribute(name, REQUEST_SCOPE);
            if (attribute == null) {
                if (getSession() != null) {
                    attribute = invokingJspCtxt.getAttribute(name, SESSION_SCOPE);
                }
                if (attribute == null) {
                    attribute = invokingJspCtxt.getAttribute(name, APPLICATION_SCOPE);
                }
            }
        }

        return attribute;
    }

    @Override
    public void removeAttribute(String name) {
        if (name == null) {
            throw new NullPointerException(Localizer.getMessage("jsp.error.attribute.null_name"));
        }

        pageAttributes.remove(name);
        invokingJspCtxt.removeAttribute(name, REQUEST_SCOPE);
        if (getSession() != null) {
            invokingJspCtxt.removeAttribute(name, SESSION_SCOPE);
        }
        invokingJspCtxt.removeAttribute(name, APPLICATION_SCOPE);
    }

    @Override
    public void removeAttribute(String name, int scope) {
        if (name == null) {
            throw new NullPointerException(Localizer.getMessage("jsp.error.attribute.null_name"));
        }

        if (scope == PAGE_SCOPE) {
            pageAttributes.remove(name);
        } else {
            invokingJspCtxt.removeAttribute(name, scope);
        }
    }

    @Override
    public int getAttributesScope(String name) {
        if (name == null) {
            throw new NullPointerException(Localizer.getMessage("jsp.error.attribute.null_name"));
        }

        if (pageAttributes.get(name) != null) {
            return PAGE_SCOPE;
        } else {
            return invokingJspCtxt.getAttributesScope(name);
        }
    }

    @Override
    public Enumeration<String> getAttributeNamesInScope(int scope) {
        if (scope == PAGE_SCOPE) {
            return pageAttributes.keys();
        }

        return invokingJspCtxt.getAttributeNamesInScope(scope);
    }

    @Override
    public void release() {
        invokingJspCtxt.release();
    }

    @Override
    public JspWriter getOut() {
        return invokingJspCtxt.getOut();
    }

    @Override
    public HttpSession getSession() {
        return invokingJspCtxt.getSession();
    }

    @Override
    public Object getPage() {
        return invokingJspCtxt.getPage();
    }

    @Override
    public ServletRequest getRequest() {
        return invokingJspCtxt.getRequest();
    }

    @Override
    public ServletResponse getResponse() {
        return invokingJspCtxt.getResponse();
    }

    @Override
    public Exception getException() {
        return invokingJspCtxt.getException();
    }

    @Override
    public ServletConfig getServletConfig() {
        return invokingJspCtxt.getServletConfig();
    }

    @Override
    public ServletContext getServletContext() {
        return invokingJspCtxt.getServletContext();
    }

    static public PageContext getRootPageContext(PageContext pageContect) {
        while (pageContect instanceof JspContextWrapper) {
            pageContect = ((JspContextWrapper) pageContect).invokingJspCtxt;
        }

        return pageContect;
    }

    @Override
    public ELContext getELContext() {
        if (elContext == null) {
            ELContextImpl elContextImpl = (ELContextImpl)
                getRootPageContextImpl().getJspApplicationContext()
                                        .createELContext(
                                            invokingJspCtxt.getELContext()
                                                           .getELResolver());

            elContextImpl.putContext(JspContext.class, this);
            elContextImpl.setVariableMapper(new VariableMapperImpl());

            elContext = new ELContextWrapper(elContextImpl, isErrorOnELNotFound());
        }

        return elContext;
    }

    public PageContextImpl getRootPageContextImpl() {
        PageContext pageContext = invokingJspCtxt;
        while (pageContext instanceof JspContextWrapper) {
            pageContext = ((JspContextWrapper) pageContext).invokingJspCtxt;
        }

        return (PageContextImpl) pageContext;
    }

    public boolean isErrorOnELNotFound() {
        if (jspTag instanceof JspSourceDependent) {
            return ((JspSourceDependent) jspTag).getErrorOnELNotFound();
        }

        return false;
    }

    @Override
    public void forward(String relativeUrlPath) throws ServletException, IOException {
        invokingJspCtxt.forward(relativeUrlPath);
    }

    @Override
    public void include(String relativeUrlPath) throws ServletException, IOException {
        invokingJspCtxt.include(relativeUrlPath);
    }

    @Override
    public void include(String relativeUrlPath, boolean flush) throws ServletException, IOException {
        invokingJspCtxt.include(relativeUrlPath, flush);
    }

    @Override
    public VariableResolver getVariableResolver() {
        return null;
    }

    @Override
    public BodyContent pushBody() {
        return invokingJspCtxt.pushBody();
    }

    @Override
    public JspWriter pushBody(Writer writer) {
        return invokingJspCtxt.pushBody(writer);
    }

    @Override
    public JspWriter popBody() {
        return invokingJspCtxt.popBody();
    }

    @Override
    public ExpressionEvaluator getExpressionEvaluator() {
        return invokingJspCtxt.getExpressionEvaluator();
    }

    @Override
    public void handlePageException(Exception ex) throws IOException, ServletException {
        // Should never be called since handleException() called with a
        // Throwable in the generated servlet.
        handlePageException((Throwable) ex);
    }

    @Override
    public void handlePageException(Throwable t) throws IOException, ServletException {
        invokingJspCtxt.handlePageException(t);
    }

    /**
     * Synchronize variables at begin of tag file
     */
    public void syncBeginTagFile() {
        saveNestedVariables();
    }

    /**
     * Synchronize variables before fragment invokation
     */
    public void syncBeforeInvoke() {
        copyTagToPageScope(NESTED);
        copyTagToPageScope(AT_BEGIN);
    }

    /**
     * Synchronize variables at end of tag file
     */
    public void syncEndTagFile() {
        copyTagToPageScope(AT_BEGIN);
        copyTagToPageScope(AT_END);
        restoreNestedVariables();
    }

    /**
     * Copies the variables of the given scope from the virtual page scope of this JSP context wrapper to the page scope of
     * the invoking JSP context.
     *
     * @param scope variable scope (one of NESTED, AT_BEGIN, or AT_END)
     */
    private void copyTagToPageScope(int scope) {
        Iterator<String> iter = null;

        switch (scope) {
        case NESTED:
            if (nestedVars != null) {
                iter = nestedVars.iterator();
            }
            break;
        case AT_BEGIN:
            if (atBeginVars != null) {
                iter = atBeginVars.iterator();
            }
            break;
        case AT_END:
            if (atEndVars != null) {
                iter = atEndVars.iterator();
            }
            break;
        }

        while (iter != null && iter.hasNext()) {
            String varName = iter.next();
            Object attribute = getAttribute(varName);
            varName = findAlias(varName);
            if (attribute != null) {
                invokingJspCtxt.setAttribute(varName, attribute);
            } else {
                invokingJspCtxt.removeAttribute(varName, PAGE_SCOPE);
            }
        }
    }

    /**
     * Saves the values of any NESTED variables that are present in the invoking JSP context, so they can later be restored.
     */
    private void saveNestedVariables() {
        if (nestedVars != null) {
            Iterator<String> iter = nestedVars.iterator();
            while (iter.hasNext()) {
                String varName = iter.next();
                varName = findAlias(varName);
                Object attribute = invokingJspCtxt.getAttribute(varName);
                if (attribute != null) {
                    originalNestedVars.put(varName, attribute);
                }
            }
        }
    }

    /**
     * Restores the values of any NESTED variables in the invoking JSP context.
     */
    private void restoreNestedVariables() {
        if (nestedVars != null) {
            Iterator<String> iter = nestedVars.iterator();
            while (iter.hasNext()) {
                String varName = iter.next();
                varName = findAlias(varName);
                Object nestedVar = originalNestedVars.get(varName);
                if (nestedVar != null) {
                    invokingJspCtxt.setAttribute(varName, nestedVar);
                } else {
                    invokingJspCtxt.removeAttribute(varName, PAGE_SCOPE);
                }
            }
        }
    }

    /**
     * Checks to see if the given variable name is used as an alias, and if so, returns the variable name for which it is
     * used as an alias.
     *
     * @param varName The variable name to check
     * @return The variable name for which varName is used as an alias, or varName if it is not being used as an alias
     */
    private String findAlias(String varName) {
        if (aliases == null) {
            return varName;
        }

        String alias = aliases.get(varName);
        if (alias == null) {
            return varName;
        }

        return alias;
    }
}
