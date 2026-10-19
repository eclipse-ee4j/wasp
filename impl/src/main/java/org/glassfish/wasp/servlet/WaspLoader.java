/*
 * Copyright (c) 2024 Contributors to Eclipse Foundation.
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

package org.glassfish.wasp.servlet;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import org.glassfish.wasp.Constants;

/**
 * Class loader for loading servlet class files (corresponding to JSP files) and tag handler class files (corresponding
 * to tag files).
 *
 * @author Anil K. Vijendran
 * @author Harish Prabandham
 * @author Jean-Francois Arcand
 * @author Kin-man Chung
 */
public class WaspLoader extends URLClassLoader {

    private ClassLoader parent;
    private Map<String, byte[]> bytecodes;

    public WaspLoader(URL[] urls, ClassLoader parent, Map<String, byte[]> bytecodes) {
        super(urls, parent);
        this.parent = parent;
        this.bytecodes = bytecodes;
    }

    /**
     * Load the class with the specified name. This method searches for classes in the same manner as
     * <code>loadClass(String, boolean)</code> with <code>false</code> as the second argument.
     *
     * @param name Name of the class to be loaded
     *
     * @exception ClassNotFoundException if the class was not found
     */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    /**
     * Load the class with the specified name, searching using the following algorithm until it finds and returns the class.
     * If the class cannot be found, returns <code>ClassNotFoundException</code>.
     * <ul>
     * <li>Call <code>findLoadedClass(String)</code> to check if the class has already been loaded. If it has, the same
     * <code>Class</code> object is returned.</li>
     * <li>If the <code>delegate</code> property is set to <code>true</code>, call the <code>loadClass()</code> method of
     * the parent class loader, if any.</li>
     * <li>Call <code>findClass()</code> to find this class in our locally defined repositories.</li>
     * <li>Call the <code>loadClass()</code> method of our parent class loader, if any.</li>
     * </ul>
     * If the class was found using the above steps, and the <code>resolve</code> flag is <code>true</code>, this method
     * will then call <code>resolveClass(Class)</code> on the resulting Class object.
     *
     * @param name Name of the class to be loaded
     * @param resolve If <code>true</code> then resolve the class
     *
     * @exception ClassNotFoundException if the class was not found
     */
    @Override
    public synchronized Class<?> loadClass(final String name, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz;

        // (0) Check our previously loaded class cache
        clazz = findLoadedClass(name);
        if (clazz != null) {
            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        }

        if (!name.startsWith(Constants.JSP_PACKAGE_NAME)) {
            // Class is not in org.apache.jsp, therefore, have our parent load it
            clazz = parent.loadClass(name);
            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        }

        return findClass(name);
    }

    @Override
    public Class<?> findClass(String className) throws ClassNotFoundException {

        // If the class file is in memory, use it
        byte[] cdata = this.bytecodes.get(className);

        String path = className.replace('.', '/') + ".class";
        if (cdata == null) {
            // read class data from file
            cdata = loadClassDataFromFile(path);
            if (cdata == null) {
                throw new ClassNotFoundException(className);
            }
        }

        // Preprocess the loaded byte code
        return defineClass(className, cdata, 0, cdata.length);
    }

    /*
     * Load JSP class data from file.
     */
    private byte[] loadClassDataFromFile(final String fileName) {
        byte[] classBytes = null;
        try {
            InputStream in = getResourceAsStream(fileName);

            if (in == null) {
                return null;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte buf[] = new byte[1024];
            for (int i = 0; (i = in.read(buf)) != -1;) {
                baos.write(buf, 0, i);
            }
            in.close();
            baos.close();
            classBytes = baos.toByteArray();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return classBytes;
    }
}
