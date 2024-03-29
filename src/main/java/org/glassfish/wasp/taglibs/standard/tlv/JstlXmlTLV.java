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
 * A SAX-based TagLibraryValidator for the JSTL XML library. Currently implements the following checks:
 * </p>
 *
 * <ul>
 * <li>Expression syntax validation.
 * <li>Choose / when / otherwise constraints</li>
 * <li>Tag bodies that must either be empty or non-empty given particular attributes.</li>
 * <li>Other minor constraints.</li>
 * </ul>
 *
 * @author Shawn Bayern
 */
public class JstlXmlTLV extends JstlBaseTLV {

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
    private final String CHOOSE = "choose";
    private final String WHEN = "when";
    private final String OTHERWISE = "otherwise";
    private final String PARSE = "parse";
    private final String PARAM = "param";
    private final String TRANSFORM = "transform";
    private final String JSP_TEXT = "jsp:text";

    // attribute names
    private final String VALUE = "value";
    private final String SOURCE = "xml";

    // *********************************************************************
    // set its type and delegate validation to super-class
    @Override
    public ValidationMessage[] validate(String prefix, String uri, PageData page) {
        return super.validate(TYPE_XML, prefix, uri, page);
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
        private Stack<Integer> chooseDepths = new Stack<>();
        private Stack<Boolean> chooseHasOtherwise = new Stack<>();
        private Stack<Boolean> chooseHasWhen = new Stack<>();
        private String lastElementName = null;
        private boolean bodyNecessary = false;
        private boolean bodyIllegal = false;
        private Stack<Integer> transformWithSource = new Stack<>();

        // process under the existing context (state), then modify it
        @Override
        public void startElement(String nameSpace, String localName, String qualifiedNamed, Attributes attributes) {
            // Substitute our own parsed 'ln' if it's not provided
            if (localName == null) {
                localName = getLocalPart(qualifiedNamed);
            }

            // For simplicity, we can ignore <jsp:text> for our purposes
            // (don't bother distinguishing between it and its characters)
            if (qualifiedNamed.equals(JSP_TEXT)) {
                return;
            }

            // Check body-related constraint
            if (bodyIllegal) {
                fail(Resources.getMessage("TLV_ILLEGAL_BODY", lastElementName));
            }

            // validate expression syntax if we need to
            Set<String> expAtts;
            if (qualifiedNamed.startsWith(prefix + ":") && (expAtts = config.get(localName)) != null) {
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
            if (qualifiedNamed.startsWith(prefix + ":") && !hasNoInvalidScope(attributes)) {
                fail(Resources.getMessage("TLV_INVALID_ATTRIBUTE", SCOPE, qualifiedNamed, attributes.getValue(SCOPE)));
            }
            if (qualifiedNamed.startsWith(prefix + ":") && hasEmptyVar(attributes)) {
                fail(Resources.getMessage("TLV_EMPTY_VAR", qualifiedNamed));
            }
            if (qualifiedNamed.startsWith(prefix + ":") && hasDanglingScope(attributes)) {
                fail(Resources.getMessage("TLV_DANGLING_SCOPE", qualifiedNamed));
            }

            // check invariants for <choose>
            if (chooseChild()) {
                // mark <choose> for the first the first <when>
                if (isXmlTag(nameSpace, localName, WHEN)) {
                    chooseHasWhen.pop();
                    chooseHasWhen.push(Boolean.TRUE);
                }

                // ensure <choose> has the right children
                if (!isXmlTag(nameSpace, localName, WHEN) && !isXmlTag(nameSpace, localName, OTHERWISE)) {
                    fail(Resources.getMessage("TLV_ILLEGAL_CHILD_TAG", prefix, CHOOSE, qualifiedNamed));
                }

                // make sure <otherwise> is the last tag
                if (chooseHasOtherwise.peek()) {
                    fail(Resources.getMessage("TLV_ILLEGAL_ORDER", qualifiedNamed, prefix, OTHERWISE, CHOOSE));
                }
                if (isXmlTag(nameSpace, localName, OTHERWISE)) {
                    chooseHasOtherwise.pop();
                    chooseHasOtherwise.push(Boolean.TRUE);
                }

            }

            // Specific check, directly inside <transform source="...">
            if (!transformWithSource.empty() && topDepth(transformWithSource) == (depth - 1)) {
                // only allow <param>
                if (!isXmlTag(nameSpace, localName, PARAM)) {
                    fail(Resources.getMessage("TLV_ILLEGAL_BODY", prefix + ":" + TRANSFORM));
                }

                // thus, if we get the opportunity to hit depth++,
                // we know we've got a <param> subtag
            }

            // now, modify state

            // we're a choose, so record new choose-specific state
            if (isXmlTag(nameSpace, localName, CHOOSE)) {
                chooseDepths.push(Integer.valueOf(depth));
                chooseHasWhen.push(Boolean.FALSE);
                chooseHasOtherwise.push(Boolean.FALSE);
            }

            // set up a check against illegal attribute/body combinations
            bodyIllegal = false;
            bodyNecessary = false;
            if (isXmlTag(nameSpace, localName, PARSE)) {
                if (hasAttribute(attributes, SOURCE)) {
                    bodyIllegal = true;
                }
            } else if (isXmlTag(nameSpace, localName, PARAM)) {
                if (hasAttribute(attributes, VALUE)) {
                    bodyIllegal = true;
                } else {
                    bodyNecessary = true;
                }
            } else if (isXmlTag(nameSpace, localName, TRANSFORM)) {
                if (hasAttribute(attributes, SOURCE)) {
                    transformWithSource.push(Integer.valueOf(depth));
                }
            }

            // record the most recent tag (for error reporting)
            lastElementName = qualifiedNamed;
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

            // make sure <choose> has no non-whitespace text
            if (chooseChild()) {
                String msg = Resources.getMessage("TLV_ILLEGAL_TEXT_BODY", prefix, CHOOSE, (s.length() < 7 ? s : s.substring(0, 7)));
                fail(msg);
            }

            // Specific check, directly inside <transform source="...">
            if (!transformWithSource.empty() && topDepth(transformWithSource) == (depth - 1)) {
                fail(Resources.getMessage("TLV_ILLEGAL_BODY", prefix + ":" + TRANSFORM));
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

            // update <choose>-related state
            if (isXmlTag(ns, ln, CHOOSE)) {
                Boolean b = chooseHasWhen.pop();
                if (!b) {
                    fail(Resources.getMessage("TLV_PARENT_WITHOUT_SUBTAG", CHOOSE, WHEN));
                }
                chooseDepths.pop();
                chooseHasOtherwise.pop();
            }

            // update <transform source="...">-related state
            if (!transformWithSource.empty() && topDepth(transformWithSource) == (depth - 1)) {
                transformWithSource.pop();
            }

            // update our depth
            depth--;
        }

        // are we directly under a <choose>?
        private boolean chooseChild() {
            return (!chooseDepths.empty() && (depth - 1) == chooseDepths.peek());
        }

        // returns the top int depth (peeked at) from a Stack of Integer
        private int topDepth(Stack<Integer> s) {
            return (s.peek());
        }
    }
}
