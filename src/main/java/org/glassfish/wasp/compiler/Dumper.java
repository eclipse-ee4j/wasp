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

import org.glassfish.wasp.WaspException;
import org.xml.sax.Attributes;

class Dumper {

    static class DumpVisitor extends Node.Visitor {
        private int indent = 0;

        private String getAttributes(Attributes attrs) {
            if (attrs == null) {
                return "";
            }

            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < attrs.getLength(); i++) {
                buf.append(" " + attrs.getQName(i) + "=\"" + attrs.getValue(i) + "\"");
            }
            return buf.toString();
        }

        private void printString(String str) {
            printIndent();
            System.out.print(str);
        }

        private void printString(String prefix, String str, String suffix) {
            printIndent();
            if (str != null) {
                System.out.print(prefix + str + suffix);
            } else {
                System.out.print(prefix + suffix);
            }
        }

        private void printAttributes(String prefix, Attributes attrs, String suffix) {
            printString(prefix, getAttributes(attrs), suffix);
        }

        private void dumpBody(Node n) throws WaspException {
            Node.Nodes page = n.getBody();
            if (page != null) {
//		indent++;
                page.visit(this);
//		indent--;
            }
        }

        @Override
        public void visit(Node.PageDirective n) throws WaspException {
            printAttributes("<%@ page", n.getAttributes(), "%>");
        }

        @Override
        public void visit(Node.TaglibDirective n) throws WaspException {
            printAttributes("<%@ taglib", n.getAttributes(), "%>");
        }

        @Override
        public void visit(Node.IncludeDirective n) throws WaspException {
            printAttributes("<%@ include", n.getAttributes(), "%>");
            dumpBody(n);
        }

        @Override
        public void visit(Node.Comment n) throws WaspException {
            printString("<%--", n.getText(), "--%>");
        }

        @Override
        public void visit(Node.Declaration n) throws WaspException {
            printString("<%!", n.getText(), "%>");
        }

        @Override
        public void visit(Node.Expression n) throws WaspException {
            printString("<%=", n.getText(), "%>");
        }

        @Override
        public void visit(Node.Scriptlet n) throws WaspException {
            printString("<%", n.getText(), "%>");
        }

        @Override
        public void visit(Node.IncludeAction n) throws WaspException {
            printAttributes("<jsp:include", n.getAttributes(), ">");
            dumpBody(n);
            printString("</jsp:include>");
        }

        @Override
        public void visit(Node.ForwardAction n) throws WaspException {
            printAttributes("<jsp:forward", n.getAttributes(), ">");
            dumpBody(n);
            printString("</jsp:forward>");
        }

        @Override
        public void visit(Node.GetProperty n) throws WaspException {
            printAttributes("<jsp:getProperty", n.getAttributes(), "/>");
        }

        @Override
        public void visit(Node.SetProperty n) throws WaspException {
            printAttributes("<jsp:setProperty", n.getAttributes(), ">");
            dumpBody(n);
            printString("</jsp:setProperty>");
        }

        @Override
        public void visit(Node.UseBean n) throws WaspException {
            printAttributes("<jsp:useBean", n.getAttributes(), ">");
            dumpBody(n);
            printString("</jsp:useBean>");
        }

        @Override
        public void visit(Node.PlugIn n) throws WaspException {
            printAttributes("<jsp:plugin", n.getAttributes(), ">");
            dumpBody(n);
            printString("</jsp:plugin>");
        }

        @Override
        public void visit(Node.ParamsAction n) throws WaspException {
            printAttributes("<jsp:params", n.getAttributes(), ">");
            dumpBody(n);
            printString("</jsp:params>");
        }

        @Override
        public void visit(Node.ParamAction n) throws WaspException {
            printAttributes("<jsp:param", n.getAttributes(), ">");
            dumpBody(n);
            printString("</jsp:param>");
        }

        @Override
        public void visit(Node.NamedAttribute n) throws WaspException {
            printAttributes("<jsp:attribute", n.getAttributes(), ">");
            dumpBody(n);
            printString("</jsp:attribute>");
        }

        @Override
        public void visit(Node.JspBody n) throws WaspException {
            printAttributes("<jsp:body", n.getAttributes(), ">");
            dumpBody(n);
            printString("</jsp:body>");
        }

        @Override
        public void visit(Node.ELExpression n) throws WaspException {
            printString(n.getText());
        }

        @Override
        public void visit(Node.CustomTag n) throws WaspException {
            printAttributes("<" + n.getQName(), n.getAttributes(), ">");
            dumpBody(n);
            printString("</" + n.getQName() + ">");
        }

        @Override
        public void visit(Node.UninterpretedTag n) throws WaspException {
            String tag = n.getQName();
            printAttributes("<" + tag, n.getAttributes(), ">");
            dumpBody(n);
            printString("</" + tag + ">");
        }

        @Override
        public void visit(Node.TemplateText n) throws WaspException {
            printString(n.getText());
        }

        private void printIndent() {
            for (int i = 0; i < indent; i++) {
                System.out.print("  ");
            }
        }
    }

    public static void dump(Node n) {
        try {
            n.accept(new DumpVisitor());
        } catch (WaspException e) {
            e.printStackTrace();
        }
    }

    public static void dump(Node.Nodes page) {
        try {
            page.visit(new DumpVisitor());
        } catch (WaspException e) {
            e.printStackTrace();
        }
    }
}
