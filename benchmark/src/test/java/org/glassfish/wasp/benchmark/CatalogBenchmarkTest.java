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
package org.glassfish.wasp.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpCookies;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpSampler;
import static us.abstracta.jmeter.javadsl.JmeterDsl.responseAssertion;
import static us.abstracta.jmeter.javadsl.JmeterDsl.testPlan;
import static us.abstracta.jmeter.javadsl.JmeterDsl.threadGroup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import us.abstracta.jmeter.javadsl.core.TestPlanStats;
import us.abstracta.jmeter.javadsl.core.stats.StatsSummary;

/**
 * Throughput and latency of one page, {@code catalog.jsp}, under load from JMeter (embedded, with jmeter-java-dsl).
 * Run it with each profile: {@code glassfish} (the default), {@code quarkus} (JVM mode), {@code quarkus-native} and
 * {@code piranha}.
 * Each run writes its results to {@code target/benchmark}, and prints them next to those of the other profiles when
 * there are any.
 *
 * <p>
 * The page is checked first. Then the load runs for a warm-up period, which isn't measured (it lets the JIT compile
 * the hot code on a JVM; a native executable gets it too, to treat all the same), and then for the measured period.
 * The load generator runs on the same machine, so the absolute numbers depend on it; the comparison is what counts.
 *
 * <p>
 * System properties: {@code wasp.benchmark.threads} (concurrent users, default 8),
 * {@code wasp.benchmark.warmupSeconds} (default 5) and {@code wasp.benchmark.durationSeconds} (default 10). The
 * defaults are minimal, as the build runs the benchmark too, only to check that it works; meaningful numbers need
 * longer runs, and several of them (see the README).
 */
@ExtendWith(ArquillianExtension.class)
class CatalogBenchmarkTest {

    private static final int THREADS = Integer.getInteger("wasp.benchmark.threads", 8);
    private static final Duration WARMUP = Duration.ofSeconds(Integer.getInteger("wasp.benchmark.warmupSeconds", 5));
    private static final Duration DURATION = Duration.ofSeconds(Integer.getInteger("wasp.benchmark.durationSeconds", 10));

    /** The profiles, each running the benchmark on its own runtime, in the order their results are printed */
    private static final List<String> RUNTIMES = List.of("glassfish", "quarkus", "quarkus-native", "piranha");

    /** The profile of this run, which names its results */
    private static final String RUNTIME = System.getProperty("wasp.benchmark.runtime", "glassfish");
    private static final Path RESULTS = Path.of("target", "benchmark");

    /** The keys of the results, in the order they're printed */
    private static final List<String> KEYS = List.of("throughput", "mean", "median", "p90", "p95", "p99", "max",
            "samples", "errors", "rssMb");

    @ArquillianResource
    private URL base;

