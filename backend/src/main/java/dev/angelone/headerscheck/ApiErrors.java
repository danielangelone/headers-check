package dev.angelone.headerscheck;

import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiErrors {

    @ExceptionHandler({BlockedException.class, MissingServletRequestParameterException.class})
    ResponseEntity<Map<String, String>> badRequest(Exception e) {
        String message = e instanceof BlockedException ? e.getMessage() : "the url parameter is required";
        return reply(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(TooManyScansException.class)
    ResponseEntity<Map<String, String>> tooMany(TooManyScansException e) {
        return reply(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler(IOException.class)
    ResponseEntity<Map<String, String>> unreachable(IOException e) {
        String reason = e instanceof java.net.UnknownHostException ? "could not resolve the host" : "could not fetch the site";
        return reply(HttpStatus.BAD_GATEWAY, reason);
    }

    private static ResponseEntity<Map<String, String>> reply(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
