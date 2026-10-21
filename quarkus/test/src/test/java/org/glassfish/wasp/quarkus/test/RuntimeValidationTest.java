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
package org.glassfish.wasp.quarkus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import org.glassfish.wasp.quarkus.test.validation.CountingTEI;
import org.glassfish.wasp.quarkus.test.validation.CountingTLV;
import org.glassfish.wasp.quarkus.test.validation.FlagTEI;
import org.glassfish.wasp.quarkus.test.validation.NoopTag;
import org.glassfish.wasp.quarkus.test.validation.RejectingTEI;
import org.glassfish.wasp.quarkus.test.validation.ThrowingTLV;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Pages using tag extra infos (TEIs) and tag library validators (TLVs) are validated again in the running application,
 * once, before their first use. The application runs with the system property {@value RejectingTEI#REJECT_PROPERTY},
 * the build doesn't, so some pages translate but don't validate at runtime.
 */
@ExtendWith(ArquillianExtension.class)
class RuntimeValidationTest {

    private static final String JAR_TLD_NATIVE = "Pages with a tag library descriptor in a jar aren't validated again natively";

    @ArquillianResource
    private URL base;

    @Deployment(testable = false)
    static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "validation.war")
                         .addAsWebResource(new File("src/main/webapp/validation/flag.jsp"), "flag.jsp")
                         .addAsWebResource(new File("src/main/webapp/validation/count.jsp"), "count.jsp")
                         .addAsWebResource(new File("src/main/webapp/validation/counts.jsp"), "counts.jsp")
                         .addAsWebResource(new File("src/main/webapp/validation/reject.jsp"), "reject.jsp")
                         .addAsWebResource(new File("src/main/webapp/validation/reject-startup.jsp"), "reject-startup.jsp")
                         .addAsWebResource(new File("src/main/webapp/validation/throwing.jsp"), "throwing.jsp")
                         .addAsWebResource(new File("src/main/webapp/validation/tlv-a.jsp"), "tlv-a.jsp")
                         .addAsWebResource(new File("src/main/webapp/validation/tlv-b.jsp"), "tlv-b.jsp")
                         .addAsWebResource(new File("src/main/webapp/validation/direct.jsp"), "direct.jsp")
                         .addAsWebResource(new File("src/main/webapp/validation/tagdir.jsp"), "tagdir.jsp")
                         .addAsWebInfResource(new File("src/main/webapp/validation/WEB-INF/tlds/direct.tld"), "tlds/direct.tld")
                         .addAsWebResource(new File("src/main/webapp/validation/tagfile.jsp"), "tagfile.jsp")
                         .addAsWebInfResource(new File("src/main/webapp/tags/WEB-INF/tags/greet.tag"), "tags/greet.tag")
                         .addAsWebInfResource(new File("src/main/webapp/validation/WEB-INF/tags/flagged.tag"), "tags/flagged.tag")
                         .setWebXML(new File("src/main/webapp/validation/web.xml"))
                         .addAsLibraries(validationLibrary());
    }

    static JavaArchive validationLibrary() {
        return ShrinkWrap.create(JavaArchive.class, "validation-taglib.jar")
                         .addClasses(NoopTag.class, FlagTEI.class, CountingTEI.class, RejectingTEI.class, CountingTLV.class, ThrowingTLV.class)
                         .addAsManifestResource(new File("src/main/webapp/validation/lib/validation.tld"), "validation.tld")
                         .addAsManifestResource(new File("src/main/webapp/validation/lib/tlv.tld"), "tlv.tld")
                         .addAsManifestResource(new File("src/main/webapp/validation/lib/throwing.tld"), "throwing.tld");
    }

    @Test
    @DisabledIfSystemProperty(named = "arquillian.quarkus.nativeImage", matches = "true", disabledReason = JAR_TLD_NATIVE)
    void teiRunsInApplicationBeforeFirstRequestOnly() throws Exception {
        // The page reads and resets the flag the TEI sets
        assertEquals("true", get("flag.jsp").body().trim());
        assertEquals("false", get("flag.jsp").body().trim());
    }

    @Test
    void tldByPathWithoutCatalogueEntry() throws Exception {
        HttpResponse<String> response = get("direct.jsp");
        assertEquals(200, response.statusCode(), response.body());
        assertEquals("true", response.body().trim());
    }

    @Test
    @DisabledIfSystemProperty(named = "arquillian.quarkus.nativeImage", matches = "true", disabledReason = JAR_TLD_NATIVE)
    void tagDirectoryInWebInf() throws Exception {
        // Parsing the page lists the tag files in WEB-INF/tags
        HttpResponse<String> response = get("tagdir.jsp");
        assertEquals(200, response.statusCode(), response.body());
        assertEquals("Hello tagdir|true", response.body().trim());
    }

    @Test
    void teiInTagFileRunsInApplication() throws Exception {
        // The page doesn't use the tag library with the TEI; the tag file it uses does. Natively, the page isn't
        // validated again, as the tag library descriptor is in a jar, but it's served.
        HttpResponse<String> response = get("tagfile.jsp");
        assertEquals(200, response.statusCode(), response.body());
        assertEquals(Boolean.getBoolean("arquillian.quarkus.nativeImage") ? "flagged|false" : "flagged|true",
                response.body().trim());
    }

    @Test
    @DisabledIfSystemProperty(named = "arquillian.quarkus.nativeImage", matches = "true", disabledReason = JAR_TLD_NATIVE)
    void rejectedPageKeepsFailing() throws Exception {
        assertEquals(500, get("reject.jsp").statusCode());
        assertEquals(500, get("reject.jsp").statusCode());
        assertEquals(500, get("reject.jsp?jsp_precompile").statusCode());
    }

    @Test
    @DisabledIfSystemProperty(named = "arquillian.quarkus.nativeImage", matches = "true", disabledReason = JAR_TLD_NATIVE)
    void rejectedLoadOnStartupPageFailsWithoutFailingStartup() throws Exception {
        assertEquals(500, get("rejectStartup").statusCode());
        assertEquals(500, get("rejectStartup").statusCode());
    }

    @Test
    @DisabledIfSystemProperty(named = "arquillian.quarkus.nativeImage", matches = "true", disabledReason = JAR_TLD_NATIVE)
    void failingTlvInitializationFailsPage() throws Exception {
        assertEquals(500, get("throwing.jsp").statusCode());
        assertEquals(500, get("throwing.jsp").statusCode());
    }

    @Test
    @DisabledIfSystemProperty(named = "arquillian.quarkus.nativeImage", matches = "true", disabledReason = JAR_TLD_NATIVE)
    void validatedOncePerPageWithInstancesOfItsOwn() throws Exception {
        // Concurrent first requests of one page share one validation
        List<HttpResponse<String>> counts = getConcurrently(List.of("count.jsp", "count.jsp", "count.jsp", "count.jsp", "count.jsp"));
        counts.forEach(response -> assertEquals(200, response.statusCode(), response.body()));

        // Concurrent first requests of two pages of the same tag library each validate with their own TLV
        List<HttpResponse<String>> tlvs = getConcurrently(List.of("tlv-a.jsp", "tlv-b.jsp", "tlv-a.jsp", "tlv-b.jsp"));
        tlvs.forEach(response -> assertEquals(200, response.statusCode(), response.body()));

        // TEI validations | TLV validations | TLV instances
        assertEquals("1|2|2", get("counts.jsp").body().trim());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return Http.get(base, path);
    }

    private List<HttpResponse<String>> getConcurrently(List<String> paths) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        var baseUri = base.toURI();
        List<CompletableFuture<HttpResponse<String>>> responses = IntStream.range(0, paths.size())
                .mapToObj(i -> client.sendAsync(HttpRequest.newBuilder(baseUri.resolve(paths.get(i))).build(), BodyHandlers.ofString()))
                .toList();

        return responses.stream().map(CompletableFuture::join).toList();
    }
}