    @Deployment(testable = false)
    static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "benchmark.war")
                         .addClasses(Catalog.class, Category.class, Product.class, CatalogListener.class, Summary.class,
                                 ForEachTag.class, MoneyTag.class, Functions.class)
                         .addAsWebResource(new File("src/main/webapp/catalog.jsp"), "catalog.jsp")
                         .addAsWebResource(new File("src/main/webapp/footer.jsp"), "footer.jsp")
                         .addAsWebResource(new File("src/main/webapp/pid.jsp"), "pid.jsp")
                         .addAsWebInfResource(new File("src/main/webapp/WEB-INF/benchmark.tld"), "benchmark.tld")
                         .addAsWebInfResource(new File("src/main/webapp/WEB-INF/tags/row.tag"), "tags/row.tag")
                         .addAsWebInfResource(new File("src/main/webapp/WEB-INF/jspf/header.jspf"), "jspf/header.jspf")
                         .addAsWebInfResource(new File("src/main/webapp/WEB-INF/glassfish-web.xml"), "glassfish-web.xml");
    }

    @Test
    void catalog() throws Exception {
        URI page = base.toURI().resolve("catalog.jsp");
        checkPage(page);

        System.out.printf("%nBenchmark (%s): %d threads, %d s warm-up, %d s measured, %s%n", RUNTIME, THREADS,
                WARMUP.toSeconds(), DURATION.toSeconds(), page);

        StatsSummary warmup = load(page, WARMUP).overall();
        assertEquals(0, warmup.errorsCount(), "errors during the warm-up");

        StatsSummary measured = load(page, DURATION).overall();
        assertEquals(0, measured.errorsCount(), "errors during the measurement");

        Properties results = toResults(measured, residentMemoryMb(base.toURI().resolve("pid.jsp")));
        Files.createDirectories(RESULTS);
        try (OutputStream out = Files.newOutputStream(RESULTS.resolve(RUNTIME + ".properties"))) {
            results.store(out, "WaSP benchmark, " + RUNTIME + ", " + THREADS + " threads, " + DURATION.toSeconds()
                    + " s measured after " + WARMUP.toSeconds() + " s warm-up");
        }

        print(results);
    }

    /** Checks the page renders the whole catalog, so that the benchmark measures a correct page. */
    private static void checkPage(URI page) throws Exception {
        HttpResponse<String> response = get(page);
        assertEquals(200, response.statusCode(), response.body());

        String body = response.body();
        int rows = body.split("<tr class=", -1).length - 1;
        assertEquals(Catalog.CATEGORIES * Catalog.PRODUCTS_PER_CATEGORY, rows, body);
        assertTrue(body.contains(String.format(Locale.ROOT, "Total value: EUR %,.2f", new Catalog().getTotalValue())), body);
        assertTrue(body.contains("sold out") && body.contains("few left"), body);
        assertTrue(body.contains("<footer>100 rows, GET "), body);
    }

    private static TestPlanStats load(URI page, Duration duration) throws IOException {
        return testPlan(
                // Each user keeps its session cookie, as a browser does. The page creates a session, so clearing the
                // cookies at each iteration, the default, would create a new one at every request
                httpCookies().clearCookiesBetweenIterations(false),
                threadGroup(THREADS, duration,
                        httpSampler(page.toString())
                                .children(responseAssertion().containsSubstrings("</footer>"))))
                .run();
    }

    private static Properties toResults(StatsSummary stats, long rssMb) {
        Properties results = new Properties();
        results.setProperty("throughput", String.format(Locale.ROOT, "%.1f", stats.samples().perSecond()));
        results.setProperty("mean", millis(stats.sampleTime().mean()));
        results.setProperty("median", millis(stats.sampleTime().median()));
        results.setProperty("p90", millis(stats.sampleTime().perc90()));
        results.setProperty("p95", millis(stats.sampleTime().perc95()));
        results.setProperty("p99", millis(stats.sampleTime().perc99()));
        results.setProperty("max", millis(stats.sampleTime().max()));
        results.setProperty("samples", Long.toString(stats.samplesCount()));
        results.setProperty("errors", Long.toString(stats.errorsCount()));
        results.setProperty("rssMb", Long.toString(rssMb));
        return results;
    }

    private static String millis(Duration duration) {
        return String.format(Locale.ROOT, "%.2f", duration.toNanos() / 1_000_000.0);
    }

    /**
     * @return the resident memory of the application process after the load, in MB, or -1 if it can't be measured
     *         (with {@code ps}, so not on Windows)
     */
    private static long residentMemoryMb(URI pidPage) throws Exception {
        String pid = get(pidPage).body().trim();
        try {
            Process ps = new ProcessBuilder("ps", "-o", "rss=", "-p", pid).redirectErrorStream(true).start();
            try (InputStream in = ps.getInputStream()) {
                String kilobytes = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
                return ps.waitFor() == 0 ? Long.parseLong(kilobytes) / 1024 : -1;
            }
        } catch (IOException | NumberFormatException e) {
            return -1;
        }
    }

    /** Prints the results, next to those of the other profiles that ran too. */
    private static void print(Properties results) throws IOException {
        Map<String, Properties> all = new LinkedHashMap<>();
        for (String runtime : RUNTIMES) {
            Path file = RESULTS.resolve(runtime + ".properties");
            if (runtime.equals(RUNTIME)) {
                all.put(runtime, results);
            } else if (Files.exists(file)) {
                Properties other = new Properties();
                try (InputStream in = Files.newInputStream(file)) {
                    other.load(in);
                }
                all.put(runtime, other);
            }
        }

        StringBuilder table = new StringBuilder(String.format("%n%-22s", ""));
        for (String runtime : RUNTIMES) {
            table.append(String.format(" %15s", heading(runtime)));
        }
        table.append(String.format("%n"));
        for (String key : KEYS) {
            table.append(String.format("%-22s", label(key)));
            for (String runtime : RUNTIMES) {
                Properties runtimeResults = all.get(runtime);
                table.append(String.format(" %15s", runtimeResults == null ? "-" : runtimeResults.getProperty(key, "-")));
            }
            table.append(String.format("%n"));
        }

        List<String> notRun = RUNTIMES.stream().filter(runtime -> !all.containsKey(runtime)).map(runtime -> "-P" + runtime).toList();
        if (!notRun.isEmpty()) {
            table.append(String.format("(no results yet for %s)%n", String.join(", ", notRun)));
        }
        System.out.println(table);
    }

    private static String heading(String runtime) {
        return switch (runtime) {
            case "quarkus" -> "Quarkus JVM";
            case "quarkus-native" -> "Quarkus native";
            case "glassfish" -> "GlassFish";
            case "piranha" -> "Piranha";
            default -> runtime;
        };
    }

    private static String label(String key) {
        return switch (key) {
            case "throughput" -> "throughput (req/s)";
            case "mean", "median", "p90", "p95", "p99", "max" -> key + " latency (ms)";
            case "rssMb" -> "resident memory (MB)";
            default -> key;
        };
    }

    private static HttpResponse<String> get(URI uri) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri).build(), BodyHandlers.ofString());
    }
}
