package org.openmrs.module.fua.web.controller;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.fua.Fua;
import org.openmrs.module.fua.FuaObservacion;
import org.openmrs.module.fua.api.FuaObservacionService;
import org.openmrs.module.fua.api.FuaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/module/fua/observacion")
public class FuaObservacionController {

	@Autowired
	private FuaObservacionService fuaObservacionService;

	@Autowired
	private FuaService fuaService;

	@RequestMapping(value = "/{fuaUuid}", method = RequestMethod.POST,
			consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<?> crearObservacion(@PathVariable("fuaUuid") String fuaUuid,
			@RequestBody Map<String, Object> body) {
		Object texto = body != null ? body.get("observacion") : null;
		Object nombreUsuario = body != null ? body.get("usuario") : null;
		if (!(texto instanceof String) || StringUtils.isBlank((String) texto) || ((String) texto).length() > 500) {
			return ResponseEntity.badRequest().body("'observacion' debe tener entre 1 y 500 caracteres.");
		}
		if (!(nombreUsuario instanceof String) || StringUtils.isBlank((String) nombreUsuario)
				|| ((String) nombreUsuario).length() > 50) {
			return ResponseEntity.badRequest().body("'usuario' debe tener entre 1 y 50 caracteres.");
		}

		Fua fua = fuaService.getFuaByUuid(fuaUuid);
		if (fua == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("FUA no encontrado con UUID: " + fuaUuid);
		}

		FuaObservacion observacion = new FuaObservacion();
		observacion.setFuaUuid(fua.getUuid());
		observacion.setObservacion((String) texto);
		observacion.setUsuario((String) nombreUsuario);
		observacion.setVersion(fua.getVersion());
		observacion.setActivo(true);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(fuaObservacionService.saveFuaObservacion(observacion));
	}

	@RequestMapping(value = "/{fuaUuid}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<?> listarObservaciones(@PathVariable("fuaUuid") String fuaUuid,
			@RequestParam(value = "version", required = false) Integer version) {
		Fua fua = fuaService.getFuaByUuid(fuaUuid);
		if (fua == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("FUA no encontrado con UUID: " + fuaUuid);
		}
		Integer versionConsultada = version != null ? version : fua.getVersion();
		if (versionConsultada == null || versionConsultada < 1) {
			return ResponseEntity.badRequest().body("La versión debe ser un entero positivo.");
		}
		List<FuaObservacion> observaciones = fuaObservacionService.getFuaObservaciones(fuaUuid, versionConsultada);
		return ResponseEntity.ok(observaciones);
	}
}
