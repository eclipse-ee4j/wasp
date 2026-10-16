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
package org.glassfish.wasp.quarkus.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.util.List;
import java.util.Locale;
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
 * Run it in JVM mode and with {@code -Dnative}: each run writes its results to {@code target/benchmark}, and prints a
 * comparison with the other mode's results when there are any.
 *
 * <p>
 * The page is checked first. Then the load runs for a warm-up period, which isn't measured (it lets the JIT compile
 * the hot code in JVM mode; a native executable gets it too, to treat both the same), and then for the measured
 * period. The load generator runs on the same machine, so the absolute numbers depend on it; the comparison is what
 * counts.
 *
 * <p>
 * System properties: {@code wasp.benchmark.threads} (concurrent users, default 8),
 * {@code wasp.benchmark.warmupSeconds} (default 30) and {@code wasp.benchmark.durationSeconds} (default 60).
 */
@ExtendWith(ArquillianExtension.class)
class CatalogBenchmarkTest {

    private static final int THREADS = Integer.getInteger("wasp.benchmark.threads", 8);
    private static final Duration WARMUP = Duration.ofSeconds(Integer.getInteger("wasp.benchmark.warmupSeconds", 30));
    private static final Duration DURATION = Duration.ofSeconds(Integer.getInteger("wasp.benchmark.durationSeconds", 60));

    private static final String MODE = Boolean.getBoolean("arquillian.quarkus.nativeImage") ? "native" : "jvm";
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
                         .addAsWebInfResource(new File("src/main/webapp/WEB-INF/jspf/header.jspf"), "jspf/header.jspf");
    }

    @Test
    void catalog() throws Exception {
        URI page = base.toURI().resolve("catalog.jsp");
        checkPage(page);

        System.out.printf("%nBenchmark (%s): %d threads, %d s warm-up, %d s measured, %s%n", MODE, THREADS,
                WARMUP.toSeconds(), DURATION.toSeconds(), page);

        StatsSummary warmup = load(page, WARMUP).overall();
        assertEquals(0, warmup.errorsCount(), "errors during the warm-up");

        StatsSummary measured = load(page, DURATION).overall();
        assertEquals(0, measured.errorsCount(), "errors during the measurement");

        Properties results = toResults(measured, residentMemoryMb(base.toURI().resolve("pid.jsp")));
        Files.createDirectories(RESULTS);
        try (OutputStream out = Files.newOutputStream(RESULTS.resolve(MODE + ".properties"))) {
            results.store(out, "WaSP Quarkus benchmark, " + MODE + ", " + THREADS + " threads, " + DURATION.toSeconds()
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

    /** Prints the results, next to those of the other mode if it was run too. */
    private static void print(Properties results) throws IOException {
        String otherMode = MODE.equals("jvm") ? "native" : "jvm";
        Path otherFile = RESULTS.resolve(otherMode + ".properties");
        Properties other = null;
        if (Files.exists(otherFile)) {
            other = new Properties();
            try (InputStream in = Files.newInputStream(otherFile)) {
                other.load(in);
            }
        }

        Properties jvm = MODE.equals("jvm") ? results : other;
        Properties nativeResults = MODE.equals("native") ? results : other;

        StringBuilder table = new StringBuilder(String.format("%n%-22s %12s %12s%n", "", "JVM", "native"));
        for (String key : KEYS) {
            table.append(String.format("%-22s %12s %12s%n", label(key),
                    jvm == null ? "-" : jvm.getProperty(key, "-"),
                    nativeResults == null ? "-" : nativeResults.getProperty(key, "-")));
        }
        if (other == null) {
            table.append(String.format("(run the benchmark %s to compare)%n", otherMode.equals("native") ? "with -Dnative" : "without -Dnative"));
        }
        System.out.println(table);
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
