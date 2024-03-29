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
 * The implementation of the and operator
 *
 * @author Nathan Abramson - Art Technology Group
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 **/
public class AndOperator extends BinaryOperator {
    // -------------------------------------
    // Singleton
    // -------------------------------------

    public static final AndOperator SINGLETON = new AndOperator();

    // -------------------------------------
    /**
     *
     * Constructor
     **/
    public AndOperator() {
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
        return "and";
    }

    // -------------------------------------
    /**
     *
     * Applies the operator to the given value
     **/
    @Override
    public Object apply(Object pLeft, Object pRight, Object pContext, Logger pLogger) throws ELException {
        // Coerce the values to booleans
        boolean left = Coercions.coerceToBoolean(pLeft, pLogger).booleanValue();
        boolean right = Coercions.coerceToBoolean(pRight, pLogger).booleanValue();

        return PrimitiveObjects.getBoolean(left && right);
    }

    // -------------------------------------
    /**
     *
     * Returns true if evaluation is necessary given the specified Left value. The And/OrOperators make use of this
     **/
    @Override
    public boolean shouldEvaluate(Object pLeft) {
        return (pLeft instanceof Boolean) && ((Boolean) pLeft).booleanValue();
    }

    // -------------------------------------
    /**
     *
     * Returns true if the operator expects its arguments to be coerced to Booleans. The And/Or operators set this to true.
     **/
    @Override
    public boolean shouldCoerceToBoolean() {
        return true;
    }

    // -------------------------------------
}
