/*
 * Copyright (c) 2022, 2022 Contributors to the Eclipse Foundation.
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

import static jakarta.servlet.jsp.tagext.TagInfo.BODY_CONTENT_EMPTY;
import static jakarta.servlet.jsp.tagext.TagInfo.BODY_CONTENT_SCRIPTLESS;
import static jakarta.servlet.jsp.tagext.TagInfo.BODY_CONTENT_TAG_DEPENDENT;
import static org.glassfish.wasp.Constants.JSP_VERSION_2_1;
import static org.glassfish.wasp.compiler.JspUtil.booleanValue;
import static org.glassfish.wasp.compiler.JspUtil.checkAttributes;
import static org.glassfish.wasp.compiler.JspUtil.getTagHandlerClassName;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.glassfish.wasp.Constants;
import org.glassfish.wasp.JspCompilationContext;
import org.glassfish.wasp.WaspException;
import org.glassfish.wasp.runtime.JspSourceDependent;
import org.glassfish.wasp.servlet.JspServletWrapper;

import jakarta.servlet.jsp.tagext.TagAttributeInfo;
import jakarta.servlet.jsp.tagext.TagExtraInfo;
import jakarta.servlet.jsp.tagext.TagFileInfo;
import jakarta.servlet.jsp.tagext.TagInfo;
import jakarta.servlet.jsp.tagext.TagLibraryInfo;
import jakarta.servlet.jsp.tagext.TagVariableInfo;
import jakarta.servlet.jsp.tagext.VariableInfo;

/**
 * 1. Processes and extracts the directive info in a tag file. 2. Compiles and loads tag files used in a JSP file.
 *
 * @author Kin-man Chung
 */

class TagFileProcessor {

    private ArrayList<Compiler> tempCompilers;

    /**
     * A visitor the tag file
     */
    private static class TagFileDirectiveVisitor extends Node.Visitor {

        private static final JspUtil.ValidAttribute[] tagDirectiveAttrs = {
                new JspUtil.ValidAttribute("display-name"),
                new JspUtil.ValidAttribute("body-content"),
                new JspUtil.ValidAttribute("dynamic-attributes"),
                new JspUtil.ValidAttribute("small-icon"),
                new JspUtil.ValidAttribute("large-icon"),
                new JspUtil.ValidAttribute("description"),
                new JspUtil.ValidAttribute("example"),
                new JspUtil.ValidAttribute("pageEncoding"),
                new JspUtil.ValidAttribute("language"),
                new JspUtil.ValidAttribute("import"),
                new JspUtil.ValidAttribute("isELIgnored"),
                new JspUtil.ValidAttribute("deferredSyntaxAllowedAsLiteral"),
                new JspUtil.ValidAttribute("trimDirectiveWhitespaces"),
                new JspUtil.ValidAttribute("errorOnELNotFound") };

        private static final JspUtil.ValidAttribute[] attributeDirectiveAttrs = { new JspUtil.ValidAttribute("name", true),
                new JspUtil.ValidAttribute("required"),
                new JspUtil.ValidAttribute("fragment"),
                new JspUtil.ValidAttribute("rtexprvalue"),
                new JspUtil.ValidAttribute("type"),
                new JspUtil.ValidAttribute("description"),
                new JspUtil.ValidAttribute("deferredValue"),
                new JspUtil.ValidAttribute("deferredValueType"),
                new JspUtil.ValidAttribute("deferredMethod"),
                new JspUtil.ValidAttribute("deferredMethodSignature") };

        private static final JspUtil.ValidAttribute[] variableDirectiveAttrs = {
                new JspUtil.ValidAttribute("name-given"),
                new JspUtil.ValidAttribute("name-from-attribute"),
                new JspUtil.ValidAttribute("alias"),
                new JspUtil.ValidAttribute("variable-class"),
                new JspUtil.ValidAttribute("scope"),
                new JspUtil.ValidAttribute("declare"),
                new JspUtil.ValidAttribute("description") };

        private ErrorDispatcher err;
        private TagLibraryInfo tagLibInfo;

