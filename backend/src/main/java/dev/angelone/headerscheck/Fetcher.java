package dev.angelone.headerscheck;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.stereotype.Component;

@Component
class Fetcher {

    static final int MAX_BODY = 512 * 1024;
    static final int MAX_REDIRECTS = 3;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    record Fetched(URI finalUri, int status, HttpHeaders headers, String body) {
    }

    // Redirects are followed by hand (see get) so every hop goes through the address guard.
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    /** GET with the address guard applied to the start URL and to every redirect target. */
    Fetched get(URI start, int maxRedirects, int maxBytes) throws IOException, InterruptedException {
        URI current = start;
        for (int hop = 0; ; hop++) {
            AddressGuard.check(current);
            HttpRequest request = HttpRequest.newBuilder(current)
                    .timeout(TIMEOUT)
                    .header("User-Agent", "headers-check/0.1")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();
            String location = response.headers().firstValue("Location").orElse(null);
            if (status >= 300 && status < 400 && location != null && hop < maxRedirects) {
                closeQuietly(response.body());
                try {
                    current = current.resolve(location.trim());
                } catch (IllegalArgumentException e) {
                    throw new IOException("invalid redirect location");
                }
                continue;
            }
            return new Fetched(current, status, response.headers(), readLimited(response.body(), maxBytes));
        }
    }

    // The client timeout stops at the response headers, so the body read gets its own deadline.
    private static String readLimited(InputStream in, int maxBytes) throws IOException, InterruptedException {
        CompletableFuture<byte[]> read = CompletableFuture.supplyAsync(() -> {
            try (in) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int n;
                while (out.size() < maxBytes && (n = in.read(buffer, 0, Math.min(buffer.length, maxBytes - out.size()))) != -1) {
                    out.write(buffer, 0, n);
                }
                return out.toByteArray();
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        });
        try {
            return new String(read.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS), StandardCharsets.UTF_8);
        } catch (TimeoutException e) {
            closeQuietly(in);
            throw new IOException("timed out reading the response");
        } catch (ExecutionException e) {
            throw new IOException("could not read the response", e.getCause());
        }
    }

    private static void closeQuietly(InputStream in) {
        try {
            in.close();
        } catch (IOException ignored) {
        }
    }
}
