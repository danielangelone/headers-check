package dev.angelone.headerscheck;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.angelone.headerscheck.Report.Check;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GradingTest {

    private static final URI SITE = URI.create("https://example.com/");

    private static Map<String, List<String>> goodHeaders() {
        Map<String, List<String>> h = new LinkedHashMap<>();
        h.put("Strict-Transport-Security", List.of("max-age=31536000; includeSubDomains"));
        h.put("Content-Security-Policy", List.of("default-src 'self'; frame-ancestors 'none'"));
        h.put("X-Content-Type-Options", List.of("nosniff"));
        h.put("Referrer-Policy", List.of("no-referrer"));
        h.put("Permissions-Policy", List.of("camera=()"));
        return h;
    }

    private static String grade(Map<String, List<String>> headers) {
        List<Check> checks = HeaderChecks.run(SITE, HttpHeaders.of(headers, (a, b) -> true));
        return Grader.grade(checks);
    }

    @Test
    void allGoodHeadersGiveA() {
        assertEquals("A", grade(goodHeaders()));
    }

    @Test
    void noHeadersGiveF() {
        assertEquals("F", grade(new LinkedHashMap<>()));
    }

    @Test
    void oneWarnIsStillA() {
        Map<String, List<String>> h = goodHeaders();
        h.put("Content-Security-Policy", List.of("default-src 'self' 'unsafe-inline'; frame-ancestors 'none'"));
        assertEquals("A", grade(h));
    }

    @Test
    void missingCspDropsToB() {
        Map<String, List<String>> h = goodHeaders();
        h.remove("Content-Security-Policy");
        h.put("X-Frame-Options", List.of("DENY"));
        assertEquals("B", grade(h));
    }

    @Test
    void shortHstsIsAWarning() {
        Map<String, List<String>> h = goodHeaders();
        h.put("Strict-Transport-Security", List.of("max-age=3600"));
        List<Check> checks = HeaderChecks.run(SITE, HttpHeaders.of(h, (a, b) -> true));
        assertEquals(Report.Result.WARN, checks.get(0).result());
    }

    @Test
    void cookieWithoutSecureOnHttpsFails() {
        Map<String, List<String>> h = goodHeaders();
        h.put("Set-Cookie", List.of("sid=abc; HttpOnly; SameSite=Lax"));
        List<Check> checks = HeaderChecks.run(SITE, HttpHeaders.of(h, (a, b) -> true));
        List<Check> cookies = new ArrayList<>(checks.stream().filter(c -> c.id().startsWith("cookie-")).toList());
        assertEquals(Report.Result.FAIL, cookies.get(0).result());
        assertEquals("B", Grader.grade(checks));
    }

    @Test
    void serverVersionLeakIsAWarning() {
        Map<String, List<String>> h = goodHeaders();
        h.put("Server", List.of("Apache/2.4.41"));
        List<Check> checks = HeaderChecks.run(SITE, HttpHeaders.of(h, (a, b) -> true));
        Check leak = checks.stream().filter(c -> c.id().equals("leak-server")).findFirst().orElseThrow();
        assertEquals(Report.Result.WARN, leak.result());
    }
}
