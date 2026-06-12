package org.example.openapi.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class OpenApiIpHelper {

    private static final Pattern IPV4 = Pattern.compile("^(\\d{1,3}\\.){3}\\d{1,3}$");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OpenApiIpHelper() {
    }

    public static String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return normalizeIp(request.getRemoteAddr());
    }

    public static String normalizeIp(String ip) {
        if (ip == null) {
            return "";
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }

    public static List<String> parseAllowedIps(String allowedIpsJson) {
        if (!StringUtils.hasText(allowedIpsJson)) {
            return Collections.emptyList();
        }
        try {
            List<String> list = MAPPER.readValue(allowedIpsJson, new TypeReference<List<String>>() {});
            List<String> normalized = new ArrayList<>();
            for (String ip : list) {
                if (StringUtils.hasText(ip)) {
                    normalized.add(normalizeIp(ip.trim()));
                }
            }
            return normalized;
        } catch (Exception e) {
            throw new RuntimeException("允许 IP 必须是合法的 JSON 数组格式");
        }
    }

    public static void validateAllowedIpsJson(String allowedIpsJson) {
        if (!StringUtils.hasText(allowedIpsJson)) {
            return;
        }
        List<String> ips = parseAllowedIps(allowedIpsJson);
        for (String ip : ips) {
            if (!isValidIp(ip)) {
                throw new RuntimeException("无效的 IP 地址: " + ip);
            }
        }
    }

    public static boolean isIpAllowed(String allowedIpsJson, String clientIp) {
        List<String> allowed = parseAllowedIps(allowedIpsJson);
        if (allowed.isEmpty()) {
            return true;
        }
        String normalizedClient = normalizeIp(clientIp);
        return allowed.contains(normalizedClient);
    }

    private static boolean isValidIp(String ip) {
        if (!IPV4.matcher(ip).matches()) {
            return false;
        }
        String[] parts = ip.split("\\.");
        for (String part : parts) {
            int n = Integer.parseInt(part);
            if (n < 0 || n > 255) {
                return false;
            }
        }
        return true;
    }
}
