package org.example.openapi.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiIpHelperTest {

    @Test
    void allowsWhenListEmpty() {
        assertTrue(OpenApiIpHelper.isIpAllowed(null, "127.0.0.1"));
        assertTrue(OpenApiIpHelper.isIpAllowed("[]", "10.0.0.1"));
    }

    @Test
    void checksAllowedIp() {
        assertTrue(OpenApiIpHelper.isIpAllowed("[\"127.0.0.1\"]", "127.0.0.1"));
        assertFalse(OpenApiIpHelper.isIpAllowed("[\"127.0.0.1\"]", "10.0.0.1"));
    }

    @Test
    void normalizesIpv6Loopback() {
        assertEquals("127.0.0.1", OpenApiIpHelper.normalizeIp("::1"));
    }
}
