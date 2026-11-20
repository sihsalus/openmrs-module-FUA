package org.openmrs.module.fua.web.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;

/** Carries only bounded field locations across the generator boundary, never its raw error body. */
public final class FuaGeneratorValidationResponse {

    private static final Set<String> FIELDS = new HashSet<String>(Arrays.asList(
            "pages", "sections", "fields", "versionTag", "versionNumber", "codeName", "name", "content",
            "pageNumber", "height", "width", "padding_top", "padding_left", "extraStyles", "top", "left",
            "bodyHeight", "bodyWidth", "titleHeight", "title", "showTitle", "showLabel", "label",
            "labelHeight", "labelWidth", "labelPosition", "labelExtraStyles", "valueType"));

    private FuaGeneratorValidationResponse() { }

    public static ResponseEntity<String> from(Exception failure) {
        if (!(failure instanceof HttpStatusCodeException)) return null;
        HttpStatusCodeException response = (HttpStatusCodeException) failure;
        if (response.getStatusCode() != HttpStatus.UNPROCESSABLE_ENTITY) return null;
        String body = response.getResponseBodyAsString();
        if (body == null || body.length() > 32768) return null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode upstream = mapper.readTree(body);
            if (upstream == null || !"INVALID_FUA_FORMAT".equals(upstream.path("error").asText())) return null;
            JsonNode details = upstream.path("details");
            if (!details.isArray() || details.size() == 0 || details.size() > 100) return null;
            ObjectNode safe = mapper.createObjectNode();
            safe.put("error", "INVALID_FUA_FORMAT");
            safe.put("message", "El formato FUA contiene campos inválidos.");
            ArrayNode safeDetails = safe.putArray("details");
            for (JsonNode issue : details) {
                JsonNode path = issue.path("path");
                if (!path.isArray() || path.size() > 32) return null;
                ObjectNode safeIssue = safeDetails.addObject();
                ArrayNode safePath = safeIssue.putArray("path");
                for (JsonNode segment : path) {
                    if (segment.isIntegralNumber() && segment.canConvertToInt() && segment.asInt() >= 0 && segment.asInt() <= 100000) {
                        safePath.add(segment.asInt());
                    } else if (segment.isTextual() && FIELDS.contains(segment.asText())) {
                        safePath.add(segment.asText());
                    } else {
                        return null;
                    }
                }
                safeIssue.put("code", "invalid_format");
                safeIssue.put("message", path.size() > 0 && "versionTag".equals(path.get(path.size() - 1).asText())
                        ? "versionTag debe ser una cadena de texto."
                        : "El campo no cumple el formato requerido.");
            }
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .contentType(MediaType.APPLICATION_JSON).body(mapper.writeValueAsString(safe));
        } catch (Exception invalidBody) {
            return null;
        }
    }
}
