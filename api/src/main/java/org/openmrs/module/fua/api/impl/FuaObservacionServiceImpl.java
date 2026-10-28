package org.openmrs.module.fua.api.impl;

import org.openmrs.api.APIException;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.fua.FuaObservacion;
import org.openmrs.module.fua.api.FuaObservacionService;
import org.openmrs.module.fua.api.dao.FuaObservacionDao;

import java.util.List;

public class FuaObservacionServiceImpl extends BaseOpenmrsService implements FuaObservacionService {

	private FuaObservacionDao dao;

	public void setDao(FuaObservacionDao dao) {
		this.dao = dao;
	}

	@Override
	public FuaObservacion saveFuaObservacion(FuaObservacion observacion) throws APIException {
		return dao.saveFuaObservacion(observacion);
	}

	@Override
	public FuaObservacion getFuaObservacion(String uuid) throws APIException {
		return dao.getFuaObservacion(uuid);
	}

	@Override
	public List<FuaObservacion> getFuaObservaciones(String fuaUuid, Integer version) throws APIException {
		return dao.getFuaObservaciones(fuaUuid, version);
	}

	@Override
	public List<FuaObservacion> getFuaObservaciones(String fuaUuid) throws APIException {
		return dao.getFuaObservaciones(fuaUuid);
	}
}
