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

import java.io.File;
import java.io.Writer;
import java.util.List;

import org.eclipse.wasp.WaspException;
import org.eclipse.wasp.JspCompilationContext;

interface JavaCompiler {

    /**
     * Start Java compilation
     *
     * @param className Name of the class under compilation
     * @param pageNode Internal form for the page, used for error line mapping
     */
    JavacErrorDetail[] compile(String className, Node.Nodes pageNodes) throws WaspException;

    /**
     * Get a Writer for the Java file. The writer is used by JSP compiler. This method allows the Java compiler control
     * where the Java file should be generated so it knows how to handle the input for java compilation accordingly.
     */
    Writer getJavaWriter(String javaFileName, String javaEncoding) throws WaspException;

    /**
     * Remove/save the generated Java File from/to disk
     */
    void doJavaFile(boolean keep) throws WaspException;

    /**
     * Return the time the class file was generated.
     */
    long getClassLastModified();

    /**
     * Save the generated class file to disk, if not already done.
     */
    void saveClassFile(String className, String classFileName);

    /**
     * Java Compiler options.
     */
    void setClassPath(List<File> cp);

    void setDebug(boolean debug);

    void setExtdirs(String exts);

    void setTargetVM(String targetVM);

    void setSourceVM(String sourceVM);

    /**
     * Initializations
     */
    void init(JspCompilationContext ctxt, ErrorDispatcher err, boolean suppressLogging);

    /**
     * Release resouces used in the current compilation
     */
    void release();
}
