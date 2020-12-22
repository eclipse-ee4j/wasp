/*
 * Copyright (c) 1997, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.apache.jasper.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.List;

import org.apache.jasper.JasperException;
import org.apache.jasper.JspCompilationContext;

/**
 * Java compiler for invoking JSP to java translation only. This only implements the part of JavaCompiler that handles
 * the writing of the generated Java file.
 *
 * @author Kin-man Chung
 */

public class NullJavaCompiler implements JavaCompiler {

    private ErrorDispatcher errDispatcher;
    private String javaFileName;

    @Override
    public void init(JspCompilationContext ctxt, ErrorDispatcher errDispatcher, boolean suppressLogging) {

        this.errDispatcher = errDispatcher;
    }

    @Override
    public void release() {
    }

    @Override
    public void setExtdirs(String exts) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTargetVM(String targetVM) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSourceVM(String sourceVM) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setClassPath(List<File> cpath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveClassFile(String className, String classFileName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDebug(boolean debug) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getClassLastModified() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Writer getJavaWriter(String javaFileName, String javaEncoding) throws JasperException {

        this.javaFileName = javaFileName;
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(javaFileName), javaEncoding);
        } catch (UnsupportedEncodingException ex) {
            errDispatcher.jspError("jsp.error.needAlternateJavaEncoding", javaEncoding);
        } catch (IOException ex) {
            errDispatcher.jspError("jsp.error.unableToCreateOutputWriter", javaFileName, ex);
        }
        return writer;
    }

    @Override
    public JavacErrorDetail[] compile(String className, Node.Nodes pageNodes) throws JasperException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void doJavaFile(boolean keep) {
        if (!keep) {
            File javaFile = new File(javaFileName);
            javaFile.delete();
        }
    }
}
