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
 * A SAX-based TagLibraryValidator for the core JSTL tag library. Currently implements the following checks:
 * </p>
 *
 * <ul>
 * <li>Expression syntax validation.
 * <li>Choose / when / otherwise constraints</li>
 * <li>Tag bodies that must either be empty or non-empty given particular attributes. (E.g., <set> cannot have a body
 * when 'value' is specified; it *must* have a body otherwise.) For these purposes, "having a body" refers to
 * non-whitespace content inside the tag.</li>
 * <li>Other minor constraints.</li>
 * </ul>
 *
 * @author Shawn Bayern
 */
public class JstlCoreTLV extends JstlBaseTLV {

    // *********************************************************************
    // Implementation Overview

    /*
     * We essentially just run the page through a SAX parser, handling the callbacks that interest us. We collapse
     * <jsp:text> elements into the text they contain, since this simplifies processing somewhat. Even a quick glance at the
     * implementation shows its necessary, tree-oriented nature: multiple Stacks, an understanding of 'depth', and so on all
     * are important as we recover necessary state upon each callback. This TLV demonstrates various techniques, from the
     * general "how do I use a SAX parser for a TLV?" to "how do I read my init parameters and then validate?" But also, the
     * specific SAX methodology was kept as general as possible to allow for experimentation and flexibility.
     */

    // *********************************************************************
    // Constants

    // tag names
    private final String CHOOSE = "choose";
    private final String WHEN = "when";
    private final String OTHERWISE = "otherwise";
    private final String EXPR = "out";
    private final String SET = "set";
    private final String IMPORT = "import";
    private final String URL = "url";
    private final String REDIRECT = "redirect";
    private final String PARAM = "param";
    // private final String EXPLANG = "expressionLanguage";
    private final String TEXT = "text";

    // attribute names
    private final String VALUE = "value";
    private final String DEFAULT = "default";
    private final String VAR_READER = "varReader";

    // alternative identifiers for tags
    private final String IMPORT_WITH_READER = "import varReader=''";
    private final String IMPORT_WITHOUT_READER = "import var=''";

