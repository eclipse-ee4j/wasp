/*
 * Copyright (c) 1997-2020 Oracle and/or its affiliates. All rights reserved.
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

package org.apache.taglibs.standard.tei;

import jakarta.servlet.jsp.tagext.TagData;
import jakarta.servlet.jsp.tagext.TagExtraInfo;

/**
 * <p>
 * An implementation of TagExtraInfo that implements validation for ForEachTag's attributes
 * </p>
 *
 * @author Shawn Bayern
 */
public class ForEachTEI extends TagExtraInfo {

    final private static String ITEMS = "items";
    final private static String BEGIN = "begin";
    final private static String END = "end";

    /*
     * Currently implements the following rules:
     * 
     * - If 'items' is not specified, 'begin' and 'end' must be
     */
    public boolean isValid(TagData us) {
        if (!Util.isSpecified(us, ITEMS))
            if (!Util.isSpecified(us, BEGIN) || !(Util.isSpecified(us, END)))
                return false;
        return true;
    }

}
