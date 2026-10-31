package org.openmrs.module.fua.web.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.User;
import org.openmrs.aop.AuthorizationAdvice;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.fua.Fua;
import org.openmrs.module.fua.FuaConfig;
import org.openmrs.module.fua.FuaObservacion;
import org.openmrs.module.fua.api.FuaObservacionService;
import org.openmrs.module.fua.api.FuaService;
import org.openmrs.module.fua.api.dao.FuaDao;
import org.openmrs.module.fua.api.dao.FuaObservacionDao;
import org.openmrs.module.fua.api.impl.FuaObservacionServiceImpl;
import org.openmrs.module.fua.api.impl.FuaServiceImpl;
import org.openmrs.module.fua.web.utils.FuaAccess;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;

import static org.junit.Assert.*;

/** Exercises the native access helper and service authorization advice, without a database. */
public class FuaObservacionControllerAccessTest {

    private static final String FUA_UUID = "synthetic-fua";
    private static final String OBS_UUID = "synthetic-observation";

    private FuaObservacionController controller;
    private Fua fua;
    private TrackingObservation observation;
    private InMemoryObservationDao dao;
    private int fuaReads;

    @Before
    public void setUp() {
        fua = new Fua();
        fua.setUuid(FUA_UUID);
        fua.setVersion(2);
        observation = new TrackingObservation();
        observation.setUuid(OBS_UUID);
        observation.setFuaUuid(FUA_UUID);
        observation.setVersion(2);
        observation.setObservacion("original");
        observation.setUsuario("original actor");
        observation.setActivo(true);
        observation.setFechaActualizacion(null);
        observation.mutations = 0;
        dao = new InMemoryObservationDao();

        FuaServiceImpl fuaTarget = new FuaServiceImpl();
        fuaTarget.setDao(new FuaDao() {
            @Override
            public Fua getFuaByUuid(String uuid) {
                fuaReads++;
                return FUA_UUID.equals(uuid) ? fua : null;
            }
        });
        FuaObservacionServiceImpl observationTarget = new FuaObservacionServiceImpl();
        observationTarget.setDao(dao);
        controller = new FuaObservacionController();
        ReflectionTestUtils.setField(controller, "fuaService", authorizedProxy(FuaService.class, fuaTarget));
        ReflectionTestUtils.setField(controller, "fuaObservacionService",
                authorizedProxy(FuaObservacionService.class, observationTarget));
    }

    @After
    public void clearContext() {
        Context.clearUserContext();
    }

    @Test
    public void anonymousCannotEditOrDeleteBeforeAnyReadOrMutation() {
        actor(false);
        assertDenied(FuaAccess.AuthenticationRequired.class, 401);
    }

    @Test
    public void readerCannotEditOrDeleteBeforeAnyReadOrMutation() {
        actor(true, FuaConfig.READ_FUA_PRIVILEGE);
        assertDenied(FuaAccess.AccessDenied.class, 403);
    }

    @Test
    public void writerWithoutReadCannotEditOrDeleteBeforeAnyReadOrMutation() {
        actor(true, FuaConfig.UPDATE_FUA_PRIVILEGE);
        assertDenied(FuaAccess.AccessDenied.class, 403);
    }

    @Test
    public void writerCanEditCurrentObservationWithoutChangingIdentityOrVersion() {
        writer();
        ResponseEntity<?> response = controller.editarObservacion(FUA_UUID, OBS_UUID, body());
        assertEquals(200, response.getStatusCodeValue());
        assertSame(observation, response.getBody());
        assertEquals("updated", observation.getObservacion());
        assertEquals("payload actor", observation.getUsuario());
        assertNotNull(observation.getFechaActualizacion());
        assertEquals(OBS_UUID, observation.getUuid());
        assertEquals(FUA_UUID, observation.getFuaUuid());
        assertEquals(Integer.valueOf(2), observation.getVersion());
        assertTrue(observation.getActivo());
        assertEquals(1, dao.writes);
    }

    @Test
    public void writerCanLogicallyDeleteCurrentObservation() {
        writer();
        assertEquals(204, controller.eliminarObservacion(FUA_UUID, OBS_UUID).getStatusCodeValue());
        assertFalse(observation.getActivo());
        assertNotNull(observation.getFechaActualizacion());
        assertEquals("original", observation.getObservacion());
        assertEquals(OBS_UUID, observation.getUuid());
        assertEquals(Integer.valueOf(2), observation.getVersion());
        assertEquals(1, dao.writes);
        assertNull(dao.getFuaObservacion(OBS_UUID));
    }

    @Test
    public void writerCannotModifyHistoricalObservations() {
        writer();
        observation.setVersion(1);
        assertEquals(409, controller.editarObservacion(FUA_UUID, OBS_UUID, body()).getStatusCodeValue());
        assertEquals(409, controller.eliminarObservacion(FUA_UUID, OBS_UUID).getStatusCodeValue());
        assertUnchanged();
    }

