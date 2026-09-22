package org.openmrs.module.fua.web.controller;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.User;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.fua.Fua;
import org.openmrs.module.fua.FuaConfig;
import org.openmrs.module.fua.api.FuaService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import static org.junit.Assert.*;

/** Capture real controller request construction; no network or patient data is used. */
public class FuaGeneratorRequestTest {

    private AdministrationService previousAdministration;
    private final List<HttpEntity<?>> requests = new ArrayList<>();
    private final List<String> urls = new ArrayList<>();
    private String header = "X-Synthetic-Fua";
    private String token = "synthetic-token-one";
    private boolean failTransport;

    @Before
    public void setUp() {
        try {
            previousAdministration = ServiceContext.getInstance().getAdministrationService();
        } catch (APIException notConfigured) {
            previousAdministration = null;
        }
        AdministrationService administration = (AdministrationService) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] { AdministrationService.class }, (proxy, method, args) -> {
                    if (!"getGlobalProperty".equals(method.getName())) {
                        throw new AssertionError("Unexpected administration operation");
                    }
                    String key = (String) args[0];
                    if (FuaConfig.FUA_GENERATOR_HEADER_NAME_GP.equals(key)) return header;
                    if (FuaConfig.FUA_GENERATOR_HEADER_VALUE_GP.equals(key)) return token;
                    if (FuaConfig.FUA_GENERATOR_URL_GP.equals(key)) return "https://generator.example.invalid";
                    if (FuaConfig.FUA_GENERATOR_IDENTIFIER.equals(key)) return "synthetic-format";
                    throw new AssertionError("Unexpected configuration property");
                });
        ServiceContext.getInstance().setAdministrationService(administration);
        Context.setUserContext(new UserContext(null) {
            @Override
            public User getAuthenticatedUser() { return new User(100); }
            @Override
            public boolean isAuthenticated() { return true; }
            @Override
            public boolean hasPrivilege(String privilege) {
                return "Read Fua".equals(privilege) || "Manage Fua".equals(privilege);
            }
        });
    }

    @After
    public void tearDown() {
        Context.clearUserContext();
        ServiceContext.getInstance().setAdministrationService(previousAdministration);
    }

    @Test
    public void everyGeneratorFlowUsesCurrentConfiguredAuthentication() throws Exception {
        Fua fua = new Fua();
        fua.setFuaGeneratorUuid("synthetic-generator-id");
        fua.setPayload("{}");
        FuaController controller = new FuaController();
        Field service = FuaController.class.getDeclaredField("fuaService");
        service.setAccessible(true);
        service.set(controller, Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { FuaService.class },
                (proxy, method, args) -> fua));
        controller.restTemplate = recordingTransport();
        FuaRedirectionController formats = new FuaRedirectionController();
        formats.restTemplate = recordingTransport();

        assertEquals(HttpStatus.OK, formats.redirectFuaFormatGetRequest().getStatusCode());
        assertEquals(HttpStatus.OK, formats.redirectFuaRequest("Synthetic", "Synthetic",
                new MockMultipartFile("formatPayload", "synthetic.txt", "text/plain", new byte[] { 1 })).getStatusCode());
        assertEquals(HttpStatus.OK, formats.redirectFuaFormatRenderRequest("synthetic-format").getStatusCode());
        assertEquals(HttpStatus.OK, controller.renderVisitInfo("synthetic-visit", "synthetic-format").getStatusCode());
        assertEquals(HttpStatus.OK, controller.renderFua("synthetic-visit").getStatusCode());
        assertEquals(HttpStatus.OK, controller.generateFuaPDF("synthetic-visit").getStatusCode());
        Method create = FuaController.class.getDeclaredMethod("generarFuadeFuaGenerator", Fua.class);
        create.setAccessible(true);
        assertEquals("synthetic-result", create.invoke(controller, fua));

        assertEquals(7, requests.size());
        for (HttpEntity<?> request : requests) {
            assertEquals("synthetic-token-one", request.getHeaders().getFirst("X-Synthetic-Fua"));
        }
        assertTrue(urls.contains("https://generator.example.invalid/ws/FUAFromVisit/synthetic-generator-id/generatePDF"));
        header = "Authorization";
        token = "Bearer synthetic-token-two";
        assertEquals(HttpStatus.OK, controller.generateFuaPDF("synthetic-visit").getStatusCode());
        assertEquals("Bearer synthetic-token-two", requests.get(7).getHeaders().getFirst("Authorization"));
        assertFalse(requests.get(7).getHeaders().containsKey("X-Synthetic-Fua"));
    }

    @Test
    public void formatFailuresDoNotExposeUpstreamBodiesOrCredentials() throws Exception {
        failTransport = true;
        FuaRedirectionController formats = new FuaRedirectionController();
        formats.restTemplate = recordingTransport();
        List<ResponseEntity<String>> responses = new ArrayList<>();
        responses.add(formats.redirectFuaFormatGetRequest());
        responses.add(formats.redirectFuaRequest("Synthetic", "Synthetic",
                new MockMultipartFile("formatPayload", new byte[] { 1 })));
        responses.add(formats.redirectFuaFormatRenderRequest("synthetic-format"));
        for (ResponseEntity<String> response : responses) {
            assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
            assertFalse(response.getBody().contains("synthetic-sensitive-upstream"));
            assertFalse(response.getBody().contains(token));
        }
    }

    private RestTemplate recordingTransport() {
        return new RestTemplate() {
            @Override
            public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> request,
                    Class<T> type, Object... variables) {
                return record(url, request, type);
            }

            @Override
            public <T> ResponseEntity<T> postForEntity(String url, Object request, Class<T> type, Object... variables) {
                return record(url, (HttpEntity<?>) request, type);
            }

            private <T> ResponseEntity<T> record(String url, HttpEntity<?> request, Class<T> type) {
                requests.add(request);
                urls.add(url);
                if (failTransport) {
                    throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "synthetic-sensitive-upstream",
                            ("synthetic-sensitive-upstream " + token).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                            java.nio.charset.StandardCharsets.UTF_8);
                }
                Object body = type == byte[].class ? new byte[] { 1, 2 } : "{\"uuid\":\"synthetic-result\",\"results\":[]}";
                return new ResponseEntity<>(type.cast(body), HttpStatus.OK);
            }
        };
    }
}
