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

import org.eclipse.wasp.WaspException;

/**
 * Collect info about the page and nodes, and make them availabe through the PageInfo object.
 *
 * @author Kin-man Chung
 * @author Mark Roth
 */

class Collector {

    /**
     * A visitor for collecting information on the page and the body of the custom tags.
     */
    static class CollectVisitor extends Node.Visitor {

        private int maxTagNesting = 0;
        private int curTagNesting = 0;
        private boolean scriptingElementSeen = false;
        private boolean usebeanSeen = false;
        private boolean includeActionSeen = false;
        private boolean paramActionSeen = false;
        private boolean setPropertySeen = false;
        private boolean hasScriptingVars = false;

        @Override
        public void visit(Node.ParamAction n) throws WaspException
        {
            if (n.getValue().isExpression()) {
                scriptingElementSeen = true;
            }
            paramActionSeen = true;
        }

        @Override
        public void visit(Node.IncludeAction n) throws WaspException
        {
            if (n.getPage().isExpression()) {
                scriptingElementSeen = true;
            }
            includeActionSeen = true;
            visitBody(n);
        }

        @Override
        public void visit(Node.ForwardAction n) throws WaspException
        {
            if (n.getPage().isExpression()) {
                scriptingElementSeen = true;
            }
            visitBody(n);
        }

        @Override
        public void visit(Node.SetProperty n) throws WaspException
        {
            if (n.getValue() != null && n.getValue().isExpression()) {
                scriptingElementSeen = true;
            }
            setPropertySeen = true;
        }

        @Override
        public void visit(Node.UseBean n) throws WaspException
        {
            if (n.getBeanName() != null && n.getBeanName().isExpression()) {
                scriptingElementSeen = true;
            }
            usebeanSeen = true;
            visitBody(n);
        }

        @Override
        public void visit(Node.PlugIn n) throws WaspException
        {
            if (n.getHeight() != null && n.getHeight().isExpression()) {
                scriptingElementSeen = true;
            }
            if (n.getWidth() != null && n.getWidth().isExpression()) {
                scriptingElementSeen = true;
            }
            visitBody(n);
        }

        @Override
        public void visit(Node.CustomTag n) throws WaspException
        {

            curTagNesting++;
            if (curTagNesting > maxTagNesting) {
                maxTagNesting = curTagNesting;
            }

            // Check to see what kinds of element we see as child elements
            checkSeen(n.getChildInfo(), n);

            curTagNesting--;
        }

        /**
         * Check all child nodes for various elements and update the given ChildInfo object accordingly. Visits body in the
         * process.
         */
        private void checkSeen(Node.ChildInfo childInfo, Node node) throws WaspException
        {
            // save values collected so far
            boolean scriptingElementSeenSave = scriptingElementSeen;
            scriptingElementSeen = false;
            boolean usebeanSeenSave = usebeanSeen;
            usebeanSeen = false;
            boolean includeActionSeenSave = includeActionSeen;
            includeActionSeen = false;
            boolean paramActionSeenSave = paramActionSeen;
            paramActionSeen = false;
            boolean setPropertySeenSave = setPropertySeen;
            setPropertySeen = false;
            boolean hasScriptingVarsSave = hasScriptingVars;
            hasScriptingVars = false;

            // Scan attribute list for expressions
            if (node instanceof Node.CustomTag) {
                Node.CustomTag ct = (Node.CustomTag) node;
                Node.JspAttribute[] attrs = ct.getJspAttributes();
                for (int i = 0; attrs != null && i < attrs.length; i++) {
                    if (attrs[i].isExpression()) {
                        scriptingElementSeen = true;
                        break;
                    }
                }
            }

            visitBody(node);

            if (node instanceof Node.CustomTag && !hasScriptingVars) {
                Node.CustomTag ct = (Node.CustomTag) node;
                hasScriptingVars = ct.getVariableInfos().length > 0 || ct.getTagVariableInfos().length > 0;
            }

            // Record if the tag element and its body contains any scriptlet.
            childInfo.setScriptless(!scriptingElementSeen);
            childInfo.setHasUseBean(usebeanSeen);
            childInfo.setHasIncludeAction(includeActionSeen);
            childInfo.setHasParamAction(paramActionSeen);
            childInfo.setHasSetProperty(setPropertySeen);
            childInfo.setHasScriptingVars(hasScriptingVars);

            // Propagate value of scriptingElementSeen up.
            scriptingElementSeen = scriptingElementSeen || scriptingElementSeenSave;
            usebeanSeen = usebeanSeen || usebeanSeenSave;
            setPropertySeen = setPropertySeen || setPropertySeenSave;
            includeActionSeen = includeActionSeen || includeActionSeenSave;
            paramActionSeen = paramActionSeen || paramActionSeenSave;
            hasScriptingVars = hasScriptingVars || hasScriptingVarsSave;
        }

        @Override
        public void visit(Node.JspElement n) throws WaspException
        {
            if (n.getNameAttribute().isExpression()) {
                scriptingElementSeen = true;
            }

            Node.JspAttribute[] attrs = n.getJspAttributes();
            for (int i = 0; i < attrs.length; i++) {
                if (attrs[i].isExpression()) {
                    scriptingElementSeen = true;
                    break;
                }
            }
            visitBody(n);
        }

        @Override
        public void visit(Node.JspBody n) throws WaspException
        {
            checkSeen(n.getChildInfo(), n);
        }

        @Override
        public void visit(Node.NamedAttribute n) throws WaspException
        {
            checkSeen(n.getChildInfo(), n);
        }

        @Override
        public void visit(Node.Declaration n) throws WaspException
        {
            scriptingElementSeen = true;
        }

        @Override
        public void visit(Node.Expression n) throws WaspException
        {
            scriptingElementSeen = true;
        }

        @Override
        public void visit(Node.Scriptlet n) throws WaspException
        {
            scriptingElementSeen = true;
        }

        public void updatePageInfo(PageInfo pageInfo) {
            pageInfo.setMaxTagNesting(maxTagNesting);
            pageInfo.setScriptless(!scriptingElementSeen);
        }
    }

    public static void collect(Compiler compiler, Node.Nodes page) throws WaspException
    {
        CollectVisitor collectVisitor = new CollectVisitor();
        page.visit(collectVisitor);
        collectVisitor.updatePageInfo(compiler.getPageInfo());

    }
}
