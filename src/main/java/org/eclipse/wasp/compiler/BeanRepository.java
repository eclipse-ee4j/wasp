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

package org.eclipse.wasp.compiler;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.wasp.WaspException;

/**
 * Repository of {page, request, session, application}-scoped beans
 *
 * @author Mandar Raje
 */
class BeanRepository {

    private Map<String, String> beanTypes;
    private ClassLoader loader;
    private ErrorDispatcher errDispatcher;

    /*
     * Constructor.
     */
    public BeanRepository(ClassLoader loader, ErrorDispatcher err) {
        this.loader = loader;
        this.errDispatcher = err;
        beanTypes = new HashMap<>();
    }

    public void addBean(Node.UseBean n, String s, String type, String scope) throws WaspException
    {
        if (scope == null || scope.equals("page") || scope.equals("request") || scope.equals("session") || scope.equals("application")) {
            beanTypes.put(s, type);
        } else {
            errDispatcher.jspError(n, "jsp.error.invalid.scope", scope);
        }

    }

    public Class<?> getBeanType(String bean) throws WaspException
    {
        try {
            return loader.loadClass(beanTypes.get(bean));
        } catch (ClassNotFoundException ex) {
            throw new WaspException(ex);
        }
    }

    public boolean checkVariable(String bean) {
        return beanTypes.containsKey(bean);
    }

}