        private String name;
        private String path;
        private TagExtraInfo tei;
        private String bodycontent;
        private String description;
        private String displayName;
        private String smallIcon;
        private String largeIcon;
        private String dynamicAttrsMapName;
        private String example;

        private List<TagAttributeInfo> tagAttributes;
        private List<TagVariableInfo> tagVariables;

        private HashMap<String, NameEntry> nameTable = new HashMap<>();
        private HashMap<String, NameEntry> nameFromTable = new HashMap<>();

        // The tag file's Jakarta Pages version
        private Double jakartaPagesVersionDouble;

        private enum Name {
            ATTR_NAME("name", "attribute"),
            VAR_NAME_GIVEN("name-given", "variable"),
            VAR_NAME_FROM("name-from-attribute", "variable"),
            VAR_ALIAS("alias", "variable"),
            TAG_DYNAMIC("dynamic-attributes", "tag");

            private String attribute;
            private String directive;

            String getAttribute() {
                return this.attribute;
            }

            String getDirective() {
                return this.directive;
            }

            Name(String attribute, String directive) {
                this.attribute = attribute;
                this.directive = directive;
            }
        }

        public TagFileDirectiveVisitor(Compiler compiler, TagLibraryInfo tagLibInfo, String name, String path) {
            err = compiler.getErrorDispatcher();
            this.tagLibInfo = tagLibInfo;
            this.name = name;
            this.path = path;
            tagAttributes = new ArrayList<>();
            tagVariables = new ArrayList<>();

            jakartaPagesVersionDouble = Double.valueOf(tagLibInfo.getRequiredVersion());
        }

        @Override
        public void visit(Node.JspRoot root) throws WaspException {
            /*
             * If a tag file in XML syntax contains a jsp:root element, the value of its "version" attribute must match the tag
             * file's Jakarta Pages version.
             */
            String jspRootVersion = root.getTextAttribute("version");
            if (jspRootVersion == null) {
                err.jspError(root, "jsp.error.mandatory.attribute", root.getQName(), "version");
            }
            if (!jspRootVersion.equals(jakartaPagesVersionDouble.toString())) {
                err.jspError(root, "jsp.error.tagfile.jspVersionMismatch", jspRootVersion, jakartaPagesVersionDouble.toString());
            }

            visitBody(root);
        }

        @Override
        public void visit(Node.TagDirective tagDirective) throws WaspException {
            checkAttributes("Tag directive", tagDirective, tagDirectiveAttrs, err);

            bodycontent = checkConflict(tagDirective, bodycontent, "body-content");
            if (bodycontent != null && !bodycontent.equals(BODY_CONTENT_EMPTY) && !bodycontent.equals(BODY_CONTENT_TAG_DEPENDENT)
                    && !bodycontent.equals(BODY_CONTENT_SCRIPTLESS)) {
                err.jspError(tagDirective, "jsp.error.tagdirective.badbodycontent", bodycontent);
            }

            dynamicAttrsMapName = checkConflict(tagDirective, dynamicAttrsMapName, "dynamic-attributes");
            if (dynamicAttrsMapName != null) {
                checkUniqueName(dynamicAttrsMapName, Name.TAG_DYNAMIC, tagDirective);
            }

            smallIcon = checkConflict(tagDirective, smallIcon, "small-icon");
            largeIcon = checkConflict(tagDirective, largeIcon, "large-icon");
            description = checkConflict(tagDirective, description, "description");
            displayName = checkConflict(tagDirective, displayName, "display-name");
            example = checkConflict(tagDirective, example, "example");

            if (tagDirective.getAttributeValue("deferredSyntaxAllowedAsLiteral") != null && Double.compare(jakartaPagesVersionDouble, JSP_VERSION_2_1) < 0) {
                err.jspError("jsp.error.invalidTagDirectiveAttrUnless21", "deferredSyntaxAllowedAsLiteral");
            }

            // Additional tag directives are validated in Validator
        }

        private String checkConflict(Node node, String oldAttrValue, String attr) throws WaspException {
            String result = oldAttrValue;
            String attrValue = node.getAttributeValue(attr);
            if (attrValue != null) {
                if (oldAttrValue != null && !oldAttrValue.equals(attrValue)) {
                    err.jspError(node, "jsp.error.tag.conflict.attr", attr, oldAttrValue, attrValue);
                }
                result = attrValue;
            }

            return result;
        }

