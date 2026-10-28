package org.openmrs.module.fua;

import org.openmrs.BaseOpenmrsObject;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "fua_observacion")
public class FuaObservacion extends BaseOpenmrsObject implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "fua_observacion_id")
	private Integer id;

	@Column(name = "uuid", unique = true, nullable = false, length = 38)
	private String uuid;

	@Column(name = "fua_uuid", nullable = false, length = 38)
	private String fuaUuid;

	@Column(name = "observacion", nullable = false, length = 500)
	private String observacion;

	@Column(name = "usuario", nullable = false, length = 50)
	private String usuario;

	@Column(name = "version", nullable = false)
	private Integer version;

	@Column(name = "activo", nullable = false)
	private Boolean activo = true;

	@Column(name = "fecha_creacion", nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date fechaCreacion;

	@Column(name = "fecha_actualizacion", nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date fechaActualizacion;

	public FuaObservacion() {
		this.uuid = UUID.randomUUID().toString();
		this.fechaCreacion = new Date();
		this.fechaActualizacion = this.fechaCreacion;
	}

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	public String getUuid() {
		return uuid;
	}

	@Override
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getFuaUuid() { return fuaUuid; }
	public void setFuaUuid(String fuaUuid) { this.fuaUuid = fuaUuid; }
	public String getObservacion() { return observacion; }
	public void setObservacion(String observacion) { this.observacion = observacion; }
	public String getUsuario() { return usuario; }
	public void setUsuario(String usuario) { this.usuario = usuario; }
	public Integer getVersion() { return version; }
	public void setVersion(Integer version) { this.version = version; }
	public Boolean getActivo() { return activo; }
	public void setActivo(Boolean activo) { this.activo = activo; }
	public Date getFechaCreacion() { return fechaCreacion; }
	public void setFechaCreacion(Date fechaCreacion) { this.fechaCreacion = fechaCreacion; }
	public Date getFechaActualizacion() { return fechaActualizacion; }
	public void setFechaActualizacion(Date fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}
