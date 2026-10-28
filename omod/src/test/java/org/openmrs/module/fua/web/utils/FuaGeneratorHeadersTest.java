package org.openmrs.module.fua.web.utils;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openmrs.api.AdministrationService;
import org.openmrs.module.fua.FuaConfig;
import org.springframework.http.HttpHeaders;

import static org.junit.Assert.*;

public class FuaGeneratorHeadersTest {

    private AdministrationService configuration(String name, String value) {
        Map<String, String> properties = new HashMap<>();
        properties.put(FuaConfig.FUA_GENERATOR_HEADER_NAME_GP, name);
        properties.put(FuaConfig.FUA_GENERATOR_HEADER_VALUE_GP, value);
        return (AdministrationService) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { AdministrationService.class }, (proxy, method, args) -> {
                    if ("getGlobalProperty".equals(method.getName())) {
                        return properties.get(args[0]);
                    }
                    throw new AssertionError("Unexpected administration call");
                });
    }

    @Test
    public void usesConfiguredHeaderAndValueWithoutStaleCaching() {
        HttpHeaders first = FuaGeneratorHeaders.create(configuration("X-Synthetic-Fua", "synthetic-first"));
        HttpHeaders rotated = FuaGeneratorHeaders.create(configuration("Authorization", "Bearer synthetic-next"));
        assertEquals("synthetic-first", first.getFirst("X-Synthetic-Fua"));
        assertEquals(1, first.size());
        assertEquals("Bearer synthetic-next", rotated.getFirst("Authorization"));
        assertFalse(rotated.containsKey("X-Synthetic-Fua"));
    }

    @Test
    public void refusesMissingOrInjectedAuthenticationWithoutLeakingConfiguration() {
        String[][] invalid = {
            { null, "synthetic-secret" }, { "", "synthetic-secret" }, { "X-Key", null },
            { "X-Key", "" }, { "X-Key", "  " }, { "X-Key", " synthetic-secret " },
            { "X-Key\r\nInjected", "synthetic-secret" }, { "X Key", "synthetic-secret" },
            { "Content-Type", "synthetic-secret" }, { "content-length", "synthetic-secret" },
            { "Transfer-Encoding", "synthetic-secret" }, { "Host", "synthetic-secret" },
            { "X-Key", "synthetic-secret\r\nInjected: yes" }, { "X-Key", "synthetic-secret\0" }
        };
        for (String[] values : invalid) {
            try {
                FuaGeneratorHeaders.create(configuration(values[0], values[1]));
                fail("Invalid generator authentication must stop before HTTP");
            } catch (IllegalStateException expected) {
                assertEquals("FUA generator authentication is not configured correctly", expected.getMessage());
                assertNull(expected.getCause());
            }
        }
    }
}