        @Override
        public void visit(Node.AttributeDirective attributeDirective) throws WaspException {
            checkAttributes("Attribute directive", attributeDirective, attributeDirectiveAttrs, err);

            String attrName = attributeDirective.getAttributeValue("name");
            boolean required = booleanValue(attributeDirective.getAttributeValue("required"));
            boolean rtexprvalue = true;
            String rtexprvalueString = attributeDirective.getAttributeValue("rtexprvalue");
            if (rtexprvalueString != null) {
                rtexprvalue = booleanValue(rtexprvalueString);
            }
            boolean fragment = booleanValue(attributeDirective.getAttributeValue("fragment"));
            String type = attributeDirective.getAttributeValue("type");

            String deferredValue = attributeDirective.getAttributeValue("deferredValue");
            String deferredMethod = attributeDirective.getAttributeValue("deferredMethod");
            String expectedType = attributeDirective.getAttributeValue("deferredValueType");
            String methodSignature = attributeDirective.getAttributeValue("deferredMethodSignature");
            if (Double.compare(jakartaPagesVersionDouble, Constants.JSP_VERSION_2_1) < 0) {
                if (deferredValue != null) {
                    err.jspError("jsp.error.invalidAttrDirectiveAttrUnless21", "deferredValue");
                }
                if (deferredMethod != null) {
                    err.jspError("jsp.error.invalidAttrDirectiveAttrUnless21", "deferredMethod");
                }
                if (expectedType != null) {
                    err.jspError("jsp.error.invalidAttrDirectiveAttrUnless21", "deferredValueType");
                }
                if (methodSignature != null) {
                    err.jspError("jsp.error.invalidAttrDirectiveAttrUnless21", "deferredMethodSignature");
                }
            }

            boolean isDeferredValue = booleanValue(deferredValue);
            boolean isDeferredMethod = booleanValue(deferredMethod);
            if (expectedType == null) {
                if (isDeferredValue) {
                    expectedType = "java.lang.Object";
                }
            } else {
                if (deferredValue != null && !isDeferredValue) {
                    err.jspError("jsp.error.deferredvaluewithtype");
                }
                isDeferredValue = true;
            }

            if (methodSignature == null) {
                if (isDeferredMethod) {
                    methodSignature = "void method()";
                }
            } else {
                if (deferredMethod != null && !isDeferredMethod) {
                    err.jspError("jsp.error.deferredmethodwithsignature");
                }
                isDeferredMethod = true;
            }

            if (fragment) {
                // type is fixed to "JspFragment" and a translation error
                // must occur if specified.
                if (type != null) {
                    err.jspError(attributeDirective, "jsp.error.fragmentwithtype");
                }

                // rtexprvalue is fixed to "true" and a translation error
                // must occur if specified.
                rtexprvalue = true;
                if (rtexprvalueString != null) {
                    err.jspError(attributeDirective, "jsp.error.frgmentwithrtexprvalue");
                }
            } else if (type == null) {
                if (isDeferredValue) {
                    type = "jakarta.el.ValueExpression";
                } else if (isDeferredMethod) {
                    type = "jakarta.el.MethodExpression";
                } else {
                    type = "java.lang.String";
                }
            } else if (isDeferredValue || isDeferredMethod) {
                err.jspError("jsp.error.deferredwithtype");
            }

            if (isDeferredValue || isDeferredMethod) {
                rtexprvalue = false;
            }

            TagAttributeInfo tagAttributeInfo =
                new TagAttributeInfo(
                        attrName, required, type,
                        rtexprvalue, fragment, description, isDeferredValue,
                        isDeferredMethod, expectedType, methodSignature);
            tagAttributes.add(tagAttributeInfo);

            checkUniqueName(attrName, Name.ATTR_NAME, attributeDirective, tagAttributeInfo);
        }

