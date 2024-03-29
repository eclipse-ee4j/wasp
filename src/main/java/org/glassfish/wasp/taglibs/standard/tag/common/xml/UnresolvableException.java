/*
 * Copyright (c) 1997-2018 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.wasp.taglibs.standard.tag.common.xml;

import javax.xml.xpath.XPathException;

/**
 * <meta name="usage" content="general"/> Derived from XPathException in order that XPath processor exceptions may be
 * specifically caught.
 */
public class UnresolvableException extends XPathException {
    /**
     * Create an UnresolvableException object that holds an error message.
     *
     * @param message The error message.
     */
    public UnresolvableException(String message) {
        super(message);
    }

    /**
     * Create an UnresolvableException object that holds an error message, and another exception that caused this exception.
     *
     * @param cause The exception that caused this exception.
     */
    public UnresolvableException(Throwable cause) {
        super(cause);
    }
}
