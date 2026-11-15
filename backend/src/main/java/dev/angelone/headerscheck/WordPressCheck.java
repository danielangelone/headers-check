package dev.angelone.headerscheck;

import dev.angelone.headerscheck.Fetcher.Fetched;
import dev.angelone.headerscheck.Report.Wordpress;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
class WordPressCheck {

    private static final Pattern GENERATOR = Pattern.compile(
            "<meta[^>]*name=[\"']generator[\"'][^>]*content=[\"']WordPress\\s*([\\d.]*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern GENERATOR_REVERSED = Pattern.compile(
            "<meta[^>]*content=[\"']WordPress\\s*([\\d.]*)[^>]*name=[\"']generator[\"']", Pattern.CASE_INSENSITIVE);

    private final Fetcher fetcher;

    WordPressCheck(Fetcher fetcher) {
        this.fetcher = fetcher;
    }

    Wordpress run(Fetched page) throws InterruptedException {
        String html = page.body();
        Matcher m = GENERATOR.matcher(html);
        boolean generator = m.find();
        if (!generator) {
            m = GENERATOR_REVERSED.matcher(html);
            generator = m.find();
        }
        boolean detected = generator || html.contains("/wp-content/");
        if (!detected) {
            return new Wordpress(false, null, List.of());
        }
        String version = generator && !m.group(1).isEmpty() ? m.group(1) : null;

        List<String> findings = new ArrayList<>();
        if (version != null) {
            findings.add("version " + version + " is advertised in the generator tag");
        }
        if (answers200(page.finalUri(), "/readme.html")) {
            findings.add("readme.html is public");
        }
        if (answers200(page.finalUri(), "/xmlrpc.php")) {
            findings.add("xmlrpc.php answers 200");
        }
        if (answers200(page.finalUri(), "/wp-json/wp/v2/users")) {
            findings.add("user list is public at /wp-json/wp/v2/users");
        }
        return new Wordpress(true, version, findings);
    }

    private boolean answers200(URI site, String path) throws InterruptedException {
        try {
            URI target = URI.create(site.getScheme() + "://" + site.getRawAuthority() + path);
            // no redirects here: a redirect to the home page is not the file being exposed
            return fetcher.get(target, 0, 1024).status() == 200;
        } catch (IOException e) {
            return false;
        }
    }
}
