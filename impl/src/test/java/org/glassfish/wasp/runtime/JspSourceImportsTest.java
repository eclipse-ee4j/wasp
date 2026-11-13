/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://apache.org
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  Assisted-by: Google Gemini
 */

package org.glassfish.wasp.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.el.ImportHandler;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.jsp.JspApplicationContext;
import jakarta.servlet.jsp.JspFactory;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.SimpleTag;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import org.glassfish.wasp.JspC;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JspSourceImportsTest {

    @Test
    void generatorEmitsJspSourceImportsForPagesAndTagFiles(@TempDir Path tempDir) throws Exception {
        Path webAppDir = tempDir.resolve("webapp");
        Path tagsDir = webAppDir.resolve("WEB-INF/tags");
        Path outDir = tempDir.resolve("generated");
        Files.createDirectories(tagsDir);
        Files.createDirectories(outDir);

        String jspContent = """
                <%@ page contentType="text/html;charset=UTF-8" session="false" %>
                <%@ page import="java.util.UUID" %>
                <%@ page import="java.time.*" %>
                <%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
                ${LocalDate.of(2026, 11, 13)}|<t:sample />
                """;
        Files.writeString(webAppDir.resolve("index.jsp"), jspContent, StandardCharsets.UTF_8);

        String tagContent = """
                <%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
                <%@ tag import="java.math.BigDecimal" %>
                <%@ tag import="java.nio.file.*" %>
                ${3 * BigDecimal.ONE}
                """;
        Files.writeString(tagsDir.resolve("sample.tag"), tagContent, StandardCharsets.UTF_8);

        JspC jspc = new JspC();
        jspc.setUriroot(webAppDir.toAbsolutePath().toString());
        jspc.setOutputDir(outDir.toAbsolutePath().toString());
        jspc.setCompile(true);
        jspc.execute();

        JspFactory previousFactory = JspFactory.getDefaultFactory();
        JspFactoryImpl factory = new JspFactoryImpl() {
            @Override
            public PageContext getPageContext(
                    Servlet servlet, ServletRequest request, ServletResponse response,
                    String errorPageURL, boolean needsSession, int buffer, boolean autoflush) {
                PageContextImpl pc = new PageContextImpl(this);
                try {
                    pc.initialize(servlet, request, response, errorPageURL, needsSession, buffer, autoflush);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return pc;
            }
        };
        JspFactory.setDefaultFactory(factory);

        try (URLClassLoader loader = new URLClassLoader(
                new URL[] { outDir.toUri().toURL() },
                Thread.currentThread().getContextClassLoader())) {

            Class<?> jspClass = loader.loadClass("org.apache.jsp.index_jsp");
            Object jspInstance = jspClass.getDeclaredConstructor().newInstance();
            JspSourceImports jspImports = assertInstanceOf(JspSourceImports.class, jspInstance);
            assertTrue(jspImports.getClassImports().contains("java.util.UUID"));
            assertTrue(jspImports.getPackageImports().contains("java.time"));

            Class<?> tagClass = loader.loadClass("org.apache.jsp.tag.web.sample_tag");
            Object tagInstance = tagClass.getDeclaredConstructor().newInstance();
            JspSourceImports tagImports = assertInstanceOf(JspSourceImports.class, tagInstance);
            assertTrue(tagImports.getClassImports().contains("java.math.BigDecimal"));
            assertTrue(tagImports.getPackageImports().contains("java.nio.file"));

            try {
                ServletContext servletContext = createMinimalServletContext();
                ServletConfig servletConfig = createMinimalServletConfig(servletContext);

                SimpleTag tag = (SimpleTag) tagInstance;

                StringWriter stringWriter = new StringWriter();
                PrintWriter outBuffer = new PrintWriter(stringWriter);
                PageContextImpl pageContext = new PageContextImpl(factory) {
                    @Override
                    public JspWriter getOut() {
                        return new JspWriter(1024, false) {
                            @Override public void write(char[] cbuf, int off, int len) { outBuffer.write(cbuf, off, len); }
                            @Override public void newLine() { outBuffer.write(System.lineSeparator()); }
                            @Override public void print(boolean b) { outBuffer.print(b); }
                            @Override public void print(char c) { outBuffer.print(c); }
                            @Override public void print(int i) { outBuffer.print(i); }
                            @Override public void print(long l) { outBuffer.print(l); }
                            @Override public void print(float f) { outBuffer.print(f); }
                            @Override public void print(double d) { outBuffer.print(d); }
                            @Override public void print(char[] s) { outBuffer.print(s); }
                            @Override public void print(String s) { outBuffer.print(s); }
                            @Override public void print(Object obj) { outBuffer.print(obj); }
                            @Override public void println() { newLine(); }
                            @Override public void println(boolean x) { print(x); newLine(); }
                            @Override public void println(char x) { print(x); newLine(); }
                            @Override public void println(int x) { print(x); newLine(); }
                            @Override public void println(long x) { print(x); newLine(); }
                            @Override public void println(float x) { print(x); newLine(); }
                            @Override public void println(double x) { print(x); newLine(); }
                            @Override public void println(char[] x) { print(x); newLine(); }
                            @Override public void println(String x) { print(x); newLine(); }
                            @Override public void println(Object x) { print(x); newLine(); }
                            @Override public void clear() {}
                            @Override public void clearBuffer() {}
                            @Override public void flush() {}
                            @Override public void close() {}
                            @Override public int getRemaining() { return 1024; }
                        };
                    }
                };

                TestJspServlet dummyParent = new TestJspServlet(
                        servletConfig, Collections.emptySet(), Collections.emptySet());
                pageContext.initialize(
                        dummyParent, createDummyRequest(), createDummyResponse(), null, false, 1024, true);

                tag.setJspContext(pageContext);
                tag.doTag();

                outBuffer.flush();
                assertEquals("3", stringWriter.toString().trim());

                Servlet jspServlet = (Servlet) jspInstance;
                jspServlet.init(servletConfig);

                StringWriter jspStringWriter = new StringWriter();
                PrintWriter jspOut = new PrintWriter(jspStringWriter);
                HttpServletResponse jspResponse =
                        (HttpServletResponse) Proxy.newProxyInstance(
                                HttpServletResponse.class.getClassLoader(),
                                new Class<?>[] { HttpServletResponse.class },
                                (proxy, method, args) -> switch (method.getName()) {
                                    case "getWriter" -> jspOut;
                                    case "getBufferSize" -> 1024;
                                    default -> defaultValue(method.getReturnType());
                                });
                jspServlet.service(createDummyRequest(), jspResponse);
                jspOut.flush();
                assertEquals("2026-11-13|3", jspStringWriter.toString().trim());
            } finally {
                JspFactory.setDefaultFactory(previousFactory);
            }
        }
    }

    @Test
    void pageContextAndJspContextWrapperRegisterIsolatedImports() throws Exception {
        JspFactory previousFactory = JspFactory.getDefaultFactory();
        JspFactoryImpl factory = new JspFactoryImpl();
        JspFactory.setDefaultFactory(factory);

        try {
            ServletContext servletContext = createMinimalServletContext();
            ServletConfig servletConfig = createMinimalServletConfig(servletContext);

            JspApplicationContext jspAppContext = factory.getJspApplicationContext(servletContext);
            assertNotNull(jspAppContext);

            TestJspServlet jspServlet = new TestJspServlet(
                    servletConfig,
                    Set.of("java.util.UUID"),
                    Set.of("java.time"));

            PageContextImpl pageContext = new PageContextImpl(factory);
            pageContext.initialize(
                    jspServlet,
                    createDummyRequest(),
                    createDummyResponse(),
                    null,
                    false,
                    1024,
                    true);

            ImportHandler pageImportHandler = pageContext.getELContext().getImportHandler();
            assertEquals(java.util.UUID.class, pageImportHandler.resolveClass("UUID"));
            assertEquals(java.time.Instant.class, pageImportHandler.resolveClass("Instant"));
            assertNull(pageImportHandler.resolveClass("BigDecimal"),
                    "Tag imports must not leak into parent JSP ELContext");

            TestSimpleTag tag = new TestSimpleTag(
                    Set.of("java.math.BigDecimal"),
                    Set.of("java.nio.file"));

            JspContextWrapper tagContext = new JspContextWrapper(
                    tag,
                    pageContext,
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    Collections.emptyMap());

            ImportHandler tagImportHandler = tagContext.getELContext().getImportHandler();
            assertEquals(java.math.BigDecimal.class, tagImportHandler.resolveClass("BigDecimal"));
            assertEquals(java.nio.file.Paths.class, tagImportHandler.resolveClass("Paths"));
            assertNull(tagImportHandler.resolveClass("UUID"),
                    "Parent JSP imports must not leak into tag file ELContext");
        } finally {
            JspFactory.setDefaultFactory(previousFactory);
        }
    }

    private static final class TestJspServlet implements Servlet, JspSourceImports {
        private final ServletConfig config;
        private final Set<String> classImports;
        private final Set<String> packageImports;

        TestJspServlet(ServletConfig config, Set<String> classImports, Set<String> packageImports) {
            this.config = config;
            this.classImports = classImports;
            this.packageImports = packageImports;
        }

        @Override
        public Set<String> getPackageImports() {
            return packageImports;
        }

        @Override
        public Set<String> getClassImports() {
            return classImports;
        }

        @Override
        public void init(ServletConfig config) {
        }

        @Override
        public ServletConfig getServletConfig() {
            return config;
        }

        @Override
        public void service(ServletRequest req, ServletResponse res) {
        }

        @Override
        public String getServletInfo() {
            return "";
        }

        @Override
        public void destroy() {
        }
    }

    private static final class TestSimpleTag extends SimpleTagSupport implements JspSourceImports {
        private final Set<String> classImports;
        private final Set<String> packageImports;

        TestSimpleTag(Set<String> classImports, Set<String> packageImports) {
            this.classImports = classImports;
            this.packageImports = packageImports;
        }

        @Override
        public Set<String> getPackageImports() {
            return packageImports;
        }

        @Override
        public Set<String> getClassImports() {
            return classImports;
        }
    }

    private static ServletContext createMinimalServletContext() {
        java.util.Map<String, Object> attributes = new java.util.HashMap<>();
        return (ServletContext) Proxy.newProxyInstance(
                ServletContext.class.getClassLoader(),
                new Class<?>[] { ServletContext.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getAttribute" -> attributes.get(args[0]);
                    case "setAttribute" -> {
                        attributes.put((String) args[0], args[1]);
                        yield null;
                    }
                    case "removeAttribute" -> {
                        attributes.remove(args[0]);
                        yield null;
                    }
                    case "getMajorVersion" -> 6;
                    case "getMinorVersion" -> 1;
                    case "getEffectiveMajorVersion" -> 6;
                    case "getEffectiveMinorVersion" -> 1;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static ServletConfig createMinimalServletConfig(ServletContext context) {
        return (ServletConfig) Proxy.newProxyInstance(
                ServletConfig.class.getClassLoader(),
                new Class<?>[] { ServletConfig.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getServletContext" -> context;
                    case "getServletName" -> "jsp";
                    default -> defaultValue(method.getReturnType());
                });
    }
    private static HttpServletRequest createDummyRequest() {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] { HttpServletRequest.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getMethod" -> "GET";
                    default -> defaultValue(method.getReturnType());
                });
    }
    private static HttpServletResponse createDummyResponse() {
        return (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[] { HttpServletResponse.class },
                (proxy, method, args) -> defaultValue(method.getReturnType()));
    }
    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        return null;
    }
}
