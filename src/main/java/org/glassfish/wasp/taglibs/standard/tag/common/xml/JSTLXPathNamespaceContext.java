/*
 * Copyright (c) 1997-2018 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.wasp.taglibs.standard.tag.common.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

/**
 * Implemenation of XML Namespace context processing given a URI or prefix
 */
public class JSTLXPathNamespaceContext implements NamespaceContext {

    // *********************************************************************
    // Constructor

    /**
     * No-arg constructor which would create empty HashMap of namespaces
     */
    public JSTLXPathNamespaceContext() {
        namespaces = new HashMap<>();
    }

    public JSTLXPathNamespaceContext(HashMap<String, String> nses) {
        namespaces = nses;
    }

    /**
     * The context to resolve the prefix from, if the context is not given.
     */
    HashMap<String, String> namespaces;

    /**
     * Get Namespace URI bound to a prefix in the current scope
     *
     * @param prefix Parameter
     *
     * @return Namespace URI bound to prefix
     *
     * @throws IllegalArgumentException if prefix is null
     */
    @Override
    public String getNamespaceURI(String prefix) throws IllegalArgumentException {
        // p("[getNamespaceURI] prefix: " + prefix);
        if (prefix == null) {
            throw new IllegalArgumentException("Cannot get Namespace URI for null prefix");
        }

        if (prefix.equals(XMLConstants.XML_NS_PREFIX)) {
            return XMLConstants.XML_NS_URI;
        }
        if (prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
            return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
        }

        String namespaceURI = namespaces.get(prefix);
        // p("[getNamespaceURI] namespaceURI: " + namespaceURI);
        if (namespaceURI != null) {
            return namespaceURI;
        }

        return XMLConstants.NULL_NS_URI;
    }

    /**
     * Get Prefix bound to Namespace URI in the current scope
     *
     * @param namespaceURI Namespace URI
     *
     * @return Prefix bound to Namespace URI
     *
     * @throws IllegalArgumentException if Namespace URI is null
     */
    @Override
    public String getPrefix(String namespaceURI) {
        // p("[getPrefix] namespaceURI: " + namespaceURI);
        if (namespaceURI == null) {
            throw new IllegalArgumentException("Cannot get prefix for null NamespaceURI");
        }

        if (namespaceURI.equals(XMLConstants.XML_NS_URI)) {
            return XMLConstants.XML_NS_PREFIX;
        }
        if (namespaceURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
            return XMLConstants.XMLNS_ATTRIBUTE;
        }

        for (String key : namespaces.keySet()) {
            String value = namespaces.get(key);
            if (value.equals(namespaceURI)) {
                // p("[getPrefix] value: " + value);
                return value;
            }
        }

        // p("[getPrefix] returning null");
        return null;
    }

    /**
     * Get all Prefixes bound to Namespace URI in the current scope
     *
     * @param namespaceURI Namespace URI
     *
     * @return Iterator of Prefixes bound to Namespace URI
     *
     * @throws IllegalArgumentException if Namespace URI is null
     */
    @Override
    public Iterator<String> getPrefixes(String namespaceURI) {
        // p("[getPrefixes] namespaceURI: " + namespaceURI);
        if (namespaceURI == null) {
            throw new IllegalArgumentException("Cannot get prefix for null NamespaceURI");
        }

        if (namespaceURI.equals(XMLConstants.XML_NS_URI)) {
            return Arrays.asList(new String[] { XMLConstants.XML_NS_PREFIX }).iterator();
        }
        if (namespaceURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
            return Arrays.asList(new String[] { XMLConstants.XMLNS_ATTRIBUTE }).iterator();
        }

        ArrayList<String> prefixList = new ArrayList<>();
        for (String key : namespaces.keySet()) {
            String value = namespaces.get(key);
            if (value.equals(namespaceURI)) {
                prefixList.add(key);
            }
        }

        // p("[getPrefixes] prefixList: " + prefixList);
        return prefixList.iterator();
    }

    /**
     * Populate map of Prefix and NameSpace URI's entries
     */
    protected void addNamespace(String prefix, String uri) {
        namespaces.put(prefix, uri);
    }

    // *********************************************************************
    // Utility methods

    private static void p(String s) {
        System.out.println("[JSTLXPathNameContext] " + s);
    }
}
