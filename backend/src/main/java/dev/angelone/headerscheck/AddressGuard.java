package dev.angelone.headerscheck;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

final class AddressGuard {

    private AddressGuard() {
    }

    /** Throws BlockedException unless the URL is http(s) and every address of its host is public. */
    static void check(URI uri) throws UnknownHostException {
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new BlockedException("only http and https URLs are allowed");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new BlockedException("URL has no host");
        }
        for (InetAddress address : InetAddress.getAllByName(host)) {
            if (!isPublic(address)) {
                throw new BlockedException("address " + address.getHostAddress() + " is not allowed");
            }
        }
    }

    static boolean isPublic(InetAddress a) {
        if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
                || a.isSiteLocalAddress() || a.isMulticastAddress()) {
            return false;
        }
        byte[] b = a.getAddress();
        if (b.length == 4) {
            int first = b[0] & 0xff;
            int second = b[1] & 0xff;
            boolean thisNetwork = first == 0;
            boolean carrierNat = first == 100 && second >= 64 && second <= 127;
            return !thisNetwork && !carrierNat;
        }
        // fc00::/7, unique local addresses; Java does not flag these as site local
        return (b[0] & 0xfe) != 0xfc;
    }
}
