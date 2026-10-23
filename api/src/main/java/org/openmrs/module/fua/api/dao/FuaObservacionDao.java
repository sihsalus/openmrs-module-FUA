package org.openmrs.module.fua.api.dao;

import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.fua.FuaObservacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("fua.FuaObservacionDao")
public class FuaObservacionDao {

	@Autowired
	private DbSessionFactory sessionFactory;

	private DbSession getSession() {
		return sessionFactory.getCurrentSession();
	}

	public FuaObservacion saveFuaObservacion(FuaObservacion observacion) {
		getSession().save(observacion);
		return observacion;
	}

	@SuppressWarnings("unchecked")
	public List<FuaObservacion> getFuaObservaciones(String fuaUuid, Integer version) {
		return getSession().createQuery(
				"FROM FuaObservacion fo WHERE fo.fuaUuid = :fuaUuid AND fo.version = :version "
						+ "AND fo.activo = true ORDER BY fo.fechaCreacion DESC, fo.id DESC")
				.setParameter("fuaUuid", fuaUuid)
				.setParameter("version", version)
				.list();
	}
}
