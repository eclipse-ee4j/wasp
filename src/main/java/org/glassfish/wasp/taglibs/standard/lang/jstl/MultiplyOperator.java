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
 * The implementation of the multiply operator
 *
 * @author Nathan Abramson - Art Technology Group
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 **/

public class MultiplyOperator extends ArithmeticOperator {
    // -------------------------------------
    // Singleton
    // -------------------------------------

    public static final MultiplyOperator SINGLETON = new MultiplyOperator();

    // -------------------------------------
    /**
     *
     * Constructor
     **/
    public MultiplyOperator() {
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
        return "*";
    }

    // -------------------------------------
    /**
     *
     * Applies the operator to the given double values, returning a double
     **/
    @Override
    public double apply(double pLeft, double pRight, Logger pLogger) {
        return pLeft * pRight;
    }

    // -------------------------------------
    /**
     *
     * Applies the operator to the given double values, returning a double
     **/
    @Override
    public long apply(long pLeft, long pRight, Logger pLogger) {
        return pLeft * pRight;
    }

    // -------------------------------------
}
