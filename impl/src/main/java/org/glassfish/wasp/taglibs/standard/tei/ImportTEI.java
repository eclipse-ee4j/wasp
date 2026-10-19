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

package org.glassfish.wasp.taglibs.standard.tei;

import static org.glassfish.wasp.taglibs.standard.tei.Util.isSpecified;

import jakarta.servlet.jsp.tagext.TagData;
import jakarta.servlet.jsp.tagext.TagExtraInfo;

/**
 * An implementation of TagExtraInfo that implements validation for &lt;c:import&gt;'s attributes
 *
 * @author Shawn Bayern
 */
public class ImportTEI extends TagExtraInfo {

    final private static String VAR = "var";
    final private static String VAR_READER = "varReader";

    @Override
    public boolean isValid(TagData us) {
        // Don't allow both VAR and VAR_READER, together
        if (isSpecified(us, VAR) && isSpecified(us, VAR_READER)) {
            return false;
        }

        return true;
    }

}
