package org.openmrs.module.fua.api.dao;

import org.hibernate.Query;
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
		getSession().saveOrUpdate(observacion);
		return observacion;
	}

	public FuaObservacion getFuaObservacion(String uuid) {
		return (FuaObservacion) getSession().createQuery(
				"FROM FuaObservacion fo WHERE fo.uuid = :uuid AND fo.activo = true")
				.setParameter("uuid", uuid).uniqueResult();
	}

	@SuppressWarnings("unchecked")
	public List<FuaObservacion> getFuaObservaciones(String fuaUuid, Integer version) {
		return getSession().createQuery(
				"FROM FuaObservacion fo WHERE fo.fuaUuid = :fuaUuid AND fo.version = :version "
						+ "AND fo.activo = true ORDER BY fo.id ASC")
				.setParameter("fuaUuid", fuaUuid)
				.setParameter("version", version)
				.list();
	}

	@SuppressWarnings("unchecked")
	public List<FuaObservacion> getFuaObservaciones(String fuaUuid) {
		return getSession().createQuery(
				"FROM FuaObservacion fo WHERE fo.fuaUuid = :fuaUuid AND fo.activo = true ORDER BY fo.id ASC")
				.setParameter("fuaUuid", fuaUuid).list();
	}

	@SuppressWarnings("unchecked")
	public List<FuaObservacion> getTodasFuaObservaciones(String fuaUuid, Integer version) {
		String query = "FROM FuaObservacion fo WHERE fo.fuaUuid = :fuaUuid ";
		if (version != null) {
			query += "AND fo.version = :version ";
		}
		query += "ORDER BY CASE WHEN fo.activo = true THEN 0 ELSE 1 END ASC, fo.id ASC";
		Query hibernateQuery = getSession().createQuery(query).setParameter("fuaUuid", fuaUuid);
		if (version != null) {
			hibernateQuery.setParameter("version", version);
		}
		return hibernateQuery.list();
	}
}
