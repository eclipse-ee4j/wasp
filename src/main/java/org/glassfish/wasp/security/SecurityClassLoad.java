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

package org.glassfish.wasp.security;

import static java.util.Arrays.asList;
import static java.util.logging.Level.SEVERE;

import java.util.List;
import java.util.logging.Logger;

/**
 * Static class used to preload java classes when using the Java SecurityManager so that the defineClassInPackage
 * RuntimePermission does not trigger an AccessControlException.
 *
 * @author Jean-Francois Arcand
 */

public final class SecurityClassLoad {

    private static Logger log = Logger.getLogger(SecurityClassLoad.class.getName());

    public static void securityClassLoad(ClassLoader loader) {
        if (System.getSecurityManager() == null) {
            return;
        }

        List<String> classNames = asList(
            "org.glassfish.wasp.runtime.JspFactoryImpl$PrivilegedGetPageContext",
            "org.glassfish.wasp.runtime.JspFactoryImpl$PrivilegedReleasePageContext",
            "org.glassfish.wasp.runtime.JspRuntimeLibrary",
            "org.glassfish.wasp.runtime.ServletResponseWrapperInclude",
            "org.glassfish.wasp.runtime.TagHandlerPool",
            "org.glassfish.wasp.runtime.JspFragmentHelper",
            "org.glassfish.wasp.runtime.ProtectedFunctionMapper",
            "org.glassfish.wasp.runtime.PageContextImpl",
            "org.glassfish.wasp.runtime.JspContextWrapper",
            "org.glassfish.wasp.servlet.JspServletWrapper",
            "org.glassfish.wasp.runtime.JspWriterImpl"
        );

        for (String className : classNames) {
            try {
                loader.loadClass(className);
            } catch (ClassNotFoundException ex) {
                log.log(SEVERE, "SecurityClassLoad", ex);
            }
        }

    }
}
