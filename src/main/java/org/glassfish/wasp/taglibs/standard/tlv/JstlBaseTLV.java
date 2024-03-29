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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.glassfish.wasp.taglibs.standard.lang.support.ExpressionEvaluator;
import org.glassfish.wasp.taglibs.standard.lang.support.ExpressionEvaluatorManager;
import org.glassfish.wasp.taglibs.standard.resources.Resources;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.PageData;
import jakarta.servlet.jsp.tagext.TagData;
import jakarta.servlet.jsp.tagext.TagLibraryValidator;
import jakarta.servlet.jsp.tagext.ValidationMessage;

/**
 * <p>
 * A base class to support SAX-based validation in JSTL.
 * </p>
 *
 * @author Shawn Bayern
 */
public abstract class JstlBaseTLV extends TagLibraryValidator {

    // *********************************************************************
    // Implementation Overview

    /*
     * We essentially just run the page through a SAX parser, handling the callbacks that interest us. The SAX parser is
     * supplied by subclasses using the protected getHandler() method.
     */

    protected abstract DefaultHandler getHandler();

    // *********************************************************************
    // Constants

    // parameter names
    private final String EXP_ATT_PARAM = "expressionAttributes";

    // attributes
    protected static final String VAR = "var";
    protected static final String SCOPE = "scope";

    // scopes
    protected static final String PAGE_SCOPE = "page";
    protected static final String REQUEST_SCOPE = "request";
    protected static final String SESSION_SCOPE = "session";
    protected static final String APPLICATION_SCOPE = "application";

    // Relevant URIs
    protected final String JSP = "http://java.sun.com/JSP/Page";

    // types of sub-classes - used on method validate()
    private static final int TYPE_UNDEFINED = 0;
    protected static final int TYPE_CORE = 1;
    protected static final int TYPE_FMT = 2;
    protected static final int TYPE_SQL = 3;
    protected static final int TYPE_XML = 4;

    // which tlv is being validated
    private int tlvType = TYPE_UNDEFINED;

    // *********************************************************************
    // Validation and configuration state (protected)

    protected String uri; // our taglib's uri (as passed by JSP container on XML View)
    protected String prefix; // our taglib's prefix
    protected Vector<ValidationMessage> messageVector; // temporary error messages
    protected Map<String, Set<String>> config; // configuration (Map of Sets)
    protected boolean failed; // have we failed >0 times?
    protected String lastElementId; // the last element we've seen

    // *********************************************************************
    // Constructor and lifecycle management

    public JstlBaseTLV() {
        super();
        init();
    }

    private synchronized void init() {
        messageVector = null;
        prefix = null;
        config = null;
    }

    @Override
    public void release() {
        super.release();
        init();
    }

    // *********************************************************************
    // Validation entry point - this method is called by the sub-classes to
    // do the validation.

    public synchronized ValidationMessage[] validate(int type, String prefix, String uri, PageData page) {
        try {
            this.tlvType = type;
            this.uri = uri;
            // Initialize
            messageVector = new Vector<>();

            // save the prefix
            this.prefix = prefix;

            // parse parameters if necessary
            try {
                if (config == null) {
                    configure((String) getInitParameters().get(EXP_ATT_PARAM));
                }
            } catch (NoSuchElementException ex) {
                // parsing error
                return vmFromString(Resources.getMessage("TLV_PARAMETER_ERROR", EXP_ATT_PARAM));
            }

            // Get a handler
            DefaultHandler handler = getHandler();

            // parse the page
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setValidating(false);
            saxParserFactory.setNamespaceAware(true);

            saxParserFactory.newSAXParser()
                            .parse(page.getInputStream(), handler);

            if (messageVector.size() == 0) {
                return null;
            }

            return vmFromVector(messageVector);

        } catch (SAXException ex) {
            return vmFromString(ex.toString());
        } catch (ParserConfigurationException ex) {
            return vmFromString(ex.toString());
        } catch (IOException ex) {
            return vmFromString(ex.toString());
        }
    }

    // *********************************************************************
    // Protected utility functions

    // delegate validation to the appropriate expression language
    protected String validateExpression(String elem, String att, String expr) {
        // Let's just use the cache kept by the ExpressionEvaluatorManager
        ExpressionEvaluator current;
        try {
            current = ExpressionEvaluatorManager.getEvaluatorByName(ExpressionEvaluatorManager.EVALUATOR_CLASS);
        } catch (JspException ex) {
            // (using JspException here feels ugly, but it's what EEM uses)
            return ex.getMessage();
        }

        String response = current.validate(att, expr);
        if (response == null) {
            return null;
        }

        return "tag = '" + elem + "' / attribute = '" + att + "': " + response;
    }

