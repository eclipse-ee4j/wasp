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

package org.glassfish.wasp.taglibs.standard.lang.jstl;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.PageContext;

/**
 *
 *
 *
 * <p>
 * This class is used to generate the implicit Map and List objects
 *
 * that wrap various elements of the PageContext. It also returns the
 *
 * correct implicit object for a given implicit object name.
 *
 *
 *
 * @author Nathan Abramson - Art Technology Group
 *
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 *
 **/

public class ImplicitObjects

{

    // -------------------------------------

    // Constants

    // -------------------------------------

    static final String sAttributeName =

            "org.glassfish.wasp.taglibs.standard.ImplicitObjects";

    // -------------------------------------

    // Member variables

    // -------------------------------------

    PageContext mContext;

    Map mPage;

    Map mRequest;

    Map mSession;

    Map mApplication;

    Map mParam;

    Map mParams;

    Map mHeader;

    Map mHeaders;

    Map mInitParam;

    Map mCookie;

    // -------------------------------------

    /**
     *
     *
     *
     * Constructor
     *
     **/

    public ImplicitObjects(PageContext pContext)

    {

        mContext = pContext;

    }

    // -------------------------------------

    /**
     *
     *
     *
     * Finds the ImplicitObjects associated with the PageContext,
     *
     * creating it if it doesn't yet exist.
     *
     **/

    public static ImplicitObjects getImplicitObjects(PageContext pContext)

    {

        ImplicitObjects objs =

                (ImplicitObjects)

                pContext.getAttribute(sAttributeName,

                        PageContext.PAGE_SCOPE);

        if (objs == null) {

            objs = new ImplicitObjects(pContext);

            pContext.setAttribute(sAttributeName,

                    objs,

                    PageContext.PAGE_SCOPE);

        }

        return objs;

    }

    // -------------------------------------

    /**
     *
     *
     *
     * Returns the Map that "wraps" page-scoped attributes
     *
     **/

    public Map getPageScopeMap()

    {

        if (mPage == null) {

            mPage = createPageScopeMap(mContext);

        }

        return mPage;

    }

    // -------------------------------------

    /**
     *
     *
     *
     * Returns the Map that "wraps" request-scoped attributes
     *
     **/

    public Map getRequestScopeMap()

    {

        if (mRequest == null) {

            mRequest = createRequestScopeMap(mContext);

        }

        return mRequest;

    }

    // -------------------------------------

    /**
     *
     *
     *
     * Returns the Map that "wraps" session-scoped attributes
     *
     **/

    public Map getSessionScopeMap()

    {

        if (mSession == null) {

            mSession = createSessionScopeMap(mContext);

        }

        return mSession;

    }

    // -------------------------------------

    /**
     *
     *
     *
     * Returns the Map that "wraps" application-scoped attributes
     *
     **/

    public Map getApplicationScopeMap()

    {

        if (mApplication == null) {

            mApplication = createApplicationScopeMap(mContext);

        }

        return mApplication;

    }

    // -------------------------------------

    /**
     *
     *
     *
     * Returns the Map that maps parameter name to a single parameter
     *
     * values.
     *
     **/

    public Map getParamMap()

    {

        if (mParam == null) {

            mParam = createParamMap(mContext);

        }

        return mParam;

    }

    // -------------------------------------

    /**
     *
     *
     *
     * Returns the Map that maps parameter name to an array of parameter
     *
     * values.
     *
     **/

    public Map getParamsMap()

    {

        if (mParams == null) {

            mParams = createParamsMap(mContext);

        }

        return mParams;

    }

    // -------------------------------------

    /**
     *
     *
     *
     * Returns the Map that maps header name to a single header
     *
     * values.
     *
     **/

    public Map getHeaderMap()

    {

        if (mHeader == null) {

            mHeader = createHeaderMap(mContext);

        }

        return mHeader;

    }

    // -------------------------------------

    /**
     *
     *
     *
     * Returns the Map that maps header name to an array of header
     *
     * values.
     *
     **/

    public Map getHeadersMap()

    {

        if (mHeaders == null) {

            mHeaders = createHeadersMap(mContext);

        }

        return mHeaders;

    }

    // -------------------------------------

