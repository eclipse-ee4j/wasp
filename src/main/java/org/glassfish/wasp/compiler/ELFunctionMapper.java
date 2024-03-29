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

package org.glassfish.wasp.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.glassfish.wasp.WaspException;

import jakarta.servlet.jsp.tagext.FunctionInfo;

/**
 * This class generates functions mappers for the EL expressions in the page. Instead of a global mapper, a mapper is
 * used for ecah call to EL evaluator, thus avoiding the prefix overlapping and redefinition issues.
 *
 * @author Kin-man Chung
 */

public class ELFunctionMapper {
    private int currFunc = 0;
    StringBuilder ds; // Contains codes to initialize the functions mappers.
    StringBuilder ss; // Contains declarations of the functions mappers.

    /**
     * Creates the functions mappers for all EL expressions in the JSP page.
     *
     * @param compiler Current compiler, mainly for accessing error dispatcher.
     * @param page The current compilation unit.
     */
    public static void map(Compiler compiler, Node.Nodes page) throws WaspException {

        ELFunctionMapper map = new ELFunctionMapper();
        map.ds = new StringBuilder();
        map.ss = new StringBuilder();

        page.visit(map.new ELFunctionVisitor());

        // Append the declarations to the root node
        String ds = map.ds.toString();
        if (ds.length() > 0) {
            Node root = page.getRoot();
            new Node.Declaration(map.ss.toString(), null, root);
            new Node.Declaration("static {\n" + ds + "}\n", null, root);
        }
    }

    /**
     * A visitor for the page. The places where EL is allowed are scanned for functions, and if found functions mappers are
     * created.
     */
    class ELFunctionVisitor extends Node.Visitor {

        /**
         * Use a global name map to facilitate reuse of function maps. The key used is prefix:function:uri.
         */
        private HashMap<String, String> gMap = new HashMap<>();

        @Override
        public void visit(Node.ParamAction n) throws WaspException {
            doMap(n.getValue());
            visitBody(n);
        }

        @Override
        public void visit(Node.IncludeAction n) throws WaspException {
            doMap(n.getPage());
            visitBody(n);
        }

        @Override
        public void visit(Node.ForwardAction n) throws WaspException {
            doMap(n.getPage());
            visitBody(n);
        }

        @Override
        public void visit(Node.SetProperty n) throws WaspException {
            doMap(n.getValue());
            visitBody(n);
        }

        @Override
        public void visit(Node.UseBean n) throws WaspException {
            doMap(n.getBeanName());
            visitBody(n);
        }

        @Override
        public void visit(Node.PlugIn n) throws WaspException {
            doMap(n.getHeight());
            doMap(n.getWidth());
            visitBody(n);
        }

        @Override
        public void visit(Node.JspElement n) throws WaspException {

            Node.JspAttribute[] attrs = n.getJspAttributes();
            for (int i = 0; attrs != null && i < attrs.length; i++) {
                doMap(attrs[i]);
            }
            doMap(n.getNameAttribute());
            visitBody(n);
        }

        @Override
        public void visit(Node.UninterpretedTag n) throws WaspException {

            Node.JspAttribute[] attrs = n.getJspAttributes();
            for (int i = 0; attrs != null && i < attrs.length; i++) {
                doMap(attrs[i]);
            }
            visitBody(n);
        }

        @Override
        public void visit(Node.CustomTag n) throws WaspException {
            Node.JspAttribute[] attrs = n.getJspAttributes();
            for (int i = 0; attrs != null && i < attrs.length; i++) {
                doMap(attrs[i]);
            }
            visitBody(n);
        }

        @Override
        public void visit(Node.ELExpression n) throws WaspException {
            doMap(n.getEL());
        }

        private void doMap(Node.JspAttribute attr) throws WaspException {
            if (attr != null) {
                doMap(attr.getEL());
            }
        }

