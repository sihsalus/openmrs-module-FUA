package org.openmrs.module.fua.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.fua.FuaConfig;
import org.openmrs.module.fua.web.utils.MultipartInputStreamFileResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.util.UriUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
public class FuaRedirectionController {

        protected final Log log = LogFactory.getLog(getClass());
        
        protected final RestTemplate restTemplate = new RestTemplate();


    @RequestMapping(value = "/FUAFormat", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> redirectFuaRequest(
            @RequestParam("name") String name,
            @RequestParam("createdBy") String createdBy,
            @RequestParam("formatPayload") MultipartFile formatPayload
    ) throws IOException {

        HttpHeaders headers = new HttpHeaders();
        headers.set("fuagentoken", "fuagenerator");
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("name", name);
        body.add("createdBy", createdBy);
        body.add("formatPayload", new MultipartInputStreamFileResource(
                formatPayload.getInputStream(),
                formatPayload.getOriginalFilename(),
                formatPayload.getSize()
        ));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();
        String baseUrl = getFuaGeneratorBaseUrl();
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/ws/FUAFormat", requestEntity, String.class);
        //ResponseEntity<String> response = restTemplate.postForEntity("http://localhost:3000/ws/FUAFormat", requestEntity, String.class);
        //ResponseEntity<String> response = restTemplate.postForEntity("http://hii1sc-dev.inf.pucp.edu.pe/services/fua-generator/ws/FUAFormat", requestEntity, String.class);

        return ResponseEntity
                .status(response.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.getBody());
    }

    @RequestMapping(value = "/FUAFormat", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> redirectFuaFormatGetRequest() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("fuagentoken", "fuagenerator");

        HttpEntity<Void> requestEntity = new HttpEntity<Void>(headers);

        RestTemplate restTemplate = new RestTemplate();
        String baseUrl = getFuaGeneratorBaseUrl();
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/ws/FUAFormat",
                HttpMethod.GET,
                requestEntity,
                String.class);

        String responseBody = response.getBody();
        if (org.apache.commons.lang3.StringUtils.isNotBlank(responseBody)) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);

            JsonNode results = root.get("results");
            if (results != null && results.isArray()) {
                for (JsonNode item : results) {
                    if (item.isObject()) {
                        ((ObjectNode) item).remove("content");
                    }
                }
            }

            root.findParents("content").forEach(node -> ((ObjectNode) node).remove("content"));

            responseBody = mapper.writeValueAsString(root);
        }

        return ResponseEntity
                .status(response.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseBody);
    }

    @RequestMapping(value = "/FUAFormat/{id}/render", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> redirectFuaFormatRenderRequest(@PathVariable("id") String id) {
        String remoteUrl = null;
        try {
            log.info("Renderizando FUAFormat para id: " + id);

            HttpHeaders headers = new HttpHeaders();
            headers.set("fuagentoken", "fuagenerator");

            HttpEntity<Void> requestEntity = new HttpEntity<Void>(headers);

            RestTemplate restTemplate = new RestTemplate();
            String baseUrl = getFuaGeneratorBaseUrl();
            remoteUrl = baseUrl + "/ws/FUAFormat/"
                    + UriUtils.encodePathSegment(id, StandardCharsets.UTF_8)
                    + "/render";

            log.info("Llamando a microservicio FUA Generator: " + remoteUrl);

            ResponseEntity<String> response = restTemplate.exchange(
                    remoteUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class);

            log.info("Respuesta de FUA Generator para FUAFormat render. Status: " + response.getStatusCode());

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Error renderizando FUAFormat. Status: " + response.getStatusCode()
                        + ", body: " + response.getBody());
                return ResponseEntity
                        .status(response.getStatusCode())
                        .contentType(MediaType.TEXT_HTML)
                        .body("<h2>Error renderizando FUAFormat</h2><pre>Status: "
                                + response.getStatusCode()
                                + "\nURL: "
                                + remoteUrl
                                + "\n\n"
                                + response.getBody()
                                + "</pre>");
            }

            return ResponseEntity
                    .ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(response.getBody());
        } catch (HttpStatusCodeException ex) {
            log.error("Error HTTP renderizando FUAFormat. Id: " + id
                    + ", URL: " + remoteUrl
                    + ", status: " + ex.getStatusCode()
                    + ", body: " + ex.getResponseBodyAsString(), ex);
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .contentType(MediaType.TEXT_HTML)
                    .body("<h2>Error renderizando FUAFormat</h2><pre>Status: "
                            + ex.getStatusCode()
                            + "\nURL: "
                            + remoteUrl
                            + "\n\n"
                            + ex.getResponseBodyAsString()
                            + "</pre>");
        } catch (Exception ex) {
            log.error("Error interno renderizando FUAFormat. Id: " + id + ", URL: " + remoteUrl, ex);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_HTML)
                    .body("<h2>Error interno renderizando FUAFormat</h2><pre>URL: "
                            + remoteUrl
                            + "\n\n"
                            + ex.getMessage()
                            + "</pre>");
        }
    }

        // Método para obtener la URL base del generador FUA
    private String getFuaGeneratorBaseUrl() {
		String url = Context.getAdministrationService()
				.getGlobalProperty(FuaConfig.FUA_GENERATOR_URL_GP);
		
		if (org.apache.commons.lang3.StringUtils.isBlank(url)) {
			url = FuaConfig.FUA_GENERATOR_URL_DEFAULT;
			log.warn("Global property " + FuaConfig.FUA_GENERATOR_URL_GP 
					+ " not set, using default: " + url);
		}
		
		return url;
	}


    
}
