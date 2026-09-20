package com.selfdiscipline.util;

import com.selfdiscipline.exception.ApiException;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

public final class RemoteUrlGuard {

    @FunctionalInterface
    public interface HostResolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    private static final Set<String> BLOCKED_HOSTS = Set.of(
            "localhost",
            "metadata.google.internal",
            "metadata.google.com"
    );

    private RemoteUrlGuard() {
    }

    public static URI assertSafeHttpUrl(String raw) {
        return assertSafeHttpUrl(raw, InetAddress::getAllByName);
    }

    public static URI assertSafeHttpUrl(String raw, HostResolver resolver) {
        if (raw == null || raw.isBlank()) {
            throw ApiException.badRequest("导入地址不能为空");
        }
        URI uri;
        try {
            uri = URI.create(raw.trim());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("导入地址无效");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw ApiException.badRequest("只允许 http/https 导入地址");
        }
        if (uri.getUserInfo() != null && !uri.getUserInfo().isBlank()) {
            throw ApiException.badRequest("导入地址不允许包含用户信息");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw ApiException.badRequest("导入地址缺少主机名");
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (normalizedHost.endsWith(".")) {
            normalizedHost = normalizedHost.substring(0, normalizedHost.length() - 1);
        }
        if (BLOCKED_HOSTS.contains(normalizedHost) || normalizedHost.endsWith(".local")) {
            throw ApiException.badRequest("不允许访问该主机");
        }
        InetAddress[] addresses;
        try {
            addresses = resolver.resolve(normalizedHost);
        } catch (UnknownHostException e) {
            throw ApiException.badRequest("无法解析导入主机");
        }
        if (addresses == null || addresses.length == 0) {
            throw ApiException.badRequest("无法解析导入主机");
        }
        for (InetAddress address : addresses) {
            if (isBlockedAddress(address)) {
                throw ApiException.badRequest("不允许访问内网或回环地址");
            }
        }
        return uri;
    }

    static boolean isBlockedAddress(InetAddress address) {
        if (address == null) {
            return true;
        }
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int a0 = bytes[0] & 0xff;
            int a1 = bytes[1] & 0xff;
            if (a0 == 0 || a0 == 10 || a0 == 127) {
                return true;
            }
            if (a0 == 169 && a1 == 254) {
                return true;
            }
            if (a0 == 172 && a1 >= 16 && a1 <= 31) {
                return true;
            }
            if (a0 == 192 && a1 == 168) {
                return true;
            }
            if (a0 == 100 && a1 >= 64 && a1 <= 127) {
                return true;
            }
        }
        if (bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc) {
            return true;
        }
        return false;
    }
}