        /**
         * Creates function mappers, if needed, from ELNodes
         */
        private void doMap(ELNode.Nodes el) throws WaspException {

            // Only care about functions in ELNode's
            class Fvisitor extends ELNode.Visitor {
                ArrayList<ELNode.Function> funcs = new ArrayList<>();
                Set<String> keys = new HashSet<>();

                @Override
                public void visit(ELNode.Function n) throws WaspException {
                    if (n.getUri() == null) {
                        // Can be a lambda expresion call
                        return;
                    }
                    String key = n.getPrefix() + ":" + n.getName();
                    if (!keys.contains(key)) {
                        keys.add(key);
                        funcs.add(n);
                    }
                }
            }

            if (el == null) {
                return;
            }

            // First locate all unique functions in this EL
            Fvisitor fv = new Fvisitor();
            el.visit(fv);
            ArrayList<ELNode.Function> functions = fv.funcs;

            if (functions.size() == 0) {
                return;
            }

            // Reuse a previous map if possible
            String decName = matchMap(functions);
            if (decName != null) {
                el.setMapName(decName);
                return;
            }

            // Generate declaration for the map statically
            decName = getMapName();
            ss.append("static private org.glassfish.wasp.runtime.ProtectedFunctionMapper " + decName + ";\n");

            ds.append("  " + decName + "= ");
            ds.append("org.glassfish.wasp.runtime.ProtectedFunctionMapper");

            // Special case if there is only one function in the map
            String funcMethod = null;
            if (functions.size() == 1) {
                funcMethod = ".getMapForFunction";
            } else {
                ds.append(".getInstance();\n");
                funcMethod = "  " + decName + ".mapFunction";
            }

            // Setup arguments for either getMapForFunction or mapFunction
            for (int i = 0; i < functions.size(); i++) {
                ELNode.Function f = functions.get(i);
                FunctionInfo funcInfo = f.getFunctionInfo();
                String key = f.getPrefix() + ":" + f.getName();
                ds.append(funcMethod + "(\"" + key + "\", " + funcInfo.getFunctionClass() + ".class, " + '\"' + f.getMethodName() + "\", " + "new Class[] {");
                String params[] = f.getParameters();
                for (int k = 0; k < params.length; k++) {
                    if (k != 0) {
                        ds.append(", ");
                    }
                    int iArray = params[k].indexOf('[');
                    if (iArray < 0) {
                        ds.append(params[k] + ".class");
                    } else {
                        String baseType = params[k].substring(0, iArray);
                        ds.append("java.lang.reflect.Array.newInstance(");
                        ds.append(baseType);
                        ds.append(".class,");

                        // Count the number of array dimension
                        int aCount = 0;
                        for (int jj = iArray; jj < params[k].length(); jj++) {
                            if (params[k].charAt(jj) == '[') {
                                aCount++;
                            }
                        }
                        if (aCount == 1) {
                            ds.append("0).getClass()");
                        } else {
                            ds.append("new int[" + aCount + "]).getClass()");
                        }
                    }
                }
                ds.append("});\n");
                // Put the current name in the global function map
                gMap.put(f.getPrefix() + ':' + f.getName() + ':' + f.getUri(), decName);
            }
            el.setMapName(decName);
        }

        /**
         * Find the name of the function mapper for an EL. Reuse a previously generated one if possible.
         *
         * @param functions An ArrayList of ELNode.Function instances that represents the functions in an EL
         * @return A previous generated function mapper name that can be used by this EL; null if none found.
         */
        private String matchMap(ArrayList<ELNode.Function> functions) {

            String mapName = null;
            for (int i = 0; i < functions.size(); i++) {
                ELNode.Function f = functions.get(i);
                String temName = gMap.get(f.getPrefix() + ':' + f.getName() + ':' + f.getUri());
                if (temName == null) {
                    return null;
                }
                if (mapName == null) {
                    mapName = temName;
                } else if (!temName.equals(mapName)) {
                    // If not all in the previous match, then no match.
                    return null;
                }
            }
            return mapName;
        }

        /*
         * @return An unique name for a function mapper.
         */
        private String getMapName() {
            return "_jspx_fnmap_" + currFunc++;
        }
    }
}
