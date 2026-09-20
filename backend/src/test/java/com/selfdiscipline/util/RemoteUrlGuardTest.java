package com.selfdiscipline.util;

import com.selfdiscipline.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteUrlGuardTest {

    @Test
    void rejectsNonHttpSchemes() {
        ApiException ex = assertThrows(ApiException.class,
                () -> RemoteUrlGuard.assertSafeHttpUrl("file:///etc/passwd"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void rejectsLoopbackAndPrivateLiterals() {
        assertThrows(ApiException.class, () -> RemoteUrlGuard.assertSafeHttpUrl("http://127.0.0.1/secret"));
        assertThrows(ApiException.class, () -> RemoteUrlGuard.assertSafeHttpUrl("http://localhost/admin"));
        assertThrows(ApiException.class, () -> RemoteUrlGuard.assertSafeHttpUrl("http://10.0.0.8/words.txt"));
        assertThrows(ApiException.class, () -> RemoteUrlGuard.assertSafeHttpUrl("http://169.254.169.254/latest/meta-data"));
        assertThrows(ApiException.class, () -> RemoteUrlGuard.assertSafeHttpUrl("http://192.168.1.10/csv"));
    }

    @Test
    void rejectsHostnamesThatResolveToPrivateAddresses() throws Exception {
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        ApiException ex = assertThrows(ApiException.class, () ->
                RemoteUrlGuard.assertSafeHttpUrl("https://evil.example/words.txt", host -> new InetAddress[]{loopback})
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("不允许"));
    }

    @Test
    void allowsPublicHttpsWhenResolverReturnsPublicIp() throws Exception {
        InetAddress publicIp = InetAddress.getByAddress(new byte[]{1, 2, 3, 4});
        URI uri = RemoteUrlGuard.assertSafeHttpUrl(
                "https://cdn.example.com/wordlist.txt",
                host -> {
                    assertEquals("cdn.example.com", host);
                    return new InetAddress[]{publicIp};
                }
        );
        assertEquals("cdn.example.com", uri.getHost());
    }

    @Test
    void rejectsUnresolvableHosts() {
        ApiException ex = assertThrows(ApiException.class, () ->
                RemoteUrlGuard.assertSafeHttpUrl("https://no-such-host.invalid/x", host -> {
                    throw new UnknownHostException(host);
                })
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
}