        @Override
        public void visit(Node.VariableDirective variableDirective) throws WaspException {
            checkAttributes("Variable directive", variableDirective, variableDirectiveAttrs, err);

            String nameGiven = variableDirective.getAttributeValue("name-given");
            String nameFromAttribute = variableDirective.getAttributeValue("name-from-attribute");
            if (nameGiven == null && nameFromAttribute == null) {
                err.jspError("jsp.error.variable.either.name");
            }

            if (nameGiven != null && nameFromAttribute != null) {
                err.jspError("jsp.error.variable.both.name");
            }

            String alias = variableDirective.getAttributeValue("alias");
            if (nameFromAttribute != null && alias == null || nameFromAttribute == null && alias != null) {
                err.jspError("jsp.error.variable.alias");
            }

            String className = variableDirective.getAttributeValue("variable-class");
            if (className == null) {
                className = "java.lang.String";
            }

            String declareStr = variableDirective.getAttributeValue("declare");
            boolean declare = true;
            if (declareStr != null) {
                declare = booleanValue(declareStr);
            }

            int scope = VariableInfo.NESTED;
            String scopeStr = variableDirective.getAttributeValue("scope");
            if (scopeStr != null) {
                if ("NESTED".equals(scopeStr)) {
                    // Already the default
                } else if ("AT_BEGIN".equals(scopeStr)) {
                    scope = VariableInfo.AT_BEGIN;
                } else if ("AT_END".equals(scopeStr)) {
                    scope = VariableInfo.AT_END;
                }
            }

            if (nameFromAttribute != null) {
                /*
                 * An alias has been specified. We use 'nameGiven' to hold the value of the alias, and 'nameFromAttribute' to hold the
                 * name of the attribute whose value (at invocation-time) denotes the name of the variable that is being aliased
                 */
                nameGiven = alias;
                checkUniqueName(nameFromAttribute, Name.VAR_NAME_FROM, variableDirective);
                checkUniqueName(alias, Name.VAR_ALIAS, variableDirective);
            } else {
                // name-given specified
                checkUniqueName(nameGiven, Name.VAR_NAME_GIVEN, variableDirective);
            }

            tagVariables.add(new TagVariableInfo(nameGiven, nameFromAttribute, className, declare, scope));
        }

        public TagInfo getTagInfo() throws WaspException {
            if (name == null) {
                // XXX Get it from tag file name
            }

            if (bodycontent == null) {
                bodycontent = BODY_CONTENT_SCRIPTLESS;
            }

            String tagClassName = getTagHandlerClassName(path, err);

            TagVariableInfo[] tagVariableInfos = tagVariables.toArray(new TagVariableInfo[0]);
            TagAttributeInfo[] tagAttributeInfo = tagAttributes.toArray(new TagAttributeInfo[0]);

            return new WaspTagInfo(
                    name, tagClassName,
                    bodycontent, description, tagLibInfo, tei, tagAttributeInfo, displayName, smallIcon, largeIcon,
                    tagVariableInfos, dynamicAttrsMapName);
        }

        static class NameEntry {
            private Name type;
            private Node node;
            private TagAttributeInfo attr;

            NameEntry(Name type, Node node, TagAttributeInfo attr) {
                this.type = type;
                this.node = node;
                this.attr = attr;
            }

            Name getType() {
                return type;
            }

            Node getNode() {
                return node;
            }

            TagAttributeInfo getTagAttributeInfo() {
                return attr;
            }
        }

        /**
         * Reports a translation error if names specified in attributes of directives are not unique in this translation unit.
         *
         * The value of the following attributes must be unique. 1. 'name' attribute of an attribute directive 2. 'name-given'
         * attribute of a variable directive 3. 'alias' attribute of variable directive 4. 'dynamic-attributes' of a tag
         * directive except that 'dynamic-attributes' can (and must) have the same value when it appears in multiple tag
         * directives.
         *
         * Also, 'name-from' attribute of a variable directive cannot have the same value as that from another variable
         * directive.
         */
        private void checkUniqueName(String name, Name type, Node node) throws WaspException {
            checkUniqueName(name, type, node, null);
        }

