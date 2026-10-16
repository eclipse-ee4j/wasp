# WaSP

Eclipse WaSP implements Jakarta Pages, a technology that helps software developers create dynamically generated web pages based on HTML, XML, or other document types.

[Website](https://eclipse-ee4j.github.io/wasp)

Building
--------

WaSP can be built by executing the following from the project root:

``mvn clean package``

Jakarta Pages TCK
-----------------

`tck` runs the Jakarta Pages TCK against WaSP. It's not part of the WaSP build: run `mvn install` in the project root
first, then `mvn install` in `tck`. Its `install` module downloads the TCK and installs it in the local Maven
repository; `run` runs it, with one of three profiles:

- `glassfish` (the default): in GlassFish 8.0.5, with GlassFish's own WaSP jar replaced by the one just built. It takes
  about a minute.
- `quarkus`: on Quarkus with the WaSP extension, with each test archive built into a Quarkus application by the
  [Arquillian Quarkus container](https://github.com/OmniFish-EE/arquillian-container-quarkus). Run `mvn install` in
  `quarkus` first. It takes about five minutes.
- `quarkus-native`: as `quarkus`, but with each test archive built into a native executable. It needs GraalVM or
  Mandrel (`GRAALVM_HOME`) and takes about an hour and a half.

For example, `mvn install -f tck -Pquarkus`.
