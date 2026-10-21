# WaSP Quarkus extension

Jakarta Pages (JSP) and Jakarta Tags (JSTL) for Quarkus applications that use the Servlet container
(`quarkus-undertow`), with [WaSP](https://github.com/eclipse-ee4j/wasp).

```xml
<dependency>
    <groupId>org.glassfish.wasp</groupId>
    <artifactId>quarkus-wasp</artifactId>
    <version>${wasp.version}</version>
</dependency>
```

## How it works

Pages are translated and compiled when the application is built, with WaSP's `JspC`. That uses the
`jsp-config` of `web.xml`, `jsp-file` Servlets, and the tag libraries in `WEB-INF` and on the class path. At runtime,
WaSP's `JspServlet` serves the precompiled pages and never compiles.

- A page that can't be translated fails with status 500 when it's requested. It doesn't fail the build.
- Listeners declared in tag library descriptors are registered.
- The files in `WEB-INF` are resources of the `ServletContext`, as in a web archive; clients can't request them.
- Servlets declared with a `jsp-file` are registered with their mappings, init parameters, `load-on-startup` and
  security role references.

Building needs a JDK with a Java compiler. In dev mode, a changed page needs a rebuild.

## Native executables

Pages and the Jakarta Tags work in a native executable. The extension registers what WaSP, the Jakarta Tags and the
Jakarta Expression Language use by reflection or as resources:

- the page and tag file classes, which WaSP loads by name, and the classes they refer to, e.g. the class of a
  `<jsp:useBean>`, also in libraries;
- the application's own classes, and the classes tag library descriptors name (tag handlers, tag extra infos,
  validators, listeners, functions), also in libraries, and those of the Jakarta Tags WaSP implements;
- the classes of the implicit objects, e.g. for `${pageContext.request.contextPath}`, and of the objects the Jakarta
  Tags expose, e.g. for `${status.last}` of `<c:forEach varStatus="status">` or `${result.rowCount}` of `<sql:query>`;
- the JDBC drivers on the class path, which the SQL tags load by the class name a page gives;
- the resource bundles of the formatting tags that pages name literally, e.g. `<fmt:setBundle basename="messages"/>`,
  and that `web.xml` names as default (`jakarta.servlet.jsp.jstl.fmt.localizationContext`);
- what the XML tags use of the Xalan in WaSP's jar, for XPath and XSLT;
- common JDK types: strings, numbers, collections, maps, dates and times, e.g. for `${list.size()}`, and
  `java.util.Date` for serialization, for `<jsp:useBean beanName="...">`;
- the Expression Language implementation, the message bundles of the Expression Language and WaSP, and all charsets;
- the application's resources, e.g. resource bundles and serialized beans, and the files in `WEB-INF`.

Other classes a page uses only through the Expression Language, e.g. a type of a library, must be registered for
reflection by the application, e.g. with `@RegisterForReflection(targets = ...)` on one of its classes. Resource
bundles that pages name with an expression, e.g. `basename="${bundle}"`, must be included by the application too, e.g.
with native-image's `-H:IncludeResourceBundles=<base name>` in `quarkus.native.additional-build-args`.

The formatting tags format and parse with the locale data the native executable has. Quarkus includes only the default
locale, unless `quarkus.locales` names others (or `all`).

In a native executable, `<x:transform>` uses the XSLT processor of the Xalan in WaSP's jar, which interprets stylesheets,
instead of the JDK's, which compiles them into classes that a native executable can't load. In JVM mode, it uses the
JDK's.

## Validating pages again at runtime

Tag extra infos (TEIs) and tag library validators (TLVs) run when a page is translated, which is at build time. By
default, pages that use them, directly or through the tag files they use, are also parsed and validated in the running
application, once, before their first use. That includes those tag files, so their TEIs and TLVs run there too. If
that fails, requests for the page fail with status 500, as for a page that could not be translated. This includes a
TEI or TLV that can't be created or initialized.

Each validation uses TEI and TLV instances of its own. Concurrent first requests for a page wait for the same
validation.

Limits:

- Validating again can reject a page, but can't change what was decided when it was translated, such as the scripting
  variables a TEI declares. That information must not differ at runtime.
- In a native executable, which has no jars, pages whose tag library descriptors or tag files are in a jar are not
  validated again. The pages are included in the executable as resources, for those that are.

| Property | Default | |
|---|---|---|
| `quarkus.wasp.validate-at-runtime` | `true` | Validate pages that use TEIs or TLVs again at runtime. Build time property: changing it needs a rebuild. |

## Modules

- `runtime`, `deployment`: the extension.
- `test`: tests, each deploying an application with the
  [Arquillian Quarkus container](https://github.com/OmniFish-EE/arquillian-container-quarkus).

The tests run in JVM mode by default, and as native executables with `mvn install -Pnative` in `test`.

The Jakarta Pages TCK and the Jakarta Tags TCK run against the extension from `tck/pages` and `tck/tags` in the WaSP
root, with `-Pquarkus` or `-Pquarkus-native`, and so does the benchmark from `benchmark`, which measures the throughput
and latency of a page with the extension and in GlassFish (see the WaSP README).