    @Test
    public void writerCannotModifyObservationFromAnotherFuaOrMissingOrInactiveObservation() {
        writer();
        assertEquals(404, controller.editarObservacion("other-fua", OBS_UUID, body()).getStatusCodeValue());
        assertEquals(404, controller.eliminarObservacion("other-fua", OBS_UUID).getStatusCodeValue());
        assertEquals(404, controller.editarObservacion(FUA_UUID, "missing", body()).getStatusCodeValue());
        assertEquals(404, controller.eliminarObservacion(FUA_UUID, "missing").getStatusCodeValue());
        assertUnchanged();
        observation.setActivo(false);
        observation.mutations = 0;
        assertEquals(404, controller.editarObservacion(FUA_UUID, OBS_UUID, body()).getStatusCodeValue());
        assertEquals(404, controller.eliminarObservacion(FUA_UUID, OBS_UUID).getStatusCodeValue());
        assertEquals(0, observation.mutations);
        assertEquals(0, dao.writes);
    }

    @Test
    public void writerInvalidEditPayloadStillReturns400WithoutReadsOrChanges() {
        writer();
        Map<String, Object> invalid = body();
        invalid.put("observacion", " ");
        assertEquals(400, controller.editarObservacion(FUA_UUID, OBS_UUID, invalid).getStatusCodeValue());
        assertEquals(0, dao.reads);
        assertEquals(0, fuaReads);
        assertUnchanged();
    }

    @Test
    public void existingReaderCanStillListAndCreateWithoutUpdatePrivilege() {
        actor(true, FuaConfig.READ_FUA_PRIVILEGE);
        assertEquals(200, controller.listarObservaciones(FUA_UUID, null).getStatusCodeValue());
        assertEquals(200, controller.listarObservaciones(FUA_UUID, 2).getStatusCodeValue());
        assertEquals(201, controller.crearObservacion(FUA_UUID, body()).getStatusCodeValue());
        assertEquals(1, dao.writes);
        assertEquals(0, observation.mutations);
    }

    private void assertDenied(Class<? extends RuntimeException> expected, int httpStatus) {
        RuntimeException edit = assertThrows(expected,
                () -> controller.editarObservacion(FUA_UUID, OBS_UUID, body()));
        RuntimeException delete = assertThrows(expected,
                () -> controller.eliminarObservacion(FUA_UUID, OBS_UUID));
        for (RuntimeException exception : Arrays.asList(edit, delete)) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            new ResponseStatusExceptionResolver().resolveException(new MockHttpServletRequest(), response,
                    controller, exception);
            assertEquals(httpStatus, response.getStatus());
        }
        assertEquals(0, dao.reads);
        assertEquals(0, fuaReads);
        assertUnchanged();
    }

    private void assertUnchanged() {
        assertEquals(0, observation.mutations);
        assertEquals(0, dao.writes);
        assertEquals("original", observation.getObservacion());
        assertEquals("original actor", observation.getUsuario());
        assertNull(observation.getFechaActualizacion());
        assertTrue(observation.getActivo());
    }

    private void writer() {
        actor(true, FuaConfig.READ_FUA_PRIVILEGE, FuaConfig.UPDATE_FUA_PRIVILEGE);
    }

    private void actor(final boolean authenticated, final String... privileges) {
        Context.setUserContext(new UserContext(null) {
            @Override
            public User getAuthenticatedUser() { return authenticated ? new User(100) : null; }
            @Override
            public boolean isAuthenticated() { return authenticated; }
            @Override
            public boolean hasPrivilege(String privilege) {
                return authenticated && Arrays.asList(privileges).contains(privilege);
            }
        });
    }

    private Map<String, Object> body() {
        Map<String, Object> body = new HashMap<>();
        body.put("observacion", "updated");
        body.put("usuario", "payload actor");
        return body;
    }

    private Object authorizedProxy(Class<?> service, Object target) {
        ProxyFactory proxy = new ProxyFactory();
        proxy.setInterfaces(service);
        proxy.setTarget(target);
        proxy.addAdvice(new AuthorizationAdvice());
        return proxy.getProxy();
    }

    private class InMemoryObservationDao extends FuaObservacionDao {
        int reads;
        int writes;

        @Override
        public FuaObservacion getFuaObservacion(String uuid) {
            reads++;
            return OBS_UUID.equals(uuid) && observation.getActivo() ? observation : null;
        }

        @Override
        public FuaObservacion saveFuaObservacion(FuaObservacion value) {
            writes++;
            return value;
        }

        @Override
        public List<FuaObservacion> getFuaObservaciones(String uuid) {
            reads++;
            return Collections.<FuaObservacion>singletonList(observation);
        }

        @Override
        public List<FuaObservacion> getFuaObservaciones(String uuid, Integer version) {
            return getFuaObservaciones(uuid);
        }
    }

    private static class TrackingObservation extends FuaObservacion {
        int mutations;
        @Override
        public void setObservacion(String value) { mutations++; super.setObservacion(value); }
        @Override
        public void setUsuario(String value) { mutations++; super.setUsuario(value); }
        @Override
        public void setActivo(Boolean value) { mutations++; super.setActivo(value); }
        @Override
        public void setFechaActualizacion(Date value) { mutations++; super.setFechaActualizacion(value); }
    }
}
