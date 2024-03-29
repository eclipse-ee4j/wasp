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

package org.glassfish.wasp.taglibs.standard.lang.jstl;

/**
 *
 * <p>
 * The implementation of the less than operator
 *
 * @author Nathan Abramson - Art Technology Group
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 **/

public class LessThanOperator extends RelationalOperator {
    // -------------------------------------
    // Singleton
    // -------------------------------------

    public static final LessThanOperator SINGLETON = new LessThanOperator();

    // -------------------------------------
    /**
     *
     * Constructor
     **/
    public LessThanOperator() {
    }

    // -------------------------------------
    // Expression methods
    // -------------------------------------
    /**
     *
     * Returns the symbol representing the operator
     **/
    @Override
    public String getOperatorSymbol() {
        return "<";
    }

    // -------------------------------------
    /**
     *
     * Applies the operator to the given value
     **/
    @Override
    public Object apply(Object pLeft, Object pRight, Object pContext, Logger pLogger) throws ELException {
        if (pLeft == pRight) {
            return Boolean.FALSE;
        } else if (pLeft == null || pRight == null) {
            return Boolean.FALSE;
        } else {
            return super.apply(pLeft, pRight, pContext, pLogger);
        }
    }

    // -------------------------------------
    /**
     *
     * Applies the operator to the given double values
     **/
    @Override
    public boolean apply(double pLeft, double pRight, Logger pLogger) {
        return pLeft < pRight;
    }

    // -------------------------------------
    /**
     *
     * Applies the operator to the given long values
     **/
    @Override
    public boolean apply(long pLeft, long pRight, Logger pLogger) {
        return pLeft < pRight;
    }

    // -------------------------------------
    /**
     *
     * Applies the operator to the given String values
     **/
    @Override
    public boolean apply(String pLeft, String pRight, Logger pLogger) {
        return pLeft.compareTo(pRight) < 0;
    }

    // -------------------------------------
}
