package dev.angelone.headerscheck;

import dev.angelone.headerscheck.Fetcher.Fetched;
import dev.angelone.headerscheck.Report.Check;
import dev.angelone.headerscheck.Report.Result;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
class ScanService {

    private final Fetcher fetcher;
    private final WordPressCheck wordpress;

    ScanService(Fetcher fetcher, WordPressCheck wordpress) {
        this.fetcher = fetcher;
        this.wordpress = wordpress;
    }

    Report scan(String url) throws IOException, InterruptedException {
        URI start = parse(url);
        // fails fast with BlockedException before anything is sent
        AddressGuard.check(start);

        Fetched page = fetcher.get(start, Fetcher.MAX_REDIRECTS, Fetcher.MAX_BODY);

        List<Check> checks = new ArrayList<>();
        checks.add(httpsRedirect(start, page));
        checks.addAll(HeaderChecks.run(page.finalUri(), page.headers()));

        return new Report(url, page.finalUri().toString(), page.status(), Grader.grade(checks), checks,
                wordpress.run(page));
    }

    private static URI parse(String url) {
        try {
            return URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            throw new BlockedException("not a valid URL");
        }
    }

    private Check httpsRedirect(URI start, Fetched page) throws InterruptedException {
        String label = "HTTP to HTTPS redirect";
        if ("http".equalsIgnoreCase(start.getScheme())) {
            boolean upgraded = "https".equalsIgnoreCase(page.finalUri().getScheme());
            return new Check("https-redirect", label, upgraded ? Result.PASS : Result.FAIL,
                    upgraded ? "redirects to https" : "stays on plain http");
        }
        // TODO: only probes port 80, a site on a custom https port is not checked
        try {
            URI plain = URI.create("http://" + start.getRawAuthority().replaceFirst(":\\d+$", "") + "/");
            Fetched probe = fetcher.get(plain, 0, 1024);
            String location = probe.headers().firstValue("Location").orElse("");
            boolean redirects = probe.status() >= 300 && probe.status() < 400
                    && location.toLowerCase().startsWith("https://");
            return new Check("https-redirect", label, redirects ? Result.PASS : Result.FAIL,
                    redirects ? "http redirects to https" : "http answers without redirecting to https");
        } catch (IOException e) {
            return new Check("https-redirect", label, Result.PASS, "port 80 is not reachable");
        }
    }
}
