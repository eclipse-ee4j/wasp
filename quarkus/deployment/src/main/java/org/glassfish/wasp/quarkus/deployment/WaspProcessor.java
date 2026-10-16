/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
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
package org.glassfish.wasp.quarkus.deployment;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.NativeImageEnableAllCharsetsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.jar.FastJarFormat;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.undertow.deployment.IgnoredServletContainerInitializerBuildItem;
import io.quarkus.undertow.deployment.ListenerBuildItem;
import io.quarkus.undertow.deployment.ServletExtensionBuildItem;
import io.quarkus.undertow.deployment.WebMetadataBuildItem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import javax.tools.ToolProvider;

import org.glassfish.wasp.JspC;
import org.glassfish.wasp.JspCompilationResult;
import org.glassfish.wasp.WaspException;
import org.glassfish.wasp.quarkus.runtime.JspFileServletDefinition;
import org.glassfish.wasp.quarkus.runtime.TldLocation;
import org.glassfish.wasp.quarkus.runtime.TranslationFailedPage;
import org.glassfish.wasp.quarkus.runtime.WaspConfig;
import org.glassfish.wasp.quarkus.runtime.WaspServletExtension;
import org.glassfish.wasp.runtime.TldScanner;
import org.glassfish.wasp.xmlparser.ParserUtils;
import org.glassfish.wasp.xmlparser.TreeNode;
import org.jboss.logging.Logger;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleRefMetaData;
import org.jboss.metadata.web.spec.JspPropertyGroupMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.metadata.web.spec.WebMetaData;

import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.walk;
import static java.util.Comparator.reverseOrder;

class WaspProcessor {

    private static final Logger LOG = Logger.getLogger(WaspProcessor.class);

    private static final String FEATURE = "wasp";

    private static final String EXPRESSION_FACTORY = "jakarta.el.ExpressionFactory";

    /** The Expression Language implementation the Jakarta Expression Language API loads when it finds no other */
    private static final String DEFAULT_EXPRESSION_FACTORY = "com.sun.el.ExpressionFactoryImpl";

    /** The release level when the application's own classes don't tell */
    private static final int DEFAULT_RELEASE = 17;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * The Servlet extension, with what it needs from web.xml: the URL patterns of the pages, and the Servlets declared
     * with a jsp-file. Undertow deploys during static init, so these are passed in at build time.
     *
     * <p>
     * Also the pages to validate again at runtime, and the tag library descriptors they use.
     */
    @BuildStep
    ServletExtensionBuildItem servletExtension(Optional<WebMetadataBuildItem> webMetadata,
            Optional<CompiledPagesBuildItem> compiledPages, WaspConfig waspConfig, NativeConfig nativeConfig) {
        WaspServletExtension servletExtension = new WaspServletExtension();

        Set<String> pagesToValidate = getPagesToValidate(compiledPages, waspConfig, nativeConfig);
        if (!pagesToValidate.isEmpty()) {
            List<TldLocation> tldLocations = new ArrayList<>(compiledPages.get().getTldLocations());
            if (nativeConfig.enabled()) {
                // A native executable has no jars to read descriptors from
                tldLocations.removeIf(tldLocation -> tldLocation.getEntryName() != null);
            }
            servletExtension.setPagesToValidate(pagesToValidate);
            servletExtension.setTldLocations(tldLocations);
            servletExtension.setWebInfFiles(new LinkedHashSet<>(compiledPages.get().getWebInfFiles()));
        }

        WebMetaData webMetaData = webMetadata.map(WebMetadataBuildItem::getWebMetaData).orElse(null);
        if (webMetaData != null) {
            if (webMetaData.getJspConfig() != null && webMetaData.getJspConfig().getPropertyGroups() != null) {
                for (JspPropertyGroupMetaData propertyGroup : webMetaData.getJspConfig().getPropertyGroups()) {
                    servletExtension.getJspUrlPatterns().addAll(propertyGroup.getUrlPatterns());
                }
            }
            servletExtension.setJspFileServlets(getJspFileServlets(webMetaData));
        }

        return new ServletExtensionBuildItem(servletExtension);
    }

