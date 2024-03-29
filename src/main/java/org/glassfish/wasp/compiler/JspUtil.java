/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.wasp.compiler;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.glassfish.wasp.Constants;
import org.glassfish.wasp.WaspException;
import org.glassfish.wasp.JspCompilationContext;
import org.glassfish.wasp.runtime.ELContextImpl;
import org.xml.sax.Attributes;

import jakarta.el.ELException;
import jakarta.el.ExpressionFactory;
import jakarta.el.FunctionMapper;
import jakarta.el.MethodExpression;

/**
 * This class has all the utility method(s). Ideally should move all the bean containers here.
 *
 * @author Mandar Raje.
 * @author Rajiv Mordani.
 * @author Danno Ferrin
 * @author Pierre Delisle
 * @author Shawn Bayern
 * @author Mark Roth
 * @author Kin-man Chung
 */
public class JspUtil {

    private static final String WEB_INF_TAGS = "/WEB-INF/tags/";
    private static final String META_INF_TAGS = "/META-INF/tags/";

    // Delimiters for request-time expressions (JSP and XML syntax)
    private static final String OPEN_EXPR = "<%=";
    private static final String CLOSE_EXPR = "%>";
    private static final String OPEN_EXPR_XML = "%=";
    private static final String CLOSE_EXPR_XML = "%";

    private static int tempSequenceNumber = 0;
    private static ExpressionFactory expFactory;

    private static final String javaKeywords[] = {
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue",
        "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
        "interface", "long", "native", "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch",
        "synchronized", "this", "throws", "transient", "try", "void", "volatile", "while" };

    public static final int CHUNKSIZE = 1024;

