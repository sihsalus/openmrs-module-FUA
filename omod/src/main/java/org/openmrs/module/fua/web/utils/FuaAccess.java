package org.openmrs.module.fua.web.utils;

import org.openmrs.api.context.Context;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Enforce access before a controller reads clinical data or calls the generator. */
public final class FuaAccess {

    private FuaAccess() {
    }

    public static void require(String privilege) {
        if (!Context.isAuthenticated()) {
            throw new AuthenticationRequired();
        }
        if (!Context.hasPrivilege(privilege)) {
            throw new AccessDenied();
        }
    }

    @ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "Authentication required")
    public static final class AuthenticationRequired extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    @ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "Not authorized")
    public static final class AccessDenied extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
