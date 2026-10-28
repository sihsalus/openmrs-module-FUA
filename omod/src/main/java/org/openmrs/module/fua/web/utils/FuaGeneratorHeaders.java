package org.openmrs.module.fua.web.utils;

import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fua.FuaConfig;
import org.springframework.http.HttpHeaders;

/** Build generator authentication consistently, without defaults or secret logging. */
public final class FuaGeneratorHeaders {

	private FuaGeneratorHeaders() {
	}

	public static HttpHeaders create() {
		return create(Context.getAdministrationService());
	}

	static HttpHeaders create(AdministrationService administration) {
        String name = administration.getGlobalProperty(FuaConfig.FUA_GENERATOR_HEADER_NAME_GP);
        String value = administration.getGlobalProperty(FuaConfig.FUA_GENERATOR_HEADER_VALUE_GP);
        if (name == null || !name.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+")
                || "Content-Type".equalsIgnoreCase(name) || "Content-Length".equalsIgnoreCase(name)
                || "Transfer-Encoding".equalsIgnoreCase(name) || "Host".equalsIgnoreCase(name)
                || value == null || value.trim().isEmpty() || !value.equals(value.trim())
                || value.chars().anyMatch(c -> c < 32 || c > 126)) {
            throw new IllegalStateException("FUA generator authentication is not configured correctly");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(name, value);
        return headers;
    }
}