    // utility methods to help us match elements in our tagset
    protected boolean isTag(String tagUri, String tagLn, String matchUri, String matchLn) {
        if (tagUri == null || tagUri.length() == 0 || tagLn == null || matchUri == null || matchLn == null) {
            return false;
        }

        // Match beginning of URI since some suffix *_rt tags can
        // be nested in EL enabled tags as defined by the spec
        if (tagUri.length() > matchUri.length()) {
            return tagUri.startsWith(matchUri) && tagLn.equals(matchLn);
        }

        return matchUri.startsWith(tagUri) && tagLn.equals(matchLn);
    }

    protected boolean isJspTag(String tagUri, String tagLn, String target) {
        return isTag(tagUri, tagLn, JSP, target);
    }

    private boolean isTag(int type, String tagUri, String tagLn, String target) {
        return this.tlvType == type && isTag(tagUri, tagLn, this.uri, target);
    }

    protected boolean isCoreTag(String tagUri, String tagLn, String target) {
        return isTag(TYPE_CORE, tagUri, tagLn, target);
    }

    protected boolean isFmtTag(String tagUri, String tagLn, String target) {
        return isTag(TYPE_FMT, tagUri, tagLn, target);
    }

    protected boolean isSqlTag(String tagUri, String tagLn, String target) {
        return isTag(TYPE_SQL, tagUri, tagLn, target);
    }

    protected boolean isXmlTag(String tagUri, String tagLn, String target) {
        return isTag(TYPE_XML, tagUri, tagLn, target);
    }

    // Utility method to determine if an attribute exists
    protected boolean hasAttribute(Attributes attributes, String attribute) {
        return attributes.getValue(attribute) != null;
    }

    /*
     * method to assist with failure [ as if it's not easy enough already :-) ]
     */
    protected synchronized void fail(String message) {
        failed = true;
        messageVector.add(new ValidationMessage(lastElementId, message));
    }

    // returns true if the given attribute name is specified, false otherwise
    protected boolean isSpecified(TagData data, String attributeName) {
        return data.getAttribute(attributeName) != null;
    }

    // returns true if the 'scope' attribute is valid
    protected boolean hasNoInvalidScope(Attributes attributes) {
        String scope = attributes.getValue(SCOPE);

        if (scope != null &&
                !scope.equals(PAGE_SCOPE) && !scope.equals(REQUEST_SCOPE) &&
                !scope.equals(SESSION_SCOPE) && !scope.equals(APPLICATION_SCOPE)) {
            return false;
        }

        return true;
    }

    // returns true if the 'var' attribute is empty
    protected boolean hasEmptyVar(Attributes a) {
        return "".equals(a.getValue(VAR));
    }

    // returns true if the 'scope' attribute is present without 'var'
    protected boolean hasDanglingScope(Attributes attributes) {
        return attributes.getValue(SCOPE) != null && attributes.getValue(VAR) == null;
    }

    // retrieves the local part of a QName
    protected String getLocalPart(String qname) {
        int colon = qname.indexOf(":");
        if (colon == -1) {
            return qname;
        }

        return qname.substring(colon + 1);
    }

    // *********************************************************************
    // Miscellaneous utility functions

    // parses our configuration parameter for element:attribute pairs
    private void configure(String info) {
        // Construct our configuration map
        config = new HashMap<>();

        // Leave the map empty if we have nothing to configure
        if (info == null) {
            return;
        }

        // Separate parameter into space-separated tokens and store them
        StringTokenizer stringTokenizer = new StringTokenizer(info);
        while (stringTokenizer.hasMoreTokens()) {
            String pair = stringTokenizer.nextToken();
            StringTokenizer pairTokens = new StringTokenizer(pair, ":");
            String element = pairTokens.nextToken();
            String attribute = pairTokens.nextToken();

            Set<String> attributes = config.get(element);
            if (attributes == null) {
                attributes = new HashSet<>();
                config.put(element, attributes);
            }

            attributes.add(attribute);
        }
    }

    // Constructs a ValidationMessage[] from a single String and no ID
    static ValidationMessage[] vmFromString(String message) {
        return new ValidationMessage[] { new ValidationMessage(null, message) };
    }

    // Constructs a ValidationMessage[] from a ValidationMessage Vector
    static ValidationMessage[] vmFromVector(Vector<ValidationMessage> v) {
        ValidationMessage[] validationMessages = new ValidationMessage[v.size()];
        for (int i = 0; i < validationMessages.length; i++) {
            validationMessages[i] = v.get(i);
        }

        return validationMessages;
    }
}