    /**
     * Registers the application's classes for reflection, for a native executable. Pages reach them in ways a native
     * image can't see: the Jakarta Expression Language resolves bean properties and methods by reflection, and the tag
     * handlers, tag extra infos, validators, listeners and functions of tag libraries are named in their descriptors.
     */
    @BuildStep
    ReflectiveClassBuildItem registerApplicationClasses(ApplicationArchivesBuildItem applicationArchives) {
        // Only the application's own classes: other indexed archives include libraries of Quarkus itself
        Set<String> classes = new LinkedHashSet<>();
        applicationArchives.getRootArchive().getIndex().getKnownClasses()
                .forEach(classInfo -> classes.add(classInfo.name().toString()));

        return ReflectiveClassBuildItem.builder(classes)
                .constructors()
                .methods()
                .fields()
                .reason("Jakarta Pages: Expression Language and tag libraries use application classes by reflection")
                .build();
    }

    /**
     * The Expression Language implementation, for a native executable: {@code ExpressionFactory.newInstance()} finds it
     * as a service provider or, failing that, loads Expressly's factory by name.
     */
    @BuildStep
    void registerExpressionFactory(BuildProducer<ServiceProviderBuildItem> serviceProviders,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        serviceProviders.produce(ServiceProviderBuildItem.allProvidersFromClassPath(EXPRESSION_FACTORY));
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(DEFAULT_EXPRESSION_FACTORY)
                .constructors()
                .reason("Jakarta Pages: the default Expression Language implementation is loaded by name")
                .build());
    }

    /**
     * The classes of the objects the Expression Language reaches through the implicit objects, e.g.
     * {@code ${pageContext.request.contextPath}}: the Expression Language resolves their properties by reflection. For
     * a native executable.
     */
    @BuildStep
    ReflectiveClassBuildItem registerImplicitObjects() {
        return ReflectiveClassBuildItem.builder(
                "org.glassfish.wasp.runtime.PageContextImpl",
                "jakarta.servlet.jsp.PageContext",
                "jakarta.servlet.jsp.JspContext",
                "jakarta.servlet.ServletRequest",
                "jakarta.servlet.http.HttpServletRequest",
                "jakarta.servlet.ServletResponse",
                "jakarta.servlet.http.HttpServletResponse",
                "jakarta.servlet.http.HttpSession",
                "jakarta.servlet.ServletContext",
                "jakarta.servlet.ServletConfig",
                "jakarta.servlet.http.HttpServletMapping",
                "jakarta.servlet.http.Cookie",
                "io.undertow.servlet.spec.HttpServletRequestImpl",
                "io.undertow.servlet.spec.HttpServletResponseImpl",
                "io.undertow.servlet.spec.HttpSessionImpl",
                "io.undertow.servlet.spec.ServletContextImpl",
                "io.undertow.servlet.spec.ServletConfigImpl",
                "io.undertow.servlet.spec.MappingImpl")
                .methods()
                .reason("Jakarta Pages: the Expression Language resolves properties of the implicit objects by reflection")
                .build();
    }

    /**
     * Common JDK types, for a native executable: the Expression Language calls their methods and reads their properties
     * by reflection, e.g. {@code ${list.size()}} or {@code ${map.isEmpty()}}, and {@code <jsp:useBean beanName="...">}
     * instantiates or deserializes beans by name. With their supertypes, whose methods {@code getMethods()} includes.
     */
    @BuildStep
    void registerCommonJdkTypes(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        Set<String> types = new LinkedHashSet<>();
        for (Class<?> type : COMMON_JDK_TYPES) {
            addWithSupertypes(type, types);
        }

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(types)
                .constructors()
                .methods()
                .reason("Jakarta Pages: the Expression Language and jsp:useBean use common JDK types by reflection")
                .build());
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(java.util.Date.class)
                .serialization()
                .reason("Jakarta Pages: jsp:useBean can deserialize a bean")
                .build());
    }

    private static void addWithSupertypes(Class<?> type, Set<String> types) {
        if (type == null || !types.add(type.getName())) {
            return;
        }
        addWithSupertypes(type.getSuperclass(), types);
        for (Class<?> superInterface : type.getInterfaces()) {
            addWithSupertypes(superInterface, types);
        }
    }

    /**
     * The resource bundles of the Expression Language and of WaSP, for their messages, and all charsets, since a page
     * can declare any encoding. For a native executable.
     */
    @BuildStep
    void registerBundlesAndCharsets(BuildProducer<NativeImageResourceBundleBuildItem> resourceBundles,
            BuildProducer<NativeImageEnableAllCharsetsBuildItem> allCharsets) {
        for (String bundle : List.of("jakarta.el.PrivateMessages", "org.glassfish.expressly.Messages",
                "org.glassfish.wasp.resources.messages")) {
            resourceBundles.produce(new NativeImageResourceBundleBuildItem(bundle));
        }
        allCharsets.produce(new NativeImageEnableAllCharsetsBuildItem());
    }

    /**
     * Includes the application's resources in a native executable, e.g. resource bundles and serialized beans that
     * pages use. Not the classes, nor the pages and WEB-INF files, which were compiled at build time.
     */
    @BuildStep
    NativeImageResourceBuildItem registerApplicationResources(ApplicationArchivesBuildItem applicationArchives) throws IOException {
        List<String> resources = new ArrayList<>();
        for (Path root : applicationArchives.getRootArchive().getResolvedPaths()) {
            if (!isDirectory(root)) {
                continue;
            }
            try (Stream<Path> paths = walk(root)) {
                for (Path file : paths.filter(Files::isRegularFile).toList()) {
                    String name = root.relativize(file).toString().replace(File.separatorChar, '/');
                    if (!name.endsWith(".class") && !name.startsWith("META-INF/") && !name.startsWith("WEB-INF/")) {
                        resources.add(name);
                    }
                }
            }
        }

        return new NativeImageResourceBuildItem(resources);
    }

    /**
     * @return the pages to validate again at runtime: those using tag extra infos or tag library validators, if enabled.
     *         A native executable has no jars, so there pages whose tag libraries or tag files are in a jar are not
     *         validated again.
     */
    private static Set<String> getPagesToValidate(Optional<CompiledPagesBuildItem> compiledPages, WaspConfig waspConfig,
            NativeConfig nativeConfig) {
        if (compiledPages.isEmpty() || !waspConfig.validateAtRuntime()) {
            return new LinkedHashSet<>();
        }

        Set<String> pagesToValidate = new LinkedHashSet<>(compiledPages.get().getPagesUsingTranslationValidators());
        if (nativeConfig.enabled()) {
            Set<String> skipped = new LinkedHashSet<>(pagesToValidate);
            skipped.retainAll(compiledPages.get().getPagesUsingJarResources());
            if (!skipped.isEmpty()) {
                LOG.infof("Pages %s use tag libraries or tag files in a jar; in a native executable, they are not validated"
                        + " again at runtime", skipped);
                pagesToValidate.removeAll(skipped);
            }
        }

        return pagesToValidate;
    }

    /**
     * The pages and the files in WEB-INF, for a native executable that validates pages again at runtime: WaSP parses
     * them again, with the tag library descriptors and tag files they use.
     */
    @BuildStep
    void registerPagesForValidation(Optional<CompiledPagesBuildItem> compiledPages, WaspConfig waspConfig,
            NativeConfig nativeConfig, ApplicationArchivesBuildItem applicationArchives,
            BuildProducer<NativeImageResourceBuildItem> resources) throws IOException {
        if (!nativeConfig.enabled() || getPagesToValidate(compiledPages, waspConfig, nativeConfig).isEmpty()) {
            return;
        }

        List<String> files = new ArrayList<>();
        for (Path root : applicationArchives.getRootArchive().getResolvedPaths()) {
            for (String directory : List.of("META-INF/resources", "WEB-INF")) {
                Path start = root.resolve(directory);
                if (!isDirectory(start)) {
                    continue;
                }
                try (Stream<Path> paths = walk(start)) {
                    for (Path file : paths.filter(Files::isRegularFile).toList()) {
                        files.add(root.relativize(file).toString().replace(File.separatorChar, '/'));
                    }
                }
            }
        }

        resources.produce(new NativeImageResourceBuildItem(files));
    }

    /**
     * WaSP's ServletContainerInitializer scans for tag library descriptors at runtime, to find the listeners they
     * declare. That's done at build time by {@link #compilePages}.
     */
    @BuildStep
    IgnoredServletContainerInitializerBuildItem ignoreTldScanner() {
        return new IgnoredServletContainerInitializerBuildItem(TldScanner.class.getName());
    }

    /**
     * Compiles the pages with WaSP's JspC and adds the page classes to the application. A page that can't be
     * translated gets a class with the same name that fails with the translation error, like a page WaSP failed to
     * compile at runtime. A failure of the compilation itself fails the build.
     *
     * <p>
     * Also registers the listeners declared in tag library descriptors, and the page classes for reflection: WaSP loads
     * a page's class by its name, which in a native executable needs the class to be registered.
     */
    @BuildStep
    void compilePages(ApplicationArchivesBuildItem applicationArchives, CurateOutcomeBuildItem curateOutcome,
            OutputTargetBuildItem outputTarget, Optional<WebMetadataBuildItem> webMetadata,
            BuildProducer<GeneratedClassBuildItem> generatedClasses, BuildProducer<ListenerBuildItem> listeners,
            BuildProducer<CompiledPagesBuildItem> compiledPages, BuildProducer<ReflectiveClassBuildItem> reflectiveClasses)
            throws Exception {

        Path workDirectory = outputTarget.getOutputDirectory().resolve("wasp");
        deleteRecursively(workDirectory);

        Path webRoot = workDirectory.resolve("webapp");
        Path outputDirectory = workDirectory.resolve("classes");

        List<Path> applicationRoots = new ArrayList<>();
        for (Path root : applicationArchives.getRootArchive().getResolvedPaths()) {
            applicationRoots.add(root);
        }
        List<Path> classPath = getClassPath(applicationRoots, curateOutcome);

        stageWebRoot(applicationRoots, webRoot);

        TldClasses tldClasses = findTldClasses(webRoot, classPath);
        for (String listener : tldClasses.listeners()) {
            listeners.produce(new ListenerBuildItem(listener));
        }
        if (!tldClasses.classes().isEmpty()) {
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(tldClasses.classes())
                    .constructors()
                    .methods()
                    .reason("Jakarta Pages: tag libraries name their tag handlers, tag extra infos, validators, listeners"
                            + " and functions, also in libraries")
                    .build());
        }

        List<String> jspFiles = new ArrayList<>();
        webMetadata.map(WebMetadataBuildItem::getWebMetaData)
                .ifPresent(webMetaData -> getJspFileServlets(webMetaData).forEach(servlet -> jspFiles.add(servlet.getJspFile())));

        if (!hasPages(webRoot) && jspFiles.isEmpty()) {
            return;
        }

        // Without a compiler every page would "fail"; that's a failure of the build, not of the pages
        if (ToolProvider.getSystemJavaCompiler() == null) {
            throw new IllegalStateException("Jakarta Pages are compiled when the application is built, which needs a JDK"
                    + " with a Java compiler (javax.tools); the build runs on " + System.getProperty("java.home"));
        }

        String release = Integer.toString(getReleaseLevel(applicationRoots));

        JspC jspC = new JspC();
        jspC.setUriroot(webRoot.toString());
        jspC.setOutputDir(outputDirectory.toString());
        jspC.setClassPath(String.join(File.pathSeparator, classPath.stream().map(Path::toString).toList()));
        jspC.setCompile(true);
        jspC.setFailOnError(false);
        jspC.setCompilerSourceVM(release);
        jspC.setCompilerTargetVM(release);
        jspFiles.forEach(jspC::addAdditionalPage);

        try {
            jspC.execute();
        } catch (WaspException e) {
            throw new IllegalStateException("Jakarta Pages could not be compiled", e);
        }

        Set<String> pageClasses = new LinkedHashSet<>();
        try (Stream<Path> paths = walk(outputDirectory)) {
            for (Path classFile : paths.filter(path -> path.toString().endsWith(".class")).toList()) {
                String name = outputDirectory.relativize(classFile).toString()
                        .replace(File.separatorChar, '/')
                        .replaceFirst("\\.class$", "");
                generatedClasses.produce(new GeneratedClassBuildItem(true, name, Files.readAllBytes(classFile)));
                pageClasses.add(name.replace('/', '.'));
            }
        }

        for (JspCompilationResult result : jspC.getCompilationResults().values()) {
            if (result.isSuccess()) {
                continue;
            }
            if (result.className() == null) {
                throw new IllegalStateException("Page " + result.jspUri() + " failed before its class name was known", result.error());
            }

            LOG.warnf("Page %s could not be translated; requests for it fail with: %s", result.jspUri(), result.error().getMessage());
            generateTranslationFailedPage(result, generatedClasses);
            pageClasses.add(result.className());
        }

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(pageClasses)
                .constructors()
                .methods()
                .reason("Jakarta Pages: WaSP loads page and tag file classes by name")
                .build());

        Set<String> pagesUsingTranslationValidators = new LinkedHashSet<>();
        Set<String> pagesUsingJarResources = new LinkedHashSet<>();
        for (JspCompilationResult result : jspC.getCompilationResults().values()) {
            String jspUri = result.jspUri().startsWith("/") ? result.jspUri() : "/" + result.jspUri();
            if (result.isSuccess() && result.usesTranslationValidators()) {
                pagesUsingTranslationValidators.add(jspUri);
            }
            if (result.isSuccess() && result.usesJarResources()) {
                pagesUsingJarResources.add(jspUri);
            }
        }

        compiledPages.produce(new CompiledPagesBuildItem(pagesUsingTranslationValidators, pagesUsingJarResources,
                getTldLocations(jspC.getTldScanner().getLocations(), curateOutcome), getWebInfFiles(applicationRoots)));
    }

    /**
     * @return the files in WEB-INF of the application roots, e.g. WEB-INF/tags/a.tag, which the web resources (in
     *         META-INF/resources) don't include
     */
    private static Set<String> getWebInfFiles(List<Path> applicationRoots) throws IOException {
        Set<String> files = new LinkedHashSet<>();
        for (Path root : applicationRoots) {
            Path webInf = root.resolve("WEB-INF");
            if (!isDirectory(webInf)) {
                continue;
            }
            try (Stream<Path> paths = walk(webInf)) {
                for (Path file : paths.filter(Files::isRegularFile).toList()) {
                    files.add(root.relativize(file).toString().replace(File.separatorChar, '/'));
                }
            }
        }

        return files;
    }

    /**
     * @return the locations of the tag library descriptors as JspC resolved them, with the names the jars they are in
     *         get in the packaged application
     */
    private static List<TldLocation> getTldLocations(Map<String, String[]> locations, CurateOutcomeBuildItem curateOutcome) {
        List<TldLocation> tldLocations = new ArrayList<>();
        for (Map.Entry<String, String[]> location : locations.entrySet()) {
            TldLocation tldLocation = new TldLocation();
            tldLocation.setUri(location.getKey());
            if (location.getValue()[1] == null) {
                tldLocation.setPath(location.getValue()[0]);
            } else {
                tldLocation.setEntryName(location.getValue()[1]);
                tldLocation.setJarFileName(getPackagedJarFileName(location.getValue()[0], curateOutcome));
            }
            tldLocations.add(tldLocation);
        }

        return tldLocations;
    }

    /**
     * @return the name the jar gets in the packaged application, or null if it's not the jar of a dependency
     */
    private static String getPackagedJarFileName(String jarUrl, CurateOutcomeBuildItem curateOutcome) {
        Path jar;
        try {
            jar = Path.of(URI.create(jarUrl));
        } catch (RuntimeException e) {
            return null;
        }

        // The jar itself, or else a copy of it (e.g. in WEB-INF/lib) with the same name
        ResolvedDependency sameName = null;
        int sameNameCount = 0;
        for (ResolvedDependency dependency : curateOutcome.getApplicationModel().getRuntimeDependencies()) {
            for (Path path : dependency.getResolvedPaths()) {
                if (path.equals(jar)) {
                    return FastJarFormat.getJarFileName(dependency, path);
                }
                if (path.getFileName() != null && path.getFileName().equals(jar.getFileName())) {
                    sameName = dependency;
                    sameNameCount++;
                }
            }
        }

        if (sameNameCount == 1) {
            return FastJarFormat.getJarFileName(sameName, jar);
        }

        return null;
    }

    /**
     * Generates a class with the name of the page class that extends {@link TranslationFailedPage} with the error.
     */
    private static void generateTranslationFailedPage(JspCompilationResult result, BuildProducer<GeneratedClassBuildItem> generatedClasses) {
        try (ClassCreator classCreator = ClassCreator.builder()
                .classOutput(new GeneratedClassGizmoAdaptor(generatedClasses, true))
                .className(result.className())
                .superClass(TranslationFailedPage.class)
                .build()) {

            MethodCreator constructor = classCreator.getMethodCreator("<init>", void.class);
            constructor.invokeSpecialMethod(
                    MethodDescriptor.ofConstructor(TranslationFailedPage.class, String.class),
                    constructor.getThis(),
                    constructor.load("Page " + result.jspUri() + " could not be translated: " + result.error().getMessage()));
            constructor.returnValue(null);
        }
    }

    /**
     * @return the Servlets declared with a jsp-file, with their mappings, init params and security role references
     */
    private static List<JspFileServletDefinition> getJspFileServlets(WebMetaData webMetaData) {
        List<JspFileServletDefinition> definitions = new ArrayList<>();
        if (webMetaData.getServlets() == null) {
            return definitions;
        }

        for (ServletMetaData servlet : webMetaData.getServlets()) {
            if (servlet.getJspFile() == null || servlet.getServletClass() != null) {
                continue;
            }

            JspFileServletDefinition jspFileServletDefinition = new JspFileServletDefinition();
            jspFileServletDefinition.setName(servlet.getServletName());
            jspFileServletDefinition.setJspFile(servlet.getJspFile().startsWith("/") ? servlet.getJspFile() : "/" + servlet.getJspFile());

            if (servlet.getLoadOnStartupInt() >= 0 && servlet.getLoadOnStartup() != null) {
                jspFileServletDefinition.setLoadOnStartup(servlet.getLoadOnStartupInt());
            }

            if (servlet.getInitParam() != null) {
                for (ParamValueMetaData initParam : servlet.getInitParam()) {
                    jspFileServletDefinition.getInitParams()
                                            .put(initParam.getParamName(), initParam.getParamValue());
                }
            }

            if (servlet.getSecurityRoleRefs() != null) {
                for (SecurityRoleRefMetaData roleRef : servlet.getSecurityRoleRefs()) {
                    jspFileServletDefinition.getSecurityRoleRefs()
                                            .put(
                                                roleRef.getRoleName(),
                                                roleRef.getRoleLink() != null ? roleRef.getRoleLink() : roleRef.getRoleName());
                }
            }

            if (webMetaData.getServletMappings() != null) {
                for (ServletMappingMetaData mapping : webMetaData.getServletMappings()) {
                    if (servlet.getServletName().equals(mapping.getServletName())) {
                        jspFileServletDefinition.getMappings().addAll(mapping.getUrlPatterns());
                    }
                }
            }

            definitions.add(jspFileServletDefinition);
        }

        return definitions;
    }

    /**
     * Lays out the web application the way JspC reads it: the web resources at the root, and the deployment descriptor
     * and other WEB-INF content under WEB-INF. Earlier roots take precedence, as in the deployed application.
     */
    private static void stageWebRoot(List<Path> applicationRoots, Path webRoot) throws IOException {
        createDirectories(webRoot.resolve("WEB-INF"));

        for (Path root : applicationRoots) {
            copyTree(root.resolve("META-INF/resources"), webRoot);
            copyTree(root.resolve("WEB-INF"), webRoot.resolve("WEB-INF"));

            Path webXml = root.resolve("META-INF/web.xml");
            if (exists(webXml) && !exists(webRoot.resolve("WEB-INF/web.xml"))) {
                copy(webXml, webRoot.resolve("WEB-INF/web.xml"));
            }
        }
    }

    private static boolean hasPages(Path webRoot) throws IOException {
        try (Stream<Path> paths = walk(webRoot)) {
            return paths.anyMatch(path -> path.toString().endsWith(".jsp") || path.toString().endsWith(".jspx"));
        }
    }

    /**
     * @return the runtime class path of the application: its own roots and all runtime dependencies
     */
    private static List<Path> getClassPath(List<Path> applicationRoots, CurateOutcomeBuildItem curateOutcome) {
        Set<Path> classPath = new LinkedHashSet<>(applicationRoots);
        for (ResolvedDependency dependency : curateOutcome.getApplicationModel().getRuntimeDependencies()) {
            for (Path path : dependency.getResolvedPaths()) {
                classPath.add(path);
            }
        }

        return new ArrayList<>(classPath);
    }

    /**
     * Finds the listeners declared in all tag library descriptors of the application: under WEB-INF, and in
     * META-INF of the libraries, also of libraries no page uses (as WaSP's runtime scan does).
     */
    /**
     * The classes that tag library descriptors name: their listeners, and all classes they name, the listeners included.
     */
    private record TldClasses(Set<String> listeners, Set<String> classes) {
    }

    /** The JDK types pages commonly use through the Expression Language or jsp:useBean */
    private static final List<Class<?>> COMMON_JDK_TYPES = List.of(
            String.class, StringBuilder.class, Boolean.class, Character.class, Byte.class, Short.class, Integer.class,
            Long.class, Float.class, Double.class, java.math.BigDecimal.class, java.math.BigInteger.class,
            java.util.ArrayList.class, java.util.LinkedList.class, java.util.Vector.class, java.util.Stack.class,
            java.util.HashMap.class, java.util.LinkedHashMap.class, java.util.TreeMap.class, java.util.Hashtable.class,
            java.util.Properties.class, java.util.HashSet.class, java.util.LinkedHashSet.class, java.util.TreeSet.class,
            java.util.Map.Entry.class, java.util.Iterator.class, java.util.Enumeration.class, java.util.Optional.class,
            java.util.Date.class, java.util.GregorianCalendar.class, java.util.Locale.class,
            java.time.LocalDate.class, java.time.LocalDateTime.class, java.time.LocalTime.class, java.time.Instant.class);

    /** A class in WaSP's jar, to recognize it by */
    private static final String WASP_CLASS = JspC.class.getName().replace('.', '/') + ".class";

    /** The elements of a tag library descriptor that name a class, with those of JSP 1.1 descriptors */
    private static final List<String> TLD_CLASS_ELEMENTS = List.of("tag-class", "tagclass", "tei-class", "teiclass",
            "validator-class", "function-class", "listener-class");

    private static TldClasses findTldClasses(Path webRoot, List<Path> classPath) throws IOException {
        Map<String, String> listeners = new LinkedHashMap<>();
        Set<String> classes = new LinkedHashSet<>();

        try (Stream<Path> paths = walk(webRoot.resolve("WEB-INF"))) {
            for (Path tld : paths.filter(path -> path.toString().endsWith(".tld")).toList()) {
                try (InputStream in = newInputStream(tld)) {
                    addTldClasses(tld.toString(), in, listeners, classes);
                }
            }
        }

        for (Path entry : classPath) {
            if (isDirectory(entry)) {
                Path metaInf = entry.resolve("META-INF");
                if (isDirectory(metaInf)) {
                    try (Stream<Path> paths = walk(metaInf)) {
                        for (Path tld : paths.filter(path -> path.toString().endsWith(".tld")).toList()) {
                            try (InputStream in = newInputStream(tld)) {
                                addTldClasses(tld.toString(), in, listeners, classes);
                            }
                        }
                    }
                }
            } else if (entry.toString().endsWith(".jar") && exists(entry)) {
                try (JarFile jarFile = new JarFile(entry.toFile())) {
                    // WaSP's own JSTL tag libraries need the JSTL API; pages that use them reference their classes
                    Set<String> jarClasses = jarFile.getEntry(WASP_CLASS) != null ? new LinkedHashSet<>() : classes;
                    Iterator<JarEntry> entries = jarFile.entries().asIterator();
                    while (entries.hasNext()) {
                        JarEntry jarEntry = entries.next();
                        if (jarEntry.getName().startsWith("META-INF/") && jarEntry.getName().endsWith(".tld")) {
                            try (InputStream in = jarFile.getInputStream(jarEntry)) {
                                addTldClasses(entry + "!/" + jarEntry.getName(), in, listeners, jarClasses);
                            }
                        }
                    }
                }
            }
        }

        return new TldClasses(new LinkedHashSet<>(listeners.keySet()), classes);
    }

    private static void addTldClasses(String location, InputStream in, Map<String, String> listeners, Set<String> classes) {
        try {
            TreeNode tld = new ParserUtils().parseXMLDocument(location, in);
            Iterator<TreeNode> listenerNodes = tld.findChildren("listener");
            while (listenerNodes.hasNext()) {
                TreeNode listenerClass = listenerNodes.next().findChild("listener-class");
                if (listenerClass != null && listenerClass.getBody() != null) {
                    listeners.putIfAbsent(listenerClass.getBody().trim(), location);
                }
            }
            addClassNames(tld, classes);
        } catch (WaspException e) {
            LOG.warnf("Ignoring tag library descriptor %s that could not be parsed: %s", location, e.getMessage());
        }
    }

    /** Adds the class names in the elements below a node that name a class, at any depth. */
    private static void addClassNames(TreeNode node, Set<String> classes) {
        Iterator<TreeNode> children = node.findChildren();
        while (children.hasNext()) {
            TreeNode child = children.next();
            if (TLD_CLASS_ELEMENTS.contains(child.getName()) && child.getBody() != null) {
                classes.add(child.getBody().trim());
            }
            addClassNames(child, classes);
        }
    }

    /**
     * @return the Java release level of the application's classes, so pages aren't compiled for a newer Java than the
     *         application
     */
    private static int getReleaseLevel(List<Path> applicationRoots) throws IOException {
        for (Path root : applicationRoots) {
            if (!isDirectory(root)) {
                continue;
            }

            try (Stream<Path> paths = walk(root)) {
                Optional<Path> classFile = paths.filter(path -> path.toString().endsWith(".class")).findFirst();
                if (classFile.isPresent()) {
                    byte[] bytes = Files.readAllBytes(classFile.get());
                    int major = ((bytes[6] & 0xff) << 8) | (bytes[7] & 0xff);
                    return Math.max(DEFAULT_RELEASE, major - 44);
                }
            }
        }

        return DEFAULT_RELEASE;
    }

    private static void copyTree(Path source, Path target) throws IOException {
        if (!isDirectory(source)) {
            return;
        }

        try (Stream<Path> paths = walk(source)) {
            for (Path path : paths.toList()) {
                Path destination = target.resolve(source.relativize(path).toString());
                if (isDirectory(path)) {
                    createDirectories(destination);
                } else if (!exists(destination)) {
                    copy(path, destination);
                }
            }
        }
    }

    private static void deleteRecursively(Path directory) throws IOException {
        if (!exists(directory)) {
            return;
        }

        try (Stream<Path> paths = walk(directory)) {
            paths.sorted(reverseOrder()).forEach(path -> {
                try {
                    delete(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }
}
