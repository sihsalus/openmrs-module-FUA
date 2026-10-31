package org.openmrs.module.fua.web.controller;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.fua.Fua;
import org.openmrs.module.fua.FuaObservacion;
import org.openmrs.module.fua.FuaConfig;
import org.openmrs.module.fua.api.FuaObservacionService;
import org.openmrs.module.fua.api.FuaService;
import org.openmrs.module.fua.web.utils.FuaAccess;
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
		if (version != null && version < 1) {
			return ResponseEntity.badRequest().body("La versión debe ser un entero positivo.");
		}
		List<FuaObservacion> observaciones = version == null
				? fuaObservacionService.getFuaObservaciones(fuaUuid)
				: fuaObservacionService.getFuaObservaciones(fuaUuid, version);
		return ResponseEntity.ok(observaciones);
	}

	@RequestMapping(value = "/editar/{fuaUuid}/{observacionUuid}", method = RequestMethod.PUT,
			consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<?> editarObservacion(@PathVariable("fuaUuid") String fuaUuid,
			@PathVariable("observacionUuid") String observacionUuid, @RequestBody Map<String, Object> body) {
		FuaAccess.require(FuaConfig.UPDATE_FUA_PRIVILEGE);
		FuaAccess.require(FuaConfig.READ_FUA_PRIVILEGE);
		Object texto = body != null ? body.get("observacion") : null;
		Object nombreUsuario = body != null ? body.get("usuario") : null;
		if (!(texto instanceof String) || StringUtils.isBlank((String) texto) || ((String) texto).length() > 500) {
			return ResponseEntity.badRequest().body("'observacion' debe tener entre 1 y 500 caracteres.");
		}
		if (!(nombreUsuario instanceof String) || StringUtils.isBlank((String) nombreUsuario)
				|| ((String) nombreUsuario).length() > 50) {
			return ResponseEntity.badRequest().body("'usuario' debe tener entre 1 y 50 caracteres.");
		}
		FuaObservacion observacion = fuaObservacionService.getFuaObservacion(observacionUuid);
		if (observacion == null || !fuaUuid.equals(observacion.getFuaUuid())) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Observación no encontrada.");
		}
		Fua fua = fuaService.getFuaByUuid(fuaUuid);
		if (fua == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("FUA no encontrado con UUID: " + fuaUuid);
		}
		if (!fua.getVersion().equals(observacion.getVersion())) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body("Solo se pueden editar observaciones de la versión actual del FUA.");
		}
		observacion.setObservacion((String) texto);
		observacion.setUsuario((String) nombreUsuario);
		observacion.setFechaActualizacion(new java.util.Date());
		return ResponseEntity.ok(fuaObservacionService.saveFuaObservacion(observacion));
	}

	@RequestMapping(value = "/eliminar/{fuaUuid}/{observacionUuid}", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<?> eliminarObservacion(@PathVariable("fuaUuid") String fuaUuid,
			@PathVariable("observacionUuid") String observacionUuid) {
		FuaAccess.require(FuaConfig.UPDATE_FUA_PRIVILEGE);
		FuaAccess.require(FuaConfig.READ_FUA_PRIVILEGE);
		FuaObservacion observacion = fuaObservacionService.getFuaObservacion(observacionUuid);
		if (observacion == null || !fuaUuid.equals(observacion.getFuaUuid())) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Observación no encontrada.");
		}
		Fua fua = fuaService.getFuaByUuid(fuaUuid);
		if (fua == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("FUA no encontrado con UUID: " + fuaUuid);
		}
		if (!fua.getVersion().equals(observacion.getVersion())) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body("Solo se pueden eliminar observaciones de la versión actual del FUA.");
		}
		observacion.setActivo(false);
		observacion.setFechaActualizacion(new java.util.Date());
		fuaObservacionService.saveFuaObservacion(observacion);
		return ResponseEntity.noContent().build();
	}
}
