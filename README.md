# WaSP

Eclipse WaSP implements Jakarta Pages, a technology that helps software developers create dynamically generated web pages based on HTML, XML, or other document types.

[Website](https://eclipse-ee4j.github.io/wasp)

Building
--------

WaSP can be built by executing the following from the project root:

``mvn clean package``

Jakarta Pages TCK
-----------------

`tck` runs the Jakarta Pages TCK against WaSP. run `mvn install` in the project root
first, then `mvn install` in `tck`. The `install` module downloads the TCK, the `run` mdoule runs it, with these profiles:

- `glassfish` (the default): GlassFish 8.0.5, with GlassFish's own WaSP jar replaced by the one just built. Each test is deployed 
  and undeployed to a running server. It takes about a minute.
- `glassfish-embedded`: GlassFish Embedded 8.0.5, in the JVM of the tests. GlassFish Embedded is a single jar with
  its own copy of WaSP inside, so the WaSP jar just built comes before it on the class path. It takes about a minute.
- `quarkus`: Quarkus with the WaSP extension, with each test archive built into a Quarkus application by the
  [Arquillian Quarkus container](https://github.com/OmniFish-EE/arquillian-container-quarkus). Run `mvn install` in
  `quarkus` first. It takes about five minutes.
- `quarkus-native`: as `quarkus`, but with each test archive built into a native executable. It needs GraalVM or
  Mandrel (`GRAALVM_HOME`) and takes about an hour and a half.
- `piranha-micro`: on [Piranha](https://github.com/eclipse-ee4j/piranha) Micro, in the JVM of the tests: each test archive
  is deployed to a Micro of its own, with Micro's WaSP jar replaced by the one just built. It needs a local build of
  Piranha 25.4.0-SNAPSHOT (`mvn install` in Piranha), and takes about two minutes.
- `piranha-webprofile`: Piranha Web Profile, with each test archive deployed to a newly started Piranha process
  , with the WaSP jar just built on the class path before Piranha's jar, which contains WaSP 4.0.0. It needs
  a local build of Piranha 25.4.0-SNAPSHOT (`mvn install` in Piranha), and takes about two minutes.

For example, `mvn install -f tck -Pquarkus`.

Benchmark
---------

`benchmark` measures the throughput and latency of one page, `catalog.jsp`, under load from
[JMeter](https://jmeter.apache.org/) (embedded, with [jmeter-java-dsl](https://abstracta.github.io/jmeter-java-dsl/)),
in GlassFish, on Quarkus with the WaSP extension and in Piranha. 

The page renders a catalog of 100 products with a static and a dynamic include, `jsp:useBean`, a classic iteration tag, a tag 
file with a fragment attribute, a simple tag, functions, and the Expression Language on beans, lists and maps. 

8 users request it, each again as soon as it has the response, and each keeping its session cookie as a browser does. 
The load runs for 30 seconds before it's measured.

It's not part of the WaSP build: run `mvn install` in the project root first, then `mvn verify` in `benchmark`, with
one of four profiles:

- `glassfish` (the default): in GlassFish 8.0.5, with GlassFish's own WaSP jar replaced by the one just built. Two
  settings make it serve the page as Quarkus does: it doesn't check pages for changes at each request (`development`
  is `false` in the application's `glassfish-web.xml`), and its HTTP thread pool serves up to 200 requests at a time
  instead of 5.
- `quarkus`: on Quarkus in JVM mode, with the WaSP extension. Run `mvn install` in `quarkus` first.
- `quarkus-native`: as `quarkus`, but as a native executable. It needs GraalVM or Mandrel (`GRAALVM_HOME`).
- `piranha`: in [Piranha](https://github.com/eclipse-ee4j/piranha) Web Profile, as a process of its own, with the WaSP
  jar just built on the class path before Piranha's jar, which contains WaSP 4.0.0. It needs a local build of Piranha
  25.4.0-SNAPSHOT (`mvn install` in Piranha). As in GlassFish, it doesn't check pages for changes at each request
  (`development` is `false`). It serves HTTP with Grizzly, as GlassFish does
  (`-Dpiranha.httpServerClass`).
 
For example, `mvn verify -f benchmark -Pquarkus`. 

Each run writes its results to `benchmark/target/benchmark`, and prints them next to those of the other profiles that ran before, s
o don't clean in between. 

`-Dwasp.benchmark.threads` (default 8), `-Dwasp.benchmark.warmupSeconds` (default 30) and `-Dwasp.benchmark.durationSeconds` (default 60) change
the load. 

`-Darquillian.quarkus.javaVmArguments` adds options to the JVM of the application on Quarkus, and
`-Dpiranha.javaVmArguments` to Piranha's, e.g. `-Xmx512m`, the maximum heap GlassFish has in its `domain.xml`.

### Results

Five rounds each running six configurations in this order, with `-Dwasp.benchmark.durationSeconds=180`: 180 seconds
measured per run, after the 30 seconds of warm-up. All 30 runs passed without errors.

- `quarkus`, `glassfish` and `piranha`, each first with the JVM's default heap, then with a heap of at most 512 MB
  (`-Xmx512m`). On this machine the default heap is at most 4 GB, a quarter of the memory, and starts at 256 MB. All
  three run on the same JVM with the G1 garbage collector; the heap of each was checked in its running process.
- GlassFish's default heap: the `-Xmx512m` of `server-config` removed from its `domain.xml` (in `benchmark/target`).

Throughput per round, in requests per second:

| Round | Quarkus JVM, default heap | Quarkus JVM, 512 MB | GlassFish, default heap | GlassFish, 512 MB | Piranha, default heap | Piranha, 512 MB |
|---|---|---|---|---|---|---|
| 1 | 8,459 | 8,169 | 7,702 | 7,448 | 8,166 | 7,445 |
| 2 | 8,501 | 7,920 | 7,898 | 7,188 | 7,994 | 7,418 |
| 3 | 8,312 | 8,031 | 7,858 | 7,425 | 8,104 | 7,388 |
| 4 | 8,239 | 7,940 | 7,672 | 7,165 | 7,975 | 7,419 |
| 5 | 8,463 | 7,952 | 7,614 | 7,246 | 8,076 | 7,442 |

Over the five rounds, with the default heap (at most 4 GB):

| | Quarkus JVM | GlassFish | Piranha |
|---|---|---|---|
| throughput, mean (requests per second) | 8,395 | 7,749 | 8,063 |
| throughput, standard deviation | 113 (1.3%) | 123 (1.6%) | 79 (1.0%) |
| throughput, lowest – highest | 8,239 – 8,501 | 7,614 – 7,898 | 7,975 – 8,166 |
| median latency (ms) | 1 | 1 | 1 |
| p90 latency (ms) | 2 – 3 | 1 – 3 | 2 |
| p99 latency (ms) | 3 – 5 | 3 – 4 | 3 |
| maximum latency (ms) | 6 – 26 | 4 – 292 | 4 – 9 |
| resident memory after the load (MB) | 740 – 856 | 526 – 1,262 | 905 – 1,000 |

With a heap of at most 512 MB:

| | Quarkus JVM | GlassFish | Piranha |
|---|---|---|---|
| throughput, mean (requests per second) | 8,002 | 7,294 | 7,422 |
| throughput, standard deviation | 102 (1.3%) | 133 (1.8%) | 23 (0.3%) |
| throughput, lowest – highest | 7,920 – 8,169 | 7,165 – 7,448 | 7,388 – 7,445 |
| median latency (ms) | 1 | 1 | 1 |
| p90 latency (ms) | 2 – 3 | 2 – 3 | 2 – 3 |
| p99 latency (ms) | 3 – 6 | 3 – 4 | 2 – 3 |
| maximum latency (ms) | 8 – 32 | 6 – 19 | 6 – 29 |
| resident memory after the load (MB) | 326 – 351 | 403 – 608 | 415 – 441 |

With either heap, Quarkus in JVM mode serves the most requests per second, then Piranha, then GlassFish. With the
default heap, Piranha serves 96% of what Quarkus serves and GlassFish 92%; with 512 MB, 93% and 91%. Piranha serves 4%
more than GlassFish with the default heap, and 2% more with 512 MB, about as much as GlassFish varies between rounds.
The smaller heap costs Quarkus 5% of its throughput, GlassFish 6% and Piranha 8%. With 512 MB, Quarkus uses the least
memory.

#### Quarkus native

Five rounds each running `glassfish`, `quarkus` and `quarkus-native`
in that order, with the same settings, GlassFish with its heap of at most 512 MB and Quarkus with the JVM's default.
All 15 runs passed without errors. Throughput differs a bit between sessions (GlassFish did 7,470 then and 7,294 in
the session above, Quarkus in JVM mode 8,519 and 8,395), so compare within one session.

Throughput per round, in requests per second:

| Round | GlassFish | Quarkus JVM | Quarkus native |
|---|---|---|---|
| 1 | 7,564 | 8,520 | 4,540 |
| 2 | 7,659 | 8,517 | 4,549 |
| 3 | 7,305 | 8,497 | 4,552 |
| 4 | 7,174 | 8,507 | 4,545 |
| 5 | 7,650 | 8,556 | 4,561 |

Over the five rounds:

| | GlassFish | Quarkus JVM | Quarkus native |
|---|---|---|---|
| throughput, mean (requests per second) | 7,470 | 8,519 | 4,549 |
| throughput, standard deviation | 219 (2.9%) | 22 (0.3%) | 8 (0.2%) |
| throughput, lowest – highest | 7,174 – 7,659 | 8,497 – 8,556 | 4,540 – 4,561 |
| median latency (ms) | 1 | 1 | 2 |
| p90 latency (ms) | 2 – 3 | 2 – 4 | 2 |
| p99 latency (ms) | 2 – 4 | 2 – 3 | 5 |
| maximum latency (ms) | 12 – 41 | 6 – 48 | 14 – 59 |
| resident memory after the load (MB) | 589 – 613 | 780 – 813 | 249 – 299 |

Quarkus in JVM mode serves 14% more requests per second than GlassFish. The native executable serves 53% of what JVM
mode serves and 61% of what GlassFish serves, with the least memory. Quarkus' throughput hardly varies between rounds
(0.3% in JVM mode, 0.2% native), GlassFish's by 2.9%.

#### Notes

- The load generator runs on the same machine as the application, so the numbers depend on the machine; the comparison
  is what counts.
- JMeter measures latency in whole milliseconds, and jmeter-java-dsl estimates the percentiles (with the P² algorithm),
  so p90 and p99 are approximate.
- Resident memory is measured once, with `ps`, right after the load, so it depends on how much of the heap is in use at
  that moment.
- The configurations ran in the same order in each round, so a drift over the two hours would affect them in the same
  pattern each round.
- GlassFish's `domain.xml` also has `-XX:NewRatio=2`, kept in both of its configurations. With it, G1 keeps the young
  generation at a third of the heap; for Quarkus and Piranha, G1 sizes it itself.

### Hardware and software

| | |
|---|---|
| Computer | MacBook Pro, model identifier MacBookPro18,3 |
| CPU | Apple M1 Pro, 10 cores: 8 performance and 2 efficiency |
| Memory | 16 GB |
| Power | AC power, low power mode off; macOS recorded no thermal or performance warnings, in either session |
| Operating system | macOS 26.5.1 (build 25F80), Darwin kernel 25.5.0 |
| Java | Eclipse Temurin 21.0.9+10 (OpenJDK 64-Bit Server VM, build 21.0.9+10-LTS), for GlassFish, Quarkus in JVM mode, Piranha, JMeter and Maven, with the G1 garbage collector |
| Native executable | GraalVM CE 25.0.2+10.1 (native-image 25.0.2), with the serial garbage collector (earlier session) |
| JVM options | GlassFish: those of its `domain.xml`, among them `-XX:NewRatio=2` and, unless the default heap is measured, `-Xmx512m`. Quarkus JVM and Piranha: none, or `-Xmx512m`. Quarkus native: none |
| WaSP | 4.0.1-SNAPSHOT, built from commit 8e3880a |
| Quarkus | 3.40.1 |
| GlassFish | Eclipse GlassFish 8.0.5 (commit 24a7d8b) |
| Piranha | Piranha Web Profile 25.4.0-SNAPSHOT, built from commit bb8e3fe with local changes not yet in Piranha (among them the HTTP server setting of its Arquillian connector, and fixes to its request and response output), on Grizzly 4.1.0-M1 |
| Load generator | Apache JMeter 5.6.3, with jmeter-java-dsl 2.3 |
| Build | Apache Maven 3.9.16, Arquillian Quarkus container 1.1.1, Arquillian GlassFish container 2.2.1, Piranha's Arquillian managed container 25.4.0-SNAPSHOT |
