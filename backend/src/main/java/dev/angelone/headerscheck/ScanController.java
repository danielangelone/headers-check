package dev.angelone.headerscheck;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ScanController {

    private final ScanService service;
    private final RateLimiter limiter;

    ScanController(ScanService service, RateLimiter limiter) {
        this.service = service;
        this.limiter = limiter;
    }

    @GetMapping("/api/scan")
    Report scan(@RequestParam String url, HttpServletRequest request) throws IOException, InterruptedException {
        // TODO: behind a reverse proxy this needs the forwarded address instead
        if (!limiter.allow(request.getRemoteAddr())) {
            throw new TooManyScansException();
        }
        return service.scan(url);
    }
}
