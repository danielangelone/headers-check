package dev.angelone.headerscheck;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
class RateLimiter {

    private static final int LIMIT = 10;
    private static final long WINDOW_MS = 60_000;

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    boolean allow(String client) {
        long now = System.currentTimeMillis();
        if (hits.size() > 10_000) {
            hits.values().removeIf(d -> d.isEmpty() || now - d.peekLast() > WINDOW_MS);
        }
        Deque<Long> window = hits.computeIfAbsent(client, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() > WINDOW_MS) {
                window.pollFirst();
            }
            if (window.size() >= LIMIT) {
                return false;
            }
            window.addLast(now);
            return true;
        }
    }
}
