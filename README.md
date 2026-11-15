# headers-check

Paste a URL and get a report of its HTTP security headers, cookie flags and obvious
information leaks, with a grade from A to F. It also detects WordPress and flags the usual exposures.

## Run

    docker build -t headers-check .
    docker run --rm -p 8080:8080 headers-check

Then open http://localhost:8080. The API is `GET /api/scan?url=https://example.com`.
For development run `mvn spring-boot:run` in `backend/` and `npm run dev` in `frontend/`.

## Checks

- Strict-Transport-Security (WARN under six months), Content-Security-Policy (WARN on `unsafe-inline`)
- X-Frame-Options or CSP `frame-ancestors`, X-Content-Type-Options, Referrer-Policy, Permissions-Policy
- http to https redirect
- `Secure`, `HttpOnly` and `SameSite` on every Set-Cookie
- version numbers in `Server` and `X-Powered-By`
- WordPress: generator tag or `/wp-content/`, then `/readme.html`, `/xmlrpc.php`, `/wp-json/wp/v2/users`

The grade starts at 100 and loses fixed points per FAIL and WARN (weights in `Grader`).

## SSRF guard

Only http and https. The host is resolved and loopback, private, link-local, multicast and
similar addresses are rejected with 400. Redirects are followed by hand, at most 3, and the address
is checked again on every hop. Timeouts are 5 seconds, bodies are cut at 512 KB, and each client IP
gets 10 scans per minute (429 above that).

## What is missing

The host is resolved once for the check and again by the HTTP client, so a DNS rebinding
attack can still slip through. The rate limit uses the socket address, not `X-Forwarded-For`.
There are no frontend tests and no scan history.