        private void checkUniqueName(String name, Name type, Node node, TagAttributeInfo attr) throws WaspException {
            HashMap<String, NameEntry> table = type == Name.VAR_NAME_FROM ? nameFromTable : nameTable;
            NameEntry nameEntry = table.get(name);
            if (nameEntry != null) {
                if (type != Name.TAG_DYNAMIC || nameEntry.getType() != Name.TAG_DYNAMIC) {
                    int line = nameEntry.getNode().getStart().getLineNumber();
                    err.jspError(node, "jsp.error.tagfile.nameNotUnique", type.getAttribute(), type.getDirective(), nameEntry.getType().getAttribute(),
                            nameEntry.getType().getDirective(), Integer.toString(line));
                }
            } else {
                table.put(name, new NameEntry(type, node, attr));
            }
        }

        /**
         * Perform miscelleaneous checks after the nodes are visited.
         */
        void postCheck() throws WaspException {
            // Check that var.name-from-attributes has valid values.
            Iterator<String> iter = nameFromTable.keySet().iterator();
            while (iter.hasNext()) {
                String nameFrom = iter.next();
                NameEntry nameEntry = nameTable.get(nameFrom);
                NameEntry nameFromEntry = nameFromTable.get(nameFrom);
                Node nameFromNode = nameFromEntry.getNode();
                if (nameEntry == null) {
                    err.jspError(nameFromNode, "jsp.error.tagfile.nameFrom.noAttribute", nameFrom);
                } else {
                    Node node = nameEntry.getNode();
                    TagAttributeInfo tagAttr = nameEntry.getTagAttributeInfo();
                    if (!"java.lang.String".equals(tagAttr.getTypeName()) || !tagAttr.isRequired() || tagAttr.canBeRequestTime()) {
                        err.jspError(nameFromNode, "jsp.error.tagfile.nameFrom.badAttribute", nameFrom, Integer.toString(node.getStart().getLineNumber()));
                    }
                }
            }
        }
    }

    /**
     * Parses the tag file, and collects information on the directives included in it. The method is used to obtain the info
     * on the tag file, when the handler that it represents is referenced. The tag file is not compiled here.
     *
     * @param parserController the current ParserController used in this compilation
     * @param name the tag name as specified in the TLD
     * @param tagfile the path for the tagfile
     * @param tagLibInfo the TagLibraryInfo object associated with this TagInfo
     * @return a TagInfo object assembled from the directives in the tag file.
     */
    public static TagInfo parseTagFileDirectives(ParserController parserController, String name, String path, TagLibraryInfo tagLibInfo) throws WaspException {
        ErrorDispatcher err = parserController.getCompiler().getErrorDispatcher();

        Node.Nodes page = null;
        try {
            page = parserController.parseTagFileDirectives(path);
        } catch (FileNotFoundException e) {
            err.jspError("jsp.error.file.not.found", path);
        } catch (IOException e) {
            err.jspError("jsp.error.file.not.found", path);
        }

        TagFileDirectiveVisitor tagFileVisitor = new TagFileDirectiveVisitor(parserController.getCompiler(), tagLibInfo, name, path);
        page.visit(tagFileVisitor);
        tagFileVisitor.postCheck();

        return tagFileVisitor.getTagInfo();
    }

