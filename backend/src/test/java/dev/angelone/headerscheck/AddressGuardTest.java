package dev.angelone.headerscheck;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.URI;
import org.junit.jupiter.api.Test;

class AddressGuardTest {

    private static boolean isPublic(String literal) throws Exception {
        return AddressGuard.isPublic(InetAddress.getByName(literal));
    }

    @Test
    void publicAddressIsAllowed() throws Exception {
        assertTrue(isPublic("93.184.216.34"));
        assertTrue(isPublic("2606:2800:220:1:248:1893:25c8:1946"));
    }

    @Test
    void loopbackIsRejected() throws Exception {
        assertFalse(isPublic("127.0.0.1"));
        assertFalse(isPublic("::1"));
    }

    @Test
    void privateRangesAreRejected() throws Exception {
        assertFalse(isPublic("10.1.2.3"));
        assertFalse(isPublic("192.168.0.10"));
        assertFalse(isPublic("172.16.5.5"));
        assertFalse(isPublic("fd00::1"));
    }

    @Test
    void linkLocalIsRejected() throws Exception {
        assertFalse(isPublic("169.254.169.254"));
        assertFalse(isPublic("fe80::1"));
    }

    @Test
    void mappedIpv4LoopbackIsRejected() throws Exception {
        assertFalse(isPublic("::ffff:127.0.0.1"));
    }

    @Test
    void checkRejectsBadSchemesAndLiteralHosts() {
        assertThrows(BlockedException.class, () -> AddressGuard.check(URI.create("file:///etc/passwd")));
        assertThrows(BlockedException.class, () -> AddressGuard.check(URI.create("http://127.0.0.1/")));
        assertThrows(BlockedException.class, () -> AddressGuard.check(URI.create("http://[::1]:8080/")));
    }
}