    /**
     *
     *
     *
     * Returns the Map that maps init parameter name to a single init
     *
     * parameter values.
     *
     **/

    public Map getInitParamMap()

    {

        if (mInitParam == null) {

            mInitParam = createInitParamMap(mContext);

        }

        return mInitParam;

    }

    // -------------------------------------

    /**
     *
     *
     *
     * Returns the Map that maps cookie name to the first matching
     *
     * Cookie in request.getCookies().
     *
     **/

    public Map getCookieMap()

    {

        if (mCookie == null) {

            mCookie = createCookieMap(mContext);

        }

        return mCookie;

    }

    // -------------------------------------

    // Methods for generating wrapper maps

    // -------------------------------------

    /**
     *
     *
     *
     * Creates the Map that "wraps" page-scoped attributes
     *
     **/

    public static Map createPageScopeMap(PageContext pContext)

    {

        final PageContext context = pContext;

        return new EnumeratedMap()

        {

            @Override
            public Enumeration enumerateKeys()

            {

                return context.getAttributeNamesInScope

                (PageContext.PAGE_SCOPE);

            }

            @Override
            public Object getValue(Object pKey)

            {

                if (pKey instanceof String) {

                    return context.getAttribute

                    ((String) pKey,

                            PageContext.PAGE_SCOPE);

                }

                else {

                    return null;

                }

            }

            @Override
            public boolean isMutable()

            {

                return true;

            }

        };

    }

    // -------------------------------------

    /**
     *
     *
     *
     * Creates the Map that "wraps" request-scoped attributes
     *
     **/

    public static Map createRequestScopeMap(PageContext pContext)

    {

        final PageContext context = pContext;

        return new EnumeratedMap()

        {

            @Override
            public Enumeration enumerateKeys()

            {

                return context.getAttributeNamesInScope

                (PageContext.REQUEST_SCOPE);

            }

            @Override
            public Object getValue(Object pKey)

            {

                if (pKey instanceof String) {

                    return context.getAttribute

                    ((String) pKey,

                            PageContext.REQUEST_SCOPE);

                }

                else {

                    return null;

                }

            }

            @Override
            public boolean isMutable()

            {

                return true;

            }

        };

    }

    // -------------------------------------

    /**
     *
     *
     *
     * Creates the Map that "wraps" session-scoped attributes
     *
     **/

    public static Map createSessionScopeMap(PageContext pContext)

    {

        final PageContext context = pContext;

        return new EnumeratedMap()

        {

            @Override
            public Enumeration enumerateKeys()

            {

                return context.getAttributeNamesInScope

                (PageContext.SESSION_SCOPE);

            }

            @Override
            public Object getValue(Object pKey)

            {

                if (pKey instanceof String) {

                    return context.getAttribute

                    ((String) pKey,

                            PageContext.SESSION_SCOPE);

                }

                else {

                    return null;

                }

            }

            @Override
            public boolean isMutable()

            {

                return true;

            }

        };

    }

    // -------------------------------------

    /**
     *
     *
     *
     * Creates the Map that "wraps" application-scoped attributes
     *
     **/

    public static Map createApplicationScopeMap(PageContext pContext)

    {

        final PageContext context = pContext;

        return new EnumeratedMap()

        {

            @Override
            public Enumeration enumerateKeys()

            {

                return context.getAttributeNamesInScope

                (PageContext.APPLICATION_SCOPE);

            }

            @Override
            public Object getValue(Object pKey)

            {

                if (pKey instanceof String) {

                    return context.getAttribute

                    ((String) pKey,

                            PageContext.APPLICATION_SCOPE);

                }

                else {

                    return null;

                }

            }

            @Override
            public boolean isMutable()

            {

                return true;

            }

        };

    }

    // -------------------------------------

    /**
     *
     *
     *
     * Creates the Map that maps parameter name to single parameter
     *
     * value.
     *
     **/

    public static Map createParamMap(PageContext pContext)

