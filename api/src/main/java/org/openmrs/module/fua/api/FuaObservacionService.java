package org.openmrs.module.fua.api;

import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.fua.FuaObservacion;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FuaObservacionService extends OpenmrsService {

	@Transactional
	FuaObservacion saveFuaObservacion(FuaObservacion observacion) throws APIException;

	@Transactional(readOnly = true)
	FuaObservacion getFuaObservacion(String uuid) throws APIException;

	@Transactional(readOnly = true)
	List<FuaObservacion> getFuaObservaciones(String fuaUuid, Integer version) throws APIException;

	@Transactional(readOnly = true)
	List<FuaObservacion> getFuaObservaciones(String fuaUuid) throws APIException;
}