    // *********************************************************************
    // set its type and delegate validation to super-class
    @Override
    public ValidationMessage[] validate(String prefix, String uri, PageData page) {
        return super.validate(TYPE_CORE, prefix, uri, page);
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
        private final Stack<Integer> chooseDepths = new Stack<>();
        private final Stack<Boolean> chooseHasOtherwise = new Stack<>();
        private final Stack<Boolean> chooseHasWhen = new Stack<>();
        private final Stack<String> urlTags = new Stack<>();
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
            if (isJspTag(nameSpace, localName, TEXT)) {
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
            if (qualifiedName.startsWith(prefix + ":") && hasDanglingScope(attributes)) {
                fail(Resources.getMessage("TLV_DANGLING_SCOPE", qualifiedName));
            }

            // check invariants for <choose>
            if (chooseChild()) {
                // mark <choose> for the first the first <when>
                if (isCoreTag(nameSpace, localName, WHEN)) {
                    chooseHasWhen.pop();
                    chooseHasWhen.push(Boolean.TRUE);
                }

                // ensure <choose> has the right children
                if (!isCoreTag(nameSpace, localName, WHEN) && !isCoreTag(nameSpace, localName, OTHERWISE)) {
                    fail(Resources.getMessage("TLV_ILLEGAL_CHILD_TAG", prefix, CHOOSE, qualifiedName));
                }

                // make sure <otherwise> is the last tag
                if (chooseHasOtherwise.peek()) {
                    fail(Resources.getMessage("TLV_ILLEGAL_ORDER", qualifiedName, prefix, OTHERWISE, CHOOSE));
                }
                if (isCoreTag(nameSpace, localName, OTHERWISE)) {
                    chooseHasOtherwise.pop();
                    chooseHasOtherwise.push(Boolean.TRUE);
                }

            }

            // check constraints for <param> vis-a-vis URL-related tags
            if (isCoreTag(nameSpace, localName, PARAM)) {
                // no <param> outside URL tags.
                if (urlTags.empty() || urlTags.peek().equals(PARAM)) {
                    fail(Resources.getMessage("TLV_ILLEGAL_ORPHAN", PARAM));
                }

                // no <param> where the most recent <import> has a reader
                if (!urlTags.empty() && urlTags.peek().equals(IMPORT_WITH_READER)) {
                    fail(Resources.getMessage("TLV_ILLEGAL_PARAM", prefix, PARAM, IMPORT, VAR_READER));
                }
            } else {
                // tag ISN'T <param>, so it's illegal under non-reader <import>
                if (!urlTags.empty() && urlTags.peek().equals(IMPORT_WITHOUT_READER)) {
                    fail(Resources.getMessage("TLV_ILLEGAL_CHILD_TAG", prefix, IMPORT, qualifiedName));
                }
            }

            // now, modify state

            // we're a choose, so record new choose-specific state
            if (isCoreTag(nameSpace, localName, CHOOSE)) {
                chooseDepths.push(depth);
                chooseHasWhen.push(Boolean.FALSE);
                chooseHasOtherwise.push(Boolean.FALSE);
            }

            // if we're introducing a URL-related tag, record it
            if (isCoreTag(nameSpace, localName, IMPORT)) {
                if (hasAttribute(attributes, VAR_READER)) {
                    urlTags.push(IMPORT_WITH_READER);
                } else {
                    urlTags.push(IMPORT_WITHOUT_READER);
                }
            } else if (isCoreTag(nameSpace, localName, PARAM)) {
                urlTags.push(PARAM);
            } else if (isCoreTag(nameSpace, localName, REDIRECT)) {
                urlTags.push(REDIRECT);
            } else if (isCoreTag(nameSpace, localName, URL)) {
                urlTags.push(URL);
            }

            // set up a check against illegal attribute/body combinations
            bodyIllegal = false;
            bodyNecessary = false;
            if (isCoreTag(nameSpace, localName, EXPR)) {
                if (hasAttribute(attributes, DEFAULT)) {
                    bodyIllegal = true;
                }
            } else if (isCoreTag(nameSpace, localName, SET)) {
                if (hasAttribute(attributes, VALUE)) {
                    bodyIllegal = true;
                // else
                // bodyNecessary = true;
                }
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
            if (!urlTags.empty() && urlTags.peek().equals(IMPORT_WITHOUT_READER)) {
                // we're in an <import> without a Reader; nothing but
                // <param> is allowed
                fail(Resources.getMessage("TLV_ILLEGAL_BODY", prefix + ":" + IMPORT));
            }

            // make sure <choose> has no non-whitespace text
            if (chooseChild()) {
                String msg = Resources.getMessage("TLV_ILLEGAL_TEXT_BODY", prefix, CHOOSE, (s.length() < 7 ? s : s.substring(0, 7)));
                fail(msg);
            }
        }

        @Override
        public void endElement(String nameSpace, String localName, String qualifiedName) {
            // Consistently, we ignore JSP_TEXT
            if (isJspTag(nameSpace, localName, TEXT)) {
                return;
            }

            // Handle body-related invariant
            if (bodyNecessary) {
                fail(Resources.getMessage("TLV_MISSING_BODY", lastElementName));
            }
            bodyIllegal = false; // reset: we've left the tag

            // Update <choose>-related state
            if (isCoreTag(nameSpace, localName, CHOOSE)) {
                Boolean b = chooseHasWhen.pop();
                if (!b) {
                    fail(Resources.getMessage("TLV_PARENT_WITHOUT_SUBTAG", CHOOSE, WHEN));
                }
                chooseDepths.pop();
                chooseHasOtherwise.pop();
            }

            // update state related to URL tags
            if (isCoreTag(nameSpace, localName, IMPORT) || isCoreTag(nameSpace, localName, PARAM) || isCoreTag(nameSpace, localName, REDIRECT) || isCoreTag(nameSpace, localName, URL)) {
                urlTags.pop();
            }

            // update our depth
            depth--;
        }

        // are we directly under a <choose>?
        private boolean chooseChild() {
            return (!chooseDepths.empty() && (depth - 1) == chooseDepths.peek());
        }

    }
}