    {

        final HttpServletRequest request =

                (HttpServletRequest) pContext.getRequest();

        return new EnumeratedMap()

        {

            @Override
            public Enumeration enumerateKeys()

            {

                return request.getParameterNames();

            }

            @Override
            public Object getValue(Object pKey)

            {

                if (pKey instanceof String) {

                    return request.getParameter((String) pKey);

                }

                else {

                    return null;

                }

            }

            @Override
            public boolean isMutable()

            {

                return false;

            }

        };

    }

    // -------------------------------------

    /**
     *
     *
     *
     * Creates the Map that maps parameter name to an array of parameter
     *
     * values.
     *
     **/

    public static Map createParamsMap(PageContext pContext)

    {

        final HttpServletRequest request =

                (HttpServletRequest) pContext.getRequest();

        return new EnumeratedMap()

        {

            @Override
            public Enumeration enumerateKeys()

            {

                return request.getParameterNames();

            }

            @Override
            public Object getValue(Object pKey)

            {

                if (pKey instanceof String) {

                    return request.getParameterValues((String) pKey);

                }

                else {

                    return null;

                }

            }

            @Override
            public boolean isMutable()

            {

                return false;

            }

        };

    }

    // -------------------------------------

    /**
     *
     *
     *
     * Creates the Map that maps header name to single header
     *
     * value.
     *
     **/

    public static Map createHeaderMap(PageContext pContext)

    {

        final HttpServletRequest request =

                (HttpServletRequest) pContext.getRequest();

        return new EnumeratedMap()

        {

            @Override
            public Enumeration enumerateKeys()

            {

                return request.getHeaderNames();

            }

            @Override
            public Object getValue(Object pKey)

            {

                if (pKey instanceof String) {

                    return request.getHeader((String) pKey);

                }

                else {

                    return null;

                }

            }

            @Override
            public boolean isMutable()

            {

                return false;

            }

        };

    }

    // -------------------------------------

    /**
     *
     *
     *
     * Creates the Map that maps header name to an array of header
     *
     * values.
     *
     **/

    public static Map createHeadersMap(PageContext pContext)

    {

        final HttpServletRequest request =

                (HttpServletRequest) pContext.getRequest();

        return new EnumeratedMap()

        {

            @Override
            public Enumeration enumerateKeys()

            {

                return request.getHeaderNames();

            }

            @Override
            public Object getValue(Object pKey)

            {

                if (pKey instanceof String) {

                    // Drain the header enumeration

                    List<String> l = new ArrayList<>();

                    Enumeration<String> enum_ = request.getHeaders((String) pKey);

                    if (enum_ != null) {

                        while (enum_.hasMoreElements()) {

                            l.add(enum_.nextElement());

                        }

                    }

                    String[] ret = l.toArray(new String[l.size()]);

                    return ret;

                }

                else {

                    return null;

                }

            }

            @Override
            public boolean isMutable()

            {

                return false;

            }

        };

    }

    // -------------------------------------

    /**
     *
     *
     *
     * Creates the Map that maps init parameter name to single init
     *
     * parameter value.
     *
     **/

    public static Map createInitParamMap(PageContext pContext)

    {

        final ServletContext context = pContext.getServletContext();

        return new EnumeratedMap()

        {

            @Override
            public Enumeration enumerateKeys()

            {

                return context.getInitParameterNames();

            }

            @Override
            public Object getValue(Object pKey)

            {

                if (pKey instanceof String) {

                    return context.getInitParameter((String) pKey);

                }

                else {

                    return null;

                }

            }

            @Override
            public boolean isMutable()

            {

                return false;

            }

        };

    }

    // -------------------------------------

    /**
     *
     *
     *
     * Creates the Map that maps cookie name to the first matching
     *
     * Cookie in request.getCookies().
     *
     **/
    public static Map<String, Cookie> createCookieMap(PageContext pContext) {

        // Read all the cookies and construct the entire map

        HttpServletRequest request = (HttpServletRequest) pContext.getRequest();

        Cookie[] cookies = request.getCookies();

        Map<String, Cookie> ret = new HashMap<>();

        for (int i = 0; cookies != null && i < cookies.length; i++) {

            Cookie cookie = cookies[i];

            if (cookie != null) {

                String name = cookie.getName();

                if (!ret.containsKey(name)) {

                    ret.put(name, cookie);

                }

            }

        }

        return ret;

    }

    // -------------------------------------

}
