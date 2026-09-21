package org.openmrs.module.fua;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.User;
import org.openmrs.aop.AuthorizationAdvice;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.api.context.UserContext;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.fua.api.FuaService;

import static org.junit.Assert.*;

/** Exercise OpenMRS's actual service authorization advice with synthetic grants. */
public class FuaAuthorizationTest {

    private MessageSourceService previousMessages;
    private final Set<String> grants = new HashSet<>();

    @Before
    public void setUp() {
        previousMessages = ServiceContext.getInstance().getMessageSourceService();
        MessageSourceService messages = (MessageSourceService) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] { MessageSourceService.class },
                (proxy, method, args) -> method.getReturnType() == String.class ? "Not authorized" : null);
        ServiceContext.getInstance().setMessageSourceService(messages);
        Context.setUserContext(new UserContext(null) {
            @Override
            public User getAuthenticatedUser() {
                return new User(100);
            }

            @Override
            public boolean hasPrivilege(String privilege) {
                return grants.contains(privilege);
            }
        });
    }

    @After
    public void tearDown() {
        Context.clearUserContext();
        ServiceContext.getInstance().setMessageSourceService(previousMessages);
    }

    @Test
    public void nativeAdviceRequiresEachCanonicalPrivilegeIndependently() throws Throwable {
        String[] names = { "Read Fua", "Manage Fua", "Update Fua", "Delete Fua" };
        Method[] methods = {
            FuaService.class.getMethod("getAllFuas"),
            FuaService.class.getMethod("saveFua", Fua.class),
            FuaService.class.getMethod("updateEstadoFua", Integer.class, FuaEstado.class),
            FuaService.class.getMethod("purgeFua", Fua.class)
        };
        AuthorizationAdvice advice = new AuthorizationAdvice();
        for (int i = 0; i < names.length; i++) {
            for (String granted : names) {
                grants.clear();
                grants.add(granted);
                try {
                    advice.before(methods[i], new Object[methods[i].getParameterCount()], new Object());
                    assertEquals("An unrelated grant must not authorize an operation", names[i], granted);
                } catch (APIAuthenticationException denied) {
                    assertNotEquals("Canonical configured grant must authorize its operation", names[i], granted);
                }
            }
            grants.clear();
            grants.add(names[i] + " Privilege");
            try {
                advice.before(methods[i], new Object[methods[i].getParameterCount()], new Object());
                fail("An unregistered suffixed alias must not authorize the operation");
            } catch (APIAuthenticationException expected) {
                // The module must use the privilege names already provisioned in config.xml.
            }
        }
    }
}