    /**
     * Compiles and loads a tagfile.
     */
    private Class<?> loadTagFile(Compiler compiler, String tagFilePath, TagInfo tagInfo, PageInfo parentPageInfo) throws WaspException {
        JspCompilationContext compilationContext = compiler.getCompilationContext();
        JspRuntimeContext runtimeContext = compilationContext.getRuntimeContext();

        synchronized (runtimeContext) {
            JspServletWrapper wrapper = runtimeContext.getWrapper(tagFilePath);
            if (wrapper == null) {
                wrapper = new JspServletWrapper(
                            compilationContext.getServletContext(),
                            compilationContext.getOptions(), tagFilePath, tagInfo,
                            compilationContext.getRuntimeContext(),
                            compilationContext.getTagFileJarUrls().get(tagFilePath));

                runtimeContext.addWrapper(tagFilePath, wrapper);

                // Use same classloader and classpath for compiling tag files
                wrapper.getJspEngineContext().setClassLoader(compilationContext.getClassLoader());
                wrapper.getJspEngineContext().setClassPath(compilationContext.getClassPath());
            } else {
                // Make sure that JspCompilationContext gets the latest TagInfo
                // for the tag file. TagInfo instance was created the last
                // time the tag file was scanned for directives, and the tag
                // file may have been modified since then.
                wrapper.getJspEngineContext().setTagInfo(tagInfo);
            }

            Class<?> tagClazz;
            int tripCount = wrapper.incTripCount();
            try {
                if (tripCount > 0) {

                    // When tripCount is greater than zero, a circular
                    // dependency exists. The circularily dependant tag
                    // file is compiled in prototype mode, to avoid infinite
                    // recursion.

                    JspServletWrapper tempWrapper = new JspServletWrapper(
                            compilationContext.getServletContext(),
                            compilationContext.getOptions(), tagFilePath, tagInfo,
                            compilationContext.getRuntimeContext(),
                            compilationContext.getTagFileJarUrls().get(tagFilePath));
                    tagClazz = tempWrapper.loadTagFilePrototype();
                    tempCompilers.add(tempWrapper.getJspEngineContext().getCompiler());
                } else {
                    tagClazz = wrapper.loadTagFile();
                }
            } finally {
                wrapper.decTripCount();
            }

            // Add the dependants for this tag file to its parent's
            // dependant list. The only reliable dependency information
            // can only be obtained from the tag instance.
            try {
                Object tagIns = tagClazz.getDeclaredConstructor().newInstance();
                if (tagIns instanceof JspSourceDependent) {
                    for (String dependant : ((JspSourceDependent) tagIns).getDependants()) {
                        parentPageInfo.addDependant(dependant);
                    }
                }
            } catch (Exception e) {
                // ignore errors
            }

            return tagClazz;
        }
    }

    /*
     * Visitor which scans the page and looks for tag handlers that are tag files, compiling (if necessary) and loading
     * them.
     */
    private class TagFileLoaderVisitor extends Node.Visitor {

        private Compiler compiler;
        private PageInfo pageInfo;

        TagFileLoaderVisitor(Compiler compiler) {
            this.compiler = compiler;
            this.pageInfo = compiler.getPageInfo();
        }

        @Override
        public void visit(Node.CustomTag customTag) throws WaspException {
            TagFileInfo tagFileInfo = customTag.getTagFileInfo();
            if (tagFileInfo != null) {
                String tagFilePath = tagFileInfo.getPath();
                if (tagFilePath.startsWith("/META-INF/")) {
                    // For tags in JARs, add the TLD and the tag as a dependency
                    String[] location = compiler.getCompilationContext().getTldLocation(tagFileInfo.getTagInfo().getTagLibrary().getURI());

                    // Add TLD
                    pageInfo.addDependant("jar:" + location[0] + "!/" + location[1]);

                    // Add Tag
                    pageInfo.addDependant("jar:" + location[0] + "!" + tagFilePath);
                } else {
                    pageInfo.addDependant(tagFilePath);
                }
                customTag.setTagHandlerClass(loadTagFile(compiler, tagFilePath, customTag.getTagInfo(), pageInfo));
            }
            visitBody(customTag);
        }
    }

    /**
     * Implements a phase of the translation that compiles (if necessary) the tag files used in a JSP files. The directives
     * in the tag files are assumed to have been proccessed and encapsulated as TagFileInfo in the CustomTag nodes.
     */
    public void loadTagFiles(Compiler compiler, Node.Nodes page) throws WaspException {
        tempCompilers = new ArrayList<>();
        page.visit(new TagFileLoaderVisitor(compiler));
    }

    /**
     * Removed the java and class files for the tag prototype generated from the current compilation.
     *
     * @param classFileName If non-null, remove only the class file with with this name.
     */
    public void removeProtoTypeFiles(String classFileName) {
        Iterator<Compiler> iter = tempCompilers.iterator();
        while (iter.hasNext()) {
            Compiler compiler = iter.next();
            if (classFileName == null) {
                compiler.removeGeneratedClassFiles();
            } else if (classFileName.equals(compiler.getCompilationContext().getClassFileName())) {
                compiler.removeGeneratedClassFiles();
                tempCompilers.remove(compiler);
                return;
            }
        }
    }
}
