package dev.angelone.headerscheck;

class TooManyScansException extends RuntimeException {

    TooManyScansException() {
        super("too many scans, try again in a minute");
    }
}
