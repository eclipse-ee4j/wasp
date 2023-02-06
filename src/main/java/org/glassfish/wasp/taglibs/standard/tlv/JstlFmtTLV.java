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
 * A SAX-based TagLibraryValidator for the JSTL i18n-capable formatting library. Currently implements the following
 * checks:
 * </p>
 *
 * <ul>
 * <li>Expression syntax validation.
 * <li>Tag bodies that must either be empty or non-empty given particular attributes.</li>
 * </ul>
 *
 * @author Shawn Bayern
 * @author Jan Luehe
 */
public class JstlFmtTLV extends JstlBaseTLV {

    // *********************************************************************
    // Implementation Overview

    /*
     * We essentially just run the page through a SAX parser, handling the callbacks that interest us. We collapse
     * <jsp:text> elements into the text they contain, since this simplifies processing somewhat. Even a quick glance at the
     * implementation shows its necessary, tree-oriented nature: multiple Stacks, an understanding of 'depth', and so on all
     * are important as we recover necessary state upon each callback. This TLV demonstrates various techniques, from the
     * general "how do I use a SAX parser for a TLV?" to "how do I read my init parameters and then validate?" But also, the
     * specific SAX methodology was kept as general as possible to allow for experimentation and flexibility.
     *
     * Much of the code and structure is duplicated from JstlCoreTLV. An effort has been made to re-use code where
     * unambiguously useful. However, splitting logic among parent/child classes isn't necessarily the cleanest approach
     * when writing a parser like the one we need. I'd like to reorganize this somewhat, but it's not a priority.
     */

    // *********************************************************************
    // Constants

    // tag names
    private final String SETLOCALE = "setLocale";
    private final String SETBUNDLE = "setBundle";
    private final String SETTIMEZONE = "setTimeZone";
    private final String BUNDLE = "bundle";
    private final String MESSAGE = "message";
    private final String MESSAGE_PARAM = "param";
    private final String FORMAT_NUMBER = "formatNumber";
    private final String PARSE_NUMBER = "parseNumber";
    private final String PARSE_DATE = "parseDate";
    // private final String EXPLANG = "expressionLanguage";
    private final String JSP_TEXT = "jsp:text";

    // attribute names
    private final String EVAL = "evaluator";
    private final String MESSAGE_KEY = "key";
    private final String BUNDLE_PREFIX = "prefix";
    private final String VALUE = "value";

    // *********************************************************************
    // set its type and delegate validation to super-class
    @Override
    public ValidationMessage[] validate(String prefix, String uri, PageData page) {
        return super.validate(TYPE_FMT, prefix, uri, page);
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
        private Stack<Integer> messageDepths = new Stack<>();
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
            if (qualifiedName.startsWith(prefix + ":") && !isFmtTag(nameSpace, localName, SETLOCALE) && !isFmtTag(nameSpace, localName, SETBUNDLE)
                    && !isFmtTag(nameSpace, localName, SETTIMEZONE) && hasDanglingScope(attributes)) {
                fail(Resources.getMessage("TLV_DANGLING_SCOPE", qualifiedName));
            }

            /*
             * Make sure <fmt:param> is nested inside <fmt:message>. Note that <fmt:param> does not need to be a direct child of
             * <fmt:message>. Otherwise, the following would not work:
             *
             * <fmt:message key="..." bundle="..."> <c:forEach var="arg" items="..."> <fmt:param value="${arg}"/> </c:forEach>
             * </fmt:message>
             */
            if (isFmtTag(nameSpace, localName, MESSAGE_PARAM) && messageDepths.empty()) {
                fail(Resources.getMessage("PARAM_OUTSIDE_MESSAGE"));
            }

            // now, modify state

            // If we're in a <message>, record relevant state
            if (isFmtTag(nameSpace, localName, MESSAGE)) {
                messageDepths.push(Integer.valueOf(depth));
            }

            // set up a check against illegal attribute/body combinations
            bodyIllegal = false;
            bodyNecessary = false;
            if (isFmtTag(nameSpace, localName, MESSAGE_PARAM) || isFmtTag(nameSpace, localName, FORMAT_NUMBER) || isFmtTag(nameSpace, localName, PARSE_NUMBER)
                    || isFmtTag(nameSpace, localName, PARSE_DATE)) {
                if (hasAttribute(attributes, VALUE)) {
                    bodyIllegal = true;
                } else {
                    bodyNecessary = true;
                }
            } else if (isFmtTag(nameSpace, localName, MESSAGE) && !hasAttribute(attributes, MESSAGE_KEY)) {
                bodyNecessary = true;
            } else if (isFmtTag(nameSpace, localName, BUNDLE) && hasAttribute(attributes, BUNDLE_PREFIX)) {
                bodyNecessary = true;
            }

            // record the most recent tag (for error reporting)
            lastElementName = qualifiedName;
            lastElementId = attributes.getValue(JSP, "id");

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

            // update <message>-related state
            if (isFmtTag(ns, ln, MESSAGE)) {
                messageDepths.pop();
            }

            // update our depth
            depth--;
        }
    }
}