    public static char[] removeQuotes(char[] chars) {
        CharArrayWriter caw = new CharArrayWriter();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '%' && chars[i + 1] == '\\' && chars[i + 2] == '>') {
                caw.write('%');
                caw.write('>');
                i = i + 2;
            } else {
                caw.write(chars[i]);
            }
        }
        return caw.toCharArray();
    }

    public static char[] escapeQuotes(char[] chars) {
        // Prescan to convert %\> to %>
        String s = new String(chars);
        while (true) {
            int n = s.indexOf("%\\>");
            if (n < 0) {
                break;
            }
            StringBuilder sb = new StringBuilder(s.substring(0, n));
            sb.append("%>");
            sb.append(s.substring(n + 3));
            s = sb.toString();
        }
        chars = s.toCharArray();

        return chars;
    }

    /**
     * Checks if the token is a runtime expression. In standard JSP syntax, a runtime expression starts with <code>'&lt;%'</code> and ends
     * with <code>'%&gt;'</code>. When the JSP document is in XML syntax, a runtime expression starts with '%=' and ends with '%'.
     *
     * @param token The token to be checked return whether the token is a runtime expression or not.
     */
    public static boolean isExpression(String token, boolean isXml) {
        String openExpr;
        String closeExpr;
        if (isXml) {
            openExpr = OPEN_EXPR_XML;
            closeExpr = CLOSE_EXPR_XML;
        } else {
            openExpr = OPEN_EXPR;
            closeExpr = CLOSE_EXPR;
        }

        if (token.startsWith(openExpr) && token.endsWith(closeExpr)) {
            return true;
        }

        return false;
    }

    /**
     * @return the "expression" part of a runtime expression, taking the delimiters out.
     */
    public static String getExpr(String expression, boolean isXml) {
        String returnString;
        String openExpr;
        String closeExpr;
        if (isXml) {
            openExpr = OPEN_EXPR_XML;
            closeExpr = CLOSE_EXPR_XML;
        } else {
            openExpr = OPEN_EXPR;
            closeExpr = CLOSE_EXPR;
        }
        int length = expression.length();
        if (expression.startsWith(openExpr) && expression.endsWith(closeExpr)) {
            returnString = expression.substring(openExpr.length(), length - closeExpr.length());
        } else {
            returnString = "";
        }

        return returnString;
    }

    /**
     * Takes a potential expression and converts it into XML form
     */
    public static String getExprInXml(String expression) {
        String returnString;
        int length = expression.length();

        if (expression.startsWith(OPEN_EXPR) && expression.endsWith(CLOSE_EXPR)) {
            returnString = expression.substring(1, length - 1);
        } else {
            returnString = expression;
        }

        return escapeXml(returnString);
    }

    /**
     * Checks to see if the given scope is valid.
     *
     * @param scope The scope to be checked
     * @param n The Node containing the 'scope' attribute whose value is to be checked
     * @param err error dispatcher
     *
     * @throws WaspException if scope is not null and different from &quot;page&quot;, &quot;request&quot;,
     * &quot;session&quot;, and &quot;application&quot;
     */
    public static void checkScope(String scope, Node n, ErrorDispatcher err) throws WaspException {
        if (scope != null && !scope.equals("page") && !scope.equals("request") && !scope.equals("session") && !scope.equals("application")) {
            err.jspError(n, "jsp.error.invalid.scope", scope);
        }
    }

    /**
     * Checks if all mandatory attributes are present and if all attributes present have valid names. Checks attributes
     * specified as XML-style attributes as well as attributes specified using the jsp:attribute standard action.
     */
    public static void checkAttributes(String typeOfTag, Node n, ValidAttribute[] validAttributes, ErrorDispatcher err) throws WaspException {
        Attributes attrs = n.getAttributes();
        Mark start = n.getStart();
        boolean valid = true;

        // AttributesImpl.removeAttribute is broken, so we do this...
        int tempLength = attrs == null ? 0 : attrs.getLength();
        ArrayList<String> temp = new ArrayList<>(tempLength);
        for (int i = 0; i < tempLength; i++) {
            String qName = attrs.getQName(i);
            if (!qName.equals("xmlns") && !qName.startsWith("xmlns:")) {
                temp.add(qName);
            }
        }

        // Add names of attributes specified using jsp:attribute
        Node.Nodes tagBody = n.getBody();
        if (tagBody != null) {
            int numSubElements = tagBody.size();
            for (int i = 0; i < numSubElements; i++) {
                Node node = tagBody.getNode(i);
                if (node instanceof Node.NamedAttribute) {
                    String attrName = node.getAttributeValue("name");
                    temp.add(attrName);
                    // Check if this value appear in the attribute of the node
                    if (n.getAttributeValue(attrName) != null) {
                        err.jspError(n, "jsp.error.duplicate.name.jspattribute", attrName);
                    }
                } else {
                    // Nothing can come before jsp:attribute, and only
                    // jsp:body can come after it.
                    break;
                }
            }
        }

        /*
         * First check to see if all the mandatory attributes are present. If so only then proceed to see if the other
         * attributes are valid for the particular tag.
         */
        String missingAttribute = null;

        for (int i = 0; i < validAttributes.length; i++) {
            int attrPos;
            if (validAttributes[i].mandatory) {
                attrPos = temp.indexOf(validAttributes[i].name);
                if (attrPos != -1) {
                    temp.remove(attrPos);
                    valid = true;
                } else {
                    valid = false;
                    missingAttribute = validAttributes[i].name;
                    break;
                }
            }
        }

        // If mandatory attribute is missing then the exception is thrown
        if (!valid) {
            err.jspError(start, "jsp.error.mandatory.attribute", typeOfTag, missingAttribute);
        }

        // Check to see if there are any more attributes for the specified tag.
        int attrLeftLength = temp.size();
        if (attrLeftLength == 0) {
            return;
        }

        // Now check to see if the rest of the attributes are valid too.
        String attribute = null;

        for (int j = 0; j < attrLeftLength; j++) {
            valid = false;
            attribute = temp.get(j);
            for (int i = 0; i < validAttributes.length; i++) {
                if (attribute.equals(validAttributes[i].name)) {
                    valid = true;
                    break;
                }
            }
            if (!valid) {
                err.jspError(start, "jsp.error.invalid.attribute", typeOfTag, attribute);
            }
        }
        // XXX *could* move EL-syntax validation here... (sb)
    }

    public static String escapeQueryString(String unescString) {
        if (unescString == null) {
            return null;
        }

        String escString = "";
        String shellSpChars = "\\\"";

        for (int index = 0; index < unescString.length(); index++) {
            char nextChar = unescString.charAt(index);

            if (shellSpChars.indexOf(nextChar) != -1) {
                escString += "\\";
            }

            escString += nextChar;
        }
        return escString;
    }

    /**
     * Escape the 5 entities defined by XML.
     */
    public static String escapeXml(String s) {
        if (s == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '<') {
                sb.append("&lt;");
            } else if (c == '>') {
                sb.append("&gt;");
            } else if (c == '\'') {
                sb.append("&apos;");
            } else if (c == '&') {
                sb.append("&amp;");
            } else if (c == '"') {
                sb.append("&quot;");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Replaces any occurrences of the character <tt>replace</tt> with the string <tt>with</tt>.
     */
    public static String replace(String name, char replace, String with) {
        StringBuilder buf = new StringBuilder();
        int begin = 0;
        int end;
        int last = name.length();

        while (true) {
            end = name.indexOf(replace, begin);
            if (end < 0) {
                end = last;
            }
            buf.append(name.substring(begin, end));
            if (end == last) {
                break;
            }
            buf.append(with);
            begin = end + 1;
        }

        return buf.toString();
    }

    public static class ValidAttribute {
        String name;
        boolean mandatory;

        public ValidAttribute(String name, boolean mandatory) {
            this.name = name;
            this.mandatory = mandatory;
        }

        public ValidAttribute(String name) {
            this(name, false);
        }
    }

    /**
     * Convert a String value to 'boolean'. Besides the standard conversions done by Boolean.valueOf(s).booleanValue(), the
     * value "yes" (ignore case) is also converted to 'true'. If 's' is null, then 'false' is returned.
     *
     * @param s the string to be converted
     * @return the boolean value associated with the string s
     */
    public static boolean booleanValue(String s) {
        boolean b = false;
        if (s != null) {
            if (s.equalsIgnoreCase("yes")) {
                b = true;
            } else {
                b = Boolean.valueOf(s);
            }
        }
        return b;
    }

    /**
     * Returns the <tt>Class</tt> object associated with the class or interface with the given string name.
     *
     * <p>
     * The <tt>Class</tt> object is determined by passing the given string name to the <tt>Class.forName()</tt> method,
     * unless the given string name represents a primitive type, in which case it is converted to a <tt>Class</tt> object by
     * appending ".class" to it (e.g., "int.class").
     */
    public static Class<?> toClass(String type, ClassLoader loader) throws ClassNotFoundException {

        Class<?> c = null;
        int i0 = type.indexOf('[');
        int dims = 0;
        if (i0 > 0) {
            // This is an array. Count the dimensions
            for (int i = 0; i < type.length(); i++) {
                if (type.charAt(i) == '[') {
                    dims++;
                }
            }
            type = type.substring(0, i0);
        }

        if ("boolean".equals(type)) {
            c = boolean.class;
        } else if ("char".equals(type)) {
            c = char.class;
        } else if ("byte".equals(type)) {
            c = byte.class;
        } else if ("short".equals(type)) {
            c = short.class;
        } else if ("int".equals(type)) {
            c = int.class;
        } else if ("long".equals(type)) {
            c = long.class;
        } else if ("float".equals(type)) {
            c = float.class;
        } else if ("double".equals(type)) {
            c = double.class;
        } else if (type.indexOf('[') < 0) {
            c = loader.loadClass(type);
        }

        if (dims == 0) {
            return c;
        }

        if (dims == 1) {
            return java.lang.reflect.Array.newInstance(c, 1).getClass();
        }

        // Array of more than i dimension
        return java.lang.reflect.Array.newInstance(c, new int[dims]).getClass();
    }

    /**
     * Produces a String representing a call to the EL interpreter.
     *
     * @param expression a String containing zero or more "${}" expressions
     * @param expectedType the expected type of the interpreted result
     * @param fnmapvar Variable pointing to a function map.
     * @return a String representing a call to the EL interpreter.
     */
    public static String interpreterCall(boolean isTagFile, String expression, Class<?> expectedType, String fnmapvar, String expectedDeferredType, String expectedReturnType, String[] expectedParamTypes) {
        /*
         * Determine which context object to use.
         */
        String jspCtxt = null;
        if (isTagFile) {
            jspCtxt = "this.getJspContext()";
        } else {
            jspCtxt = "_jspx_page_context";
        }

        if (expectedType == jakarta.el.ValueExpression.class) {

            if (expectedDeferredType == null) {
                expectedDeferredType = "java.lang.Object";
            }

            return "org.glassfish.wasp.runtime.PageContextImpl.getValueExpression" + "(" + Generator.quote(expression) + ", " + "(PageContext)" + jspCtxt + ", "
                    + expectedDeferredType + ".class, " + fnmapvar + ")";

        }

        if (expectedType == MethodExpression.class) {

            if (expectedReturnType == null) {
                expectedReturnType = "Void";
            }

            StringBuilder params = new StringBuilder();
            if (expectedParamTypes != null) {
                for (int i = 0; i < expectedParamTypes.length; i++) {
                    if (i > 0) {
                        params.append(", ");
                    }
                    params.append(expectedParamTypes[i] + ".class");
                }
            }

            return "org.glassfish.wasp.runtime.PageContextImpl.getMethodExpression" + "(" + Generator.quote(expression) + ", " + "(PageContext)" + jspCtxt + ", "
                    + fnmapvar + ", " + expectedReturnType + ".class, " + "new Class[] {" + params.toString() + "})";
        }

        /*
         * Determine whether to use the expected type's textual name or, if it's a primitive, the name of its correspondent
         * boxed type.
         */
        String returnType = expectedType.getName();
        String targetType = returnType;
        String primitiveConverterMethod = null;
        if (expectedType.isPrimitive()) {
            if (expectedType.equals(Boolean.TYPE)) {
                returnType = Boolean.class.getName();
                primitiveConverterMethod = "booleanValue";
            } else if (expectedType.equals(Byte.TYPE)) {
                returnType = Byte.class.getName();
                primitiveConverterMethod = "byteValue";
            } else if (expectedType.equals(Character.TYPE)) {
                returnType = Character.class.getName();
                primitiveConverterMethod = "charValue";
            } else if (expectedType.equals(Short.TYPE)) {
                returnType = Short.class.getName();
                primitiveConverterMethod = "shortValue";
            } else if (expectedType.equals(Integer.TYPE)) {
                returnType = Integer.class.getName();
                primitiveConverterMethod = "intValue";
            } else if (expectedType.equals(Long.TYPE)) {
                returnType = Long.class.getName();
                primitiveConverterMethod = "longValue";
            } else if (expectedType.equals(Float.TYPE)) {
                returnType = Float.class.getName();
                primitiveConverterMethod = "floatValue";
            } else if (expectedType.equals(Double.TYPE)) {
                returnType = Double.class.getName();
                primitiveConverterMethod = "doubleValue";
            }
        } else {
            returnType = toJavaSourceType(returnType);
        }

        targetType = toJavaSourceType(targetType);

        StringBuilder call = new StringBuilder("(" + returnType + ") " + "org.glassfish.wasp.runtime.PageContextImpl.evaluateExpression" + "("
                + Generator.quote(expression) + ", " + targetType + ".class, " + "(PageContext)" + jspCtxt + ", " + fnmapvar + ")");

        /*
         * Add the primitive converter method if we need to.
         */
        if (primitiveConverterMethod != null) {
            call.insert(0, "(");
            call.append(")." + primitiveConverterMethod + "()");
        }

        return call.toString();
    }

    /**
     * Validates the syntax of all EL expressions within the given string.
     *
     * @param where the approximate location of the expressions in the JSP page
     * @param expressions a string containing an EL expressions
     * @param err an error dispatcher to use
     */
    public static void validateExpressions(Mark where, String expressions, FunctionMapper functionMapper, ErrorDispatcher err) throws WaspException {
        try {
            ELContextImpl elContext = new ELContextImpl(null);
            elContext.setFunctionMapper(functionMapper);
            getExpressionFactory().createValueExpression(elContext, expressions, Object.class);
        } catch (ELException e) {
            err.jspError(where, "jsp.error.invalid.expression", expressions, e.toString());
        }
    }

    public static Object coerce(Class<?> targetType, String value) throws ELException {
        return getExpressionFactory().coerceToType(value, targetType);
    }

    /**
     * Resets the temporary variable name. (not thread-safe)
     */
    public static void resetTemporaryVariableName() {
        tempSequenceNumber = 0;
    }

    /**
     * Generates a new temporary variable name. (not thread-safe)
     */
    public static String nextTemporaryVariableName() {
        return Constants.TEMP_VARIABLE_NAME_PREFIX + tempSequenceNumber++;
    }

    public static String coerceToPrimitiveBoolean(String s, boolean isNamedAttribute) {
        if (isNamedAttribute) {
            return "org.glassfish.wasp.runtime.JspRuntimeLibrary.coerceToBoolean(" + s + ")";
        }
        
        if (s == null || s.length() == 0) {
            return "false";
        }
        
        return Boolean.valueOf(s).toString();
    }

    public static String coerceToBoolean(String s, boolean isNamedAttribute) {
        if (isNamedAttribute) {
            return "(Boolean) org.glassfish.wasp.runtime.JspRuntimeLibrary.coerce(" + s + ", Boolean.class)";
        }
        
        if (s == null || s.length() == 0) {
            return "Boolean.FALSE";
        }
        
        // Detect format error at translation time
        return "new Boolean(" + Boolean.valueOf(s).toString() + ")";
    }

    public static String coerceToPrimitiveByte(String s, boolean isNamedAttribute) {
        if (isNamedAttribute) {
            return "org.glassfish.wasp.runtime.JspRuntimeLibrary.coerceToByte(" + s + ")";
        }

        if (s == null || s.length() == 0) {
            return "(byte) 0";
        }

        return "((byte)" + Byte.valueOf(s).toString() + ")";
    }

    public static String coerceToByte(String s, boolean isNamedAttribute) {
        if (isNamedAttribute) {
            return "(Byte) org.glassfish.wasp.runtime.JspRuntimeLibrary.coerce(" + s + ", Byte.class)";
        }

        if (s == null || s.length() == 0) {
            return "Byte.valueOf((byte) 0)";
        }
        
        // Detect format error at translation time
        return "new Byte((byte)" + Byte.valueOf(s).toString() + ")";
    }

    public static String coerceToChar(String s, boolean isNamedAttribute) {
        if (isNamedAttribute) {
            return "org.glassfish.wasp.runtime.JspRuntimeLibrary.coerceToChar(" + s + ")";
        }

        if (s == null || s.length() == 0) {
            return "(char) 0";
        }

        char ch = s.charAt(0);
        // this trick avoids escaping issues
        return "((char) " + (int) ch + ")";

    }

    public static String coerceToCharacter(String s, boolean isNamedAttribute) {
        if (isNamedAttribute) {
            return "(Character) org.glassfish.wasp.runtime.JspRuntimeLibrary.coerce(" + s + ", Character.class)";
        }
        
        if (s == null || s.length() == 0) {
            return "new Character((char) 0)";
        }
        
        char ch = s.charAt(0);
        // this trick avoids escaping issues
        return "new Character((char) " + (int) ch + ")";
    }

    public static String coerceToPrimitiveDouble(String s, boolean isNamedAttribute) {
        if (isNamedAttribute) {
            return "org.glassfish.wasp.runtime.JspRuntimeLibrary.coerceToDouble(" + s + ")";
        }
        
        if (s == null || s.length() == 0) {
            return "(double) 0";
        }
        
        return Double.valueOf(s).toString();
    }

    public static String coerceToDouble(String s, boolean isNamedAttribute) {
        if (isNamedAttribute) {
            return "(Double) org.glassfish.wasp.runtime.JspRuntimeLibrary.coerce(" + s + ", Double.class)";
        }
        
        if (s == null || s.length() == 0) {
            return "new Double(0)";
        }
        
        // Detect format error at translation time
        return "new Double(" + Double.valueOf(s).toString() + ")";
    }

    public static String coerceToPrimitiveFloat(String s, boolean isNamedAttribute) {
        if (isNamedAttribute) {
            return "org.glassfish.wasp.runtime.JspRuntimeLibrary.coerceToFloat(" + s + ")";
        }
        
        if (s == null || s.length() == 0) {
            return "(float) 0";
        }
        
        return Float.valueOf(s).toString() + "f";
    }

    public static String coerceToFloat(String s, boolean isNamedAttribute) {
        if (isNamedAttribute) {
            return "(Float) org.glassfish.wasp.runtime.JspRuntimeLibrary.coerce(" + s + ", Float.class)";
        }
        
        if (s == null || s.length() == 0) {
            return "Float.valueOf(0)";
        }
        
        // Detect format error at translation time
        return "new Float(" + Float.valueOf(s).toString() + "f)";
    }

    public static String coerceToInt(String s, boolean isNamedAttribute) {
        if (isNamedAttribute) {
            return "org.glassfish.wasp.runtime.JspRuntimeLibrary.coerceToInt(" + s + ")";
        }
        
        if (s == null || s.length() == 0) {
            return "0";
        }
        
        return Integer.valueOf(s).toString();
    }

    public static String coerceToInteger(String s, boolean isNamedAttribute) {
        if (isNamedAttribute) {
            return "(Integer) org.glassfish.wasp.runtime.JspRuntimeLibrary.coerce(" + s + ", Integer.class)";
        }
        
        if (s == null || s.length() == 0) {
            return "Integer.valueOf(0)";
        }
        
        // Detect format error at translation time
        return "new Integer(" + Integer.valueOf(s).toString() + ")";
    }

    public static String coerceToPrimitiveShort(String s, boolean isNamedAttribute) {
        if (isNamedAttribute) {
            return "org.glassfish.wasp.runtime.JspRuntimeLibrary.coerceToShort(" + s + ")";
        }
        
        if (s == null || s.length() == 0) {
            return "(short) 0";
        }
        
        return "((short) " + Short.valueOf(s).toString() + ")";
    }

    public static String coerceToShort(String s, boolean isNamedAttribute) {
        if (isNamedAttribute) {
            return "(Short) org.glassfish.wasp.runtime.JspRuntimeLibrary.coerce(" + s + ", Short.class)";
        }
        
        if (s == null || s.length() == 0) {
            return "Short.valueOf((short) 0)";
        }
        
        // Detect format error at translation time
        return "new Short(\"" + Short.valueOf(s).toString() + "\")";

    }

    public static String coerceToPrimitiveLong(String s, boolean isNamedAttribute) {
        if (isNamedAttribute) {
            return "org.glassfish.wasp.runtime.JspRuntimeLibrary.coerceToLong(" + s + ")";
        }
        
        if (s == null || s.length() == 0) {
            return "(long) 0";
        }
        
        return Long.valueOf(s).toString() + "l";
    }

    public static String coerceToLong(String s, boolean isNamedAttribute) {
        if (isNamedAttribute) {
            return "(Long) org.glassfish.wasp.runtime.JspRuntimeLibrary.coerce(" + s + ", Long.class)";
        }
        
        if (s == null || s.length() == 0) {
            return "Long.valueOf(0)";
        }
        
        // Detect format error at translation time
        return "new Long(" + Long.valueOf(s).toString() + "l)";
    }

    public static String coerceToEnum(String s, String enumClass, boolean isNamedAttribute) {
        if (isNamedAttribute) {
            return "org.glassfish.wasp.runtime.JspRuntimeLibrary.coerce(" + s + "," + enumClass + ".class)";
        }
        
        if (s == null || s.length() == 0) {
            return "null";
        }
        
        return "Enum.valueOf(" + enumClass + ".class, \"" + s + "\")";
    }

    public static InputStream getInputStream(String fname, JarFile jarFile, JspCompilationContext ctxt, ErrorDispatcher err) throws WaspException, IOException {
        InputStream in = null;

        if (jarFile != null) {
            String jarEntryName = fname.substring(1, fname.length());
            ZipEntry jarEntry = jarFile.getEntry(jarEntryName);
            if (jarEntry == null) {
                err.jspError("jsp.error.file.not.found", fname);
            }
            in = jarFile.getInputStream(jarEntry);
        } else {
            in = ctxt.getResourceAsStream(fname);
        }

        if (in == null) {
            err.jspError("jsp.error.file.not.found", fname);
        }

        return in;
    }

    /**
     * Gets the fully-qualified class name of the tag handler corresponding to the given tag file path.
     *
     * @param path Tag file path
     * @param err Error dispatcher
     *
     * @return Fully-qualified class name of the tag handler corresponding to the given tag file path
     */
    public static String getTagHandlerClassName(String path, ErrorDispatcher err) throws WaspException {

        String className = null;
        int begin = 0;
        int index;

        index = path.lastIndexOf(".tag");
        if (index == -1) {
            err.jspError("jsp.error.tagfile.badSuffix", path);
        }

        // It's tempting to remove the ".tag" suffix here, but we can't.
        // If we remove it, the fully-qualified class name of this tag
        // could conflict with the package name of other tags.
        // For instance, the tag file
        // /WEB-INF/tags/foo.tag
        // would have fully-qualified class name
        // org.apache.jsp.tag.web.foo
        // which would conflict with the package name of the tag file
        // /WEB-INF/tags/foo/bar.tag

        index = path.indexOf(WEB_INF_TAGS);
        if (index != -1) {
            className = "org.apache.jsp.tag.web.";
            begin = index + WEB_INF_TAGS.length();
        } else {
            index = path.indexOf(META_INF_TAGS);
            if (index != -1) {
                className = "org.apache.jsp.tag.meta.";
                begin = index + META_INF_TAGS.length();
            } else {
                err.jspError("jsp.error.tagfile.illegalPath", path);
            }
        }

        className += makeJavaPackage(path.substring(begin));

        return className;
    }

    /**
     * Converts the given path to a Java package or fully-qualified class name
     *
     * @param path Path to convert
     *
     * @return Java package corresponding to the given path
     */
    public static final String makeJavaPackage(String path) {
        String classNameComponents[] = split(path, "/");
        StringBuilder legalClassNames = new StringBuilder();
        for (int i = 0; i < classNameComponents.length; i++) {
            legalClassNames.append(makeJavaIdentifier(classNameComponents[i]));
            if (i < classNameComponents.length - 1) {
                legalClassNames.append('.');
            }
        }
        return legalClassNames.toString();
    }

    /**
     * Splits a string into it's components.
     *
     * @param path String to split
     * @param pat Pattern to split at
     * @return the components of the path
     */
    private static final String[] split(String path, String pat) {
        ArrayList<String> comps = new ArrayList<>();
        int pos = path.indexOf(pat);
        int start = 0;
        while (pos >= 0) {
            if (pos > start) {
                String comp = path.substring(start, pos);
                comps.add(comp);
            }
            start = pos + pat.length();
            pos = path.indexOf(pat, start);
        }
        if (start < path.length()) {
            comps.add(path.substring(start));
        }

        String[] result = new String[comps.size()];
        for (int i = 0; i < comps.size(); i++) {
            result[i] = comps.get(i);
        }

        return result;
    }

    /**
     * Converts the given identifier to a legal Java identifier
     *
     * @param identifier Identifier to convert
     *
     * @return Legal Java identifier corresponding to the given identifier
     */
    public static final String makeJavaIdentifier(String identifier) {
        StringBuilder modifiedIdentifier = new StringBuilder(identifier.length());
        if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
            modifiedIdentifier.append('_');
        }
        for (int i = 0; i < identifier.length(); i++) {
            char ch = identifier.charAt(i);
            if (Character.isJavaIdentifierPart(ch) && ch != '_') {
                modifiedIdentifier.append(ch);
            } else if (ch == '.') {
                modifiedIdentifier.append('_');
            } else {
                modifiedIdentifier.append(mangleChar(ch));
            }
        }
        if (isJavaKeyword(modifiedIdentifier.toString())) {
            modifiedIdentifier.append('_');
        }
        return modifiedIdentifier.toString();
    }

    /**
     * Mangle the specified character to create a legal Java class name.
     */
    public static final String mangleChar(char ch) {
        char[] result = new char[5];
        result[0] = '_';
        result[1] = Character.forDigit(ch >> 12 & 0xf, 16);
        result[2] = Character.forDigit(ch >> 8 & 0xf, 16);
        result[3] = Character.forDigit(ch >> 4 & 0xf, 16);
        result[4] = Character.forDigit(ch & 0xf, 16);

        return new String(result);
    }

    /**
     * Test whether the argument is a Java keyword
     */
    public static boolean isJavaKeyword(String key) {
        int i = 0;
        int j = javaKeywords.length;
        while (i < j) {
            int k = (i + j) / 2;
            int result = javaKeywords[k].compareTo(key);
            if (result == 0) {
                return true;
            }
            if (result < 0) {
                i = k + 1;
            } else {
                j = k;
            }
        }
        return false;
    }

    /**
     * Converts the given Xml name to a legal Java identifier. This is slightly more efficient than makeJavaIdentifier in
     * that we only need to worry about '.', '-', and ':' in the string. We also assume that the resultant string is further
     * concatenated with some prefix string so that we don't have to worry about it being a Java key word.
     *
     * @param name Identifier to convert
     *
     * @return Legal Java identifier corresponding to the given identifier
     */
    public static final String makeXmlJavaIdentifier(String name) {
        if (name.indexOf('-') >= 0) {
            name = replace(name, '-', "$1");
        }
        if (name.indexOf('.') >= 0) {
            name = replace(name, '.', "$2");
        }
        if (name.indexOf(':') >= 0) {
            name = replace(name, ':', "$3");
        }
        return name;
    }

    static InputStreamReader getReader(String fname, String encoding, JarFile jarFile, JspCompilationContext ctxt, ErrorDispatcher err)
            throws WaspException, IOException {

        InputStreamReader reader = null;
        InputStream in = getInputStream(fname, jarFile, ctxt, err);

        try {
            reader = new InputStreamReader(in, encoding);
        } catch (UnsupportedEncodingException ex) {
            err.jspError("jsp.error.unsupported.encoding", encoding);
        }

        return reader;
    }

    /**
     * Class.getName() return arrays in the form "[[[<et>", where et, the element type can be one of ZBCDFIJS or
     * L<classname>; It is converted into forms that can be understood by javac.
     */
    public static String toJavaSourceType(String type) {
        if (type.charAt(0) != '[') {
            return type;
        }

        int dims = 1;
        String t = null;
        for (int i = 1; i < type.length(); i++) {
            if (type.charAt(i) == '[') {
                dims++;
            } else {
                switch (type.charAt(i)) {
                case 'Z':
                    t = "boolean";
                    break;
                case 'B':
                    t = "byte";
                    break;
                case 'C':
                    t = "char";
                    break;
                case 'D':
                    t = "double";
                    break;
                case 'F':
                    t = "float";
                    break;
                case 'I':
                    t = "int";
                    break;
                case 'J':
                    t = "long";
                    break;
                case 'S':
                    t = "short";
                    break;
                case 'L':
                    t = type.substring(i + 1, type.indexOf(';'));
                    break;
                }
                break;
            }
        }
        StringBuilder resultType = new StringBuilder(t);
        for (; dims > 0; dims--) {
            resultType.append("[]");
        }
        return resultType.toString();
    }

    /**
     * Compute the canonical name from a Class instance. Note that a simple replacment of '$' with '.' of a binary name
     * would not work, as '$' is a legal Java Identifier character.
     *
     * @param c A instance of java.lang.Class
     * @return The canonical name of c.
     */
    public static String getCanonicalName(Class<?> c) {

        String binaryName = c.getName();
        c = c.getDeclaringClass();

        if (c == null) {
            return binaryName;
        }

        StringBuilder buf = new StringBuilder(binaryName);
        do {
            buf.setCharAt(c.getName().length(), '.');
            c = c.getDeclaringClass();
        } while (c != null);

        return buf.toString();
    }

    private static ExpressionFactory getExpressionFactory() {
        if (expFactory == null) {
            expFactory = ExpressionFactory.newInstance();
        }
        return expFactory;
    }

    static Map<String, Manifest> manifestMap = new ConcurrentHashMap<>();
    static Manifest nullManifest = new Manifest();

    /**
     * Given a list of jar files, their manifest attribute Class-path are scanned, and jars specified there are added to the
     * list. This is carried out recursively. Note: This is needed to work around the JDK bug 6725230.
     */
    public static List<String> expandClassPath(List<String> files) {

        for (int i = 0; i < files.size(); i++) {
            String file = files.get(i);
            if (!file.endsWith(".jar")) {
                continue;
            }
            Manifest manifest = manifestMap.get(file);
            JarFile jarfile = null;
            if (manifest == null) {
                try {
                    jarfile = new JarFile(file, false);
                    manifest = jarfile.getManifest();
                    if (manifest == null) {
                        // mark jar file as known to contain no manifest
                        manifestMap.put(file, nullManifest);
                        continue;
                    } else if (!file.contains("/WEB-INF")) {
                        // Don't cache any jars bundled with the app.
                        manifestMap.put(file, manifest);
                    }
                } catch (IOException ex) {
                    // Ignored
                    continue;
                } finally {
                    try {
                        if (jarfile != null) {
                            jarfile.close();
                        }
                    } catch (IOException ex) {
                        // Ignored
                    }
                }
            } else if (manifest == nullManifest) {
                continue;
            }

            java.util.jar.Attributes attrs = manifest.getMainAttributes();
            String classPath = attrs.getValue("Class-Path");
            if (classPath == null) {
                continue;
            }

            String[] paths = classPath.split(" ");
            int lastIndex = file.lastIndexOf(File.separatorChar);
            String baseDir = "";
            if (lastIndex > 0) {
                baseDir = file.substring(0, lastIndex + 1);
            }
            for (String path : paths) {
                String p;
                if (path.startsWith(File.separator)) {
                    p = path;
                } else {
                    p = baseDir + path;
                }
                if (!files.contains(p)) {
                    files.add(p);
                }
            }
        }

        return files;
    }

}
