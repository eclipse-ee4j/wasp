/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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

package org.glassfish.wasp.taglibs.standard.lang.jstl.parser;

public interface ELParserConstants {
    String SYSTEM_PROPERTY_ALLOW_FUNCTIONS = "jakarta.servlet.jsp.functions.allowed";

    int EOF = 0;
    int NON_EXPRESSION_TEXT = 1;
    int START_EXPRESSION = 2;
    int INTEGER_LITERAL = 7;
    int FLOATING_POINT_LITERAL = 8;
    int EXPONENT = 9;
    int STRING_LITERAL = 10;
    int BADLY_ESCAPED_STRING_LITERAL = 11;
    int TRUE = 12;
    int FALSE = 13;
    int NULL = 14;
    int END_EXPRESSION = 15;
    int DOT = 16;
    int GT1 = 17;
    int GT2 = 18;
    int LT1 = 19;
    int LT2 = 20;
    int EQ1 = 21;
    int EQ2 = 22;
    int LE1 = 23;
    int LE2 = 24;
    int GE1 = 25;
    int GE2 = 26;
    int NE1 = 27;
    int NE2 = 28;
    int LPAREN = 29;
    int RPAREN = 30;
    int COMMA = 31;
    int COLON = 32;
    int LBRACKET = 33;
    int RBRACKET = 34;
    int PLUS = 35;
    int MINUS = 36;
    int MULTIPLY = 37;
    int DIVIDE1 = 38;
    int DIVIDE2 = 39;
    int MODULUS1 = 40;
    int MODULUS2 = 41;
    int NOT1 = 42;
    int NOT2 = 43;
    int AND1 = 44;
    int AND2 = 45;
    int OR1 = 46;
    int OR2 = 47;
    int EMPTY = 48;
    int IDENTIFIER = 49;
    int IMPL_OBJ_START = 50;
    int LETTER = 51;
    int DIGIT = 52;
    int ILLEGAL_CHARACTER = 53;

    int DEFAULT = 0;
    int IN_EXPRESSION = 1;

    String[] tokenImage = { "<EOF>", "<NON_EXPRESSION_TEXT>", "\"${\"", "\" \"", "\"\\t\"", "\"\\n\"", "\"\\r\"", "<INTEGER_LITERAL>",
            "<FLOATING_POINT_LITERAL>", "<EXPONENT>", "<STRING_LITERAL>", "<BADLY_ESCAPED_STRING_LITERAL>", "\"true\"", "\"false\"",
            "\"null\"", "\"}\"", "\".\"", "\">\"", "\"gt\"", "\"<\"", "\"lt\"", "\"==\"", "\"eq\"", "\"<=\"", "\"le\"", "\">=\"", "\"ge\"",
            "\"!=\"", "\"ne\"", "\"(\"", "\")\"", "\",\"", "\":\"", "\"[\"", "\"]\"", "\"+\"", "\"-\"", "\"*\"", "\"/\"", "\"div\"",
            "\"%\"", "\"mod\"", "\"not\"", "\"!\"", "\"and\"", "\"&&\"", "\"or\"", "\"||\"", "\"empty\"", "<IDENTIFIER>", "\"#\"",
            "<LETTER>", "<DIGIT>", "<ILLEGAL_CHARACTER>", };

}
