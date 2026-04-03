package org.openmrs.module.fua.api;

import org.openmrs.annotation.Authorized;
import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.fua.Fua;
import org.openmrs.module.fua.FuaEstado;
import org.openmrs.module.fua.FuaConfig;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface FuaService extends OpenmrsService {
	
	@Authorized(FuaConfig.READ_FUA_PRIVILEGE)
	@Transactional(readOnly = true)
	List<Fua> getAllFuas() throws APIException;
	
	@Authorized(FuaConfig.READ_FUA_PRIVILEGE)
	@Transactional(readOnly = true)
	Fua getFua(Integer fuaId) throws APIException;
	
	@Authorized(FuaConfig.MANAGE_FUA_PRIVILEGE)
	@Transactional
	Fua saveFua(Fua fua) throws APIException;
	
	@Authorized(FuaConfig.DELETE_FUA_PRIVILEGE)
	@Transactional
	void purgeFua(Fua fua) throws APIException;
	
	@Authorized(FuaConfig.UPDATE_FUA_PRIVILEGE)
	@Transactional
	Fua updateEstadoFua(Integer fuaId, FuaEstado nuevoEstado) throws APIException;
	
	@Authorized(FuaConfig.READ_FUA_PRIVILEGE)
	@Transactional(readOnly = true)
	Fua getFuaByUuid(String uuid) throws APIException;
	
	@Authorized(FuaConfig.READ_FUA_PRIVILEGE)
	@Transactional(readOnly = true)
	Fua getFuaByVisitUuid(String visitUuid) throws APIException;
	
	@Authorized(FuaConfig.READ_FUA_PRIVILEGE)
	@Transactional(readOnly = true)
	Fua getFuaById(Integer id) throws APIException;
	
	@Authorized(FuaConfig.READ_FUA_PRIVILEGE)
	@Transactional(readOnly = true)
	List<Fua> getFuasFiltrados(String estado, LocalDate inicio, LocalDate fin, int page, int size);

	@Authorized(FuaConfig.READ_FUA_PRIVILEGE)
	@Transactional(readOnly = true)
	List<Fua> getFuasByPatientUuid(String patientUuid) throws APIException;

}
