package dev.angelone.headerscheck;

/** The requested URL is not something we are willing to fetch. */
public class BlockedException extends RuntimeException {

    public BlockedException(String message) {
        super(message);
    }
}
