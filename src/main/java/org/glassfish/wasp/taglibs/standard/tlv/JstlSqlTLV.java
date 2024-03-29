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

package org.glassfish.wasp.taglibs.standard.tlv;

import java.util.Set;
import java.util.Stack;

import org.glassfish.wasp.taglibs.standard.resources.Resources;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import jakarta.servlet.jsp.tagext.PageData;
import jakarta.servlet.jsp.tagext.ValidationMessage;

/**
 * <p>
 * A SAX-based TagLibraryValidator for the JSTL SQL tag library.
 *
 * @author Shawn Bayern
 */
public class JstlSqlTLV extends JstlBaseTLV {

    // *********************************************************************
    // Constants

    // tag names
    private final String SETDATASOURCE = "setDataSource";
    private final String QUERY = "query";
    private final String UPDATE = "update";
    private final String TRANSACTION = "transaction";
    private final String PARAM = "param";
    private final String DATEPARAM = "dateParam";

    private final String JSP_TEXT = "jsp:text";

    // attribute names
    private final String SQL = "sql";
    private final String DATASOURCE = "dataSource";

    // *********************************************************************
    // set its type and delegate validation to super-class
    @Override
    public ValidationMessage[] validate(String prefix, String uri, PageData page) {
        return super.validate(TYPE_SQL, prefix, uri, page);
    }

    // *********************************************************************
    // Contract fulfillment

    @Override
    protected DefaultHandler getHandler() {
        return new Handler();
    }

    // *********************************************************************
    // SAX event handler

    /** The handler that provides the base of our implementation. */
    private class Handler extends DefaultHandler {

        // parser state
        private int depth = 0;
        private Stack<Integer> queryDepths = new Stack<>();
        private Stack<Integer> updateDepths = new Stack<>();
        private Stack<Integer> transactionDepths = new Stack<>();
        private String lastElementName = null;
        private boolean bodyNecessary = false;
        private boolean bodyIllegal = false;

        // process under the existing context (state), then modify it
        @Override
        public void startElement(String nameSpace, String localName, String qualifiedName, Attributes attributes) {
            // Substitute our own parsed 'ln' if it's not provided
            if (localName == null) {
                localName = getLocalPart(qualifiedName);
            }

            // For simplicity, we can ignore <jsp:text> for our purposes
            // (don't bother distinguishing between it and its characters)
            if (qualifiedName.equals(JSP_TEXT)) {
                return;
            }

            // Check body-related constraint
            if (bodyIllegal) {
                fail(Resources.getMessage("TLV_ILLEGAL_BODY", lastElementName));
            }

            // Validate expression syntax if we need to
            Set expAtts;
            if (qualifiedName.startsWith(prefix + ":") && (expAtts = config.get(localName)) != null) {
                for (int i = 0; i < attributes.getLength(); i++) {
                    String attName = attributes.getLocalName(i);
                    if (expAtts.contains(attName)) {
                        String vMsg = validateExpression(localName, attName, attributes.getValue(i));
                        if (vMsg != null) {
                            fail(vMsg);
                        }
                    }
                }
            }

            // validate attributes
            if (qualifiedName.startsWith(prefix + ":") && !hasNoInvalidScope(attributes)) {
                fail(Resources.getMessage("TLV_INVALID_ATTRIBUTE", SCOPE, qualifiedName, attributes.getValue(SCOPE)));
            }
            if (qualifiedName.startsWith(prefix + ":") && hasEmptyVar(attributes)) {
                fail(Resources.getMessage("TLV_EMPTY_VAR", qualifiedName));
            }
            if (qualifiedName.startsWith(prefix + ":") && hasDanglingScope(attributes) && !qualifiedName.startsWith(prefix + ":" + SETDATASOURCE)) {
                fail(Resources.getMessage("TLV_DANGLING_SCOPE", qualifiedName));
            }

            // now, modify state

            /*
             * Make sure <sql:param> is nested inside <sql:query> or <sql:update>. Note that <sql:param> does not need to be a
             * direct child of <sql:query> or <sql:update>. Otherwise, the following would not work:
             *
             * <sql:query sql="..." var="..."> <c:forEach var="arg" items="..."> <sql:param value="${arg}"/> </c:forEach>
             * </sql:query>
             */
            if ((isSqlTag(nameSpace, localName, PARAM) || isSqlTag(nameSpace, localName, DATEPARAM)) && (queryDepths.empty() && updateDepths.empty())) {
                fail(Resources.getMessage("SQL_PARAM_OUTSIDE_PARENT"));
            }

            // If we're in a <query>, record relevant state
            if (isSqlTag(nameSpace, localName, QUERY)) {
                queryDepths.push(Integer.valueOf(depth));
            }
            // If we're in a <update>, record relevant state
            if (isSqlTag(nameSpace, localName, UPDATE)) {
                updateDepths.push(Integer.valueOf(depth));
            }
            // If we're in a <transaction>, record relevant state
            if (isSqlTag(nameSpace, localName, TRANSACTION)) {
                transactionDepths.push(Integer.valueOf(depth));
            }

            // set up a check against illegal attribute/body combinations
            bodyIllegal = false;
            bodyNecessary = false;

            if (isSqlTag(nameSpace, localName, QUERY) || isSqlTag(nameSpace, localName, UPDATE)) {
                if (!hasAttribute(attributes, SQL)) {
                    bodyNecessary = true;
                }
                if (hasAttribute(attributes, DATASOURCE) && !transactionDepths.empty()) {
                    fail(Resources.getMessage("ERROR_NESTED_DATASOURCE"));
                }
            }

            if (isSqlTag(nameSpace, localName, DATEPARAM)) {
                bodyIllegal = true;
            }

            // record the most recent tag (for error reporting)
            lastElementName = qualifiedName;
            lastElementId = attributes.getValue("http://java.sun.com/JSP/Page", "id");

            // we're a new element, so increase depth
            depth++;
        }

        @Override
        public void characters(char[] ch, int start, int length) {

            bodyNecessary = false; // body is no longer necessary!

            // ignore strings that are just whitespace
            String s = new String(ch, start, length).trim();
            if (s.equals("")) {
                return;
            }

            // check and update body-related constraints
            if (bodyIllegal) {
                fail(Resources.getMessage("TLV_ILLEGAL_BODY", lastElementName));
            }
        }

        @Override
        public void endElement(String ns, String ln, String qn) {

            // consistently, we ignore JSP_TEXT
            if (qn.equals(JSP_TEXT)) {
                return;
            }

            // handle body-related invariant
            if (bodyNecessary) {
                fail(Resources.getMessage("TLV_MISSING_BODY", lastElementName));
            }
            bodyIllegal = false; // reset: we've left the tag

            // update <query>-related state
            if (isSqlTag(ns, ln, QUERY)) {
                queryDepths.pop();
            }
            // update <update>-related state
            if (isSqlTag(ns, ln, UPDATE)) {
                updateDepths.pop();
            }
            // update <update>-related state
            if (isSqlTag(ns, ln, TRANSACTION)) {
                transactionDepths.pop();
            }

            // update our depth
            depth--;
        }
    }
}
