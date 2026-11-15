package dev.angelone.headerscheck;

import dev.angelone.headerscheck.Report.Check;
import dev.angelone.headerscheck.Report.Result;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class HeaderChecks {

    private static final long SIX_MONTHS = 15_552_000L;
    private static final Pattern MAX_AGE = Pattern.compile("max-age=\"?(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern VERSION = Pattern.compile("\\d+\\.\\d+|/\\d");

    private HeaderChecks() {
    }

    static List<Check> run(URI finalUri, HttpHeaders headers) {
        boolean https = "https".equalsIgnoreCase(finalUri.getScheme());
        List<Check> checks = new ArrayList<>();
        if (https) {
            checks.add(hsts(headers));
        }
        checks.add(csp(headers));
        checks.add(framing(headers));
        checks.add(nosniff(headers));
        checks.add(presence("referrer", "Referrer-Policy", headers, Result.WARN));
        checks.add(presence("permissions", "Permissions-Policy", headers, Result.WARN));
        checks.addAll(cookies(headers, https));
        checks.add(leak("leak-server", "Server", headers));
        checks.add(leak("leak-powered-by", "X-Powered-By", headers));
        return checks;
    }

    private static Check hsts(HttpHeaders h) {
        String value = h.firstValue("Strict-Transport-Security").orElse(null);
        if (value == null) {
            return new Check("hsts", "Strict-Transport-Security", Result.FAIL, "header is missing");
        }
        Matcher m = MAX_AGE.matcher(value);
        if (!m.find()) {
            return new Check("hsts", "Strict-Transport-Security", Result.FAIL, "no max-age in " + value);
        }
        Result result = Long.parseLong(m.group(1)) < SIX_MONTHS ? Result.WARN : Result.PASS;
        return new Check("hsts", "Strict-Transport-Security", result, value);
    }

    private static Check csp(HttpHeaders h) {
        String value = h.firstValue("Content-Security-Policy").orElse(null);
        if (value == null) {
            return new Check("csp", "Content-Security-Policy", Result.FAIL, "header is missing");
        }
        if (value.toLowerCase(Locale.ROOT).contains("unsafe-inline")) {
            return new Check("csp", "Content-Security-Policy", Result.WARN, "allows unsafe-inline");
        }
        return new Check("csp", "Content-Security-Policy", Result.PASS, "present");
    }

    private static Check framing(HttpHeaders h) {
        String label = "X-Frame-Options / frame-ancestors";
        String xfo = h.firstValue("X-Frame-Options").orElse(null);
        if (xfo != null) {
            return new Check("framing", label, Result.PASS, "X-Frame-Options: " + xfo);
        }
        String csp = h.firstValue("Content-Security-Policy").orElse("");
        if (csp.toLowerCase(Locale.ROOT).contains("frame-ancestors")) {
            return new Check("framing", label, Result.PASS, "CSP frame-ancestors");
        }
        return new Check("framing", label, Result.FAIL, "neither header is set");
    }

    private static Check nosniff(HttpHeaders h) {
        String value = h.firstValue("X-Content-Type-Options").orElse("");
        boolean ok = value.trim().equalsIgnoreCase("nosniff");
        return new Check("nosniff", "X-Content-Type-Options", ok ? Result.PASS : Result.FAIL,
                ok ? "nosniff" : "nosniff is not set");
    }

    private static Check presence(String id, String name, HttpHeaders h, Result whenMissing) {
        String value = h.firstValue(name).orElse(null);
        return value == null
                ? new Check(id, name, whenMissing, "header is missing")
                : new Check(id, name, Result.PASS, value);
    }

    private static List<Check> cookies(HttpHeaders h, boolean https) {
        List<Check> checks = new ArrayList<>();
        for (String cookie : h.allValues("Set-Cookie")) {
            String[] parts = cookie.split(";");
            String name = parts[0].split("=", 2)[0].trim();
            boolean secure = false;
            boolean httpOnly = false;
            boolean sameSite = false;
            for (int i = 1; i < parts.length; i++) {
                String attr = parts[i].trim().toLowerCase(Locale.ROOT);
                secure |= attr.equals("secure");
                httpOnly |= attr.equals("httponly");
                sameSite |= attr.startsWith("samesite=");
            }
            List<String> missing = new ArrayList<>();
            if (!secure) missing.add("Secure");
            if (!httpOnly) missing.add("HttpOnly");
            if (!sameSite) missing.add("SameSite");

            Result result = Result.PASS;
            if (!missing.isEmpty()) {
                result = (!secure && https) ? Result.FAIL : Result.WARN;
            }
            String detail = missing.isEmpty() ? "Secure, HttpOnly, SameSite set" : "missing " + String.join(", ", missing);
            checks.add(new Check("cookie-" + name, "Cookie " + name, result, detail));
        }
        return checks;
    }

    private static Check leak(String id, String name, HttpHeaders h) {
        String value = h.firstValue(name).orElse(null);
        if (value == null) {
            return new Check(id, name, Result.PASS, "not sent");
        }
        if (VERSION.matcher(value).find()) {
            return new Check(id, name, Result.WARN, "leaks a version: " + value);
        }
        return new Check(id, name, Result.PASS, value);
    }
}
