package org.openmrs.module.fua.web.controller;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.fua.web.utils.FuaAccess;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;

import static org.junit.Assert.*;

public class FuaControllerAccessTest {

    @After
    public void clearContext() {
        Context.clearUserContext();
    }

    @Test
    public void everyMappedActionRejectsAnonymousAndUnprivilegedRequestsBeforeAnyWork() throws Exception {
        Object[] controllers = { new FuaController(), new FuaEstadoController(), new FuaRedirectionController() };
        Map<String, String> operations = new HashMap<>();
        for (String name : new String[] { "onGet", "getAllFuas", "getFuaByUuid", "getFuasByPatientUuid",
                "getFuaById", "getSolicitudesFUA", "renderFua", "generateFuaPDF", "getAllEstados",
                "redirectFuaFormatGetRequest", "redirectFuaFormatRenderRequest" }) {
            operations.put(name, "Read Fua");
        }
        for (String name : new String[] { "onPost", "renderVisitInfo", "generateFuaFromVisit",
                "createEstado", "redirectFuaRequest" }) {
            operations.put(name, "Manage Fua");
        }
        operations.put("actualizarEstadoFua", "Update Fua");
        int mapped = 0;
        for (Object controller : controllers) {
            for (Method method : controller.getClass().getDeclaredMethods()) {
                if (method.getAnnotation(RequestMapping.class) == null) {
                    continue;
                }
                mapped++;
                assertNotNull("Every mapped operation needs an explicit access contract", operations.get(method.getName()));
                denied(controller, method, false, false, operations.get(method.getName()));
                denied(controller, method, true, false, operations.get(method.getName()));
                if ("onPost".equals(method.getName())) {
                    denied(controller, method, true, true, "Delete Fua");
                }
            }
        }
        assertEquals(19, mapped);
    }

    @Test
    public void clinicalWriteActionsAlsoRequireReadBeforeTouchingRecords() throws Exception {
        Context.setUserContext(new UserContext(null) {
            @Override
            public User getAuthenticatedUser() { return new User(100); }
            @Override
            public boolean isAuthenticated() { return true; }
            @Override
            public boolean hasPrivilege(String privilege) {
                return "Manage Fua".equals(privilege) || "Update Fua".equals(privilege);
            }
        });
        FuaController controller = new FuaController();
        Method[] methods = {
            FuaController.class.getMethod("generateFuaFromVisit", String.class),
            FuaController.class.getMethod("renderVisitInfo", String.class, String.class),
            FuaController.class.getMethod("actualizarEstadoFua", Integer.class, Map.class)
        };
        for (Method method : methods) {
            try {
                method.invoke(controller, new Object[method.getParameterCount()]);
                fail("Clinical writes must reject missing read access before dependencies are called");
            } catch (InvocationTargetException expected) {
                assertEquals(FuaAccess.AccessDenied.class, expected.getCause().getClass());
            }
        }
    }

    private void denied(Object controller, Method method, final boolean authenticated,
            boolean purge, final String expectedPrivilege) throws Exception {
        Context.setUserContext(new UserContext(null) {
            @Override
            public User getAuthenticatedUser() {
                return authenticated ? new User(100) : null;
            }

            @Override
            public boolean isAuthenticated() {
                return authenticated;
            }

            @Override
            public boolean hasPrivilege(String privilege) {
                assertEquals(expectedPrivilege, privilege);
                return false;
            }
        });
        Object[] arguments = new Object[method.getParameterCount()];
        for (int i = 0; i < arguments.length; i++) {
            Class<?> type = method.getParameterTypes()[i];
            arguments[i] = type == int.class ? Integer.valueOf(1) : type == String.class && purge ? "purge" : null;
        }
        try {
            method.invoke(controller, arguments);
            fail("Mapped action must reject before services, payload handling or HTTP");
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            assertEquals(authenticated ? FuaAccess.AccessDenied.class : FuaAccess.AuthenticationRequired.class,
                    cause.getClass());
            MockHttpServletResponse response = new MockHttpServletResponse();
            new ResponseStatusExceptionResolver().resolveException(new MockHttpServletRequest(), response,
                    controller, (Exception) cause);
            assertEquals(authenticated ? 403 : 401, response.getStatus());
        }
    }
}
