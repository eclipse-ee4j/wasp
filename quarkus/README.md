# WaSP Quarkus extension

Jakarta Pages (JSP) for Quarkus applications that use the Servlet container (`quarkus-undertow`), with
[WaSP](https://github.com/eclipse-ee4j/wasp).

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
- Servlets declared with a `jsp-file` are registered with their mappings, init parameters, `load-on-startup` and
  security role references.

Building needs a JDK with a Java compiler. In dev mode, a changed page needs a rebuild.

## Native executables

Pages work in a native executable. The extension registers what WaSP and the Jakarta Expression Language use by
reflection or as resources:

- the page and tag file classes, which WaSP loads by name;
- the application's own classes, and the classes tag library descriptors name (tag handlers, tag extra infos,
  validators, listeners, functions), also in libraries;
- the classes of the implicit objects, e.g. for `${pageContext.request.contextPath}`;
- common JDK types: strings, numbers, collections, maps, dates and times, e.g. for `${list.size()}`, and
  `java.util.Date` for serialization, for `<jsp:useBean beanName="...">`;
- the Expression Language implementation, the message bundles of the Expression Language and WaSP, and all charsets;
- the application's resources, e.g. resource bundles and serialized beans.

Other classes a page uses only through the Expression Language, e.g. a type of a library, must be registered for
reflection by the application, e.g. with `@RegisterForReflection(targets = ...)` on one of its classes.

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
  validated again. The pages and the files in `WEB-INF` are included in the executable as resources, for those that
  are.

| Property | Default | |
|---|---|---|
| `quarkus.wasp.validate-at-runtime` | `true` | Validate pages that use TEIs or TLVs again at runtime. Build time property: changing it needs a rebuild. |

## Modules

- `runtime`, `deployment`: the extension.
- `test`: tests, each deploying an application with the
  [Arquillian Quarkus container](https://github.com/OmniFish-EE/arquillian-container-quarkus).
- `benchmark`: the throughput and latency of one page, `catalog.jsp`, under load from JMeter (embedded, with
  [jmeter-java-dsl](https://abstracta.github.io/jmeter-java-dsl/)). The page uses a static and a dynamic include,
  `jsp:useBean`, a classic iteration tag, a tag file with a fragment attribute, a simple tag, functions, and the
  Expression Language on beans, lists and maps. It's not part of this build: `mvn verify -f benchmark` after
  `mvn install`, and again with `-Dnative`. Each run writes its results to `benchmark/target/benchmark`, and prints
  them next to those of the other mode if that ran before (so don't clean in between). The load runs for a warm-up
  period first, which isn't measured. `-Dwasp.benchmark.threads` (default 8), `-Dwasp.benchmark.warmupSeconds`
  (default 30) and `-Dwasp.benchmark.durationSeconds` (default 60) change the load, and
  `-Darquillian.quarkus.javaVmArguments`, e.g. `-Xmx512m`, the options of the application in both modes. JMeter
  measures latency in whole milliseconds, and runs on the same machine as the application.
- `tck`: runs the Jakarta Pages TCK against the extension. It's not part of this build:
  `mvn verify -f tck` after `mvn install`. With `-Dnative`, each test archive is built into a native executable, which
  needs GraalVM or Mandrel (`GRAALVM_HOME`) and takes about an hour and a half.
