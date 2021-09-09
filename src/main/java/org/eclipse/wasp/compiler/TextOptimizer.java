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
import org.eclipse.wasp.Options;

/**
 */
public class TextOptimizer {

    /**
     * A visitor to concatenate contiguous template texts.
     */
    static class TextCatVisitor extends Node.Visitor {

        private int textNodeCount = 0;
        private Node.TemplateText firstTextNode = null;
        private StringBuilder textBuffer;
        private final String emptyText = new String("");
        private boolean prePass;
        private boolean trim;

        public TextCatVisitor(boolean prePass, boolean trim) {
            this.prePass = prePass;
            this.trim = trim;
        }

        @Override
        public void doVisit(Node n) throws WaspException
        {
            collectText();
        }

        /*
         * The following directives are ignored in text concatenation except in the pre pass phase.
         */

        @Override
        public void visit(Node.PageDirective n) throws WaspException
        {
            if (prePass) {
                collectText();
            }
        }

        @Override
        public void visit(Node.TagDirective n) throws WaspException
        {
            if (prePass) {
                collectText();
            }
        }

        @Override
        public void visit(Node.TaglibDirective n) throws WaspException
        {
            if (prePass) {
                collectText();
            }
        }

        @Override
        public void visit(Node.AttributeDirective n) throws WaspException
        {
            if (prePass) {
                collectText();
            }
        }

        @Override
        public void visit(Node.VariableDirective n) throws WaspException
        {
            if (prePass) {
                collectText();
            }
        }

        /*
         * Don't concatenate text across body boundaries
         */
        @Override
        public void visitBody(Node n) throws WaspException
        {
            super.visitBody(n);
            collectText();
        }

        @Override
        public void visit(Node.TemplateText n) throws WaspException
        {

            if (trim && !prePass && n.isAllSpace()) {
                n.setText(emptyText);
                return;
            }

            if (textNodeCount++ == 0) {
                firstTextNode = n;
                textBuffer = new StringBuilder(n.getText());
            } else {
                // Append text to text buffer
                textBuffer.append(n.getText());
                n.setText(emptyText);
            }
        }

        /**
         * This method breaks concatenation mode. As a side effect it copies the concatenated string to the first text node
         */
        private void collectText() {

            if (textNodeCount > 1) {
                // Copy the text in buffer into the first template text node.
                firstTextNode.setText(textBuffer.toString());
            }
            textNodeCount = 0;
        }

    }

    public static void concatenate(Compiler compiler, Node.Nodes page) throws WaspException
    {

        Options options = compiler.getCompilationContext().getOptions();
        PageInfo pageInfo = compiler.getPageInfo();
        boolean trim = options.getTrimSpaces() || pageInfo.isTrimDirectiveWhitespaces();

        if (trim) {
            TextCatVisitor v = new TextCatVisitor(true, trim);
            page.visit(v);
            v.collectText();
        }
        TextCatVisitor v = new TextCatVisitor(false, trim);
        page.visit(v);

        // Cleanup, in case the page ends with a template text
        v.collectText();
    }
}
