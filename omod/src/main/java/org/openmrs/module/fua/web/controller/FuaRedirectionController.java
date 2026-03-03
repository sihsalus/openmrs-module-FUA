package org.openmrs.module.fua.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.fua.FuaConfig;
import org.openmrs.module.fua.web.utils.MultipartInputStreamFileResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

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
